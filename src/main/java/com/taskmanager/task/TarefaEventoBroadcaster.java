package com.taskmanager.task;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registro em memoria dos clientes SSE conectados por projeto, pra avisar em
 * tempo real quando uma tarefa muda de status (board com varios usuarios
 * olhando o mesmo projeto). Em memoria de proposito - so funciona com uma
 * unica instancia da API (mesma premissa do cache Caffeine em
 * ConfiguracaoCache); com mais de uma instancia atras de um load balancer,
 * precisaria de um broker externo (Redis pub/sub, etc.) pra emissores
 * conectados em instancias diferentes se enxergarem.
 */
@Slf4j
@Component
public class TarefaEventoBroadcaster {

    // cliente deve reconectar apos esse tempo (o board so precisa reabrir a conexao,
    // como qualquer EventSource ja faz sozinho em erro/timeout)
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emissoresPorProjeto = new ConcurrentHashMap<>();

    public SseEmitter inscrever(Long projetoId) {
        SseEmitter emissor = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> emissores = emissoresPorProjeto.computeIfAbsent(projetoId, id -> new CopyOnWriteArrayList<>());
        emissores.add(emissor);

        Runnable remover = () -> emissores.remove(emissor);
        emissor.onCompletion(remover);
        emissor.onTimeout(remover);
        emissor.onError(ex -> remover.run());

        return emissor;
    }

    /** Visibilidade de pacote so pra teste - nao ha um caso de uso real pra consultar isso de fora. */
    int quantidadeInscritos(Long projetoId) {
        List<SseEmitter> emissores = emissoresPorProjeto.get(projetoId);
        return emissores == null ? 0 : emissores.size();
    }

    public void publicar(Long projetoId, String nomeEvento, Object dados) {
        List<SseEmitter> emissores = emissoresPorProjeto.get(projetoId);
        if (emissores == null || emissores.isEmpty()) {
            return;
        }
        for (SseEmitter emissor : emissores) {
            try {
                emissor.send(SseEmitter.event().name(nomeEvento).data(dados));
            } catch (IOException | IllegalStateException falhaDeEnvio) {
                // cliente desconectou sem passar pelo onCompletion/onError (ex.: aba fechada
                // abruptamente) - so limpa o registro, nao ha nada a recuperar aqui
                log.debug("Removendo emissor SSE morto do projeto {}: {}", projetoId, falhaDeEnvio.getMessage());
                emissor.completeWithError(falhaDeEnvio);
                emissores.remove(emissor);
            }
        }
    }
}
