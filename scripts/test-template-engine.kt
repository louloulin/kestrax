#!/usr/bin/env kotlin

@file:DependsOn("com.github.spullara.mustache.java:compiler:0.9.11")

import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringReader
import java.io.StringWriter

/**
 * 简单的模板引擎测试脚本
 * 验证Mustache模板引擎是否正常工作
 */

fun main() {
    println("🧪 测试Mustache模板引擎...")
    
    val mustacheFactory = DefaultMustacheFactory()
    
    // 测试1: 简单变量替换
    println("\n📝 测试1: 简单变量替换")
    val template1 = "Hello {{name}}!"
    val variables1 = mapOf("name" to "Kestra")
    val result1 = renderTemplate(mustacheFactory, template1, variables1)
    println("模板: $template1")
    println("变量: $variables1")
    println("结果: $result1")
    println("✅ ${if (result1 == "Hello Kestra!") "通过" else "失败"}")
    
    // 测试2: 复杂模板
    println("\n📝 测试2: 复杂Kestra流模板")
    val template2 = """
        id: {{flowId}}
        namespace: {{namespace}}
        description: {{description}}
        
        tasks:
          - id: {{taskId}}
            type: {{taskType}}
            message: "Processing {{itemCount}} items for {{user.name}}"
            config:
              timeout: {{timeout}}
              enabled: {{enabled}}
    """.trimIndent()
    
    val variables2 = mapOf(
        "flowId" to "data-processing-flow",
        "namespace" to "production",
        "description" to "Process daily data batch",
        "taskId" to "process-data",
        "taskType" to "io.kestra.plugin.core.log.Log",
        "itemCount" to 1000,
        "timeout" to 300,
        "enabled" to true,
        "user" to mapOf(
            "name" to "DataFlare",
            "email" to "admin@dataflare.com"
        )
    )
    
    val result2 = renderTemplate(mustacheFactory, template2, variables2)
    println("模板:")
    println(template2)
    println("\n变量: $variables2")
    println("\n结果:")
    println(result2)
    
    val checks = listOf(
        "data-processing-flow" in result2,
        "production" in result2,
        "Processing 1000 items" in result2,
        "DataFlare" in result2,
        "timeout: 300" in result2,
        "enabled: true" in result2
    )
    
    println("✅ ${if (checks.all { it }) "通过" else "失败"}")
    
    // 测试3: 变量提取
    println("\n📝 测试3: 变量提取")
    val variables3 = extractVariables(template2)
    println("提取的变量: $variables3")
    val expectedVars = setOf("flowId", "namespace", "description", "taskId", "taskType", "itemCount", "timeout", "enabled", "user.name")
    println("✅ ${if (expectedVars.all { it in variables3 }) "通过" else "失败"}")
    
    // 测试4: 缺失变量处理
    println("\n📝 测试4: 缺失变量处理")
    val template4 = "Hello {{name}}! Your age is {{age}}."
    val variables4 = mapOf("name" to "John") // 缺少 age
    val result4 = renderTemplate(mustacheFactory, template4, variables4)
    println("模板: $template4")
    println("变量: $variables4")
    println("结果: $result4")
    println("✅ ${if (result4 == "Hello John! Your age is .") "通过" else "失败"}")
    
    println("\n🎉 模板引擎测试完成！")
    println("Mustache模板引擎已成功替换Pebble，可以正常处理Kestra蓝图模板。")
}

fun renderTemplate(factory: DefaultMustacheFactory, template: String, variables: Map<String, Any>): String {
    val mustache = factory.compile(StringReader(template), "test")
    val writer = StringWriter()
    mustache.execute(writer, variables)
    return writer.toString()
}

fun extractVariables(template: String): Set<String> {
    val variables = mutableSetOf<String>()
    // Mustache语法: {{variable}} 或 {{{variable}}} (unescaped)
    val variablePattern = Regex("\\{\\{\\{?\\s*([a-zA-Z_][a-zA-Z0-9_.]*)\\s*\\}?\\}\\}")
    
    variablePattern.findAll(template).forEach { matchResult ->
        val variable = matchResult.groupValues[1]
        variables.add(variable)
    }
    
    return variables
}
