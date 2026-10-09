# D29 — oráculos e limites do complemento local

A proposta e os snapshots anteriores permanecem intactos. Esta adenda detalha a autoridade de cada assertiva, sem alterar os testes nem as fontes produtivas.

No rateio de três notas, são normativos a conservação do valor único, as cotas explícitas e o determinismo pela ordem de IDs (doc29:43/53/93). Para R$1,00 com cotas 1/1/1, a derivação independente é 100 centavos: duas parcelas de 33 e uma de 34, soma exatamente 100, sem replicar o total por nota. As seis permutações precisam preservar os mesmos valores por ID e as mesmas cotas.

A norma não fixa qual ponta recebe o resíduo. O teste fixa o ID33 com 34 centavos **somente como a escolha técnica existente**, enquanto ID11 e ID22 recebem 33. O parecer anterior `orchestracao/.runtime/d29-vigia-financeiro39-ampliado.json`, F37, já distingue essa escolha do aceite comercial. A proposta anterior não deve ser interpretada como aprovação do gestor para essa ponta. O oráculo de conservação/permutação é separado da assertiva que protege a escolha técnica atual.

O registro real recebe cada ordem do comando e grava cotas nessa ordem nos mocks. A consulta simulada devolve as cotas por ID ascendente, conforme o contrato explícito `findByFatoIdOrderByNotaIdAsc`. O calculador real distribui os centavos. Isso cobre registro → fronteira simulada ordenada → cálculo, mas não prova a implementação SQL do ORDER BY, persistência, transação, rollback, permissões nativas ou HTTP.

Para configuração (doc29:90), o oráculo é a faixa expressa 1..31: corte 1 com duração ausente aceito pelo serviço real; 0/32 recusados respectivamente por Min/Max no proxy de validação de método. A recusa ocorre com zero interações em todos os colaboradores. Esse predicado local não substitui resposta HTTP nem evidência de rollback/persistência SQL Server.

A primeira tentativa produziu XML 9/0/0/0, mas seu auxiliar PowerShell interrompeu a captura final ao tratar o aviso stderr do Mockito como erro fatal. O log, XML e classes01 foram preservados. O runner02 captura stdout/stderr juntos e respeita o código de saída nativo; compilação e verify02 terminaram com código0. A repetição dos nove casos foi necessária para concluir o ciclo Maven com saída verificável; as duas execuções não serão somadas. Não houve red de comportamento de produção ou correção produtiva.

498, 14/0, tentativa focal01 e focal02 são execuções distintas. Este complemento fecha somente as duas lacunas locais delimitadas para revisão por Vigia. Corte financeiro real, aprovação nativa, preservação final1895+5, witnessSQL300, incidente e validações externas continuam nos seus impedimentos individuais.
