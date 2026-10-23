# Regras de negócio da retirada fiscal e da integração Notazz

Este inventário reúne as regras que a integração de NF-e de mercadoria precisa preservar e os critérios para conferir sua implementação. O pedido de 22/10/2026 reúne as opções 1 e 2 da área Fiscal e retirada em **Retirada fiscal da mercadoria**, com consulta dos pedidos, conferência dos dados e emissão pela Notazz. A autorização fiscal continua distinta da confirmação da retirada física, da baixa do estoque e do faturamento dos serviços.

As regras estão identificadas de forma estável de FIS-R001 a FIS-R107. Este registro é documental: não implementa integração nem modifica cadastros, parâmetros tributários, dados operacionais ou documentos emitidos. Comportamentos observados no código não equivalem a homologação fiscal. Os critérios abaixo são cenários de aceite a aplicar; a leitura de testes existentes não é uma nova execução. O [mapeamento técnico da integração](46-mapeamento-fiscal-notazz.md) detalha a API, os [cenários e a validação](48-cenarios-e-validacao-fiscal-notazz.md) organizam a conferência futura e o [alinhamento posterior](49-alinhamento-fiscal-respostas-e-exemplos.md) consolida as respostas do gestor, incluindo a terceira rodada de 23/10/2026, e os DANFEs.

## Proveniência e leitura das evidências

| Marca | Significado | Como usar |
| --- | --- | --- |
| E | Regra expressa no pedido atual, no PDF, nas respostas Q ou em decisão expressa posterior | Preservar o sentido da fonte; observar eventuais conciliações identificadas separadamente. |
| I | Interpretação ou proposta anterior, identificada como I, PR ou AC | Preservar sua classificação; implementação anterior não a transforma em aprovação tácita da operação ou do fiscal. |
| C | Comportamento ou limite constatado na fonte atual do código | Orienta compatibilidade e cenários; não comprova execução atual nem adequação tributária. |
| P | Proposta desta análise para concretizar o novo fluxo | Desenho recomendado, ainda sem atribuir aprovação ao responsável. |

Quando uma linha reúne marcas, a evidência indica qual parte é expressa, interpretada ou implementada. Os identificadores de fonte abaixo representam caminhos relativos a esta pasta `docs`; `fonte:linha` aponta a linha inicial pertinente, e um intervalo delimita o trecho. Para o PDF, usa-se a página interna do original. Para o Word, usam-se os temas Q e os apêndices, pois a paginação não foi verificada. As imagens de exemplo não definem preços ou tributos de uso real.

| Fonte | Caminho ou origem |
| --- | --- |
| PEDIDO | Mensagem do responsável em 22/10/2026 com o requisito encaminhado de Fiscal e retirada; complementos autorizam análise e documentação, preservando código/configuração/banco. |
| D49 | [49-alinhamento-fiscal-respostas-e-exemplos.md](49-alinhamento-fiscal-respostas-e-exemplos.md), respostas posteriores do gestor e conferência dos dois DANFEs; fonte expressa de emitente, destinatário, agrupamento, valor e procedimento de desistência. |
| PDF | [referencias/Especificacao_Funcional_WMS_Rodogarcia.pdf](referencias/Especificacao_Funcional_WMS_Rodogarcia.pdf), versão 1.0, páginas internas 1 a 10. |
| WORD | [referencias/Questionamentos_WMS_Rodogarcia_2026-10-05.docx](referencias/Questionamentos_WMS_Rodogarcia_2026-10-05.docx), respostas recebidas aos temas Q01 a Q27 e apêndices. |
| D02 | [02-arquitetura.md](02-arquitetura.md) |
| D03 | [03-regras-de-negocio.md](03-regras-de-negocio.md) |
| D04 | [04-fluxos-operacionais.md](04-fluxos-operacionais.md) |
| D10 | [10-respostas-recebidas-2026-10-05.md](10-respostas-recebidas-2026-10-05.md) |
| D11 | [11-alinhamentos-apos-respostas.md](11-alinhamentos-apos-respostas.md) |
| D14 | [14-cadastros-acesso-e-persistencia.md](14-cadastros-acesso-e-persistencia.md) |
| D24 | [24-pedido-saida-fifo-e-reserva.md](24-pedido-saida-fifo-e-reserva.md) |
| D27 | [27-separacao-retirada-retornos-e-avaria.md](27-separacao-retirada-retornos-e-avaria.md) |
| D29 | [29-cadastros-servicos-e-calculo.md](29-cadastros-servicos-e-calculo.md) |
| D45 | [45-pedido-entrada-manual-xml.md](45-pedido-entrada-manual-xml.md) |
| DTO-FISCAL | [../backend/src/main/java/br/com/rodogarcia/wms/dto/FiscalCadastroDto.java](../backend/src/main/java/br/com/rodogarcia/wms/dto/FiscalCadastroDto.java) |
| DTO-PRODUTO | [../backend/src/main/java/br/com/rodogarcia/wms/dto/ProdutoDto.java](../backend/src/main/java/br/com/rodogarcia/wms/dto/ProdutoDto.java) |
| DTO-SAIDA | [../backend/src/main/java/br/com/rodogarcia/wms/dto/PedidoSaidaDto.java](../backend/src/main/java/br/com/rodogarcia/wms/dto/PedidoSaidaDto.java) |
| DTO-EXP | [../backend/src/main/java/br/com/rodogarcia/wms/dto/ExpedicaoDto.java](../backend/src/main/java/br/com/rodogarcia/wms/dto/ExpedicaoDto.java) |
| DTO-XML | [../backend/src/main/java/br/com/rodogarcia/wms/dto/NfeImportacaoDto.java](../backend/src/main/java/br/com/rodogarcia/wms/dto/NfeImportacaoDto.java) |
| S-EXP | [../backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java) |
| S-SAIDA | [../backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java) |
| S-SAIDA-XML | [../backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaXmlService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaXmlService.java) |
| S-FISCAL | [../backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java) |
| S-XML | [../backend/src/main/java/br/com/rodogarcia/wms/services/NfeXmlService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/NfeXmlService.java) |
| S-DOCUMENTO | [../backend/src/main/java/br/com/rodogarcia/wms/services/NfeDocumentoService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/NfeDocumentoService.java) |
| S-CONVERSAO | [../backend/src/main/java/br/com/rodogarcia/wms/services/ConversaoUnidadeNfe.java](../backend/src/main/java/br/com/rodogarcia/wms/services/ConversaoUnidadeNfe.java) |
| S-DEVOLUCAO | [../backend/src/main/java/br/com/rodogarcia/wms/services/RegistroDevolucaoService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/RegistroDevolucaoService.java) |
| S-ACESSO | [../backend/src/main/java/br/com/rodogarcia/wms/services/AcessoService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/AcessoService.java) |
| S-RESOLUCAO | [../backend/src/main/java/br/com/rodogarcia/wms/services/ResolucaoCadastroService.java](../backend/src/main/java/br/com/rodogarcia/wms/services/ResolucaoCadastroService.java) |
| S-SUPPORT | [../backend/src/main/java/br/com/rodogarcia/wms/services/CadastroSupport.java](../backend/src/main/java/br/com/rodogarcia/wms/services/CadastroSupport.java) |
| M-NOTA | [../backend/src/main/java/br/com/rodogarcia/wms/models/NotaEntrada.java](../backend/src/main/java/br/com/rodogarcia/wms/models/NotaEntrada.java) |
| M-ITEM-NOTA | [../backend/src/main/java/br/com/rodogarcia/wms/models/ItemNotaEntrada.java](../backend/src/main/java/br/com/rodogarcia/wms/models/ItemNotaEntrada.java) |
| M-DOCUMENTO | [../backend/src/main/java/br/com/rodogarcia/wms/models/DocumentoSaida.java](../backend/src/main/java/br/com/rodogarcia/wms/models/DocumentoSaida.java) |
| M-REFERENCIA | [../backend/src/main/java/br/com/rodogarcia/wms/models/ReferenciaFiscalProduto.java](../backend/src/main/java/br/com/rodogarcia/wms/models/ReferenciaFiscalProduto.java) |
| M-MEDIDAS | [../backend/src/main/java/br/com/rodogarcia/wms/models/MedidasUnidade.java](../backend/src/main/java/br/com/rodogarcia/wms/models/MedidasUnidade.java) |

## Tela e resultado fiscal solicitado

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R001 | E | Reunir as opções atuais 1 e 2 em uma opção chamada Retirada fiscal da mercadoria. | PEDIDO, agrupamento expresso. | A jornada oferece a opção única com o nome solicitado; os fatos fiscais e físicos continuam identificáveis. |
| FIS-R002 | E + P | Relacionar todos os pedidos de saída do alcance autorizado; situações operacionais não devem ocultar silenciosamente pedidos da consulta. Alcance e filtros são a proposta de aplicação desse requisito. | PEDIDO; D24:45-51; D27:65. | Rascunho, separado e retirado podem ser consultados dentro do alcance; filtro ativo fica visível. |
| FIS-R003 | E | Mostrar número do pedido, data do pedido, cliente e status da NF-e. | PEDIDO. | Cada linha identifica pedido e cliente sem obrigar abertura do detalhe. A data do pedido não é substituída pela data da NF-e. |
| FIS-R004 | E | Exibir PENDENTE quando a NF-e ainda não tiver sido emitida. | PEDIDO. | Pedido sem NF-e emitida recebe o texto solicitado, inclusive quando faltar cadastro para emitir. |
| FIS-R005 | E | Exibir FINALIZADO quando a NF-e tiver sido emitida. | PEDIDO; interpretação operacional do resultado detalhada nas regras de cobertura e de confirmação fiscal abaixo. | A situação da NF-e não renomeia o pedido operacional para RETIRADO. |
| FIS-R006 | E | Disponibilizar EMITIR NFe na coluna Nº NFe de um pedido pendente. | PEDIDO. | A ação abre a conferência do pedido; impedimento operacional ou fiscal tem motivo legível, sem envio indevido. |
| FIS-R007 | E | Mostrar o número da NF-e na coluna Nº NFe quando finalizado. | PEDIDO. | Número retornado fica vinculado ao pedido correto e permanece consultável após recarregar a lista. |
| FIS-R008 | E | Ao escolher emitir, apresentar os dados do pedido de saída para conferência. | PEDIDO; PDF p.7 §9. | A conferência inclui contexto, partes fiscais, itens, quantidades, origens e valores efetivamente preparados, sem completar lacunas por suposição. |
| FIS-R009 | E | A confirmação da conferência solicita a emissão pela integração Notazz. | PEDIDO. | Abrir ou consultar o detalhe não emite; somente o comando confirmado inicia a solicitação correspondente. |
| FIS-R010 | E | Buscar no retorno o número da NF-e e associá-lo à solicitação e ao pedido. | PEDIDO. | Uma resposta referente a outro contexto não preenche o número da linha selecionada. |
| FIS-R011 | E | Se suportado pela integração e pela configuração fiscal, informar o número da saída do WMS nas observações da NF-e. | PEDIDO, requisito condicionado por se possível. | Observação utiliza a referência real do pedido; não substitui a identidade persistida da solicitação nem apaga outras observações necessárias. |
| FIS-R012 | E + P | Preservar várias NF-e no mesmo pedido; agrupamento segue critério do cliente. Bracell pode reunir várias notas de entrada em uma saída. Apresentar todos os números quando o plano exigir várias saídas é proposta de apresentação. | WORD Q13.b; D10:55; D11:25; D49, resposta agrupamento. | Bracell não é obrigada a uma saída por origem; quando houver duas saídas, ambas aparecem sem sobrescrever a primeira e sem perder cobertura por origem. |
| FIS-R013 | E + I + C | Emissão fiscal e confirmação da retirada física são fatos distintos. | PDF p.9 §15; D03:53; D11:13-17, AC01; D27:9. | Nota emitida hoje e retirada amanhã mantêm estoque físico até a confirmação de amanhã. |
| FIS-R014 | E | Transporte e entrega permanecem no TMS; WMS não controla veículo, motorista, rota, CT-e ou MDF-e. | PDF p.6 §§8.5-8.6; D03:13,35. | Dados eventualmente exigidos na NF-e não criam um módulo de execução de transporte no WMS. |
| FIS-R015 | E | Transferência entre armazéns está fora da primeira versão. | WORD Q11.a-b; D10:61. | O fluxo não cria pedido interarmazéns ou estoque em trânsito por interpretar retirada como transferência. |
| FIS-R016 | E | Notazz trata documentos de mercadoria; ESL emite NFS-e dos serviços. | WORD Q13.c e Q17.d; D10:61. | Taxa de armazenagem ou paletização não vira automaticamente um produto da NF-e de retirada. |

## Proprietário e contexto fiscal

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R017 | E | O cliente proprietário na entrada é o emitente da nota; a decisão D33 aplica a regra a todas as entradas. | WORD Q12.a; D10:45; D45:3,7. | Importação não permite escolher como proprietário uma empresa diferente do emitente sem fluxo autorizado distinto. |
| FIS-R018 | E | O destinatário/armazém da entrada guarda a mercadoria e não se torna proprietário por recebê-la. | D45:3, decisão D33. | Não usar o destinatário da nota de entrada como cliente proprietário da saída. |
| FIS-R019 | E | Mudança de proprietário enquanto a mercadoria permanece armazenada foi excluída. | WORD Q12.c; D10:45. | Emissão de retorno simbólico não muda automaticamente o dono do estoque. |
| FIS-R020 | E + C | Cada pedido identifica um único proprietário e um armazém/filial. | WORD Q07.c e Q12.b; D10:45; DTO-SAIDA:26-32. | Itens de outros proprietários ou de outro armazém não são agregados ao mesmo pedido. |
| FIS-R021 | E + C | O SKU pertence ao cadastro do cliente proprietário. | PDF p.3 §3.4; WORD Q01.c; S-SAIDA:151-158. | Produto de outro cliente, ainda que tenha o mesmo texto de SKU, é recusado. |
| FIS-R022 | E + I + C | Preservar nota de origem, produto, lote aplicável e referência FIFO ao preparar documentos. | WORD Q02.c e Q21; D11:128-132; DTO-SAIDA:69-80; D27:29. | Duas origens do mesmo SKU permanecem rastreáveis até a cobertura e a baixa. |
| FIS-R023 | E + C | O CFOP não precisa ser fixo no SKU; a referência fiscal é contextual por produto, armazém e operação. | PDF p.3 §3.4; RN07 em D03:17; M-REFERENCIA:21-47. | Duas operações do mesmo produto podem ter referências distintas sem duplicar o produto. |
| FIS-R024 | C | As referências atuais de NCM, CFOP, CEST, enquadramento livre e alíquotas ICMS/IPI são opcionais e não constituem motor tributário. | D29:21,93; DTO-FISCAL:101-118. | Cadastro salvo sem CFOP não é tratado como pronto para qualquer operação; ausência permanece identificada. |
| FIS-R025 | C | Documento fiscal cadastral é normalizado como texto; isso não valida dígito verificador ou situação fiscal. | D14:20; S-SUPPORT:33-35. | Documento aceito pelo cadastro pode ser recusado na conferência específica da emissão sem alterar o histórico do cadastro. |
| FIS-R026 | C | Cliente e armazém dispõem de razão social, IE, endereço textual, UF, país e contato; a presença do campo não garante preenchimento ou validade fiscal. | DTO-FISCAL:23-38; D29:21,91-92. | Conferência mostra dado ausente como pendência e dado cadastrado como sua fonte efetiva. |
| FIS-R027 | C | Não há no complemento atual código IBGE, indicador de IE/contribuinte, CRT/regime ou inscrição municipal estruturados. | DTO-FISCAL:23-38; D29:91-93. | Matriz da integração informa se cada dado aplicável vem da configuração Notazz ou exige ampliação futura; não chamar todo campo de obrigatoriamente ausente no emissor. |
| FIS-R028 | E + P | A NF-e de saída deve ser emitida pelo CNPJ do armazém onde ocorreu a entrada fiscal. O vínculo com a empresa/credencial Notazz é implementação futura dessa regra expressa. | D49, resposta emitente; WORD Q13.a-b. | Não emitir pelo CNPJ do proprietário ou de outro armazém. A origem efetiva e seu estabelecimento permanecem rastreáveis mesmo se houver mudança cadastral posterior. |
| FIS-R029 | E + C | A NF-e de retirada é sempre emitida para o proprietário da mercadoria. O pedido já identifica esse proprietário; não há outro destinatário autorizado nesta regra. | D49, resposta destinatário; DTO-SAIDA:26-32,82-94; D45:3. | A prévia usa o proprietário correto da origem e rejeita substituição por terceiro ou pelo próprio armazém como destinatário dessa saída. |
| FIS-R030 | P | Definir uma fonte por campo da emissão e a precedência entre dado WMS, configuração Notazz e escolha da operação. | Derivada de RN26, PDF p.7 §9; D29:21. | A prévia informa de onde veio CFOP/endereço/tributação; duas fontes divergentes não são combinadas silenciosamente. |
| FIS-R031 | E + P | Não inventar CFOP, origem, CST/CSOSN, PIS/COFINS, IPI, regimes, benefícios ou valores para completar um envio. A validação por campo é proposta. | D03:82; D11:190-200; D29:5,21; DTO-FISCAL:101-118. | Falta de parâmetro aplicável gera pendência concreta com a fonte responsável; ausência não equivale a zero, isenção ou não incidência. |
| FIS-R032 | E + C | Gestor mantém dados fiscais com revisão, motivo e auditoria; complemento não muda por si a identidade do cadastro. | WORD Q20.b; S-FISCAL:83-111,116-151,166-240. | Alteração concorrente de complemento é recusada pela versão; auditoria conserva antes/depois e autor. |
| FIS-R033 | C | Referência fiscal pode registrar fonte, responsável e data de conferência externa; responsável/data são um par, com data não futura. | DTO-FISCAL:115-118; S-FISCAL:183-190. | Responsável sem data ou data futura é recusado; declaração não transforma o Gestor em validador tributário externo. |
| FIS-R034 | E + C | Configurar cadastro fiscal não emite documento. | PEDIDO distingue cadastro e emissão; D29:21; S-FISCAL:83-240. | Salvar NCM ou endereço não cria solicitação de NF-e nem muda a situação da retirada. |
| FIS-R035 | P | Preservar os dados usados em cada solicitação/documento; correção posterior do cadastro não reescreve emissão anterior. | Derivada de RN29 em D03:39; PR04 em D03:67; D27:51. | Alterar IE ou CFOP permite nova preparação futura, mas mantém consultável o conteúdo que foi efetivamente confirmado/enviado. |

## Quantidades unidades peso e origens

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R036 | C | Pedido manual contém um item por produto/SKU e quantidade positiva. | DTO-SAIDA:21-32; S-SAIDA:147-149; D24:23. | Mesmo produto repetido no comando manual é recusado; não somar duplicatas por conveniência da emissão. |
| FIS-R037 | C | Quantidade de saída respeita precisão do produto e limite decimal do contrato. | DTO-SAIDA:21-24; S-SUPPORT:88-92; D24:45. | Quantidade com casas além da precisão é recusada, sem truncamento para fechar o pedido. |
| FIS-R038 | I + C | Produto contado exige precisão zero e não admite fração de unidade indivisível. | D11:124; D14:22; DTO-PRODUTO:22-24. | Pedido de meia unidade de produto contado não se torna válido por arredondamento. |
| FIS-R039 | I + C | Produto medido usa unidade e precisão cadastradas, até seis casas. | D14:22; DTO-PRODUTO:22-24; D24:45. | Um produto em KG conserva quantidade decimal exata; não é apresentado como número de bobinas. |
| FIS-R040 | E + C | O pedido inteiro deve ser atendido, inclusive quando possui mais de um SKU. | WORD Q07.a-b e Q22.d; S-EXP:548-564. | Documento ou estoque que atende apenas um dos dois itens não permite retirada parcial do pedido. |
| FIS-R041 | E + C | Para mudar a quantidade solicitada, cancelar o pedido inteiro e criar outro na quantidade correta. | WORD Q07.b; D10:43; D24:23. | Não editar quantidade de pedido já reservado para coincidir com uma nota emitida ou com falta constatada. |
| FIS-R042 | E + C | É permitido retirar parte do conteúdo de um pallet para atender integralmente o pedido. | WORD Q02.a; D27:7; S-EXP:385-395,427-442. | Retirar 100 de pallet com 500 conserva 400, a origem e a identidade física remanescente. |
| FIS-R043 | C | Bobina é indivisível no recorte operacional atual. | D24:23,68; S-SAIDA:685-687. | Bobina de peso 6 não atende pedido de peso 4 por fracionamento; composição alternativa integral exige seleção válida. |
| FIS-R044 | E | Pedido por peso não tem tolerância aprovada para sair mais ou menos que a quantidade correspondente à origem. | WORD Q02.c; D10:21. | Não criar tolerância percentual para ajustar o peso da NF-e ao pedido ou à combinação física. |
| FIS-R045 | E + I + C | Peso previsto pela nota e contagem física de bobinas são informações distintas. | WORD Q01.b; D04:20, complemento aprovado FE05-XML-BOBINAS01; DTO-XML:38-56. | Peso total não gera automaticamente quantidade de bobinas nem peso individual presumido. |
| FIS-R046 | C | A conversão existente de massa aceita relações exatas entre KG e TO/T/TON, sem inferir embalagem ou contagem. | S-CONVERSAO:5-25. | 1 TO pode virar 1000 KG nesse conversor; 1000 KG não vira uma bobina. O suporte de entrada não implica que a expedição já converta unidades. |
| FIS-R047 | I + C | Quantidade de volumes não substitui quantidade do produto. | D03:51, I04; D45:9; DTO-XML:38-56. | Exemplo com 24 volumes e 32.000 peças mantém as duas medidas separadas. |
| FIS-R048 | C | Unidades e quantidades comercial e tributável do XML permanecem separadas. Quando as unidades coincidem e as quantidades divergem, o parser atual gera aviso, sem correção automática. | DTO-XML:45-51; S-DOCUMENTO:103-109,180-193. | Quantidade comercial 1,5870 e tributável 1,5480 na mesma unidade não são igualadas pelo sistema e geram aviso; seu uso fiscal exige regra validada. |
| FIS-R049 | C | Na conferência atual de XML da saída, a unidade comercial deve corresponder à unidade do produto e suas quantidades devem coincidir com a cobertura. | S-EXP:648-669. | XML em unidade incompatível é recusado; conversão fiscal futura deve ser explícita e não assumir suporte já existente. |
| FIS-R050 | I + C | Divisão e retirada parcial preservam a composição de origem e sua quantidade remanescente. | RN16/PR04 em D03:26,67; D27:7,29; S-EXP:409-426. | A soma das parcelas baixadas e remanescentes fecha com o conteúdo anterior; inconsistência impede a operação. |
| FIS-R051 | E + I | Reagrupamento exige mesmo produto, lote aplicável e data FIFO; a exigência adicional de mesma nota é proteção proposta em AC10. | WORD Q02.b e Q21.d; D11:130-132. | Não misturar recebimentos incompatíveis para simplificar a NF-e; marcar mesma nota como proposta, não resposta literal do gestor. |
| FIS-R052 | E + C + P | Usar o valor da nota de entrada proporcional à quantidade retirada dessa origem, inclusive no parcial de pallet. O pedido atual não transporta esse valor; cálculo decimal e conservação do saldo são necessários à futura implementação. | D49, resposta valores; M-ITEM-NOTA:40-49; DTO-SAIDA:21-32. | Parcela retirada não copia o total da entrada. Origens com preços distintos conservam seus valores mesmo em uma única NF-e; retiradas mais saldo fecham com o valor original. |

## Estoque reserva FIFO e separação

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R053 | E + C | FIFO usa chegada física original; nota recebida em partes usa a primeira chegada. | WORD Q21.a-b; D10:47; D24:29. | Data de emissão da NF-e e início da cobrança não substituem a data FIFO. |
| FIS-R054 | I + C | Desempate FIFO segue nível inferior, sequência de coleta, código do endereço e identidade estável após a data FIFO. | AC11 em D11:138-140; D24:29. | Código de endereço é desempate, não prova de distância física calculada. |
| FIS-R055 | E + I + C | Qualquer perfil autorizado pode justificar nota/unidade fora do FIFO; Supervisor/Gestor autoriza a exceção na reserva. | WORD Q08/Q20.c; D11:134-140; D24:31. | Justificativa não libera unidade avariada, indisponível ou de outro proprietário. |
| FIS-R056 | E + C | Triagem, quarentena, avaria, bloqueio e expedição não tornam mercadoria elegível a nova saída. | PDF p.6 §7; WORD Q05; D24:91. | Mesmo com documento fiscal pronto, impedimento físico continua bloqueando prosseguimento. |
| FIS-R057 | E + I + C | Confirmar reserva retira o comprometido da disponibilidade de outros pedidos, preservando o físico até a retirada. | PDF p.6 §8.4; I02 em D03:49; D24:25. | Reserva de 100 em pallet de 500 não baixa fisicamente 100 nem disponibiliza automaticamente o restante protegido. |
| FIS-R058 | E + C | Reserva não vence com o tempo, inclusive após separação, emissão ou problema fiscal. | WORD Q09.a-c; D10:49; D24:23. | Rejeição fiscal ou espera de vários dias não remove a reserva nem inicia nova reserva equivalente. |
| FIS-R059 | E | Urgência de outro pedido não autoriza transferir silenciosamente uma reserva; exige cancelar o pedido original. | WORD Q09.d; D10:49. | Segundo pedido não usa unidade comprometida enquanto o primeiro não tiver sido resolvido pelo fluxo aplicável. |
| FIS-R060 | E + I + C | Avaria posterior sinaliza o pedido e bloqueia expedição, conservando reserva e saldo físico até tratativa. | WORD Q04.e; PR11 em D03:74; D27:15. | Detectar avaria não substitui automaticamente a unidade nem libera saldo para outro pedido. |
| FIS-R061 | I + C | Revalidar disponibilidade e integridade ao confirmar a operação; consulta ou sugestão anterior pode ficar desatualizada. | PR01/PR06 em D03:64,69; D24:35,60; S-EXP:354-371. | Avaria surgida após abrir a conferência impede retirada, preservando dados e motivo do impedimento. |
| FIS-R062 | E + C | Ler cada unidade reservada pela etiqueta vigente; mudança de conteúdo ou reparo exige nova leitura/separação compatível. | WORD Q22.a; S-EXP:147-173,201-205,363-367; D27:43. | Etiqueta com UUID/revisão antiga não confirma a retirada. |
| FIS-R063 | I + C | Separação move a unidade física inteira; no parcial de pallet, o remanescente acompanha o mesmo pallet até a retirada. | D27:7; S-EXP:216-236. | Separar reserva de 100 não cria uma segunda unidade física de 100 ou libera as 400 restantes para outra separação. |
| FIS-R064 | E + C + P | Emitir após a separação completa. O registro atual de documento exige SEPARADO; aplicar a mesma elegibilidade à emissão integrada é proposta de compatibilidade. | WORD Q13.a; S-EXP:249. | Pedido em rascunho aparece na consulta, mas a emissão informa o que falta antes de enviar. |

## Documentos cobertura e acompanhamento

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R065 | C | Cada cobertura identifica reserva, nota de origem, SKU e quantidade compatíveis com o pedido. | DTO-EXP:58-63; S-EXP:268-275. | Cobertura repetida ou com origem/SKU divergente é recusada, mesmo quando o total agregado coincide. |
| FIS-R066 | C | Documentos físicos ativos não podem exceder a quantidade reservada. | S-EXP:277-280. | Duas notas com cobertura de 60 e 50 para reserva de 100 não são aceitas como conjunto válido. |
| FIS-R067 | I + C | Somente documentos ativos de retorno de mercadoria contam na cobertura necessária à retirada física. | D27:9,27; S-EXP:590-600. | Documento cancelado não cobre reserva; documento simbólico também não entra nessa soma. |
| FIS-R068 | E + I + C | Retorno simbólico pode ser operação somente fiscal, sem retirada física. Seu documento não baixa saldo nem encerra permanência por si só e não cobre sozinho a retirada física no contrato atual. | D49, segunda rodada, resposta sobre operação somente fiscal; WORD, apêndice Cadastro do Cliente; AC01 em D11:17; D27:9. | Registrar documento simbólico conserva o físico e sua localização; eventual retirada real segue confirmação e cobertura aplicáveis próprias. |
| FIS-R069 | E + I + C | Para retirar, a soma da cobertura física autorizada deve coincidir com todas as reservas do pedido inteiro. | WORD Q07/Q13; AC02 em D11:25; S-EXP:368-371. | Duas notas podem completar a cobertura; apenas a primeira, insuficiente, não libera a retirada. |
| FIS-R070 | E + C | Confirmação física exige XML de todos os documentos físicos ativos e coerência de identidade, SKU, unidade e quantidade. | WORD Q22.c; S-EXP:603-669. | XML de outra nota, comprovante repetido ou XML faltante impede retirada sem baixa parcial. |
| FIS-R071 | C | Identidade fiscal é protegida por emitente/série/número e chave de acesso quando disponível. | M-DOCUMENTO:19-27; M-NOTA:16-24,46-47. | Repetir o mesmo documento não cria outra cobertura equivalente com outro ID local. Número isolado não identifica unicamente uma NF-e. |
| FIS-R072 | C | Leitura local de XML não prova assinatura, autenticidade ou situação atual na SEFAZ. | S-XML:27-69; S-DOCUMENTO:173-176; D45:19. | XML estruturalmente legível não é apresentado como autorização fiscal consultada. |
| FIS-R073 | C | AUTORIZADO no cadastro manual atual é declaração local de documento existente; não é retorno de emissão integrada. | D27:9,26; S-EXP:284-302; M-DOCUMENTO:60-67. | Documento legado declarado fica identificável como tal; não inventar ID de solicitação ou confirmação externa para ele. |
| FIS-R074 | P | Na integração, tratar como FINALIZADO o plano documental completo da operação, com todos os documentos exigidos autorizados e conciliados, físicos e simbólicos conforme aplicável; mero recebimento da solicitação não basta. | Derivada de PEDIDO; RN26 em D03:36; AC02 em D11:25; distinção física em S-EXP:590-600. | Solicitação em processamento conserva PENDENTE. Uma exigência simbólica pendente pode impedir finalizar o plano fiscal sem alterar a soma da cobertura física. |
| FIS-R075 | P | Se houver várias notas exigidas, completar todo o plano documental antes de FINALIZADO; cancelamento que retire documento necessário volta a sinalizar pendência fiscal, com tratamento próprio se o pedido já foi retirado. | Derivada de WORD Q13.b; D11:25; S-EXP:590-600; FIS-R102. | Uma das duas notas cancelada não deixa a linha parecendo documentalmente completa; a retirada histórica já feita permanece registrada e não habilita reemissão livre. |
| FIS-R076 | I + P | Ausência de resposta é resultado desconhecido, não rejeição confirmada. | PR10 em D03:73; D04:67; proposta de aplicação à nova integração. | Timeout mantém a identidade da solicitação e explica a situação ao operador, sem marcar FINALIZADO ou gerar automaticamente outra NF-e. |
| FIS-R077 | I + P | Consultar o resultado de uma emissão desconhecida antes de repetir uma emissão equivalente. | D02:89-94; D03:73; D04:67. | Se a consulta localizar autorização já existente, recuperar documento/número em vez de emitir duplicado. |
| FIS-R078 | P | Guardar identidade estável da intenção de emissão e sua ligação ao pedido, aos itens/origens e ao resultado externo. | Derivada de PR02/PR10 em D03:65,73 e RN29 em D03:39. | Duplo clique, reconexão ou reinício permite consultar a mesma intenção; tentativa nova não ignora a intenção anterior ainda desconhecida. |
| FIS-R079 | E + P | Falha, rejeição ou pendência não conclui a baixa; apresentar motivo e encaminhamento sem perder a conferência. | PDF p.7 §9; RN26; proposta de apresentação para PEDIDO. | Rejeição fiscal mantém estoque/reserva e informa qual dado ou situação precisa de tratativa. |

## Cancelamento retorno interno e devolução

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R080 | E + I + C | Cancelamento fiscal é distinto de cancelamento do pedido e de retorno físico. | WORD Q10.a-b; D04:67,87; S-EXP:306-324. | Cancelar a NF-e não demonstra que a unidade voltou à posição ou que já pode ser usada por outro pedido. |
| FIS-R081 | C + P | O cancelamento hoje exposto registra localmente um cancelamento existente; envio e acompanhamento de cancelamento na Notazz são ampliação futura explícita. | D27:69; S-EXP:306-324. | Não rotular um cancelamento local como cancelamento externo executado. A nova integração deve distinguir solicitação, confirmação, rejeição e resultado desconhecido. |
| FIS-R082 | E + I + C | No retorno interno atual, todos os documentos ativos devem ter seu cancelamento registrado. | WORD Q10.a; D27:11; S-EXP:464-468. | Documento ainda ativo impede concluir o fluxo atual. A regularização por nota de entrada de FIS-R104 exige caminho próprio, sem falsificar cancelamento para satisfazer o contrato antigo. |
| FIS-R083 | E + I + C | Retorno interno confirma todas e somente as unidades separadas, com destinos válidos, antes de liberar suas reservas. | WORD Q10.b; D27:11; S-EXP:469-510. | Retornar apenas um dos dois pallets separados não libera o restante nem cancela pela metade. |
| FIS-R084 | E + I + C | Mercadoria avariada ou bloqueada retorna à quarentena e conserva o impedimento. | WORD Q04.e; D27:11,15; S-EXP:484-489. | Cancelar retirada de unidade avariada não a torna disponível em armazenagem comum. |
| FIS-R085 | E + C | O retorno interno atual à posição dispensa nova conferência de recebimento e não cria nova entrada. | WORD Q10.b; D10:59; S-EXP:449-510. | A mesma unidade conserva identidade, quantidade e origem, com movimento de retorno auditado. Não generalizar ao novo processo de entrada após o prazo, definido em FIS-R104. |
| FIS-R086 | E + C | No fluxo existente, mercadoria que já saiu fisicamente volta por novo pedido de entrada ligado à saída original. | WORD Q10.c; D27:13; S-EXP:514-526; complemento de D49 em 23/10/2026. | Devolução não apaga a retirada original nem reabre artificialmente seu saldo histórico. FIS-R103 confirma nova nota de entrada quando a circulação impede o cancelamento, mesmo dentro de 24 horas. |
| FIS-R087 | C | Quantidade devolvida por origem não pode exceder retirado menos devoluções anteriores. | S-DEVOLUCAO:83-103. | Devoluções sucessivas de 60 e 50 para baixa de 100 recusam o excesso; origem duplicada no comando é recusada. |
| FIS-R088 | E + C | Devolução preserva FIFO original e registra nova chegada real, posterior à retirada e não futura. | WORD Q21.c; D27:13; S-DEVOLUCAO:73-80,159-167. | Data FIFO não é trocada pela emissão da nova nota ou pela data de retorno. |
| FIS-R089 | C + P | Refaturamento ou substituição fiscal não pode reescrever a retirada já confirmada; precisa de vínculo fiscal identificado e regra validada. | Conservação em D27:13,51; S-EXP:522-526; proposta para a integração. | Documento substituto pode referenciar o anterior sem criar uma segunda baixa da mesma mercadoria. |

## Valores serviços e armazenagem

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R090 | E | Serviços já executados continuam cobrados quando o pedido é cancelado ou a mercadoria volta. | WORD Q10.d; D10:59. | Cancelar a NF-e não apaga fatos executados de entrada, armazenagem ou movimentação adicional. |
| FIS-R091 | E + I + C | Armazenagem continua durante reserva, separação e quarentena posterior; retirada física encerra a permanência cobrável. | WORD Q16.a-c; D10:72; AC01/AC04; D29:59. | Nota autorizada na segunda e coleta na terça não encerra a cobrança na segunda por causa da emissão. |
| FIS-R092 | E + I | Entrada e saída no mesmo dia não geram armazenagem; a convenção de calendário/pico permanece vinculada ao contrato e às propostas AC04-AC08. | WORD Q15.a-e; D10:68-70; D11:39-64. | Emissão no mesmo dia sem retirada não cria isenção de armazenagem; serviço de entrada/saída segue seu próprio fato. |
| FIS-R093 | E | Valor do estoque nos relatórios e valor fiscal da saída têm fontes expressas separadas: Q18 definiu o relatório; a resposta posterior definiu valor da entrada proporcional para a NF-e. | WORD Q18.a-c; D10:86; D49, resposta valores; FIS-R052. | Não usar preço de serviço ou preço atual de venda. O mesmo fato de origem pode alimentar os dois cálculos, respeitando finalidade, precisão e histórico de cada um. |
| FIS-R094 | E + I | Correção depois de nota de serviços emitida vai ao próximo ciclo de cobrança; essa regra não determina o cancelamento ou refaturamento da NF-e de mercadoria. | WORD Q17.c-d; D10:76; AC07 em D11:90-98. | Ajuste ESL/NFS-e mantém a memória do fechamento; eventual ajuste Notazz segue seu tratamento fiscal próprio. |

## Permissões encerramento e integridade

| ID | Origem | Regra | Evidência | Critério ou cenário |
| --- | --- | --- | --- | --- |
| FIS-R095 | E + I + C | Consultas e comandos exigem identidade válida e alcance simultâneo no cliente e no armazém; Gestor tem alcance geral. | WORD Q20.a; D24:45; S-ACESSO:21-35,42-80. | Conhecer ID de pedido ou documento de outro contexto não autoriza consulta, emissão ou recuperação de resultado. |
| FIS-R096 | E + C + P | Operação pode ler/separar; Supervisor/Gestor registra documento e confirma retirada. Conservar esse perfil para emitir é proposta; não criar exigência de segunda pessoa. | WORD Q20.e/Q22.c; D27:65-72; S-EXP:244,346. | Operação não emite por burlar a interface. Supervisor/Gestor pode executar sua própria confirmação permitida. |
| FIS-R097 | E + I + C | Cadastros em encerramento bloqueiam novos vínculos; Gestor pode resolver compromisso remanescente identificado em ENCERRAMENTO_PENDENTE, sem reativar cadastro INATIVO. | WORD Q24; AC12 em D11:144-150; S-RESOLUCAO:18-24. | Resolução autorizada permite concluir compromisso existente, mas não abre nova reserva comum nem contorna inativação definitiva. |
| FIS-R098 | I + C + P | Mesma identidade de operação e mesmo conteúdo devolvem o resultado original; conteúdo divergente é conflito. Verificar permissão atual também no replay; estender a proteção à intenção de emissão. | PR02; D24:37; D27:51,57; proposta para o envio externo. | Duplo clique não duplica documento; usuário que perdeu alcance não recupera resultado por repetir uma chave antiga. |
| FIS-R099 | E + I + C | Gravar efeitos locais indivisíveis e auditoria juntos, preservando autor, instante, motivo, referência de origem e resultado histórico. | RN29 em D03:39; PR04; D27:49-57. | Falha local não deixa cobertura sem documento ou baixa parcial. Atomicidade local não prova que uma chamada externa foi desfeita; o resultado externo exige reconciliação. |
| FIS-R100 | P | Coordenar emissão concorrente com cancelamento, avaria e alteração cadastral, revalidando a revisão conferida e preservando a solicitação externa já iniciada. | Derivada de PR01/PR02/PR11; D24:35-37; D27:49-51. | Dois operadores não iniciam emissão equivalente simultânea. Se a mercadoria for bloqueada após o envio, acompanhar a NF-e sem baixar estoque ou apagar a solicitação. |
| FIS-R101 | C + P | Documento NOTAZZ/XML existente sem identificador da API não significa ausência de emissão; incorporá-lo ao plano documental e conciliar sua identidade antes de propor novo envio. | Legado em DTO-EXP:141-150 e M-DOCUMENTO:44-75; proposta de migração funcional compatível com D11:23, que proíbe provocar outra emissão de XML já emitido. | Preservar os números existentes. A falta de vínculo com API ou a ausência de prova externa automática não habilita duplicação; emitir complemento apenas para necessidade documental demonstrada e sem intenção equivalente pendente. |
| FIS-R102 | C + P | O registro atual permite cancelar documento depois da retirada, enquanto registrar documento exige SEPARADO; pendência fiscal pós-retirada precisa de tratativa própria, sem reabrir saldo físico ou liberar reemissão comum. | S-EXP:249,306-324,354,444; proposta para integrar cancelamento/regularização ao plano documental. | Após cancelamento fiscal de um documento de pedido RETIRADO, manter a saída, indicar pendência e encaminhar regularização identificada. Conciliação de NF-e já emitida continua necessária mesmo se avaria/reparo tiver mudado o pedido durante o envio. |
| FIS-R103 | E + P | Dentro de 24 horas, distinguir cancelamento cabível sem circulação de retorno após circulação. Se a circulação impedir cancelar a NF-e, emitir nova nota de entrada e seguir o processo de retorno/reendereçamento, preservando a saída original. | D49, complemento do gestor em 23/10/2026, que resolve a pendência da segunda rodada. | Não cancelar automaticamente pelo prazo nem falsificar CANCELADO. Fiscal resolve a nova nota; operador processa entrada e reendereçamento da quantidade realmente retornada. Resposta externa desconhecida exige conciliação e não autoriza criar documento substituto automaticamente. |
| FIS-R104 | E + P | Depois de 24 horas, emitir nota de entrada, processar como entrada nova para regularizar físico/fiscal e exigir reendereçamento, inclusive se a mercadoria não tiver saído. | D49, segunda rodada, resposta sobre prazo vencido sem retirada. | Não reduzir esse caso a uma anotação fiscal sem novo processo operacional. Relacionar os ciclos e a nota original, conservar a quantidade física única e não declarar CANCELADO sem evento correspondente. |
| FIS-R105 | E + P | O retorno fiscal deve corresponder ao tratamento da entrada do cliente; o gestor confirmou que pode existir operação somente fiscal. | D49, respostas sobre tipo da operação e segunda rodada sobre retorno simbólico; FIS-R068. | Não copiar literalmente CFOP/direção/tributação da entrada nem inferir movimentação física pelo nome do documento. Operação apenas fiscal preserva saldo/localização enquanto não houver fato físico próprio. |
| FIS-R106 | E + I + P | O novo processo de entrada/regularização deve distinguir mercadoria já presente de mercadoria retornada, exigir o reendereçamento informado e conservar uma única quantidade real com histórico dos ciclos. | D49, segunda rodada; AC01 em D11:13-17 e regras de origem/FIFO existentes. | Nunca saiu: novo processo utiliza a mercadoria existente sem somar cópia. Saiu e retornou: reconstituir somente quantidade efetivamente retornada, preservando fatos e vínculos; os caminhos fiscais diferem conforme procedimento admitido. |
| FIS-R107 | E + P | Regularização começa pelo fiscal e, depois da resolução fiscal aplicável, o operador realiza o endereçamento. | D49, segunda rodada, resposta de responsabilidade e ordem. | O operador recebe uma pendência operacional identificada; processo fiscal pendente, recusado ou desconhecido não é apresentado como regularização concluída. Registrar responsáveis e sequência. |

## Dados que já existem e limites de aproveitamento

O quadro não é uma lista de campos obrigatórios universais da Notazz. A obrigatoriedade depende da operação, do estabelecimento e da configuração do emissor. Ele delimita o que a integração pode obter do modelo atual sem inventar preenchimento.

| Informação | Fonte atual | Limite para emissão |
| --- | --- | --- |
| Número e data do pedido | DTO-SAIDA:82-94; S-SAIDA:135-136, referência interna SAI | Não usar ID técnico ou número da nota como substituto silencioso da referência operacional. |
| Cliente e armazém | DTO-SAIDA:26-32; D49 | Proprietário é o destinatário fiscal; CNPJ do armazém da entrada é o emitente. O vínculo técnico usa essas regras já respondidas. |
| Razão social, IE, endereço e contato | DTO-FISCAL:23-38 | Campos majoritariamente opcionais; cidade/UF do armazém têm validação específica em S-FISCAL:127-133. |
| Código IBGE, indicador IE, CRT/regime e IM | Não integram DTO-FISCAL:23-38 | Definir fonte quando aplicáveis; a Notazz pode possuir esses dados em sua configuração. |
| Produto, descrição, unidade e precisão | DTO-PRODUTO:18-27 | Não há preço fiscal unitário nem GTIN de cadastro nesse contrato. |
| NCM, CFOP, CEST e alíquotas ICMS/IPI | DTO-FISCAL:101-118; M-REFERENCIA:49-66 | Referência contextual opcional; enquadramento é texto livre. |
| Origem da mercadoria, CST/CSOSN, PIS/COFINS e detalhamento de IPI/ICMS | Não integram DTO-FISCAL:101-118 | Não deduzir de ausência nem copiar automaticamente do XML de entrada; definir responsabilidade WMS/Notazz. |
| Chave da nota de origem | M-NOTA:34-54; reserva em DTO-SAIDA:69-80 | Pode faltar em entrada manual; a operação deve definir a exigência de referência antes do envio. |
| Quantidade e valor total do item de origem | M-ITEM-NOTA:40-49; D49 | Gestor confirmou proporção pelo valor da entrada. Ausência de valor continua sendo pendência de dado; conferir precisão e conservação por origem. |
| Campos completos preservados do XML | DTO-XML:38-85; M-NOTA:52-54 | Dados de prévia/original não se tornam automaticamente cadastro fiscal ou tributação aprovada para saída. |
| Peso físico da unidade logística | M-MEDIDAS:8-25 | Peso único não distingue líquido/bruto; volumes e pesos fiscais de retirada parcial não estão prontos por esse campo. |
| Natureza do registro local | DTO-EXP:65-75 | RETORNO_MERCADORIA/RETORNO_SIMBOLICO não são CFOP, finalidade ou regra tributária completos. |
| Identidade e situação de documento existente | DTO-EXP:141-150; M-DOCUMENTO:44-75 | Modelo atual registra documento declarado existente; não possui por si todo o acompanhamento da emissão integrada. |

## Cenários completos para conferência

Os cenários combinam as regras atômicas sem acrescentar aprovação de tratamento fiscal. Exemplos monetários ou de quantidade usados na futura validação devem ser fictícios e rastreáveis.

| Cenário | Resultado de referência | Regras principais |
| --- | --- | --- |
| Plano documental exige somente uma NF-e física, integralmente autorizada e conciliada; coleta posterior | Lista fiscal completa; físico e armazenagem permanecem até retirada confirmada com XML. | FIS-R013, FIS-R064, FIS-R069, FIS-R070, FIS-R074, FIS-R091 |
| Pedido exige duas NF-e e apenas uma foi autorizada | Mostrar o número conhecido e a pendência restante; não confirmar retirada nem ocultar a segunda emissão necessária. | FIS-R012, FIS-R066, FIS-R069, FIS-R075 |
| Notazz recebeu a solicitação, mas a conexão terminou sem resposta | Conservar identidade da intenção; consultar resultado antes de iniciar emissão equivalente; manter reserva e físico. | FIS-R076, FIS-R077, FIS-R078, FIS-R098 |
| Autorização existe na Notazz e a gravação local falhou | Recuperar o resultado externo e reconciliar a mesma intenção; não pressupor rollback da NF-e. | FIS-R078, FIS-R099 |
| Um documento simbólico foi registrado | Preservar registro fiscal e origem; não usar sua cobertura para retirar ou baixar mercadoria. | FIS-R019, FIS-R067, FIS-R068 |
| Pedido de 100 atendido por pallet de 500 | Fiscal cobre 100; retirada mantém 400 no pallet original com destino válido, origem e revisão atualizados. | FIS-R040, FIS-R042, FIS-R050, FIS-R063 |
| Pedido por peso só fecha se uma bobina for fracionada | Recusar o fracionamento/tolerância; selecionar composição integral válida ou corrigir pedido pelo fluxo aplicável. | FIS-R041, FIS-R043, FIS-R044 |
| Cliente desiste com pallet separado e nota ativa | Resolver cancelamento fiscal, registrar seu resultado e retornar todas as unidades a destinos válidos; serviços executados permanecem. | FIS-R080, FIS-R081, FIS-R082, FIS-R083, FIS-R090 |
| Avaria é registrada depois da preparação ou do envio | Bloquear retirada, manter reserva e intenção externa, acompanhar situação da nota e tratativa fiscal/operacional. | FIS-R060, FIS-R061, FIS-R079, FIS-R100 |
| Devolução física depois de saída confirmada | Criar nova entrada ligada à baixa, limitada ao saldo devolvível; manter FIFO original, chegada nova e saída histórica. | FIS-R086, FIS-R087, FIS-R088, FIS-R089 |
| Gestor resolve retirada durante encerramento do cadastro | Usar resolução identificada do compromisso existente; sem liberar novos pedidos comuns ou reativar INATIVO. | FIS-R095, FIS-R097, FIS-R098 |
| Cadastro foi alterado enquanto o operador conferia a emissão | Solicitar conferência da revisão aplicável ou preservar a solicitação já iniciada; documento anterior mantém seus dados originais. | FIS-R032, FIS-R035, FIS-R100 |
| Pedido possui documento legado sem identificador da API | Mostrar número e procedência, conciliar a identidade e apurar a necessidade documental restante antes de oferecer novo envio. | FIS-R073, FIS-R078, FIS-R101 |
| Documento é cancelado depois de retirada confirmada | Conservar saída e baixas, indicar a pendência fiscal e encaminhar regularização própria; não oferecer reemissão comum pelo botão da lista. | FIS-R075, FIS-R080, FIS-R089, FIS-R102 |

## Insumos fiscais externos realmente necessários

Não é necessário reenviar o questionário Q01 a Q27. As respostas de D49 também fecharam emitente, destinatário, agrupamento por cliente, valor proporcional, sequência da regularização e possibilidade de operação apenas fiscal. O complemento de 23/10/2026 definiu nova nota de entrada quando a circulação impedir o cancelamento, inclusive dentro de 24 horas. Os insumos abaixo completam configuração e evidência fiscal, sem transformar definições já respondidas em novas dúvidas do gestor.

| Insumo | Exemplo mínimo útil | Responsável de referência e uso |
| --- | --- | --- |
| Configuração do estabelecimento e da operação | XMLs completos dos exemplos Tigre/Castro e dos demais contextos aplicáveis. Operação simbólica precisa de exemplo se fizer parte da entrega. | Fiscal/Contabilidade, com suporte Notazz: completar finalidade/natureza, tributos, referências e campos preenchidos pelo emissor, respeitando partes já definidas. |
| Cadastro do critério de múltiplas notas | Aplicar o critério do cliente; Bracell permite consolidar. Obter um exemplo consolidado com duas origens para comparação. | Fiscal/Contabilidade e responsável pela operação: validar cobertura e representação das origens, sem reabrir a permissão de consolidação já respondida. |
| Quantidade e valor fiscal em saída parcial | Comparar a proporção aprovada pelo gestor com as quantidades, unidades e valores do XML de origem/saída. | Fiscal/Contabilidade: conferir precisão e conversões aplicáveis; operação confere a parcela efetivamente retirada. A fonte do valor fiscal está definida em FIS-R052. |
| Tributação e precedência entre sistemas | Para os exemplos anteriores, indicar quais dados de regime, origem, situação tributária, alíquotas e demais grupos aplicáveis ficam na Notazz e quais o WMS envia. | Fiscal/Contabilidade com suporte Notazz: preencher a matriz de fonte por campo sem copiar tributação da entrada cegamente. |
| Cancelamento e regularização fiscal | XMLs de cancelamento permitido e de nova nota de entrada, por prazo vencido ou circulação que impede cancelamento, distinguindo mercadoria presente de mercadoria retornada. | Fiscal/Contabilidade: completar parâmetros e vínculos do procedimento já respondido em D49. Preservar os fatos físicos e a situação real de cada nota; não reabrir a decisão entre cancelar e dar entrada nesse caso. |

Credenciais e dados protegidos da conta Notazz são insumos técnicos de implantação por canal protegido, não conteúdo deste inventário nem parâmetro a solicitar no chat. Nenhuma imagem de tabela de outro cliente fornece preço, CFOP ou alíquota real por autorização implícita.

## Evidências de teste existentes e limites desta conferência

Foram lidas as fontes dos testes abaixo para identificar cenários reaproveitáveis. Não foram executadas nesta elaboração; resultados datados de entregas anteriores continuam nos respectivos recibos e na trilha oficial.

| Fonte de teste | Cenários identificados por leitura | Limite |
| --- | --- | --- |
| [../backend/src/test/java/br/com/rodogarcia/wms/services/NfeXmlServiceTest.java](../backend/src/test/java/br/com/rodogarcia/wms/services/NfeXmlServiceTest.java):15-105 | DTD, entidades, acesso externo, profundidade, bytes UTF-8, campos duplicados, namespace/modelo e documento/envelope. | Parsing não confirma situação fiscal nem integração Notazz. |
| [../backend/src/test/java/br/com/rodogarcia/wms/NfeDocumentoServiceTest.java](../backend/src/test/java/br/com/rodogarcia/wms/NfeDocumentoServiceTest.java):27-164 | Quantidades comercial/tributável, linhas repetidas preservadas, volumes separados, protocolo de outra nota, grupos IBS/CBS, CPF e zeros. | Suporte de leitura não define tributação de saída nem autentica documento. |
| [../backend/src/test/java/br/com/rodogarcia/wms/NfeBobinasTest.java](../backend/src/test/java/br/com/rodogarcia/wms/NfeBobinasTest.java):193-268 | Conversão exata de massa, proibição de peso virar contagem, distribuição por quantidade/condição. | Não comprova pesagem física nem conversão geral de embalagens. |
| [../backend/src/test/java/br/com/rodogarcia/wms/NfeSkuTest.java](../backend/src/test/java/br/com/rodogarcia/wms/NfeSkuTest.java):47-172 | Correspondência contextual, preservação de zeros, ambiguidade, associação explícita e cliente divergente. | Regra contextual não autoriza normalizar qualquer SKU de qualquer cliente. |
| [../backend/src/test/java/br/com/rodogarcia/wms/ExpedicaoIntegrationTest.java](../backend/src/test/java/br/com/rodogarcia/wms/ExpedicaoIntegrationTest.java):234-1564 | Parcial de pallet, multinota/XML, cobertura inválida, cancelamento/retorno, devolução, avaria, permissões, encerramento e concorrência. | Somente leitura. Possui preparação de banco; não executar diretamente no DEV populado nem tratar provas históricas H2 como validação SQL atual. |

O pedido novo exige implementação e validação próprias da emissão integrada. A evidência atual sustenta o inventário e os cenários, não uma declaração de que NF-e já foi emitida pelo WMS ou de que os parâmetros fiscais reais foram homologados.
