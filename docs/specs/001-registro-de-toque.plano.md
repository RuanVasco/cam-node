# Plano: SPEC-001 registro de toque

## Ordem

O backend vem primeiro e é validado com o simulador, sem depender da placa. O firmware entra quando a placa estiver em mãos.

### 1. Base do backend

- Projeto Maven em `backend/`: Spring Boot 4.1.x, Java 25, starters `webmvc`, `data-jpa`, `flyway`, `validation`, `actuator`; `micrometer-registry-prometheus`, driver PostgreSQL, AWS SDK v2 (`s3`), Testcontainers (`postgresql`, `minio`).
- `application.yml` com perfis padrão (local) e `prd`; configuração de saúde e encerramento do `CLAUDE.md`.
- `docker-compose.yml` na raiz com Postgres e MinIO locais, e `.env.example` com as chaves vazias.
- `@RestControllerAdvice` com Problem Details.

### 2. Dados

- Migration `V<timestamp>__create_ring.sql`: tabela `ring` (`id uuid`, `ringed_at timestamptz`, `photo_key text`, `photo_size_bytes int`), índice em `ringed_at desc`. Expand puro.
- Entidade `Ring` e `RingRepository` em `ring/`.

### 3. Armazenamento de foto (`photo/`)

- Porta `PhotoStorage` (`store`, `open`) e adaptador `S3PhotoStorage` apontando para o MinIO (path style, endpoint e bucket por configuração).
- Falha do SDK vira exceção de domínio `PhotoStorageUnavailableException`, mapeada para 503 (#6).

### 4. Chave do dispositivo (`device/`)

- Interceptor que protege apenas `POST /api/v1/rings`, compara `X-Device-Key` com `DEVICE_API_KEY` usando `MessageDigest.isEqual` e responde 401 em Problem Details (#1).

### 5. Caso de uso (`ring/`)

- `RingService.register(photo)`: valida assinatura JPEG (#3), grava a foto, depois persiste o toque com `ringedAt = clock.instant()` (#5, `Clock` injetado), depois publica o evento de domínio `RingRegistered` pelo `ApplicationEventPublisher` (#7). O adaptador SSE da SPEC-002 escuta esse evento; sem porta própria nem implementação vazia enquanto ele não existe.
- Limite de 2 MB por `spring.servlet.multipart.max-file-size` e `max-request-size`, com `MaxUploadSizeExceededException` mapeada para 413 (#4).
- `RingController` com `POST /api/v1/rings` e DTO de resposta `RingResponse`.

### 6. Testes

| Teste | Tipo | Cobre |
|---|---|---|
| Upload válido cria toque e responde 201 | integração (Postgres e MinIO em Testcontainers) | CA-1 |
| Chave ausente e chave errada | integração | CA-2 |
| Sem campo `photo` | integração | CA-3 |
| PNG declarado como JPEG | unitário do validador e integração | CA-4 |
| Arquivo de 2,5 MB | integração | CA-5 |
| MinIO fora do ar: 503 e tabela vazia | integração com endpoint de storage sem ninguém escutando | CA-6 |
| Evento `RingRegistered` publicado com o toque criado | unitário com dublê do publicador | CA-7 |

### 7. Simulador (`tools/simulator/`)

- Completar a lib `http` com uma função de upload multipart (`curl_mime`), mais o header da chave.
- `main.c` recebe URL, chave e caminho do JPEG por argumento ou ambiente, envia e imprime status e corpo.
- Dockerfile: adicionar `curl-dev` no build e `libcurl` no runtime.

### 8. Firmware (`doorbell/`)

- Projeto PlatformIO para a placa ESP32-S3 com PSRAM, `esp32-camera`, `build_flags` lendo `WIFI_SSID`, `WIFI_PASS`, `API_URL`, `DEVICE_API_KEY` do ambiente.
- Módulos pequenos: `camera` (init e captura), `wifi`, `button` (debounce e janela de 5 s, #8), `ring_client` (POST multipart por HTTPS com bundle de CAs).
- CA-8 verificado à mão na placa, apertando o botão três vezes em 2 s e conferindo um único toque no histórico.

## Arquivos afetados

`backend/**`, `docker-compose.yml`, `.env.example`, `tools/simulator/**`, `doorbell/**`.

## Riscos

- Pinout da placa ainda não conferido: pino do botão e modelo de câmera definidos com a placa em mãos.
- Rede do IF: WPA2-Enterprise ou portal cativo impedem o ESP; plano B é o hotspot do celular, com firmware compilado para ele.
- Rolling: só expand (tabela nova), sem risco para versão anterior.
- A MinIO parou de publicar imagens no Docker Hub e no quay.io. Testes e compose local usam `bitnamilegacy/minio:2025.7.23`, congelada; o homelab segue com a imagem que já tem em cache. Rever quando o MinIO da `utils` for atualizado.
