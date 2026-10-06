import random
import time
from base_sensor import conectar_rabbitmq, make_false_positive_event, make_false_positive_reset_event, enviar_evento

conexao = conectar_rabbitmq()
canal = conexao.channel()

print("\n=======================================================")
print(" INJETANDO 10 FALSOS POSITIVOS (Janela < 30s)")
print(" Contexto: Usuário levanta, mas deita rápido demais (5 a 15s).")
print(" Objetivo: O Java deve BLOQUEAR, provando a tolerância a ruído.")
print("=======================================================\n")

for i in range(10):
    print(f"\n[Falso Positivo {i+1}/10]")
    evento_a = make_false_positive_event()
    enviar_evento(canal, evento_a)

    tempo_fora = random.randint(5, 15)
    print(f"   ... aguardando retorno rápido simulado ({tempo_fora}s) ...")
    time.sleep(tempo_fora)

    enviar_evento(canal, make_false_positive_reset_event(evento_a['timeOfDay'], tempo_fora))

print("\n-> Lote Falsos Positivos finalizado! (Verifique a barra AMARELA no Grafana)")
conexao.close()