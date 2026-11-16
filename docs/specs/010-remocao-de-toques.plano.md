# Plano: SPEC-010 remoção de toques

A SPEC-004 e a SPEC-005 ainda são rascunho, então valem o #7 e o #8 (senha de administrador) e o #1 fica para quando houver login. Depende do histórico e do stream da SPEC-002.

## Ordem

### 1. Backend: remoção protegida pela senha de administrador

- `admin/`, no molde de `device/`:
  - `AdminProperties` (`camnode.admin.api-key`, lido de `ADMIN_API_KEY`). Opcional: vazio desliga a remoção (toda tentativa responde 401) e o início registra um aviso. Assim o deploy não depende da ordem entre o segredo no Infisical e a imagem nova.
  - `AdminKeyInterceptor` em `DELETE /api/v1/rings/*` e `POST /api/v1/rings/removal`: primeiro o bloqueio (#8, 429 com `Retry-After`), depois a senha em comparação de tempo constante (#7, 401).
  - `AdminKeyLockout` com `Clock`: guarda o momento das senhas erradas; com 5 dentro de 15 min, recusa até a mais antiga sair da janela. Tentativa recusada pelo bloqueio não conta como erro, senão o bloqueio nunca acaba. Senha certa zera a contagem. Bloqueio global, não por cliente: o IP real vem do Cloudflare e a senha é provisória.
- `photo/`:
  - `PhotoStorage.delete(key)`; o S3 não falha para chave inexistente, então apagar é idempotente.
  - Tabela `photo_deletion` (`photo_key` chave primária, `requested_at`) e `PhotoDeletionQueue`: `enqueue` só roda dentro da transação de quem chama (`Propagation.MANDATORY`); `deleteNow` tenta apagar fora da transação e tira da fila o que conseguiu; `retryPending` agendado (padrão a cada 1 h, configurável) percorre a fila e para no primeiro erro de indisponibilidade (#3).
  - `@EnableScheduling` em `config/`.
- `ring/`:
  - `RingService.remove(ids)`: tira repetidos; numa transação curta (`TransactionTemplate`, sem rede) busca os toques, falha com 404 se faltar algum, apaga pelo id conferindo a contagem (outra remoção simultânea faz a segunda falhar e desfazer tudo) e enfileira as chaves das fotos. Depois do commit publica `RingsRemoved` e chama `deleteNow`.
  - `DELETE /api/v1/rings/{id}` e `POST /api/v1/rings/removal` com `RingRemovalRequest` (`@NotNull @Size(min = 1, max = 100)`, ids não nulos) (#6). 204 nos dois.
  - Mensagens PT-BR para corpo inválido e para os novos 401 e 429.
- `application-prd.yml` e `.env.example` com `ADMIN_API_KEY`.

### 2. Backend: evento `ring-removed` no stream

- `SseRingNotifier` escuta `RingsRemoved` e envia um `ring-removed` com `{"id": ...}` por toque, pela mesma thread do agendador (#4).

### 3. Frontend

- `core/http/api-error.interceptor.ts`: `HttpContextToken` com a mensagem de erro da chamada (padrão a atual) e com os status que a própria chamada trata; 429 vira "Muitas tentativas. Tente de novo em alguns minutos."
- `features/ring/ring-api.ts`: `remove(ids, adminKey)` com header `X-Admin-Key`; um id usa `DELETE`, mais de um usa `POST /removal`. O 401 fica com quem chamou.
- `features/ring/ring-stream.ts`: evento `ring-removed` vira `{ kind: 'removed', id }`.
- `features/ring/ring.service.ts`: `remove(ids, adminKey)` e o evento do stream tiram o toque da lista e descontam do total uma vez só, mesmo se os dois chegarem (#2, #4).
- `features/ring/admin-key.store.ts`: senha em `sessionStorage`, que é por aba e some quando ela fecha (#7, CA-10). Leitura e escrita protegidas por `try/catch`.
- `features/ring/ring-removal.ts`: fluxo confirmação (#5), senha (pede se não houver; no 401 apaga a guardada e pede de novo com "Senha incorreta."), remoção e aviso "Toque removido." ou "N toques removidos.".
- Diálogos `ring-removal-confirm-dialog/` e `admin-key-dialog/` (formulário tipado). `@angular/forms` entra como dependência (pacote oficial do Angular).
- `ring-history`: ação "Remover" em cada toque, modo de seleção com `mat-checkbox` e "Remover selecionados (N)", seleção limpa dos ids que sumiram. `ring-photo-dialog`: ação "Remover"; fecha sozinho quando o toque some da lista, por remoção local ou de outra tela.
- Textos novos em `messages.ts`; ícones `delete` e `checklist` registrados.

### 4. Publicação

- `gitops`: `ADMIN_API_KEY` no `ExternalSecret` `cam-node-api-env`.
- Infisical (manual, fora do repositório): segredo `ADMIN_API_KEY` no projeto `cam-node`, ambiente `prod`. Até existir, a remoção fica desligada em produção e o resto funciona.

## Testes

| Teste | Tipo | Cobre |
|---|---|---|
| Remove: 204, some do histórico, `totalElements` cai 1, foto 404 e objeto apagado do MinIO | integração | CA-1, #2, #3 |
| Segunda remoção do mesmo toque responde 404 | integração | CA-7 |
| Armazenamento fora do ar: 204, foto 404 e chave na fila | integração | CA-3 |
| Fila reprocessada apaga o objeto e esvazia a fila | integração | CA-3 |
| Reprocessamento para no primeiro erro de indisponibilidade e mantém a fila | unitário | #3 |
| 3 ids removidos de uma vez; 101 ids é 400 sem remover; lista vazia é 400; um id inexistente é 404 sem remover os outros | integração | CA-6, #6 |
| Sem senha e com senha errada: 401 e o toque continua | integração | CA-8 |
| Sexta tentativa depois de 5 erros: 429 mesmo com a senha certa | integração | CA-9 |
| Janela de 15 min libera; tentativa bloqueada não conta; senha certa zera | unitário com relógio fixo | #8 |
| Senha não configurada: 401 | unitário | #7 |
| Assinante recebe `ring-removed` em até 2 s | integração | CA-4 |
| Remoção local e evento do stream descontam o total uma vez só | unitário do serviço | #2, #4 |
| Confirmação cancelada não chama a API | unitário do fluxo | CA-5 |
| Senha guardada reaproveitada; 401 pede de novo com "Senha incorreta." | unitário do fluxo | CA-10, #7 |
| `ring-removed` vira evento de domínio | unitário do stream | #4 |
| 429 mostra a mensagem de bloqueio; status tratado pela chamada não mostra aviso | unitário do interceptor | #8 |

CA-4 entre duas telas e CA-10 numa aba nova são conferidos à mão no navegador depois do deploy.

## Arquivos afetados

`backend/src/main/java/.../{admin,photo,ring,notification,config,shared/error}/**`, migration nova, `application*.yml`, `messages.properties`, `.env.example`, `frontend/src/app/**`, `frontend/package*.json`, `docs/glossario.md`, e `apps/cam-node/secret-store.yaml` no `gitops`.

## Riscos

- Rolling: só a tabela nova `photo_deletion` (expand puro); a versão anterior não a conhece e não é afetada. Durante a troca de pod, uma remoção feita no pod novo não chega por SSE às telas ligadas no antigo; a reconexão recarrega a primeira página.
- Bloqueio global: quem não tem a senha consegue travar a remoção por 15 min errando 5 vezes. Aceito enquanto a senha for provisória.
- A senha fica no `sessionStorage` da aba; um XSS a leria. Aceito pelo mesmo motivo; sai com a SPEC-004.
