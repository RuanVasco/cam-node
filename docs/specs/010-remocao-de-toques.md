# SPEC-010: exclusão de toques e lixeira

| Campo | Valor |
|---|---|
| Status | aprovada |
| Entrega | MVP |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | 2026-11-16 |

## Problema

Toda foto tirada na porta fica guardada para sempre. O morador não consegue apagar uma foto que não quer manter, seja por privacidade de quem tocou, seja por ser um toque de teste. Uma exclusão feita por engano, ou por quem não devia, não tem volta.

## Escopo

Exclusão de toques pela tela do histórico, que os leva para uma lixeira em vez de apagá-los, refletida ao vivo em todas as telas abertas. A lixeira tem página própria, protegida por senha de administrador enquanto não houver login (SPEC-004), onde o toque pode ser restaurado ou apagado de vez, com a foto.

## Fora do escopo

- Desfazer logo depois de excluir, na própria tela do histórico.
- Esvaziar a lixeira de uma vez, ou restaurar e apagar de vez vários toques juntos.
- Remoção automática por idade (retenção).
- Remover toques de um período inteiro de uma vez.

## Regras de negócio

- **#1** Com a SPEC-005, só um membro da campainha do toque pode removê-lo; para quem não é membro, o toque responde como inexistente (404). Com a SPEC-004 e sem a SPEC-005, qualquer usuário logado.
- **#2** Removida: excluir leva à lixeira (#9); só apagar de vez (#12) é definitivo.
- **#3** Ao apagar de vez, a foto é apagada também do armazenamento. Se ele estiver indisponível no momento, a remoção é aceita mesmo assim e o arquivo é apagado em até 24 h, sem nunca voltar a ser servido.
- **#4** Os membros com a tela aberta veem o toque sumir sem recarregar.
- **#5** A tela pede confirmação antes de remover.
- **#6** É possível selecionar vários toques e removê-los de uma vez, até 100 por vez.
- **#7** Enquanto não houver login (SPEC-004), a lixeira (#10, #11, #12) exige a senha de administrador; sem ela ou com ela errada, nada é mostrado nem alterado. A página pede a senha ao abrir e a guarda só até a aba ser fechada. Excluir pelo histórico não pede senha.
- **#8** Depois de 5 senhas de administrador erradas em 15 minutos, novos acessos à lixeira são recusados até o fim da janela, mesmo com a senha certa.
- **#9** Excluir leva o toque para a lixeira: ele sai do histórico, da contagem e das telas abertas na hora, e a foto deixa de ser servida pela rota pública. Para quem excluiu, o toque foi excluído; a tela não menciona a lixeira.
- **#10** A lixeira fica em uma página própria, "Lixeira", sem link na tela do histórico. Ela lista os toques excluídos, do excluído mais recentemente para o mais antigo, com a foto, o momento do toque e o da exclusão.
- **#11** Restaurar devolve o toque ao histórico, no lugar do momento em que tocou, com a mesma foto, e ele reaparece nas telas abertas sem recarregar.
- **#12** Apagar de vez só existe na lixeira, pede confirmação e remove o toque e a foto definitivamente (#3).

## Critérios de aceite

- **CA-1** Dado um toque no histórico, quando qualquer pessoa o exclui, sem senha, então 204, ele some do histórico, `totalElements` diminui 1 e a foto pública responde 404. Cobre #9.
- **CA-2** Dado um usuário que não é membro (SPEC-005), quando tenta excluir, então 404 e o toque continua. Cobre #1.
- **CA-3** Dado o armazenamento fora do ar, quando apago de vez, então 204, a foto responde 404 na hora, e com o armazenamento de volta o arquivo some em até 24 h. Cobre #3, #12.
- **CA-4** Dadas duas telas abertas, quando um toque é excluído em uma, então ele some na outra em até 2 s; quando é restaurado, reaparece na posição do seu horário em até 2 s. Cobre #4, #11.
- **CA-5** Dada a confirmação cancelada, ao excluir ou ao apagar de vez, então nada muda. Cobre #5, #12.
- **CA-6** Dados 3 toques selecionados, quando confirmo a exclusão, então os 3 somem. Dados 101, então 400 e nada é excluído. Cobre #6.
- **CA-7** Dado um toque já excluído, quando é excluído de novo, então 404. Dado um toque apagado de vez, então restaurar ou apagar de novo responde 404. Cobre #9, #12.
- **CA-8** Dado o acesso à lixeira sem a senha ou com a senha errada, então 401 e nada é listado nem alterado. Cobre #7.
- **CA-9** Dadas 5 senhas erradas em 15 min, quando tento a sexta, então 429, mesmo com a senha certa. Cobre #8.
- **CA-10** Dada a senha informada na lixeira, quando restauro ou apago outro toque na mesma aba, então a senha não é pedida de novo; numa aba nova, é. Cobre #7.
- **CA-11** Dados dois toques excluídos, então a lixeira lista primeiro o excluído por último, com a foto e os dois horários. Cobre #10.
- **CA-12** Dado um toque restaurado, então ele volta ao histórico com o mesmo id, o mesmo `ringedAt` e a mesma foto, e sai da lixeira. Cobre #11.
- **CA-13** Dada a tela do histórico, então não há link para a lixeira nem menção a ela na confirmação ou no aviso de exclusão. Cobre #9, #10.

## Contrato de API

Exclusão, pública (o header `X-Admin-Key`, se vier, é ignorado):

```
DELETE /api/v1/rings/{id}
POST   /api/v1/rings/removal   { "ids": ["0193f1c2-...", "..."] }
```

204; o lote exclui todos ou nenhum. Erros: 400 lista vazia ou com mais de 100, 404 toque inexistente, já excluído ou sem acesso (no lote, nenhum é excluído).

Lixeira: todas as rotas exigem o header `X-Admin-Key` com a senha de administrador (segredo `ADMIN_API_KEY`). Com a SPEC-004, o header sai e entram a sessão e o token anti-CSRF. Erros comuns: 401 senha ausente ou errada, 429 bloqueado pelo #8.

```
GET /api/v1/trash?page=0&size=20
{ "content": [ { "id": "0193f1c2-...", "ringedAt": "2026-11-23T19:42:10.123Z", "removedAt": "2026-11-23T20:01:00.000Z", "photoUrl": "/api/v1/trash/0193f1c2-.../photo" } ],
  "page": 0, "size": 20, "totalElements": 1 }

GET    /api/v1/trash/{id}/photo     200 image/jpeg; 404 fora da lixeira; 503 armazenamento indisponível
POST   /api/v1/trash/{id}/restore   204; 404 fora da lixeira
DELETE /api/v1/trash/{id}           204; 404 fora da lixeira
```

Eventos no stream, ignorados por clientes antigos:

```
event: ring-removed
data: {"id":"0193f1c2-..."}

event: ring-restored
data: {"id":"0193f1c2-...","ringedAt":"2026-11-23T19:42:10.123Z","photoUrl":"/api/v1/rings/0193f1c2-.../photo"}
```

`ring-removed` vale para excluir e para apagar de vez; o stream não diz quando o toque está na lixeira.

## Impacto em dados

Toques ganham o momento da exclusão, nulo enquanto estão no histórico (coluna nova nullable, expand puro). Apagar de vez remove a linha e usa o registro de fotos pendentes que já existe. Toques removidos antes desta mudança já foram apagados e não aparecem na lixeira.

## Interface

- Histórico: cada toque tem a ação "Excluir" (ícone de lixeira com rótulo acessível), na lista e na foto ampliada, e o modo de seleção tem "Excluir selecionados (N)". Confirmação: "Excluir este toque?" / "Excluir N toques?", botões "Cancelar" e "Excluir". Aviso: "Toque excluído." / "N toques excluídos." Nenhuma senha.
- Lixeira (`/lixeira`): título "Lixeira", diálogo "Senha de administrador" ao abrir (campo de senha, "Cancelar" e "Confirmar"; "Senha incorreta."; "Muitas tentativas. Tente de novo em alguns minutos."). Cada toque mostra a foto, "Tocou em" e "Excluído em", e as ações "Restaurar" e "Apagar de vez". Confirmação de apagar: "Apagar este toque de vez? A foto será apagada definitivamente.", botões "Cancelar" e "Apagar". Avisos: "Toque restaurado." / "Toque apagado." Lixeira vazia: "A lixeira está vazia."

## Riscos e decisões

- A foto já baixada continua no cache do navegador de quem a viu (SPEC-005 #7); a remoção vale para o servidor e para as telas, não apaga cópias já feitas.
- Exclusão pública sem senha (#9): qualquer pessoa com o endereço pode esvaziar o histórico. Aceito porque nada se perde: o administrador restaura pela lixeira. Alternativa descartada: senha para excluir, que prendia a tela do morador a um segredo compartilhado.
- Antes do login, a senha de administrador (#7) é o contorno temporário para proteger a lixeira; sai quando a SPEC-004 for implementada.
- A foto da lixeira exige o header da senha, então a página baixa a imagem pelo serviço da feature em vez de usar o endereço direto na tag de imagem.
- A senha vai em header, não em cookie, então não há risco de CSRF nessa fase.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
| 2026-11-16 | #7 e #8: senha de administrador enquanto não houver login; aprovada |
| 2026-11-16 | Lixeira: excluir passa a ser público e reversível (#9 a #12); #2 removida; #3, #7 e #8 passam a valer para a lixeira; aprovada |
