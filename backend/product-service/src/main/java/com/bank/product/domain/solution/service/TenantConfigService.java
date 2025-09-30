package com.bank.product.domain.solution.service;

import com.bank.product.domain.solution.model.ConfigurationStatus;
import com.bank.product.domain.solution.model.TenantSolutionConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TenantConfigService {

    TenantSolutionConfiguration createConfiguration(String tenantId, String catalogProductId,
                                                   TenantSolutionConfiguration config, String userId);

    TenantSolutionConfiguration updateConfiguration(String tenantId, String configurationId,
                                                   TenantSolutionConfiguration config, String userId);

    TenantSolutionConfiguration getConfiguration(String tenantId, String configurationId);

    Page<TenantSolutionConfiguration> getConfigurations(String tenantId, Pageable pageable);

    Page<TenantSolutionConfiguration> getConfigurationsByStatus(String tenantId,
                                                               ConfigurationStatus status, Pageable pageable);

    List<TenantSolutionConfiguration> getConfigurationsByCatalogProduct(String tenantId, String catalogProductId);

    TenantSolutionConfiguration submitForApproval(String tenantId, String configurationId, String userId);

    TenantSolutionConfiguration approveConfiguration(String tenantId, String configurationId, String approverId);

    TenantSolutionConfiguration rejectConfiguration(String tenantId, String configurationId,
                                                   String approverId, String reason);

    TenantSolutionConfiguration activateConfiguration(String tenantId, String configurationId, String userId);

    void deleteConfiguration(String tenantId, String configurationId);

    void syncWithCatalog(String tenantId, String configurationId);
}