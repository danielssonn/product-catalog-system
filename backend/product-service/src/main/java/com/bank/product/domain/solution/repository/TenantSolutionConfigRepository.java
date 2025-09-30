package com.bank.product.domain.solution.repository;

import com.bank.product.domain.solution.model.ConfigurationStatus;
import com.bank.product.domain.solution.model.TenantSolutionConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSolutionConfigRepository extends MongoRepository<TenantSolutionConfiguration, String> {

    Optional<TenantSolutionConfiguration> findByTenantIdAndConfigurationId(String tenantId, String configurationId);

    List<TenantSolutionConfiguration> findByTenantIdAndStatus(String tenantId, ConfigurationStatus status);

    Page<TenantSolutionConfiguration> findByTenantId(String tenantId, Pageable pageable);

    Page<TenantSolutionConfiguration> findByTenantIdAndStatus(String tenantId, ConfigurationStatus status, Pageable pageable);

    Page<TenantSolutionConfiguration> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId, Pageable pageable);

    List<TenantSolutionConfiguration> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId);

    boolean existsByTenantIdAndConfigurationId(String tenantId, String configurationId);
}