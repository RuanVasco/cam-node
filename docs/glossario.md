# Glossário de domínio

Termo de negócio em PT-BR e o nome correspondente no código, que é escrito em inglês. Um conceito tem um nome só. Tradução divergente para o mesmo conceito é erro de revisão.

Ao criar termo novo, acrescentar aqui antes de usar no código.

## Conceitos

| PT-BR | Código | Significado |
|---|---|---|
| Campainha | `Doorbell` | O conjunto físico na porta: ESP32-S3-CAM, câmera e botão. |
| Dispositivo | `Device` | Quem chama a API de upload: a campainha real ou o simulador. Se identifica pela chave do dispositivo. |
| Chave do dispositivo | `deviceApiKey` | Segredo compartilhado enviado em header no upload. |
| Toque | `Ring` | Um acionamento do botão, com data e hora e a foto capturada. |
| Foto | `Photo` | Imagem JPEG capturada no toque, guardada no armazenamento de fotos. |
| Momento do toque | `ringedAt` | Data e hora em que o backend registrou o toque. Ordena o histórico. |
| Histórico | `RingHistory` | Lista paginada de toques, do mais recente para o mais antigo. |
| Stream de toques | `RingStream` | Conexão SSE aberta pelo frontend para receber toques novos em tempo real. |
| Assinante | `subscriber` | Cliente conectado ao stream de toques. |
| Sinal de vida | `heartbeat` | Comentário enviado periodicamente no stream para a conexão não ser encerrada por inatividade. |
| Simulador | `simulator` | Programa em C (`tools/simulator/`) que faz o papel do dispositivo sem hardware. |
