package com.taskmanager.task.relatorio;

import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.task.tarefa.TarefaRepository;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolado de GerarRelatorioTarefaUseCase de proposito: o proxy de cache do
 * Spring so intercepta a chamada ANTES do metodo rodar - se a checagem de
 * membership (obterMembro) estivesse dentro do mesmo metodo anotado com
 * @Cacheable, um segundo usuario (mesmo sem ser membro do projeto) poderia
 * receber uma resposta ja cacheada sem nunca passar pela checagem de
 * acesso, porque um cache hit nao executa o corpo do metodo. Isolar a
 * agregacao aqui deixa essa garantia visivel na assinatura: quem chama
 * GerarRelatorioTarefaUseCase.executar e obrigado a autorizar antes de
 * chegar aqui.
 */
@Service
@RequiredArgsConstructor
public class RelatorioTarefaService {

    private final TarefaRepository tarefaRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public RespostaRelatorio gerar(Long projetoId) {
        Map<StatusTarefa, Long> porStatus = new EnumMap<>(StatusTarefa.class);
        for (StatusTarefa statusTarefa : StatusTarefa.values()) {
            porStatus.put(statusTarefa, 0L);
        }
        tarefaRepository.contarPorStatus(projetoId).forEach(c -> porStatus.put(c.getStatus(), c.getTotal()));

        Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);
        for (Prioridade prioridadeValor : Prioridade.values()) {
            porPrioridade.put(prioridadeValor, 0L);
        }
        tarefaRepository
                .contarPorPrioridade(projetoId)
                .forEach(c -> porPrioridade.put(c.getPrioridade(), c.getTotal()));

        return new RespostaRelatorio(porStatus, porPrioridade);
    }
}
