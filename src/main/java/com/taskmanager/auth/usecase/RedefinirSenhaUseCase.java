package com.taskmanager.auth.usecase;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.auth.passwordreset.PasswordResetTokenService;
import com.taskmanager.auth.token.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoRedefinirSenha;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Troca a senha a partir de um token de reset valido. Tambem revoga todas as
 * sessoes (refresh tokens) existentes do usuario - se a troca foi motivada
 * por uma conta comprometida, uma sessao que o atacante ja tenha nao pode
 * sobreviver a troca de senha.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedefinirSenhaUseCase {

    private final PasswordResetTokenService passwordResetTokenService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void executar(RequisicaoRedefinirSenha requisicao) {
        Usuario usuario = passwordResetTokenService.validarEConsumir(requisicao.token());

        usuario.setSenha(passwordEncoder.encode(requisicao.novaSenha()));
        usuarioRepository.save(usuario);
        refreshTokenService.revogarTodosDoUsuario(usuario.getId());

        log.info("Senha redefinida para usuario {}", usuario.getId());
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.SENHA_REDEFINIDA, TipoEntidadeAuditoria.USUARIO, usuario.getId(), null, usuario.getId()));
    }
}
