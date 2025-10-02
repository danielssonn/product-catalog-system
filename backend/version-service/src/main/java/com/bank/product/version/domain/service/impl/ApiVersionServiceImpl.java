package com.bank.product.version.domain.service.impl;

import com.bank.product.version.domain.model.ApiVersion;
import com.bank.product.version.domain.model.VersionStatus;
import com.bank.product.version.domain.repository.ApiVersionRepository;
import com.bank.product.version.domain.service.ApiVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiVersionServiceImpl implements ApiVersionService {

    private final ApiVersionRepository apiVersionRepository;

    @Override
    public ApiVersion registerVersion(ApiVersion apiVersion) {
        log.info("Registering API version {} for service {}",
                apiVersion.getVersion(), apiVersion.getServiceId());

        // Check if version already exists
        if (apiVersionRepository.existsByServiceIdAndVersion(
                apiVersion.getServiceId(), apiVersion.getVersion())) {
            throw new IllegalArgumentException(
                    String.format("Version %s already exists for service %s",
                            apiVersion.getVersion(), apiVersion.getServiceId()));
        }

        // Set timestamps
        if (apiVersion.getCreatedAt() == null) {
            apiVersion.setCreatedAt(LocalDateTime.now());
        }
        if (apiVersion.getReleasedAt() == null && apiVersion.getStatus() == VersionStatus.STABLE) {
            apiVersion.setReleasedAt(LocalDateTime.now());
        }

        return apiVersionRepository.save(apiVersion);
    }

    @Override
    public Optional<ApiVersion> getVersion(String serviceId, String version) {
        return apiVersionRepository.findByServiceIdAndVersion(serviceId, version);
    }

    @Override
    public List<ApiVersion> getAllVersions(String serviceId) {
        return apiVersionRepository.findByServiceIdOrderByReleasedAtDesc(serviceId);
    }

    @Override
    public Optional<ApiVersion> getStableVersion(String serviceId) {
        List<ApiVersion> stableVersions = apiVersionRepository
                .findByServiceIdAndStatus(serviceId, VersionStatus.STABLE);

        // Return the most recent stable version
        return stableVersions.stream()
                .max((v1, v2) -> v1.getReleasedAt().compareTo(v2.getReleasedAt()));
    }

    @Override
    public Optional<ApiVersion> getDefaultVersion(String serviceId) {
        // Default to stable version, or the most recent version
        Optional<ApiVersion> stable = getStableVersion(serviceId);
        if (stable.isPresent()) {
            return stable;
        }

        List<ApiVersion> allVersions = getAllVersions(serviceId);
        return allVersions.isEmpty() ? Optional.empty() : Optional.of(allVersions.get(0));
    }

    @Override
    public ApiVersion updateVersionStatus(String serviceId, String version, VersionStatus newStatus) {
        log.info("Updating version {} status to {} for service {}",
                version, newStatus, serviceId);

        ApiVersion apiVersion = apiVersionRepository
                .findByServiceIdAndVersion(serviceId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Version %s not found for service %s", version, serviceId)));

        VersionStatus oldStatus = apiVersion.getStatus();
        apiVersion.setStatus(newStatus);
        apiVersion.setUpdatedAt(LocalDateTime.now());

        // Update lifecycle timestamps
        if (newStatus == VersionStatus.STABLE && apiVersion.getReleasedAt() == null) {
            apiVersion.setReleasedAt(LocalDateTime.now());
        } else if (newStatus == VersionStatus.DEPRECATED && apiVersion.getDeprecatedAt() == null) {
            apiVersion.setDeprecatedAt(LocalDateTime.now());
        } else if (newStatus == VersionStatus.SUNSET && apiVersion.getSunsetAt() == null) {
            apiVersion.setSunsetAt(LocalDateTime.now());
        } else if (newStatus == VersionStatus.EOL && apiVersion.getEolAt() == null) {
            apiVersion.setEolAt(LocalDateTime.now());
        }

        return apiVersionRepository.save(apiVersion);
    }

    @Override
    public ApiVersion deprecateVersion(String serviceId, String version, String reason) {
        log.info("Deprecating version {} for service {}: {}",
                version, serviceId, reason);

        ApiVersion apiVersion = updateVersionStatus(serviceId, version, VersionStatus.DEPRECATED);

        // Add deprecation metadata
        if (apiVersion.getMetadata() == null) {
            apiVersion.setMetadata(new java.util.HashMap<>());
        }
        apiVersion.getMetadata().put("deprecationReason", reason);
        apiVersion.getMetadata().put("deprecatedAt", LocalDateTime.now().toString());

        return apiVersionRepository.save(apiVersion);
    }

    @Override
    public boolean isVersionSupported(String serviceId, String version) {
        Optional<ApiVersion> apiVersion = getVersion(serviceId, version);

        if (apiVersion.isEmpty()) {
            return false;
        }

        VersionStatus status = apiVersion.get().getStatus();
        // EOL versions are not supported
        return status != VersionStatus.EOL;
    }

    @Override
    public Optional<ApiVersion> getVersionBySemanticVersion(String serviceId, String semanticVersion) {
        return getAllVersions(serviceId).stream()
                .filter(v -> semanticVersion.equals(v.getSemanticVersion()))
                .findFirst();
    }
}
