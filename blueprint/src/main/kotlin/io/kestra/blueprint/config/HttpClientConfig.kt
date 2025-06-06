package io.kestra.blueprint.config

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Named
import jakarta.inject.Singleton

/**
 * HTTP客户端配置，支持代理
 * 使用系统属性配置代理
 */
@Factory
class HttpClientConfig {

    init {
        // 设置系统代理属性
        System.setProperty("http.proxyHost", "127.0.0.1")
        System.setProperty("http.proxyPort", "4780")
        System.setProperty("https.proxyHost", "127.0.0.1")
        System.setProperty("https.proxyPort", "4780")
    }

    /**
     * 创建支持代理的GitHub API客户端
     */
    @Bean
    @Named("github-api")
    @Singleton
    fun githubApiClient(@Client("https://api.github.com") client: HttpClient): HttpClient {
        return client
    }

    /**
     * 创建支持代理的GitHub Raw客户端
     */
    @Bean
    @Named("github-raw")
    @Singleton
    fun githubRawClient(@Client("https://raw.githubusercontent.com") client: HttpClient): HttpClient {
        return client
    }
}
