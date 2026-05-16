# 📡 Emulador IoT — Visão Funcional (`emulator.py`)

Este documento descreve o comportamento do emulador IoT (`script/iot-emulator/emulator.py`) e define a estrutura dos payloads JSON publicados na fila RabbitMQ. O foco principal é a semântica das mensagens geradas e os cenários simulados para validar o motor proativo.

---

## 📌 Objetivo do Emulador

O script atua como um produtor de dados, simulando eventos de contexto que combinam o estado do usuário e variáveis do ambiente. Cada evento é publicado na fila `iot.sensor.events` contendo a propriedade `content_type: application/json`, permitindo que o consumidor teste regras de negócio e ações proativas.

---

## ⚙️ Cenários Simulados

O emulador gera eventos estatísticos baseados em probabilidades pré-definidas para reproduzir três comportamentos principais:

**1. Rotina Segura (Comportamento Padrão)**
*   **Período:** Diurno (08:00 às 18:00).
*   **Condições Típicas:** `roomLocation` na sala ou cozinha, `bedPressureStatus` como desocupada, e alta luminosidade.
*   **Ação Esperada:** Nenhuma. O motor não deve disparar ações proativas.

**2. Risco de Evasão / Esquecimento**
*   **Período:** Noturno (ex: 23:10 às 23:50).
*   **Condições Típicas:** Usuário deitado (`LYING_DOWN`), no quarto (`BEDROOM`), cama ocupada (`OCCUPIED`), baixa luminosidade, porém com a porta destrancada (`UNLOCKED`).
*   **Ação Esperada:** Disparo do comportamento da rotina "Boa noite autônoma" (ex: trancar portas e desligar luzes restantes).

**3. Movimentação Noturna**
*   **Período:** Madrugada (ex: 02:00 às 04:00).
*   **Condições Típicas:** Cama desocupada (`UNOCCUPIED`), presença detectada (`true`) e luminosidade muito baixa.
*   **Ação Esperada:** Prevenção de colisão da rotina "Deslocamento Noturno Seguro" (ex: acender luzes guia no nível mínimo).

---

## 📦 Estrutura do Payload (JSON)

Para garantir o mapeamento correto dos dados no consumidor Java (ex: `ContextEventPayload`), o emulador publica os objetos com as seguintes chaves estritas:

| Campo | Tipo | Descrição | Exemplos de Valores |
| :--- | :--- | :--- | :--- |
| `timeOfDay` | String | Hora do evento no formato `HH:MM:SS`. | `"03:15:00"`, `"23:05:00"` |
| `userPosture` | String | Postura atual do utilizador. | `"SITTING"`, `"STANDING"`, `"LYING_DOWN"` |
| `roomLocation` | String | Localização da detecção. | `"BEDROOM"`, `"LIVING_ROOM"`, `"KITCHEN"` |
| `doorStatus` | String | Estado da fechadura inteligente. | `"LOCKED"`, `"UNLOCKED"` |
| `bedPressureStatus` | String | Sensor de pressão no colchão. | `"OCCUPIED"`, `"UNOCCUPIED"` |
| `presenceDetected` | Boolean | Detecção de movimento no ambiente. | `true`, `false` |
| `luminosityLux` | Integer | Nível de luminosidade do cômodo. | `0`, `5`, `150` |

---

## 📨 Exemplos de Mensagens

**Evento de movimentação noturna (Acionamento de luzes de caminho):**
```json
{
  "timeOfDay": "03:12:00",
  "userPosture": "SITTING",
  "roomLocation": "BEDROOM",
  "doorStatus": "LOCKED",
  "bedPressureStatus": "UNOCCUPIED",
  "presenceDetected": true,
  "luminosityLux": 4
}

```
**Evento de risco de evasão (Acionamento de "Boa noite autônoma"):**
```json
{
  "timeOfDay": "23:30:00",
  "userPosture": "LYING_DOWN",
  "roomLocation": "BEDROOM",
  "doorStatus": "UNLOCKED",
  "bedPressureStatus": "OCCUPIED",
  "presenceDetected": false,
  "luminosityLux": 10
}
```
