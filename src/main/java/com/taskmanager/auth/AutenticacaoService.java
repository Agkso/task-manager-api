package com.taskmanager.auth;

import com.taskmanager.auth.dto.RequisicaoLogin;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import com.taskmanager.auth.dto.RequisicaoRegistro;
import com.taskmanager.auth.dto.RespostaLogin;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public RespostaLogin registrar(RequisicaoRegistro requisicao) {
        if (usuarioRepository.existsByEmail(requisicao.email())) {
            log.warn("Tentativa de registro com email ja cadastrado: {}", requisicao.email());
            throw new RegraNegocioException("Ja existe um usuario cadastrado com esse email");
        }

        Usuario usuario = Usuario.builder()
                .nome(requisicao.nome())
                .email(requisicao.email())
                .senha(passwordEncoder.encode(requisicao.senha()))
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario {} registrado", usuario.getId());
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }

    @Transactional
    public RespostaLogin autenticar(RequisicaoLogin requisicao) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requisicao.email(), requisicao.senha()));
        } catch (BadCredentialsException credenciaisInvalidas) {
            log.warn("Tentativa de login com credenciais invalidas para o email {}", requisicao.email());
            throw credenciaisInvalidas;
        }

        Usuario usuario = usuarioRepository
                .findByEmail(requisicao.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: "
                        + requisicao.email()));

        log.info("Login bem-sucedido: usuario={}", usuario.getId());
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }

    @Transactional
    public RespostaLogin renovar(RequisicaoRefreshToken requisicao) {
        var refreshToken = refreshTokenService.validarERotacionar(requisicao.refreshToken());
        Usuario usuario = refreshToken.getUsuario();
        log.info("Access token renovado via refresh token para usuario {}", usuario.getId());
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }

    @Transactional
    public void logout(RequisicaoRefreshToken requisicao) {
        refreshTokenService.revogar(requisicao.refreshToken());
    }
}
