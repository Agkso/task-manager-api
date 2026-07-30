package com.taskmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RequisicaoRefreshToken(@NotBlank(message = "refreshToken e obrigatorio") String refreshToken) {}
