package io.kestra.blueprint.service

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.template.PebbleTemplate
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.StringWriter
import java.util.*

/**
 * 蓝图模板引擎服务
 * 使用Pebble模板引擎处理蓝图模板
 */
@Singleton
class BlueprintTemplateService(
    private val pebbleEngine: PebbleEngine
) {
    
    private val logger = LoggerFactory.getLogger(BlueprintTemplateService::class.java)
    
    /**
     * 渲染蓝图模板
     * 
     * @param templateContent 模板内容
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    fun renderTemplate(templateContent: String, variables: Map<String, Any>): String {
        return try {
            val template = pebbleEngine.getLiteralTemplate(templateContent)
            val writer = StringWriter()
            template.evaluate(writer, variables)
            writer.toString()
        } catch (e: Exception) {
            logger.error("模板渲染失败: {}", e.message, e)
            throw BlueprintTemplateException("模板渲染失败: ${e.message}", e)
        }
    }
    
    /**
     * 验证模板语法
     * 
     * @param templateContent 模板内容
     * @return 验证结果
     */
    fun validateTemplate(templateContent: String): TemplateValidationResult {
        return try {
            pebbleEngine.getLiteralTemplate(templateContent)
            TemplateValidationResult(true, null)
        } catch (e: Exception) {
            logger.warn("模板语法验证失败: {}", e.message)
            TemplateValidationResult(false, e.message)
        }
    }
    
    /**
     * 提取模板中的变量
     * 
     * @param templateContent 模板内容
     * @return 变量列表
     */
    fun extractVariables(templateContent: String): Set<String> {
        val variables = mutableSetOf<String>()
        val variablePattern = Regex("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_.]*)\\s*\\}\\}")
        
        variablePattern.findAll(templateContent).forEach { matchResult ->
            val variable = matchResult.groupValues[1]
            variables.add(variable)
        }
        
        return variables
    }
    
    /**
     * 生成模板示例
     * 
     * @param variables 变量列表
     * @return 示例变量值
     */
    fun generateExampleVariables(variables: Set<String>): Map<String, Any> {
        val examples = mutableMapOf<String, Any>()
        
        variables.forEach { variable ->
            examples[variable] = when {
                variable.contains("name", ignoreCase = true) -> "example-name"
                variable.contains("id", ignoreCase = true) -> UUID.randomUUID().toString()
                variable.contains("url", ignoreCase = true) -> "https://example.com"
                variable.contains("port", ignoreCase = true) -> 8080
                variable.contains("count", ignoreCase = true) -> 10
                variable.contains("enable", ignoreCase = true) -> true
                variable.contains("timeout", ignoreCase = true) -> 30
                else -> "example-value"
            }
        }
        
        return examples
    }
}

/**
 * 模板验证结果
 */
data class TemplateValidationResult(
    val isValid: Boolean,
    val errorMessage: String?
)

/**
 * 蓝图模板异常
 */
class BlueprintTemplateException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Pebble引擎配置工厂
 */
@Factory
class PebbleEngineFactory {
    
    @Bean
    @Singleton
    fun pebbleEngine(): PebbleEngine {
        return PebbleEngine.Builder()
            .strictVariables(false) // 允许未定义的变量
            .cacheActive(true) // 启用缓存
            .build()
    }
}