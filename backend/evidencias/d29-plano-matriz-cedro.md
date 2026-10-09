# D29 — plano e matriz Cedro

Cada caso e os 39 esperados independentes estão ligados no JSON antes da execução. Fonte, insumo e efeito esperado delimitam a prova; candidatos ainda exigem curadoria.

| Caso | Requisitos | Esperado |
| --- | --- | --- |
| D29-ENTRADA | RN01, RN02, RN06, RN10, RN12, RN13, RN14, RN15, RN16, RN17, I01, I03, I04, PR02, PR03, PR05, PR06, V01, V02, V03, V04, V09, V14, V16, V26, V27, V28, V29, AC03, AC10 | 1000 previstas/recebidas:2 IDs500; XML previsto nao fisico; 98/102/95+5 exigem aceite rastreavel, quarentena exclui disponibilidade; repetir conserva IDs e saldo |
| D29-ESTOQUE | RN02, RN04, RN05, RN10, RN11, RN17, RN18, RN19, RN20, RN21, RN23, I03, I04, I05, PR06, PR07, V05, V06, V08, V16, V23, V25, V29, V30, V38, AC04, AC05, AC10 | Identidade/origem/FIFO preservados; movimentos alteram somente destino/historico; duas posicoes atomicas; capacidade ocupada e SKU/lote/nota/data incompatíveis recusados |
| D29-SAIDA | RN22, RN23, RN24, RN25, RN26, I02, I06, PR01, PR02, PR04, PR05, PR08, PR10, PR11, V07, V10, V11, V14, V17, V24, V31, V32, V33, V34, V39, AC01, AC02, AC11 | Reserva mantém fisico, não expira; integral100 com retirada100 de500 deixa400; fiscal não baixa; avaria bloqueia pedido sem liberar; reversões vinculadas e replay sem repetição |
| D29-CARGA-CONTAGEM | RN02, RN13, RN20, RN29, PR02, PR04, PR05, PR06, V15, V21, V41, V43, V44, AC12, AC14 | Revisao inicial aceita carga ficticia rastreavel; contagem resolve diferenca por ajuste com motivo; saldo/reserva/ocupação consistentes; folha contingencia replay nao duplica |
| D29-FISCAL-SERVICOS | RN03, RN07, RN08, RN09, RN26, RN27, RN29, PR02, PR05, PR09, PR10, V10, V18, V19, V20, V34, V40, V47, AC02, AC06, AC09, AC13 | Regra fiscal por operação e vigência; documento existente sem emissão; serviço com origem/vinculo posterior não duplica; responsabilidade transporte permanece externa |
| D29-FINANCEIRO | RN08, RN21, RN27, RN28, RN29, I07, PR02, PR04, PR09, V12, V19, V20, V35, V36, V37, V45, V46, V48, AC04, AC05, AC06, AC07, AC08 | Todos insumos ficticios; numericos independentes F01..39; local sintetico separado de previsão real e aprovação ciclo fechado; histórico antigo imutável; PENDENTE=null |
| D29-CONCORRENCIA | RN24, RN29, PR01, PR02, PR06, V13, V14, V15, V42 | HTTP real barreira de inicio, repeticoes/controlos;200/409, perdedor sem efeitos, replay unico e SELECT posterior; nenhum gateSQL; sem prova nativa locks |
| D29-FIN-F01 | AC04, V35 | {'posicoesDia': '1', 'armazenagem': '7.00'} |
| D29-FIN-F02 | AC04, V35, AC06 | {'armazenagem': '0.00', 'GRIS': '15.00', 'picoFisicoExiste': True} |
| D29-FIN-F03 | AC04, RN19 | {'posicoesDia': '2', 'armazenagem': '14.00'} |
| D29-FIN-F04 | AC04, AC05 | {'picosCobraveis': ['1', '2', '2'], 'armazenagem': '35.00', 'ocupacaoFisicaMesmoEnderecoAposSeparacao': 1} |
| D29-FIN-F05 | AC04 | {'posicoesDia': '2', 'armazenagem': '14.00'} |
| D29-FIN-F06 | AC04 | {'picosCobraveis': ['1', '1'], 'armazenagem': '14.00'} |
| D29-FIN-F07 | AC05, V30 | {'armazenagem': '42.00', 'entrada': '4.00', 'saida': '5.00', 'total': '51.00'} |
| D29-FIN-F08 | AC04, AC05 | {'posicoesDia': '5', 'armazenagem': '35.00'} |
| D29-FIN-F09 | AC04, AC10, V24 | {'armazenagem': '14.00', 'valorRemanescente': '720.00', 'posicaoFisica': '1'} |
| D29-FIN-F10 | AC08, V46 | {'armazenagem': '18.20', 'posicaoFisica': '1', 'suspensaoFinanceira': '0.2', 'espacoLiberado': '0'} |
| D29-FIN-F11 | AC08, Q18 | {'armazenagem': '21.00', 'valorIndicador': '800.00', 'fisicoTotal': 100} |
| D29-FIN-F12 | AC08 | {'situacao': 'PENDENTE', 'total': None} |
| D29-FIN-F13 | AC08 | {'armazenagem': '9.80', 'suspensoes': ['0.2', '0.4'], 'posicaoFisica': '1'} |
| D29-FIN-F14 | AC08 | {'situacao': 'PENDENTE', 'total': None} |
| D29-FIN-F15 | AC08 | {'armazenagem': '19.60', 'picosLiquidos': ['1', '0.8', '1']} |
| D29-FIN-F16 | AC04, AC06, V48 | {'armazenagem': '28.00', 'memoriaTarifas': ['5', '5', '9', '9']} |
| D29-FIN-F17 | AC06 | {'situacao': 'PENDENTE', 'total': None} |
| D29-FIN-F18 | AC06 | {'situacao': 'COMPLETO', 'total': '0.00'} |
| D29-FIN-F19 | AC06, RN08 | {'situacao': 'PENDENTE', 'total': None} |
| D29-FIN-F20 | AC04, AC06 | {'armazenagem': '18.00', 'tarifaAgregada': None, 'regras': 2} |
| D29-FIN-F21 | AC06 | {'complementoMinimo': '20.00', 'total': '66.00'} |
| D29-FIN-F22 | AC06 | {'complementoMinimo': '0.00', 'total': '76.00'} |
| D29-FIN-F23 | AC06, AC07 | {'minimoAplicavel': '30.00', 'complemento': '18.00', 'total': '30.00'} |
| D29-FIN-F24 | AC06 | {'NAO_INFORMADO': 'PENDENTE', 'NAO_APLICAVEL': 'COMPLETO_SE_DEMAIS_DADOS', 'APLICAVEL_ZERO': 'COMPLETO_SE_PARAMETROS_EXPLICITOS'} |
| D29-FIN-F25 | AC06 | {'GRIS': '20.00'} |
| D29-FIN-F26 | AC06 | {'GRIS': '13.33'} |
| D29-FIN-F27 | AC06 | {'GRIS': '40.00', 'duracaoMultiplicadaNovamente': False} |
| D29-FIN-F28 | AC06 | {'GRIS': '10.00'} |
| D29-FIN-F29 | AC06 | {'GRIS': '300.00'} |
| D29-FIN-F30 | AC08, Q18 | {'semOrigem': 'PENDENTE', 'totalSemOrigem': None, 'valorComOrigemA': '1400.00'} |
| D29-FIN-F31 | AC07, V45 | {'diasMensais': 31, 'diasIntervalo': 30, 'sobreposicaoDias': 0} |
| D29-FIN-F32 | AC07 | {'dias': [28, 31], 'corteNominal': 31, 'sobreposicao': 0} |
| D29-FIN-F33 | AC07, V36 | {'versao1': 'SUPERADA', 'versao2': 'PENDENTE_REVISAO', 'memoriaAnteriorImutavel': True} |
| D29-FIN-F34 | AC07, PR10 | {'reabertura': 'RECUSADA', 'versaoNovaCriada': False} |
| D29-FIN-F35 | AC07 | {'deltas': ['-20.00', '-10.00'], 'acumulado': '-30.00', 'originalImutavel': True} |
| D29-FIN-F36 | AC06, AC09 | {'valor': '10.00'} |
| D29-FIN-F37 | AC09 | {'parcelas': {'11': '0.01', '12': '0.00'}, 'soma': '0.01'} |
| D29-FIN-F38 | AC04, AC07 | {'dataCivil': '2026-10-07'} |
| D29-FIN-F39 | AC06, AC08 | {'antigoImutavel': True, 'calculoAtual': 'COMPLETO_SE_TODOS_DADOS'} |
