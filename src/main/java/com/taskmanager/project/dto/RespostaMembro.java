package com.taskmanager.project.dto;

import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.enums.Papel;

public record RespostaMembro(Long usuarioId, String nome, String email, Papel papel) {

    public static RespostaMembro de(MembroProjeto membro) {
        return new RespostaMembro(
                membro.getUsuario().getId(),
                membro.getUsuario().getNome(),
                membro.getUsuario().getEmail(),
                membro.getPapel());
    }
}
