package io.kestra.blueprint.service

import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.repository.BlueprintRepository
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * 官网蓝图同步服务测试
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfficialBlueprintSyncServiceTest {
    
    @Inject
    lateinit var officialBlueprintSyncService: OfficialBlueprintSyncService
    
    @Inject
    lateinit var blueprintRepository: BlueprintRepository
    
    @Test
    fun `test sync all blueprints`() = runBlocking {
        // 执行同步
        val result = officialBlueprintSyncService.syncAllBlueprints()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.successCount >= 0)
        assertTrue(result.failureCount >= 0)
        assertTrue(result.totalCount > 0)
        assertEquals(result.totalCount, result.successCount + result.failureCount)
        
        // 验证数据库中的蓝图
        if (result.successCount > 0) {
            val blueprints = blueprintRepository.findAll()
            assertTrue(blueprints.isNotEmpty())
            
            // 验证至少有一个官网蓝图
            val officialBlueprints = blueprints.filter { it.tags.contains("official") }
            assertTrue(officialBlueprints.isNotEmpty())
        }
    }
    
    @Test
    fun `test sync single blueprint`() = runBlocking {
        val officialBlueprint = OfficialBlueprint(
            id = "test-blueprint",
            title = "测试蓝图",
            description = "这是一个测试蓝图",
            content = """
                id: test-blueprint
                namespace: io.kestra.tests
                description: 测试蓝图
                
                tasks:
                  - id: hello
                    type: io.kestra.core.tasks.log.Log
                    message: Hello World!
            """.trimIndent(),
            tags = listOf("test", "official"),
            kind = "workflow"
        )
        
        // 执行单个蓝图同步
        val result = officialBlueprintSyncService.syncBlueprint(officialBlueprint)
        
        // 验证结果
        assertTrue(result)
        
        // 验证数据库中的蓝图
        val savedBlueprint = blueprintRepository.findById("test-blueprint")
        assertNotNull(savedBlueprint)
        assertEquals("测试蓝图", savedBlueprint?.title)
        assertTrue(savedBlueprint?.tags?.contains("official") == true)
        assertTrue(savedBlueprint?.isTemplate == true)
    }
    
    @Test
    fun `test get official blueprints`() {
        val blueprints = officialBlueprintSyncService.getOfficialBlueprints()
        
        // 验证预置的官网蓝图
        assertNotNull(blueprints)
        assertTrue(blueprints.isNotEmpty())
        assertEquals(5, blueprints.size)
        
        // 验证蓝图内容
        val businessProcess = blueprints.find { it.id == "business-process-approval" }
        assertNotNull(businessProcess)
        assertEquals("业务流程审批", businessProcess?.title)
        assertTrue(businessProcess?.tags?.contains("business") == true)
        assertTrue(businessProcess?.tags?.contains("approval") == true)
        assertTrue(businessProcess?.tags?.contains("official") == true)
    }
}