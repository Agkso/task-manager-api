package com.taskmanager.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extrairToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extrairEmail(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);
                if (jwtService.tokenValido(token, usuario.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException tokenInvalido) {
            // token invalido/expirado ou usuario removido: segue sem autenticar,
            // o PontoEntradaNaoAutenticado se encarrega de barrar la na frente.
            // DEBUG (nao WARN) porque token expirado e rotina, nao anomalia -
            // nunca logar o token em si, so o motivo.
            log.debug("Token JWT rejeitado em {}: {}", request.getRequestURI(), tokenInvalido.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization: Bearer e o caminho normal. O fallback via query param
     * (?token=) existe so pro endpoint de eventos SSE (ver
     * InscreverEventosTarefaUseCase): a API nativa EventSource do browser
     * nao deixa mandar headers customizados, entao nao ha como anexar
     * Authorization numa conexao SSE feita direto pelo browser sem isso.
     * O token e' de curta duracao (60min por padrao) e HTTPS em producao
     * cobre a exposicao na URL - o mesmo tradeoff que qualquer API que
     * expoe SSE/websocket autenticado por token precisa fazer.
     */
    private String extrairToken(HttpServletRequest request) {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith(PREFIXO_BEARER)) {
            return cabecalho.substring(PREFIXO_BEARER.length());
        }
        return request.getParameter("token");
    }
}
