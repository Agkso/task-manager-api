package com.taskmanager.task.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.membro.MembroProjeto;
import com.taskmanager.project.membro.MembroProjetoService;
import com.taskmanager.project.projeto.Projeto;
import com.taskmanager.project.projeto.ProjetoService;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.task.tarefa.TarefaHelper;
import com.taskmanager.task.tarefa.TarefaMapper;
import com.taskmanager.task.tarefa.TarefaRepository;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CriarTarefaUseCaseTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private MembroProjetoService membroProjetoService;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CriarTarefaUseCase criarTarefaUseCase;

    private static final Long PROJETO_ID = 1L;
    private static final Long RESPONSAVEL_ID = 2L;
    private static final Long SOLICITANTE_ID = 4L;

    @BeforeEach
    void montarUseCase() {
        TarefaHelper tarefaHelper = new TarefaHelper(tarefaRepository, membroProjetoService, usuarioRepository);
        criarTarefaUseCase = new CriarTarefaUseCase(
                tarefaRepository, membroProjetoService, projetoService, tarefaHelper, new TarefaMapper(), eventPublisher);
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().id(id).nome("Usuario " + id).email("user" + id + "@ex.com").build();
    }

    private Projeto projeto() {
        return Projeto.builder().id(PROJETO_ID).nome("Projeto").dono(usuario(99L)).build();
    }

    private MembroProjeto membro(Papel papel) {
        return MembroProjeto.builder().projeto(projeto()).usuario(usuario(SOLICITANTE_ID)).papel(papel).build();
    }

    @Test
    void executar_deveRejeitarQuandoResponsavelNaoEhMembroDoProjeto() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.ADMIN));
        when(projetoService.buscarPorId(PROJETO_ID)).thenReturn(projeto());
        when(membroProjetoService.ehMembro(PROJETO_ID, RESPONSAVEL_ID)).thenReturn(false);

        RequisicaoTarefa requisicao = new RequisicaoTarefa("Titulo", "desc", Prioridade.LOW, null, RESPONSAVEL_ID);

        assertThatThrownBy(() -> criarTarefaUseCase.executar(PROJETO_ID, requisicao, SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("membro");

        verify(tarefaRepository, never()).save(any());
        verify(usuarioRepository, never()).findById(anyLong());
    }
}
