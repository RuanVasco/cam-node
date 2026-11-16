# SPEC-010: remoção de toques

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

Toda foto tirada na porta fica guardada para sempre. O morador não consegue apagar uma foto que não quer manter, seja por privacidade de quem tocou, seja por ser um toque de teste.

## Escopo

Remoção definitiva de um toque, com a foto, por um membro da campainha, refletida ao vivo em todas as telas abertas. Depende da SPEC-004; com a SPEC-005, fica restrita aos membros.

## Fora do escopo

- Lixeira ou desfazer.
- Remoção automática por idade (retenção).
- Remover toques de um período inteiro de uma vez.

## Regras de negócio

- **#1** Só um membro da campainha do toque pode removê-lo; para quem não é membro, o toque responde como inexistente (404). Antes da SPEC-005, qualquer usuário logado.
- **#2** A remoção é definitiva: o toque sai do histórico e a foto deixa de ser servida na hora, para todos.
- **#3** A foto é apagada também do armazenamento. Se ele estiver indisponível no momento, a remoção é aceita mesmo assim e o arquivo é apagado em até 24 h, sem nunca voltar a ser servido.
- **#4** Os membros com a tela aberta veem o toque sumir sem recarregar.
- **#5** A tela pede confirmação antes de remover.
- **#6** É possível selecionar vários toques e removê-los de uma vez, até 100 por vez.

## Critérios de aceite

- **CA-1** Dado um membro e um toque da sua campainha, quando remove, então 204, o toque some do histórico, `totalElements` diminui 1 e a foto responde 404. Cobre #2.
- **CA-2** Dado um usuário que não é membro, quando tenta remover, então 404 e o toque continua. Cobre #1.
- **CA-3** Dado o armazenamento fora do ar, quando removo, então 204, a foto responde 404 na hora, e com o armazenamento de volta o arquivo some em até 24 h. Cobre #3.
- **CA-4** Dados dois membros com a tela aberta, quando um remove um toque, então ele some na tela do outro em até 2 s. Cobre #4.
- **CA-5** Dada a confirmação cancelada, então nada é removido. Cobre #5.
- **CA-6** Dados 3 toques selecionados, quando confirmo, então os 3 somem. Dados 101, então 400 e nada é removido. Cobre #6.
- **CA-7** Dado o mesmo toque removido duas vezes, então a segunda resposta é 404. Cobre #2.

## Contrato de API

```
DELETE /api/v1/rings/{id}
```

204. Erros: 401 sem sessão, 403 sem token anti-CSRF, 404 toque inexistente ou de campainha de que o usuário não é membro.

```
POST /api/v1/rings/removal
{ "ids": ["0193f1c2-...", "..."] }
```

204, remove todos ou nenhum. Erros: 400 lista vazia ou com mais de 100, 404 algum id inexistente ou sem acesso (nenhum é removido).

Evento novo no stream, ignorado por clientes antigos:

```
event: ring-removed
data: {"id":"0193f1c2-..."}
```

## Impacto em dados

Linhas de toques apagadas. Fotos pendentes de remoção no armazenamento precisam ficar registradas até serem apagadas (tabela ou marcação, definida no plano). Expand puro.

## Interface

- Cada toque ganha a ação "Remover" (ícone de lixeira com rótulo acessível) na lista e na foto ampliada.
- Modo de seleção na lista, com "Remover selecionados (N)".
- Confirmação: "Remover este toque? A foto será apagada definitivamente." / "Remover N toques? As fotos serão apagadas definitivamente.", botões "Cancelar" e "Remover".
- Depois de remover: aviso "Toque removido." / "N toques removidos."

## Riscos e decisões

- A foto já baixada continua no cache do navegador de quem a viu (SPEC-005 #7); a remoção vale para o servidor e para as telas, não apaga cópias já feitas.
- Sem remoção pública: sem a SPEC-004, esta spec não é implementada.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
