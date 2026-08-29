package com.smartlib.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;

    private String name;

    private String email;

    private String role;

    private Boolean active;
}