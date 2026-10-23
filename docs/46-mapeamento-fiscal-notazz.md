# Mapeamento da integração fiscal Notazz

Atualizado em 22/10/2026. Escopo BE10-NOTAZZ-MAP01 e FE10-NOTAZZ-MAP01: documentar as regras e preparar a implementação solicitada para Fiscal e retirada. Código, configurações, migrations, bancos e documentos fiscais permanecem sem alteração nesta análise.

A mudança exige uma jornada de emissão rastreável, além da reorganização da tela. O WMS atual registra documentos já emitidos; ainda não transmite NF-e à Notazz. A base de reservas, cobertura documental, retirada integral e auditoria pode ser aproveitada, preservando a independência entre autorização fiscal, movimentação física e cobrança.

O inventário de regras está no [documento 47](47-regras-de-negocio-fiscal-notazz.md). Os cenários, verificações e limites estão no [documento 48](48-cenarios-e-validacao-fiscal-notazz.md). Estes três documentos formam o levantamento da futura implementação; propostas técnicas não constituem parâmetros tributários aprovados.

As [respostas posteriores do gestor e os exemplos conferidos](49-alinhamento-fiscal-respostas-e-exemplos.md) fecham emitente, destinatário, agrupamento, valor proporcional, procedimento de desistência, sequência fiscal antes do operador e possibilidade de operação somente fiscal. Esse alinhamento prevalece sobre as pendências iniciais correspondentes. Resta validar com Natalina a admissibilidade do cancelamento informado quando a mercadoria já circulou e retornou dentro de 24 horas.

## Pedido recebido

O responsável solicitou estudar a integração e, depois, explicitou que nenhum código deve ser aplicado agora. Complementou o escopo para documentar profundamente e esgotar fontes e verificações antes de apresentar dúvidas reais.

O requisito encaminhado por Caio prevê:

1. Agrupar Documento de mercadoria e Retirada física integral em **Retirada fiscal da mercadoria**.
2. Relacionar pedidos de saída com número, data, cliente, status da NF-e e última coluna **Nº NFe**.
3. Mostrar **PENDENTE** quando a NF-e não tiver sido emitida e **FINALIZADO** quando tiver sido emitida.
4. Na última coluna, apresentar **EMITIR NFe** para pendentes elegíveis e o número para finalizados.
5. Abrir os dados completos do pedido para conferência antes da confirmação.
6. Confirmar o envio dos dados à Notazz e recuperar a identificação da NF-e emitida.
7. Incluir o número da saída nas observações, quando suportado.
8. Aproveitar dados fiscais de cliente, armazém e produto, verificando lacunas cadastrais.

O agrupamento visual não autoriza transformar o clique de emissão em baixa física. “Todos os pedidos” deve respeitar os clientes e armazéns autorizados, a paginação e o contexto selecionado.

## Fontes e precedência

| Fonte | Uso neste levantamento |
| --- | --- |
| Pedido atual do responsável e texto encaminhado de Caio | Novo comportamento solicitado; análise e documentação autorizadas, implementação ainda não iniciada |
| [Respostas do gestor e DANFEs de exemplo](49-alinhamento-fiscal-respostas-e-exemplos.md) | Emitente é o CNPJ do armazém da entrada; destinatário é sempre o proprietário; agrupamento conforme cliente, com consolidação permitida para Bracell; valor proporcional da entrada; regularização por prazo e situação física |
| [Regras RN](03-regras-de-negocio.md), PDF original, páginas 3, 6, 7 e 9 | Cadastros fiscais, parametrização por operação, saída, integração, rastreabilidade e separação do fato físico |
| [Respostas recebidas](10-respostas-recebidas-2026-10-05.md) e Word original, Q01, Q02, Q07 a Q13, Q16 a Q22, Q24 | Pedido integral, parcial de pallet, origem, reserva, responsáveis, emissão após separação, várias notas e devolução |
| [Alinhamentos AC](11-alinhamentos-apos-respostas.md) | Soluções propostas para conciliar fiscal, físico, cobrança, unidades e encerramento; sua classificação permanece explícita |
| [Pedido e reserva](24-pedido-saida-fifo-e-reserva.md), [expedição](27-separacao-retirada-retornos-e-avaria.md), [cadastros fiscais](29-cadastros-servicos-e-calculo.md) | Contratos e invariantes implementados; evidências antigas conservam suas datas e seus limites |
| [Entrada manual e XML](45-pedido-entrada-manual-xml.md) e decisões posteriores de bobinas | Proprietário pela origem, medidas e conversões limitadas; suporte posterior não deve ser confundido com o recorte histórico |
| Fontes atuais Java, React e contratos gerados | Comportamento efetivamente disponível e lacunas estáticas |
| Documentação pública Notazz e anexos ligados abaixo | Contrato publicado; aceitação real do provedor exige ambiente próprio de homologação |

O procedimento manual de AC02 descreve a primeira fase anterior. O pedido atual prepara a integração da segunda fase, sem apagar notas existentes ou modificar retrospectivamente os registros anteriores.

## Estrutura existente

Os caminhos desta tabela partem da raiz do repositório.

| Responsabilidade | Fonte atual | Constatação |
| --- | --- | --- |
| Jornada fiscal | `frontend/src/modules/saida/definition.ts:54` | Três etapas; ajuda explicita que nenhuma ação emite nota externa |
| Composição de páginas | `frontend/src/domain/recordPages.ts:175` e `:375` | Mesma lista de pedidos e consulta de expedição; ações fiscais limitadas a SEPARADO |
| Tabela compartilhada | `frontend/src/components/records/RecordTable.tsx:46` | Referência, situação e revisão; alteração global afetaria também Saída, FIFO e reserva |
| Consulta paginada | `backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java:44` | Exige cliente e armazém; não entrega resumo de NF-e |
| Pedido | `backend/src/main/java/br/com/rodogarcia/wms/dto/PedidoSaidaDto.java:21` e `:93` | Número/referência, criadoEm, cliente/armazém e itens; sem destinatário distinto ou preço fiscal |
| Operações de expedição | `backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java:18` | Leitura, separação, documento existente, cancelamento local, retirada, retorno e devolução |
| Registro documental | `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java:240` | Origem NOTAZZ é declaração de nota previamente emitida, sem chamada ao provedor |
| Modelo documental | `backend/src/main/java/br/com/rodogarcia/wms/models/DocumentoSaida.java:83` | Exige identidade fiscal e nasce AUTORIZADO; não representa solicitação pendente |
| Cobertura | `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java:590` | Apenas documento autorizado de retorno de mercadoria participa da liberação física |
| Retirada | `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java:343` | Revalida pedido, unidades, impedimentos, cobertura, XMLs e remanescentes; grava baixa separadamente |
| Repetição local | `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java:837` | UUID, hash e resultado original; não garantem sozinhos a unicidade de uma emissão externa |
| Persistência | `database/migrations/V6__separacao_retirada_retornos_e_avaria.sql:78` | Documento exige número/série/protocolo e AUTORIZADO/CANCELADO; migrations existentes são históricas e imutáveis |

Não foi localizado cliente de integração Notazz, envio fiscal, webhook, vínculo de empresa/ambiente, fila de emissão ou estado de tentativa no código produtivo examinado.

## Significado dos estados

| Informação | Estados ou fatos | Consequência |
| --- | --- | --- |
| Pedido operacional | RASCUNHO, RESERVADO, EM_SEPARACAO, SEPARADO, RETIRADO, CANCELADO | Determina a jornada logística; não deve receber FINALIZADO fiscal como substituto |
| Reserva | Quantidade vinculada e situação própria | Falha fiscal conserva reserva; não há vencimento automático |
| Solicitação fiscal proposta | Preparada, aguardando envio, em processamento, resultado desconhecido, rejeitada, autorizada, cancelamento em processamento, cancelada, conflito | Explica o que o sistema pode fazer sem duplicar emissão |
| Resumo fiscal solicitado | PENDENTE ou FINALIZADO | Deve ser calculado no backend a partir do plano documental e dos resultados conciliados |
| Cobertura física | Itens/quantidades por reserva e nota de origem | Continua obrigatória para retirar; retorno simbólico não comprova cobertura física |
| Retirada e cobrança | Data real e fatos físicos/financeiros | Autorização, consulta ou cancelamento fiscal não substituem estes fatos |

**Proposta para a lista:** manter os dois rótulos solicitados e apresentar, junto ao pendente, o motivo e a ação possível. Pedido ainda não separado, emissão em andamento, dados incompletos, rejeição e resultado desconhecido são pendências diferentes. EMITIR NFe só pode transmitir quando os requisitos estiverem satisfeitos; enquanto houver resultado desconhecido, a ação deve consultar/acompanhar a solicitação existente.

FINALIZADO deve significar que todas as notas previstas para aquela operação estão autorizadas e conciliadas, sem tratá-lo como confirmação de retirada. Uma nota entre várias não conclui o conjunto. O plano documental precisa explicar retorno físico/simbólico e agrupamento por origem; documentos simbólicos não podem ser somados à cobertura física atual para liberar estoque.

Documento NOTAZZ/XML já registrado sem vínculo de API não significa ausência de emissão. A prévia deve incorporá-lo ao plano e conciliá-lo; a falta de DOCUMENT_ID/EXTERNAL_ID, isoladamente, nunca habilita nova emissão. Preservar seus números e histórico. Eventual complemento só pode abranger saldo documental demonstrado, sem repetir a mercadoria já coberta.

Se uma nota for cancelada depois da retirada, o pedido continua RETIRADO e ganha pendência fiscal identificável. Não mostrar reemissão livre como se o pedido tivesse voltado a SEPARADO. O registro atual de cancelamento aceita esse estado, enquanto o registro documental atual exige SEPARADO: a substituição posterior precisa de tratamento próprio.

## Dados disponíveis e lacunas

Cadastro existente não significa dado completo nem regra tributária validada.

| Grupo | Origem disponível | Lacuna ou decisão para o contrato futuro |
| --- | --- | --- |
| Número e data do pedido | `PedidoSaida.referencia`, `criadoEm`; referência automática SAI | Usar referência comercial e data do pedido, preservando ID técnico na integração |
| Proprietário | Cliente do pedido e do produto | Gestor confirmou que sempre será o destinatário fiscal; conferir a identidade preservada da origem, sem substituir por terceiro |
| Estabelecimento emissor | Armazém possui documento e complemento fiscal | Gestor definiu o CNPJ do armazém da entrada; vincular esse estabelecimento à empresa Notazz, credencial e ambiente, preservando a origem |
| Identificação e endereço | `FiscalCadastroDto` e `ComplementoFiscal` | Existem razão social, IE, logradouro, número, bairro, cidade, UF, CEP, país e contato, geralmente opcionais |
| Tipo fiscal e município | Documento é string; cidade textual; país alfa 2 | Faltam tipo CPF/CNPJ, indicador IE e código IBGE estruturados; validar onde cada conversão será resolvida |
| Regime e inscrições do emissor | Sem estrutura específica | Definir fonte efetiva na configuração Notazz e dados que precisam ser conhecidos pelo WMS |
| SKU e descrição | Produto por proprietário | Preservar SKU do cliente e associação explícita; normalização de zeros existente é contextual, não geral |
| Unidade e precisão | Produto, tipo quantidade e casas decimais | Conversão comercial/tributável e contagem física precisam de contrato próprio; DUN não identifica unidade física |
| NCM, CFOP, CEST | Referência por produto, armazém e operação | Campos opcionais e validação de formato; selecionar regra vigente aplicável, sem CFOP fixo universal |
| Tributos | Enquadramento textual, alíquotas ICMS e IPI | Faltam origem, CST/CSOSN, PIS/COFINS e estrutura tributária detalhada; separar parametrização do cálculo e da obrigação real |
| Outros grupos tributários | Sem cadastro correspondente | Mapear o aplicável por operação, inclusive estruturas novas publicadas, sem preencher zeros como se fossem decisão fiscal |
| Quantidade | Item do pedido e reservas/origens | Pedido integral; conferir quantidade comercial e fiscal com seleção efetiva, sem tolerância de peso inventada |
| Valor da mercadoria | Valor total opcional do item de entrada | Gestor aprovou o valor da entrada proporcional à quantidade retirada. Falta persistência/projeção na saída; preservar preço e saldo de cada origem, sem média indiscriminada |
| Descontos e despesas | Sem campos próprios na saída | Definir se existem na operação e sua fonte; não somar serviços de armazenagem à NF-e da mercadoria |
| Notas referenciadas | Notas de origem das reservas, chave e XML quando disponíveis | Usar origens efetivas e chaves válidas; nota manual pode não ter chave disponível |
| Natureza e finalidade | Natureza local físico/simbólico | Aplicar retorno correspondente à entrada e ao critério fiscal do cliente, conforme resposta. Natureza local não equivale automaticamente a CFOP/finalidade; delimitar operação apenas fiscal |
| Retirada e entrega | Armazém/endereço operacional | Destinatário fiscal é o proprietário. Dados adicionais de retirada/entrega, quando exigidos fiscalmente, não criam gestão de transporte no WMS |
| Frete e volumes | Dimensões/peso operacional e prévia XML | Sem composição fiscal pronta de saída; peso único não distingue líquido/bruto e parcial de pallet não equivale a volume integral |
| Observações | Referência SAI e histórico | Acrescentar saída WMS sem sobrescrever texto fiscal configurado; evitar dados técnicos sensíveis |
| Identificação externa | Inexistente | Solicitação, documento externo, empresa, ambiente, vínculo ao pedido, eventos e tentativas precisam de persistência |
| Evidência de autorização | Documento local e XML informado | Conciliação deve conferir situação externa, emitente, chave, ambiente, número, série, protocolo e conteúdo |

Fontes principais: `FiscalCadastroDto.java:23,101`, `ComplementoFiscal.java:8`, `ReferenciaFiscalProduto.java:21,49`, `ProdutoDto.java:18`, `ItemNotaEntrada.java:40`, `NfeImportacaoDto.java:36`, `MedidasUnidade.java:8` e `FiscalCadastroService.java:82,115,165`.

O formulário de integração deve consumir uma matriz por campo: **fonte WMS ou configuração Notazz → regra aplicável → validação → responsável → momento de congelamento**. Não duplicar a mesma configuração em dois sistemas sem definir precedência. As informações devem ser conferidas na prévia e congeladas na solicitação, para que mudanças cadastrais posteriores não alterem silenciosamente o documento já enviado.

O [inventário estruturado de campos](levantamentos/fiscal-notazz-20261022/inventario-campos-notazz.json) relaciona os 101 nomes da tabela pública de criação, a indicação de obrigatoriedade dessa tabela e a origem proposta no WMS. Inclui também o índice de 848 caminhos do modelo completo de documento e 559 do modelo de produto. Esses índices abrangem grupos genéricos além do piloto; não tornam todos os campos necessários nem configuram um esquema de validação fiscal. Campo opcional na API pode ser necessário na operação concreta. O arquivo conserva as fontes e os hashes consultados.

## Contrato público da Notazz

O guia publica POST `https://app.notazz.com/api`, chave `API_KEY`, método `create_nfe_55` e consulta `consult_nfe_55`. A criação retorna ID; transmissão exige `TRANSMITIR=1`, cujo padrão é zero. A consulta recebe `DOCUMENT_ID` e `EXTERNAL_ID`, deixando um vazio quando necessário. A resposta inclui situação, número, chave e arquivos. `EXTERNAL_ID` identifica o vínculo; `SALE_ID` alcança xPed. Há limite de 100 requisições por minuto e recomendação de serialização. Erros incluem duplicidade 101, limite 110/997, processamento anterior 120, acesso 121/303/305, repetição 996 e manutenção 999. POSTBACK inclui token por empresa, identificação, situação e motivo. Estes campos não provam entrega ordenada ou única. [Guia da API](https://app.notazz.com/docs/api/#nfe-criar).

O Swagger publica rotas por operação, como `/create_nfe_55` e `/update_nfe_55`, com JSON e API_KEY no cabeçalho. Seus exemplos são menores e omitem campos presentes no guia, incluindo transmissão e grupos recentes. A forma de transporte escolhida deverá ser conferida em homologação. [OpenAPI oficial](https://app.notazz.com/docs/api/notazz-swagger.yaml).

O modelo completo permite `ide.NFref`, `ide.tpAmb`, emitente, destinatário, `dest.indIEDest`, retirada, entrega e `infAdic.infCpl`. Isso oferece um caminho para referências múltiplas e a observação da saída; a configuração é enviada conforme o formato descrito no guia, em Base64. A existência de `tpAmb` no leiaute não comprova conta, certificado ou sandbox utilizáveis. [Modelo do documento](https://app.notazz.com/docs/api/DOCUMENT_CUSTOM_CONFIGURATION.json).

O modelo de produto tem raiz `det`, com `prod.uCom/qCom/uTrib/qTrib/vUnCom/vUnTrib` e grupos tributários. Não deve ser tratado como um objeto com raiz `prod`, nem copiado integralmente com valores vazios. [Modelo do produto](https://app.notazz.com/docs/api/DOCUMENT_PRODUCT_CUSTOM_CONFIGURATION.json). O leiaute distingue ambiente, participantes, referências, medidas, tributos e informações adicionais. O preenchimento concreto depende da operação fiscal. [Leiaute NF-e](https://app.notazz.com/docs/nfe/documentacao/).

### Divergências que o desenvolvimento deve considerar

| Achado público | Tratamento proposto |
| --- | --- |
| Exemplo de atualizar NF-e contém método de criar; Swagger distingue atualização | Não copiar o exemplo literalmente; confirmar operação contratada com teste isolado em homologação |
| Exemplo de exclusão de webhook usa método de NFS-e | Não transportar esse método para o conector |
| Campo de descrição aparece opcional na tabela de criação e como obrigatório no erro genérico | Preparar descrição significativa; verificar contrato real no cenário aplicável |
| Respostas exemplificadas têm trechos com vírgulas ausentes | Usar parser robusto de JSON real, sem transcrever exemplos como código executável |
| Formato de POSTBACK ilustrado como array PHP | Confirmar Content-Type, codificação, autenticação e repetição reais; não presumir JSON assinado |
| Coleção não especifica garantia de entrega, ordenação ou prazo do callback | Usar confirmação por consulta e conciliação idempotente |
| Link público do SDK retornou HTTP 200 sem conteúdo ZIP na inspeção | Não houve SDK utilizável confirmado; transporte será implementado conforme contrato verificado, sem depender desse download |

Esses achados são limitações do material publicado, não falhas reproduzidas de emissão. Nenhuma chamada autenticada ou emissão foi feita.

### Recursos da API fora da emissão solicitada

A página também contém NFS-e, estoque do provedor, logística, rastreio, PLP, transportadoras, mensagens e captura de transações. A presença desses recursos não amplia o escopo do WMS. Emissão de serviço continua no ESL; estoque físico continua nos serviços do WMS; transporte continua no TMS. Não chamar recursos de mensagem, atualizar estoque Notazz ou contratar logística como efeito implícito de emitir NF-e.

## Fluxo proposto para implementação

1. **Consultar:** backend entrega a página fiscal com referência, data, cliente, estado operacional, resumo fiscal, números e impedimentos. Filtro fiscal ocorre no servidor sobre o conjunto autorizado, não apenas sobre a página já carregada.
2. **Preparar:** backend revalida pedido separado, reservas, produtos, origens, cadastros, permissão e configuração fiscal. Monta o plano de documentos e identifica dados ausentes por cadastro/campo.
3. **Conferir:** apresentar destinatário, estabelecimento, operação, origens, itens, medidas, valores, tributos aplicáveis, observação e documentos planejados. A prévia deve possuir identidade e revisão/hash verificáveis.
4. **Confirmar:** conferir novamente permissão, estado e revisões. Persistir solicitação e snapshot imutável antes de qualquer envio. Duas confirmações concorrentes para a mesma finalidade não criam duas solicitações ativas.
5. **Transmitir:** executar fora dos locks/transação de estoque; serializar por empresa e respeitar o limite compartilhado. Solicitação que ainda não foi enviada pode ser suspensa se perder elegibilidade.
6. **Acompanhar:** guardar o ID retornado e conciliar por consulta, complementada por callback. Atraso, falha de rede ou janela fechada não apagam a solicitação.
7. **Conciliar:** guardar o resultado fiscal mesmo que o pedido tenha sofrido avaria, reparo, mudança de revisão ou encerramento após o envio. Conferir identidade e autorização antes de criar/vincular o documento fiscal; preservar o impedimento físico atual.
8. **Exibir:** atualizar situação e números, com XML/PDF acessíveis no alcance. Resultado parcial entre várias notas permanece explicado. Consulta nunca inicia outra emissão.
9. **Retirar:** continuar com confirmação física própria, XMLs, cobertura integral, leitura vigente, remanescentes e auditoria existentes.

O serviço de envio deve possuir recuperação após reinício. Uma resposta perdida entre aceitação externa e gravação local exige consulta pela identidade persistida; não gerar novo identificador como primeira reação. Mesmo UUID com conteúdo diferente deve ser conflito. O vínculo externo precisa distinguir documentos legítimos diferentes do mesmo pedido, sem habilitar duplicatas da mesma intenção.

### Limites transacionais

Não inserir a chamada remota diretamente em `ExpedicaoService.registrarDocumento` ou `retirarDatado`. Esses serviços adquirem locks que protegem operações do armazém. A Notazz pode concluir uma emissão e a transação local falhar; rollback SQL não desfaz o documento externo.

Adotar transações curtas para preparar/persistir e consolidar resultado, coordenadas por serviço convencional Spring. Persistência de intenção/estado/evento é necessária; um framework genérico de integração não é requisito. Tentativas automáticas só devem repetir operações seguras e rastreáveis, com espera e teto configurados, nunca uma nova criação às cegas.

### Segurança e evidência fiscal

Credencial de empresa e token de callback ficam externos ao repositório, ao navegador e aos logs. A permissão deve ser validada no serviço para consulta, prévia, confirmação, repetição e download. Supervisor/Gestor é a proposta natural para emissão, acompanhando o registro fiscal atual; administração de usuários não concede esse poder por si só.

O cadastro deve ligar cada contexto ao estabelecimento fiscal correto. Conferir o emitente do XML com esse vínculo explicitamente configurado; não pressupor que o documento operacional do armazém é sempre a identidade da empresa que autoriza a nota.

Antes do envio, exigir associação entre armazém, estabelecimento emissor, empresa Notazz, credencial e ambiente. O documento do emitente deve corresponder à empresa configurada; não inferir essa associação pelo proprietário ou destinatário. Retorno divergente fica em conflito e não gera cobertura física válida.

Callback autenticado é sinal para conciliar. Verificar empresa, ambiente e vínculo da solicitação; retorno repetido ou fora de ordem não pode regressar documento autorizado ou cancelar por simples texto recebido. Consulta de reconciliação resolve conflito. Arquivos externos exigem origem permitida, limites de tamanho/tempo e parser seguro; não aceitar URLs arbitrárias do cliente para o backend baixar.

O parser atual da expedição, `NfeXmlService`, extrai identidade e itens. Ele não verifica protocolo, ambiente ou autorização. `NfeDocumentoService` projeta mais dados e possui validação estrutural, mas também não autentica SEFAZ/assinatura; além disso, não é o parser chamado pelo registro atual de saída. O futuro serviço deve fazer a conciliação fiscal explícita, sem promover automaticamente o estado local declarado AUTORIZADO a prova de emissão externa.

## Mudanças futuras por camada

| Parte | Trabalho preparado pelo levantamento |
| --- | --- |
| Modelos e persistência | Solicitação, snapshot/plano documental, empresa/ambiente, identidade externa, tentativas e eventos; migration nova após reconferir sequência vigente |
| Serviços | Preparação, validação fiscal, confirmação idempotente, transporte, consulta/conciliação e atualização do resumo; preservar retirada como operação distinta |
| Repositórios | Resumo fiscal paginado, controle de solicitação ativa e recuperação por identidade; evitar consulta de expedição por linha |
| APIs | Listagem fiscal, prévia, confirmação, andamento e receptor de callback; nomes definitivos serão definidos no contrato de implementação |
| Cadastros | Completar apenas campos cuja fonte deve ser o WMS; configuração por operação/estabelecimento e validação de completude na prévia |
| Frontend | Composição específica fiscal, duas primeiras etapas unificadas, vários números, dados para conferência e acompanhamento persistente |
| Contratos | Gerar a partir do backend com ferramenta existente; não corrigir apenas os arquivos gerados TypeScript/JSON |
| Ajuda | Atualizar `frontend/src/content/information.ts` e consumidores para explicar estado fiscal, retirada e tratamento da pendência |
| Cancelamento | Distinguir registro local do cancelamento externo confirmado; receber mudanças externas sem estorno físico automático |

## Insumos que não podem ser inventados

A investigação e as respostas posteriores resolvem a jornada principal, os invariantes de estoque, o emitente, o destinatário, o critério de agrupamento, a fonte do valor fiscal e a sequência operacional da regularização. Após o prazo, haverá novo processo de entrada e reendereçamento, mesmo sem retirada; retorno simbólico pode ser somente fiscal. Permanecem insumos de implantação: conta de homologação, credenciais protegidas, XMLs completos e parâmetros tributários aplicáveis por operação/estabelecimento. Natalina/Controladoria é a referência fiscal, inclusive para validar o cancelamento após circulação informado pelo gestor.

Os DANFEs 316 e 319 fornecem exemplos de Tigre/Castro com uma origem. Emitente, destinatário, valor proporcional e consolidação Bracell já foram respondidos. Obter os XMLs e a configuração fiscal aplicável para conferir campos completos; exemplos de várias origens, regularização e operação apenas fiscal completam os casos ainda não demonstrados. O [documento 49](49-alinhamento-fiscal-respostas-e-exemplos.md) separa esses insumos técnicos/fiscais das definições operacionais já respondidas e da validação específica de cancelamento após circulação.

## Verificações desta entrega

A documentação pública principal, Swagger, dois modelos JSON e leiaute foram obtidos por leitura HTTP. Os modelos JSON foram parseados e seus caminhos relevantes confrontados. A inspeção não executou código do site nem enviou dados ao endpoint fiscal. O Graphify foi utilizado como navegação; os fatos foram conferidos nas fontes atuais.

O [resumo de verificação](levantamentos/fiscal-notazz-20261022/validacao.json), a [auditoria das fontes públicas](levantamentos/fiscal-notazz-20261022/auditoria-notazz-publico.json) e o [resultado frontend](levantamentos/fiscal-notazz-20261022/frontend-vitest.json) preservam a evidência desta entrega. Foram confirmadas 17 verificações estruturais dos artefatos publicados; a inspeção do link do SDK não confirmou ZIP utilizável. Os dois resultados têm natureza documental e não são testes de emissão.

Foram executados testes puros de frontend com rede bloqueada. A seleção de testes puros de backend foi preparada dentro da whitelist vigente, mas sua execução foi recusada pela proteção de memória antes de iniciar o programa. Resultados, evidências e os limites completos estão no [documento 48](48-cenarios-e-validacao-fiscal-notazz.md).

Este levantamento não declara integração real, homologação tributária ou aceite SQL. A futura implementação deve validar a matriz do documento 48 no nível correspondente, preservando fontes, dados e histórico existentes.
