package com.example.taskmanager.excecao;

/**
 * Violacao de uma regra de negocio (ex.: limite de WIP, transicao de status
 * invalida, email duplicado). Mapeada para 409 pelo ManipuladorGlobalExcecoes.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
