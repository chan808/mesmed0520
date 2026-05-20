package com.chan.med0515.production.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductModelRequest(
        @NotBlank String name,
        String description
) {
}