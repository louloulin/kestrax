package io.kestra.blueprint.service

import io.kestra.blueprint.models.Blueprint
import io.kestra.blueprint.models.BlueprintVersion
import io.kestra.blueprint.repository.BlueprintRepository
import io.kestra.blueprint.repository.BlueprintVersionRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * 官网蓝图同步服务
 * 负责从Kestra官网同步蓝图模板
 */
@Singleton
class OfficialBlueprintSyncService(
    private val blueprintRepository: BlueprintRepository,
    private val blueprintVersionRepository: BlueprintVersionRepository,
    @Client("https://kestra.io") private val httpClient: HttpClient
) {
    
    private val logger = LoggerFactory.getLogger(OfficialBlueprintSyncService::class.java)
    
    /**
     * 同步所有官网蓝图
     */
    suspend fun syncAllBlueprints(): SyncResult {
        logger.info("开始同步官网蓝图")
        
        val syncResult = SyncResult()
        
        try {
            // 获取官网蓝图列表
            val officialBlueprints = getOfficialBlueprints()
            
            officialBlueprints.forEach { officialBlueprint ->
                try {
                    syncBlueprint(officialBlueprint)
                    syncResult.successCount++
                } catch (e: Exception) {
                    logger.error("同步蓝图失败: ${officialBlueprint.id}", e)
                    syncResult.failedBlueprints.add(officialBlueprint.id to e.message)
                    syncResult.failureCount++
                }
            }
            
            logger.info("蓝图同步完成: 成功 ${syncResult.successCount}, 失败 ${syncResult.failureCount}")
            
        } catch (e: Exception) {
            logger.error("同步官网蓝图失败", e)
            throw BlueprintSyncException("同步官网蓝图失败: ${e.message}", e)
        }
        
        return syncResult
    }
    
    /**
     * 同步单个蓝图
     */
    private suspend fun syncBlueprint(officialBlueprint: OfficialBlueprint) {
        logger.debug("同步蓝图: ${officialBlueprint.id}")
        
        // 检查蓝图是否已存在
        val existingBlueprint = blueprintRepository.findByTitleAndNamespaceId(officialBlueprint.title, "official")
        
        val blueprint = if (existingBlueprint.isPresent) {
            // 更新现有蓝图 - 由于Blueprint不是data class，需要创建新实例
            val existing = existingBlueprint.get()
            Blueprint(
                id = existing.id,
                namespaceId = existing.namespaceId,
                title = existing.title,
                description = officialBlueprint.description,
                content = officialBlueprint.content,
                tags = officialBlueprint.tags,
                includedTasks = existing.includedTasks,
                kind = existing.kind,
                isPublic = existing.isPublic,
                isTemplate = existing.isTemplate,
                createdBy = existing.createdBy,
                createdAt = existing.createdAt,
                updatedAt = Instant.now(),
                version = existing.version
            )
        } else {
            // 创建新蓝图
            Blueprint(
                namespaceId = "official",
                title = officialBlueprint.title,
                description = officialBlueprint.description,
                content = officialBlueprint.content,
                tags = officialBlueprint.tags,
                createdBy = "system"
            )
        }
        
        val savedBlueprint = blueprintRepository.save(blueprint)
        
        // 创建新版本
        val blueprintVersion = BlueprintVersion(
            blueprintId = savedBlueprint.id,
            versionNumber = generateNextVersionNumber(savedBlueprint.id),
            title = officialBlueprint.title,
            content = officialBlueprint.content,
            changeLog = "从官网同步",
            createdBy = "system"
        )
        
        blueprintVersionRepository.save(blueprintVersion)
        
        logger.debug("蓝图同步完成: ${officialBlueprint.id}")
    }
    
    /**
     * 获取官网蓝图列表
     */
    private suspend fun getOfficialBlueprints(): List<OfficialBlueprint> {
        // 这里返回一些预定义的官网蓝图
        // 在实际实现中，可以通过API或爬虫获取
        return listOf(
            createBusinessProcessBlueprint(),
            createDataPipelineBlueprint(),
            createApiOrchestrationBlueprint(),
            createNotificationBlueprint(),
            createScheduledTaskBlueprint()
        )
    }
    
    /**
     * 创建业务流程蓝图
     */
    private fun createBusinessProcessBlueprint(): OfficialBlueprint {
        val content = """
id: business-processes
namespace: tutorial
description: Business Processes
inputs:
  - id: request.name
    type: STRING
    defaults: Rick Astley
  - id: request.start_date
    type: DATE
    defaults: 2024-07-01
  - id: request.end_date
    type: DATE
    defaults: 2024-07-07
  - id: slack_webhook_uri
    type: URI
    defaults: https://kestra.io/api/mock
tasks:
  - id: send_approval_request
    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
    url: "{{ inputs.slack_webhook_uri }}"
    payload: |
      {
        "channel": "#vacation",
        "text": "Validate holiday request for {{ inputs.request.name }}. To approve the request, click on the `Resume` button here http://localhost:8080/ui/executions/{{flow.namespace}}/{{flow.id}}/{{execution.id}}"
      }
  - id: wait_for_approval
    type: io.kestra.plugin.core.flow.Pause
  - id: process_request
    type: io.kestra.plugin.core.http.Request
    uri: https://kestra.io/api/mock
    method: POST
    contentType: application/json
    body: "{{ inputs.request }}"
""".trimIndent()
        
        return OfficialBlueprint(
            id = "business-processes",
            title = "Business Processes",
            description = "业务流程自动化示例，包含审批流程和通知",
            content = content,
            tags = listOf("Getting Started", "Notifications", "API", "Kestra"),
            category = "Business"
        )
    }
    
    /**
     * 创建数据管道蓝图
     */
    private fun createDataPipelineBlueprint(): OfficialBlueprint {
        val content = """
id: data-pipeline
namespace: tutorial
description: ETL Data Pipeline
tasks:
  - id: extract_data
    type: io.kestra.plugin.core.http.Request
    uri: https://api.example.com/data
    method: GET
  - id: transform_data
    type: io.kestra.plugin.scripts.python.Script
    script: |
      import json
      import pandas as pd
      
      # 读取提取的数据
      data = json.loads('{{ outputs.extract_data.body }}')
      df = pd.DataFrame(data)
      
      # 数据转换
      df['processed_at'] = pd.Timestamp.now()
      df['status'] = 'processed'
      
      # 保存转换后的数据
      df.to_csv('transformed_data.csv', index=False)
  - id: load_data
    type: io.kestra.plugin.jdbc.postgresql.Query
    url: jdbc:postgresql://localhost:5432/kestra
    username: kestra
    password: kestra
    sql: |
      COPY processed_data FROM '{{ outputs.transform_data.outputFiles['transformed_data.csv'] }}'
      WITH (FORMAT CSV, HEADER true)
""".trimIndent()
        
        return OfficialBlueprint(
            id = "data-pipeline",
            title = "ETL Data Pipeline",
            description = "完整的ETL数据管道示例，包含提取、转换和加载",
            content = content,
            tags = listOf("ETL", "Python", "SQL", "Data"),
            category = "Data"
        )
    }
    
    /**
     * 创建API编排蓝图
     */
    private fun createApiOrchestrationBlueprint(): OfficialBlueprint {
        val content = """
id: api-orchestration
namespace: tutorial
description: API Orchestration
inputs:
  - id: user_id
    type: STRING
    required: true
tasks:
  - id: get_user_info
    type: io.kestra.plugin.core.http.Request
    uri: https://api.example.com/users/{{ inputs.user_id }}
    method: GET
  - id: get_user_orders
    type: io.kestra.plugin.core.http.Request
    uri: https://api.example.com/users/{{ inputs.user_id }}/orders
    method: GET
  - id: process_user_data
    type: io.kestra.plugin.core.flow.Parallel
    tasks:
      - id: update_profile
        type: io.kestra.plugin.core.http.Request
        uri: https://api.example.com/users/{{ inputs.user_id }}/profile
        method: PUT
        body: |
          {
            "last_login": "{{ now() }}",
            "order_count": {{ outputs.get_user_orders.body | length }}
          }
      - id: send_notification
        type: io.kestra.plugin.notifications.mail.MailSend
        to: "{{ outputs.get_user_info.body.email }}"
        subject: "Account Update"
        htmlTextContent: "Your account has been updated successfully."
""".trimIndent()
        
        return OfficialBlueprint(
            id = "api-orchestration",
            title = "API Orchestration",
            description = "API编排示例，展示如何协调多个API调用",
            content = content,
            tags = listOf("API", "Parallel", "HTTP", "Notifications"),
            category = "Integration"
        )
    }
    
    /**
     * 创建通知蓝图
     */
    private fun createNotificationBlueprint(): OfficialBlueprint {
        val content = """
id: notification-system
namespace: tutorial
description: Multi-channel Notification System
inputs:
  - id: message
    type: STRING
    required: true
  - id: priority
    type: STRING
    defaults: normal
    enum: [low, normal, high, critical]
tasks:
  - id: determine_channels
    type: io.kestra.plugin.core.flow.Switch
    value: "{{ inputs.priority }}"
    cases:
      critical:
        - id: send_sms
          type: io.kestra.plugin.notifications.sms.SmsSend
          message: "CRITICAL: {{ inputs.message }}"
        - id: send_email
          type: io.kestra.plugin.notifications.mail.MailSend
          subject: "Critical Alert"
          htmlTextContent: "{{ inputs.message }}"
        - id: send_slack
          type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
          payload: |
            {
              "channel": "#alerts",
              "text": "🚨 CRITICAL: {{ inputs.message }}"
            }
      high:
        - id: send_email
          type: io.kestra.plugin.notifications.mail.MailSend
          subject: "High Priority Alert"
          htmlTextContent: "{{ inputs.message }}"
        - id: send_slack
          type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
          payload: |
            {
              "channel": "#alerts",
              "text": "⚠️ HIGH: {{ inputs.message }}"
            }
      normal:
        - id: send_email
          type: io.kestra.plugin.notifications.mail.MailSend
          subject: "Notification"
          htmlTextContent: "{{ inputs.message }}"
      low:
        - id: log_message
          type: io.kestra.plugin.core.log.Log
          message: "Low priority: {{ inputs.message }}"
""".trimIndent()
        
        return OfficialBlueprint(
            id = "notification-system",
            title = "Multi-channel Notification System",
            description = "多渠道通知系统，根据优先级选择不同的通知方式",
            content = content,
            tags = listOf("Notifications", "Switch", "Email", "Slack", "SMS"),
            category = "Notifications"
        )
    }
    
    /**
     * 创建定时任务蓝图
     */
    private fun createScheduledTaskBlueprint(): OfficialBlueprint {
        val content = """
id: scheduled-maintenance
namespace: tutorial
description: Scheduled Maintenance Tasks
triggers:
  - id: daily_maintenance
    type: io.kestra.plugin.core.trigger.Schedule
    cron: "0 2 * * *"  # 每天凌晨2点执行
tasks:
  - id: system_health_check
    type: io.kestra.plugin.scripts.shell.Commands
    commands:
      - df -h
      - free -m
      - ps aux | head -20
  - id: cleanup_logs
    type: io.kestra.plugin.scripts.shell.Commands
    commands:
      - find /var/log -name "*.log" -mtime +7 -delete
      - find /tmp -name "temp_*" -mtime +1 -delete
  - id: backup_database
    type: io.kestra.plugin.jdbc.postgresql.Query
    url: jdbc:postgresql://localhost:5432/kestra
    username: kestra
    password: kestra
    sql: |
      SELECT pg_dump('kestra') AS backup_data
  - id: send_maintenance_report
    type: io.kestra.plugin.notifications.mail.MailSend
    to: admin@example.com
    subject: "Daily Maintenance Report - {{ now() | date('yyyy-MM-dd') }}"
    htmlTextContent: |
      <h2>Daily Maintenance Report</h2>
      <h3>System Health Check</h3>
      <pre>{{ outputs.system_health_check.stdOut }}</pre>
      <h3>Cleanup Results</h3>
      <pre>{{ outputs.cleanup_logs.stdOut }}</pre>
      <p>Database backup completed successfully.</p>
""".trimIndent()
        
        return OfficialBlueprint(
            id = "scheduled-maintenance",
            title = "Scheduled Maintenance Tasks",
            description = "定时维护任务，包含系统检查、日志清理和数据库备份",
            content = content,
            tags = listOf("Schedule", "Maintenance", "System", "Backup"),
            category = "System"
        )
    }
    
    /**
     * 生成下一个版本号
     */
    private suspend fun generateNextVersionNumber(blueprintId: String): Int {
        val maxVersionNumber = blueprintVersionRepository.findMaxVersionNumberByBlueprintId(blueprintId)
        return if (maxVersionNumber.isPresent) {
            maxVersionNumber.get() + 1
        } else {
            1
        }
    }
}

/**
 * 官网蓝图数据类
 */
data class OfficialBlueprint(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val tags: List<String>,
    val category: String
)

/**
 * 同步结果
 */
data class SyncResult(
    var successCount: Int = 0,
    var failureCount: Int = 0,
    val failedBlueprints: MutableList<Pair<String, String?>> = mutableListOf()
)

/**
 * 蓝图同步异常
 */
class BlueprintSyncException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)