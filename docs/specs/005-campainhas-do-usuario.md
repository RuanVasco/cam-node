# SPEC-005: campainhas do usuário

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

Com mais de uma campainha no sistema, cada pessoa deve ver só os toques das campainhas da sua casa, e uma casa tem vários moradores.

## Escopo

Relação n:n entre usuários e campainhas: vínculo por código de pareamento, histórico e stream filtrados pelas campainhas do usuário, e chave própria para cada dispositivo. Depende da SPEC-004.

## Fora do escopo

- Papéis diferentes entre membros (dono, convidado): todo membro tem os mesmos direitos.
- Convite por e-mail.
- Cadastro de campainha nova pela interface: é feito por comando de administração, junto da gravação do firmware.

## Regras de negócio

- **#1** Cada campainha tem chave do dispositivo própria; o upload registra o toque na campainha dona da chave. Chave de uma campainha não registra toque em outra.
- **#2** O usuário se torna membro de uma campainha informando o código de pareamento dela (impresso no adesivo do case).
- **#3** Uma campainha pode ter vários membros e um usuário pode ser membro de várias campainhas.
- **#4** O histórico, as fotos e o stream mostram só toques das campainhas de que o usuário é membro. Toque de outra campainha responde como inexistente (404), sem revelar que existe.
- **#5** Qualquer membro pode renomear a campainha, gerar um novo código de pareamento (o anterior deixa de valer na hora) e sair dela.
- **#6** Sair de uma campainha não apaga os toques; a campainha sem membros continua registrando toques e pode ser vinculada de novo pelo código vigente.
- **#7** A foto não pode ficar em cache compartilhado: o cache passa a ser só do navegador do usuário.
- **#8** A chave do dispositivo é guardada só como hash e pode ser trocada pelo comando de administração, invalidando a anterior.

## Critérios de aceite

- **CA-1** Dadas as campainhas A e B, quando A envia um toque com a sua chave, então ele aparece no histórico de A e não no de B. Cobre #1, #4.
- **CA-2** Dado um usuário sem campainhas, quando informa o código de A, então A aparece na lista dele e o histórico mostra os toques de A. Cobre #2.
- **CA-3** Dados os usuários U1 e U2 membros de A, e U2 também de B, então U1 vê só A e U2 vê A e B. Cobre #3, #4.
- **CA-4** Dado U1 membro só de A, quando pede a foto de um toque de B, então 404. Cobre #4.
- **CA-5** Dado um código gerado de novo, quando alguém usa o anterior, então 404. Cobre #5.
- **CA-6** Dado o último membro saindo de A, quando A recebe um toque e alguém se vincula pelo código, então essa pessoa vê o toque. Cobre #6.
- **CA-7** Dada a foto de um toque, então `Cache-Control: private, max-age=31536000, immutable`. Cobre #7.
- **CA-8** Dado um código errado informado 10 vezes em 15 min pelo mesmo usuário, então a 11ª tentativa responde 429. Cobre #2.

## Contrato de API

| Método e rota | Entrada | Sucesso | Erros |
|---|---|---|---|
| `GET /api/v1/doorbells` | | 200 `[ { "id", "name", "memberCount" } ]` | 401 |
| `POST /api/v1/doorbells/pair` | `{ "pairingCode" }` | 201 `{ "id", "name" }` | 404 código inválido, 409 já é membro, 429 |
| `PATCH /api/v1/doorbells/{id}` | `{ "name" }` | 200 | 400, 404 |
| `POST /api/v1/doorbells/{id}/pairing-code` | | 200 `{ "pairingCode" }` | 404 |
| `DELETE /api/v1/doorbells/{id}/membership` | | 204 | 404 |

`GET /api/v1/rings` ganha o filtro opcional `doorbellId`; cada item e cada evento do stream ganham `doorbellId` e `doorbellName`. Campos novos, contrato da SPEC-002 preservado.

## Impacto em dados

Tabelas novas de campainhas (nome, hash da chave, código de pareamento) e de membros. Coluna `doorbell_id` em toques: expand nullable, a campainha atual (chave `DEVICE_API_KEY`) vira a primeira campainha e recebe os toques existentes; `not null` em release seguinte.

## Interface

- Lista "Minhas campainhas" com o botão "Adicionar campainha" (campo do código).
- Histórico com filtro por campainha quando o usuário tem mais de uma; cada toque mostra o nome da campainha.
- Na campainha: "Renomear", "Gerar novo código" (mostra o código e o QR) e "Sair desta campainha", com confirmação.

## Riscos e decisões

- Código de pareamento é segredo de quem tem acesso físico ao adesivo; vazou, qualquer membro gera outro (#5). Tamanho mínimo e formato definidos no plano.
- Chave por dispositivo substitui a chave única do ADR-003: ADR novo junto com o da SPEC-004.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
