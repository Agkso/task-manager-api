package com.taskmanager.project.projeto;

import com.taskmanager.common.Auditavel;
import com.taskmanager.user.Usuario;
import com.taskmanager.project.membro.MembroProjeto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Espaco de trabalho que agrupa tarefas e membros. O dono e sempre criado
 * como membro ADMIN (ver MembroProjetoService.criarComoAdmin) e nao pode
 * ser removido da lista de membros.
 */
@Entity
@Table(name = "projetos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 2000)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dono_id", nullable = false)
    private Usuario dono;

    @Builder.Default
    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MembroProjeto> membros = new HashSet<>();

    /** Nulo = ativo. Setado no lugar de um DELETE fisico - ver ProjetoService.excluir. */
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Projeto projeto)) {
            return false;
        }
        return id != null && id.equals(projeto.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
