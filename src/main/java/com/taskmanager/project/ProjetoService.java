package com.taskmanager.project;

import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.project.dto.RequisicaoProjeto;
import com.taskmanager.user.Usuario;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de projeto. Quem-pode-fazer-o-que fica em MembroProjetoService -
 * ver o javadoc de la para o motivo da separacao.
 */
@Slf4j
@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final MembroProjetoService membroProjetoService;

    public ProjetoService(ProjetoRepository projetoRepository, MembroProjetoService membroProjetoService) {
        this.projetoRepository = projetoRepository;
        this.membroProjetoService = membroProjetoService;
    }

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
        return projeto;
    }

    public List<Projeto> listarDoUsuario(Long usuarioId) {
        return projetoRepository.findByMembros_Usuario_Id(usuarioId);
    }

    public Projeto buscarPorId(Long projetoId) {
        return projetoRepository
                .findById(projetoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto nao encontrado: " + projetoId));
    }

    @Transactional
    public Projeto atualizar(Long projetoId, RequisicaoProjeto requisicao, Long solicitanteId) {
        membroProjetoService.exigirAdmin(projetoId, solicitanteId);
        Projeto projeto = buscarPorId(projetoId);
        projeto.setNome(requisicao.nome());
        projeto.setDescricao(requisicao.descricao());
        return projetoRepository.save(projeto);
    }

    @Transactional
    public void excluir(Long projetoId, Long solicitanteId) {
        Projeto projeto = buscarPorId(projetoId);
        if (!projeto.getDono().getId().equals(solicitanteId)) {
            throw new AccessDeniedException("Apenas o dono do projeto pode exclui-lo");
        }
        projetoRepository.delete(projeto);
        log.info("Projeto {} excluido por usuario {}", projetoId, solicitanteId);
    }
}
