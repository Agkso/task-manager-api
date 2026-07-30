package com.taskmanager.auth.dto;

public record RespostaLogin(String token, String tipo, String refreshToken) {

    public RespostaLogin(String token, String refreshToken) {
        this(token, "Bearer", refreshToken);
    }
}
