# SPEC-011: retorno sonoro ao visitante

| Campo | Valor |
|---|---|
| Status | aprovada |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | 2026-11-16 |

## Problema

Na rede do IF um envio pode levar dezenas de segundos (SPEC-003, Riscos e decisões). Sem retorno, o visitante não sabe se o aperto contou nem se o toque chegou, e aperta de novo ou vai embora.

## Escopo

Um buzzer na campainha que avisa o visitante, por som, que o aperto foi aceito e qual foi o resultado do envio.

## Fora do escopo

- LED ou qualquer retorno visual (SPEC-001).
- Som dentro da casa (campainha interna, SPEC-007).
- Volume, melodia ou padrão configurável em tempo de execução.

## Regras de negócio

- **#1** O aperto aceito (passou pelo debounce da SPEC-003 #3 e pelo intervalo da SPEC-001 #8) gera um bipe curto logo em seguida, antes do resultado do envio.
- **#2** O bipe do aperto não atrasa a captura da foto em mais de 50 ms (SPEC-003 #4).
- **#3** Envio aceito pelo backend (2xx) gera dois bipes curtos.
- **#4** Toque perdido (sem WiFi, falha na captura, foto grande demais ou falha depois da última tentativa) gera um bipe longo.
- **#5** Aperto ignorado pelo intervalo não gera som.
- **#6** O pino do buzzer é definido na compilação. Sem pino definido, a campainha funciona igual, sem som.

## Critérios de aceite

- **CA-1** Dado o dispositivo conectado e o backend no ar, quando aperto o botão, então ouço um bipe curto em seguida e, quando o toque aparece na tela, dois bipes curtos. Cobre #1, #3.
- **CA-2** Dado o backend fora do ar, quando aperto o botão, então ouço um bipe curto e, depois das duas tentativas, um bipe longo. Cobre #1, #4.
- **CA-3** Dado o WiFi desligado, quando aperto o botão, então ouço um bipe curto e um bipe longo. Cobre #4.
- **CA-4** Dado um toque recém-enviado, quando aperto de novo antes de 5 s, então não há som. Cobre #5.
- **CA-5** Dada a compilação sem pino de buzzer, quando aperto o botão, então o toque é registrado como antes. Cobre #6.
- **CA-6** Dado um padrão de bipes, então as durações e pausas de cada padrão são as definidas no plano, verificadas por teste unitário. Cobre #1, #3, #4.

## Contrato de API

Nenhum. O backend não muda.

## Impacto em dados

Nenhum.

## Interface

Som: bipe curto no aperto, dois bipes curtos no sucesso, bipe longo na falha. Log serial sem mudança.

## Riscos e decisões

- Buzzer ativo (toca sozinho ao receber tensão) ligado entre um GPIO livre e o GND. Um GPIO do ESP32-S3 fornece até cerca de 20 mA com folga; buzzer que consome mais vai por módulo com transistor.
- O bipe não bloqueia o laço: o envio continua enquanto o padrão toca.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
