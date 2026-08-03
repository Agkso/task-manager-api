package com.taskmanager.project.projeto;

import com.taskmanager.project.dto.RespostaProjeto;
import org.springframework.stereotype.Component;

/** Converte a entidade {@link Projeto} para o DTO de resposta da API. */
@Component
public class ProjetoMapper {

    public RespostaProjeto paraResposta(Projeto projeto) {
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
