# SPEC-007: campainha real

| Campo | Valor |
|---|---|
| Status | rascunho |
| Entrega | fora do MVP |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

O protótipo dispara pelo botão BOOT da placa. Na porta, o visitante aperta o botão de uma campainha comum, que já toca o aviso dentro de casa; a foto precisa sair desse aperto, sem perder a campainha que já funciona.

## Escopo

Ligação do botão de uma campainha sem fio existente (transmissor a pilha na porta e aviso de dentro) à placa, com a foto tirada no instante do aperto. O aviso de dentro é o original da campainha, reaproveitado.

## Fora do escopo

- Construir ou acionar o aviso de dentro pela placa.
- Campainhas com fio.

## Regras de negócio

- **#1** O aperto do botão da campainha existente gera o toque, com as mesmas regras de debounce e intervalo entre toques (SPEC-003 #3, SPEC-001 #8).
- **#2** A foto é tirada no instante em que o botão é pressionado, não quando é solto: quem segura o botão por muito tempo já foi fotografado no primeiro instante.
- **#3** Segurar o botão por qualquer tempo gera um único toque.
- **#4** O aviso de dentro continua tocando como antes, inclusive com a placa desligada, sem bateria ou travada.
- **#5** A placa fica eletricamente isolada do circuito do transmissor, que tem pilha própria.
- **#6** O botão BOOT da placa deixa de gerar toque.

## Critérios de aceite

- **CA-1** Dado um aperto curto no botão da campainha, então o aviso de dentro toca e o toque aparece no histórico. Cobre #1, #4.
- **CA-2** Dado alguém que aperta e segura o botão por 10 s enquanto se afasta da câmera, então há um toque só e a foto mostra a pessoa na posição em que apertou. Cobre #2, #3.
- **CA-3** Dado o log serial, então o intervalo entre o aperto e a captura é de no máximo 300 ms. Cobre #2.
- **CA-4** Dada a placa desligada ou sem bateria, quando aperto o botão, então o aviso de dentro toca. Cobre #4.
- **CA-5** Dado um aperto no BOOT, então nenhum toque é registrado. Cobre #6.
- **CA-6** Dada a pilha do transmissor e a alimentação da placa medidas com multímetro, então não há continuidade entre os dois circuitos. Cobre #5.

## Contrato de API

Nenhuma mudança.

## Impacto em dados

Nenhum.

## Interface

Sem tela. Log serial com o instante do aperto e o da captura.

## Riscos e decisões

- Ligação por optoacoplador acionado pelo próprio circuito do transmissor quando o botão fecha: o transmissor segue funcionando sozinho (#4) e a placa só enxerga o aperto (#5). Detalhe elétrico no plano, a partir do transmissor aberto.
- Descartado: receptor de 433 MHz na placa escutando o transmissor. Dispensa fio, mas o receptor ligado o tempo todo impede o sono profundo da SPEC-008, e o sinal chega depois do aperto.
- O pino do botão é escolhido entre os livres da GOOUUU ESP32-S3-CAM; se a SPEC-008 for adiante, precisa ser um pino capaz de acordar a placa do sono profundo.
- O #6 (sem o BOOT) evita toque acidental ao manusear a placa; o BOOT continua servindo para gravação.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
| 2026-11-16 | Campainha sem fio: ligação por optoacoplador no transmissor |
