# Plano: SPEC-011 retorno sonoro ao visitante

Depende do firmware da SPEC-003. Etapa única: a mudança é pequena e só no firmware.

## Passos

- `lib/doorbell_core/beep_pattern`: os três padrões como sequência de durações que alterna ligado e desligado, começando ligado, e o mapeamento do resultado do envio para o padrão (2xx: entregue; qualquer outro: perdido). Durações: aperto aceito 100 ms ligado (#1, #2); entregue 80 ligado, 80 desligado, 80 ligado (#3); perdido 600 ligado (#4).
- `src/buzzer`: tarefa do FreeRTOS que recebe padrões por uma fila e toca cada um com `vTaskDelay`, sem bloquear o laço principal, que fica parado no HTTPS durante o envio (#2). Com `BUZZER_PIN` igual a -1, nada é criado e cada chamada vira nada (#6).
- `main`: bipe de aperto aceito antes da captura; bipe de entregue ou perdido no fim de `sendRing`, inclusive nos retornos antecipados (sem WiFi, captura, foto grande). Aperto no intervalo continua só no log (#5).
- `scripts/inject_config.py`: `BUZZER_PIN` opcional, padrão -1, validado como número de GPIO. `.env.example` e `CLAUDE.md` ganham a chave.

## Testes

| Teste | Tipo | Cobre |
|---|---|---|
| Durações e alternância de cada padrão; bipe de perda bem mais longo que o de aperto; pausa entre padrões seguidos | unitário (`native`) | #1, #3, #4, CA-6 |
| Bipe de aperto enfileirado sem esperar o som (captura não atrasa) | manual na placa, pelo tempo no log | #2 |
| 2xx vira entregue; 4xx, 5xx e erro de transporte viram perdido | unitário (`native`) | #3, #4 |
| CA-1 a CA-5 | manual na placa | #1 a #6 |

## Arquivos afetados

`doorbell/**`, `docs/specs/011-*`, `CLAUDE.md`.

## Riscos

- Buzzer ativo que consome mais que o GPIO aguenta soa fraco ou derruba a tensão: trocar por módulo com transistor.
- Rolling: nenhum; o backend não muda.
