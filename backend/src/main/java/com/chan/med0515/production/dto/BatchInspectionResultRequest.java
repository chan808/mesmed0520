package com.chan.med0515.production.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchInspectionResultRequest(
        @NotEmpty @Valid List<InspectionResultRequest> items
) {}
