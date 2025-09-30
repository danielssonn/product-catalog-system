package com.bank.productcatalog.catalog.domain.product.repository;

import com.bank.productcatalog.common.domain.product.model.ConfigurationStatus;
import com.bank.productcatalog.common.domain.product.model.TenantProductConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantProductConfigRepository extends MongoRepository<TenantProductConfiguration, String> {

    Optional<TenantProductConfiguration> findByTenantIdAndConfigurationId(String tenantId, String configurationId);

    List<TenantProductConfiguration> findByTenantIdAndStatus(String tenantId, ConfigurationStatus status);

    Page<TenantProductConfiguration> findByTenantId(String tenantId, Pageable pageable);

    Page<TenantProductConfiguration> findByTenantIdAndStatus(String tenantId, ConfigurationStatus status, Pageable pageable);

    Page<TenantProductConfiguration> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId, Pageable pageable);

    List<TenantProductConfiguration> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId);

    boolean existsByTenantIdAndConfigurationId(String tenantId, String configurationId);
}