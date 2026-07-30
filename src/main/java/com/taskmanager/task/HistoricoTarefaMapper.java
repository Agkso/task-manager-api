package com.taskmanager.task;

import com.taskmanager.task.dto.RespostaHistoricoTarefa;
import org.springframework.stereotype.Component;

/** Converte a entidade {@link HistoricoTarefa} para o DTO de resposta da API. */
@Component
public class HistoricoTarefaMapper {

    public RespostaHistoricoTarefa paraResposta(HistoricoTarefa historico) {
        return new RespostaHistoricoTarefa(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getUsuario().getNome(),
                historico.getAlteradoEm());
    }
}
