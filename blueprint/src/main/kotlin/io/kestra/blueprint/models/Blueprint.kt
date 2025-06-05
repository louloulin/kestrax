package io.kestra.blueprint.models

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.*

/**
 * 蓝图实体类
 * 基于Kotlin + Micronaut + JPA实现的多租户蓝图数据模型
 */
@Entity
@Table(name = "blueprints", indexes = [
    Index(name = "idx_blueprint_namespace", columnList = "namespaceId"),
    Index(name = "idx_blueprint_created_by", columnList = "createdBy"),
    Index(name = "idx_blueprint_kind", columnList = "kind")
])
data class Blueprint(
    @Id
    @Column(length = 36)
    val id: String = UUID.randomUUID().toString(),
    
    @NotBlank
    @Column(nullable = false, length = 36)
    val namespaceId: String, // 租户隔离字段
    
    @NotBlank
    @Column(nullable = false, length = 255)
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @NotNull
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String, // YAML 内容
    
    @ElementCollection
    @CollectionTable(
        name = "blueprint_tags",
        joinColumns = [JoinColumn(name = "blueprint_id")]
    )
    @Column(name = "tag")
    val tags: List<String> = emptyList(),
    
    @ElementCollection
    @CollectionTable(
        name = "blueprint_included_tasks",
        joinColumns = [JoinColumn(name = "blueprint_id")]
    )
    @Column(name = "task")
    val includedTasks: List<String> = emptyList(),
    
    @Column(length = 50)
    val kind: String? = null,
    
    @Column(nullable = false)
    val isPublic: Boolean = false,
    
    @Column(nullable = false)
    val isTemplate: Boolean = false,
    
    @NotBlank
    @Column(nullable = false, length = 36)
    val createdBy: String,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @Version
    val version: Long = 0
) {
    // JPA要求的无参构造函数
    constructor() : this(
        namespaceId = "",
        title = "",
        content = "",
        createdBy = ""
    )
}