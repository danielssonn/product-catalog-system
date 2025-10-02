package com.bank.product.repository;

import com.bank.product.security.TenantContext;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Base repository interface that provides tenant-aware query methods.
 * All repositories extending this interface automatically filter by current tenant.
 *
 * Usage:
 * <pre>
 * public interface SolutionRepository extends TenantAwareRepository<Solution, String> {
 *     // Custom queries automatically get tenant filtering
 *     List<Solution> findByStatus(SolutionStatus status);
 * }
 *
 * // In service:
 * Solution solution = solutionRepository.findByIdTenantAware(solutionId);
 * </pre>
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends MongoRepository<T, ID> {

    /**
     * Find by ID with automatic tenant filtering.
     * Retrieves tenant from TenantContext.
     *
     * @param id the entity ID
     * @return the entity if found and belongs to current tenant
     */
    default Optional<T> findByIdTenantAware(ID id) {
        String tenantId = TenantContext.getCurrentTenant();
        return findByIdAndTenantId(id, tenantId);
    }

    /**
     * Find by ID and tenant ID.
     * Must be implemented by extending repository using method name derivation.
     *
     * Example implementation (automatic by Spring Data):
     * <pre>
     * Optional<Solution> findByIdAndTenantId(String id, String tenantId);
     * </pre>
     */
    Optional<T> findByIdAndTenantId(ID id, String tenantId);

    /**
     * Delete by ID with automatic tenant filtering.
     * Only deletes if entity belongs to current tenant.
     *
     * @param id the entity ID
     * @return number of deleted entities (0 or 1)
     */
    default long deleteByIdTenantAware(ID id) {
        String tenantId = TenantContext.getCurrentTenant();
        return deleteByIdAndTenantId(id, tenantId);
    }

    /**
     * Delete by ID and tenant ID.
     * Must be implemented by extending repository.
     */
    long deleteByIdAndTenantId(ID id, String tenantId);

    /**
     * Check if entity exists with automatic tenant filtering.
     *
     * @param id the entity ID
     * @return true if entity exists and belongs to current tenant
     */
    default boolean existsByIdTenantAware(ID id) {
        String tenantId = TenantContext.getCurrentTenant();
        return existsByIdAndTenantId(id, tenantId);
    }

    /**
     * Check if entity exists by ID and tenant ID.
     * Must be implemented by extending repository.
     */
    boolean existsByIdAndTenantId(ID id, String tenantId);
}
