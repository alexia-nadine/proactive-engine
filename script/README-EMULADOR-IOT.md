# 📡 Emulador IoT — Visão Funcional (`emulator.py`)


Este módulo contém o script Python responsável por simular o ecossistema de sensores de um Ambiente de Vida Assistida (AAL). Ele atua como o produtor (*Publisher*) na arquitetura orientada a eventos, injetando cargas de contexto dinâmico no RabbitMQ para validar o comportamento e a resiliência do Back-end Proativo.

---

## 📌 Objetivo do Emulador

O emulador não envia dados de forma engessada. Ele utiliza uma abordagem estocástica para construir um "baralho" de 100 cenários aleatórios a cada execução. Esses cenários simulam variações de luminosidade, postura, localização e tempo, resultando na transmissão massiva de **1.250 eventos contínuos** para submeter as *guard clauses* e a rastreabilidade de estado (em memória) do Java a testes de estresse.

---

## ⚙️ Cenários Simulados

O emulador utiliza uma abordagem estocástica para construir um "baralho" de 100 cenários aleatórios a cada execução. A distribuição é dividida entre eventos neutros (ruído) e gatilhos de rotinas proativas:

**1. Dia - Evento Neutro (30%)**
*   **Período:** Diurno (08:00 às 18:00).
*   **Condições Típicas:** Alta luminosidade (300-800 lux), usuário em áreas comuns (sala/cozinha), cama desocupada.
*   **Ação Esperada:** O sistema atua como barreira primária do ciclo circadiano, descartando o evento silenciosamente.

**2. Falso Positivo / Alívio de Pressão (20%)**
*   **Período:** Madrugada.
*   **Condições Típicas:** Sensor da cama detecta ausência (`UNOCCUPIED`), mas o usuário retorna à posição de repouso em um intervalo curto (15 segundos).
*   **Ação Esperada:** O motor processa o rastreamento em memória e mitiga o acionamento, validando o filtro de tolerância contra falsos positivos.

**3. Rotina Boa Noite Autônoma (25%)**
*   **Período:** Noturno (23:10 às 23:50).
*   **Condições Típicas:** Usuário deitado (`LYING_DOWN`), no quarto (`BEDROOM`), cama ocupada (`OCCUPIED`), baixa luminosidade, porém com a porta destrancada (`UNLOCKED`).
*   **Ação Esperada:** Disparo de segurança residencial.

**4. Deslocamento Noturno Seguro (25%)**
*   **Período:** Madrugada.
*   **Condições Típicas:** Cama desocupada (`UNOCCUPIED`), presença detectada (`true`) e baixa luminosidade.
*   **Ação Esperada:** O sistema antecipa a necessidade visual do usuário, acendendo o caminho de luz após a consolidação da ausência (limiar de 30s) para prevenção de colisão.

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
