# SPEC-009: app Android com notificação push

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

O morador só fica sabendo de um toque se estiver com a página aberta. Longe de casa, ou com o celular no bolso, ele perde quem tocou.

## Escopo

App Android com login, campainhas, histórico e notificação push a cada toque, com a foto. Depende das SPEC-004 e SPEC-005.

## Fora do escopo

- iPhone e push no navegador (Web Push).
- Silenciar notificações por campainha ou por horário dentro do app (o Android já permite desligar o canal).
- Publicação na Play Store: o app é instalado por APK na apresentação.

## Regras de negócio

- **#1** O app entra com Google ou e-mail e senha, nas mesmas contas da web (SPEC-004).
- **#2** O app mostra as campainhas do usuário e o histórico com as mesmas regras da web (SPEC-002, SPEC-005), atualizando ao vivo com o app aberto.
- **#3** A cada toque, todos os membros da campainha com o app instalado e logado recebem uma notificação, com o app aberto, em segundo plano ou fechado.
- **#4** A notificação mostra o nome da campainha, o horário e a foto do toque.
- **#5** A foto nunca trafega pelo serviço de push nem fica acessível sem sessão: o app baixa a foto com a sessão do usuário ao receber o aviso.
- **#6** Tocar na notificação abre o toque com a foto ampliada.
- **#7** Sair da conta no app para as notificações daquele celular.
- **#8** Sem permissão de notificação, o app funciona e mostra um aviso com o atalho para conceder a permissão.
- **#9** A campainha pode ser adicionada lendo o QR code de pareamento com a câmera do celular (SPEC-005 #2).
- **#10** O app roda do Android 8.0 em diante.

## Critérios de aceite

- **CA-1** Dado o app fechado e o celular com internet, quando há um toque, então a notificação com a foto chega em até 10 s. Cobre #3, #4, #5.
- **CA-2** Dados dois usuários membros da mesma campainha, cada um com o app, quando há um toque, então os dois são notificados; um terceiro usuário sem vínculo não é. Cobre #3.
- **CA-3** Dada a notificação, quando toco nela, então o app abre no toque, com a foto ampliada. Cobre #6.
- **CA-4** Dado o usuário que saiu da conta, quando há um toque, então aquele celular não é notificado. Cobre #7.
- **CA-5** Dada a permissão negada, então o histórico funciona e o aviso de permissão aparece. Cobre #8.
- **CA-6** Dado o QR do adesivo, quando leio pelo app, então a campainha entra na lista. Cobre #9.
- **CA-7** Dado o conteúdo enviado ao serviço de push, então ele não contém a foto nem endereço que dê acesso a ela sem sessão. Cobre #5.
- **CA-8** Dado um emulador com Android 8.0, então login, histórico e notificação funcionam. Cobre #10.

## Contrato de API

| Método e rota | Entrada | Sucesso | Erros |
|---|---|---|---|
| `POST /api/v1/auth/google/token` | `{ "idToken" }` (login Google feito no app) | 204, sessão iniciada | 401 token inválido ou emitido para outro app |
| `POST /api/v1/push-tokens` | `{ "token", "platform": "android" }` | 204 | 400, 401 |
| `DELETE /api/v1/push-tokens/{token}` | | 204 | 401 |

Aviso enviado ao celular (somente dados): `{ "type": "ring", "ringId", "doorbellId", "doorbellName", "ringedAt" }`. Token recusado pelo serviço de push como inválido é descartado pelo backend.

## Impacto em dados

Tabela nova de tokens de push por usuário. Expand puro.

## Interface

Telas: Entrar, Minhas campainhas (com "Adicionar campainha" por QR ou código), Histórico, Toque ampliado. Notificação: título "<nome da campainha>", texto "Alguém tocou às HH:mm", imagem da foto.

## Riscos e decisões

- Serviço de push: Firebase Cloud Messaging, o único que entrega com o app fechado no Android. Vira ADR.
- App em Flutter: um código só, com plugins mantidos pelo Google para Firebase e login Google. Vira ADR junto com o do push. Nativo (Kotlin) descartado por ser mais código para as mesmas telas; Capacitor descartado por depender de plugins de terceiros para notificação e câmera.
- Android 8.0 (API 26) como mínimo: é onde os canais de notificação passam a existir, cobre quase todos os aparelhos em uso e fica acima do mínimo exigido pelo Firebase.
- O login Google no app usa o fluxo nativo do Android e entrega um token ao backend (rota nova acima), em vez do redirecionamento da web (SPEC-004).
- Fabricantes que matam apps em segundo plano (Xiaomi, Samsung) podem atrasar o aviso; o CA-1 é verificado no celular da apresentação.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
| 2026-11-16 | Flutter, Android 8.0 como mínimo, rota de login Google por token |
