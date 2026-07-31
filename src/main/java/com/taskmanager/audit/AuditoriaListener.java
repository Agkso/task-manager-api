package com.taskmanager.audit;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Grava {@link EventoAuditoria} em {@link LogAuditoria}, desacoplado de quem
 * publica (ProjetoService, MembroProjetoService, use cases de tarefa e de
 * autenticacao) - mesmo racional do HistoricoTarefaListener: gravar
 * auditoria e' uma reacao a mutacao, nao responsabilidade de quem muta.
 *
 * AFTER_COMMIT + REQUIRES_NEW pelo mesmo motivo do HistoricoTarefaListener:
 * nao grava auditoria de uma transacao que sofreu rollback, e precisa da
 * propria transacao porque a original ja fechou quando o listener roda.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaListener {

    private final LogAuditoriaRepository logAuditoriaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoOcorrerMutacao(EventoAuditoria evento) {
        LogAuditoria registro = LogAuditoria.builder()
                .acao(evento.acao())
                .tipoEntidade(evento.tipoEntidade())
                .entidadeId(evento.entidadeId())
                .projetoId(evento.projetoId())
                .usuarioId(evento.usuarioId())
                .detalhe(evento.detalhe())
                .criadoEm(LocalDateTime.now())
                .build();
        logAuditoriaRepository.save(registro);
        log.debug(
                "Auditoria registrada: {} {} (id={}, projeto={}, usuario={})",
                evento.acao(),
                evento.tipoEntidade(),
                evento.entidadeId(),
                evento.projetoId(),
                evento.usuarioId());
    }
}
