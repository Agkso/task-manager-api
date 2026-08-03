package com.taskmanager.task.tarefa;

import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.membro.MembroProjetoService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Busca e validacoes de {@link Tarefa} compartilhadas por varios use cases
 * (criar, atualizar, buscar, mudar status, historico). Extraido para nao
 * duplicar a checagem "a tarefa pertence a este projeto?" e a resolucao do
 * responsavel em cada use case individual.
 */
@Component
@RequiredArgsConstructor
public class TarefaHelper {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Busca a tarefa garantindo que ela pertence ao projeto informado -
     * tratada como "nao encontrada" (404) e nao como acesso negado, para nao
     * revelar a um nao-membro que o id existe em outro projeto.
     */
    public Tarefa buscarEntidade(Long projetoId, Long tarefaId) {
        Tarefa tarefa = tarefaRepository
                .findByIdAndExcluidoEmIsNull(tarefaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.tarefaNaoEncontrada(tarefaId)));
        if (!tarefa.getProjeto().getId().equals(projetoId)) {
            throw new RecursoNaoEncontradoException(MensagensErro.tarefaNaoEncontrada(tarefaId));
        }
        return tarefa;
    }

    /** Resolve o responsavel opcional de uma tarefa, exigindo que ele seja membro do projeto. */
    public Usuario resolverResponsavel(Long projetoId, Long responsavelId) {
        if (responsavelId == null) {
            return null;
        }
        if (!membroProjetoService.ehMembro(projetoId, responsavelId)) {
            throw new RegraNegocioException(MensagensErro.RESPONSAVEL_NAO_E_MEMBRO_DO_PROJETO);
        }
        return usuarioRepository
                .findById(responsavelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MensagensErro.usuarioNaoEncontrado(responsavelId)));
    }
}
