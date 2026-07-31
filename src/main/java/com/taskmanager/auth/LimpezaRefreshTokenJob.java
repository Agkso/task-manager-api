package com.taskmanager.auth;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * refresh_tokens so cresce: cada login/renovacao insere uma linha, e nada
 * removia as que ja nao servem pra nada (expiradas ou revogadas). Sem esse
 * job, a tabela cresce indefinidamente num sistema de vida longa, so pra
 * guardar tokens que nunca mais vao ser validados. Mantem uma janela de
 * retencao (nao apaga na hora que expira) pra dar folga caso algum dia
 * precise investigar uso indevido de um token recente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpezaRefreshTokenJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-cleanup-retention-days}")
    private final long retencaoDias;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void limpar() {
        LocalDateTime antesDe = LocalDateTime.now().minusDays(retencaoDias);
        int removidos = refreshTokenRepository.excluirExpiradosOuRevogadosAntesDe(antesDe);
        if (removidos > 0) {
            log.info("Limpeza de refresh tokens: {} removidos (expirados/revogados antes de {})", removidos, antesDe);
        }
    }
}
