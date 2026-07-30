package com.taskmanager.task;

import java.util.Comparator;

/**
 * Regra pura (sem dependencia de repositorio), extraida do TarefaService
 * pra ficar facil de testar e de trocar isoladamente.
 *
 * Ordenacao por prioridade usa o ordinal do enum (LOW=0 ... CRITICAL=3), que
 * so da a ordem certa de severidade porque Prioridade foi declarado nessa
 * ordem crescente - se a ordem de declaracao do enum mudar, isso quebra
 * silenciosamente. Uma alternativa mais robusta seria um CASE WHEN no
 * banco (ver README, secao de decisoes).
 */
public final class TarefaOrdenador {

    private TarefaOrdenador() {}

    public static Comparator<Tarefa> comparador(String ordenarPor, String direcao) {
        Comparator<Tarefa> comparador =
                switch (ordenarPor == null ? "" : ordenarPor) {
                    case "prioridade" -> Comparator.comparingInt(t -> t.getPrioridade().ordinal());
                    case "prazo" -> Comparator.comparing(
                            Tarefa::getPrazo, Comparator.nullsLast(Comparator.naturalOrder()));
                    default -> Comparator.comparing(Tarefa::getCriadoEm);
                };

        return "desc".equalsIgnoreCase(direcao) ? comparador.reversed() : comparador;
    }
}
