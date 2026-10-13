# FE04-EXCEL01 — importação de endereços e organização dos formulários

Pedido de Lucas em 13/10/2026: baixar uma planilha modelo, organizar a prévia Excel, corrigir o botão de download, aplicar o padrão aos formulários semelhantes e testar o resultado.

## Modelo de endereços

O download aparece na página Importar endereços Excel e no popup da prévia. O arquivo estático `frontend/public/modelos/enderecos.xlsx` usa somente a aba `Enderecos`, sem posições fictícias preenchidas, fórmulas, macros ou vínculos externos.

As sete colunas obrigatórias são `codigo`, `rua`, `nivel`, `posicao`, `descricao`, `tipo` e `sequenciaColeta`. As opcionais são `tipoUnidadePermitido`, `capacidadePesoKg`, `alturaMetros`, `larguraMetros`, `profundidadeMetros` e `empilhamentoMaximo`. Os nomes correspondem ao parser `ExcelEnderecoService`; não renomear os cabeçalhos nem acrescentar uma aba de instruções.

Código, rua e posição têm formato de texto para conservar zeros à esquerda. O cabeçalho fica congelado; as primeiras sete colunas usam azul e as opcionais cinza, com explicação textual no popup. As 5.000 linhas de preenchimento têm formatos preparados e listas de tipo de endereço e unidade permitida. O modelo vazio precisa ser preenchido antes do envio. As validações e os limites continuam no backend; a lista do Excel apenas auxilia o preenchimento.

## Padrão visual compartilhado

- Links de download têm aparência de ação secundária, ícone de 16 px, espaçamento, borda, hover e foco conforme os tokens existentes. Permanecem links nativos com `download`.
- O formulário Excel apresenta orientação e download, armazém por nome, arquivo e justificativa em largura inteira. Instruções ficam em uma superfície discreta. O UUID continua automático no comando, oculto na edição e na confirmação conforme FE04-AUTO01.
- Os formulários de operação em `RecordDialog` usam duas colunas no desktop e uma abaixo de 700 px. Textos longos, arquivos e grupos de campos ocupam uma linha inteira. Referências fixas e listas internas preservam seus componentes e contratos.
- O título da ação aparece uma única vez quando o cabeçalho já o apresenta. Ao editar um registro, o nome do registro e o título da ação continuam distintos.
- CSS é carregado pela folha externa `src/styles.css`. Importar CSS pelo componente injeta estilos incompatíveis com `style-src 'self'` no DEV. A política de segurança permanece inalterada.

Não foram alteradas regras de estoque, permissões, payloads, confirmação, repetição ou tratamento de resposta desconhecida. O fluxo continua prévia → consulta → confirmação integral.

## Conferência

O arquivo é conferido estruturalmente contra os cabeçalhos do parser e visualmente. O teste `tests/browser/address-import.spec.ts` usa HTTP interceptado para verificar download, CSS externo no DEV e no build, campos, envio multipart, UUID e arquivo preservados ao voltar, erro de prévia, teclado e responsividade. A configuração própria é `playwright.address-import.config.ts`.

Esta verificação local não representa importação no SQL Server, homologação operacional ou medição da versão atualmente servida pelo BAT.

Resultado: tipagem, lint e build aprovados; 566 testes frontend passaram com dois workers. Os oito casos Chrome passaram em rodadas focais: seis da importação (download, multipart, retorno, erros, tema e teclado) e dois cobrindo clientes, armazéns e endereços em 1440/390 px, no DEV e no build. Capturas inspecionadas e arquivo comparado byte a byte no download. [Recibo local](../../orchestracao/.runtime/fe04-excel01/resultado.md).

## FE04-AUTO01 — códigos internos automáticos

Complemento solicitado por Lucas em 13/10/2026. Formato escolhido expressamente: sequências por cadastro, como CLI-000001 e ENT-000001.

| Cadastro | Prefixo |
| --- | --- |
| Cliente | CLI |
| Armazém | ARM |
| Conjunto de posições | CON |
| Serviço de cobrança | SRV |
| Tabela de cobrança | TAB |
| Pedido de entrada | ENT |
| Pedido de saída | SAI |
| Carga inicial | CAR |

O formulário pede os dados do cadastro e informa que o código será gerado ao confirmar. O código retornado pelo servidor aparece na consulta e no resultado. Criações não herdam códigos de registros selecionados. Entrada por XML, devolução e efetivação de carga inicial também usam ENT; resolução de remanescente usa SAI.

O UUID técnico permanece automático no estado e no payload, conservado na repetição do mesmo comando. Não aparece como campo, coluna ou item de conferência nos componentes compartilhados. A recuperação de importação XML continua usando a chave guardada internamente.

SKU do cliente, DUN, documentos fiscais e endereços físicos continuam informados conforme seus contratos. Os cadastros existentes mantêm seus códigos. A API aceita os códigos explícitos dos consumidores anteriores e gera quando ausentes; a interface usa geração automática.

O backend calcula o próximo sufixo numérico da família com bloqueio da tabela na transação de criação, incluindo registros históricos/inativos. Mínimo de seis dígitos, sem truncar números maiores. Não há tabela nova, migration ou sequência no navegador. A serialização da alocação pode gerar espera; a concorrência de gravação requer validação real própria.

Conferências: 568 testes frontend, 67 testes puros backend, dez casos Chrome, tipagem/lint/build e oito consultas de sequência reais WMS_DEV/WMSDEV após guarda atual. Gravação concorrente HTTP/JPA e versão ativa ainda não validadas. [Evidências e limites](../../orchestracao/.runtime/fe04-auto01/resultado.md).

## FE04-AUTO02 — referências técnicas do cadastro e da unitização

Complemento de Lucas em 13/10/2026: em Dados Fiscais Cadastrais, retirar os campos Identificador, Identificador desta operação e Revisão atual; em Novo pedido de entrada, conservar a referência automática; em Distribuir em unidades, retirar Pedido (ID), UUID e Versão do Pedido da edição.

ID e revisão dos dados fiscais vêm do cadastro consultado. Na distribuição, o pedido é o pedido existente selecionado, criado pelo sistema anteriormente; abrir a ação consulta seu detalhe para obter a revisão atual. Não se cria outro pedido durante a unitização. A entrada conferida continua escolhida entre as referências do pedido. O acesso pelo cabeçalho exige seleção de pedido; sem ID/revisão confirmados, a ação orienta selecionar e consultar o registro antes de apresentar o formulário.

A revisão permanece obrigatória no contrato: o backend compara a revisão enviada com a persistida e recusa alterações concorrentes. O operador não digita nem altera esse controle. UUID, ID e revisão continuam no comando, ocultos também na conferência. Resposta incerta preserva o pedido, a revisão e a chave do comando original mesmo que o contexto externo seja atualizado.

O padrão usa os componentes compartilhados e evita grupos vazios depois de ocultar campos automáticos. Dados fiscais de negócio continuam disponíveis conforme seus contratos; o pedido não elimina a configuração fiscal nem autoriza inventar valores fiscais. Catálogo de popups informativos revisado: nenhuma entrada correspondente a esses campos técnicos em `information.ts`; as instruções de seleção ficam junto à ação.
