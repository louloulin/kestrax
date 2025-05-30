package io.kestra.blueprint.service

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

/**
 * 蓝图模板服务测试类
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlueprintTemplateServiceTest {
    
    @Inject
    lateinit var templateService: BlueprintTemplateService
    
    @Test
    fun `test render simple template`() {
        // 准备测试数据
        val template = """
            id: {{ flowId }}
            namespace: {{ namespace }}
            tasks:
              - id: hello
                type: io.kestra.core.tasks.log.Log
                message: Hello {{ name }}!
        """.trimIndent()
        
        val variables = mapOf(
            "flowId" to "test-flow",
            "namespace" to "test-namespace",
            "name" to "World"
        )
        
        // 执行测试
        val result = templateService.renderTemplate(template, variables)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("id: test-flow"))
        assertTrue(result.contains("namespace: test-namespace"))
        assertTrue(result.contains("message: Hello World!"))
    }
    
    @Test
    fun `test render template with loops`() {
        // 准备测试数据
        val template = """
            id: multi-task-flow
            namespace: test
            tasks:
            {% for task in tasks %}
              - id: {{ task.id }}
                type: {{ task.type }}
                message: {{ task.message }}
            {% endfor %}
        """.trimIndent()
        
        val variables = mapOf(
            "tasks" to listOf(
                mapOf(
                    "id" to "task1",
                    "type" to "io.kestra.core.tasks.log.Log",
                    "message" to "First task"
                ),
                mapOf(
                    "id" to "task2",
                    "type" to "io.kestra.core.tasks.log.Log",
                    "message" to "Second task"
                )
            )
        )
        
        // 执行测试
        val result = templateService.renderTemplate(template, variables)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("id: task1"))
        assertTrue(result.contains("id: task2"))
        assertTrue(result.contains("message: First task"))
        assertTrue(result.contains("message: Second task"))
    }
    
    @Test
    fun `test render template with conditionals`() {
        // 准备测试数据
        val template = """
            id: conditional-flow
            namespace: test
            tasks:
              - id: hello
                type: io.kestra.core.tasks.log.Log
                message: Hello World!
            {% if includeEmail %}
              - id: email
                type: io.kestra.plugin.notifications.mail.MailSend
                to: {{ emailTo }}
                subject: Notification
            {% endif %}
        """.trimIndent()
        
        // 测试包含邮件任务的情况
        val variablesWithEmail = mapOf(
            "includeEmail" to true,
            "emailTo" to "test@example.com"
        )
        
        val resultWithEmail = templateService.renderTemplate(template, variablesWithEmail)
        assertTrue(resultWithEmail.contains("id: email"))
        assertTrue(resultWithEmail.contains("to: test@example.com"))
        
        // 测试不包含邮件任务的情况
        val variablesWithoutEmail = mapOf(
            "includeEmail" to false
        )
        
        val resultWithoutEmail = templateService.renderTemplate(template, variablesWithoutEmail)
        assertFalse(resultWithoutEmail.contains("id: email"))
    }
    
    @Test
    fun `test validate template syntax - valid template`() {
        // 准备有效的模板
        val validTemplate = """
            id: {{ flowId }}
            namespace: {{ namespace }}
            tasks:
              - id: hello
                type: io.kestra.core.tasks.log.Log
                message: Hello {{ name }}!
        """.trimIndent()
        
        // 执行测试
        val result = templateService.validateTemplate(validTemplate)
        
        // 验证结果
        assertTrue(result.isValid)
        assertNull(result.error)
    }
    
    @Test
    fun `test validate template syntax - invalid template`() {
        // 准备无效的模板（缺少结束标签）
        val invalidTemplate = """
            id: {{ flowId }}
            namespace: {{ namespace
            tasks:
              - id: hello
                type: io.kestra.core.tasks.log.Log
                message: Hello {{ name }}!
        """.trimIndent()
        
        // 执行测试
        val result = templateService.validateTemplate(invalidTemplate)
        
        // 验证结果
        assertFalse(result.isValid)
        assertNotNull(result.error)
    }
    
    @Test
    fun `test extract template variables`() {
        // 准备测试模板
        val template = """
            id: {{ flowId }}
            namespace: {{ namespace }}
            description: {{ description | default('Default description') }}
            tasks:
            {% for task in tasks %}
              - id: {{ task.id }}
                type: {{ task.type }}
                message: {{ task.message }}
            {% endfor %}
            {% if includeEmail %}
              - id: email
                type: io.kestra.plugin.notifications.mail.MailSend
                to: {{ emailTo }}
            {% endif %}
        """.trimIndent()
        
        // 执行测试
        val variables = templateService.extractVariables(template)
        
        // 验证结果
        assertNotNull(variables)
        assertTrue(variables.contains("flowId"))
        assertTrue(variables.contains("namespace"))
        assertTrue(variables.contains("description"))
        assertTrue(variables.contains("tasks"))
        assertTrue(variables.contains("includeEmail"))
        assertTrue(variables.contains("emailTo"))
    }
    
    @Test
    fun `test generate sample variables`() {
        // 准备测试模板
        val template = """
            id: {{ flowId }}
            namespace: {{ namespace }}
            description: {{ description }}
            tasks:
            {% for task in tasks %}
              - id: {{ task.id }}
                type: {{ task.type }}
            {% endfor %}
        """.trimIndent()
        
        // 执行测试
        val sampleVariables = templateService.generateSampleVariables(template)
        
        // 验证结果
        assertNotNull(sampleVariables)
        assertTrue(sampleVariables.containsKey("flowId"))
        assertTrue(sampleVariables.containsKey("namespace"))
        assertTrue(sampleVariables.containsKey("description"))
        assertTrue(sampleVariables.containsKey("tasks"))
        
        // 验证示例值的类型
        assertTrue(sampleVariables["flowId"] is String)
        assertTrue(sampleVariables["namespace"] is String)
        assertTrue(sampleVariables["description"] is String)
        assertTrue(sampleVariables["tasks"] is List<*>)
    }
    
    @Test
    fun `test render template with missing variables`() {
        // 准备测试数据
        val template = """
            id: {{ flowId }}
            namespace: {{ namespace }}
            message: {{ message }}
        """.trimIndent()
        
        val incompleteVariables = mapOf(
            "flowId" to "test-flow"
            // 缺少 namespace 和 message
        )
        
        // 执行测试并验证异常
        assertThrows<RuntimeException> {
            templateService.renderTemplate(template, incompleteVariables)
        }
    }
    
    @Test
    fun `test render template with filters`() {
        // 准备测试数据
        val template = """
            id: {{ flowId | lower }}
            namespace: {{ namespace | upper }}
            description: {{ description | default('No description provided') }}
            timestamp: {{ timestamp | date('yyyy-MM-dd HH:mm:ss') }}
        """.trimIndent()
        
        val variables = mapOf(
            "flowId" to "TEST-FLOW",
            "namespace" to "test-namespace",
            "timestamp" to System.currentTimeMillis()
        )
        
        // 执行测试
        val result = templateService.renderTemplate(template, variables)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("id: test-flow")) // 转换为小写
        assertTrue(result.contains("namespace: TEST-NAMESPACE")) // 转换为大写
        assertTrue(result.contains("description: No description provided")) // 默认值
    }
    
    @Test
    fun `test render complex nested template`() {
        // 准备复杂的嵌套模板
        val template = """
            id: {{ workflow.id }}
            namespace: {{ workflow.namespace }}
            description: {{ workflow.description }}
            
            inputs:
            {% for input in workflow.inputs %}
              - id: {{ input.id }}
                type: {{ input.type }}
                {% if input.required %}required: true{% endif %}
                {% if input.default %}default: {{ input.default }}{% endif %}
            {% endfor %}
            
            tasks:
            {% for task in workflow.tasks %}
              - id: {{ task.id }}
                type: {{ task.type }}
                {% if task.properties %}
                {% for key, value in task.properties %}
                {{ key }}: {{ value }}
                {% endfor %}
                {% endif %}
            {% endfor %}
        """.trimIndent()
        
        val variables = mapOf(
            "workflow" to mapOf(
                "id" to "complex-workflow",
                "namespace" to "test",
                "description" to "A complex workflow example",
                "inputs" to listOf(
                    mapOf(
                        "id" to "input1",
                        "type" to "STRING",
                        "required" to true,
                        "default" to "default-value"
                    ),
                    mapOf(
                        "id" to "input2",
                        "type" to "INTEGER",
                        "required" to false
                    )
                ),
                "tasks" to listOf(
                    mapOf(
                        "id" to "task1",
                        "type" to "io.kestra.core.tasks.log.Log",
                        "properties" to mapOf(
                            "message" to "Hello from task1",
                            "level" to "INFO"
                        )
                    ),
                    mapOf(
                        "id" to "task2",
                        "type" to "io.kestra.core.tasks.scripts.Bash",
                        "properties" to mapOf(
                            "commands" to listOf("echo 'Hello World'")
                        )
                    )
                )
            )
        )
        
        // 执行测试
        val result = templateService.renderTemplate(template, variables)
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("id: complex-workflow"))
        assertTrue(result.contains("namespace: test"))
        assertTrue(result.contains("id: input1"))
        assertTrue(result.contains("type: STRING"))
        assertTrue(result.contains("required: true"))
        assertTrue(result.contains("default: default-value"))
        assertTrue(result.contains("id: task1"))
        assertTrue(result.contains("message: Hello from task1"))
        assertTrue(result.contains("level: INFO"))
    }
}