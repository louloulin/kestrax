package io.kestra.blueprint.config

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

/**
 * GitHub API配置
 */
@ConfigurationProperties("github.api")
class GitHubConfig {

    /**
     * GitHub API Token (可选，用于提高速率限制)
     */
    var token: String = ""

    /**
     * GitHub API基础URL
     */
    var baseUrl: String = "https://api.github.com"

    /**
     * 请求超时时间
     */
    var timeout: Duration = Duration.ofSeconds(30)

    /**
     * 重试次数
     */
    var retryAttempts: Int = 3

    /**
     * 重试延迟
     */
    var retryDelay: Duration = Duration.ofSeconds(2)

    /**
     * 是否有认证Token
     */
    fun hasToken(): Boolean = token.isNotBlank()

    /**
     * 获取认证头
     */
    fun getAuthHeader(): String? = if (hasToken()) "Bearer $token" else null
}
