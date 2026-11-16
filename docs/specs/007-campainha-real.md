# SPEC-007: campainha real

| Campo | Valor |
|---|---|
| Status | rascunho |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

O protótipo dispara pelo botão BOOT da placa. Na porta, o visitante aperta o botão de uma campainha comum, que já toca o aviso dentro de casa; a foto precisa sair desse aperto, sem perder a campainha que já funciona.

## Escopo

Ligação do botão de uma campainha existente (botão de fora e aviso de dentro) à placa, com a foto tirada no instante do aperto. O aviso de dentro é o original da campainha, reaproveitado.

## Fora do escopo

- Construir ou acionar o aviso de dentro pela placa.
- Ligação à rede elétrica de 127 V ou 220 V pela placa.

## Regras de negócio

- **#1** O aperto do botão da campainha existente gera o toque, com as mesmas regras de debounce e intervalo entre toques (SPEC-003 #3, SPEC-001 #8).
- **#2** A foto é tirada no instante em que o botão é pressionado, não quando é solto: quem segura o botão por muito tempo já foi fotografado no primeiro instante.
- **#3** Segurar o botão por qualquer tempo gera um único toque.
- **#4** O aviso de dentro continua tocando como antes, inclusive com a placa desligada, sem bateria ou travada.
- **#5** A placa fica eletricamente isolada do circuito da campainha sempre que ele usa tensão diferente da placa.
- **#6** O botão BOOT da placa deixa de gerar toque.

## Critérios de aceite

- **CA-1** Dado um aperto curto no botão da campainha, então o aviso de dentro toca e o toque aparece no histórico. Cobre #1, #4.
- **CA-2** Dado alguém que aperta e segura o botão por 10 s enquanto se afasta da câmera, então há um toque só e a foto mostra a pessoa na posição em que apertou. Cobre #2, #3.
- **CA-3** Dado o log serial, então o intervalo entre o aperto e a captura é de no máximo 300 ms. Cobre #2.
- **CA-4** Dada a placa desligada, quando aperto o botão, então o aviso de dentro toca. Cobre #4.
- **CA-5** Dado um aperto no BOOT, então nenhum toque é registrado. Cobre #6.

## Contrato de API

Nenhuma mudança.

## Impacto em dados

Nenhum.

## Interface

Sem tela. Log serial com o instante do aperto e o da captura.

## Riscos e decisões

- O tipo da campainha define a ligação: sem fio a pilha (contato do botão do transmissor), com fio de baixa tensão (optoacoplador) ou com fio na rede elétrica (não suportado, #5). Definido no plano a partir do modelo.
- O pino do botão é escolhido entre os livres da GOOUUU ESP32-S3-CAM; se a SPEC-008 for adiante, precisa ser um pino capaz de acordar a placa do sono profundo.
- O #6 (sem o BOOT) evita toque acidental ao manusear a placa; o BOOT continua servindo para gravação.

## Em aberto antes da aprovação

| Pergunta | Por que importa |
|---|---|
| Modelo da campainha (foto ou link) e se é sem fio, com fio de baixa tensão ou ligada na tomada | Define a ligação elétrica e se o #5 exige optoacoplador |

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
