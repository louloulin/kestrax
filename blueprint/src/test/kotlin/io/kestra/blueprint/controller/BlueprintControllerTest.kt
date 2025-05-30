package io.kestra.blueprint.controller

import io.kestra.blueprint.dto.CreateBlueprintRequest
import io.kestra.blueprint.dto.UpdateBlueprintRequest
import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.Namespace
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.NamespaceRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * 蓝图控制器测试类
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlueprintControllerTest {
    
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient
    
    @Inject
    lateinit var blueprintRepository: BlueprintRepository
    
    @Inject
    lateinit var namespaceRepository: NamespaceRepository
    
    private lateinit var testNamespace: Namespace
    private lateinit var authToken: String
    
    @BeforeEach
    fun setup() {
        // 创建测试命名空间
        testNamespace = Namespace(
            name = "test-namespace",
            tenantId = "test-tenant",
            description = "Test namespace for blueprint tests"
        )
        testNamespace = namespaceRepository.save(testNamespace)
        
        // 模拟认证（在实际测试中，这里应该使用真实的认证流程）
        authToken = "Bearer test-token"
    }
    
    @AfterEach
    fun cleanup() {
        // 清理测试数据
        blueprintRepository.deleteAll()
        namespaceRepository.deleteAll()
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
        
        // 发送请求
        val httpRequest = HttpRequest.POST("/api/v1/blueprints", request)
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, Map::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.CREATED, response.status)
        assertNotNull(response.body())
        
        val responseBody = response.body() as Map<*, *>
        assertEquals("Test Blueprint", responseBody["title"])
        assertEquals("A test blueprint", responseBody["description"])
        assertEquals(testNamespace.id, responseBody["namespaceId"])
        assertEquals(listOf("test", "example"), responseBody["tags"])
        assertEquals("workflow", responseBody["kind"])
        assertEquals(false, responseBody["isPublic"])
        assertEquals(false, responseBody["isTemplate"])
    }
    
    @Test
    fun `test get blueprints`() {
        // 准备测试数据
        val blueprint1 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Blueprint 1",
            content = "test content 1",
            tags = listOf("tag1"),
            createdBy = "test-user"
        )
        val blueprint2 = Blueprint(
            namespaceId = testNamespace.id,
            title = "Blueprint 2",
            content = "test content 2",
            tags = listOf("tag2"),
            createdBy = "test-user"
        )
        
        blueprintRepository.save(blueprint1)
        blueprintRepository.save(blueprint2)
        
        // 发送请求
        val httpRequest = HttpRequest.GET<Any>("/api/v1/blueprints")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, Map::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        
        val responseBody = response.body() as Map<*, *>
        val blueprints = responseBody["blueprints"] as List<*>
        assertEquals(2, blueprints.size)
        assertEquals(2L, responseBody["total"])
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
        
        // 发送请求
        val httpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/${savedBlueprint.id}")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, Map::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        
        val responseBody = response.body() as Map<*, *>
        assertEquals(savedBlueprint.id, responseBody["id"])
        assertEquals("Test Blueprint", responseBody["title"])
        assertEquals("Test description", responseBody["description"])
        assertEquals("test content", responseBody["content"])
    }
    
    @Test
    fun `test update blueprint`() {
        // 准备测试数据
        val blueprint = Blueprint(
            namespaceId = testNamespace.id,
            title = "Original Title",
            content = "original content",
            createdBy = "test-user"
        )
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        val updateRequest = UpdateBlueprintRequest(
            title = "Updated Title",
            description = "Updated description",
            content = "updated content"
        )
        
        // 发送请求
        val httpRequest = HttpRequest.PUT("/api/v1/blueprints/${savedBlueprint.id}", updateRequest)
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, Map::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body())
        
        val responseBody = response.body() as Map<*, *>
        assertEquals("Updated Title", responseBody["title"])
        assertEquals("Updated description", responseBody["description"])
        assertEquals("updated content", responseBody["content"])
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
        
        // 发送删除请求
        val httpRequest = HttpRequest.DELETE<Any>("/api/v1/blueprints/${savedBlueprint.id}")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, String::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.NO_CONTENT, response.status)
        
        // 验证蓝图已被删除
        assertFalse(blueprintRepository.existsById(savedBlueprint.id))
    }
    
    @Test
    fun `test get blueprint not found`() {
        // 发送请求获取不存在的蓝图
        val httpRequest = HttpRequest.GET<Any>("/api/v1/blueprints/non-existent-id")
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, String::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.NOT_FOUND, response.status)
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
        
        val httpRequest = HttpRequest.POST("/api/v1/blueprints", request)
            .header("Authorization", authToken)
            .header("X-Namespace-Id", testNamespace.id)
        
        val response = client.toBlocking().exchange(httpRequest, String::class.java)
        
        // 验证响应
        assertEquals(HttpStatus.CONFLICT, response.status)
    }
}