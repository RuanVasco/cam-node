# CLAUDE.md

Guia de desenvolvimento do cam-node, uma campainha inteligente: um ESP32-S3-CAM (OV2640) com botão físico tira uma foto quando alguém toca e envia para o backend, que avisa o frontend web em tempo real. Vale para qualquer código deste repositório.

## MVP (prazo: 2026-11-23)

Apresentação feita de dentro do IF, numa rede que só libera saída HTTP/HTTPS. O dispositivo fala com o backend pela internet, pelo domínio público.

1. Botão pressionado no ESP32: captura a foto e faz `POST` multipart para o backend.
2. Backend guarda a foto no MinIO e os metadados no Postgres, e avisa os clientes conectados por SSE.
3. Frontend mostra o histórico de toques por data e hora (mais recente primeiro), atualizando ao vivo.
4. Tudo publicado no homelab (ver Deploy).

Sem login no frontend no MVP. Fora do MVP, planejado: notificação push (Web Push ou FCM), MQTT, campainha interna reaproveitando a existente (optoacoplador ou relé), stream MJPEG ao vivo, deep sleep e bateria, configuração de WiFi dinâmica (WiFiManager e NVS).

## Estrutura do repositório

| Pasta | O que é | Stack |
|---|---|---|
| `firmware/` | Firmware do ESP32-S3-CAM | C++ (framework Arduino) no PlatformIO, `esp32-camera` |
| `doorbell/` | Programa de desktop que simula o ESP (envia uma foto ao backend) | C, libcurl, compilado e executado via Docker (Alpine) |
| `backend/` | API REST e SSE | ver Stack |
| `frontend/` | Interface web | ver Stack |
| `specs/` | Especificações (fonte da verdade do comportamento) | Markdown |
| `docs/adr/` | Decisões de arquitetura | Markdown |
| `docs/glossario.md` | Glossário de domínio PT-BR para EN | Markdown |

## Stack e comandos

| Camada | Tecnologia |
|---|---|
| Runtime | Java 25 (LTS). O JDK local 26 em `C:\jdk\jdk-26.0.2` compila com `release 25` |
| Backend | Spring Boot 4.1.x (Spring Framework 7.0.x), Maven, `application.yml` |
| Banco | PostgreSQL (LXC `db` do homelab) |
| Migrations | Flyway (gerenciado pelo Boot) |
| Armazenamento de foto | MinIO (VM `utils`), acessado pelo AWS SDK v2 (S3) |
| Frontend | Angular 22 + Angular Material 22, TypeScript strict |
| Testes back | JUnit 6, Testcontainers (Postgres e MinIO) |
| Firmware | PlatformIO, Arduino-ESP32 3.x |

Ao criar cada parte, conferir se existe versão estável mais nova e usar a mais recente.

Versões vindas do Boot 4.1.x que quebram exemplo antigo de internet:

- Jackson 3: o pacote é `tools.jackson`, não `com.fasterxml.jackson`. Não copiar snippet de Jackson 2.
- JUnit 6: não é JUnit 5. Conferir a API antes de copiar teste de projeto anterior.
- Spring Security 7: a configuração mudou em relação ao 6.x.
- Hibernate 7.
- Starters renomeados: `spring-boot-starter-webmvc` (não `-web`) e `spring-boot-starter-flyway`.

Comandos (atualizar assim que cada parte existir):

```
docker compose up -d          # Postgres e MinIO locais
./mvnw clean verify           # build e testes do backend
./mvnw spring-boot:run        # backend local
npm ci && npm start           # frontend local
npm run build                 # build de produção do front
pio run -t upload             # grava o firmware (WIFI_SSID, WIFI_PASS, API_URL, DEVICE_API_KEY no ambiente)
docker compose -f doorbell/docker-compose.yml up --build   # simulador do ESP
```

Nunca subir versão de Spring Boot, Angular ou Java sem commit dedicado (`build:`) e suíte verde.

## Idioma

- Código em inglês: classes, funções, variáveis, arquivos, pastas, branches, commits e comentários.
- Interface e mensagens ao usuário final em PT-BR.
- Documentação (README, ADRs, specs, este arquivo) em PT-BR.
- Termos de domínio seguem `docs/glossario.md`. Tradução divergente para o mesmo conceito é erro de revisão.

## Estilo de escrita

- Não usar travessão (U+2014) nem meia-risca (U+2013) em nenhum texto: código, comentário, commit, documentação ou UI. Usar vírgula, dois pontos, parênteses ou frase separada.
- Não usar emojis em lugar nenhum: UI, README, commit, log ou comentário.
- Não abrir parágrafo com rótulo em negrito seguido de ponto. Se o rótulo organiza a leitura, ele é título ou coluna de tabela; se não organiza, sai.
- Um fato tem uma casa só. README aponta, `docs/` detalha, spec normatiza.
- Ícone sempre de biblioteca (SVG), com nome semântico e rótulo acessível quando carrega significado sozinho.

## Comentários

Comentar apenas o necessário. Comentário explica o porquê, nunca o quê.

Comenta-se: regra de negócio com origem externa, decisão técnica com a alternativa descartada e o motivo, contorno temporário com a condição para remover, invariante ou efeito colateral não óbvio.

Não se comenta: o que o código já diz, getter, setter, construtor, bloco decorativo, cabeçalho de autor ou data, código comentado, histórico de alteração.

- Se o comentário precisa explicar o que o método faz, o problema é o nome ou o tamanho do método.
- Comentário desatualizado bloqueia o PR.
- Javadoc e TSDoc apenas em API pública consumida por outro módulo, e só quando acrescentam algo além da assinatura.
- Comentário de regra de negócio aponta a spec de origem: `Rule: SPEC-001 #3`.

## Desenvolvimento orientado a especificação

A especificação é a fonte da verdade, o código é a implementação dela. Não se escreve código de funcionalidade antes da spec aprovada.

1. Spec: criar `specs/NNN-nome-curto.md` a partir de `specs/TEMPLATE.md`, com problema, escopo, regras numeradas, critérios de aceite, contrato de API, impacto em dados e fora do escopo.
2. Aprovação: discordância sobre comportamento se resolve no markdown, não no código.
3. Plano: passos, arquivos afetados, ordem e risco de rolling, em `specs/NNN-nome-curto.plano.md`.
4. Implementação: cada regra numerada vira pelo menos um teste, que cita o identificador da regra no nome ou no display name.
5. Fechamento: o que mudou durante a implementação volta para a spec no mesmo PR.

- Identificador de regra estável (`SPEC-001 #3`), nunca reaproveitado nem renumerado.
- Spec descreve comportamento observável, não implementação.
- Critério de aceite verificável. "Deve ser rápido" não é critério.
- Contrato de API definido na spec antes da implementação, incluindo erros e códigos de status.
- Decisão que atravessa várias funcionalidades vai para `docs/adr/NNN-titulo.md` (modelo em `docs/adr/TEMPLATE.md`).
- Spec pequena. Se não cabe em uma página, são duas funcionalidades.
- Correção de bug referencia a regra violada; se nenhuma cobre, a spec é atualizada no mesmo PR.

## Commits

- Conventional Commits, em inglês, no imperativo presente: `type(scope): subject`.
- Sem coautoria, sem trailer de ferramenta, sem assinatura de agente, em commit ou PR.
- Tipos: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`, `revert`.
- Escopos usuais: `api`, `frontend`, `firmware`, `doorbell`, `deploy`, `specs`.
- Assunto em minúsculas, até 72 caracteres, sem ponto final.
- Breaking change: `!` após o tipo ou escopo e rodapé `BREAKING CHANGE:`.
- Um commit por unidade lógica, pequeno e frequente. Refatoração não entra junto com correção.
- Commit de funcionalidade referencia a spec no rodapé: `Refs: SPEC-001`.

## SOLID

Aplicar de forma prática, não ritualística.

- SRP: uma classe tem um motivo para mudar.
- OCP: variação de regra entra como nova implementação, não como mais um `if`. No terceiro caso, extrair estratégia.
- LSP: implementação não enfraquece o contrato.
- ISP: interface pequena, por caso de uso.
- DIP: o domínio depende de abstração; ORM, HTTP, storage e notificação dependem do domínio. Injeção por construtor.

Não criar interface com implementação única só para cumprir o princípio. Abstrair quando houver segunda implementação real, fronteira de I/O (aqui: armazenamento de foto e notificação) ou teste que exige substituição.

## Camadas e DTOs

- Controller fino: valida entrada, chama o caso de uso, mapeia a saída.
- Service: regra de negócio e limite transacional, não conhece HTTP.
- Repository ou gateway: acesso a dados e integrações, sem regra de negócio.
- Entidade de persistência nunca cruza a fronteira da API. DTO de entrada e de saída são tipos distintos.
- Mapeamento explícito e testável. Sem dependência circular entre módulos.

## Estrutura de pastas

Organização por funcionalidade, não por tipo técnico.

Backend, dentro de `com.ruanvascolabs.camnode`:

```
ring/
  RingController.java
  RingService.java
  RingRepository.java
  Ring.java                 # entidade, não sai desta pasta
  dto/
photo/                      # porta de armazenamento e adaptador MinIO
notification/               # porta de notificação e adaptador SSE
device/                     # autenticação do dispositivo
shared/                     # só o que é usado por duas ou mais features
config/
```

Frontend, dentro de `src/app`:

```
features/ring/
  ring-history/
  ring.service.ts
  ring.model.ts
core/
shared/
```

Se um arquivo em `shared` é usado por uma feature só, ele não pertence a `shared`.

## Backend

- Injeção por construtor, dependências `final`. Sem `@Autowired` em campo.
- DTO como `record`. Validação declarativa (Bean Validation) com `@Valid` na borda.
- `spring.jpa.open-in-view: false` e `spring.jpa.hibernate.ddl-auto: validate` em todo ambiente.
- Transação no serviço, escopo mínimo, sem chamada de rede (MinIO incluído) dentro de transação aberta.
- Erro centralizado em `@RestControllerAdvice`, corpo padronizado (Problem Details). Nunca vazar exceção do Hibernate, do driver ou do SDK S3.
- Exceção de domínio própria. Sem `Optional.get()` sem checagem, sem retorno `null` em serviço público.
- Paginação obrigatória em listagem.
- SSE com heartbeat periódico, para o Cloudflare (timeout de ociosidade de cerca de 100 s) e o nginx não derrubarem a conexão.

Saúde e encerramento, no `application.yml`:

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

Liveness não checa banco. Readiness checa.

## Frontend

- Componente standalone, `OnPush`, zoneless, signals para estado e `computed` para valor derivado.
- `inject()` para dependências. Estado compartilhado em serviço `providedIn: 'root'`.
- Formulário tipado (Signal Forms se estável na versão instalada, senão Reactive Forms tipado).
- Componente não conhece HTTP nem `EventSource`: passa por serviço da feature, que devolve modelo do domínio.
- Sem lógica no template. TypeScript strict, sem `any`, sem `as` para calar o compilador.
- Angular Material antes de componente próprio. Ícone via `mat-icon` com conjunto SVG registrado (Material Symbols), sem fonte de ícone por ligadura.
- Texto de tela em PT-BR, centralizado. Erro de API tratado em interceptor.

## Firmware

- Credenciais e endereço do backend injetados na compilação por `build_flags` do PlatformIO lendo variáveis de ambiente (`${sysenv.WIFI_SSID}` e afins). Nada sensível no código.
- HTTPS até o Cloudflare na porta 443, validando o certificado (bundle de CAs do Arduino-ESP32), com a chave do dispositivo em header.
- Botão em GPIO livre (a câmera ocupa vários pinos; conferir o pinout da placa), `INPUT_PULLUP` e debounce em software.
- PSRAM habilitada para os frame buffers da câmera.

## Simulador (`doorbell/`)

- C puro, bibliotecas pequenas em `src/lib/<nome>/` (`.h` e `.c`).
- HTTP via libcurl (`curl_mime` para o upload multipart). Mesmo contrato e mesmo header de chave que o firmware.

## Testes e gates de qualidade

- Toda regra de negócio nova nasce com teste unitário. Todo bug corrigido nasce com teste que falha antes.
- Não testar getter, setter, framework ou mock de mock.
- Teste de integração para query escrita à mão, migration, endpoint crítico e integração externa.
- Banco e storage em Testcontainers (PostgreSQL e MinIO reais). Nunca H2.
- Teste determinístico: sem `sleep`, relógio injetado (`Clock`), sem rede externa, sem estado compartilhado.
- Antes de commitar: build, lint, format e testes passando. Não usar `--no-verify`.

## Migrations

- Em `src/main/resources/db/migration`, nome `V<timestamp>__verbo_alvo.sql`.
- Imutável depois do merge; correção é nova migration. `flyway.clean` desabilitado, `validate-on-migrate` ligado.
- Escrever como se duas versões estivessem no ar: sem `DROP` ou `RENAME` de estrutura em uso, coluna nova nullable ou com default. Expand, migrate e contract em releases separados.

## Docker

| Uso | Imagem |
|---|---|
| Build backend | `eclipse-temurin:25-jdk-alpine` |
| Runtime backend | `eclipse-temurin:25-jre-alpine` |
| Build frontend | `node:24-alpine` |
| Runtime frontend | `nginx:alpine` (porta 8080, não root) |

- Multi-stage. Base fixada, nunca `latest`; atualização de base é commit `build:`.
- `ENTRYPOINT` em forma exec, para o SIGTERM chegar à JVM e o graceful shutdown acontecer.
- Usuário não root com UID fixo. Layered jar do Spring Boot. `.dockerignore` obrigatório.
- Segredo nunca na imagem nem em `ARG`. Configuração por variável de ambiente em runtime.
- Sem `HEALTHCHECK` no Dockerfile (o alvo é Kubernetes, quem decide é a probe).
- Memória da JVM por `-XX:MaxRAMPercentage`, nunca `-Xmx`.
- nginx do front: estático com fallback para `index.html`, proxy de `/api/` para `backend:8080`, `proxy_buffering off` na rota de SSE.
- `docker compose` local sobe só Postgres e MinIO; backend e front rodam fora do container para hot reload.

## Deploy

Mesmo modelo dos outros apps do homelab (referência: `apematch` no gitops e o workflow do `formulei`). Repositórios ao lado deste:

- `../infra` (Terraform e Ansible): Postgres no LXC `db` (10.0.0.21), MinIO na VM `utils` (10.0.0.22:9000), Infisical na `utils` (:8088), k3s em 10.0.0.23, rotas do Cloudflare tunnel em `terraform/tunnel.tf`.
- `../gitops` (ArgoCD): `argocd-apps/cam-node.yaml` e manifests em `apps/cam-node/`.

Pipeline: GitHub Actions faz build e testes, publica `ghcr.io/ruanvasco-labs/cam-node-{api,frontend}:<sha>` e atualiza a tag no gitops com `sed`. O ArgoCD sincroniza. Artefato sempre versionado por commit.

No k3s, namespace `cam-node`, réplica única: `cam-node-frontend` (nginx, entrada do tunnel em `campainha.formulei.com.br`) e `cam-node-api` (Spring, perfil `prd`, probes do actuator, `/actuator/prometheus` raspado pelo Alloy). Segredos do Infisical via External Secrets (projeto `cam-node`, slug `cam-node-n-yb8`, ambiente `prod`).

Mudanças necessárias em `../infra`: banco e usuário `camnode` com entrada em `pg_clients` para 10.0.0.23, entrada em `minio_app_users` (usuário `camnode`, bucket `cam-node-photos`), item `campainha` em `tunnel_routes`.

## Preparado para rolling deploy

- Sem estado no processo, com uma exceção registrada: os emissores SSE ficam em memória enquanto houver réplica única. Com mais de uma, a notificação passa por pub/sub externo (Redis do cluster ou `LISTEN/NOTIFY` do Postgres) atrás da mesma porta de notificação.
- Configuração por variável de ambiente, sem valor de ambiente hardcoded.
- Liveness e readiness separados; readiness só OK depois de migration e warmup.
- Graceful shutdown obrigatório. Contrato de API retrocompatível dentro do release.

## Segurança e configuração

- Segredo nunca no repositório, nem em teste, nem em exemplo. Manter `.env.example` com as chaves vazias.
- O endpoint de upload exige a chave do dispositivo. Leitura do histórico é pública no MVP (sem login).
- Toda entrada externa validada na borda: tipo e tamanho máximo da foto.
- Erro ao usuário em PT-BR, sem stack trace e sem detalhe de infraestrutura.
- Log estruturado, com id de correlação, sem dado sensível.

## Convenções de trabalho

- Branch em inglês: `type/short-description`.
- PR pequeno, com o que muda e por que muda.
- Dependência nova exige justificativa: manutenção ativa, licença compatível e ganho real.
- Código morto é removido, não comentado. `TODO` sempre com contexto e condição para resolver.

## Regras operacionais para o Claude

- Não escrever código de funcionalidade sem spec aprovada. Se a tarefa chega sem spec, escrever a spec primeiro e apresentar para aprovação.
- Commitar aos poucos: unidade lógica pronta e suíte verde vira commit, sem pedir autorização a cada um. Push e PR exigem pedido explícito.
- Não adicionar coautoria, trailer de ferramenta ou assinatura de agente em commit ou PR.
- Antes de criar arquivo, classe ou abstração nova, procurar equivalente no repositório.
- Ao terminar uma tarefa, rodar build, lint e testes e relatar o resultado real, inclusive falhas.
- Decidir sozinho o que for rotina. Perguntar quando duas leituras da tarefa levariam a trabalhos diferentes.
- Manter "Stack e comandos" atualizada.
