package com.example.taskmanager.dto.projeto;

import com.example.taskmanager.dominio.MembroProjeto;
import com.example.taskmanager.dominio.enums.Papel;

public record RespostaMembro(Long usuarioId, String nome, String email, Papel papel) {

    public static RespostaMembro de(MembroProjeto membro) {
        return new RespostaMembro(
                membro.getUsuario().getId(),
                membro.getUsuario().getNome(),
                membro.getUsuario().getEmail(),
                membro.getPapel());
    }
}
