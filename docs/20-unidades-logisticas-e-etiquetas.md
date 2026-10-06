# Unidades logísticas, origem e dados das etiquetas

Entrega D16 de 05/10/2026: implementação de BE07 e ampliação dos contratos BE01 para FE06. Mantém as camadas MVC, validação nos serviços, properties e transações. Consome as entradas conferidas de [BE06](18-recebimento-e-conferencia.md); não cria novo recebimento nem disponibiliza estoque para saída.

## Fontes e escolhas deste recorte

RN10/RN11 exigem identidade permanente e endereço fora da etiqueta; RN15/RN16 exigem um SKU por unidade e fechamento das quantidades. As [respostas Q01/Q02/Q21](10-respostas-recebidas-2026-10-05.md) distinguem DUN da identidade física e acrescentam nota/quantidade à etiqueta. [AC10](11-alinhamentos-apos-respostas.md#ac10-dun-retiradas-parciais-e-etiquetas) propõe preservar origem, FIFO e ID remanescente, com mesma nota no reagrupamento. Essa proteção continua sendo proposta, sem se tornar resposta literal do gestor.

Escolhas técnicas para esta entrega:

- Confirmar toda a quantidade de uma entrada conferida por vez, separando BOA e AVARIADA. Um pedido pode ter entradas ainda pendentes de unitização; o progresso só fica concluído quando todas foram organizadas.
- Um UUID permanente identifica cada unidade física. O ID numérico interno continua como chave relacional; código DUN e quantidade por DUN pertencem à embalagem.
- Divisão/reagrupamento deste recorte são correções da organização física antes do endereçamento, autorizadas por Supervisor/Gestor e justificadas. Operação pode unitizar, consultar e obter etiquetas.
- Reagrupamento exige mesmo pedido, nota, produto, lote, validade, FIFO, instante de chegada real, embalagem/DUN, tipo e condição. Exigir instante igual é uma restrição técnica conservadora deste recorte: não aproxima datas por fuso ou por dia. Assim, duas parcelas da mesma nota com FIFO igual, mas recebidas em dias diferentes, não são misturadas.
- Manter mesma embalagem/tipo e validade é proteção adicional deste recorte. Não há conversão automática de embalagem nem descarte da validade. Produto sem controle de lote expõe explicitamente `controlaLote=false`; nenhum lote fictício é criado.
- Há limites de 100 unidades por confirmação, 20 origens por reagrupamento, 100 vínculos de entrada por unidade e 10.000 identidades históricas por pedido. São limites de implementação, não capacidade física ou comercial aprovada.

## Modelo e conservação

| Registro | Responsabilidade |
| --- | --- |
| `EntradaConferida.unitizadaEm` | Indicar confirmação única da distribuição; mantém quantidades originais, condição, item físico e FIFO |
| `UnidadeLogistica` | UUID, versão, pedido/nota/produto/embalagem, PALLET/BOBINA/VOLUME, BOA/AVARIADA, quantidade atual, lote, validade, datas e identidade ativa/encerrada |
| `ConteudoUnidade` | Quantidade atual de cada entrada que compõe a unidade; vínculos zerados são preservados |
| `OperacaoUnidade` | Chave da solicitação, hash do conteúdo, ação, responsável, instante e resposta original para repetição segura |
| Auditoria existente | Antes/depois e composição das unidades, motivo e correlação, na mesma transação |

A soma das unidades BOAS deve ser exatamente a quantidade de triagem da entrada; a soma das AVARIADAS deve ser exatamente sua quantidade de quarentena. Não se aceita transformar avaria em quantidade boa. Quantidades são decimais exatos na unidade de medida do produto, respeitando a precisão cadastrada: contagem não admite fração indivisível. O DUN informa a quantidade da configuração de embalagem; a quantidade atual da unidade física pode ser diferente, sem conversão implícita.

Dividir mantém o UUID do remanescente, gera um UUID para a nova unidade e transfere a composição pelas entradas em ordem de ID. A soma permanece igual e as datas não são reiniciadas. A divisão deve deixar quantidade positiva nos dois lados.

Reagrupar preserva o UUID do destino, transfere todas as quantidades das origens e encerra as identidades consumidas com quantidade zero. IDs encerrados continuam consultáveis, não são reutilizados, não podem ser divididos nem receber nova etiqueta. Antes/depois da auditoria preserva os vínculos históricos mesmo quando a quantidade atual de um vínculo fica zero. Nenhum registro operacional é apagado.

`ativa` significa identidade com conteúdo; não significa disponível, endereçada ou autorizada para saída. D17/BE08 passou a calcular `disponivelParaSaida` pela localização, condição, bloqueios e vínculos, conforme [documento 22](22-enderecamento-movimentacao-e-estoque.md). A quantidade das entradas e a quantidade das unidades são duas representações da mesma mercadoria: não somá-las para calcular estoque.

**Restrição acrescentada em D17:** divisão/reagrupamento destas rotas exigem unidade ainda não endereçada e sem bloqueio operacional. Transformações de unidade já localizada precisam coordenar ocupação/reserva no fluxo futuro de saída; a restrição preserva posição e datas. A revisão da etiqueta passou a ser independente da versão concorrente: posição/bloqueio não mudam `versaoConteudo`.

## API e permissões

Todas as rotas exigem JWT e alcance simultâneo sobre cliente e armazém; Gestor mantém acesso geral. O servidor obtém a origem pelo pedido/entrada, sem aceitar cliente, produto, lote ou data substitutos no corpo.

Prefixo do pedido: `/api/v1/pedidos-entrada/{pedidoId}`.

| Método e rota | Resultado |
| --- | --- |
| POST `/entradas/{entradaId}/unitizacao` | Distribuir uma entrada efetivada; todos os perfis autorizados |
| GET `/unitizacao` | Total de entradas conferidas, unitizadas, pendentes e conclusão |
| GET `/unidades?pagina=0&tamanho=20` | Lista paginada, incluindo identidades encerradas |
| GET `/unidades/{unidadeId}` | Conteúdo atual, identidade e vínculos de origem |
| POST `/unidades/{unidadeId}/divisao` | Supervisor/Gestor; manter remanescente e criar outra unidade |
| POST `/unidades/{unidadeId}/reagrupamento` | Supervisor/Gestor; incorporar as origens no destino indicado na rota |

Rotas de leitura por código:

- GET `/api/v1/unidades-logisticas/{codigo}`: código UUID completo lido na etiqueta; consulta sempre verifica o alcance.
- GET `/api/v1/unidades-logisticas/{codigo}/etiqueta`: dados atuais para impressão/reimpressão; somente identidade ativa.

Corpos dos comandos:

| Comando | Campos |
| --- | --- |
| Unitizar | `operacaoId` UUID, `versaoPedido`, `motivo`, `unidades[]` com `embalagemId`, `tipo`, `condicao` e `quantidade` |
| Dividir | `operacaoId`, `versao` da unidade original, `quantidadeNovaUnidade`, `motivo` |
| Reagrupar | `operacaoId`, `versaoDestino`, `origens[]` com `unidadeId` e `versao`, `motivo` |

Os comandos devolvem 200 com `operacaoId`, `pedidoId`, `versaoPedido` após a confirmação e detalhes das unidades envolvidas. O GET de entradas de BE06 passa a incluir `unitizadaEm`. Valores de revisão são os devolvidos pelo servidor.

Exemplo fictício de unitização: entrada com 1.000 unidades boas, embalagem previamente cadastrada com 500 unidades por DUN. Submeter duas unidades PALLET/BOA, cada uma com quantidade 500 e a mesma embalagem. O resultado contém dois UUIDs e mantém a origem única das 1.000 unidades.

Erros preservam ProblemDetail e identificação da solicitação: 400 para contrato/precisão incompatível; 403 para alcance/função; 404 para recurso ausente ou pertencente a outro pedido; 409 para revisão antiga, entrada já distribuída, condição/quantidade divergente, unidade encerrada, origens incompatíveis ou conflito de persistência. Paginação mantém tamanho de 1 a 100.

## Repetição, concorrência e integridade

A chave `operacaoId` é única por pedido e compartilhada entre os três comandos. O hash inclui ação, recurso da rota e DTO recebido, inclusive revisão e ordem das listas. Ao repetir a mesma solicitação, o servidor verifica a permissão atual e devolve a **resposta original**, sem gerar identidades, efeitos ou auditorias adicionais. Mesma chave com outro conteúdo retorna 409. Repetição de uma transformação ainda exige perfil Supervisor/Gestor.

A resposta original pode conter quantidades/revisões anteriores a uma transformação posterior: após recuperar uma confirmação, consultar o GET para obter o estado atual. O cliente deve reutilizar exatamente a solicitação original; para outra intenção, gerar nova chave e consultar as revisões atuais.

Toda escrita bloqueia o pedido antes de ler/alterar unidades. A ordem seguinte é cliente, armazém, produtos em ordem de ID e embalagens em ordem de ID; projeções escalares impedem carregar situação antiga antes dos locks. Revalidam-se vínculos ativos e versões. A unitização inicial usa revisão do pedido; divisão/reagrupamento usam revisões das unidades envolvidas. Todas incrementam a revisão do pedido para auditoria.

Unidades, composição, marcador da entrada, revisão do pedido, auditoria e resposta de repetição são confirmados juntos. Falha em qualquer gravação desfaz todos esses efeitos. A serialização por pedido é uma escolha conservadora; desempenho e comportamento dos locks precisam ser conferidos no SQL Server real.

## Etiqueta e ligação com FE06

A etiqueta fornece código permanente, versão do conteúdo, SKU, cliente/armazém, nota/série/número, data FIFO apresentada como `dataEntrada`, chegada real separada, controle de lote/lote/validade, tipo, condição, quantidade atual, unidade de medida, DUN e quantidade por DUN. Não contém endereço físico.

Obter/reobter a etiqueta é consulta: não aumenta estoque, não cria unidade, não reinicia data nem marca impressão como concluída. Após divisão/reagrupamento, a quantidade e a versão mudam; FE06 deverá substituir a etiqueta anterior. Leitura operacional deve consultar o código no servidor e usar o estado atual.

Não foram escolhidos tamanho de papel, linguagem de impressora ou simbologia física. Esta API entrega dados; não gera PDF/ZPL nem envia trabalhos à Tanca. Layout, impressão, legibilidade e leitura no coletor permanecem na FE06 e na validação com Mickael. Não há frontend entregue.

## Banco, validação e próximos passos

V3 prepara três tabelas, FKs, unicidade de UUID/operação, limites de quantidade/estado, índices e a coluna `unitizada_em`; amplia ações da auditoria. V1/V2 permanecem preservadas. O modelo da auditoria passou a declarar `nvarchar(max)`, alinhando-o ao SQL já existente em V1 e evitando truncar eventos com muitas unidades. Não se altera a tabela antiga silenciosamente.

Verificações da entrega original no [registro de validação D16](21-validacao-unidades-logisticas.md), preservado como histórico. D17 acrescentou [endereçamento e disponibilidade](22-enderecamento-movimentacao-e-estoque.md), com [validação atual](23-validacao-estoque-backend.md). As migrations não foram aplicadas; testes locais usam JPA/H2, sem executar SQL Server. Próximo macrobloco: BE09. Reserva/retirada parcial de saída e suas restrições pertencem a BE09/BE10, correções posteriores a BE11 e cobrança a BE12/BE13.
