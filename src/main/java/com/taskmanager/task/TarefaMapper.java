package com.taskmanager.task;

import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.user.Usuario;
import org.springframework.stereotype.Component;

/** Converte a entidade {@link Tarefa} para o DTO de resposta da API. */
@Component
public class TarefaMapper {

    public RespostaTarefa paraResposta(Tarefa tarefa) {
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
