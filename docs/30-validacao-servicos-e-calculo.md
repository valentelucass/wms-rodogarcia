# Validação local de cadastros, serviços e cálculo — D19

**Recorte BE01/BE05/BE12 aceito localmente após P2:292 testes, Vigia favorável e V7/JPA compatíveis em leitura.** Inativação definitiva BE05 segue após BE13/BE14. D19 autoriza continuidade; contrato/schema registrados antes do Java e migration somente em arquivo. Ordem, arquivos e donos externos no [26](26-execucao-continua-backend.md); andamento oficial no [states.md](../states.md).

**Resultado atual:** P2 financeiro BE08 corrigido e freeze p2-avaria-final conferido; aceito por Farol com pareceres/evidências abaixo. CHECK V7 corrigido em arquivos, outputs anteriores preservados. Contrato31 liberado para refinamento das lacunas de Lume antes do Java BE13; nenhuma emissão ou validação externa concluída.

Cedro registrou o [contrato/schema29](29-cadastros-servicos-e-calculo.md) antes do Java. Farol o encaminhou a Prumo para V7 e leitores estáticos, a Lume para apoio de fontes/integração e a Vigia para revisão preventiva. `check` confirmou leitura real dos três; nenhum resultado de teste BE05/BE12 declarado nessa distribuição.

## Fronteiras a conferir

| Área | Evidência local exigida antes do aceite |
| --- | --- |
| Importação de endereços | Prévia com erros por linha; duplicidades no lote e no armazém; confirmação e repetição sem sobrescrever estrutura ou ocupação; rollback e alcance no serviço. Layout técnico identificado como escolha local, sem template comercial homologado presumido |
| Referências fiscais | IE/endereço e dados aplicáveis separados da identidade imutável; revisão/auditoria; registro não tratado como autorização fiscal nem validação externa do documento |
| Configuração comercial | Cliente/armazém/serviço/unidade e vigência; ausência explícita; sobreposição recusada ou resolvida por regra documentada; configuração antiga preservada; nenhum preço Tigre copiado automaticamente |
| Serviço executado | Mesmo fato operacional não gera duas cobranças por sugestão e lançamento manual com UUIDs diferentes; parcelas por nota conservam o total; liberação do gestor e correção identificada |
| Armazenagem | Calendário, início e retirada física; mesma data gera zero; remanejamento não multiplica posições-dia; separação/quarentena mantêm equivalência; duas posições e remanescente parcial explicáveis |
| Pico | Maior ocupação simultânea cobrável por dia e contexto; distinguir pico físico; passagem isenta na mesma data não aumenta o pico cobrável; fronteiras de vigência cobertas |
| Avaria | Quantidade/base histórica comprovada e instante reconhecido; suspensão proporcional por responsabilidade validada; físico/reserva conservados; cálculo ainda aberto recalculável por versão |
| Mínimo e GRIS | Complemento `max(0, mínimo - subtotal elegível)` com abrangência/proporcionalidade configuradas; GRIS exige percentual, base e periodicidade, sem inferência a partir do indicador de estoque |
| Segurança e integridade | Perfil/alcance também no replay; versões, concorrência, idempotência por conteúdo, rollback e auditoria; resposta com fatos, quantidades, tarifas/versões, parcelas e pendências |

Os exemplos fictícios e as convenções de cobrança de AC04–AC09 continuam propostas/interpretações documentadas em [11](11-alinhamentos-apos-respostas.md). A revisão de Lume em leitura orienta o desenho; não representa aprovação comercial ou teste executado.

## Revisão preventiva em leitura

06/10/2026, antes do primeiro build deste bloco: Vigia identificou sobreposição histórica de avarias que o registro físico atual soma somente enquanto abertas. Exemplo fictício: base100, ocorrência80 já reparada e outra80 retroativa antes daquele reparo. O financeiro deve conciliar o agregado por unidade/intervalo; inconsistência precisa produzir pendência explícita, sem 160%, valor negativo ou redução silenciosa para 100%. Encaminhado a Cedro com fronteiras temporais/rollback/memória a testar.

Lume conferiu fontes/contrato29 e vínculos FE04/FE11/FE12 em leitura. Duas ambiguidades encaminhadas: memória diária por regra/categoria com tarifa própria, mesmo quando a linha agregada é única por cálculo/data; GRIS DIARIA restrito aos dias do período sem segunda multiplicação por dias/ciclo. Fixar também precificação do serviço pela execução, não pelo registro tardio, e testar rateio de um centavo entre duas notas, anulação sem duplicação da mesma execução, configuração ausente/zero explícito e avaria retroativa com destino não comprovado. Prumo recebeu a possibilidade de esclarecimento estrutural; não há tabela adicional presumida para resolver esses pontos.

Esses pareceres são preventivos, não aceite final ou resultados de teste. Cedro corrige/documenta, Prumo reconcilia V7 e Vigia revisa após freeze com output real.

Vigia acrescentou quatro achados em leitura do código parcial às 00:59 de 06/10: multipart desabilitado e rotas de consulta/confirmação da importação ausentes da configuração de segurança; hyperlink em célula ainda não recusado; cabeçalho com fórmula/número capaz de produzir erro genérico 500; cidade vazia no complemento fiscal capaz de apagar o campo obrigatório do armazém. Encaminhados a Cedro para correção e testes HTTP reais, preservando validações de perfil/alcance nos serviços. Não são falhas de uma suíte executada nem parecer sobre módulo finalizado.

Cedro esclareceu formalmente o contrato29: a memória diária agrega segmentos por regra/categoria, deixando tabela/item/tarifa nulos quando não únicos; item exato PALLET/BOBINA prevalece sobre geral, enquanto múltiplos serviços de armazenagem aplicáveis geram pendência. GRIS DIARIA usa somente os dias incluídos, sem segunda proporção; POR_CICLO usa a proporção configurada. Serviço usa tarifa do instante executado. Sobreposição histórica de avarias gera reconciliação agregada por intervalo e pendência se incompatível, sem redução artificial do agregado. Implementação e testes dessas fronteiras continuam pendentes.

Nova leitura financeira de Vigia às 01:11, com classificação complementada às 01:14: cinco achados P2 encaminhados a Cedro antes do freeze. São cenários em leitura, não testes executados:

| Serviço/consulta na leitura | Achado e correção exigida |
| --- | --- |
| CalculoCobrancaService, valorEstoque | Dano sem origem identificada é distribuído proporcionalmente entre notas com preços diferentes. No exemplo fictício50 aR$10 e50 aR$100, dano20 não comprova saldoR$4.400: poderia serR$5.300 ouR$3.500. Exigir origem comprovada ou pendência de valor/GRIS |
| CalculoCobrancaService, calendário | Exclusão do dia da retirada para armazenagem também exclui estoque físico do pico de valor. Separar intervalo físico do calendário cobrável; passagem na mesma data pode ter armazenagem zero e valor físico conhecido |
| MarcoFinanceiroAvariaService | Mesmo dano pode ter remanescente0 numa baixa e20 na seguinte. Validar continuidade cronológica/quantitativa, inclusive inserção fora de ordem, sem ressuscitar quantidade retirada |
| FatoServicoService | Informar/omitir unidade opcional muda a chave da mesma execução adicional/pedido/serviço. Chave canônica deve impedir duplicação por contexto opcional ou outro UUID |
| RetiradaSaidaRepository, sugestões | Confirmar quantidade do SKU A elimina sugestão de toda a retirada e oculta SKU B ainda não lançado. Distinguir execução por produto quando a unidade tarifária exigir |

Prumo reconferiu os esclarecimentos sem mudança de SQL: [alinhamento estático](../database/evidencias/d19-v7-alinhamento-2026-10-06.md). Naquela leitura o doc29 ainda não expunha integralmente os complementos recebidos no ask; Cedro os acrescentou aos trechos de memória/GRIS/execução e continua corrigindo/testando os achados. Comparação final aguarda freeze; não há schema extra presumido.

## Registro de execução

Primeira compilação parcial BE05/BE12 de Cedro: **BUILD SUCCESS**, 16,400 s, término 00:54:46 de 06/10/2026; [output real](../backend/evidencias/d19-bloco2-compile-inicial.log). Spotless check conferiu 196 Java, compilação de 181 fontes principais. Isso comprova somente esse recorte em construção, sem execução de teste BE05/BE12 ou clean verify do bloco.

Compilações seguintes, também sem executar testes: [serviços](../backend/evidencias/d19-bloco2-compile-servicos.log), BUILD SUCCESS, 191 fontes principais, 15,855 s às 01:01:35; [cálculo](../backend/evidencias/d19-bloco2-compile-calculo.log), BUILD SUCCESS, 194 fontes, 14,933 s às 01:04:47. Vigia recebeu leitura preventiva financeira enquanto Cedro prepara a bateria; revisão final continua condicionada a código estável e outputs reais.

Primeira bateria [CobrancaIntegrationTest](../backend/evidencias/d19-bloco2-testes-inicial.log): **BUILD FAILURE**, 31 testes, zero falhas de asserção, 31 erros e zero ignorados, 1:03 min às 01:11:08 de 06/10. Todos os erros estão no preparo do fixture, que tentou limpar `wms.divergencia_recebimento`, tabela inexistente nesse schema H2. Não comprova os cenários de negócio; corrigir o preparo e repetir, preservando este log. Cedro já leu o output e está tratando a preparação.

Tentativas preservadas: [ajuste de fixture](../backend/evidencias/d19-bloco2-testes-ajuste-fixture.log), BUILD FAILURE,31/0/31/0,45,982 s às 01:12:24, ainda com a tabela inexistente; [fronteiras](../backend/evidencias/d19-bloco2-testes-fronteiras.log), BUILD FAILURE,31 testes/2 falhas/6 erros/0 ignorados,1:00 min às 01:14:12. O preparo alcançou os cenários na terceira execução; há erros de instrumentação Mockito e falhas a tratar. Nenhuma bateria BE05/BE12 aprovada ainda.

Novas execuções reais de06/10, todas preservadas fora de target:

| Output | Resultado observado |
| --- | --- |
| [Compilação após prevenção](../backend/evidencias/d19-bloco2-compile-prevencao.log) | BUILD SUCCESS,9,098 s às01:20:25; não executa testes |
| [Novos cenários](../backend/evidencias/d19-bloco2-testes-prevencao.log) | BUILD FAILURE,testCompile por import ArrayList ausente,25,450 s às01:24:16; não chegou aos testes |
| [Import corrigido](../backend/evidencias/d19-bloco2-testes-prevencao-ajuste-import.log) | BUILD FAILURE,38 testes/0 falhas/38 erros/0 ignorados,1:03 min às01:28:25; cast Object[] no preparo Mockito/AOP |
| [Instrumentação corrigida](../backend/evidencias/d19-bloco2-testes-prevencao-ajuste-spy.log) | BUILD FAILURE,39 testes/4 falhas/0 erros/0 ignorados,1:15 min às01:30:48; asserções de snapshot/avaria sobreposta/pico ainda em correção |

Cedro informou correção dos cinco achados no Java e esclarecimento do29, sem colunas/tabelas novas: intervalosValor e origens detalhadas usam o JSON existente. Farol encaminhou a Prumo; compatibilidade JPA, bateria aprovada e parecer final Vigia continuam pendentes. Esse reporte não substitui resultados de testes.

Prumo identificou divergência de tamanho: `servico_cobranca.situacao` em16 no contrato não comporta `ENCERRAMENTO_PENDENTE` (21 caracteres), enquanto o Java usa24. Cedro formalizou VARCHAR(24) no doc29 e Prumo preparou24 na V7. A preparação estática tem [output de 494/494 verificações](../database/evidencias/d19-v7-preparacao-2026-10-06.json), sem divergências, e [parecer/limites](../database/evidencias/d19-v7-preparacao-2026-10-06.md). Comparação final com JPA aguarda freeze. V1–V6 e evidências anteriores preservadas; nenhum SQL executado.

Após correções, a [bateria relevante](../backend/evidencias/d19-bloco2-testes-fronteiras-finais.log) terminou com **BUILD SUCCESS, 42 testes, zero falhas/erros/ignorados**, 1:05 min às 01:33:35 de 06/10. Inclui os cinco achados financeiros e as quatro fronteiras fiscal/Excel, sugestão por SKU, anulação/replay e instrumentação corrigida com alvo AOP em variável tipada. O [spotless final](../backend/evidencias/d19-bloco2-spotless-final.log) passou em 4,204 s às 01:34:54; o [clean verify](../backend/evidencias/d19-bloco2-clean-verify-final.log) passou com **283/0/0/0**, JAR empacotado, 2:42 min às 01:37:56.

Cedro acrescentou somente um teste de rateio literal de R$ 0,01 entre duas notas, além do caso de R$ 0,05. [Spotless centavos](../backend/evidencias/d19-bloco2-spotless-centavos.log): BUILD SUCCESS, 4,182 s às 01:38:39. [Clean verify centavos](../backend/evidencias/d19-bloco2-clean-verify-centavos.log): **BUILD SUCCESS, 284 testes, zero falhas/erros/ignorados**, JAR empacotado, 2:54 min às 01:41:40. Outputs lidos por Farol; XMLs/hash do artefato definitivo aguardam o freeze, pois outra verificação completa substituirá target.

Antes do freeze, Cedro identificou outra fronteira do XLSX: número fracionário 1,5 com formato `00` pode aparecer arredondado como `02`. Vai exigir equivalência exata entre valor bruto e apresentação, com teste de rejeição, sem schema adicional. Novos logs serão separados dos anteriores. Revisão final de Vigia e comparação JPA de Prumo aguardam a entrega explícita; os builds aprovados acima não são ainda aceite do bloco.

## Freeze parser-final e conferência de Farol

Cedro entregou o freeze explícito em 06/10: [relatório](../backend/evidencias/d19-bloco2-entrega-parser-final.txt), [resumo dos XMLs](../backend/evidencias/d19-bloco2-resumo-parser-final.json) e [manifesto de 222 fontes](../backend/evidencias/d19-bloco2-freeze-parser-final.sha256). README/doc29 foram atualizados depois do build, sem Java posterior. A proteção adicional também rejeita hyperlink em linha aparentemente vazia. Vigia e Prumo receberam a revisão final; Cedro prepara somente contrato/schema31 enquanto aguarda o aceite local.

| Comando real em backend, JDK Temurin 21.0.12.1+1 e Maven Wrapper 3.9.16 | Output final |
| --- | --- |
| `mvnw.cmd -B -ntp spotless:apply test -Dtest=CobrancaIntegrationTest` | Bateria relevante acima: 42/0/0/0, BUILD SUCCESS |
| `mvnw.cmd -B -ntp spotless:apply` | [Parser-final](../backend/evidencias/d19-bloco2-spotless-parser-final.log): BUILD SUCCESS, 210 Java, 4,193 s às 01:42:37 |
| `mvnw.cmd -B -ntp clean verify` | [Parser-final](../backend/evidencias/d19-bloco2-clean-verify-parser-final.log): **BUILD SUCCESS, 286/0/0/0**, 2:47 min às 01:45:30, JAR empacotado |
| `mvnw.cmd -v` | [Ambiente](../backend/evidencias/d19-bloco2-ambiente-parser-final.log): JDK 21.0.12.1, Maven 3.9.16, UTF-8 |

`JAVA_HOME=C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21`. Logs redirecionados por cmd para evidências fora de target. Farol leu o output e somou os **14 XMLs reais: 286 testes, zero falhas/erros/ignorados**, sendo 240 anteriores e 46 novos de CobrancaIntegrationTest. As 36 fronteiras BE07, 43 expedição e 31 estoque anteriores passaram. Não se trata de execução SQL Server: os testes usam H2 isolado e HTTP local com tokens/chaves fictícios.

Artefato desse freeze: `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`, **78.220.106 bytes**, SHA-256 `DCA2708F9A8143F4D99203ABAD662A3FECBC881020F73F2620F3A5F1AD069BFE`, conferido por Farol e coincidente com o resumo de Cedro. Será substituído pelos builds dos próximos blocos D19; o hash conserva a identificação desta entrega. Os 222 hashes do manifesto conferiram sem diferenças. V7 permanece `6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328`.

Preservação: 20 fontes anteriores de testes/migrations V1–V5 comparadas à baseline D19, sem diferenças; Git main/zero commits. application.properties recebeu os limites multipart autorizados; perfis local/SQL Server continuam em properties. Outputs 233/240/283/284 e tentativas falhas foram preservados. Os resultados de build não antecipam o parecer de Vigia, a comparação final JPA/V7 ou o aceite local.

Graphify atualizado após o código: `graphify update .` reextraiu 81 fontes, inicialmente 3.279 nós/12.921 arestas/160 comunidades. Consolidação semântica local de 20 fontes e `graphify cluster-only . --no-label` resultaram em **3.292 nós/12.957 arestas/154 comunidades, zero referências a nós inexistentes**. Conferidos os quatro serviços financeiros/parser principais e o conceito de marco de avaria em doc29:65. Nenhuma chamada de API; tokens dessa leitura não foram medidos. Rótulos das comunidades alteradas usam os hubs locais. Após os pareceres, atualizar os registros de aceite e seus vínculos no mapa.

Conferência documental de Farol nessa fase: dez arquivos centrais, 222 links locais existentes e 29 IDs BE01–BE16/FE01–FE13 preservados. Essa leitura não executa aplicação ou banco. Ainda registrar revisão, comparação e aceite, preservando as limitações externas.

## Limites e continuidade

Parecer final de Vigia em leitura às 01:59 de 06/10: cinco achados financeiros anteriores, quatro fiscal/Excel e proteções de parser corrigidos; evidências286/hashes conferidas e P2 BE07 preservado. **P2 material remanescente:** `MovimentacaoEstoqueService.java:275` marca avariaPosterior pela rota BE08 sem ocorrência detalhada; `CalculoCobrancaService.java:570/614/654` considera ocorrências/condição original, mas ignora esse marcador. Unidade originalmente BOA pode ficar avariada e gerar cálculo COMPLETO com valor integral/GRIS. Sem quantidade/período/responsabilidade comprovados, reconciliar o histórico do movimento e produzir PENDENTE, preservando bloqueio/reserva. Vigia não executou testes/build nem editou arquivos.

Farol encaminhou correção restrita a Cedro com testes HTTP antes/depois do marco e preservação da incerteza passada após eventual liberação/reparo. Não usar somente a flag atual como fotografia do histórico nem inventar quantidade/responsabilidade. Preservar build286/manifesto anterior; novos logs, spotless, clean verify e freeze para revisão das diferenças. Prumo continua corrigindo o CHECK em arquivos e reconferirá o novo freeze, sem schema adicional presumido.

Execuções P2 preservadas: [inicial](../backend/evidencias/d19-bloco2-testes-p2-avaria-inicial.log), BUILD FAILURE no testCompile por argumentos incompatíveis do helper, 17,722 s às 02:08:40; [fixture](../backend/evidencias/d19-bloco2-testes-p2-avaria-ajuste-fixture.log), BUILD FAILURE, 51/0/1/0, 1:25 min às 02:10:31, consulta do teste a coluna nota_origem_id inexistente na unidade. Não foram resultados positivos; preparo corrigido sem suprimir regra de produção.

A [bateria relevante P2](../backend/evidencias/d19-bloco2-testes-p2-avaria-relevantes.log) terminou com **BUILD SUCCESS, 162/0/0/0** às 02:13:39: 52 CobrancaIntegrationTest +31 EstoqueIntegrationTest +43 ExpedicaoIntegrationTest +36 UnidadeLogisticaIntegrationTest. Farol leu os resultados reais; formatação, clean verify completo, manifesto e revisão das diferenças ainda aguardam a entrega. Não há schema novo presumido para o P2.

Na comparação final em leitura, Prumo encontrou uma fronteira não coberta pelo H2: `ConfiguracaoCobrancaService` grava `operacao_administrativa.tipo=CRIACAO_SERVICO` e `CRIACAO_TABELA`, enquanto o CHECK preparado da V7 admite `CRIACAO`. Farol confirmou os literais de persistência e o CHECK. Encaminhamento autorizado: Cedro esclarece somente o contrato29; Prumo preserva a V7 anterior como evidência, amplia o CHECK para os tipos concretos e confere todos os tipos realmente gravados com leitor suplementar. A ação de auditoria continua `CRIACAO`; Java, conteúdo idempotente e build286 não mudam por esse ajuste em arquivos. Comparação final/aceite ainda pendentes; 494/494 da preparação anterior não comprovaram essa fronteira.

O [leitor suplementar inicial](../database/evidencias/d19-v7-2026-10-06-freeze-parser-final-suplementar.json) teve saída 1: **67 verificações, 65 atendidas**, com exatamente esses dois tipos divergentes. Output e [V7 anterior](../database/evidencias/d19-v7-2026-10-06-freeze-parser-final-V7-antes-tipos-concretos.sql) preservados. É análise de literais/DTOs/JSON em arquivos; não prova SQL emitido ou constraints executadas. Nova evidência corrigida ficará separada.

Após doc29 formalizar os13 tipos realmente gravados, Prumo corrigiu somente os dois literais do CHECK V7, preservando CRIACAO anterior e as ações da auditoria. [Parecer intermediário](../database/evidencias/d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.md), [principal](../database/evidencias/d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.json) e [suplementar](../database/evidencias/d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-suplementar.json): saídas0, **518/518 e67/67**, zero divergências, lidos por Farol. São15 tabelas/172 colunas +26 extensões, com198 anotações conferidas, sem iniciar Hibernate ou banco. Hash V7 corrigida: `3193D8436D360BA0D1D36E290112F39F76AB3A300AC2C2AD931208B6745B8940`. Os494 checks anteriores não cobriam os literais; os24 adicionais cobrem essa fronteira. Nada disso comprova execução SQL Server ou o P2 financeiro em correção. Reconferência do novo freeze continua pendente.

## Freeze p2-avaria-final — 292 testes

Cedro entregou [relatório final P2](../backend/evidencias/d19-bloco2-entrega-p2-avaria-final.txt), [resumo](../backend/evidencias/d19-bloco2-resumo-p2-avaria-final.json), [preservação](../backend/evidencias/d19-bloco2-preservacao-p2-avaria-final.json) e [manifesto](../backend/evidencias/d19-bloco2-freeze-p2-avaria-final.sha256). Backend/doc29 congelados; doc31 suspenso fora desse freeze. Não houve mudanças de models/schema nem Java posterior ao build.

| Comando real em backend, mesmo JDK21/Maven Wrapper | Resultado observado |
| --- | --- |
| `mvnw.cmd -B -ntp spotless:apply test -Dtest=CobrancaIntegrationTest,EstoqueIntegrationTest,ExpedicaoIntegrationTest,UnidadeLogisticaIntegrationTest` | [Relevante P2](../backend/evidencias/d19-bloco2-testes-p2-avaria-relevantes.log): BUILD SUCCESS,162/0/0/0,1:54 min,02:13:39 |
| `mvnw.cmd -B -ntp spotless:apply` | [Spotless P2](../backend/evidencias/d19-bloco2-spotless-p2-avaria-final.log): BUILD SUCCESS,210 Java,1,844 s,02:14:01 |
| `mvnw.cmd -B -ntp clean verify` | [Clean verify P2](../backend/evidencias/d19-bloco2-clean-verify-p2-avaria-final.log): BUILD SUCCESS,292/0/0/0,2:50 min,02:17:02; JAR empacotado |
| `mvnw.cmd -v` | [Ambiente P2](../backend/evidencias/d19-bloco2-ambiente-p2-avaria-final.log): Temurin21.0.12.1+1/Maven3.9.16 |

Farol leu o output real, somou os14 XMLs do target estável e conferiu o JAR: **78.223.834 bytes**, SHA-256 `83235E12A8CE2A0F2756B8702E1D3AF7E5ECDDCD584E64A25ED1C356AE910D5A`. Os222 hashes coincidem com o manifesto,217 iguais ao freeze286 e cinco diferenças declaradas: CalculoCobrancaService, MovimentoEstoqueRepository, CobrancaIntegrationTest, backend/README e doc29. Os históricos286 e anteriores ficam preservados; próximos builds substituem target, sem invalidar esta evidência.

Os seis novos cenários HTTP/H2 elevam a suíte financeira de46 a52: marca BE08 com reserva80/origens/replay/bloqueio; cobertura detalhada retroativa versus lacuna anterior à ocorrência; reparo seguido de novo ciclo BE08; valor físico no dia da retirada sem diária; rollback da auditoria. Movimentos imutáveis AVARIA_ESTOQUE delimitam a pendência por unidade/instante/ciclo; cobertura somente pelo AVARIA_DETALHADA posterior comprovado do mesmo ciclo. Sem prova de quantidade/período/responsabilidade, valores afetados permanecem ausentes e total PENDENTE. Reparo ou dano antigo não apagam a lacuna nem autorizam cobertura presumida. Nenhum cálculo financeiro muda estoque/reserva.

Vigia recebeu a revisão final dessas diferenças e Prumo a reconferência estática do novo freeze em06/10. `check` confirmou leitura de fontes por Vigia e execução do pedido por Prumo, após enviar somente Enter ao texto comprovadamente não submetido. Cedro apenas lê os refinamentos31/32 enquanto aguarda; nenhum teste BE13/BE14 declarado.

Parecer explícito Vigia às02:33 de06/10: **favorável ao aceite local do freeze p2-avaria-final BE05/BE12, P2 corrigido, nenhum novo achado material nas cinco diferenças**. Revisão somente em leitura, sem edição/build/testes executados por Vigia. A conferência independente de Farol e o parecer não comprovam SQL Server, regras comerciais reais, integrações fiscais, equipamentos ou piloto.

Graphify após este P2: `graphify update .` reextraiu33 fontes AST,3.404 nós/13.240 arestas. Consolidação semântica do aceite em21 fontes e `graphify cluster-only . --no-label`: **3.409 nós/13.245 arestas/166 comunidades, zero referências a nós inexistentes**. Consulta mostrou CalculoCobrancaService/MovimentoEstoqueRepository atuais; `explain` conferiu cobertura de ciclo em doc29:67. API não chamada; tokens da leitura não medidos. Conferência documental: dez centrais,252 links locais existentes e29 IDs BE/FE preservados. Mapa seguirá atualizado com os próximos blocos.

## Aceite local e próximo bloco

Prumo reconferiu o novo freeze: [principal](../database/evidencias/d19-v7-2026-10-06-p2-avaria-final.json), **518/518**, e [suplementar](../database/evidencias/d19-v7-2026-10-06-p2-avaria-final-suplementar.json), **67/67**, sem divergência. São198 colunas JPA em leitura; não houve Hibernate/SQL/banco. Hash V7 `3193D8436D360BA0D1D36E290112F39F76AB3A300AC2C2AD931208B6745B8940`; doc29 `DF9C92A474A0617CC6AD2EF2D7BB6E149DB78F23B887F64070F33802A772581C`. Prumo confirmou222 arquivos coincidentes/217 preservados e seção schema igual; somente hash da transcrição mudou. Metadados de invocação PowerShell registraram LASTEXITCODE nulo: conclusão normal com JSON integral não é prova de saída nativa0; esclarecimento preservado separadamente pela área.

Com build292, conferência independente, parecer favorável Vigia e compatibilidade estática Prumo, Farol **aceita o recorte local BE01/BE05/BE12** e libera BE13. Inativação definitiva BE05 permanece após BE13/BE14. Cedro refina31 conforme as seis lacunas contratuais de Lume registradas no32 e comunica schema antes do Java; Prumo prepara V8 somente em arquivos com essa fonte refinada. Continuar BE14/integrado, sem nova autorização por macrobloco ou parada após este aceite.

O [parecer final Prumo](../database/evidencias/d19-v7-2026-10-06-p2-avaria-final.md) registra preservação:361 arquivos capturados,356 iguais/cinco alterações documentais locais autorizadas;112 evidências históricas iguais, V1–V7 e leitores preservados. Farol leu o parecer completo, incluindo o esclarecimento LASTEXITCODE nulo e o hash do artefato. A área conserva [manifesto final](../database/evidencias/d19-v7-2026-10-06-p2-avaria-final.sha256) e antes/depois; compatibilidade estrutural não aprova regra comercial nem executa SQL.

H2 isolado não comprova o dialeto, CHECKs, índices, locks ou aplicação das migrations SQL Server. Preços, vigências comerciais, mínimo, GRIS e corte reais pertencem ao gestor/comercial; referências e enquadramento fiscal, à Natalina/Controladoria; layout/endereço/capacidade operacionais, ao Caio/gestor. Ausência desses insumos não bloqueia implementação e testes fictícios, mas impede declarar cálculo ou cobrança real homologados.

Após o aceite local deste bloco, seguir BE13 e depois BE14/inativação segura, sem nova autorização por macrobloco. Não houve frontend, conexão SQL Server real, emissão fiscal/NFS-e, cobrança a clientes, publicação, commit/push, rotina, alteração Hermes ou uso do ETL por esta preparação documental.
