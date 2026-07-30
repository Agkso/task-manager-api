package com.taskmanager.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache local (Caffeine), nao distribuido - adequado pra uma instancia
 * unica da aplicacao, que e o caso aqui. TTL curto (5 min) porque o
 * relatorio precisa refletir mudancas de status em tempo razoavel; ainda
 * assim, os use cases de escrita de tarefa (criar/atualizar/excluir/mudar
 * status, em com.taskmanager.task.usecase) evitam a leitura desatualizada
 * de verdade evictando a entrada (ver @CacheEvict neles), o TTL e so uma
 * rede de seguranca pra caso alguma via de escrita futura esqueca de
 * evictar.
 */
@Configuration
@EnableCaching
public class ConfiguracaoCache {

    public static final String CACHE_RELATORIO_TAREFAS = "relatorio-tarefas";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
        return cacheManager -> {
            cacheManager.setCacheNames(List.of(CACHE_RELATORIO_TAREFAS));
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .maximumSize(1_000));
        };
    }
}
