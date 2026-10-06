from os import getenv
from dotenv import load_dotenv
import json
import random
import time
import pika

load_dotenv()

RABBIT_HOST = getenv('RABBITMQ_HOST', 'localhost')
RABBIT_PORT = 5672
RABBIT_USER = getenv('RABBITMQ_USER', 'guest')
RABBIT_PASS = getenv('RABBITMQ_PASSWORD', 'guest')
QUEUE_NAME = 'iot.sensor.events'

def conectar_rabbitmq():
    credentials = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
    parameters = pika.ConnectionParameters(
        RABBIT_HOST, RABBIT_PORT, '/', credentials,
        heartbeat=600, blocked_connection_timeout=300
    )
    connection = pika.BlockingConnection(parameters)
    return connection, connection.channel()

def random_night_time():
    hour = random.choice([0, 1, 2, 3, 4, 5])
    return f"{hour:02d}:{random.randint(0, 59):02d}:{random.randint(0, 59):02d}"

def random_daytime_time():
    hour = random.choice([10, 11, 12, 13, 14, 15])
    return f"{hour:02d}:{random.randint(0, 59):02d}:{random.randint(0, 59):02d}"

def build_payload(hora, postura, local, porta, cama, presenca, lux, expected_decision, expected_action, descricao):
    return {
        "timeOfDay": hora, "userPosture": postura, "roomLocation": local,
        "doorStatus": porta, "bedPressureStatus": cama, "presenceDetected": presenca,
        "luminosityLux": lux, "expectedDecision": expected_decision,
        "expected_action": expected_action, "descricao": descricao
    }

def make_poison_pill_event():
    """Gera um pacote faltando 'timeOfDay', forçando o Input Adapter do Java a registrar um Erro."""
    return {
        "userPosture": "SITTING", "roomLocation": "BEDROOM", "doorStatus": "LOCKED",
        "presenceDetected": True, "expectedDecision": "error", "expected_action": "none",
        "descricao": "Carga Fase 2 - CAOS: Poison Pill"
    }

def gerar_payload_aleatorio():
    """Distribuição: 95% de carga útil, 5% de Poison Pills para testar tolerância a falhas."""
    tipo = random.choices(
        ['light_action', 'door_action', 'daytime_neutral', 'poison_pill'],
        weights=[40, 40, 15, 5],
        k=1
    )[0]

    if tipo == 'light_action':
        return build_payload(random_night_time(), "SITTING", "BEDROOM", "LOCKED", "UNOCCUPIED", True, random.randint(0, 50), "turn_on_path_lights", "light_path", "Carga Útil")
    elif tipo == 'door_action':
        return build_payload(random_night_time(), "LYING_DOWN", "BEDROOM", "UNLOCKED", "OCCUPIED", True, random.randint(0, 50), "lock_doors", "trigger_security_alert", "Carga Útil")
    elif tipo == 'poison_pill':
        return make_poison_pill_event()
    else:
        return build_payload(random_daytime_time(), "SITTING", "BEDROOM", "LOCKED", "OCCUPIED", True, random.randint(500, 800), "no_action", "none", "Carga Útil")

def simular_tempestade_eventos(canal, quantidade=200):
    """Dispara um burst de pacotes no mesmo segundo, simulando defeito de hardware (flapping)."""
    properties = pika.BasicProperties(content_type='application/json', delivery_mode=1)
    payload = build_payload(random_night_time(), "LYING_DOWN", "BEDROOM", "LOCKED", "OCCUPIED", True, 0, "no_action", "none", "CAOS: Flapping")

    print(f"-> Injetando ruído: {quantidade} eventos simultâneos...")
    for _ in range(quantidade):
        canal.basic_publish(exchange='', routing_key=QUEUE_NAME, body=json.dumps(payload), properties=properties)

def executar_teste_carga(total_mensagens=50000):
    """Executa o teste de saturação. Justificado teoricamente para provar limites arquiteturais."""
    try:
        conexao, canal = conectar_rabbitmq()
        print(f'\n[INÍCIO] Teste de Estresse Arquitetural - {total_mensagens} Eventos\n')
        inicio = time.time()
        properties = pika.BasicProperties(content_type='application/json', delivery_mode=1)

        for i in range(1, total_mensagens + 1):
            if random.random() < 0.005: # 0.5% de chance de tempestade
                simular_tempestade_eventos(canal)

            payload = gerar_payload_aleatorio()
            canal.basic_publish(exchange='', routing_key=QUEUE_NAME, body=json.dumps(payload), properties=properties)

            if i % 1000 == 0:
                print(f"[Progresso] {i} pacotes enviados...")

        duracao = time.time() - inicio
        print(f'\n[FIM] Vazão de Injeção: {total_mensagens / duracao:.2f} msgs/seg')

    except Exception as exc:
        print(f'Falha: {exc}')
    finally:
        if 'conexao' in locals() and conexao.is_open:
            conexao.close()

if __name__ == '__main__':
    executar_teste_carga(50000)