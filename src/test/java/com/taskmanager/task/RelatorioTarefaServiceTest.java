package com.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.taskmanager.task.TarefaRepository.ContagemPrioridade;
import com.taskmanager.task.TarefaRepository.ContagemStatus;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelatorioTarefaServiceTest {

    private static final Long PROJETO_ID = 1L;

    @Mock
    private TarefaRepository tarefaRepository;

    private RelatorioTarefaService relatorioTarefaService;

    private ContagemStatus contagemStatus(StatusTarefa status, long total) {
        return new ContagemStatus() {
            @Override
            public StatusTarefa getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private ContagemPrioridade contagemPrioridade(Prioridade prioridade, long total) {
        return new ContagemPrioridade() {
            @Override
            public Prioridade getPrioridade() {
                return prioridade;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void gerar_devePreencherComZeroStatusEPrioridadesSemNenhumaTarefa() {
        relatorioTarefaService = new RelatorioTarefaService(tarefaRepository);
        when(tarefaRepository.contarPorStatus(PROJETO_ID))
                .thenReturn(List.of(contagemStatus(StatusTarefa.TODO, 3L)));
        when(tarefaRepository.contarPorPrioridade(PROJETO_ID))
                .thenReturn(List.of(contagemPrioridade(Prioridade.HIGH, 2L)));

        RespostaRelatorio relatorio = relatorioTarefaService.gerar(PROJETO_ID);

        assertThat(relatorio.byStatus())
                .containsEntry(StatusTarefa.TODO, 3L)
                .containsEntry(StatusTarefa.IN_PROGRESS, 0L)
                .containsEntry(StatusTarefa.DONE, 0L);
        assertThat(relatorio.byPriority())
                .containsEntry(Prioridade.HIGH, 2L)
                .containsEntry(Prioridade.LOW, 0L)
                .containsEntry(Prioridade.MEDIUM, 0L)
                .containsEntry(Prioridade.CRITICAL, 0L);
    }
}
