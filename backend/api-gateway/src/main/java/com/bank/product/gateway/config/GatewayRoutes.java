package com.bank.product.gateway.config;

import com.bank.product.gateway.filter.ChannelIdentificationFilter;
import com.bank.product.gateway.filter.MultiTenancyFilter;
import com.bank.product.gateway.filter.PartyAwareFilter;
import com.bank.product.gateway.filter.AuditLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration for all channels
 * Routes requests to appropriate backend services based on channel type
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRoutes {

    private final ChannelIdentificationFilter channelIdentificationFilter;
    private final MultiTenancyFilter multiTenancyFilter;
    private final PartyAwareFilter partyAwareFilter;
    private final AuditLoggingFilter auditLoggingFilter;

    @Value("${services.product-service.url:http://product-service:8082}")
    private String productServiceUrl;

    @Value("${services.workflow-service.url:http://workflow-service:8089}")
    private String workflowServiceUrl;

    @Value("${services.party-service.url:http://party-service:8083}")
    private String partyServiceUrl;

    @Value("${services.file-processing-service.url:http://file-processing-service:8094}")
    private String fileProcessingServiceUrl;

    @Value("${services.payment-service.url:http://payment-service:8095}")
    private String paymentServiceUrl;

    @Value("${services.application-service.url:http://application-service:8096}")
    private String applicationServiceUrl;

    @Value("${services.auth-service.url:http://auth-service:8097}")
    private String authServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // ======== AUTHENTICATION SERVICE (OAuth) ========
            // OAuth endpoints are public (no filters) - handled by auth-service
            .route("auth_service_oauth", r -> r
                .path("/oauth/**")
                .uri(authServiceUrl))

            // ======== PUBLIC API CHANNEL ========
            .route("public_api_product_service", r -> r
                .path("/api/v*/products/**", "/api/v*/solutions/**", "/api/v*/catalog/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("PUBLIC_API")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter))
                .uri(productServiceUrl))
            
            .route("public_api_workflow_service", r -> r
                .path("/api/v*/workflows/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("PUBLIC_API")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter)
)
                .uri(workflowServiceUrl))
            
            .route("public_api_party_service", r -> r
                .path("/api/v*/parties/**", "/api/v*/relationships/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("PUBLIC_API")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter)
)
                .uri(partyServiceUrl))
            
            // ======== HOST-TO-HOST FILE CHANNEL ========
            .route("host_to_host_file_upload", r -> r
                .path("/channel/host-to-host/files/upload")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("HOST_TO_HOST")))
                    .filter(multiTenancyFilter)
                    .filter(auditLoggingFilter)
)
                .uri(fileProcessingServiceUrl))
            
            .route("host_to_host_file_status", r -> r
                .path("/channel/host-to-host/files/{fileId}/status")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("HOST_TO_HOST")))
                    .filter(multiTenancyFilter)
                    .filter(auditLoggingFilter))
                .uri(fileProcessingServiceUrl))
            
            // ======== ERP INTEGRATION CHANNEL (Kyriba, SAP Treasury, etc.) ========
            .route("erp_product_configure", r -> r
                .path("/channel/erp/products/configure")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("ERP_INTEGRATION")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter)
)
                .uri(productServiceUrl))
            
            .route("erp_payment_originate", r -> r
                .path("/channel/erp/payments/originate")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("ERP_INTEGRATION")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter)
)
                .uri(paymentServiceUrl))
            
            // ======== CLIENT SELF-SERVICE PORTAL ========
            .route("client_portal_products", r -> r
                .path("/channel/portal/products/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("CLIENT_PORTAL")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter)
)
                .uri(productServiceUrl))
            
            .route("client_portal_applications", r -> r
                .path("/channel/portal/applications/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("CLIENT_PORTAL")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter))
                .uri(applicationServiceUrl))
            
            // ======== SALESFORCE OPERATIONS WORKBENCH ========
            .route("salesforce_workflows", r -> r
                .path("/channel/salesforce/workflows/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("SALESFORCE_OPS")))
                    .filter(multiTenancyFilter)
                    .filter(auditLoggingFilter)
                    .circuitBreaker(config -> config.setName("workflow-service-cb")))
                .uri(workflowServiceUrl))
            
            .route("salesforce_parties", r -> r
                .path("/channel/salesforce/parties/**")
                .filters(f -> f
                    .filter(channelIdentificationFilter.apply(new ChannelIdentificationFilter.Config("SALESFORCE_OPS")))
                    .filter(multiTenancyFilter)
                    .filter(partyAwareFilter)
                    .filter(auditLoggingFilter))
                .uri(partyServiceUrl))

            .build();
    }
}
