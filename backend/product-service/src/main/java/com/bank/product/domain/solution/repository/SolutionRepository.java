package com.bank.product.domain.solution.repository;

import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolutionRepository extends MongoRepository<Solution, String> {

    Optional<Solution> findByTenantIdAndSolutionId(String tenantId, String solutionId);

    List<Solution> findByTenantIdAndStatus(String tenantId, SolutionStatus status);

    Page<Solution> findByTenantId(String tenantId, Pageable pageable);

    Page<Solution> findByTenantIdAndStatus(String tenantId, SolutionStatus status, Pageable pageable);

    Page<Solution> findByTenantIdAndCategory(String tenantId, String category, Pageable pageable);

    Page<Solution> findByTenantIdAndCatalogProductId(String tenantId, String catalogProductId, Pageable pageable);

    @Query("{ 'tenantId': ?0, 'availableChannels': { $in: [?1] } }")
    Page<Solution> findByTenantIdAndChannel(String tenantId, String channel, Pageable pageable);

    boolean existsByTenantIdAndSolutionId(String tenantId, String solutionId);

    void deleteByTenantIdAndSolutionId(String tenantId, String solutionId);
}