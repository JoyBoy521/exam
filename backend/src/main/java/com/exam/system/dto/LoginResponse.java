package com.exam.system.dto;

public record LoginResponse(
        String token,
        String username,
        String role,
        Long userId
) {
}
