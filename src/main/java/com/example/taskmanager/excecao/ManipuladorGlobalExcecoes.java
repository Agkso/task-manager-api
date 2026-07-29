package com.example.taskmanager.excecao;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Respostas de erro padronizadas em RFC 7807 (ProblemDetail) para toda a API.
 */
@RestControllerAdvice
public class ManipuladorGlobalExcecoes {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, "Violacao de regra de negocio", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(BadCredentialsException ex, HttpServletRequest request) {
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoInvalido(HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Requisicao invalida", "Corpo da requisicao malformado", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroGenerico(Exception ex, HttpServletRequest request) {
        return construir(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado", request);
    }

    private ProblemDetail construir(HttpStatus status, String titulo, String detalhe, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }
}
