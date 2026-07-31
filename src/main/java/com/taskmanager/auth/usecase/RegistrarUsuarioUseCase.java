package com.taskmanager.auth.usecase;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.auth.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoRegistro;
import com.taskmanager.auth.dto.RespostaLogin;
import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cadastra um novo usuario e ja devolve o par de tokens (access + refresh), como um login implicito. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RespostaLogin executar(RequisicaoRegistro requisicao) {
        if (usuarioRepository.existsByEmail(requisicao.email())) {
            log.warn("Tentativa de registro com email ja cadastrado: {}", requisicao.email());
            throw new RegraNegocioException(MensagensErro.EMAIL_JA_CADASTRADO);
        }

        Usuario usuario = Usuario.builder()
                .nome(requisicao.nome())
                .email(requisicao.email())
                .senha(passwordEncoder.encode(requisicao.senha()))
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario {} registrado", usuario.getId());
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.USUARIO_REGISTRADO, TipoEntidadeAuditoria.USUARIO, usuario.getId(), null, usuario.getId()));
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }
}
