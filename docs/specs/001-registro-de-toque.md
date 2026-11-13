# SPEC-001: registro de toque

| Campo | Valor |
|---|---|
| Status | aprovada |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-13 |
| Aprovada em | 2026-11-13 |

## Problema

Quando alguém toca a campainha, o morador não sabe quem está na porta sem ir até ela. É preciso que cada toque gere um registro com a foto de quem tocou.

## Escopo

O dispositivo (campainha real ou simulador) detecta o botão, captura uma foto e a envia ao backend, que registra o toque e o torna disponível para o histórico (SPEC-002).

## Fora do escopo

- Fila offline no dispositivo: toque sem rede é perdido.
- Mais de um dispositivo com chaves distintas. Existe uma chave só.
- Som interno, notificação push, vídeo ao vivo.
- Retorno visual ao visitante (LED). Entra no produto final, não no MVP.
- Retenção e exclusão de fotos antigas.

## Regras de negócio

- **#1** O upload só é aceito com a chave do dispositivo correta no header `X-Device-Key`.
- **#2** O toque exige exatamente uma foto, no campo `photo`.
- **#3** A foto precisa ser JPEG, verificado pelo conteúdo (assinatura `FF D8 FF`) e não só pelo tipo declarado.
- **#4** A foto tem no máximo 2 MB.
- **#5** O momento do toque é a hora do backend ao receber a requisição, porque o dispositivo não tem relógio confiável.
- **#6** Toque e foto são atômicos do ponto de vista de quem consulta: se a foto não puder ser guardada, o toque não aparece no histórico.
- **#7** Todo toque registrado é avisado aos clientes conectados (SPEC-002 #4).
- **#8** O dispositivo ignora novos acionamentos do botão por 5 s após um envio, para um toque não virar vários.
- **#9** Removida em 2026-11-13: retorno por LED saiu do MVP.

## Critérios de aceite

- **CA-1** Dado um JPEG válido de 300 KB e a chave correta, quando o dispositivo envia, então a resposta é 201 com `id` e `ringedAt`, e o toque aparece no histórico. Cobre #1, #2, #5.
- **CA-2** Dada a chave ausente ou errada, quando envia, então 401 e nenhum toque é criado. Cobre #1.
- **CA-3** Dada uma requisição sem o campo `photo`, então 400. Cobre #2.
- **CA-4** Dado um PNG enviado com `Content-Type: image/jpeg`, então 415. Cobre #3.
- **CA-5** Dado um JPEG de 2,5 MB, então 413. Cobre #4.
- **CA-6** Dado o MinIO indisponível, quando envia, então 503 e o histórico não muda. Cobre #6.
- **CA-7** Dado um cliente inscrito no stream, quando um toque é registrado, então ele recebe o evento `ring` com o mesmo `id`. Cobre #7.
- **CA-8** Dado o botão pressionado três vezes em 2 s, então o backend recebe um único toque. Cobre #8.

## Contrato de API

```
POST /api/v1/rings
X-Device-Key: <chave>
Content-Type: multipart/form-data; campo "photo"
```

Sucesso (201):

```json
{
  "id": "0193f1c2-7a4e-7c3b-9f00-1a2b3c4d5e6f",
  "ringedAt": "2026-11-23T19:42:10.123Z",
  "photoUrl": "/api/v1/rings/0193f1c2-7a4e-7c3b-9f00-1a2b3c4d5e6f/photo"
}
```

Erros (corpo Problem Details, `application/problem+json`):

| Status | Quando |
|---|---|
| 400 | Campo `photo` ausente ou vazio |
| 401 | Chave do dispositivo ausente ou inválida |
| 413 | Foto acima de 2 MB |
| 415 | Conteúdo não é JPEG |
| 503 | Armazenamento de fotos indisponível |

## Impacto em dados

Tabela nova de toques (expand, sem impacto em estrutura existente): identificador UUID v7, momento do toque, chave do objeto no armazenamento, tamanho em bytes. Índice por momento do toque, decrescente. Bucket `cam-node-photos` no MinIO.

## Interface

Sem tela.

## Riscos e decisões

- Armazenamento da foto e dos metadados: ADR-002.
- Autenticação por chave fixa: ADR-003.
- A foto é gravada no MinIO antes de inserir o toque no banco. Se o insert falhar, sobra um objeto órfão no bucket, custo aceito no MVP (sem transação aberta durante chamada de rede).

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-13 | Criação |
| 2026-11-13 | Removida a #9 (LED), aprovada |
