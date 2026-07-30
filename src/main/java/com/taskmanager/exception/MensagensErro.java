package com.taskmanager.exception;

/**
 * Ponto unico dos textos usados pelas excecoes de negocio, autorizacao e
 * autenticacao da aplicacao. Antes da extracao, mensagens como "Projeto nao
 * encontrado: " apareciam repetidas em services diferentes, cada uma podendo
 * divergir com o tempo; centralizar aqui deixa a redacao (e uma eventual
 * traducao futura) num lugar so.
 */
public final class MensagensErro {

    private MensagensErro() {}

    // Projeto
    public static String projetoNaoEncontrado(Long projetoId) {
        return "Projeto nao encontrado: " + projetoId;
    }

    public static final String APENAS_DONO_PODE_EXCLUIR_PROJETO = "Apenas o dono do projeto pode exclui-lo";

    // Membro / autorizacao de projeto
    public static final String NAO_E_MEMBRO_DO_PROJETO = "Voce nao e membro deste projeto";
    public static final String APENAS_ADMIN_PODE_REALIZAR_ACAO = "Apenas o ADMIN do projeto pode realizar esta acao";
    public static final String USUARIO_JA_E_MEMBRO = "Usuario ja e membro deste projeto";
    public static final String DONO_NAO_PODE_SER_REMOVIDO_DOS_MEMBROS =
            "O dono do projeto nao pode ser removido dos membros";

    public static String usuarioNaoEncontradoPorEmail(String email) {
        return "Usuario nao encontrado com o email: " + email;
    }

    // Tarefa
    public static String tarefaNaoEncontrada(Long tarefaId) {
        return "Tarefa nao encontrada: " + tarefaId;
    }

    public static final String RESPONSAVEL_NAO_E_MEMBRO_DO_PROJETO =
            "O responsavel informado nao e membro deste projeto";

    public static String usuarioNaoEncontrado(Long usuarioId) {
        return "Usuario nao encontrado: " + usuarioId;
    }

    // Transicao de status de tarefa
    public static final String TRANSICAO_DONE_PARA_TODO_PROIBIDA =
            "Uma tarefa concluida (DONE) nao pode voltar para TODO, apenas para IN_PROGRESS";
    public static final String APENAS_ADMIN_CONCLUI_TAREFA_CRITICAL =
            "Apenas o ADMIN do projeto pode concluir uma tarefa de prioridade CRITICAL";

    public static String limiteDeTarefasEmAndamentoAtingido(int limite) {
        return "Limite de " + limite + " tarefas em andamento (IN_PROGRESS) atingido para este responsavel";
    }

    // Autenticacao e usuario
    public static final String EMAIL_JA_CADASTRADO = "Ja existe um usuario cadastrado com esse email";
    public static final String REFRESH_TOKEN_INVALIDO = "Refresh token invalido";

    public static String usuarioAutenticadoNaoEncontrado(String email) {
        return "Usuario autenticado nao encontrado: " + email;
    }

    public static String usuarioNaoEncontrado(String email) {
        return "Usuario nao encontrado: " + email;
    }

    public static String algoritmoHashIndisponivel(String algoritmo) {
        return "Algoritmo de hash indisponivel: " + algoritmo;
    }
}
