package com.bank.product.version.domain.repository;

import com.bank.product.version.domain.model.ApiEndpointVersion;
import com.bank.product.version.domain.model.VersionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiEndpointVersionRepository extends MongoRepository<ApiEndpointVersion, String> {

    /**
     * Find endpoint by service, version, path, and method
     */
    Optional<ApiEndpointVersion> findByServiceIdAndApiVersionAndEndpointPathAndHttpMethod(
            String serviceId, String apiVersion, String endpointPath, String httpMethod);

    /**
     * Find all endpoints for a service version
     */
    List<ApiEndpointVersion> findByServiceIdAndApiVersion(String serviceId, String apiVersion);

    /**
     * Find deprecated endpoints
     */
    List<ApiEndpointVersion> findByServiceIdAndStatus(String serviceId, VersionStatus status);
}
