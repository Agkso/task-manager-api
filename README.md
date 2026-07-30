# Task Manager API

Sistema simplificado de gerenciamento de tarefas para equipes de desenvolvimento. Backend em Spring Boot com autenticacao JWT, projetos com membros, tarefas com regras de negocio, filtros/busca/relatorio e documentacao via Swagger.

## Stack

- Java 17 (roda em 21 tambem) + Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL + Flyway (migrations versionadas)
- JWT (jjwt)
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito + Testcontainers

## Como rodar

Pre-requisitos: Java 17+, Docker (pra subir o Postgres local).

```bash
docker compose up -d        # sobe o Postgres em localhost:5433
./mvnw spring-boot:run       # aplica as migrations e sobe a API em localhost:8080
```

Swagger UI: http://localhost:8080/swagger-ui.html
Console nao existe pra Postgres (era do H2); pra inspecionar o banco use qualquer client (DBeaver, psql) apontando pra `localhost:5433`, banco `taskmanager`, usuario/senha `taskmanager`.

Rodar os testes (sobe um Postgres descartavel via Testcontainers, precisa do Docker rodando):

```bash
./mvnw test
```

### Fluxo minimo pra testar na mao

```bash
# registrar (retorna token)
curl -X POST localhost:8080/api/auth/registrar -H "Content-Type: application/json" \
  -d '{"nome":"Ana","email":"ana@ex.com","senha":"senha1234"}'

# criar projeto (usa o token do passo anterior)
curl -X POST localhost:8080/api/projetos -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" -d '{"nome":"Projeto X","descricao":"..."}'
```

O restante dos endpoints (membros, tarefas, filtros, relatorio) esta documentado no Swagger.

## Arquitetura

Pacotes organizados por feature (nao por camada) - cada modulo carrega sua entidade, repositorio, service, controller e DTOs:

```
com.taskmanager
├── user        Usuario (entidade central, sem papel embutido)
├── auth        registro/login, emissao de token
├── security    JWT (filtro, service, UserDetails), config do Spring Security
├── project     Projeto, MembroProjeto (o papel ADMIN/MEMBER mora aqui, nao em Usuario)
│               ProjetoService (CRUD) e MembroProjetoService (membership/autorizacao) separados
├── task        Tarefa, TarefaService (orquestra), RegrasTransicaoStatusTarefa e TarefaOrdenador
│               (regras puras extraidas, sem dependencia de repositorio), filtros, busca, relatorio
├── common      Auditavel (criadoEm/atualizadoEm via JPA auditing) e PaginaResposta<T>
├── exception   excecoes de dominio + ManipuladorGlobalExcecoes (RFC 7807)
└── config      OpenAPI/Swagger
```

Escolhi package-by-feature em vez de package-by-layer (controller/service/repository cada um num pacote so) porque, assim que `task` cresceu, ficava dificil saber rapido o que pertencia a `project` vs `task` olhando pastas separadas por tipo. Com feature module, abrir `task/` mostra tudo que existe sobre tarefa.

`security` e `exception` ficam fora dos modulos porque sao transversais (usados por todos).

Dentro de `project` e `task` tambem apliquei SRP num segundo passo: `ProjetoService` originalmente misturava CRUD de projeto com "quem pode fazer o que" (membership/autorizacao) - virou `MembroProjetoService` separado, que `TarefaService` tambem usa pra checar acesso. Da mesma forma, as 3 regras de transicao de status e a logica de ordenacao saíram do `TarefaService` para `RegrasTransicaoStatusTarefa`/`TarefaOrdenador` - classes puras, sem repositorio, testaveis sem mock (ver `RegrasTransicaoStatusTarefaTest`).

## Decisoes tecnicas e tradeoffs

**Papel (ADMIN/MEMBER) vive em `MembroProjeto`, nao em `Usuario`.**
O enunciado pede dois perfis, mas fechar uma tarefa CRITICAL exige ser "ADMIN do projeto" - ou seja, e uma permissao por projeto, nao global. Um usuario pode ser ADMIN no projeto A e MEMBER no projeto B. Modelar como campo em `Usuario` teria sido mais simples de ler, mas incorreto: nao daria pra expressar "ADMIN so nesse projeto".

**Toda mudanca de status passa por um unico metodo (`TarefaService.mudarStatus`).**
POST/PUT de tarefa nao aceitam `status` no corpo - toda tarefa nasce `TODO`. As 3 regras do enunciado (DONE nao volta pra TODO, CRITICAL so fecha por ADMIN, limite de 5 IN_PROGRESS por responsavel) ficam concentradas nesse metodo em vez de espalhadas. Motivo: se o cliente pudesse setar status livremente num PUT generico, teria que replicar as mesmas validacoes em dois lugares (criar/atualizar e a transicao dedicada) ou arriscar burlar as regras por um caminho alternativo.

**Postgres + Flyway em vez de H2.**
O desafio permite H2 "pra simplificar". Comecei exatamente assim, mas troquei pra Postgres com Flyway porque queria migrations reais (versionadas, auditáveis) e evitar qualquer diferenca de dialeto entre o que roda aqui e o que rodaria em producao. O custo e exigir Docker pra rodar - contrabalanceado com `spring-boot-docker-compose`, que sobe o `compose.yaml` sozinho ao iniciar a aplicacao.

**IDs `Long`/`BIGSERIAL`, nao UUID.**
Mais simples, indice menor, sequencial. UUID valeria a pena se os IDs fossem expostos com risco de enumeracao (IDOR) em um contexto mais sensivel, ou se precisasse gerar ID no client antes de persistir - nenhum dos dois se aplica aqui.

**Enums com valores em ingles (`ADMIN`/`MEMBER`, `TODO`/`IN_PROGRESS`/`DONE`, `LOW`/`MEDIUM`/`HIGH`/`CRITICAL`), mas nomes de classe/campo em portugues.**
O enunciado (em portugues) define esses valores literalmente, inclusive no exemplo de JSON do relatorio (`"byStatus": {"TODO": 12, ...}`). Traduzir os valores quebraria esse contrato sem necessidade. Por isso os campos do DTO do relatorio tambem se chamam `byStatus`/`byPriority` (nao `porStatus`/`porPrioridade`) - seguem o exemplo a risca.

**Ordenacao por prioridade usa `Prioridade.ordinal()`.**
`Prioridade` foi declarado na ordem `LOW, MEDIUM, HIGH, CRITICAL`, entao o ordinal (0..3) ja da a ordem certa de severidade sem precisar de um mapa de pesos. Documentei no codigo (`TarefaOrdenador.comparador`) que isso depende da ordem de declaracao - se alguem reordenar o enum sem perceber essa dependencia, a ordenacao quebra silenciosamente. Uma alternativa mais robusta seria uma expressao `CASE WHEN` no banco; optei pela solucao mais simples dado o volume de dados esperado no desafio (ver "o que eu faria diferente").

**Filtro + busca textual no mesmo endpoint de listagem (`GET /tarefas?busca=...`), em vez de uma rota `/tarefas/busca` separada.**
O enunciado pede os dois como itens separados, mas implementar como parametro opcional do mesmo endpoint permite combinar busca com os outros filtros (status, prioridade, prazo) numa unica chamada, o que e mais util na pratica do que forcar o cliente a escolher entre filtrar OU buscar.

**Busca textual com indice GIN trigram (`pg_trgm`) em `lower(titulo)`/`lower(descricao)`.**
A consulta usa `LOWER(coluna) LIKE '%termo%'` pra ser case-insensitive; pra isso realmente usar o indice, o indice precisa ser funcional na mesma expressao (`lower(...)`), nao na coluna crua. Isso foi um bug real que cometi na primeira versao (V1 da migration indexava a coluna crua) e corrigi numa V2 - **nunca editei a V1 ja aplicada**, criei uma migration nova, que e a pratica correta com Flyway.

**Paginacao calculada em memoria (busca tudo via Specification, ordena, faz `subList`), nao com `Pageable` do Spring Data.**
Simplificação deliberada: ordenar por prioridade exige uma logica (ordinal do enum) que o `Sort` do Spring Data nao expressa nativamente sem uma expressao SQL customizada. Pra nao misturar "ordenacao no banco pra 2 campos" com "ordenacao em memoria pra 1 campo", padronizei tudo em memoria. Funciona bem pro volume esperado de um desafio; nao escalaria pra um projeto com dezenas de milhares de tarefas (ver abaixo).

**Relatorio via `GROUP BY` no banco, nao contagem em memoria.**
`TarefaRepository.contarPorStatus`/`contarPorPrioridade` fazem a agregacao no Postgres. Ao contrario da listagem paginada, aqui nao ha ordenacao por enum envolvida, entao nao existe motivo pra trazer tudo pra aplicacao so pra contar.

**`@EntityGraph` em vez de abrir a sessao na view (`open-in-view: false` continua desligado).**
Sem isso, `MembroProjeto.usuario` e `Projeto.dono` (ambos `@ManyToOne(LAZY)`) estourariam `LazyInitializationException` ao montar a resposta fora da transacao - foi exatamente o bug que encontrei testando manualmente o endpoint de listar membros. Corrigi com `@EntityGraph(attributePaths = ...)` nas consultas que precisam da associação, em vez de reabrir a sessão na view (que reintroduziria N+1 silenciosos em outros lugares).

**Sem papel/role global no JWT.**
O token (`UsuarioAutenticado`) carrega so a identidade (email/id), com uma authority generica (`ROLE_USER`). Toda autorizacao de projeto (e' membro? e' ADMIN?) e resolvida em `MembroProjetoService.obterMembro`/`exigirAdmin`, consultando `MembroProjeto` a cada chamada. Reflete o modelo de dados (papel e por projeto) e evita um token que fica desatualizado se o papel do usuario mudar no meio da validade dele.

**Logging: WARN sem stack trace pra erro esperado, ERROR com stack trace so no fallback generico.**
`ManipuladorGlobalExcecoes` loga cada excecao tratada (404/403/409/etc) em WARN, so com a mensagem - stack trace ali so teria poluido o log de algo que e comportamento normal da API. O fallback de `Exception` generica loga em ERROR com stack trace completo, porque por definicao e algo que eu nao previ; sem isso, um bug de verdade em producao passaria em silencio (foi literalmente assim antes dessa mudanca).

## Testes

- **Unitarios (Mockito)**: `TarefaServiceTest` cobre as 3 regras de negocio de status + validacao de responsavel; `AutenticacaoServiceTest` cobre o caso de email duplicado. Priorizei os pontos onde um bug de logica passaria despercebido em teste manual e onde o enunciado exige explicitamente uma regra.
- **Integracao (`@SpringBootTest` + Testcontainers)**: `FluxoCriticoIntegrationTest` sobe um Postgres real (nao H2) e roda o fluxo completo registrar → login → criar projeto → criar tarefa → mudar status → conferir relatorio, alem do caso de rota protegida sem token. Usar Postgres real aqui (em vez de H2) evita que o teste passe por causa de uma diferenca de dialeto que so apareceria em producao.
- Não persegui cobertura de 100% - o enunciado explicitamente não pede isso, e testar getters/setters ou DTOs (records) não agrega nada.

## O que eu faria diferente com mais tempo

- **Ordenacao e paginacao no banco**: trocar a ordenacao em memoria por uma `Specification`/`CriteriaBuilder` com `CASE WHEN` para prioridade, permitindo usar `Pageable` de verdade (o banco so devolve a pagina pedida, nao a tabela inteira).
- **Historico de alteracoes (audit log)**: um `TarefaHistorico` gravado a cada mudanca de status/campos relevantes (quem, o que, quando) - ficou de fora por tempo, mas o design atual (tudo passando por `TarefaService.mudarStatus`) ja facilita adicionar isso como um listener/evento sem tocar nos controllers.
- **Refresh token**: hoje o JWT expira em 60 minutos sem renovacao; um fluxo de refresh token evitaria obrigar o usuario a logar de novo.
- **Cache no relatorio/listagem**: o relatorio é lido com frequência e muda pouco; daria pra cachear por `projetoId` com invalidação no `save`/`delete` de `Tarefa` (Spring Cache + `@CacheEvict`). Não implementei porque, sem métricas reais de uso, adicionar cache é otimização prematura.
- **Frontend**: React com um board simples (colunas por status, drag-and-drop) consumindo essa API - ficou fora por escopo/tempo, priorizei fechar os requisitos obrigatorios do backend primeiro.
- **Rate limiting no login/registro** para mitigar força bruta - ok para um teste técnico, mas eu adicionaria antes de qualquer coisa perto de produção.
- **Mais testes de integração**: hoje só o fluxo crítico tem cobertura de ponta a ponta; endpoints de projeto (membership, autorização ADMIN vs MEMBER) mereceriam o mesmo tratamento.
