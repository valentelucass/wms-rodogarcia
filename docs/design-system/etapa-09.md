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

## FE04-REVISAO01 — pente fino da sequência, 13/10/2026

Pedido de Lucas: verificar funcionamento, alinhamento e completude de tudo feito nesta sequência. Revisão de fonte, contratos, documentação, modelo publicado e testes. As alterações preexistentes foram preservadas.

| Pedido | Resultado da revisão |
| --- | --- |
| Modelo Excel e download | Implementados; download byte a byte no Chrome e cópia preenchida aceita pelo parser Java real. Modelo vazio recusado sem criar posições; zeros à esquerda preservados e capacidade incompleta apontada na prévia. |
| Organização dos popups semelhantes | Padrão compartilhado implementado; desktop/mobile, temas, teclado, CSS externo e formulários fiscal/cadastro/unitização conferidos no Chrome. |
| UUID oculto e códigos sequenciais | Implementados; contratos atuais conferidos, testes puros e apresentação aprovados. Concorrência de gravação por API continua pendente. |
| ID e revisão automáticos no fiscal e na unitização | Implementados; duas falhas de estado corrigidas nesta revisão, descritas abaixo. |
| Importação XML existente | Preenche quantidades comerciais e associa SKU cadastrado. Parser/esquema local conferidos; não cadastra produto inexistente nem converte unidades diferentes. |
| Complemento aprovado de peso e bobinas | Ainda não implementado como experiência específica da etapa 3: o contrato de chegada registra quantidade boa/avariada na unidade do produto, sem contagem independente de bobinas. A unitização já permite listar unidades físicas e exige soma igual à conferida, mas acontece em outra etapa. Conversão TO → kg e criação assistida de produto inexistente continuam pendentes. |

**Correções comprovadas:** (1) contexto recebido durante requisição pendente era marcado como consumido; após recusa conhecida, a revisão oculta podia permanecer antiga. Agora só é consumido quando a edição pode acompanhar a atualização. (2) ausência posterior do contexto externo escondia os controles de recuperação de uma resposta incerta. Agora a operação pendente/incerta/concluída conserva sua apresentação e o comando original, incluindo UUID. Testes reproduziram as falhas antes e passaram após as correções. O bloqueio de uma nova operação sem contexto continua preservado.

**Validação final:** 575 testes frontend em 56 arquivos, tipagem/lint/build; 12 testes Chrome em build e DEV local com HTTP interceptado; 76 testes backend comprovadamente sem banco e build/Spotless. A whitelist ganhou somente o teste do modelo publicado e a classe existente de leitura de documento NF-e, ambas revisadas sem Spring/DataSource/HTTP. A execução inicial sem limite de workers apresentou quatro timeouts; limite padrão de dois workers estabilizou a suíte completa sem ampliar prazos nem retirar casos. Capturas de Excel, fiscal e unidades inspecionadas. Hashes das fontes e dos contratos gerados conferidos, sem divergência.

**Ambiente real e limites:** guarda atual aprovada em WMS_DEV/WMSDEV com TLS obrigatório e certificado validado, somente consultas de guarda/metadados. As 559 classes Java do pacote em execução (run `16f339cf-0c27-4be9-b126-f9e175ef78fa`, PID 54280) coincidiram byte a byte com a compilação desta revisão; isso não equivale ao fingerprint integral atual, pois a política de testes no POM foi ampliada depois. Nenhum processo reiniciado ou pacote promovido. Não havia navegador/sessão autenticada disponível pela ferramenta (`apps=[]`, `browsers=[]`), impedindo a prova de jornada e gravações concorrentes autenticadas nesta sessão. O novo build frontend e os testes locais não comprovam a versão carregada na aba do operador.

**Pendências:** implementar o complemento FE05-XML-BOBINAS01; receber XML original compatível para a NF 32325 e esclarecer os 60 kg de diferença, sem inventar a contagem ou os pesos individuais; executar confirmação real do Excel, fiscal e unitização e criações/repetições concorrentes autenticadas após os gates atuais. Nenhuma entrada real ou estoque foi gravado por esta revisão. [Recibo verificável](../../orchestracao/.runtime/fe04-revisao01/resultado.json).

## FE04-INTEGRACAO01 — validação autenticada no DEV, 13/10/2026

Após Lucas disponibilizar acesso, guarda nova aprovada em 26 verificações WMS_DEV/WMSDEV, com TLS obrigatório e certificado validado. Instância existente preservada, sem BAT/reinício. Login, renovação e encerramento da sessão própria conferidos; senha e tokens não persistidos. **35 verificações aprovadas**, com 74 chamadas de negócio registradas no [recibo](../../orchestracao/.runtime/fe04-integracao01/resultado.json) e na [jornada com IDs](../../orchestracao/.runtime/fe04-integracao01/jornada.json).

| Escopo exercitado | Evidência obtida |
| --- | --- |
| Modelo Excel servido | Download idêntico ao arquivo publicado por SHA-256; modelo vazio recusado; quatro posições fictícias aceitas, confirmadas e consultadas, preservando zeros à esquerda. |
| Códigos automáticos | Duas criações simultâneas por família CLI/ARM/CON/SRV/TAB/ENT/SAI/CAR, com sufixos consecutivos e distintos; IDs/códigos reconsultados. Concorrência de dois comandos não equivale a teste de carga. |
| Repetição de comandos | Excel, serviço, carga inicial, saída com referência automática e unitização repetidos simultaneamente sem novos registros; fiscal repetido preserva o resultado. |
| Fiscal | Cliente e armazém gravados com contexto técnico no payload e confirmados por consulta; versão antiga recusada. A ocultação dos campos foi validada nos testes frontend anteriores, não pela API. |
| Recebimento/unidades | Nota manual fictícia de 12 kg, conferência, efetivação e distribuição em duas bobinas de 5/7 kg. Soma incorreta e UUID repetido com conteúdo alterado recusados. Duas unidades persistidas, entrada sem pendência de unitização. |
| Estoque/etiquetas | Duas posições ocupadas, etiquetas com origem/peso/código preservados; saldo total e disponível 12 kg, reserva zero após criar pedidos de saída sem reservá-los. Impressora física não exercitada. |

Ocorrências do executor preservadas no recibo: duas respostas 401 por referência antiga ao token na sessão persistente, corrigida com estado compartilhado; consulta com tamanho 200 ajustada ao limite 100; assertiva de endereços no GET da importação corrigida para a consulta própria de endereços, conforme contrato. Não houve erro 5xx nem falha de produto pendente nos critérios finais.

Fixtures `TESTE-FE04-20261013-D3A412` permanecem identificadas no DEV, sem limpeza: clientes 75/76, armazéns 41/42, produto 80, embalagem 55, importação 6, endereços 138–141, entrada 101, unidades 1343/1344, pedidos de entrada 82/83 e saída 88/89. Demais IDs no recibo. Nenhuma nota real, emissão externa, produção, DDL, H2 ou regressão global de 879 casos foi executada.

Esta rodada fecha as gravações autenticadas desse recorte FE04. Não houve alteração do código da aplicação nem repetição desnecessária das suítes locais já aprovadas. Navegador integrado autenticado não estava disponível; a inspeção visual continua apoiada nos 12 casos Chrome anteriores com HTTP interceptado. O complemento FE05-XML-BOBINAS01 continua pendente (TO → kg, contagem independente na etapa 3 e cadastro assistido de SKU), assim como XML original e esclarecimento dos 60 kg da amostra NF 32325. A unitização existente testada não substitui essa implementação.
