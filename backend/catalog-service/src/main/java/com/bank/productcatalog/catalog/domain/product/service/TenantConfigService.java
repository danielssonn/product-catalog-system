package com.bank.productcatalog.catalog.domain.product.service;

import com.bank.productcatalog.common.domain.product.model.ConfigurationStatus;
import com.bank.productcatalog.common.domain.product.model.TenantProductConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TenantConfigService {

    TenantProductConfiguration createConfiguration(String tenantId, String catalogProductId,
                                                   TenantProductConfiguration config, String userId);

    TenantProductConfiguration updateConfiguration(String tenantId, String configurationId,
                                                   TenantProductConfiguration config, String userId);

    TenantProductConfiguration getConfiguration(String tenantId, String configurationId);

    Page<TenantProductConfiguration> getConfigurations(String tenantId, Pageable pageable);

    Page<TenantProductConfiguration> getConfigurationsByStatus(String tenantId,
                                                               ConfigurationStatus status, Pageable pageable);

    List<TenantProductConfiguration> getConfigurationsByCatalogProduct(String tenantId, String catalogProductId);

    TenantProductConfiguration submitForApproval(String tenantId, String configurationId, String userId);

    TenantProductConfiguration approveConfiguration(String tenantId, String configurationId, String approverId);

    TenantProductConfiguration rejectConfiguration(String tenantId, String configurationId,
                                                   String approverId, String reason);

    TenantProductConfiguration activateConfiguration(String tenantId, String configurationId, String userId);

    void deleteConfiguration(String tenantId, String configurationId);

    void syncWithCatalog(String tenantId, String configurationId);
}