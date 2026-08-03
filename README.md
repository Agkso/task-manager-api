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

Alternativa sem Java/Maven instalado - stack inteira (Postgres + API) containerizada:

```bash
docker compose -f compose.full.yaml up --build   # API em localhost:8080
```

(`compose.yaml`, sem sufixo, continua sendo so o Postgres - e o arquivo que o `spring-boot-docker-compose` sobe sozinho quando voce roda `./mvnw spring-boot:run` direto na maquina.)

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

Pacotes organizados por feature (nao por camada) com subpacotes internos por subdominio — **Package by Feature** com granularidade de Modular Monolith:

```
com.taskmanager
├── user/           Usuario (entidade central, sem papel embutido)
├── auth/
│   ├── token/      RefreshToken, RefreshTokenRepository, RefreshTokenService,
│   │               LimpezaRefreshTokenJob (rotacao de refresh token em uso unico)
│   ├── passwordreset/  PasswordResetToken, PasswordResetTokenRepository,
│   │               PasswordResetTokenService, LimpezaPasswordResetTokenJob
│   ├── usecase/    RegistrarUsuarioUseCase, AutenticarUsuarioUseCase, RenovarTokenUseCase,
│   │               LogoutUseCase, SolicitarRedefinicaoSenhaUseCase, RedefinirSenhaUseCase
│   ├── dto/        RequisicaoLogin, RequisicaoRegistro, RespostaLogin, ...
│   └── AutenticacaoController
├── security/       JWT (filtro, service, UserDetails), config do Spring Security, limitacao de
│                   tentativas de login/registro por IP
├── project/
│   ├── projeto/    Projeto, ProjetoController, ProjetoMapper, ProjetoRepository, ProjetoService
│   ├── membro/     MembroProjeto (o papel ADMIN/MEMBER mora aqui, nao em Usuario),
│   │               MembroMapper, MembroProjetoRepository, MembroProjetoService
│   ├── dto/        RequisicaoProjeto, RequisicaoAdicionarMembro, RespostaProjeto, RespostaMembro
│   └── enums/      Papel (ADMIN, MEMBER)
├── task/
│   ├── tarefa/     Tarefa, TarefaController, TarefaMapper, TarefaRepository, TarefaHelper,
│   │               TarefaSpecifications, RegrasTransicaoStatusTarefa
│   ├── historico/  HistoricoTarefa, HistoricoTarefaListener, HistoricoTarefaMapper,
│   │               HistoricoTarefaRepository
│   ├── evento/     TarefaStatusAlteradoEvent, TarefaEventoBroadcaster,
│   │               TarefaEventoSseListener (board em tempo real via SSE)
│   ├── relatorio/  RelatorioTarefaService (agregacao cacheada)
│   ├── usecase/    CriarTarefaUseCase, AtualizarTarefaUseCase, ExcluirTarefaUseCase,
│   │               MudarStatusTarefaUseCase, ListarTarefasUseCase, BuscarTarefaUseCase,
│   │               GerarRelatorioTarefaUseCase, BuscarHistoricoTarefaUseCase,
│   │               InscreverEventosTarefaUseCase
│   ├── dto/        RequisicaoTarefa, RequisicaoFiltroTarefa, RespostaTarefa, ...
│   └── enums/      StatusTarefa (TODO, IN_PROGRESS, DONE), Prioridade (LOW/MEDIUM/HIGH/CRITICAL)
├── audit/          LogAuditoria + AuditoriaListener (ciclo de vida de projeto/membro/tarefa/auth,
│                   via o mesmo padrao evento+listener do historico de tarefa)
├── common/         Auditavel (criadoEm/atualizadoEm via JPA auditing) e PaginaResposta<T>
├── exception/      excecoes de dominio + MensagensErro (textos de erro centralizados) +
│                   ManipuladorGlobalExcecoes (RFC 7807)
└── config/         OpenAPI/Swagger, cache (Caffeine)
```

**Regra de dependencia entre features**: `task` depende de `project` (via `MembroProjetoService`), nunca o contrario. `audit`, `security` e `exception` sao transversais (usados por todos). Imports cruzados diretos entre subpacotes de features diferentes sao aceitos — o limite e nao inverter a direcao da dependencia.

Escolhi package-by-feature em vez de package-by-layer (controller/service/repository cada um num pacote so) porque, assim que `task` cresceu, ficava dificil saber rapido o que pertencia a `project` vs `task` olhando pastas separadas por tipo. Com feature module, abrir `task/` mostra tudo que existe sobre tarefa. Os subpacotes internos (`tarefa/`, `historico/`, `evento/`, `relatorio/`) seguem o mesmo principio: cada subdominio agrupa suas proprias classes em vez de misturar entidade + listener + broadcaster + regras num pacote so.

`security` e `exception` ficam fora dos modulos porque sao transversais (usados por todos).

Dentro de `project` e `task` tambem apliquei SRP em mais de um passo. Primeiro, `ProjetoService` originalmente misturava CRUD de projeto com "quem pode fazer o que" (membership/autorizacao) - virou `MembroProjetoService` separado (hoje em `project/membro/`), usado tambem pelos use cases de tarefa pra checar acesso. Depois, `TarefaService`/`AutenticacaoService` (que ainda concentravam varias operacoes - criar, atualizar, excluir, mudar status, listar, relatorio, historico - num service so, com injecao de dependencia escrita na mao) foram quebrados numa classe por operacao em `task/usecase/`/`auth/usecase/`, injetadas direto no controller: fica explicito o que cada endpoint realmente precisa, sem um service "guarda-chuva" no meio do caminho. As 3 regras de transicao de status ja tinham saido do `TarefaService` para `RegrasTransicaoStatusTarefa` (hoje em `task/tarefa/`) - classe pura, sem repositorio, testavel sem mock (ver `RegrasTransicaoStatusTarefaTest`); os filtros/busca/ordenacao da listagem, para `TarefaSpecifications`.

A conversao entidade -> DTO, que vivia como metodo estatico `de()` dentro de cada record de resposta, virou um `@Component` dedicado (`TarefaMapper`, `HistoricoTarefaMapper`, `ProjetoMapper`, `MembroMapper`) injetado no use case/controller - deixa o DTO como dado puro e a conversao testavel/substituivel via DI, no mesmo espirito da extracao dos use cases.

## Decisoes tecnicas e tradeoffs

**Papel (ADMIN/MEMBER) vive em `MembroProjeto`, nao em `Usuario`.**
O enunciado pede dois perfis, mas fechar uma tarefa CRITICAL exige ser "ADMIN do projeto" - ou seja, e uma permissao por projeto, nao global. Um usuario pode ser ADMIN no projeto A e MEMBER no projeto B. Modelar como campo em `Usuario` teria sido mais simples de ler, mas incorreto: nao daria pra expressar "ADMIN so nesse projeto".

**Toda mudanca de status passa por um unico metodo (`MudarStatusTarefaUseCase.executar`).**
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
`TarefaRepository.findAll(spec, pageable)` traz so a pagina pedida - o banco faz `LIMIT`/`OFFSET` e devolve o total via uma query de `COUNT` separada (`Page<Tarefa>` cuida disso). O caso chato era ordenar por prioridade: a ordem alfabetica do enum (`CRITICAL, HIGH, LOW, MEDIUM`) nao e a ordem de severidade, e nem `Sort.by(...)` nem `JpaSort.unsafe(...)` resolvem isso via Criteria API (`JpaSort.unsafe` so funciona em query derivada por nome de metodo). A solucao ficou em `TarefaSpecifications.ordenarPorPrioridade`: monta um `CASE WHEN` direto no `CriteriaBuilder` (`LOW=1 ... CRITICAL=4`) e chama `query.orderBy(...)` dentro da propria `Specification`; `ListarTarefasUseCase.executar` passa `Sort.unsorted()` pro `Pageable` nesse caso especifico, pra ele nao sobrescrever a ordenacao que a `Specification` ja fixou. Pra ordenacao simples (`criadoEm`/`prazo`) uso `Sort.by(...)` normal.

`TarefaSpecifications.comResponsavelCarregado()` faz fetch join de `responsavel` (evita N+1 ao montar `RespostaTarefa`) - so e seguro porque `responsavel` e `@ManyToOne` (to-one); precisa checar `query.getResultType()` porque `findAll(spec, Pageable)` dispara uma query de `COUNT` auxiliar, e fetch join nao e permitido nela.

**Relatorio via `GROUP BY` no banco, nao contagem em memoria.**
`TarefaRepository.contarPorStatus`/`contarPorPrioridade` fazem a agregacao no Postgres. Ao contrario da listagem paginada, aqui nao ha ordenacao por enum envolvida, entao nao existe motivo pra trazer tudo pra aplicacao so pra contar.

**`@EntityGraph` em vez de abrir a sessao na view (`open-in-view: false` continua desligado).**
Sem isso, `MembroProjeto.usuario` e `Projeto.dono` (ambos `@ManyToOne(LAZY)`) estourariam `LazyInitializationException` ao montar a resposta fora da transacao - foi exatamente o bug que encontrei testando manualmente o endpoint de listar membros. Corrigi com `@EntityGraph(attributePaths = ...)` nas consultas que precisam da associação, em vez de reabrir a sessão na view (que reintroduziria N+1 silenciosos em outros lugares).

**Sem papel/role global no JWT.**
O token (`UsuarioAutenticado`) carrega so a identidade (email/id), com uma authority generica (`ROLE_USER`). Toda autorizacao de projeto (e' membro? e' ADMIN?) e resolvida em `MembroProjetoService.obterMembro`/`exigirAdmin`, consultando `MembroProjeto` a cada chamada. Reflete o modelo de dados (papel e por projeto) e evita um token que fica desatualizado se o papel do usuario mudar no meio da validade dele.

**Logging: WARN sem stack trace pra erro esperado, ERROR com stack trace so no fallback generico.**
`ManipuladorGlobalExcecoes` loga cada excecao tratada (404/403/409/etc) em WARN, so com a mensagem - stack trace ali so teria poluido o log de algo que e comportamento normal da API. O fallback de `Exception` generica loga em ERROR com stack trace completo, porque por definicao e algo que eu nao previ; sem isso, um bug de verdade em producao passaria em silencio (foi literalmente assim antes dessa mudanca).

**Historico de alteracoes (`HistoricoTarefa`) via evento, desacoplado de `MudarStatusTarefaUseCase`.**
`MudarStatusTarefaUseCase.executar` publica um `TarefaStatusAlteradoEvent`; quem grava o registro (quem, status anterior, status novo, quando) e' `HistoricoTarefaListener`, um `@TransactionalEventListener` separado. Duas escolhas deliberadas: `AFTER_COMMIT` (nao grava historico de uma transacao que sofreu rollback) e `REQUIRES_NEW` (a transacao original ja fechou quando o listener roda, entao ele precisa abrir a propria). O design de concentrar toda mudanca de status num unico metodo (ver acima) foi o que permitiu plugar isso como um listener sem tocar em nenhum controller.

**Mensagens de erro centralizadas em `MensagensErro`.**
Textos como `"Projeto nao encontrado: "` ou `"Voce nao e membro deste projeto"` eram literais repetidos em varios services (cada excecao lancada com sua propria string). Uma classe `MensagensErro` com constantes e metodos estaticos parametrizados (`tarefaNaoEncontrada(id)`, `limiteDeTarefasEmAndamentoAtingido(limite)`) virou o unico lugar que escreve esses textos - evita divergencia entre copias e facilita achar/trocar uma mensagem sem grep em N arquivos.

**Filtros de listagem agrupados em `RequisicaoFiltroTarefa` via `@ModelAttribute`.**
`GET /tarefas` tinha 10 `@RequestParam` soltos no metodo do controller (status, prioridade, responsavelId, prazoDesde, prazoAte, busca, ordenarPor, direcao, pagina, tamanho). Viraram um `record` vinculado com `@ModelAttribute` - suportado como *constructor binding* desde o Spring Framework 6.1, entao nao exige o record ter setters nem construtor sem argumentos. Um construtor compacto assume os defaults que antes viviam em `@RequestParam(defaultValue = ...)`. Os campos `pagina`/`tamanho` sao `Integer` (boxed), nao `int`: quando o cliente nao manda o parametro, o Spring nao converte "ausente" pra um primitivo e a requisicao inteira falharia com 400 antes do construtor compacto rodar - só um teste de integração real (`GET /tarefas?status=X` sozinho, sem paginação) pegou isso.

**Exclusao de `Projeto`/`Tarefa` e soft delete, nao `DELETE` fisico.**
`excluidoEm` (nulo = ativo) e setado em vez de remover a linha - os repositorios expõem `findByIdAndExcluidoEmIsNull`/variantes e `TarefaSpecifications.naoExcluida()` filtra a listagem, entao um recurso excluido se comporta como inexistente em toda consulta. Motivo: preserva `historico_tarefa` (que tinha `ON DELETE CASCADE` em `tarefa_id` - um DELETE fisico apagava a propria auditoria da tarefa junto) e permite desfazer uma exclusao por engano. Deletar um projeto nao cascateia soft delete pras tarefas dele (o pacote `task` depende de `project`, nunca o contrario) - nao abre brecha de acesso porque todo endpoint de tarefa passa por `MembroProjetoService.obterMembro` primeiro, que ja nega acesso a um projeto excluido.

**Board em tempo real via SSE (Server-Sent Events), nao WebSocket.**
`GET /projetos/{id}/tarefas/eventos` mantem a conexao aberta e `TarefaEventoBroadcaster` reenvia pra todo inscrito do projeto quando `TarefaStatusAlteradoEvent` e publicado (mesmo evento que já alimenta `HistoricoTarefaListener` - so mais um listener reagindo, sem duplicar a logica de mudanca de status). SSE em vez de WebSocket porque o fluxo e unidirecional (servidor -> cliente); WebSocket exigiria lidar com upgrade de protocolo e canal bidirecional pra um caso que nao precisa disso. Limitacao real: registro em memoria (`ConcurrentHashMap`), so funciona com uma instancia da API - a mesma premissa do cache Caffeine (`ConfiguracaoCache`); com mais de uma instancia atras de um load balancer precisaria de um broker externo (Redis pub/sub, etc.). Outra pegadinha: `EventSource` do browser não manda headers customizados, entao `Authorization: Bearer` nao chega - o filtro JWT aceita o token via `?token=` como fallback so pra esse caso.

**Log de auditoria via evento generico (`EventoAuditoria`), nao uma classe por acao.**
`LogAuditoria` registra ciclo de vida de projeto/membro/tarefa e autenticacao (criar/atualizar/excluir projeto, adicionar/remover membro, criar/atualizar/excluir tarefa, registro, login) - mesmo padrao evento+listener (`AuditoriaListener`, `AFTER_COMMIT` + `REQUIRES_NEW`) do historico de tarefa. Nao duplica mudanca de status (isso já é o `HistoricoTarefa`, com mais detalhe - `statusAnterior`/`statusNovo`). Um evento generico (`acao`/`tipoEntidade` como enum, nao uma classe `ProjetoCriadoEvent`/`TarefaExcluidaEvent`/etc.) porque com ~10 pontos de publicacao, uma classe por acao seria muito boilerplate pra pouco ganho de tipagem. Consulta (`GET /projetos/{id}/auditoria`) e restrita ao ADMIN do projeto, mesma regra de `exigirAdmin`.

**CORS explicito, nunca `*`.**
Origem vem de config (`app.cors.allowed-origins`, default `localhost:3000`/`localhost:5173`) porque a API usa `Authorization: Bearer` com `allowCredentials(true)` - wildcard de origem e credenciais sao mutuamente exclusivos na spec de CORS, então nao daria pra usar `*` mesmo se quisesse.

## Testes

- **Unitarios (Mockito)**: `MudarStatusTarefaUseCaseTest` cobre as 3 regras de negocio de status, `CriarTarefaUseCaseTest` cobre a validacao de responsavel, `RegistrarUsuarioUseCaseTest` cobre o caso de email duplicado, `LimpezaRefreshTokenJobTest` e `TarefaEventoBroadcasterTest` cobrem o job de limpeza e o registro/remoção de inscritos SSE isoladamente. Priorizei os pontos onde um bug de logica passaria despercebido em teste manual e onde o enunciado exige explicitamente uma regra.
- **Integracao (`@SpringBootTest` + Testcontainers)**: `FluxoCriticoIntegrationTest` sobe um Postgres real (nao H2) e roda o fluxo completo registrar → login → criar projeto → criar tarefa → mudar status → conferir relatorio, alem do caso de rota protegida sem token; `ProjetoAutorizacaoIntegrationTest` cobre membership, autorizacao ADMIN vs MEMBER e soft delete de projeto ponta a ponta; `TarefaConsultaIntegrationTest` cobre listagem com filtro/paginação, exclusão (soft delete, some da listagem mas não conta mais pro limite de WIP), histórico e a conexão SSE (via header e via `?token=`); `RelatorioCacheIntegrationTest` confirma o comportamento do cache (hit/evict) via HTTP, algo que Mockito puro nao alcança porque `@Cacheable`/`@CacheEvict` dependem do proxy real do Spring; `AuditoriaIntegrationTest` confirma que `AuditoriaListener` grava e que só ADMIN consulta; `FiltroLimitacaoRequisicoesIntegrationTest` cobre o bloqueio por excesso de tentativas de login/registro. Usar Postgres real aqui (em vez de H2) evita que o teste passe por causa de uma diferenca de dialeto que so apareceria em producao.
- Não persegui cobertura de 100% - o enunciado explicitamente não pede isso, e testar getters/setters ou DTOs (records) não agrega nada.

## O que eu faria diferente com mais tempo

Refresh token, cache do relatório, rate limiting no login/registro, CORS, segredos via env var, limpeza de refresh tokens, Actuator, CI, Dockerfile da API, mais testes de integração, soft delete e board em tempo real - tudo isso já estava listado aqui como pendência e foi implementado (ver seções acima). O que seguiria de fato:

- **Broker externo pro SSE em múltiplas instâncias**: `TarefaEventoBroadcaster` guarda os emissores em memória, então só funciona com uma instância da API. Escalar horizontalmente exigiria Redis pub/sub (ou similar) pra um emissor conectado numa instância receber evento publicado em outra.
- **Log de auditoria mais rico**: hoje `LogAuditoria` guarda ação/entidade/ator/quando, mas não o diff (o que mudou de fato num `PROJETO_ATUALIZADO`, por exemplo). Um `detalhe` estruturado (JSON com antes/depois) valeria a pena se a auditoria virar uma feature consultada de verdade, não só um registro de "aconteceu".
- **Verificação de email**: registro aceita qualquer email sem confirmar posse dele. Ok pra teste técnico; produção pediria um fluxo de verificação antes de liberar login.
- **Rate limiting além de `/api/auth/**`**: hoje só login/registro tem limite por IP. Endpoints autenticados de escrita (criar tarefa em massa, por exemplo) não têm proteção equivalente contra abuso de um usuário autenticado.
- **Métricas de negócio no Actuator**: `/actuator/health`/`/info` estão expostos, mas nenhuma métrica customizada (Micrometer) foi adicionada - ex.: contagem de tarefas criadas por minuto, latência por endpoint segmentada por rota.
