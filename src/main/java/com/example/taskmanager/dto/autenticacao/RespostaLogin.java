package com.example.taskmanager.dto.autenticacao;

public record RespostaLogin(String token, String tipo) {

    public RespostaLogin(String token) {
        this(token, "Bearer");
    }
}
