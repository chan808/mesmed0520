package com.chan.med0515.auth.dto;

public record LoginResponse(
        String accessToken,
        Long userId,
        String role
) {}