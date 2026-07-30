package com.taskmanager.auth.usecase;

import com.taskmanager.auth.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoLogin;
import com.taskmanager.auth.dto.RespostaLogin;
import com.taskmanager.exception.MensagensErro;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Autentica email/senha via {@link AuthenticationManager} e emite um novo par de tokens. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RespostaLogin executar(RequisicaoLogin requisicao) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requisicao.email(), requisicao.senha()));
        } catch (BadCredentialsException credenciaisInvalidas) {
            log.warn("Tentativa de login com credenciais invalidas para o email {}", requisicao.email());
            throw credenciaisInvalidas;
        }

        Usuario usuario = usuarioRepository
                .findByEmail(requisicao.email())
                .orElseThrow(() -> new IllegalStateException(
                        MensagensErro.usuarioAutenticadoNaoEncontrado(requisicao.email())));

        log.info("Login bem-sucedido: usuario={}", usuario.getId());
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }
}
