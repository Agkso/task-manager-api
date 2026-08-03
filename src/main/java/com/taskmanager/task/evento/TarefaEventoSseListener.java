package com.taskmanager.task.evento;

import com.taskmanager.task.dto.RespostaEventoStatusTarefa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ponte entre o evento de dominio ({@link TarefaStatusAlteradoEvent}) e os
 * clientes SSE inscritos no projeto ({@link TarefaEventoBroadcaster}).
 * Desacoplado de {@link HistoricoTarefaListener} de proposito: sao duas
 * reacoes independentes ao mesmo evento (uma persiste, a outra so notifica
 * quem esta olhando o board agora) - misturar as duas faria um listener
 * carregar responsabilidade dupla, e um erro na notificacao SSE (ex.:
 * cliente desconectado) nao deveria arriscar a gravacao do historico.
 *
 * AFTER_COMMIT pelo mesmo motivo do HistoricoTarefaListener: nao faz sentido
 * avisar o board sobre uma mudanca que acabou sofrendo rollback. Nao precisa
 * de REQUIRES_NEW (ao contrario do HistoricoTarefaListener) porque nao
 * escreve no banco - so envia dados em memoria pros emissores SSE.
 */
@Component
@RequiredArgsConstructor
public class TarefaEventoSseListener {

    private static final String EVENTO_STATUS_ALTERADO = "status-alterado";

    private final TarefaEventoBroadcaster tarefaEventoBroadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoMudarStatus(TarefaStatusAlteradoEvent evento) {
        tarefaEventoBroadcaster.publicar(
                evento.projetoId(),
                EVENTO_STATUS_ALTERADO,
                new RespostaEventoStatusTarefa(evento.tarefaId(), evento.statusAnterior(), evento.statusNovo()));
    }
}
