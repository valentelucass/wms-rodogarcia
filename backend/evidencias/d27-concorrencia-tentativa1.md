# D27 — readiness recusada, sem gate

Rodada D27C2F6B9BB preservada. O observador recusou `D27_WITNESS_PEDIDO_HTTP_NAO_COMPROVADO` em PEDIDO_PUBLICO, zero tentativas SQL, antes de emitir readiness; helper expirou90s e encerrou a API sem gate/reservas. Não é erro SQL administrativo ou falha de regra da reserva.

Causa de contrato demonstrada na fonte HTTP: POST /api/v1/pedidos-saida retorna201 com envelope Confirmacao (`pedido.id`), não `resposta.id`. CONCA/CONCB foram201 e os IDs reais25/26 vieram de resposta.pedido.id. O observador corrente exige resposta.id para pedidos; sua verificação de cliente direto permanece correta. Prumo possui o observador: corrigir somente a leitura do envelope real e incluir fixture desse envelope. Nenhuma mudança de backend HTTP, credencial, SQL ou permissão é necessária.

Próxima tentativa precisa de nova readiness real, antes do gate; esta família deve ser reutilizada sem recriar cliente/estoque/pedidos, pois ambos continuam RASCUNHO e nenhuma reserva foi enviada. Não repetir IT/build/suíte. O helper será ajustado com modo de retomada concorrente para reutilizar IDs/payloads de versão dos dois pedidos, conservar tentativa inicial e atualizar appPID/UTC da nova JVM.

Atualização01:24UTC: Prumo corrigiu o envelope no observador. Reaproveitar a primeira família exigiria mudar também seu contrato de fonte de criação/JAR PID entre rodadas. Para manter esse contrato estrito já revisado, a tentativa seguinte terá família nova; a inicial permanece RASCUNHO sem reserva e não será contada como concorrência aprovada. Nenhuma mutação D26 ou reset.