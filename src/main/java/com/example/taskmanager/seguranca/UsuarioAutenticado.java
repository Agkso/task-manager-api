package com.example.taskmanager.seguranca;

import com.example.taskmanager.dominio.Usuario;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Autorizacao de projeto (ADMIN/MEMBER) e verificada por projeto na camada de
 * servico, consultando MembroProjeto - nao existe papel global aqui, so uma
 * authority generica pra satisfazer o contrato do Spring Security.
 */
@Getter
public class UsuarioAutenticado implements UserDetails {

    private final Usuario usuario;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Long getUsuarioId() {
        return usuario.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }
}
