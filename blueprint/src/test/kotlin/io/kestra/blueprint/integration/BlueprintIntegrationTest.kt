package io.kestra.blueprint.integration

import io.kestra.blueprint.dto.CreateBlueprintRequest
import io.kestra.blueprint.dto.UpdateBlueprintRequest
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.Namespace
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintVersionRepository
import io.kestra.blueprint.repository.NamespaceRepository
import io.kestra.blueprint.service.BlueprintService
import io.kestra.blueprint.service.BlueprintTemplateService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * 蓝图模块集成测试类
 * 测试完整的端到端功能，包括控制器、服务层和数据访问层的集成
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlueprintIntegrationTest {
    
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    
    @Inject
    lateinit var blueprintService: BlueprintService
    
    @Inject
    lateinit var templateService: BlueprintTemplateService
    
    @Inject
    lateinit var blueprintRepository: BlueprintRepository
    
    @Inject
    lateinit var blueprintVersionRepository: BlueprintVersionRepository
    
    @Inject
    lateinit var namespaceRepository: NamespaceRepository
    
    private lateinit var testNamespace: Namespace
    private val authToken = "Bearer test-token"
    
    @BeforeEach
    fun setup() {
        // 创建测试命名空间
        testNamespace = Namespace(
            name = "integration-test-namespace",
            tenantId = "integration-test-tenant",
            description = "Integration test namespace"
        )
        testNamespace = namespaceRepository.save(testNamespace)
    }
    
    @AfterEach
    fun cleanup() {
        // 清理测试数据
        blueprintVersionRepository.deleteAll()
        blueprintRepository.deleteAll()
        namespaceRepository.deleteAll()
    }
    
    @Test
    fun `test complete blueprint lifecycle`() {
        // 1. 创建蓝图
        val createRequest = CreateBlueprintRequest(
            title = "Integration Test Blueprint",
            description = "A blueprint for integration testing",
            content = """
                id: integration-test-flow
                namespace: {{ namespace }}
                description: {{ description }}
                
                inputs:
                  - id: message
                    type: STRING
                    defaults: "Hello World"
                
                tasks:
                  - id: log-message
                    type: io.kestra.core.tasks.log.Log
                    message: "{{ inputs.message }}"
                  
                  - id: bash-task
                    type: io.kestra.core.tasks.scripts.Bash
                    commands:
                      - echo "Processing: {{ inputs.message }}"
                      - date
            """.trimIndent(),
            tags = listOf("integration", "test", "example"),
            kind = "workflow",
            isPublic = true,
            isTemplate = true
        )
        
        val createHttpRequest = HttpRequest.POST("/api/v1/blueprints", createRequest)
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val createResponse = client.toBlocking().exchange(createHttpRequest, Map::class.java)
        assertEquals(HttpStatus.CREATED, createResponse.status)
        
        val createdBlueprint = createResponse.body() as Map<*, *>
        val blueprintId = createdBlueprint["id"] as String
        
        // 验证创建的蓝图
        assertEquals("Integration Test Blueprint", createdBlueprint["title"])
        assertEquals(true, createdBlueprint["isPublic"])
        assertEquals(true, createdBlueprint["isTemplate"])
        assertEquals(1, createdBlueprint["version"])
        
        // 2. 获取蓝图详情
        val getHttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/$blueprintId")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val getResponse = client.toBlocking().exchange(getHttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, getResponse.status)
        
        val retrievedBlueprint = getResponse.body() as Map<*, *>
        assertEquals(blueprintId, retrievedBlueprint["id"])
        assertEquals("Integration Test Blueprint", retrievedBlueprint["title"])
        
        // 3. 测试模板渲染功能
        val templateVariables = mapOf(
            "namespace" to "test-namespace",
            "description" to "Rendered from template"
        )
        
        val renderedContent = templateService.renderTemplate(
            createdBlueprint["content"] as String,
            templateVariables
        )
        
        assertTrue(renderedContent.contains("namespace: test-namespace"))
        assertTrue(renderedContent.contains("description: Rendered from template"))
        
        // 4. 更新蓝图
        val updateRequest = UpdateBlueprintRequest(
            title = "Updated Integration Test Blueprint",
            description = "Updated description for integration testing",
            content = """
                id: updated-integration-test-flow
                namespace: {{ namespace }}
                description: {{ description }}
                
                inputs:
                  - id: message
                    type: STRING
                    defaults: "Hello Updated World"
                  - id: count
                    type: INTEGER
                    defaults: 5
                
                tasks:
                  - id: log-message
                    type: io.kestra.core.tasks.log.Log
                    message: "{{ inputs.message }} (Count: {{ inputs.count }})"
                  
                  - id: loop-task
                    type: io.kestra.core.tasks.flows.EachSequential
                    value: "{{ range(inputs.count) }}"
                    tasks:
                      - id: log-iteration
                        type: io.kestra.core.tasks.log.Log
                        message: "Iteration: {{ taskrun.value }}"
            """.trimIndent(),
            tags = listOf("integration", "test", "updated")
        )
        
        val updateHttpRequest = HttpRequest.PUT("/api/v1/blueprints/$blueprintId", updateRequest)
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val updateResponse = client.toBlocking().exchange(updateHttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, updateResponse.status)
        
        val updatedBlueprint = updateResponse.body() as Map<*, *>
        assertEquals("Updated Integration Test Blueprint", updatedBlueprint["title"])
        assertEquals(2, updatedBlueprint["version"]) // 版本应该递增
        
        // 5. 获取版本历史
        val versionsHttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/$blueprintId/versions")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val versionsResponse = client.toBlocking().exchange(versionsHttpRequest, List::class.java)
        assertEquals(HttpStatus.OK, versionsResponse.status)
        
        val versions = versionsResponse.body() as List<*>
        assertEquals(2, versions.size)
        
        // 验证版本顺序（最新版本在前）
        val latestVersion = versions[0] as Map<*, *>
        val previousVersion = versions[1] as Map<*, *>
        assertEquals(2, latestVersion["version"])
        assertEquals(1, previousVersion["version"])
        
        // 6. 获取特定版本
        val version1HttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/$blueprintId/versions/1")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val version1Response = client.toBlocking().exchange(version1HttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, version1Response.status)
        
        val version1Blueprint = version1Response.body() as Map<*, *>
        assertEquals(1, version1Blueprint["version"])
        assertEquals("Integration Test Blueprint", version1Blueprint["title"]) // 原始标题
        
        // 7. 测试搜索功能
        val searchHttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints?search=Updated")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val searchResponse = client.toBlocking().exchange(searchHttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, searchResponse.status)
        
        val searchResult = searchResponse.body() as Map<*, *>
        val foundBlueprints = searchResult["blueprints"] as List<*>
        assertEquals(1, foundBlueprints.size)
        
        val foundBlueprint = foundBlueprints[0] as Map<*, *>
        assertEquals(blueprintId, foundBlueprint["id"])
        
        // 8. 测试标签过滤
        val tagFilterHttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints?tags=updated")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val tagFilterResponse = client.toBlocking().exchange(tagFilterHttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, tagFilterResponse.status)
        
        val tagFilterResult = tagFilterResponse.body() as Map<*, *>
        val tagFilteredBlueprints = tagFilterResult["blueprints"] as List<*>
        assertEquals(1, tagFilteredBlueprints.size)
        
        // 9. 删除蓝图
        val deleteHttpRequest = HttpRequest.DELETE<Any>("/api/v1/blueprints/$blueprintId")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val deleteResponse = client.toBlocking().exchange(deleteHttpRequest, String::class.java)
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.status)
        
        // 10. 验证蓝图已被删除
        val verifyDeleteHttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/$blueprintId")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val verifyDeleteResponse = client.toBlocking().exchange(verifyDeleteHttpRequest, String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, verifyDeleteResponse.status)
    }
    
    @Test
    fun `test blueprint template functionality`() {
        // 创建模板蓝图
        val templateBlueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Database Migration Template",
            description = "Template for database migration workflows",
            content = """
                id: {{ migrationId }}
                namespace: {{ namespace }}
                description: Database migration for {{ databaseName }}
                
                inputs:
                  - id: migrationScript
                    type: FILE
                    description: SQL migration script
                  - id: rollbackScript
                    type: FILE
                    description: SQL rollback script
                  - id: dryRun
                    type: BOOLEAN
                    defaults: true
                
                tasks:
                  - id: validate-scripts
                    type: io.kestra.core.tasks.scripts.Bash
                    commands:
                      - echo "Validating migration scripts for {{ databaseName }}"
                      - ls -la {{ inputs.migrationScript }}
                      - ls -la {{ inputs.rollbackScript }}
                  
                  {% if inputs.dryRun %}
                  - id: dry-run-migration
                    type: io.kestra.plugin.jdbc.postgresql.Query
                    url: {{ databaseUrl }}
                    sql: "EXPLAIN {{ inputs.migrationScript }}"
                  {% else %}
                  - id: execute-migration
                    type: io.kestra.plugin.jdbc.postgresql.Query
                    url: {{ databaseUrl }}
                    sql: "{{ inputs.migrationScript }}"
                  {% endif %}
                  
                  - id: log-completion
                    type: io.kestra.core.tasks.log.Log
                    message: "Migration {{ migrationId }} completed for {{ databaseName }}"
            """.trimIndent(),
            tags = listOf("template", "database", "migration"),
            kind = "workflow",
            isTemplate = true,
            isPublic = true,
            createdBy = "integration-test"
        )
        
        val savedTemplate = blueprintRepository.save(templateBlueprint)
        
        // 测试模板变量提取
        val extractedVariables = templateService.extractVariables(savedTemplate.content)
        
        assertTrue(extractedVariables.contains("migrationId"))
        assertTrue(extractedVariables.contains("namespace"))
        assertTrue(extractedVariables.contains("databaseName"))
        assertTrue(extractedVariables.contains("databaseUrl"))
        
        // 测试模板渲染
        val templateVariables = mapOf(
            "migrationId" to "migration-001",
            "namespace" to "production",
            "databaseName" to "user_service_db",
            "databaseUrl" to "jdbc:postgresql://localhost:5432/user_service"
        )
        
        val renderedWorkflow = templateService.renderTemplate(savedTemplate.content, templateVariables)
        
        // 验证渲染结果
        assertTrue(renderedWorkflow.contains("id: migration-001"))
        assertTrue(renderedWorkflow.contains("namespace: production"))
        assertTrue(renderedWorkflow.contains("Database migration for user_service_db"))
        assertTrue(renderedWorkflow.contains("url: jdbc:postgresql://localhost:5432/user_service"))
        assertTrue(renderedWorkflow.contains("Validating migration scripts for user_service_db"))
        assertTrue(renderedWorkflow.contains("Migration migration-001 completed for user_service_db"))
        
        // 测试条件渲染 - dry run 模式
        val dryRunVariables = templateVariables + ("inputs" to mapOf("dryRun" to true))
        val dryRunWorkflow = templateService.renderTemplate(savedTemplate.content, dryRunVariables)
        assertTrue(dryRunWorkflow.contains("dry-run-migration"))
        assertTrue(dryRunWorkflow.contains("EXPLAIN"))
        assertFalse(dryRunWorkflow.contains("execute-migration"))
        
        // 测试条件渲染 - 执行模式
        val executeVariables = templateVariables + ("inputs" to mapOf("dryRun" to false))
        val executeWorkflow = templateService.renderTemplate(savedTemplate.content, executeVariables)
        assertFalse(executeWorkflow.contains("dry-run-migration"))
        assertTrue(executeWorkflow.contains("execute-migration"))
        assertFalse(executeWorkflow.contains("EXPLAIN"))
    }
    
    @Test
    fun `test multi-tenant blueprint isolation`() {
        // 创建另一个租户的命名空间
        val otherNamespace = Namespace(
            name = "other-tenant-namespace",
            tenantId = "other-tenant",
            description = "Other tenant namespace"
        )
        val savedOtherNamespace = namespaceRepository.save(otherNamespace)
        
        // 在第一个租户下创建蓝图
        val blueprint1 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Tenant 1 Blueprint",
            content = "tenant 1 content",
            createdBy = "tenant1-user"
        )
        blueprintRepository.save(blueprint1)
        
        // 在第二个租户下创建蓝图
        val blueprint2 = Blueprint(
            namespaceId = savedOtherNamespace.id,
            title = "Tenant 2 Blueprint",
            content = "tenant 2 content",
            createdBy = "tenant2-user"
        )
        blueprintRepository.save(blueprint2)
        
        // 测试租户隔离 - 第一个租户只能看到自己的蓝图
        val tenant1HttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val tenant1Response = client.toBlocking().exchange(tenant1HttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, tenant1Response.status)
        
        val tenant1Result = tenant1Response.body() as Map<*, *>
        val tenant1Blueprints = tenant1Result["blueprints"] as List<*>
        assertEquals(1, tenant1Blueprints.size)
        
        val tenant1Blueprint = tenant1Blueprints[0] as Map<*, *>
        assertEquals("Tenant 1 Blueprint", tenant1Blueprint["title"])
        
        // 测试租户隔离 - 第二个租户只能看到自己的蓝图
        val tenant2HttpRequest = HttpRequest.GET<Any>("/api/v1/blueprints")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", savedOtherNamespace.id)
        
        val tenant2Response = client.toBlocking().exchange(tenant2HttpRequest, Map::class.java)
        assertEquals(HttpStatus.OK, tenant2Response.status)
        
        val tenant2Result = tenant2Response.body() as Map<*, *>
        val tenant2Blueprints = tenant2Result["blueprints"] as List<*>
        assertEquals(1, tenant2Blueprints.size)
        
        val tenant2Blueprint = tenant2Blueprints[0] as Map<*, *>
        assertEquals("Tenant 2 Blueprint", tenant2Blueprint["title"])
        
        // 清理其他租户数据
        blueprintRepository.deleteByNamespaceId(savedOtherNamespace.id)
        namespaceRepository.delete(savedOtherNamespace)
    }
}