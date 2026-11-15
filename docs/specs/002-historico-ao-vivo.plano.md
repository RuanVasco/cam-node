# Plano: SPEC-002 histórico de toques ao vivo

Depende da base do backend e da tabela `ring` do plano da SPEC-001.

## Ordem

### 1. Leitura no backend (`ring/`)

- `GET /api/v1/rings`: parâmetros `page` e `size` explícitos com Bean Validation (o resolver de `Pageable` troca página negativa por 0 em silêncio, e a spec pede 400); o serviço limita `size` a 100 (#2). Resposta em DTO próprio de página (`content`, `page`, `size`, `totalElements`), sem expor o `Page` do Spring. Erros de parâmetro (validação e conversão) com texto PT-BR no `ApiExceptionHandler`.
- `GET /api/v1/rings/{id}/photo`: busca o toque, abre o objeto pela porta `PhotoStorage` e devolve em streaming com `Cache-Control: public, max-age=31536000, immutable` (#3); toque inexistente vira `RingNotFoundException`, 404.

### 2. Notificação (`notification/`)

- `SseRingNotifier` escutando o evento `RingRegistered` (`@EventListener`), com a lista de `SseEmitter` em memória (ADR-001) e o endpoint `GET /api/v1/rings/stream`.
- Sinal de vida agendado (`@Scheduled`), com intervalo por configuração, padrão 30 s (#5); emissor que falha ao enviar é removido.

### 3. Frontend

- Projeto Angular 22 em `frontend/`: standalone, zoneless, Angular Material, locale `pt-BR` registrado, ícones Material Symbols em SVG registrados no `MatIconRegistry`.
- `features/ring/ring.service.ts`: busca páginas por `HttpClient` e expõe o stream do `EventSource` convertido em modelo de domínio; ao reabrir a conexão depois de erro, recarrega a primeira página (#9).
- `features/ring/ring-history/`: estado em signals; `computed` que agrupa por dia no fuso `America/Sao_Paulo` com rótulos "Hoje", "Ontem" ou a data (#6, #7); destaque de 10 s no toque novo (#8); estado vazio (#10); "Carregar mais"; diálogo com a foto ampliada.
- Indicador "Ao vivo" ou "Reconectando" na barra superior.
- Textos de tela centralizados em um arquivo de mensagens PT-BR.
- `proxy.conf.json` para `ng serve` encaminhar `/api` ao backend local.

### 4. Testes

| Teste | Tipo | Cobre |
|---|---|---|
| Ordem decrescente e paginação padrão | integração | CA-1 |
| `size=500` limitado a 100 | integração | CA-2 |
| Foto com cabeçalho de cache, e 404 | integração | CA-3, CA-4 |
| Cliente SSE recebe `ring` após registro | integração | CA-5 |
| Sinal de vida com intervalo curto configurado no teste | integração | CA-6 |
| Agrupamento por dia e conversão de fuso | unitário do `computed` | CA-7 |
| Toque novo no topo e destaque expira | unitário com relógio falso | CA-8 |
| Reconexão recarrega primeira página | unitário do serviço com `EventSource` dublê | CA-9 |

CA-6 roda com intervalo de sinal de vida reduzido por configuração, para o teste não esperar 65 s.

### 5. Publicação

Segue a seção Deploy do `CLAUDE.md`:

1. Dockerfiles multi-stage da API e do frontend, `nginx.conf` (fallback para `index.html`, proxy de `/api/`, `proxy_buffering off` e `proxy_read_timeout` longo no stream) e `.dockerignore`.
2. Workflows do GitHub Actions para `cam-node-api` e `cam-node-frontend` (build, testes, GHCR, atualização da tag no gitops). Antes: segredo `GITOPS_DEPLOY_KEY` no repositório e conferir se o `GHCR_PULL_TOKEN` do Infisical lê pacotes da conta pessoal.
3. PR no `infra`: banco e usuário `camnode`, `pg_clients`, `minio_app_users`. Depois do apply: senha do MinIO em `/etc/minio-apps/camnode.env` e criação do banco.
4. PR no `gitops`: `argocd-apps/cam-node.yaml`, `apps/cam-node/` (namespace, `SecretStore` com slug `cam-node-n-yb8`, `ExternalSecret`, deployments e services).
5. PR no `infra` com o item `campainha` em `tunnel_routes`, aplicado só com o frontend rodando.
6. Conferir o Bot Fight Mode da zona antes do teste com a placa.

## Arquivos afetados

`backend/**`, `frontend/**`, `.github/workflows/**`, e os repositórios `../infra` e `../gitops`.

## Riscos

- Buffering no nginx ou no Cloudflare segurando eventos: validado com `curl -N` no domínio público antes da apresentação.
- Rolling: nenhuma mudança de dados.
