# SPEC-008: energia solar e bateria

| Campo | Valor |
|---|---|
| Status | rascunho |
| Entrega | fora do MVP |
| Autor | Ruan Vasconcelos |
| Criada em | 2026-11-16 |
| Aprovada em | |

## Problema

A campainha depende de um cabo USB ligado na tomada, o que limita onde ela pode ser instalada. Na porta, normalmente não há tomada.

## Escopo

Campainha alimentada por bateria recarregável com painel solar, dormindo entre toques para economizar energia, e o nível da bateria visível para os membros.

## Fora do escopo

- Previsão de autonomia ou histórico de carga em gráfico.
- Aviso de bateria fraca por push (entra na SPEC-009 se aprovada).
- Funcionamento sem bateria e sem sol: sem energia, a campainha não registra toques; o aviso de dentro segue funcionando (SPEC-007 #4).

## Regras de negócio

- **#1** Entre toques, a campainha fica em sono profundo e acorda pelo aperto do botão.
- **#2** Ao acordar por um aperto, a foto é tirada antes de conectar ao WiFi, em até 1,5 s após o aperto. O limite de 300 ms da SPEC-007 vale para a campainha acordada.
- **#3** O toque aparece no histórico em até 10 s após o aperto, com WiFi e backend disponíveis.
- **#4** Cada toque leva junto a tensão da bateria e se ela está carregando.
- **#5** Sem toques, a campainha acorda sozinha a cada 6 h só para informar o estado da bateria.
- **#6** A tela mostra o nível da bateria de cada campainha em porcentagem, calculado no backend a partir da tensão.
- **#7** Abaixo de 20%, a campainha aparece com o aviso "Bateria fraca".
- **#8** Sem notícia da campainha por mais de 13 h (dois relatos perdidos), ela aparece como "Sem comunicação".
- **#9** Com a bateria abaixo do mínimo seguro da célula, a campainha não tenta usar WiFi e volta a dormir até o painel recarregar.
- **#10** Autonomia mínima de 7 dias sem sol com 10 toques por dia.
- **#11** A bateria não carrega fora da faixa de 0 °C a 45 °C, medida junto dela; fora da faixa, a campainha segue funcionando com a carga que tem.
- **#12** A temperatura da bateria vai junto de cada relato, e acima de 45 °C a campainha aparece com o aviso "Bateria quente".

## Critérios de aceite

- **CA-1** Dada a campainha dormindo, quando aperto o botão, então o log mostra a captura em até 1,5 s e o toque aparece em até 10 s. Cobre #1, #2, #3.
- **CA-2** Dado um toque, então a campainha na tela mostra a porcentagem correspondente à tensão enviada. Cobre #4, #6.
- **CA-3** Dada a campainha sem toques por 6 h, então o backend recebeu um relato de estado nesse período. Cobre #5.
- **CA-4** Dada a tensão enviada equivalente a 15%, então a tela mostra "Bateria fraca". Cobre #7.
- **CA-5** Dado o último contato há 14 h, então a tela mostra "Sem comunicação". Cobre #8.
- **CA-6** Dada uma fonte de bancada abaixo do mínimo seguro no lugar da bateria, quando aperto o botão, então nenhuma conexão WiFi é tentada (log). Cobre #9.
- **CA-7** Dado o consumo médio medido em sono e por toque, então a conta de autonomia com a capacidade da bateria dá pelo menos 7 dias. Cobre #10.
- **CA-8** Dado o sensor de temperatura aquecido acima de 45 °C (soprador térmico, com a bateria afastada), então a corrente de carga cai a zero e a tela mostra "Bateria quente". Cobre #11, #12.

## Contrato de API

`POST /api/v1/rings` (SPEC-001) ganha os campos opcionais `batteryMillivolts` (inteiro), `charging` (booleano) e `batteryCelsius` (decimal) no multipart; sem eles, o toque é aceito como antes.

```
POST /api/v1/devices/status
X-Device-Key: <chave>
{ "batteryMillivolts": 3310, "charging": false, "batteryCelsius": 31.5 }
```

204. Erros: 400 tensão fora de 2500 a 4500 mV ou temperatura fora de -40 a 125 °C, 401 chave inválida.

`GET /api/v1/doorbells` (SPEC-005) ganha `batteryPercent`, `charging`, `batteryCelsius`, `lastSeenAt`, opcionais (nulos para campainha sem relato).

## Impacto em dados

Colunas novas na campainha: última tensão, carregando, última temperatura, momento do último contato. Expand puro.

## Interface

Na lista de campainhas e no filtro do histórico: ícone de bateria com a porcentagem, ícone de raio quando carregando, e os estados "Bateria fraca", "Bateria quente" e "Sem comunicação".

## Riscos e decisões

- O #2 inverte a ordem da SPEC-003 (foto antes do WiFi), porque conectar leva de 1 a 3 s e a pessoa pode sair da frente da câmera.
- O momento do toque continua sendo a chegada no backend (SPEC-001 #5); com o sono, ele fica alguns segundos depois do aperto, o que é aceito.
- A câmera OV2640 e o LED de energia da placa consomem em sono; pode ser preciso cortar a alimentação da câmera por um pino ou transistor. Medido no plano.
- O botão precisa estar num pino capaz de acordar a placa (SPEC-007).
- Sol direto: dentro do case a temperatura passa de 60 °C. Bateria LiFePO4 em vez de Li-ion comum, por tolerar melhor o calor e não ter fuga térmica; carregador solar com sensor de temperatura (NTC) que corta a carga sozinho (#11), sem depender do firmware. Case claro, com a bateria do lado da sombra. Peças compradas a partir da lista do plano.
- A curva de tensão para porcentagem (#6) e o mínimo seguro (#9) seguem a química escolhida.

## Histórico

| Data | Mudança |
|---|---|
| 2026-11-16 | Criação |
| 2026-11-16 | Sol direto: proteção térmica da carga (#11, #12), LiFePO4, peças compradas pela lista do plano |
