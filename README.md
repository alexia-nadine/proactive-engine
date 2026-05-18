# Proactive Engine - IoT Simulation Environment

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Messaging-ff6600.svg)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Containers-blue.svg)](https://www.docker.com/)
[![Academic Project](https://img.shields.io/badge/Pesquisa-DSR-purple.svg)]()


> **Artefato de Pesquisa (TCC):** Arquitetura de Backend Proativa em IoT para Autonomia de Pessoas com Mobilidade Reduzida.  
> **Programa:** MBA em Engenharia de Software (USP/Esalq).

## 🎯 Visão Geral
O **Proactive Engine** é um motor de regras autónomo desenvolvido para minimizar o esforço de interação manual em ambientes de *Smart Homes*. Através da receção contínua de eventos de sensores IoT, o sistema avalia contextos (como horário, postura do utilizador e luminosidade) e toma decisões proativas para garantir a segurança e o conforto do ambiente.
Este repositório contém a Prova de Conceito (PoC) do Artefato de Software desenvolvido como parte do Trabalho de Conclusão de Curso (TCC) em Engenharia de Software (MBA USP/Esalq).


## 🏗️ Arquitetura (Ports and Adapters)
Este projeto foi estruturado utilizando a **Arquitetura Hexagonal**. O objetivo principal é garantir o isolamento absoluto do Domínio da aplicação em relação a frameworks e ferramentas de infraestrutura.
* **Event-Driven:** O processamento assíncrono de eventos de contexto através de mensageria, garantindo baixo acoplamento e alta resiliência.
* **Inversão de Dependência:** O núcleo de domínio não conhece o RabbitMQ ou o console de saída, comunicando-se exclusivamente através de abstrações e interfaces (Portas).
* **Observabilidade:** Monitoramento em tempo real do consumo da JVM e métricas da aplicação.


## 🛠️ Stack Tecnológica
* **Linguagem:** Java 17+
* **Framework:** Spring Boot 3
* **Mensageria:** RabbitMQ
* **Métricas e Monitoramento:** Prometheus, Grafana e Micrometer
* **Emulação de Eventos:** Python
* **Containerização:** Docker e Docker Compose

## ⚙️ Pré-requisitos
Para executar esta Prova de Conceito (PoC) localmente, é necessário ter instalado:
1. [Docker Desktop](https://www.docker.com/products/docker-desktop)
2. [JDK 17+](https://adoptium.net/)
3. [Python 3.8+](https://www.python.org/downloads/)


## 🚀 Como Executar o Projeto
A execução divide-se em três etapas lógicas para garantir a correta inicialização dos serviços.

### 1. Iniciar a Infraestrutura (Mensageria e Métricas)
Abra o terminal na raiz do projeto e execute:
```bash
  docker compose up -d
```
*Isto irá disponibilizar os serviços locais (as credenciais de acesso refletem o padrão de fábrica das imagens Docker e devem ser configuradas no arquivo `.env`):*
* **RabbitMQ (Management):** `http://localhost:15672`
* **Prometheus:** `http://localhost:9090`
* **Grafana:** `http://localhost:3005`

### 2. Iniciar o Motor Proativo
A aplicação pode ser iniciada via sua IDE de preferência (IntelliJ/Eclipse) rodando a classe principal `ProactiveEngineApplication.java`, ou via terminal utilizando o Maven:

```bash
  ./mvnw spring-boot:run
```
> **Nota:** O motor ficará em execução na porta `8080`, escutando ativamente a fila `iot.sensor.events` no RabbitMQ.

### 3. Disparar o Emulador IoT (Carga Sintética)
Para validar as rotinas de decisão do motor, utilize o script Python para injetar dados que simulam sensores reais:

```bash
  cd script/iot-emulator
  python emulator.py
```
> **Nota:** Certifique-se de ter a biblioteca `pika` instalada no Python: `pip install pika`.

## 📊 Validação e Observabilidade

Com o sistema em execução e recebendo dados, a validação de performance e footprint da aplicação pode ser acompanhada pelo Grafana.

- Acesse o painel em [http://localhost:3005](http://localhost:3005).
- O Painel JVM (Micrometer) já se encontra importado.
- No canto superior esquerdo, certifique-se de que a variável `Application` está definida como `ProactiveEngine`.
- Acompanhe os gráficos de **Heap Memory**, **CPU Usage** e **I/O Overview** reagirem em tempo real aos eventos injetados pelo Python.
- Os gráficos demonstrarão o comportamento do uso de CPU, Heap Memory e processamento de rotinas durante os picos de injeção de eventos.

## 📁 Estrutura de Diretórios

A organização das pastas reflete o fluxo direcional de dependências da Arquitetura Limpa:

```text
proactive-engine/
├── src/main/java/br/com/tcc/iot/proactiveengine/
│   ├── domain/               # Entidades, Enums e Payloads (Regras Puras, Zero Framework)
│   ├── application/
│   │   ├── ports/            # Interfaces de Entrada (Use Cases) e Saída (Triggers)
│   │   └── services/         # Coração do Motor: Lógica de Decisão Proativa
│   └── infrastructure/       # Implementação Técnica e Comunicação Externa
│       ├── adapters/
│       │   ├── inbound/      # Consumidor RabbitMQ (Ouve os eventos)
│       │   └── outbound/     # Gatilhos de Ação (Executa as decisões)
│       └── config/           # Configurações do Spring e Filas
├── script/
│   └── iot-emulator/         # Emulador Python de Sensores
└── docker-compose.yml        # Orquestração da Trindade da Observabilidade