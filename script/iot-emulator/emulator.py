print("SUCESSO: O Python conseguiu ler o arquivo!")

from os import getenv
from dotenv import load_dotenv

import pika
import json
import time
import random


# CONFIGURAÇÕES DA ARQUITETURA
load_dotenv()

RABBIT_HOST = getenv('RABBITMQ_HOST')
RABBIT_PORT = 5672
RABBIT_USER = getenv('RABBITMQ_USER')
RABBIT_PASS = getenv('RABBITMQ_PASSWORD')
QUEUE_NAME = 'iot.sensor.events'
TOTAL_EVENTOS = 300

def conectar_rabbitmq():
    credentials = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    parameters = pika.ConnectionParameters(RABBIT_HOST, RABBIT_PORT, '/', credentials)
    connection = pika.BlockingConnection(parameters)
    return connection, connection.channel()

def gerar_cenario_estatistico():
    probabilidade = random.random()

    # CENÁRIO 1 (Rotina Segura / Ignorada pelo motor)
    if probabilidade > 0.50:
        hora = f"{random.randint(8, 18):02d}:00:00"
        postura = random.choice(["SITTING", "STANDING"])
        local = random.choice(["LIVING_ROOM", "KITCHEN"]) # Usando o Enum exato
        porta = random.choice(["LOCKED", "UNLOCKED"])
        cama = "UNOCCUPIED" # Atualizado para o Enum
        presenca = True
        lux = random.randint(300, 800)
        tipo = "Rotina Segura sem anomalia"

    # CENÁRIO 2 (Gatilho da Rotina 1 - Risco de Evasão)
    elif probabilidade > 0.25:
        hora = f"{random.randint(23, 23):02d}:{random.randint(10, 50):02d}:00"
        postura = "LYING_DOWN"
        local = "BEDROOM"
        porta = "UNLOCKED"
        cama = "OCCUPIED"
        presenca = True
        lux = random.randint(0, 15)
        tipo = "Rotina 1 - Risco de Evasão"

    # CENÁRIO 3 (Gatilho da Rotina 2 - Movimentação Noturna)
    else:
        hora = f"{random.randint(2, 4):02d}:{random.randint(10, 50):02d}:00" # Alta Madrugada
        postura = "SITTING" # Ex: Movimentando na cadeira de rodas
        local = "BEDROOM"
        porta = "LOCKED"
        cama = "UNOCCUPIED" # Gatilho: Usuario não está na cama
        presenca = True     # Gatilho: Tem presença no quarto
        lux = random.randint(0, 5) # Gatilho: escuro total (Risco de queda/colisão)
        tipo = "Rotina 2 - Movimentação Noturna"

    payload = {
        "timeOfDay": hora, "userPosture": postura, "roomLocation": local,
        "doorStatus": porta, "bedPressureStatus": cama, "presenceDetected": presenca, "luminosityLux": lux
    }
    return payload, tipo

def iniciar_simulacao():
    print("Iniciando Simulação Estatística DSR...\n")
    conexao, canal = conectar_rabbitmq()

    for i in range(1, TOTAL_EVENTOS + 1):
        payload, tipo_cenario = gerar_cenario_estatistico()
        properties = pika.BasicProperties(content_type='application/json')
        canal.basic_publish(exchange='', routing_key=QUEUE_NAME, body=json.dumps(payload), properties=properties)
        print(f"[{i}/{TOTAL_EVENTOS}] Enviado: {tipo_cenario} | Hora: {payload['timeOfDay']}")
        time.sleep(1)

    conexao.close()
    print("\nSimulação concluída!")

if __name__ == '__main__':
    iniciar_simulacao()