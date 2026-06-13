print("SUCESSO: O Python conseguiu ler o arquivo unificado!")

from os import getenv
from dotenv import load_dotenv
from datetime import datetime

import pika
import json
import time
import random

# CONFIGURAÇÕES DA ARQUITETURA
load_dotenv()

RABBIT_HOST = getenv('RABBITMQ_HOST', 'localhost')
RABBIT_PORT = 5672
RABBIT_USER = getenv('RABBITMQ_USER', 'guest')
RABBIT_PASS = getenv('RABBITMQ_PASSWORD', 'guest')
QUEUE_NAME = 'iot.sensor.events'


def conectar_rabbitmq():
    credentials = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    parameters = pika.ConnectionParameters(RABBIT_HOST, RABBIT_PORT, '/', credentials)
    connection = pika.BlockingConnection(parameters)
    return connection, connection.channel()

def enviar_evento(canal, hora, postura, local, porta, cama, presenca, lux, descricao, silencioso=False):
    payload = {
        "timeOfDay": hora,
        "userPosture": postura,
        "roomLocation": local,
        "doorStatus": porta,
        "bedPressureStatus": cama,
        "presenceDetected": presenca,
        "luminosityLux": lux
    }
    properties = pika.BasicProperties(content_type='application/json')
    canal.basic_publish(exchange='', routing_key=QUEUE_NAME, body=json.dumps(payload), properties=properties)

    # O modo silencioso evita poluir a tela com 35 prints repetidos durante o delay da cama
    if not silencioso:
        print(f"[{hora}] {descricao} | Cama: {cama} | Lux: {lux}")

def executar_simulacao_dinamica():
    try:
        conexao, canal = conectar_rabbitmq()
        print("\n==========================================================")
        print(" INICIANDO SIMULAÇÃO ESTATÍSTICA (EMBARALHAMENTO DINÂMICO)")
        print("==========================================================\n")

        # 1. Criação do "Baralho" de Cenários (Garante a distribuição estatística exata)
        # 20 Cenários no total: 50% Eventos Neutros/Ruído e 50% Eventos de Ação (equitativos)
        cenarios = (
            ['Dia-Evento Neutro'] * 30 +                            # 30% - Evento neutro diurno
            ['Falso_Positivo'] * 20 +                               # 20% - Evento neutro noturno (Filtro de 30s)
            ['Rotina-Boa_Noite_Autonoma'] * 25 +                    # 25% - Ação Proativa: Boa Noite Autônoma
            ['Rotina-Deslocamento_Noturno_Seguro'] * 25             # 25% - Ação Proativa: Deslocamento Noturno Seguro
        )

        # Embaralha para ser completamente diferente a cada execução
        random.shuffle(cenarios)

        for i, cenario in enumerate(cenarios, 1):
            print(f"\n--- [Cenário {i}/100] Sorteado: {cenario} ---")

            if cenario == 'Dia-Evento Neutro':
                hora = f"{random.randint(8, 18):02d}:00:00"
                lux_dinamico = random.randint(300, 800)
                enviar_evento(canal, hora, "SITTING", "LIVING_ROOM", "UNLOCKED", "UNOCCUPIED", True, lux_dinamico, "Descarte Diurno")
                time.sleep(2)

            elif cenario == 'Rotina-Boa_Noite_Autonoma':
                hora = f"23:{random.randint(10, 50):02d}:00"
                lux_dinamico = random.randint(5, 15)
                enviar_evento(canal, hora, "LYING_DOWN", "BEDROOM", "UNLOCKED", "OCCUPIED", True, lux_dinamico, "Alerta de Evasão/Segurança")
                time.sleep(2)

            elif cenario == 'Falso_Positivo':
                print("Iniciando Alívio de Pressão", end="")
                for seg in range(15):
                    hora = f"03:15:{seg:02d}"
                    lux_dinamico = random.randint(5, 20)
                    enviar_evento(canal, hora, "SITTING", "BEDROOM", "LOCKED", "UNOCCUPIED", True, lux_dinamico, "", silencioso=True)
                    print(".", end="", flush=True) # Feedback visual de progresso sem poluir a tela
                    time.sleep(1)

                print(" Concluído!")
                enviar_evento(canal, "03:15:15", "LYING_DOWN", "BEDROOM", "LOCKED", "OCCUPIED", True, 10, "Fim Falso Positivo (Retorno ao Leito)")
                time.sleep(2)

            elif cenario == 'Rotina-Deslocamento_Noturno_Seguro':
                print("Iniciando Transferência para Cadeira (Aguardando regra de 30s)", end="")
                for seg in range(1, 36):
                    hora = f"04:20:{seg:02d}"
                    lux_dinamico = random.randint(5, 20)
                    enviar_evento(canal, hora, "SITTING", "BEDROOM", "LOCKED", "UNOCCUPIED", True, lux_dinamico, "", silencioso=True)
                    print(".", end="", flush=True) # Feedback visual
                    time.sleep(1)

                print(" Concluído! (Verifique log do Java)")
                time.sleep(2)

    except Exception as e:
        print(f"Erro na simulação: {e}")
    finally:
        if 'conexao' in locals() and conexao.is_open:
            conexao.close()
            print("\nSimulação concluída com sucesso! Verifique o Grafana.")

if __name__ == '__main__':
    executar_simulacao_dinamica()