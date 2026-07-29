package com.taskmanager.task.dto;

import com.taskmanager.task.Tarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import java.time.LocalDateTime;

public record RespostaTarefa(
        Long id,
        Long projetoId,
        String titulo,
        String descricao,
        StatusTarefa status,
        Prioridade prioridade,
        LocalDateTime prazo,
        Long responsavelId,
        String responsavelNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static RespostaTarefa de(Tarefa tarefa) {
        Usuario responsavel = tarefa.getResponsavel();
        return new RespostaTarefa(
                tarefa.getId(),
                tarefa.getProjeto().getId(),
                tarefa.getTitulo(),
                tarefa.getDescricao(),
                tarefa.getStatus(),
                tarefa.getPrioridade(),
                tarefa.getPrazo(),
                responsavel != null ? responsavel.getId() : null,
                responsavel != null ? responsavel.getNome() : null,
                tarefa.getCriadoEm(),
                tarefa.getAtualizadoEm());
    }
}
