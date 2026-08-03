package com.taskmanager.auth.passwordreset;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Mesmo motivo do LimpezaRefreshTokenJob: password_reset_tokens so cresce, sem isso nunca some. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpezaPasswordResetTokenJob {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.jwt.refresh-cleanup-retention-days}")
    private final long retencaoDias;

    @Transactional
    @Scheduled(cron = "0 15 3 * * *")
    public void limpar() {
        LocalDateTime antesDe = LocalDateTime.now().minusDays(retencaoDias);
        int removidos = passwordResetTokenRepository.excluirExpiradosOuUsadosAntesDe(antesDe);
        if (removidos > 0) {
            log.info(
                    "Limpeza de tokens de reset de senha: {} removidos (expirados/usados antes de {})",
                    removidos,
                    antesDe);
        }
    }
}
