package com.taskmanager.project;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.dto.RequisicaoAdicionarMembro;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsavel exclusivamente por membership e autorizacao de projeto
 * (quem e membro, quem e ADMIN). Extraido do ProjetoService porque essa
 * checagem e usada tanto por operacoes de projeto quanto pelos use cases
 * de tarefa (com.taskmanager.task.usecase), e misturar "CRUD de projeto"
 * com "quem pode fazer o que" tornava o ProjetoService uma classe com dois
 * motivos de mudar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembroProjetoService {

    private final MembroProjetoRepository membroProjetoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MembroProjeto obterMembro(Long projetoId, Long usuarioId) {
        return membroProjetoRepository
                .findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(projetoId, usuarioId)
                .orElseThrow(() -> new AccessDeniedException(MensagensErro.NAO_E_MEMBRO_DO_PROJETO));
    }

    public void exigirAdmin(Long projetoId, Long usuarioId) {
        MembroProjeto membro = obterMembro(projetoId, usuarioId);
        if (membro.getPapel() != Papel.ADMIN) {
            throw new AccessDeniedException(MensagensErro.APENAS_ADMIN_PODE_REALIZAR_ACAO);
        }
    }

    /**
     * Usado por outros modulos (TarefaHelper, ao validar o responsavel de
     * uma tarefa) que precisam saber se um usuario e membro sem precisar de
     * autorizacao para a propria consulta nem acesso direto a
     * MembroProjetoRepository - mantem o repositorio como detalhe interno
     * deste modulo.
     */
    public boolean ehMembro(Long projetoId, Long usuarioId) {
        return membroProjetoRepository.existsByProjetoIdAndUsuarioId(projetoId, usuarioId);
    }

    public List<MembroProjeto> listar(Long projetoId, Long solicitanteId) {
        obterMembro(projetoId, solicitanteId);
        return membroProjetoRepository.findByProjetoId(projetoId);
    }

    @Transactional
    public MembroProjeto adicionar(Long projetoId, RequisicaoAdicionarMembro requisicao, Long solicitanteId) {
        exigirAdmin(projetoId, solicitanteId);

        Projeto projeto = projetoRepository
                .findByIdAndExcluidoEmIsNull(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.projetoNaoEncontrado(projetoId)));

        Usuario usuario = usuarioRepository
                .findByEmail(requisicao.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        MensagensErro.usuarioNaoEncontradoPorEmail(requisicao.email())));

        if (membroProjetoRepository.existsByProjetoIdAndUsuarioId(projetoId, usuario.getId())) {
            throw new RegraNegocioException(MensagensErro.USUARIO_JA_E_MEMBRO);
        }

        MembroProjeto membro = MembroProjeto.builder()
                .projeto(projeto)
                .usuario(usuario)
                .papel(requisicao.papel())
                .build();
        membro = membroProjetoRepository.save(membro);
        log.info("Usuario {} adicionado ao projeto {} com papel {}", usuario.getId(), projetoId, requisicao.papel());
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.MEMBRO_ADICIONADO,
                TipoEntidadeAuditoria.MEMBRO_PROJETO,
                membro.getId(),
                projetoId,
                solicitanteId,
                "papel=" + requisicao.papel()));
        return membro;
    }

    @Transactional
    public void remover(Long projetoId, Long usuarioId, Long solicitanteId) {
        exigirAdmin(projetoId, solicitanteId);

        Projeto projeto = projetoRepository
                .findByIdAndExcluidoEmIsNull(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.projetoNaoEncontrado(projetoId)));

        if (projeto.getDono().getId().equals(usuarioId)) {
            throw new RegraNegocioException(MensagensErro.DONO_NAO_PODE_SER_REMOVIDO_DOS_MEMBROS);
        }

        MembroProjeto membro = obterMembro(projetoId, usuarioId);
        membroProjetoRepository.delete(membro);
        log.info("Usuario {} removido do projeto {}", usuarioId, projetoId);
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.MEMBRO_REMOVIDO, TipoEntidadeAuditoria.MEMBRO_PROJETO, membro.getId(), projetoId, solicitanteId));
    }

    @Transactional
    public MembroProjeto criarComoAdmin(Projeto projeto, Usuario usuario) {
        MembroProjeto membro = MembroProjeto.builder()
                .projeto(projeto)
                .usuario(usuario)
                .papel(Papel.ADMIN)
                .build();
        return membroProjetoRepository.save(membro);
    }
}
