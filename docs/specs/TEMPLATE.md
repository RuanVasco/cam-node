# SPEC-NNN: título curto

| Campo | Valor |
|---|---|
| Status | rascunho / aprovada / implementada / substituída |
| Entrega | MVP / fora do MVP |
| Autor | |
| Criada em | AAAA-MM-DD |
| Aprovada em | |

## Problema

Qual dor existe hoje e para quem. Uma ou duas frases, sem solução.

## Escopo

O que esta funcionalidade entrega.

## Fora do escopo

O que explicitamente não entra. Esta seção evita a maior parte das discussões de revisão.

## Regras de negócio

Numeradas, estáveis e verificáveis. O número nunca é reaproveitado.

- **#1** Descrição da regra em uma frase, no comportamento observável.
- **#2** Origem externa quando houver (norma, decisão da área, exigência legal).
- **#3** Condição de exceção, se existir.

## Critérios de aceite

Cada critério tem que ser verificável por um teste. Um critério cobre uma ou mais regras.

- **CA-1** Dado que <estado inicial>, quando <ação>, então <resultado observável>. Cobre #1.
- **CA-2** Dado que <estado inicial>, quando <ação inválida>, então <erro esperado e código de status>. Cobre #2.

## Contrato de API

Endpoints, formato de entrada, formato de saída, erros e códigos de status. Campo novo entra opcional.

```
POST /api/v1/recurso
```

Entrada:

```json
{}
```

Sucesso (201):

```json
{}
```

Erros:

| Status | Quando | Corpo |
|---|---|---|
| 400 | | |
| 404 | | |
| 409 | | |

## Impacto em dados

Tabelas e colunas afetadas. Se há migration, indicar a etapa: expand, migrate ou contract, e o que fica para o release seguinte.

## Interface

Telas afetadas e comportamento esperado. Textos em PT-BR.

## Riscos e decisões

Decisão tomada, alternativa descartada e o motivo. Decisão que atravessa várias funcionalidades vai para `docs/adr/`, não aqui.

## Histórico

| Data | Mudança |
|---|---|
| AAAA-MM-DD | Criação |
