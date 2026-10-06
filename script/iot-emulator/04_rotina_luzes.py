import random
import time
from base_sensor import conectar_rabbitmq, make_light_action_event, make_safe_return_to_bed_event, add_seconds_to_time, enviar_evento

conexao = conectar_rabbitmq()
canal = conexao.channel()

print("\n=======================================================")
print(" INJETANDO 40 AÇÕES DE LUZ COM HEARTBEAT (> 30s)")
print(" Contexto: Usuário levanta no escuro e passa de 30s fora.")
print(" Objetivo: O Java deve ACENDER as luzes apenas após o Heartbeat.")
print("=======================================================\n")

for i in range(40):
    print(f"\n--- Cenário de Luz {i+1}/40 ---")
    evento_saida = make_light_action_event()
    enviar_evento(canal, evento_saida)

    tempo_fora = random.randint(32, 35)
    print(f"   ... cronômetro do Java correndo: aguardando ({tempo_fora}s) ...")
    time.sleep(tempo_fora)

    evento_heartbeat = make_light_action_event(base_time=add_seconds_to_time(evento_saida['timeOfDay'], tempo_fora))
    evento_heartbeat['descricao'] = "Rotina 2 - Heartbeat (Confirma ausência > 30s)"
    enviar_evento(canal, evento_heartbeat)

    tempo_retorno = random.randint(32, 35)
    print(f"   ... ação acionada! Aguardando usuário voltar em segurança ({tempo_retorno}s) ...")
    time.sleep(tempo_retorno)

    evento_retorno = make_safe_return_to_bed_event(evento_heartbeat['timeOfDay'], tempo_retorno)
    enviar_evento(canal, evento_retorno)

print("\n-> Lote de Luzes finalizado! (Verifique a barra VERDE no Grafana)")
conexao.close()