package io.kestra.blueprint.models

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.*

/**
 * 命名空间实体类
 * 用于实现多租户架构的租户隔离
 */
@Entity
@Table(name = "namespaces", indexes = [
    Index(name = "idx_namespace_tenant", columnList = "tenantId"),
    Index(name = "idx_namespace_parent", columnList = "parentId"),
    Index(name = "idx_namespace_name", columnList = "name")
])
data class Namespace(
    @Id
    @Column(length = 36)
    val id: String = UUID.randomUUID().toString(),
    
    @NotBlank
    @Column(nullable = false, length = 100, unique = true)
    val name: String,
    
    @Column(length = 36)
    val parentId: String? = null,
    
    @NotBlank
    @Column(nullable = false, length = 36)
    val tenantId: String,
    
    @Column(length = 500)
    val description: String? = null,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @Version
    val version: Long = 0
) {
    // JPA要求的无参构造函数
    constructor() : this(
        name = "",
        tenantId = ""
    )
}