package com.taskmanager.auth.dto;

public record RespostaLogin(String token, String tipo) {

    public RespostaLogin(String token) {
        this(token, "Bearer");
    }
}
