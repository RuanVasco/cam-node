# SPEC-004: login de usuário

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

O histórico e as fotos da porta são públicos: qualquer pessoa com o endereço vê quem tocou na casa de alguém (ADR-003). Para uso real, só pessoas identificadas podem ver os toques.

## Escopo

Conta de usuário com dois jeitos de entrar, Google e e-mail com senha, e o histórico, as fotos e o stream passando a exigir login. Quais campainhas cada usuário vê é a SPEC-005.

## Fora do escopo

- Recuperação de senha por e-mail e confirmação de e-mail (exigem envio de e-mail).
- Outros provedores (Apple, Microsoft), autenticação em dois fatores.
- Exclusão de conta.

## Regras de negócio

- **#1** O histórico, as fotos e o stream de toques só respondem a usuário logado; o upload do dispositivo continua autenticado pela chave do dispositivo.
- **#2** O usuário entra com a conta Google ou com e-mail e senha.
- **#3** Um e-mail corresponde a uma conta só: entrar com o Google usando o e-mail de uma conta já criada com senha entra nessa mesma conta, e vice-versa.
- **#4** A senha tem pelo menos 8 caracteres e no máximo 128, sem exigência de tipos de caractere, e nunca é guardada nem registrada em log de forma legível.
- **#5** Erro de login não diz se o e-mail existe: e-mail inexistente e senha errada têm a mesma resposta.
- **#6** Depois de 5 tentativas erradas para o mesmo e-mail em 15 minutos, novas tentativas são recusadas até o fim da janela.
- **#7** A sessão dura 30 dias sem uso e termina ao sair; sair encerra também o stream aberto por ela.
- **#8** Requisição que altera dado, feita pelo navegador, exige o token anti-CSRF da sessão.

## Critérios de aceite

- **CA-1** Dado um visitante sem sessão, quando pede o histórico, uma foto ou o stream, então 401. Cobre #1.
- **CA-2** Dado o upload com a chave correta e sem sessão, então 201 (SPEC-001 inalterada). Cobre #1.
- **CA-3** Dado um cadastro com e-mail e senha de 8 caracteres, quando entro, então vejo o histórico. Cobre #2, #4.
- **CA-4** Dada uma conta criada com senha, quando entro pelo Google com o mesmo e-mail, então é a mesma conta (mesmo `id` em `/me`). Cobre #3.
- **CA-5** Dado e-mail inexistente e, em outra tentativa, senha errada, então as duas respostas são idênticas (status e corpo). Cobre #5.
- **CA-6** Dadas 5 senhas erradas em 15 min, quando tento a sexta, então 429, mesmo com a senha certa. Cobre #6.
- **CA-7** Dado um usuário logado com o stream aberto, quando sai, então o stream é encerrado e o histórico volta a responder 401. Cobre #7.
- **CA-8** Dado um POST autenticado sem o token anti-CSRF, então 403. Cobre #8.

## Contrato de API

| Método e rota | Entrada | Sucesso | Erros |
|---|---|---|---|
| `POST /api/v1/auth/register` | `{ "email", "password", "name" }` | 201, sessão iniciada | 400 dados inválidos, 409 e-mail já cadastrado |
| `POST /api/v1/auth/login` | `{ "email", "password" }` | 204, sessão iniciada | 401 credenciais inválidas, 429 bloqueado |
| `GET /api/v1/auth/google` | | 302 para o Google; na volta, 302 para `/` | 302 para `/entrar?erro=google` |
| `POST /api/v1/auth/logout` | | 204 | |
| `GET /api/v1/me` | | 200 `{ "id", "email", "name" }` | 401 |

Sessão em cookie `HttpOnly`, `Secure`, `SameSite=Lax`. Token anti-CSRF no cookie `XSRF-TOKEN`, devolvido no header `X-XSRF-TOKEN`. Rotas de leitura das SPEC-002 passam a responder 401 sem sessão.

## Impacto em dados

Tabela nova de usuários (e-mail único, nome, hash da senha opcional, identificador Google opcional) e de sessões. Expand puro.

## Interface

- Tela "Entrar": botão "Entrar com Google", campos e-mail e senha, link "Criar conta".
- Tela "Criar conta": nome, e-mail, senha.
- Mensagem de erro única: "E-mail ou senha inválidos." Bloqueio: "Muitas tentativas. Tente de novo em alguns minutos."
- Menu do usuário na barra superior com "Sair".

## Riscos e decisões

- Substitui a parte de leitura pública do ADR-003; a decisão de sessão por cookie (que também autentica o `EventSource`) vai para um ADR novo.
- Vincular contas pelo e-mail (#3) confia que o Google verificou o e-mail; aceito porque o Google só devolve e-mail verificado.
- Credenciais do OAuth do Google no Infisical.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
