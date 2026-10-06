# Recebimento, conferência e entradas conferidas

Entrega D15 de 05/10/2026: novo macrobloco de backend, com contrato operacional de BE01 e implementação de BE06. Mantém MVC por camadas, properties, DTOs, validação nos serviços e verificações de engenharia de BE16. APIs disponíveis para FE05; FE06/BE07 consumirão a origem física, os lotes e as quantidades conferidas.

## Fontes e escolhas

As [respostas](10-respostas-recebidas-2026-10-05.md), especialmente Q03/Q04/Q05/Q07/Q19/Q20/Q21, definem nota única por pedido, múltiplas chegadas, FIFO pela primeira chegada, quarentena da carga, supervisor responsável pela liberação e separação entre XML e presença física. [AC03](11-alinhamentos-apos-respostas.md#ac03-recebimento-em-partes-e-alcance-da-quarentena) propõe delimitar a carga pelo pedido e aceitar a quantidade real após tratativa com o cliente. Essa proposta permanece identificada; D15 autoriza implementação, sem transformá-la em resposta literal do gestor.

Escolhas técnicas deste bloco: previsão congelada ao iniciar conferência; revisão otimista combinada com lock do pedido; chave de repetição nas chegadas; estorno antes da efetivação; seis tabelas adicionais; XML limitado à extração operacional da NF-e 4.00/modelo 55. A efetivação gera registros de entrada conferida que conservam origem e quantidade. Unitização, etiqueta, localização física, disponibilidade para saída e cobrança serão implementadas em suas próprias etapas.

## Modelo e efeitos

| Registro | Conteúdo e proteção |
| --- | --- |
| `PedidoEntrada` | Cliente, armazém, referência única nesse contexto, situação, versão, momentos e motivo de conclusão |
| `NotaEntrada` | Um pedido; emitente derivado do cliente, série/número, emissão, chave opcional, XML original e SHA-256 quando fornecido |
| `ItemNotaEntrada` | Número do item da nota, produto do cliente, quantidade prevista e valor de mercadoria opcional; mantém a previsão original |
| `ChegadaRecebimento` | Um fato físico, data real, autor, observação, UUID da operação e hash dos dados; estorno preserva o fato, seu autor e motivo |
| `ItemChegada` | Item da nota, lote/validade quando aplicáveis, quantidade boa e avariada; ambas representam mercadoria física |
| `EntradaConferida` | Uma por item de chegada ativo, com FIFO da nota, momento de efetivação, quantidade destinada à triagem e quantidade avariada em quarentena |

Datas de emissão, chegada real, FIFO e efetivação são distintas. Não há data de início de cobrança neste bloco. A primeira chegada é o menor instante das chegadas não estornadas de cada nota, incluindo todos os seus itens. Ela é copiada para as entradas na efetivação. Estornar uma chegada antes de efetivar recalcula a primeira chegada válida; preserva-se o histórico original.

Quantidades usam unidade base e precisão do produto. Não há conversão automática entre embalagem/DUN, peso e unidade comercial do XML. Lote é obrigatório para produto com controle de lote; validade é obrigatória quando o produto a controla. Validade vencida permanece informada, sem inventar bloqueio adicional à regra recebida; alertas ainda são futuros.

O valor opcional por item é o valor de mercadoria informado na nota (`vProd` no XML), sem apuração de impostos, frete, seguro, preço de serviço ou fechamento. Valor desconhecido permanece nulo. XML posterior pode complementar esse valor desconhecido com auditoria; não substitui valor conhecido, quantidade, produto ou identidade da nota.

## Situações e comandos

| Origem | Comando | Resultado |
| --- | --- | --- |
| Criação | Criar pedido | `RASCUNHO`, sem mercadoria física |
| `RASCUNHO` | Incluir nota manual ou XML | Previsão registrada; sem chegada nem entrada efetivada |
| `RASCUNHO` com nota | Iniciar conferência | `EM_CONFERENCIA`; não aceita novas notas/itens previstos |
| `EM_CONFERENCIA` ou `QUARENTENA` | Registrar chegada | Soma somente os fatos ativos; falta, sobra ou avaria mantém o pedido inteiro em `QUARENTENA` |
| Conferência aberta | Estornar chegada, por supervisor/gestor | Registra motivo/autor/momento, retira o fato dos totais ativos e mantém `QUARENTENA` |
| Conferência aberta | Efetivar, por supervisor/gestor | Exige quantidade física, motivo e aceite explícito se houver divergência; gera entradas uma vez e passa a `EFETIVADO` |
| Rascunho/conferência sem qualquer chegada histórica | Cancelar, por supervisor/gestor | `CANCELADO`, preservando notas e auditoria |

Quarentena não desaparece automaticamente quando a última chegada completa a previsão. O supervisor ainda confirma a conferência. Com divergência, `aceitarDivergencias=true` e motivo documentam a solução com o cliente. Não há aprovação obrigatória por segunda pessoa.

Exemplos de AC03 implementados:

- Previsto 100, recebido 98: o pedido fica em quarentena; após aceite, registra 98, preservando a falta de 2.
- Previsto 100, recebido 102: após tratativa, registra os 102 existentes, mantendo a sobra identificável.
- Recebidas 95 boas e 5 avariadas: toda a carga aguarda liberação; ao efetivar, 95 ficam destinadas à triagem e 5 continuam avariadas/quarentenadas. A efetivação não recupera mercadoria avariada.
- Uma nota pode ter quantidade zero recebida ao resolver um pedido com outras notas recebidas, desde que haja aceite da falta. Não se cria mercadoria para essa nota. Um pedido sem qualquer quantidade física não é efetivado.

`quantidadeTriagem` indica destino operacional pendente; não comprova endereçamento físico. Todas as entradas retornam `disponivelParaSaida=false`. Nenhuma reserva ou saldo disponível é produzido por este bloco.

Depois de efetivado, novas chegadas e estornos são recusados. Correções posteriores e devoluções precisam do fluxo operacional de BE11. Um pedido com histórico físico também não é cancelado por esta API, mesmo se suas chegadas foram estornadas. A nota continua vinculada ao pedido cancelado; eventual reaproveitamento exige evolução rastreável, sem liberar a identidade silenciosamente.

## API para FE05

Prefixo `/api/v1/pedidos-entrada`. JSON e Bearer JWT conforme BE04. Todas as operações verificam simultaneamente alcance por cliente e armazém; Gestor possui alcance geral. Operação/Supervisor/Gestor podem criar, consultar, registrar notas/XML e conferir. Efetivação, estorno e cancelamento exigem Supervisor/Gestor.

| Método e caminho | Contrato/efeito |
| --- | --- |
| `POST /` | `clienteId`, `armazemId`, `referencia`; retorna 201, resumo e Location |
| `GET /?clienteId=&armazemId=&pagina=&tamanho=` | Lista paginada do contexto autorizado |
| `GET /{id}` | Resumo/versão, notas/itens, previsto, recebido bom/avariado, diferença e primeira chegada |
| `POST /{id}/notas` | `versao`, `serie`, `numero`, `emissao`, `chaveAcesso` opcional e `itens` |
| `POST /{id}/notas/xml` | `versao` e `xml`; cria previsão em rascunho ou vincula a nota manual compatível |
| `POST /{id}/iniciar-conferencia` | `versao`, `motivo` |
| `POST /{id}/chegadas` | `versao`, `operacaoId` UUID, `chegouEm`, `observacao` e `itens` físicos |
| `GET /{id}/chegadas` | Histórico paginado, incluindo estornos e os itens de cada chegada |
| `POST /{id}/chegadas/{chegadaId}/estorno` | `versao`, `motivo`; mantém o lançamento original |
| `POST /{id}/efetivacao` | `versao`, `aceitarDivergencias`, `motivo` |
| `POST /{id}/cancelamento` | `versao`, `motivo` |
| `GET /{id}/entradas` | Registros conferidos paginados, origem, lote, datas e quantidades destinadas à triagem/quarentena |

As ações após criação retornam 200 e o resumo atualizado do pedido, inclusive sua nova versão. Para obter os IDs dos itens, consultar o detalhe após inserir notas. Não enviar entidades, situação ou saldo decidido pelo frontend. Campos JSON desconhecidos são recusados.

Item da nota: `numeroItem`, `produtoId`, `quantidadePrevista`, `valorMercadoria` opcional. Item físico: `itemNotaId`, `lote`/`validade` opcionais conforme produto, `quantidadeBoa` e `quantidadeAvariada`. A soma física precisa ser positiva; os componentes não podem ser negativos e respeitam individualmente a precisão do produto. Lotes diferentes podem constar em registros distintos do mesmo item. Uma combinação item/lote/validade não se repete dentro da mesma chegada.

Limites técnicos iniciais: referência de até 40 caracteres; motivo/observação de 5 a 500; lote até 60; 20 notas e 1000 itens previstos por pedido; até 200 itens por nota ou chegada; 100 lançamentos de chegada por pedido, incluindo estornados. Quantidades usam até 13 dígitos inteiros e 6 decimais, sujeitas à precisão do produto e ao limite acumulado por item. Paginação segue 0/20 por padrão, tamanho 1 a 100. Limites restringem carga da API e não são regras comerciais.

Datas de chegada usam ISO-8601 com fuso/UTC, não podem estar no futuro nem antes de 1900. Persistência dos instantes usa UTC com microssegundos. Emissão/validade são datas sem horário. Valores monetários opcionais usam até 17 dígitos inteiros e 2 decimais.

## Repetição, concorrência e auditoria

Cada comando trava o pedido e verifica a versão informada. A criação usa referência única no contexto; notas usam identidade única por emitente/série/número e chave de acesso quando presente. Uma nota não pode pertencer a dois pedidos.

O frontend gera `operacaoId` antes de enviar uma chegada. Ao repetir, conserva esse UUID e todos os campos do comando, inclusive a versão original. Mesmo UUID e mesmos dados retornam o resumo atual sem nova chegada/auditoria, inclusive depois de efetivar ou estornar o fato. Reutilizar UUID com conteúdo diferente retorna 409. Uma correção exige estorno e novo UUID. Decimais ou textos serializados de outra forma podem resultar em conflito; preservar o comando enviado na repetição.

Repetir o mesmo XML na mesma nota retorna o resumo, sem duplicação. XML diferente não substitui um já armazenado. Demais comandos repetidos recebem conflito de versão/situação. Efetivações simultâneas produzem uma única gravação válida; também existe unicidade do item de chegada em `entrada_conferida`.

Ordem de locks: pedido, cliente, armazém e produtos ordenados por ID. A verificação de cadastros ativos ocorre sob lock, usando IDs projetados antes de carregar os produtos. Cadastro em encerramento impede novos vínculos e efetivação. XML posterior também exige vínculos ativos nesta versão.

Todas as escritas e sua auditoria participam da mesma transação. Falha na auditoria desfaz quantidade, estado e versão. A tabela de auditoria existente foi ampliada por V2 para o tipo `PEDIDO_ENTRADA`; o nome físico `auditoria_cadastro` foi preservado por compatibilidade. Registros guardam autor, motivo, correlação, resumo anterior/posterior e dados do comando pertinentes; não copiam XML completo para a auditoria. Consulta de auditoria continua restrita ao Gestor em `/api/v1/auditoria?tipo=PEDIDO_ENTRADA&registroId=...`.

## XML: extração e limites

O extrator aceita uma NF-e 4.00/modelo 55, diretamente em `NFe` ou no envelope `nfeProc`, no namespace oficial. Lê identificação, emissão, CNPJ emitente e itens (`cProd`, `uCom`, `qCom`, `vProd`). A chave deve ter formato de 44 dígitos. Emitente deve corresponder ao documento do cliente; SKU/unidade devem corresponder a produtos já cadastrados. Não cria cadastros automaticamente.

Nota manual pode receber XML posteriormente, inclusive após efetivação, desde que série/número, emitente, emissão, chave quando conhecida, itens, produtos, unidades, quantidades e valores já conhecidos sejam compatíveis. A importação não muda chegadas, FIFO, quantidades efetivadas ou situação operacional. Novas notas por XML somente em rascunho.

XML é limitado a 1.000.000 bytes UTF-8 e profundidade 64. DTD, entidades externas, XInclude e acesso externo a schemas estão desabilitados. Erros não retornam XML, valores fiscais, caminhos de arquivos ou detalhes do parser. O original recebido como texto e seu SHA-256 ficam preservados no banco; respostas e logs não reproduzem esse conteúdo.

Esta extração não executa o XSD fiscal completo, validação de assinatura, dígito verificador da chave, consulta/autorização/cancelamento SEFAZ, vínculo com protocolo ou integração NOTAZZ. Ela não atesta validade fiscal. Outros modelos, formatos, conversões de unidade e cadastros de emitente fora do formato suportado precisam de tratamento específico. O armazenamento operacional não autoriza emissão fiscal ou saída.

Referências técnicas: [esquemas do Portal NF-e](https://www.nfe.fazenda.gov.br/POrtal/listaConteudo.aspx?AspxAutoDetectCookieSupport=1&tipoConteudo=BMPFMBoln3w%3D) e [segurança XML no JDK 21](https://docs.oracle.com/en/java/javase/21/security/security-developer-guide.pdf).

## Banco e próximos passos

[V2](../database/migrations/V2__recebimento_e_entradas_conferidas.sql) cria as seis tabelas, vínculos, índices, restrições de quantidade, unicidade da chave opcional e amplia os tipos/ações da auditoria. V1 é preservada. A execução segue [migrations](../database/migrations/README.md), separada da aplicação; nenhuma migration foi aplicada nesta entrega.

Evidências originais em [validação do recebimento](19-validacao-recebimento-backend.md). D16 implementou [BE07](20-unidades-logisticas-e-etiquetas.md), unitização e identidade permanente, usando a quantidade física conferida sem duplicá-la; o GET de entradas passou a expor `unitizadaEm`. Próxima frente: BE08, posições, movimentação e disponibilidade. SQL Server real, documentos reais de homologação, provedor de identidade e FE05 continuam com validações próprias.
