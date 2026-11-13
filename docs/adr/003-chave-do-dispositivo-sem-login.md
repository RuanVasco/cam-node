# ADR-003: dispositivo autenticado por chave fixa, histórico sem login no MVP

| Campo | Valor |
|---|---|
| Status | aceita |
| Data | 2026-11-13 |

## Contexto

O dispositivo envia fotos pela internet, por `campainha.formulei.com.br`, porque a apresentação é feita de uma rede que só libera saída HTTP e HTTPS. Sem proteção, qualquer pessoa poderia criar toques. O MVP é uma demonstração com prazo curto e não tem usuários além do autor.

## Decisão

O upload exige a chave do dispositivo no header `X-Device-Key`, comparada em tempo constante com o segredo `DEVICE_API_KEY` vindo do Infisical. A leitura do histórico, das fotos e do stream é pública no MVP.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| mTLS com certificado por dispositivo | Gestão de certificados no ESP32 e no Cloudflare sem ganho para um dispositivo só. |
| Login no frontend já no MVP | Fora do prazo e sem usuários reais para proteger. |
| Cloudflare Access na frente do histórico | Bloquearia também o dispositivo, que exigiria service token, e tira a autenticação da aplicação. Descartado em 2026-11-13: a proteção da leitura será feita no Spring Security. |

## Consequências

- Configuração simples no firmware e no simulador (mesma chave, injetada na compilação ou por ambiente).
- A chave fica gravada no firmware; quem tiver a placa em mãos pode extraí-la. Troca exige regravar o firmware.
- Fotos da porta ficam visíveis a quem conhecer a URL.

## Revisão

Antes de uso real em casa: proteger a leitura com Spring Security (login com cookie de sessão, que também autentica o `EventSource` do SSE) e permitir chave por dispositivo com revogação.
