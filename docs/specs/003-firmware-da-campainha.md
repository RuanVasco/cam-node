# SPEC-003: firmware da campainha

| Campo | Valor |
|---|---|
| Status | aprovada |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | 2026-11-16 |

## Problema

O backend e a tela já recebem e mostram toques, mas só o simulador os envia. Falta a campainha física: o ESP32-S3-CAM precisa transformar o aperto do botão em um toque registrado pela internet, inclusive na rede do IF, que só libera HTTP e HTTPS.

## Escopo

Firmware do ESP32-S3-CAM (OV2640) que conecta no WiFi, detecta o botão, captura a foto e a envia ao backend pelo contrato da SPEC-001, pelo domínio público.

## Fora do escopo

- Fila offline: toque sem rede é perdido (SPEC-001).
- Configuração de WiFi em tempo de execução (WiFiManager, NVS): credenciais entram na compilação.
- LED ou som de retorno ao visitante (SPEC-001 #9).
- Deep sleep, bateria, stream ao vivo, atualização remota de firmware.

## Regras de negócio

- **#1** Credenciais do WiFi, endereço do backend e chave do dispositivo entram na compilação, nunca no código versionado.
- **#2** O envio usa HTTPS na porta 443 e valida o certificado do servidor; com certificado inválido, nada é enviado.
- **#3** Um aperto do botão só conta depois de estável por 50 ms, para ruído elétrico não virar toque.
- **#4** Cada toque aceito gera exatamente uma captura e um envio, com a foto tirada no momento do aperto, não uma foto antiga do buffer da câmera.
- **#5** A foto é JPEG de até 2 MB (SPEC-001 #3, #4), com resolução suficiente para reconhecer um rosto a 1 m.
- **#6** Falha de rede ou resposta 5xx tem uma única nova tentativa após 2 s; resposta 4xx não é repetida.
- **#7** Sem WiFi, o dispositivo tenta reconectar sozinho, sem reiniciar, e volta a aceitar toques assim que conectar.
- **#8** O resultado de cada toque (status HTTP ou erro) aparece no log serial, sem imprimir a chave do dispositivo nem a senha do WiFi.
- **#9** O dispositivo se recupera sozinho de travamento: se a câmera falhar ao iniciar ou o programa parar de responder, ele reinicia.

## Critérios de aceite

- **CA-1** Dado o dispositivo conectado e o backend no ar, quando aperto o botão, então em até 5 s o toque aparece na tela com a foto de quem está na frente da câmera. Cobre #2, #4, #5.
- **CA-2** Dado o botão apertado três vezes em 2 s, então o backend recebe um único toque. Cobre #3 e SPEC-001 #8.
- **CA-3** Dada uma chave errada na compilação, quando aperto o botão, então o log mostra 401 e não há segunda tentativa. Cobre #6, #8.
- **CA-4** Dado o backend fora do ar, quando aperto o botão, então o log mostra duas tentativas com falha e o dispositivo continua aceitando toques depois. Cobre #6.
- **CA-5** Dado o roteador desligado e religado, quando a rede volta, então um aperto seguinte é registrado sem reiniciar a placa. Cobre #7.
- **CA-6** Dado o log serial de uma sessão inteira, então não aparecem a chave do dispositivo nem a senha do WiFi. Cobre #1, #8.
- **CA-7** Dado o endereço do backend apontando para um host com certificado inválido, quando aperto o botão, então nenhum dado é enviado e o log mostra erro de certificado. Cobre #2.

## Contrato de API

Consome o `POST /api/v1/rings` da SPEC-001, sem mudança: header `X-Device-Key`, multipart com o campo `photo`, `Content-Type: image/jpeg` na parte.

## Impacto em dados

Nenhum.

## Interface

Sem tela. Log serial em 115200 baud, uma linha por evento (conexão, toque, tentativa, resultado).

## Riscos e decisões

- Placa: GOOUUU ESP32-S3-CAM V1.5 (ESP32-S3 com PSRAM, OV2640). O pinout da câmera é o mesmo do ESP32-S3-EYE do exemplo `CameraWebServer`; conferir com a placa na primeira gravação.
- Botão: até o botão do IF estar em mãos, o toque vem do botão BOOT da própria placa (GPIO0, já com pull-up). O pino é definido na compilação, então trocar pelo botão externo (ligado entre um GPIO livre e o GND) não muda o código. Disparo por timer foi descartado: gera toques que ninguém fez e não exercita o debounce (#3).
- Rede do IF: "IFRS Alunos" usa WPA3-Personal (SSID e senha, sem portal nem Enterprise), suportado pelo ESP32-S3. O ESP32 só opera em 2,4 GHz; se a rede não oferecer essa faixa, não conecta. Plano B: hotspot de celular em 2,4 GHz.
- Bot Fight Mode da Cloudflare pode desafiar um cliente que não é navegador; conferir com a placa real antes da apresentação.
- O relógio do dispositivo não é usado para o momento do toque (SPEC-001 #5), mas a validação do certificado precisa da data certa: o firmware sincroniza a hora por NTP ao conectar.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
| 2026-11-16 | Placa, botão provisório (BOOT) e tipo da rede do IF definidos |
