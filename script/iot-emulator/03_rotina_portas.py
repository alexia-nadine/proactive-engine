import time
from base_sensor import conectar_rabbitmq, make_door_action_event, enviar_evento

conexao = conectar_rabbitmq()
canal = conexao.channel()

print("\n=======================================================")
print(" INJETANDO 40 AÇÕES DE PORTA (Instantâneo)")
print(" Contexto: Usuário deita para dormir e a porta está UNLOCKED.")
print(" Objetivo: O Java deve detectar a vulnerabilidade e TRANCAR.")
print("=======================================================\n")

for i in range(40):
    enviar_evento(canal, make_door_action_event())
    time.sleep(0.5)

print("\n-> Lote de Portas finalizado! (Verifique a barra VERDE no Grafana)")
conexao.close()