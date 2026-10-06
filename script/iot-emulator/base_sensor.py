# Arquivo: base_sensor.py
from os import getenv
from dotenv import load_dotenv
import json
import random
import pika

load_dotenv()

RABBIT_HOST = getenv('RABBITMQ_HOST', 'localhost')
RABBIT_PORT = 5672
RABBIT_USER = getenv('RABBITMQ_USER', 'guest')
RABBIT_PASS = getenv('RABBITMQ_PASSWORD', 'guest')
QUEUE_NAME = 'iot.sensor.events'

def conectar_rabbitmq():
    credentials = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    parameters = pika.ConnectionParameters(RABBIT_HOST, RABBIT_PORT, '/', credentials, heartbeat=600, blocked_connection_timeout=300)
    return pika.BlockingConnection(parameters)

def random_night_time():
    hour = random.choice([0, 1, 2, 3])
    return f"{hour:02d}:{random.randint(0, 59):02d}:{random.randint(0, 59):02d}"

def random_daytime_time():
    hour = random.choice([11, 12, 13, 14])
    return f"{hour:02d}:{random.randint(0, 59):02d}:{random.randint(0, 59):02d}"

def add_seconds_to_time(time_str, seconds):
    hours, minutes, secs = map(int, time_str.split(':'))
    total_seconds = (hours * 3600) + (minutes * 60) + secs + seconds
    total_seconds %= 86400
    return f"{total_seconds // 3600:02d}:{(total_seconds % 3600) // 60:02d}:{total_seconds % 60:02d}"

def build_payload(hora, postura, local, porta, cama, presenca, lux, expected_decision, expected_action, descricao):
    return {
        "timeOfDay": hora, "userPosture": postura, "roomLocation": local, "doorStatus": porta,
        "bedPressureStatus": cama, "presenceDetected": presenca, "luminosityLux": lux,
        "expectedDecision": expected_decision, "expectedAction": expected_action, "descricao": descricao
    }

def make_light_action_event(base_time=None):
    return build_payload(hora=random_night_time()
    if base_time is None
    else base_time, postura="SITTING", local="BEDROOM", porta="LOCKED", cama="UNOCCUPIED", presenca=True, lux=random.randint(0, 50), expected_decision="turn_on_path_lights", expected_action="light_path", descricao="Rotina 2 - Saída da Cama")

def make_door_action_event():
    return build_payload(hora=random_night_time(), postura="LYING_DOWN", local="BEDROOM", porta="UNLOCKED", cama="OCCUPIED", presenca=True, lux=random.randint(0, 50), expected_decision="lock_doors", expected_action="trigger_security_alert", descricao="Rotina 1 - Trancar Portas")

def make_false_positive_event():
    return build_payload(hora=random_night_time(), postura="SITTING", local="BEDROOM", porta="LOCKED", cama="UNOCCUPIED", presenca=True, lux=random.randint(0, 50), expected_decision="no_action", expected_action="none", descricao="Falso Positivo - Saída")

def make_false_positive_reset_event(base_time, elapsed_secs):
    return build_payload(hora=add_seconds_to_time(base_time, elapsed_secs), postura="LYING_DOWN", local="BEDROOM", porta="LOCKED", cama="OCCUPIED", presenca=True, lux=random.randint(0, 50), expected_decision="no_action", expected_action="none", descricao="Falso Positivo - Retorno Rápido")

def make_daytime_event():
    return build_payload(
        hora=random_daytime_time(), postura="SITTING", local="BEDROOM",
        porta="LOCKED",
        cama="UNOCCUPIED", # <--- MUDE AQUI! (Simula que o usuário saiu da cama de dia)
        presenca=True, lux=random.randint(500, 800),
        expected_decision="no_action", expected_action="none",
        descricao="Evento Diurno Ignorado"
    )

def make_safe_return_to_bed_event(base_time, elapsed_secs):
    return build_payload(hora=add_seconds_to_time(base_time, elapsed_secs), postura="LYING_DOWN", local="BEDROOM", porta="LOCKED", cama="OCCUPIED", presenca=True, lux=0, expected_decision="no_action", expected_action="none", descricao="Retorno Seguro à Cama")

def enviar_evento(canal, payload):
    properties = pika.BasicProperties(content_type='application/json')
    canal.basic_publish(exchange='', routing_key='iot.sensor.events', body=json.dumps(payload), properties=properties)

    # Log Universal: Cobre as Guard Clauses da Rotina 1 (Portas) e Rotina 2 (Luzes)
    contexto = f"Postura: {payload['userPosture']:<10} | Local: {payload['roomLocation']:<7} | Cama: {payload['bedPressureStatus']:<10} | Porta: {payload['doorStatus']:<8} | Lux: {payload['luminosityLux']:>3}"

    print(f"[{payload['timeOfDay']}] {payload['descricao']:<42} -> [{contexto}]")