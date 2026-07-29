package com.taskmanager.project;

import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.dto.RequisicaoAdicionarMembro;
import com.taskmanager.project.dto.RequisicaoProjeto;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final MembroProjetoRepository membroProjetoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProjetoService(
            ProjetoRepository projetoRepository,
            MembroProjetoRepository membroProjetoRepository,
            UsuarioRepository usuarioRepository) {
        this.projetoRepository = projetoRepository;
        this.membroProjetoRepository = membroProjetoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Projeto criar(RequisicaoProjeto requisicao, Usuario dono) {
        Projeto projeto = Projeto.builder()
                .nome(requisicao.nome())
                .descricao(requisicao.descricao())
                .dono(dono)
                .build();
        projeto = projetoRepository.save(projeto);

        MembroProjeto membroDono = MembroProjeto.builder()
                .projeto(projeto)
                .usuario(dono)
                .papel(Papel.ADMIN)
                .build();
        membroProjetoRepository.save(membroDono);

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
        exigirAdmin(projetoId, solicitanteId);
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
    }

    public List<MembroProjeto> listarMembros(Long projetoId, Long solicitanteId) {
        obterMembro(projetoId, solicitanteId);
        return membroProjetoRepository.findByProjetoId(projetoId);
    }

    @Transactional
    public MembroProjeto adicionarMembro(Long projetoId, RequisicaoAdicionarMembro requisicao, Long solicitanteId) {
        exigirAdmin(projetoId, solicitanteId);
        Projeto projeto = buscarPorId(projetoId);

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
        return membroProjetoRepository.save(membro);
    }

    @Transactional
    public void removerMembro(Long projetoId, Long usuarioId, Long solicitanteId) {
        exigirAdmin(projetoId, solicitanteId);
        Projeto projeto = buscarPorId(projetoId);

        if (projeto.getDono().getId().equals(usuarioId)) {
            throw new RegraNegocioException("O dono do projeto nao pode ser removido dos membros");
        }

        MembroProjeto membro = obterMembro(projetoId, usuarioId);
        membroProjetoRepository.delete(membro);
    }

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
}
