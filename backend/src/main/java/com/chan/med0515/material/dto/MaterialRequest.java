package com.chan.med0515.material.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterialRequest(

        @NotBlank
        String modelName,

        @NotBlank
        String partName,

        String partCode,

        String supplier,

        String materialSpec
) {
}
