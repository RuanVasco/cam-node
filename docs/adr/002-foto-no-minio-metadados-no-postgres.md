# ADR-002: foto no MinIO, metadados no Postgres

| Campo | Valor |
|---|---|
| Status | aceita |
| Data | 2026-11-13 |

## Contexto

Cada toque gera um JPEG de algumas centenas de KB e um registro pequeno (momento e referência da foto). O homelab já oferece Postgres no LXC `db` e MinIO na VM `utils`, ambos com backup e com o padrão de um usuário e um bucket por app gerenciado pelo Ansible do `infra`.

## Decisão

Guardamos a foto como objeto no bucket `cam-node-photos` do MinIO, acessado pela API S3, e os metadados do toque numa tabela do Postgres, com a chave do objeto. O acesso ao MinIO fica atrás de uma porta de armazenamento de fotos.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Foto em coluna `bytea` no Postgres | Infla o banco e o backup dele; o homelab já tem storage de objetos. |
| Disco local do pod | Estado no processo, perdido em reagendamento do pod, sem backup. |
| Só MinIO, sem banco | Paginação e ordenação do histórico ficariam sobre listagem de objetos, frágil. |

## Consequências

- Banco pequeno e consultas de histórico simples; fotos com backup do MinIO.
- Duas dependências externas na escrita, sem transação entre elas: grava a foto antes do registro, e um insert que falha deixa objeto órfão.
- Testes de integração precisam de MinIO em Testcontainers.

## Revisão

Se objetos órfãos crescerem a ponto de importar, ou se houver retenção de fotos: adicionar limpeza periódica que cruze bucket e tabela.
