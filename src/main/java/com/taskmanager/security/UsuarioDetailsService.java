package com.taskmanager.security;

import com.taskmanager.exception.MensagensErro;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Ponte entre {@link com.taskmanager.user.Usuario} e o {@link UserDetails} que o Spring Security espera. */
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository
                .findByEmail(email)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException(MensagensErro.usuarioNaoEncontrado(email)));
    }
}
