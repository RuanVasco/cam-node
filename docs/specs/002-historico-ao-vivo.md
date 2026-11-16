# SPEC-002: histórico de toques ao vivo

| Campo | Valor |
|---|---|
| Status | aprovada |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-13 |
| Aprovada em | 2026-11-13 |

## Problema

O morador precisa ver quem tocou agora e quem tocou antes, sem recarregar a página a cada toque.

## Escopo

Tela única com o histórico de toques agrupado por dia, foto ampliável, e atualização em tempo real quando um toque novo chega.

## Fora do escopo

- Login e controle de acesso (ADR-003).
- Notificação push e som no navegador.
- Filtro por período, busca, exclusão de toque.

## Regras de negócio

- **#1** O histórico é público, sem autenticação.
- **#2** Toques ordenados do mais recente para o mais antigo, paginados: 20 por página por padrão, no máximo 100.
- **#3** A foto de um toque nunca muda, então pode ser guardada em cache pelo navegador indefinidamente.
- **#4** A cada toque registrado, os clientes conectados recebem um evento com os mesmos campos de um item do histórico.
- **#5** O stream envia um sinal de vida a cada 30 s, para a conexão não ser encerrada por inatividade.
- **#6** Datas e horas aparecem em PT-BR, no fuso `America/Sao_Paulo`.
- **#7** Os toques aparecem agrupados por dia, com cabeçalho "Hoje", "Ontem" ou a data por extenso.
- **#8** Um toque novo entra no topo sem recarregar a página e fica destacado por 10 s.
- **#9** Ao reconectar o stream depois de uma queda, a tela recarrega a primeira página, para não perder toques ocorridos durante a queda.
- **#10** Sem toques, a tela mostra "Nenhum toque registrado ainda."
- **#11** Ao abrir o stream, o servidor envia um sinal de vida na hora, para a conexão aparecer como aberta sem esperar o primeiro intervalo (proxies como o Cloudflare só repassam a resposta quando chega o primeiro dado).

## Critérios de aceite

- **CA-1** Dados 25 toques, quando consulto sem parâmetros, então recebo os 20 mais recentes em ordem decrescente e `totalElements` 25. Cobre #1, #2.
- **CA-2** Dado `size=500`, então a página tem no máximo 100 itens. Cobre #2.
- **CA-3** Dado um toque existente, quando peço a foto, então 200 `image/jpeg` com `Cache-Control: public, max-age=31536000, immutable`. Cobre #3.
- **CA-4** Dado um id inexistente, quando peço a foto, então 404. Cobre #3.
- **CA-5** Dado um cliente no stream, quando um toque é registrado, então ele recebe o evento `ring` em até 2 s. Cobre #4.
- **CA-6** Dado um cliente no stream sem toques por 65 s, então ele recebeu pelo menos dois sinais de vida e a conexão segue aberta. Cobre #5.
- **CA-7** Dado um toque às 2026-11-23T01:30:00Z, então ele aparece no grupo do dia 22 de novembro de 2026 com o horário 22:30, e a foto ampliada mostra 22 de nov. de 2026, 22:30. Cobre #6, #7.
- **CA-8** Dada a tela aberta, quando chega um toque, então ele aparece no topo, destacado, sem recarga. Cobre #8.
- **CA-9** Dado o stream caído e um toque registrado durante a queda, quando reconecta, então o toque aparece. Cobre #9.
- **CA-10** Dado o intervalo padrão de 30 s, quando um cliente abre o stream, então recebe um sinal de vida em até 2 s. Cobre #11.

## Contrato de API

```
GET /api/v1/rings?page=0&size=20
```

Sucesso (200):

```json
{
  "content": [
    { "id": "0193f1c2-...", "ringedAt": "2026-11-23T19:42:10.123Z", "photoUrl": "/api/v1/rings/0193f1c2-.../photo" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25
}
```

Erros: 400 para `page` negativo ou acima de 1.000.000, ou `size` menor que 1.

```
GET /api/v1/rings/{id}/photo
```

200 com o JPEG. 404 quando o toque não existe.

```
GET /api/v1/rings/stream
Accept: text/event-stream
```

Eventos:

```
event: ring
data: {"id":"0193f1c2-...","ringedAt":"2026-11-23T19:42:10.123Z","photoUrl":"/api/v1/rings/0193f1c2-.../photo"}

: heartbeat
```

## Impacto em dados

Somente leitura da tabela de toques criada pela SPEC-001.

## Interface

- Barra superior com o título "Campainha" e um indicador de conexão: "Conectando" até o stream abrir pela primeira vez, depois "Ao vivo" ou "Reconectando".
- Lista agrupada por dia; cada item mostra a miniatura da foto e o horário.
- Clicar na miniatura abre a foto ampliada em diálogo.
- Botão "Carregar mais" ao fim da lista.

## Riscos e decisões

- Notificação via SSE com emissores em memória: ADR-001.
- Histórico público expõe as fotos da porta a quem tiver a URL. Aceito para a apresentação do MVP; revisar antes de uso real (ADR-003).

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-13 | Criação |
| 2026-11-13 | Aprovada |
| 2026-11-15 | CA-7 com o formato de data do pt-BR; estado "Conectando" no indicador |
| 2026-11-16 | #11 e CA-10: sinal de vida ao abrir o stream (a tela ficava até 30 s em "Conectando" atrás do Cloudflare) |
| 2026-11-16 | Contrato: 400 para `page` acima de 1.000.000 (antes respondia 500 por estouro do offset) |
