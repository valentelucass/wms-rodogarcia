D30 Cedro — caminhos documentais e lacunas

V40.02/AC09.06: há pedidoSaidaId opcional no registro inicial de FatoServicoDto.Registrar, mas FatoServicoService.registrar recusa fato já confirmado; FatoServico não permite atualizar esse vínculo e o controller não oferece associação posterior. Não encontrado equivalente no atlas. Correção mínima proposta no JSON: conservar identidade e toda execução, admitindo apenas vínculo futuro real do mesmo contexto, com comparação integral dos dados e cotas, replay e auditoria identificados. Ainda não implementada ou executada.

Peso: NfeXmlService lê qCom/uCom/vProd, enquanto EstoqueDto.Medidas.pesoKg é medida explícita usada por MovimentacaoEstoqueService. Não existe captura do peso da nota nem ligação comprovada à medida física. Não converter KG/DUN automaticamente. Farol trata a parcela documental; o pedido medido e a retirada exata continuam na fila local. AC15 e arquitetura conservam os limites documental, externo e frontend.

JSON 636CD9AABF92371BB2B73139E465D8A722850721F1B4FB4FC269D06542F49F0A. Snapshot completo v02 preserva as fontes; captura v01 parcial foi mantida e qualificada. Zero ações SQL Server; nenhum aceite integral.
