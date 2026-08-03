package com.taskmanager.auth.usecase;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.auth.passwordreset.PasswordResetTokenService;
import com.taskmanager.auth.dto.RequisicaoEsqueciSenha;
import com.taskmanager.email.EmailService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o token de reset e dispara o email - responde do mesmo jeito (void,
 * 200) independente do email existir ou nao na base. Se o endpoint
 * respondesse diferente (404 pra email desconhecido, por ex.), qualquer um
 * conseguiria descobrir quais emails tem conta so tentando varios - por
 * isso ifPresentOrElse com o "senao" so logando, nunca lancando.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitarRedefinicaoSenhaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url}")
    private final String frontendUrl;

    @Value("${app.password-reset.expiration-minutes}")
    private final long expiracaoMinutos;

    @Transactional
    public void executar(RequisicaoEsqueciSenha requisicao) {
        usuarioRepository
                .findByEmail(requisicao.email())
                .ifPresentOrElse(this::gerarTokenEEnviarEmail, () -> log.info(
                        "Solicitacao de redefinicao de senha para email nao cadastrado"));
    }

    private void gerarTokenEEnviarEmail(Usuario usuario) {
        String tokenBruto = passwordResetTokenService.gerar(usuario);
        String link = frontendUrl + "/redefinir-senha?token=" + tokenBruto;

        emailService.enviar(
                usuario.getEmail(), "Redefinir sua senha - Task Manager", corpoEmail(usuario.getNome(), link));

        log.info("Solicitacao de redefinicao de senha gerada para usuario {}", usuario.getId());
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.SENHA_RESET_SOLICITADO,
                TipoEntidadeAuditoria.USUARIO,
                usuario.getId(),
                null,
                usuario.getId()));
    }

    private String corpoEmail(String nome, String link) {
        return """
                <p>Ola, %s.</p>
                <p>Recebemos um pedido pra redefinir a senha da sua conta no Task Manager.</p>
                <p><a href="%s">Clique aqui pra escolher uma nova senha</a></p>
                <p>Esse link expira em %d minutos. Se voce nao pediu essa redefinicao, pode ignorar este email - sua senha continua a mesma.</p>
                """
                .formatted(nome, link, expiracaoMinutos);
    }
}
