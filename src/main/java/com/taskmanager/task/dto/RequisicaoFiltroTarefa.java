package com.taskmanager.task.dto;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros, ordenacao e paginacao da listagem de tarefas, agrupados num so
 * objeto e vinculados via {@code @ModelAttribute} - evitava uma lista de 10+
 * {@code @RequestParam} soltos no metodo do controller. O construtor
 * compacto aplica os defaults que antes viviam em {@code @RequestParam(defaultValue = ...)}.
 *
 * {@code pagina}/{@code tamanho} sao {@link Integer}, nao {@code int}: quando o
 * cliente nao manda o parametro (ex.: {@code ?status=DONE} sozinho, sem
 * paginacao nenhuma), o Spring nao tem como converter "ausente" pra um
 * primitivo (nao existe int nulo) e a requisicao inteira falha com 400 antes
 * do construtor compacto rodar. Com o tipo boxed, o parametro ausente vira
 * null - dai o construtor compacto aplica o default normalmente.
 */
public record RequisicaoFiltroTarefa(
        StatusTarefa status,
        Prioridade prioridade,
        Long responsavelId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prazoDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prazoAte,
        String busca,
        String ordenarPor,
        String direcao,
        @Min(value = 0, message = "pagina nao pode ser negativa") Integer pagina,
        @Min(value = 1, message = "tamanho deve ser no minimo 1")
                @Max(value = 100, message = "tamanho deve ser no maximo 100")
                Integer tamanho) {

    public RequisicaoFiltroTarefa {
        ordenarPor = ordenarPor == null ? "criadoEm" : ordenarPor;
        direcao = direcao == null ? "asc" : direcao;
        pagina = pagina == null ? 0 : pagina;
        tamanho = (tamanho == null || tamanho == 0) ? 20 : tamanho;
    }
}
