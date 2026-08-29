package com.smartlib.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String token;

    private String tokenType;

    private Long userId;

    private String name;

    private String email;

    private String role;
}