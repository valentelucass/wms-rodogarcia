# D29 — confronto dos39 exemplos financeiros

Valores/precondicoes conferidos no fonte/testcase/XML; nenhum metodo/HTTP200 fecha uma clausula automaticamente. Datas sinteticas somente local; H2/mocks separados de WMS_DEV real.

| ID | Estado delimitado | Limite |
| --- | --- | --- |
| D29-VIG-F01 | PROVA_LOCAL_DELIMITADA | Retirada excluida agora exercitada; SQLDEV dos dias distintos nao simulado. |
| D29-VIG-F02 | PROVA_LOCAL_DELIMITADA | Intervalo fisico e GRIS no mesmo dia em motor local; nao retirada fiscal/Clock DEV. |
| D29-VIG-F03 | PROVA_LOCAL_DELIMITADA | Tres movimentos datados nas fontes isoladas nao somam equivalencia; nao confirma movimento fisico de armazem. |
| D29-VIG-F04 | PROVA_LOCAL_DELIMITADA | Picos1/2/2 e35 locais; liberacao/ocupacao no mesmo endereco nao representada por estes mocks, precisa prova HTTP/SELECT pertinente. |
| D29-VIG-F05 | PROVA_LOCAL_DELIMITADA | Identidades distintas sucessivas e retirada; sem alegar enderecamento real. |
| D29-VIG-F06 | PROVA_LOCAL_DELIMITADA | Passagem retirada no mesmo dia e outra permanencia; pico local14. |
| D29-VIG-F07 | PROVA_LOCAL_DELIMITADA | Complemento tem tipos ENTRADA/SAIDA e quantidades1/1, valores4/5; duas posicoes42, total51. Nao prova geracao automatica unica dos fatos por HTTP. |
| D29-VIG-F08 | PROVA_LOCAL_DELIMITADA | Fato de equivalencia1->2 datado no modelo isolado, quantidade100->100; nao prova comando HTTP que altere medidas registradas nem origem do fato AJUSTE_ESTOQUE. |
| D29-VIG-F09 | PROVA_LOCAL_DELIMITADA | Retirada20 e remanescente80; origem900 gera720. Confirmacao/posicao persistida tem prova distinta na familia47. |
| D29-VIG-F10 | PROVA_LOCAL_DELIMITADA | Ocorrencia no inicio do segundo dia; F19Lume intradia tem expectativas diferentes preservadas. |
| D29-VIG-F11 | PROVA_LOCAL_DELIMITADA | Responsabilidade CLIENTE e indicador800; fluxo Gestor/SQL separado. |
| D29-VIG-F12 | PROVA_LOCAL_DELIMITADA | Tres negativos: responsabilidade desconhecida, cadeia temporal incompatível e flag antiga sem data. Nao fabricar datas nulas em cadastro valido. |
| D29-VIG-F13 | PROVA_LOCAL_DELIMITADA | Marco afetados20 comprovado apos retirada100->50; motor publico9,80 e suspensoes0,2/0,4. Registro de marco SQL nao inferido. |
| D29-VIG-F14 | PROVA_LOCAL_DELIMITADA | Agregado160>100 totalNULL; nenhuma suposicao de partes disjuntas. |
| D29-VIG-F15 | PROVA_LOCAL_DELIMITADA | Reparo datado isolado; evento fisico DEV/releitura separados. |
| D29-VIG-F16 | PROVA_LOCAL_DELIMITADA | Vinculos contiguos [inicio,fim), tarifas5/9; motor28, sem SQL/retroatividade DEV. |
| D29-VIG-F17 | PROVA_LOCAL_DELIMITADA | Item aplicavel ausente em tabela válida; nao cadastro ilegal com precoNULL. |
| D29-VIG-F18 | PROVA_LOCAL_DELIMITADA | Zero explicito no local; nao ausencia convertida para zero. |
| D29-VIG-F19 | PROVA_LOCAL_DELIMITADA | Sem vinculo nao fallback; vigencias SQL precisam contrato próprio. |
| D29-VIG-F20 | PROVA_LOCAL_DELIMITADA | PALLET7/BOBINA11 separados,18 e tarifa agregadaNULL; nao tarifa media. |
| D29-VIG-F21 | PROVA_LOCAL_DELIMITADA | Local66 e HTTP53/25 previsao66; nao ciclo aprovado. |
| D29-VIG-F22 | PROVA_LOCAL_DELIMITADA | Local76 e HTTP53/25 previsao76; minimo0 nao soma50. |
| D29-VIG-F23 | PROVA_LOCAL_DELIMITADA | Proporcao10/30 isolada, nao periodo real ficticio de10dias. |
| D29-VIG-F24 | PROVA_LOCAL_DELIMITADA | Tres modos locais distintos; nenhuma aprovacao comercial. |
| D29-VIG-F25 | PROVA_LOCAL_DELIMITADA | Picos reais da fixture isolada1000/2000/1000; POR_CICLO20. |
| D29-VIG-F26 | PROVA_LOCAL_DELIMITADA | Mesmos picos, MEDIA13,33 HALF_UP; nao valores históricos fabricados no DEV. |
| D29-VIG-F27 | PROVA_LOCAL_DELIMITADA | DIARIA40, sem segundo fator temporal. |
| D29-VIG-F28 | PROVA_LOCAL_DELIMITADA | POR_CICLO10/30=10 somente local. |
| D29-VIG-F29 | PROVA_LOCAL_DELIMITADA | DIARIA10dias=300 somente local. |
| D29-VIG-F30 | PROVA_LOCAL_DELIMITADA | PENDENTE para UL com origens40@10+60@20 e dano20 sem origem. Positivo1400 prova origens em identidades separadas A/B; nao identifica dano por origem dentro de UL reagrupada. Este restante permanece explícito. |
| D29-VIG-F31 | PROVA_LOCAL_DELIMITADA | Fixture corrigida: MES_DIA_FIXO sem duracaoDias; DIAS_CORRIDOS sem diaCorte;31 versus30/contiguidade. |
| D29-VIG-F32 | PROVA_LOCAL_DELIMITADA | Dia nominal31 preservado e duracaoDiasNULL mensal;28/31. SQL e ano bissexto em outros testcase/build separados. |
| D29-VIG-F33 | PROVA_LOCAL_DELIMITADA | H2 novo build verifica ciclo aprovado/bytes; DEV apenas reabertura de revisao76->106/v1SUPERADA. Aprovacao nativa v2 ainda futuro08/10, nao aprovação comercial. |
| D29-VIG-F34 | PROVA_LOCAL_DELIMITADA | Teste atual HTTP local/H2 com Clock isolado da suite existente; nenhuma emissao externa. Corte nativo DEV08/10 impede fluxo aprovado/entrega. |
| D29-VIG-F35 | PROVA_LOCAL_DELIMITADA | Teste local/H2100->80->70/deltas-20/-10 e original imutavel. Nativo depende ciclo realmente aprovado, impedido ate08/10. |
| D29-VIG-F36 | PROVA_LOCAL_DELIMITADA | Execucao antiga e registro tardio apenas fonte isolada; tarifa5 produz10 sem historico falso DEV. |
| D29-VIG-F37 | PROVA_LOCAL_DELIMITADA | Refinamento independente preservado: contrato determina ordem/soma/determinismo, nao ponta do residuo. Ordem11/12 repetida, soma.01/um unico centavo; nao declarar11=.01 como obrigatorio. |
| D29-VIG-F38 | PROVA_LOCAL_DELIMITADA | Exato instante2026-10-08T02:59:59Z vira07/10America/Sao_Paulo, motor inclui servico no dia civil07. So isolado. |
| D29-VIG-F39 | PROVA_LOCAL_DELIMITADA | Snapshot PENDENTE/NULL depois configuracao completa e IDs/hash novos no motor; GET DEV pendente original apos106 em complemento, SQL distinto. |
