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
| Usuário | `User` | Pessoa com conta, que entra com Google ou com e-mail e senha (SPEC-004). |
| Sessão | `session` | Login ativo de um usuário num navegador ou no app. |
| Membro | `DoorbellMember` | Vínculo entre um usuário e uma campainha; dá acesso aos toques dela (SPEC-005). |
| Código de pareamento | `pairingCode` | Código impresso no adesivo da campainha que torna o usuário membro dela. |
| Modo de configuração | `setupMode` | Estado da campainha em que ela cria a rede própria para receber o WiFi da casa (SPEC-006). |
| Rede própria | `setupAccessPoint` | Rede `CamNode-XXXX` criada pela campainha no modo de configuração. |
| Aviso de dentro | `chime` | Parte interna da campainha existente, que toca dentro de casa (SPEC-007). |
| Sono profundo | `deepSleep` | Estado de economia de energia da campainha entre toques (SPEC-008). |
| Relato de estado | `statusReport` | Envio periódico da tensão da bateria, sem toque (SPEC-008). |
| Nível da bateria | `batteryPercent` | Carga da bateria em porcentagem, calculada no backend a partir da tensão. |
| Notificação push | `pushNotification` | Aviso de toque entregue no celular mesmo com o app fechado (SPEC-009). |
| Token de push | `pushToken` | Identificador de um celular no serviço de push, registrado pelo app. |
| Exclusão de toque | `ringRemoval` | Envio de um toque para a lixeira: some do histórico, mas pode ser restaurado (SPEC-010 #9). |
| Lixeira | `trash` | Toques excluídos, visíveis só para o administrador (SPEC-010 #10). |
| Restaurar | `restore` | Devolver um toque da lixeira ao histórico (SPEC-010 #11). |
| Apagar de vez | `purge` | Remoção definitiva de um toque da lixeira e da sua foto (SPEC-010 #12). |
| Senha de administrador | `adminApiKey` | Segredo enviado em header para acessar a lixeira enquanto não há login (SPEC-010 #7). |
| Bloqueio de tentativas | `adminKeyLockout` | Recusa temporária do acesso à lixeira depois de senhas de administrador erradas demais (SPEC-010 #8). |
| Remoção pendente de foto | `PendingPhotoDeletion` | Foto de toque apagado de vez que ainda precisa sair do armazenamento (SPEC-010 #3). |
