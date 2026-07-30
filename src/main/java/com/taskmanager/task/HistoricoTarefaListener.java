package com.taskmanager.task;

import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Desacoplado de MudarStatusTarefaUseCase de proposito: gravar historico e
 * uma reacao a mudanca de status, nao responsabilidade de quem muda o
 * status. Isso bate com o que o design ja permitia (toda mudanca de status
 * passa por MudarStatusTarefaUseCase) - so precisou publicar um evento la,
 * sem mexer em controller nenhum.
 *
 * AFTER_COMMIT evita gravar historico de uma transacao que acabou dando
 * rollback (ex.: se uma regra de negocio barrasse a mudanca depois do
 * evento ser publicado - hoje nao acontece, mas o listener nao deveria
 * depender de saber disso).
 *
 * REQUIRES_NEW e obrigatorio aqui: apos o commit, a transacao original ja
 * fechou, entao esse metodo precisa abrir a propria transacao pra poder
 * salvar. O Spring ate recusa subir o contexto sem isso (fail-fast).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricoTarefaListener {

    private final HistoricoTarefaRepository historicoTarefaRepository;
    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoMudarStatus(TarefaStatusAlteradoEvent evento) {
        Tarefa tarefa = tarefaRepository.getReferenceById(evento.tarefaId());
        Usuario usuario = usuarioRepository.getReferenceById(evento.usuarioId());

        HistoricoTarefa historico = HistoricoTarefa.builder()
                .tarefa(tarefa)
                .usuario(usuario)
                .statusAnterior(evento.statusAnterior())
                .statusNovo(evento.statusNovo())
                .alteradoEm(LocalDateTime.now())
                .build();

        historicoTarefaRepository.save(historico);
        log.debug(
                "Historico registrado: tarefa {} {} -> {}",
                evento.tarefaId(),
                evento.statusAnterior(),
                evento.statusNovo());
    }
}
