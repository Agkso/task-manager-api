package com.taskmanager.project.dto;

import com.taskmanager.project.enums.Papel;

/** Ver {@link com.taskmanager.project.MembroMapper} para a conversao a partir de {@code MembroProjeto}. */
public record RespostaMembro(Long usuarioId, String nome, String email, Papel papel) {}
