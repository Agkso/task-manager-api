package com.taskmanager.common.dto;

import java.util.List;

public record PaginaResposta<T>(List<T> conteudo, int paginaAtual, int totalPaginas, long totalElementos) {}
