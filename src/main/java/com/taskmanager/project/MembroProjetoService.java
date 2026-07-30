package com.taskmanager.project;

import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.dto.RequisicaoAdicionarMembro;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsavel exclusivamente por membership e autorizacao de projeto
 * (quem e membro, quem e ADMIN). Extraido do ProjetoService porque essa
 * checagem e usada tanto por operacoes de projeto quanto por TarefaService,
 * e misturar "CRUD de projeto" com "quem pode fazer o que" tornava o
 * ProjetoService uma classe com dois motivos de mudar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembroProjetoService {

    private final MembroProjetoRepository membroProjetoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;

    public MembroProjeto obterMembro(Long projetoId, Long usuarioId) {
        return membroProjetoRepository
                .findByProjetoIdAndUsuarioId(projetoId, usuarioId)
                .orElseThrow(() -> new AccessDeniedException("Voce nao e membro deste projeto"));
    }

    public void exigirAdmin(Long projetoId, Long usuarioId) {
        MembroProjeto membro = obterMembro(projetoId, usuarioId);
        if (membro.getPapel() != Papel.ADMIN) {
            throw new AccessDeniedException("Apenas o ADMIN do projeto pode realizar esta acao");
        }
    }

    /**
     * Usado por outros modulos (TarefaService, ao validar o responsavel de
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
                .findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado: " + projetoId));

        Usuario usuario = usuarioRepository
                .findByEmail(requisicao.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado com o email: " + requisicao.email()));

        if (membroProjetoRepository.existsByProjetoIdAndUsuarioId(projetoId, usuario.getId())) {
            throw new RegraNegocioException("Usuario ja e membro deste projeto");
        }

        MembroProjeto membro = MembroProjeto.builder()
                .projeto(projeto)
                .usuario(usuario)
                .papel(requisicao.papel())
                .build();
        membro = membroProjetoRepository.save(membro);
        log.info("Usuario {} adicionado ao projeto {} com papel {}", usuario.getId(), projetoId, requisicao.papel());
        return membro;
    }

    @Transactional
    public void remover(Long projetoId, Long usuarioId, Long solicitanteId) {
        exigirAdmin(projetoId, solicitanteId);

        Projeto projeto = projetoRepository
                .findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado: " + projetoId));

        if (projeto.getDono().getId().equals(usuarioId)) {
            throw new RegraNegocioException("O dono do projeto nao pode ser removido dos membros");
        }

        MembroProjeto membro = obterMembro(projetoId, usuarioId);
        membroProjetoRepository.delete(membro);
        log.info("Usuario {} removido do projeto {}", usuarioId, projetoId);
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
