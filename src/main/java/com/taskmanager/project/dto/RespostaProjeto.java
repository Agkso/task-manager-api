package com.taskmanager.project.dto;

import java.time.LocalDateTime;

/** Ver {@link com.taskmanager.project.ProjetoMapper} para a conversao a partir de {@code Projeto}. */
public record RespostaProjeto(
        Long id,
        String nome,
        String descricao,
        Long donoId,
        String donoNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {}
