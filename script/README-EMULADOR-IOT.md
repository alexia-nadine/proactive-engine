# Script IoT Emulator e Testes de Validação

Esta pasta reúne os scripts Python usados para simular sensores e injetar eventos em fila no RabbitMQ. Eles servem como camada de produção de mensagens para validar a lógica de negócio e as regras proativas implementadas no Java.

Estrutura da pasta:

- `iot-emulator/` — conjunto de emissores e simuladores de cenário
- `README-EMULADOR-IOT.md` — documentação geral do emulador

Objetivo geral:

- Testar a arquitetura desacoplada por eventos
- Validar regras de negócio em diferentes condições
- Gerar carga artificial para avaliar throughput, latência e robustez
- Provocar cenários de ruído, falsos positivos e payloads inválidos

## Visão geral dos scripts

### 1) `iot-emulator/base_sensor.py`
Arquivo base compartilhado por todos os testes. Ele centraliza:

- conexão com RabbitMQ
- geração de payloads JSON
- criação de eventos por cenário
- publicação das mensagens na fila `iot.sensor.events`

Funções principais:

- `conectar_rabbitmq()`
  - estabelece a conexão e retorna connection + channel
- `random_night_time()`
  - gera horários noturnos aleatórios
- `random_daytime_time()`
  - gera horários diurnos aleatórios
- `add_seconds_to_time()`
  - adiciona segundos a um horário e preserva o ciclo diário
- `build_payload()`
  - monta um payload padronizado com os campos do evento
- `make_light_action_event()`
  - simula saída da cama com necessidade de luz de caminho
- `make_door_action_event()`
  - simula risco de porta destrancada durante sono
- `make_false_positive_event()`
  - simula alívio de pressão sem risco real
- `make_false_positive_reset_event()`
  - simula retorno rápido à cama para testar filtro de falso positivo
- `make_daytime_event()`
  - simula evento diurno, que deve ser ignorado
- `make_safe_return_to_bed_event()`
  - simula retorno seguro à cama
- `enviar_evento()`
  - publica a mensagem no RabbitMQ

Este arquivo é a base para os testes de validação do comportamento do sistema.

---

### 2) `iot-emulator/01_eventos_diurnos.py`
Teste para validar o filtro de janela diurna.

Objetivo:
- enviar 10 eventos em horário diurno com alta luminosidade
- confirmar que o Java ignora os eventos sem disparar decisão proativa

Fluxo:
- conecta no RabbitMQ
- itera 10 vezes
- chama `make_daytime_event()`
- publica a mensagem
- pausa 0,5s entre eventos

Mensagens esperadas:
- evento diurno sempre ignorado
- métrica de evento diurno descartado deve aumentar
- nenhuma ação de luz ou porta deve ser disparada

Observação:
- esse teste valida a guard clause de ciclo circadiano e a proteção contra falso acionamento em horário de luz natural

---

### 3) `iot-emulator/02_falsos_positivos.py`
Teste para validar mitigação de falsos positivos.

Objetivo:
- simular levantamentos curtos da cama antes do usuário realmente se levantar
- garantir que o sistema não dispare ação proativa sem estabilidade da condição

Fluxo:
- cria um evento de saída da cama (`make_false_positive_event()`)
- publica o evento
- aguarda entre 5 e 15 segundos
- publica um evento de retorno rápido (`make_false_positive_reset_event()`)
- repete 10 vezes

Regra de negócio envolvida:
- ausência da cama deve ser considerada somente se persistir acima do limite temporal
- se o usuário retorna em poucos segundos, o sistema deve filtrar o evento como falso positivo

Esse cenário mede a tolerância ao ruído e a robustez das decisões. Em termos de teorias de mensageria, ele representa um caso em que o produtor injeta eventos “quase redundantes” em curto intervalo, e o consumer precisa decidir se o dado é relevante ou apenas flapping.

---

### 4) `iot-emulator/03_rotina_portas.py`
Teste para validar a rotina 1: “Boa Noite Autônoma” / segurança da porta.

Objetivo:
- simular usuário deitado, cama ocupada, porta destrancada, presença detectada
- garantir que o Java detecte a vulnerabilidade e trave a porta

Fluxo:
- conecta no RabbitMQ
- repete 40 vezes
- chama `make_door_action_event()`
- publica a mensagem
- aguarda 0,5s

Regra tecnológica:
- usuário em postura `LYING_DOWN` + quarto + cama ocupada + porta `UNLOCKED`
- disparo de ação de segurança

Esse cenário é importante para demonstrar que o sistema pode agir de modo proativo para reduzir risco em ambientes de mobilidade reduzida.

---

### 5) `iot-emulator/04_rotina_luzes.py`
Teste para validar a rotina 2: "Deslocamento Noturno Seguro" / iluminação do caminho.

Objetivo:
- simular usuário saindo da cama durante a noite
- confirmar que o Java só acende as luzes após a ausência prolongada e baixa luminosidade

Fluxo:
- publica um evento de saída da cama (`make_light_action_event()`)
- aguarda entre 32 e 35 segundos
- publica `evento_heartbeat` com base no tempo das saídas
- aguarda mais 32 a 35 segundos
- publica evento de retorno seguro à cama (`make_safe_return_to_bed_event()`)
- repete 40 vezes

Conceitos importantes:
- ausência prolongada da cama
- heartbeat como confirmação de permanência fora da cama
- retorno seguro para resetar o rastreio de estado
- filtro de ação de iluminação para não acender luzes em falso evento


---

### 6) `iot-emulator/teste_estresse.py`
Teste de estresse e resiliência arquitetural.

Objetivo:
- simular carga intensa de eventos no RabbitMQ
- testar robustez, vazão e tolerância a dados inválidos
- provocar ruído de sensor e “flapping” em massa

Principais mecanismos:

#### a) Geração aleatória de payloads
A função `gerar_payload_aleatorio()` distribui eventos da seguinte forma:

- 40% `light_action`
- 40% `door_action`
- 15% `daytime_neutral`
- 5% `poison_pill`

Isso cria uma carga realista, mista e pouco previsível.

#### b) Poison pill
`make_poison_pill_event()` gera um payload incompleto, por exemplo sem `timeOfDay`.

Esse tipo de mensagem tem objetivo específico: forçar o Java a detectar dados inválidos e descartar a mensagem sem quebrar o processamento geral.

#### c) Flapping / ruído em massa
`simular_tempestade_eventos(canal, quantidade=200)` publica rapidamente 200 eventos idênticos, simulando falha de sensor ou oscilação de estado.

A chamada é disparada aleatoriamente com 0,5% de chance a cada iteração.

#### d) Carga de 50.000 mensagens
`executar_teste_carga(total_mensagens=50000)` envia 50 mil mensagens em sequência.

Esse teste permite analisar:

- throughput de ingestão
- latência de processamento
- estabilidade do consumidor
- capacidade de ignorar payloads inválidos
- comportamento sob picos de carga

O script é a prova prática de que a arquitetura baseada em mensageria assíncrona pode ser submetida a estresse operacional para evidenciar viabilidade arquitetural.

---

## Padrão comum nos scripts

Todos os scripts seguem a mesma estrutura:

1. conectar ao RabbitMQ
2. abrir channel
3. construir evento ou lote de eventos
4. publicar em `iot.sensor.events`
5. registrar logs de progresso
6. fechar a conexão

Isso facilita a manutenção e permite comparar cenários de forma padronizada.

## Mensagem publicada no RabbitMQ

O JSON enviado para a fila tem o formato abaixo:

```json
{
  "timeOfDay": "03:15:00",
  "userPosture": "SITTING",
  "roomLocation": "BEDROOM",
  "doorStatus": "LOCKED",
  "bedPressureStatus": "UNOCCUPIED",
  "presenceDetected": true,
  "luminosityLux": 20,
  "expectedDecision": "turn_on_path_lights",
  "expected_action": "light_path",
  "descricao": "Carga Útil"
}
```

Em alguns casos, por exemplo no stress test, a mensagem pode ser malformada ou incompleta, como objetivo de testar robustez.

## Como rodar

Exemplo de execução simples:

```bash
cd script/iot-emulator
python 01_eventos_diurnos.py
```

Ou para o teste de estresse:

```bash
cd script/iot-emulator
python teste_estresse.py
```

Precondições:

- RabbitMQ em execução
- variáveis de ambiente configuradas (`RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`)
- aplicação Java do projeto já em execução para consumir as mensagens

## Relação com o TCC

Esses scripts não são apenas “geradores de dados”. Eles funcionam como ferramenta de validação experimental para a arquitetura:

- Producer: scripts Python
- Broker: RabbitMQ
- Consumer: aplicação Java
- Lógica de negócio: `ProactiveDecisionService`
- Observabilidade: Micrometer + métricas

Isso materializa, na prática, o conceito teórico de mensageria assíncrona e demonstra como uma arquitetura orientada a eventos pode desacoplar sensores, processamento e decisão proativa.

---

## Conclusão

A pasta `script` é o laboratório experimental do projeto. Ela permite provar, com indicadores concretos, como a aplicação reage a:

- eventos diurnos
- falsos positivos
- portas destrancadas
- ausência prolongada da cama
- luzes de caminho
- carga em massa
- payloads inválidos

Esses cenários são essenciais para demonstrar que a solução não apenas “funciona”, mas também resiste a condições adversas comuns em ambientes IoT e Ambient Intelligence.

