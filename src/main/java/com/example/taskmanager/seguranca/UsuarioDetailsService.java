package com.example.taskmanager.seguranca;

import com.example.taskmanager.repositorio.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository
                .findByEmail(email)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));
    }
}
