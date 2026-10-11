# Documentação do WMS Rodogarcia

**QUAL-CONF01-SQL01 — estratégia vigente e execução SQL real:** [regra permanente SQL Server/WMS_DEV](06-decisoes-e-pendencias.md#qual-conf01-sql01--testes-reais-sql-server-10102026), [estado](../states.md#qual-conf01-sql01--testes-reais-sql-server-10102026) e [procedimento](19-desenvolvimento-integrado-dev.md#política-vigente-de-testes--qual-conf01-sql01). H2 anterior é histórico; testes puros sem banco ficam separados. Guarda atual e dados preservados antes de integração, sem selecionar testes destrutivos/banco vazio.


[QUAL-CONF01-PERF01-DEC01 — decisão vigente de processamento adequado no banco](06-decisoes-e-pendencias.md#qual-conf01-perf01-dec01--processamento-adequado-no-banco-10102026): arquitetura por camadas permite Java → SQL com ganho e equivalência demonstrados; sem autorização adicional para intervenção real.

**QUAL-CONF01 — rodada atual de qualidade e confiabilidade:** [andamento e critérios](../STATES.md#qual-conf01--qualidade-e-confiabilidade-10102026), [escopo autorizado](06-decisoes-e-pendencias.md#qual-conf01--qualidade-e-confiabilidade-10102026) e [continuidade](09-continuidade.md#qual-conf01--rodada-atual-10102026). Evidências novas e saídas isoladas em `orchestracao/.runtime/qual-conf01/`; baseline, correções e limites reais registrados separadamente.

**QUAL-CONF01-PERF01 — complemento material em andamento:** [Início, dashboard/visão/contexto e segurança](../STATES.md#qual-conf01-perf01--início-desempenho-e-segurança), [autorização/limites](06-decisoes-e-pendencias.md#qual-conf01-perf01--inicio-desempenho-e-seguranca). Bloco anterior é baseline local parcial; novas provas em `orchestracao/.runtime/qual-conf01/perf01/`, com custos locais separados das medidas do portal.

**QUAL-CONF01-PERF01-CARGA01 — implementação local validada e revisada, fecho geral parcial:** [otimização ponta a ponta de carregamento](../STATES.md#qual-conf01-perf01-carga01--implementação-do-carregamento-10102026), [autorização e limites](06-decisoes-e-pendencias.md#qual-conf01-perf01-carga01--implementação-do-carregamento-10102026); lote/SQL adequado e carregamento progressivo com ganho demonstrado, preservando INIT01 e Information.

**QUAL-CONF01-PERF01-INIT01 — verificação local datada:** [carregamento das telas sem visitar Início](../STATES.md#qual-conf01-perf01-init01--independência-de-início-10102026), com rotas/catálogos/contexto/auth.fresh e ordem HTTP isolada; sem preparador adicional ou reinício.

**FE05-PED01:** [Pedidos de entrada, notas, conferência e histórico no mesmo contexto](design-system/etapa-07.md).

**FE02-REG01:** [listas, detalhes e ações por registro nas 12 áreas](design-system/etapa-06.md), [inventário e mapeamento das 50 páginas](design-system/inventario-paginas.md).

**Design system:** [fonte visual](design-system/design.md), [fundação, temas e navegação](design-system/etapa-01.md), [login, topo e rodapé](design-system/etapa-02.md), [organização das páginas internas](design-system/etapa-03.md) e [gráficos do Início (DASH01)](design-system/etapa-04.md) e [seleção por nome e visão geral atual (DASH02)](design-system/etapa-05.md).

**D32 — login e administração de usuários:** [uso, segurança, contratos e ativação DEV](43-login-e-administracao-de-usuarios.md). Conta principal protegida, administradores delegados e senha temporária obrigatória; implantação SQL Server separada da validação local.

**PROD-LAUNCH01:** [launcher Windows, portas e publicação futura](44-launcher-producao-e-publicacao-futura.md). Build isolado e testes locais separados da subida operacional WMS_PROD; Cloudflare/DNS não configurados.

**BE06-XML01 / FE05-XML01:** [novo pedido manual ou por XML](45-pedido-entrada-manual-xml.md), com prévia sem escrita, associação por proprietário, confirmação íntegra e origem/documento original. D33 confirma que o emitente continua proprietário em todas as entradas.


Esta documentação reúne o contexto necessário para continuar o projeto sem depender do histórico da conversa. Foi iniciada em 03/10/2026, durante a fase de análise de negócio e arquitetura.



## Como interpretar os registros



| Classificação | Significado |

| --- | --- |

| Definido pelo responsável | Decisão expressa do usuário responsável pelo projeto |

| Especificado no PDF | Requisito presente na especificação funcional recebida |

| Informado nas respostas | Informação do questionário recebido em 05/10/2026; conflitos são conciliados de forma explícita |

| Interpretação | Conclusão deduzida do texto, identificada como tal |

| Proposta | Recomendação de desenho ou proteção ainda sem aprovação expressa |

| Pendente | Questão que a documentação disponível não resolve |



Uma proposta não se torna regra aprovada por estar escrita aqui. O silêncio do gestor ou do responsável também não representa aprovação.



## Documentos



| Documento | Conteúdo |

| --- | --- |

| [Orientações do projeto](../AGENTS.md) | Regras de trabalho, arquitetura e manutenção do estado |

| [Trilha de implementação](../states.md) | Etapas backend e frontend, dependências, critérios de conclusão, evidências e próximo passo |

| [Contexto e escopo](01-contexto-e-escopo.md) | Objetivo, limites, áreas funcionais, termos e indicadores |

| [Arquitetura](02-arquitetura.md) | Tecnologias definidas, camadas convencionais e propostas de organização |

| [Regras de negócio](03-regras-de-negocio.md) | Requisitos rastreados até o PDF e proteções propostas |

| [Fluxos operacionais](04-fluxos-operacionais.md) | Recebimento, localização, reserva, saída e cobrança |

| [Questionário original](05-perguntas-para-o-gestor.md) | Histórico dos 27 temas e 110 subitens já respondidos no Word |

| [Decisões e pendências](06-decisoes-e-pendencias.md) | Estado das decisões, dependências e registro de alterações |

| [Plano de entregas](07-plano-de-entregas.md) | Sequência proposta e condições para avançar |

| [Cenários de validação](08-cenarios-de-validacao.md) | Exemplos para conferir regras e proteções antes do piloto |

| [Continuidade](09-continuidade.md) | Ponto atual, orientações e próximas atividades |

| [Respostas recebidas](10-respostas-recebidas-2026-10-05.md) | Regras informadas no Word e complementos das imagens |

| [Regras consolidadas](11-alinhamentos-apos-respostas.md) | Soluções por raciocínio, exemplos de cálculo e dados de implantação |

| [Base e contratos backend](12-base-e-contratos-backend.md) | Escolhas de BE02, contrato HTTP inicial e limites do recorte de BE01 |

| [Validação da base backend](13-validacao-base-backend.md) | Build, 12 testes, execução local do JAR e verificações ainda futuras |

| [Cadastros, acesso e persistência](14-cadastros-acesso-e-persistencia.md) | Modelo/API dos cinco cadastros, permissões JWT, auditoria e recortes de BE01/BE03/BE04/BE05 |

| [Validação do bloco cadastral](15-validacao-cadastros-backend.md) | Build, 52 testes, HTTP/JPA/H2, assinatura JWT, concorrência e limites da validação local |

| [Referências](referencias/README.md) | Origem e integridade do PDF e do questionário respondido |

| [Padrões de engenharia backend](16-padroes-de-engenharia-backend.md) | Properties, camadas, validação dos serviços e verificações automáticas de BE16 |

| [Orquestração Maestri e Hermes próprio](17-orquestracao-maestri-hermes.md) | Equipe WMS, divisão de arquivos, Hermes com memória/sessões próprias, separação do ETL e ativação |

| [Recebimento e conferência](18-recebimento-e-conferencia.md) | Pedido, notas manual/XML, chegadas, quarentena, estorno, efetivação e contratos FE05 |

| [Validação do recebimento](19-validacao-recebimento-backend.md) | Build com 86 testes, concorrência, segurança XML, rollback, JAR e limites do ambiente local |

| [Unidades logísticas e etiquetas](20-unidades-logisticas-e-etiquetas.md) | Unitização, identidade, composição, divisão/reagrupamento, repetição e dados para FE06 |

| [Validação das unidades](21-validacao-unidades-logisticas.md) | Build com 122 testes, conservação de quantidades/origens, permissões, concorrência e rollback; limites de SQL Server/equipamentos |

| [Endereçamento, movimentação e estoque](22-enderecamento-movimentacao-e-estoque.md) | Capacidade, conjuntos de posições, bloqueios, saldo, histórico e contratos FE07/FE08; propostas AC04/AC05 identificadas |

| [Validação do estoque](23-validacao-estoque-backend.md) | Build com 153 testes, disputa por posições, repetição, permissões e rollback; V4 e ambiente real ainda pendentes |

| [Pedido de saída, FIFO e reserva](24-pedido-saida-fifo-e-reserva.md) | Modelo/API BE09, pedido integral, parcial de pallet, exceção por perfil, saldo reservado, avaria posterior e contratos FE08/FE09 |

| [Validação da saída e reserva](25-validacao-pedido-saida-e-reserva.md) | Formatação e build com 197 testes, 44 novos, revisão WMS, V5 preparada e limites H2/SQL Server |

| [Execução contínua do backend](26-execucao-continua-backend.md) | Autorização D19, ordem do escopo local restante BE01–BE16, arquivos exclusivos, critérios e donos externos |

| [Separação, retirada, retornos e avaria](27-separacao-retirada-retornos-e-avaria.md) | Contrato/schema BE10/BE11 entregue localmente; fiscal separado do físico e fatos para cálculo |

| [Validação de separação, retirada e retornos](28-validacao-separacao-retirada-retornos.md) | Aceite local D19: 240 testes, revisão após P2, V6 preparada, preservação e limites externos |

| [Cadastros, serviços e memória de cálculo](29-cadastros-servicos-e-calculo.md) | Contrato BE05/BE12 aceito localmente: importação Excel, referências fiscais, serviços/vigências e cálculo; extensão histórica compatível BE13 |

| [Validação de cadastros, serviços e cálculo](30-validacao-servicos-e-calculo.md) | Aceite local do recorte BE05/BE12 após P2:292 testes, Vigia favorável e V7/JPA compatíveis em arquivos; inativação definitiva complementada no31/32 |

| [Fechamento, contagem, contingência e encerramento](31-fechamento-contagem-e-contingencia.md) | BE13 aceito localmente após três P2; BE14/inativação aceitos localmente após P2-locks, com schema antes do respectivo Java |

| [Validação de fechamento e contingência](32-validacao-fechamento-e-contingencia.md) | BE13 aceito localmente:333 testes,15 XMLs/JAR/251 hashes, Vigia favorável/V8-JPA523/523 e163/163; fecho BE14/integrado367/0/0/0 após P2-locks e revisão favorável |

| [Preparação técnica local](33-preparacao-tecnica-local-backend.md) | Configuração externa, permissões e planos SQL/recuperação em arquivos; não comprova execução real |

| [Matriz e validação integrada do backend](34-matriz-e-validacao-final-backend.md) | Matriz BE01–BE16 consolidada no fecho local D19; separa código local, testes observados e pendências externas |

| [Modelo integrado e jornadas backend](35-modelo-integrado-e-jornadas-backend.md) | Contratos BE01 integrados e jornada HTTP verificada no fecho local367; jornadas frontend futuras |

| [Database local: engenharia e validação](36-database-local-engenharia-e-validacao.md) | Macrobloco D20 BE03/BE15: auditoria V1–V9, integração, melhorias locais e evidências separadas do ensaio SQL Server |

| [Bootstrap e upgrade manual da database](37-bootstrap-e-upgrade-manual-database.md) | D21: BAT único, Flyway dinâmico, DEV antes de PROD, testes locais e limites do ensaio real |

| [Launcher automático e credencial protegida](38-launcher-automatico-e-credencial-protegida.md) | D22: incremento D21, DPAPI exclusiva WMS, configuração UMA VEZ e execução normal sem perguntas |



| [Conexão compartilhada e SQL real](39-conexao-compartilhada-e-sql-real.md) | D24: bootstrap/upgrade reais, catálogo/histórico e reexecução; sa administrativo separado da aplicação |

| [Testes backend e leitura SQL DEV](40-testes-backend-e-leitura-sql-dev.md) | D25: build408, JAR6/6, correções locais e leitura JDBC DEV; aplicação SQL condicionada à identidade restrita |

| [Ensaio HTTP e persistência DEV](41-ensaio-http-e-persistencia-dev.md) | D26: identidade própria, build412/IT7, jornadas reais159/160, matrizes RN/AC, provas SQL e ajuste bloqueado por schema |
| [Validação técnica API/SQL DEV](42-validacao-tecnica-api-sql-dev.md) | D27 técnica encerrada: V10 somente DEV, RN22/XML e ajustes corrigidos, build420,726 HTTP novos/persistência e witness SQL online; matriz/limites/revisão/recibo WMS, D26 preservada |


## Atualização da documentação



Quando houver uma resposta ou decisão, registrar sua data, origem e consequência em `06-decisoes-e-pendencias.md`. Em seguida, atualizar os documentos afetados e o contexto de continuidade.



O andamento da implementação fica em `../states.md`. As etapas BE01 a BE16 e FE01 a FE13 mantêm status e critérios próprios; este índice, o plano geral e a continuidade apenas apontam para a trilha, sem manter listas concorrentes de tarefas.



Os temas Q01 a Q27 devem manter seus identificadores; registrar respostas por número e letra, como Q02.a. Os temas Q01 a Q20 preservam a numeração inicial. Se uma resposta contradizer o PDF, registrar o conflito antes de substituir a regra. Uma nova versão da especificação deve ser preservada como nova referência.



Não copiar senhas, certificados, chaves de acesso ou dados reais de produção para os documentos.



## Ponto atual em 07 de outubro

**D30 vigente, nova demanda expressa de Lucas após o fecho localD29:** verificação integral da parcela local backend/documentação/contratos para frontend futuro. [Escopo/dez critérios](../orchestracao/.runtime/d30-farol-escopo-e-distribuicao.json), [adenda de fontes/donos e HTTP+H2](../orchestracao/.runtime/d30-farol-adenda-donos-fontes.json) e [matriz de aceite em andamento](../orchestracao/.runtime/d30-matriz-aceite-local.json). HTTPLOCAL+H2 efêmero isolado e fictício autorizado após comprovar URL/perfis/configuração por suíte; nenhuma conexãoSQLServer/HTTPDEV/escritaBancoReal ou intervenção de servidor. H2 não provaSQLServer. Frontend não iniciado; somente contratos para futura demanda. Aprovação local integral exige zero critério local pendente e revisão independente de todo pacote. RegistrosD29 abaixo são históricos e conservam seus limites; não autorizam novas sondas.

**D29 em execução por autorização expressa de Lucas:** cobertura documental e técnica ampliada, com novas famílias fictícias somente WMS_DEV/WMSDEV e escritas de negócio pela API. [Distribuição e limites](../orchestracao/.runtime/d29-autorizacao-e-distribuicao.md), [matriz central em ampliação](../orchestracao/.runtime/d29-matriz-requisitos-farol.json) e [preflight novo](../orchestracao/.runtime/d29-prumo-preflight.json). A matriz confronta RN/I/PR, cenários V, propostas AC, respostas e cláusulas dos contratos; inventário ou HTTP200 não constituem aceite. Frontend, dispositivos, fiscal externo, aceitação comercial e recuperação em destino isolado permanecem separados. Andamento e critérios oficiais no states.md.

**D28 encerrada com limites:** [recibo Farol](../orchestracao/.runtime/d28-resultado-final.json), build novo420/0/0/0, jornadas HTTP/API e17 fotografias SQL contemporâneas, revisão independente de29 critérios. Sem nova correção de negócio confirmada; reds de roteiro/fixture/oráculo preservados. Witness nativo SQL não comprovado pela identidade restrita; alternativa HTTP/JVM/SELECT posterior explicitada. D28 não homologa todo o backend nem atende as novas lacunas D29 por herança.

**D27 técnica encerrada no [42](42-validacao-tecnica-api-sql-dev.md):** ajustes, RN22/XML e financeiro fictício nos casos documentados; V10 aplicada somente DEV na autorização histórica própria. V1–V10/checksums/históricos preservados e imutáveis. A D29 não autoriza migrations/DDL nem launcher DEV→PROD.

## Histórico até 06 de outubro


**D26, ensaios encerrados no [41](41-ensaio-http-e-persistencia-dev.md):** identidade própria WMSDEV, build412/IT7 e API real159/160;1.404 solicitações com recusas/tentativas preservadas. SQL236/0+GET17/0+extra76/0, duas causas locais corrigidas. Ajuste físico bloqueado por CHECK, V10 só proposta; snapshot20 não aprovado por anulação/desatualização.32 JARs encerrados; matrizes e lacunas, sem backend total aprovado/frontend/PROD.



**Histórico D25 no [40](40-testes-backend-e-leitura-sql-dev.md):** 408 testes finais e JAR6/6; JDBC administrativo de leitura WMS_DEV/catálogo/vazio comprovados. API/JPA/IT SQL então aguardavam identidade própria restrita, supersedida pelo provisionamento autorizado D26; regra DEV permanente preservada.



**D24 concluída no [39](39-conexao-compartilhada-e-sql-real.md):** conexão compartilhada e execução real DEV/PROD pelo BAT, com nove migrations e catálogos completos. SQL Server 2022 Standard preservado. Fonte protegida já provisionada. Registros D20–D23 abaixo são históricos; aplicação e homologação continuam separadas.



**Histórico D22 concluído localmente no [38](38-launcher-automatico-e-credencial-protegida.md):** execução normal automática sem senha/confirmação, credencial DPAPI WMS exclusiva e [configurador UMA VEZ](../database/configurar-credencial.bat). 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; Vigia favorável. Credencial real AUSENTE, provisionamento do operador pendente; SQL/TLS reais não executados pelo agente. Fluxo completo D21 e seus históricos preservados. [Recibo WMS](../database/evidencias/d22-relatorio-final.md). D21/D20 abaixo são históricos.



**D21 concluída localmente no [37](37-bootstrap-e-upgrade-manual-database.md):** [BAT único](../database/iniciar-bancos.bat) e [README curto](../database/README.md), bootstrap/upgrade completo DEV depois PROD, fonte Flyway dinâmica.41/41 fixtures,672/672 checks,25/25 Maven,675/84 hashes e revisão favorável; SQL real não executado. Interação anterior de senha/confirmação substituída por D22: configurar UMA VEZ e executar normalmente sem perguntas. D20 abaixo é histórico; a entrega atual inclui schema e pendentes também PROD pelo operador.



Começar por AGENTS e states; consultar respostas/regras conforme a tarefa, sem reenviar o questionário. D19 preservada nos28/30/32/34/35. **D20 database BE03/BE15 concluída localmente no36:** [BAT](../database/iniciar-bancos.bat) pronto/testado e [README curto](../database/README.md), raiz organizada,34 movimentos/489 originais e722 hashes preservados/conferidos;80/21/13/336 checks aprovados e backend396/19/404. Vigia favorável ao launcher/organização; VIG07 exige atestação completa dos privilégios antes de uso do runtime/backend, sem certificação pela guarda parcial. **Próximo passo histórico D20, substituído por D21:** Lucas abriria o BAT, fornecerá senha oculta no console e confirmará127.0.0.1:1433; cria apenas WMS_DEV/WMS_PROD ausentes, DEV primeiro, preservando existentes. Nenhum SQL pelo agente. Migrations DEV separadas, PROD sem migrations/cargas. [Guia compartilhado sem senha](../../.runtime/SQL-SERVER-EXISTENTE.md). Validação SQL Server/TLS/identidades/recuperação, frontend, fiscal/comercial, equipamentos e homologação/publicação/piloto continuam separados. Pacote local encerrado.
