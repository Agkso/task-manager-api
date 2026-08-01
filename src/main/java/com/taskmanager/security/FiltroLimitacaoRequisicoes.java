package com.taskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita tentativas de login/registro/esqueci-senha por IP (token bucket)
 * pra dificultar forca bruta de senha, criacao em massa de contas e spam de
 * email de reset (cada envio custa uma chamada real ao provedor de email) -
 * so nessas rotas, o resto da API ja exige um JWT valido, que e uma
 * barreira bem mais cara de forcar do que uma senha.
 *
 * Um bucket por IP, guardado num cache Caffeine com expiracao por
 * inatividade: sem isso, cada IP novo que aparecesse ficaria pra sempre em
 * memoria (memory leak lento). Nao e distribuido (cada instancia da app
 * teria seu proprio contador) - ok pro single instance atual; um cluster
 * real precisaria de um backend compartilhado (Redis, por ex - o Bucket4j
 * suporta isso trocando o ProxyManager, sem mudar a logica aqui).
 *
 * request.getRemoteAddr() confia no IP que chega direto na aplicacao - atras
 * de um proxy/load balancer isso seria sempre o IP do proxy, e precisaria
 * ler X-Forwarded-For (com a devida validacao de quem tem permissao de
 * setar esse header). Fora de escopo pro cenario atual (sem proxy reverso
 * na frente).
 *
 * `janelaMinutos` e' o valor de config injetado; `janela` e `buckets` sao
 * derivados dele (nao sao config nem bean), entao nao da pra injeta-los
 * direto via @RequiredArgsConstructor - @PostConstruct monta os dois uma
 * unica vez, depois que o Lombok popula os campos finais.
 */
@Component
@RequiredArgsConstructor
public class FiltroLimitacaoRequisicoes extends OncePerRequestFilter {

    private static final Set<String> ROTAS_LIMITADAS =
            Set.of("/api/auth/login", "/api/auth/registrar", "/api/auth/esqueci-senha");

    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.capacidade}")
    private final int capacidade;

    @Value("${app.rate-limit.janela-minutos}")
    private final long janelaMinutos;

    private Duration janela;
    private Cache<String, Bucket> buckets;

    @PostConstruct
    void inicializar() {
        this.janela = Duration.ofMinutes(janelaMinutos);
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(this.janela.multipliedBy(2))
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!ROTAS_LIMITADAS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.get(request.getRemoteAddr(), ip -> criarBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        responderLimiteExcedido(response, request, probe);
    }

    private Bucket criarBucket() {
        Bandwidth limite = Bandwidth.classic(capacidade, Refill.greedy(capacidade, janela));
        return Bucket.builder().addLimit(limite).build();
    }

    private void responderLimiteExcedido(HttpServletResponse response, HttpServletRequest request, ConsumptionProbe probe)
            throws IOException {
        long segundosEspera = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(segundosEspera));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas. Tente novamente em " + segundosEspera + "s");
        problema.setTitle("Limite de requisicoes excedido");
        problema.setInstance(URI.create(request.getRequestURI()));

        objectMapper.writeValue(response.getWriter(), problema);
    }
}
