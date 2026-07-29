package com.taskmanager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ConfiguracaoSeguranca {

    private static final String[] ROTAS_PUBLICAS = {
        "/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
    };

    private final FiltroAutenticacaoJwt filtroAutenticacaoJwt;
    private final UsuarioDetailsService usuarioDetailsService;
    private final PontoEntradaNaoAutenticado pontoEntradaNaoAutenticado;

    public ConfiguracaoSeguranca(
            FiltroAutenticacaoJwt filtroAutenticacaoJwt,
            UsuarioDetailsService usuarioDetailsService,
            PontoEntradaNaoAutenticado pontoEntradaNaoAutenticado) {
        this.filtroAutenticacaoJwt = filtroAutenticacaoJwt;
        this.usuarioDetailsService = usuarioDetailsService;
        this.pontoEntradaNaoAutenticado = pontoEntradaNaoAutenticado;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handler -> handler.authenticationEntryPoint(pontoEntradaNaoAutenticado))
                .addFilterBefore(filtroAutenticacaoJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
