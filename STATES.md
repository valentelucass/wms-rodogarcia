# Estado e trilha de implementação do WMS Rodogarcia

## FE02-DADOS-AUD01 — auditoria real dos gráficos, 23/10/2026

**Status:** conferência de leitura concluída. SQL independente, API autenticada e navegador real em quatro contextos conferem: ocupação 89/3/8/0 = 100 posições; fila 43/20/0/0 = 63 pedidos. Cinco retirados e doze cancelados permanecem no histórico. 28 reservas pertencem a 20 pedidos; 92 unidades endereçadas ocupam 100 posições. Das 1.354 unidades ativas, 1.262 não têm endereço, sendo 1.250 das massas REG01; existem no banco e não entram na ocupação física.

Guarda atual 26 checks, 22 SELECTs incluindo identidade; sem escrita de negócio, H2, DDL, reinício ou PROD. Backend ativo b8511b96 preservado e distinguido da fonte fiscal local em desenvolvimento. Dois testes frontend acrescentados; 28 aprovados, tipagem/lint/formatação/build aprovados. Ajuda central esclarece unidades sem endereço, escopo do KPI e exclusão de pedidos encerrados. [Relatório e evidências](orchestracao/.runtime/dashboard-data-audit-20261023/auditoria.md); sem renovar aceite histórico dos 879 casos.

**Pendência lateral FE02-DADOS-AUD01-OBS01:** pedido 95/SAI-000006 cancelado e reserva 56 cancelada conservam leitura 11 em LEITURA sem encerramento. Sem impacto nos totais auditados; encaminhar correção do encerramento de leituras no cancelamento/retorno, com teste próprio e preservação do histórico. Nenhuma correção SQL da fixture ou alteração de expedição foi feita neste recorte.

## BE10-NOTAZZ-IMP01 e FE10-NOTAZZ-IMP01 — implementação autorizada, 23/10/2026

**Status:** em implementação, a pedido expresso do responsável: “pode implementar de forma testada e rigorosa”. Esta autorização substitui a restrição anterior de somente documentação para esta frente. Dependências: BE05/BE09/BE10/BE11 e FE04/FE09/FE10; levantamento 46–49 e resposta final sobre retorno com cancelamento impedido.

- [ ] Solicitação fiscal persistida antes do envio, identidade estável, snapshot, concorrência, conciliação e proteção contra duplicidade.
- [ ] Completar dados fiscais com fonte explícita e bloqueio de emissão incompleta; emitente da origem, destinatário proprietário e valores proporcionais.
- [ ] Unificar emissão/conferência na tela Retirada fiscal da mercadoria; listar pedidos, situação e números, mantendo retirada física distinta.
- [ ] Cancelamento externo confirmado e nova entrada vinculada quando cancelamento é impedido, preservando fatos e estoque.
- [ ] Testes puros, contratos, build, conferência visual e integração SQL guardada proporcional; Notazz real exige configuração/ambiente próprios comprovados.

Preservar as alterações preexistentes das demais frentes. Somente WMS_DEV, com guardas atuais; sem PROD, emissão fiscal de produção, commit ou push. Credenciais externas protegidas, nunca no navegador/log/repositório. Evidências próprias em orchestracao/.runtime/notazz-implementation-20261023/; fonte, testes locais, ambiente ativo e homologação externa serão relatados separadamente.

## FE02-PRODUTOS-UX01 — disponibilidade compacta por SKU, 23/10/2026

**Status:** concluído na fonte local. Lucas pediu compactar a Disponibilidade por produto e rejeitou a tentativa com tabela e grupo de zerados. Apresentação final: todos os SKUs visíveis, linhas com composição curta e total próximo, duas colunas no desktop/uma no celular. Produtos zerados indicam Sem saldo; seleção por clique/foco mostra cliente e quatro quantidades exatas uma vez abaixo. Paginação, contagem, ordem da página, unidades e contratos preservados. Cabeçalho/rodapé mais baixos; sem tabela ou grupo recolhível.

26 testes existentes, tipagem/lint/formatação/build aprovados. Navegador isolado fictício: oito layouts claro/escuro, 1640/1024/768/390 px, cerca de 316 px para seis produtos no desktop; cinco estados/interações, seleção sem consulta, paginação/foco, zero, saldo, Long/decimal exatos e vazio. Sem transbordamento, erro React ou HTTP de negócio. Evidências em orchestracao/.runtime/product-availability-20261023/. Atualização Graphify tentada sem force, preservada pelo guard de redução. Sem backend, SQL, reinício ou homologação integrada nova.

## FE11-FATURA-LISTA-UX02 — organização da lista de fechamentos, 23/10/2026

**Status:** concluído na fonte local. A pedido de Lucas, lista com filtros compactos/alinhados, ações no cabeçalho e orientação inicial central. Atualizar lista aparece após consulta/erro pertinente; contagem somente com resultados. Cliente/armazém sem legenda visual redundante, altura de 44 px e adaptação ao celular. Estados inicial/vazio/carregando/erro distintos; restauração sem filtros oculta tabela/contador/paginação anteriores. Novo fechamento, conferência, acesso, valores da versão e permissões preservados.

Sete testes existentes, tipagem/lint/formatação/build aprovados. Navegador isolado com componentes reais/transporte fictício: oito layouts claro/escuro, 1640/1024/768/390 px, alinhamento medido, teclado/aplicação, lista com valor, restauração, vazio/carregando/erro e perfil restrito, sem transbordamento ou HTTP de negócio. Evidências em orchestracao/.runtime/invoice-list-layout-20261023/. Sem backend, SQL, reinício ou nova homologação integrada.

## FE02-GRAFICOS-UX01 — linhas e pontos nos gráficos operacionais, 23/10/2026

**Status:** concluído na fonte local. A pedido de Lucas, Ocupação física e Fila de saída passam de barras para linhas com pontos e preenchimento suave. Valores atuais por área/etapa, total recebido, seleção por ponto/cartão e detalhe preservam os dados e os atalhos. A faixa azul lateral e a caixa cinza do detalhe foram removidas após a correção visual do responsável. Linha representa comparação entre categorias atuais, sem histórico inventado; capacidade/livres somente quando permitidos e informados. Ajuda central atualizada; disponibilidade por produto preservada.

26 testes existentes, tipagem/lint/formatação/build aprovados. Navegador isolado com transporte fictício explícito: oito layouts claro/escuro em 1640/1024/768/390 px, foco/Escape/seleção/atalhos, sete estados incluindo zero, Long acima de 2^53, capacidade ausente, acesso restrito, séries vazias, carregamento e erro. Sem transbordamento, geometria inválida ou HTTP de negócio; processos próprios fechados. Evidências em orchestracao/.runtime/operation-charts-20261023/. Graphify preservado pelo guard de redução, sem force. Sem backend, SQL, reinício ou homologação integrada nova.

## FE02-KPI-CORES01 — fundos dos indicadores, 23/10/2026

**Status:** concluído na fonte local. A pedido de Lucas, os dez KPIs do Início recebem fundos por significado: quarentena com unidades vermelha/sem unidades verde; posições livres verdes, capacidade ativa sem posições livres amarela; financeiro completo verde/incompleto amarelo e falha de previsão vermelha. Ocupação/posições ocupadas em azul, armazenadas/entradas em turquesa e reservas/saídas em violeta identificam volumes informativos. Sem meta arbitrária de ocupação, classificação de atraso ou alteração de valores/regras/permissões. Carregamento, falta de acesso e capacidade não informada preservam estado neutro pertinente. Ajuda central revisada.

36 testes existentes aprovados, nove do contexto revalidados após tratar campos nulos; tipagem/lint/formatação/build aprovados. Navegador isolado: oito layouts em dois temas/1640/1024/768/390 px, contraste textual mínimo 4,5:1, seis estados e ajuda por foco/Escape, sem transbordamento ou HTTP de negócio. Evidências em orchestracao/.runtime/dashboard-kpi-colors-20261023/. Graphify preservou o mapa pelo guard de redução, sem force. Sem backend, SQL, reinício ou nova homologação integrada.

## FE12-INVENTARIO-UX03 — alinhamento da visão geral, 23/10/2026

**Status:** reorganização visual na fonte local, a pedido de Lucas. Cliente, Armazém, Produto/SKU e Endereço têm o mesmo alinhamento e altura; a busca acompanha a grade e as ações ficam alinhadas à sua base. As três visões usam abas simples com sublinhado ativo, sem a faixa cinza rejeitada pelo responsável. Mensagem inicial e adaptação ao celular organizadas; regras, consultas e permissões preservadas.

Conferência em navegador isolado com componentes reais e transporte fictício explícito: oito layouts, claro/escuro, 1640/1024/768/390 px; medidas dos campos, teclado, três visões, consulta vazia/erro/restauração e ausência de transbordamento aprovados. Nove testes existentes do inventário, tipagem, lint, formatação e build aprovados. Evidências em orchestracao/.runtime/inventory-layout-20261023/. Graphify recusou a redução e preservou o mapa histórico, sem force. Sem backend, SQL ou reinício; esta conferência visual não renova a homologação integrada anterior.

## FE12-INVENTARIO-UX02 / BE14-INVENTARIO01 — inventários por escopo, 23/10/2026

**Status:** implementado e ativo no WMS_DEV em 23/10/2026, com V14 aplicada, guarda atual aprovada, fonte/pacote atuais e validação HTTP/navegador/SQL real. Lucas confirmou que ajustes necessários no WMS_DEV não exigem nova autorização por etapa. Leitor físico e volume de 20.000 unidades permanecem sem homologação; não transportar este aceite para PROD.

- [x] Uma tela Inventário com visão por SKU/posição, filtros, histórico e Realizar inventário; três etapas antigas removidas da apresentação principal.
- [x] Modal às cegas, por SKU ou por posição; contagem exige todas as etiquetas do escopo cliente/armazém. Na contagem, só o observado é retornado/apresentado; esperado e divergências exigem encerramento.
- [x] Leituras únicas por etiqueta, recontagem substitutiva explícita, precisão de quantidade, versão e repetição da mesma chave após resposta incerta.
- [x] Relatório por SKU e por mercadoria/posição, não lidas como faltantes com confirmação, ocorrências de fora da base e movimentação, exportação CSV e relatório fechado imutável. Sem ajuste/reendereçamento automático.
- [x] Carga inicial e contagens/ajustes históricos preservados em acesso administrativo secundário, com papéis existentes.
- [x] 847 testes frontend aprovados; após ajuste final de foco do leitor, 9 testes do inventário revalidados; tipagem/lint/formatação/build aprovados. 31 testes backend puros aprovados, sem Spring/banco; pacote oficial SomenteBuild aprovado com SQL/H2 zero.
- [x] Antes/depois dos componentes reais, dados fictícios; cinco layouts antes/atual, claro/escuro e 1440/390 px, com seleções dos três tipos, foco/diálogo/relatório, sem erro React, transbordamento ou HTTP de negócio.
- [x] V14 aplicada por Flyway protegido somente WMS_DEV; duas tabelas/23 colunas, histórico V1–V13 preservado e direitos mínimos WMSDEV conferidos. Ativação exclusiva em application-frontend-dev.properties; padrão geral false.
- [x] 27 verificações HTTP/navegador reais: três tipos, relatório, recontagem substitutiva, início/leitura/fim/cancelamento idempotentes, disputa nativa 200/409, base cega oculta, relatório fechado imutável e estoque preservado. Movimentação controlada da fixture sinalizada sem desfazê-la.
- [x] SELECT independente WMSDEV/TLS confirma cinco inventários (quatro concluídos/um cancelado), relatórios JSON válidos, vinte operações únicas e três mercadorias/25 unidades de produto. Fixtures fictícias criadas somente pela API autenticada; sem limpeza ou SQL de negócio direto.
- [x] CSS do inventário/fatura e controles financeiros registrado no agregador externo; imports por JavaScript removidos para respeitar style-src 'self'. Navegador real confirma grid/flex, SKU separado e zero violações de estilos; quatro fotos finais sem estado de carregamento. Tipagem/lint/build finais aprovados.
- [ ] Homologar leitor físico e volume máximo com cenário operacional. A amostra real contém três mercadorias; não comprova carga de 20.000.

Proposta histórica preservada em database/docs/propostas; fonte aplicada imutável em database/migrations/V14__inventarios_por_escopo.sql. Registros locais anteriores: orchestracao/.runtime/inventory-20261023/validation-summary.json. Ativação: orchestracao/.runtime/inventory-activation-20261023/validation-summary.json; versão ativa b8511b96-0296-49fd-a7f7-2f6fa3ca37b2, portas 25580/25581. Corrigido o registro da rota no controle de acesso após o primeiro HTTP403. Contrato/replay estrutural: dez testes aprovados; preparo do ambiente: 18/18. Atualização AST do Graphify sem force preservou o grafo pelo guard de redução; adendo focal dos nove endpoints mantido. Sem H2, PROD, emissão fiscal, integração externa ou leitor físico.

## FE12-INVENTARIO-NOME01 — nome da página, 23/10/2026

**Status:** concluído na fonte local. A pedido de Lucas, Contagem e carga inicial passa a se chamar **Inventário** no menu, cabeçalho e título. FE12 continua vinculado a BE14; rota contagem, etapas, contratos e regras preservados. Referências atuais de telas/contratos e expectativa do roteiro de navegação atualizadas.

- [x] Atualizar nome central da jornada e suas referências atuais.
- [x] Conferir formatação, tipagem, lint, 25 testes existentes de páginas e build.

Evidências em orchestracao/.runtime/inventory-name-20261023/validation-summary.json. graphify update . executado sem force; resultado registrado. Sem alteração backend, SQL, integração externa ou publicação.

## FE11-FATURA-UX01 e BE13-ESL-MINUTA01 — Fechamento de Fatura, 22/10/2026

**Status:** FE11-FATURA-UX01 concluído e validado na fonte local; dependências BE12/BE13 preservadas. BE13-ESL-MINUTA01 continua pendente do ambiente/token e das regras da Dalga. Pacotes locais gerados; a versão ativa de DEV/PROD não foi reiniciada nem homologada nesta entrega.

- [x] Reunir as duas pendências: Fechamento de Fatura sem cinco etapas e sem Serviços e cálculo na navegação.
- [x] Lista por cliente/período/valor da versão vigente; detalhe dos serviços com referências exatas, preservando zero, crédito e cálculo incompleto.
- [x] Serviços realizados/adicionais e avarias na preparação; aprovação integral, reabertura, versões, ajustes e documentos preservados.
- [x] Preparar a ação Enviar minuta ao ESL, desabilitada até integração validada.
- [x] Ajuste visual solicitado: filtros alinhados à altura das seleções e selos Aprovado/Aprovada em verde nos dois temas. Sete testes do fluxo, tipagem/lint/formatação/build e quatro layouts conferidos novamente; evidências em orchestracao/.runtime/invoice-alignment-20261022/.
- [x] 829 testes frontend aprovados; tipagem, lint e build aprovados; conferência visual em 1440/390 px, claro/escuro, sem erro de página ou transbordamento horizontal.
- [x] Ordenar a consulta dos fechamentos por início do período e ID decrescentes; backend empacotado pelo preparador oficial SomenteBuild, com zero testes/SQL/H2.
- [ ] Validar integração real/token/regras no ambiente próprio ESL da Dalga, em entrega separada.

[Decisão](docs/06-decisoes-e-pendencias.md#fe11-fatura-ux01-e-be13-esl-minuta01--fechamento-de-fatura-22102026), [apresentação](docs/design-system/etapa-06.md#fechamento-de-fatura--fe11-fatura-ux01) e evidências em orchestracao/.runtime/invoice-reorg-20261022/validation-summary.json. Única alteração backend: ordenação de leitura; sem mudança de contrato, schema, credenciais ou emissão externa. graphify update . executado sem force; preservado pelo guard de redução do grafo histórico. Não houve teste com banco/integração reais nem implantação. Próximo passo da integração: acesso protegido, ambiente/empresa Dalga, campos/travas, retorno e prevenção de duplicidade.

## BE10-NOTAZZ-ALINH01 e FE10-NOTAZZ-ALINH01 — respostas fiscais e exemplos, 22/10/2026

**Status:** complemento de 23/10/2026 incorporado; última dúvida operacional resolvida, sem implementação. Se a circulação impedir cancelar a NF-e, emitir nova nota de entrada e processar o retorno, inclusive dentro de 24 horas. [Alinhamento e exemplos](docs/49-alinhamento-fiscal-respostas-e-exemplos.md).

Gestor definiu emissão pelo CNPJ do armazém da entrada, destinatário sempre proprietário, retorno correspondente ao tratamento da entrada, agrupamento conforme cliente com consolidação permitida à Bracell e valor proporcional da entrada. Complemento de 23/10/2026: circulação que impede cancelamento exige nova nota de entrada, com saída original preservada e reendereçamento da quantidade retornada. Após o prazo, permanece o novo processo de entrada mesmo sem retirada, conservando quantidade física única. Fiscal atua primeiro, operador depois; retorno simbólico pode ser somente fiscal. Não repetir a pergunta de circulação ao gestor.

Dois DANFEs Tigre/Castro conferidos integralmente por extração e imagem; 30 verificações documentais/aritméticas aprovadas, sem consulta fiscal autenticada. Inventário atualizado para 107 regras e 75 cenários planejados, mantendo IDs anteriores. As dúvidas operacionais foram respondidas; detalhes de XML/tributação seguem com o fiscal, sem novo questionário técnico ao gestor. Código, configuração, banco e notas reais preservados. Prévia visual antes/depois produzida para compreensão, sem aplicação no sistema.

Pesquisa oficial de 22/10/2026 e resposta de 23/10/2026 conciliadas no documento 49: ausência de circulação para cancelamento comum; nova nota de entrada quando a circulação impede cancelamento. A pendência de negócio está encerrada. Credenciamento, XMLs, configuração tributária e homologação permanecem insumos da implementação futura. Atualização somente documental; sem nova execução da aplicação ou fiscal.

## BE10-NOTAZZ-MAP01 e FE10-NOTAZZ-MAP01 — mapeamento fiscal para implementação futura, 22/10/2026

**Status:** levantamento documental concluído e revisado, com insumos fiscais externos identificados; implementação não iniciada. **Dependências:** BE05, BE09, BE10, BE11, FE04, FE09 e FE10; contrato Notazz e parâmetros fiscais por operação/estabelecimento.

Pedido atual do responsável: ler AGENTS.md, estudar profundamente a API Notazz e o projeto, documentar todas as regras pertinentes e esgotar fontes/verificações antes de retornar dúvidas reais. Restrição expressa: não aplicar código agora. Documentação e testes puros focais estão autorizados; emissão externa, banco, configuração, migration e reinício não foram executados.

- [x] Conferir PDF/Word originais, regras RN/Q/AC, contratos e fontes atuais, mantendo a proveniência.
- [x] Mapear tela/contratos, cadastros, estados, cobertura multinota, emissão versus retirada, cancelamento/devolução, avaria e cobrança.
- [x] Consultar guia Notazz, Swagger, modelos JSON e leiaute público; confrontar divergências sem chamadas fiscais autenticadas.
- [x] Executar 68 testes puros de frontend em 12 arquivos, com rede bloqueada e evidências isoladas; zero falhas/ignorados.
- [x] Concluir revisão dos documentos de regras, cenários e sequência de implementação: 102 regras, 65 cenários, 101 campos públicos de criação e índices dos modelos completos. Revisão independente corrigiu completude fiscal, proteção do legado e condição do aviso comercial/tributável.
- [ ] Implementar a integração em demanda futura; os critérios desta implementação permanecem separados do estudo.

[Mapeamento](docs/46-mapeamento-fiscal-notazz.md), [regras](docs/47-regras-de-negocio-fiscal-notazz.md) e [cenários e validação](docs/48-cenarios-e-validacao-fiscal-notazz.md). Três testes puros de backend foram selecionados na whitelist; o executor recusou o início por memória disponível abaixo do mínimo/comprometimento acima do teto. Não houve execução dos testes backend ou contorno da proteção. Integração Notazz/SQL, homologação tributária e parâmetros reais continuam sem comprovação nesta entrega.

Evidências documentais em [resumo](docs/levantamentos/fiscal-notazz-20261022/validacao.json), [inventário de campos](docs/levantamentos/fiscal-notazz-20261022/inventario-campos-notazz.json) e arquivos adjacentes. Próxima implementação deve consumir o plano e registrar sua própria validação; não iniciar automaticamente código, conexão ou emissão por existir este levantamento. O grafo existente foi consultado, sem rebuild nesta entrega documental.

## FE04-TABELA-UX01 — formulário de tabela reorganizado, 20/10/2026

Lucas pediu reorganizar Nova tabela e seus itens. Criar/editar usam apresentação própria: descrição em uma linha, identificação e vigência agrupadas, datas alinhadas, serviços com contador e rótulos curtos, sem fieldsets aninhados; justificativa e resolução histórica separadas. A área histórica começa recolhida, informa quando há resolução no rascunho e conserva seu conteúdo ao recolher/voltar. Confirmação permanece acessível durante a rolagem. Ajuda no catálogo central descreve tipo/proprietário, preço ou percentual e categoria de armazenagem conforme o serviço atual, sem cálculo no navegador.

232 testes frontend/contratos aprovados, incluindo inclusão/remoção, valores/IDs exatos, zero/ausência, conferência sem envio, resolução preservada/removida, edição com revisão e conflito conservando o mesmo ID de operação. Tipagem/lint/formatação/build próprios e Chrome isolado em sete combinações de tema/largura mais baseline; sem nova API/SQL, cadastro ou reinício. Capturas usam componentes reais com opções/valores fictícios em memória; não comprovam nova integração SQL nem a versão servida pelo DEV habitual. [Recibo e antes/depois](orchestracao/.runtime/billing-table-layout-20261020/validacao.json). As regras e os dados financeiros não foram alterados; pendências do histórico abaixo permanecem.

## FE02-KPI-UX01 — layout e conferência dos dez indicadores, 20/10/2026

Lucas pediu refazer os cartões e conferir regras/atributos com fatos, especialmente Valor armazenado. Oito operacionais têm alturas uniformes; os dois financeiros ocupam linha própria, com previsão mais larga e parcial/contexto/detalhes separados. Ajuda explicita unidade de contagem, situações e alcance físico versus cliente. Valor armazenado conserva total legado null quando incompleto, mas agora informa conhecido, cobertura e motivos no mesmo GET; mostra parcial apenas quando existe contribuição apurável. Perfil Operação oculta também valores previamente carregados. Entrada só avariada não exige preço para demonstrar zero; mercadoria boa continua proporcional à origem, sem duplicar item após unitização.

WMS_DEV/WMSDEV real: dois GETs autenticados200 e SELECTs independentes conferiram 17 relações. Observado: 1.317 posições, 86 ocupadas/1.231 livres, 7%, 1.351 unidades, 8 em quarentena, 28 reservas, 16 entradas e 62 saídas abertas. Valor conhecido R$130.788,9165433: 1.348 unidades e 8 itens de entrada valorados, 3 unidades/2 itens pendentes; total ainda null. Previsão R$1.143,20 parcial/236 ocorrências. Nenhum preço ou histórico saneado por suposição. [Auditoria dos dez KPIs](orchestracao/.runtime/kpi-layout-20261020/auditoria-kpis.json), [atributos e regras](docs/design-system/etapa-05.md#kpis-compactos-e-atributos-conferidos--fe02-kpi-ux01-20102026) e [validação](orchestracao/.runtime/kpi-layout-20261020/validacao.json).

28 testes puros backend, 238 frontend/contratos, TS/lint/formatação e builds próprios; replay Chrome dos DTOs reais em nove layouts depois mais baseline, sem overflow, com teclado/foco e quatro estados adicionais fictícios. O auxiliar visual inicial não reproduziu o CSS anterior; comparação final exige explicitamente o grid de cinco colunas, corrigida sem mudar o produto para o teste. Auxiliar SQL parou antes de abrir conexão por setter local e depois por diretório de recibo; guardas preservadas, sem tentativa de transporte nessas falhas. Após correções e guarda atual26/26, uma abertura WMSDEV e dois SELECTs concluíram a conferência, sem repetir login. Sessão/auditoria nativas normais, sem comando de negócio, DDL ou fixture na validação.

BAT oficial carregou fonte `9c3464984169be7b10ce88bd447afed4c08b3fdf66242a6f0afbf3c653de2342`, JAR `9b2d0a0dea632e46768c5bcc8a976b584b23932f4076147d9584808fd0d2afd9`, run `a82acf15-c4fe-4c19-8656-fd7116f81509`. Reiniciou somente o DEV atestado anterior; backend13100/25580 e frontend25624/25581 mantidos para uso por link HTTPS da porta25581 no VSCode. PIDs/provas são datados, não autorização para encerramento futuro. API/SQL atuais passaram; navegador desta rodada é replay local, não nova jornada AuthApp integrada. Originais, demais bancos/processos e pendências anteriores preservados; sem commit/push pelo agente.

## FE02-FAT-AUTO01 — destaque do valor parcial, 20/10/2026

Lucas apontou o traço no card apesar de haver R$ 1.143,20 conhecidos. Ajustada a apresentação: o conhecido passa a ser o número principal com Parcial — cálculo incompleto e contagem de ocorrências; no diálogo, aparece antes do total identificado como indisponível. Total completo continua distinto. Agregado sem moeda única ou sem valor permanece —, com explicação de moeda quando aplicável. Ajuda central revisada; nenhum cálculo ou dado de negócio alterado.

18 testes focais e 24 de contexto/ajuda/performance frontend, tipagem, lint, formatação e build próprio aprovados. Chrome local reproduz o DTO real já consultado: mesmos R$ 1.143,20/236 ocorrências, antes/depois, claro/escuro e 390/1440 px, sem overflow, com Enter/Escape e foco restaurado. Não é uma nova consulta integrada. As ocorrências incluem 125 de tarifa de armazenagem ausente e 27 de contrato ausente; repetem-se por contexto/período/unidade, não são 236 cadastros. Configuração e histórico continuam pendentes, sem inventar preços. [Diagnóstico](orchestracao/.runtime/faturamento-parcial-20261020/diagnostico.json), [validação](orchestracao/.runtime/faturamento-parcial-20261020/validacao.json), [antes](orchestracao/.runtime/faturamento-parcial-20261020/before-dark-1440.png) e [depois](orchestracao/.runtime/faturamento-parcial-20261020/after-dark-1440.png). Previews próprios encerrados; BAT/DEV habitual preservado, sem nova API/SQL, login ou reinício. A conferência visual não mede a fonte servida pelo DEV habitual.

## BE02-DEV-FONTE01 — fonte estável e DEV iniciado, 20/10/2026

O BAT do usuário `7b5ce937` terminou em DEV_CURRENT_SOURCE_CHANGED porque a limpeza Java de `CalculoCobrancaService` ocorreu depois do build, durante a preparação do banco. Único fingerprint divergente; guarda 26/26 aprovada e nenhum processo encerrado. Causa da sobreposição assumida pelo agente; proteção/launcher/migrations não foram alterados para contorná-la.

Nova execução oficial `da3049cb-f932-486d-88af-c1fcc9d65f79` concluiu as quatro etapas, sem edições de fonte no intervalo. Fonte antes/depois `d4fd3cf1df10c3d23d4f21a73038fcf7fbde5078c2ca8c0ae4a7f58228fb1ce6`, JAR `19f712e2de13ec06d26b45d560b1bb386823353b497ce2ab78b3b1096a0c2d4f`, WMS_DEV/WMSDEV/sqlserver-dev e guarda atual 26/26. Backend PID60452/25580 e frontend PID29472/25581 online, mantidos para uso. Modo tunnel: acessar link HTTPS da porta25581 no painel Portas do VS Code. GET público CSRF e página inicial retornaram200, sem guardar corpo/cookie. Estado PROCESSES_READY_ROUNDTRIP_PENDING: login/jornada autenticada não repetidos nesta ativação. [Recibo](orchestracao/.runtime/dev-source-changed-20261020/resultado.json), [launcher](orchestracao/.runtime/be02-dev-fonte01/prumo/runs/da3049cb-f932-486d-88af-c1fcc9d65f79/launcher.json). Estes PIDs/provas são datados; próxima intervenção exige comprovação atual. A ativação substitui a pendência de carregar a fonte, sem transportar aceite dos demais testes pendentes abaixo.

## BE14-FAT-AUTO01 — aviso Java de sobrecarga sem uso, 20/10/2026

Removida somente a sobrecarga privada `contribuicao` de quatro parâmetros, sem chamadores. A de cinco permanece porque `VisaoOperacaoIntegrationTest` a chama por reflexão; comentário e supressão localizada identificam esse uso. Implementação de seis parâmetros e cálculos preservados. Compilação, formatação de 391 arquivos e 20 testes puros de previsão/motor aprovados, sem banco, H2 ou reinício. [Recibo](orchestracao/.runtime/unused-contribuicao-20261020/validacao.json). A fonte mudou após o pacote da entrega abaixo: seus hashes/provas de integração permanecem históricos, sem atribuí-los a este ajuste. Próximo preparo DEV deve consumir a fonte atual pelo fluxo documentado.

## BE14-FAT-AUTO01 / FE02-FAT-AUTO01 / FE06-LOTE01 — previsão de faturamento e lote na etiqueta, 20/10/2026

Fonte corrigida conforme pedido de Lucas: o card acompanha a prévia por ciclos contratuais, com fatos físicos/serviços deduplicados, vigências, mínimo/GRIS e pendências explícitas. Consulta independente do mapa, somente leitura; não confirma fatos nem altera fechamentos. Competência pelo mês do último dia incluído do ciclo, sem rateio mensal novo. A antiga soma de NFS-e permanece legado, não aprovação expressa da fórmula pelo usuário. A etiqueta apresenta `L: <lote>` recebido da unidade, inclusive na mídia de impressão; ausência e produto sem controle são distintos. A pedido adicional de Lucas, Atualizar previsão foi removido: Atualizar visão reconsulta mapa/resumo e previsão em paralelo. [Escopo](orchestracao/.runtime/faturamento-etiqueta-20261020/escopo-e-oraculos.json), [regras](docs/29-cadastros-servicos-e-calculo.md#prévia-automática-do-faturamento--be14-fat-auto01) e [etiqueta](docs/design-system/etapa-06.md#lote-na-etiqueta--fe06-lote01).

Conferência local: 214 casos úteis backend (215 executados, excluído um teste de string HQL removido), 280 testes frontend, tipagem/lint/formatação e builds aprovados. Históricos em lotes de até 500 por apuração, sem consultas por unidade/dia; prévia READ_COMMITTED. Inativas com fim cobrável ambíguo conservam diagnóstico histórico. Preparador oficial SomenteBuild gerou JAR idêntico ao testado, SHA `28f7405a9277c01fe777aca5ca052676fe5d4a6ef49a78c7eedeb8dc31e64904`, fonte `b0c7c7ed0e8cd930652a7f3e73dd988c35fe44fe29bed3646a9c0ee97b76275c`. [Recibo](orchestracao/.runtime/faturamento-etiqueta-20261020/validacao.json). Fotos fictícias antes/depois usam os mesmos dados; tela e impressão 150 mm conferidas, sem impressora física. Graphify AST recusou redução de nós e preservou o grafo histórico.

SQL real exclusivamente WMS_DEV/WMSDEV, TLS Mandatory/TSCfalse e guardas atuais 26/26. Candidato próprio: login nativo e cinco GETs 200 (previsões Todos e 83/46, resumo, unidade e etiqueta). Subtotais conhecidos R$ 1.143,20/236 pendências e R$ 16,25/9 pendências; somas Decimal conferidas, sem transformar parcial em total. AuthApp real confirmou o card Todos. O auxiliar interrompeu a seleção do contexto por catálogo vazio; causa não cravada, não atribuída ao produto. Não houve snapshot financeiro depois, portanto ausência de escrita por comparação before/after não foi comprovada. Nenhum comando de negócio, H2, DDL, migration, grant ou PROD executado; acesso nativo criou sua sessão/auditoria normais. [Conferência independente](orchestracao/.runtime/faturamento-etiqueta-20261020/financial-independent-check.json).

Backend/Node próprios encerrados; DEV habitual 25580/25581 preservado. Fonte, pacote e versão ativa permanecem distintos; não houve reinício/publicação. Próximo uso habitual deve consumir a fonte atual pelo iniciar-dev.bat documentado, sem encerrar dono não comprovado. Pendem conferência integrada da troca de contexto sem interferência do auxiliar, comparação financeira depois e equipamentos físicos; configurações/históricos incompletos da base devem ser resolvidos pelo fluxo próprio, sem inventar preços ou apagar legado. Contingência e demais alterações preservadas.

## FE12-CONTING-AUTO01 — dicas e identificação automática da ocorrência, 20/10/2026

Pedido expresso de Lucas: corrigir dicas atrás do modal, gerar a identidade do fato e retirar Fonte do cadastro. Dependências: FE12-CONTING-FORM01/FE02. A tela gera uma identidade nova por rascunho, inclusive com ocorrência anterior no contexto; conserva essa identidade ao editar, voltar, receber erro e repetir a solicitação. Fonte sai do formulário e recebe descrição factual do canal quando ausente. Contrato/API, hash, dependências, permissões e conciliação explícita preservados. Dicas passam a pertencer ao diálogo do gatilho; Escape fecha somente a dica. Ajuda central revisada.

Concluído na fonte local: 25 testes focais em quatro arquivos, tipagem, lint/formatação e build próprio aprovados. Chrome fictício: oito layouts, 48 interações de dicas e quatro envios em memória; sem overflow, erro ou acesso API/externo. Identidade/reenvio/conflito/novo registro e valores exatos conferidos. [Antes](orchestracao/.runtime/contingency-fixes-20261020/antes.png), [depois](orchestracao/.runtime/contingency-fixes-20261020/depois.png), [dica visível](orchestracao/.runtime/contingency-fixes-20261020/depois-dica.png), [recibo](orchestracao/.runtime/contingency-fixes-20261020/validacao.json) e [definição](docs/design-system/etapa-06.md#contingência-dicas-e-identificação--fe12-conting-auto01). Fonte local e pacote próprio distintos da versão ativa, não medida; sem SQL/backend, reinício operacional ou publicação. Graphify consultado; atualização tentou reduzir 15.220 para 14.591 nós e foi recusada pela guarda, preservando o grafo anterior. Próximo deste recorte: conferir a fonte carregada na versão em execução pelo fluxo DEV documentado. FE13-MANUAL01 e alterações anteriores preservados.

## FE09-RESERVAS-UX03 — nota de origem próxima da quantidade, 16/10/2026

Lucas aprovou a melhora do cartão e apontou a nota ainda solta no centro. A primeira coluna agora acompanha o conteúdo, limitada a 240 px; a nota fica a 24 px da quantidade, com unidade na linha seguinte e celular empilhado. Ajuste somente CSS; ajuda `information.dispatchReservations` revisada e continua adequada. Sete testes existentes em dois arquivos, formatação/build e oito layouts Chrome fictícios aprovados; quantidade extensa, reserva cancelada selecionada, teclado/toque e ausência de overflow conferidos. [Prévia](orchestracao/.runtime/dispatch-reservation-origin-20261016/reserva-cancelada-agrupada-dark-1280.png), [recibo](orchestracao/.runtime/dispatch-reservation-origin-20261016/validacao.json). Preview próprio encerrado; fonte local, versão ativa não aferida, sem SQL/backend, reinício ou publicação. Alterações anteriores preservadas.

## FE02-ALERTAS-UX01 — erros das operações mais discretos, 16/10/2026

Lucas apontou o alerta vermelho de saldo insuficiente. `OperationFeedback` passa a usar fundo suave, faixa lateral, ícone e mensagem em texto neutro. Código/correlação ficam integrais em Detalhes técnicos, inicialmente recolhidos; ajuda em `information.operationErrorDetails`. Resultado desconhecido usa âmbar e mantém orientação/bloqueios fora dos detalhes. Foco automático/leitura acessível preservados; halo após mouse retirado sem remover foco visível do teclado. Sessão expirada orienta entrar novamente, sem referência obsoleta ao provedor pendente. [Definição](docs/design-system/etapa-06.md#alertas-das-operações--fe02-alertas-ux01).

25 testes em seis arquivos, três novos, tipagem/lint/formatação/build e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px aprovados; quatro casos extras de resultado desconhecido, sessão, validação e texto extenso. Detalhes por Enter/Space, IDs completos, foco, formulário conservado e ausência de reenvio conferidos. [Galeria](orchestracao/.runtime/operation-alerts-20261016/galeria.html), [recibo](orchestracao/.runtime/operation-alerts-20261016/validacao.json). Fonte local; preview próprio encerrado, sem SQL/backend, reinício ou publicação; versão ativa não aferida. Alterações anteriores preservadas.

## FE09-PEDIDO-UX01 — formulário do pedido de saída organizado, 16/10/2026

Lucas apontou o excesso de molduras/rótulos na criação do pedido de saída. `DispatchOrderFields` organiza cliente/armazém, itens e justificativa; cada item tem uma moldura, produto/quantidade com rótulos curtos e remoção no cabeçalho. Inclusão fica no cabeçalho da lista, orientação decimal aparece uma vez e justificativa ocupa toda a largura. Celular empilha campos; cabeçalho/confirmação permanecem acessíveis na rolagem. Ajuda central em `information.dispatchOrder`/`dispatchOrderQuantity`. [Definição](docs/design-system/etapa-06.md#formulário-do-pedido-de-saída--fe09-pedido-ux01).

12 testes em quatro arquivos, três novos, tipagem/lint/formatação/build e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px aprovados. Inclusão/remoção, retorno da conferência, contexto fixo, IDs/decimais exatos, referência automática, lista vazia, bloqueio no envio e recuperação de conflito conferidos. [Galeria](orchestracao/.runtime/dispatch-order-form-20261016/galeria.html), [recibo](orchestracao/.runtime/dispatch-order-form-20261016/validacao.json). Fonte local; preview próprio encerrado, sem SQL/backend, reinício ou publicação; versão ativa não aferida. Alterações anteriores preservadas.

## FE02-TABELA-TEXTO01 — reticências e popup do texto completo, 16/10/2026

Lucas pediu evitar linhas enormes, pela imagem do Relato da avaria. Células das tabelas compartilhadas de `Result` ficam em uma linha, com reticências quando necessário. Texto cortado abre popup por clique/Enter/Space; texto curto continua simples. Popup mantém texto integral, quebras de linha e seleção para cópia, com rolagem e fechamento acessível; troca do conteúdo consultado encerra a leitura anterior. Ajuda em `information.tableCellText`. [Definição](docs/design-system/etapa-06.md#texto-longo-nas-células--fe02-tabela-texto01).

27 testes em cinco arquivos, três novos, tipagem, lint/formatação/build e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px aprovados. Linhas compactas, texto/HTML literal, foco, Tab/Shift+Tab, Escape sem fechar o diálogo de origem, ausência de overflow, área de toque e seleção original conferidos. [Galeria](orchestracao/.runtime/table-text-popup-20261016/galeria.html), [recibo](orchestracao/.runtime/table-text-popup-20261016/validacao.json). Fonte local; preview próprio encerrado, sem SQL/backend, reinício ou publicação; versão ativa não aferida. Alterações anteriores preservadas.

## FE09-RESERVAS-UX02 — cartão da reserva com informações separadas, 16/10/2026

Lucas pediu melhorar o cartão com quantidade, nota, UUID e ação espremidos na mesma linha. Quantidade/nota ficam acima; unidade completa tem linha própria em fonte monoespaçada. Seleção atual aparece no cabeçalho e o botão verde ocupa rodapé separado por divisor. No celular, campos e ação empilham; ajuda revisada em `information.dispatchReservations`. [Definição](docs/design-system/etapa-06.md#cartão-da-reserva--fe09-reservas-ux02).

21 testes existentes em cinco arquivos, tipagem, lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Unidade em largura inteira, ação abaixo dos campos, ausência de overflow, teclado/foco, objetos/IDs/quantidades exatos, alcance por pedido e bloqueio preservados; diálogo desktop/celular também conferido. [Prévia](orchestracao/.runtime/dispatch-reservation-card-20261016/reservas-dark-1440.png), [recibo](orchestracao/.runtime/dispatch-reservation-card-20261016/validacao.json). Fonte local; preview próprio encerrado, sem SQL/backend, reinício ou publicação. Versão ativa não aferida; alterações anteriores preservadas.

## FE02-CONTINUAR-ESP01 — espaço antes da continuação, 16/10/2026

Lucas apontou Confirmar reserva da sugestão do servidor encostado no resultado. A continuação direta de `OperationResults` recebe margem superior de 16 px pelo token existente; etiqueta e bloco próprio da aprovação conservam seus espaçamentos. Texto, cor, callback e regras de disponibilidade preservados. 11 testes existentes em três arquivos, tipagem, formatação/lint e quatro layouts Chrome fictícios claro/escuro em 390/1440 px aprovados, com gap medido e continuação por Enter. [Definição](docs/design-system/etapa-06.md#espaço-antes-da-continuação--fe02-continuar-esp01), [prévia](orchestracao/.runtime/operation-continue-gap-20261016/reserva-dark-1440.png), [conferência visual](orchestracao/.runtime/operation-continue-gap-20261016/visual.json). Fonte local; preview próprio encerrado, sem SQL/backend, reinício ou publicação; alterações anteriores preservadas.

## FE05-NOTAS-DET01 — notas e documento original organizados, 16/10/2026

Lucas pediu organizar Notas e itens com o documento original aberto. A nota separa número/série, origem e datas; itens de conferência aparecem antes do XML, com seleção verde curta e identificadores recolhidos. Documento em bloco próprio, ações de abrir/ocultar/baixar juntas, dados fiscais e participantes separados, aviso local âmbar e informações adicionais recolhidas na consulta. A prévia de importação mantém os adicionais abertos. Cabeçalho/fechamento acessíveis na rolagem; ajuda central em `information.ts`. [Definição](docs/design-system/etapa-07.md#notas-e-documento-original--fe05-notas-det01).

Na prévia, o documento podia aparecer ainda com Consultando e download bloqueado; o sucesso agora encerra o carregamento antes de guardar os dados, sem depender do `finally` após o cleanup do efeito. 39 testes em seis arquivos, três novos, tipagem, lint, formatação e build próprio aprovados. Seis layouts Chrome fictícios claro/escuro em 390/768/1440 px, download XML exato, teclado, IDs/objetos originais, reabertura sem GET duplicado e bloqueio conferidos. [Galeria](orchestracao/.runtime/receiving-note-detail-20261016/galeria.html), [recibo](orchestracao/.runtime/receiving-note-detail-20261016/validacao.json).

Fonte local pronta; preview próprio encerrado. Sem SQL/backend, alteração de notas reais, reinício ou publicação; versão ativa não aferida. Alterações anteriores preservadas.

## FE05-CONFERENCIA-BOTAO01 — ação de conferência na tabela, 16/10/2026

Lucas pediu harmonizar o botão azul Continuar conferência com os demais elementos da lista. `ReceivingRowActions` usa o fundo azul suave e a borda das ações da tabela, com ícone de conferência, altura alinhada e texto inteiro. Mesmo padrão em Iniciar conferência; área de toque de 44 px no celular. [Definição](docs/design-system/etapa-07.md#ação-de-conferência-na-tabela--fe05-conferencia-botao01).

Conferidos 13 testes existentes de recebimento, tipagem, lint do componente, formatação, build próprio e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px. Texto inteiro, coluna sem sobreposição, teclado/callbacks com IDs exatos e bloqueio durante atualização aprovados. Fonte local alterada; integração e versão em execução não aferidas, sem SQL/backend, reinício ou publicação. Preview próprio encerrado; alterações anteriores preservadas. [Prévia](orchestracao/.runtime/receiving-conference-button-20261016/galeria.html), [recibo](orchestracao/.runtime/receiving-conference-button-20261016/validacao.json).

## FE03-USUARIO-UX01 — vínculos do usuário legíveis, 16/10/2026

Lucas pediu corrigir Novo usuário. A grade de duas colunas do formulário atingia também os fieldsets internos, colocando busca e lista lado a lado; labels e linhas com nomes longos ficavam cortados. Regra limitada ao fieldset direto e `ScopeChoices` organizado em busca acima da lista, opção com checkbox/nome alinhados, altura conforme o conteúdo, seleção destacada e contagem. Clientes/armazéns empilham em telas menores, segurança mantém largura útil e ações acessíveis com Cancelar vermelho. Ajuda revisada em `information.userOperationalAccess`. [Definição](docs/43-login-e-administracao-de-usuarios.md#vínculos-do-usuário-legíveis--fe03-usuario-ux01).

Preservados IDs Long, busca por nome/código, vínculos existentes fora do catálogo, perfil/administração distintos, senha temporária, revisão, proteção da conta principal e controles de envio/recusa. 83 testes existentes em seis arquivos, tipagem/lint/formatação/build aprovados. Dez layouts Chrome fictícios claro/escuro em 320/390/768/1024/1440 px, incluindo nomes extensos sem corte, seleção por Space, busca sem resultado, criação/recusa/tentativa explícita, edição com vínculo indisponível, redefinição, foco e catálogo carregando/vazio/erro conferidos. [Galeria](orchestracao/.runtime/user-scope-layout-20261016/galeria.html), [recibo](orchestracao/.runtime/user-scope-layout-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Catálogo/administração fictícios em memória, sem SQL/backend, conta real alterada, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas.

## FE11-APROVACAO-UX01 — resultado da aprovação organizado, 16/10/2026

Lucas pediu organizar o resultado de Aprovar ciclo integral, com ajustes espremidos em tabela horizontal. `BillingApprovalResult` apresenta ciclo/contexto, versão/saldo/motivo e ajustes em grupos próprios; histórico e identificadores completos ficam recolhidos. `BillingAdjustments`, usado por `Result` no DTO de ajuste e sua lista, mostra origem/destino, cálculos, valores, motivo/evidência e aplicação em cartões; hash integral sob demanda. Seleções identificadas como ciclo/versão/ajuste e entrega manual em Próximo passo. Ajuda em `information.billingApprovalResult`/`billingAdjustments`. [Definição](docs/design-system/etapa-06.md#resultado-da-aprovação--fe11-aprovacao-ux01).

Preservados DTOs, objetos/tipos de seleção, comando, precisão, zero/crédito/ausência, hashes, textos e instantes originais. Resultado indexa suas referências já recebidas no mesmo escopo, sem novo GET; histórico mantém entregas, confirmações, NFS-e e resolução financeira com renderização/seleção existentes. Continuação conserva a entrega manual, bloqueia durante atualização e continua ausente em repetição histórica.

27 testes direcionados em oito arquivos, três novos; suíte completa de 770 testes em 94 arquivos aprovada. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados, sem tabela horizontal nos ajustes, hash inteiro por teclado, seleção original, envio único, próximo passo, histórico/série zero, vazio/ausência e atualização/repetição conferidos. [Galeria](orchestracao/.runtime/billing-approval-20261016/galeria.html), [recibo](orchestracao/.runtime/billing-approval-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, aprovação real, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; integração real permanece separada.

## FE11-REABRIR-UX01 — formulário da reabertura organizado, 16/10/2026

Lucas pediu organizar Reabrir ciclo. `ReopenCycleFields` separa versão/cálculo, justificativa e resolução financeira; rótulos Versão atual do ciclo/Cálculo de referência e motivo em largura inteira com altura inicial de 104 px. Ciclo selecionado identificado pelo período, EM_REVISAO apresentado como Em revisão, ícone de fechamento e cabeçalho/confirmação acessíveis na rolagem. Resolução usa `HistoricalResolutionField`, extraído do cálculo e compartilhado pelos dois formulários: rótulos curtos, compromissos consultados e remoção vermelha compacta em linha própria. Ajuda em `information.reopenCycle`. [Definição](docs/design-system/etapa-06.md#formulário-da-reabertura--fe11-reabrir-ux01).

Comando, revisão, número atual, cálculo, IDs, motivo, resolução, confirmação explícita e repetição preservados; backend continua gerando a nova versão. 46 testes direcionados em oito arquivos, dois novos para retorno/remoção/reinclusão/envio exato e bloqueio/repetição após resposta desconhecida; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px sem overflow, toque/teclado, conflito, restrição ao Gestor e regressão visual de Calcular serviços conferidos. [Galeria](orchestracao/.runtime/reopen-cycle-20261016/galeria.html), [recibo](orchestracao/.runtime/reopen-cycle-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reabertura real, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; integração real permanece separada.

## FE11-CALC-UX01 — formulário do cálculo organizado, 16/10/2026

Lucas pediu organizar Calcular serviços. `CalculationFields` separa contexto, período, justificativa e resolução financeira. Rótulos da resolução sem caminho repetido, tipo/compromisso alinhados, opção do Gestor identificada e remoção vermelha em linha própria, com altura normal. Justificativa ganha espaço; orientação do fim exclusivo em `information.serviceCalculation`. Referências compactas e cabeçalho/confirmação acessíveis na rolagem; celular em uma coluna. [Definição](docs/design-system/etapa-06.md#formulário-do-cálculo--fe11-calc-ux01).

Metadados, campos/seletores oficiais, compromisso, datas, motivo, confirmação explícita e bloqueios preservados. Supervisor não herda resolução do Gestor. 44 testes direcionados em seis arquivos, dois novos para comando/retorno/remoção/reinclusão e perfil/conflito, tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem overflow, remoção de altura normal, toque/teclado, envio único e dados exatos conferidos. [Galeria](orchestracao/.runtime/calculation-form-20261016/galeria.html), [recibo](orchestracao/.runtime/calculation-form-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, cálculo real, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; integração real permanece separada.

## FE11-FATO-REC01 — consulta e recuperação da anulação organizadas, 16/10/2026

Lucas pediu organizar Anular fato identificado. `ServiceFactRecovery` separa o aviso de resultado desconhecido da situação consultada do fato, mantém consulta turquesa e saída vermelha com texto curto e confirmação explícita. Consulta anterior é identificada durante atualização ou após erro; rodapé duplicado fica oculto enquanto bloqueado. `ServiceFactDetail`, usado por `Result` no objeto `FatoServicoDto.Fato`, organiza serviço/contexto, execução, vínculos, quantidade/valor e rateio. Cotas aparecem por nota sem ação genérica sobre cada cota; identificadores/revisão/instante original ficam recolhidos. Ajuda em `information.serviceFactRecovery`. [Definição](docs/design-system/etapa-06.md#recuperação-da-anulação-do-fato--fe11-fato-rec01).

Preservados comando, motivo, revisão, identificador da operação, bloqueios e repetição explícita do conteúdo original. GET não declara sucesso nem resolve a incerteza, inclusive quando retorna ANULADO. Seleção do fato conserva objeto/tipo e bloqueio durante recarga; quantidade/cota/valor mantêm precisão, zero e ausência distintos. Referências continuam usando o catálogo do escopo, sem nova leitura automática.

52 testes direcionados em sete arquivos, quatro novos; suíte completa de 763 testes em 91 arquivos aprovada. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px conferidos, incluindo consulta/erro, repetição original, saída confirmada por diálogo nativo, nota/série zero e toque/teclado. [Recibo](orchestracao/.runtime/service-fact-recovery-20261016/validacao.json), [galeria](orchestracao/.runtime/service-fact-recovery-20261016/galeria.html).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; integração real permanece separada.

## FE11-FATO-CONF01 — confirmação do fato legível, 16/10/2026

Lucas pediu organizar a confirmação de Registrar fato. `ServiceFactConfirmation` separa contexto, serviço/execução, vínculos, quantidade/valor, critério/cotas, justificativa e resolução histórica. Critério e motivo ocupam largura inteira; ausência e zero são distintos, instante original/identificadores ficam recolhidos. Cabeçalho/ações acessíveis, foco no retorno e layout de uma coluna no celular. Ajuda em `information.serviceFactConfirmation`. [Definição](docs/design-system/etapa-06.md#confirmação-do-fato-de-serviço--fe11-fato-conf01).

Corrigida a perda do nome do serviço carregado pelo seletor ao passar à confirmação. O leitor de referências guarda apenas o registro selecionado já consultado no escopo montado. Troca de escopo invalida também as opções de um seletor ainda aberto antes de consultar novamente; não reaproveita nomes/opções anteriores. Identificação não substitui o catálogo autorizador nem altera o ID enviado.

Conferência direcionada: 33 testes em sete arquivos, quatro novos para nome/retorno/envio exato, escopo com seletor desmontado ou aberto e zero/ausências/textos/instante. Duas reproduções iniciais perderam o nome; o caso de escopo aberto também reproduziu a retenção anterior à guarda. Suíte completa: 759 testes em 90 arquivos aprovados. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, diálogo embutido/próprio, justificativa completa fora das ações, troca do serviço, confirmação única, conflito e cotas/resolução conferidos. Resultados no [recibo](orchestracao/.runtime/service-fact-confirmation-20261016/validacao.json); [galeria](orchestracao/.runtime/service-fact-confirmation-20261016/galeria.html).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; integração real permanece separada.

## FE11-FATO-UX01 — registro do fato organizado, 16/10/2026

Lucas pediu organizar Registrar fato. `ServiceFactFields` separa cliente/armazém, serviço/execução, vínculos, quantidade/valor, rateio e justificativa/resolução. Serviço, referência, critério e motivo recebem largura útil; orientação decimal única, rótulos claros e seletores oficiais preservados. Referências compactas, ícone de serviços, cabeçalho e confirmação acessíveis na rolagem. Ajuda em `information.serviceFact`/`serviceFactValues`. [Definição](docs/design-system/etapa-06.md#registro-do-fato-de-serviço--fe11-fato-ux01).

Na conferência foi corrigido um preenchimento anterior: `billingReferences` copiava `chaveFato` para `referenciaExecucao`. Chaves como `ENTRADA:UNIDADE:1359` eram recusadas pelo formato do DTO; entrada/saída também exigem referência manual nula no serviço. Sugestões agora inicializam esse campo como nulo, conservando chave, serviço, instante e vínculos na sugestão. Registro manual conserva a referência informada. Backend/contrato não foram alterados.

34 testes em dez arquivos aprovados, três novos para entrada/saída com chave real e referência manual. Reprodução anterior falhou em dois casos; correção aprovada. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Comando exato após confirmação, retorno, valores grandes/precisão, cotas, envio, conflito e perfis conferidos. [Galeria](orchestracao/.runtime/service-fact-20261016/galeria.html), [recibo](orchestracao/.runtime/service-fact-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas; validação integrada permanece separada.

## FE11-SUGESTOES-UX01 — consulta de sugestões organizada, 16/10/2026

Lucas pediu organizar Consultar sugestões de serviço. Referências compactas, cliente/armazém identificados, serviço e Consultar alinhados e paginação recolhida em Opções de paginação. A situação de um fato anterior não aparece como se fosse resultado desta consulta; detalhe original continua acessível. Ícone de serviços, cabeçalho acessível na rolagem e orientação em `information.serviceSuggestions`. Formulário mantém campos/metadados e GET existentes. [Definição](docs/design-system/etapa-06.md#consulta-de-sugestões-de-serviço--fe11-sugestoes-ux01).

19 testes existentes em seis arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Paginação por teclado, IDs Long/página/tamanho exatos, carregamento, erro/recuperação, seleção preservada, vazio e perfil Operação conferidos. [Galeria](orchestracao/.runtime/service-suggestions-20261016/galeria.html), [recibo](orchestracao/.runtime/service-suggestions-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas.

## FE09-RESERVAS-UX01 — reservas do pedido em cartões, 16/10/2026

Lucas pediu organizar o bloco de reservas. `DispatchReservations` separa pedido, situação, quantidade reservada, nota de origem e identificador completo da unidade em cartões responsivos. Botão verde com texto curto e nome acessível que mantém o UUID; escolha atual indicada por texto, borda e `aria-pressed`. Orientação centralizada em `information.dispatchReservations`. Filtro de propriedade, objeto/tipo selecionado, quantidade exata e bloqueio durante operação preservados. [Definição](docs/design-system/etapa-06.md#reservas-do-pedido--fe09-reservas-ux01).

21 testes existentes de saída/seleção/ajuda em cinco arquivos, tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios em 390/768/1024/1440 px claro/escuro, seleção por Enter, uma/várias reservas, UUID longo, quantidade grande/zero, nota/série zero, referências ausentes, filtro por pedido, bloqueio e uso no diálogo conferidos. [Galeria](orchestracao/.runtime/dispatch-reservations-20261016/galeria.html), [recibo](orchestracao/.runtime/dispatch-reservations-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. FE13-MANUAL01 e alterações anteriores preservadas.

## FE02-DETALHE-LINHA01 — detalhe da tabela em largura útil, 16/10/2026

Lucas pediu corrigir o detalhe de endereço/limites espremido dentro da coluna de ações, aumentando a altura da linha inteira. `ResultTable`, interno a `Result`, mantém a linha compacta e abre os dados em uma linha própria com todas as colunas. Um detalhe por tabela, botão roxo com estado acessível, fechamento com foco no botão de origem e painel adaptado à largura visível mesmo após rolagem horizontal. Seleção conserva o objeto original e fica bloqueada durante atualização, inclusive nos detalhes. [Definição](docs/design-system/etapa-06.md#detalhe-abaixo-da-linha--fe02-detalhe-linha01).

Conferência direcionada: 23 testes em oito arquivos, cinco novos para expansão, troca/fechamento/foco, atualização dos dados, bloqueio durante recarga e notas/série zero. Dois testes de indicadores passaram a abrir o painel antes de conferir unidades e saldos. Suíte completa: 752 testes em 88 arquivos aprovados. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Resultados detalhados no [recibo](orchestracao/.runtime/result-row-detail-20261016/validacao.json); [galeria](orchestracao/.runtime/result-row-detail-20261016/galeria.html).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE04-VIGENCIA-UX01 — encerramento da vigência organizado, 16/10/2026

Lucas pediu organizar Encerrar vigência identificada. `EndValidityFields` separa data de corte, resolução financeira, identificação do compromisso e justificativa. Rótulos curtos, campos alinhados e motivo em largura inteira. `ResolutionReferences` apresenta os compromissos consultados em cartões com período/identificação e ação roxa; escolha atual sinalizada, referências numerosas com rolagem própria e ausência explícita. Ajuda centralizada em `information.ts`; cabeçalho/confirmação acessíveis na rolagem. [Definição](docs/design-system/etapa-06.md#encerramento-da-vigência--fe04-vigencia-ux01).

21 testes em cinco arquivos, três novos para comando exato/retorno, bloqueio/conflito e perfil/ausências; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, seleção por teclado, 12 referências/busca, justificativa longa e vazio conferidos. Tipo, ID/revisão Long, corte, compromisso e motivo preservados na confirmação explícita. [Galeria](orchestracao/.runtime/end-validity-20261016/galeria.html), [recibo](orchestracao/.runtime/end-validity-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, encerramento real, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE02-TABELA-ACOES01 — padrão de ações aplicado às tabelas gerais, 16/10/2026

Lucas pediu verificar a tabela com Ver registro/Selecionar registro. A fonte ainda aplicava a organização somente às notas de mercadoria. `Result` agora usa a classe compartilhada em todas as linhas: detalhes roxos, seleção verde, larguras iguais e texto em uma linha; cabeçalho Ações. Rótulos, abertura nativa, callbacks/objetos e tratamento específico das notas preservados. [Definição](docs/design-system/etapa-06.md#ações-das-tabelas-gerais--fe02-tabela-acoes01).

16 testes existentes em seis arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Toque/teclado, seleção exata, consulta sem callback e regressão das notas/série zero conferidos. [Galeria](orchestracao/.runtime/result-table-actions-20261016/galeria.html), [recibo](orchestracao/.runtime/result-table-actions-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE05-XML-UX01 — arquivo e ações da importação organizados, 16/10/2026

Lucas pediu organizar Importar XML da NF-e e dar cor aos botões. `ReceivingCreateDialog` reúne o arquivo em cartão com nome único/completo, seleção/troca roxa, remoção vermelha e leitura azul; atualização turquesa e demais ações do mesmo diálogo seguem o padrão existente. Erro tem aviso próprio com mensagem original preservada. Orientação curta em `information.ts`, cabeçalho acessível na rolagem e ações empilhadas no celular. [Definição](docs/design-system/etapa-06.md#importação-do-xml--fe05-xml-ux01).

Conferidos 22 testes em três arquivos, dois novos para seleção/remoção/reseleção e erro/recuperação; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, seletor nativo por clique/Enter, nome longo, bloqueios e ausência de gravação automática. Conteúdo do XML preservado, inclusive BOM, acentos e quebras de linha. [Galeria](orchestracao/.runtime/receiving-xml-ux-20261016/galeria.html), [recibo](orchestracao/.runtime/receiving-xml-ux-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem validação fiscal real, SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE06-ETIQUETA-UX01 — consulta e reimpressão organizadas, 16/10/2026

Lucas pediu corrigir o diálogo Consultar / reimprimir etiqueta. Consulta alinhada, referências/correlação recolhidas, prévia centralizada e painel próprio de impressão com botão roxo, ajuda e próximo passo. Dados completos continuam acessíveis em área recolhida; telas menores empilham a composição e o cabeçalho permanece acessível na rolagem. [Definição](docs/design-system/etapa-06.md#consulta-e-reimpressão-da-etiqueta--fe06-etiqueta-ux01).

Conferidos 38 testes em sete arquivos, quatro casos novos para impressão explícita, navegação, recarga/histórico, falha do QR e recuperação da consulta; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, teclado/toque e conteúdo extenso. Mídia de impressão conferida: somente etiqueta, 150 mm e conteúdo preservado; sem teste de impressora física. [Galeria](orchestracao/.runtime/label-preview-20261016/galeria.html), [recibo](orchestracao/.runtime/label-preview-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE12-CONTAGEM-CONF01 — confirmação da contagem organizada, 16/10/2026

Lucas pediu organizar a confirmação de contagem física no coletor. `CountConfirmation` separa cliente/armazém, quantidade observada, instante/UUID e motivo em largura inteira; versão, operação e instante original ficam em Dados técnicos da contagem. Título curto e ícone de contagem, modal mais largo, cabeçalho/ações acessíveis na rolagem e foco inicial no retorno. Ajuda no catálogo `information.ts`; registro continua distinto de aplicação de ajuste. [Definição](docs/design-system/etapa-06.md#confirmação-da-contagem--fe12-contagem-conf01).

Conferidos 27 testes em cinco arquivos, três casos novos de resumo/retorno, comando exato/envio único/conflito e zero/ausências; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem overflow, teclado/toque e justificativa longa conferidos. UUID, versão Long, decimal, instante e motivo mantidos no comando. [Galeria](orchestracao/.runtime/count-confirmation-20261016/galeria.html), [recibo](orchestracao/.runtime/count-confirmation-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE02-REMOVER-MEDIDAS01 — cor da remoção de medidas, 16/10/2026

Lucas pediu cor em Remover Medidas físicas. O botão de remoção de objeto opcional em `Fields` passa a usar `action-remove`, herdando vermelho, temas, hover/foco e estado desabilitado do padrão existente. Texto e callback preservados; o mesmo controle compartilhado mantém cor de remoção nos demais objetos opcionais. Conferidos quatro testes existentes do coletor, tipagem/lint/formatação/build. [Definição](docs/design-system/etapa-06.md#cor-de-remover-medidas--fe02-remover-medidas01), [recibo](orchestracao/.runtime/remove-measures-color-20261016/validacao.json). Sem nova execução de navegador/integração, SQL/backend, reinício ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE04-ENDERECO-EDIT01 — edição de endereço organizada, 16/10/2026

Lucas pediu organizar o formulário de edição com capacidade, dimensões, coleta e tipos de unidade. `AddressEditFields` separa identificação/localização, capacidade/dimensões, coleta/unidades e justificativa. Rótulos curtos com unidades, dimensões alinhadas e orientação decimal única. Ajuda completa da capacidade recolhida; cabeçalho e confirmação acessíveis na rolagem. Metadados, campos fixos, valores e `UnitTypesField` preservados. [Definição](docs/design-system/etapa-06.md#edição-de-endereço--fe04-endereco-edit01).

Conferidos 30 testes em quatro arquivos, quatro casos novos para comando exato, conflito, bloqueio de envio e perfil Operação; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px sem overflow, dimensões alinhadas, IDs HTML únicos, toque/teclado, IDs/revisão Long, decimais e combinação de unidades preservados. [Galeria](orchestracao/.runtime/address-edit-20261016/galeria.html), [recibo](orchestracao/.runtime/address-edit-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE02-CONF-MOTIVO01 — motivo com espaço na conferência, 16/10/2026

Lucas pediu dar mais espaço ao conteúdo do motivo em Confirmar inativação. O motivo apresentado por `Result` na confirmação passa a ocupar a largura inteira, abaixo dos demais dados, com espaço interno, peso regular e parágrafos preservados. Estilo limitado à conferência compartilhada; texto, ajuda, comandos e valores não alterados. [Definição](docs/design-system/etapa-06.md#motivo-na-conferência--fe02-conf-motivo01).

Conferidos 16 testes existentes em três arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem overflow, inclusive texto longo sem espaços. Retorno/confirmação por teclado e conteúdo original preservados. [Galeria](orchestracao/.runtime/confirmation-reason-20261016/galeria.html), [recibo](orchestracao/.runtime/confirmation-reason-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação. Versão ativa não medida; inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE13-AUDIT-DET01 — detalhe da alteração organizado, 16/10/2026

Lucas pediu organizar o detalhe de auditoria com strings JSON extensas lado a lado. `AuditDetail`, usado por `Result` para `AuditoriaResponse`, separa ação, registro/tipo/data/usuário/origem física, justificativa e os painéis Antes/Depois. JSON com indentação e rolagem própria; celular empilha os painéis. Conteúdo original e identificadores ficam acessíveis em áreas recolhidas; cabeçalho/fechamento permanecem visíveis na rolagem. Ajuda em `information.ts`. [Definição](docs/design-system/etapa-06.md#detalhe-da-alteração--fe13-audit-det01).

Conferidos 22 testes em seis arquivos, incluindo seis casos novos de precisão, ausência/zero/falso/nulo, texto inválido/HTML, legado e seleção. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados, sem overflow. Teclado, JSON original exato, fechamento/foco e seleção do objeto original conferidos. [Galeria](orchestracao/.runtime/audit-detail-20261016/galeria.html), [recibo](orchestracao/.runtime/audit-detail-20261016/validacao.json).

Snapshots permanecem strings recebidas; a formatação usa `lossless-json` somente para apresentação, sem recalcular diferenças ou alterar estado/referências. Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE13-AUDIT-GRADE01 — cartões do histórico alinhados, 16/10/2026

Lucas pediu corrigir a organização do bloco Histórico de alterações com cinco registros selecionados. Introdução acima dos cartões, grade responsiva com larguras iguais e botões alinhados por linha; nomes completos têm a largura do cartão. Tipo aparece uma vez, sem repetir Registro selecionado. Ver histórico permanece roxo e fica abaixo da identificação; celular usa botão largo com toque de 44 px. Texto central em `information.ts` esclarece a escolha do registro. [Definição](docs/design-system/etapa-06.md#cartões-do-histórico--fe13-audit-grade01).

Conferidos 16 testes existentes em cinco arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px. Cinco destinos por teclado preservam DTO/ID Long; estado vazio/bloqueio e 17 alvos, incluindo legado separado, conferidos. [Galeria](orchestracao/.runtime/audit-cards-layout-20261016/galeria.html), [recibo](orchestracao/.runtime/audit-cards-layout-20261016/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE05-NOTAS-ACOES01 — botões das notas de mercadoria, 16/10/2026

Lucas pediu ajustar os botões da tabela Notas de mercadoria no detalhe do pedido de entrada. `Result` identifica `PedidoEntradaDto.Nota` e apresenta Ver detalhes em roxo e Selecionar nota em verde, com a mesma largura, espaçamento e texto sem quebra. Cabeçalho Ações; nomes acessíveis incluem número e série da nota. Expansão e seleção conservam o comportamento existente. [Definição](docs/design-system/etapa-06.md#botões-das-notas-de-mercadoria--fe05-notas-acoes01).

Conferidos sete testes existentes em três arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px. Teclado, toque de 44 px no celular, seleção do objeto/tipo original, número com zeros, série zero e consulta somente leitura conferidos. [Galeria](orchestracao/.runtime/note-table-buttons-20261015/galeria.html), [recibo](orchestracao/.runtime/note-table-buttons-20261015/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 e alterações preexistentes preservadas.

## FE12-CONTING-FORM01 — formulário de ocorrência organizado, 15/10/2026

Lucas pediu organizar Registrar ocorrência, na imagem do fato CONTAGEM. `ContingencyRegistrationFields` separa identificação, registro na planilha, dependências, dados do fato e justificativa. Contagem usa rótulos curtos e duas colunas; Motivo da contagem e Justificativa do registro ficam distintos. Dependências têm inclusão/remoção próprias; cabeçalho e confirmação ficam acessíveis na rolagem. Demais tipos reutilizam os metadados/`Fields`, sem o prefixo duplicado. [Definição](docs/design-system/etapa-06.md#formulário-de-ocorrência--fe12-conting-form01).

Conferidos 29 testes em cinco arquivos, incluindo cinco casos novos; tipagem/lint/formatação/build e 16 layouts Chrome fictícios de contagem/chegada claro/escuro em 390/768/1024/1440 px, sem overflow. IDs/versão/decimais/instantes/motivos, dependências, troca de tipo, confirmação, bloqueio/conflito e perfil Operação conferidos. [Galeria](orchestracao/.runtime/contingency-form-20261015/galeria.html), [recibo](orchestracao/.runtime/contingency-form-20261015/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Seletores pertinentes da contingência no teste browser atualizados, sem execução da suíte integrada completa. Inspeção FE13-MANUAL01 e alterações anteriores preservadas.

## FE02-REPETIR-CORES01 — cores das ações após uma operação, 15/10/2026

Lucas pediu cores nos botões mostrados após criar uma carga. `OperationForm` usa âmbar em Repetir exatamente a mesma operação (`action-replay`) e verde em Iniciar nova operação (`action-add`), em todos os fluxos que compartilham esses controles. Tokens existentes para claro/escuro, hover/foco e comportamento preservados. [Definição](docs/design-system/etapa-06.md#cores-das-ações-após-operação--fe02-repetir-cores01).

Conferidos 14 testes existentes em três arquivos, tipagem/lint/formatação/build e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px; repetição conserva payload/chave e nova operação não envia automaticamente. [Prévia](orchestracao/.runtime/operation-button-colors-20261015/galeria.html), [recibo](orchestracao/.runtime/operation-button-colors-20261015/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; registros da inspeção manual e alterações anteriores preservados.

## FE12-CARGA-CONS01 — consulta da carga inicial organizada, 15/10/2026

Pedido de Lucas pela imagem de Consultar detalhe. Título/rótulo específicos, seletor e Consultar alinhados; busca entre várias cargas na mesma linha no desktop e uma coluna no celular. Correlação recolhida em Dados da consulta, com estados de carregamento/erro mantidos. O resultado tipado da carga agora reutiliza `InitialLoadDetail`, inclusive nesta consulta, com seleção somente da carga completa. [Definição](docs/design-system/etapa-06.md#consulta-da-carga-inicial--fe12-carga-cons01).

**Conferido:** 53 testes em sete arquivos, três novos casos da consulta; tipagem/lint/formatação/build aprovados. Doze layouts Chrome fictícios claro/escuro, 390/768/1024/1440 px, duas/doze opções, alinhamento, teclado/toque e ausência de overflow. GET/ID Long, seleção exata, carregamento/resultado anterior, erro/recuperação e perfil Operação conferidos. [Galeria](orchestracao/.runtime/initial-load-query-20261015/galeria.html), [recibo](orchestracao/.runtime/initial-load-query-20261015/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; a instância aberta não foi atualizada por esta conferência. Inspeção FE13-MANUAL01 e alterações anteriores preservadas. DEV pelo operador no fluxo habitual.

## FE12-CARGA-CONF01 — confirmação da carga inicial organizada, 15/10/2026

Pedido de Lucas a partir da imagem de Confirmar: resumo separa carga/revisão, cliente/armazém, leitura, etiquetas e justificativa; hash recolhido em Dados técnicos da revisão. Título específico, menos caixas, comandos acessíveis na rolagem e abertura pelo início do resumo. Na prévia foi encontrado e corrigido o objeto da revisão sobrescrevendo seu número ao abrir preparar/confirmar pela carga consultada. [Definição](docs/design-system/etapa-06.md#confirmação-da-carga-inicial--fe12-carga-conf01).

**Conferido:** 33 testes em seis arquivos, incluindo regressão dos dois comandos, confirmação explícita/envio único, payload exato e preservação após retorno/conflito; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro, 390/768/1024/1440 px, teclado/toque e doze etiquetas longas sem overflow. [Galeria](orchestracao/.runtime/initial-load-confirmation-20261015/galeria.html), [recibo](orchestracao/.runtime/initial-load-confirmation-20261015/validacao.json).

Fonte pronta; preview próprio encerrado. Sem SQL/backend, reinício operacional, publicação ou atualização da instância aberta; versão ativa não medida. Inspeção manual FE13-MANUAL01 permanece separada, com registro preservado. DEV pelo operador no fluxo habitual.

## FE12-CARGA-UX01 — consulta e revisão da carga inicial organizadas, 15/10/2026

Lucas pediu reorganizar Revisar dados levantados e a consulta da carga, após imagens com rótulos extensos e caixas/tabelas aninhadas. `InitialLoadFields` separa identificação, origem, datas/lote, quantidades, unidades e fonte/justificativa; rótulos curtos por unidade, inclusão/remoção próprias e confirmação visível. `InitialLoadDetail` apresenta unidades em cartões sem seleção de configuração interna, etiquetas completas, pendências e identificação da revisão separadas. Título/fechamento permanecem acessíveis na rolagem. [Definição](docs/design-system/etapa-06.md#carga-inicial-consulta-e-revisão--fe12-carga-ux01).

**Conferido:** 23 testes frontend em cinco arquivos, incluindo cinco casos de criação/ausências, edição/lista/payload exato, conflito, consulta/seleção e dados parciais; tipagem/lint/formatação/build aprovados. Chrome fictício em 16 layouts de consulta/revisão, claro/escuro em 390/768/1024/1440 px, sem overflow. [Galeria](orchestracao/.runtime/initial-load-ux-20261015/galeria.html), [recibo](orchestracao/.runtime/initial-load-ux-20261015/validacao.json).

IDs/revisão/decimais/instantes, ausências, confirmações e permissões preservados; ajuda em `information.ts`. Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Prova fictícia não substitui a inspeção manual em andamento nem atualiza a instância aberta; versão ativa não medida. DEV pelo operador no fluxo habitual. Registro FE13-MANUAL01 e alterações preexistentes preservados.

## FE13-MANUAL01 — inspeção manual pelo Dev Tunnel, 15/10/2026

**Situação: relatório por página entregue em16/10; homologação integral ainda não aprovada, com falhas e limites identificados.** Pedido expresso do responsável: ler a documentação, operar a interface botão a botão e registrar os problemas para correção posterior. Dependências: FE03–FE13 e contratos BE04–BE14 pertinentes. URL observada: https://1z8126n0-25581.brs.devtunnels.ms/. Conta principal autenticada como Gestor; nenhuma senha/token é incluída neste registro.

**Base consultada:** AGENTS.md; README; estado corrente; documentos 04 (fluxos), 08 (cenários V01–V48), 11 (AC01–AC16), 43 (login) e 45 (entrada manual/XML), decisões e continuidade; mapeamento frontend de telas/contratos e consulta ao grafo do próprio projeto. Requisitos operacionais serão confrontados com os resultados visíveis, preservando a distinção entre regra explícita e proposta.

**Método e limites:** interação manual pelo navegador com o ambiente fornecido, sem chamadas diretas à API, SQL, mocks, reinício ou publicação pelo auditor. Código preservado durante os testes; exceção posteriorF25: correção pontual de CSS solicitada expressamente pelo responsável em16/10, com empacotamento estático separado. Dados existentes preservados. Validar consultas, filtros, navegação, detalhes, ajuda, formulários e rejeições locais; operações conclusivas dependem de contexto de teste identificável. A URL indica a porta DEV documentada, mas banco/identidade SQL e versão servida não foram atestados nesta rodada; não declarar homologação SQL nem equivalência da fonte local com a instância.

**Cobertura inicial:** aviso do túnel; login com campos vazios (obrigatoriedade exibida); autenticação bem-sucedida; carregamento da página inicial e indicadores. Sem falha confirmada nesses passos.

### FE13-MANUAL01-OBS01 — navegação interrompida ao sair de rascunho

**Classificação:** impedimento da execução manual, origem inconclusiva; não confirmado como defeito do WMS. FE04/FE13. Prioridade para retomada: alta, pois impede continuar o percurso.

1. Entrar com a conta principal e abrir Cadastros → Clientes → Novo cliente.
2. Clicar Conferir e confirmar com campos vazios: mensagem observada `Preencha nome com texto.` e `Campo: nome`; nenhum sucesso apresentado.
3. Preencher somente Nome com `QA MANUAL 15-10 rascunho não salvar` e clicar Voltar à lista.

**Esperado:** a proteção de descarte documentada permite manter ou abandonar o rascunho, devolvendo o controle da tela sem gravar cliente.

**Observado:** o controle do navegador integrado excedeu o tempo de resposta no clique. A aba original deixou de responder à leitura e à captura. A ferramenta não informou diálogo JavaScript ativo. Uma segunda aba recuperou a sessão e carregou a visão geral; entretanto, cliques de menu por nome, índice e área visível não mudaram a tela. Navegação explícita para a rota Cadastros já observada carregou a lista, mas Enter em Armazéns apenas mudou o foco, sem abrir a etapa. Não há prova suficiente para atribuir isso à aplicação, ao aviso de descarte ou ao navegador integrado.

**Integridade:** não foi enviada confirmação de criação/alteração de cadastro. Nenhuma gravação de negócio foi intencionalmente concluída. Não há confirmação de que a aba original tenha sido fechada; tentativa de fechamento também excedeu o tempo da ferramenta. Sem reinício do WMS ou do navegador do usuário.

**Encaminhamento:** verificar/fechar eventual aviso pendente no Codex e retomar pelo descarte de Novo cliente. Reproduzir em navegador que responda normalmente antes de abrir correção de código. Solicitação de informação enviada ao responsável nesta conversa. Demais módulos continuam pendentes; este impedimento não os aprova.

**Continuação em 15/10/2026:** responsável reiterou testar todas as telas/ações, preencher e confirmar operações de teste antes de encerrar. A aba original deixou de constar no navegador; a aba preservada voltou a responder a cliques. OBS01 deixa de impedir o avanço, mas sua causa não foi determinada e o descarte ainda exige reprodução. Uso de registros explicitamente fictícios com prefixo `QA MANUAL 20261015`; confirmar somente operações de teste rastreáveis, preservando dados operacionais existentes. A ausência de validação de dígito fiscal é comportamento documentado no contrato14, não um defeito novo.
### FE13-MANUAL01-F01 — confirmação real de Excel descrita como fictícia (P2, aberto)

**FE04 → BE05; tipo:** texto/identificação da origem da resposta. **Passos:** Cadastros → Importar endereços Excel → baixar modelo; no armazém fictício `ARM-000006`, enviar `qa-manual-enderecos.xlsx` com quatro posições `QA1510-ARM02`, `QA1510-TRI`, `QA1510-QUA`, `QA1510-SEP`; preencher motivo; conferir e consultar a prévia sem erros; Confirmar a prévia consultada sem erros → preencher motivo → Conferir e confirmar → Confirmar agora.

**Esperado:** confirmação identificada como retorno do servidor no ambiente fornecido, separando dado fictício de transporte simulado. **Observado:** estado CONFIRMADA e correlação `b806bcb0-2db7-418b-a669-7ab072f56c53`, mas mensagem estável `Importação confirmada pelo retorno fictício.` no diálogo e na consulta posterior. As quatro posições constaram na consulta independente Endereços, somadas à posição manual (5 registros). Não se trata de lista vazia nem falha de importação; a inconsistência é a classificação do retorno. SQL/versão da instância não atestados por acesso direto.

**Impacto:** operador pode interpretar uma importação persistida como simulação. **Evidência:** [captura da confirmação](orchestracao/.runtime/qa-manual-20261015/F01-importacao-retorno-ficticio.png). **Base:** documentos14/43, inventário cadastros-7 e distinção documentada entre integração real e simulação. **Próximo ajuste:** revisar mensagem conforme transporte em uso; repetir esta jornada após correção. Nenhum código alterado nesta rodada.

### FE13-MANUAL01-F02 — coluna Código de Endereços mostra a descrição (P2, aberto)

**FE04 → BE05; tipo:** apresentação do valor em coluna. **Passos:** com contexto `QA MANUAL 20261015 - Armazém` aplicado, abrir Cadastros → Endereços; conferir as cinco linhas criadas; abrir Ver detalhes da primeira posição.

**Esperado:** coluna Código mostra `QA1510-ARM01`, descrição mostra `QA MANUAL 20261015 - Posição 1`. **Observado:** a célula da coluna Código mostra `QA MANUAL 20261015 - Posição 1`, igual à descrição; os detalhes do mesmo registro mostram o código correto `QA1510-ARM01`. O mesmo padrão apareceu nas outras quatro linhas importadas. Dados existem e a referência foi conferida no detalhe; não é ausência de cadastro nem filtro incorreto.

**Impacto:** código operacional não fica disponível na coluna destinada a ele; descrições semelhantes tornam a escolha ambígua. **Evidência:** [captura da lista](orchestracao/.runtime/qa-manual-20261015/F02-enderecos-coluna-codigo.png). **Base:** inventário cadastros-5, identidade por código do endereço no documento14 e fluxo de leitura no documento04. **Próximo ajuste:** corrigir conteúdo da célula principal preservando a abertura dos detalhes; retestar busca/seleção/código. Nenhum código alterado.
### Continuação manual — dados e verificações observados

- Cliente fictício criado pela interface: `QA MANUAL 20261015 - Cliente`, código `CLI-000008`; produto contado `QA1510-UN` (20 unidades previstas na jornada), embalagem `QA1510-DUN` de 10 UN; armazém `ARM-000006`. Uma posição criada manualmente `QA1510-ARM01` e quatro importadas, total 5, com peso/dimensões/tipos/empilhamento explícitos. Modelo oficial baixado e preenchido; prévia manteve posição textual `02`.
- Entrada `ENT-000011`, NF manual série15/número15102026, emissão15/10/2026, produto88/item100 (IDs observados na interface). Chegada fictícia 15/10/2026 20:00 Brasília, 20 boas/0 avariadas; comando repetido pelo botão próprio e consulta posterior manteve 20, diferença0 e revisão3. Efetivação sem divergência dispensou a escolha de aceite antes do envio e retornou EFETIVADO/revisão4, às20:48 exibidas na tela. Nenhuma nota, chegada ou unitização real foi simulada como homologação fiscal.
- Validações esperadas, não defeitos: documento fiscal alfanumérico permitido pelo contrato14; produto sem dados no cliente recém-criado apresentou vazio; arquivo/motivo obrigatórios na prévia Excel; série de NF não inteira rejeitada. A dificuldade inicial de preencher data por ferramenta foi superada com teclado nativo; emissão foi conferida como 15/10/2026 antes do envio, sem abrir defeito de calendário.
- Ai-memory consultado em escopo `rodogarcia/wms-rodogarcia`: páginas recentes, busca temática e observações de prompts da sessão `01a140b9-23a9-74a2-bbdc-060503541c83`. Resumos heurísticos de sessões recentes contêm principalmente inventário de ferramentas, portanto não comprovam por si o conteúdo entregue. Atualizações efetivas foram confrontadas com os registros canônicos FE04-EXCEL01/AUTO01/AUTO02/INTEGRACAO01, FE05-DATA-PESO01/ACEITE01 e FE06-REFERENCIAS01. Graphify consultado com caminho explícito do repositório; mapa aponta várias fontes históricas e não substitui o estado atual.
### FE13-MANUAL01-F03 — conferência identifica unidade pelo DUN compartilhado (P2, aberto)

**FE06 → BE07; tipo:** identificação visual ambígua, sem prova de ID incorreto no comando. **Passos:** unitizar ENT-000011 em dois PALLET/BOA de10 UN com a mesma embalagem `QA1510-DUN`; em Corrigir organização física limpar filtros opcionais de UUID/endereço/disponibilidade; abrir Dividir unidade para a identidade `fc14c5c0-acba-4f77-8d34-63770730ee9a`; informar5 e motivo; Conferir e confirmar.

**Esperado:** o campo Unidade logística identifica o código único da unidade física selecionada; DUN aparece como embalagem separada. **Observado:** bloco Registro selecionado e bloco Dados para conferência exibem `Unidade logística: QA1510-DUN`. A outra identidade `bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80` usa o mesmo DUN, portanto esse texto não distingue as duas. O cabeçalho da operação e a lista exibem o UUID correto, mitigando o risco; não foi constatada divisão da unidade errada. **Impacto:** identificação contraditória dentro da confirmação e possível confusão entre embalagem e identidade física.

**Base:** documento20, AC10 e atualização FE06-REFERENCIAS01/documento de design etapa16 (DUN e identidade permanente são distintos). **Evidência:** [confirmação da divisão](orchestracao/.runtime/qa-manual-20261015/F03-unidade-identificada-pelo-dun.png). **Próximo ajuste:** usar o código da unidade no resumo/confirmacão de referência, preservando embalagem em campo próprio; retestar duas unidades com DUN igual. Nenhum código alterado.
**Cuidados reiterados pelo responsável durante a rodada:** não confundir operação incorreta, referência/filtro errado, falta real de dados, atualização de nomes ainda carregando ou limitação da ferramenta com defeito do WMS. Toda suspeita exige confronto documental e repetição pertinente. Respostas transitórias `Referência indisponível` em produto/importação resolveram após carregamento e não foram abertas como falha. O inventário original é histórico: Entrada reúne hoje as ações nas seções internas de Pedidos de entrada (etapa06/07), sem exigir quatro páginas globais antigas.

**Unitização/divisão já observadas:** tentativa10+9 frente a20 conferidas recusada com QUANTIDADE_DIVERGENTE, correlação `fe1ca50b-23dc-4d2b-b84a-7134a786deba`, sem marcar progresso concluído; correção10+10 retornou duas unidades, progresso atualizado1/1 e pendentes0. Identidades `fc14c5c0-acba-4f77-8d34-63770730ee9a` e `bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80`, 10 UN cada, sem disponibilidade antes de endereçar. Divisão da primeira gerou `1485dcb6-171f-414b-86c3-099fd10ee89f` com5, preservando FIFO/chegada20:00 e origem. Reagrupamento dessa nova unidade na primeira recebeu VERSAO_DESATUALIZADA (`a8d69ac0-8212-429a-a2a4-a7672bbaac0c`); precisa reconsulta antes de atribuir falha ao sistema. A reconsulta abriu proteção de descarte esperada, cuja resposta não foi acessível pela ferramenta do Codex. Pedido ao responsável para confirmar o aviso; não foi realizada repetição cega do comando de negócio. Captura complementar de F03: [origem/destino com o mesmo DUN](orchestracao/.runtime/qa-manual-20261015/F03-reagrupamento-origem-destino-dun-igual.png).

[Controle das ocorrências do inventário](orchestracao/.runtime/qa-manual-20261015/cobertura-inventario.json): controle atualizado na planilhaJSON vinculada; os números mudam conforme a execução. Cada ocorrência identifica recorte manual, rejeição esperada, bloqueio por defeito ou ausência de execução. Consultar detalhes por ocorrência; isso não mede todos os ramos, perfis ou cenários. Não converter as 171 ocorrências históricas em alegação de cobertura atual.
### Cobertura desta rodada

| Área | Executado e observado | Pendente |
| --- | --- | --- |
| Acesso (FE03/BE04) | Login inicial correto; recuperação de sessão; Minha senha inspecionada/cancelada; ajuda de recuperação/primeiro acesso; Sair mostrou login e recarga permaneceu inicialmente protegida | Novo envio de login confirmado após CSS estabilizado; outros perfis e troca de senha não executados |
| Início | Contexto QA e outro armazém fictício; indicadores conferidos com saldo19UN; atualização; quatro ajudas; filtros/busca/limpeza; cinco detalhes; colunas e paginação ida/volta; atalhos e encaminhamento de destino ao coletor; recorte390px/teclado | F24; verificar estabilidade após interrupção de estilos da instância em16/10; não equivale a todos os dados/perfis |
| Cadastros (FE04/BE05) | Cinco cadastros criados/editados; complementos fiscais; Excel; encerramento/reativação; conjuntos; impedimentos; remanescente B10; inativação CLI9 após resolução financeira | Configurar capacidadeF13; encerramento genérico de vigênciaF17; revisão/justificativa do remanescenteF18/F19 e vínculo com cargaF23 |
| Entrada, unidades, estoque e saída (FE05–FE10) | Manual/XML/nota anexada; conferência/estorno/replay/efetivação; unitização/divisão/reagrupamento/etiqueta; capacidade/movimentação; bloqueio/avaria/reparo; saída/XML/reserva/reversão/leitura/separação/documento/retorno/retirada; revalidação positivaSAI7 e negativaSAI6; cancelamento sem físico/saldo19 | DevoluçãoF08; seletores de duas posiçõesF15; avaria após reservaV17 concluída com reserva preservada, cancelamento e reparo; F26 no código lido; downloadXML inconclusivo pelo controle do navegador; impressão/scanner físicos não aferidos |
| Serviços, fechamento e regularização (FE11/FE12) | Configuração/edição/vigências; fatos; cálculos; ciclos14→15 e15→16 aprovados/finalizadosZERO; demonstrativo/hash; entrega local/replay; não emissão/NFS-e tardias/conflito/tratativa; ajusteVALIDADO→APLICADO único; contagem; carga revisada/preparada/regularizada/cancelada antes de efeitos; contingência conciliada/replay | Ajuste de estoqueF10; anulação de fatoF21; avaria financeiraF22; cancelamento positivo de cargaPREPARADA depende de resolver físico, bloqueadoF23/F10; integrações externas reais não aferidas |
| Relatórios, coletor e usuários | Consultas/indicadores/auditoria; leituras e endereçamentos; contagem; usuários/proteção da principal; rascunho com filtros/perfis/Space; edição/redefinição apenas inspecionadas/canceladas | Criação/alteração de acesso e credenciais não executadas; alcance efetivo de Operação/Supervisor sem sessão própria; falhasF02/F07/F14/F15 pertinentes |

**Defeitos confirmados nesta rodada:** vinte e seis até este ponto (F01–F26): F01–F24 eF26 abertos; F25 de carregamentoCSS corrigido por pedido expresso e conferido no navegador. Detalhes e evidências na continuidade abaixo. **Observações não classificadas como defeito:** limitações do controle do navegador, ausência efetiva de dados, recusa temporal/configuração esperada e downloadXML inconclusivo. O inventário documental informa 50 páginas e 171 ocorrências de ações nas 12 áreas; isso é referência de planejamento, não contagem de ações testadas. Validações passadas simuladas não foram reaproveitadas como resultado manual atual.

**Próximo passo atual,16/10:** relatório das50páginas do inventário +Início/Coletor/Usuários/login registrado na continuidade final e na pasta da rodada. Falta/avaria sem aceite recusadasV27/V28; aceite fictício deENT13 efetivou com histórico e FIFO da primeira chegadaV26. Painel final21UN (19QA1510-UN+2QA1510-XMLsem unitização),10disponíveis/zeroreservas/zeroentradas e saídas abertas. F08/F13/F15/F22 reconfirmados. Corrigir e retestarF08/F10/F13/F15/F17/F21/F22/F23 para concluir seus caminhos positivos; demais variantes/perfis/equipamentos/download e combinações financeiras não homologados estão discriminados no relatório. F25CSS corrigido a pedido expresso; demais falhas sem correção pelo auditor. Relatório entregue não equivale a100%de testes aprovados. Aba4 emInício, desktop, sem operação em envio. Sem API/SQL/testes automatizados de negócio pelo auditor.

**Retomada após solicitação “volte”:** login renovado, contexto QA reaplicado, consulta mostrou destino `fc14c5c0-acba-4f77-8d34-63770730ee9a` com5, segunda unidade `bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80` com10 e origem dividida `1485dcb6-171f-414b-86c3-099fd10ee89f` com5. Pelo detalhe atual do destino, origem marcada explicitamente e motivo preenchido, reagrupamento confirmado (correlação `bc88c714-76c9-48d6-990b-4cbacb11dfc6`). Retorno mostrou destino10/ativo e origem0/inativa, FIFO preservado15/10/2026 20:00; total ativo20. A rejeição anterior VERSAO_DESATUALIZADA não é classificada como falha, pois a reconsulta permitiu concluir. Consulta posterior da etiqueta: mesmo UUID, versão de conteúdo2, 10 por embalagem, notas20, numeração1/2, barras/QR com mesmo UUID; correlação `093e1e50-fc20-457d-a79c-3a033da67e80`. [Captura da conclusão](orchestracao/.runtime/qa-manual-20261015/reagrupamento-confirmado.png). F03 permanece restrita à ambiguidade visual do DUN. Impressão física ainda não aferida.
**Coletor/estoque/avaria — continuação manual:** etiqueta encaminhou corretamente o UUID ao coletor. Tentativa de endereçar peso5001kg em posição de5000kg recusada com CAPACIDADE_EXCEDIDA (`718130e1-b528-4f0d-bbbf-02b4cb46a017`), sem gravar destino. Com100kg/1×1×1m/empilhamento1/uma posição e sem conjunto, confirmou `QA1510-ARM01`, revisão3 (`b6d18047-6de0-4300-8934-06533ebce782`). Replay pelo botão próprio manteve revisão3 (`505612fb-d20e-44dc-824f-5a69555202be`). Consulta independente: duas unidades ativas de10, físico20, disponível10, não endereçado10; movimentos mostrou uma única linha ENDERECAMENTO. Bloqueado10 no agregado inclui indisponível não endereçado, conforme contrato24, e não indica bloqueio preventivo das duas unidades. Não registrar como falha de saldo.

Motivo vazio de bloqueio foi corretamente rejeitado; bloqueio preventivo (`25a7ef4f-96f6-4673-9974-076e777d5db9`) preservou10/FIFO/posição e retirou disponibilidade; liberação após reconsulta/revisão4 (`fa8a2d9f-1d1d-4397-a6f6-ea8575deffe3`) devolveu disponibilidade. Avaria estruturada fictícia2UN, ocorrência15/10/2026 21:32 após formação/reagrupamento, destino `QA1510-QUA`, confirmou unidade inteira em quarentena/bloqueada, físico10, base10 (`80c14dad-5f6c-4004-8169-4509445340a4`). Ocorrência consultada e marcada; responsabilidade fictícia RODOGARCIA reconhecida (`0d218cdb-4b36-4704-8739-93faeb2308a4`) retornou proporção0,2 desde21:32. Lista relacionada atualizada e ocorrência marcada antes do reparo; reparo (`e4ab369a-88cb-492f-b9d7-47b9986ae8f7`) retornou REPARADA, armazenagem emARM01, sem bloqueio/avaria posterior, disponívelSim, físico10 e responsabilidade histórica preservada. Revisão da unidade7. [Coletor](orchestracao/.runtime/qa-manual-20261015/coletor-primeiro-enderecamento.png), [reparo](orchestracao/.runtime/qa-manual-20261015/reparo-confirmado.png). F03 também reaparece nas referências Unidade logística dessas telas (DUN compartilhado); nenhuma troca indevida de unidade constatada. Ciclo exibiu Referência indisponível de forma persistente; sem referência independente do ciclo pela interface, não foi promovido a defeito confirmado nesta etapa.
### FE13-MANUAL01-F04 — confirmação de reserva sem identificar o pedido (P2, aberto)

**FE09 → BE09; tipo:** falta de identidade no passo final. **Passos:** criar pedido fictício `SAI-000005` com5UN deQA1510-UN; consultar sugestão FIFO; clicar Confirmar reserva da sugestão do servidor; formulário mostra IdentificadorSAI-000005/revisão0; preencher motivo e clicar Conferir e confirmar.

**Esperado:** conferência final mantém referência do pedido selecionado para validar o alvo antes da reserva integral (documento24 e padrão de conferência/código/revisão de etapa06). **Observado:** título apenas Confirmar reserva integral; contexto identifica cliente/armazém; Dados para conferência mostra somente Justificativa Não informado e Motivo. ReferênciaSAI-000005 e quantidade5 não aparecem na conferência. Não é falta de pedido ou seleção: formulário anterior estava preenchido e o retorno confirmou SAI-000005/RESERVADO com5 da unidade correta. **Impacto:** operador perde o alvo e o volume ao revisar o comando; não foi constatada reserva de pedido errado. [Captura do passo final](orchestracao/.runtime/qa-manual-20261015/F04-confirmacao-reserva-sem-pedido.png). **Próximo ajuste:** mostrar pedido e resumo integral na confirmação, conservando ID/revisão reais; retestar pelo atalho FIFO e pelo registro. Sem código alterado.

**Saída até este ponto:** pedido11UN frente a10 disponíveis recusado SALDO_INSUFICIENTE (`27d11a2e-8b88-482a-8078-31718547e095`); corrigido5UN, criadoSAI-000005/RASCUNHO (`28c08652-203b-402b-b15c-b4dfc249fd9c`), sugestão5 noUUID original (`a4fd71f9-14f4-464a-ac06-f46048714d39`), reserva integral confirmada (`f7aeaa55-77f6-4a9e-baac-dff4056c9eba`), PodeProsseguirSim. Ainda sem separação/retirada, saldo físico não baixado.
### FE13-MANUAL01-F05 — atalho de leitura perde a reserva ativa marcada (P2, aberto)

**FE09 → BE10; tipo:** seleção substituída ao mudar de operação. **Passos:** emSAI-000005, reservar5UN noUUID `fc14c5c0-acba-4f77-8d34-63770730ee9a`; reverter antes da separação (`87f5e6d4-a63f-45c3-a008-1b6be61c6839`); reservar novamente (`349190b0-fdef-47c3-9e60-d3f33dcbdbc4`). Resultado conserva a primeira reserva REVERTIDA e a nova ATIVA. Desmarcar a antiga e marcar a ATIVA nos checkboxes; leitura seguinte confirmou antiga0/ativa1. Clicar Ler a etiqueta da reserva confirmada.

**Esperado:** preservar a reserva ATIVA explicitamente marcada ou pedir seleção sem assumir a histórica. **Observado:** em Conferir leitura da reserva, checkbox da REVERTIDA voltou a1 e o da ATIVA a0; select Reserva voltou à primeira opção. Ambas opções usam o mesmo UUID como nome, pois pertencem ao mesmo pallet. **Integridade:** nenhum comando foi enviado contra a revertida; desmarcada antiga e marcada ativa novamente na tela de leitura, select passou à segunda opção antes de conferir/enviar. Portanto não é uma falha provocada por esquecer de marcar a reserva. [Captura da tela de leitura com seleção restaurada indevidamente](orchestracao/.runtime/qa-manual-20261015/F05-leitura-volta-reserva-revertida.png). **Impacto:** atalho induz leitura de reserva encerrada e obriga seleção repetida; etiquetas iguais no select dificultam notar a troca. Base: contrato24/27, seleção explícita de reserva e continuidade da jornada FE09. **Próximo ajuste:** conservar ID da seleção ao abrir a ação e impedir assumir automaticamente reserva encerrada; retestar histórico de duas reservas do mesmo UUID. Sem alteração de código.
### FE13-MANUAL01-F06 — alternativas NOTAZZ/XML inconsistentes no formulário (P2, aberto)

**FE10 → BE10; tipo:** preenchimento/validação por origem. **Cenário:** SAI-000005 separado, reserva ATIVA marcada (antiga REVERTIDA desmarcada), cobertura5UN noSKUQA1510-UN. OrigemNOTAZZ, naturezaRETORNO_MERCADORIA, Nota informada com emitente sintético `99151515000150`, série15/número15102027, emissão15/10/2026, chave sintética de44dígitos com formato/dígito consistentes `35261099151515000150550150151020271123456783`, protocolo `100000000000015`, motivo explícito de teste. XML deixado vazio. Conferência mostrou esses valores. Servidor recusou DADOS_INVALIDOS: `NOTAZZ exige identidade do documento existente; XML usa origem XML.` Correlação final `1bd49c42-7863-469f-9e75-95356d68c3f0`; ensaios anteriores com/sem chave também recusados. Nenhum documento NOTAZZ criado.

**Contraprova pela interface:** mudar origem paraXML, removerNota, limpar protocolo e informar XML sintético NF-e4.00/modelo55 com a MESMA identidade/protocolo e item5UN. Conferir e confirmar é bloqueado localmente por `Preencha protocolo com texto. Campo: protocolo`, mesmo estando o protocolo dentro do XML e o campo rotulado `Protocolo existente NOTAZZ *`. Preencher esse campo adicional com o mesmo protocolo permite confirmar; documentoXML/AUTORIZADO retornou, com pedido aindaSEPARADO, sem retirada/baixa (`51a625b3-86cd-42e6-b046-9a39bfed8deb`). Isso comprova que o cenário/identidade/cobertura foi aceito pela alternativaXML; não comprova autorização fiscal real nem causa técnica exata da recusaNOTAZZ.

**Esperado:** respeitar alternativas do contrato27: NOTAZZ com nota/protocolo e XML com dados extraídos; erro deve apontar o dado incompatível quando os campos conferidos estão preenchidos. **Impacto:** caminho manualNOTAZZ não concluído com dados completos; caminhoXML exige entrada redundante rotulada para outra origem. [RecusaNOTAZZ](orchestracao/.runtime/qa-manual-20261015/F06-notazz-identidade-preenchida-recusada.png), [exigência indevida noXML](orchestracao/.runtime/qa-manual-20261015/F06-xml-exige-protocolo-notazz.png). **Próximo ajuste:** conferir obrigatoriedade/serialização condicional de nota/XML/protocolo; causa não inspecionada por API/código. Sem código alterado; todos os dados são fictícios e marcados sem validade fiscal.

**Continuidade da saída:** F05 repetiu também no atalho Confirmar destino da reserva lida; seleção antiga reapareceu antes da separação e foi corrigida na própria tela. [Segunda reprodução](orchestracao/.runtime/qa-manual-20261015/F05-separacao-tambem-volta-revertida.png). Leitura ativa confirmou (`064630e1-9f4d-4f1a-a05f-e9a393731210`), separação noQA1510-SEP confirmouSEPARADO (`dd460993-c6e8-474e-a8f2-09f284a0faa9`), fato físico10→10 e reserva5 preservada, conforme parcial de pallet do contrato27. Nenhuma retirada efetuada até esse ponto.
### FE13-MANUAL01-F07 — documento selecionado aparece como referência indisponível (P2, aberto)

**FE10; tipo:** identificação de referência existente. **Passos:** após registrar o único documentoXML deSAI-000005, reabrir o detalhe atual do pedido pela lista; tabelaDocumentos mostra XML/RETORNO_MERCADORIA/protocolo100000000000015/AUTORIZADO. Selecionar registro1 dessa tabela; abrir Registrar cancelamento.

**Esperado:** campoDocumento e confirmação identificam nota/protocolo do registro selecionado. **Observado:** select mostra `Referência atual indisponível` e só essa opção além do placeholder; confirmação mostra `Documento: Referência indisponível`, inclusive após os carregamentos anteriores. **Contraprova:** documento existe no detalhe independente, foi selecionado explicitamente, e a confirmação retornou o mesmo protocolo comoCANCELADO às21:44 (`7d30c9f4-8f5b-4b96-af45-d7eb487c1053`). Não é lista vazia, falta real do documento nem falha de cancelamento. [Captura](orchestracao/.runtime/qa-manual-20261015/F07-documento-existente-referencia-indisponivel.png). **Impacto:** alvo fiscal invisível ao operador no passo decisivo. Próximo ajuste: carregar/identificar documento selecionado no campo e na conferência; preservar identidade real. Sem código alterado.

**Retorno interno observado:** documento cancelado não retirou nem baixou estoque; reserva continuavaATIVA e pallet separado. Retorno integral aoQA1510-ARM01, com reservaATIVA marcada explicitamente, confirmouSAI-000005/CANCELADO, reservaCANCELADA, separaçãoRETORNADA e fatoRETORNO_INTERNO10→10 (`6c1dcdbb-28df-4f7d-aaa5-d88c1a91518f`). Permanecemsem retirada física e sem baixas. Não confundir cancelamento documental com movimento físico.
### FE13-MANUAL01-F08 — devolução bloqueada por campo id fora do contrato (P1, aberto)

**FE10 → BE10; tipo:** validação local impede avançar à confirmação. **Pré-condição comprovada:** pedido `XML-909D8001BB8495C6DD6FB1A61F1FB43A6056` RETIRADO, uma baixa física de5UN e nenhuma devolução prévia. Retirada confirmou às21:49 (`81ea9257-576a-45e4-b48d-b398f20385f5`); replay (`5291a167-573e-48c9-bd42-dc34b608a0cf`) manteve uma baixa e fato10→5. Consulta independente Estoque comUUID eDisponívelSim encontrou5 emARMAZENAGEM, sem bloqueio. [Remanescente](orchestracao/.runtime/qa-manual-20261015/remanescente-5-estoque.png).

**Passos:** Fiscal → Retorno e devolução → Registrar devolução / nova entrada na linha desse pedido; preencher nota sintética emitente99151515000150/série15/número15102029/emissão15/10/2026; chegada15/10/2026 21:50 (posterior à retirada e anterior ao relógio atual21:50:47); escolher a única baixa física do pedido; quantidade5, avariada1, motivo explícitoQA; Conferir e confirmar. **Observado:** `Campo fora do contrato: id`, `Campo: id`. Formulário não oferece campoid editável; não abre confirmação, não envia comando e não apresenta correlação. Antes, quantidade6 também parou na MESMA validação local; corrigir para5 dentro do limite não mudou o resultado. Portanto recusa de quantidade excedente não foi aferida e não se confunde com esse bloqueio.

**Esperado:** campos do formulário de devolução conforme contrato27, pedido na rota/contexto e baixa selecionada, permitindo revisar a devolução válida antes do envio. **Impacto:** devolução pelo comando contextual não pode prosseguir; nenhuma nova entrada criada nesta tentativa. [Captura](orchestracao/.runtime/qa-manual-20261015/F08-devolucao-campo-id-fora-contrato.png), [texto do formulário](orchestracao/.runtime/qa-manual-20261015/F08-devolucao-formulario.txt). **Próximo ajuste:** revisar composição/validação do comando no contexto do registro, sem inserir campo fora do contrato; depois retestar excesso, devolução válida, replay e FIFO da nova entrada. Causa técnica não alterada/investigada por API; auditoria permanece manual.

**Segundo pedido/XML observado:** criação sintética5UN confirmou referênciaXML acima (`70d6455b-41e2-47cf-97fd-0eaf8012cbe3`), repetição retornou a mesma referência (`1cb04fd6-4044-4e9d-ada0-52cdd6169d88`). Seleção manual justificada5UN registrada (`983d954e-b37b-42e8-beb1-9286de19968f`, justificativa4331e996-de19-4b01-a265-89452db7550f); retornoExcecaoFifoNão é coerente, pois a seleção coincide com a sugestão disponível — exceção real permanecependente. Reserva com justificativa confirmou (`c1c571d5-61d0-4382-9171-628e4052108e`). DUN no campoCódigoLido foi corretamente recusado localmente; UUID correto confirmou leitura (`cf4234a4-3e99-4e62-9334-0d1d38236260`), separação (`de2589a7-7eb9-4a0c-a4f4-712d3b3328a3`) e documentoXML (`f71ac846-a94e-4ff9-98aa-091511d85f61`). XML sintético no diretório de evidências, sem envio externo/validade fiscal. Saldo físico ativo após retirada:15UN (remanescente5 e unidadeB10 ainda não endereçada); devolução continua não executada porF08.
**Serviços/tabela/contrato — continuação manual:** aba da devolução bloqueada preservada para inspeção; nova aba de continuidade com contextoQA reaplicado. Serviços fictícios criados: `SRV-000003` ARMAZENAGEM/POSICAO_DIA (`a74bc0cb-dc01-4dc2-b573-59b52b84684e`), `SRV-000004` ENTRADA/UNIDADE_LOGISTICA (`d2c6b1c3-c3f3-4f02-b356-c97c1cc99a36`), `SRV-000005` SAIDA/QUANTIDADE_PRODUTO (`317cf3b3-d4e8-49d4-88db-15b5ee79abd7`), todos com descriçãoQA MANUAL20261015 e motivo sem aceite comercial. Tabela `TAB-000004`, ESPECIFICA paraCLI-000008/ARM-000006, início15/10/2026 sem fim, três tarifas fictícias1,25/2,50/0,75, categoria geral vazia, confirmou (`1847ddfd-fd9e-4aca-aa39-0bbe83a66d7d`). Categoria vazia passou na conferência e no servidor, conforme regra geral do contrato29; o asterisco não foi tratado como prova de rejeição. Reconsulta independente manteve as tarifas em escala6 (`915384aa-478c-4494-a9e3-88802aa2ffec`); vínculo explícito início15/10 confirmou (`76cce1ec-f389-4276-822b-c1e4a8280a54`). Configuração fictícia do contexto: início15/10/2026, America/Sao_Paulo, BRL, DIAS_CORRIDOS/duração1, mínimoNAO_APLICAVEL, GRISNAO_APLICAVEL (`354e00d3-9e61-476b-8771-88da3e2fbe29`). Sem assinatura/aceite comercial, envio ou cobrança real.

**Ampliação de evidências de apresentação:** F02 também aparece na listaTabelas: colunaCódigo usa descriçãoQA, enquanto o detalhe/reconsulta comprovaTAB-000004. F07 reaparece no atalhoConsultar tabela e vigência recebidas: Identificador mostraReferência atual indisponível, mas Consultar devolveTAB-000004 com as três tarifas; [captura](orchestracao/.runtime/qa-manual-20261015/F07-tabela-existente-referencia-indisponivel.png). A confirmação de reserva com justificativa4331e996-de19-4b01-a265-89452db7550f exibiuJustificativa:Referência indisponível, embora o campo anterior trouxesse aUUID e o resultado guardasse autor/motivo. CamposNotaOrigem/BaixaFísica também ficam sem nome no fiscal, com registros conhecidos no detalhe; não confundir essas apresentações com falta dos dados ou com erro de consulta. Os avisos transitórios de cliente/armazém durante invalidação do catálogo continuam excluídos dos defeitos.
**Fatos/cálculo — resultado manual ainda não conclusivo para aprovação financeira:** fato de saídaSRV-000005 confirmado a partir do pedidoXML retirado, categoriaPALLET, produto88, quantidade/data derivadas5UN/21:49 e chaveSAIDA:RETIRADA:5:SKU:88 (`6d6d902a-8087-4f2f-b68d-1670591efdd1`); replay manteve o fato (`bdb2dabc-40f1-4935-a629-c80a07349704`). Para entrada, estoque consultado peloUUID exato e selecionado explicitamente; fatoSRV-000004/ENT-000011 confirmou chaveENTRADA:UNIDADE:1358, uma unidade logística/21:29, cota histórica10 na nota (`4eef1f6b-5da3-4991-9857-7536e84a5600`), conservando a diferença entre cota original e saldo atual5.

Cálculo civil15/10 a16/10 exclusivo (`9e666981-c8e5-4f34-9815-97ecbaa4969d`) ficouPENDENTE, subtotal conhecido1,25, totalausente. DuasTARIFA_SERVICO_AUSENTE vieram da configuração desta auditoria: tarifas de entrada/saída sem categoria, enquanto fatosPALLET. Não são defeitos do sistema. Edição da tabela pela interface permitiu corrigir ambas categorias paraPALLET, preservar armazenagem geral e preços, com retornoTAB-000004 (`a6053484-2c63-4646-9192-e77bb01274ef`); novo cálculo solicitado, resultado ainda em conferência. As outras duas pendênciasHISTORICO_QUANTIDADE_INSUFICIENTE são ponto a apurar pela cadeia de divisão/reagrupamento/permanência; a interface não identifica claramente a segunda unidade. Sem prova independente da causa, não são promovidas a defeito confirmado nem interpretadas como saldo zero. Memória mostrou um dia/pico1, equivalência suspensa0,2 e vigência correta; [estado](orchestracao/.runtime/qa-manual-20261015/calculo-qa-estado.txt), [texto](orchestracao/.runtime/qa-manual-20261015/calculo-qa-texto.txt). Fechamento de período civil ainda não encerrado deve ser impedido de aprovação, conforme contrato31, mesmo com cálculo salvo.
### FE13-MANUAL01-F09 — cálculos diferentes têm o mesmo rótulo no seletor (P2, aberto)

**FE11; tipo:** seleção ambígua na preparação de fechamento. **Passos:** calcular15/10 a16/10 com tabela de teste sem categoria de entrada/saída (subtotal conhecido1,25/PENDENTE); editarTAB-000004, informarPALLET nos dois itens e calcular novamente (subtotal conhecido7,50/PENDENTE, somente duas pendências temporais). No resultado mais recente, clicarPreparar fechamento com este cálculo.

**Esperado:** opções identificam o cálculo persistente e permitem distinguir execução anterior/atual, por código/instante/situação ou equivalente. **Observado:** selectCálculo mostra duas opções iguais `15/10/2026 · 16/10/2026`; confirmação também mostra somente esse intervalo. São cálculos com entradasHash/subtotal/pendências diferentes. O atalho preservou a segunda opção, portanto nenhuma troca de cálculo foi constatada; o problema é a impossibilidade de distinguir escolhas pelos nomes exibidos. [Captura](orchestracao/.runtime/qa-manual-20261015/F09-calculos-mesmo-rotulo.png), [texto das opções](orchestracao/.runtime/qa-manual-20261015/F09-calculos-opcoes.txt). Base: memória imutável/versionamento e conferência de origem nos contratos29/31. Próximo ajuste: mostrar identidade e instante do cálculo, além do período, no seletor/resumo. Sem código alterado.

**Correção da configuração de teste comprovada:** novo cálculo após edição das categorias (`1bfcddb6-073f-4406-878f-940ac4027dac`) eliminou as duasTARIFA_SERVICO_AUSENTE; subtotal conhecido7,50 = armazenagem1,25 + entrada2,50 + saída5×0,75=3,75. Restam somente duasHISTORICO_QUANTIDADE_INSUFICIENTE; total ausente/PENDENTE preservado. [Memória após correção](orchestracao/.runtime/qa-manual-20261015/calculo-qa-categorias-corrigidas.txt). Ainda sem conclusão sobre causa das pendências temporais; não abrir falha de preço/soma por esse resultado. Preparação de fechamento solicitada a partir do cálculo mais recente, sem aprovação financeira.
**Fechamento/arquivo e proteções:** preparado fechamento19 (ID lido no arquivo baixado pela interface), período15/10 a16/10, versão1/PENDENTE_REVISAO/NAO_ENVIADO (`8f227468-adfc-46c5-8fd1-ad2c49ceabd1`). Consulta da versão (`66769aba-e47e-4fa7-a4f3-e3504b6c7f73`) e demonstrativo (`d00f166a-e758-4545-b526-a7841fd859ec`) preservaram hash `34fe279e502e5945e009d657256c475f11417d98e8e83830f8bdf3056d8f462c`. Clique no download gerou `Downloads/demonstrativo-ficticio.json` (19.999 bytes); espera da ferramenta não capturou o evento, mas arquivo real foi encontrado e SHA-256 conferiuEXATAMENTE. Portanto não é falha de download. Cópia de evidência [JSON guardado](orchestracao/.runtime/qa-manual-20261015/demonstrativo-qa-v1.json); nenhum envio aoESL.

Análise somente do arquivo baixado pela interface (semAPI/SQL) identificou cálculo42 e pendências nas unidades1358(A) e1360(C consumida pelo reagrupamento); intervalos de valor entre formação/divisão e ocorrência21:32 têmquantidade/base temporal nulas para essas identidades. B1359 aparece com10. Isso comprova a lacuna apresentada, não sua causa nem que o sistema deva presumir valores; contrato27 delimita reconstrução após transformações. Manter como observação funcional a apurar, sem promover guarda de histórico insuficiente a defeito por falta real da prova temporal.

Aprovar versão1 com cálculoPENDENTE foi corretamente recusado CALCULO_PENDENTE (`a9153d7c-4aa3-4189-be4e-0bc097ee4037`), sem aprovação. Rejeição integral confirmada após consulta atual (`86458dc4-49a6-4204-b534-503cf518400c`): fechamentoREJEITADO/versão1REJEITADA, mesmohash eNAO_ENVIADO, decisão/autor/motivo guardados. Nova memória solicitada para reabertura. A listaFechamentos exibe `Registro consultado` na colunaCliente, embora detalhe mostreCLI-000008; ampliação deF02 (valor principal substitui dado da coluna). Rótulo do download `demonstrativo fictício`/nome do arquivo foi observado, mas bytes/hash reais conferem; preservar distinção entre dadosQA e transporte.
**Reabertura concluída:** nova memória (`51d4e85f-4459-44c9-bea3-39aaf3dc4c30`) mantevePENDENTE/subtotal7,50. Fechamento19 reaberto com essa memória (`89539c27-19f8-4eed-b7f6-c84dd706e731`): versão2/PENDENTE_REVISAO/NAO_ENVIADO, hash `500ebd160279c1f4ce8ba0bd14823bcae6d7afc87cc05619b296688aa5a4bd20`. Consulta explícita de versão1 (`3cb791fb-33e7-40db-9fa9-2e5b96e83dd8`) comprovouSUPERADA, mesmohash34fe…462c e decisão anterior intacta. Consulta de versão2 (`da2ae502-6690-4e51-b49b-d0459ba1d8a3`) retomou a atual. F09 agora apresenta três cálculos com rótulo de intervalo igual. Sem aprovação/emissão/entrega externa.

**Impedimento atual de navegação:** ao fechar essa consulta após trocar número1→2, abriu confirmação nativa de descarte. Ferramenta informou o aviso, mas getJsDialog não o expõe; tentativa deEnter não liberou o controle. Pedido ao responsável para clicarOK enviado, sem atribuir falha aoWMS. Registros confirmados estão salvos. Não repetir comandos de negócio nem interpretar esta interrupção como cobertura dos módulos restantes. Graphify consultado novamente no escopo do projeto sobre histórico temporal, mas trouxe referências históricas amplas, insuficientes para concluir causa; contratos atuais e arquivo da interface seguem como evidência.
### FE13-MANUAL01-F10 — ajuste de contagem bloqueado por campo não editável (P1, aberto)

**FE12 → BE14; tipo:** validação local incompatível com dados preenchidos pela própria ação. **Passos:** consultar unidade QA `fc14c5c0-acba-4f77-8d34-63770730ee9a`, revisão15/saldo5; registrar contagem4 em15/10/2026 22:18; consultar a linha PENDENTE; abrir Aplicar ajuste conferido pela ação da linha. O formulário já traz revisão2, unidade15 e origem NF15102026/série15/item1/SKU QA1510-UN. Preencher delta−1, motivo, causa, destino e comprovação QA; Conferir e confirmar.

**Esperado:** conferir o ajuste identificado da origem correspondente, sem aplicação automática pela simples contagem. **Observado:** erro local `Campo fora do contrato: itemNotaId` / `Campo: itemNotaId`. Esse campo não aparece entre os controles da origem (somente Entrada conferida e Delta). Nenhuma confirmação final nem comando de ajuste enviado; saldo não foi reduzido. A entrada114 foi conferida também em Unidades → Distribuir em unidades, com a mesma NF/item e unitização concluída. Não foi escolhida a opção antiga06/10/2026 oferecida em outro caminho.

**Impacto:** impede concluir ajuste válido pela ação da própria contagem; divergência permanece pendente. **Evidências:** [formulário e erro](orchestracao/.runtime/qa-manual-20261015/F10-ajuste-contagem-itemNotaId.png), [texto dos controles](orchestracao/.runtime/qa-manual-20261015/F10-ajuste-contagem-itemNotaId.txt). **Base:** documento31, ajuste com revisão/origem/delta/causa/destino/comprovação. Revisar a montagem e validação do corpo pelo frontend; causa interna ainda não inspecionada. Sem correção de código.

### FE13-MANUAL01-F11 — instruções de exercício fictício nas telas operacionais (P2, aberto)

**FE12; tipo:** orientação incompatível com dados do contexto real consultado. Contagem física apresenta `Exercício preparado: leitura 8 sem reserva; leitura 7 retorna impedimento de reserva.`; Carga inicial excepcional apresenta `O roteiro fictício conhecido usa a entrada 201 na revisão; outras origens continuam pendentes.`. A contagem QA efetivamente observada foi5/5 reconciliada e5/4 pendente, conforme saldo/revisão, sem relação com7/8. A entrada QA conferida é114, não201. Não foi testada a alegação sobre201 nem registrado erro de carga por ausência de dados.

**Impacto:** roteiro fixo de simulação pode induzir preenchimento errado ou interpretação de rejeição esperada como regra real. **Esperado:** orientação baseada no contrato e estado retornado, com exemplos claramente separados da operação. **Evidência:** [cabeçalho da contagem](orchestracao/.runtime/qa-manual-20261015/contagem-texto-exercicio.png). Registrar captura da carga na continuidade. Sem afirmar que a regra backend depende desses números.

**Continuação da contagem e navegação:** primeira observação5 às22:17 retornou RECONCILIADA/diferença0/reservado0/sem impedimento (`bee767b3-b257-4b23-b0d0-3744d04c7d8c`); replay (`dd59f655-9780-46ea-9736-6a0773f7450f`) manteve revisão1. Segunda observação4 às22:18 retornou revisão2/PENDENTE/diferença−1/impedimento (`da319f53-6ff9-4d45-a415-71863dbac073`). Consulta independente e histórico devolveram duas revisões (`4a35bd65-552a-4296-a82e-5805552df5db`); ajuste bloqueadoF10. A lista inicialmente vazia tinha Impedimento=Não: ao remover esse filtro apareceu a linha pendente; vazio não era defeito. A coluna UUID mostrou Registro consultado: extensão do padrãoF02, UUID correto nos detalhes. Histórico passou a mostrar impedimentoSim também na revisão1 após a segunda observação; diferença frente ao retorno original foi preservada como observação, sem conclusão sobre imutabilidade desse indicador.

**Navegação:** responsável liberou o aviso anterior. Outro aviso nativo ao sair do ajuste não foi exposto pelo controle de diálogos; a aba2 continuou acessível e permitiu prosseguir sem nova intervenção do responsável. Aba3 conserva o impedimento da ferramenta, sem atribuição aoWMS. Auditoria continua ativa na aba2, em Carga inicial excepcional. Defeitos confirmados agora: onze (F01–F11); cobertura integral ainda pendente.

### FE13-MANUAL01-F12 — revisão da carga inicial aparece como objeto (P2, aberto)

**FE12 → BE14; tipo:** apresentação de estrutura composta como texto. **Passos:** listar a carga QA `CAR-000003` após revisar seus dados. **Esperado:** coluna Revisao identifica o número da revisão (2 naquele momento), com hash/dados disponíveis no detalhe. **Observado:** célula literal `[object Object]`; detalhe/retorno da revisão mostram Numero2 e hash próprios. Uma linha existe no contexto correto; não é dado ausente nem falha de gravação. **Evidência:** [coluna Revisao](orchestracao/.runtime/qa-manual-20261015/F12-carga-revisao-object.png). **Impacto:** operador não consegue conferir a revisão pela coluna destinada a ela. Corrigir apresentação da propriedade composta e retestar lista/detalhe, sem alterar os dados históricos.

**Carga inicial executada pela interface:** CAR-000003, etiqueta fornecida `QA1510-CARGA-INICIAL-01`, produto QA1510-UN/quantidade3. Criação incompleta retornou PENDENTE com ORIGEM_DESCONHECIDA/DATAS_FISICAS_DESCONHECIDAS/CONFERENCIA_PENDENTE (`d26c4313-4361-43c9-99e2-84c716bfe949`); não inventou nota nem datas. Revisão2 forneceu NF fictícia série15/número15102030/emissão15/10/2026, item1/3UN/valor30, chegada/FIFO15/10/2026 22:20/3boas/0avariadas (`7bf66583-579d-46ee-9db0-b1ae60b80ff9`). Preparação sem unidades recusou corretamente UNITIZACAO_PENDENTE (`716a13e1-f467-402c-887b-e3d47fdf091e`).

Após consultar/selecionar explicitamente a embalagem QA1510-DUN no Cadastro (seletor antes vazio, não interpretado como falta do dado), revisão3 acrescentou1PALLET/BOA/3UN (`d8497568-2b7d-437e-835e-e91d02546391`), hash `798634449d62d83ba53926ede190c7038a0d72336327a1d7a106325bc1e2f731`. Preparação (`6585a68d-358d-4075-b472-f6d4750c5149`) PREPARADA/quantidadeEstagio0, etiqueta `0e165012-78d7-4567-aa67-76161b92d820`, mantendo CONFERENCIA_PENDENTE. Replay (`c29c8148-f376-40c6-bcd6-113a687cc4ce`) conservou a mesma etiqueta. Leitura incorreta recusou LEITURA_INCOMPLETA (`e1a46cec-5d76-461d-a4cb-dd1893a5b1db`); correção com referência e única etiqueta devolvida confirmou REGULARIZADA/sem pendências (`a68a9ed5-6f19-423f-9eb8-9a164b3f2e1d`). [Retorno](orchestracao/.runtime/qa-manual-20261015/carga-regularizada.txt), [captura](orchestracao/.runtime/qa-manual-20261015/carga-regularizada.png). Nenhum documento fiscal real foi emitido; toda a origem foi fornecida como fixture QA explícita.

**Atualização:** doze defeitos confirmados (F01–F12). A captura adicionalF11 da carga está em [roteiro201](orchestracao/.runtime/qa-manual-20261015/F11-carga-roteiro-201.png). Seguir consultas/histórico da carga, contingência, relatórios e demais ações/perfis pendentes; não declarar conclusão.

**Contingência e relatórios, continuidade manual:** linha fictícia `QA1510-PLANILHA-CONT-L1`/CONTAGEM, instante físico15/10/2026 22:30, operador/fonteQA, unidadeA/revisão15/contado5/sem dependências. Registro (`948336aa-0b0b-40a2-86ad-6293c797a4a0`) ficou PENDENTE/CONCILIACAO_PENDENTE, hash `5d61441c2bc27f0d5a28d3af398890b6a5846c355975a2e54c0cc96b346f5523`. Consulta (`261baf80-fece-4040-809d-aa388f3e0156`) preservou o fato. Conciliação explícita EXECUTAR (`f1f689cd-d19b-47bb-981d-08c482eb3cd9`) retornou CONCILIADA às22:32, efeito `3ba86d68-9ba1-3243-9e9e-b436625de5b7`, contagemID15/revisão3/esperado5/contado5/diferença0/sem impedimento. Replay (`c12d7900-f4ac-4b40-85ac-a1f26604f210`) conservou o mesmo efeito/revisão. [Evidência](orchestracao/.runtime/qa-manual-20261015/contingencia-conciliada-replay.txt).

Relatórios de estoque, valor/validade, entradas, saídas, cálculos e auditoria consultados com contextoQA. Estoque mostrou somente três unidades ativas: A5+B10+carga3, total18. Indicador com valorGestor confirmou180 (150 da origem original remanescente +30 da carga), disponível5, nãoendereçado13, estágio0/conferênciaPendente0/contagemPendente0. Não somou carga e entrada duas vezes. AVISO_VALIDADE_NAO_CONFIGURADO é ausência real de configuração do cliente novo; não defeito. Pedido gerado pela carga é ENT-000012, nota15102030 e primeira chegada22:20. Cálculo histórico continuaPENDENTE/subtotal7.50, sem recalcular por simples consulta. [Indicador](orchestracao/.runtime/qa-manual-20261015/indicadores-18-valor180.txt).

Histórico da contingência abriu pelo alvo selecionado correto, mostrando exatamente REGISTRO_CONTINGENCIA e CONCILIACAO_CONTINGENCIA, motivos/usuário/antes/depois; replay não duplicou auditoria. JSON visível no detalhe confirmou entrada114/itemNota100 na origem da contagem, reforçandoF10 sem leitura deAPI/SQL. ExtensãoF02: colunasUUID da contagem, SKU do indicador, Cliente do cálculo e Tipo da auditoria usam Registro consultado, enquanto o detalhe contém o valor correto. ExtensãoF07: seletor da contingência recém-consultada continua Referência atual indisponível embora o GET pelo botão retorne o fato certo; dados transitórios durante carregamento foram excluídos.

Cancelamento de teste separado: CAR-000004, etiquetaQA1510-CARGA-CANCELAR-02, estágio3/sem nota/sem unidades, criado (`5386db80-159e-4e55-a7fc-995c569fb09b`) e cancelado pelo detalhe com motivo explícito; CANCELADA/estágio0/sem entrada/sem etiquetas, revisão1/hash preservados. [Retorno](orchestracao/.runtime/qa-manual-20261015/carga-cancelada.txt). HistóricoCAR3 consultado mostrou as três revisões e respectivos hashes ([texto](orchestracao/.runtime/qa-manual-20261015/carga-tres-revisoes.txt)). Resolver cancelamento PREPARADA e demais ramos de contingência ainda não executados.

**Cadastros/fiscal, continuidade manual:** paginação de Clientes52registros/3páginas e Armazéns45/3, busca explicitamente limitada à página; QA localizado na última página sem interpretar páginas anteriores vazias como ausência global. ClienteCLI-000008 alterado para `QA MANUAL 20261015 - Cliente revisado` (`f329ca4a-7b68-476f-9944-29f62d375f6a`), código/CPF-CNPJ preservados. Encerramento solicitado (`215b801d-c00c-44f9-a556-c553b43afe15`) ficou ENCERRAMENTO_PENDENTE; consulta específica listou11impedimentos físicos/financeiros com IDs. Confirmar inativação recusou ENCERRAMENTO_IMPEDIDO (`5f815be4-4979-419b-a430-6911244efcfa`); reativação (`6d678bf7-9f27-4cef-b723-7ef23bfd7738`) voltouATIVO, sem apagar compromissos. [Impedimentos](orchestracao/.runtime/qa-manual-20261015/cliente-encerramento-11-impedimentos.txt), [reativação](orchestracao/.runtime/qa-manual-20261015/cliente-reativado.txt).

Fiscal do cliente consultado inicialmente sem complemento e preenchido com razão/endereço/contatos/faturamento inteiramente fictícios, e-mails reservados example.invalid, UFSP/paísBR (`d6d3f3e7-5500-4194-8d4d-f3cd562e76ca`). Rejeições locais de UFsp/paísBRASIL foram corrigidas para os códigos aceitos; não classificadas como defeito. ArmazémARM6 consultado/complementado com dadosQA (`730bfea3-8f1e-474f-934f-8f08e82befb6`), mantendo Osasco/SP e sua identidade. ProdutoQA consultado; consulta fiscal retornou vazio real (`6348cbda-06a0-4b87-ac9e-642bec652253`); referência de operação exclusiva `QA_TESTE_MANUAL` registrada (`09e7c7e9-5dfe-4dda-879f-cb14e3fc3852`) com códigos/aliquotas0 expressamente fictícios e fonte de teste, sem emissão nem enquadramento real. [Cliente](orchestracao/.runtime/qa-manual-20261015/cliente-fiscal-completo.txt), [armazém](orchestracao/.runtime/qa-manual-20261015/armazem-fiscal.txt), [produto](orchestracao/.runtime/qa-manual-20261015/produto-referencia-fiscal-qa.txt). ExtensãoF02: colunaSKU de Produtos e DUN da embalagem mostram descrição; detalhes preservam QA1510-UN/QA1510-DUN.

**Estado para continuação:** clienteQA ATIVO/revisado; armazém/produto/embalagem ativos. EstoqueA5/B10/carga3 permanece18; ajusteF10 não executado, divergência resolvida por nova contagem reconciliada da contingência, com revisão antiga preservada. Fechamento19/version2 permanece EM_REVISAO/PENDENTE; cálculo anterior não recalculado por alterações cadastrais. Auditoria continua, sem encerramento declarado.

### FE13-MANUAL01-F13 — Configurar capacidade não oferece endereços existentes (P1, aberto)

**FE04 → BE08; tipo:** referência obrigatória sem caminho de seleção. **Passos:** contextoQA ARM-000006,5endereços ativos; Cadastros → Conjuntos de duas posições → Configurar. Identificador obrigatório tem somente Selecione pelo nome ou código. Fechar sem preencher; consultar Endereços → QA1510-ARM02 → Selecionar este registro; retornar à configuração: Identificador continua vazio/sem opções e revisão sem preenchimento. Não há consulta relacionada dentro do formulário.

**Esperado:** escolher a posição do armazém e conferir sua revisão/limites para o comando de capacidade. **Observado:** seletor não permite selecionar nenhuma posição; portanto não foi enviado comando com ID inventado nem de outro registro. No mesmo contexto, Novo conjunto de posições oferece os cinco códigos, e Endereços consulta ARM02 com seus dados completos. Não é falta real de cadastro ou apenas carregamento. **Impacto:** o comando específico fica inacessível pela interface; edição cadastral de limites permanece como caminho alternativo documentado, sem equivaler a testar este comando. **Evidências:** [captura](orchestracao/.runtime/qa-manual-20261015/F13-capacidade-sem-enderecos.png), [controles](orchestracao/.runtime/qa-manual-20261015/F13-capacidade-sem-enderecos.txt). **Base:** documento22, Configurar preservado por compatibilidade. Revisar resolução de recurso/contexto do campo Identificador. Sem alteração de código.

**Edições e estados cadastrais adicionais:** ARM6 nome revisado (`7b3d16a0-ddf9-45a5-afaa-2a7c57d8e33d`), encerramento (`c8d63076-5264-4a7d-8086-2e40d68f66c4`) e reativaçãoATIVO (`7fdace36-ece8-4d41-badd-ab50670fd355`). ProdutoQA descrição revisada (`db62ae63-5211-4d17-a609-99b81677f934`), encerramento (`168c07f5-4a49-48a1-87f9-a9c8146a394f`) e reativaçãoATIVO (retorno emproduto-reativado.txt). SKU/medida/controles preservados. Embalagem usada recusou mudar conversão10→11 com EMBALAGEM_EM_USO (`b197fc23-ac76-4644-82d1-a39b94808401`), conforme etapa17; descrição corrigida com conversão10 (`14ac7b88-5eb1-490b-9b90-31439d72fcfc`), encerrada (`8d74b9fd-6e4e-413c-acd3-245491261a92`) e reativadaATIVO (`4ae19a97-13ae-4fd6-bc98-f25d8ed33602`).

Endereço ocupadoARM01 recusou limite5000→4999 com ENDERECO_EM_USO (`2bec5bc2-57a9-4d53-9741-a48c0c42b5f1`); correção somente de descrição preservando os limites passou (`b2f033bd-005e-4c9c-b168-cde67ed3b174`). Encerramento ficouPENDENTE (`6278e35e-67fc-443a-8405-2e2d05a54344`), seguido de reativaçãoATIVO (retorno emendereco-reativado.txt). Nenhuma dessas rejeições esperadas é defeito. Provas textuais respectivas na mesma pasta de evidências, nomes armazem/produto/embalagem/endereco-editado(a), -encerramento e -reativado(a). Documentos14/22 e etapa17 atualizados foram relidos para distinguir regras históricas de edição limitada da edição atual e suas proteções.

**Conjunto:** tentativa comARM01 repetida recusou DADOS_INVALIDOS/Informe duas posições diferentes (`9038a656-0e5b-4e65-a9a3-2ba6c9e9d12c`). CorreçãoARM01+ARM02 criou CON-000003/ATIVO (`b73a6ed7-7348-4ac3-9ed5-9e68ee99ddf3`), limites fictícios10000kg/altura5/largura10/profundidade5/empilhamento1/todos os tipos. Criação não moveu a unidade já localizada emARM01. Uso físico e encerramento do conjunto seguem pendentes. **Defeitos confirmados: treze (F01–F13).**

### FE13-MANUAL01-F14 — remanejamento do coletor reenvia medidas já existentes (P2, aberto)

**FE07 → BE08; tipo:** preenchimento automático inadequado ao estado da unidade. **Passos:** Coletor → Endereçar/movimentar → lerA `fc14c5c0-acba-4f77-8d34-63770730ee9a` já endereçada/revisão15; ler destinoQA1510-TRI; preencher somente motivo e conferir. Medidas prévias100kg/1x1x1m/empilhamento1/uma posição aparecem automaticamente na confirmação, embora não tenham sido digitadas ou modificadas nesta operação. Confirmar.

**Esperado:** remanejar unidade inteira conservando medidas já registradas. **Observado:** DADOS_INVALIDOS/`As medidas já estão registradas; não as substitua no remanejamento.` (`bc03b66e-e0eb-4b24-84c5-493af8b46971`). A rejeição backend protege corretamente o histórico; problema do fluxo é trazer e reenviar o bloco por padrão no remanejamento. Abrir Medidas físicas e conjunto de duas posições → Remover Medidas físicas → conferir novamente (medidasNão informado, mesmo destino/motivo) → confirmar funcionou (`5ae5a586-8a6c-4c2e-9d6d-b6e82e1eb8eb`), unidade revisão16/destinoQA1510-TRI. **Impacto:** fluxo normal falha e exige remoção manual em seção inicialmente fechada. **Evidências:** [rejeição e medidas automáticas](orchestracao/.runtime/qa-manual-20261015/F14-coletor-remanejamento-medidas.png), [texto](orchestracao/.runtime/qa-manual-20261015/F14-coletor-remanejamento-medidas.txt), [confirmação corrigida](orchestracao/.runtime/qa-manual-20261015/coletor-remanejamento-confirmado.png). Base documento22: medidas do primeiro endereçamento são preservadas nos remanejamentos. Sem alteração de código.

### FE13-MANUAL01-F15 — coletor não oferece segunda posição nem conjunto cadastrados (P1, aberto)

**FE07 → BE08; tipo:** seletores necessários à ocupação dupla sem opções. **Passos:** criar conjuntoCON-000004/ATIVO deQA1510-ARM01+ARM02 com limites explícitos; noColetor lerB `bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80`, ainda sem medidas/endereço/revisão0; lerARM01 e conferir; Adicionar Posições de destino; abrir medidas, informar100kg/1x6x1m/empilhamento1/duas posições e código lidoARM02. **Observado:** Endereço do item2 tem apenas Selecione pelo nome ou código; Conjunto de posições também não tem opções. A própria tela lista as cinco posições, e o cadastro consultou/criou o conjunto ativo no mesmo armazém. Conferir bloqueia localmente com `Preencha destinos.1.enderecoId.`. Não há comando para consultar/ler a segunda posição dentro do item; não foi inventado ID nem enviada ocupação parcial.

**Esperado:** selecionar segunda posição e conjunto do contexto consultado, podendo conferir o par e seus limites. **Impacto:** não permite concluir o ensaio de duas posições pela interface atual do coletor. Preparação da unidadeB não foi enviada; permanece sem endereço/medidas. Confirmar se há outro encaminhamento de referência aplicável antes de atribuir causa técnica; os seletores vazios são a evidência atual, não uma alegação de registro inexistente. **Evidência:** captura/textoF15 na pasta da rodada. Base documento22/AC05; sem inspeção deAPI/SQL ou bypass dos seletores.

**Conjuntos/coletor, estado atual:** CON3 livre foi encerrado com motivo (`efff0aba-3247-4b07-8bf7-24e28a45eee3`), preservando par/limites emENCERRAMENTO_PENDENTE. CON4 criadoATIVO para o ensaio (`278febf4-a17c-4517-9ca5-3ed84c9bfacd`) com mesmo par/limites. A foi remanejada inteiraARM01→TRI, revisão16; B10 e unidade da carga3 seguem sem endereço. Total físico permanece18, sem baixa neste remanejamento. DUN digitado no leitorUUID foi corretamente recusado antes de consultar; UUID correto carregou o registro. Uso físico do conjunto e encerramento enquanto ocupado permanecem bloqueados pelo percursoF15, não declarados testados. **Defeitos confirmados até aqui: quinze.**

### FE13-MANUAL01-F16 — quantidade atual da etiqueta rotulada como quantidade por embalagem (P2, aberto)

**FE06; tipo:** rótulo de quantidade incorreto no retorno detalhado. Em Ler/reimprimir etiqueta da unidadeA após retirada parcial, `Quantidade de produto por embalagem` mostra5, enquanto `Quantidade Produto Por Dun` mostra10 e o DUN éQA1510-DUN. O detalhe físico confirma saldo atual5 e a configuração da embalagem10. **Esperado:** identificar5 como quantidade atual da unidade; reservar a descrição por embalagem para conversão10. A prévia impressa distingue corretamente Produtos por DUN10 e Quantidade nas notas20; não se conclui que o estoque ou o conteúdo do código de barras esteja errado. **Impacto:** operador recebe duas quantidades diferentes com significado de embalagem, confundindo remanescente e conversão. [Evidência textual](orchestracao/.runtime/qa-manual-20261015/etiqueta-a-apos-retirada-remanejamento.txt). Consulta da etiqueta (`416db90a-436d-4867-8375-2942bfd14517`) manteveUUID, revisãoConteudo3, chegada20:00, conferenteAdministradorprincipal e numeração1/2. Impressão em equipamento continua não validada.

**ComplementoF15:** consultarCON4/ATIVO no cadastro, clicar Selecionar este registro, retornar aoColetor e relerB/destinoARM01 não carregou os dois seletores; o vazio permaneceu ([após seleção explícita](orchestracao/.runtime/qa-manual-20261015/F15-apos-selecionar-con4.txt)). CapturasF15: `F15-coletor-segunda-posicao-conjunto-vazios.png/.txt`. Contagem da unidadeB peloColetor retornou RECONCILIADA10/10/diferença0/sem impedimento em15/10/2026 22:50 (`5f6035cb-579a-4130-bed6-b68c618a9948`), revisão1; [retorno](orchestracao/.runtime/qa-manual-20261015/coletor-contagem-b.txt). Sem endereço/medidas novos na unidadeB.

**Importação XML e estorno, continuidade:** primeiro arquivo mínimo era insuficiente aoXSD do fluxo de entrada; rejeição correta, sem defeito aberto. A fixture completa documentada embackend/src/test/resources/nfe foi adaptada para identidade/nomeSKU/quantidadeQA; leitura estrutural passou, mantendo aviso de que assinatura/SEFAZ não foram verificadas. Ambiente2 trouxe pendência por configuração de importação1; alinhado somente o campo da fixture para1, sem alterar configuração nem emitir documento. Nenhum dado fiscal real foi inferido. ClienteQA manteveID83/códigoCLI8 e recebeu documento fictício99151515000150 por edição explícita (`792303f9-32cc-4cb7-88ef-90913a1a9cf1`), exercitando o campo permitido na etapa17. Notas anteriores não foram reescritas.

Prévia reconheceu proprietário pelo documento exato, permitiu escolher ARM6 autorizado com aviso de destinatário diferente, e cadastrou produtoQA1510-XML/ID89 pelo botão do item, UN/CONTAGEM/precisão0/sem lote/validade. Atualizar prévia aplicou a associação; confirmou ENT-000013/ID96, NF15/15102031/chave41261099151515000150550150151020311123456783,2UN/valor20, origemXML/RASCUNHO/sem chegada. Iniciar conferência (`50557945-cd10-4d7c-9d9d-f51d01d595b4`), selecionar item103 e registrar chegadaQA1boa/0avariada às23:00 (`03389c96-caf6-41f8-861e-6a749e91e455`) retornouQUARENTENA/DIVERGENTE. Consulta de chegadas (`e900589c-021b-40e9-a686-7b114f04f92f`) conservou instante físico23:00 distinto de registro23:06. Selecionada a única chegada, estorno (`6769c122-4148-48fd-a601-0e7b180a831e`) preservou histórico e consulta posterior mostrou0recebidas/diferença−2/sem primeira chegada válida. Sem efetivação/unidade/baixa.

MesmoXML já vinculado àENT13 recusou XML_DIVERGENTE com mensagem explícita deXML existente (`0c679ccd-77e9-4d46-88c9-4d4ca20e79f5`), sem mudança física; esperado. Cancelar não foi oferecido após histórico físico, inclusive estornado, conforme documento18; não aberto como falha. Para o caminho positivo, ENT-000014/ID97 foi criada manualmente (`d992bac8-2ca7-4e88-954c-66cae0a0c66d`), recebeu outroXMLcompatível NF15102032/chave41261099151515000150550150151020321123456780 (`7d5bf833-275c-41d7-aeb3-143df5c934e7`), conservando origemMANUAL/RASCUNHO/sem chegada. Cancelamento antes de qualquer fato físico confirmou CANCELADO/revisão2 (`eb1306b3-8db7-489a-bd29-020222fe35ce`), sem liberar identidade da nota nem criar estoque. Provas `entrada-ent13-estorno.txt`, `entrada-ent13-apos-estorno-zero.txt`, `entrada-ent14-importacao-xml.txt`, `entrada-ent14-cancelada.txt` e fixturesXML na pasta da rodada. **Defeitos confirmados: dezesseis; testes ainda em andamento.**

## FE12-CONTING-TEXTO01 — apresentação curta da contingência, 15/10/2026

Lucas considerou grande e estranho o texto de Registrar fatos da planilha. Cabeçalho reduzido a uma frase; Como funciona fica ao lado de Registrar ocorrência e abre orientações por Dados do fato, Conciliação, Pendências e Reenvio. Ajuda centralizada em `information.ts`, sem bloco técnico extenso na apresentação. [Definição](docs/design-system/etapa-06.md#apresentação-da-contingência--fe12-conting-texto01).

**Conferido:** 44 testes frontend existentes em quatro arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px; resumo em até duas linhas, ajuda dentro da tela, Enter/Escape/foco, toque, critérios, restauração, erro/recuperação e perfis preservados. [Galeria](orchestracao/.runtime/contingency-intro-20261015/galeria.html), [recibo](orchestracao/.runtime/contingency-intro-20261015/validacao.json).

Regras de registro/conciliação preservadas. Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida. DEV pelo operador no fluxo habitual; alterações anteriores preservadas.

## FE02-AUD-CLAREZA01 — identificação e ação do histórico separadas, 15/10/2026

Lucas considerou confusa a marcação roxa Auditar Serviço. `AuditSelection` apresenta cartão neutro com Registro selecionado/tipo, nome e código do serviço separados; roxo fica no botão Ver histórico com ícone. Explicação visível informa o propósito da consulta. O clique passa a selecionar e abrir a etapa de auditoria da jornada, preservando a guarda de rascunho. Legado continua separado. [Definição](docs/design-system/etapa-06.md#identificação-da-consulta-de-histórico--fe02-aud-clareza01).

**Conferido:** 34 testes frontend em sete arquivos, incluindo dois novos casos de abertura com tipo/ID exatos; tipagem/lint/formatação/build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px, mais múltiplos alvos, 17 tipos, textos longos, dados parciais, vazio, bloqueio, teclado, namespace de legado e perfis. [Galeria](orchestracao/.runtime/audit-target-clarity-20261015/galeria.html), [recibo](orchestracao/.runtime/audit-target-clarity-20261015/validacao.json).

Textos em `information.ts`; sem alteração de dados, contratos ou permissões. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida. DEV pelo operador no fluxo habitual; alterações anteriores preservadas.

## FE12-CONTING-FILTROS01 — filtros da contingência organizados, 15/10/2026

Lucas pediu organizar a consulta Registrar fatos da planilha. Seis filtros em Cliente e armazém e Registro e situação. Identidade/tipo e situação/pendência ficam em linhas alinhadas; comandos abaixo e uma coluna no celular. [Definição](docs/design-system/etapa-06.md#filtros-da-contingência--fe12-conting-filtros01).

**Conferido:** 42 testes frontend existentes em cinco arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: sem overflow, rótulo longo/controles/comandos alinhados, teclado/toque, critérios exatos, restauração, pendência/erro/recuperação e perfis. [Galeria](orchestracao/.runtime/contingency-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/contingency-filters-20261015/validacao.json).

Somente apresentação; conciliação, prova da operação e permissões preservadas. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida. DEV pelo operador no fluxo habitual; alterações anteriores preservadas.

## FE12-CARGA-FILTROS01 — filtros da carga inicial organizados, 15/10/2026

Lucas pediu organizar a consulta de Carga inicial excepcional. Cinco filtros em dois grupos empilhados: Cliente e armazém e Dados da carga. Produto com largura maior; referência/situação e Aplicar/Restaurar alinhados. Tablet dá linha própria ao produto; celular usa uma coluna. [Definição](docs/design-system/etapa-06.md#filtros-da-carga-inicial--fe12-carga-filtros01).

**Conferido:** 42 testes frontend existentes em cinco arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: sem overflow, campos/comandos alinhados, teclado/toque, critérios exatos, restauração, pendência/erro/recuperação e perfis. [Galeria](orchestracao/.runtime/initial-load-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/initial-load-filters-20261015/validacao.json).

Somente apresentação; regras, contratos e permissões preservados. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida. DEV pelo operador no fluxo habitual; alterações anteriores preservadas.

## FE12-CONT-FILTROS01 — filtros da contagem física organizados, 15/10/2026

Lucas pediu organizar a consulta de Contagem física mostrada na imagem. Sete filtros em Cliente e armazém, Identificação da unidade e Localização e situação, com endereço mais largo e Aplicar/Restaurar em faixa própria. Reutiliza o padrão de consultas de estoque; grupos se reorganizam em tablet/celular. [Definição](docs/design-system/etapa-06.md#filtros-da-contagem-física--fe12-cont-filtros01).

**Conferido:** 42 testes frontend existentes em cinco arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: sem overflow, seletores e comandos alinhados, teclado/toque, critérios exatos, restauração, pendência/erro/recuperação e perfis. [Galeria](orchestracao/.runtime/count-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/count-filters-20261015/validacao.json).

Somente apresentação; contagem não aplica ajuste, cálculos e permissões continuam no backend. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida. DEV pelo operador no fluxo habitual; alterações anteriores preservadas.

## FE11-SERV-SEL01 — seleção do serviço com retorno visível, 15/10/2026

Lucas informou que Selecionar este registro parecia não fazer nada. Causa: abrir os detalhes já selecionava o serviço, e o botão repetia a seleção mantendo o diálogo aberto. Agora a consulta só seleciona pelo botão: fecha os detalhes, anuncia o nome escolhido e marca a linha da lista. Consultar/fechar outro serviço mantém a seleção anterior; o serviço escolhido chega ao item de Nova tabela com ID/revisão exatos. [Definição](docs/design-system/etapa-06.md#seleção-do-serviço--fe11-serv-sel01).

**Conferido:** 46 testes frontend em cinco arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1440 px; seleção por Enter, retorno do foco, troca explícita, uso na próxima etapa e perfis Gestor/Supervisor. [Galeria](orchestracao/.runtime/service-selection-20261015/galeria.html), [recibo](orchestracao/.runtime/service-selection-20261015/validacao.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real; versão ativa não medida neste pedido. DEV pelo operador no fluxo habitual. Seleção não grava operação de negócio nem amplia permissões; alterações anteriores preservadas.

## FE11-CONTRATO-UX01 — configuração do contrato organizada, 15/10/2026

Pedido de Lucas após imagem de mínimo, GRIS e resolução histórica. Formulário de `ConfiguracaoCobrancaController.configurar` organizado em Cliente/vigência, Ciclo, Mínimo, GRIS e Justificativa/resolução histórica. Rótulos e opções legíveis; serviços do mínimo identificados pelo nome, inclusão verde/remoção vermelha. Resolução histórica mantém orientação e tipo/compromisso alinhados; botão de informar roxo. Confirmação visível durante rolagem. [Definição](docs/design-system/etapa-06.md#configuração-do-contrato--fe11-contrato-ux01).

**Conferido:** 199 testes frontend em cinco arquivos, incluindo três novos casos de lista/precisão/ausência/resolução/conflito; tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1440 px; preenchimento também conferido nos dois temas em desktop/celular. Sem overflow, confirmação visível, controles de 44 px no celular, IDs/decimais/códigos exatos, bloqueio durante envio e Supervisor sem configuração. [Galeria](orchestracao/.runtime/contract-form-20261015/galeria.html), [recibo](orchestracao/.runtime/contract-form-20261015/visual.json).

Ajuda centralizada em `information.ts`; cálculo, parâmetros, contrato, ausência/zero, permissões e compromisso histórico preservados. Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Prova fictícia não comprova integração real; DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE12-AUD-FILTROS01 — filtros do histórico alinhados, 15/10/2026

Pedido de Lucas após imagem de Histórico do vínculo. Tipo com largura menor; busca e seleção do registro ficam lado a lado quando há espaço, com mais área para identificar o registro. Aplicar/Restaurar em faixa própria à direita; tablet reorganiza os campos e celular usa uma coluna. Aplicado às consultas `AuditoriaController.listar` em `RecordWorkspace`, sem alterar controles ou critérios. [Definição](docs/design-system/etapa-06.md#filtros-do-histórico--fe12-aud-filtros01).

**Conferido:** 45 testes frontend existentes em quatro arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício: oito layouts claro/escuro em 390/768/1024/1600 px, sem overflow; teclado, toque de 44 px, muitos/um registro, consulta pendente, erro/recuperação e restauração. Tipo/ID Long e namespace de legado preservados; Supervisor continua sem acesso. [Galeria](orchestracao/.runtime/audit-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/audit-filters-20261015/visual.json).

Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Prova fictícia não comprova integração real; DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE11-TAB-UX01 — página de tabelas e auditoria organizada, 15/10/2026

Pedido de Lucas após imagem de auditoria/visão/Tabelas e vigências. Auditoria compacta e alinhada à consulta, com tipo e identificação do alvo separados. Visão integrada ao cabeçalho junto de Nova tabela; armazém com largura limitada e Aplicar/Restaurar alinhados à caixa. Seletores de visão compartilhados agora ficam dentro da consulta. Corrigida reabertura do comando inicial ao trocar a visão; foco permanece no seletor. [Definição](docs/design-system/etapa-06.md#tabelas-e-auditoria--fe11-tab-ux01).

**Conferido:** 38 testes frontend em cinco arquivos, incluindo dois novos casos de troca de visão/foco/contexto/perfil; tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1600 px: sem overflow, cabeçalho/filtros alinhados, teclado, busca preservada ao atualizar, IDs Long exatos e perfis preservados. Vários alvos/legado também conferidos em desktop/celular. [Galeria](orchestracao/.runtime/pricing-workspace-20261015/galeria.html), [recibo](orchestracao/.runtime/pricing-workspace-20261015/visual.json).

Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Contratos, parâmetros financeiros, histórico e permissões preservados. Prova fictícia não comprova integração real; DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE11-SERV-UX01 — detalhes do serviço compactos, 15/10/2026

Pedido de Lucas após imagem de Armazenagem fictícia. Diálogo de serviço com largura proporcional, título específico/ícone de serviços, descrição sem repetição, situação em badge e código/tipo/unidade de cobrança no mesmo bloco. Fechar e Selecionar reunidos no rodapé, sem faixa exclusiva de seleção. [Definição](docs/design-system/etapa-06.md#detalhes-do-serviço--fe11-serv-ux01).

**Conferido:** 37 testes frontend em cinco arquivos, incluindo três novos casos de seleção exata, valores desconhecidos e ausência/inatividade; tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1440 px; textos longos também conferidos nos dois temas em desktop/celular. Sem overflow; Tab/Enter/Escape, retorno de foco e seleção sem escrita com ID/revisão exatos. [Galeria](orchestracao/.runtime/service-detail-20261015/galeria.html), [recibo](orchestracao/.runtime/service-detail-20261015/visual.json).

Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Contrato, dados, permissões e comportamento da seleção preservados. Prova fictícia não comprova integração real; DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE08-IND-FILTROS01 — filtros de indicadores organizados, 15/10/2026

Pedido de Lucas após imagem de Indicadores e validade. Filtros divididos em Contexto da consulta (cliente/armazém e produto em linha própria) e Opções do relatório (incluir valor/fuso). Aplicar/Restaurar ficam alinhados em faixa própria; grupos se reorganizam no tablet e celular. Padrão aplicado às páginas que consultam `IndicadorEstoqueController.listar`. [Definição](docs/design-system/etapa-06.md#filtros-de-indicadores--fe08-ind-filtros01).

**Conferido:** 44 testes frontend em cinco arquivos, incluindo três novos casos de aplicação/restauração e valor por perfil; tipagem/lint/formatação/build aprovados. Chrome fictício: dez layouts claro/escuro em 390/768/1024/1280/1600 px, sem overflow, controles com pelo menos 250 px, teclado, IDs Long/fuso exatos e Operação sem inclusão de valor. [Galeria](orchestracao/.runtime/indicator-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/indicator-filters-20261015/visual.json).

Contrato, padrões, permissões e referências preservados; ajuda do catálogo revisada. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Validação fictícia não comprova integração real; partida DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE08-AVARIA-UX01 — formulário de registro do fato, 15/10/2026

Pedido de Lucas após imagem de Registrar fato. Formulário organizado em Unidade afetada, Dados do fato, Destino da mercadoria e Justificativa/pendências. Destinos em linhas numeradas com rótulos curtos Endereço/Código lido, Adicionar verde e Remover vermelho; referências herdam roxo/âmbar/turquesa. Confirmação permanece visível ao rolar. Cores de inclusão/remoção também aplicadas às listas genéricas de `Fields`. [Definição](docs/design-system/etapa-06.md#registro-do-fato-de-avaria--fe08-avaria-ux01).

**Conferido:** 61 testes frontend em sete arquivos, incluindo três novos casos de inclusão/remoção sem perder o fato, confirmação exata, conflito/bloqueio e unidade fixa/perfil; tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1440 px, mais destinos preenchidos nos dois temas e desktop/celular: sem overflow, seções/campos alinhados, confirmação visível, teclado, ID Long/revisão/decimal exatos e restrição de pendências ao Gestor. [Galeria](orchestracao/.runtime/damage-form-20261015/galeria.html), [recibo](orchestracao/.runtime/damage-form-20261015/visual.json).

Ajuda centralizada em `information.ts`; contrato, validações, permissões, dados selecionados e revisão preservados. Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Validação fictícia não comprova integração real; partida DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE02-REF-COR01 — cores nas consultas de referências, 15/10/2026

Pedido de Lucas após imagem dos três botões de referências de Estoque. `ReferenceLookup` usa cores por família: consultas gerais/catálogos em roxo, unidade/estoque (origem) em âmbar e endereço/capacidade em turquesa. Inclui os três botões da imagem e consultas equivalentes das outras jornadas, com normal/hover/foco e bloqueio existentes. [Definição](docs/design-system/etapa-06.md#cores-das-consultas-de-referências--fe02-ref-cor01).

**Conferido:** 24 testes frontend em quatro arquivos, tipagem/lint/formatação/build aprovados; Chrome fictício em seis layouts claro/escuro e 390/768/1440 px, sem overflow. Dezoito medições de contraste normal/hover/foco acima de 4,5:1, teclado, toque de 44 px, consulta pendente e erro preservados. [Galeria](orchestracao/.runtime/reference-colors-20261015/galeria.html), [recibo](orchestracao/.runtime/reference-colors-20261015/visual.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Rótulos, destinos, permissões e seleção preservados; validação fictícia não comprova integração real. Alterações anteriores da árvore mantidas.

## FE08-FILTROS01 — filtros de unidades organizados, 15/10/2026

Pedido de Lucas após imagem de “Corrigir organização física”. Os sete filtros das páginas baseadas em `EstoqueController.listar` ficam em três grupos: cliente/armazém, identificação da unidade e localização/situação. Seletores com mais largura, controles alinhados e Aplicar/Restaurar em faixa própria à direita; tablet reorganiza os grupos e celular usa uma coluna. Busca local/Atualizar lista continuam separados. [Definição](docs/design-system/etapa-06.md#filtros-de-unidades--fe08-filtros01).

**Conferido:** 45 testes frontend em quatro arquivos, incluindo dois novos casos de aplicação/restauração exata dos sete critérios e bloqueio/erro sem perda dos filtros; tipagem/lint/formatação/build aprovados. Chrome fictício: dez layouts, claro/escuro e 390/768/1024/1280/1600 px, sem overflow, campos com pelo menos 250 px, teclado, toque de 44 px, referências contextuais e IDs Long/UUID exatos. [Galeria](orchestracao/.runtime/stock-filters-20261015/galeria.html), [recibo](orchestracao/.runtime/stock-filters-20261015/visual.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Defaults, contratos, permissões e carregamento das referências preservados; validação fictícia não comprova integração real. Partida DEV pelo operador no fluxo habitual. Alterações anteriores da árvore preservadas.

## FE02-RESULT-ALIGN01 — dados e consultas relacionados alinhados, 15/10/2026

Pedido de Lucas após imagem de Pedido/Entradas conferidas/Progresso. Padrão compartilhado de resultados com blocos delimitados, títulos e espaçamentos uniformes, campos em duas colunas nos diálogos e uma no celular. Seleção fica ao final dos dados do registro. Cada consulta relacionada possui cabeçalho com seu próprio Atualizar; margens e larguras coincidem. Lista vazia não exibe paginação inativa; listas preenchidas relacionadas agora usam a paginação existente da consulta, preservando alvo/filtros. [Definição](docs/design-system/etapa-06.md#dados-e-consultas-alinhados--fe02-result-align01).

**Conferência:** 686 testes frontend em 70 arquivos, incluindo três novos casos de vazio/zero/falso, paginação/seleção e atualização isolada após erro; tipagem/lint/formatação/build isolado aprovados. Chrome fictício: oito layouts claro/escuro em 390/768/1024/1440 px, larguras/margens/cabeçalhos alinhados e sem overflow; teclado, recarga independente, seleção e IDs Long exatos conferidos. [Galeria](orchestracao/.runtime/result-alignment-20261015/galeria.html), [recibo](orchestracao/.runtime/result-alignment-20261015/visual.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Prova de navegador usa transporte fictício e não comprova integração real. Partida DEV pelo operador e atualização pelo fluxo habitual. Alterações anteriores preservadas.

## FE04-ENC-UX01 — consulta de encerramento organizada, 15/10/2026

Pedido de Lucas após a imagem dos impedimentos. A consulta tem título específico, cadastro identificado e situação legível. Impedimentos aparecem em lista numerada, com recurso, descrição integral e referência consultada; removidos tabela genérica, colunas duplicadas, detalhe aninhado e seleção individual do impedimento. A seleção única continua sendo a do cadastro. Operações ficam recolhidas em seção própria, com duas colunas no desktop e uma no celular; fechar e recarregar permanecem no rodapé. [Definição](docs/design-system/etapa-06.md#consulta-de-encerramento--fe04-enc-ux01).

**Conferido localmente:** 50 testes frontend em cinco arquivos, incluindo cinco novos casos da consulta; tipagem/lint/formatação/build aprovados. Chrome fictício: oito layouts claro/escuro em 390/768/1024/1440 px, sem overflow; zero, múltiplos e lista não informada diferenciados, seleção sem escrita e confirmação com ID Long/tipo/revisão preservados. [Galeria](orchestracao/.runtime/closure-detail-20261015/galeria.html), [recibo](orchestracao/.runtime/closure-detail-20261015/visual.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. A verificação visual usa transporte fictício e não comprova integração. Atualização da instância pelo fluxo habitual, DEV pelo operador. Alterações anteriores preservadas.

## FE02-DIALOG-ORG01 — ação, cadastro e navegação separados, 15/10/2026

Pedido de Lucas após a imagem do encerramento definitivo. Cabeçalho informa a ação, com nome do cadastro e situação consultada em faixa própria; retirada a superfície vazia de campos internos. Tipo do cadastro selecionado permanece fixo junto à identidade/revisão. Outras operações ficam em grupo separado, recolhido durante edição, com proteção de descarte e foco no formulário após seleção. Rodapé exclusivo para voltar/fechar, detalhes e recarga; alinhado no desktop e em coluna no celular. Encerramentos simples usam largura menor. [Definição](docs/design-system/etapa-06.md#organização-dos-diálogos--fe02-dialog-org01).

**Conferido:** suíte completa frontend **678/678 em 68 arquivos**, incluindo três novos casos de identificação/organização/troca protegida/contrato/bloqueio; tipagem/lint/formatação/build isolado aprovados. Chrome fictício: oito layouts, claro/escuro e 390/768/1024/1440 px, sem overflow; menu por teclado, foco, edição pendente e confirmação com ID Long/tipo/revisão atual. Capturas inspecionadas. [Galeria](orchestracao/.runtime/dialog-organization-20261015/galeria.html), [recibo](orchestracao/.runtime/dialog-organization-20261015/visual.json).

Fonte pronta; sem SQL/backend, reinício operacional ou publicação. Atualização da instância pelo fluxo habitual, com partida DEV pelo operador. Alterações anteriores da árvore preservadas.

## END-CONJ-UX01 — formulário do conjunto de posições, 15/10/2026

Pedido de Lucas para reorganizar o formulário mostrado na imagem. Criação com título específico e três blocos: armazém/duas posições, limites do conjunto e justificativa. Nomes legíveis, posições lado a lado, peso/empilhamento separados das dimensões e orientação decimal única. Código automático identificado sem campo adicional; confirmação permanece visível durante a rolagem. [Definição](docs/22-enderecamento-movimentacao-e-estoque.md#end-conj-ux01--formulário-do-conjunto-de-posições-15102026).

Posições ficam desabilitadas até selecionar o armazém; troca de armazém limpa somente as posições, preservando limites e motivo. IDs/contrato exatos, validação, permissões e confirmação existentes mantidos, sem cálculo automático de capacidade. Textos de ajuda em `frontend/src/content/information.ts`.

**Conferido localmente:** 64 testes frontend em sete arquivos, incluindo cinco novos casos de contexto/limpeza seletiva/contrato/confirmação/envio/permissão; tipagem/lint/formatação/build isolado aprovados. Chrome fictício em 390/768/1024/1440 px, claro/escuro: oito layouts sem overflow, teclado, botão de confirmação visível e envio fictício com IDs/decimais exatos. Capturas inspecionadas. [Galeria](orchestracao/.runtime/position-set-20261015/galeria.html), [recibo](orchestracao/.runtime/position-set-20261015/visual.json).

Fonte pronta, sem SQL/backend, reinício operacional ou publicação. Atualização da instância continua pelo fluxo habitual; partida DEV pelo operador. Alterações anteriores de cores/alinhamento preservadas.

## FE02-ACOES01 — cores por função e alinhamento dos filtros, 15/10/2026

Pedido de Lucas a partir dos diálogos de Clientes e dos filtros de Embalagens/DUN. Cores distintas nos componentes compartilhados: detalhes roxo, seleção verde, recarga turquesa, voltar/fechar vermelho suave; ações principais e edição mantêm azul. Inclui ícone de detalhes, atualização da lista, referências consultadas e X dos diálogos de conta. Filtros alinham os botões à linha do controle, deixando a orientação abaixo do campo; no celular, ações em linha própria. [Definição](docs/design-system/etapa-06.md#cores-por-função-e-alinhamento-dos-filtros--fe02-acoes01).

**Conferido localmente:** 49 testes frontend, tipagem/lint/formatação/build isolado aprovados. Chrome com componentes oficiais e transporte fictício: 46 medições de contraste, mínimo 4,66:1; claro/escuro, desktop/celular, hover, teclado, seleção/retorno/recarga e bloqueio do fechar preservados. Dez verificações dos filtros em 1920/1440/1024/768/390 px, com 0 px de diferença entre topo do campo e botões quando lado a lado. Capturas inspecionadas, sem overflow/erro JavaScript/acesso API. [Galeria](orchestracao/.runtime/button-colors-20261015/galeria.html), [recibo](orchestracao/.runtime/button-colors-20261015/visual.json).

Fonte pronta; atualização da instância continua pelo launcher habitual, com DEV sob partida do operador. Nenhum SQL/backend, reinício operacional ou publicação neste recorte. Build preserva o aviso preexistente de bundle acima de 500 kB.

## PREPARO-AUTO01-TRAVA01 — espera pela preparação e DEV conferido, 15/10/2026

Novo console de Lucas: `database/iniciar-bancos.bat` concluiu DEV/PROD até V13; `iniciar-dev.bat` recusou a trava local (`31da0a2e710841909ed3c7bcecb6f5c5`) antes de SQL. A trava já estava livre na inspeção; a versão antiga não registrou quem a segurava naquela tentativa, portanto não presumir processo travado ou abandonado.

Correção: trava comum agora aguarda até180s **antes de qualquer SQL**, informa PID observado quando disponível e é liberada em finally. Plan sem SQL não adquire essa trava. Mesmo nome para bootstrap manual e launchers, preservando exclusão e reentrada do bootstrap dentro do preparador. Espera local não altera a regra de interromper após transporte/guarda SQL falhos; não repete conexões/migrations, encerra processos alheios ou modifica schema/permissões por causa de conflito.

**Conferido:** 7 testes com processos Windows fictícios (espera, timeout, dono, reentrada, exceção, abandono e Plan),18 do preparador,18 bootstrap e8 PM2; parsing PS/diff aprovados. Preparo real somente WMS_DEV concluiu `AMBIENTE_ATUALIZADO`, catálogo68tabelas/717colunas/1346itens, histórico13 e guardas/permissões aprovados, sem novo grant. Trava ocupada durante o preparo e adquirida imediatamente por outro processo após o fim. Recibos em `orchestracao/.runtime/trava-preparo-20261015/` e `orchestracao/.runtime/preparo-ambiente/b76dddca0e2c4746aa63a05ac181e540/resultado.json`.

Partida da aplicação DEV continua com Lucas: usar somente `iniciar-dev.bat` na raiz; não precisa executar o BAT de bancos antes. Nenhum reinício/publicação PROD nesta correção. Uma publicação posterior foi encontrada pronta no PM2 (`554e29f2de994d22bd2da7edbe897d67`, observada17:28UTC); preservada, sem atribuí-la a este incremento.

## PREPARO-AUTO01 — partida sempre atualizada, 15/10/2026

Pedido de Lucas: iniciar qualquer ambiente com a fonte atual; PROD publicado pelo agente e partida DEV reservada ao operador. Fluxo build atual → migrations/permissões → guarda → troca somente dos processos atestados. DEV prepara somente WMS_DEV; PROD mantém DEV completo antes de PROD pelo bootstrap existente, com exclusão de execuções simultâneas e somente bancos existentes. Aplicação continua com identidade própria, TLS validado, validate/initnever/migrationsOFF.

**Concluído em 15/10/2026:** V13 e seis permissões novas aplicadas em WMS_DEV/WMSDEV e WMS_PROD/WMSPROD, com histórico/checksums, catálogo completo e guardas antes/depois aprovados. PROD ativo no run `33cef6964be2430baf9f05201e5c9939`, ativado às 14:06:47 de Brasília; API Java75980/25590 e UI80740/25591, supervisores PM2 próprios56528/82344 online, lista salva. Sete verificações operacionais HTTP200, locais/HTTPS e versão pública exata; hashes do pacote conferidos. [Recibo](orchestracao/.runtime/preparo-20261015/producao-conferida.json). Nenhuma fixture ou operação de negócio em PROD. Validação de negócio autenticada/edição persistida ainda não foi executada neste incremento; DEV não foi iniciado pelo agente e Lucas pode rodar `iniciar-dev.bat`.

**Correção da tentativa DEV enviada por Lucas:** PreValidate passou; erro ocorreu no Migrate, mas o classificador antigo o rotulou como checksum. Metadados reais confirmaram ambos na V12, V13 ausente e sem resíduo parcial. A V13 pendente foi corrigida com `GO` entre criação da coluna e uso no índice; novo SHA3794C8638C4A3F2B823B76288967EE0E03C81E804879F15F4A4986BECFB7DD28/checksum1550763371, agora aplicado e imutável. V1–V12 preservadas, sem repair/baseline/clean. Parser local aceita índice multilinha e DEFAULT WITH VALUES; diagnóstico diferencia SQL/checksum/TLS/transporte e preserva somente códigos seguros. 145 verificações locais no conjunto, incluindo dois novos casos de diagnóstico, além do build/planos e execução real. [Guia](docs/45-producao-pm2.md).

A primeira elevação retornou cancelada; Lucas informou que a janela não apareceu. Solicitação via shell padrão do Windows iniciou processo elevado. Tentativa seguinte foi recusada por DEV simultâneo, sem SQL PROD/parada. Após correção e liberação da trava, publicação terminou exit0. Recibos das tentativas preservados em `orchestracao/.runtime/preparo-20261015/`.

## TAB-EDIT01 — editar tabelas e integrar os vínculos, 15/10/2026

Pedido de Lucas implementado na fonte: Editar em tabelas ativas para Gestor, com descrição/contexto/vigência e alteração, inclusão e remoção de serviços. Retirada a etapa lateral “Vínculos por cliente”; manutenção dos vínculos integrada a “Tabelas e vigências”. Preços antigos e IDs dos itens históricos permanecem preservados; revisão, motivo, repetição, alcance e auditoria transacionais. Mudanças de contexto ou período que reinterpretariam vínculo/cálculo são recusadas com orientação. [Contrato, V13 e seis permissões novas](docs/design-system/etapa-18-tabelas-cobranca.md).

Validação local em `orchestracao/.runtime/tab-edit01/` e logs `tab-edit01-*.log`: **189/189 backend sem banco e 670/670 frontend**, mapeamento SQL sem JDBC, tipagem/lint/build, Chrome fictício desktop/celular claro/escuro, 31 verificações do novo contrato, 18 da V12 e planos DEV/PROD sem execução SQL. Guardas/preparadores reconhecem a V13 exata e a transição dos 17 direitos anteriores aos 23 totais; históricos originais e V10 própria de PROD preservados. [Prévia local](orchestracao/.runtime/tab-edit01/galeria.html).

**Próximo deste recorte:** liberar/aplicar V13 e seis permissões no alvo específico, validar persistência e fluxo em WMS_DEV/WMSDEV com guarda atual e depois atualizar a instância pelo fluxo documentado. Fonte/pacotes locais distintos da versão ativa, não medida nesta entrega. Sem conexão SQL, DDL/grant, reinício operacional ou publicação pelo agente. O relato anterior de execução da V12 pelo operador continua sem resultado confirmado; não inferir sucesso nem executar migrations em paralelo.

## END-TIPOS01 — vários tipos e Todas as opções, 14/10/2026

**END-TIPOS01-LAUNCH01, correção local dos bloqueios de partida:** console de Lucas mostrou DEV `GUARD_CURRENT_BLOCKED` e PM2 `PROD_PM2_APLICAR_PERMISSOES_PROD_REFUSED_20`. Recibos confirmaram divergência de inventário antes de qualquer abertura SQL: V12 na fonte, contratos limitados à V11. Contratos DEV/PROD agora acrescentam somente a V12 revisada, com SHA/checksum fixados, preservando baselines, catálogo, direitos e histórico anterior. Guardas continuam exigindo V12 efetivamente aplicada no histórico real. Etapa cadastral aceita a extensão exata, sem novo delta de direitos; mensagens DEV/PM2 mostram causa controlada e recibo próprio.

Validação desse incremento: 18 casos de contrato, 12 de diagnóstico DEV e 12 de PM2 aprovados, planos de permissões DEV/PROD sem execução SQL e parsing de oito PS1 aprovados. Sem nova partida, conexão SQL, aplicação da V12 ou publicação pelo agente. Recibo `orchestracao/.runtime/tipos-unidades-20261014/resultado-guardas.json`. **Lucas informou que está executando `database/iniciar-bancos.bat`; aplicação pelo operador ainda sem resultado confirmado.** Após sucesso de DEV e PROD no BAT, nova partida DEV e atualização PM2 exigem as guardas reais atuais; não repetir a execução do banco em paralelo.

**Implementado e validado localmente, com ativação pendente.** Pedido de Lucas: Todos ou seleção múltipla nos tipos permitidos do endereço, com fotos antes/depois. Cadastro, edição e Configurar capacidade oferecem ambas as opções. Perfil da posição separado dos três tipos físicos; contratos antigos individuais preservados. Backend valida pertencimento do tipo, limites completos e interseção dos perfis nos conjuntos, mantendo Gestor/revisão/motivo/auditoria e bloqueios por uso. Excel e apresentação dos detalhes acompanham os perfis.

Validação: **667/667 testes frontend, 178/178 testes backend da whitelist sem banco**, tipagem/lint/builds/Spotless/diff check aprovados. Chrome local com transporte fictício explícito, desktop e celular390px claro/escuro; tipos anteriores, combinações e TODAS conferidos no formulário oficial, sem erros JavaScript/overflow. Antes capturado da fonte anterior ao patch nesta sessão. Comparativo e recibo em `orchestracao/.runtime/tipos-unidades-20261014/`; [contrato](docs/22-enderecamento-movimentacao-e-estoque.md#end-tipos01--tipos-permitidos-na-posição-14102026).

**Próximo deste recorte:** autorizar/aplicar a nova V12 no alvo específico com guardas atuais, validar persistência/endereçamento em WMS_DEV/WMSDEV e então publicar pelo fluxo documentado. CHECK antigo recusa os novos códigos; não basta atualizar o PM2. V1–V11 preservadas, nenhuma execução SQL/DDL, fixture real, reinício operacional ou publicação nesta entrega. Pacotes locais distintos da versão ativa, que não foi medida.

## END-VIS01 — alinhamento do coletor e conjuntos, 14/10/2026

**Revisão solicitada novamente por Lucas, 14/10/2026:** nova execução com 652/652 testes frontend e 144/144 testes backend puros, tipagem/lint/builds/diff check aprovados. Comparativo de cinco painéis em `orchestracao/.runtime/enderecamento-revisao-20261014/comparativo.html`, com PNGs separados. “Antes” reconstruído em cópia isolada com os arquivos visuais anteriores do Git; mesmos dados fictícios, viewport e componentes no navegador. Não é captura histórica de produção. “Depois” conferido em 1590/1024/390 px, claro/escuro, sem erros JavaScript/overflow; comando de endereçamento preservado. Nenhuma mudança adicional da fonte da aplicação, SQL, estoque ou publicação nesta revisão.

A pedido de Lucas, reorganizadas as telas dos prints: leitura de destino e botão alinhados no mesmo painel; confirmação com título e destino próprios; dados consultados em quatro colunas; formulário em duas colunas, medidas em três/duas/uma conforme largura; botões fora da grade de medidas e molduras repetidas simplificadas. Filtros/ações de conjuntos alinhados e limites da configuração em grade responsiva. Ajuda centralizada no catálogo; comandos, campos, permissões e regras mantidos.

**Validação local:** 652/652 testes frontend, tipagem/lint/build e diff check aprovados. Chrome fictício em 1590, 1024 e 390 px, coletor claro/escuro, configuração e lista de conjuntos sem overflow horizontal; coordenadas dos campos conferidas e confirmação do comando fictício preservada. Imagens/recibo em `orchestracao/.runtime/enderecamento-visual-20261014/`. Nenhuma execução backend/banco/PROD nem reinício DEV operacional neste recorte visual. Fonte pronta para o operador publicar junto de END-CAP01 por `pm2-wms.bat atualizar`.

## END-CAP01 — tipo permitido no cadastro da posição, 14/10/2026

Lucas relatou `CAPACIDADE_NAO_CONFIGURADA` ao endereçar bobina, apesar de preencher dimensões. Diagnóstico de fonte: POST/PUT de endereço não expunham `tipoUnidadePermitido`; a validação de endereçamento exige esse campo além dos cinco limites físicos. O print comprova destino reconhecido e erro, mas não permite afirmar quais campos estão nulos no banco de produção.

Corrigido cadastro/edição para informar PALLET/BOBINA/VOLUME diretamente, com Gestor, revisão, motivo e auditoria. Comando antigo preserva tipo já informado; mudança de tipo bloqueada em posição ocupada ou integrante de conjunto ativo. Mensagem identifica posição e campos ausentes. Tela renomeada para “Conjuntos de duas posições”, com explicação de que uma unidade em uma posição dispensa conjunto. Nenhum limite real presumido, migration/grant novo ou operação de estoque.

Validação local: 144 testes backend puros, tipagem/lint/build frontend e Chrome fictício desktop/mobile/escuro sem erros/overflow. Regressão frontend: 651/652 na primeira execução; único erro era precisão da fixture do teste novo, corrigida e arquivo revalidado (15/15). Evidências em `orchestracao/.runtime/enderecamento-capacidade-20261014/`. Sem SQL/HTTP autenticado real ou substituição de DEV/PROD neste recorte. Próximo: operador pode completar a posição já na versão ativa por Capacidade e conjuntos > Configurar; para receber a melhoria de formulário/mensagem, usar `pm2-wms.bat atualizar` e conferir cadastro/endereço com dados reais.

**Atualização anterior confirmada pelo console enviado por Lucas:** comando PM2 publicou o run `85bfb2d6abd043ee9163a06689249693`, com API/UI online e lista salva. Evidência fornecida pelo operador, sem nova conexão PROD do agente; não inclui END-CAP01 posterior.

## CAD-EDIT01 / FE06-ETIQUETA01 — DEV aplicado e integrado; produção preparada para o operador, 14/10/2026

**Resultado após autorização “pode aplicar”, 14/10/2026:** 17 permissões de UPDATE por coluna aplicadas em WMS_DEV/WMSDEV em transação, com guardas reais antes/depois, TLS validado e catálogo/histórico preservados. BAT atual preparou a fonte e substituiu somente DEV25580/25581. Run `c2dcbb1c-b3b1-4f25-8e88-b2468ebf615e`, Java70768 e Node67740 na entrega; PID/início/comando/pacote/JDBC revalidados para o ensaio. **73 verificações integradas aprovadas**, 44 chamadas API registradas mais cinco submissões pelos formulários oficiais, com releituras persistidas e etiqueta SQL renderizada sem erros JS. Fixtures identificadas: clientes81/82, armazém45, produto87, embalagem59, posição152; nenhum reset/DELETE. Recibo `orchestracao/.runtime/cadastros-etiqueta-20261014/resultado-ativacao.json`.

Lucas pediu edição dos campos cadastrais e etiqueta conforme imagem. Implementados os campos de cliente/armazém/produto/embalagem/posição no PUT, models e formulários; somente Gestor, motivo/revisão/auditoria preservados. Configurações que reinterpretariam produto/conversão em uso ou estrutura de posição ocupada são recusadas; identidades/proprietários permanecem imutáveis. Etiqueta com SKU/data destacados, produto/cliente, notas/quantidade nas notas, autor da unitização, barras e QR do UUID, DUN e sequência das unidades ativas.

[Contrato e limites](docs/design-system/etapa-17.md). Verificação local: testes puros e frontend, Chrome fictício dos cinco editores, etiqueta desktop/mobile/escuro/impressão. Evidências/resultado em `orchestracao/.runtime/cadastros-etiqueta-20261014/`. Não comprovam SQL nem equipamento físico.

**Produção preparada a pedido posterior de Lucas:** executar `pm2-wms.bat atualizar` no terminal administrador habitual. O comando ajusta idempotentemente as mesmas 17 colunas somente WMS_PROD/WMSPROD, com canal administrativo protegido, guarda preliminar sem liberação de backend e guarda normal posterior; depois prepara a fonte, confere artefatos e atualiza somente as entradas WMS do PM2. Falhas de grant/preparo preservam processos ativos. Plan local e dez testes dos limites PM2 aprovados; **nenhuma conexão ou alteração PROD executada pelo agente neste incremento**. Impressão/leitura física continuam pendentes. A limpeza anterior liberou aproximadamente1GB, preservando evidências e pacotes; não remover o novo pacote DEV ativo.

## FE06-REFERENCIAS01 — nomes/códigos e organização da unitização, 14/10/2026

Pedido de Lucas/gestor: substituir IDs visíveis por referências reais em telas semelhantes e entregar antes/depois. Componentes compartilhados de listas, detalhes, formulários e confirmações agora apresentam nomes, SKU, pedido, NF e código da etiqueta. Unitização organizada em cartões de nota/item com quantidades e rastreabilidade; vínculos de usuários selecionados por nome. Identidades, revisões e valores exatos dos comandos preservados. Consultas de nomes respeitam tipo, escopo e concorrência limitada; referência não disponível permanece explicitamente indisponível.

Validação local concluída: **637/637 testes em 63 arquivos**, incluindo 12 novos; tipagem, lint, build isolado e diff check aprovados. Chrome fictício desktop/mobile 390 px, temas claro/escuro, teclado, seleção e descarte protegido, sem overflow horizontal nem erros JavaScript. [Contrato](docs/design-system/etapa-16.md); imagens e recibo `orchestracao/.runtime/reference-names-20261014/resultado.json`.

Fonte pronta para a atualização operacional pelo fluxo documentado. Este incremento não executou SQL/HTTP autenticado real, reinício DEV/PROD ou publicação; o build isolado não comprova atualização da instância em execução. Preservados PM2, dados e processos operacionais.

## FE05-ACEITE01 — escolha de aceite dispensada na conferência sem divergência, 13/10/2026

Lucas relatou que Efetivar carga integral exigia Sim/Não mesmo sem divergência. Implementada apresentação condicional com consulta do mesmo pedido/revisão: false oculta campo/linha de confirmação e envia false; divergência/avaria/consulta não comprovada mantém escolha explícita, sem Sim automático. Dados de outro pedido/revisão antiga não dispensam o campo. Regra transacional backend/DTO Boolean obrigatório preservados. Cenário fictício de avaria alinhado; orientações no catálogo único.

Resultado local: seis testes novos, regressão completa **625/625 em 62 arquivos**, tipagem/lint/build/diff check aprovados. Chrome fictício explícito comprovou envio false sem pergunta e exigência de aceite para avaria com diferença zero; celular/tema escuro sem overflow/erros JS. [Contrato](docs/design-system/etapa-15.md), recibo `orchestracao/.runtime/receiving-finalization-20261013/resultado.json`. Sem SQL/backend real ou publicação/reinício DEV/PROD. Seleção/API/UI operacionais ainda no run 4573ed7613a6431c84839dd473e3d213; um `pm2-wms.bat atualizar` no CMD administrador publica esta correção e FE05-DATA-PESO01 juntos.

## FE05-DATA-PESO01 — calendário e apresentação de pesos, 13/10/2026

Pedido expresso de Lucas: calendário com horário na chegada física e peso KG em português (`1820.000000` → `1.820,00`). Campo compartilhado `Instant` usa controle nativo acessível e horário de Brasília, independente do fuso da máquina, enviando instante UTC explícito. Instante já carregado mantém sua precisão até edição. Formatação exata aplicada ao resumo, detalhe, prévia XML e pesos dos volumes; KG tem pelo menos duas casas, preservando frações adicionais, negativos, ausentes e valores grandes. IDs/SKU/quantidades/documento/cálculo não mudam.

Validado localmente: 23 testes novos e regressão completa **619/619 em 61 arquivos**, tipagem, lint e build final. Chrome fictício explícito, computador UTC/locale pt-BR, envio de 08:45 Brasília como 11:45Z e peso original 1820.000000; desktop/mobile 390 px claro/escuro, 44 px no campo, sem erros JS/overflow. Primeira tentativa de build teve falta de memória; repetição após fechar processos locais passou com heap 512 MB. [Contrato](docs/design-system/etapa-14.md), recibo `orchestracao/.runtime/receiving-presentation-20261013/resultado.json`. Sem integração SQL, escrita real ou publicação/reinício DEV/PROD por este incremento. Pacote ativo ainda 4573ed7613a6431c84839dd473e3d213; publicar fonte nova pelo PM2 principal em terminal administrador.

## PM2-PROD02 — transferência para PM2 principal autorizada, 13/10/2026

**Concluída pelo comando do operador em 13/10/2026, 20:04 (Brasília).** Recibo `orchestracao/.runtime/launcher-producao/pm2/main-migration.json` registra API/UI online, IDs PM2 19/20, supervisores 43604/44940 e lista salva (`saved=true`). Processos atuais Java 71156/25590 e Node 17688/25591; pacote continua `4573ed7613a6431c84839dd473e3d213`. DEV 5904/35416 preservado. Esta confirmação supera as pendências históricas abaixo; não implica publicação das alterações de fonte posteriores.

Tentativa posterior pelo CMD administrador chegou ao helper principal e retornou `PROD_PM2_HELPER_REFUSED_1`, sem registro de início da migração nem alteração de portas. Removida a dependência da string `Daemon.js` no comando: atestação agora cruza PID real do pipe com pai de processo vivo devolvido pelo PM2. Erros incluem código da fase/helper; sete testes locais aprovados, incluindo pipe Windows real fictício com vínculo aceito e vínculo errado recusado. A mensagem anterior ocultava a causa interna, portanto o mecanismo foi corrigido/testado sem afirmar que o diagnóstico elevado já foi reproduzido. Pendente executar novamente `pm2-wms.bat migrar` no CMD administrador. DEV/PROD ainda com mesmos donos 5904/35416/33516/68932.

Resultado da tentativa de elevação: Windows retornou “operação cancelada pelo usuário”; job não iniciado, sem transferência nem save principal. O comando pronto `pm2-wms.bat migrar` deve ser executado no terminal administrador do operador. Conferência posterior confirmou ambos WMS isolados online e as quatro portas DEV/PROD com mesmos donos. Helper `pm2-main-check.ps1` identifica o dono real do pipe e corrige somente o resíduo conhecido 42760 antes da transferência; parsing aprovado, execução elevada ainda pendente.

Lucas esclareceu que precisa dos dois WMS no `pm2 ls` principal, sem reiniciar a VM, e pediu `pm2 save`. Controlador alterado para filtrar somente entradas WMS por nome/diretório/script/ID, preservando os demais aplicativos; comando `migrar` retira as entradas isoladas e inicia o pacote selecionado no principal, salvando ambas as listas. Seis testes locais de propriedade/protocolo aprovados. O cliente agora recusa acesso negado antes de tentar criar outro daemon.

Execução principal recebeu `EPERM` no pipe; sessão atual tem integridade média e daemon principal exige administrador. Solicitada elevação UAC para `orchestracao/.runtime/pm2-prod-20261013/migrar-principal.ps1`; ainda sem recibo de execução no momento deste registro. **Transferência e save principal pendentes da confirmação Windows.** Processos WMS isolados 33516/68932 continuam disponíveis. Não repetir a transferência se o job estiver em execução; conferir `migrar-principal-result.json` e `main-migration.json`. Uma chamada genérica `pm2 ls` criou um daemon falho 42760, encerrado após atestar PID/início/pai/comando; ela deixou o metadado `.pm2/pm2.pid` apontando para 42760. Corrigir esse metadado apenas após comprovar o PID real do pipe principal no contexto elevado, sem encerrar outros daemons. Nenhum reinício da VM ou dos serviços alheios.

## PM2-PROD01 — API e UI ativas separadamente, 13/10/2026

Pedido expresso de Lucas: subir produção no PM2, nomes com porta e arquivo na raiz. Criados `ecosystem.config.js` e `pm2-wms.bat`; operação documentada em [produção no PM2](docs/45-producao-pm2.md). A autorização inclui a substituição desta instância PROD. PM2 próprio isolado dos demais projetos, preservando canais protegidos e guarda WMS_PROD/WMSPROD/TLS.

Publicado run `4573ed7613a6431c84839dd473e3d213`: `wms-api-25590` online (supervisor 68112, Java 33516) e `wms-ui-25591` online (supervisor 54624, Node 68932). Ativação `2026-10-13T22:26:34.5950008Z`, rodapé `13/10/2026, 19:26:34 (Brasília)`. Terminal controlador encerrou com sucesso e ambos permaneceram ativos; lista própria salva. HTTPS UI/API/relay/metadados HTTP 200. Guarda nova 26/26; 32 testes locais (2 wrapper, 6 seleção, 24 launcher) e build BE/FE aprovados. Somente antigos PROD 64800/66528 encerrados após atestação; DEV 5904/35416 preservado.

Evidência: `orchestracao/.runtime/pm2-prod-20261013/resultado.json`. Sem DDL/migrations/grants, fixtures ou testes de negócio em PROD; login autenticado não repetido. Fontes concorrentes preservadas, sem converter build em aceite de negócio. **Supera a instrução histórica abaixo de manter o BAT aberto.** Atualizar com `pm2-wms.bat atualizar`; consultar com `pm2-wms.bat status`. Inicialização automática do Windows não instalada: após reiniciar a máquina, `pm2-wms.bat iniciar`.

## QUAL-LEVE01-COR01 — três ajustes e validação local, 13/10/2026

Lucas autorizou corrigir e testar os três achados da revisão leve: unidade do produto nas tabelas/conferência e orientação física; ajuda de ocupação com uma unidade por posição; normalização kg/Kg na sugestão de bobina. Posse BE Cedro/FE Lume confirmada antes do patch, Vigia revisão, Farol canônicos/Graphify e Prumo sem conflito. SKU-ZEROS01, RegraSkuXml e FOOTER-PROD01 preexistentes são preservados.

**Concluído e revisado favoravelmente no recorte local.** ItemConferencia.unidadeMedida obrigatório vem somente do produto cadastrado; tabelas/guia física, ajuda e sugestão de bobina corrigidas, sem inferência pelo XML/peso. Backend: RED 46/11 falhas esperadas → GREEN 46/0; final verify 107/0/0/0 em oito classes (19 novos + 88 atuais), exit 0. Frontend: RED 8 falhas → GREEN 8/0; regressão final 227/227 em seis arquivos, tipagem/lint/build isolado/diff check exit 0. Não somar fases. Três classes/20 casos históricos que criam contexto Spring/Hibernate foram excluídos, sem transportar 108. [Relatório e pacotes locais](orchestracao/.runtime/qualidade-leve-20261013/cor01/resultado.md), [parecer Vigia](orchestracao/.runtime/qualidade-leve-20261013/cor01/vigia/revisao.md).

Limites e próximo: fonte corrigida e pacotes exclusivamente em cor01, nenhuma atualização/ativação da versão em execução por COR01. Sem SQL/HTTP/browser integrado, H2, credenciais, BAT/reinício/kill, publicação/PROD operacional, DDL/migrations/grants, commit/push ou ETL/jobs. Preservação 13/14 hashes iguais; drift externo de infra/prod/iniciar-prod.ps1 preservado e fora do incremento. Graphify update . executado após código, exit 1: guarda recusou 13.919 nós contra 15.220; grafo principal/backup preservados, sem force ou aceite de atualização. Revisão entregue; aguardar outra demanda, sem ativar/reabrir campanha. [Contrato pertinente](docs/18-recebimento-e-conferencia.md#qual-leve01-cor01--unidade-do-produto-no-contrato-de-conferência-13102026).

## FOOTER-PROD01 — horário da atualização publicado, 13/10/2026

Lucas pediu data/hora da última atualização bem-sucedida ao lado dos direitos reservados e autorizou subir produção. Rodapé compartilhado consulta recibo validado da versão ativa, em Brasília; compilar sozinho não altera o horário. Uma aba de versão anterior recebe aviso para atualizar, sem recarga automática. DEV não mostra informação de produção.

Validação: 588 testes frontend, 16 do servidor, 24 do launcher, tipagem/lint/build e Chrome desktop/mobile claro/escuro. Run ativo `e2165b2e2e3a4f8ca7c84aab687d3bef`, backend 64800/25590, frontend 66528/25591. Ativação `2026-10-13T22:10:08.4190579Z`, exibida como `13/10/2026, 19:10:08 (Brasília)`. HTTPS frontend, metadados e relay HTTP 200; data/versão iguais ao recibo. Guarda PROD 26/26. Reiniciados somente os processos PROD anteriores 57608/54916, após atestação; DEV 5904/35416 preservado. Sem DDL/migrations, testes de negócio ou fixtures PROD. Login autenticado não repetido. [Comportamento e limites](docs/design-system/etapa-13.md), recibo `orchestracao/.runtime/footer-prod-20261013/resultado.json`. Sem pendência de publicação; manter sessão do BAT aberta.

## SKU-ZEROS01 — regra parametrizada publicada em produção, 13/10/2026

Pedido do gestor encaminhado por Lucas: reconhecer SKU a partir do primeiro algarismo diferente de zero. Implementado parâmetro por CNPJ/CPF em `.properties`, padrão Bracell 53943098000187. Somente SKU numérico; zeros internos/alfanuméricos/XML/cadastros preservados. Ambiguidades exigem associação manual, inclusive se um SKU for exato. Regra de bobina acompanha a correspondência sem ampliar produtos/unidades.

108 testes puros (17 novos), guarda DEV 26/26, 15 verificações HTTP/SQL reais e Chrome desktop/mobile aprovados. Validação DEV no run `467776d9-683d-4a05-95bc-7bd0b6f985b4` (47272/25580, 54680/25581). Fixtures: produtos 83–85, pedido 89, nota 80; sem estoque.

Lucas autorizou expressamente a atualização de produção. Após atestar PID/início/comando/perfil/recibos/portas, encerrados somente os processos PROD 16776/5016. BAT normal recompilou a fonte atual e ativou o run `8e2ad08966344568a19be1a2158ebdd5`: backend 57608/25590, frontend 54916/25591. JAR idêntico ao candidato preparado `554545971e4a4abb9c08330c245cde7d`; 311 fingerprints atuais, regra e configuração Bracell conferidos. Guarda PROD 26/26; frontend, API pública e relay HTTPS HTTP 200; formulário de login sem erros JavaScript. Login autenticado não repetido nesta atualização. DEV observado no início/fim preservado (39564/25580, 43424/25581). Sem DDL, migrations, testes de negócio PROD ou commit/push. Detalhes em docs/design-system/etapa-12.md; recibos orchestracao/.runtime/sku-zeros/resultado.json e publicacao.json. Sem pendência de ativação deste recorte; manter a sessão do launcher aberta.

## PROD-LAUNCH03 — produção ativa e login HTTPS conferido, 13/10/2026

Lucas autorizou conferir exclusivamente WMS_PROD, criar WMSPROD com permissões mínimas, aplicar somente migrations pendentes e preparar o administrador inicial. Confirmou expressamente que este é o banco da subida para produção. Preservar DEV, dados e demais acessos; sem testes de negócio em PROD.

Inspeção atual confirmou V1–V9 e ausência de WMSPROD. Aplicadas V10 específica PROD e V11 pelo Flyway, com validação anterior/posterior; fontes e histórico DEV preservados. Criados login/usuário WMSPROD e 333 direitos explícitos restritos por objeto/coluna. Guarda real própria aprovada. Recibo: `prod-provisionamento-65d47e09dd944328a1fb7e8231d1dfec.json`, em `orchestracao/.runtime/`. Aplicação ativa no run `4364ffe9d1714d65b577775dfd2d4cab`: backend 16776/25590, frontend 5016/25591. HTTPS frontend/API/login HTTP 200, logout 204, conta principal Gestor/administrador com troca inicial obrigatória. 54 testes locais aprovados, guarda 26/26 e build atual concluído. Evidência posterior: `orchestracao/.runtime/prod-launch03-20261013/https-login.json`. DEV preservado (PIDs 47028/6332), sem operações de negócio ou fixtures em PROD. O titular deve escolher sua nova senha no primeiro acesso; produção permanece ligada na sessão do BAT.

## PROD-LAUNCH02 — preparação local anterior, 13/10/2026

Lucas confirmou o tunnel HTTPS para 25591/25590. Chave AUTH exclusiva PROD gerada sob DPAPI/ACL; binding WMSPROD e guarda implementados. Foram aprovados 49 testes locais e o preparo real do BAT (run 3b85e81c0ac644a88b949b6ec87bed12). A execução normal parou por ausência do canal SQL (run 9a24fc3f80be4b8ebf420e1c4c15c296), sem SQL/processos. HEAD HTTPS respondeu 502/Cloudflare naquela ocasião.

Esse registro precede a autorização específica e o provisionamento PROD-LAUNCH03. [Contrato e evidências](docs/44-launcher-producao-e-publicacao-futura.md).

## PROD-LAUNCH01 — bloqueio operacional confirmado, 13/10/2026

Lucas executou `iniciar-prod.bat`; run `b2b8f99e74034b1aaf6d228addda6436` recusado por `PROD_AUTH_PUBLIC_METADATA_MISSING`, antes de SQL/processos. Inspeção somente de metadados confirmou ausência de `auth-prod`. A guarda PROD ainda não implementa conexão/ramo aprovado, e o launcher não fornece o binding da credencial SQL ao helper. Material AUTH/canal WMSPROD próprios e entrada HTTPS continuam pendentes; não basta criar o JSON. A entrega FE05 concluída abaixo vale para DEV.

Diagnóstico do console/recibo esclarecido, com 23 testes locais do launcher aprovados; sem banco ou aplicação PROD, alteração de credenciais, grants, processos existentes ou configuração externa. [Pré-requisitos e estado](docs/44-launcher-producao-e-publicacao-futura.md#bloqueio-confirmado-em-13102026). Próximo deste recorte: concluir a implantação operacional com canais próprios e permissões específicas; não usar credenciais DEV nem remover a guarda para liberar a subida.

## FE05-EXPORTACAO01 — amostras reais e fluxo por bobina concluídos, 13/10/2026

Lucas entregou os XMLs/PDFs das NFs 32332 e 32485 e confirmou a regra do gestor: nesta operação Bracell, cada item é uma bobina e ocupará uma posição. Não há informação adicional a pedir ao gestor para implementar esse fluxo. A confirmação sobre erros do fornecedor não autoriza corrigir valores fiscais por suposição.

Implementado adaptador restrito ao formato de exportação recebido, com validação operacional e preservação integral do XML; NF-e convencional mantém o esquema local. Prévia identifica o formato e expõe divergências. Regra configurada por emitente/SKU sugere uma bobina por item sem agrupar SKU repetido. Conferência sugere peso comercial convertido e contagem, exige confirmação física; distribuição de uma bobina usa o peso conferido. Lotes em texto livre não são associados automaticamente.

**Validado:** 584 testes frontend, 91 testes backend puros, tipagem/lint/build e 31 verificações da jornada/ambiente integrado. Guarda nova 26/26 WMS_DEV/WMSDEV/TLS; Chrome autenticado com API real. XMLs reais somente lidos; gravações fictícias: pedidos 87/88, unidades 1347–1355, nove posições e 15.759 kg disponíveis. Repetição, peso divergente, duplicidade e posição ocupada conferidos. DEV atualizado pelo BAT: backend 47028/25580, frontend 6332/25581; 323 fingerprints e JAR atestados. Preservar fixtures e histórico.

[Contrato e evidências](docs/design-system/etapa-11.md), [recibo](orchestracao/.runtime/fe05-exportacao/resultado.json). Este resultado supera a exigência técnica de outro XML/original e a pendência de implementação para as amostras entregues. Nenhuma nota real foi lançada no estoque; impressão física, consulta SEFAZ e produção não integram esta validação.

## FE05-XML-BOBINAS01 — implementação e validação concluídas, 13/10/2026

Pedido vigente de Lucas: concluir tudo no mesmo padrão e testado. Complemento implementado: conversão exata TO/T/TON ↔ KG preservando o XML, cadastro assistido de produto pelo Gestor, contagem física independente na conferência e distribuição por bobina com soma/condição validadas no backend. Contagem persistida no snapshot transacional existente, sem DDL. Duas falhas encontradas no navegador corrigidas: lote opcional vazio e sincronização do parâmetro da entrada selecionada.

**Concluído no DEV:** 581 testes frontend, tipagem/lint/build, 86 testes puros backend/build e 33 verificações integradas autenticadas. Guarda 26/26 WMS_DEV/WMSDEV/TLS antes do BAT; nova instância backend 9004/25580 e frontend 22364/25581 mantida aberta. Fonte backend atual comprovada por 308 fingerprints; frontend Vite consumiu as correções. [Detalhes](docs/design-system/etapa-10.md), [resultado](orchestracao/.runtime/fe05-xml-bobinas01/resultado.json).

Preservar fixtures e histórico do recibo: pedidos 85/86, entrada 102, unidades 1345/1346, saldo fictício 12 KG não endereçado. Este registro supera as pendências de implementação/navegador do recorte nas seções históricas abaixo. Não há nova aprovação a pedir ao gestor para esse fluxo. Para usar a NF 32325 real permanecem XML original compatível e esclarecimento da diferença de 60 KG; não houve lançamento dessa nota, teste de equipamento físico ou produção.

## FE04-INTEGRACAO01 — confirmação autenticada no DEV, 13/10/2026

Lucas forneceu acesso para concluir a validação integrada. Guarda nova aprovada em 26 verificações, WMS_DEV/WMSDEV/TLS obrigatório; usada a instância existente BE54280/FE80748, sem reinício. **35 verificações aprovadas**: download e importação real do modelo com quatro posições; criação concorrente das oito famílias de códigos; fiscal cliente/armazém; recebimento fictício de 12 kg distribuído em duas bobinas de 5 e 7 kg; repetição sem duplicidade, recusa de soma incorreta/UUID reutilizado/revisão antiga; endereçamento, etiquetas, saldo e consultas posteriores. [Resultado e IDs preservados](orchestracao/.runtime/fe04-integracao01/resultado.json), [escopo e limites](docs/design-system/etapa-09.md#fe04-integracao01--validação-autenticada-no-dev-13102026).

A pendência de acesso/gravações dos recortes FE04 acima foi suprida para essas APIs. Sessão própria encerrada; senha/tokens não gravados. Fixtures fictícias identificadas por `TESTE-FE04-20261013-D3A412` permanecem no DEV com 12 kg de saldo, sem reserva ou expedição. Não houve importação da NF real, emissão fiscal, produção, DDL ou teste H2. Navegador autenticado/dispositivos não exercitados; testes visuais anteriores permanecem locais/interceptados. Próximo: implementar FE05-XML-BOBINAS01 (TO → kg, contagem independente na etapa 3 e cadastro assistido de SKU), preservando a solicitação do XML original e a divergência de 60 kg.

## FE04-REVISAO01 — revisão da sequência Excel, códigos e recebimento, 13/10/2026

Pedido de Lucas: pente fino de funcionamento, alinhamento e completude. Duas falhas corrigidas: revisão oculta recebida durante espera não era aplicada após recusa; ausência de contexto externo escondia recuperação de resposta incerta. Testes reproduziram ambas antes da correção. Resultado: 575 testes frontend, 12 Chrome com HTTP interceptado, tipagem/lint/build e 76 testes puros backend/build. Modelo Excel publicado conferido pelo parser Java com cópias em memória; contratos alinhados. [Matriz e limites](docs/design-system/etapa-09.md#fe04-revisao01--pente-fino-da-sequência-13102026).

Guarda real atual aprovada, somente metadados; 559 classes Java do backend ativo coincidiram com as compiladas. Sem gravação/reinício. Sessão autenticada indisponível na ferramenta, portanto integração das gravações e concorrência permanece pendente. FE05-XML-BOBINAS01 continua parcialmente apoiado pela unitização existente, mas conversão TO → kg, contagem independente na etapa 3 e criação assistida de SKU não foram implementadas. Próximo: implementar esse complemento e validar as jornadas autenticadas, preservando a pendência do XML original e da divergência da amostra. [Recibo](orchestracao/.runtime/fe04-revisao01/resultado.json).

## FE05-XML-BOBINAS01 — fluxo aprovado pelo gestor, 13/10/2026

Aprovação do fluxo de importação após análise da NF 32325, encaminhada por Lucas: XML preenche produtos/peso previsto; etapa 3 do fluxo discutido registra a quantidade física de bobinas. [Decisão e limites](docs/06-decisoes-e-pendencias.md#fe05-xml-bobinas01--peso-da-nota-e-quantidade-física-de-bobinas-13102026). Atualização somente documental, sem nova implementação ou entrada real.

Próximo passo deste recorte: detalhar/aplicar contagem e distribuição das bobinas na jornada existente, com conversão explícita TO → kg quando necessária e testes correspondentes. Permanecem pendentes XML original compatível, explicação da diferença de 60 kg da amostra e validação integrada. Não calcular contagem ou pesos individuais apenas pelo total da nota; não transportar o aceite do fluxo para aceite técnico ou de estoque.

## FE04-AUTO02 — contexto automático em fiscal e unitização, 13/10/2026

Pedido de Lucas: retirar ID/UUID/revisão da edição em Dados Fiscais Cadastrais; referência automática no novo pedido; pedido e revisão automáticos em Distribuir em unidades. Dependência FE04-AUTO01 e formulários compartilhados FE02. O ID da unitização é o pedido já existente selecionado, sem gerar outro pedido; sua revisão continua no contrato para proteção contra concorrência.

- [x] Campos técnicos removidos da edição e da conferência nas ações fiscais e de unitização, sem retirar o payload exigido pelo backend.
- [x] Abrir distribuição por cabeçalho/continuidade usa o pedido selecionado e consulta seu detalhe. Sem contexto válido, orientar seleção e impedir envio; versão zero não substitui dado ausente.
- [x] Contexto acompanha seleção antes do envio e preserva o comando original durante pendência/resposta incerta/conclusão. Grupos vazios eliminados.
- [x] Tipagem, lint, build e 573 testes frontend aprovados. Testes de navegador e limites no [recibo](orchestracao/.runtime/fe04-auto02/resultado.md).

Fonte frontend alterada, sem backend/SQL/BAT/reinício nesta rodada. Versão ativa não medida; pendência de gravação integrada da FE04-AUTO01 permanece separada. [Apresentação](docs/design-system/etapa-09.md#fe04-auto02--referências-técnicas-do-cadastro-e-da-unitização).

## FE04-AUTO01 — códigos internos automáticos e UUID oculto, 13/10/2026

**Implementado e validado localmente; consultas SQL reais aprovadas, gravação integrada pendente.** Pedido de Lucas: retirar o identificador técnico dos formulários e gerar códigos internos no sistema. Formato escolhido expressamente: sequências por cadastro, como CLI-000001 e ENT-000001. Dependências FE04-EXCEL01, formulários FE02 e serviços de cadastro/recebimento/saída/cobrança existentes.

- [x] UUID oculto nos formulários, conferências e resultados compartilhados, preservado nos comandos e na repetição segura já existente.
- [x] Backend gera CLI, ARM, CON, SRV, TAB, ENT, SAI e CAR nas transações de criação; frontend envia código ausente e apresenta o retornado. Entrada manual/XML/devolução/carga inicial e saída/remanescente usam as respectivas sequências. Códigos existentes e externos preservados.
- [x] Tipagem, lint, build e 568 testes frontend; 67 testes backend na whitelist sem banco; dez casos Chrome em build/DEV com HTTP interceptado. Consultas de sequência das oito tabelas executadas em WMS_DEV/WMSDEV após guarda atual 26/26, TLS válido, somente SELECT e rollback.
- [ ] Criações/repetições concorrentes via API real e versão ativa com o novo backend. Guarda e SELECT não equivalem à integração HTTP/JPA. Nenhum BAT, reinício, DDL ou escrita de negócio executado nesta demanda.

[Contrato e apresentação](docs/design-system/etapa-09.md#fe04-auto01--códigos-internos-automáticos) e [evidências/limites](orchestracao/.runtime/fe04-auto01/resultado.md). Próximo passo deste recorte: consumir a fonte atual pelo launcher documentado e validar gravação/repetição concorrente autenticada após gates atuais, preservando a instância do operador e sem reutilizar esta guarda como aprovação futura.

## FE04-EXCEL01 — modelo de endereços e organização da prévia, 13/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas e complementos:** disponibilizar a planilha modelo na página Importar endereços Excel, reorganizar o popup, corrigir o botão de download e estender o padrão aos formulários semelhantes. Dependências BE05 → FE04, FE02-REG01 e design system. Modelo corresponde ao parser existente: aba Enderecos, cabeçalhos exatos, identidades textuais e nenhum endereço de exemplo a importar por engano. [Apresentação](docs/design-system/etapa-09.md).

- [x] Download do modelo `.xlsx` na página e na prévia; estrutura e formatos conferidos.
- [x] Popup com armazém por nome, arquivo e justificativa organizados, preservando UUID, confirmação e bloqueios existentes. CSS externo compatível com CSP no DEV.
- [x] Padrão compartilhado de colunas, textos longos e títulos aplicado aos formulários de operação em RecordDialog.
- [x] Conferência estrutural e visual do arquivo; tipagem/lint/build, 566 testes frontend e oito casos Chrome aprovados (seis de importação e dois de formulários semelhantes, em rodadas focais). Desktop/mobile, temas claro/escuro, CSS DEV/build, download byte a byte, multipart, UUID, retorno, erros, teclado e foco.

Resultado local em [recibo](orchestracao/.runtime/fe04-excel01/resultado.md). Nenhum backend/SQL/BAT executado; HTTP interceptado e processos próprios de teste encerrados. Versão atualmente servida e importação real não medidas. Próximo passo permanece na trilha vigente, sem reabrir regressões históricas por esta alteração visual.

## FE05-PED01-A02 — tabela de entrada com rolagem lateral, 12/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas e complementos:** corrigir altura excessiva, textos e botões comprimidos; manter as colunas em uma linha com rolagem lateral. Dependências FE05-PED01/A01 e componentes FE02 → BE06, sem mudança em contratos ou regras de recebimento. [Apresentação vigente](docs/design-system/etapa-07.md#tabela-de-entrada-com-rolagem-lateral--fe05-ped01-a02).

- [x] Causa localizada: tabela especializada sem largura própria para ações; colunas iguais comprimiam os comandos, nomes quebravam e o menu expandia a linha.
- [x] Larguras próprias, rolagem interna acessível pelo teclado, textos longos com reticências e revelação por mouse/foco/toque; referência/ID em uma linha e menu flutuante.
- [x] Olho para Ver pedido, ⋮ vertical sem fundo/borda para Mais ações, ações/cabeçalho à direita e Conferência/Situação mais próximas. Permissões, IDs, consultas e confirmação preservados.
- [x] Tipagem, lint focal, formatação e build aprovados; 20 testes de recebimento/popup e 17 casos Chrome aprovados, sete rechecados após o alinhamento final. 1440/1152/390 px, temas claro/escuro, textos extensos, teclado, foco e toque com hasTouch/tap; capturas inspecionadas.

HTTP interceptado; preview próprio encerrado. [Revisão Vigia favorável](orchestracao/.runtime/receiving-table/vigia-review.md), sem achado impeditivo. [Resultado e limites](orchestracao/.runtime/receiving-table/resultado.md). Este recorte não executou backend/SQL, BAT, reinício ou integração real. Versão servida na sessão DEV e dispositivos físicos não foram medidos. Próximo passo da aplicação continua na trilha existente; esta correção visual não retoma REG01/879 ou a guarda do banco.

## FE03-AUTH-INICIO01 — novo login começa no Início, 12/10/2026

**Concluído localmente; integrado e versão servida não medidos.** Pedido do usuário: novo login deve começar no Início. Vinculado a FE03-AUTH01/navegação FE02, distinto de INIT01.

- [x] Causa: o hash da última rota sobrevivia ao logout; AuthApp atualizava usuário sem alterar a rota, e useNavigation reutilizava esse hash.
- [x] Delta mínimo: uma linha em AuthApp:119 define #inicio com replaceState(history.state) somente no sucesso de novo login, antes de montar Workspace.
- [x] RED: 6 falhas esperadas de rota/3 aprovados. GREEN: 9 casos novos + 1 regressão pertinente aprovados, 0 falhas; 6 casos existentes não selecionados.
- [x] Tipagem, lint focal, formatação e build local aprovados; revisão Vigia favorável ao recorte local. Reload/restauração/renovação, navegação explícita, senha obrigatória, permissões e rascunhos preservados.

Farol canônicos/ID, Lume fonte/testes, Vigia leitura. [Resultado e evidências](orchestracao/.runtime/fe03-auth-inicio01/resultado.md). Sem backend, SQL, HTTP integrado, autenticação real, BAT/restart/kill, commit/push ou retomada REG01/879. Fonte alterada não comprova módulo servido; jornada integrada futura exige escopo e guarda atuais antes de executar.

## BE02-DEV-GUARDA02 — timeout na identificação do banco, 11/10/2026

**Correção local revisada e única execução real autorizada chegou a SISTEMA PRONTO; falha anterior enviada por Lucas no BAT run8ccd961e preservada.** Dependência BE02-DEV-FONTE01. Conexão abriu em197,2814ms/TLS concluído; primeira SELECT de identidade inconclusiva, SqlNumber-2/native258, elapsed5694,4447ms e CommandTimeout fixo3s. O console chamou isso de abertura da conexão em120s, diagnóstico incorreto. Nenhum backend/frontend iniciou ou processo existente foi substituído.

- [x] Conferir o recibo original e localizar limite e mensagem no código atual; consulta ao mapa e memória de apoio, sem nova SQL/HTTP/DPAPI.
- [x] Consulta30s centralizada, abertura120s/global240s preservados; diagnóstico distingue fases e usa prazo do recibo. Função SELECT e ramo real do launcher: red5/11→green11/11, espera7/7 e parser5files0; [revisão12/12](orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02/vigia-review.json). Uma abertura/retry0/TLS/26checks/posse preservados.
- [x] Lucas autorizou uma execução: run6baf7482 build novoexit0/tests0/fonte6549/JAR423e, sem reuse; guarda26/26/oneOpen/12consultas30s/identidade208ms, WMS_DEV/WMSDEV/TLS. Reinício condicionado à posse: FE33036 encerrado, BE53800 encerrado pelo próprio parent anterior; BE19480/FE48932 online, SQL48480/início preservados. JDBC pool real aprovado, currentArtifact pósREADY conferido. [Autorização](orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02/autorizacao-validacao-real.json), [execução](orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02/validacao-real.json). Sem segunda tentativa/fallback; causa da demora anterior ainda não comprovada.

**Estado atual:** instância nova mantida aberta na sessão34658, BE19480/25580 e FE48932/25581. [Validação da partida e limites](orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02/validacao-real.md); [Revisão real17/17 favorável](orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02/vigia-real-review.json) à partida/JDBC, sem aceite integral. Guard/readiness/JDBC não são login/browser: launcher PROCESSES_READY_ROUNDTRIP_PENDING, realRoundtripVerified=false. Próximo da trilha BE02-FONTE01: API/navegador autenticados quando consumidor e guarda/gate atuais estiverem prontos, sem repetir BAT ou REG01/879 por este fecho.

## BE02-DEV-FONTE01 — BAT sempre consome a fonte DEV atual, 11/10/2026

**Correção permanente e classe de reinício concluídas; DEV real restaurada/revisada. Aceite API/browser continua pendente.** Autorização real: pedido do usuário para sempre subir a fonte atual pelo BAT e ativar/reiniciar somente WMS25580/25581 após preparo/guardas/posse. Separar partida de READY/API e recuperar/delimitar retornos de ferramenta são coordenação técnica Hermes WMS, não novas falas literais de Lucas. [Qualificação](orchestracao/.runtime/be02-dev-fonte01/cor-receipt01/qualificacao-atribuicao.json). Dependências BE02-DEV-REINICIO01 + COR01 → preparo/fingerprint → reinício seguro → build/BAT/JDBC atuais → API/browser quando consumidor/gate prontos. REG01/879 fechados. Posses: Farol canônicos, Prumo infra/executor, Cedro fonte/artefato, Lume consumidor/browser, Vigia leitura.

- [x] Raiz do pacote stale: BAT anterior validava CB0/4c005 sem preparo automático. Agora **build novo em todo BAT**, sem reuse/mtime/pin/fallback/promoção manual; fonte/config/wrapper/toolchain/receita pre/post/pre-stop/prelaunch e integridade ZIP/manifest/classes/libs/H2 conferidos. [Primeiro achado](orchestracao/.runtime/be02-dev-fonte01/primeiro-achado.md), [primeira entrega datada](orchestracao/.runtime/be02-dev-fonte01/resultado.md).
- [x] Fronteira inicial44 puros/80validators/typecheck/lint aprovada e separada de SQL; não repetida na continuidade. Produto/POM/consumidor sem delta.
- [x] Falha real usuário e4d: stoppedFE63340/existingProcessesAltered=true, hash do launcher antigo mudou e novo BAT bloqueou DEV_RESTART_RECEIPT_CHANGED. Oldbackend-end d2fc registra encerramento próprio8124; não afirmar ambos preservados. RED dois processos fictícios próprios+writer real reproduziu; GREEN/seis puros/parser2 aprovados. Delta somente iniciar-dev70FC/reiniciar14E4: snapshot CreateNew por processo, mesmos bytes para parse/hash, todos antesprimeirostop, pósstop somente estado/observadoUtc com witness próprio e demais campos/refs/identidade iguais. [READY/casos](orchestracao/.runtime/be02-dev-fonte01/prumo/cor-receipt01/ready-prumo.json).
- [x] Um BAT corrigido novo run3ac81b84 restaurou DEV: build33,381s/exit0/tests0, fonte6549/fingerprint318e77f/JARfresh423e, sem reuse. Guarda26/26/oneOpen WMS_DEV/WMSDEV/MandatoryTSCfalse e JDBC do pool real53800 aprovados; validate/initnever/migrationsOFF/observerOFF. Cedro8312 confirma bytes/fonte, Vigia75F/B9A2 aceita restauração/readiness/JDBC. [Resultado](orchestracao/.runtime/be02-dev-fonte01/cor-receipt01/resultado.md), [recibo](orchestracao/.runtime/be02-dev-fonte01/cor-receipt01/resultado.json), [review real](orchestracao/.runtime/be02-dev-fonte01/vigia/cor-receipt01/runtime-jdbc-review.json).
- [ ] API atual/três módulos servidos/browser Início: preparo LOCAL READYv2 sessão49833/PID69236/início00:33:12,391500UTC observado ativo, ponto interno desconhecido/exit ausente; objeto completo e polls preservados. Sem duplicar criação/kill desconhecido/consumir gate expirado. Não é falha WMS/SQL; original integrado d2fc irrecuperável é histórico separado. [Metadados locais](orchestracao/.runtime/be02-dev-fonte01/lume/cor-receipt01/preparo-49833-pid-metadata.json).

**Versão/processos registrados na restauração:** BE53800/25580/início00:46:07,774725UTC e FE33036/25581/início00:46:39,9433115UTC abertos, sessão43123 retida; SQL48480/início09/10T18:31:41,951705UTC preservado. Esta chamada foi nova ativação/stopped[] porque antigos já ausentes; nenhum stop/snapshot WMS real exercitado após o patch, mecanismo validado nos fictícios reais. Fonte6549/e77/JAR423e atuais conferidos, frontend servido/browser ainda sem prova.

**Limites/próximo:** manter DEV aberta e retornar restauração revisada + impedimento local estreito a Hermes WMS. API/browser seguem pendentes neste ID; sem nova SQL/guarda/BAT/stop/renovaçãoTTL/duplicação de criação por este fecho. JDBCmetadata1/cinco sockets não são SQLcount/tempo/CPU/API/UI; nenhum ganho COR01 transportado. Guards/posse inconclusivos ou transporte falho STOP sem retry/fallback, fonte/build/seleção falhos antes substituir, dono desconhecido/escuta adicional não encerrados. Segredos externos, sem H2 novo/PROD/saAPI/DDL/migrations/índices/grants/SQLserver/sharedruntime/outrosprocessos/ETL/jobs/commit/push; preservar dados/histórico/principal/sessões/alterações. Entrega DEV consumível pelo BAT atual; diferença ativa explicitada se ocorrer.

## QUAL-CONF01-PERF02-INICIO01-COR01 — lote na visão e validação focal, 11/10/2026

**Concluído e revisado no recorte real finito.** Lucas autorizou a necessidade mínima Prumo v2 para otimização real no SQL Server DEV: helpers revisados, uma guarda atual, uma partida própria423e/25586 e quatro GET de negócio readonly comparáveis. Os cinco chats já foram resetados; não repetir reset nem confundir com restart. Dependência PERF02-INICIO01 → fonte/lote/DTO → candidato/checks locais → plano/READY/revisão prévia → guarda atual → runtime/JDBC → API/DTO real → revisão final. REG01/879 permanece encerrado, diagnóstico preservado sem repetição.

- [x] Distribuir Cedro backend somente se ajuste necessário, Lume consumidor/medição pertinente, Prumo guarda/vínculo/necessidade, Vigia revisão e Farol canônicos.
- [x] Constatar primeiro delta: fonte atual já usa lote na rota visão, consumidor compatível; nenhuma alteração artificial de código/contrato.
- [x] Registrar único bloqueio integrado: Prumo21:39:43UTC comprovou PID55292 ausente/sem listener25586; operador61588/25580 e frontend59336/25581 preservados. [Prova](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/prumo/primeiro-bloqueio.json).
- [x] Concretizar candidato423e/fonte6549 conferidos por302 registros e conjunto de paths existente; build histórico rotulado, sem novo build de aplicação. Novos checks8 backend puros e3 consumidor FE aprovados, typecheck0, separados de aceite SQL real. Delta produto/FE=0.
- [x] Entregar necessidade mínima exata Prumo v2 e plano finito before/after; Lucas autorizou explicitamente a execução mínima. Original helperREG01 imutável, novo helperCOR01 create-only.
- [x] Adaptar somente launcher/Bootstrap e novo helperCOR01 protegido; ROOT/READY/revisão pré-partida vinculados antes da guarda, REG01 original intacto.
- [x] Executar uma guarda atual26/26/oneOpen WMS_DEV/WMSDEV/Mandatory/TSCfalse e uma partida própria48048/25586/JAR423e/fonte6549; comprovar DataSource/JDBC real do candidato com TLS validado e uma SELECT de metadados, observer por request OFF.
- [x] Concluir uma authHTTPS privada e exatamente quatro GET200 (warmup por alvo e medida por alvo), mesmo ator/canal/rota/contexto/parâmetros; DTO raw20 campos igual exceto consultadoEm. API2.920,6733→483,7328ms; uma amostra por braço, sem contagem/tempoSQL/CPU ou ganhoUI atribuídos.
- [x] Encerrar somente candidato48048 às22:34:02UTC com identidade/handle/recibos/portas atestados; operador61588/FE59336/SQL48480 e todos os artefatos preservados.
- [x] Fixar parecer final Vigia favorável ao recorte real finito sobre as provas e recibos centrais, com limites de cobertura/causalidade; Cedro favorável ao par executado. [Vigia](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/vigia/parecer-real-final.json), [Cedro](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/cedro/parecer-real-finito.md).
- [x] Enviar o fecho real revisado e os paths a Hermes WMS, sem nova autorização de promoção/execução inferida.

**Limites:** somente WMS_DEV/WMSDEV/TLS Mandatory/TSCfalse, validate/initnever/migrationsOFF; sem H2/DDL/índices/grants/migrations/PROD/escrita de negócio/ETL/jobs/commit/push. Preservar operador61588/25580 e FE59336/25581, sem restart/kill/promoção/proxy/launcher do operador. Uma partida própria candidata25586 autorizada; eventual encerramento somente desse novo candidato mediante identidade/recibo/handle/portas atestados conforme v2. Fonte/lote existente deve ser reutilizado; preservar valores/null/alcance/permissões/mapa. Primeira falha de transporte/guarda inconclusiva interrompe sem retry/fallback. Sem campanha879, framework/executor/inventário extenso ou limpeza automática. Evidências novas em `orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/`.

**Resultado:** redução observada83,4376% nesse par API com corpo completo, auth fora do cronômetro e warmups separados. Todos504bytes,501 unidades/valor1252,50 preservados, contexto67/33/GESTOR/dia11/competência2026-10. Mapa vazio e nenhum campo null nesta resposta real; células preenchidas/null/outros perfis somente nos checks puros separados. Cache compartilhado/JIT/tempo de vida não equalizados e NOW não fixado; não é p95/SLA ou prova do ganho total da tela. Build candidato histórico07:07:11UTC reutilizado/fonte302 registros conferidos, zero novo build de aplicação/delta produto/FE. [Relatório](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/resultado.md), [recibo](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/resultado.json), [comparação bruta e hashes](orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/comparacao-real.json).

**Próximo:** fecho revisado enviado a Hermes WMS; aguardar nova demanda, sem nova execução neste recorte. A proposta mínima foi exercitada no candidato423e/fonte6549 na rota exata visão; aplicar ao operador exige autorização específica de promoção/ativação e futura medição da tela nessa versão. Operador permanece2C1A, pacote selecionadoCB0 não promovido.9,16s UI/IPC anterior não é baseline API; contadorSQL simétrico indisponível e HTTP não mede SQL/CPU. Nenhuma nova guarda/SQL/HTTP/partida está pendente no recorte COR01; não executar rodada adicional por histórico.

## QUAL-CONF01-PERF02-INICIO01 — diagnóstico focal do Início, 11/10/2026

**Diagnóstico focal concluído, revisado e enviado a Hermes WMS, nova demanda expressa de Lucas após reset.** REG01 permanece entregue/encerrado; esta etapa não reabre o ledger879. Dependências: resumo atual → consulta graphify → versão efetivamente aberta/guarda atual → primeira medição em leitura → trace frontend/backend → revisão focal → proposta concreta para Hermes WMS. Relaciona FE02-DASH02 e BE14-DASH01, preservando CARGA01/INIT01.

- [x] Confirmar cinco chats WMS e distribuir: Lume waterfall/catálogos/contexto/gráficos/render, Cedro endpoints usados/N+1/agregações, Prumo runtime/guarda, Vigia revisão e Farol consolidação.
- [x] Identificar runtime/fonte/JAR/pacote DEV por metadados seguros; operador61588/JAR2C1A/fonte declaradaae6, pacote selecionadoCB0/4c005 e candidatoIND501423e/6549 são distintos, sem promoção.
- [x] Medir uma Atualizar visão na sessão já aberta após guarda atual26/26 WMS_DEV/WMSDEV/Mandatory/TSCfalse e vínculo17/17. Tempo9.158,380ms UI/IPC, limite observacional7.790–9.158,380ms; sem API/TTFB/SQL/render. CSP ResourceTiming e CLI sem Network impedem comparação separada; login adicional dispensado, gráficos/primeira carga fria não medidos.
- [x] Revisar caminho crítico, repetição unitária comprovada por bytecode antes do mapa e proposta mínima, com versão/evidências/limites, e retornar a Hermes WMS. Vigia favorável ao diagnóstico/proposta com limites instrumentais,11 comparações/17 snapshots documentais; não confundir com testes de desempenho.

**Resultado focal:** o JAR aberto chama indicadorUnidade em cada unidade autorizada antes de devolver o mapa, com seleção+3U invocações estruturais de repositório (avarias/fatos/movimentos e ramificações); não são statements SQL medidos ou causa temporal dominante demonstrada. A fonte atual já contém lote. Proposta futura mínima: usar esse caminho na rota exata visão, com equivalência valores/nulos/alcance/permissões e vínculo candidato/pacote/runtime. Separar leitura financeira do mapa é alternativa de contrato, sem implementação. [Relatório](orchestracao/.runtime/qual-conf01/perf02-inicio01/resultado.md), [recibo](orchestracao/.runtime/qual-conf01/perf02-inicio01/resultado.json), [revisão focal](orchestracao/.runtime/qual-conf01/perf02-inicio01/vigia/parecer-focal-final.md).

**Limites:** somente diagnóstico/proposta; sem implementação, campanha879, novo processo de aplicação, restart/kill/launcher/promoção, fixture/escrita de negócio, DDL/índices/migrations/PROD/H2/ETL/jobs/commit/push. Preservar instância aberta e interromper no primeiro transporte falho ou guarda inconclusiva, sem retries. Reutilizar ferramentas; evidências focais em `orchestracao/.runtime/qual-conf01/perf02-inicio01/`, canônicos somente Farol.

**Próximo:** aguardar nova demanda para implementação ou instrumentação focal capaz de separar API/rede/UI na mesma superfície. Diagnóstico/proposta entregues; nenhuma execução adicional pendente/autorizada nesta fase. Instância e sessão preservadas, REG01 encerrado.

## QUAL-CONF01-SQL01 — testes reais SQL Server, 10/10/2026

### QUAL-CONF01-SQL01-REG01 — regressão real após BOOT02, 10/10/2026

**Concluído no recorte finito autorizado; qualidade global e879 não integrais. Autorização expressa de Lucas:** “vamos concluir aquilo q estavamos fazendo ja q desbloqueei”. A restrição de apenas retornar terminais terminou. Dependências BOOT02 → candidato/fonte atuais → guarda atual → SUT/JDBC → CARGA01/INIT01 e navegador REAL integrado. [Escopo e posses](orchestracao/.runtime/qual-conf01/sql01/reg01/escopo.json).

- [x] Retomar os quatro papéis próprios: Cedro backend/testes/ledger879, Prumo guarda/infra, Lume frontend/navegador, Vigia revisão; Farol mantém os canônicos.
- [x] Comprovar alvo/identidade/TLS/processo/ACL/permissões/catálogo/histórico atuais e vincular fonte/JAR/runtime antes das primeiras operações; fase2 exige guarda suplementar atual antes de autenticação.
- [x] Executar a regressão SQL do recorte finito autorizado, com esperado independente e fixtures somente API legítima identificadas/idempotentes, IDs relidos; limites de atores/históricos e879 permanecem no recibo final.
- [x] Executar navegador integrado REAL no recorte autorizado: carregamento/contexto/listas/mapa, entrada direta, sessão própria/renovação e INIT01; instrumento observado e limites admin/fresh403/stale explicitados.
- [x] Atualizar o ledger delimitado dos879 IDs, build/fonte oficial/pacote exercitado e matriz finita sem somar focais; provas independentes e limites P01/instrumentação mantidos. Revisão final da consolidação vinculada em recibo separado.

**Primeiro marco REG01 real, 11/10/2026 02:24–02:28 UTC:** guarda26/26 e JDBC SQL Server WMS_DEV/WMSDEV/TLS do backend próprio34172, fonte4c005/JARCB0. Login Gestor200 sem troca e três GETs200 antes do Início (uma identidade/dois de negócio), statements inspecionados4/5/29. Navegador REAL: três etapas aprovadas/uma parada após aplicar contexto, vermelho preservado e causa em diagnóstico. [Andamento/matriz](orchestracao/.runtime/qual-conf01/sql01/reg01/andamento.json), [revisão independente](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/first-real-review.md). Lote/históricos/alcance/concorrência e ledger879 final continuam em execução. Fase2 usa o mesmo SUT e guarda suplementar atual antes de nova callback legítima, sem exportar tokens ou reescrever recibos da partida. Observations/JDBC ficam somente em `orchestracao/.runtime/qual-conf01/sql01/cedro/reg01/796ff269-4319-473e-8c39-7104b8c8b7fb/observations`, dentro da allowlist existente; [interface coordenada](orchestracao/.runtime/qual-conf01/sql01/reg01/interface-coordenada.json). Sem rebuild/preparador/promoção/reinício do operador neste marco.

**Marco REG01 de 11/10/2026 03:03–03:05 UTC:** guarda corrected26/26, uma conexão, mesmo SUT34172/fonte4c005/JARCB0. A API autenticou sem troca e completou doze solicitações com status/observação esperados, criando e relendo produto49/embalagem45; cliente67/armazém33 foram reutilizados. Exit1 preservado na comparação textual de quantidade1.000000 versus1, com igualdade decimal exata demonstrada sem nova API. Os dois casos repetidos não são somados: cinco IDs novos SQL únicos nos recibos executados, nenhum dos879 IDs históricos ainda comprovado migrado. O runner do navegador entrou na callback e saiu0 sem relatório de jornadas, portanto nenhum novo caso browser aceito. Correções mínimas dos instrumentos/focais e revisão precedem a próxima guarda; lote501, históricos, alcance, concorrência e navegador integral permanecem em execução. [Confronto/contagens](orchestracao/.runtime/qual-conf01/sql01/reg01/phase2-corrected-stop-confronto-farol.json), [fronteira coordenada](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-phase2-corrected2-interface.json).

**Marco REG01 de 11/10/2026 03:25–03:28 UTC:** guarda corrected2 atual26/26 antes das duas execuções, mesmo backend próprio34172/fonte4c005/JARCB0. API exit0 com12 casos da rodada aprovados,101 solicitações correlacionadas mais CSRF/login; cliente67/armazém33/produto49/embalagem45 reutilizados sem novo POST de cadastro. Pedido71 produziu501 IDs distintos, iguais à releitura paginada e à valoração, valor exato1.252,50; conservação, rollback DOCTYPE e replay completo/alterado409 conferidos. Três amostras reais por massa:28 statements em1/8/40/200/500 e36 em501, máximo500 placeholders. Não é comparação de tempo contra runtime antigo nem medida de CPU SQL. A união dos recibos contém15 IDs SQL novos únicos, sem somar repetições; nenhum dos879 casos históricos é declarado integralmente migrado. Navegador REAL terminou0 com relatório obrigatório e4/4 jornadas atuais aprovadas: entrada direta, navegação rápida com catálogo pendente, reload/renovação200 e contexto64/31 seguido de Produtos200, sem Início/dashboard. Reds e saída0 anterior sem relatório preservados. Históricos/null/fracionário/alcance/concorrência e jornadas frontend restantes seguem em execução; somente Gestor global atual está atestado, sem administração ou canal atual de ator restrito. [Confronto calculado](orchestracao/.runtime/qual-conf01/sql01/reg01/corrected2-real-confronto-farol.json), [próxima fronteira funcional](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-phase3-funcional-interface.json).

**Marco REG01 de 11/10/2026 04:18–04:21 UTC:** guarda phase3 atual26/26, uma conexão, no mesmo backend próprio34172/fonte4c005/JARCB0. Navegador REAL terminou0 com relatório obrigatório e13/13 jornadas atuais aprovadas, incluindo as quatro anteriores sem somá-las novamente: mapa antes dos gráficos, refresh com informação de carregamento, vazio por filtro, produto49 em página única, duas recusas de rascunho sem PUT, uma escrita identificada de descrição49/versão0→1, invalidação/releitura e descarte na troca de contexto. Abort de navegação foi observado nesta rodada; a causa UNKNOWN anterior permanece histórica. A API autenticou200 sem troca, completou a identidade e cinco solicitações correlacionadas, criou e releu o cliente fictício F3 de ID68, mas encerrou1 antes dos cenários financeiros. Documento fiscal esperado com separadores divergiu da normalização contratual; correção somente dos literais da fixture e prova de reaproveitamento precedem a retomada. Não é defeito de produto demonstrado. Família F3 preserva67/33/49/45/pedido71;501 são unidades logísticas, não produtos. Revisão dos relatórios atuais em curso; históricos, fracionário, null, concorrência, recusas401/403 da própria sessão e ledger879 ainda exigem provas. Ator restrito/admin atual não atestado e P01 material são limites separados. [Confronto calculado](orchestracao/.runtime/qual-conf01/sql01/reg01/phase3-real-confronto-farol.json), [próxima fronteira exata](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-phase3-corrected-own-session-interface.json).


**Marco REG01 de 11/10/2026 05:05–05:10 UTC:** API F3 terminou0 com19 casos reportados aprovados,113 solicitações de negócio e três de autenticação (116 transportes;114 registros incluem EU). Decimal1,24→1,2165433, null distinto de zero, precisão6, origens/revisões/replay, duas avarias/reparos e concorrência nativa200+409 foram revisados no recorte. União sem identidade repetida:33 IDs SQL novos; um suboráculo de preservação da nota ainda exige GET posterior, portanto não há aceite integral dos33 por essa contagem. Navegador de sessão própria terminou0 com5/5 casos revisados: CSRF403, Origin403, renovação200/logout204 e espera natural271058ms, duas chamadas AuthClient rejeitadas por uma renovação401/zeroGETusuarios. Não repete nem soma as13 jornadas funcionais anteriores, nem prova revogação administrativa. Pures isolados247 IDs/27 XML concluídos sem falhas/erros/skips; wrapperPSexit1 por stderr Mockito e nativeMavenexit desconhecido, sem claimverify0. Restantes83, ledger879 porID, releitura das notas e viabilidade de retirada/marcos continuam trabalho; alcance restrito/admin e P01 são limites materiais separados. [API19 revisada](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/phase3-corrected-api-real-review-v2.md), [OWN5 revisada](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/own-session-real-review.md), [pendências autorizadas](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v1.json). Não repetir fases aprovadas para documentação.


**Marco REG01 de 11/10/2026 05:31–05:45 UTC:** os330 IDs puros novos foram executados e revisados em módulos isolados (247+83, sobreposição0), separados dos41 históricos sem reexecução e sem aceite SQL. O módulo83 terminou com83/83 e nativeMaven/parent0; o247 possui247/247 XML e nativeMaven desconhecido/parent1, sem inferir verify330exit0 ou causa exclusiva do aviso do wrapper. A callback readback terminou0 com três GETs novos dos pedidos72/73/74, notas/itens/IDs/quantidades previstas/valores/null preservados e zero escrita; revisão independente do actual ainda em curso neste corte. É complemento da preservação, não três casos somados à união33. Retirada/baixa e marco financeiro têm plano HTTP natural viável em fixtures novas; CARGAfinal exige pareceres API/infra e comando pronto antes da guarda. No frontend, Workspace/draft sob própria fresh e duas GETs de negócio compartilhando fresh200 continuam trabalho específico com Gestor atual, distinto do OWN5 e sem impedimento genérico de administrador. Alcance restrito/revogação exigem canal atual pertinente; ledger879 porID, limites instrumentais/P01 e fecho global permanecem abertos. [Puros330](orchestracao/.runtime/qual-conf01/sql01/reg01/pure330-exact-union-confronto-farol.json), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v2.json), [interface CARGAfinal](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-carga-final-interface.json).


**Complemento REG01 de 11/10/2026 05:45 UTC revisado:** três GETs reais posteriores dos pedidos72/73/74 preservaram a projeção original completa das notas66/67/68 e itens74/75/76/77, quantidades4+6/2/10 e valores0,40+0,84/null/zero. Parecer independente25/25 sem flags supriu a lacuna de nota após transformações. A união permanece33 casos SQL novos do recorte, sem somar três releituras e sem migrar automaticamente os879 históricos. Próximos executivos: retirada/baixa/marcos e HTTP público CARGAfinal, consumidor Workspace/fresh específico e ledger porID; nota,19 famílias,13 jornadas,5OWN e330puros não exigem repetição. [Parecer do readback](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/note-readback-real-review-v2.md), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v3.json).

**Marco REG01 de 11/10/2026 06:44–06:46 UTC:** CARGA corrigida terminou0 com17 casos reportados aprovados,87 registros de protocolo mais CSRF/login (89 transportes),86 observações correlacionadas; a rejeição400 de header ocorreu antes do conector e seu contador SQL é desconhecido. Retirada/baixa, avaria, ajuste e marco seguiram20→16→12→pendência→8→10; IDs existentes e nota original preservados, sem recriação. Workspace literal terminou0 com dois casos reportados aprovados numa callback e duas esperas naturais271000ms: uma renovação200 para duas GETs negócio, e, após logout próprio204, renovação401 sem enviar PUT do rascunho. Não certifica o timer240s da raiz AuthApp, direitos administrativos ou proteção de rascunho além da sessão. Revisões independentes e união por assertion estão em curso;17 não são somados cegamente aos33. Achado real de desempenho: indicadores-estoque501 registra4023 statements preparados (15+8×501), enquanto36 de Home é outra rota. Correção batch isolada, novo artefato/runtime revisados, ledger879 final e avaliação das jornadas restantes continuam trabalho autorizado, sem repetir fases aprovadas. [CARGA actual](orchestracao/.runtime/qual-conf01/sql01/reg01/cedro/796ff269-4319-473e-8c39-7104b8c8b7fb/http-carga-final-corrected-v1/http-result.json), [Workspace actual](orchestracao/.runtime/qual-conf01/sql01/reg01/lume/browser-workspace-causal-796ff269-4319-473e-8c39-7104b8c8b7fb/result.json), [residual medido](orchestracao/.runtime/qual-conf01/sql01/reg01/cedro/carga-final-v1/indicator-residual-501-real.json), [dependências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v7.json).

**Marco REG01 de 11/10/2026 07:07–07:15 UTC:** revisões independentes confirmaram CARGA17 funcional e Workspace2 no recorte, mantendo o residual de desempenho e o fecho geral abertos. IND501 teve vermelho estrutural12/5 falhas e verde12/zero, com teste idêntico; build package isolado native0/tests0/POM oficial. Materializado somente o serviço aprovado, fonte oficial6549ffb6 e JAR423eeb81 do candidato; os outros301 arquivos e o POM permaneceram iguais. A instância própria34172 continua executando a fonte4c005/JARCB0, separada do novo candidato. Interface final seleciona UUID4f61b450/API25586, sem partida ou aceite SQL por esse registro; revisão unificada, comando pronto e guarda atual precedem a prova real do mesmo endpoint501. Ledger879 final e cancelamento/fresh, timer da raiz240s e paginação real de produtos continuam trabalho viável ainda não feito; alcance/admin e fresh403 nativo têm barreiras específicas. Não repetir19/17/13/5/2/330 para documentação. [Materialização revisada](orchestracao/.runtime/qual-conf01/sql01/reg01/cedro/indicator-residual-v1/official-source-materialization.json), [interface final](orchestracao/.runtime/qual-conf01/sql01/reg01/interface-indicadores-batch-coordenada.json), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v8.json).

**Marco REG01 de 11/10/2026 07:41–08:00 UTC:** o candidato corrigido foi exercitado em novo backend próprio55292/UUID4f61/API25586, fonte6549ffb6/JAR423eeb81/JDBC SQL Server WMS_DEV/WMSDEV/TLS. Guarda26/26, revisão unificada e comando pronto precederam uma partida e uma callback legítima dentroTTL. Resultado7/7 registros aprovados, cinco escopos (três amostras501 e MIX/NULL/ZERO/FINAL), nove transportes e zero escrita/retry/cleanup. No mesmo endpoint indicadores-estoque501,4023→31 statements inspecionados/preparados em cada amostra, máximo500 placeholders; DTO byteigual,501 IDs/1252,50 preservados. Sete famílias IN500+1 e operação por JSON com duas chamadas/um parâmetro, sem inferir tamanho do JSON. WholeDTO501/MIX/NULL/FINAL; ZERO limita comparação integral anterior ao split e comprova os IDs atuais589/590/zero distinto de null. Não é medida de CPU/rows/SLA, nem sete casos somados à união48 candidata ou aos879. Instância34172/fonte4c005/JARCB0 permanece preservada. Parecer real26/26 favorável ao recorte concluiu o residual IND501; fecho geral permanece aberto. API LISTA21 identificada em contexto novo, timer nativo240s/cancelamento durante fresh/paginação21 e próprios refresh replay/concorrente são trabalho viável em preparo, ainda sem novas execuções. Ledger ASTv6 corrigiu métodos falsos for/if:451 candidatos naturais/42 subconjuntos/13Login são inventário, não passes; qualificação de prefixo é texto normalizadoLF, não bytes iguais. Reviews/comandos prontos precedem cada nova guarda temporal. Não repetir fases aprovadas por documentação. [IND501 real revisado](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/indicadores-batch-real-review.md), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v9.json), [interface API LISTA21](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-fe03-lista21-interface.json), [interface FE03/FE04](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-fe03-fe04-residual-interface.json).

**Marco REG01 de 11/10/2026 08:52–09:00 UTC:** LISTA21 consumiu guarda26/26, reviews API/infra eREADY atuais no mesmo backend próprio55292/fonte6549/JAR423e. Uma autenticação Gestor200 sem troca, seis transportes (quatro negócio/dois auth), umPOST de cliente fictício69 e releitura200; terminou1 com zero casos aprovados antes de armazém/produtos/paginação/recusa administrativa. Fixture21 e seu descriptor não existem. Diferença apenas nos timestamps POST/GET; saveAndFlush já ocorreu, mas o POST usa o Instant da entidade sem recarga. Correção instrumental projeta somente timestamps de criação à precisão datetime2(6) e usa DTOpersistido; demais campos eGET→GET exatos. Focal22 local não é aceite SQL; nova interface/reviews/READY/guarda corrected precedem reutilização69 semPOST. FE quatro cópias observadas ebuild isolado preservam seteassets produto, com vínculo do ticket antigo aoUUID; ainda depende de fixture21 actual e revisada antes de seus próprios gates. LedgerASTv6 teve a única ressalvaCRLF qualificada por revisão8/8, sem novoPASS; planoV7 mantém451 candidatos naturais/42subsets/13Login pendentes. Massas antigas menores exigem valor n×2,50/duas posições/Page1, ainda não cobertos pelas amostras agrupadas de501produtos. Próprios refresh replay/concorrente seguem viáveis em preparação separada; admin/atorrestrito eP01 são limites específicos. IND501 real revisado permanece concluído no recorte4023→31; fecho geral aberto, sem repetir fases aprovadas. [STOP revisado](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/fe03-lista21-actual-stop-review.json), [fronteira corrected](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-fe03-lista21-corrected-interface.json), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v10.json).

**Escopo finito REG01 encerrado, 2026-10-11T15:34:33.399794+00:00 UTC:** R13 concluiu a última família autorizada por união de três critérios M001 capturados e revisados em rodada native/protected1 com doze critérios distintos da conclusão. M001 não foi repetida; não há alegação de uma callback verde de quinze casos. Resultados reais e revisão independente estão vinculados ao recibo final. As cinco massas1/8/40/200/501 preservaram preço2,50,750raízes totais (593 reutilizada e até749novas),capacidade/mapa/reserva no recorte. LISTA21 final4/4,FE03/FE04 final5/5,IND5014023→31,R12 Replay1 eConcurrent derivado1 já revisados não foram repetidos. Concurrent conserva native/protected1 e aggregate original ausente; somente o critério funcional derivado é aceito. Quatro famílias/cinco protocolos/quinze critérios centrais encerrados no escopo finito, sem nova campanha. Ledger879 delimitado:330puros atuais+41históricos+2HTTP integrais+30parciais+476não migrados;wholepersist0/whole879false. Limites admin/atorrestrito/Supervisor/ALL64/history-clock/P01 e instrumentação permanecem explícitos. Fonte oficial IND501 recebeu somente o serviço após o build, raiz6549 igual ao candidato revisado;JAR423e foi exercitado no próprio55292, sem atualização inferida do pacote DEV ativo ou versão do operador. Guardas expiradas/STOPs/reds e todos os dados/processos preservados. A consolidação final recebeu revisão independente favorável, vinculada ao [recibo final revisado](orchestracao/.runtime/qual-conf01/sql01/reg01/resultado-final-revisado.md). Aguardar nova demanda expressa; nenhuma execução adicional ou rotina foi iniciada. FIFO R13 qualificado ao unico root colocado elegivel por namespace; sem prova de discriminacao entre varios elegiveis. [Resultado](orchestracao/.runtime/qual-conf01/sql01/reg01/resultado-final.md),[matriz finita](orchestracao/.runtime/qual-conf01/sql01/reg01/fechamento-finito-reg01-final.json),[ledger879](orchestracao/.runtime/qual-conf01/sql01/reg01/ledger879-final-delimitado.json).

BOOT02 permanece prova datada de partida/JDBC/API e duas leituras de negócio, com pacote selecionado e DEV preservado; não é regressão integral. Usar processo SQLDEV próprio se necessário após guarda, sem reiniciar a instância do operador. Preservar dados, histórico, principal, sessões e processos. Não repetir a troca concluída do Gestor nem usar seu envelope antigo. H2 novo proibido; SQL somente WMS_DEV/WMSDEV, Encrypt=true/Mandatory, TrustServerCertificate=false, validate/initnever/migrationsOFF. Falha de transporte ou guarda inconclusiva interrompe sem retry/fallback; demais limites SQL01 permanecem. Demais recibos novos em `orchestracao/.runtime/qual-conf01/sql01/reg01/`, com a única exceção observations explícita acima. As contagens efetivamente executadas são delimitadas no marco atual, sem transportar casos históricos.

**Entrega parcial revisada: política implementada; partida e primeiras leituras SQL reais confirmadas em BOOT02; regressão integral pendente.** Nova autorização expressa de Lucas, dependências CARGA01/INIT01 → fonte/pacote atuais → guarda SQLDEV → SUT/JDBC → testes seguros → frontend/backend/SQL real. CARGA01 conserva seu fecho local histórico; 879 casos históricos não equivalem a 879 casos SQL. [Decisão permanente](docs/06-decisoes-e-pendencias.md#qual-conf01-sql01--testes-reais-sql-server-10102026), [relatório histórico](orchestracao/.runtime/qual-conf01/sql01/resultado.md), [resultado histórico](orchestracao/.runtime/qual-conf01/sql01/resultado.json), [verificação atual](orchestracao/.runtime/qual-conf01/sql01/verificacao/resultado.md).

- [x] Registrar ID e posses exclusivas: Cedro backend/testes/contratos; Prumo guarda/infra/preparador; Lume integração frontend; Vigia revisão; Farol canônicos.
- [x] Comprovar guardas reais datadas 22:18/22:58/23:15 UTC: WMS_DEV, ORIGINAL_LOGIN=USER_NAME=WMSDEV, TLS Mandatory/TSCfalse, processo/ACL/permissões/catálogo68/716/históricoV1–V11. Uma conexão/26 checks por recibo; não são testes funcionais. [Última guarda](orchestracao/.runtime/qual-conf01/sql01/prumo/runs/77369c08-51da-4764-b07b-b8d132754bc4/guarda-corrected-cim-v2.json).
- [x] Mapear 879 IDs de64 classes: 0 SQL executados, 41 puros executados, 335 candidatos puros não executados e503 impedimentos. 14 novos puros de instrumentação/seis de política separados; não transportar contagens históricas. [Ledger](orchestracao/.runtime/qual-conf01/sql01/cedro/policy-1312ea14-9416-426d-a73f-4542320263ca/ledger879-final.json).
- [x] Recusar H2 em toda execução nova, inclusive perfil histórico/opt-in/override; default e selectors inseguros bloqueados em validate. Oito negativas reais sem cenários; perfil puro literal de seis classes, 61/61, seis XML/zero falhas/erros/skips e build sem banco. [Recibo](orchestracao/.runtime/qual-conf01/sql01/cedro/policy-1312ea14-9416-426d-a73f-4542320263ca/resultado-policy-final.json). Preparador defaultSTOP e SomenteBuild puro/tests0/semativos; nenhum preparo real.
- [ ] Completar CARGA/INIT/lote/BigDecimal/históricos/alcance/concorrência/dialeto em SQL real: BOOT02 confirmou SUT/listener/JDBC e primeiras leituras; demais casos continuam não executados.
- [ ] Exercitar navegador integrado no candidato SQL e concluir regressão SQL por dependências: BOOT02 comprovou login e HTTP funcional no recorte de API; navegador e regressão integral permanecem pendentes.

**Bloqueio histórico, superado por BOOT02:** após revisão conjunta e guarda positiva de23:15 UTC, Java58212 iniciou23:15:49 e encerrou antes de listener25584/JDBC, `SQL01_PROCESS_EXITED`. Runtime/readiness SHA903E9F iguais; causa, exit nativo e conexões SQL do backend desconhecidos. [STOP](orchestracao/.runtime/qual-conf01/sql01/prumo/runs/77369c08-51da-4764-b07b-b8d132754bc4/corrected-attempt-stop.json), [revisão dos arquivos28/28](orchestracao/.runtime/qual-conf01/sql01/vigia/corrected-stop-review.md), sem PASS SUT. Primeiro STOP préJava preservado; nenhuma repetição real após o bloqueio.

**Versões históricas anteriores ao BOOT02:** fonte final393/AF21FB2A, hash oficial4c005964; pacote puroCB0AFB70, 558 classes/253 DTO/13 recursos, 571 entradas iguais compilado. Não selecionado/executado, não deixa BAT pronto. Tentativa encerrada3f523/b096 preservada; último runtime do operador atestado22:17 UTC BE61588/FE59336/JAR2C1A é datado e não foi alterado. Recibos ativos permanecem intocados; frontend paralelo preservado.

**Revisão concluída:** [política28/28](orchestracao/.runtime/qual-conf01/sql01/vigia/policy-final-review.md), [infra22/22](orchestracao/.runtime/qual-conf01/sql01/vigia/infra-final-diagnostic-review.md) e [central25/25](orchestracao/.runtime/qual-conf01/sql01/vigia/central-final-review.md), favoráveis locais delimitadas, sem SQLLaunchApproved. **Encaminhamento histórico, substituído pelo BOOT02:** integração SQL estava impedida. Diagnóstico opt-in de fase/tipos/cadeia/ExitCode passou12 casos fictícios, mas não recupera a causa histórica. Prumo executa, Farol coordena, Cedro vincula candidato/cliente e Vigia revisa; somente após nova fronteira coordenada/autorizada e prova source/JAR/interfaces/guarda atuais, uma partida -Diagnostico em outputs create-only novos, sem reutilizar runtime/readiness773. Não executada agora; sem retry, fallback, H2, reset/DELETE/DDL/índices/grants/migrations/PROD/saAPI ou restart de processos existentes. P01 e Network/TTFB/CPU reais permanecem limites materiais. [Andamento](orchestracao/.runtime/qual-conf01/sql01/andamento.json).


Limite do diagnóstico: catch pode encerrar OWNchild vivo antes de observar exit; os12 fictícios tinham HasExited=true. Não comprova saída natural nem causa histórica. Nenhuma nova execução real autorizada pelo parecer/fecho.

### QUAL-CONF01-SQL01-BOOT01 — verificação do encerramento, 10/10/2026

**Concluído localmente, pedido de Lucas para verificar o aviso de Hermes.** Dependência SQL01. Treze hashes e os seis XML confirmam 61 testes puros/zero falhas, build exit0 e oito recusas; apenas conferência das provas históricas. Reproduzida falha no carregamento real do Spring Boot sem contexto Spring/banco: APPLICATION_LAUNCH → MalformedURLException, exit20. Corrigido o comando Java com `-cp <JAR>`; regressão da função Start real em cópia isolada passou de3/4 para4/4, e o diagnóstico existente passou12/12. [Relatório e limites](orchestracao/.runtime/qual-conf01/sql01/verificacao/resultado.md), [recibo](orchestracao/.runtime/qual-conf01/sql01/verificacao/resultado.json).

- [x] Identificar e corrigir falha concreta do carregador, com vermelho/verde e regressão pura.
- [ ] Confirmar partida/API/JDBC e jornadas reais no WMS_DEV; SQL funcional continua0. O evento histórico não teve logs/exit suficientes para atribuição definitiva da causa.

**Encaminhamento BOOT01, atendido pelo BOOT02:** vincular fonte/pacote/candidato e revisão ao launcher corrigido, depois guarda atual e partida própria com diagnóstico em saídas novas. Candidato antigo3f523 diverge da fonte atual4c005964; revisão antiga não aprova o hash novo do launcher. Nesta verificação não houve SQL/guarda/HTTP/processo WMS real, alteração dos recibos ativos ou reinício. Backend e frontend integrado continuam pendentes. O relatório anterior e suas revisões permanecem históricos.

Mapa auxiliar: `graphify update .` interrompido por804 avisos de extração/schema; sem `--force`, retry ou atualização certificada. Limite separado no [recibo do mapa](orchestracao/.runtime/qual-conf01/sql01/verificacao/graph-resultado.json).

### QUAL-CONF01-SQL01-BOOT02 — desbloqueio real solicitado, 10/10/2026

**Concluído o desbloqueio de partida e preparo DEV, pedido expresso de Lucas: “preciso que desbloqueie isso”.** Dependências SQL01/BOOT01 → build atual → revisão vinculada → guarda atual → processo próprio → JDBC/API autenticada. Autoriza concluir o desbloqueio técnico dentro dos limites SQL01, inclusive primeira troca normal da senha da conta fictícia Gestor exata, se exigida, preservando o principal. Nova senha cifrada DPAPI fora Git e verificada antes do POST; uma entrada inicial e uma entrada após troca confirmada, sem retry ou redefinição administrativa. Evidências novas e create-only; processos existentes preservados.

- [x] Build novo: seis XML, 61 testes puros/zero falhas/erros/skips; JAR atual sem H2 e compilado igual. [Build](orchestracao/.runtime/qual-conf01/sql01/cedro/b02-6daae4b6/end.json).
- [x] Protocolo de acesso passou seis casos puros, inclusive recusa de identidade/persistência/troca e falha inconclusiva, sem banco/rede/DPAPI.
- [x] Revisão independente, guarda26/26, processo próprio59140/listener25584, JDBC SQLServer WMS_DEV/WMSDEV/TLS e autenticação legítima; primeira troca normal204 e nova entrada200 da conta fictícia Gestor. Três GETs200 passaram: identidade, quatro produtos do cliente64 e vinte posições do armazém31. Duas leituras de negócio e uma de identidade; nenhum estoque/fixture criado.
- [x] Pacote CB0AFB70 selecionado nos dois recibos DEV, fonte4c005964; cópias anteriores e backups atômicos exatos. [Seleção](orchestracao/.runtime/qual-conf01/sql01/prumo/promocao-46c9b929/resultado.json), [parecer6/6](orchestracao/.runtime/qual-conf01/sql01/vigia/boot02-post-selection-review.json). Preparador agora seleciona somente candidato com prova real vinculada; SomenteBuild continua separado. Corrigidos escopo de funções e assinatura File.Replace com backup explícito, sem retry cego; tentativas locais anteriores preservadas.

**Evidência atual:** execução real aprovada em [R3](orchestracao/.runtime/qual-conf01/sql01/prumo/runs/ddc2555b-5518-4fa7-b486-694e6c983e2f/execution-result.json), [revisão](orchestracao/.runtime/qual-conf01/sql01/vigia/boot02-real-proof-review.json). Processo próprio fechado e DEV61588/59336/inícios preservados. Senha Gestor atual no [visualizador local](orchestracao/.runtime/qual-conf01/sql01/lume/ver-senhas-atuais.bat), envelope antigo preservado. Não comprova navegador ou regressão SQL integral. O BAT pode consumir o preparo atual, mas fará nova guarda e só reiniciará DEV no fluxo de dono/recibo documentado. Instância existente não foi reiniciada por BOOT02. [Relatório do desbloqueio](orchestracao/.runtime/qual-conf01/sql01/verificacao/boot02-resultado.md). Próximo da trilha geral: ampliar a regressão SQL por dependências, sem transportar879 casos históricos ou repetir a senha já alterada.

**Marco REG01 de 11/10/2026 06:13–06:23 UTC:** P01 teve uma Save/Replace fictícia própria, tmp0 e child0; revisão independente16/16 confirmou somente não reprodução. A causa histórica continua desconhecida, P01 material aberto e qualidade integral do runner não encerrada; não há nova execução P01 a fazer por esse resultado. A CARGA autenticou200 uma vez e encerrou1 com um caso de baseline aprovado e24 solicitações de negócio mais CSRF/login: massa fictícia nova pedido75/raízes591–592/origens88–89 preservada, valor20. O POST de endereço409 reutilizou a coordenada do endereço122; causa da fixture comprovada em fontes/JSON, sem defeito de produto demonstrado. Retomada corrigida exige releitura dos IDs exatos e zero recriação/recebimento/unitização/merge desses existentes, fontes finais/reviews/READY antes guarda própria. Workspace literal entregou instrumentos congelados, mas ainda não executou seus dois casos. União aceita33 SQL,13 jornadas funcionais,5OWN e330puros permanece separada; ledger879 final e recortes funcionais restantes seguem em execução. [P01 revisado](orchestracao/.runtime/qual-conf01/sql01/reg01/vigia/p01-focal-actual-review.md), [fronteira corrigida](orchestracao/.runtime/qual-conf01/sql01/reg01/auth-carga-final-corrected-interface.json), [pendências atuais](orchestracao/.runtime/qual-conf01/sql01/reg01/pendencias-autorizadas-v6.json).


## FE03-AUTH01-COLOR01 — cores das ações de usuários, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas e complemento de alinhamento.** Dependências FE03-AUTH01 e tokens FE02. Editar usa azul da marca e Redefinir senha usa âmbar; cabeçalho Ações, botões e Conta protegida alinhados à direita. Coluna dimensionada pelo conteúdo, com rótulos dos botões em uma linha. Preservados nomes, permissões, desabilitação, diálogos e foco.

- [x] Aplicar cores por tokens, hover/pressionamento e alinhamento local da coluna; corrigir quebra de rótulos identificada na inspeção visual.
- [x] Tipagem, lint focal, formatação, build isolado e 14 testes existentes de autenticação aprovados. Três cenários Chrome aprovados em 390/1024/1440 px, nos temas claro/escuro: contraste mínimo de texto 4,5:1, hover, foco via Tab, alinhamento, rótulos em uma linha e regressão dos diálogos/paginação. Capturas desktop inspecionadas.

**Evidências e limite:** [recibo](orchestracao/.runtime/user-button-colors/resultado.json), [Chrome](orchestracao/.runtime/user-button-colors/browser.json), [definição](docs/43-login-e-administracao-de-usuarios.md#cores-e-alinhamento-das-ações--fe03-auth01-color01). Preview próprio e API interceptada, sem backend/SQL real ou reinício de serviços existentes. Graphify update executado e recusou extração menor, sem `--force`. Próximo: atualizar a página do operador para carregar a apresentação atual.

## FE02-HOME01 — ícone da marca como acesso ao Início, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas.** Dependência FE02/AppShell. Ícone azul do WMS aciona a navegação existente para Início, preservando a guarda de saída, foco e comportamento do menu. Na tela de login, a marca permanece sem ação.

- [x] Tornar o ícone um botão com nome acessível e ação compartilhada com o menu.
- [x] Conferir clique/Enter, menu expandido/recolhido e celular, claro/escuro: dois cenários Chrome aprovados, incluindo fechamento do menu móvel após navegar. Tipagem, lint, formatação, build e dois testes existentes de workspace passaram.

**Evidências:** [recibo](orchestracao/.runtime/brand-home/resultado.json), [Chrome](frontend/evidencias/brand-home-browser.json). Preview isolado com HTTP interceptado; sem backend/SQL real ou reinício de processos existentes. Graphify executado, recusando substituir o grafo por extração menor, sem `--force`. Próximo: carregar a fonte atual na sessão do operador.

## FE03-AUTH01-TEST01 — contas de teste DEV, 10/10/2026

**População DEV concluída e conferida, pedido expresso de Lucas.** Dependências D32/FE03-AUTH01. Após o preparo sem navegador conectado, Lucas esclareceu que a população deve ocorrer no WMS_DEV; o recorte foi executado com WMSDEV, guarda atual de alvo/TLS/permissões/catálogo/histórico e uma transação exclusiva de três contas fictícias. Sem administração de usuários nas contas novas. Supervisor/Operação vinculados ao cliente fictício 64 e armazém 31, ativos e revalidados; Gestor tem alcance operacional geral pela regra existente.

- [x] Conferir contrato de criação e preparar [nomes, e-mails e vínculos](orchestracao/.runtime/usuarios-teste-dev/plano.json), sem senha.
- [x] Criar e reler Gestor, Supervisor e Operação exclusivamente WMS_DEV: ativos, não principais, sem administração, com troca obrigatória de senha.
- [x] Conferir hashes com o codificador Spring real, rejeição de senha incorreta, salts distintos e política de tamanho; gravar senhas cifradas DPAPI fora do repositório e validar recuperação/construção do visualizador local sem exibi-las.
- [x] Registrar três eventos identificados como população fictícia, preservar conta principal e verificar ausência de UPDATE/DELETE/DDL/PROD no executor.

**Evidências e limite:** [recibo real](orchestracao/.runtime/usuarios-teste-dev/20261010-1bbe9c5b/resultado.json), [guarda atual](orchestracao/.runtime/usuarios-teste-dev/20261010-1bbe9c5b/guarda.json) e [procedimento](docs/43-login-e-administracao-de-usuarios.md#contas-fictícias-de-teste-no-wms_dev--fe03-auth01-test01). Criação por SQL parametrizado, sem sessão/JWT forjado e sem atribuir a execução ao administrador principal. Três INSERTs de usuários e três de eventos; duplicidade recusa sem redefinir senhas. Sem reinício dos serviços existentes. Próximo: operador atualizar a lista de usuários e testar primeiro login/troca obrigatória com as senhas do [visualizador local](orchestracao/.runtime/usuarios-teste-dev/20261010-1bbe9c5b/ver-senhas.bat). A releitura SQL não comprova essa jornada de navegador.

Verificações locais no [registro](orchestracao/.runtime/usuarios-teste-dev/20261010-1bbe9c5b/validacao-local.json); sintaxe PowerShell e diferenças documentais conferidas. Graphify update executado e recusou substituir o grafo por extração menor, sem `--force`.

## FE07-ALINHA01 — alinhamento da identificação no Coletor, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas.** Dependência FE02-DS03-A02/FE07. Etiqueta, página e consulta alinhadas na mesma faixa quando houver largura, cabeçalho da operação compacto e quebra pela largura disponível. Sem alteração dos comandos/contratos BE08.

- [x] Ajustar composição e margens locais, mantendo controles de 52 px e ordem de leitura.
- [x] Conferir seis larguras (320–1920 px), dois temas, controles de 52 px, envio por Enter e sete testes existentes do Coletor. Tipagem, formatação/build e dois cenários Chrome aprovados; capturas desktop/mobile inspecionadas.

**Evidências e limites:** [definição](docs/design-system/etapa-03.md#alinhamento-do-coletor--fe07-alinha01-10102026), [recibo](orchestracao/.runtime/collector-align/resultado.json), [Chrome](frontend/evidencias/collector-align-browser.json). HTTP interceptado; não comprova equipamento/backend real. Graphify executado, recusando reduzir o grafo existente, sem forçar. Próximo: carregar o CSS atualizado na sessão do operador; nenhum serviço existente reiniciado.

## FE02-INFO01 — popups informativos e catálogo único, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** explicações por mouse/foco nos KPIs, cartões e mapa, com textos centralizados em `frontend/src/content/information.ts`. Dependências FE02-DASH02/A08–A13, BE14-DASH01/02 e contratos já existentes. Sem nova consulta ou regra de negócio no navegador. Complemento: “i” visual de 18 px, mais contrastante e junto ao título, com área de toque de 44 px.

- [x] Rastrear indicadores/cartões/atalhos/gráficos e criar catálogo único de textos e dados apresentados nos popups.
- [x] Compartilhar atraso de abertura, posicionamento, exclusividade, fechamento e acessibilidade com as posições do mapa.
- [x] Conferir conteúdo, teclado/mouse/toque, tamanhos de tela, temas e regressão dos consumidores: tipagem, lint focal, formatação/build, 24 testes em três arquivos e três cenários Chrome aprovados. Quatro larguras de 320 a 1500 px nos dois temas, toque, limite da janela, atalhos, detalhes e ausência de novas consultas por hover.

**Evidências:** [definição e manutenção](docs/design-system/etapa-05.md#popups-informativos--fe02-info01), [recibo](orchestracao/.runtime/information-popups/resultado.json), [Chrome](frontend/evidencias/information-browser.json). Capturas inspecionadas; regra de revisão do catálogo registrada em AGENTS.md. Graphify executado e recusou substituir 15.220 nós por extração menor; sem `--force`. Preview isolado/HTTP interceptado não comprova backend/SQL real; nenhum serviço DEV/PROD reiniciado. Próximo: conferir a fonte atual na sessão do operador.

## QUAL-CONF01 — qualidade e confiabilidade, 10/10/2026

**Fecho parcial local revisado, demanda expressa de Lucas após reset.** Rodada sobre a fonte suja atual, com histórico preservado e sem retomar POP. Dependências: contratos e entregas existentes BE01–BE16 → FE01–FE13, incluindo D32/D33 e incrementos atuais. [Escopo e limites](docs/06-decisoes-e-pendencias.md#qual-conf01--qualidade-e-confiabilidade-10102026); evidências novas em `orchestracao/.runtime/qual-conf01/`.

- [x] Registrar baseline suja e distribuir posses exclusivas: Cedro backend; Lume frontend/browser; Prumo database/infra; Vigia revisão; Farol canônicos.
- [x] Executar primeira baseline antes de ampliar documentos: frontend 493/493 em 47 arquivos, tipagem/lint/build real aprovados; scripts DEV 9/9 e 7/7; checks independentes de contratos/configuração aprovados.
- [x] Preservar baseline Maven com JDK 21: 836 casos, três falhas e oito erros; ambiente, seleção indevida e helpers de evidência/oráculo diagnosticados. Focal corrigido: 220 casos, zero falhas/erros. JDK 21 existente somente no processo, sem desativar Enforcer.
- [x] Concluir recorte frontend: 500/500 em 48 arquivos, seis casos browser integralmente interceptados, tipagem/lint/build final aprovados e parecer independente favorável.
- [x] Consolidar o primeiro bloco local datado: inventário, 855 casos backend padrão + LoginBrowser H2 separado, build, oito casos D22 atuais e revisão independente por recorte. P01 permanece material.
- [x] Consolidar os incrementos locais PERF01/INIT01/CARGA01, regressão/build das fontes datadas e revisão independente, mantendo fecho geral parcial.
- [x] Fechar relatório parcial com contagens calculadas, versões e áreas executadas ou impedimentos concretos; P01 e validações reais não aceitos por simulação.

**Versão servida — prova inicial datada de QUAL-CONF01:** inspeção de leitura identificou BE PID 50200/JAR `2b94a77f`, fonte correspondente e 549/549 classes iguais à compilação; FE PID 73436, Vite da raiz em modo real. Assets novos correspondem à fonte; o DOM aberto conservava a composição anterior. A única recarga autorizada, após conferir ausência de edição/operação pendente, trouxe o cabeçalho atual e quatro atalhos de 44 px. Sessão continuou acessível. Causa profunda da retenção não comprovada; sem reinício. [Provas](orchestracao/.runtime/qual-conf01/lume/resultado.json).

**Bloco local anterior, parcial e datado:** QUAL-CONF01-A01 corrigiu 401 atrasado que encerrava sessão renovada, com vermelho/verde, build e revisão independente. Backend: verify padrão com 856 selecionados/855 aprovados/zero falhas ou erros/um condicionado; LoginBrowser H2 separado 1/1. Frontend: 500/500 e browser interceptado 6/6. JPA–DDL: 3.695/0, sem SQL. Oito casos locais do runner D22 atual cobriram famílias vigentes; não comprovam Flyway/locks/transporte real. `File.Replace` (P01) continua material: diagnóstico mínimo fictício do helper e .NET não reproduziu, sem correção. Próxima ocorrência: Prumo/Farol, revisão Vigia, uma execução focal com cadeia/HResult e metadados no ponto Replace, sem probe anterior/retries. [Relatório parcial](orchestracao/.runtime/qual-conf01/resultado.md).

### QUAL-CONF01-PERF01 — Início, desempenho e segurança

**Fecho parcial local revisado, ampliação expressa de Lucas na mesma rodada.** Dependências: BE14-DASH01/BE14-DASH02 → FE02-DASH01/FE02-DASH02 e FE02-CTX01. [Autorização e limites](docs/06-decisoes-e-pendencias.md#qual-conf01-perf01--inicio-desempenho-e-seguranca); novas provas em `orchestracao/.runtime/qual-conf01/perf01/`.

- [x] Confrontar metadados atuais com o runtime existente: PIDs/inícios/JAR/config/TLS/ACL compatíveis com a guarda de lançamento WMS_DEV/WMSDEV, sem nova conexão SQL. Permissões/catálogo/histórico/handshake continuam provas datadas de 17:15 UTC.
- [x] Medir uma atualização normal na mesma sessão/origem: 10.541,001 ms até observar `aria-busy=false`, incluindo UI/IPC; não mede TTFB/API/SQL/CPU. Network/DevTools indisponíveis, ResourceTiming recusado por CSP e inventário CUA vazio; sem bypass.
- [x] Reproduzir localmente trabalho excessivo: financeiro H2 24/112/827 statements para 1/12/101 produtos; mapa 14/21/53 para páginas 1/8/40 com a mesma massa. Resultados funcionais preservados antes das assertions estruturais. Frontend: quatro falhas em seis casos demonstraram catálogos repetidos e paginação após erro/cancelamento.
- [x] Corrigir frontend: quatro vermelhos passaram sem mudar assertions; regressão 507/507 em 49 arquivos, focal pós-delta de tabelas 57/57 em seis, browser interceptado 3/3, tipagem/lint/build verdes. Snapshot 19:25:45–19:27:55 UTC, 270 arquivos estáveis incluindo A08/tabelas; pacote preparado, sem promoção.
- [x] Corrigir a origem mínima, preservar alcance/sessão/CSRF/BigDecimal/null, demonstrar antes/depois local e regressão/build final: 865 casos padrão e um LoginBrowser H2 separado atual, sem falhas/erros; cinco contextos H2 reais.
- [x] Confrontar fonte/pacote/runtime, incluindo deltas paralelos A08/tabelas: fonte backend 388 arquivos/113F…, JAR 2C1A… e frontend 270/b086… na prova datada. Confronto posterior de 20:46:58 vincula BE 61588/JAR a8 2C1A e FE 59336 ao recibo 78f; 43BD/50200/73436 são históricos. Vigia 12/12 no vínculo do recibo, sem roundtrip/SQL/performance real.
- [x] Preparar pacote utilizável pelo fluxo local documentado após verify/revisão: execução própria `e64eafb…` terminou com exit 0; dois recibos/JAR/fonte/backups conferidos. Vigia 36 confrontos sem divergência na cadeia 43BD → a8/2C1A → e64/2C1A. Os 103 testes do preparo são subconjunto da regressão, sem somar; nenhum site/SQL reiniciado.
- [x] Reavaliar o custo residual conforme [QUAL-CONF01-PERF01-DEC01](docs/06-decisoes-e-pendencias.md#qual-conf01-perf01-dec01--processamento-adequado-no-banco-10102026): oportunidades SQL/lote e próximo focal temporal documentados; ganho/equivalência adicional ainda não demonstrados. Vigia 16 confrontos sem divergência após corrigir apenas a aritmética do recibo, vermelho preservado.

**Resultado local datado de PERF01, anterior a CARGA01:** mapa 14/21/53 → 13/13/13 statements; financeiro vazio 24/112/827 → 16/16/16; com 1/8/40 unidades, valoração 28/105/457 → 25/81/337 e leituras de origem 2/16/80 → 1/8/40. Valores literais 55,5525/null e 2,50/20/100 preservados. Ainda há consultas por unidade/histórico; [reavaliação DEC01](orchestracao/.runtime/qual-conf01/perf01/cedro/dec01-reassessment.json) explicita oportunidades SQL/lote e equivalência temporal adicional não demonstrada. Não atribuir a demora do portal ao H2 nem chamar pacote preparado de runtime promovido.

**Preparo conciliado por arquivos:** o run `a8c58…`, também registrado na entrega paralela BE02-DEV-ARTEF01-A02 abaixo, produziu nove XML/103 aprovados e JAR `2C1A…`; autoria por PID e exit desse processo não comprovados. O wrapper já iniciado de Cedro terminou naturalmente, executando o run `e64eafb…` de 20:33:45 a 20:37:20 UTC, exit 0. Dois recibos ativos apontam ao JAR novo e os backups integrais preservam a cadeia 43BD → a8/2C1A → e64/2C1A; revisão Vigia 36/36. Não somar os 103 de cada preparo, subconjuntos dos 865 padrão, nem alegar execução global única. Pré-inventário 19:55–20:33 atravessou a troca às 19:57: não atômico. Leitor amplo terminou naturalmente às 21:08:15, exit1, preservando o vermelho de backup e64/a8 versus hash antigo do pré-inventário; 129.109 outros arquivos conferidos sem delta de hash naquele intervalo. Não é corrupção nem snapshot atômico; sourceassert foi datado antes de CARGA01. [Vermelho temporal](orchestracao/.runtime/qual-conf01/perf01/cedro/preparation-confront-temporal-red.json), [linhagem](orchestracao/.runtime/qual-conf01/perf01/cedro/preparation-lineage-proof.json), [revisão](orchestracao/.runtime/qual-conf01/perf01/vigia/preparation-lineage-current-review.json).

**Runtime atual comprovado por metadados às 20:46:58 UTC:** BE 61588 iniciado 20:04:16/JAR a8 `2C1A…`, FE 59336 iniciado 20:04:54, SQL 48480 preservado. Recibo 78f vincula processos/config/perfil/fonte ae6; bytes do JAR iguais ao pacote e64, caminhos distintos. A diferença de 100 ns no início FE entre CIM/recibo está explícita, mesmo microssegundo; não alegar igualdade exata. B097/43BD/50200/73436 são históricos, sem autoria ou reinício inferidos. [Conciliação](orchestracao/.runtime/qual-conf01/perf01/prumo/confronto-final-e64-conciliado.json), [revisão do vínculo](orchestracao/.runtime/qual-conf01/perf01/vigia/runtime-bound-receipt-review.json). Não comprova roundtrip/SQL/benefício real.

**Próximo:** CARGA01 entregue e revisado localmente, com fecho geral parcial; seguir somente os encaminhamentos materiais P01/instrumento e o procedimento posterior do operador. Confronto amplo natural encerrado/revisado8/8, sem preparador adicional pela equipe. INIT01 está favorável no corte datado abaixo. [Limite instrumental exato](orchestracao/.runtime/qual-conf01/perf01/limite-instrumental-farol.json): operador/Farol e responsável Maestri precisam disponibilizar Network documentado na mesma superfície antes de outra medida; sem bypass/nova aba/conexão. Não orientar reinício por premissa de JAR 43BD atual. O [procedimento DEV vigente](docs/19-desenvolvimento-integrado-dev.md#reinicio-dev-exclusivo) permanece disponível ao operador, sem execução pela equipe QUAL; não exige Ctrl+C prévio. P01 segue material, sem correção; preservação por conteúdo/hashes e sem autoria inferida de mudanças externas.

### QUAL-CONF01-PERF01-CARGA01 — implementação do carregamento, 10/10/2026

**Implementação e consolidação concluídas e revisadas localmente; fecho geral parcial.** Autorização expressa de Lucas na mesma rodada. Dependências: PERF01/DEC01, INIT01 e BE14-DASH01/02 → FE02-DASH01/02/CTX01. [Decisão e limites](docs/06-decisoes-e-pendencias.md#qual-conf01-perf01-carga01--implementação-do-carregamento-10102026), [relatório calculado](orchestracao/.runtime/qual-conf01/perf01/carga01/resultado.md).

- [x] Executar baseline focal: H2 1/8/40/200 unidades, 25/81/337/1.617 statements e valores exatos; browser interceptado separado do portal real.
- [x] Ler históricos por lotes de até 500 IDs autorizados, mapas somente na chamada/transação; preservar cadeia futura, BigDecimal/null/pendências, legado e alcance. Retirar fetch da Nota/XML sem uso. Valores/JSON temporal e IDs501 exatos conferidos; Vigia temporal34/34.
- [x] Publicar catálogos independentemente, paralelizar páginas restantes com limite três e habilitar contexto pelas referências escolhidas. Payload/opções/DOM, invalidação após escrita e INIT01 preservados; Vigia frontend v2 28/28, v1 vermelho preservado.
- [x] Concluir regressão/build da fonte final: 64 XML/879 selecionados/878 aprovados/zero falhas ou erros/um LoginBrowser condicionado; browser H2 atual separado 1/1 com quatro jornadas. Frontend 76/76 em nove arquivos e browser16/16 no corte21:11, suplemento wire1/1, tipagem/lint/build verdes. Vigia verify18/18 e browser-métricas26/26; Prumo artefato15/15. Focais/preparos não somados.
- [x] Concluir confronto independente do relatório/canônicos: Vigia 20/20, 101 referências existentes e parecer PARCIAL_LOCAL_FAVORAVEL. P01, instrumento real e SQL Server não atestado impedem aceite integral.

**Ganho da mesma massa final H2:** 1/8/40/200 unidades agora25 statements em três rodadas; 501/dois lotes33, máximo500 binds por statement e501 IDs distintos/exatos. Redução40=92,58%,200=98,45%; uma unidade sem ganho e501 sem baseline anterior. Entidades200final406, iguais à baseline;407 foi apenas focal anterior ao corte da Nota. Tempos/bytes SQL/JSON locais não são latência/transferência/heap SQL Server. [Métricas finais](orchestracao/.runtime/qual-conf01/perf01/carga01/metricas-finais.json).

**Fonte/pacote/runtime separados:** backend388/C25A4209, onze main e um teste alterados,376 preservados; fonte oficial300/bd62f70f. JAR isoladoCD82EFD1,552classes/253DTO/13recursos/565 entradas iguais compilado,semH2. Frontend corte21:11:280/app157/1516fa e sete arquivos529d, Information/INIT01 preservados; mudanças paralelas posteriores não atestadas por esse pacote. Dois recibos ativos seguem ae6/2C1A, não CD82. Última observação runtime20:46:58: BE61588/JARa8 2C1A,FE59336,SQL48480, prova datada;43BD é histórico. [Recibo e artefatos](orchestracao/.runtime/qual-conf01/perf01/carga01/resultado.json).

**Fecho e próximo delimitado:** [parecer geral final](orchestracao/.runtime/qual-conf01/perf01/carga01/vigia/parecer-geral-final.md) favorável localmente, estado FINAL_PARCIAL_LOCAL_REVISADO; operador carrega nova fonte pelo [preparo oficial antes do BAT](docs/19-desenvolvimento-integrado-dev.md#reinicio-dev-exclusivo), sem seleção manual do JAR ou Ctrl+C prévio obrigatório. Nenhum preparo adicional/ativo/restart/SQL real pela equipe CARGA. P01 File.Replace não corrigido: próxima ocorrência fictícia focal única por Prumo/Farol com Vigia captura cadeia/HResult/metadados no ponto Replace, sem probe anterior/retries. Network documentado da mesma superfície depende de Lucas/Farol/responsável Maestri; sem bypass. Graphify tentou uma vez, wrapperexit1/nativeunknown, raw preservado e mapa não atualizado, sem shrink observado/retry/force; não bloqueou QA. [Limites e encaminhamentos](orchestracao/.runtime/qual-conf01/perf01/carga01/resultado.md).

### QUAL-CONF01-PERF01-INIT01 — independência de Início, 10/10/2026

**Favorável localmente, recorte datado; não encerra a rodada ampliada CARGA01.** Dependências: identidade/autenticação e FE02-CTX01 → workspace/catálogos/telas operacionais; dashboard/visão separado da escolha obrigatória de cliente/armazém. [Escopo vigente](docs/06-decisoes-e-pendencias.md#qual-conf01-perf01-init01--independência-de-início-10102026); saídas exclusivas `orchestracao/.runtime/qual-conf01/perf01/inicio-independencia/`.

- [x] Exercitar entrada/rota/reload operacional sem visitar Início e navegação rápida com dashboard pendente/500/sucesso versus catálogo/contexto/auth.fresh.
- [x] Exercitar HTTP H2 autenticado de identidade/catálogo/operacional antes de dashboard, mantendo negativos de sessão/alcance: cinco casos aprovados, zero dashboard/visão; Vigia 21 confrontos sem divergência. Operação vazia e endpoints listados delimitam a prova.
- [x] Demonstrar independência nas rotas/endpoints exercitados e corrigir três mecanismos fresh/stale com regressão/build e revisão Vigia; fonte/pacote/runtime explícitos, sem homologação real presumida.

**Resultado executado:** rotas testadas carregam sem visitar Início; contexto obrigatório continua requisito distinto. Três mecanismos corrigidos: fresh descartava User; comando antigo prosseguia e perdia rascunho/guarda após mudar acesso; segunda consulta antiga concorrente passava comparando com usuário já trocado. Captura do acesso antes de await e revalidação depois recusam ambas antes do envio, sem replay; guarda recusada preserva rascunho, aceite explícito limpa contexto/workflow. 401/403/503 sem operação/fallback REAL. Backend cinco casos HTTP H2/zero dashboard-visão, operação vazia/endpoints listados; Vigia 21/21. Frontend integral 514/514 em 50 arquivos datada 20:41; último corte 20:49:17, 277 arquivos/157 app, agregado 08cca44a; build REAL sete arquivos/fbe9fc91, 71/71 pertinentes em oito arquivos e 12/12 browser, tipagem/lint/build verdes. Vigia 32/32 do corte e 13/13 do recibo. Deltas Information posteriores ficam fora do pacote, preservados sem perseguição/autoria inferida. [Recibo](orchestracao/.runtime/qual-conf01/perf01/inicio-independencia/lume/recibo.md), [parecer](orchestracao/.runtime/qual-conf01/perf01/inicio-independencia/vigia/parecer-frontend-delivery.md). Não prova causa do relato Início nem autorização indevida backend. Graphify INIT recusou 13.421 < 15.220, raw preservado sem force; não bloqueou QA.

## BE02-DEV-ARTEF01-A02 — atualização do pacote local, 10/10/2026

**Concluído localmente, solicitação de Lucas após `DEV02_ARTIFACT_NOT_CURRENT` na execução `afa22fb8500b40f1a57c9acdf6950231`:** pacote atualizado pelo script existente, com 103 testes H2 e zero falhas. Dependência BE02-DEV-ARTEF01; separado da conclusão geral de QUAL-CONF01-PERF01 e da versão em execução.

- [x] Confirmar divergência e ausência de outro processo executando o mesmo preparo.
- [x] Concluir preparo e conferir `login-d32-preparo.json` e `frontend-dev02-preparo.json`: fonte/JAR/configuração correspondentes. Build, JAR e backups em [a8c58ce2a3864a98a9ed920967ac4a33](orchestracao/.runtime/backend-dev-builds/a8c58ce2a3864a98a9ed920967ac4a33/).

**Próximo:** operador executar `iniciar-dev.bat` para carregar o pacote; nenhum serviço real iniciado ou SQL Server consultado neste preparo.

## BE02-DEV-REINICIO01 — reinício exclusivo do DEV, 10/10/2026

**Implementado e validado localmente; integração real pendente da execução do operador.** Dependência BE02-DEV-ARTEF01-A02 → FE integrado real. [Decisão e limites](docs/06-decisoes-e-pendencias.md#be02-dev-reinicio01--reinício-exclusivo-do-dev-10102026).

- [x] Restringir portas a 25580/25581 e comprovar origem por recibo, projeto, PID/início, executável/comando e perfil DEV.
- [x] Preservar outros projetos/portas/PROD, recusar escuta adicional, revalidar todos os donos antes de encerrar, usar handles fixados e serializar inicializações.
- [x] Preservar instância anterior em falha de pré-requisitos/guarda e registrar cada encerramento no recibo da nova execução.
- [x] Validar 27 casos isolados de reinício, 7 de espera, 9 de acesso e 6 de artefato. [Prova dos 27 casos](orchestracao/.runtime/reinicio-dev/51bec53676c24953a3a5e581ed491da3/resultado.json). Sem processos reais encerrados, SQL ou produção.
- [ ] Conferir reinício integrado real na próxima execução do BAT pelo operador.

**Próximo:** executar o BAT normalmente; não precisa fechar previamente a instância DEV anterior. O novo comportamento substitui o encerramento manual citado em históricos anteriores.

**Índice auxiliar:** `graphify update .` executado; recusou substituir o grafo existente de 15.220 nós pela extração de 13.382. Grafo preservado, sem `--force`; [log](orchestracao/.runtime/reinicio-dev/graphify-update.log). Não altera o resultado dos testes do launcher.

## FE02-REG01-A08 — ações compactas nas tabelas, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** corrigir rótulos quebrados e linhas excessivamente altas nas tabelas fiscais, de encerramento e equivalentes. Dependência FE02-REG01-A04 e contratos de ações existentes. [Definição](docs/design-system/etapa-06.md#operações-compactas-nas-tabelas--fe02-reg01-a08).

- [x] Rastrear o componente: operações contextuais ainda exibiam rótulos extensos na coluna estreita reservada a ícones.
- [x] Uniformizar ações contextuais com detalhes/edição, mantendo nome completo acessível e comando original.
- [x] Conferir alinhamento/altura, teclado, destinos, estados e telas menores.

**Evidência:** tipagem, lint focal, formatação/build real, 25 testes e dois casos Chrome aprovados. Cinco visões × quatro larguras × dois temas, com botões compactos/alinhados, títulos/nomes acessíveis, toque, abertura dos formulários e retorno do foco. Capturas inspecionadas. [Recibo](orchestracao/.runtime/table-actions/resultado.json). API interceptada, sem backend/SQL/reinício/publicação; graphify recusou extração menor, sem forçar.

**Próximo:** conferir a apresentação na versão atualizada do frontend.

## FE02-REG01-A07 — comandos alinhados aos filtros, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** colocar Aplicar/Restaurar ao lado dos campos quando houver espaço, em todas as páginas e etapas com o padrão compartilhado. Dependência FE02-REG01 e consultas existentes BE05–BE14. [Definição](docs/design-system/etapa-06.md#comandos-ao-lado-dos-filtros--fe02-reg01-a07).

- [x] Rastrear o padrão e integrar os comandos à mesma faixa dos campos.
- [x] Conferir páginas de cadastro, serviços/cálculo, avaria e entrada, teclado, consulta/restauração e telas menores.

**Evidência:** tipagem, lint focal, formatação/build real, 36 testes existentes e dois casos Chrome aprovados. Seis páginas × três larguras (1500/1024/390) × dois temas: alinhamento, quebra, toque, teclado, aplicação/restauração e ausência de transbordamento conferidos; capturas inspecionadas. Rastreamento de 49 definições em 12 módulos inclui os mapeamentos históricos de Entrada, hoje reunidos numa tela. [Recibo](orchestracao/.runtime/filter-inline/resultado.json). API interceptada, sem backend/SQL/reinício/publicação. Graphify recusou extração menor, sem forçar substituição.

**Próximo:** conferir a apresentação na versão atualizada do frontend.

## FE02-DASH02-A09 — códigos legíveis nas células, 10/10/2026

### FE02-DASH02-A11 — largura distribuída conforme conteúdo, 10/10/2026

**Concluído e conferido localmente, pedidos expressos de Lucas e correção de limite:** aproveitar sobra lateral para reduzir reticências e completar faixas quando útil, ampliando cartões/células. Após apontar barras excessivas em ruas com uma/duas posições, Lucas delimitou a expansão: células com teto conforme conteúdo (192–240 px), sem esticar ruas pequenas pela tela inteira. Priorizar textos cortados antes da sobra restante; manter espaços quando o teto for atingido. Dependências A08/A09/A10 e contrato BE14-DASH02 vigente.

- [x] Medir conteúdo e distribuir espaço livre sem perder encaixe, alinhamento ou navegação. Faixas compartilhadas com cartões de alturas diferentes preservam lacunas já ocupadas.
- [x] Validar exemplos de três/cinco ruas, ruas com uma/duas posições, telas estreitas, temas, retorno ao desktop e alteração de filtros.

### FE02-DASH02-A12 — hierarquia de superfícies no mapa, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** remover alternância de fundos de armazéns e cartões pretos inconsistentes. Armazéns usam uma base neutra uniforme, ruas recebem contraste suave e cabeçalhos/contornos separam níveis. Posições preservam cores semânticas; indisponíveis ganham contorno e texto legíveis. Dependência A08/A11, sem mudança operacional.

- [x] Conferir hierarquia consistente, contraste e responsividade nos temas claro e escuro; capturas inspecionadas.

### FE02-DASH02-A13 — destaque de interação nas posições, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** hover/foco com mudança de tom perceptível e contorno discreto, mantendo verde/vermelho/neutro, sem deslocar células. Transição curta respeita movimento reduzido; hover limitado a dispositivos com suporte.

- [x] Conferir livre/ocupada/indisponível, saída do mouse, foco por teclado, dimensões e movimento reduzido nos dois temas.

**Provas conjuntas A11–A13:** tipagem integral, lint focal, formatação, build real isolado, nove testes de contexto/mapa e dois casos Chrome com API interceptada. Seis larguras de 320 a 1920 px, dois temas e retorno ao desktop; navegação das 39 posições da rua de prova, filtros, popup/detalhes, limites de tamanho e estados de interação. [Recibo](orchestracao/.runtime/map-adaptive/resultado.json). Sem backend/SQL real, reinício ou publicação. Próximo: conferir a fonte atual no frontend em uso; testes isolados não comprovam integração real.

### Entrega A09 preservada

**Concluído e conferido localmente, pedido expresso de Lucas:** ampliar o espaço lateral das posições para reduzir reticências recorrentes. Dependência BE14-DASH02 → FE02-DASH02-A08, sem mudança de contrato ou consulta.

- [x] Ampliar células de 64 para 144 px, com preenchimento lateral de 8 px; preservar encaixe das ruas, prévia e navegação por colunas.
- [x] Conferir códigos, temas, telas pequenas e ausência de transbordamento em 12 combinações: 320/390/768/1024/1500/1920 px, claro/escuro. Códigos de 15 caracteres da massa ficaram completos. Evidência conjunta A09/A10 abaixo.

## FE02-DASH02-A10 — indicação de colunas vizinhas, 10/10/2026

**Concluído e conferido localmente, complemento expresso de Lucas:** mostrar parte da próxima posição com desfoque/degradê para indicar continuidade. Dependência FE02-DASH02-A08/A09 e contrato BE14-DASH02 existente.

- [x] Mostrar recorte da coluna real seguinte e da anterior, por nível, somente quando houver uma posição naquele lado; preservar lacunas.
- [x] Reservar espaço adaptável para a prévia, manter células completas e setas, sem criar controles adicionais ou consultas.
- [x] Conferir limites, navegação completa das 39 posições da rua de prova, prévia, detalhes/teclado e ambos os temas em telas pequenas/grandes. Capturas inspecionadas; altura da prévia acompanha toque de 44 px.

**Evidência A09/A10:** nove testes de contexto/mapa, dois casos Chrome com API interceptada e 12 combinações de tela/tema; build real isolado, lint focal, formatação e tipagem de `src`/`dev` aprovados. Tipagem integral encontrou erros `exact` em `tests/qual-conf01-init01.test.tsx`, arquivo de trabalho paralelo preservado; não atribuídos a este recorte. [Recibo](orchestracao/.runtime/map-width/resultado.json). Nenhum backend/SQL/reinício real ou publicação. Graphify executado, recusou redução de 15.220 para 13.401 nós, sem forçar substituição.

**Próximo:** conferir a apresentação no frontend com a fonte atualizada; correção dos testes paralelos pertence à respectiva entrega QUAL-CONF01-INIT01.

## FE02-DASH02-A08 — ruas com larguras adaptáveis, 10/10/2026

**Concluído e conferido localmente, pedidos expressos de Lucas:** ruas dimensionadas pelas posições, encaixe nos espaços livres, divisões visíveis e popup de informações. Dependência BE14-DASH02 → FE02-DASH02-A06/A07; apresentação com os contratos existentes. [Definição](docs/design-system/etapa-05.md#encaixe-das-ruas-divisões-e-prévia--fe02-dash02-a08).

- [x] Substituir ruas de largura total por blocos de largura própria, limitados ao espaço disponível.
- [x] Encaixar ruas menores nas lacunas disponíveis, com contornos de armazém/rua e título separado das posições.
- [x] Mostrar prévia por mouse/foco, com código completo, localização, estado, capacidade e avisos disponíveis; clique preserva detalhes.
- [x] Conferir tamanhos variados, redimensionamento, níveis/lacunas, setas, teclado, filtros e temas.

**Evidência:** tipagem, lint focal, formatação/build real, nove testes e dois casos Chrome aprovados. Dezesseis passagens em sete larguras de 320–1920 px, claro/escuro; preenchimento da lacuna do exemplo, ausência de sobreposição/transbordamento, todas as colunas, filtro, detalhe/foco e popup nas bordas conferidos. Capturas inspecionadas. [Recibo](orchestracao/.runtime/map-wrap/resultado.json). API interceptada, sem backend/SQL/reinício/publicação; validação separada de QUAL-CONF01.

**Próximo:** conferir a apresentação na versão atualizada do frontend. Manutenção do índice graphify registrada separadamente no recibo, sem substituição forçada.

## FE02-DS01-A04 — setas e desfoque na navegação lateral, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** substituir a barra visível da lateral curta por navegação com setas e desfoque nas bordas. Dependências FE02-DS01/DS03; navegação transversal, sem mudança nos contratos backend. [Definição](docs/design-system/etapa-01.md#setas-e-desfoque-na-lateral--fe02-ds01-a04).

- [x] Setas condicionais ao espaço disponível, limites desabilitados e desfoque apenas na direção com itens ocultos.
- [x] Preservar roda/toque, módulo selecionado, foco, menu minimizado/expandido, drawer e movimento reduzido.
- [x] Conferir tipagem/lint/formatação, builds e regressão local de navegação.

**Evidência:** 25 testes existentes e cinco casos Chrome aprovados; dois temas, lateral de 64/256 px, alturas de 400/600/1200 px, acesso a todos os módulos, retorno do foco, mouse e drawer móvel de 390×560 px. Capturas inspecionadas. [Recibo](orchestracao/.runtime/sidebar-arrows/resultado.json). API interceptada, sem backend/SQL ou reinício/publicação. Preview próprio encerrado; graphify recusou extração menor, sem substituição forçada. Validação deste incremento separada da rodada QUAL-CONF01.

**Próximo:** conferir a navegação na versão atualizada do frontend.

## FE02-DASH02-A07 — ícones da legenda do mapa, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** corrigir os pequenos símbolos desalinhados de Bloqueada/Reservada. Dependência BE14-DASH02 → FE02-DASH02-A06. Cadeado e marcador SVG substituem !/R na legenda e nas posições; rótulos e estados acessíveis preservados. [Definição](docs/design-system/etapa-05.md#ícones-do-mapa--fe02-dash02-a07).

- [x] Uniformizar tamanho e alinhamento, mantendo cada ícone junto do respectivo rótulo.
- [x] Conferir tipagem, lint focal, formatação/build real, oito testes e um caso Chrome em seis larguras e dois temas.

**Evidência:** 12 combinações de 320–1920 px, símbolos da legenda em 18 px e células em 12 px, sem transbordamento; capturas inspecionadas e teclado/detalhes/filtros conferidos. [Recibo](orchestracao/.runtime/map-icons/resultado.json). API interceptada, sem backend/SQL ou reinício/publicação; preview próprio encerrado. Graphify AST recusou substituir o índice por extração menor, sem forçar; manutenção separada.

**Próximo:** conferir os ícones na versão atualizada do frontend.

## FE02-REG01-A06 — botões da seleção de auditoria, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** organizar os botões do painel Auditoria de um registro consultado. Dependências FE02-REG01/FE12 e contrato existente `AuditoriaController.listar`. [Definição](docs/design-system/etapa-06.md#botões-da-auditoria--fe02-reg01-a06).

- [x] Alinhar botões lado a lado com espaçamento e quebra natural, mantendo aviso/consulta de legado no grupo próprio.
- [x] Conferir tipagem, lint focal, formatação, build real, seleção/permissões e disposição em 1500/360 px, claro/escuro.

**Evidência:** oito testes existentes e quatro casos Chrome aprovados; capturas inspecionadas, Tab, IDs longos, alvos de toque e ausência de transbordamento conferidos. [Recibo](orchestracao/.runtime/audit-buttons/resultado.json). API interceptada, sem backend/SQL ou reinício/publicação. Preview próprio encerrado; resultado da tentativa de atualização AST do graphify no recibo.

**Próximo:** conferir visualmente a versão atualizada do frontend.

## FE05-PED01-A01 — posição dos botões da consulta, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** reorganizar os botões da tela Pedidos de entrada. Dependências BE06 → FE05-PED01 e componente de consulta FE02-REG01. Aplicar/Restaurar juntos à esquerda e Atualizar lista à direita da mesma faixa; Novo pedido permanece no cabeçalho. Mobile usa duas colunas e atualização na linha seguinte. [Definição](docs/design-system/etapa-07.md#botões-da-consulta--fe05-ped01-a01).

- [x] Reunir os comandos de consulta e adaptar a disposição ao celular.
- [x] Conferir teclado, carregamento, consulta, temas e larguras 1500/768/360 px.

**Conferência:** tipagem, lint focal, formatação e build real aprovados; 36 testes de recebimento/páginas e quatro casos Chrome com API interceptada. Alinhamento, ordem Tab, toque de 44 px, ausência de transbordamento, pré-requisitos da consulta e atualização por leitura verificados; capturas inspecionadas. [Recibo](orchestracao/.runtime/buttons-a01/resultado.json). Sem backend/SQL, publicação ou reinício da aplicação existente. Preview próprio encerrado; tentativa de atualização AST do graphify registrada separadamente no recibo.

**Próximo:** conferir a apresentação com a versão atualizada do frontend.

## FE02-REG01-A05 — detalhes e formulários compactos, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** reorganizar os dois diálogos enviados e rastrear no frontend blocos vazios, separadores e acordeões desnecessários. Dependências FE02-REG01/A04 e contratos existentes BE05 → FE04, com componentes comuns às demais jornadas. [Definição e inventário](docs/design-system/etapa-06.md#diálogos-e-formulários-compactos--fe02-reg01-a05).

- [x] Corrigir na origem divisórias vazias, repetição de ações/dados, referências sem conteúdo e formulários excessivamente espaçados.
- [x] Rastrear os oito usos de acordeão; preservar os cinco usos com conteúdo operacional útil.
- [x] Concluir conferência de tipagem, lint, builds, fluxos, teclado, temas e larguras 1500/768/360 px.

**Conferência:** 493 testes da suíte frontend e dez casos Chrome aprovados; tipagem, lint, formatação e builds real/fictício aprovados. Navegação das 12 áreas, cadastro/edição/situação, erro, permissões, revisão, repetição de chegada, teclado e descarte pendente preservados. Capturas de detalhes e edição em 1500/768/360 px, claro/escuro, inspecionadas; sem rolagem lateral. [Suíte](frontend/evidencias/dialog-a05-suite.json), [navegador](frontend/evidencias/dialog-a05-browser.json), [recibo](orchestracao/.runtime/dialog-a05/resultado.json). API interceptada localmente, sem integração real/backend/SQL; preview próprio encerrado. Graphify AST recusou substituir 15.220 nós por extração menor de 13.291, sem forçar; avisos de esquema/comunidades registrados e manutenção do índice separada.

**Próximo:** conferir visualmente a apresentação atualizada na aplicação do operador. Nenhum reinício/publicação executado; não há pendência de implementação deste incremento.

## FE02-REG01-A04 — tabelas ajustadas à largura disponível, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** organizar a tabela de Cadastros que exigia rolagem lateral em monitor de 1500 px. Dependências FE02-REG01/A01/A03, preservando contratos BE05 → FE04 e os demais leitores da tabela compartilhada. [Definição vigente](docs/design-system/etapa-06.md#tabelas-ajustadas-à-largura--fe02-reg01-a04).

- [x] Distribuir colunas, quebrar valores extensos e compactar ações com identificação acessível.
- [x] Conferir 1500 px, larguras menores, temas, teclado, detalhes/edição, permissões e outros tipos da tabela.
- [x] Registrar conferência e definição na etapa 06.

**Conferência:** tipagem, lint focal, formatação e builds real/fictício aprovados; 25 testes de páginas por registro e oito casos Chrome com API interceptada. Vinte medições de Clientes em 1200/1280/1400/1500/1920 px, claro/escuro e menu aberto/recolhido: zero rolagem lateral da tabela/página, códigos/documentos longos completos e ações visíveis. Armazéns em três larguras; estoque, saída, contraste, estados, filtro, perfil Operação, detalhes/edição, Enter/Escape e retorno do foco conferidos. Mobile 360 px mantém rolagem somente dentro da tabela e ações de 44 px. Capturas inspecionadas. [Medidas](frontend/evidencias/table-a04-medidas.json), [recibo](orchestracao/.runtime/table-a04/resultado.json). Previews próprios encerrados; sem backend/SQL ou reinício de processos existentes. Graphify AST recusou substituição integral por extração menor, sem forçar; manutenção separada.

**Próximo:** recarregar Cadastros para carregar a nova apresentação.

## FE02-DASH02-A06 — mapa do Início conforme referência compacta, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** organizar o mapa como a imagem fornecida, com células pequenas, ruas separadas, códigos e legenda junto da seleção do armazém. Dependência BE14-DASH02 → FE02-DASH02-A04/A05. Ajuste frontend compartilhado; [definição vigente](docs/design-system/etapa-05.md#mapa-do-início-conforme-referência--fe02-dash02-a06).

- [x] Compactar apresentação desktop, manter toque mobile e recolher filtros/informações secundárias.
- [x] Conferir tipagem/build, testes pertinentes, temas, teclado, detalhes, lacunas e telas pequenas.
- [x] Registrar evidências e definição na etapa 05.

**Conferência:** tipagem, lint focal, formatação e builds real/fictício aprovados; oito testes existentes de contexto e cinco casos Chrome. Doze medições em seis larguras de 320 a 1920 px, claro/escuro: células de 64 × 28 px no desktop e 64 × 44 px no mobile, ruas e níveis alinhados, lacunas preservadas, ausência de transbordamento e todas as 20 colunas acessíveis. Busca/filtro, paginação, erro/vazio, seleção de armazém, bloqueio/reserva simultâneos, Enter/Escape e retorno do foco conferidos. Capturas desktop/mobile inspecionadas. [Medidas](frontend/evidencias/map-a06-medidas.json), [recibo](orchestracao/.runtime/map-a06/resultado.json). Prova local com API interceptada e exercício separado, sem backend/SQL ou reinício de processos existentes. Graphify AST recusou substituição integral por extração menor, sem forçar; manutenção do grafo separada.

**Próximo:** atualizar o Início para carregar a nova composição.

## FE02-DASH02-A05 — separação dos armazéns empilhados, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** alternar fundo suave e fundo normal entre os blocos de armazém, para distinguir cada grupo no empilhamento. Dependência BE14-DASH02 → FE02-DASH02-A04. Ajuste CSS compartilhado; [definição](docs/design-system/etapa-05.md#armazéns-com-fundos-alternados--fe02-dash02-a05).

- [x] Aplicar fundos alternados aos blocos completos, incluindo título e ruas.
- [x] Conferir temas, larguras e navegação existente.

**Conferência:** formatação e build real aprovados. Chrome com API interceptada: quatro armazéns em seis composições, 320/390/1500 px e temas claro/escuro. Fundos alternados, rótulos sem manchas, separação dos blocos, cartões de 44/54 px, ausência de transbordamento e ida/volta das setas conferidos; capturas desktop/mobile inspecionadas. [Medidas](frontend/evidencias/map-a05-medidas.json), [recibo](orchestracao/.runtime/map-a05/resultado.json). Preview próprio encerrado; sem backend, SQL ou reinício de processos existentes. Graphify AST recusou substituição integral por extração menor, sem forçar; manutenção do grafo separada.

**Próximo:** atualizar a página aberta para carregar os fundos alternados.

## FE02-DASH02-A04 — mapa compacto de ruas e posições, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** simplificar ruas/cartões, corrigir o alinhamento e substituir a barra horizontal por setas de anterior/próximo. Dependências BE14-DASH02 → FE02-DASH02/FE02-DASH02-A03. [Definição vigente](docs/design-system/etapa-05.md#mapa-compacto-e-navegação-por-setas--fe02-dash02-a04).

- [x] Compactar ruas, níveis e posições sem esconder bloqueio ou reserva.
- [x] Conferir temas, teclado, detalhes, lacunas entre níveis e telas pequenas.

**Conferência:** tipagem, lint focal, formatação e builds real/fictício aprovados; oito testes existentes de contexto/visão geral. Chrome: dois casos no modo real com API interceptada e três no exercício; 16 medições em oito larguras de 320 a 1920 px e dois temas. Cartões comuns de 44 px contra 64 px anteriores; bloqueados/reservados de 54 px. Todas as 20 colunas do cenário percorridas pelas setas, com limites, redimensionamento, lacunas, detalhes e foco conferidos. Capturas desktop/mobile inspecionadas. [Recibo](orchestracao/.runtime/map-a04/resultado.json), [medidas](frontend/evidencias/map-a04-medidas.json). Previews próprios encerrados; sem backend, SQL ou reinício de processos existentes. Graphify AST recusou substituição integral por extração menor, sem forçar; manutenção do grafo permanece separada.

**Próximo:** atualizar a página aberta para carregar o mapa atual.

## FE02-DS03-A06 — atalhos compactos na faixa do Início, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** atalhos e administração alinhados ao Início, compactos e neutros, com borda fina e maior separação dos indicadores. A orientação final substitui a apresentação colorida de A05. Dependências FE02-DS03-A05/FE02-DASH02-A03; [definição vigente](docs/design-system/etapa-03.md#atalhos-compactos-e-neutros--fe02-ds03-a06).

- [x] Integrar atalhos compactos à faixa de título.
- [x] Conferir temas, teclado, destinos e adaptação para telas pequenas.

**Conferência:** tipagem, lint focal, formatação e builds real/fictício aprovados; 24 testes existentes em quatro arquivos. Chrome: 88 medições em 11 larguras de 320 a 1920 px, dois temas, menu aberto/recolhido e ambos os modos. Altura de 44 px contra 170 px anteriores; textos, bordas, alinhamento, separação, hover, foco, Tab/Shift+Tab/Enter, quatro destinos e administração/ausência para não administrador conferidos. Capturas desktop claro e mobile escuro inspecionadas. [Recibo](orchestracao/.runtime/shortcuts-a06/resultado.json), [medidas](frontend/evidencias/shortcuts-a06-resultados.json). API interceptada e exercício separado; sem backend/SQL real ou reinício de processos existentes. Previews próprios encerrados. Graphify AST recusou substituição integral por extração menor, sem forçar.

**Próximo:** atualizar a página aberta para carregar a apresentação final.

## FE02-CTX01-A02 — contexto na linha do cabeçalho a partir de 1200 px, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** manter cliente, armazém e Aplicar contexto na linha principal a partir de 1200 px. Dependência FE02-CTX01-A01/FE02-CTX01. Ajuste somente CSS; a definição atual está na [etapa 03](docs/design-system/etapa-03.md#contexto-na-linha-principal--fe02-ctx01-a02).

- [x] Ajustar largura e limite sem mudar a seleção de contexto.
- [x] Conferir cabeçalho, ações, listas, temas e menu lateral em larguras intermediárias e mobile.

**Conferência:** formatação e build real próprios aprovados. Chrome com API interceptada: 24 medições em 12 larguras de 320 a 1920 px, com menu lateral aberto/recolhido; textos completos, listas dentro da tela e ausência de rolagem horizontal. Cabeçalho de 64 px a partir de 1200 px; capturas de 1200/1500 px inspecionadas, além da conferência no tema escuro. [Recibo](orchestracao/.runtime/context-header-a02/resultado.json). Preview próprio encerrado, sem backend, SQL ou reinício de processos existentes. Atualização AST do Graphify recusou a substituição integral por extração menor, sem forçar; manutenção do mapa permanece separada.

**Próximo:** atualizar a página aberta para carregar o CSS.

## BE14-DASH01-A01 → FE02-DASH02-A03 — gráficos novamente no Início, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** reincluir os gráficos existentes junto dos dez indicadores e do mapa; mostrar também o agregado com Todos os clientes e Todos os armazéns. Dependências BE14-DASH01/BE14-DASH02 → FE02-DASH01/FE02-DASH02 e FE02-CTX01. Consulta backend com escopo opcional, mantendo regras e permissões operacionais.

- [x] Reutilizar os gráficos nos modos real e fictício, sem duplicar os cartões.
- [x] Conferir montagem no Início, seleção de contexto, atualização, permissões, teclado e telas pequenas.
- [x] Registrar evidências e limites da conferência.

**Conferência:** `verify` com 86 testes HTTP/JPA/H2 isolados, incluindo seis cenários de dashboard e agregação de dois clientes/dois armazéns com SKU igual e saldos distintos; seleção parcial, alcance vazio, restrição e paginação. 491 testes frontend em 46 arquivos aprovados com dois workers após um timeout na primeira execução concorrente; tipagem, lint, formatação e builds real/fictício aprovados. Cinco casos Chrome com respostas interceptadas/exercício, seleção por nome, Long/Bearer, Todos, 403, teclado, mapa e telas pequenas nos dois temas. Capturas inspecionadas. Pacote DEV preparado com recibos, JAR, fontes e contratos concordantes. [Definição](docs/design-system/etapa-05.md#gráficos-no-início-e-opção-todos--be14-dash01-a01--fe02-dash02-a03), [recibo local](orchestracao/.runtime/graficos-inicio/resultado.json).

**Próximo:** operador encerra o console DEV anterior e executa `iniciar-dev.bat` para carregar o pacote preparado, depois recarrega o Início; pode manter Todos. Todos significa alcance permitido, com produtos separados por cliente, SKU e unidade de medida. Sem SQL Server, migrations, reinício de processos existentes ou homologação real nesta entrega; desempenho/dialeto reais e versão em execução permanecem separados. Graphify AST executado, com substituição integral recusada por extração menor (13.283 contra 15.220 nós), sem force; manutenção do mapa permanece separada da entrega funcional.

## POP-DEV01-SQL01 — população direta de WMS_DEV, 10/10/2026

**Concluído e conferido no banco real, orientação expressa de Lucas:** popular por SQL, sem entrar no site. Esta orientação substitui o canal HTTP e a dependência de sessão do POP-DEV01 para este recorte. Dependências BE03/BE05–BE14 → FE04–FE13 e SQL-TLS-ATUAL. Somente WMS_DEV/WMSDEV, dados fictícios identificados por `LUCASDEV20261010`; dados e históricos existentes, usuários, senhas, processos e migrations preservados.

- [x] Guarda atual de alvo real, identidade, TLS, permissões, catálogo e histórico aprovada; inventário de contagens e prefixo sem registros anteriores conferido.
- [x] Inserção transacional de cadastros, entradas, estoque, reservas, saídas, configurações fictícias, contagens e cargas pendentes; origem, quantidades, ocupação e propriedade validadas antes do commit.
- [x] Releitura SQL após commit e recibo com IDs e contagens observados.

**Conferência:** guarda atual com 26 critérios aprovada em 10/10/2026 14:46Z, inventário anterior e prefixo ausente; commit realizado e releitura fora da transação. 579 linhas inseridas em 33 tabelas: 3 clientes, 2 armazéns, 12 produtos/embalagens, 40 endereços/ocupações, 18 entradas (12 efetivadas, 3 rascunhos, 3 em conferência com divergência), 15 notas manuais fictícias, 24 unidades/12.000 produtos, 9 saídas (6 rascunhos, 3 reservadas), 3 reservas, 4 serviços, 2 tabelas/8 preços fictícios, 6 contratos/vínculos/fatos de serviço, 3 contagens reconciliadas e 3 cargas pendentes. 216 registros de auditoria identificam `DEV-SEED-SQL`. Zero inconsistências de composição, origem/condição, ocupação, reserva, propriedade ou datas; deltas de todas as tabelas iguais ao lote, sem alteração de contagens de autenticação ou Flyway. [Recibo e IDs](orchestracao/.runtime/populacao-dev/sql-20261010-1a12647782b/popular-resultado.json).

**Próximo:** Lucas pode recarregar o WMS e selecionar `DEV fictício - Armazém 1` ou `2` para testar. Sem reinício necessário para estes dados. A população SQL não comprova jornadas HTTP/frontend nem homologação fiscal. Sem DDL/grants/DELETE/PROD ou emissão externa. O plano HTTP anterior permanece histórico; esta demanda SQL está entregue. Conferência documental sem erros de diff; Graphify AST recusou reduzir 15.220 → 13.280 nós, mapa preservado sem force.

## BE06-XML01 → FE05-XML01 — novo pedido manual ou XML, 10/10/2026

**Concluído e conferido localmente, pedido expresso de Lucas:** cadastro manual preservado e importação com prévia, associações, confirmação atômica, origem, documento original e prevenção de repetição no mesmo acompanhamento de entrada. Dependências BE06/FE05-PED01/FE02-REG01/D32. Gestor confirmou que os produtos pertencem ao emitente em todos os pedidos; DALGA apenas armazena. [Regra D33](docs/06-decisoes-e-pendencias.md#d33--propriedade-da-mercadoria-na-entrada-10102026), [contrato e limites](docs/45-pedido-entrada-manual-xml.md).

- [x] Leitura segura com esquemas controlados, prévia sem escrita e associação validada pelo backend.
- [x] Confirmação íntegra, duplicidade, revisão da prévia, histórico e origem persistente.
- [x] Ação Novo pedido com manual/XML, dados originais, pendências e mesma conferência física.
- [x] Conferências locais backend/frontend/navegador e limites documentados.

**Conferência:** `verify` isolado com 80 testes de login/visão/arquitetura/recebimento/XML, incluindo HTTP/H2, concorrência, repetição, revisão, rollback e alcance; 488 testes frontend em 46 arquivos; seis casos Chrome anteriores e quatro novos de XML em 1440/360px claro/escuro, teclado, filtros e documento original. Tipagem, lint, build, seis guardas de artefato e inspeção visual aprovados. Ajuste final do arquivo selecionado também revalidado em seis testes de componente e quatro Chrome. [Recibo local](orchestracao/.runtime/entrada-xml/resultado.json). Pacote DEV atual preparado e recibos concordantes, com classes/XSD conferidos. Origem persistida no recibo de criação existente, sem nova migration. Graphify recusou redução 15.220 → 13.280 nós; mapa preservado sem force.

**Próximo:** carregar o pacote preparado pelo `iniciar-dev.bat` após encerrar o console DEV do operador; conferir integração real com as guardas existentes e um XML original. Original do exemplo não localizado; testes usam reconstrução fictícia identificada. Nesta entrega não houve SQL Server, migration aplicada, reinício de processos existentes ou operação PROD; validação local não declara homologação fiscal ou integração SQL.

### BE06-XML01-A01 — avisos de método obsoleto, 10/10/2026

**Concluído localmente:** as duas leituras de `tipoRecurso` em `OrigemEntradaService` e `PedidoEntradaXmlService` passaram de `asText()` para `asString()`. A biblioteca instalada delega o método antigo ao novo; comportamento preservado. Dependência BE06-XML01 → FE05-XML01. Compilação/formatação e 80 testes existentes de login, visão geral, arquitetura e recebimento/XML aprovados em H2 isolado. Pacote DEV preparado e recibos concordantes, sem reiniciar processos ou acessar SQL Server. [Evidência](orchestracao/.runtime/entrada-xml/asstring-resultado.json). Graphify AST recusou redução do mapa, preservado sem force. Próximo: carregar o pacote preparado no próximo reinício normal do operador.

## BE02-DEV-ARTEF01 → FE02-DASH02-A02 — pacote DEV e acesso à visão geral, 09/10/2026

**Concluído no recorte local, pedido expresso de Lucas:** a conta principal recebia 403 na visão geral porque o pacote selecionado pelo launcher não contém `VisaoOperacaoController`, `VisaoOperacaoService` nem a regra da rota. Conferência estática do JAR real e hash do recibo da execução `8fa3c9e6722b4170898dc66aabfa044f`; a fonte atual já oferece a API ao Gestor. Dependências BE02/D32/BE14-DASH02 → FE02-DASH02, sem ampliar permissões. Pacote atual preparado e recibos native/externo atualizados; fontes, hash do JAR e APIs conferidos antes da guarda SQL no launcher.

- [x] Identificar pacote efetivamente iniciado e confrontar as classes/rota com a fonte.
- [x] Gerar pacote atual, testar login principal/escopos em H2 e impedir pacote obsoleto antes da guarda SQL.
- [x] Atualizar recibos do launcher, registrar evidências e separar preparação de versão em execução.

**Conferência:** `verify` com 19 testes de login/visão geral/arquitetura em H2 isolado, seis casos estruturais de artefato, nove de acesso DEV, sete de prazo/progresso e sintaxe do launcher aprovados; recibos concordantes. [Regra e preparação](docs/43-login-e-administracao-de-usuarios.md#acesso-à-visão-geral-e-pacote-dev--be02-dev-artef01), [evidência](orchestracao/.runtime/acesso-visao-a01/resultado.json). Graphify recusou redução do mapa, sem force. Sem SQL Server, migration, mudança de conta/permissão ou reinício pelo agente.

**Próximo:** executar `iniciar-dev.bat` para carregar o pacote preparado e conferir a visão geral real. A01 abaixo liberou as portas após a tentativa de reinício. A resposta sobre propriedade foi recebida em D33; o incremento manual/XML está registrado em BE06-XML01 / FE05-XML01 acima.

### BE02-DEV-ARTEF01-A01 — porta ocupada no reinício, 09/10/2026

**Resolvido no recorte solicitado:** a tentativa `38f4368c40634dcc922e54aba2494e4e` parou antes do SQL com `PORT_OCCUPIED_25580`. PID 69000 confirmado como Java do WMS_DEV/WMSDEV, perfil sqlserver-dev, JAR/hash e início compatíveis com o recibo `b78ab2180bdd4b81aead61b196da2fbf`; processo pai já ausente. Encerrado somente esse backend órfão para viabilizar o reinício solicitado. Portas 25580/25581 comprovadamente livres; nenhum SQL ou novo backend iniciado. [Evidência](orchestracao/.runtime/acesso-visao-a01/porta-liberada.json). **Próximo:** nova execução manual do `iniciar-dev.bat`, com suas guardas normais; ativação e leitura autenticada da visão geral continuam separadas da preparação local.

## POP-DEV01 — massa real para teste de Lucas, 09/10/2026

**Atualização 10/10:** o novo pedido expresso de SQL direto foi executado e conferido no incremento POP-DEV01-SQL01 acima. A dependência de sessão e o plano HTTP descritos abaixo conservam somente o histórico da tentativa anterior.

**Em execução por autorização expressa:** popular exclusivamente WMS_DEV pela API normal com dados fictícios rastreáveis, prefixo `LUCASDEV20261009`. BE05–BE14 → FE04–FE13, dependências BE02/FE02/D32-DEV04/SQL-TLS-ATUAL e contratos atuais das áreas. Reaproveitar ferramentas e aplicação existente após guarda atual; preservar dados/históricos/migrations, conta principal/senhas/sessões, frente visual e processos.

- [x] Guarda atual única WMS_DEV/WMSDEV aprovada em 09/10 local (10/10 01:04Z): TLS validado, 26 critérios, permissões/catálogo/histórico V11; [prova](orchestracao/.runtime/populacao-dev/prumo/guarda-atual.json).
- [ ] Sessão própria legítima protegida e vínculo backend/configuração DEV efetivos, sem senha de chat ou JWT fabricado.
- [ ] Massa via HTTP nas áreas suportadas, com cenários abertos/editáveis; preconsulta para evitar duplicação e leitura posterior exata de IDs/quantidades.
- [ ] Leituras reais por API/frontend, matriz área/cenário/prova/limite, revisão proporcional e guia curto.

**Donos:** Prumo guarda; Cedro único escritor HTTP da massa; Lume cobertura/leituras frontend; Vigia revisão independente; Farol registros centrais. Evidências somente `orchestracao/.runtime/populacao-dev/`. Sem SQL de fixtures/reset/DELETE/DDL/migrations/grants/PROD/saAPI, emissão/cobrança externa, servidor/runtime compartilhado, kill/restart existentes, ETL/publicação/rotina. Falha de transporte interrompe sem repetição cega ou mock. **Impedimento atual de acesso:** portal `WMS - POP-DEV01` criado e conectado a Lume/Cedro, origem HTTPS fornecida por Lucas; após Continuar no túnel, documento vazio e sessão WMS não confirmada. Lucas recarrega e entra interativamente nesse próprio portal, sem senha ao agente. Nenhum dado novo populado/ID inventado ou leitura posterior de negócio; matriz planejada 12 áreas/47 páginas, contagens de banco não observadas. [Recibo parcial](orchestracao/.runtime/populacao-dev-resultado.json). **Próximo:** sessão legítima/perfil/alcance, então lotes HTTP e leitura posterior; nenhuma nova guarda automática. [Escopo](orchestracao/.runtime/populacao-dev/inicio.json).

## FE02-REG01-A03 — filtros visíveis sem sanfona, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** removida a sanfona isolada de Filtros e contexto da consulta nas páginas que compartilham `RecordWorkspace`. Formulário sempre visível com um único título e sem borda interna; aplicação/restauração, espera, permissões e responsividade preservadas. Dependências FE02-REG01 e FE02-REG01-A02; consultas e vínculos BE04–BE14 → FE03–FE13 mantidos.

- [x] Remover abertura/fechamento e títulos/bordas duplicados dos filtros compartilhados.
- [x] Conferir consultas, restauração, teclado, espera, temas e tamanhos de tela.

**Conferência:** tipagem/build real próprio, lint/formatação, [74 testes existentes](frontend/evidencias/record-filters-a03-regression.json) e [seis casos Chrome](frontend/evidencias/record-filters-a03-browser.json) aprovados; filtros com dois/sete campos em 1440/768/360px, ambos os temas, sem vazamento horizontal, aplicação por teclado, campos bloqueados durante espera e valores mantidos após resposta/restaurados ao estado inicial. Capturas inspecionadas. Assets do frontend DEV existente; APIs interceptadas com dados fictícios, sem backend/SQL ou reinício. [Definição](docs/design-system/etapa-06.md#filtros-visíveis-sem-sanfona--fe02-reg01-a03), [recibo](orchestracao/.runtime/record-filters-a03/resultado.json). **Próximo:** recarregar a página para carregar a fonte atual; integração real mantém sua etapa própria.

## FE02-REG01-A02 — alinhamento compartilhado das barras de filtros, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** aplicar o alinhamento a todas as barras semelhantes à imagem de Atualizar lista. Dependências FE02-REG01-A01 e FE02-DASH02-A01. Regra comum em `forms.css` para filtros de registros (incluindo recebimento) e mapa; margem já corrigida nos estilos locais agora centralizada. Contratos e vínculos backend/frontend mantidos.

- [x] Unificar o alinhamento das barras de filtros, preservando a quebra responsiva.
- [x] Conferir os arquivos servidos pelo frontend DEV, temas, teclado e tamanhos de tela.

**Conferência:** formatação, tipagem/build real próprio e [seis casos Chrome](frontend/evidencias/record-refresh-a02-browser.json) aprovados em 1590/768/360px, claro/escuro, nas barras de Clientes, Armazéns e Estoque; alinhamento medido, Enter preserva a busca, toque e página sem vazamento horizontal, capturas inspecionadas. Assets do frontend DEV existente em 127.0.0.1:25581; todas as APIs interceptadas com dados fictícios. [Definição](docs/design-system/etapa-06.md#alinhamento-compartilhado-dos-filtros--fe02-reg01-a02), [recibo](orchestracao/.runtime/record-refresh-a02/resultado.json). **Próximo:** recarregar a página para carregar a fonte atual. Sem backend/SQL ou reinício; processo DEV existente preservado.

## FE02-DASH02-A01 — alinhamento da busca do mapa, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** removida a margem inferior de 8px dos rótulos dos filtros do mapa, alinhando Buscar com Buscar endereço e Mostrar posições. Dependência FE02-DASH02; BE14-DASH02 → FE02-DASH02 mantém contratos e regras.

- [x] Corrigir o alinhamento apenas nos filtros do mapa e conferir ambos os temas em desktop/tablet/mobile.

**Conferência:** formatação, tipagem/build real próprio e seis casos Chrome com HTTP interceptado em 1440/768/360px aprovados; alinhamento medido, Enter/Limpar e quebra de linha no mobile preservados, capturas inspecionadas. [Definição](docs/design-system/etapa-05.md#alinhamento-da-busca--fe02-dash02-a01), [recibo](orchestracao/.runtime/map-search-a01/resultado.json). **Próximo:** carregar o CSS atual na página. Sem backend/banco ou reinício de processos existentes; preview próprio encerrado.

## FE02-REG01-A01 — tabelas, badges e ações, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** tabelas com filtros proporcionais, cabeçalhos e ações organizados; badges com rótulos legíveis e cores semânticas, botões azuis com ícones. Dependências FE02-REG01, FE02-DS01–DS03 e FE05-PED01; contratos BE05 → FE04 e demais leitores preservados.

- [x] Ajustar composição compartilhada, situações e ações nos temas claro/escuro.
- [x] Conferir tipagem, comportamento existente, teclado, contraste e desktop/mobile.

**Conferência:** tipagem/lint/formatação/builds próprios, 43 testes focais, sete casos Chrome de apresentação e duas jornadas existentes de Clientes aprovados; cinco tons e botões com contraste mínimo 4,5:1, desktop/mobile, teclado e permissões. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-06.md#tabelas-badges-e-ações--fe02-reg01-a01), [recibo](orchestracao/.runtime/record-colors-a01/resultado.json). **Próximo:** carregar a fonte atual na página; integração real conserva sua etapa própria. Sem backend, SQL, alteração operacional, publicação ou reinício de processos existentes; HTTP interceptado e dados fictícios nos previews próprios encerrados.

## FE05-PED01 — entrada centrada no pedido, 09/10/2026

**Concluído no recorte local, pedido expresso de Lucas:** as quatro etapas globais de Entrada e conferência foram reunidas em Pedidos de entrada, com criação no cabeçalho e notas/itens, conferência, divergências, histórico e efetivação no pedido persistente. Dependências FE02-REG01, FE02-DS01–DS03, BE06 → FE05. Comandos, revisões, origens, permissões e efeitos backend preservados; sem nova situação manual, migration ou fusão de entidades.

- [x] Mapear todos os comandos e estados atuais e definir seu destino no pedido.
- [x] Implementar lista, criação, detalhe e operações com atualização após confirmação.
- [x] Conferir fronteiras, temas, teclado, mobile e documentar evidências e limites.

**Conferência:** tipagem, lint, formatação e builds real/fictício próprios aprovados; 478 testes na regressão completa, 15 focais e seis casos Chrome com HTTP interceptado, ambos os temas, desktop/mobile, notas múltiplas, criação seguida de nota/XML, identidade/revisão/UUID, histórico, conflito, filtros preservados após erro, teclado e paginação. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-07.md), [recibo](orchestracao/.runtime/receiving-ped01/resultado.json). Graphify recusou substituir 15.220 nós por 13.119, sem `--force`; mapa preservado. **Próximo:** conferir fonte em execução e integração WMS_DEV/desempenho pelo fluxo autorizado e guarda vigente. Filtros complementares usam a página atual; dados gerais não têm endpoint de edição. Sem backend, SQL, migration, reinício de processo existente ou publicação nesta entrega; alterações preexistentes preservadas.

## FE02-REG01 — listas, detalhes e ações por registro, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** 12 áreas, 50 páginas e 171 ocorrências de ações inventariadas e migradas. Dependências FE02-DS01–DS03, FE04–FE12 e contratos BE04–BE14. Lista/fila/consulta principal ao entrar; criação no cabeçalho; detalhe e comandos no registro persistente; edição em diálogo, mantendo etapas operacionais, permissões, revisão, motivo e confirmação. Cada área tem fontes, colunas e comandos próprios, além da estrutura compartilhada.

- [x] Inventariar fontes e ações das 12 áreas e definir seu destino por página.
- [x] Implementar Clientes e expandir as páginas com adaptações específicas.
- [x] Conferir integrações de transporte, estados, contexto, teclado e mobile; registrar evidências e limites.

**Conferência:** tipagem/lint/builds real e fictício, 467 testes da suíte e 25 focais, seis casos Chrome HTTP interceptado, cobrindo as 50 páginas em desktop/mobile, temas, clientes e falhas. [Padrão e limites](docs/design-system/etapa-06.md), [inventário](docs/design-system/inventario-paginas.md), [recibo](orchestracao/.runtime/record-pages-resultado.json). Graphify recusou extração menor; grafo preservado sem force.

**Próximo:** conferir a fonte carregada na aplicação e integração real no WMS_DEV pelo fluxo autorizado com guarda vigente. Sem SQL Server, backend, migrations, emissão/impressão real, publicação ou reinício nesta entrega. Cidade/vínculos de armazém não constam da resposta básica de Cliente; sua inclusão exige contrato/relação definidos, preservando múltiplos vínculos. Demais frentes preexistentes conservadas.

## FE02-CTX01-A01 — espaço dos seletores de contexto, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** ajuste somente visual do topo em Cadastros e demais páginas, preservando seleção de todos os clientes/armazéns e Aplicar contexto. Dependência FE02-CTX01; a implementação funcional CTX01/DASH02 mantém seu estado próprio.

- [x] Remover largura fixa que sobrepunha as colunas, ampliar o espaço e proteger a largura das ações.
- [x] Distribuir o contexto em outra linha no tablet/notebook e empilhar os seletores no mobile.
- [x] Conferir formatação/build e Chrome com API interceptada em sete larguras (320–1440 px), textos completos, listas dentro da tela e temas; capturas inspecionadas.

**Prova:** [medidas](frontend/evidencias/context-spacing-resultados.json), [composição](docs/design-system/etapa-03.md#espaço-dos-seletores--fe02-ctx01-a01). Alteração de apresentação em `context.css`, sem backend/banco ou regra de seleção. Preview próprio encerrado. Graphify recusou extração menor, sem forçar. **Próximo:** atualizar a página em execução para carregar o CSS; nenhuma correção visual pendente neste recorte.

## FE02-CTX01 / BE14-DASH02 → FE02-DASH02 — seletores e visão geral, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** três correções: seleção de cliente por nome, seleção de armazém por nome e Início conforme o texto anexado. Dependências BE05/FE04, BE08/FE07, BE09/FE08, BE12–BE14 e FE02-DS03. Substitui a composição do Início de DASH01, preservando seu contrato/componente. Catálogos e leituras respeitam alcance; IDs permanecem nos contratos. Dez indicadores, todos os armazéns permitidos, mapa paginado, detalhes e endereçamento pelo coletor existente. CTX01-A01 conserva sua entrega visual própria.

- [x] Cliente pesquisável por nome/código no topo e nos campos correspondentes dos formulários operacionais.
- [x] Armazém pesquisável, seleção de todos para a visão geral e contexto compartilhado com o mapa.
- [x] Dez indicadores e mapa ligados à leitura backend, detalhes autorizados, filtros, atualização e destino preenchido sem gravar automaticamente.

**Conferência:** `verify` backend com dez testes locais (arquitetura/JPA H2 isolada/serviço) e artefato próprio; suíte frontend com 441 aprovados e 23 focais finais, tipagem/lint/builds real/fictício. Dezoito casos Chrome com API interceptada e três do exercício fictício, incluindo temas, 320–1920px, nomes/Long/Bearer, filtros/paginação, foco e encaminhamento. Capturas inspecionadas. [Definições e limites](docs/design-system/etapa-05.md), [recibo](orchestracao/.runtime/visao-operacao-resultado.json).

**Próximo:** carregar a fonte atual pelos fluxos autorizados e conferir dados/consultas no WMS_DEV após a guarda vigente. Sem SQL Server, migration, restart, publicação ou comprovação da versão já em execução neste recorte. Desempenho/dialeto reais e validação de armazém/equipamento/fiscal continuam separados. As demais frentes foram preservadas.

## BE14-DASH01 → FE02-DASH01 — gráficos do Início, 09/10/2026

**Concluído localmente, autorização expressa de Lucas neste chat:** contrato agregado de leitura em `backend/` e gráficos abaixo do Acesso rápido: ocupação física, disponibilidade por SKU e fila de saída, com quatro indicadores. Dependências BE04/BE08/BE09/BE14, FE02/FE03/FE08 e FE02-DS03-A05. Permissões, regras operacionais e frentes preexistentes preservadas; nenhuma migration ou intervenção em SQL Server/processos existentes. Séries históricas e cobrança permanecem evolução separada.

- [x] Implementar contrato agregado com escopo validado e disponibilidade apurada no backend.
- [x] Apresentar gráficos e indicadores nos dois modos, com estados de espera/erro/vazio, teclado e responsividade.
- [x] Conferir backend isolado, contratos, frontend e navegador; registrar evidências e limites.

**Conferência:** `verify` isolado com cinco cenários HTTP/H2 do dashboard e cinco verificações de arquitetura; tipagem/lint, 432 testes frontend e cinco focais finais, builds real/fictício próprios. Três casos Chrome no exercício e um no modo real com API interceptada aprovados, incluindo ambos os temas, cinco larguras, foco após paginação, contexto Long, erro/vazio e CSP; capturas inspecionadas. [Definição](docs/design-system/etapa-04.md), [recibo](orchestracao/.runtime/dashboard-dash01-resultado.json). Graphify recusou substituir o mapa por extração menor, sem `--force`. **Próximo:** carregar o backend atualizado pelo fluxo DEV autorizado e conferir a integração SQL Server; esta entrega local não comprova dialeto, desempenho ou versão já em execução. Previews próprios encerrados; frentes A06/PROD e manutenção do mapa conservam seus estados.

## FE02-DS03-A06 — diálogos de senha e usuários, 09/10/2026

**Concluído no recorte local, pedido expresso de Lucas:** Minha senha abre sobre a página atual; cadastro de usuários tem campos agrupados e diálogo destacado. Lucas rejeitou a faixa azul no topo; ela foi removida. Dependências: FE02-DS03 e BE04-AUTH01 → FE03-AUTH01. Validações, permissões e encerramento das sessões após troca de senha preservados.

- [x] Abrir Minha senha como modal sem substituir a página ou perder sua edição.
- [x] Agrupar campos de usuários/senha, destacar superfície e reorganizar ações.
- [x] Conferir teclado, fechamento/foco, espera/erro, temas e tamanhos de tela.

**Conferência:** tipagem final, lint focal, formatação e builds real/fictício aprovados; 14 testes unitários focais e 16 casos Chrome com API interceptada, incluindo cinco larguras, ambos os temas, foco inicial/retorno, edição preservada, senha obrigatória e envio incerto de usuários. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-03.md#diálogos-de-conta--fe02-ds03-a06), [prova](frontend/evidencias/account-a06-corrigido-browser-resultados.json). Preview próprio encerrado. **Próximo:** conferir a página em execução para carregar o ajuste; sem backend/banco ou reinício de processos existentes. Frentes DASH01 e PROD-LAUNCH01 preservadas.

## PROD-LAUNCH01 — launcher Windows de produção, 09/10/2026

**Preparo local testado e revisado; entrega parcial, operação PROD bloqueada:** BE02-PROD01 → FE02-PROD01, dependências BE02/FE02/D32/SQL-TLS-ATUAL. Entrega `iniciar-prod.bat` (grafia recebida `.bar` preservada no pedido), build candidato isolado/servidor frontend estático real, backend perfil PROD estrito e informações para os domínios futuros wms.rodogarcia.com.br e wms-api.rodogarcia.com.br. Nenhuma configuração Cloudflare/DNS/túnel nesta etapa.

Farol BAT/auxiliares e canônicos; Cedro backend/configs PROD; Lume novos build/servidor frontend, fonte visual somente leitura; Prumo portas/canais protegidos/guarda; Vigia revisão independente. Portas locais25590/25591 inventariadas livres, revalidadas no launcher; bind127.0.0.1, sem kill por porta. Canal SQL/AUTH PROD próprio ainda não descoberto no recorte de metadados; não usar material DEV/admin ou fabricar identidade. [Escopo e donos](orchestracao/.runtime/launcher-producao/inicio.json).

**Conferência:** BAT `--preparar` real exit0 no run5bcb; BAT normal real exit40 AUTH PROD ausente no run9626, antes de guarda SQL ou servidores. Root23 focais verdes; frontend17 focais e13 verificações HTTP de assets com upstream sintético, guard de recusa sem SQL. Builds recentes recusaram promoção quando a frente visual mudou. Três achados nos helpers/config efetiva corrigidos e reconfirmados por Vigia; zero material local aberto. Cedro15 cleanup e8 configuração externa finais, sem novo package; versões/REDS originais preservados. Build PASS não comprova startup; complemento03DE inicial histórico, DFB55 corrigido separado. [Recibo final](orchestracao/.runtime/launcher-producao-resultado.json), [revisão](orchestracao/.runtime/launcher-producao/vigia/parecer-final02.json), [uso e suporte](docs/44-launcher-producao-e-publicacao-futura.md).

**Próximo:** aguardar Lucas. Encaminhamento material: responsável de produção definir/provisionar canais SQL/AUTH e principal WMS próprios; então concluir/revisar binding operacional e executar a guarda/subida autorizada. O gate atual não implementa Open/PASS; identidade/permissões/catálogo/histórico PROD e entrada HTTPS continuam não comprovados. Nenhum recurso provisionado aqui ou URL PROD ativa. Não repetir builds por handoff, testar/escrever negócio em PROD ou reiniciar processos existentes. Frente visual ativa preservada; VALID anterior permanece encerrada.

## FE02-DS03-A05 — fundos dos acessos rápidos, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** fundos marcantes e distintos nos quatro cards de Acesso rápido, com base no design system e preservando conteúdo interno/destinos. Dependência: FE02-DS03; ligações backend/frontend dos quatro destinos preservadas.

- [x] Criar variantes de fundo com tokens próprios e geometria da fundação visual.
- [x] Conferir contraste, temas, responsividade, teclado e destinos dos quatro cards.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; seis casos Chrome em seis larguras e ambos os temas, incluindo contraste, hover, teclado e quatro destinos. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-03.md#fundos-dos-acessos-rápidos--fe02-ds03-a05), [prova](frontend/evidencias/shortcuts-a05-final-resultados.json). Preview próprio encerrado, sem backend/banco ou alteração de operações. **Próximo:** conferir a página em execução para carregar as variantes; manutenção do mapa conserva sua frente própria.

## FE02-DS03-A04 — painel de referências da jornada, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** painel Referências da jornada no contexto atual e botão estilizados nas 12 jornadas. Dependência: FE02-DS03; vínculos BE04–BE14 → FE03–FE13 preservados. Escopo visual compartilhado, mantendo seleção explícita, bloqueio durante confirmação e aviso de descarte da edição.

- [x] Aplicar superfície de apoio, hierarquia das referências e botão destacado com tokens dos dois temas.
- [x] Conferir painel vazio/preenchido, teclado, tamanhos de tela e aplicação das referências no formulário.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; cinco testes existentes e seis casos Chrome finais nas 12 jornadas, claro/escuro e cinco larguras. Referências preenchidas e aplicação por Enter conferidas; capturas inspecionadas. [Entrega e limites](docs/design-system/etapa-03.md#referências-da-jornada--fe02-ds03-a04), [prova](frontend/evidencias/references-a04-final-resultados.json). Preview próprio encerrado, sem backend/banco/reinício de processos existentes. Graphify recusou redução do mapa, sem forçar. **Próximo:** conferir a página em execução para carregar o novo painel; manutenção do mapa e validações reais continuam separadas.

## FE02-DS03-A03 — limite de altura dos campos de texto, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** expansão vertical limitada globalmente, incluindo motivo/justificativa de Cadastros → Clientes. Dependência: FE02-DS03; recorte visual compartilhado, preservando valores, limites de caracteres e contratos existentes.

- [x] Aplicar altura máxima global aos campos multilinha, com rolagem interna e redimensionamento vertical limitado.
- [x] Conferir campos de justificativa e XML em desktop/mobile e registrar a regra e a prova local.

**Conferência:** tipagem, formatação dos dois CSS e build fictício em diretório isolado aprovados; oito verificações Chrome de justificativa/XML, 390/1440 px e claro/escuro, com arraste real de redimensionamento, expansão forçada, rolagem interna e ausência de rolagem horizontal. [Regra e limites](docs/design-system/etapa-03.md#limite-de-altura-dos-campos--fe02-ds03-a03), [prova local](frontend/evidencias/textarea-height-final.json). Sem backend/banco; preview próprio encerrado. Manutenção do mapa registrada no mesmo documento, sem forçar redução de fontes.

**Próximo:** atualizar a página já aberta para carregar o limite. As frentes A01/A02 mantêm seus estados próprios.

## FE02-DS03-A02 — composição do Coletor, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** Coletor integrado ao design system. Na conferência, Lucas corrigiu a proposta de centralização: o painel alinha com o título e ocupa a largura útil da página. Dependência: FE02-DS03; vínculos backend/frontend do Coletor preservados. Recorte de apresentação, sem mudar contratos ou regras de leitura/confirmação.

- [x] Alinhar a área operacional à página e organizar tarefa, leitura, destino e confirmação com os tokens existentes.
- [x] Conferir desktop/mobile, claro/escuro, teclado/scanner web e regressão focal do Coletor.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; seis testes existentes e sete casos Chrome no exercício fictício. Temas e alinhamento em 320/390/1024/1440/1920 px; Enter/foco do destino/confirmação em 390/1440 px, capturas inspecionadas. [Entrega e limites](docs/design-system/etapa-03.md#composição-do-coletor--fe02-ds03-a02), [prova](frontend/evidencias/collector-a02-resultados.json). Sem banco/equipamento/reinício de processos existentes; preview próprio encerrado. **Próximo:** conferir a página já aberta para carregar o ajuste. Refinamento da textura A01 interrompido a pedido de Lucas; não retomar automaticamente. A03 mantém seus próprios critérios.

## FE02-DS03-A01 — preenchimento lateral das jornadas, 09/10/2026

**Interrompido por mudança expressa de foco de Lucas:** painel lateral implementado; o refinamento da textura foi deixado de lado para priorizar o Coletor (A02). Dependência: FE02-DS03; componente comum às 12 jornadas, preservando vínculos BE04–BE14 → FE03–FE13. Não há aceite visual da textura.

- [ ] Compor painel decorativo com altura automática no desktop e ocultá-lo no layout empilhado.
- [ ] Conferir mudanças de etapa/ação, temas, teclado, responsividade e deslocamento do topo.

**Próximo:** aguardar nova demanda para esta textura. O fecho geral FE02-DS03 abaixo mantém seus próprios critérios.

## FE02-DS03 — organização interna das páginas, 09/10/2026

**Concluído localmente, por pedido expresso de Lucas:** padronizada a hierarquia de ações, agrupamentos, uso de espaço, paginação e diálogos nas páginas internas. Dependências: FE02-DS01-A03 e FE02-DS02-A01; ligações BE04–BE14 → FE03–FE13 existentes preservadas. Recorte visual frontend; regras, contratos, permissões e conexão com banco preservados.

- [x] Organizar Início e a estrutura compartilhada das jornadas de Cadastros até Consultas/relatórios.
- [x] Padronizar formulários, listas, paginação e confirmações centrais.
- [x] Aplicar organização proporcional ao Coletor e Usuários/acessos.
- [x] Conferir contexto no topo desktop de 64 px, placeholders, ação em verde petróleo e persistência da sidebar, conforme refinamentos expressos de Lucas.
- [x] Conferir tipagem, estados operacionais, teclado, temas e tamanhos de tela; registrar evidências e limites.

**Conferência:** tipagem, lint, formatação dos arquivos alterados e builds real/fictício; 424 testes unitários únicos aprovados por composição de 422 amplos + dois timeouts resolvidos em 18 focais, sem apresentar a rodada ampla como integralmente verde. 33 casos Chrome únicos: 23 de exercício e dez de aplicação nativa com API interceptada; oito casos repetidos qualificam o delta intermediário paralelo da coluna de etapas. Incrementos posteriores A01 mantêm sua própria validação. Capturas conferidas. [Composição e limites](docs/design-system/etapa-03.md), [recibo local](orchestracao/.runtime/design-system-etapa03-resultado.json). Sem integração de negócio real ou equipamentos nesta etapa. Graphify recusou redução 15.220→12.661 e o mapa principal foi preservado, sem forçar.

**Próximo:** atualizar a página já aberta para carregar os ajustes; conferir novas observações de uso somente por demanda. Frentes reais e de ambiente abaixo mantêm seus estados próprios.

## FE02-DS01-A03 / FE02-DS02-A01 — preferência de tema e links de ajuda, 09/10/2026

**Concluído localmente:** A03 substitui a reinicialização automática de A02, conforme [regra atual](docs/design-system/etapa-01.md#temas). A01 restringe o destaque das ações de ajuda ao texto, com sublinhado e foco por teclado, preservando diálogos e alvos de toque. Dependências: FE02-DS01-A02 e FE02-DS02; sem alterar contratos BE04-AUTH01 → FE03-AUTH01. Builds real/fictício, tipagem, lint focal e formatação aprovados; 22 casos Chrome passaram (7 login/API interceptada + 15 preferência/navegação fictícia), com capturas de foco conferidas. Provas `frontend/evidencias/design-system-a03-{login,preferencia}-browser-resultados.json`. Graphify mantém recusa anterior, sem forçar. Próximo: atualizar a página para carregar a correção; composição interna segue na nova FE02-DS03.

## FE02-DS02 — login, topo e rodapé, 09/10/2026

**Concluído no recorte visual local:** login informativo e compacto, ajuda em diálogo sem deslocar o card, área reservada para avisos, mensagens de falha próprias de acesso e ícone de headset. Topo alinhado e rodapé compartilhado com Lucas Andrade, LinkedIn e suporte confirmados nos projetos vizinhos em leitura. Dependências: FE02-DS01-A02 e BE04-AUTH01 → FE03-AUTH01. Contratos e regras de autenticação preservados; sem SQL ou reinício de processos existentes.

- [x] Melhorar composição e orientações do login usando os tokens existentes.
- [x] Compartilhar rodapé com crédito, LinkedIn e contato confirmados nas referências.
- [x] Alinhar identificação e ações do topo nos tamanhos desktop/mobile.
- [x] Conferir tipagem, lint, build, fluxos de autenticação e aparência no navegador.

**Evidência:** [entrega e limites](docs/design-system/etapa-02.md), [recibo](orchestracao/.runtime/design-system-etapa02-resultado.json). Tipagem/lint/formatação e builds real/fictício aprovados; 66 testes focais e 19 casos de Chrome (7 com API interceptada, 12 fictícios). Login sem rolagem entre 320×640 e 1440×720, sem deslocamento por ajuda/erros; capturas claro/escuro conferidas. Mapa Graphify mantém impedimento próprio de atualização, sem força.

**Próximo:** atualizar a página aberta para carregar o visual. Próxima composição de páginas depende de nova demanda; investigação SQL e validações reais mantêm os estados próprios abaixo.

## BE02-SQL-DIAG01 — investigação de indisponibilidade SQL, 09/10/2026

**Em andamento, somente diagnóstico:** Lucas confirmou timeout também ao acessar o SQL Server e pediu investigação aprofundada. Dependências: BE02/BE03 → FE02 e FE02-DEV-CON02. Escopo atual: eventos Windows/SQL, recursos do host, processo/listener e ERRORLOG com acesso permitido; sem nova conexão SQL após a falha, reinício, mudança de servidor/ACL ou acesso a dados de outros projetos.

Há cinco eventos Windows2004 em07–08/10 com memória comprometida acima de99% do limite; PowerShell14–17,55GiB e Python20–21,87GiB aparecem como maiores consumidores. Os eventos de07/10 cercam os quatro erros SQL701 históricos. Serviço/PID atual permanecem ligados desde07/10; essa correlação histórica ainda não explica o timeout de09/10. Evidências em `orchestracao/.runtime/sql-investigacao-20261009-*.json`. Próximo: cruzar os recursos atuais e obter os diagnósticos internos delimitados do SQL; origem dos processos antigos não atribuída.

## FE02-DEV-CON02 — prazo de conexão DEV, 09/10/2026

**Ajuste do cliente validado; inicialização real bloqueada:** por pedido expresso de Lucas, abertura SQL120s, guarda completa240s e andamento a cada10s, por política compartilhada. Sete casos de espera/progresso,20 focais do consumo da guarda e sintaxe de quatro scripts aprovados. A única guarda real nova expirou após120,19s em prelogin/handshake (-2/258), sem consultas ou BE/FE; o prazo maior não resolveu a indisponibilidade. Metadados SQLArg1/ERRORLOG recusados pelo Registro Windows com SecurityException, sem elevacao ou leitura do log.

**Dependências:** FE02-DEV-CON01 e BE02 → FE02. **Evidências:** [resultado e hashes](orchestracao/.runtime/dev-conexao-20261009-con02-resultado.json), [guarda real bloqueada](orchestracao/.runtime/dev-conexao-20261009-con02-guarda.json). Conexão única WMS_DEV/WMSDEV/TLS e servidor preservados; sem nova repetição automática. Graphify recusou redução15220→12622, sem forçar. **Próximo encaminhamento:** obter extrato administrativo do ERRORLOG no intervalo14:40–14:42BRT para identificar a causa; retomar a guarda após encaminhamento concreto. Entrega local do prazo concluída, disponibilidade real permanece em validação.

## VALID-LOGIN-CAD01 — fecho parcial autorizado por Lucas, 09/10/2026

**BE04-COR01-DEV01 → FE03-COR01-DEV01 parcial real revisado:** reaproveitado run existente8fa3c9e6722b4170898dc66aabfa044f, frontend http://127.0.0.1:25581, BE72644/25580 e FE70328/25581 com JAR corrigido99128/configD415/preparo947D. Guarda3293 confirmou WMS_DEV/WMSDEV/TLSMandatory/trustServerCertificatefalse,26/26critérios/permissões/catálogo/V1–V11, uma Open215,5245ms e12SELECTmetadados, zero negócio. SQLPID48480/início18:31:41Z e confiança existente atual conferidos; nenhuma outraBAT/Open/renovação ou intervenção em processo nesta retomada. Relato Lucas preservado; CON02/E7B204 permanecem históricos, sem bloqueioSQL corrente diante destePASS datado.

Lume observou realmente18:46:53–57UTC: statusAPI200, usuários anônimo401, CSS/Inter/foco/clique/Tab/Enter1440+390,24asserções públicas aprovadas sem mock/interceptação. CSRF200/renovação automática403,0pageerror/CSP e1consolemetadata sem texto; causa do403 não inferida. Chrome próprio encerrado, servidores preservados. Deltas visuais externos durante/depois da prova qualificados; DOM/capturas datados, sem freeze geral ou equivalência integral posterior.

**7 locais aprovados preservados/0 critérios integrais operacionais reais:** não disponível à equipe sessão WMS própria autenticada; hash bootstrap não recupera senha. Workspace006, Users007 e demais jornadas protegidas não executados. Login nativo D32/DEV07 existe; não retomar proposta histórica Keycloak/BFF. Link loopback é superfície técnica, modo tunnel/Secure preservado; entrada HTTPS operacional e sessão não comprovadas por status/listener. Long gigante/201null/corrida específica continuam complementos locais, sem injeção no banco/API.

[Recibo atual e limites por001–007](orchestracao/.runtime/login-cadastros-validacao-dev-resultado.md), [JSON/hashes](orchestracao/.runtime/login-cadastros-validacao-dev-resultado.json), [parecer independente](orchestracao/.runtime/login-cadastros-validacao-dev/vigia/parecer-retomada-02.md), [prova pública real](orchestracao/.runtime/login-cadastros-validacao-dev/lume/browser-publico-real02/resultado.json) e [parcial anterior literal](orchestracao/.runtime/login-cadastros-validacao-dev/retomada-20261009T183735Z/historico/login-cadastros-validacao-dev-resultado.md). Prumo ambiente/guarda; Cedro fonte/JAR/API; Lume browser; Vigia confronto independente; Farol consolidação. Nenhum material novo demonstrado, sem homologação operacional001–007/roundtrip de negócio.

**Atualização após disponibilização da conta:** Lucas confirmou login real funcionando; CUA do Farol retornou apps[]/browsers[]/zero abas acessíveis. Não há contradição com sessão existente no computador. [Observação deste acesso](orchestracao/.runtime/login-cadastros-validacao-dev/sessao-disponibilizada-20261009T192151Z/resultado.md); nenhuma senha usada/lida/registrada, nenhuma API/SQL/guarda/app nova. Perfil/alcance da conta ainda não observado, sem autorização de escrita por inferência.

**Fecho por decisão expressa de Lucas:** origem https://1z8126n0-25581.brs.devtunnels.ms/ fornecida pelo responsável; consulta única CUA retornou0 navegadores/abas WMS e resolução `No browser is available`. Sessão/perfil/alcance não observados, sem inferir falha WMS/login/SQL. Lucas relata login funcional e trabalha na frente visual; determinou encerrar sem novas tentativas nem outra ação exigida dele. SQLPASS datado e sete locais aprovados preservados,0/7 critérios operacionais integrais reais. [Fecho autorizado](orchestracao/.runtime/login-cadastros-validacao-dev/origem-https-cua-20261009T194922Z/fecho-autorizado.json). Somente documentação/aviso à equipe; nenhum frontend/backend/estilo/launcher/mapa/processo do usuário alterado, sem sondas/instalação/login/revisões novas. **Rodada encerrada parcialmente; participantes aguardam nova demanda.**

## FE02-DEV-CON01 — timeout do launcher relatado em 09/10/2026

**Diagnóstico local concluído; inicialização real bloqueada:** Lucas apresentou o run `874b0c4b965c4e9f831487a48ec94acd` de 14:27 BRT. Abertura única WMS_DEV/WMSDEV expirou após 30.217 ms em prelogin/handshake (-2/258), sem identidade confirmada, consultas ou backend/frontend iniciados. Serviço SQL Running/PID21812 e listener1433 observados em leitura; isso não comprova resposta SQL. Helper, módulo cliente, SqlClient e hashes TLS coincidem com a guarda aprovada de 12:06 BRT (232 ms); nenhuma causa ou alteração do servidor demonstrada.

**Dependências:** BE02 → FE02, guarda atual de D31-DEV02 e limites D32-DEV04. **Evidência:** [diagnóstico delimitado](orchestracao/.runtime/dev-conexao-20261009-874b0c4b.md). Nenhuma nova conexão, SQL, mudança de aplicação/servidor/runtime ou reinício. **Próximo encaminhamento:** esclarecer se a falha foi observada somente no WMS ou também em outro cliente e obter evidência atual da disponibilidade antes de nova guarda autorizada. Etapas visuais e correções locais concluídas abaixo permanecem concluídas.

## CORR-LOGIN-CAD01 — sete correções concluídas e revisadas localmente, 09/10/2026

**BE04-COR01 → FE03-COR01 concluídos no recorte autorizado:** AUD-CONS-001 a007 corrigidos e revisados independentemente; nenhum achado material local aberto nesta entrega. Login próprio D32/DEV07 e incrementos DS01 preservados. Cedro backend004/005; Lume AUTH/UI e leitores005; Prumo contratos/persistência em arquivo; Vigia revisão; Farol consolidação/canônicos. Atalho006 já corrigido pela frente DS01, com autoria externa preservada e handler atual idêntico por diff.

Provas: frontend253 verdes únicos por composição252+1, tipagem/lint/build e3 testes Chrome com API sintética. Backend66 verdes na fonte MARCO02 e14 novos verdes no delta final MARCO03, separados por versão; builds offline aprovados e JAR não executado. Parecer final confirma os sete critérios. Não é certificação geral de segurança, teste integral da fonte global ou integração real com banco.

[Recibo final e limites](orchestracao/.runtime/login-cadastros-correcoes-resultado.md), [JSON/hashes](orchestracao/.runtime/login-cadastros-correcoes-resultado.json), [matriz individual](orchestracao/.runtime/login-cadastros-correcoes/matriz-problemas-casos.md) e [parecer independente](orchestracao/.runtime/login-cadastros-correcoes/vigia/parecer-final.md).

Fonte/provas datadas: deltas posteriores da frente visual qualificados por comparação/adenda, sem alterar os caminhos revisados dos7critérios; não declarar200fontes atuais globalmente iguais. Geração03 preserva174inputs históricos, incluindo AuditoriaService5801 anterior ao delta0F12; DTO/API/permissão/outputs permanecem iguais por adenda. Mapa AST das correções atualizado semforce/LLM, com backups; comunidades/semântica integral não recalculadas e últimos ajustes visuais externos posteriores qualificados.

Nenhum SQL/banco/HTTPDEV/DDL/migration/PROD/sa/provider novo/alteração servidor/runtime/kill ou restart existente/ETL/Hermes. Auditoria original, FINAL14, TLS01/DEV04, migrations/dados e históricos preservados. Próximo passo: aguardar nova demanda de Lucas; não iniciar aplicação, banco, teste ou macrobloco automaticamente. Homologação real/custo JPA e histórico ambíguo permanecem limites próprios, sem reabrir tarefas antigas.

### Histórico preservado — auditoria e incrementos anteriores

## FE02-DS01 — fundação visual e navegação, 09/10/2026

**FE02-DS01-A02 — detecção automática sem ícone de Sistema:** concluído localmente, por correção expressa de Lucas. Somente o botão circular sol/lua; cada abertura/recarga inicia pelo tema do dispositivo. Escolha manual vale durante o uso e substitui a persistência de A01. Dependência: FE02-DS01-A01; tipagem/lint/builds e 14 casos de navegador aprovados, com inicialização, sistema, teclado, edição e aparência em sete larguras. Login/painel com API interceptada; contratos BE→FE preservados. [Detalhes e limites](docs/design-system/etapa-01.md).

**FE02-DS01-A01 — ajuste solicitado de tema e lateral, histórico anterior a A02:** concluído localmente. Sistema permanece o padrão sem preferência salva; botão circular sol/lua alterna claro/escuro, com retorno ao sistema pelo monitor. “Minimizar menu” na própria lateral recolhe à esquerda de 256 para 64 px, somente ícones; seta direita expande. Dependência: FE02-DS01; contratos BE→FE preservados. Tipagem/lint/formatação/builds e 14 casos de navegador aprovados; recarga, sistema/abas, teclado, formulários preservados, sete larguras e capturas conferidos. API nativa interceptada, sem banco real. [Detalhes e limites](docs/design-system/etapa-01.md). Graphify recusou redução do mapa atual, sem forçar. Próximo passo visual permanece a composição de componentes e páginas.

**Status:** Concluído no recorte visual local. **Dependências:** FE02, FE03 e [design system fornecido](docs/design-system/design.md). Pedido expresso de Lucas: primeira etapa com organização, tipografia, cores, temas claro/escuro, topo e lateral. Implementação visual local autorizada; sem mudança de regras, contratos ou backend.

- [x] Centralizar tokens, fonte Inter local, escala tipográfica e cores semânticas.
- [x] Aplicar tema antes do CSS; a detecção automática e o botão sol/lua seguem o ajuste A02, com escolha manual durante o uso da página.
- [x] Compartilhar topo e lateral entre aplicação real e exercício fictício; conferir menu mobile e teclado.
- [x] Conferir tipagem, lint, regressão pertinente, builds e aparência nos dois temas.

**Evidência:** [entrega, fontes e limites](docs/design-system/etapa-01.md). Tipagem/lint/formatação/builds aprovados; 14 testes de navegador aprovados (12 fictícios + 2 da aplicação real com API interceptada), sete larguras 320–1440 px e revisão de capturas claro/escuro. Regressão inicial374/374; execução posterior401 aprovados/1 falha no teste do novo argumento AbortSignal da frente paralela CORR-LOGIN-CAD01. Não declara check integral atual aprovado nem integração SQL/autenticação real. Graphify recusou redução; mapa oficial preservado.

**Ligação BE → FE:** preserva BE04-AUTH01 → FE03-AUTH01 e os contratos operacionais existentes. **Próximo passo do design system:** componentes compartilhados e composição de páginas em nova etapa. Pendências da frente de autenticação e manutenção do mapa permanecem nos respectivos escopos; detalhes na entrega visual.

## AUD-LOGIN-CAD01 — auditoria concluída com achados; correções aguardam Lucas

BE04-AUD01 → FE03-AUD01, pedido expresso de 09/10/2026. Cedro, Lume, Prumo e Vigia concluíram seus recortes; Farol consolidou. Login próprio D32 e administração de usuários existem; Keycloak/BFF histórico não adotado. Sete achados materiais abertos: um P1 (401 antigo encerra sessão nova) e seis P2 (Long nativo, resposta inválida/sucesso indevido, categoria após normalização, namespace de vínculo, atalho de conteúdo e carregamento após erro). Sem vulnerabilidade crítica/escalada demonstrada; sem aceite geral de segurança ou integração real. [Recibo e propostas](orchestracao/.runtime/login-cadastros-auditoria-resultado.md), [JSON/provas](orchestracao/.runtime/login-cadastros-auditoria-resultado.json).

Matriz individual: 74 contratos HTTP; 1395 instâncias HTTP/frontend, 240 complementos backend e 252 atributos persistentes em 22 entidades, com grãos distintos. Pareamentos JPA-DDL/direitos em arquivo252/252; divergências e limites por fronteira preservados. [Matriz](orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.md), [atributos completos](orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.json) e [parecer independente](orchestracao/.runtime/login-cadastros-auditoria/vigia/parecer.md). Não somar reutilizações/contagens como cobertura ou aceite.

Aplicação somente leitura. Provas novas isoladas e sintéticas; nenhuma correção app, SQL/guarda/JDBC/H2/HTTPDEV/provider/launcher/build backend integral/processo real/segredo/ETL/Hermes nesta auditoria. Fonte inicial411:404iguais/7deltas DEV06/DEV07 datados; nenhum freeze global presumido. Os15focais Origin são DEV06; DEV07 posterior lido separadamente e confiança dos headers/topologia real é limite. TLS01/DEV04, FINAL14, migrations, dados e processos existentes preservados.

Próximo passo: Lucas definir o início das correções propostas; nenhum job, refatoração, auth nova ou processo iniciado automaticamente no fecho. Estado/regras D32-DEV07 abaixo continuam preservados como incremento distinto. Registro do fechoUTC: 2026-10-09T15:53:57.298249+00:00.

### Estado anterior preservado — D32 e demais entregas

## D32 — login e administração de usuários (09/10/2026)

**D32-DEV07 — túnel variável e validação de senha conferidos localmente.** Configuração agora contém somente `modo: tunnel`, sem URL pública fixa. Origem HTTPS deve corresponder exatamente ao destino atual no proxy loopback; alias localhost, CSRF e cookies Secure preservados. Console orienta usar o link atual da porta no VS Code. Trinta e dois testes backend passaram, incluindo três hosts sem reinício, troca de senha/novo login, recusa de outra origem e CSRF; sete testes frontend, lint/tipagem/build e nove verificações PowerShell passaram. JAR `target-auth-d32-dynamic` registrado no launcher. Frontend explica e valida senha diferente da atual e comprimento12–128 antes do envio. Próximo passo: usuário reinicia o BAT uma vez para carregar esta versão; novas mudanças apenas na URL do túnel dispensam reinício. Causa do400 real ainda não confirmada: aguardando mensagem da tela, sem senha. [Resultado e limites](orchestracao/.runtime/login-d32/dev07-resultado.json). BE04-AUTH01 → FE03-AUTH01, mesmo escopo D32.

**D32-DEV06 — acesso pelo Dev Tunnel validado localmente; reinício pelo usuário pendente.** Relato403 em `https://1z8126n0-25581.brs.devtunnels.ms`; diagnóstico sem senha na rota CSRF confirmou `ORIGEM_INVALIDA`, antes da autenticação. Configuração pública em `infra/dev/acesso-dev.json`, origem HTTPS exata, cookies Secure e alias de Origin localhost restrito ao proxy loopback/perfil DEV. Não amplia para outros túneis nem desliga CSRF. BE04-AUTH01 → FE03-AUTH01: 20 testes backend, incluindo HTTP interno de proxy/H2, passaram; login/renovação, cookies Secure, outra origem403 e CSRF inválido403 conferidos. Treze verificações de configuração e sintaxe PowerShell passaram. JAR `target-auth-d32-tunnel` preparado no recibo do launcher. Próximo passo: usuário encerra o console antigo, executa o BAT e entra pelo link do túnel; login real novo ainda não conferido. Os 11 usos obsoletos `asText()` nos dois testes relatados foram substituídos por `asString()`; compilação aprovada. [Resultado e limites](orchestracao/.runtime/login-d32/dev06-resultado.json). Auditoria paralela acima preservada.

**D32-DEV05 — correção de senha e console validados localmente; reinício pelo usuário pendente.** O construtor legado do encoder Spring usava SHA1, enquanto `PrepararLogin.java` gera PBKDF2-SHA256. Algoritmo agora explícito, com regressão que executa o configurador real e fixture independente via JCA. Falha reproduzida antes da correção; 20 testes backend, incluindo navegador/H2, e build frontend aprovados depois. Hash/configuração protegida existentes preservados. Console com quatro etapas, horários/cores, painel de acesso e orientação de falha; favicon SVG respondeu200. Novo JAR `target-auth-d32-loginfix` registrado no launcher. Instância atual do usuário preservada. [Evidências e limites](orchestracao/.runtime/login-d32/dev05-resultado.json). Atualização graphify recusada pela guarda de redução; sem forçar.

**D32-DEV04 — ativação concluída; `iniciar-dev.bat` validado em ambiente real.** V11 aplicada via Flyway somente WMS_DEV; 22 direitos mínimos adicionados ao WMSDEV (311 → 333), com V1–V10 e dados preservados. Guarda atual passou: TLS validado, identidade WMSDEV/WMS_DEV, 68 tabelas/716 colunas e histórico V11 íntegros. Backend real `sqlserver-dev`, migrations desligadas e frontend real subiram pelo BAT no [run ce0bf9dd](orchestracao/.runtime/frontend-integracao-dev-runs/ce0bf9dd46b3423ba148782a52cf212f/launcher.json). Chrome confirmou login desktop/360px, CSS sem violações CSP, API/status200, chave pública200 e acesso anônimo a clientes401. Corrigido CSS de login para folha externa no DEV. `npm run check`: 359 testes, tipagem, lint e build aprovados.

Próximo passo vigente: o usuário encerra seu console atual com Ctrl+C, executa `iniciar-dev.bat` novamente, atualiza `http://127.0.0.1:25581` e entra com a conta principal e a senha já configurada. Não repetir o configurador. A senha real não foi usada nos testes do agente: primeiro login, troca de senha e operações autenticadas em SQL Server ficam para a validação pelo usuário. A ativação DEV04 permanece como prova histórica de banco/subida, não de login autenticado. [Ativação e limites](orchestracao/.runtime/login-d32/dev04-resultado.json).

### Histórico da implementação D32 e impedimentos superados

**BE04-AUTH01 / FE03-AUTH01 — implementação e validação local concluídas; ativação WMS_DEV pendente.** Pedido direto de Lucas neste chat: login próprio WMS, JWT, conta principal `desenvolvedor@rodogarcia.com.br` protegida contra exclusão, painel de usuários, administradores delegados, senhas temporárias e troca obrigatória no primeiro acesso e após redefinição. Dependências: BE04, FE03 e integração D31-DEV02. A autorização de implementação substitui a pendência de escolha AUTH; a proposta Keycloak/BFF anterior não foi adotada. Permissões operacionais por perfil/cliente/armazém continuam no backend. Senha inicial não integra fontes, documentação ou logs.

Evidências: 18 testes backend direcionados e uma jornada automatizada de navegador com quatro verificações completas, HTTP/JPA/H2 efêmero; 359 testes frontend, tipagem, lint e build; 9 verificações de contratos/material fictício e 20 do helper. Capturas desktop/360px e teclado conferidos. A jornada visual detectou e validou a correção da chamada nativa `fetch`. [Resultado local](orchestracao/.runtime/login-d32/resultado.json), [guia e contratos](docs/43-login-e-administracao-de-usuarios.md). JAR próprio gerado; nenhuma conta real criada. Atualização graphify recusada pela proteção contra redução do grafo; mapa anterior preservado, sem `--force`.

Próximo passo: obter liberação específica para nova verificação e aplicação da V11 e dos 22 direitos mínimos WMSDEV exclusivamente em WMS_DEV, seguida da jornada integrada pelo `iniciar-dev.bat`. Configuração inicial protegida concluída pelo usuário. Migration, contrato e launcher preparados; V1–V10 e oráculo D29 preservados. Testes locais não comprovam integração WMS_DEV; BE04-AUTH01/FE03-AUTH01 permanecem em validação de ambiente até essa evidência.

**D32-DEV03, diagnóstico em 09/10/2026:** execução manual do usuário [8a778edc](orchestracao/.runtime/frontend-integracao-dev-runs/8a778edc4be045ffb83c4b3fbb24d98a/guarda.json) confirmou presença do material de login, mas a abertura SQL expirou em 5,26 segundos, código -2/258, fase prelogin/handshake; zero SELECT e BE/FE não iniciados. Inspeção do Windows constatou serviço MSSQLSERVER ligado e listener1433/PID21812; isso não comprova conexão SQL funcional nem a causa do timeout. Cliente ajustado de 5 para 30 segundos, mantendo conexão única, TLS validado e zero retries; launcher agora explica a classe do bloqueio. Sintaxe e 20 testes isolados do helper passaram. Nenhuma nova conexão foi executada pelo agente neste diagnóstico; nova verificação e ativação DEV aguardam liberação específica apresentada ao usuário.

Organização D32 em 09/10/2026, a pedido de Lucas: configurador movido da raiz para `infra/auth/configurar-login-dev.bat`; chamada do PS1 e orientação do launcher ajustadas. Caminhos e sintaxe conferidos estaticamente, sem executar configuração de senha ou banco. O bloqueio relatado `AUTH_NATIVE_CONFIGURATION_REQUIRED` indica ausência da configuração protegida local; a ativação SQL continua pendente conforme o próximo passo acima.

## Histórico anterior à autorização de login D32

## Resultado atual D31-DEV02-TLS01 — SQL/TLS aprovado; AUTH pendente

Handoff expresso de Lucas absorvido por Farol em 09/10/2026. Provas do apoio Codex: guarda WMS_DEV/WMSDEV aprovada em 2026-10-09T11:56:41.5843363Z, 26/26 critérios; SqlClient e JDBC PASS, TLS validado (`Encrypt=true/Mandatory`, `TrustServerCertificate=false`), zero alterações SQL. O helper atual corresponde ao SHA D2FF82F7241F448FA5FA836A732A1F9CB2ECE2137B6735DAFDBD1AD4F06B35A9. A baseline D29 de permissões/catálogo/histórico foi preservada. [Provas e proveniência](orchestracao/.runtime/frontend-integracao-dev-tls-handoff-20261009.md).

SQL/TLS resolvido conforme estas provas datadas; pin/recibo antigos e recusas anteriores ficam históricos, sem inferir alteração servidor/sa. Regra permanente em [AGENTS.md](AGENTS.md): confiança corresponde ao processo atual; renovação segura autorizada, sem recaptura automática. Guarda normal futura exige atualidade e pertence à execução integrada após resolução AUTH, não ao recebimento deste handoff.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, Keycloak/BFF não aprovado. Backend/API não iniciados; URL integrada=null, roundtrip=false, entrega integrada aberta. Mapa anterior preservado, atualização estrutural pendente após rejeição; nenhum mapa/SQL/guarda/JDBC/build/teste/processo executado por Farol nesta absorção. FINAL14 local aprovada; D30 geral aberta, aceiteLocalIntegral=false; C06/C07/C10 requeridos impedidos históricos preservados. [Recibo vigente](orchestracao/.runtime/frontend-integracao-dev-resultado.json).

## Histórico preservado — conteúdo anterior à absorção Farol TLS01

## Apoio atual D31-DEV02-TLS01 — certificado renovado e guarda real aprovada

Em 09/10/2026, por pedido direto do responsável para ajudar Hermes WMS, o Codex deste apoio renovou a confiança privada pelo helper existente do runtime. O recibo ainda era do PID65088; o SQL atual PID21812 apresenta outro certificado fallback. SqlClient e JDBC13.4/JDK21 passaram em leituras reais WMS_DEV/WMSDEV, com TLS validado. A guarda cliente foi corrigida para verificar recibo/PID/início/hashes/validade atuais antes da abertura e confirmar o processo depois, preservando a baseline D29 de direitos/catálogo/histórico. Guarda completa em 11:56:41UTC:26 critérios aprovados,64 tabelas/687 colunas,V1–V10 e permissões conferidos; uma abertura e12SELECTs de identidade/metadados, zero negócio/DML/DDL.

TLS resolvido neste recorte; AUTH dos usuários da aplicação continua pendente, sem backend/API/URL integrada. Nenhum sa/PROD/grants/servidor/serviço/trust global/migrations alterado. [Relatório e limites](orchestracao/.runtime/tls-apoio-hermes-wms-20261009/LEIA-HERMES-WMS-CERTIFICADO-SQL.txt), [guarda atual](orchestracao/.runtime/tls-apoio-hermes-wms-20261009/certificado-sql-wms-guarda.json). Encaminhamento direto ao Hermes WMS/Farol indisponível neste chat externo; relatório e provas deixados no runtime WMS para a equipe. Não repetir rodadas encerradas nem considerar a integração concluída por este apoio.

## Histórico preservado — estado anterior ao apoio TLS01

## Resultado atual D31-DEV02 — nova guarda SQL bloqueada TLS; AUTH separada

Após relato Lucas “sql server voltou”, Prumo verificou canal próprio independenteAUTH: iníciohelper2026-10-09T11:30:14.2409217Z,Open2026-10-09T11:30:16.2451044Z,fim2026-10-09T11:30:16.5307190Z,PID29060,UMAOpen258.9705ms/exit20 filho capturado. TLS0x800B0110/native-2146762480 faseABERTURA antesSELECT; queries0/handshakefalse, alvo/identidade/permissões/catálogo/histórico não confirmados. Relato não éPASS nem comprova estado sa/disponibilidade geralSQL. [Resultado](orchestracao/.runtime/frontend-integracao-dev-retomada-sql-20261009.md), [guarda](frontend/evidencias/frontend-prumo-dev02-guarda-retomada-20261009.json).

Interrompido sem segundaOpen/BE/API/proxy/sa/PROD/bypass/TLSglobal/DDL/migrations/grants/servidor/restartkill/ETL/Hermes. Helper1376 antes/depois igual e credencialWMSDEV protegida em memória descartada; segredo não publicado. Vigia conferiu recibos em leitura sem novaSQL. Responsável TLS/SQL apresenta encaminhamento seguro para a recusa atual; causa/peer não comprovados, nenhum ajuste compartilhado ou nova sonda automática.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, independenteSQL; Keycloak/BFF NÃO aprovado nem criado. Nenhum backend/URL integrada sem autenticação real adequada. Mesma demandaDEV02, sem missão duplicada; auditoriaSA/FINAL14/MARCO03/históricos preservados. FINAL14 local aprovada; D30geralaberta, aceiteLocalIntegral=false,C06/C07/C10 requeridos impedidos históricos. RegistroUTC2026-10-09T11:38:25.736665Z.

## Histórico preservado — estado anterior à nova verificação SQL

## Resultado atual D31-DEV02 após AUD-SQL-SA01 — BAT real40; decisão AUTH e TLS pendentes

BAT WMS real executado após auditoria em 2026-10-09T02:22:04.2682459Z: exit40 AUTH_CONFIGURATION_MISSING das três referências públicas; SQLGuard/BE/FE=false, URL=null, roundtrip=false, sem mock. Portas/proprietários observados iguais antes/depois; nenhuma nova Open ou alteração do SQL Server. [Run atual](orchestracao/.runtime/frontend-integracao-dev-runs/e169e141b28d4537b02a2a33cd9a8463/launcher.json), [consolidação](orchestracao/.runtime/frontend-integracao-dev-apos-auditoria-adenda.md).

Referência EXATA dashboards-etl examinada por Lume:14 fontes técnicas/10WMS e24 hashes conferidos. Organização BE→readiness→FE é aproveitável; login próprio HMAC daquele projeto não corresponde a ResourceServerRS256/JWK WMS. Nenhum launcher alheio executado ou env/conexão/conta/credencial/segredo/controle copiado; não portar killporporta/limpalogs/Securefalse/CSRFdisable/replay401. Keycloak/BFF NÃO aprovado nem criado. Nenhuma fonte própria OIDC localizada nos canais examinados; ausência delimitada, não universal. Não há loaderfix demonstrado.

Decisão material ÚNICA continua D31-DEV02-AUTH-DECISAO01: proposta Keycloak DEV exclusivoWMS+BFF, dono de identidade designado, API RS256 preservada. Pedido exige decisão antes provider/servidor novo ou mudança de autenticação. Sessão operacional FE/catálogos/adapter ainda necessários após contrato aprovado. TLS SQL é impedimento independente0x800B0110 antesSELECT na única Open; sem causa/peer comprovado e sem segunda sonda. Responsável TLS/SQL apresenta encaminhamento seguro; nenhuma mudança servidor/trust global autorizada. [Proposta](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md).

AUD-SQL-SA01 encerrada em leitura: D26identidadeWMSDEV/direitos eD27V10 históricos comprovados; nenhuma mudança sa/senha/authmode demonstrada no recorte. SQLPID65088→21812/início07out20:57:17.273Z sem autor/causa. ERRORLOGnegado/18456state/reason desconhecidos; operador autorizado obter extrato e flags pela sessão Windows existente, semsenha/novaSQLagente/reparo. [Auditoria](orchestracao/.runtime/sql-auditoria-login-sa-resultado.md).

Mesma DEV02 aberta, não novo macrobloco. Sem repetição FINAL14/suítes antigas, Keycloak/provider, SQL/sa/PROD/DDL/grants/restartkill/ETL/Hermes. Lume referência; Prumo OS/registro; Cedro execuções/AUTH; Vigia revisão; Farol BAT/canônicos/consolidação. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Registro UTC 2026-10-09T02:26:22.791284Z.

## Histórico preservado — auditoria SA e contexto DEV02 anteriores

## Resultado atual AUD-SQL-SA01 — auditoria encerrada em leitura; causa sa não determinada

Efeitos históricos WMS comprovados: D24 validação administrativa sa; D26 criação de login/usuário/direitos WMSDEV; D27 V10 WMS_DEV. Prumo por atribuição documental, operador Windows não individualizado. Nenhuma execução/efeito de mudança de sa/senha/authmode demonstrado no recorte; não é garantia universal. Processo SQL mudou: PID65088 baseline07out20:38:51Z → PID21812 criado07out20:57:17.273Z, sem autor/causa identificados. Registro gravado LoginMode2/ForceEncryption0/certificado vazio sem baseline comparável, não flags sa/peerTLS.

18456 sa/state/reason atuais indisponíveis: Parameters/ERRORLOG negados e Windows sem evento utilizável; não significa ausência de falhas. Próximo passo do operador já autorizado: extrato nativo sanitizado e consulta única de flags pela sessão Windows existente, sem senha no chat/nova conexão do agente/reparo. ZERO SQL/Open/sa/DPAPI/BE/API/grants/servidor/restart/kill na auditoria. [Resultado](orchestracao/.runtime/sql-auditoria-login-sa-resultado.md), [fontes/hashes](orchestracao/.runtime/sql-auditoria-login-sa-resultado.json).

Lucas determinou retomar a MESMA DEV02 após os achados, confrontando somente referência técnica dashboards-etl. Keycloak/BFF não aprovado; integração ainda exige BAT real, guardas WMS_DEV/WMSDEV/TLS e roundtrip. FINAL14 e demais históricos preservados. Fecho UTC 2026-10-09T02:20:38.625201Z.

## Contexto anterior preservado — D31-DEV02 ainda pendente

## Resultado atual D31-DEV02 — integração real pendente; decisão AUTH e impedimento TLS atuais

**Run real de Lucas:** iniciar-dev.bat em 2026-10-09T01:14:35.6191414Z → exit40 AUTH_CONFIGURATION_MISSING das três referências OIDC. Guarda SQL não invocada; backend/frontend não iniciados; URL integrada e roundtrip inexistentes. [Recibo observado](orchestracao/.runtime/frontend-integracao-dev-runs/7483449c3bcf41608496bc8eb35e3982/launcher.json).

**Origem AUTH investigada:** Prumo/Cedro verificaram presença apenas: 9/9 ausentes em Process/User/Machine, zero valores publicados. Fontes públicas WMS definem ResourceServer RS256/Bearer e placeholders; canais locais consultados são SQL. Nenhuma fonte OIDC própria previamente aprovada/configurada foi localizada nesses escopos/fontes; isso não prova ausência universal corporativa. Não há correção de carregamento demonstrada. O frontend real ainda precisa da sessão operacional, além das três referências.

**Uma proposta, não aprovação:** Farol propõe Keycloak DEV exclusivo WMS, gerido por responsável de identidade designado, com sessão BFF same-origin no backend; tokens/secret fora do navegador, API RS256/Bearer stateless preservada. Provider/contas/BFF não criados. Lucas precisa decidir este arranjo antes de provisionar ou mudar autenticação, como exigiu no pedido. [Descoberta e proposta](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md), [fontes/hashes](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.json). Não pedir três valores técnicos soltos nem usar identidades de outros projetos.

**SQL/TLS independente:** única Open atual em 2026-10-09T00:51:02.8522544Z, PID32144, 454,7449ms, recusou TLS0x800B0110 antes SELECT. Alvo real/identidade/permissões/catálogo/histórico não confirmados; zero SELECT/API/backend. PEM/DER configurados no cliente não são peer atual nem comprovam causa. Sem segunda abertura/bypass/recaptura. Responsável TLS precisa apresentar correção segura; aprovar AUTH não resolve nem libera automaticamente SQL. [Guarda atual](frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json).

**Preparo local revisado, entrega integrada aberta:** launcher único REAL/default, frontend sem fallback, helper/readiness BE e guardas foram implementados e revisados. Vigia encerrou achados locais001–003; FE CORE01 tem188 fontes/63 focais, tipo/lint e dois builds locais; browser estático4, sem API. Cedro20 focais isolados; launcher AUTH4 green e cleanup10 green. Estas provas não comprovam autenticação ou WMS_DEV real. FINAL14 não foi repetida. Só arquivos/recibos próprios dos cinco WMS; sem agentes/conexões novos, Hermes ou ETL. Nenhuma alteração de fonte frontend após CORE01 por este registro.

Farol: launcher/canônicos/recibo/mapa/nota; Cedro: backend/config/readiness/AUTH origem; Lume: frontend/sessão/cliente; Prumo: guarda/TLS/canais; Vigia: revisão independente. Processos históricos5178/5188/5189 preservados; não são URL integrada desta entrega. Após decisões e pré-requisitos seguros no escopo autorizado, será necessário executar BAT real e comprovar login/consulta browser→API→WMS_DEV/WMSDEV. Sem PROD/sa/DDL/migrations/grants/servidor/sharedruntime/killrestart existentes/publicação/commit/push/ETL/rotinas.

Recibo vigente: [JSON](orchestracao/.runtime/frontend-integracao-dev-resultado.json), [MD](orchestracao/.runtime/frontend-integracao-dev-resultado.md). Leitura mínima: AGENTS → states → docs/09 → docs/19 → recibo → proposta AUTH/guarda e parecer conforme pertinência. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos históricos. Base frontend MARCO03 local preservada; não equivale ao sistema inteiro ou integração real. Registro UTC 2026-10-09T01:30:19.545206+00:00.

## Histórico preservado — fecho local anterior e limite DEV01

## Resultado atual D31 — base local concluída e revisada; integração real impedida

FIM_FRONTEND_LUCAS_20261008: base React/TypeScript funcional concluída e aprovada na revisão local integrada de Vigia. Fonte SHA256 C58B64105A7B3232F314D56F8150CCBD6CE1530B6FE4D33837BA46AD3DD4F922 (179 arquivos); tipagem/lint/build, 299 testes locais e 16 Chromium, todos exit0. As 22 parcelas essenciais usam provas compartilhadas; smokes contratuais não equivalem a jornadas integrais. Organização, legibilidade, acoplamento e testabilidade revisados; CSS preto/branco/azul efetivamente aplicado no navegador desktop/coletor. [Resultado JSON](orchestracao/.runtime/frontend-resultado-lucas-20261008.json), [MD](orchestracao/.runtime/frontend-resultado-lucas-20261008.md), [parecer](orchestracao/.runtime/frontend-vigia-parecer-lucas-20261008.json). Registro UTC 2026-10-09T00:08:45.382787+00:00.

**Abrir frontend:** http://127.0.0.1:5189, processo próprio Lume PID19856, exercício fictício. Vínculo de fonte e observações em [recibo de execução](frontend/evidencias/frontend-lume-exercicio-marco03.json). npm run dev usa vite.dev.config.ts: [guia](frontend/docs/desenvolvimento-dev.md). 5178/5188 preservados; 5188 é histórico com FE-VIG-008 e não é a versão corrigida. API/SQL, provider/contas/tokens/CORS reais, usuários sem rota, equipamentos/fiscal/piloto permanecem pendentes; aprovação local não homologa o sistema inteiro ou FE13 real.

**DEV01 real BLOQUEADO G01:** falta prova atual de resolução segura D29. [Guarda Prumo](frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json): guardaRealAprovada=false. Canal WMSDEV conferido apenas por metadados; zero leitura de segredo/DPAPI, SQL/SELECT/API/backend/fixtures. Configuração DEV preparada, modo real fechado. Responsável SQL deve fornecer evidência atual; depois avaliar nova guarda WMS_DEV/WMSDEV/TLS/permissões/catálogo/histórico. Nenhuma sonda/fallback ou intervenção em servidor/acessos/runtime compartilhado autorizada por este fecho.

Lume: integração frontend/checks; Cedro: FE12 e preparo mínimo backend DEV; Prumo: CSS/DEVconfig/guarda; Vigia: revisão independente; Farol: canônicos/recibo/mapa/nota. Fonte congelada, bloco local encerrado, somente exercício fictício próprio aberto. Aguardar demanda/decisão ou prova segura SQL; sem retomada automática. FINAL14 local preservada/aprovada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Históricos/IDs/checksums preservados, sem repetir backend/FINAL14 ou tarefas reconciliadas.


## MAN01 — proteção do versionamento, 08/10/2026

Concluído por pedido direto de Lucas: `.gitignore` ampliado para runtime, credenciais locais, dumps, relatórios automatizados e snapshots/pacotes de ensaios. Retirados apenas do índice 274 arquivos gerados: 80 fixtures (incluindo 43 CLIXML) na primeira entrega e 194 artefatos adicionais na revisão aprofundada; todos preservados no disco e no histórico existente. Conferido com `git check-ignore`, `git ls-files -ci --exclude-standard` (zero restante) e `git diff --check` no ignore. Código BE/FE, migrations oficiais, lockfile e `.env.example` permanecem versionáveis; configuração Spring local sem segredo continua fonte do projeto. Sem aplicação, banco, commit ou push. Fase e pendências BE/FE abaixo permanecem próprias.

Revisão aprofundada solicitada em 08/10/2026: inventário e varredura por padrões de segredos no conteúdo elegível, no índice e nos três commits alcançáveis localmente; última varredura completa cobriu 1.306 arquivos de texto e 49 binários inventariados antes da exclusão final das capturas PNG geradas. Evidência sanitizada em `.runtime/git-audit-report.json`, complemento de literais históricos em `.runtime/git-audit-history-literals.json`. Alertas de texto correspondem a variáveis/parâmetros, placeholders Maven, fixtures explícitas e senha de truststore da fixture HTTPS gerada; nenhum segredo real confirmado pelos padrões examinados. Incluídos snapshots antigos, configurações Maven geradas, JSONs de execução, HAR, bases locais, material de chave e JAR com extensão histórica (78 MB). Limites: ferramenta de padrões local, sem gitleaks/trufflehog instalados; binários originais de requisitos e fixture XLSX não tiveram inspeção integral de conteúdo, objetos inalcançáveis/reflogs e histórico remoto não consultados. Exclusão do índice não expurga os 43 CLIXML históricos. Próximo passo deste item: revisar o diff pretendido antes de commit/publicação; não tratar a varredura como garantia absoluta de ausência de segredos.

## D31 — frontend funcional autorizado por Lucas em 08/10/2026 (registro intermediário preservado; resultado atual acima)

**D31-DEV01 — incremento expresso de Lucas; integração real BLOQUEADA em G01:** “ja deveria juntar o frotend modo run dev com backend e sql server _DEV sim para ser possivel testar enquanto esta sendo feito as atualizacoes”. Autorizado preparar desenvolvimento integrado frontend run dev + backend + SQL Server exclusivamente WMS_DEV, identidade própria WMSDEV restrita. Prumo precisa comprovar resolução segura atual do incidente D29 antes da nova guarda mínima real de alvo/identidade/TLS/permissões/catálogo/histórico. A autorização, listener ativo e provas antigas não comprovam recuperação. [Guarda atual](frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json), SHA256 9135BC92A2C1BF3DF012D47B8EA19E196B835D0A153F840F10D3AA34F15C9005: guardaRealAprovada=false; nenhuma prova atual segura localizada nas fontes direcionadas; canal WMSDEV existente/ACL compatível apenas por metadados. Zero abertura SQL/SELECT/HTTP/backend/fixtures neste recorte. Cedro prepara exclusivamente perfil/guia backend DEV; Lume prepara frontend real/proxy/baseURL/run dev sem segredos; nenhum backend com SQL ou FE→API com SQL será iniciado enquanto bloqueado. Após pré-requisitos comprovados: somente processos próprios, consultas primeiro; escritas fictícias pertinentes somente HTTP/API rastreável. Sem DDL/Flyway pendente/mutação automática do schema, SQL de fixtures/reset/DELETE, PROD/sa na aplicação, alteração de servidor/acessos/runtime compartilhado, kill/restart existentes ou reabertura FINAL14. Responsável SQL deve fornecer prova atual de recuperação segura; partes frontend independentes continuam. Registro de incorporação: 2026-10-08T21:35:56.890749+00:00; horário do pedido não presumido. [Registro do incremento](orchestracao/.runtime/frontend-farol-dev01-lucas-20261008.json).

**CSS aplicado — prova Prumo encerrada:** link CSS externo no index, import JS duplicado removido, CSP preservada; 30 capturas/estados DEV e BUILD em 1440/768/390 px, HTTP200 text/css, computed Arial/margin0/header azul, zero violações CSP/erros de console/overflow do documento/API/externos. [Recibo](frontend/evidencias/frontend-prumo-css-lucas-20261008.json), SHA256 2453DC770565379D01BDC21635F134080A578767418739C9200757010FA5F608. Diagnóstico reproduziu HTML cru no DEV devido à injeção CSS inline bloqueada; build anterior já estilizado. Não identifica a URL vista por Lucas. O defeito de navegação do skip link é separado, corrigido por Lume e ainda aguarda reconferência; CSS aprovado na fatia não aceita teclado global nem a entrega inteira.

**Fase atual: implementação da base frontend FE01–FE13 em andamento**, por demanda expressa FIM_FRONTEND_LUCAS_20261008. React com TypeScript em frontend/, visual básico preto/branco/azul, sem rodada estética. [Demanda integral](orchestracao/.runtime/frontend-demanda-lucas-20261008.txt) e [distribuição/limites](orchestracao/.runtime/frontend-farol-escopo-lucas-20261008.json). Posse atual distribuída por autorização expressa de Lucas: Cedro assume somente frontend/src/modules/regularizacao/ e testes próprios FE12; Prumo, estilos/entrypoint (src/styles/, styles.css, main.tsx, index.html) e prova do CSS; Lume mantém os demais arquivos frontend, contratos/cliente/workflow/agregadores e integração/checks finais. Vigia revisa em leitura fatias estabilizadas e artefatos próprios, em paralelo. Farol mantém canônicos, decisões, continuidade, mapa e recibos centrais. A abertura inicial não havia enviado trabalho a Cedro/Prumo; a distribuição paralela e o incremento DEV01 acima são os estados atuais.

**Prioridade expressa de Lucas — CSS aplicado e execução paralela:** “nenhum css de fato foi aplicado sendo que tem css, eu falei pra aplicar so uma base fina, e nao deixar html puro, e precisamos adiantar rapido, coloque outros agents para adiantar outras coisas”. Prumo confronta imports/entrypoint/asset e DOM/computed styles em navegação real desktop/coletor; corrige a base fina preto/branco/azul, legibilidade/espaçamento/alinhamento/controles/tabelas/feedback/foco/responsividade, sem refinamento estético. Cedro completa FE12; Lume os demais recortes; Vigia reconfirma fatias estáveis sem esperar fecho global. Handoff imutável F49F86454F8852DDF334C71A04E4EBD624968B28892D62FAAA70883711E2220B; formatter anterior encerrado, donos liberados sem concorrência. [Distribuição e fronteiras](orchestracao/.runtime/frontend-farol-distribuicao-paralela-lucas-20261008.json).

**Escopo concreto:** telas administrativas e fluxos de coletor segundo contratos atuais; navegação, formulários, consultas/ações, carga/vazio/erro/sucesso, prevenção de envio repetido, acessibilidade/teclado e responsividade. Cliente HTTP central e DTOs precisos; backend continua responsável por regras, disponibilidade, permissões e cálculo. Configuração externa sem segredos; nenhuma rota ou autenticação inventada. Provedor JWT/CORS real e equipamentos permanecem dependências próprias. Referências reais inventariadas: um PDF de especificação, dois DOCX e README; originais preservados.

**Validação autorizada:** instalação/build/tipagem/lint/testes e exercício do frontend local próprio. Respostas fictícias explicitamente identificadas para ensaio isolado não comprovam integração real. Fonte, comandos/logs/hashes, cobertura concreta BE→FE e revisão independente serão consolidados; FE13 real com API/dispositivos permanece pendente enquanto impedida. Não concluir pela existência de scaffold, formulário genérico ou contagem de testes.

**Preservação e limites:** FINAL14 backend LOCAL concluída/revisada/aprovada, sem faltante local executável conhecido; não repetir tarefas/testes/build backend. D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos conservados. D29/317nativas/6equipamento/45externas/47históricas/corte temporal e históricos/IDs/checksums preservados. O limite inicial sem SQL/HTTPDEV foi incrementado exclusivamente por D31-DEV01 acima, condicionado à resolução segura e nova guarda; a integração real permanece bloqueada. Nenhuma migration/DDL/PROD/sa/runtime compartilhado/restart/kill existentes/fiscalreal/dispositivos/publicação/commit/push/ETL/rotina/callback ou ask Hermes autorizado. Processos próprios FE e, após guardas, BE DEV permitidos.

**Critério de fecho D31:** base funcional entregue e exercitada; contratos/documentação coerentes; parecer independente sobre fonte atual; limitações reais delimitadas. Recibo final somente [JSON](orchestracao/.runtime/frontend-resultado-lucas-20261008.json) e [MD](orchestracao/.runtime/frontend-resultado-lucas-20261008.md). Registro iniciado em 2026-10-08T17:55:54.3120267Z. Próximo passo: Lume implementa/valida, Vigia revisa fonte estável; Farol consolida evidências e encerra aguardando decisão de Lucas.

**Revisão do MARCO01 em 08/10/2026:** tipagem/lint/211 testes/build/oito jornadas de navegador passaram também em cópia independente, mas três focais novos de Vigia reproduziram defeitos: FE-VIG-002 (vínculo de pedido e reserva/derivados), FE-VIG-003 (constraints de elementos primitivos de listas) e FE-VIG-004 (null em resposta primitiva obrigatória). O MARCO01 não recebeu aceite local. Lume preserva o marco e corrige exclusivamente frontend/ para novo freeze/provas/MARCO02; Vigia reconfirma as correções antes do fecho. Organização estrutural e recortes anteriores foram tratados, sem compensar os três defeitos. O mapa em execução é intermediário, sem aceite de comportamento; será reconciliado com a fonte final. FINAL14 permanece intocada.

**Suficiência das jornadas locais (revisão 8):** além dos três defeitos, o parecer preserva parcelas operacionais locais ainda não demonstradas em FE04/05/06/08/09/10/11/12, especialmente retirada/retorno/devolução em FE10. Lume recebeu complemento focal da mesma demanda para resolver percursos essenciais e provar comandos, referências, revisões e feedback; Vigia delimita suficiência em artefatos próprios. Formulários, mapa de 161 rotas e mocks isolados não aceitam passos ausentes. Integração real permanece requisito separado. [Confronto e encaminhamentos](orchestracao/.runtime/frontend-farol-suficiencia-jornadas-20261008.json).

**Correção expressa de Lucas nesta mesma demanda (registro 2026-10-08T18:44:36.079090+00:00):** “estou achando o codigo muito mal organizado, cuidado”. Organização, legibilidade, acoplamento e testabilidade passam a critério de entrega: separar telas/módulos, componentes, contratos/HTTP, hooks e estilos por responsabilidade concreta; corrigir concentração, duplicação e abstrações sem uso, preservando comportamento. Lume corrige; Vigia revisa explicitamente a organização na fonte estável; verificar frontend após as correções. Sem rodada estética, missão duplicada ou ampliação dos limites. Base não será declarada pronta com desorganização material conhecida. Inspeção e snapshots em [D31-ORG01](orchestracao/.runtime/frontend-farol-organizacao-20261008.json).

## Histórico anterior preservado — FINAL14 e demais fases

A demanda D31 inicia frontend expressamente; textos antigos “frontend não iniciado/aguarda demanda” conservam sua data e não descrevem mais esta nova fase. A aprovação e os limites backend históricos permanecem inalterados.

## Resultado atual — FINAL14 local encerrada; registro de retomada dos chats

**Pedido de Lucas registrado em 08/10/2026 14:32:23 -03:00 (UTC: 2026-10-08T17:32:23.3521232Z). Escopo desta entrega: somente documentação e handoff.** Lucas informou que já reiniciou seu chat Farol antes do pedido de ajuste; esta entrega ocorre no chat reiniciado. O usuário reinicia os chats; o agente não reinicia nada. O bloco LOCAL da fonte FINAL14 está **concluído, revisado e aprovado**, nos predicados, canais, oráculos e limites inventariados. **Nenhum faltante local executável concreto conhecido.** A D30 geral permanece aberta: aceiteLocalIntegral=false, D30Fechada=false; C06/C07/C10 mantêm parcelas requeridas impedidas no denominador. Isso não declara o sistema inteiro concluído nem frontend testado.

Fonte atual: [snapshot de retomada](orchestracao/.runtime/d30-retomada-apos-reinicio-chats.md), [JSON com caminhos e SHA-256](orchestracao/.runtime/d30-retomada-apos-reinicio-chats.json), [recibo parcial FINAL14](orchestracao/.runtime/d30-resultado-parcial-final14.json), [matriz final14](orchestracao/.runtime/d30-matriz-requisito-evidencia-limite-final14.json), [parecer geral Vigia14](orchestracao/.runtime/d30-vigia-final-geral14-parecer-v01.json), [delta documental revisado](orchestracao/.runtime/d30-vigia-delta-documental-v91-parecer-v01.json) e [freeze v92](orchestracao/.runtime/d30-farol-freeze-decisao-final-v92.json). O snapshot registra estes bytes e a atualização documental posterior ao freeze; o mapa final existente foi preservado, sem nova execução ou atualização nesta entrega.

FINAL14, somente sua execução: **708/0/0/0, exit0, BUILD SUCCESS, 1 JAR, 52 XML/classes locais**; fonte/snapshot/log/XML/capturas/JAR correspondentes. SQLIT: sete métodos somente compilados. Disposições v93e/v117, inventário javac v22, GET65/provider/A08/contextos e AST11/mapa final permanecem nas referências atuais; contagens não equivalem a cobertura ou aceite de pais.

Limites preservados por ID: incidente D29 sem resolução segura comprovada nem nova guarda WMS_DEV/WMSDEV/TLS; **317 nativas SQLServer, seis facetas de equipamento, 45 externas/47 históricas** e corte **08/10/2026 03:00Z ultrapassado**, sem retrodata ou soma de execuções. Frontend **não iniciado** (FE01–FE13); aguarda nova demanda expressa.

**Leitura no chat reiniciado:** ler AGENTS.md → este resumo de states.md → docs/09-continuidade.md → snapshot de retomada → recibo e matriz FINAL14, conforme pertinência. Reinício não autoriza retomar jobs ou tarefas. FINAL02–13, as 387 variáveis e os 23 grupos já reconciliados são históricos, não fila corrente. Nenhuma nova missão, teste/build/mapa/backend/FE/SQL/sonda/HTTPDEV/DDL/PROD/sa/servidor/kill/publicação/commit/push/ETL/rotina foi autorizada por este registro; nenhum terminal/chat/modelo/processo deve ser reiniciado pelo agente.

Posse: Farol mantém canônicos, decisões, continuidade, snapshots e recibos centrais; Cedro backend e artefatos próprios; Lume contratos para futuro FE e artefatos próprios, sem FE iniciado; Prumo database/infra em arquivos e artefatos próprios; Vigia revisão e pareceres próprios. graphify-out/ permanece sob coordenação de Farol, sem writer nesta entrega. Entrada exclusiva **Hermes WMS**, perfil **wms-rodogarcia**; somente os seis papéis WMS e a nota **WMS - Continuidade**, sem compartilhar contexto com ETL ou outros projetos. Este registro encerra o bloco documental autorizado; próximo passo: aguardar nova demanda expressa.

## Histórico preservado — registros anteriores ao snapshot de reinício

Os textos abaixo conservam integralmente IDs, detalhes, fontes e conclusões de suas datas. Pendências e comandos antigos não são instruções atuais de retomada; o resultado acima e o recibo FINAL14 prevalecem para o estado corrente.

**D30 — verificação local integral backend/documentação/contratos, demanda Lucas07/10/2026:** em andamento BE01–BE16. Frontend não iniciado; FE01–FE13 recebem somente ligações documentais. [Escopo e dez critérios](orchestracao/.runtime/d30-farol-escopo-e-distribuicao.json), [matriz atual](orchestracao/.runtime/d30-matriz-aceite-local.json) e [andamento](orchestracao/.runtime/d30-farol-andamento.json). Aceite integral somente com zero critério local pendente, sem percentual por número de testes.

**Produto integral de disposição recebido:** [Lumev10](orchestracao/.runtime/d30-lume-revisao-integral-v10.json) preserva1131IDs,727/706,1373/192,161rotas,48/39 e45/47. Cada linha possui definição/usos/oráculo/prova relacionada ou limite/faltante; não aceite de comportamento por estrutura, URI, chamada, annotation ou nome. [Fila de encaminhamentos](orchestracao/.runtime/d30-farol-fila-integral-lume-v09.json) conserva51qualificações individuais, sem limitar o conjunto. Cedro confronta equivalentes e completa faltantes locais; Vigia revisa assertivas/recibos. Reserva no cálculo, DUN contextual, marcos encerradaEm, hash decimal e compromissos/rollback do encerramento continuam com disposição específica.

**Provas revisadas e versões:** [Vigia01/02](orchestracao/.runtime/d30-vigia-parecer-focal01-02.json), [03–05](orchestracao/.runtime/d30-vigia-parecer-predicados-focais03-05.json), [Prumo/SQM/06–08/header](orchestracao/.runtime/d30-vigia-parecer-prumo-focais06-08-sqm-header.json) e [Long12/13](orchestracao/.runtime/d30-vigia-coercao-long-focais12-13-conferencias.json) favoráveis somente aos predicados delimitados, sem somar execuções ou fechar pais. [Integer14/15 autor](backend/evidencias/d30-cedro-coercao-integer-recibo.json) conserva red4/4/0/0 e green4/0/0/0 dos quatro negativos; [Parecer INTEGER independente](orchestracao/.runtime/d30-vigia-coercao-integer-focais14-15-conferencias.json) favorável somente aos quatro negativos; positivos/nulos/exatos/overflow/Long/Decimal e regressão completa pendentes na última fonte. Módulo correnteDF7D Long/Integer distingue versão histórica5C630 do green13;259main atuais,258anteriores preservadas nas capturas. Header64KB é correção local de transporte, domínio500/Long/sub200/900s intacto. Prumo317 estático e SQM7/34zeroJDBC revisados com limites; A11 valor lexical suficiente na captura5F230, textoUTF8autorD01D/F183 qualificado separadamente em [Vigia](orchestracao/.runtime/d30-vigia-a11-utf8-versao-parecer.json), sem retroagir hashes ou repetir confronto. Não materialização/ORDERBY/locks reais. A associação futura RN09.01/V40.02/AC09.06 recebeu [parecer Vigia17–20](orchestracao/.runtime/d30-vigia-associacao-focais17-20-parecer.json) favorável somente aos vetores do green20, com fixture/red/parcial separados; rollback tardio, audit/op por assertiva dedicada, SKU/dados/nulos e FINAL permanecem pendentes. FatoServico e seu service mudaram por caminho/SHA; preservação das258 fontes anteriores é histórica, sem certificar todasasatuais iguais.

**Ainda necessário para concluir LOCAL:** resolver todos faltantes pertinentes nas706/727/1373/161/classes/variáveis, incluindo fonte nova no inventário de símbolos após última mudança. A08 temporal lossless, A10 schemaH2 e três ramosv09 têm primeira execução somente na regressãoFINAL;65GETs precisam observação guardada por rota/cenário, reaproveitando jornadas, sem duplicar fixtures/focais. Depois das últimasfontes: finalCOMPLETO seguro com fonte/snapshot/classes/JAR/XML/log da mesma execução, mapa produtivo por caminho/SHA, freeze e revisão independente de TODO pacote. [Disposição documental Farol](orchestracao/.runtime/d30-farol-disposicoes-documentais-v09.json) confirma fontes do vínculo futuro adicional/peso e explicita lacunas locais; não cria conversão ou saída fictícia. [Contrato derivado atualizado](orchestracao/.runtime/d30-farol-adenda-contratos-associacao-inteiros-v01.json) separa associação/replays/consulta atual e negativos Long/Integer, preservando prefixo do documento35. [Adenda Lumev05](orchestracao/.runtime/d30-lume-adenda-v10-vigia-assertivas-v05.json) preserva as disposições anteriores e individualiza equivalentes/faltantes de recebimento, unitização e saída; [gate de cobertura](orchestracao/.runtime/d30-farol-gate-final-disposicoes-v10-v01.json) e [faltantes por atributo](orchestracao/.runtime/d30-farol-gate-final-atributos-faltantes-v10.json) mantêm IDs/pointers em batches sem aceite estrutural. limites de recebimento, unitização e saída continuam na mesma fila, com comandos válidos e conservação na recusa, sem repetir positivos já provados. Ausência de prova realSQL não impede predicados locais. Nenhum impedimento concreto de execuçãoLOCAL identificado; trabalho restante não é aceite nem bloqueio externo.

**Autoridade e preservação:** Cedro único escritor/executor backend; Farol docs/canônicos; Lume/Prumo/Vigia leitura e próprios artefatos. HTTPLOCAL+H2 efêmero fictício e mocks autorizados com configuração/URL/perfis e guardas fail-closed antes dos casos. SQLIT somente compilação. SQLServer/HTTPDEV/sondas/banco real,PROD,sa real,servidor/runtime/acessos,DDL/migrations reais,restart/kill/reset/limpeza,publicação/commit/push,ETL e rotinas continuam suspensos. D29 e históricos preservados; byte-identidade das caudas apenas na linhagem temporal conferida em [Vigia](orchestracao/.runtime/d30-vigia-canonicos-vinculos-temporais.json), sem afirmar todosbytes imutáveis desde baseline inicial. Dependências nativas/equipamento/externos conservam requisitos e encaminhamento próprio; nenhuma simulação os aceita. FE aguarda demanda posterior.

**Delimitação atual de defaults e provas:** a [adenda dos defaults de Avaria](orchestracao/.runtime/d30-farol-adenda-contratos-defaults-avaria-v01.json) qualifica somente os dois construtores compactos Java NULL→false; binding HTTP, permissões, replay e efeitos dos callers continuam com prova própria pendente. O [parecer de oráculos da primeira FINAL](orchestracao/.runtime/d30-vigia-oraculos-final-inteiros-get-v01.json) exige capturas H2 pertinentes por rota/caso/contexto, handler real e fisicoIgual=true, ligadas à fonte/XML da mesma execução; XML verde sozinho não atesta as fotografias. Captura ausente, false/null ou concorrente sem correspondente permanece pendente. Contextos sem banco conservam prova própria. A [revisão das assertivas de componentes](orchestracao/.runtime/d30-vigia-componentes-v13-v15-limites-assertivas-v01.json) mantém validação aninhada, causalidade do400 e transformações por caller nos mesmos IDs, sem aceite por roundtrip sintético.

**Consolidação de disposições,08/10/2026:** [Vigia:706locais/727IDs](orchestracao/.runtime/d30-vigia-cobertura-facetas727-local706-v01.json), [1373atributos](orchestracao/.runtime/d30-vigia-cobertura-atributosDTO1373-v01.json) e [161contratos](orchestracao/.runtime/d30-vigia-cobertura-contratosHTTP161-v01.json) têm disposições individuais conferidas; fontes/corpos relacionados e faltantes preservados, sem equivalência comportamental por contagem. [Lumev07](orchestracao/.runtime/d30-lume-adenda-v10-delta-produtivo-v07.json) e [Vigia32](orchestracao/.runtime/d30-vigia-delta-produtivo-v07-simbolos32-v01.json) delimitam32símbolos selecionados, não total corrente8716; onze do módulo faltavam no índice histórico. [Posição v08](orchestracao/.runtime/d30-lume-adenda-v10-posicao-por-id-v08.json) qualifica a restrição na mesma posição, sem exclusividade global deSKU/cliente; [sobrecargas v09](orchestracao/.runtime/d30-lume-adenda-v10-sobrecargas-v09.json) corrige cinco vínculos derivados: Indicador146 conserva fuso e fixa produtoId=null, controller chama157 com ambos. [Consolidação v32](orchestracao/.runtime/d30-farol-consolidacao-contratos-guardas-v32.json) importa planos Cedro34–37 e a leitura estática C07 Prumo: receita declarativa não é executor/FINAL. format31/compile31 exit0 informado pelo autor; nenhum JUnit novo após focal20. Ainda pendem disposições correntes completas de classes/variáveis, complementos finitos, último código/símbolos, freeze/executor seguro/primeiraFINALcompleta e revisão geral. Fragmentos graph v2 são históricos após esta atualização dos cabeçalhos; integração e fonte corrente mantêm gate próprio C09. C01–C10 abertos; SQLServer/FE suspensos.

**Regressão e partições,08/10/2026:** a [consolidação FINAL02](orchestracao/.runtime/d30-farol-consolidacao-final02-particoes-v34.json) confere os50XML/artefatos preservados e87reds porcaso:695/67/20/0,608green,exit1 e0JAR somente nessa execução. FINAL01 anterior falhou em formato semJUnit/JAR; não somar tentativas nem inferir87defeitos produtivos. Cedro trata causas compartilhadas, fixtures/oráculos/instrumentação e candidatos materiais antes da próxima regressão completa após últimas fontes. [Lume autoria exclusiva v02](orchestracao/.runtime/d30-lume-particao-semantica-v02.json) e [Prumo autoria exclusiva v1](orchestracao/.runtime/d30-prumo-particao-semantica-v1.json) foram recebidas comIDs/pointers/SHA preservados; suficiência semântica/provas e FINAL continuam em revisão independente. [Parecer de suficiência Cedro](orchestracao/.runtime/d30-vigia-cedro-indice-semantico-linhagens-v01.json) exige substituir disposições genéricas por transformação/invariante/caller/assertiva/oráculo/versão ou faltante concreto. [Ligações por dono/caller](orchestracao/.runtime/d30-farol-ligacoes-prumo-v36-distribuicao.json) preservam356grupos sem duplicação, sem testes novos automáticos. Histórico8716 permanece; inventárioV06 corrente8744 e delta28 são declarações, não requisitos/testes/aceite. O [mapa documental v34](orchestracao/.runtime/d30-farol-graph-documental-integrado-v34.json) foi integrado combackup/guardas e posse liberadaCedro; seusSHAs são temporais anteriores a este registro. C09 exige fonte/mapa final após últimas mudanças. Freeze/regressão/revisão geral não aguardam mera redação; achados reais permanecem na fila. C01–C10/D30 abertos; SQLServer/FE/históricos e limites mantidos.

**FINAL04 e faltantes locais comprovados,08/10/2026:** a [conferência FINAL04](orchestracao/.runtime/d30-farol-final04-pacote-conferencia.json) verifica696/0/0/0,50XML,1JAR e7095artefatosSHA da mesma execução,exit0; FINAL01/02/03 e seusreds continuam históricos separados, sem soma. Isso comprova regressão/proveniência deste snapshot, não aceite integral por número de testes. A [conferência GET65](orchestracao/.runtime/d30-farol-get-final04-conferencia-v42.json) confronta rota/handler/caso/XML/hash das fotos64 e conserva18faltantes positivos:47rotas têm captura2xx pertinente, sem aceitar403/handlerNULL/concorrentes sem correspondente. Cedro complementa somente faltantes na fila vigente; fonte nova exige nova correspondência e regressão final após últimas mudanças. As [ligações Lume/Prumo](orchestracao/.runtime/d30-farol-ligacoes-lume-prumo-v40.json) têm33grupos originais sem omissão/duplicação, com provas delimitadas ou falta concreta; instantes de cadastro, strings fiscais/cnull e limites individuais provider/declarações sem caller permanecem em confronto de equivalentes. [Plano de cadastros](backend/evidencias/d30-cedro-transformacoes-cadastros-confronto-plano-v52.json) identifica candidato fiscal positivo real, sem generalizarcnull/tempo. Os [exemplos semânticos Cedro](backend/evidencias/d30-cedro-semantica-autoral-v49.json) não concluem57classes/323grupos por contagem; autoria específica e parecer independente integral ainda necessários. O [mapa documental v41](orchestracao/.runtime/d30-farol-graph-documental-integrado-v41.json) preserva ASTv04/backup e oito fontes temporais anteriores a este registro; posse liberadaCedro para AST após últimas fontes. Não repetir verdes por mera redação. C01–C10/D30 continuam abertos; faltantes locais são trabalho autorizado, não impedimentoSQL. IncidenteD29/guardaSQLServer não resolvidos nem reexecutados; FE não iniciado.

**FINAL05 e deltas por item,08/10/2026:** a [conferencia FINAL05](orchestracao/.runtime/d30-farol-final05-pacote-conferencia.json) registra700/2/0/0,51XML,exit1 e0JAR na execucao encerrada naturalmente; FINAL04696/0/0/0/JAR permanece prova de seu snapshot, sem soma ou heranca para fontes posteriores. Os doisreds foram preservados por caso/XML e [importados com a prova fiscal Lume](orchestracao/.runtime/d30-farol-lume-final04-final05-deltas-v44.json): consulta da importacao retorna enderecos vazios versus expectativa da confirmacao; alteracao fisica no mesmo instante aplica ramo monotono de um microssegundo. Cedro confronta contrato/oraculo antes de corrigir, preservando main se a falha for expectativa; isso nao infere bug geral nem autoriza tolerancia temporal. A [consolidacao Prumo/Vigia](orchestracao/.runtime/d30-farol-prumo-vigia-final04-registros-v45.json) conserva29grupos e33pareceres porID; MapDecimalMAX/associacao/rollback simulado/captura temporal tipada sao suficientes somente nos vetores revisados FINAL04. NULL/default/omissao/limites nao herdam o positivo fiscal11; identidades, versoes otimistas e repositories conservam faltante concreto de ligacao/caller quando aplicavel. Autoria Cedro57classes/323grupos, provas GET65/complementos atuais, ultima regressao verde comJAR/freeze/mapa e revisao independente geral continuam pendentes. Nao repetir focais verdes por redacao ou criar teste por getter/alias/variavel. C01–C10/pais/D30 abertos; SQLServer/FE/incidenteD29/historicos e limites permanecem.

**Próximo passo D30:** [decisão local revisada e impedimentos](orchestracao/.runtime/d30-farol-consolidacao-decisao-local-v90.json): bloco LOCAL da fonte14 conferido e aprovado somente nos predicados/canais/oráculos/limites revisados. [Parecer geral Vigia](orchestracao/.runtime/d30-vigia-final-geral14-parecer-v01.json) conclui C01–C10 na parcela local, sem faltante local concreto conhecido ou novo teste/código necessário. FINAL14:708/0/0/0,exit0,1JAR,fonte/ZIP/classes/log/XML/capturas mesma execução; número de testes não é cobertura. Disposições93e/v117 e GET65/contextos/provider/A08 atuais revisados, AST11/mapa84 na fonte14 e preservação de relações confirmados. D30 geral/pais permanecem abertos:317facetas nativas,6equipamento,45externos/47históricos e corte03:00Z não cumprido; parcelas requeridas impedidas conservadas no denominadorC06/C07/C10, sem aceite porH2/static/mocks nem relabel. SQLD29 sem resolução segura/nova guardaWMS_DEV/WMSDEV/TLS. Próximos encaminhamentos são os donos/IDs da matriz e futura retomada nativa só no escopo seguro autorizado; nenhuma execução atual. Recibo parcial previsto em orchestracao/.runtime/d30-resultado-parcial-final14.json; frontend aguarda demanda posterior. D29/reds01–13/preexistentes preservados, sem soma/backdate/publicação/commit/push/ETL.

### D30 — inventário integral e aceite local

- [x] **D30-C01 Inventário completo de fontes/classes produtivas e atributos relevantes** — Cedro/Farol. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C02 Rastreabilidade normativa e decomposição das727lacunas** — Lume/Farol. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C03 Atributos, tipos e transformação entre camadas** — Cedro/Lume. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C04 JPA/migrations em arquivo e consultas estáticas** — Prumo/Vigia. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C05 Contratos destinados ao futuro frontend** — Lume/Cedro. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [ ] **D30-C06 Todos casos locais pertinentes e achados materiais** — Cedro. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**
- [ ] **D30-C07 Build final completo e proveniência da execução** — Cedro/Vigia. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**
- [x] **D30-C08 Revisão independente de todo pacote local** — Vigia. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C09 Preservação, registros e mapa** — Farol/Cedro. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [ ] **D30-C10 Decisão de aceite local integral** — Farol/Vigia. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**

**Bloco local D29,07/10/2026, concluído e revisado:** [parecer final Vigia](orchestracao/.runtime/d29-fecho-local-vigia-final.json) favorável com limites; [recibo parcial](orchestracao/.runtime/d29-resultado-parcial-fecho-local.json). [Matriz](orchestracao/.runtime/d29-fecho-local-lume.json) preserva1.131IDs/48complementos/39financeiros, seis aceites anteriores delimitados,47externos históricos/45posteriores,727IDs sem equivalência integral selecionada e pais abertos. [Prumo](orchestracao/.runtime/d29-fecho-local-prumo.json) individualiza cinco impedimentos; parecer498 anterior preservado. Nove novos locais9/0/0/0 revisados: seis permutações de três notas somenteR$1,00 (0,33/0,33/0,34) e cortes1/0/32. R$0,01 de RN09.03/CT29-L043-01, ORDER BY nativo e demais facetas não recebem esse aceite. UTF8 Cedro corrigido pelo autor; referências históricas de hashes diferentes qualificadas sem reconstrução. Nenhuma nova sonda/SQL/HTTP/escrita nesta consolidação. D29 geral e backend integral permanecem abertos; nenhum frontend ou sistema completo declarado.



**Incidente SQL prioritário D29,07/10/2026:** Lucas relatou connection timeout. Novos ensaios SQL, chamadas HTTP e escritas de negócio estão suspensos até resolução segura e nova guarda real WMS_DEV/WMSDEV/TLS. A tentativa única de diagnóstico de Prumo já terminou; a retomada atual não autoriza nova sonda, SQL ou HTTP. Cedro completa somente lacunas locais comprovadas; Lume consolida a matriz existente, Prumo os impedimentos em arquivos e Vigia revisa o pacote local. Farol mantém os registros e o recibo delimitado. Sem restart, mudança de servidor/runtime/acessos, kill de transações ou limpeza. Timeout de abertura/prelogin antes de identidade já registrado; serviço/listener não comprovam login, saúde SQL ou autoria. [Controle de suspensão](orchestracao/.runtime/d29-incidente-suspensao-farol.md). D29 permanece aberta; nenhuma atribuição causal sem prova.



**Diagnóstico do incidente D29,07/10/2026:** sonda única Prumo20:39:17Z falhou em5,12s no prelogin (SQL_-2/native258),pooling=false,zeroSELECT/HTTP e identidade/TLSatuais não confirmados. MSSQLSERVERRunning/PID65088 desde05/10,mesmoPIDD24/listener1433; não prova saúde. Quatro eventos70117:02Z de memória no poolinternal precedem preflightválido19:14:15Z; causa atual/aplicação causadora inconclusivas. Histórico administrativoD24/D26/D27 separado deD29HTTPDEV; nenhum comandoD29 de alteração global encontrado nos registros revisados, sem afirmar inexistência universal. [Recibo do incidente](orchestracao/.runtime/d29-incidente-diagnostico-farol.md). Escritas/ensaios suspensos até resolução segura e nova guarda. Vigia1131/39 ePRIO06local14/0 revisados porcanal, semaceitepais/DEVporH2; preservação final1895+5tardios e demais limites individuais mantidos. D29 não encerrada.



**Correlação701 D29,07/10/2026:** nos21arquivos HTTP/1491respostas,19fotos/1274intervalos de queries de collectors e85logs WMS examinados, não há resposta, intervalo de consulta publicado ou linha JVM timestampada entre14:02:08 e14:02:20BRT. Isso não exclui atividade/API/SQL/JVM ou consumo de memória e não identifica aplicação causadora. Fonte/checkpoint documental17:02:35Z não prova execução SQL naquele segundo. O processo65088 sem reinício e a configuração atual são critérios distintos: configuração atual permanece não comprovada enquanto a guarda falha. Aplicativo/tela, horário e erro exato do timeout relatado porLucas foram solicitados neste canal e seguem pendentes; sondaPrumo e erroEnsaioD29 não os substituem. [Correlação offline](orchestracao/.runtime/d29-incidente-correlacao701-farol.json). AdendaVigia57/0 resolveu fonte04versus14/UTF16 sem mudar bytes; nove novosPRIO06 e consolidado14 já revisados. Escritas/ensaios continuam suspensos, sem reexecução ou atribuição causal.



**Aceite delimitado dos seis vínculos D29,07/10/2026:** RN24.02/MULTI01, V33.02/RET01, AC05.01/CAP01, Q-D10-L047-S02/RET02, Q-D10-L059-S02/RET05 e Q-D10-L059-S03/RET03 atendidos somente nos casos DEV enumerados e revisados. Base LumeFINAL19:49:39.577Z/DEFCDF57, parecerVigia23/0/9C42A9F2; nenhuma nova execução. Pais abertos. Saldo porSKU10=8reservado+2bloqueado+0disponível; texto disponível2 original preservado, sem usar como esperado. Revisão local14 posterior preservada. CheckpointCedro029 está na cópia histórica; corrente456 é atualização20:45, não prova de negócio ou posse. [Aceite e fontes](orchestracao/.runtime/d29-farol-aceite-seis-vinculos.json). Incidente e escritas suspensas permanecem; configuraçãoSQLatual não comprovada, causa/aplicação e relatoLucas pendentes. Sem fecho geral.



**D29 em andamento,07/10/2026 — BE03–BE15:** nova autorização expressa de Lucas para suprir lacunas documentais/técnicas do backend no DEV, com novas famílias fictícias por API e matriz completa RN/I/PR/V/AC/respostas/contratos/correções. D28 permanece encerrada, como inventário histórico; não substitui execução D29. Cedro jornadas/build/correções mínimas; Prumo preflight/SELECT/observabilidade permitida; Vigia revisão independente; Lume somente leitura; Farol registros/matriz/recibo. [Escopo e arquivos](orchestracao/.runtime/d29-autorizacao-e-distribuicao.md).



**Diagnostico de posse Cedro D29,07/10/2026 19:29:47 UTC:** prioridade atual de Lucas. `maestri check` mostra chat 01a117bb-d254-7c80-bd98-23f94a2e65df, diferente do original 01a11636-be42-77a3-9d1a-4cf73804deab. Leitura oficial de metadados do original confirma `active`/`inProgress`; duas entregas ao mesmo turno, separadas por reconexao oficial, recusadas com `Server is draining; retry after reconnecting`. Aplicativo/PID dono nao expostos: nao inferir VS Code pela origem nem declarar instancia residual. Sem encerramento/reset/lock apagado/servidor/config/seguranca raw ou negocio novo. Build/arquivos anteriores sao historicos, nao prova de terminal operacional atual. [Diagnostico sanitizado](orchestracao/.runtime/d29-cedro-sessao-diagnostico-farol.json). Retomada so aceita apos aviso ausente no mesmo chat original e resposta nova do modelo; Prumo/Lume seguem leituras independentes ja atribuidas.



### D29 — cobertura ampliada e critérios



- [ ] Inventariar fontes normativas e decompor requisito/regra/cenário/correção com origem, oráculo e status atendido/não atendido/depende de validação externa.

- [x] Novo preflight real WMS_DEV/WMSDEV restrita:36/0 em07/10/2026 16:09:36 UTC, TLS exato,64/687,V1–V10 e311 direitos preservados. [Conferência Farol](orchestracao/.runtime/d29-preflight-conferido-farol.json). Revalidação própria obrigatória antes de cada etapa; catálogo não é prova de negócio.

- [ ] Build/regressão atuais e jornadas ponta a ponta novas; AC04–08 exemplos positivos/zero/limites/recalculo com esperado independente e preços fictícios.

- [ ] SELECT novo de efeitos/replay/rollback/históricos; concorrência HTTP real repetida sem gate artificial como lock nativo, investigação de observabilidade sob direitos atuais.

- [ ] Corrigir defeitos concretos com red/green, build/reexecução e revisão; contratos/documentação honestos sobre dispositivos/fiscal/comercial/operação/recuperação externos.

- [ ] Revisão/cobertura/contagens, processos próprios encerrados, mapa AST após código e recibo final somente orchestracao/.runtime, sem callback Hermes/ETL.



**Próximo passo D29:** o bloco local e seu parecer estão concluídos; não repetir498/14/nove/seis ou jornadas encerradas. O responsável SQL deve comprovar resolução segura do incidente antes de qualquer nova guarda WMS_DEV/WMSDEV/TLS dentro da D29 autorizada. Preservação final1.895+cinco tardios/catálogo64/687/V1–V10/311direitos depende dessa guarda; corte nativo08/10/2026 03:00Z também exige origem admissível. Witness mantém limite de permissões. Confrontar prova específica existente paraR$0,01 entre três notas, sem execução adicional neste bloco; demais equivalências e45externos têm responsável/encaminhamento porID na matriz. Não atribuir todas as727lacunas ao SQL. Resultado somente parcial, sem callbackHermes/ETL.



**Parcial D29,17:00 UTC:** entrada, red de estoque e sua continuação verde, concorrência nova:462 chamadas HTTP/76 assertivas; duas recusas inesperadas do roteiro preservadas no red, sem defeito backend inferido. Quatro fotos SQL novas771 comparações/0 divergências,268 consultas; focais reutilizam fotografias. Concorrência:três pares200/409, inversão no segundo, três replays200/200 e controle sequencial em pedidos novos; intervalos HTTP e JVM próprios não são locks SQL nativos. [Ledger parcial](orchestracao/.runtime/d29-ledger-parcial-farol.json). A emissão manual herdada da primeira entrada desqualifica prova temporal; quantidades/origem/replay têm revisão própria. Guarda nova8/0 isolada, sem remutação. Baseline derivada corrige somente IDs vazios, conserva1.895 hashes e foto16:27; [conferência](orchestracao/.runtime/d29-preservacao-baseline-conferida-farol.json), confronto final ainda pendente. Financeiro local novo14/0 após corrigir fixture do teste, com repositórios simulados; não é jornada SQL/API. Financeiro real, demais jornadas/regras, revisão final, build final e encerramento geral continuam pendentes. [Matriz em andamento](orchestracao/.runtime/d29-matriz-requisitos-farol.json); partes atendidas limitadas às provas vinculadas.





**Parcial D29,17:51 UTC:**11 jornadas/complementos com980 chamadas HTTP/156 assertivas; sete divergências HTTP preservadas, incluindo o defeito concreto XInclude (contrato400, observado201 com pedido persistido), em correção red/green/build novo por Cedro e revisão Vigia. Demais reds identificados são de roteiro/fixture/oráculo. Onze fotos SQL ligadas a HTTP mais uma foto diagnóstica de auxiliar com zeroHTTP:1.699 comparações/0 divergências genéricas,738+67 consultas respectivamente; focal XInclude91/5 permanece vermelho e não é apagado pelo confronto genérico. Financeiro53/25 comprova previsões66/76 e snapshots preservados; complemento recusa aprovação antes do corte real08/10, não comprova aprovação/reabertura. Carga6 retomada sem recontagem; XML/extremos e Excel/contingência em confronto próprio. [Ledger atual](orchestracao/.runtime/d29-ledger-parcial-farol.json), [matriz](orchestracao/.runtime/d29-matriz-requisitos-farol.json):471 cláusulas,487 contratos,172 partes de respostas recebidas e avaliações independentes vinculadas, sem aceitar pais por referência geral. Cedro original operacional confirmado por checkpoints; entrada reaberta tem posse em outra app, sem intervenção/reset/hook/config alterados. [Recuperação observada](orchestracao/.runtime/d29-recuperacao-farol.json). Preservação17:17 dos1.895 IDs/hashes antigos é checkpoint, com uma adição de auditoria rastreada à API D29; confronto final/build final/cobertura completa/fecho geral ainda pendentes. Nenhum100% declarado.



**Parcial D29,18:39 UTC:**15 jornadas/complementos,1217 chamadas HTTP/194 assertivas; reds preservados. 14 fotos SQL ligadas a HTTP (939 consultas) mais uma foto diagnóstica de auxiliar com zeroHTTP (67 consultas), sem somar focais como novas consultas. Build448/0/0/0 novo e JAR7E22471E…939FD. Green XInclude400, SELECT134/0/focal101/0 sem novo pedido e red66 intacto; revisão independente em conclusão. Contingência L1/L2 e financeiroV1 rejeitada/V2 revisão106 em confronto, sem aprovação por corte08/10. FIFO novo D295BD16414 tem104 HTTP/22 assertivas e contrato para Prumo. Cedro confirmou leitura do [complemento financeiro](orchestracao/.runtime/d29-cedro-lacunas-financeiro-farol.md) e executa lacunas sem repetir fatos. Primeira tentativa local ampliada46 testes/1 falha/14 erros preservada; análise de fixtures/oráculo em curso, sem inferir defeito de negócio. [Índice de todos os critérios publicados](orchestracao/.runtime/d29-indice-auditoria-farol.md) e [JSON](orchestracao/.runtime/d29-indice-auditoria-farol.json) mantêm471 cláusulas/488 contratos/172 partes de respostas, candidatos Vigia/externos/39 financeiros separados. Somente aceites delimitados com revisão; nenhum percentual ou homologação integral. Preservação1.895 hashes ainda exige confronto final após últimas mutações; próximos testes/build final/processos/mapa/revisão geral continuam pendentes.



**Retomada D29 após queda,07/10/2026:** respostas novas de Cedro/Prumo/Vigia e confronto Lume nos chats recuperados confirmados. As sessões originais continuaram produzindo provas até19:42 e Cedro original concluiu build completo486/0/0/0 em26XML às20:06:35;307fontes/hash conferidos e JAR7E224…939FD preservado. [Conferência486](orchestracao/.runtime/d29-build-fronteiras-conferido-farol.json). Retomada Cedro executou5casos locais F33/F34/fronteiras XML64/200/1000000bytes UTF8, green5/0, com reds de fixture/oráculo preservados; não somar5+486 nem aceitar DEV por H2. Limites locais de configuração financeira1/366 e campos exclusivos em complemento. Vigia favoreceu17partes8B/13/964 com pais abertos; red1915/2 do leitor preservado e adenda4/0 fundamentada na reserva exclusiva do pallet: físico10=reservado8+bloqueado2+disponível0. [Parecer](orchestracao/.runtime/d29-vigia-retomada-20261007T195308Z-parecer.md). Lume confrontou1131IDs/48complementos sem renumeração; propostas DEV restantes aguardam revisão, não nova execução dos casos concluídos. [Confronto](orchestracao/.runtime/d29-lume-retomada-confronto-final.json).39PIDs históricos próprios e listeners estavam ausentes na conferência Farol posterior, sem encerramento por Farol. SQL_-2 antes de DB_NAME/identidade mantém retidos negócio DEV, preflight e preservação final; último confronto1895 comprovado17:17 está na cópia imutável, não no caminho corrente interrompido. Cinco separações tardias19:16:17 não são retroativas. Corte08/10/03Z, witnessSQL e externos permanecem individualizados. Revisão geral, complemento local e recibo ainda em andamento.



**D28 encerrada no escopo comprovado, 07/10/2026 — BE03–BE15:** regressão nova das correções D26/D27 nos mesmos chats WMS, com revisão independente favorável às 29 linhas e limites explícitos. Build420/0/0/0 em20 XMLs;17 rodadas reais,761 chamadas HTTP (756 individuais aprovadas+2 do par200/409+3 divergências de roteiro preservadas),211 assertivas (210 aprovadas+1 precondição inválida preservada). Fotografias SQL atuais1315/0,1141 consultas nelas; focais211/0 reutilizam as fotos, sem novas consultas. Ajustes/rollback/replay, temporal, financeiro50 aprovado/v2, XML/RN22/extremos, perfis/filtros/FIFO/avaria e concorrência HTTP/JVM+SELECT posterior reexecutados. Witness SQL nativo impedido por SQL300 sob WMSDEV; nenhuma ampliação de privilégio. Preflight final WMS_DEV/WMSDEV,64/687,V1–V10 e311 direitos sem alteração;300 fontes de backend/migrations intactas. Zero APIs/helpers/listeners D28 na conferência OS; fixtures/históricos preservados. [Recibo final](orchestracao/.runtime/d28-resultado-final.json), [matriz29](orchestracao/.runtime/d28-matriz-correcoes-farol.json), [parecer Vigia](orchestracao/.runtime/d28-vigia-final.md). Sem frontend/PROD/reset/DELETE/DDL/grants/migration/publicação/commit/push/callback Hermes/ETL. Não homologa o backend inteiro.



### Regressão D28 — critérios e evidências atuais



- [x] Inventário29 correções/casos/evidências atuais; auxiliares offline e negócio real separados, revisão Vigia e referências/hashes conferidos por Farol.

- [x] Comprovar DB_NAME=WMS_DEV/WMSDEV/TLS no preflight novo de 07/10/2026 12:14:59 UTC, catálogo64/687, V1–V10 e311 direitos preservados; runner revalida antes das etapas. [Preflight](orchestracao/.runtime/d28-prumo-preflight.json).

- [x] Build novo em target-d28-atual:420 testes,0 falhas/erros/ignorados,20 XMLs, sem IT SQL que exige banco vazio. SHA4304D428…E8900CE1 reproduz o conteúdo preservado. [Conferência](orchestracao/.runtime/d28-build-conferido-farol.json).

- [x] Reexecutar ajuste zero/parcial/rollback/replay, contagem/carga e temporal; financeiro50/aprovação/reabertura/v2; XML/RN22/extremos; permissões/filtros/FIFO/avaria e concorrência HTTP/JVM. Limite nativo abaixo.

- [x] Confrontar HTTP/GET/snapshots/auditoria/saldos por SELECT novo:17 fotos atuais1315/0 e focais211/0; IDs/perfis/UUIDs/hash do corte preservados, sem contar focal como nova consulta.

- [x] Revisão independente favorável com limites,29 vínculos/2574 checks de referências sem diferenças; processos próprios encerrados, Graphify AST atualizado e recibo final somente em orchestracao/.runtime/.

- [ ] Witness SQL nativo simultâneo: impedimento concreto SQL300 sob WMSDEV; somente CONNECT SQL/VIEW ANY DATABASE observados, sem autorização de sa/grants. Alternativa HTTP/JVM+gate SELECT/rollback e SELECT posterior comprovada separadamente.



**Próxima ação D28:** aguardar nova demanda. Não iniciar nova jornada, fixture, IT vazio, migration ou reset por checkpoint/prompt antigo. Reds de roteiro/coletor/precondição permanecem classificados, com green/complementos novos; o modo fresco do auxiliar saldo XML foi removido/bloqueado antes de credenciais/JVM/SQL/HTTP após a continuação green; guarda isolada3/0, sem nova jornada. Persistência das permissões em novo chat e flags reais de início não confirmadas. [Limites e recibo](orchestracao/.runtime/d28-resultado-final.json). D27 abaixo permanece histórica e concluída.



**D27 concluída no escopo técnico comprovado, 06/10/2026:** retomada autorizada às21:33 BRT nos mesmos chats WMS. V10 DEV, ajustes/rollback, financeiro50 aprovado, RN22/XML, filtros/perfis/FIFO/avaria e concorrência com witness SQL online executados. Build atual420/0/0/0;726 HTTP (715 individuais aprovados,4 dos dois pares e7 divergências históricas corrigidas/classificadas),248 assertivas (247 aprovadas e1 guarda histórica que recusou retomada já reservada antes de gate/POST). [Resultado e limites](docs/42-validacao-tecnica-api-sql-dev.md), [parecer favorável Vigia](orchestracao/.runtime/d27-vigia-final.md), [recibo final WMS](orchestracao/.runtime/d27-resultado-final.md). API WMS_DEV/WMSDEV, escritas fictícias por HTTP e históricos preservados; nenhum achado material atual aberto no incremento revisto. D26 abaixo permanece histórico. Aguardar nova demanda, sem iniciar outro bloco.



### Retomada D27 — BE03–BE15



- [x] Inspecionar alvo administrativo/TLS/CHECK/histórico e revisar V10; aplicar Flyway exclusivamente WMS_DEV por executor DEV protegido. [Conferência real](orchestracao/.runtime/d27-migration-conferida-farol.json): PreValidate/Migrate/PostValidate/Info exit0, V10 success/checksum−562012523, seis tipos/habilitada/confiável; V1–V9/histórico/permissões/fixtures preservados, nenhum PROD.

- [x] Revalidar E052/AC13 por HTTP próprio: ajustes50→45,50→0 e45→35, replay/recusas/reserva ativa/reversão/revisão superada e GET. [SELECT próprio28/0](orchestracao/.runtime/d27-prumo-ajustes-final-20261007T014149078.json): físico/disponível35, reservado0, ocupação/ID e marcos preservados, fatos/movimentos/auditoria/snapshots únicos, recusas sem efeitos e cálculo25 legitimamente PENDENTE/totalNULL sem contrato.409 anterior/localização e rollback preservados; sem DELETE/grants/DDL adicional.

- [x] Executar jornada financeira completa não zero em fixture nova válida no corte real, sem alterar relógio/fatos históricos: mínimo fictício50, aprovação/replay/recusas/reabertura/versão2 preservando versão1. HTTP inicial51/13 e JAR final19/6; [SELECT WMSDEV/DEV65/0](orchestracao/.runtime/d27-prumo-casos-20261007T012544553.json) inclui memória/versões/auditoria/snapshots e ausência de emissão. Validação delimitada desse contrato, não de todas fórmulas.

- [x] Confrontar lacunas técnicas selecionadas da matriz D26: RN22/XML com cinco limites400 e positivo/replay/SELECT20/0, filtros/paginação/perfis/alcance; variantes FIFO/tarifas/GRIS/avaria com fotografia106/0. [Matriz](backend/evidencias/d27-matriz.md)161 métodos positivos combinados D26/D27,61 D27 em diferentes JARs; não161 no último artefato. [Green concorrente](backend/evidencias/d27-D2748A39681-http.json)16HTTP/2assertivas, witness SQL82→64→88 verdadeiro e [SELECT final24/0](orchestracao/.runtime/d27-prumo-concorrencia-final-select-20261007T024151964.json):100 físico/80 reservado/20 bloqueado/0 disponível, replay e perdedor sem efeitos. Propostas AC e combinações não executadas delimitadas.

- [x] Build/regressão proporcional/JAR final: [420/0/0/0,20 XMLs e SHA4304](orchestracao/.runtime/d27-build-numerico-conferido-farol.json). HTTP real no artefato final, leituras SQL compatíveis com banco populado; nenhum novo IT vazio. [Vigia final favorável](orchestracao/.runtime/d27-vigia-final.md) aos casos/limites, sem backend total ou AC homologada.

- [x] [21 JARs encerrados/42 portas sem listener](orchestracao/.runtime/d27-processos-conferidos-farol.json), helpers/readers próprios encerrados, dados/fixtures/históricos preservados. [Graphify AST WMS](orchestracao/.runtime/d27-graphify-ast-farol.json)5700 nós/21421 arestas, sem LLM/grafos alheios. Registros e [recibo final somente WMS](orchestracao/.runtime/d27-resultado-final.md).



**D26 — ensaios encerrados com falha aberta, 06/10/2026:** API real com Gestor/Supervisor/Operação e persistência exclusivamente WMS_DEV/WMSDEV, JWT HTTPS/JWKS/RSA efêmero e Hibernate validate. Toda escrita de negócio por HTTP fictício; identidade/DPAPI/direitos restritos atestados. Regra permanente Hermes AGENTS.md:55 preservada. Cedro API, Prumo acesso/SELECT, Vigia revisão, Farol registros; nenhum frontend/PROD/reset/DELETE/DDL de schema/sa na aplicação.



**Resultado D26:** build novo atual412/0/0/0 em20 XMLs e SQL IT7/0/0/0 antes da população.1.404 requisições HTTP:1.375 aprovadas individualmente,8 dos quatro pares concorrentes e21 tentativas históricas divergentes;159/160 métodos positivos, não aprovação de todas regras. Correções de revisão inicial e rollback-only financeiro reexecutadas em JAR412. SELECT próprios236/0, GET/cálculo4 17/0 e adicional financeiro76/0; históricos/memórias/fixtures preservados. E052 falhou409 por CHECK sem AJUSTE_ESTOQUE, rollback42/0 e V10 proposta inativa. Snapshot financeiro20 não aprovado: fato anulado, recálculo0 e409 CALCULO_DESATUALIZADO correto. [Resultado41](docs/41-ensaio-http-e-persistencia-dev.md), [build](orchestracao/.runtime/d26-build-financeiro-conferido-farol.json), [recibo WMS](orchestracao/.runtime/d26-resultado-final.md). Backend total não aprovado.



**D25 — entrega local e leituras DEV concluídas, 06/10/2026:** clean verify atual com **408 testes/0 falhas/0 erros/0 skips em 20 XMLs**, JAR atual com **6/6 verificações HTTP** em loopback, processo encerrado e porta liberada. Correções de TLS cliente, critério de lock, perfil do IT e cookie de sessão local revisadas por Vigia. SQL Server real WMS_DEV por SqlClient/JDBC administrativo somente SELECT: 64 tabelas vazias/687 colunas, 1.295 itens de catálogo, nove migrations/histórico iguais à D24. **API/JPA validate e sete ITs SQL não executados: identidade própria restrita ausente.** Nenhum DDL/DML/grant/SQL PROD. Regra AGENTS.md:55 de Hermes preservada. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md) e [recibo WMS](orchestracao/.runtime/d25-resultado-final.md). BE03/BE15 continuam em validação externa; D22/D23 são históricos.



**D24 concluída no escopo SQL administrativo, 06/10/2026:** conexão compartilhada em `../.runtime/sql-server` provisionada e BAT real concluído DEV → PROD. Ambos ONLINE, 64 tabelas, 687 colunas, nove migrations e 1.295 itens de catálogo conferidos por banco. SQL Server 2022 Standard 16.0.1000.6 preservado, mesmo processo, sem reinício. [Resultado e limites](docs/39-conexao-compartilhada-e-sql-real.md); [leitura autenticada](database/evidencias/d24-resultado-real.json). Backend/piloto continuam separados. D20–D23 abaixo são históricos, superados quanto ao bloqueio de autenticação e à exigência de fonte exclusiva WMS.





**Continuação focal D23, 06/10/2026:** padrão de autenticação dos runners públicos comparado; fonte administrativa SQL compartilhada não localizada/documentada nos guias/metadados autorizados de ../.runtime. 33/33 booleanos de presença dos canais consultados ausentes; configs de referência existem sem leitura do conteúdo, DPAPI WMS ausente. O caminho fixo D22 não recebe a configuração como os demais. Menor integração: somente aquisição de credencial no launcher existente, após fonte administrativa protegida explicitamente disponibilizada ao WMS; sem novo configurador ou credencial alheia. Nenhuma conexão nova/DDL; servidor indisponível não comprovado. [Comparação e resultado](orchestracao/.runtime/d23-comparacao-fontes-autenticacao.md).



**Histórico D23 — acesso então bloqueado em 06/10/2026:** Lucas autorizou bootstrap/upgrade real DEV → PROD pelo agente, sem configurador/prompts, com identidade Windows corrente ou fonte protegida WMS existente. Probe SqlClient confirmou TLS -2146893019; pin público privado WMS verificado resolveu esse bloqueio no sqlcmd Go. SQL Server então recusou logon de ROD-SRVW-001\suporte. Credencial protegida WMS AUSENTE. Nenhuma leitura sys.databases/histórico/schema ou DDL concluída; permissões não determinadas. Única ação indispensável: disponibilizar autenticação SQL válida já autorizada em canal protegido exclusivo WMS, fora do chat, sem grants ou alterações globais. D22 abaixo é histórico local, não conclusão da execução real D23.



**Histórico D22 em 06/10/2026:** D22 BE03/BE15 concluído localmente: BAT normal automático sem senha/confirmação, DPAPI WMS exclusiva e configurador UMA VEZ. 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão Vigia favorável. Credencial real AUSENTE por metadados, provisionamento pelo operador pendente; nenhum segredo real/SQL pelo agente. [Resultado38](docs/38-launcher-automatico-e-credencial-protegida.md) e [recibo WMS](database/evidencias/d22-relatorio-final.md). D20/D21 abaixo são históricos preservados.



Atualizado em06/10/2026. **D21 BE03/BE15 concluída localmente:** BAT de bootstrap/upgrade completo WMS_DEV e WMS_PROD, DEV primeiro, pela sequência vigente do Flyway. 41/41 fixtures,672/672 checks de pacote,25/25 fixtures Maven,cmd.exe real offline0/0 e inválidos2/2;675 hashes database e84 backend conferidos, revisão Vigia favorável. V1–V9 e históricos D20 preservados. Nenhum SQL pelo agente; ensaio real será manual pelo operador. [Resultado37](docs/37-bootstrap-e-upgrade-manual-database.md) e [recibo WMS](orchestracao/.runtime/d21-resultado-final.md).



## Resumo histórico D27/D29 — preservado, substituído pelos resultados datados acima

Esta fotografia pertence ao histórico anterior ao frontend e ao login D32. Não descreve a fonte nem o escopo atual; consultar o primeiro bloco deste arquivo para a rodada vigente.



| Frente | Situação comprovada |

| --- | --- |

| Documentação | PDF e Word preservados; respostas e regras consolidadas; arquitetura e cenários documentados |

| Backend | D27 concluída tecnicamente nas jornadas da matriz: build420/0/0/0,726 HTTP novos e persistência DEV/concorrência SQL real.161 métodos positivos combinados D26/D27,61 na D27 em artefatos distintos; parecer Vigia favorável com limites por caso. Homologação operacional e frontend separados. |

| Frontend | Somente orientação de estrutura; sem aplicação React/TypeScript, dependências, build ou testes executados |

| Banco | D27: V10 aplicada/conferida somente WMS_DEV, dez migrations SQL e CHECK com AJUSTE_ESTOQUE confiável/habilitado;64/687 preservados. PROD não acessado na D27, última fotografia D24 com V1–V9. SQL Server 2022 Standard preservado. [DEV real](orchestracao/.runtime/d27-migration-conferida-farol.json), [histórico D24](docs/39-conexao-compartilhada-e-sql-real.md) |

| Ambiente e piloto | Sem aplicação publicada; equipamentos, infraestrutura e recuperação ainda sem validação no WMS |

| Escopo autorizado agora | D29 em andamento por nova autorização expressa: cobertura documental/técnica ampla, famílias fictícias API somente WMS_DEV/WMSDEV. Limites em d29-autorizacao-e-distribuicao; D28 histórica encerrada. |



Base definida: React/TypeScript, Java/Spring com MVC convencional e SQL Server existente. Primeiro piloto em Osasco, depois Castro/Tigre; Caio valida a operação. A necessidade informada de iniciar em 13/10/2026 não representa estimativa técnica nem promessa de entrega.



## Onde consultar e como retomar



- [AGENTS.md](AGENTS.md): orientações de trabalho e de atualização deste arquivo.

- [Decisões](docs/06-decisoes-e-pendencias.md): escolhas do responsável e estado dos temas Q01 a Q27.

- [Respostas](docs/10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](docs/11-alinhamentos-apos-respostas.md): comportamento recebido e interpretações/propostas AC01 a AC16.

- [Arquitetura](docs/02-arquitetura.md), [fluxos](docs/04-fluxos-operacionais.md) e [cenários](docs/08-cenarios-de-validacao.md): responsabilidades, sequência operacional e verificações V01 a V48.

- [Plano geral](docs/07-plano-de-entregas.md) e [continuidade](docs/09-continuidade.md): visão da entrega e contexto resumido.



Escopo local D19 preservado nos [28](docs/28-validacao-separacao-retirada-retornos.md), [30](docs/30-validacao-servicos-e-calculo.md), [32](docs/32-validacao-fechamento-e-contingencia.md), [34](docs/34-matriz-e-validacao-final-backend.md) e [35](docs/35-modelo-integrado-e-jornadas-backend.md). D24 comprovou migrations; D25 é histórico de build/leituras; D26 executou API/JPA/SQL com identidade própria, conforme [41](docs/41-ensaio-http-e-persistencia-dev.md). [D27](docs/42-validacao-tecnica-api-sql-dev.md) resolveu ajuste/RN22/financeiro e variantes técnicas selecionadas; homologação/publicação/frontend permanecem fora do escopo. Pendências SQL citadas nas evidências antigas descrevem a data original e não substituem esse resultado atual.



**Próximo passo vigente:** executar D29 no DEV com novas provas e revisão independente, sem pedir autorização por etapas internas; fechar comprovável ou apontar bloqueio concreto e dependência externa exata.



**Histórico D20, substituído por D21 no launcher:** alvo confirmado TCP `127.0.0.1:1433`, SQL auth `sa`, conexão inicial `master`. Lucas passou à execução **manual**: `iniciar-bancos.bat` deve criar ambos os nomes ausentes, DEV primeiro, preservar/conferir existentes e permitir retomada; PROD sem migrations/cargas. Senha oculta local em memória e confirmação de identidade/alvo na interação. Nenhum SQL pelo agente; TCP alcançável/MSSQLSERVER Running não comprovam identidade SQL. Regra imutável do servidor em AGENTS/D20; guia compartilhado sem senha autorizado, sem ler `.env`/credenciais/dados/ETL nem alterar terceiros. Falta de credencial no canal do agente é observação histórica, sem impedir o launcher.



**Organização da mesma D20 entregue:**34 movimentos byte a byte,489 originais preservados, raiz database somente README curto e BAT. Scripts/docs/config nas pastas próprias; históricos, fontes e manifestos anteriores intactos; V1–V9 sem renomeação/alteração. Caminhos/BATs/links e testes conferidos após os movimentos, manifesto corrente722 hashes e revisão Vigia favorável ao layout/launcher. Sem SQL pelo agente ou novo build backend integral.



**Ressalva VIG07 do runtime:** guarda enumera recusas, não certifica mínimo privilégio geral. Antes do uso/ensaio do backend, exigir atestação completa das identidades/permissões próprias. Java/JAR396/404 são histórico D20; nenhuma atestação SQL executada. Não condiciona o bootstrap/migrations administrativo manual D21 com sa a novos grants/conta de aplicação.



### Conferência D26 — BE03/BE04–BE15



- [x] Preservar a regra Hermes AGENTS.md:55; SQL de testes somente WMS_DEV, sem PROD/fallback.

- [x] Provisionar identidade própria WMSDEV/DPAPI exclusiva, atestar direitos efetivos e recusas DDL/histórico imutável/PROD por metadados.

- [x] Executar clean verify novo412/0/0/0 e sete ITs nativos SQL7/0/0/0 antes da população.

- [x] Executar jornadas HTTP reais dos três perfis, escrita fictícia pela API, Hibernate validate e JWT efêmero HTTPS/JWKS sem desabilitar segurança.

- [x] Inventariar160 métodos;159 positivos, E052 falhou/bloqueado, matrizes RN/AC com limites explícitos.

- [x] Confrontar persistência/estoque/auditoria/memórias: fotografia236/0 e cálculo corrigido/GET17/0, além dos recortes separados.

- [x] Corrigir primeira revisão/INSERT e rollback-only financeiro, com red/green e repetição HTTP real no JAR412.

- [x] Executar último complemento financeiro após corte UTC real e SELECT76/0: snapshot20 preservado, fato anulado/recálculo0 e aprovação409 corretamente recusada.32 JARs encerrados/64 portas livres; nenhuma aprovação20 alegada.

- [x] Encerrar a [revisão independente](orchestracao/.runtime/d26-vigia-final.md) e consolidar o recibo WMS: VIG14 corrigido, VIG15 delimitado, E052 aberto; Cedro/Prumo/Vigia finalizados, Farol encerra este ask.

- [ ] Aprovar ajuste físico E052: depende de migration nova em escopo separado DEV; V1–V9 imutáveis, rollback42/0 comprovado.

- [ ] Aprovar backend total: variantes/lacunas da matriz, witness SQL simultâneo, homologação operacional/provedor/equipamentos/recuperação permanecem separados.



### Conferência histórica D25 — BE03/BE15/BE16



- [x] Preservar a regra permanente de Hermes em AGENTS.md:55: testes SQL apenas WMS_DEV, recusar PROD/outros/fallback.

- [x] Inspecionar alvo real, integridade, catálogo, vazio, histórico e existência de identidade da aplicação por leitura administrativa DEV.

- [x] Executar clean verify novo e corrigir achados locais; 408/0/0/0, 20 XMLs finais conferidos por Cedro, Vigia e Farol.

- [x] Executar JAR atual com seis checks HTTP locais, encerrar a própria JVM e conferir porta liberada.

- [x] Comprovar TLS/JDBC standalone com dois SELECTs administrativos DEV; não equivale a pool/JPA da aplicação.

- [x] Obter revisão independente favorável, registrar evidências e fechar o pacote sem callback Hermes/ETL.

- [ ] Disponibilizar e atestar identidade própria/credencial protegida restrita exclusivamente WMS_DEV — operação administrativa ainda não autorizada/executada.

- [ ] Executar API/JPA validate e sete sqlserver-it no DEV com essa identidade; nenhum fixture SQL criado na D25.



Escopo local concluído; os dois últimos itens impedem o ensaio SQL da aplicação e não anulam os testes independentes. [Resultado e proposta40](docs/40-testes-backend-e-leitura-sql-dev.md).



## Convenções do acompanhamento



| Status | Significado |

| --- | --- |

| Planejado | Escopo descrito, trabalho ainda não iniciado |

| Em andamento | Há trabalho efetivo, mas faltam partes ou verificações |

| Em validação | Entrega preparada e aguardando verificação identificada |

| Bloqueado | Impedimento concreto registrado, com responsável ou próximo encaminhamento |

| Concluído | Critérios atendidos, evidência e limites registrados |

| Adiado | Fora da entrega atual, com motivo e condição de retomada |



Uma caixa marcada significa que aquela ação foi concluída; não conclui automaticamente a etapa. Ao mudar um status, atualizar também a evidência. **BE01/BE02/BE16 concluídas localmente; BE03–BE15 em validação externa, com escopo local D19 aceito**. Nenhuma caixa externa foi marcada sem evidência. Etapas FE permanecem planejadas. A preparação documental abaixo também está concluída.



As dependências indicadas precisam estar disponíveis para concluir a etapa. Estudo de contratos e protótipos pode avançar antes, com dados fictícios e indicação de simulação. Protótipo não comprova integração. Não usar percentual de avanço calculado apenas pela quantidade de caixas.



## Preparação documental concluída



- [x] **DOC01** Estrutura `backend`, `frontend`, `database`, `infra` e `docs`, com finalidade registrada nos respectivos arquivos de orientação.

- [x] **DOC02** Fontes originais preservadas e 110 respostas analisadas, com regras e propostas distinguíveis nos documentos 10 e 11.

- [x] **DOC03** Arquitetura, fluxo geral e 48 cenários futuros de validação documentados, sem alegar testes executados.

- [x] **DOC04** `AGENTS.md` adaptado e este `states.md` criado com trilhas separadas, dependências e critérios de conclusão.



Evidência: arquivos presentes na raiz e em `docs`, referências verificadas e histórico ao final. Isso comprova documentação, não funcionalidades executáveis.



## Preparação da equipe no Maestri



### OR01 Equipe WMS com Hermes próprio



**Status:** Em validação. **Base:** D14. A preparação não altera os status das etapas BE/FE nem inicia outro bloco de implementação.



- [x] Criar Farol, Cedro, Lume, Prumo e Vigia com papéis WMS exclusivos e diretório do WMS; Farol com Maestro próprio.

- [x] Criar Hermes WMS real com perfil `wms-rodogarcia`, memória e sessões próprias, sem clonar o perfil anterior ou trocar o perfil padrão.

- [x] Ligar os quatro especialistas ao Farol e Hermes WMS ao Farol; compartilhar a nota própria somente entre os seis terminais WMS, sem ligação ao ETL.

- [x] Preservar nomes, pastas, papéis e posições dos seis terminais antigos, as oito conexões de agentes e as duas da nota antiga.

- [x] Registrar papéis, IDs, limites e responsabilidades em `orchestracao/` e no [documento 17](docs/17-orquestracao-maestri-hermes.md).

- [ ] Após escolha de privacidade no Hermes WMS e liberação de confiança da pasta nos agentes Codex pelo usuário, conferir respostas breves e o caminho Hermes WMS → Farol, sem iniciar implementação.



**Evidência da preparação D14 em 05/10/2026:** seis terminais WMS no mesmo canvas Projetos, por orientação do usuário. Listagem do Farol confirmou quatro especialistas, Hermes WMS e nota própria. Auditoria das conexões confirmou cinco ligações internas de agentes, seis da nota WMS e nenhuma entre WMS e ETL; o grupo antigo permaneceu preservado nos campos conferidos. Cadastro em [equipe.json](orchestracao/equipe.json) e [perfil Hermes](orchestracao/hermes/README.md). Naquela conferência, Hermes WMS abriu `Help improve Hermes?` e os agentes Codex aguardavam `Trust and continue`; esse é um registro histórico, não o estado de todos os terminais após D18. Telegram WMS não configurado; gateway, ponte e perfil padrão existentes não foram reconfigurados.



**Atualização D18/D19:** Farol recebeu as tarefas por este canal; `maestri list` confirmou os nomes/papéis e a nota própria. `ask`/`check` comprovaram leitura/entrega de Cedro/Prumo, parecer de Vigia e, em D19, parecer de contratos de Lume somente em leitura. Resultados D18 no [documento 25](docs/25-validacao-pedido-saida-e-reserva.md) e andamento D19 no [26](docs/26-execucao-continua-backend.md). Não houve resposta automática a confiança/privacidade. Prumo/Vigia relataram ausência do CLI/ambiente Maestri próprio; Farol recebeu e encaminhou pelos terminais conectados. **Pendente OR01:** retorno autônomo de todos os workers e conferência completa do caminho Hermes WMS, sem iniciar frontend nem alterar Hermes. Essa conferência permanece separada do backend autorizado.



## Trilha backend



As entregas desta trilha ficam em `backend` e, quando necessário, `database` e `infra`. A organização permanece por camadas convencionais. Nomes de classes, tabelas e rotas serão definidos durante o detalhamento; os IDs abaixo identificam entregas de negócio.



### BE01 Modelo conceitual e contratos



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Status:** Concluído localmente. **Dependências:** DOC01 a DOC04.



- [x] Relacionar cliente, armazém, produto, embalagem/DUN, nota, pedido, recebimento, unidade, posição, reserva, movimento, serviço, tabela e fechamento.

- [x] Descrever transições, ações permitidas, quantidades, datas e precisão; separar estoque físico, disponível e cobrável.

- [x] Definir contratos de entrada/saída, erros, filtros e paginação para as jornadas FE01, preservando propostas identificadas nos documentos.



**Conclusão:** modelo e contratos explicam uma entrada, uma reserva, uma saída parcial de pallet e um fechamento sem contradição entre quantidades, identidade e datas. **Base:** RN02, RN10, RN16, PR05 e AC01 a AC13.



**Evidência histórica em 05/10/2026:** [contrato técnico inicial](docs/12-base-e-contratos-backend.md) de status, erros e identificação da solicitação, disponível para FE02. Naquela etapa faltavam modelo completo, transições, precisão, contratos operacionais, filtros/paginação e ligação com jornadas FE01. Esses itens foram complementados no fecho local D19 abaixo.



**Ampliação D12:** [modelo e contratos cadastrais](docs/14-cadastros-acesso-e-persistencia.md), incluindo quantidade/embalagem, paginação, revisão e encerramento pendente. O recorte atende aos cadastros de FE04; os contratos completos de entrada/reserva/saída/fechamento e jornadas FE01 ainda faltam. Não confundir precisão de embalagem entregue com cálculo de estoque/cobrança.



**Ampliação D15:** [contratos de recebimento](docs/18-recebimento-e-conferencia.md) para FE05, com notas, itens, fatos físicos, estornos, divergências, FIFO e entradas conferidas. BE01 continua parcial: unidade logística, posições, reserva, saída, serviços e fechamento ainda precisam do modelo integrado.



**Ampliação D16:** [contratos de unidades](docs/20-unidades-logisticas-e-etiquetas.md) para FE06, com identidade permanente, composição por entrada, conservação de quantidades, transformação e etiqueta. Permanecem posições, reserva, saída, serviços e fechamento em BE01.



**Ampliação D17:** [capacidade, posições, movimentos e saldo](docs/22-enderecamento-movimentacao-e-estoque.md) para FE07/FE08. Reserva, saída, serviços e fechamento continuam pendentes em BE01; regras AC04/AC05 não se tornam aprovação comercial.



**Ampliação D18:** [pedido de saída, FIFO e reserva](docs/24-pedido-saida-fifo-e-reserva.md) para FE09 e saldo FE08; [197 testes](docs/25-validacao-pedido-saida-e-reserva.md) entregues. **D19:** BE10/BE11 aceitos localmente no [contrato27](docs/27-separacao-retirada-retornos-e-avaria.md)/[evidência28](docs/28-validacao-separacao-retirada-retornos.md), build final de240 testes. Recorte BE05/BE12 aceito localmente pelo [contrato29](docs/29-cadastros-servicos-e-calculo.md)/[evidência30](docs/30-validacao-servicos-e-calculo.md), build292, revisão favorável e V7/JPA compatíveis em arquivos. Naquela entrega, faltava integrar fechamento/contagem do31 e completar o [modelo35](docs/35-modelo-integrado-e-jornadas-backend.md). AC10/AC11 permanecem propostas identificadas.



**Ampliação D19 após aceite BE13:** fechamento/ciclo/ajustes do31 aceitos localmente com333 testes, Vigia favorável e V8/JPA compatíveis, conforme32. Naquela fase, contagem/carga/contingência e encerramento BE14/BE05 ainda estavam em construção. Contratos/transições e percurso HTTP integrado foram complementados no fecho D19 abaixo; o diagrama sozinho não comprova execução nem frontend.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Modelo35/contratos14–31 e jornada HTTP própria ligam entrada em partes, etiquetas, armazenagem, pedido integral/parcial de pallet, retirada, remanescente, serviço único, cálculo e fechamento. Lume encerrou a divergência de replay de chegada sem alterar o contrato legado18. **Ligação FE:** FE01–FE12 por contrato; telas não entregues.



### BE02 Base Java e configuração dos ambientes



**Status:** Concluído. **Dependências:** contrato técnico inicial de BE01 disponível; modelo operacional integrado BE01 disponível localmente.



- [x] Definir versões compatíveis, ferramenta de construção, pacote e procedimento de execução local.

- [x] Criar aplicação Spring nas camadas `controllers`, `services`, `repositories`, `models`, `dto`, `config` e `exceptions`.

- [x] Preparar configuração por ambiente, mensagens de erro, identificação das operações e estrutura de verificação sem credenciais versionadas.



**Conclusão:** aplicação inicia em ambiente de desenvolvimento definido, possui construção reproduzível e informa falhas sem expor segredos. Inicialização não pode operar por engano no ambiente real. **Base:** D03 a D05, T01 a T03, T07.



**Evidência da entrega inicial D11 em 05/10/2026:** [escolhas/contratos](docs/12-base-e-contratos-backend.md) e [validação](docs/13-validacao-base-backend.md): 12 testes, JAR local/loopback e recusa de `prod`. Naquela entrega, models/repositories eram apenas pacotes. D12 acrescentou cadastros e acesso, descritos em BE03/BE04/BE05 abaixo; [backend/README.md](backend/README.md) reflete o estado atual. **Ligação FE:** contrato de base para FE02; frontend e M01 continuam abertos.



### BE03 Estrutura SQL Server e migrações



**Status:** Em validação externa; D24 bootstrap real, D26 identidade WMSDEV/atestação e D27 V10/evolução com dados/API/concorrência DEV executados. **Dependências:** BE01, BE02; recuperação por backup/restauração e implantação operacional continuam pendentes. sa somente administrativo autorizado, nunca API.



- [x] Definir esquema WMS, ferramenta/procedimento de migrations, proteção de alvo e separação de ambientes em arquivos.

- [x] Lucas informar nomes WMS_DEV/WMS_PROD e alvo TCP127.0.0.1:1433, sa/master administrativo; execução manual definida na D20.

- [x] Conferir identidade/TLS/defaults/versão/collation reais no bootstrap administrativo D24.

- [x] Definir e atestar identidade própria WMSDEV/credencial protegida e direitos efetivos no WMS_DEV (D26; SELECT/INSERT e UPDATE em179 colunas; nenhum DELETE/DDL/role admin/PROD).

- [x] Preparar V1–V9, vínculos, restrições, histórico e procedimentos em arquivos, sem aplicar SQL.

- [x] Entregar launcher manual, organizar database com preservação e conferir fixtures/cmd.exe/caminhos/manifesto/revisão local D20.

- [x] Verificar criação, aplicação V1–V9 e retomada do BAT no SQL Server real, com DEV antes de PROD (D24).

- [x] Ensaiar evolução com dados, falhas/rollback e concorrência em WMS_DEV (D26/D27), preservando migrations/histórico/fixtures.

- [ ] Validar recuperação por backup/restauração em escopo próprio, sem reset do DEV para repetir ensaio.

- [x] Preparar o recorte cadastral: seis tabelas, mapeamentos JPA, V1, ferramenta/procedimento de migrations e proteção contra DDL automático.

- [x] Preparar V2 e seis modelos de recebimento, vínculos, restrições, índices e ampliação da auditoria, preservando V1.

- [x] Preparar V3, três modelos de unidades/composição/repetição e marcador de unitização, preservando V1/V2 e os fatos conferidos.

- [x] Preparar V4, perfil físico, conjuntos, ocupação/histórico e revisão independente de etiqueta, preservando V1/V2/V3.

- [x] Preparar V5, pedidos/itens, reservas históricas, operações idempotentes, ponte exclusiva e avaria posterior, preservando V1–V4.



**Conclusão:** estrutura pode ser criada e atualizada de modo controlado no alvo correto; inconsistências de origem e duplicidade são impedidas. Preparar script não equivale a aplicar migration. **Base:** RN04, RN16, RN29, PR02, PR04 e T03.



**Evidência D12/D15/D16/D17/D18:** [V1 a V5 e procedimento](database/migrations/README.md), configuração `sqlserver-dev` e testes locais dos documentos 15/19/21/23/[25](docs/25-validacao-pedido-saida-e-reserva.md). V5 revisada contra JPA por Cedro/Prumo/Vigia; índice filtrado, CHECKs e opções JDBC não executados no H2. **Pendente:** alvo/credenciais/versão SQL Server, aplicação e validação de V1 a V5 (inclusive evolução com dados), permissões/recuperação e ensaio integrado das V1–V9 preparadas. H2 não valida dialeto/concorrência SQL Server. Nenhuma migration foi aplicada. **Ligação FE:** persistência FE04 a FE09, sem integração real concluída.



**Complemento D19 de BE03:** V6/V7/V8 preparadas e comparadas com os respectivos freezes locais aceitos, sem SQL executado. V8/JPA523/523 e163/163; V9/fonte424/424 em leitura estática, oito tabelas/79 colunas, incluindo solicitação de encerramento e ENTRADA_CONTINGENCIA; preparo técnico194/194, fecho22/22 e307 hashes conferidos. Outputs413/18/250 preservados como histórico. JPA/guardas finais BE14 conferidos no fecho P2-locks: 904/904 e129/129, somente em arquivos. Validação de todas as migrations no alvo real, evolução/locks/permissões/recuperação continuam externas com Lucas/DBA.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Flyway externo ao build, ddl-auto=validate e SQL-init=never. H2 não comprova dialeto, índice filtrado/CHECK, isolamento, deadlock, collation ou recuperação SQL Server. **Externos:** Lucas/TI e DBA.



**Fecho local D20:** auditoria efetiva64 tabelas/687 colunas/130 FKs/200 CHECKs/61 UNIQUE/83 índices e0 divergências JPA em arquivos;34 UPDATEs Hibernate preparados, sem JDBC. Sem novo índice/migration/modelo de negócio. BAT pronto/testado, V1–V9 iguais9/9,34 movimentos/489 originais e722 hashes conferidos; revisão favorável ao launcher/organização. Integração backend396/19/404 preservada. Limite VIG07 e atestação externa obrigatória explícitos no [36](docs/36-database-local-engenharia-e-validacao.md). SQL real não executado.



**Fecho local D21 de BE03:** [BAT](database/iniciar-bancos.bat) pronto para bootstrap/upgrade completo manual dos dois nomes exatos. Migrations canônicas dinâmicas, V1–V9 intactas, V10 em fixture isolada; checksum/schema divergente e falha DEV impedem PROD.675 hashes database,84 backend, revisão favorável e evidências no [37](docs/37-bootstrap-e-upgrade-manual-database.md). Criar/aplicar/validar SQL real permanece externo.



**Fecho local D22 de BE03:** execução normal automática sem senha/confirmação, canal DPAPI WMS exclusivo por usuário/máquina, auxiliar [configurar-credencial.bat](database/configurar-credencial.bat) UMA VEZ. 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão favorável. Diretório/arquivo real ausente: operador configura, depois executa BAT; nenhuma provisão/importação real/SQL pelo agente. [38](docs/38-launcher-automatico-e-credencial-protegida.md). D21 permanece histórico, mesmos IDs/modelo/migrations.



**Evidência atual D24 de BE03:** [resultado real39](docs/39-conexao-compartilhada-e-sql-real.md); ambos os catálogos conferidos com a fonte e histórico Flyway validado. Os parágrafos D12–D22 anteriores descrevem o estado de cada entrega histórica.



**Evidência D26 de BE03:** WMSDEV/DPAPI/ACL própria provisionada e direitos completos atestados; JPA validate e IT SQL7/0/0/0 reais antes da população, fixture preservada. Jornadas HTTP159/160, provas236/0+17/0+76/0 e rollback42/0 do ajuste CHECK. V1–V9/histórico iguais D24; V10 só proposta não aplicada. [Resultado41](docs/41-ensaio-http-e-persistencia-dev.md). BE03/BE15 em validação; backend total não aprovado.



**Evidência D25 de BE03:** catálogo e histórico DEV reobservados por leitura SqlClient/JDBC protegida: 1.295/1.295 itens, 64 tabelas/687 colunas, 64 vazias e nove migrations iguais à D24. Nenhum DDL/DML/migration/reparo; identidade própria ausente, JPA validate não executado. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



### BE04 Identidade, permissões e auditoria

**Atual D32 / AUD-LOGIN-CAD01:** login e usuários próprios implementados; mecanismo nativo, contratos e ativação DEV04 descritos no [documento 43](docs/43-login-e-administracao-de-usuarios.md). Auditoria BE04-AUD01/FE03-AUD01 concluída com sete achados abertos e propostas no [recibo](orchestracao/.runtime/login-cadastros-auditoria-resultado.md); correções aguardam Lucas. Primeiro login real/negócio autenticado não presumido por esta auditoria.

#### Histórico do item antes de D32 — preservado




**Status:** Em validação externa; acesso técnico HTTP/SQL DEV e auditoria nos casos D26/D27 exercitados; provedor/identidades operacionais não homologados. **Dependências:** BE02, BE03.



- [x] Implementar Resource Server RS256 e validação de claims/configuração conforme contrato local.

- [ ] Validar provedor, contas, login, renovação, revogação/rotação e contenção no ambiente real.

- [x] Aplicar Gestor, Supervisor e Operação, com alcance por cliente/armazém e proteção em cada ação relevante.

- [x] Registrar usuário, momento, motivo e origem/destino das operações críticas; verificar recusas de acesso.

- [x] Implementar o recorte cadastral: JWT assinado com emissor/audiência/prazo, três perfis, alcance por cliente/armazém e auditoria atômica das alterações.



**Conclusão:** acesso indevido é recusado mesmo sem usar a tela; justificativa e autorização ficam distinguíveis; o histórico permite atribuir ações. **Base:** Q20, AC11, PR06, V16 e V21.



**Evidência D12:** contrato de identidade/auditoria no [documento 14](docs/14-cadastros-acesso-e-persistencia.md), testes RSA/escopos/recusas/rollback no documento 15. **Pendente:** provedor real, ciclo de contas/login/renovação/revogação, validação de permissões no SQL Server e integração do provedor real com as ações operacionais implementadas. **Ligação FE:** contrato para FE03; login/telas ainda não implementados. O backend não emite tokens nem possui usuários padrão.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Perfis/alcances/resolução antes do replay e auditoria/rollback verificados nos serviços, incluindo BE14. Ausência de provedor/política externa não foi preenchida com emissor ou blacklist inventados. **Ligação FE:** FE03; login/telas não implementados.



### BE05 Cadastros, contratos e posições



**Status:** Em validação externa; código e jornadas HTTP/SQL DEV D26/D27 conferidos no recorte da matriz, cadastros/preços reais não homologados. **Dependências:** BE03, BE04 disponíveis para o ensaio.



- [x] Implementar clientes, armazéns, endereços, produtos, embalagens/DUN, lotes/validade aplicáveis e dados fiscais de referência.

- [x] Implementar importação de endereços com conferência de erros e duplicidades, posições especiais e limites de ocupação.

- [x] Manter tabelas/serviços, unidades de cobrança, vigências e parâmetros contratuais; aplicar encerramento/inativação conforme AC12.

- [x] Entregar API de cliente/armazém/produto/embalagem/endereço, consultas paginadas, revisão, auditoria e solicitação de encerramento/reativação.



**Conclusão:** cadastros respeitam proprietário e armazém; não há posição ambígua nem alteração silenciosa de preço histórico. **Base:** RN04 a RN09, AC05, AC06, AC10 a AC12 e AC16.



**Evidência D12/D13:** cinco conjuntos de models/repositories/services/controllers/DTOs; [contratos e limites](docs/14-cadastros-acesso-e-persistencia.md),52 testes no15. **D19:** importação Excel, tabelas/serviços/valores/vigências e complemento fiscal aceitos localmente no29/30,292 testes. **Complemento final D19:** inativação definitiva com revalidação de compromissos BE13/BE14 e avisos de validade entregues/testados localmente. Flags de lote/validade/limites cadastrados; ocupação/estoque entregues em BE08. **Ligação FE:** contratos FE04, nenhuma tela/M01 entregue.



**D19 aceito localmente no recorte:** complemento fiscal, prévia/confirmar Excel e configuração comercial do [contrato29](docs/29-cadastros-servicos-e-calculo.md); clean verify292, revisão favorável Vigia e V7/JPA compatíveis, conforme [30](docs/30-validacao-servicos-e-calculo.md). Tabelas/serviços/vigências entregues; inativação complementada no fecho D19, conforme32. Referências fiscais não comprovam autorização externa.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Inativação definitiva dos sete tipos revalida compromissos físicos/financeiros na transação; resolução específica de Gestor não reativa cadastro nem dá disponibilidade geral. Encerramento de vigência conserva o passado. **Externos:** Caio/gestor/Natalina e dados reais.



### BE06 Recebimento, XML e divergências



**Status:** Em validação externa; jornadas técnicas HTTP/SQL DEV D26/D27 exercitadas. **Dependências:** acesso/cadastros BE04/BE05 e SQL DEV BE03 disponíveis; documentos reais/conferência física/homologação operacional pendentes.



**Macrobloco autorizado em 05/10/2026:** implementar pedido/nota manual ou XML, chegadas, conferência, quarentena, correção por estorno e efetivação única da quantidade física. Registro de entrada em triagem/quarentena prepara BE07/BE08; não representa saldo disponível, endereçamento ou cobrança. Aplicar AC03 como proposta identificada.



- [x] Criar pedido manual ou por XML, validar arquivo no formato operacional suportado e associar proprietário, notas, SKUs e quantidades, sem duplicar importação.

- [x] Registrar chegadas em partes, primeira data FIFO e conferência física; preservar previsto, recebido e divergente.

- [x] Bloquear a carga conforme AC03, registrar tratativa/liberação e efetivar uma única vez a quantidade real aceita.



**Conclusão:** nota em dois dias não duplica previsão nem libera saldo incompleto; liberação tem responsável e justificativa. **Base:** RN12 a RN17, AC03, V01 a V05 e V26 a V29.



**Evidência D15:** [modelo/API](docs/18-recebimento-e-conferencia.md), seis tabelas em V2 e [86 testes totais](docs/19-validacao-recebimento-backend.md), incluindo 28 cenários de recebimento e seis de segurança XML. **Pendente de validação:** persistência/concorrência no SQL Server e documentos/cenários operacionais reais com Caio. O extrator não valida fiscalmente assinatura, XSD completo ou autorização SEFAZ. **Entregas distintas:** unitização disponível em BE07 após D16; endereçamento/disponibilidade ainda em BE08 e correções após efetivação em BE11. **Ligação FE:** contratos FE05 disponíveis, tela não implementada. M02 não concluído.



### BE07 Unidades logísticas e etiquetas



**Status:** Em validação externa; unitização/etiquetas via API e persistência DEV D26/D27 exercitadas. **Dependências:** BE05/BE06/SQL DEV disponíveis; impressora/leitor/uso físico e homologação operacional pendentes.



**Macrobloco autorizado D16 em 05/10/2026:** unitização por entrada conferida, identidade permanente, divisão/reagrupamento rastreáveis e dados para etiqueta. Endereçamento e disponibilidade continuam em BE08; impressão/leitura física dependem de FE06 e equipamentos.



- [x] Unitizar quantidades conferidas com ID permanente e vínculo de origem; impedir mistura de SKU/lote/data não permitida.

- [x] Representar quantidade de produto e embalagem separadamente; manter vínculos em divisão ou reagrupamento autorizado.

- [x] Fornecer dados da etiqueta com SKU, ID, código de leitura, data, nota e quantidade; reimprimir preservando identidade.



**Conclusão:** quantidades unitizadas fecham com o recebido e etiquetas não criam saldo; mudanças de conteúdo permanecem rastreáveis. **Base:** RN10, RN11, RN15, RN16, AC10, V02 a V04, V09 e V25.



**Evidência D16:** [modelo/API e limites](docs/20-unidades-logisticas-e-etiquetas.md), V3 e [122 testes totais na entrega original](docs/21-validacao-unidades-logisticas.md), com 36 casos novos de conservação, permissões, repetição, concorrência e rollback. **Ampliação D17:** BE08 calcula disponibilidade e separa revisão da etiqueta; divisão/reagrupamento restringidos a unidades não endereçadas e sem bloqueio. **Pendente de validação:** persistência/locks no SQL Server e cenários físicos com Caio. **Ligação FE:** contratos FE06 disponíveis; layout, simbologia, impressão Tanca e leitura física ainda não implementados/validados. M02 não concluído.



### BE08 Endereçamento, movimentação e saldo



**Status:** Em validação externa; movimentos/saldo/API/SQL DEV D26/D27 confrontados nos casos publicados. **Dependências:** BE05 a BE07 disponíveis; geometria/equipamentos/uso operacional permanecem validações próprias.



**Macrobloco D17 autorizado em 05/10/2026:** capacidade física, conjuntos de duas posições, confirmação de leitura/endereço, movimentos atômicos, bloqueio/liberação e consulta de disponibilidade. Preservar AC04/AC05 como propostas e manter reserva, saída e cobrança nas etapas próprias.



- [x] Confirmar unidade/endereço, validar capacidade e ocupar conjuntamente as posições necessárias.

- [x] Centralizar alterações de estoque e localização, com proteção contra concorrência, repetição e gravação incompleta.

- [x] Expor físico, disponível e bloqueado; registrar primeiro endereço, início de armazenagem, equivalência e histórico. D18 integra reservado e avaria posterior conforme o documento 24.

- [ ] Conferir V4, locks, isolamento do saldo e permissões no SQL Server; validar capacidade real e leituras com a operação.



**Conclusão:** duas operações não ocupam indevidamente a mesma posição; remanejamento não cria entrada nem perde histórico. **Base:** RN18 a RN21, PR01 a PR07, AC04/AC05, V08, V14/V15, V23 e V30.



**Evidência D17:** [contratos e limites](docs/22-enderecamento-movimentacao-e-estoque.md), V4 preparada e [153 testes totais](docs/23-validacao-estoque-backend.md), com 31 novos de saldo, capacidade, concorrência entre clientes, quarentena, permissões, repetição e rollback. JAR local conferido e encerrado naquela entrega. **Integração D18:** saldo reservado, bloqueio por avaria posterior e lock do armazém antes de encerrar endereço; os 31 testes BE08 e os demais anteriores passaram no [build com 197 testes](docs/25-validacao-pedido-saida-e-reserva.md). **Pendente de validação:** itens externos acima. AC04/AC05 continuam propostas. **Ligação FE:** contratos FE07/FE08 disponíveis; telas/coletor não implementados. M02 não concluído.



### BE09 Pedido de saída, FIFO e reserva



**Status:** Em validação externa; pedido/RN22/FIFO/reserva e concorrência HTTP/SQL DEV D26/D27 exercitados. **Dependências:** BE04/BE08/SQL DEV disponíveis; provedor e operação reais não homologados. **Autorização:** D18/D26/D27 nos respectivos limites.



- [x] Criar pedido por proprietário/armazém, validar saldo por SKU e sugerir FIFO com data original e desempate documentado.

- [x] Confirmar reserva sem vencimento, com justificativa/autorização de exceção e proteção contra disputa simultânea.

- [x] Reservar quantidade parcial de pallet sem permitir atendimento parcial do pedido; sinalizar avaria posterior e pedido afetado.

- [ ] Homologar V5, concorrência, saldo e permissões no SQL Server e conferir os cenários com a operação.



**Conclusão:** reserva não duplica nem desaparece por tempo; quantidade e unidade permanecem coerentes; bloqueios impedem expedição. **Base:** RN23/RN24, Q07 a Q09, AC10/AC11, V06/V07, V13/V17, V24 e V31/V32.



**Evidência D18 em 05/10/2026:** [contratos](docs/24-pedido-saida-fifo-e-reserva.md), V5 preparada e [validação](docs/25-validacao-pedido-saida-e-reserva.md). `spotless:apply` e `clean verify` reais aprovados com JDK 21: **197 testes, zero falhas/erros/ignorados**, sendo 44 novos. Concorrência, pedido inteiro/rollback, repetição, FIFO/exceção, permissões/escopos, indisponibilidades, avaria posterior, reconciliação, cancelamento/reversão/auditoria testados em H2 isolado. Vigia favorável, sem novos achados materiais; schema JPA/V5 conferido em leitura. Graphify atualizado. Git `main`, zero commits e conteúdo preexistente preservado. **Pendente:** validação externa acima; H2 não comprova dialeto, índices, CHECKs, isolamento ou deadlocks SQL Server. **Ligação FE:** FE09 e saldo FE08 com contratos disponíveis, sem telas. AC10/AC11 continuam propostas. **Continuação D19:** BE10/BE11 aceitos localmente no [documento28](docs/28-validacao-separacao-retirada-retornos.md); demais blocos seguem pelo [plano26](docs/26-execucao-continua-backend.md), sem nova autorização por etapa.



### BE10 Separação, documentos e retirada física



**Status:** Em validação externa. **Dependências:** BE07, BE09 disponíveis localmente. **Autorização:** D19; código local e revisão concluídos junto com BE11.



- [x] Conferir leituras das unidades reservadas e completar separação com quantidades corretas.

- [x] Vincular documentos existentes do NOTAZZ/XML no procedimento inicial, inclusive várias notas e itens; impedir repetição divergente ou conclusão com pendência. Sem emissão pelo WMS.

- [x] Confirmar retirada por Supervisor/Gestor com XML, baixar estoque e registrar término da permanência conforme AC01; separar retorno simbólico de movimento físico.



**Evidência D19:** [contrato 27](docs/27-separacao-retirada-retornos-e-avaria.md) e [validação 28](docs/28-validacao-separacao-retirada-retornos.md). Spotless/clean verify reais com JDK21, **240 testes, zero falhas/erros/ignorados**, 43 expedição + 197 anteriores. Vigia favorável no freeze P2; V6 preparada e compatível em leitura, 74 colunas/58 checks. **Externos:** SQL Server/locks/dialeto com Lucas; documentos/procedimento fiscal com Natalina; percurso/equipamento com Caio/Mickael. H2 e XML extraído não homologam essas etapas. **Ligação FE:** FE09/FE10 com contrato, sem telas.



**Conclusão:** nota emitida hoje e retirada amanhã mantêm datas e saldo corretos; rejeição ou ausência de documento exigido não conclui a saída. **Base:** RN25/RN26, AC01/AC02, V10/V11, V18, V34 e V39.



### BE11 Cancelamentos, retornos e avaria



**Status:** Em validação externa. **Dependências:** BE06, BE08 a BE10 disponíveis localmente; código desenvolvido no mesmo bloco D19.



- [x] Cancelar pedido inteiro conforme os efeitos já ocorridos, preservando pendências fiscais e serviços prestados.

- [x] Retornar separado à posição sem criar recebimento; tratar retorno após retirada como nova entrada com vínculo e FIFO original.

- [x] Registrar quantidade avariada, destino, responsabilidade e momento reconhecido; fornecer os fatos para ajuste financeiro.



**Evidência D19:** mesmos contrato/build/revisão de BE10. Bases temporais de avaria por fato/snapshot imutável BE07, ou recusa explícita `HISTORICO_AVARIA_INSUFICIENTE`; reparo conserva condição original/reserva e considera somente o bloqueio do ciclo vigente. Retorno de 201 reservas e rollback integral, perfis no replay, duas posições e snapshot por ciclo testados. **Integração seguinte:** efeito financeiro BE12/BE13. **Externos:** tratativa e destino reais com Caio/cliente, fiscal com Natalina e demonstrativo com gestor. **Ligação FE:** FE10/FE11, sem telas.



**Conclusão:** cancelamento não libera mercadoria fisicamente separada ou avariada por engano; retorno externo conserva origem sem apagar saída anterior. **Base:** Q04/Q10/Q21, AC01/AC08/AC10, V17, V21, V33 e V46.



### BE12 Serviços e cálculo de armazenagem



**Status:** Em validação externa; código local aceito. **Dependências:** recorte comercial BE05 e BE08/BE10/BE11 disponíveis localmente. **Autorização:** D19; contrato/schema antes do código e evidência no [documento30](docs/30-validacao-servicos-e-calculo.md).



- [x] Registrar serviços únicos, quantidade, unidade, origem, responsável, execução e aprovação; impedir duplicidade entre sugestão e lançamento manual.

- [x] Implementar cálculo por calendário, posição equivalente, pico e vigência, incluindo mesma data, duas posições, separação, quarentena e avaria conforme propostas AC04 a AC08.

- [x] Parametrizar preços, mínimo e eventual GRIS com memória de cálculo; informação ausente não vira valor zero nem preço copiado de outro cliente.



**Conclusão:** exemplos documentais e testes explicam cada valor e período. Fórmula proposta fica identificada até conferência do demonstrativo real; não confundir valor da mercadoria com valor de serviços. **Base:** RN21/RN27, AC04 a AC09, V19/V20, V35, V40 e V46 a V48.



**D19 aceito localmente:** P2 financeiro BE08 corrigido por movimentos/ciclos imutáveis e pendência sem prova histórica. Clean verify **292/0/0/0**, BUILD SUCCESS às02:17:02; Farol conferiu14 XMLs/JAR/222 hashes,217 iguais ao freeze286/cinco diferenças declaradas. Vigia favorável às02:33 sem achados materiais; Prumo V7/JPA518/518 e suplemento67/67 compatíveis. Evidência no [30](docs/30-validacao-servicos-e-calculo.md), contrato no [29](docs/29-cadastros-servicos-e-calculo.md). Naquela etapa seguiu BE13 autonomamente; fecho integrado D19 abaixo. **Externos:** gestor/comercial valida AC04–AC09/preços/cortes, Natalina fiscal e Lucas SQL Server.



### BE13 Fechamento e entrega de valores ao ESL



**Status:** Em validação externa; código local aceito. **Dependências:** BE12 aceito localmente. **Autorização:** D19; refinamento31 antes do código/schemaV8, sem nova autorização do usuário.



- [x] Consolidar ciclos do cliente, respeitar último corte e exigir aprovação/rejeição integral do gestor.

- [x] Preservar versões, reabertura anterior à emissão e ajustes posteriores no próximo ciclo; impedir faturamento repetido.

- [x] Preparar demonstrativo para o procedimento inicial de NFS-e no ESL, com referência do documento emitido e sem duplicar tributos.



**Conclusão:** ciclos não repetem dias; correções explicam sua origem e documento faturado não é silenciosamente reescrito. **Base:** AC02/AC06/AC07, V12, V19, V36 e V45.



**D19 aceito localmente:** [contrato31](docs/31-fechamento-contagem-e-contingencia.md) e refinamentos registrados antes dos ajustes Java/V8. Três P2 finais corrigidos: ajustes originados, período e auditoria da resolução histórica. Bateria93/0/0/0 e clean verify **333/0/0/0**, BUILD SUCCESS04:17:56; spotless238 Java aprovado. Farol conferiu15 XMLs/JAR/251 hashes; Vigia favorável04:29, sem achado material remanescente. V8/JPA **523/523** e suplemento**163/163**,17 models/118 campos compatíveis em leitura;11 tabelas/112 colunas SQL preparadas. Histórico328/251 e anteriores preservados; [32](docs/32-validacao-fechamento-e-contingencia.md) registra outputs/limites. Índice nullable H2 não comprova o filtro SQL Server. **Externos:** Natalina/gestor validam procedimento ESL/fiscal e condições reais; Lucas/DBA, SQL Server. Naquela etapa seguiu BE14/inativação BE05, agora entregues localmente; nenhum envio/emissão/cobrança real.



### BE14 Consultas, contagem e contingência



**Status:** Em validação externa; código local aceito. **Dependências:** BE06 a BE13, conforme a consulta ou operação; consultas físicas disponíveis após BE08.



**Recorte D17:** lista por cliente/armazém, filtros de produto/disponibilidade, saldo por SKU e histórico paginado de movimentos entregues para FE08, conforme [documentos 22](docs/22-enderecamento-movimentacao-e-estoque.md) e [23](docs/23-validacao-estoque-backend.md). Naquele recorte, indicadores financeiros, contagem, carga inicial, ajustes e contingência não estavam entregues; foram complementados no fecho D19 abaixo.



**Complemento D18:** reservado e bloqueado reconciliados com o físico unitizado, indicação de avaria posterior e reserva vinculada à unidade no [documento 24](docs/24-pedido-saida-fifo-e-reserva.md); [197 testes](docs/25-validacao-pedido-saida-e-reserva.md) aprovados. Não entrega valor financeiro, contagem ou contingência.



**Andamento D19:** após aceite local BE13/333, Cedro formalizou schema/API BE14 e inativação BE05 no rodapé do [31](docs/31-fechamento-contagem-e-contingencia.md), antes do Java: oito tabelas de contagem/revisão, estágio/revisão inicial, linha/dependência de contingência, aviso de validade e resolução de remanescente. Backend/contrato31 atribuídos a Cedro; V9 somente em arquivos a Prumo; Lume conferiu o novo contrato em leitura. Check comprovou trabalho/leitura reais. Registro da implementação anterior ao fecho final D19; aceite/checklist atual abaixo.



**Revisão preliminar D19:** Lume encerrou os residuais de desenho da carga/dependências/efeito registrado; Vigia apontou um P1 e cinco P2 no código em construção, encaminhados a Cedro e formalizados no31 antes do ajuste. Os achados exigiam correção de revisão antiga, tempo do vínculo, obrigação financeira, cortes de vigência/tabela padrão e ordem de locks; foram corrigidos e verificados no fecho abaixo. Após correções, bateria de integração físico-financeira117/0/0/0, BUILD SUCCESS06:46:08:52 financeiro,12 contingência,43 fechamento e10 jornada HTTP própria. Entrada temporal conserva instante físico; replay devolve confirmação original; ajuste a zero comprova fim físico no cálculo. Naquela bateria parcial, validade e concorrência de encerramento/compromisso ainda estavam em teste; passaram posteriormente no fecho367 abaixo. Tentativas iniciais preservadas. Consultas FE08/FE12 e jornada BE01 formalizadas antes do Java no31; nenhum aceite BE14 antecipado. Detalhes e outputs no32; revisão final somente após freeze e build completos.



- [x] Expor filtros, histórico, indicadores essenciais e valor da mercadoria proporcional, incluindo exclusão de avaria somente do indicador aplicável.

- [x] Implementar contagem simples, eventual carga inicial e ajustes aprovados, sem editar a nota para esconder diferenças ou desconsiderar reservas.

- [x] Permitir lançamento/conciliação rastreável da planilha de contingência, com identificador único e prevenção de repetição; não pressupor importação automática irrestrita.



**Conclusão:** consultas reconciliam saldos com movimentos; contagem e contingência mantêm origem, reserva e responsável. **Base:** RN20/RN28/RN29, Q18/Q19, AC13 a AC15, V21, V37, V41 e V44.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. P2 residual resolvido: o alvo do DTO executado define pedido/cliente/armazém/linha; referências redundantes incompatíveis são recusadas antes dos efeitos. Duas ordens concorrentes, replay/perfis/rollback testados. Carga PENDENTE/PREPARADA/REGULARIZADA conserva ausências e entrada única; contagem não corta pedido/reserva nem recompõe baixas retiradas; contingência preserva identidade global/instante real/dependências e GET atual. **Ligação FE:** FE08/FE12; nenhum frontend. **Externos:** Caio/cliente, operação/Mickael/TI e Lucas/DBA.



### BE15 Validação técnica e preparação do piloto



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Status:** D29 em andamento: cobertura normativa/cenários/propostas e lacunas técnicas no DEV por nova autorização. D28 encerrada com limites preservados. Homologação operacional/comercial/piloto permanece externa. **Dependências:** preflight novo DEV/WMSDEV, oráculos e revisão; dispositivos/fiscal externo/recuperação em destino isolado fora deste escopo. [Escopo D29](orchestracao/.runtime/d29-autorizacao-e-distribuicao.md).



**Preparação D19 observada:** Prumo preparou [configuração externa, diagnóstico e planos de recuperação/ensaio](docs/33-preparacao-tecnica-local-backend.md) em arquivos. O diagnóstico final real conferiu194/194 condições estáticas, incluindo as56 originais; fecho22/22,307 hashes e240 links conferidos por Farol. Vigia favorável às06:09, sem achado material no novo pacote; preparo local em arquivos aceito. Não consulta ambiente, rede ou SQL nem executa JVM/build. Testes integrados finais de367 cenários, artefato e revisão local concluídos no32; ensaios externos continuam pendentes.



- [x] Conferir regras, permissões, concorrência, repetições, falhas fiscais e cálculo nos testes locais adequados.

- [x] Validar conexão administrativa, migrations e catálogo no SQL Server existente (D24).

- [x] Executar operações HTTP integradas e concorrência determinística no SQL Server DEV/WMSDEV (D26: build412/IT7/API159/160, fotografias236/0+17/0+76/0; HTTP/JVM e SQL posterior separados).

- [x] Fechar as lacunas técnicas selecionadas da D26 por D27: ajuste E052, financeiro não zero e witness SQL nos casos do resultado42; demais combinações RN/AC e homologação operacional permanecem delimitadas.

- [x] D28: regressão das29 correções inventariadas com evidências atuais, matriz e revisão favorável no escopo comprovado; auxiliares offline e impedimento witnessSQL300 separados, sem homologação total.

- [ ] D29: matriz integral documental e novos casos DEV, exemplos AC04–08, concorrência real repetida/observabilidade permitida, correções red/green e encaminhamentos externos precisos.

- [x] Preparar execução, configuração, diagnóstico, cópias de segurança e recuperação em arquivos.

- [ ] Ensaiar backup/restauração, recuperação e desempenho com Lucas/DBA, medindo critérios reais.

- [x] Registrar artefato construído, verificações, limites e plano de recuperação para a liberação conjunta; publicação é um registro separado.



**Conclusão:** há evidência da versão candidata e do que foi verificado; nenhuma afirmação de operação real decorre apenas de build ou testes locais. **Base:** PR01 a PR11, AC14 a AC16, V13 a V23 e V42/V44.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Candidato local, procedimentos e recuperação separados da homologação/piloto/publicação. Donos e lacunas externos no33/34; não inventados RPO/RTO, volumetria, credenciais, preço, parâmetro fiscal ou data de piloto. Frontend e validação conjunta futura permanecem fora desta entrega.



**Fecho local D20:** runner manual/guardas/procedimentos/ensaio optativo preparados, backend396/0/0/0 em19 suítes e arquivo conferido; testes locais após organização80/21/13/336, cmd.exe0/0/2/2,722 hashes. SQL Server, TLS/identidades, privilégios efetivos completos (VIG07), schema/locks e recuperação continuam externos, sem aceite presumido. [Resultado36](docs/36-database-local-engenharia-e-validacao.md). Frontend/homologação conjunta fora do bloco; trabalho local encerrado.



**Fecho local D21 de BE15:**41/41 fixtures do fluxo,672/672 checks de pacote,25/25 Maven e cmd.exe offline0/0/2/2;675 hashes database e84 backend conferidos. Quedas parciais/reexecução e versão futura verificadas por mocks, não SQL Server. Perfil manual administrativo permite DEV/PROD sem novos grants, runtime continua restrito. Revisão Vigia favorável; [37](docs/37-bootstrap-e-upgrade-manual-database.md) e recibo WMS. Não repetir396/build por launcher; nenhum SQL real pelo agente.



**Fecho local D22 de BE15:** 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão independente favorável. Ausência, SID/owner/ACL, erro DPAPI/tipo/login e descarte de cópias verificados com fixtures isoladas; console Explorer/FOR-F/filho sem recursão por metadados simulados. Sem duplo clique visual ou SQL real. Backend/POM sem alterações, nenhuma suíte integral/Maven repetida. Provisionamento real e ensaio SQL/TLS permanecem do operador, conforme [38](docs/38-launcher-automatico-e-credencial-protegida.md).



**Evidência atual D24 de BE15:** conexão e BAT real DEV/PROD concluídos; 18 testes D22 e nove testes D24 aprovados. [Resultado39](docs/39-conexao-compartilhada-e-sql-real.md). Homologação operacional, permissões da aplicação e recuperação continuam pendentes.



**Evidência D25 de BE15:** build atual 408/0/0/0 e JAR 6/6 HTTP reais; SqlClient/JDBC administrativo DEV com TLS validado. Vigia favorável; sa nunca API/IT. Operações/concorrência SQL e sete ITs não executados por falta de identidade própria; backup/restauração e homologação permanecem externos. Nenhuma fixture SQL criada. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



### BE16 Padrões e verificações de engenharia

**BE16-JAVA01 — avisos Java informados em 08/10/2026:** correção e validação Java concluídas; atualização do mapa em andamento. Chamada de `NfeXmlService.hash` pela classe, conversões JSON/Map tipadas em Contingencia/Expedicao/Auditoria e remoção dos dois membros sem uso em D29FinanceiroRetomadaTest. Dependência: BE16; contratos BE→FE preservados. [Resultado e fontes](orchestracao/.runtime/avisos-java-20261008/resultado.json), [diff deste recorte](orchestracao/.runtime/avisos-java-20261008/alteracoes.diff) e fontes anteriores em `orchestracao/.runtime/avisos-java-20261008/antes/`. Formatação aprovada; 259 fontes main e 66 fontes test compiladas; dez testes pertinentes em cinco classes, zero falhas/erros/ignorados, quatro contextos H2 em memória comprovados pela guarda local. `javac -Xlint:unchecked,static -Werror` nos cinco arquivos: antes exit1/16 avisos, depois exit0/zero avisos. Compilação geral conserva um aviso de depreciação preexistente em D30BearerCookieTest, fora do pedido. Build próprio `backend/target-java-avisos-20261008`, sem SQL Server, JAR ou regressão integral. Manutenção posterior à FINAL14, com provas históricas preservadas; não reabre a auditoria D30. Próximo passo deste recorte: concluir e registrar `graphify update .`.



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Evidência atual D25:** JDK 21.0.12.1/Maven 3.9.16, formatação e clean verify atual com 408/0/0/0 em 20 suítes; HTTP/arquitetura/serviços/regressão executados. Correções focais de TLS cliente, critério/perfil do IT e sessão local, sem alteração POM/MVC/models/migrations. JAR final conferido e encerrado após 6/6 checks; revisão Vigia favorável. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



**Status:** Concluído. **Dependências:** BE02; recorte cadastral de BE03/BE04/BE05 disponível.



- [x] Padronizar configuração Spring em `.properties`, preservando ambientes e proteções.

- [x] Automatizar conferência de ferramentas, formatação e separação das camadas MVC no build.

- [x] Validar contratos também na entrada dos serviços e testar chamadas fora do HTTP.

- [x] Registrar convenções, comandos e evidências da validação local.



**Conclusão:** padrões definidos e verificáveis no backend atual, com build/testes aprovados. Não substitui BE15 nem homologação dos módulos futuros. **Base:** pedido do responsável de usar properties e boas práticas de engenharia. **Ligação FE:** mantém os contratos FE02/FE03/FE04; sem tela nova.



**Evidência D13 em 05/10/2026:** [padrões](docs/16-padroes-de-engenharia-backend.md), Enforcer/Spotless, testes ArchUnit, validação dos serviços e perfis properties. `clean verify` aprovado com 52 testes (12 adicionados), conforme [registro](docs/15-validacao-cadastros-backend.md). **Próximo passo:** aplicar esses padrões às próximas entregas do resumo; não há pendência neste recorte. BE15/validações reais permanecem com escopo próprio.



**Manutenção em 06/10/2026:** `.gitignore` ajustado a pedido do responsável para builds alternativos, binários, temporários e evidências brutas/cópias locais. Fontes, migrations oficiais, referências, relatórios de entrega, manifestos SHA-256 e scripts atuais permanecem versionáveis. Conferência por inventário Git e `git check-ignore`; arquivos preservados no disco, sem alteração do índice ou commit. Mudança restrita ao versionamento, sem novo build/teste de aplicação e sem alterar o aceite D19 ou os contratos frontend.



**Complemento do `.gitignore` em 06/10/2026:** a pedido do responsável, `.agents/`, toda a pasta `orchestracao/`, `.ai-memory.toml` e `.graphifyignore` passam a ser apoio local fora do Git. `.codex/`, `.maestri/`, `.graphify/`, `graphify-out/` e `.tools/` já estavam excluídas. Foram retirados **28 arquivos adicionais** da lista de candidatos ao versionamento; **369 arquivos de fontes, migrations, documentação e infraestrutura** foram conferidos como preservados nas regras. `AGENTS.md`, `STATES.md` e a configuração compartilhada `.vscode/settings.json` continuam versionáveis. Arquivos das ferramentas permanecem no disco e operacionais; nenhum arquivo apagado, índice modificado ou commit realizado. Conferência com `git check-ignore`, sem build de aplicação. Escopo BE16, sem mudança de contratos frontend ou do próximo passo vigente D20.



**Manutenção Java em 06/10/2026:** avisos de APIs obsoletas e membros/variáveis sem uso corrigidos, com atualização automática Java/Maven configurada no VS Code. Restrições SQL, preparação dos cenários e contratos frontend preservados. `spotless:apply clean verify` aprovado com **367/0/0/0 em 17 suítes**; compilação adicional Eclipse JDT sem avisos/erros e Graphify atualizado. [Evidência da manutenção](backend/evidencias/manutencao-java-2026-10-06.md), com artefato separado em `backend/target-avisos/`; o aceite/JAR D19 anterior permanece histórico e preservado. **Complemento do editor:** servidor Java do workspace WMS reiniciado e reimportação/atualização Maven confirmadas nos logs do VS Code; controle visual indisponível. Aviso novo de `SqlServerUpdateMappingTest` corrigido com a API atual do Hibernate: teste direcionado **1/0/0/0**, compilação Eclipse JDT sem avisos/erros e Graphify atualizado; detalhes na mesma evidência. Alterações concorrentes de D20 preservadas. **Próximo passo:** segue a etapa vigente do resumo, sem nova entrega frontend nem alteração das pendências de homologação.



## Trilha frontend



As entregas desta trilha ficam em `frontend`. Telas de gestão e coletor usam React/TypeScript. Usar linguagem simples, feedback de confirmação e estados claros de erro; ações críticas dependem da resposta do backend.



### FE01 Jornadas, telas e contratos



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** DOC01 a DOC04; alinhamento com BE01.



- [x] Desenhar jornadas por perfil: cadastro, entrada, conferência, etiqueta, endereço, consulta, reserva, separação, retirada e fechamento.

- [x] Definir campos, ações, bloqueios, mensagens e navegação; destacar quais tarefas usam coletor.

- [x] Alinhar contratos e exemplos fictícios com BE01, incluindo erro, vazio, carregamento, conflito e indisponibilidade.



**Conclusão:** cada ação de tela tem responsável, condição e resultado de negócio; a jornada não inventa liberações ou cálculos locais. **Base:** RN30, AC01 a AC16 e FE/BE conforme as entregas seguintes.



**Evidência D31 atual FE01:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE02 Base React e componentes comuns



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE01 e contrato inicial de BE02.



- [x] Definir ferramentas e organização interna, criar base React/TypeScript e procedimento de execução local.

- [x] Preparar navegação, formulários, tabelas, diálogos, mensagens e comunicação centralizada com a API.

- [x] Implementar tratamento comum de carregamento, erros, conflito e repetição de envio; preparar verificação de tipagem e construção.



**Conclusão:** estrutura é utilizável em telas administrativas e coletor, com confirmação explícita e sem dependência de dados produtivos. **Base:** D02, RN30, PR02 e T02.



**Evidência D31 atual FE02:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE03 Acesso e navegação por perfil

**Atual D32 / AUD-LOGIN-CAD01:** login e usuários próprios implementados; mecanismo nativo, contratos e ativação DEV04 descritos no [documento 43](docs/43-login-e-administracao-de-usuarios.md). Auditoria BE04-AUD01/FE03-AUD01 concluída com sete achados abertos e propostas no [recibo](orchestracao/.runtime/login-cadastros-auditoria-resultado.md); correções aguardam Lucas. Primeiro login real/negócio autenticado não presumido por esta auditoria.

#### Histórico do item antes de D32 — preservado




**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE02, BE04.



- [ ] Implementar entrada/saída de sessão e apresentação das ações autorizadas para Gestor, Supervisor e Operação.

- [x] Exibir contexto de cliente/armazém e tratar sessão expirada, acesso recusado e troca de contexto sem reutilizar dados indevidos.

- [ ] Disponibilizar gestão de usuários e atribuições conforme o contrato aprovado em BE04.



**Conclusão:** interface e API concordam quanto ao acesso; ocultar botão não é a única proteção. **Base:** Q20, AC11 e V16.



**Evidência D31 atual FE03:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Sessão fictícia exercitada; provider real e usuários sem rota pendentes.

### FE04 Cadastros e importação de endereços



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, BE05.



- [x] Criar telas de cliente, armazém, posição, produto, embalagem/DUN e informações fiscais necessárias ao fluxo.

- [x] Permitir conferência da importação de endereços, com erros identificáveis e resultado sem duplicidade.

- [x] Apresentar tabelas/serviços, vigências e parâmetros; mostrar impedimentos de inativação/encerramento e histórico pertinente.



**Conclusão:** usuário distingue campos obrigatórios, erros e efeito das alterações; valores reais não são preenchidos com exemplos de outro cliente. **Base:** RN04 a RN09 e AC05/AC06/AC10/AC12.



**Evidência D31 atual FE04:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE05 Entrada, conferência e quarentena



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE04, BE06.



- [x] Criar lista/detalhe e pedido manual/XML, com notas, itens e chegadas em partes.

- [x] Registrar contagem, falta, sobra, avaria e tratativa, mostrando diferença entre previsto e recebido.

- [x] Exibir bloqueio da carga e permitir liberação somente ao perfil autorizado, com confirmação do servidor.



**Conclusão:** operador entende o que está em conferência, bloqueado ou liberado; importar XML não aparece como recebimento físico concluído. **Base:** AC03, V01 e V26 a V29.



**Histórico D15:** rotas, DTOs, permissões, estados e regras de repetição no [documento 18](docs/18-recebimento-e-conferencia.md). Naquele momento, FE05 estava planejada, sem aplicação/tela ou teste de integração frontend.

**Atualização FE05-PED01:** a implementação local FE05 foi entregue em D31 e reorganizada em FE05-PED01, com [Pedidos de entrada e seções do pedido](docs/design-system/etapa-07.md). Validação real de ambiente permanece separada.



**Evidência D31 atual FE05:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE06 Unitização, etiquetas e impressão



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE05, BE07.



- [x] Distribuir quantidades em unidades, mostrar identidade/origem e apresentar divergências da soma.

- [ ] Exibir e imprimir etiqueta no formato definido, com SKU, ID, data, nota, quantidade e código legível, sem endereço fixo.

- [x] Reimprimir sem criar unidade e substituir etiqueta de quantidade desatualizada após retirada parcial.



**Conclusão:** etiqueta e registro correspondem; impressão/leitura são verificadas nos equipamentos disponibilizados por Mickael antes do piloto. **Base:** RN10/RN11/RN16, AC10, V02 a V04, V09 e V43.



**Backend disponível após D16:** contratos de unitização, divisão/reagrupamento, progresso, leitura por UUID e dados atuais da etiqueta no [documento 20](docs/20-unidades-logisticas-e-etiquetas.md). FE06 permanece planejada: não há tela, layout de impressão, PDF/ZPL ou envio à impressora. Reimpressão consulta a identidade existente; frontend deve substituir etiqueta cujo conteúdo mudou.



**Evidência D31 atual FE06:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Prévia/QR e dados atuais; impressão/layout físico pendentes.

### FE07 Coletor para endereçamento e movimentação



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, FE06, BE08.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D17:** capacidade, conjuntos, leitura/posição, bloqueio/liberação e repetição em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md). Sem tela implementada ou leitura no coletor real.



- [x] Criar sequência simples de leitura da unidade e posição, com foco adequado e ações acessíveis.

- [x] Mostrar armazém, destino, limites, posições necessárias e recusa de leitura/capacidade incompatível.

- [x] Aguardar confirmação do servidor antes de exibir sucesso; tratar leitura repetida, perda de resposta e tentativa concorrente.



**Conclusão:** operador identifica o movimento confirmado e consegue corrigir a leitura sem duplicar operação; verificar no coletor real. **Base:** RN18/RN30, AC05, V08, V14/V15, V23 e V30.



**Evidência D31 atual FE07:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE08 Estoque, filtros e rastreabilidade



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, BE08 e contratos de consulta de BE14.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D17/D18:** lista, saldo por produto e histórico em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md); reservado e avaria posterior complementados no [documento 24](docs/24-pedido-saida-fifo-e-reserva.md). Indicadores financeiros seguem suas etapas; nenhuma tela entregue.



- [x] Exibir saldo físico, disponível, reservado, quarentena e avaria, com filtros por cliente, SKU, unidade, armazém, endereço e situação.

- [x] Mostrar origem, localização, movimentos e datas FIFO/chegada/armazenagem sem confundi-las.

- [x] Apresentar indicadores essenciais e aviso de validade quando aplicável, sem tratar erro de consulta como estoque vazio.



**Conclusão:** quantidades e condições reconciliam com o backend e respeitam o alcance do usuário. **Base:** RN20/RN28, Q01/Q18/Q21, V07, V17, V37 e V38.



**Evidência D31 atual FE08:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE09 Saída, FIFO, reserva e separação



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE06, FE08, BE09 e etapa de separação de BE10.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D18:** criação, consulta, sugestão FIFO, justificativa/autorização, reserva, reversão/cancelamento e revalidação em [documento24](docs/24-pedido-saida-fifo-e-reserva.md). **Complemento D19:** leitura/separação BE10 no [documento27](docs/27-separacao-retirada-retornos-e-avaria.md), aceito localmente com testes/build e revisão após correção temporal no [28](docs/28-validacao-separacao-retirada-retornos.md). Frontend não iniciado.



- [x] Criar pedido, mostrar sugestão FIFO e confirmar reserva; permitir justificativa/autorização de exceção por perfil.

- [x] Exibir reserva sem vencimento, pendência fiscal, indisponibilidade concorrente e avaria posterior.

- [x] Conferir leitura para separação, incluindo parcial de pallet com pedido integral e etiqueta correta do remanescente.



**Conclusão:** a interface não promete saldo antes da confirmação nem conclui parte do pedido; erros de concorrência são compreensíveis. **Base:** AC10/AC11, V06/V07, V13/V17, V24 e V31/V32.



**Evidência D31 atual FE09:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE10 Fiscal, retirada, cancelamento e devolução



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE09, BE10, BE11.



- [x] Vincular/exibir documentos, sua cobertura do pedido e pendências do NOTAZZ no procedimento inicial.

- [x] Separar visualmente nota emitida de retirada confirmada e limitar a conclusão a Supervisor/Gestor.

- [x] Guiar cancelamento, retorno à posição, devolução por nova entrada e avaria, mostrando consequências e serviços preservados.



**Conclusão:** operador sabe o que falta para retirar e o que um cancelamento desfaz; documento simbólico não é mostrado como expedição física. **Base:** AC01/AC02/AC08, V10/V11, V33/V34 e V39.



**Evidência D31 atual FE10:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE11 Serviços, armazenagem e fechamento



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE04, FE10, BE12, BE13.



- [x] Registrar serviços, quantidades e origem; disponibilizar liberação financeira ao gestor sem duplicar lançamento.

- [x] Exibir memória de diárias, posições equivalentes, vigência, mínimo, avaria e ajustes, distinguindo ocupação física de cobrança.

- [x] Conferir/aprovar o ciclo inteiro, reabrir quando permitido e apresentar demonstrativo para ESL e referência da NFS-e.



**Conclusão:** cada valor é explicável e a interface exibe cálculo do servidor; exemplo documental não aparece como contrato real confirmado. **Base:** AC04 a AC09, V12, V19/V20, V35/V36, V40 e V45 a V48.



**Evidência D31 atual FE11:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE12 Contagem, contingência e relatórios



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE08, FE11, BE14.



- [x] Apoiar contagem simples e eventual carga inicial, com diferença, justificativa e aprovação do ajuste.

- [x] Apoiar lançamento/conferência dos registros de contingência, identificando conciliados e conflitos sem criar sincronização automática sem rede.

- [x] Disponibilizar consultas e relatórios essenciais de estoque, ocupação, movimentos, valor, pedidos e cobrança com o mesmo alcance de acesso.



**Conclusão:** registros podem ser conferidos pelo supervisor/gestor; diferença ou falha não desaparece da tela nem altera saldo sem confirmação. **Base:** AC13 a AC15, V21, V37 e V44.



**Evidência D31 atual FE12:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE13 Validação das jornadas e preparação do piloto



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE02 a FE12; BE15 para fechamento conjunto.



- [x] Verificar tipagem, comportamento, contratos e construção; validar teclado, legibilidade, tamanhos de tela e feedback de erro/sucesso.

- [ ] Executar percurso completo com API, impressora/coletor e dados de validação, registrando falhas de conexão e repetição.

- [x] Preparar artefato frontend, instruções para a operação e evidências da versão candidata, distinguindo simulação de teste real.



**Conclusão:** jornadas funcionam com o backend e nos dispositivos do piloto; versão construída e versão publicada possuem registros separados. **Base:** RN30, AC14 a AC16 e V01 a V48 conforme a jornada.



**Evidência D31 atual FE13:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Validação local/artefato/guia; integração/piloto pendentes.

## Entregas conjuntas e ordem de avanço



Esses marcos agrupam as duas trilhas, sem duplicar o controle de cada etapa. Todos estão planejados. Não é necessário concluir todo o backend antes de começar o frontend.



| Marco | Backend | Frontend | Resultado para a operação |

| --- | --- | --- | --- |

| M01 Base e cadastros | BE01 a BE05 | FE01 a FE04 | Acessar com perfil correto e cadastrar o contexto da operação |

| M02 Receber e localizar | BE06 a BE08 | FE05 a FE07 | Receber, conferir, identificar e endereçar uma unidade |

| M03 Consultar e expedir | BE09 a BE11; consultas iniciais BE14 | FE08 a FE10 | Consultar saldo, reservar, separar, documentar, retirar e tratar retorno |

| M04 Cobrar e conferir | BE12 a BE14 | FE11 e FE12 | Explicar serviços, armazenagem, ajustes, contagens e fechamento |

| M05 Validar o piloto | BE15 | FE13 | Conferir percurso com Caio, equipamentos, fiscal e recuperação |



BE14 pode disponibilizar consultas de estoque para FE08 depois de BE08, sem aguardar sua parte financeira. Essa entrega parcial deve ser registrada sem marcar BE14 inteiro como concluído. FE01 pode avançar junto com BE01; ambos fecham os contratos antes de concluir telas integradas.



## Dados e verificações de implantação



São dependências das etapas afetadas, não um bloqueio geral do projeto nem outra rodada das 110 perguntas.



| Item | Responsável de referência | Etapa afetada | Situação atual |

| --- | --- | --- | --- |

| Banco/esquema WMS, acesso e ambientes SQL Server | Lucas/TI | BE03, BE15 | D24 schemas; D26 identidade DEV/atestação/API reais. Ajuste demanda V10 DEV em escopo separado; recuperação pendente |

| Forma de autenticação, hospedagem e operação | Equipe técnica com Lucas | BE02, BE04, BE15, FE03/FE13 | A definir; não copiar configuração de outro projeto |

| Cliente, limites, embalagens, endereços e eventual estoque inicial | Caio/operação | BE05/BE06/BE14, FE04/FE12 | Preparar dados; razão social resolve grafia Brasel/Bracel |

| Preços, unidades de cobrança, mínimo, corte e eventual GRIS | Gestor/comercial | BE12/BE13, FE11 | Parâmetros reais a cadastrar; tabela Tigre é referência |

| Exemplos fiscais e procedimento NOTAZZ/ESL | Natalina/Controladoria | BE10/BE13, FE10/FE11 | Responsável identificado; fluxo real a validar |

| Coletor, Tanca, etiqueta e cobertura | Mickael e TI | FE06/FE07/FE13 | Tipos informados; modelos e teste real pendentes |

| Volumes e simultaneidade real | Caio/operação e equipe técnica | BE15/FE13 | Dez usuários/quatro simultâneos estimados; movimentações desconhecidas |

| Backups, restauração e reconciliação | Lucas/TI e Caio | BE15/FE12/FE13 | Até 36h de parada informadas; capacidade real a comprovar |

| Conferência de baixa e cálculo propostos | Caio e gestor | BE10/BE12/BE13, FE10/FE11 | Usar exemplos de AC01/AC04/AC05/AC08 antes do uso real |



Nenhum desses itens está registrado como teste concluído. Dados fictícios podem apoiar o desenvolvimento; não substituem configuração e validação do piloto.



## Fora da primeira entrega



- **Adiado por resposta:** transferência entre armazéns, conforme Q11.

- **Evolução proposta:** integração automática NOTAZZ/ESL, mapa visual elaborado e inventário completo. O piloto prevê procedimento fiscal intermediário, consultas de ocupação e contagem simples.

- Aplicativo que registra e sincroniza movimentos sem conexão não está definido; a contingência recebida é por planilha com conciliação.

- Expansão Castro/Tigre depende da validação inicial. A arquitetura comporta outros clientes/armazéns sem obrigar ativação simultânea.



## Registro das próximas atualizações



Ao iniciar ou concluir uma etapa, atualizar o cabeçalho dela e registrar: data; IDs BE/FE; escopo entregue; arquivos/artefatos; validação executada e resultado; limitação ou impedimento concreto; próximo passo. Registrar versão em execução somente quando observada. Referenciar evidências existentes por links, sem inventar comandos, relatórios ou arquivos futuros.



Manter próximos passos no resumo e status nas próprias etapas; conservar no histórico apenas o que explica o avanço. Decisões de negócio continuam no documento 06, sem redefinir fórmulas neste arquivo.



## Histórico de evolução



| Data | Registro | Evidência e limite |

| --- | --- | --- |

| 03/10/2026 | Estrutura local e documentação inicial preparadas | Pastas e documentos do projeto; sem implementação |

| 05/10/2026 | Questionário recebido e regras consolidadas pelo raciocínio | Documentos 10 e 11; 110 subitens respondidos, com interpretações/propostas identificadas |

| 05/10/2026 | Orientações e trilha organizadas a pedido do responsável | AGENTS.md e states.md; 15 etapas backend e 13 frontend, todas planejadas; referências e dependências verificadas |

| 05/10/2026 | Pedido D11 inicia o backend; escolhida e concluída BE02, com recorte técnico de BE01 | Spring MVC por camadas, Maven Wrapper, status/erros/identificação, bloqueio de acesso e perfis; 12 testes e execução do JAR local aprovados. Documentos 12/13. Sem SQL Server, frontend ou publicação |

| 05/10/2026 | D12 amplia o backend para cinco cadastros, persistência, acesso e auditoria | BE01/BE03/BE04/BE05 em andamento; V1 preparada; JWT/escopos, versões, encerramento e transações testados. Build final `verify`: 40 testes aprovados; documentos 14/15. Sem SQL Server real, provedor real, frontend ou publicação |

| 05/10/2026 | D13 e BE16 padronizam properties e engenharia | Configuração convertida, validação nos serviços, formatação/imports explícitos, ferramentas e arquitetura conferidas no build. `clean verify`: 52 testes aprovados; padrões no documento 16. IDs existentes preservados; nenhum ambiente real alterado |

| 05/10/2026 | D15 entrega o macrobloco de recebimento | Pedido/notas manual/XML, chegadas, quarentena, estorno e efetivação única; seis modelos e V2 preparada. `clean verify`: 86 testes aprovados, sendo 34 novos; documentos 18/19. BE06 em validação externa; próxima frente BE07. Sem SQL Server, emissão fiscal ou publicação |

| 05/10/2026 | D16 entrega unidades logísticas e dados de etiquetas | Unitização por entrada, UUID, origem, divisão/reagrupamento, repetição e auditoria; V3 preparada. `clean verify`: 122 testes aprovados, sendo 36 novos; documentos 20/21. BE07 em validação externa; próxima frente BE08. Sem SQL Server, frontend, impressão real ou publicação |

| 05/10/2026 | D17 entrega endereçamento e estoque | Capacidade, conjuntos, ocupação/movimentos atômicos, bloqueio/liberação e saldo/histórico; V4 preparada. `clean verify`: 153 testes aprovados, sendo 31 novos; documentos 22/23. BE08 em validação externa e BE14 parcial; próximo código BE09. Sem SQL Server, frontend, cobrança ou publicação |

| 05/10/2026 | D18 entrega pedido de saída, FIFO e reserva pela equipe WMS | Cedro backend/contrato, Prumo V5/procedimento, Vigia revisão favorável em leitura e Farol registros/mapa. `spotless:apply` e `clean verify` aprovados: 197 testes, 44 novos; documentos 24/25. BE09 em validação externa e BE01/BE14 parciais; próximo código proposto BE01/BE10 após nova demanda. Git zero commits, conteúdo preexistente preservado. Sem SQL Server real, frontend, fiscal/cobrança, publicação, rotinas, Hermes/ETL |

| 05/10/2026 | D19 autoriza execução contínua de todo o backend restante | Ordem, arquivos e aceite local no documento 26. Iniciadas BE10/BE11, sem antecipar testes; base D18/197 preservada. Autorização abrange os próximos blocos locais, não homologação ou ações externas proibidas |

| 06/10/2026 | D19 aceita localmente BE10/BE11 e segue BE05/BE12 | Após correções de Vigia e P2 temporal BE07, spotless e clean verify reais: 240/0/0/0; contrato27/evidência28, JAR e hashes conferidos. Vigia favorável; V6 compatível em arquivos, sem SQL real. Base197 e V1–V5 preservadas. BE10/BE11 em validação externa; schema BE05/BE12 e V7 seguem autonomamente, sem nova autorização |

| 06/10/2026 | D19 aceita recorte BE05/BE12 e libera BE13 | Após P2 financeiro BE08, spotless/clean verify292/0/0/0,14 XMLs/JAR/222 hashes conferidos. Vigia favorável02:33; V7/JPA518/518 e67/67 suplementares compatíveis, arquivos somente. Contratos29/31, evidências30/32. Cedro refina31 antes do Java/V8 e segue BE13/BE14; inativação BE05 após esses fatos. Não conclui SQL Server/comercial/fiscal/equipamentos externos |



Padrões de organização consultados: `satelite-tms-api`, `dashboards-etl` e `avaliacao-desempenho-competencias`, especialmente separação entre orientação e estado, IDs de tarefas, evidência e próximo passo. Regras específicas de bancos, portas, bibliotecas e operação desses projetos não foram incorporadas ao WMS.



<!-- mcp-setup-evidence:start -->

## Integração de ferramentas MCP — 05/10/2026



- ai-memory acessível pelo MCP no escopo exclusivo workspace=rodogarcia, project=wms-rodogarcia; declaração em .ai-memory.toml. Históricos existentes preservados e espaços novos registrados vazios pelo mecanismo nativo, sem sessões ou páginas artificiais.

- Graphify consultável pelo MCP com project_path deste repositório: 1629 nós e 5861 relações na conferência. JSON, relatório e HTML presentes; verificação do grafo exportado encontrou zero relações com endpoint ausente. Consultas reais de saúde e conteúdo executadas pelos dois MCPs.

- Instruções oficiais e sete skills locais preparados, com escopo explícito em AGENTS.md; hooks Git de commit e troca de branch instalados. A captura de sessões usa os hooks globais já instalados no Codex.

- Preparador comum e guia em ../.runtime/mcp-tools/README.md; integração automática para novos repositórios Git diretamente em projetos, com processo próprio no login do usuário. Recibos e logs ficam na pasta da ferramenta. Não modifica runtime das aplicações, banco ou equipe Maestri.

- Limites: Conteúdo de 42 documentos/PDFs incluído; imagens incorporadas do Word não reavaliadas. Diagnóstico bruto: 778 referências sem nó definido e 285 relações paralelas agrupadas; grafo exportado com zero endpoints ausentes. Auditoria original preservada em graphify-out/extraction-audit.json e integrity-audit.json. Tokens dos agentes não medidos.



Este registro trata somente das ferramentas dos agentes. Critérios e pendências de implementação deste projeto permanecem nas seções próprias acima.

<!-- mcp-setup-evidence:end -->





**Fecho D19 em06/10/2026:** escopo local BE01–BE16 concluído após P2-locks, 367/0/0/0, revisão Vigia favorável e V9/JPA conferidos. Graphify atualizado por Farol, registros centrais/índice/modelo/matriz sincronizados e alterações preexistentes preservadas. Nenhuma ação externa proibida realizada; próxima etapa é definição/autorização dos ensaios externos pelo33/34, sem backend independente faltante identificado na auditoria/revisão.





**Preparo técnico final aceito após P2 de identidade:** Vigia favorável às08:34, histórico360 e atual367 separados; fixtures18/18 com12 recusas, diagnóstico306/306, fecho77/77 e488 hashes conferidos. Backend289 e V1–V9 intactos. Evidência32 e preparo33; sem execução SQL/JPA/JVM ou novo build por essa correção documental.

### FE13-MANUAL01 — continuação: consulta de unidades por pedido
- ENT-000011: a operação Consultar lista, em Outras operações da distribuição, retornou A=5 e B=10 ativas e C=0 inativa; correlação c252cad8-395e-4741-b701-89b099fd808c. Detalhe A pela etapa Corrigir organização física confirmou pedido, UN, DUN=10, quantidade física5, origem5 e indisponibilidade em triagem. O filtro anterior ARM02 não devolvia A corretamente; ajustado para TRI, sem atribuir ausência ao sistema. Evidências: unidades-ent11-lista.txt e unidade-a-detalhe-por-pedido.txt na pasta QA já referida.


### FE13-MANUAL01 — ciclo financeiro encerrado em contexto fictício independente
- Criado CLI-000009, QA MANUAL 20261015 - Financeiro fictício, documento sintético QAFIN20261015 (1bc58106-02f7-47b4-8408-16d526be03b2), mantendo armazém QA ARM-000006. Contexto sem estoque/fatos; não altera o ciclo15→16 do CLI8.
- Contrato fictício desde14/10/2026, America/Sao_Paulo/BRL, DIAS_CORRIDOS1, mínimo e GRIS explicitamente NAO_APLICAVEL (108503b4-c9e1-413c-ba0b-ab79513257a3). TAB-000005 específica desde14/10, tarifa fictícia armazenagem1.25 usando serviço já existente DEV fictício - ARMAZENAGEM (8d1c63a3-1b9b-4532-8d71-98451043516a); vínculo desde14/10 (269cb2ed-cfed-493e-9329-cd091891004f), lista independente confirmou vínculo.
- Cálculo14→15 COMPLETO, sem pendências, pico/estoque0, total0.00 (43a77ea3-3943-45b1-ac63-52f0d4cabf1d). Zero decorre da ausência real de movimento neste contexto e da configuração explícita, não de preenchimento automático de insumo ausente.
- Preparação d7b1d20a-8b37-4a1e-90a2-196aa1f674de: versão1, ZERO0.00, hash ef25e7c062d3356d7b1850fa9e1c990f4bad44bcd1c18132157d40d1baa35752. Aprovação67242fec-0772-4ce6-b733-f0510d232d82: APROVADO/APROVADA, NAO_ENVIADO, mesmo hash. Evidências cliente-financeiro-criado.txt, contrato-financeiro-qa.txt, tabela-financeiro-qa.txt, vinculo-financeiro-qa.txt, vinculos-financeiro-lista.txt, calculo-financeiro-zero.txt, fechamento-zero-preparado.txt e fechamento-zero-aprovado.txt; captura também nos outputs do chat. Nenhuma cobrança, emissão ou envio externo realizado.

### FE13-MANUAL01 — entrega local, declaração e finalização do saldo ZERO
- CLI9/ARM6 ciclo14→15 V1: entrega fictícia local referência QA-LOCAL-FICTICIO-SEM-ENVIO-20261015-V1 às23:27 (983044a3-06d5-4881-8590-caca276435f3). Estado DESCONHECIDO; repetir exatamente01ee21e5-c4a7-4363-b68d-3dfaaa06945a manteve uma entrega/sequência1, sem duplicação. Nenhum arquivo/mensagem transmitido a terceiros.
- Tentativa inicial às23:26 recusada por ser anterior ao instante completo da aprovação; data corrigida para23:27 e aceita. Edição incorreta do controle nativo e preenchimento futuro23:31 antes de chegar23:31 foram erros da execução, não falhas do WMS. Correção por controle de data e verificação visual antes de confirmar.
- Consulta tratativas67f01c53-b97c-400b-84fc-5359d9cff005 sem registros, coerente com ainda não haver conflito/tratativa.
- Declaração de não emissão fictícia, fonte/responsável explicitamente QA, às23:30:1e4342f5-ebee-4d29-970b-41d1caf23449 → NAO_EMITIDO_CONFIRMADO.
- Resolução financeira fictícia de saldo zero, referência QA-RESOLUCAO-ZERO-FICTICIA-20261015-V1, às23:31:3b4d0ed0-f83c-4c81-a412-e84ef5af798f → FINALIZADO_SEM_EMISSAO/FINALIZADA_SEM_EMISSAO e tipoSALDO_ZERO. Não presume dispensa fiscal. Hash ef25e7c062d3356d7b1850fa9e1c990f4bad44bcd1c18132157d40d1baa35752 permanece igual desde a preparação.
- Evidências fechamento-entrega-ficticia.txt, fechamento-entrega-replay.txt, fechamento-tratativas-vazio.txt, fechamento-nao-emissao.txt e fechamento-resolucao-zero.txt. Rótulo Referência indisponível no seletor opcional Entrega reproduz F07; declaração foi possível sem essa referência opcional.

### FE13-MANUAL01 — referências fiscais fictícias tardias e reconciliação integral
- No ciclo14→15 CLI9/ARM6 já FINALIZADO_SEM_EMISSAO, registradas referências locais inteiramente fictícias de emissorQAFINEMISSOR1510: QA-NFSE-FICTICIA-20261015-A/QA1510A às23:29 (516acf44-91b9-4121-86ee-18237319a6ce) e B/QA1510B às23:30 (cd0d1826-b61c-4436-a4c3-7f7adcc77ba5). Ambas preservadas; CONFLITO_EXTERNO, estado externoEMITIDO, estado financeiro originalFINALIZADA_SEM_EMISSAO e hash inalterados, coerentes com doc31. Nenhuma NFS-e real emitida/alterada/cancelada.
- Tratativa local fictícia b52ef4ed-8cbe-4498-91b9-df3ac1e588d7: lista exata das duas referências predefinidas, ambas CANCELAMENTO_COMPROVADO, resultadoNAO_EMITIDO_CONFIRMADO, conferência23:33, fonte/responsável QA. Restaurou FINALIZADO_SEM_EMISSAO sem apagar referências/criar versão/trocar hash. Consulta independente3ba1204d-c04d-44c8-8b7e-58a9b135346e confirmou; lista de versões f76465c7-bed6-462b-8dc8-33e2f2ff0f20 contém somenteV1 FINALIZADA_SEM_EMISSAO/NAO_EMITIDO_CONFIRMADO, saldoZERO0 e hash original.
- F07 ampliada: formulário de tratativa preenche corretamente duas referências, mas os dois seletores e a confirmação dizem Referência atual indisponível/Referência indisponível, sem distinguir A/B. Como ambas receberam a mesma decisão neste teste, foi possível validar a reconciliação sem presumir qual era cada opção. A interface não permitiu conferir identidades em decisões distintas. Evidência F07-tratativa-duas-referencias-sem-rotulo.txt; resultados fechamento-nfse-tardia-a.txt, fechamento-nfse-tardia-a-b.txt, fechamento-tratativa-nao-emitido.txt e fechamento-versoes-financeiro-qa.txt.
- Validação de limite emissorDocumento recusou um identificador fictício excessivamente longo; corrigido paraQAFINEMISSOR1510. Não classificado como defeito.

### FE13-MANUAL01 — ajuste identificado entre ciclos e replay
- CLI9/ARM6: cálculo15→16 completo0 e fechamento seguinte preparado EM_REVISAO/V1 (arquivos calculo-financeiro-ciclo15-16.txt e fechamento-destino15-16.txt). Ciclo ainda aberto civilmente; não tratado como cobrança executada/aprovável.
- Consultada explicitamente versão1 do original14→15, guardada como origem; nova consulta8c86a0bf-107c-4a37-8c3a-c9a5cbba82d1 preservou finalização sem emissão, duas referências e hash. Recálculo novo do original tambémCOMPLETO0, selecionado pelo resultado (calculo-correcao-original-zero.txt).
- Ajuste eb91f04e-858c-4b78-ace9-f7a15a6d0768: destino15→16/revisão0 conferida, base0/corrigido0/diferença0, CORRECAO_CALCULO/VALIDADO, hashcorreção d804f54533f7046c872ad821a28e2c53090d7a1e78d1d3cf18415668b6cf7485; aplicadoVersao/Em ausentes. Replay790c26bc-c3cf-4477-94ae-237c526895d7 e lista independente mantiveram exatamente um ajuste. Não declarou APLICADO na mera validação.
- Evidências ajuste-ciclo-zero.txt, ajuste-ciclo-zero-replay.txt e ajustes-lista-um-apos-replay.txt; captura nos outputs do chat. F09 também aparece em Origem Versao com duas opções indistinguíveis Versão1; utilizou-se a origem explicitamente guardada na consulta, sem adivinhar outra opção. Ao selecionar destino posterior, revisão não muda automaticamente: preenchida manualmente0 conforme registro atual; não se enviou revisão7 do original por engano.

### FE13-MANUAL01 — encerramentos de vigência fictícios
- No contexto financeiro CLI9/ARM6, encerrados por comandos próprios de Serviços e tabelas, com fim exclusivo16/10/2026: TAB-000005 (8cca3aeb-ca5a-4505-947a-3a85745c6763), vínculo (correlação em vinculo-financeiro-encerrado16.txt), contrato (3ab9aa1f-dfd6-4c21-b325-ccb6a6a11d03). Mantidos início14/10, tarifa e parâmetros; ciclo14→15 finalizado e destino15→16 em revisão permanecem históricos.
- Evidências tabela-financeiro-encerrada16.txt, vinculo-financeiro-encerrado16.txt e contrato-financeiro-encerrado16.txt. Não interpretado encerramento de vigência como quitação do fechamento em revisão ou aplicação do ajuste.

### FE13-MANUAL01-F17 — encerramento de vigência usa tipo CLIENTE sem opção do alvo correto (P1, aberto)
- Reprodução: Cadastros → Encerramento e impedimentos → CLI-000009 QA financeiro → Solicitar encerramento definitivo (3b6764a7-ecd0-4ff0-8875-6f15aa42196e, sucesso) → Outras operações → Encerrar vigência identificada. Contexto explícito CLI9/ARM6; tabela/vínculo/contrato QA previamente consultados. Informar corte16/10/2026 e usar compromisso consultado fechamento15→16.
- Formulário não oferece tipo/identificador da vigência. A confirmação gera automaticamente Tipo CLIENTE e o alvo do cadastro, em vez de CONTRATO/TABELA/VINCULO (tipos documentados no doc31, seção encerramento). O operador só pode editar corte, compromisso e motivo; não há como escolher a configuração correta nesse caminho.
- Confirmar retorna DADOS_INVALIDOS, Tipo de vigência inválido., correlação4f345a0e-c52e-4d03-a144-6c7b83e2c323. Proteção do servidor correta; nenhum encerramento efetuado por esse comando. Defeito de encaminhamento/formulário da UI, não ausência de dados nem corte incompatível. Os comandos próprios de Serviços e tabelas encerraram as mesmas configurações de teste corretamente antes, conforme registro anterior.
- Esperado: oferecer explicitamente contrato/tabela/vínculo consultado e sua revisão, ou encaminhar à respectiva configuração; não disponibilizar ação com CLIENTE na rota de vigência. Evidências F17-vigencia-confirma-tipo-cliente.txt, F17-vigencia-tipo-cliente-recusado.txt/.png. Sem correção de código.
- A solicitação do CLI9 mostrou exatamente dois impedimentos reais: ajusteVALIDADO ainda não aplicado e fechamento15→16 ainda não finalizado. Encerramentos de vigência não eliminaram essas obrigações, corretamente (encerramento-solicitado-cli9.txt). Aviso nativo de descarte foi respondido pelo próprio agente; usuário não precisou clicar.

### FE13-MANUAL01 — configuração de validade e bloqueio legado por avaria
- Aviso de validade CLI8/ARM6 anteriormente ausente: configurado30dias, revisão inicial0, ConfiguradaSim, c0bf6cac-54a7-4dac-9688-71efe0aea14b (aviso-validade-configurado30.txt). Produtos QA atuais não controlam validade; ausência de alerta de vencimento não é defeito.
- RemanejamentoA TRI→ARM01 aceito dc44cd03-e73a-4e50-91e5-0b4ca542f7ac, unidade revisão17, medidas100kg/1m/empilhamento1 preservadas e disponibilidadeSim confirmada em detalhe. Contornado F14 removendo o bloco de medidas já existentes antes de enviar, sem mudar medidas.
- Botão Registrar bloqueio por avaria (legado da etapa Bloqueio e liberação), A/revisão17:958c458a-da1f-4825-a4fb-9beed65d3b1d. Quantidade física5/posiçãoARM01 preservadas; BloqueadaSim/AvariaPosteriorSim/DisponívelNão; resposta AvariaNãoinformado, sem fingir ocorrência detalhada. Estado provocado pelo teste e explicitamente conhecido; não classificado como falha. Evidência bloqueio-avaria-legado-a.txt.
- B10 recebeu primeiro endereçamento fictício100kg/1×1×1m/empilhamento1/uma posiçãoARM02; confirmação e revisão em coletor-b-primeiro-arm02.txt. Físico total permanece18 (A5+B10+D3), sem criação de estoque. A permanece bloqueada por avaria deste teste; B será usada para remanescente. Unidades não formam o conjuntoCON4 nesta operação individual.

### FE13-MANUAL01-F18 — remanescente preenche revisão do cadastro no item da unidade (P2, aberto)
- CLI8 solicitado para encerramento, B explicitamente consultada/selecionada em Estoque pelo UUID bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80 após primeiro endereçamento; revisão da unidade1 comprovada na confirmação do coletor e referência da jornada. Ao voltar ao detalhe de encerramento do cliente e Resolver estoque remanescente → Adicionar Unidades, a opção selecionada é B (valor1359), mas Unidades/Item1/Revisão atual vem6, revisão do cadastro selecionado, em vez da revisão1 da unidade.
- Aconteceu antes e depois da consulta/seleção explícita de B. Corrigido manualmente para1 antes do envio; operação aceita48ba98d1-8d6c-455b-90ce-bacbf0ed689e confirma que1 era a revisão aplicável. Não foi enviado6 para provocar uma recusa falsa. Esperado: obter a revisão da unidade selecionada ou exigir preenchimento sem sugerir revisão de outra entidade. Evidências coletor-b-primeiro-arm02.txt, unidade-b-atual-antes-remanescente.txt; estado de formulário e sequência registrados nas observações da sessão. O relatório remanescente-justificativa-fifo-vazia.txt foi salvo após a correção da revisão, portanto não serve como captura visual do valor6 original.

### FE13-MANUAL01-F19 — justificativa FIFO opcional vazia bloqueia a resolução sem exceção (P2, aberto)
- Mesmo formulário de remanescente B10, todos os campos obrigatórios preenchidos, revisão1 e etiquetaUUID corretas, Justificativa Fifo sem asterisco/vazia: Conferir e confirmar recusa localmente Tamanho inválido: justificativaFifo. Doc31/inventário tratam justificativaFifo como opcional, necessária para exceção efetiva; não há campo faltante de quantidade/etiqueta.
- Preencher justificativa explicativa permitiu confirmar. Resposta48ba98d1-8d6c-455b-90ce-bacbf0ed689e criou SAI-000006 RESERVADO, B10/ATIVA, ExcecaoFifoNão/JustificadaPorNãoinformado. Esperado: omitir/aceitar string vazia opcional quando não há exceção; manter exigência para exceção real. Evidências remanescente-justificativa-fifo-vazia.txt e remanescente-b10-reservado.txt. Não houve correção de código.

### FE13-MANUAL01 — resolução de remanescente físico
- CLI8 passou a ENCERRAMENTO_PENDENTE para testar exclusivamente remanescente já recebido; B10 boa/endereçoARM02 foi explicitamente selecionada. A5 continua bloqueada por avaria; D3 sem posição. Não se tratou indisponibilidade normal do cadastro pendente como defeito de estoque.
- Resolução48ba98d1-8d6c-455b-90ce-bacbf0ed689e criou SAI-000006 e reservaB10 sem nova entrada/baixa e sem exceçãoFIFO. Detalhe genérico informa PodeProsseguirNão/unidade1359 impedida; será conferido o caminho Gestor resolverPendentes antes de qualquer conclusão de defeito. Estoque físico total18 preservado.

### FE13-MANUAL01 — revalidação, leitura com resolução e cancelamento antes da separação
- SAI6/B10: GET do detalhe confirmou RESERVADO e PodeProsseguirNão no cliente pendente. Revalidação comum sem corpo recusou RESERVA_IMPEDIDA6e839280-597f-48c4-87e1-c013d9b281a0, preservando reserva. Não classificado defeito: para concluir compromisso de cadastro pendente, doc27 exige opção resolverPendentes/Gestor.
- Consultar etiqueta da unidade dentro do formulário confirmou B/versão de conteúdo0 e preencheu campo. Reserva B explicitamente marcada; leitura com Resolver pendências (Gestor)=Sim foi aceita, registrando LEITURA às23:52, sem mover posição/baixar quantidade. Detalhe genérico continua identificando impedimento comum do cadastro, distinto da autorização pontual de resolução.
- Registrar cancelamento da SAI6 antes da separação física aceito às23:53: pedidoCANCELADO, reservaB10CANCELADA com encerramento, histórico mantido. Nenhuma retirada/novo recebimento. Evidências sai6-detalhe-pendente.txt, sai6-revalidacao-pendente.txt, sai6-leitura-resolver-pendentes-gestor.txt e sai6-cancelada-antes-separacao.txt.

### FE13-MANUAL01-F20 — sugestão de serviço preenche referência de execução inválida (P2, aberto)
- Serviços e cálculo → detalhe do fato de entradaA → Consultar sugestões de serviçoSRV-000004 → Consultar: retornou sugestãoB ENTRADA:UNIDADE:1359/quantidade1/execução23:46 (15c41f58-867f-4930-a137-410b650f9664). Selecionar registro e abrir Registrar fato preenche automaticamente OrigemSUGESTAO, unidadeB, quantidade1, instante exato e ReferenciaExecucao=ENTRADA:UNIDADE:1359.
- Com campos complementares válidos, Conferir e confirmar recusa localmente Formato inválido: referenciaExecucao. O texto inválido foi produzido pela própria seleção, não digitado pelo operador. Limpar somente essa referência opcional permitiu avançar; backend gerou corretamente a chave do fato a partir da origem sugerida.
- Esperado: não transcrever chaveFato no campo destinado à referência manual, ou usar preenchimento compatível com o contrato. Evidências F20-sugestao-referencia-execucao-invalida.txt/.png, sugestoes-servico-entrada-a-b.txt e fato-entrada-b-criado.txt.
- Erro separado da execução: foram adicionadas cotas manuais à entrada sugerida; backend recusou corretamente DADOS_INVALIDOS c88cc939-c9a5-4b7d-9831-803346226c76, Entrada/saída usa rateio de origens, sem substituir cotas físicas. Removidas antes da confirmação positiva. Não incluído como defeito do WMS.

### FE13-MANUAL01-F21 — anulação de fato recém-confirmado retorna erro interno repetido (P1, aberto)
- Criado fato QA de entradaB, chaveENTRADA:UNIDADE:1359, serviçoSRV4, quantidade1PALLET, origemSUGESTAO, notaNF15102026/cota física10 derivada pelo servidor; confirmação f23c13c1-1d81-4970-aea9-080b32ecd182. ClienteCLI8 reativado antes; não há fechamento finalizado deste contexto, somente revisão pendente. Consulta independente confirmou identidade/estado CONFIRMADO.
- Anular fato identificado apenas no registroB recém-criado, com motivo explícito: ERRO_INTERNO93f7cc33-7fc7-42b1-979a-521fdf634005, resultado desconhecido. Consultar estado atual sem repetir mostrou mesmo fatoCONFIRMADO. Repetir exatamente a mesma operação (mesmo conteúdo/identificador) retornou novamenteERRO_INTERNO b736196b-9bd9-4647-ba7c-359c582199be; nova consulta continuouCONFIRMADO. Não criada nova chave nem presumida anulação.
- Esperado: anulação identificada com histórico, ou recusa de negócio explícita se houver impedimento; erro interno não explica nem conclui operação válida. Causa técnica não investigada por API/log/banco, pois escopo é manual. Os dois fatos anteriores permanecem; o fatoB continua confirmado e precisa de acompanhamento/correção posterior. Estoque não foi baixado.
- Evidências F21-anular-fato-b-erro-interno.txt/.png, F21-consulta-fato-b-ainda-confirmado.txt, F21-replay-e-consulta-confirmado.txt, fato-b-consulta-antes-anular.txt. Captura nos outputs do chat.

### FE13-MANUAL01 — fatos físicos, sugestões e reativação
- CLI8 reativado após cancelar remanescente, confirmado ATIVO (cliente-reativado-apos-remanescente.txt). A continua bloqueada pelo teste legado; B10 emARM02/reserva cancelada; D3 sem posição.
- Consultar fatos físicos na saídaXML retirada retornou6 fatos históricos da unidade, incluindo avaria/reparo, separação/retorno anterior e retirada10→5 às21:49; sem duplicação da baixa. Evidência expedicao-fatos-fisicos-consulta.txt.
- Consulta de sugestões de entrada retornou somenteB ainda sem fato confirmado, não A já registrada; ausência de A é correta. Confirmado fatoB como descritoF21, aindaCONFIRMADO devido erro na tentativa de anulação.

### FE13-MANUAL01-F22 — marcos financeiros usam catálogo de fatos de serviço no lugar de avaria/fato físico (P1, aberto)
- Serviços e cálculo → 2.Avaria financeira: Identificador oferece somente as três execuções dos fatos de serviço do contexto (21:49 saída,21:29 entradaA,23:46 entradaB), seleciona o último fato e a consulta mostra Cadastro não encontrado. O contrato doc29/inventário exige avaria{id}, não fato de serviço{id}.
- Para excluir falta de seleção/dado, retornou-se a Estoque → Avarias e reparos → unidadeA UUID explícito → detalhe: consulta relacionada retornou ocorrência real2UN/base10/RODOGARCIA/REPARADA, ocorrida21:32/resolvida21:35. Selecionar registro1 da ocorrência e voltar à avaria financeira não alterou o filtro: continuam somente os três fatos de serviço e Cadastro não encontrado.
- Registrar marco financeiro também oferece esses mesmos três fatos tanto em Identificador (avaria) quanto em Fato Permanencia. Os fatos físicos consultados na expedição incluem avaria21:32, reparo21:35, separações/retorno e retirada, distintos das execuções comerciais oferecidas. Não há opção para a ocorrência consultada nem para selecionar corretamente seu fato de permanência por este formulário.
- Esperado: catálogo/seleção tipados por avaria e fato de permanência, preservando contexto/identidade, e consulta dos marcos da ocorrência escolhida. Bloqueio na UI antes de comando; não se submeteu ID de fato comercial como avaria/fato físico nem se chamou ausência de registros financeiros vazios de defeito.
- Evidências marcos-financeiros-filtro-fatos-observacao.txt, avaria-a-existente-reparada-para-marcos.txt, F22-marcos-aponta-fatos-apos-selecionar-avaria.txt e F22-marco-selectores-fatos-servico.txt/.png. Causa de cadastro/roteamento a investigar depois, sem correção de código.

### FE13-MANUAL01 — carga CAR5 preparada e guarda contra cancelamento com físico
- Criada CAR-000005/etiqueta fornecidaQA1510-CARGA-RESOLUCAO-03, uma unidadeUN boa, nota15/15102033/emissão15/10, chegada/FIFO23:55, valor10 e PALLET/DUNQA1510-DUN. Dados explicitamente fictícios, carga independente para testar cancelamento após preparação. Criação5508a286-6009-46e2-a091-ce1b2ef894d7; conteúdo revisão1/hash2a4f57f20bb669966b12f8411f300fe079426d56912f5f715168a669928f61b3.
- F12 ampliada: formulário Preparar também preenche Revisao como [object Object], não apenas a coluna da lista. Com motivo válido, Conferir e confirmar recusa Inteiro sem fração esperado: revisao. Corrigido para número1 conforme revisão consultada; preparação aceita, uma entrada/etiqueta f77808af-df9b-4277-861f-907c71275c20, PREPARADA/CONFERENCIA_PENDENTE. Evidências F12-preparar-revisao-object-invalida.txt/.png e carga-car5-preparada.txt.
- Resolver cancelamento pendente, teste negativo declarado sem referência física ainda inexistente: recusou FISICO_ESTAGIO_PENDENTE, Resolva todo o físico/reserva/ocupação antes de cancelar a preparação., correlação2b8dbc63-7dc0-4180-92bb-b2b666354f87. CAR5 continuaPREPARADA, não apagou entrada/etiqueta/conteúdo. Guarda correta conforme doc31; não classificada falha. Cancelamento positivo requer resolver esse físico por fluxo identificado.
- Total físico esperado agora19 (A5+B10+D3+E1 preparada), sem somar estágio novamente. E=f77808af-df9b-4277-861f-907c71275c20 segue indisponível enquanto preparada. Evidências carga-resolucao03-criada.txt, carga-car5-preparada.txt e carga-car5-cancelamento-fisico-pendente.txt. Conferência independente do total ainda a fazer.

### FE13-MANUAL01 — ciclo15→16 após virada civil: aprovação e aplicação única do ajuste
- Já em16/10/2026, novo cálculo15→16 COMPLETO0 usando resolução histórica identificada FECHAMENTO do próprio destino21, clienteCLI9 aindaENCERRAMENTO_PENDENTE; vigências haviam terminado16. Comando atualiza a obrigação existente sem reativar cadastro nem criar serviço futuro (calculo15-16-apos-ajuste-vigencias.txt).
- O registro inicial do ajuste criara V2 automaticamente no destino, preservandoV1. Reabertura com cálculo atualizado gerouV3, um único ajusteVALIDADO e hash3963986e71b5c6ea30dff7418f5016a6c018664b9f9d54f40525c77ba6db3e26 (ciclo15-16-reaberto-v3.txt).
- Aprovação integralV3 do ciclo já encerrado aceitou saldoZERO; ajuste continuouVALIDADO, sem aplicação antecipada. Declaração fictícia de não emissão V3 às00:08:23, 141d6815-999b-4226-9b7c-254dee4bf6b1, sem entrega/NFS-e real (ciclo15-16-v3-aprovada-ajuste-validado.txt e ciclo15-16-v3-nao-emissao.txt).
- ResoluçãoZERO fictícia QA-ZERO-FICTICIO-20261016-C15V3 às00:09:14 finalizou ciclo/V3 SEM_EMISSAO e marcou ajusteAPLICADO, aplicadoVersao3/data00:09. Repetir mesmaoperação dbd0b775-bd87-4619-9429-fd9bfa027172 manteve uma aplicação, mesma versão/hash; lista independente de ajustes confirmou exatamente umAPLICADO. Evidências ciclo15-16-v3-finalizada-ajuste-aplicado.txt, ciclo15-16-v3-finalizacao-replay.txt e captura nos outputs. Nenhuma cobrança, emissão ou transmissão externa.

### FE13-MANUAL01 — inativação positiva após resolução financeira integral
- CLI9 financeiro fictício, que antes tinha dois impedimentos (ajuste/ciclo), passou a0 impedimentos depois da finalização15→16V3 e aplicação do ajuste; vigências terminadas16/10. Consulta independente registrada em cliente-financeiro-zero-impedimentos.txt.
- Confirmar inativação aceitou aa9d1de4-ed50-4b81-b3c6-f52a297031ab: clienteINATIVO/zero impedimentos. Nenhum estoque/cadastro físico do CLI8 alterado. Ciclos, referências fictícias e ajuste permanecem históricos; não presume reativação deINATIVO. Evidência cliente-financeiro-inativado.txt e captura nos outputs.

### FE13-MANUAL01-F23 — seletor de carga inicial falha e impede resolução da PREPARADA (P1, aberto)
- CAR5 consultada PREPARADA/uma unidadeE f77808af-df9b-4277-861f-907c71275c20, entradaENT-000015, nota15102033, física1/não disponível/sem posição. Unidade explicitamente selecionada em Estoque. Cadastros → Encerramento e impedimentos → CLI8 (ATIVO) → Resolver estoque remanescente: Carga Inicial passa de Carregando opções para somente Selecione pelo nome ou código e Não foi possível consultar as opções. Reabra a seleção para tentar novamente.
- Reabrir o seletor e fechar com Enter manteve erro/zeroopções. O mesmo erro havia aparecido no testeB, antes da criaçãoCAR5, mas não foi classificado então; agora a carga elegívelPREPARADA e o conteúdo estão comprovados pela consulta independente, sem ausência de dados ou dúvida de contexto. Também foi reaberto o formulário depois das consultas; não resolveu.
- Doc31 permite clienteATIVO quando cargaPREPARADA fornece a necessidade concreta, exigindo cargaInicialId e leitura exaustiva das etiquetas. Sem opção selecionável/nem campo de identidade manual válido, não se pode declarar corretamente cargaInicialId pela UI. Não se enviou o remanescente sem essa ligação para depois chamar a recusa de negócio de defeito.
- Esperado: carregar as cargas autorizadas do cliente/armazém, exibir referência/situação e permitir selecionarCAR5; preservar guardas de conteúdo, etiqueta e FIFO. Bloqueio da resolução para retirada identificada; caminho alternativo de contagem aplicada continua com F10. Cancelamento positivo após zerar não pôde ser concluído. CAR5 permanecePREPARADA para reprodução, sem apagar histórico nem presumir cancelamento.
- Evidências unidade-e-preparada-sem-posicao.txt, F23-remanescente-carga-nao-carrega-apos-reabrir.txt/.png e captura nos outputs. Causa técnica não determinada nesta auditoria manual.
- F18 reproduzida novamente comE/revisão0 explicitamente selecionada: adicionar unidade preenche revisão7, a do cadastroCLI8 atual, enquanto opção selecionada é1362/E. Captura adicional F18-remanescente-revisao7-para-unidade-e0.txt/.png. Isso elimina a hipótese de operador ter deixado de selecionarB no primeiro recorte.

### FE13-MANUAL01 — retomada16/10: usuários e conferência independente de saldos
- Retomada no navegador integrado: a aba estava no Início, sem formulário de usuário pendente. Consultados ai-memory (recentes do projeto rodogarcia/wms-rodogarcia), Graphify e documentação atual. As entregas de apresentação FE03-USUARIO-UX01/FE11 datadas16/10 registram fonte pronta e versão ativa não medida; não foram tratadas como validação integrada nem como correção já comprovada dos achados antigos.
- Usuários: quatro contas, principal protegida sem Editar/Redefinir. Novo usuário preenchido somente como rascunho fictício; busca QA, seleção de CLI8/ARM6, alternância Operação/Supervisor/Gestor e filtro sem correspondência seguido de retorno preservaram as seleções. Cancelar descartou o formulário; lista permaneceu com quatro contas. Edição e redefinição da conta DEV Gestor abertas e canceladas, sem salvar acesso/credencial. Evidência usuarios-rascunho-filtro-preserva-selecao.txt. Cadastro, alteração de privilégios e novas senhas não executados; inspeção não comprova autenticação como outro perfil.
- Conferência independente registrada antes da pausa: físico19, unitizado19, disponível10, reservado0, bloqueado9, sem endereço4, armazenagem15, avariado5, estágio0/conferência pendente1. Valor conhecido140, valor exato indisponível pela pendência AVARIA_BE08_SEM_DETALHE da unidadeA, provocada pelo teste legado, e estágio separado do valor confirmado. Não é falha aritmética comprovada nem soma adicional de estoque da carga preparada. Evidência indicadores-19-valor-conhecido140-pendente-avaria.txt.
- Início no contexto explicitamente aplicado CLI8/ARM6: SKU QA1510-UN19UN =10disponíveis+0reservadas+9demais indisponíveis; SKU QA1510-XML0. Duas posições de armazenagem ocupadas/duas ativas, quatro unidades logísticas ativas, zero reservas/saídas abertas, uma entrada aberta e valoração pendente. Distinguidos quantidade de produto, unidade logística e posição física. Evidência inicio-qa-19un-10disponiveis-2posicoes.txt. Ajuda de ocupação/fila/disponibilidade/mapa aberta, texto coerente com essas distinções.

### FE13-MANUAL01-F24 — detalhe do mapa apresenta occupancy/Ocupação como conteúdo (P2, aberto)
- Início → contexto CLI8/ARM6 aplicado → mapa → QA1510-ARM01 ocupada/bloqueada. Após terminar a consulta, o quadro exibe literalmente rótulo inglês occupancy e valor Ocupação, sem informação operacional nesse par. Captura visual confirma que não é somente nome acessível ou carregamento.
- Fechar e consultar QA1510-ARM02 ocupada reproduziu o mesmo par literal. Os demais dados retornaram corretamente: A5UN/UUIDfc14c5c0…/bloqueada e B10UN/UUIDbf1b96bc…/sem bloqueio, capacidade5000kg e limites5m. Portanto há registros/dados efetivos, e o achado não depende de filtro vazio nem de estoque ausente.
- Esperado: rótulo legível em português e informação efetiva correspondente ao campo, ou ausência explícita se não fornecida; não exibir palavra genérica como se fosse valor da ocupação. Não foi inferido número/percentual esperado nem erro da regra de capacidade. Evidências mapa-arm01-occupancy-literal.txt/.png e mapa-arm02-occupancy-literal.txt. Sem correção de código.

### FE13-MANUAL01 — reinício informado16/10 e continuação dos atalhos
- Responsável informou reinício por volta10:36. Aba anterior não existia mais; aberta nova aba no mesmo Dev Tunnel, sessão recuperada sem alterar senha. Reaplicados CLI8/ARM6; painel confirmou19UN/10disponíveis/9indisponíveis/quatro unidades/duas posições ocupadas e uma entrada aberta. Evidência apos-reinicio16-1037-painel-qa.txt. O reinício foi feito pelo usuário, não pelo auditor.
- Mapa antes do reinício: Ocupadas retornou ARM01/ARM02; Disponíveis retornou zero, coerente com duas posições de armazenagem ocupadas. Buscar QA1510-ARM02 retornou uma; código inexistente por Enter retornou zero explícito; Limpar restaurou cinco. Detalhes das cinco posições abertos; três áreas especiais sem unidades. Atualizar visão funcionou; Atualizar gráficos iniciou atualização mantendo anterior identificada. A interrupção/reinício ocorreu antes de concluir a sequência seguinte, sem atribuir esse evento ao WMS.
- Atalhos após reinício: Receber e conferir abriu Entrada mantendo contexto; Ler e endereçar abriu Coletor/Endereçar movimentar; Reservar e separar abriu Saída/Pedido integral. Navegação somente, sem confirmar movimentação.
- ENT13/XML persistido: documento original consultado e ocultado com sucesso, chave41261099151515000150550150151020311123456783, nota15102031/série15, quantidade2UN/total20, protocolo fictício31 e limites de autenticidade claramente indicados. Pedido permanece QUARENTENA, físico0 após estorno, revisão5; não confundido com perda de estoque. Evidência xml-original-pedido13-consultado-apos-reinicio.txt.
- Baixar XML original foi clicado, mas o controle do navegador não capturou evento/arquivo (uma espera expirou; nova tentativa limitada5s também inconclusiva), sem erro visível do WMS. Nenhum arquivo com referência15102031 encontrado no Downloads padrão. Não classificado como defeito confirmado nem como download aprovado; caminho do arquivo e integridade ainda sem prova manual. Não utilizado acesso direto à API para contornar o teste.

### FE13-MANUAL01 — revalidação positiva e cancelamento SAI7,16/10
- Criada saída fictícia integral SAI-000007/1UN SKUQA1510-UN, contextoCLI8ATIVO/ARM6, correlação797551f4-91d5-4d78-b513-ea554d906b51. FIFO retornou1UN; reserva automática19428b4b-5203-48ee-8304-268a37c9b2d5 confirmou unidadeB UUIDbf1b96bc-62e4-4f29-80fb-9cbf2bfaba80/quantidade1/ATIVA, sem exceçãoFIFO, pedidoRESERVADO/revisão1/PodeProsseguirSim.
- Revalidar disponibilidade confirmado0dd22cc8-ad53-4bc9-b055-de905174c273 retornouRESERVADO/PodeProsseguirSim/zero unidades impedidas. Completa o ramo positivo antes pendente; recusa RESERVA_IMPEDIDA no CLI8 pendente/SAI6 continua resultado negativo esperado, não falha.
- Registrar cancelamento antes de leitura/separação aceito1ab42a04-4168-4674-b3eb-3907b0ff40d7 às10:43: SAI7CANCELADO, reserva1UN CANCELADA com encerramento, sem retirada/baixa. Consulta independente no Início manteve SKU19UN =10disponíveis+0reservadas+9indisponíveis; fila de saída zero. B permanece emARM02, A bloqueada, D/E sem posição; nenhum efeito físico necessário neste teste.
- Evidências sai7-criada-1un-ativa.txt, sai7-reservada-b1-podeprosseguir.txt, sai7-revalidada-positiva.txt, sai7-cancelada-sem-separacao.txt e captura nos outputs do chat. F04 permanece na confirmação genérica sem pedido/quantidade estruturados; F07 reapareceu em unidade sugerida/nota da reserva e cliente/armazém de alguns resultados, embora detalhes do pedido e UUID da reserva permitam conferir o alvo. Não criado novo defeito duplicado para essas manifestações.

### FE13-MANUAL01 — conferência visual e teclado390px, desktop restaurado
- Recorte de responsividade em390×844: Início sem overflow horizontal da página (clientWidth/scrollWidth375). Mapa passou a uma coluna por vez; Próximas/Anteriores percorreu ARM01↔ARM02 com limites desabilitados. Enter abriu ARM02, Escape fechou e devolveu foco ao botão correto. UUID quebrou em linhas dentro do diálogo. F24 permaneceu após reinício, inclusive no recorte móvel. Evidência mapa-mobile390-detalhe-arm02.png.
- Menu móvel abriu, percorreu módulos para frente/atrás e navegou a Usuários, fechando corretamente. Novo usuário já apresentou atualização FE03-USUARIO-UX01: ajuda revisada, busca acima da lista, nomes QA completos com quebra, seleção destacada e contagem. Space selecionou CLI8/ARM6; busca inexistente informou Nenhuma opção corresponde à busca e 1selecionado/0exibidas, sem perder vínculo. Evidência usuarios-mobile390-qa-selecao.png. Rascunho cancelado; nenhuma conta/senha/permissão criada.
- Responsável perguntou pelo formato mobile; explicado o recorte de teste e restaurada imediatamente a dimensão padrão. Leitura do viewport confirmou1543×892 e navegação desktop. Continuação segue desktop. Isso foi alteração temporária da prévia, não implementação de layout nem preferência persistente do WMS.
- Atalho Conferir fechamento abriu Fechamentos e ESL/Ciclo e versões mantendoCLI8/ARM6. Não recalculou nem alterou cobrança ao navegar.

### FE13-MANUAL01 — controles do mapa e acesso,16/10
- Desktop: seletor do mapa alterou também o armazém do contexto para DEV fictício - Armazém1. RuaR01 avançou colunas1–8→5–12; posição livreA1-10 abriu detalhe coerente e Endereçar encaminhou ao coletor com Destino selecionado:A1-10. Nenhum UUID lido nem alocação confirmada nesse outro contexto. Reaplicar ARM6 limpou o destino anterior, corretamente. Evidências mapa-enderecar-encaminha-destino-sem-mover.txt e coletor-contexto-qa-limpa-destino-anterior.txt.
- Produtos do painel percorreram página1→2→1; mapa geral página1→2→1, com atualização terminada antes de continuar. Contexto QA reaplicado depois. Dados vazios no cruzamento CLI8/outro armazém são esperados; ocupação física geral e gráficos do cliente têm escopos distintos conforme ajuda.
- Minha senha abriu orientação de encerramento de sessões e três campos; cancelada vazia. Sair mostrou login; ajuda Esqueceu sua senha/Primeiro acesso aberta e fechada. Recarga mostrou inicialmente login sem módulos (logout-reload-continua-login.txt). Antes de preencher novamente, a tela retornou ao Início autenticado e tentativa no índice antigo não executou. Origem dessa transição não aferida; não comprova nova autenticação pelo auditor nem defeito de logout. Houve também mudanças de arquivos/recarga da instância no intervalo; manter o ponto inconclusivo, sem expor credenciais ou fazer tentativas incorretas.

### FE13-MANUAL01-F25 — folha de estilos indisponível no DEV (P1, corrigido e conferido16/10)
- Após alterações da fonte durante a auditoria, a instância exibiu página branca/sem estilos e erro visível plugin:vite:css/postcss ENOENT, tentando abrir frontend/modules/recebimento/receivingNotes.css. Persistiu após Escape e recarga. A leitura do código só começou depois do pedido expresso do responsável ta tudo branco, ajeite isso; essa correção pontual amplia o escopo anterior de somente registrar falhas.
- Arquivo efetivo existe em frontend/src/modules/recebimento/receivingNotes.css; importação na folha de entrada era relativa. Renovar apenas o timestamp da folha não resolveu o erro. Ajustada somente essa importação para /src/modules/recebimento/receivingNotes.css, mantendo conteúdo do módulo e alterações preexistentes. Recarga do mesmo Dev Tunnel recuperou tema escuro, menu, cabeçalho, cards e gráficos em1543×892; sem novo reinício de servidor, enfraquecer CSP ou mexer em dados de negócio.
- Conferência de arquivos:33 importações CSS presentes. Empacotamento Vite direto aprovado295módulos, CSS172.87kB, saída isolada css-build dentro da pasta de evidências; não substitui tipagem/testes. npm run build completo parou antes do empacotamento por erros TS2769 preexistentes na fonte atual tests/receiving-note-detail.test.tsx:76/80 (opção exact de ByRoleOptions), arquivo não alterado pelo auditor. Não ampliada esta correção para testes de outra entrega. Logs correcao-css-build.log e correcao-css-vite.log.
- Evidências interrupcao-css-receivingNotes-ausente.txt, captura interrupcao-estilos-dev.png e css-restaurado-desktop.png nos outputs do chat. Erro de carregamento confirmado; causa da resolução relativa/cache não determinada além do caminho observado. Correção operacional verificada no navegador; demais achadosF01–F24 continuam com seus estados próprios.

### FE13-MANUAL01 — V17 em execução, SAI8 identificada
- Após restauraçãoCSS, contextoCLI8/ARM6 reaplicado. Criada SAI-000008/1UN QA1510-UN, correlação69717ca9-e3fb-4073-988d-4784665e2ed6; FIFOa8eb49fd-380a-4045-9bc8-8fd06aeb2399 retornou1UN. Reserva5132456b-1c3f-4f08-8002-0376581bd2e0 confirmouB/bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80/1UNATIVA, pedidoRESERVADO/PodeProsseguirSim. Sem leitura/separação/retirada.
- Evidências sai8-v17-criada.txt e sai8-v17-reservada-b1.txt. UnidadeB consultada depois:10UN/BOA/ARM02/sem bloqueio, indisponível para outra saída por reserva ativa; detalhe da mesma unidade explicitamente selecionado. Formulário Registrar fato (avaria estruturada) aberto, B/revisão6 preenchidos automaticamente; ainda sem envio de avaria neste checkpoint. Próximo: quantidade1, instante posterior à reserva e passado, destinoQUA/código lido, relato explícito; depois revalidar expedição, cancelar e reparar, preservando histórico e físico.

### FE13-MANUAL01-F26 — código lido do endereço recebe UUID da unidade (P2, aberto)
- Estoque → Avarias e reparos → B explicitamente consultada/selecionada → Registrar fato → Adicionar posição: Código lido veio automaticamente bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80, UUID da unidade, embora o destino exija leitura do endereço. Corrigido paraQA1510-QUA antes da avaria. Reproduzido no formulário Registrar reparo da mesma ocorrência: Adicionar Posições de destino inicializa o código com UUID deB; escolherQA1510-ARM02 não atualiza/remove esse valor.
- Captura feita após selecionar ARM02 e antes da correção: endereçoQA1510-ARM02/código lidoUUIDB. Não digitado pelo operador. Doc27 exige código do endereço na destinação e preserva UUID da unidade em campo próprio. Esperado: deixar código lido vazio até a leitura, ou distinguir qualquer sugestão válida; não sugerir UUID de outra entidade como leitura de endereço.
- Corrigido manualmente paraQA1510-ARM02 antes de conferir/enviar, e reparo aceitoe3236ad1-2c04-4caa-b42d-4838737ecbdf. Não enviado o valor errado para provocar recusa e não apontada falha de backend. Evidências F26-reparo-endereco-arm02-codigo-uuid-unidade.txt/.png. Sem correção desse preenchimento.

### FE13-MANUAL01 — V17 concluído: avaria após reserva conserva impedimento e físico
- SAI8 reservada1UN/B10; avaria estruturada1UN/base10/equivalência1, ocorrida16/10 às10:59 e registrada11:01, correlaçãob3c44c67-4004-443c-8a59-a55707179c4d. DestinoQUA e códigoQA1510-QUA conferidos; preservou10UN/UUID/FIFO/início da armazenagem, bloqueadaSim/avariaPosteriorSim. Data preenchida pelo controle nativo e valor conferido antes do envio; dificuldades da ferramenta não foram classificadas como defeito de calendário.
- Revalidar SAI8 após o fato recusouRESERVA_IMPEDIDA1f2d207d-7aa8-4c76-b93f-105bea6da9b7. Consulta independente do pedido manteveRESERVADO/1UNATIVA/unidade1359 impedida/PodeProsseguirNão, sem liberar reserva ou baixar estoque. CumpreV17/doc27.
- Cancelar antes de leitura/separação aceitou b830e115-487b-426a-8f07-200f5c2dd6be: pedidoCANCELADO/reservaCANCELADA. B permaneceu bloqueada emQUA até reparo, sem liberação automática.
- Selecionada ocorrênciaB da lista relacionada; reconhecimento fictícioPROPRIETARIO7a787ada-d517-47ec-b978-cf46dec5d2c1 às11:04, proporçãoSuspensa0/inícioSuspensãoausente, sem suspender cobrança Rodogarcia. Selecionado resultado atualizado da ocorrência, reparo com revisão atualB8 e destinoARM02/código lido correto aceitoe3236ad1-2c04-4caa-b42d-4838737ecbdf: ocorrênciaREPARADA/PROPRIETARIO, B10/ARMAZENAGEM/bloqueadaNão/avariaPosteriorNão/disponívelSim, medidas100kg/1m/uma posição e FIFO15/10 20:00 preservados.
- Consulta independente0235bae8-5591-4d56-ba2e-a834604880e6 confirmouB10/BOA/ARM02/disponívelSim. Nenhuma retirada/nova entrada ou aumento de quantidade; histórico mantém avaria/reparo. Evidências sai8-v17-avaria-b1-quarentena.txt, sai8-v17-revalidacao-reserva-impedida.txt, sai8-v17-detalhe-reserva-preservada-impedida.txt, sai8-v17-cancelada-sem-retirada.txt, sai8-v17-avaria-responsabilidade-proprietario.txt, sai8-v17-reparo-b-arm02-disponivel.txt e sai8-v17-b-consulta-independente-reparada.txt; captura nos outputs. Agregado final ainda a conferir após fechar diálogo.

### FE13-MANUAL01 — V31/insuficiência de atendimento integral
- ApósV17, consulta do painel confirmou novamente19UNfísicas/10disponíveis/0reservadas/9indisponíveis, duas posições ocupadas e zero saídas abertas (v17-final-painel19-disponivel10-reservado0.txt).
- Nova saída fictícia11UN deQA1510-UN, no contextoCLI8/ARM6 com somente10elegíveis: criação recusou SALDO_INSUFICIENTE/Pedido excede o saldo disponível de um dos itens., correlação5f452461-e9e8-49b3-a76d-9a5a7a3cb2f5. Recusa de negócio esperada, não defeito. Não chegou a criar rascunho que exigisse cancelamento; nenhum atendimento parcial foi autorizado.
- Rascunho local descartado pelo próprio agente, respondendo ao aviso nativo; Atualizar lista acionado e consulta terminada com os mesmos5pedidos (SAI5,XMLretirado,SAI6,SAI7,SAI8), últimos cancelados. Evidências v31-pedido11-saldo10-recusado.txt e v31-lista5-apos-recusa-sem-novo-pedido.txt. Reserva antigaCANCELADA deSAI8 ainda aparece no contexto da jornada/Nova saída; não presumida como reserva do pedido recusado.

### FE13-MANUAL01 — autenticação normal confirmada após estabilizaçãoCSS
- Segunda sequência controlada: Sair devolveu login; e-mail/senha existentes fornecidos pelo responsável preenchidos nos campos corretos e Entrar acionado. Interface mostrou Entrando… com campos bloqueados e depois Início autenticado às11:09. Nenhuma senha nova, mudança de privilégio, recuperação real ou salvamento de credencial no navegador. Não gravados valores de credencial nas evidências.
- O ramo positivo atual elimina a pendência de comprovar novo envio de login; não determina retrospectivamente a origem da transição anterior nem comprova revogação concorrente de sessões. Recuperação e primeiro acesso continuam só inspecionados, sem troca de senha por agente.

### FE13-MANUAL01 — continuação 16/10, recebimento parcial, dois dias e avaria (11:22–11:26)

- ENT-000013/CLI8/ARM6/NF15/15102031/item QA1510-XML: chegada fictícia1 boa em15/10 às23:20, correlação6fbe081b-ef42-4beb-b47e-f64f017171b2, revisão6. Consulta independenteae61a61b-697c-42ad-b382-efc64a563895 mostrou previsto2/boa1/avariada0/diferença−1, QUARENTENA e sem efetivação. Efetivar com aceiteNão recusou DIVERGENCIA_PENDENTE9331abdc-980a-4129-b06e-c89095ac20ba, proteção esperadaV27; não classificar como defeito.
- Segunda chegada fictícia0 boas/1 avariada em16/10 às11:22, e303c2e6-c31e-4a7c-ab40-bc81674dfd19, revisão7. Totais2 recebidas/diferença0, mas pedido continuou QUARENTENA/Divergente. AceiteNão recusoue3397631-0c11-47e2-b7e8-3cb71044c12c/DIVERGENCIA_PENDENTE: quantidade total não libera avaria sem soluçãoV28. Uma atualização da página interrompeu uma confirmação anterior antes de enviar; reconsultado o pedido e repetida a conferência, sem atribuir a interrupção ao WMS.
- AceiteSim com tratativa explicitamente fictícia de QA efetivoudd262f84-7775-424d-8122-b401e8a3b57d às11:25, revisão8/EFETIVADO/Conferido com divergência; histórico/tratativa preservados. Sem aceite comercial ou emissão real. Entradas conferidas9143bb54-37fc-4a74-a131-120bd21636a8: primeira1 em triagem, segunda1 em quarentena, ambas a distribuir.
- Datas e rastreabilidade abertas nas duas entradas: chegada15/10 23:20 e16/10 11:22 distintas; ambas FIFO15/10 23:20 e efetivação16/10 11:25. V26 confirmado. Evidências v26-v27-ent13-primeira-chegada-parcial.txt, v27-ent13-falta-rejeitada-sem-aceite.txt, v28-ent13-avaria-rejeitada-sem-aceite.txt, v26-v28-ent13-efetivada-aceite-explicito.txt, v26-ent13-fifo-duas-chegadas-confirmado.txt na pasta da rodada. Estado anterior19UN não é mais o final: acrescentadas2UN conferidas ainda sem unitização, nenhuma nova reserva/retirada.
- Revisão de cadastros no contexto correto: cinco endereços retornados; F02 persiste na colunaCódigo. Configurar capacidade permanece sem opções deIdentificador (F13). Busca de armazém é explicitamente nesta página: QA apareceu na página3/3, sem defeito de ausência de dados.

### FE13-MANUAL01 — relatório por página e conferência final da rodada, 16/10 às11:37

**Relatório de cobertura entregue:** [50 páginas do inventário e quatro superfícies adicionais](orchestracao/.runtime/qa-manual-20261015/relatorio-por-pagina-20261016.md). Uma linha por página com casos exercitados, falhas e limites; 171 ocorrências documentais de ações continuam separadas de uma contagem de testes aprovados. Vínculos hoje integra a visão de Tabelas; Entrada usa seções do pedido. Resultados de fonte/preview de outras frentes não encerram achado manual sem reteste próprio. Homologação integral NÃO aprovada; não há prova de todos os ramos/perfis/combinações, nem promessa de ter encontrado toda falha possível.

- RetesteF08: pedidoXML-909D8001BB8495C6DD6FB1A61F1FB43A6056/RETIRADO/reserva5/baixa6. Formulário preenchido com emitente fictício99151515000150/série16/número161020261/emissão16/10/chegada16/10 às11:20/quantidade5/avariada0/motivoQA. Número inicial com10 dígitos foi corretamente rejeitado e corrigido para9 antes do reteste; essa recusa não é defeito. Com dados válidos, novamente Campo fora do contrato:id/Campo:id, sem confirmação/envio da devolução. Rascunho descartado pelo auditor. Prova revisao-final-F08-devolucao-id-fora-contrato.txt.
- RetesteF15: coletor consultou B/revisão9 e as5 posições QA; códigoQA1510-ARM02 conferido. Segundo endereço e conjunto sem opções após carregamento; medidas existentes100kg/1m³/empilhamento1 continuam preenchidas. Nenhum movimento enviado. Prova revisao-final-F15-coletor-segunda-posicao-conjunto.txt.
- RetesteF22: com ocorrências QA A/B já existentes, formulário de marco financeiro retornou nos dois seletores os três fatos comerciais21:49/21:29/23:46, em vez de avaria/permanência física. Nenhum ID inadequado enviado. Prova revisao-final-F22-marcos-opcoes-servicos.txt. F13/F02/F11 também reconfirmados.
- Páginas de estoque/saída/fiscal/preços/cálculo/fechamentos/contagem/contingência/relatórios/usuários novamente percorridas sem nova gravação de negócio. Evidências revisao-final-*.txt. Auditoria deENT13 retornou8ações, com as2novas chegadas e1efetivação; não duplicadas. ContingênciaQA permaneceCONCILIADA/tipoCONTAGEM; outras variantes não promovidas a aprovação.
- Estado final do painel,11:37: QA1510-UN19físicas/10disponíveis/0reservadas/9indisponíveis; QA1510-XML2físicas/0disponíveis/2sem unitização; total21UN. Quatro unidades logísticas ativas, duas posições armazenagem ocupadas, zero entradas/saídas abertas. CAR5 continuaPREPARADA. Sem confundir unidade ainda sem unitização com posição física ocupada na quarentena.
- Quatro contas permanecem; principal protegido. Formulário de criar usuário aberto e cancelado, sem nova conta/privilégio/credencial. Operação/Supervisor ainda com troca de senha pendente. Política do controle de navegador exige usuário preencher/enviar credencial nova e confirmação específica para concessão de acesso; não executadas essas ações. DownloadXML, hardware, demais tipos de contingência, concorrência/falhas/recuperação e combinações financeiras adicionais seguem explicitamente não homologados.
- Contagem de achados preservada:26; F25CSS corrigido e conferido,25abertos (8P1/17P2). Caminhos positivos diretamente impedidos por F08/F10/F13/F15/F17/F21/F22/F23 dependem das correções e de reteste. Esta entrega fecha a organização do relatório por página, sem declarar teste total aprovado nem implementar as correções de negócio.
