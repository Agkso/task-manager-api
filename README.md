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
├── task        Tarefa, TarefaService (orquestra), RegrasTransicaoStatusTarefa (regra pura
│               extraida, sem dependencia de repositorio), TarefaSpecifications (filtros, busca,
│               ordenacao), HistoricoTarefa/HistoricoTarefaListener (audit log via evento)
├── common      Auditavel (criadoEm/atualizadoEm via JPA auditing) e PaginaResposta<T>
├── exception   excecoes de dominio + ManipuladorGlobalExcecoes (RFC 7807)
└── config      OpenAPI/Swagger
```

Escolhi package-by-feature em vez de package-by-layer (controller/service/repository cada um num pacote so) porque, assim que `task` cresceu, ficava dificil saber rapido o que pertencia a `project` vs `task` olhando pastas separadas por tipo. Com feature module, abrir `task/` mostra tudo que existe sobre tarefa.

`security` e `exception` ficam fora dos modulos porque sao transversais (usados por todos).

Dentro de `project` e `task` tambem apliquei SRP num segundo passo: `ProjetoService` originalmente misturava CRUD de projeto com "quem pode fazer o que" (membership/autorizacao) - virou `MembroProjetoService` separado, que `TarefaService` tambem usa pra checar acesso. Da mesma forma, as 3 regras de transicao de status saíram do `TarefaService` para `RegrasTransicaoStatusTarefa` - classe pura, sem repositorio, testavel sem mock (ver `RegrasTransicaoStatusTarefaTest`). Os filtros/busca/ordenacao da listagem saíram para `TarefaSpecifications` pelo mesmo motivo: manter `TarefaService` como orquestrador, nao como dono da logica de consulta.

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

**Filtro + busca textual no mesmo endpoint de listagem (`GET /tarefas?busca=...`), em vez de uma rota `/tarefas/busca` separada.**
O enunciado pede os dois como itens separados, mas implementar como parametro opcional do mesmo endpoint permite combinar busca com os outros filtros (status, prioridade, prazo) numa unica chamada, o que e mais util na pratica do que forcar o cliente a escolher entre filtrar OU buscar.

**Busca textual com indice GIN trigram (`pg_trgm`) em `lower(titulo)`/`lower(descricao)`.**
A consulta usa `LOWER(coluna) LIKE '%termo%'` pra ser case-insensitive; pra isso realmente usar o indice, o indice precisa ser funcional na mesma expressao (`lower(...)`), nao na coluna crua. Isso foi um bug real que cometi na primeira versao (V1 da migration indexava a coluna crua) e corrigi numa V2 - **nunca editei a V1 ja aplicada**, criei uma migration nova, que e a pratica correta com Flyway.

**Paginacao e ordenacao de verdade no banco (`Pageable` + `Specification`), inclusive ordenacao por prioridade.**
`TarefaRepository.findAll(spec, pageable)` traz so a pagina pedida - o banco faz `LIMIT`/`OFFSET` e devolve o total via uma query de `COUNT` separada (`Page<Tarefa>` cuida disso). O caso chato era ordenar por prioridade: a ordem alfabetica do enum (`CRITICAL, HIGH, LOW, MEDIUM`) nao e a ordem de severidade, e nem `Sort.by(...)` nem `JpaSort.unsafe(...)` resolvem isso via Criteria API (`JpaSort.unsafe` so funciona em query derivada por nome de metodo). A solucao ficou em `TarefaSpecifications.ordenarPorPrioridade`: monta um `CASE WHEN` direto no `CriteriaBuilder` (`LOW=1 ... CRITICAL=4`) e chama `query.orderBy(...)` dentro da propria `Specification`; `TarefaService.listar` passa `Sort.unsorted()` pro `Pageable` nesse caso especifico, pra ele nao sobrescrever a ordenacao que a `Specification` ja fixou. Pra ordenacao simples (`criadoEm`/`prazo`) uso `Sort.by(...)` normal.

`TarefaSpecifications.comResponsavelCarregado()` faz fetch join de `responsavel` (evita N+1 ao montar `RespostaTarefa`) - so e seguro porque `responsavel` e `@ManyToOne` (to-one); precisa checar `query.getResultType()` porque `findAll(spec, Pageable)` dispara uma query de `COUNT` auxiliar, e fetch join nao e permitido nela.

**Relatorio via `GROUP BY` no banco, nao contagem em memoria.**
`TarefaRepository.contarPorStatus`/`contarPorPrioridade` fazem a agregacao no Postgres. Ao contrario da listagem paginada, aqui nao ha ordenacao por enum envolvida, entao nao existe motivo pra trazer tudo pra aplicacao so pra contar.

**`@EntityGraph` em vez de abrir a sessao na view (`open-in-view: false` continua desligado).**
Sem isso, `MembroProjeto.usuario` e `Projeto.dono` (ambos `@ManyToOne(LAZY)`) estourariam `LazyInitializationException` ao montar a resposta fora da transacao - foi exatamente o bug que encontrei testando manualmente o endpoint de listar membros. Corrigi com `@EntityGraph(attributePaths = ...)` nas consultas que precisam da associação, em vez de reabrir a sessão na view (que reintroduziria N+1 silenciosos em outros lugares).

**Sem papel/role global no JWT.**
O token (`UsuarioAutenticado`) carrega so a identidade (email/id), com uma authority generica (`ROLE_USER`). Toda autorizacao de projeto (e' membro? e' ADMIN?) e resolvida em `MembroProjetoService.obterMembro`/`exigirAdmin`, consultando `MembroProjeto` a cada chamada. Reflete o modelo de dados (papel e por projeto) e evita um token que fica desatualizado se o papel do usuario mudar no meio da validade dele.

**Logging: WARN sem stack trace pra erro esperado, ERROR com stack trace so no fallback generico.**
`ManipuladorGlobalExcecoes` loga cada excecao tratada (404/403/409/etc) em WARN, so com a mensagem - stack trace ali so teria poluido o log de algo que e comportamento normal da API. O fallback de `Exception` generica loga em ERROR com stack trace completo, porque por definicao e algo que eu nao previ; sem isso, um bug de verdade em producao passaria em silencio (foi literalmente assim antes dessa mudanca).

**Historico de alteracoes (`HistoricoTarefa`) via evento, desacoplado do `TarefaService`.**
`TarefaService.mudarStatus` publica um `TarefaStatusAlteradoEvent`; quem grava o registro (quem, status anterior, status novo, quando) e' `HistoricoTarefaListener`, um `@TransactionalEventListener` separado. Duas escolhas deliberadas: `AFTER_COMMIT` (nao grava historico de uma transacao que sofreu rollback) e `REQUIRES_NEW` (a transacao original ja fechou quando o listener roda, entao ele precisa abrir a propria). O design de concentrar toda mudanca de status num unico metodo (ver acima) foi o que permitiu plugar isso como um listener sem tocar em nenhum controller.

## Testes

- **Unitarios (Mockito)**: `TarefaServiceTest` cobre as 3 regras de negocio de status + validacao de responsavel; `AutenticacaoServiceTest` cobre o caso de email duplicado. Priorizei os pontos onde um bug de logica passaria despercebido em teste manual e onde o enunciado exige explicitamente uma regra.
- **Integracao (`@SpringBootTest` + Testcontainers)**: `FluxoCriticoIntegrationTest` sobe um Postgres real (nao H2) e roda o fluxo completo registrar → login → criar projeto → criar tarefa → mudar status → conferir relatorio, alem do caso de rota protegida sem token. Usar Postgres real aqui (em vez de H2) evita que o teste passe por causa de uma diferenca de dialeto que so apareceria em producao.
- Não persegui cobertura de 100% - o enunciado explicitamente não pede isso, e testar getters/setters ou DTOs (records) não agrega nada.

## O que eu faria diferente com mais tempo

- **Refresh token**: hoje o JWT expira em 60 minutos sem renovacao; um fluxo de refresh token evitaria obrigar o usuario a logar de novo.
- **Cache no relatorio/listagem**: o relatorio é lido com frequência e muda pouco; daria pra cachear por `projetoId` com invalidação no `save`/`delete` de `Tarefa` (Spring Cache + `@CacheEvict`). Não implementei porque, sem métricas reais de uso, adicionar cache é otimização prematura.
- **Frontend**: React com um board simples (colunas por status, drag-and-drop) consumindo essa API - ficou fora por escopo/tempo, priorizei fechar os requisitos obrigatorios do backend primeiro.
- **Rate limiting no login/registro** para mitigar força bruta - ok para um teste técnico, mas eu adicionaria antes de qualquer coisa perto de produção.
- **Mais testes de integração**: hoje só o fluxo crítico tem cobertura de ponta a ponta; endpoints de projeto (membership, autorização ADMIN vs MEMBER) mereceriam o mesmo tratamento.
