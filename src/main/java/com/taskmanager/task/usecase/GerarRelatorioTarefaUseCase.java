package com.taskmanager.task.usecase;

import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.RelatorioTarefaService;
import com.taskmanager.task.dto.RespostaRelatorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o relatorio agregado (contagem por status e por prioridade) de um
 * projeto. A agregacao em si (cacheada por projetoId) vive em
 * {@link RelatorioTarefaService} - ver o javadoc de la para o motivo de nao
 * cachear este metodo diretamente: a checagem de membership precisa rodar
 * sempre, mesmo em cache hit.
 */
@Component
@RequiredArgsConstructor
public class GerarRelatorioTarefaUseCase {

    private final MembroProjetoService membroProjetoService;
    private final RelatorioTarefaService relatorioTarefaService;

    @Transactional(readOnly = true)
    public RespostaRelatorio executar(Long projetoId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        return relatorioTarefaService.gerar(projetoId);
    }
}
