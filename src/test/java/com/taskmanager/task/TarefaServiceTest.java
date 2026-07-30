package com.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.project.Projeto;
import com.taskmanager.project.ProjetoService;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * RegrasTransicaoStatusTarefa entra como instancia real (nao mock): e uma
 * regra pura, sem dependencia de repositorio, entao mockar so esconderia
 * o comportamento que a gente quer garantir aqui. So mockamos o que tem
 * IO de verdade (repositorios, o service de membership).
 */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private MembroProjetoService membroProjetoService;

    @Mock
    private ProjetoService projetoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private TarefaService tarefaService;

    private static final Long PROJETO_ID = 1L;
    private static final Long RESPONSAVEL_ID = 2L;
    private static final Long TAREFA_ID = 3L;
    private static final Long SOLICITANTE_ID = 4L;

    @BeforeEach
    void montarService() {
        tarefaService = new TarefaService(
                tarefaRepository,
                membroProjetoService,
                projetoService,
                usuarioRepository,
                new RegrasTransicaoStatusTarefa());
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

    private Tarefa tarefa(StatusTarefa status, Prioridade prioridade, Long responsavelId) {
        return Tarefa.builder()
                .id(TAREFA_ID)
                .projeto(projeto())
                .titulo("Tarefa")
                .status(status)
                .prioridade(prioridade)
                .responsavel(responsavelId == null ? null : usuario(responsavelId))
                .build();
    }

    @Test
    void mudarStatus_deveRejeitarVoltaDeDoneParaTodo() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.ADMIN));
        when(tarefaRepository.findById(TAREFA_ID))
                .thenReturn(Optional.of(tarefa(StatusTarefa.DONE, Prioridade.LOW, RESPONSAVEL_ID)));

        assertThatThrownBy(() -> tarefaService.mudarStatus(
                        PROJETO_ID, TAREFA_ID, new RequisicaoAtualizarStatus(StatusTarefa.TODO), SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("DONE");

        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void mudarStatus_deveBloquearFechamentoDeCriticaPorMembroComum() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.MEMBER));
        when(tarefaRepository.findById(TAREFA_ID))
                .thenReturn(Optional.of(tarefa(StatusTarefa.IN_PROGRESS, Prioridade.CRITICAL, RESPONSAVEL_ID)));

        assertThatThrownBy(() -> tarefaService.mudarStatus(
                        PROJETO_ID, TAREFA_ID, new RequisicaoAtualizarStatus(StatusTarefa.DONE), SOLICITANTE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void mudarStatus_devePermitirFechamentoDeCriticaPorAdmin() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.ADMIN));
        Tarefa critica = tarefa(StatusTarefa.IN_PROGRESS, Prioridade.CRITICAL, RESPONSAVEL_ID);
        when(tarefaRepository.findById(TAREFA_ID)).thenReturn(Optional.of(critica));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

        var resposta = tarefaService.mudarStatus(
                PROJETO_ID, TAREFA_ID, new RequisicaoAtualizarStatus(StatusTarefa.DONE), SOLICITANTE_ID);

        assertThat(resposta.status()).isEqualTo(StatusTarefa.DONE);
    }

    @Test
    void mudarStatus_deveRejeitarQuandoResponsavelAtingiuLimiteDeWip() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.MEMBER));
        when(tarefaRepository.findById(TAREFA_ID))
                .thenReturn(Optional.of(tarefa(StatusTarefa.TODO, Prioridade.MEDIUM, RESPONSAVEL_ID)));
        when(tarefaRepository.countByResponsavelIdAndStatus(RESPONSAVEL_ID, StatusTarefa.IN_PROGRESS))
                .thenReturn(5L);

        assertThatThrownBy(() -> tarefaService.mudarStatus(
                        PROJETO_ID, TAREFA_ID, new RequisicaoAtualizarStatus(StatusTarefa.IN_PROGRESS), SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Limite");

        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void mudarStatus_devePermitirQuandoAbaixoDoLimiteDeWip() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.MEMBER));
        when(tarefaRepository.findById(TAREFA_ID))
                .thenReturn(Optional.of(tarefa(StatusTarefa.TODO, Prioridade.MEDIUM, RESPONSAVEL_ID)));
        when(tarefaRepository.countByResponsavelIdAndStatus(RESPONSAVEL_ID, StatusTarefa.IN_PROGRESS))
                .thenReturn(4L);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(chamada -> chamada.getArgument(0));

        var resposta = tarefaService.mudarStatus(
                PROJETO_ID, TAREFA_ID, new RequisicaoAtualizarStatus(StatusTarefa.IN_PROGRESS), SOLICITANTE_ID);

        assertThat(resposta.status()).isEqualTo(StatusTarefa.IN_PROGRESS);
    }

    @Test
    void criar_deveRejeitarQuandoResponsavelNaoEhMembroDoProjeto() {
        when(membroProjetoService.obterMembro(PROJETO_ID, SOLICITANTE_ID)).thenReturn(membro(Papel.ADMIN));
        when(projetoService.buscarPorId(PROJETO_ID)).thenReturn(projeto());
        when(membroProjetoService.ehMembro(PROJETO_ID, RESPONSAVEL_ID)).thenReturn(false);

        RequisicaoTarefa requisicao = new RequisicaoTarefa("Titulo", "desc", Prioridade.LOW, null, RESPONSAVEL_ID);

        assertThatThrownBy(() -> tarefaService.criar(PROJETO_ID, requisicao, SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("membro");

        verify(tarefaRepository, never()).save(any());
        verify(usuarioRepository, never()).findById(anyLong());
    }
}
