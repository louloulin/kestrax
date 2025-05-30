package io.kestra.blueprint.models

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.*

/**
 * 蓝图版本实体类
 * 用于管理蓝图的版本历史
 */
@Entity
@Table(name = "blueprint_versions", indexes = [
    Index(name = "idx_blueprint_version_blueprint", columnList = "blueprintId"),
    Index(name = "idx_blueprint_version_number", columnList = "versionNumber"),
    Index(name = "idx_blueprint_version_created", columnList = "createdAt")
])
data class BlueprintVersion(
    @Id
    @Column(length = 36)
    val id: String = UUID.randomUUID().toString(),
    
    @NotBlank
    @Column(nullable = false, length = 36)
    val blueprintId: String,
    
    @NotNull
    @Column(nullable = false)
    val versionNumber: Int,
    
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
        name = "blueprint_version_tags",
        joinColumns = [JoinColumn(name = "blueprint_version_id")]
    )
    @Column(name = "tag")
    val tags: List<String> = emptyList(),
    
    @ElementCollection
    @CollectionTable(
        name = "blueprint_version_included_tasks",
        joinColumns = [JoinColumn(name = "blueprint_version_id")]
    )
    @Column(name = "task")
    val includedTasks: List<String> = emptyList(),
    
    @Column(length = 50)
    val kind: String? = null,
    
    @Column(nullable = false)
    val isPublic: Boolean = false,
    
    @Column(nullable = false)
    val isTemplate: Boolean = false,
    
    @Column(columnDefinition = "TEXT")
    val changeLog: String? = null,
    
    @NotBlank
    @Column(nullable = false, length = 36)
    val createdBy: String,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
) {
    // JPA要求的无参构造函数
    constructor() : this(
        blueprintId = "",
        versionNumber = 1,
        title = "",
        content = "",
        createdBy = ""
    )
}