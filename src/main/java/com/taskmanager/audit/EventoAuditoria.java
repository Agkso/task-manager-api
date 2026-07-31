package com.taskmanager.audit;

/**
 * Evento generico de auditoria, publicado no ponto da mutacao (ProjetoService,
 * MembroProjetoService, use cases de tarefa e de autenticacao) e persistido
 * de forma assincrona por {@link AuditoriaListener}. Um so tipo de evento
 * (em vez de um por acao, ex.: ProjetoCriadoEvent, ProjetoExcluidoEvent...)
 * de proposito: com ~10 pontos de publicacao, criar uma classe de evento por
 * acao seria muito boilerplate pra pouco ganho de tipagem - "acao" e
 * "tipoEntidade" como String cobrem o mesmo caso de uso com uma fracao do
 * codigo. Se o numero de campos variar muito por acao no futuro, essa e a
 * proxima refatoracao natural.
 *
 * {@code projetoId} e nulo pra eventos que nao pertencem a um projeto
 * (registro de usuario, login).
 */
public record EventoAuditoria(
        String acao, String tipoEntidade, Long entidadeId, Long projetoId, Long usuarioId, String detalhe) {

    public static EventoAuditoria de(
            AcaoAuditoria acao, TipoEntidadeAuditoria tipoEntidade, Long entidadeId, Long projetoId, Long usuarioId) {
        return new EventoAuditoria(acao.name(), tipoEntidade.name(), entidadeId, projetoId, usuarioId, null);
    }

    public static EventoAuditoria de(
            AcaoAuditoria acao,
            TipoEntidadeAuditoria tipoEntidade,
            Long entidadeId,
            Long projetoId,
            Long usuarioId,
            String detalhe) {
        return new EventoAuditoria(acao.name(), tipoEntidade.name(), entidadeId, projetoId, usuarioId, detalhe);
    }
}
