package com.taskmanager.project.dto;

import com.taskmanager.project.Projeto;
import java.time.LocalDateTime;

public record RespostaProjeto(
        Long id,
        String nome,
        String descricao,
        Long donoId,
        String donoNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static RespostaProjeto de(Projeto projeto) {
        return new RespostaProjeto(
                projeto.getId(),
                projeto.getNome(),
                projeto.getDescricao(),
                projeto.getDono().getId(),
                projeto.getDono().getNome(),
                projeto.getCriadoEm(),
                projeto.getAtualizadoEm());
    }
}
