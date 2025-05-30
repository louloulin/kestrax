package io.kestra.blueprint.service

import io.kestra.blueprint.dto.CreateBlueprintRequest
import io.kestra.blueprint.dto.UpdateBlueprintRequest
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.Namespace
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintVersionRepository
import io.kestra.blueprint.repository.NamespaceRepository
import io.kestra.blueprint.security.TenantContext
import io.micronaut.data.model.Pageable
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

/**
 * 蓝图服务测试类
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlueprintServiceTest {
    
    @Inject
    lateinit var blueprintService: BlueprintService
    
    @Inject
    lateinit var blueprintRepository: BlueprintRepository
    
    @Inject
    lateinit var blueprintVersionRepository: BlueprintVersionRepository
    
    @Inject
    lateinit var namespaceRepository: NamespaceRepository
    
    @Inject
    lateinit var tenantContext: TenantContext
    
    private lateinit var testNamespace: Namespace
    
    @BeforeEach
    fun setup() {
        // 创建测试命名空间
        testNamespace = Namespace(
            name = "test-namespace",
            tenantId = "test-tenant",
            description = "Test namespace for blueprint tests"
        )
        testNamespace = namespaceRepository.save(testNamespace)
        
        // 设置租户上下文
        tenantContext.setTenantId("test-tenant")
        tenantContext.setNamespaceId(testNamespace.id)
        tenantContext.setUserId("test-user")
    }
    
    @AfterEach
    fun cleanup() {
        // 清理测试数据
        blueprintVersionRepository.deleteAll()
        blueprintRepository.deleteAll()
        namespaceRepository.deleteAll()
        
        // 清理租户上下文
        tenantContext.clear()
    }
    
    @Test
    fun `test create blueprint`() {
        // 准备测试数据
        val request = CreateBlueprintRequest(
            title = "Test Blueprint",
            description = "A test blueprint",
            content = """
                id: test-flow
                namespace: test
                tasks:
                  - id: hello
                    type: io.kestra.core.tasks.log.Log
                    message: Hello World!
            """.trimIndent(),
            tags = listOf("test", "example"),
            kind = "workflow",
            isPublic = false,
            isTemplate = false
        )
        
        // 执行测试
        val result = blueprintService.createBlueprint(request)
        
        // 验证结果
        assertNotNull(result)
        assertEquals("Test Blueprint", result.title)
        assertEquals("A test blueprint", result.description)
        assertEquals(testNamespace.id, result.namespaceId)
        assertEquals(listOf("test", "example"), result.tags)
        assertEquals("workflow", result.kind)
        assertEquals(false, result.isPublic)
        assertEquals(false, result.isTemplate)
        assertEquals("test-user", result.createdBy)
        assertEquals(1, result.version)
        
        // 验证数据库中的数据
        val savedBlueprint = blueprintRepository.findById(result.id).orElse(null)
        assertNotNull(savedBlueprint)
        assertEquals(result.title, savedBlueprint.title)
        
        // 验证版本记录
        val versions = blueprintVersionRepository.findByBlueprintIdOrderByVersionDesc(result.id)
        assertEquals(1, versions.size)
        assertEquals(1, versions[0].version)
    }
    
    @Test
    fun `test get blueprints with pagination`() {
        // 准备测试数据
        repeat(5) { index ->
            val blueprint = Blueprint(
                namespaceId = testNamespace.id,
                title = "Blueprint $index",
                content = "test content $index",
                createdBy = "test-user"
            )
            blueprintRepository.save(blueprint)
        }
        
        // 执行测试
        val pageable = Pageable.from(0, 3)
        val result = blueprintService.getBlueprints(pageable = pageable)
        
        // 验证结果
        assertNotNull(result)
        assertEquals(3, result.blueprints.size)
        assertEquals(5L, result.total)
        assertEquals(0, result.page)
        assertEquals(3, result.size)
    }
    
    @Test
    fun `test get blueprints with search`() {
        // 准备测试数据
        val blueprint1 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Search Test Blueprint",
            description = "This is a searchable blueprint",
            content = "test content",
            createdBy = "test-user"
        )
        val blueprint2 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Another Blueprint",
            description = "Different description",
            content = "test content",
            createdBy = "test-user"
        )
        
        blueprintRepository.save(blueprint1)
        blueprintRepository.save(blueprint2)
        
        // 执行搜索测试
        val result = blueprintService.getBlueprints(search = "Search")
        
        // 验证结果
        assertNotNull(result)
        assertEquals(1, result.blueprints.size)
        assertEquals("Search Test Blueprint", result.blueprints[0].title)
    }
    
    @Test
    fun `test get blueprint by id`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Test Blueprint",
            description = "Test description",
            content = "test content",
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 执行测试
        val result = blueprintService.getBlueprintById(savedBlueprint.id)
        
        // 验证结果
        assertNotNull(result)
        assertEquals(savedBlueprint.id, result.id)
        assertEquals("Test Blueprint", result.title)
        assertEquals("Test description", result.description)
        assertEquals("test content", result.content)
    }
    
    @Test
    fun `test get blueprint by id not found`() {
        // 执行测试并验证异常
        assertThrows<RuntimeException> {
            blueprintService.getBlueprintById("non-existent-id")
        }
    }
    
    @Test
    fun `test update blueprint`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Original Title",
            description = "Original description",
            content = "original content",
            version = 1,
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        val updateRequest = UpdateBlueprintRequest(
            title = "Updated Title",
            description = "Updated description",
            content = "updated content",
            tags = listOf("updated", "tag")
        )
        
        // 执行测试
        val result = blueprintService.updateBlueprint(savedBlueprint.id, updateRequest)
        
        // 验证结果
        assertNotNull(result)
        assertEquals("Updated Title", result.title)
        assertEquals("Updated description", result.description)
        assertEquals("updated content", result.content)
        assertEquals(listOf("updated", "tag"), result.tags)
        assertEquals(2, result.version) // 版本应该递增
        
        // 验证版本记录
        val versions = blueprintVersionRepository.findByBlueprintIdOrderByVersionDesc(result.id)
        assertEquals(2, versions.size)
        assertEquals(2, versions[0].version)
        assertEquals(1, versions[1].version)
    }
    
    @Test
    fun `test delete blueprint`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "To Be Deleted",
            content = "test content",
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 执行测试
        blueprintService.deleteBlueprint(savedBlueprint.id)
        
        // 验证结果
        assertFalse(blueprintRepository.existsById(savedBlueprint.id))
    }
    
    @Test
    fun `test get blueprint versions`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Versioned Blueprint",
            content = "original content",
            version = 1,
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 更新蓝图以创建新版本
        val updateRequest = UpdateBlueprintRequest(
            content = "updated content"
        )
        blueprintService.updateBlueprint(savedBlueprint.id, updateRequest)
        
        // 执行测试
        val versions = blueprintService.getBlueprintVersions(savedBlueprint.id)
        
        // 验证结果
        assertNotNull(versions)
        assertEquals(2, versions.size)
        assertEquals(2, versions[0].version) // 最新版本在前
        assertEquals(1, versions[1].version)
    }
    
    @Test
    fun `test get blueprint by version`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Versioned Blueprint",
            content = "original content",
            version = 1,
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 更新蓝图以创建新版本
        val updateRequest = UpdateBlueprintRequest(
            content = "updated content"
        )
        blueprintService.updateBlueprint(savedBlueprint.id, updateRequest)
        
        // 执行测试 - 获取版本1
        val version1 = blueprintService.getBlueprintByVersion(savedBlueprint.id, 1)
        
        // 验证结果
        assertNotNull(version1)
        assertEquals(1, version1.version)
        assertEquals("original content", version1.content)
        
        // 执行测试 - 获取版本2
        val version2 = blueprintService.getBlueprintByVersion(savedBlueprint.id, 2)
        
        // 验证结果
        assertNotNull(version2)
        assertEquals(2, version2.version)
        assertEquals("updated content", version2.content)
    }
    
    @Test
    fun `test create blueprint with duplicate title`() {
        // 先创建一个蓝图
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Duplicate Title",
            content = "test content",
            createdBy = "test-user"
        )
        blueprintRepository.save(blueprint)
        
        // 尝试创建同名蓝图
        val request = CreateBlueprintRequest(
            title = "Duplicate Title",
            content = "different content"
        )
        
        // 执行测试并验证异常
        assertThrows<RuntimeException> {
            blueprintService.createBlueprint(request)
        }
    }
    
    @Test
    fun `test filter blueprints by tags`() {
        // 准备测试数据
        val blueprint1 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Blueprint 1",
            content = "test content",
            tags = listOf("tag1", "common"),
            createdBy = "test-user"
        )
        val blueprint2 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Blueprint 2",
            content = "test content",
            tags = listOf("tag2", "common"),
            createdBy = "test-user"
        )
        val blueprint3 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Blueprint 3",
            content = "test content",
            tags = listOf("tag3"),
            createdBy = "test-user"
        )
        
        blueprintRepository.save(blueprint1)
        blueprintRepository.save(blueprint2)
        blueprintRepository.save(blueprint3)
        
        // 执行测试 - 按标签过滤
        val result = blueprintService.getBlueprints(tags = listOf("common"))
        
        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.blueprints.size)
        assertTrue(result.blueprints.all { it.tags.contains("common") })
    }
    
    @Test
    fun `test filter blueprints by type`() {
        // 准备测试数据
        val blueprint1 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Workflow Blueprint",
            content = "test content",
            kind = "workflow",
            createdBy = "test-user"
        )
        val blueprint2 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Template Blueprint",
            content = "test content",
            kind = "template",
            createdBy = "test-user"
        )
        
        blueprintRepository.save(blueprint1)
        blueprintRepository.save(blueprint2)
        
        // 执行测试 - 按类型过滤
        val result = blueprintService.getBlueprints(kind = "workflow")
        
        // 验证结果
        assertNotNull(result)
        assertEquals(1, result.blueprints.size)
        assertEquals("workflow", result.blueprints[0].kind)
        assertEquals("Workflow Blueprint", result.blueprints[0].title)
    }
}