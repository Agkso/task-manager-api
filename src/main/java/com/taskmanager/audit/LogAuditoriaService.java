package com.taskmanager.audit;

import com.taskmanager.audit.dto.RespostaLogAuditoria;
import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.project.MembroProjetoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** So o ADMIN do projeto consulta a auditoria dele - restricao igual a de MembroProjetoService.exigirAdmin. */
@Service
@RequiredArgsConstructor
public class LogAuditoriaService {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final MembroProjetoService membroProjetoService;
    private final LogAuditoriaMapper logAuditoriaMapper;

    @Transactional(readOnly = true)
    public PaginaResposta<RespostaLogAuditoria> listar(Long projetoId, Long solicitanteId, int pagina, int tamanho) {
        membroProjetoService.exigirAdmin(projetoId, solicitanteId);

        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = tamanho <= 0 ? TAMANHO_PAGINA_PADRAO : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);

        Page<LogAuditoria> paginaLogs =
                logAuditoriaRepository.findByProjetoIdOrderByCriadoEmDesc(projetoId, PageRequest.of(paginaSegura, tamanhoSeguro));
        List<RespostaLogAuditoria> conteudo =
                paginaLogs.getContent().stream().map(logAuditoriaMapper::paraResposta).toList();

        return new PaginaResposta<>(
                conteudo, paginaLogs.getNumber(), paginaLogs.getTotalPages(), paginaLogs.getTotalElements());
    }
}
