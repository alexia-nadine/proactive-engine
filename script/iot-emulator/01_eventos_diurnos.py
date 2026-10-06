import time
from base_sensor import conectar_rabbitmq, make_daytime_event, enviar_evento

conexao = conectar_rabbitmq()
canal = conexao.channel()

print("\n=======================================================")
print(" INJETANDO 10 CENÁRIOS DIURNOS")
print(" Contexto: Usuário ativo, mas a luminosidade (Lux) está alta.")
print(" Objetivo: O Java deve IGNORAR, provando o filtro de luz diurna.")
print("=======================================================\n")

for i in range(10):
    enviar_evento(canal, make_daytime_event())
    time.sleep(0.5)

print("\n-> Lote de Diurnos finalizado! (Verifique a barra AZUL no Grafana)")
conexao.close()