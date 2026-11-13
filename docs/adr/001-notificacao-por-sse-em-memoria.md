# ADR-001: notificação ao frontend por SSE com emissores em memória

| Campo | Valor |
|---|---|
| Status | aceita |
| Data | 2026-11-13 |

## Contexto

O frontend precisa saber de um toque novo assim que ele acontece. O fluxo é só do servidor para o navegador. O tráfego passa pelo Cloudflare tunnel e pelo nginx do frontend. A API roda com uma réplica no k3s, e o prazo do MVP é 2026-11-23.

## Decisão

Usamos Server-Sent Events (`SseEmitter`), com a lista de emissores em memória do processo, atrás de uma porta de notificação do domínio.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Polling periódico | Atraso de até um intervalo inteiro e requisições constantes sem novidade. |
| WebSocket | Bidirecional sem necessidade; mais configuração no nginx e no Spring. |
| Pub/sub externo (Redis ou `LISTEN/NOTIFY`) já no MVP | Só se paga com mais de uma réplica, o que não existe. |

## Consequências

- A notificação chega em menos de um segundo e o `EventSource` do navegador reconecta sozinho.
- A API fica com estado no processo: com duas réplicas, um toque recebido numa delas não chega aos clientes conectados na outra.
- Exige sinal de vida periódico (o Cloudflare encerra conexão ociosa por volta de 100 s) e `proxy_buffering off` no nginx.
- O `EventSource` não envia header `Authorization`; quando houver login, a autenticação do stream será por cookie.

## Revisão

Ao subir a segunda réplica da API: trocar a implementação da porta por pub/sub no Redis do cluster ou `LISTEN/NOTIFY` do Postgres.
