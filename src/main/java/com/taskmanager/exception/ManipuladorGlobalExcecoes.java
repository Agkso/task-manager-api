package com.taskmanager.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Respostas de erro padronizadas em RFC 7807 (ProblemDetail) para toda a API.
 *
 * Politica de log: WARN pra erro esperado/causado pelo cliente (400/401/403/
 *404/409) - registra o que aconteceu sem stack trace, que so poluiria o log.
 * ERROR com stack trace so pro fallback generico (500), que por definicao e
 * algo que eu nao previ - sem isso, um bug em producao seria invisivel.
 */
@Slf4j
@RestControllerAdvice
public class ManipuladorGlobalExcecoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        log.warn("Recurso nao encontrado em {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        log.warn("Regra de negocio violada em {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.CONFLICT, "Violacao de regra de negocio", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado em {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Credenciais invalidas", "Email ou senha incorretos", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problema = construir(
                HttpStatus.BAD_REQUEST, "Erro de validacao", "Um ou mais campos sao invalidos", request);
        problema.setProperty(
                "erros",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                        .toList());
        return problema;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail tratarViolacaoDeConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problema = construir(
                HttpStatus.BAD_REQUEST, "Erro de validacao", "Um ou mais parametros sao invalidos", request);
        problema.setProperty(
                "erros",
                ex.getConstraintViolations().stream()
                        .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                        .toList());
        return problema;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detalhe = "Valor invalido para o parametro '%s': %s".formatted(ex.getName(), ex.getValue());
        return construir(HttpStatus.BAD_REQUEST, "Parametro invalido", detalhe, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail tratarMetodoNaoSuportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao suportado", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoInvalido(HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Requisicao invalida", "Corpo da requisicao malformado", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarViolacaoDeIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        // ex: corrida entre duas requisicoes concorrentes tentando o mesmo email/membro
        // unico - as checagens de aplicacao (existsBy...) nao cobrem essa janela de tempo,
        // quem garante de fato e a constraint do banco. Detalhe do SQL nao vaza pro cliente.
        log.warn("Violacao de integridade em {}: {}", request.getRequestURI(), ex.getMessage());
        return construir(
                HttpStatus.CONFLICT, "Conflito de dados", "O registro conflita com dados ja existentes", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroGenerico(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado ao processar {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado", request);
    }

    private ProblemDetail construir(HttpStatus status, String titulo, String detalhe, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }
}
