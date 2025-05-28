package io.kestra.core.tenant;

import io.kestra.core.models.TenantInterface;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract base class for tenant-aware repositories
 * Provides automatic tenant isolation for data access operations
 */
@Slf4j
public abstract class TenantAwareRepository<T extends TenantInterface> {
    
    /**
     * Find entity by ID within current tenant context
     */
    public Optional<T> findById(String id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return findByIdAndTenant(id, tenantId);
    }
    
    /**
     * Find entity by ID and specific tenant
     */
    public Optional<T> findByIdAndTenant(String id, String tenantId) {
        validateTenantAccess(tenantId);
        return doFindByIdAndTenant(id, tenantId);
    }
    
    /**
     * Find all entities within current tenant context
     */
    public List<T> findAll() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return findAllByTenant(tenantId);
    }
    
    /**
     * Find all entities for specific tenant
     */
    public List<T> findAllByTenant(String tenantId) {
        validateTenantAccess(tenantId);
        return doFindAllByTenant(tenantId);
    }
    
    /**
     * Save entity within current tenant context
     */
    public T save(T entity) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return saveWithTenant(entity, tenantId);
    }
    
    /**
     * Save entity with specific tenant
     */
    public T saveWithTenant(T entity, String tenantId) {
        validateTenantAccess(tenantId);
        
        // Ensure entity has correct tenant ID
        if (!tenantId.equals(entity.getTenantId())) {
            throw new TenantContext.TenantAccessException(
                String.format("Entity tenant ID '%s' does not match context tenant ID '%s'", 
                            entity.getTenantId(), tenantId)
            );
        }
        
        return doSaveWithTenant(entity, tenantId);
    }
    
    /**
     * Update entity within current tenant context
     */
    public T update(T entity) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return updateWithTenant(entity, tenantId);
    }
    
    /**
     * Update entity with specific tenant
     */
    public T updateWithTenant(T entity, String tenantId) {
        validateTenantAccess(tenantId);
        
        // Ensure entity has correct tenant ID
        if (!tenantId.equals(entity.getTenantId())) {
            throw new TenantContext.TenantAccessException(
                String.format("Entity tenant ID '%s' does not match context tenant ID '%s'", 
                            entity.getTenantId(), tenantId)
            );
        }
        
        // Verify entity exists in the tenant
        if (!existsByIdAndTenant(getEntityId(entity), tenantId)) {
            throw new TenantContext.TenantAccessException(
                String.format("Entity with ID '%s' not found in tenant '%s'", 
                            getEntityId(entity), tenantId)
            );
        }
        
        return doUpdateWithTenant(entity, tenantId);
    }
    
    /**
     * Delete entity by ID within current tenant context
     */
    public boolean deleteById(String id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return deleteByIdAndTenant(id, tenantId);
    }
    
    /**
     * Delete entity by ID and specific tenant
     */
    public boolean deleteByIdAndTenant(String id, String tenantId) {
        validateTenantAccess(tenantId);
        return doDeleteByIdAndTenant(id, tenantId);
    }
    
    /**
     * Check if entity exists by ID within current tenant context
     */
    public boolean existsById(String id) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return existsByIdAndTenant(id, tenantId);
    }
    
    /**
     * Check if entity exists by ID and specific tenant
     */
    public boolean existsByIdAndTenant(String id, String tenantId) {
        validateTenantAccess(tenantId);
        return doExistsByIdAndTenant(id, tenantId);
    }
    
    /**
     * Count entities within current tenant context
     */
    public long count() {
        String tenantId = TenantContext.requireCurrentTenantId();
        return countByTenant(tenantId);
    }
    
    /**
     * Count entities for specific tenant
     */
    public long countByTenant(String tenantId) {
        validateTenantAccess(tenantId);
        return doCountByTenant(tenantId);
    }
    
    /**
     * Find entities by field value within current tenant context
     */
    public List<T> findByField(String fieldName, Object value) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return findByFieldAndTenant(fieldName, value, tenantId);
    }
    
    /**
     * Find entities by field value and specific tenant
     */
    public List<T> findByFieldAndTenant(String fieldName, Object value, String tenantId) {
        validateTenantAccess(tenantId);
        return doFindByFieldAndTenant(fieldName, value, tenantId);
    }
    
    /**
     * Filter entities to only include those belonging to current tenant
     */
    protected List<T> filterByCurrentTenant(List<T> entities) {
        String tenantId = TenantContext.requireCurrentTenantId();
        return filterByTenant(entities, tenantId);
    }
    
    /**
     * Filter entities to only include those belonging to specific tenant
     */
    protected List<T> filterByTenant(List<T> entities, String tenantId) {
        return entities.stream()
                      .filter(entity -> tenantId.equals(entity.getTenantId()))
                      .collect(Collectors.toList());
    }
    
    /**
     * Validate tenant access
     */
    protected void validateTenantAccess(String tenantId) {
        if (!TenantContext.hasTenantContext()) {
            throw new TenantContext.TenantAccessException("No tenant context set");
        }
        
        String currentTenantId = TenantContext.requireCurrentTenantId();
        if (!currentTenantId.equals(tenantId)) {
            throw new TenantContext.TenantAccessException(
                String.format("Access denied: Current tenant '%s' cannot access tenant '%s'", 
                            currentTenantId, tenantId)
            );
        }
    }
    
    // Abstract methods to be implemented by concrete repositories
    
    /**
     * Get entity ID for tenant validation
     */
    protected abstract String getEntityId(T entity);
    
    /**
     * Implementation-specific find by ID and tenant
     */
    protected abstract Optional<T> doFindByIdAndTenant(String id, String tenantId);
    
    /**
     * Implementation-specific find all by tenant
     */
    protected abstract List<T> doFindAllByTenant(String tenantId);
    
    /**
     * Implementation-specific save with tenant
     */
    protected abstract T doSaveWithTenant(T entity, String tenantId);
    
    /**
     * Implementation-specific update with tenant
     */
    protected abstract T doUpdateWithTenant(T entity, String tenantId);
    
    /**
     * Implementation-specific delete by ID and tenant
     */
    protected abstract boolean doDeleteByIdAndTenant(String id, String tenantId);
    
    /**
     * Implementation-specific exists by ID and tenant
     */
    protected abstract boolean doExistsByIdAndTenant(String id, String tenantId);
    
    /**
     * Implementation-specific count by tenant
     */
    protected abstract long doCountByTenant(String tenantId);
    
    /**
     * Implementation-specific find by field and tenant
     */
    protected abstract List<T> doFindByFieldAndTenant(String fieldName, Object value, String tenantId);
}
