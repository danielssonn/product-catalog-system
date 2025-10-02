package com.bank.product.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to publish (activate) a workflow template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishTemplateRequest {

    @NotBlank(message = "Publisher ID is required")
    private String publishedBy;

    /**
     * Optional confirmation flag for safety
     */
    private boolean confirmed;
}
