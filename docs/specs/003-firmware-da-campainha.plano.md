# Plano: SPEC-003 firmware da campainha

Depende do `POST /api/v1/rings` da SPEC-001 publicado em `campainha.formulei.com.br`.

## Ordem

### 1. Projeto e lógica pura testada

- Projeto PlatformIO em `doorbell/` com a plataforma `pioarduino` fixada em versão estável (Arduino-ESP32 3.x; a plataforma oficial parou no 2.x). Placa base `esp32-s3-devkitc-1` com PSRAM octal (`qio_opi`) e `-DBOARD_HAS_PSRAM`. A câmera vem do próprio core (`esp_camera.h`), sem biblioteca extra.
- `build_flags` com `WIFI_SSID`, `WIFI_PASS`, `API_URL`, `DEVICE_API_KEY` e `BUTTON_PIN` (padrão 0, botão BOOT) lidos por `${sysenv.*}`; `#error` na compilação se alguma credencial vier vazia (#1).
- Lógica sem dependência do Arduino em `lib/`, testável no computador:
  - `Debouncer`: aceita o aperto depois de 50 ms estável (#3).
  - `RingGate`: ignora apertos por 5 s depois de um envio (SPEC-001 #8).
  - `RetryPolicy`: decide nova tentativa por resultado (falha de rede ou 5xx: uma, após 2 s; 4xx e 2xx: nenhuma) (#6).
  - `Multipart`: monta o cabeçalho e o rodapé do corpo com o campo `photo` e `Content-Type: image/jpeg`.
- Ambiente `native` com Unity para esses testes, rodado num container (não há `gcc` no host Windows), no mesmo modelo do simulador.

### 2. Integração na placa

- `camera`: pinout do ESP32-S3-EYE, JPEG em SVGA (800x600) e qualidade 12, frame buffer na PSRAM. Antes de cada captura descarta o quadro que já estava no buffer (#4). Foto acima de 2 MB não é enviada (#5). Falha ao iniciar a câmera reinicia a placa (#9).
- `network`: WiFi em modo estação com reconexão automática e checagem no laço, sem reiniciar (#7); hora por NTP ao conectar, para validar o certificado.
- `ring_client`: POST por HTTPS com o bundle de CAs do core (sem `setInsecure`) e header `X-Device-Key`; timeout total abaixo do orçamento de 5 s do CA-1 (#2).
- `main`: laço com watchdog de tarefa (#9), log serial de uma linha por evento, sem imprimir a chave nem a senha (#8).
- `CLAUDE.md`: comandos de teste e de monitor serial em "Stack e comandos".

## Testes

| Teste | Tipo | Cobre |
|---|---|---|
| Ruído menor que 50 ms não vira aperto; nível estável por 50 ms vira um aperto só | unitário (`native`) | #3 |
| Três apertos em 2 s liberam um único envio; aperto depois de 5 s libera outro | unitário (`native`) | SPEC-001 #8, CA-2 |
| Falha de rede e 5xx: uma nova tentativa com espera de 2 s; 4xx e 2xx: nenhuma | unitário (`native`) | #6 |
| Corpo multipart com o campo e o tipo esperados pelo backend | unitário (`native`) | contrato da SPEC-001 |
| CA-1 a CA-7 | manual na placa, com o log serial e o histórico em `campainha.formulei.com.br` | #1 a #9 |

## Arquivos afetados

`doorbell/**`, `CLAUDE.md`.

## Riscos

- Pinout e tipo de PSRAM assumidos para a GOOUUU V1.5 (ESP32-S3-EYE, N16R8); se a câmera não iniciar ou a placa reiniciar em laço, conferir pelo log do boot e pela saída do `esptool`.
- Driver CH340 no Windows para gravar pela porta TTL.
- Bot Fight Mode da Cloudflare desafiando o cliente: verificado no primeiro envio real (resposta 403 com página HTML).
- Rolling: nenhum; o firmware só consome o contrato existente.
