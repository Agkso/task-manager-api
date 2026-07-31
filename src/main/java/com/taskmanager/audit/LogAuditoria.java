package com.taskmanager.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro imutavel de uma mutacao relevante (criar/atualizar/excluir
 * projeto, membro ou tarefa; registro e login) - nunca e atualizado depois
 * de criado, por isso nao estende Auditavel (que e' pra entidades editadas ao
 * longo do tempo) e seta o proprio criadoEm manualmente, no mesmo padrao de
 * HistoricoTarefa.
 *
 * Nao duplica o que HistoricoTarefa ja cobre (mudanca de status de tarefa,
 * com statusAnterior/statusNovo) - esse log foca no que HistoricoTarefa nao
 * ve: ciclo de vida de projeto, membership e autenticacao.
 */
@Entity
@Table(name = "log_auditoria")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(name = "tipo_entidade", nullable = false, length = 40)
    private String tipoEntidade;

    @Column(name = "entidade_id")
    private Long entidadeId;

    @Column(name = "projeto_id")
    private Long projetoId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
