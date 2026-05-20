package com.chan.med0515.inspection.dto;

import jakarta.validation.constraints.NotBlank;

public record InspectionItemRequest(

        @NotBlank
        String itemName,

        String specification,

        String method,

        String equipment,

        String timing
) {
}
