package com.bank.product.core.repository;

import com.bank.product.core.model.TenantCoreSystemMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for tenant-to-core system mappings.
 */
@Repository
public interface TenantCoreSystemMappingRepository extends MongoRepository<TenantCoreSystemMapping, String> {

    /**
     * Find mapping by tenant ID.
     *
     * @param tenantId the tenant ID
     * @return the mapping if found
     */
    Optional<TenantCoreSystemMapping> findByTenantId(String tenantId);

    /**
     * Check if mapping exists for tenant.
     *
     * @param tenantId the tenant ID
     * @return true if mapping exists
     */
    boolean existsByTenantId(String tenantId);
}
