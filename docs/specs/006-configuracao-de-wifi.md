# SPEC-006: configuração de WiFi pela rede própria

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

Hoje o nome e a senha do WiFi entram na compilação (SPEC-003 #1): trocar de rede exige computador e regravar o firmware, o que impede uma campainha de ser instalada por quem não é desenvolvedor.

## Escopo

Modo de configuração em que a campainha cria uma rede WiFi própria e serve uma página para escolher a rede da casa e digitar a senha, pelo celular, sem app. Substitui a SPEC-003 #1 para as credenciais do WiFi; endereço do backend e chave do dispositivo continuam na gravação.

## Fora do escopo

- Configuração por Bluetooth ou por app.
- Redes WPA2-Enterprise, com portal de login ou ocultas.
- Mais de uma rede salva.

## Regras de negócio

- **#1** Sem rede salva, a campainha entra no modo de configuração ao ligar.
- **#2** No modo de configuração, ela cria a rede `CamNode-XXXX` (os 4 últimos dígitos do MAC), protegida por senha própria da campainha, impressa no adesivo junto de um QR code que conecta o celular a ela.
- **#3** Conectado nessa rede, o celular abre a página de configuração sozinho (portal), em PT-BR, com a lista de redes encontradas e o campo de senha.
- **#4** A campainha testa a rede escolhida antes de salvar; se não conectar, a página mostra o motivo (rede não encontrada ou senha incorreta) e nada é salvo.
- **#5** Conectada com sucesso, a campainha salva a rede, desliga a rede própria e passa a funcionar normalmente.
- **#6** Rede salva indisponível (roteador desligado) não faz a campainha entrar sozinha no modo de configuração: ela continua tentando reconectar (SPEC-003 #7).
- **#7** Segurar o botão da campainha por 10 s apaga a rede salva e entra no modo de configuração.
- **#8** Sem nenhuma configuração concluída em 10 minutos, a rede própria é desligada; a campainha volta a ela ao religar ou pelo #7.
- **#9** A senha do WiFi nunca aparece no log nem na página depois de salva.
- **#10** Toques durante o modo de configuração são ignorados e registrados no log.

## Critérios de aceite

- **CA-1** Dada uma campainha recém-gravada, quando ligo, então a rede `CamNode-XXXX` aparece no celular e o QR do adesivo conecta nela. Cobre #1, #2.
- **CA-2** Dado o celular conectado na rede própria, então a página de configuração abre sem digitar endereço, em iPhone e Android. Cobre #3.
- **CA-3** Dada a senha errada, quando salvo, então a página mostra "Senha incorreta" e, religando a campainha, ela volta ao modo de configuração. Cobre #4.
- **CA-4** Dada a senha certa, quando salvo, então em até 30 s a rede própria some e um toque aparece no histórico. Cobre #5.
- **CA-5** Dada uma rede salva, quando desligo o roteador por 5 min e religo, então a rede própria não aparece em nenhum momento e o próximo toque é registrado. Cobre #6.
- **CA-6** Dado o botão segurado por 10 s, então a rede própria aparece e a rede antiga não é mais usada. Cobre #7.
- **CA-7** Dados 10 min sem configuração, então a rede própria some. Cobre #8.
- **CA-8** Dado o log serial de uma configuração completa, então a senha digitada não aparece. Cobre #9.

## Contrato de API

Nenhuma mudança no backend. A página de configuração é servida pela própria campainha.

## Impacto em dados

Nenhum no backend. Na campainha, rede e senha salvas na memória não volátil.

## Interface

Página no celular: título "Configurar campainha", lista de redes com força do sinal, campo "Senha", botão "Conectar"; estados "Conectando...", "Conectado. Pode fechar esta página." e as mensagens de erro do #4.

## Riscos e decisões

- Rede própria com portal escolhida em vez de Bluetooth (exige app de terceiros) e de WiFi Direct (não suportado pelo ESP32).
- A senha da rede própria por campainha precisa ser gerada e impressa na gravação: entra no mesmo comando de administração da SPEC-005.
- Interação com a SPEC-008: no modo de configuração a campainha não dorme, daí o limite do #8.
- O #7 usa o mesmo botão do toque: o toque é registrado no aperto (SPEC-007), e segurar 10 s depois disso apaga a rede.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
