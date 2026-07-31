package com.taskmanager.project;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.dto.RequisicaoAdicionarMembro;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MembroProjetoServiceTest {

    @Mock
    private MembroProjetoRepository membroProjetoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MembroProjetoService membroProjetoService;

    private static final Long PROJETO_ID = 1L;
    private static final Long DONO_ID = 2L;
    private static final Long SOLICITANTE_ID = 3L;

    @BeforeEach
    void montarService() {
        membroProjetoService =
                new MembroProjetoService(membroProjetoRepository, projetoRepository, usuarioRepository, eventPublisher);
    }

    private Usuario usuario(Long id) {
        return Usuario.builder().id(id).nome("Usuario " + id).email("user" + id + "@ex.com").build();
    }

    private MembroProjeto membro(Papel papel) {
        return MembroProjeto.builder().usuario(usuario(SOLICITANTE_ID)).papel(papel).build();
    }

    @Test
    void exigirAdmin_deveRejeitarQuandoSolicitanteEhApenasMember() {
        when(membroProjetoRepository.findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(PROJETO_ID, SOLICITANTE_ID))
                .thenReturn(Optional.of(membro(Papel.MEMBER)));

        assertThatThrownBy(() -> membroProjetoService.exigirAdmin(PROJETO_ID, SOLICITANTE_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void exigirAdmin_deveRejeitarQuandoSolicitanteNaoEhMembro() {
        when(membroProjetoRepository.findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(PROJETO_ID, SOLICITANTE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> membroProjetoService.exigirAdmin(PROJETO_ID, SOLICITANTE_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void remover_deveRejeitarQuandoTentaRemoverODono() {
        when(membroProjetoRepository.findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(PROJETO_ID, SOLICITANTE_ID))
                .thenReturn(Optional.of(membro(Papel.ADMIN)));
        Projeto projeto = Projeto.builder().id(PROJETO_ID).dono(usuario(DONO_ID)).build();
        when(projetoRepository.findByIdAndExcluidoEmIsNull(PROJETO_ID)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> membroProjetoService.remover(PROJETO_ID, DONO_ID, SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("dono");

        verify(membroProjetoRepository, never()).delete(any());
    }

    @Test
    void adicionar_deveRejeitarQuandoUsuarioJaEhMembro() {
        when(membroProjetoRepository.findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(PROJETO_ID, SOLICITANTE_ID))
                .thenReturn(Optional.of(membro(Papel.ADMIN)));
        Projeto projeto = Projeto.builder().id(PROJETO_ID).dono(usuario(DONO_ID)).build();
        when(projetoRepository.findByIdAndExcluidoEmIsNull(PROJETO_ID)).thenReturn(Optional.of(projeto));

        Usuario novoMembro = usuario(5L);
        when(usuarioRepository.findByEmail(novoMembro.getEmail())).thenReturn(Optional.of(novoMembro));
        when(membroProjetoRepository.existsByProjetoIdAndUsuarioId(PROJETO_ID, novoMembro.getId()))
                .thenReturn(true);

        RequisicaoAdicionarMembro requisicao = new RequisicaoAdicionarMembro(novoMembro.getEmail(), Papel.MEMBER);

        assertThatThrownBy(() -> membroProjetoService.adicionar(PROJETO_ID, requisicao, SOLICITANTE_ID))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja e membro");

        verify(membroProjetoRepository, never()).save(any());
    }
}
