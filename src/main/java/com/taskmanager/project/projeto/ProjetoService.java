package com.taskmanager.project.projeto;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.project.dto.RequisicaoProjeto;
import com.taskmanager.project.membro.MembroProjetoService;
import com.taskmanager.user.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de projeto. Quem-pode-fazer-o-que fica em MembroProjetoService -
 * ver o javadoc de la para o motivo da separacao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final MembroProjetoService membroProjetoService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Projeto criar(RequisicaoProjeto requisicao, Usuario dono) {
        Projeto projeto = Projeto.builder()
                .nome(requisicao.nome())
                .descricao(requisicao.descricao())
                .dono(dono)
                .build();
        projeto = projetoRepository.save(projeto);

        membroProjetoService.criarComoAdmin(projeto, dono);
        log.info("Projeto {} criado por usuario {}", projeto.getId(), dono.getId());
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.PROJETO_CRIADO, TipoEntidadeAuditoria.PROJETO, projeto.getId(), projeto.getId(), dono.getId()));
        return projeto;
    }

    public List<Projeto> listarDoUsuario(Long usuarioId) {
        return projetoRepository.findByMembros_Usuario_IdAndExcluidoEmIsNull(usuarioId);
    }

    public Projeto buscarPorId(Long projetoId) {
        return projetoRepository
                .findByIdAndExcluidoEmIsNull(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.projetoNaoEncontrado(projetoId)));
    }

    @Transactional
    public Projeto atualizar(Long projetoId, RequisicaoProjeto requisicao, Long solicitanteId) {
        membroProjetoService.exigirAdmin(projetoId, solicitanteId);
        Projeto projeto = buscarPorId(projetoId);
        projeto.setNome(requisicao.nome());
        projeto.setDescricao(requisicao.descricao());
        Projeto salvo = projetoRepository.save(projeto);
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.PROJETO_ATUALIZADO, TipoEntidadeAuditoria.PROJETO, projetoId, projetoId, solicitanteId));
        return salvo;
    }

    /**
     * Soft delete: marca excluidoEm em vez de fazer DELETE fisico (ver
     * migration V5). Publica ProjetoExcluidoEvent para que task (via
     * TarefaProjetoExcluidoListener) cascade o soft delete nas tarefas do
     * projeto - sem inverter a dependencia (project nunca importa task).
     */
    @Transactional
    public void excluir(Long projetoId, Long solicitanteId) {
        Projeto projeto = buscarPorId(projetoId);
        if (!projeto.getDono().getId().equals(solicitanteId)) {
            throw new AccessDeniedException(MensagensErro.APENAS_DONO_PODE_EXCLUIR_PROJETO);
        }
        projeto.setExcluidoEm(LocalDateTime.now());
        projetoRepository.save(projeto);
        log.info("Projeto {} excluido por usuario {}", projetoId, solicitanteId);
        eventPublisher.publishEvent(new ProjetoExcluidoEvent(projetoId));
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.PROJETO_EXCLUIDO, TipoEntidadeAuditoria.PROJETO, projetoId, projetoId, solicitanteId));
    }
}
