package com.taskmanager.project.membro;

import com.taskmanager.project.dto.RespostaMembro;
import org.springframework.stereotype.Component;

/** Converte a entidade {@link MembroProjeto} para o DTO de resposta da API. */
@Component
public class MembroMapper {

    public RespostaMembro paraResposta(MembroProjeto membro) {
        return new RespostaMembro(
                membro.getUsuario().getId(),
                membro.getUsuario().getNome(),
                membro.getUsuario().getEmail(),
                membro.getPapel());
    }
}
