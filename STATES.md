# Estado e trilha de implementação do WMS Rodogarcia

**D26 em execução — API com persistência SQL real DEV, 06/10/2026:** Lucas autorizou identidade própria WMSDEV restrita, credencial protegida, atestação, JPA validate e jornadas HTTP reais populando WMS_DEV com dados fictícios rastreáveis. Toda escrita de negócio por API; IT de banco vazio planejado antes da população. Somente DEV; nenhum reset/DELETE/schema/grant amplo/PROD/sa na aplicação. Cedro roteiro/API, Prumo acesso e persistência, Vigia revisão, Farol registros. [D26](docs/06-decisoes-e-pendencias.md) supersede exclusivamente a pendência administrativa da D25; regra permanente Hermes AGENTS:55 preservada.

**D25 — entrega local e leituras DEV concluídas, 06/10/2026:** clean verify atual com **408 testes/0 falhas/0 erros/0 skips em 20 XMLs**, JAR atual com **6/6 verificações HTTP** em loopback, processo encerrado e porta liberada. Correções de TLS cliente, critério de lock, perfil do IT e cookie de sessão local revisadas por Vigia. SQL Server real WMS_DEV por SqlClient/JDBC administrativo somente SELECT: 64 tabelas vazias/687 colunas, 1.295 itens de catálogo, nove migrations/histórico iguais à D24. **API/JPA validate e sete ITs SQL não executados: identidade própria restrita ausente.** Nenhum DDL/DML/grant/SQL PROD. Regra AGENTS.md:55 de Hermes preservada. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md) e [recibo WMS](orchestracao/.runtime/d25-resultado-final.md). BE03/BE15 continuam em validação externa; D22/D23 são históricos.

**D24 concluída no escopo SQL administrativo, 06/10/2026:** conexão compartilhada em `../.runtime/sql-server` provisionada e BAT real concluído DEV → PROD. Ambos ONLINE, 64 tabelas, 687 colunas, nove migrations e 1.295 itens de catálogo conferidos por banco. SQL Server 2022 Standard 16.0.1000.6 preservado, mesmo processo, sem reinício. [Resultado e limites](docs/39-conexao-compartilhada-e-sql-real.md); [leitura autenticada](database/evidencias/d24-resultado-real.json). Backend/piloto continuam separados. D20–D23 abaixo são históricos, superados quanto ao bloqueio de autenticação e à exigência de fonte exclusiva WMS.


**Continuação focal D23, 06/10/2026:** padrão de autenticação dos runners públicos comparado; fonte administrativa SQL compartilhada não localizada/documentada nos guias/metadados autorizados de ../.runtime. 33/33 booleanos de presença dos canais consultados ausentes; configs de referência existem sem leitura do conteúdo, DPAPI WMS ausente. O caminho fixo D22 não recebe a configuração como os demais. Menor integração: somente aquisição de credencial no launcher existente, após fonte administrativa protegida explicitamente disponibilizada ao WMS; sem novo configurador ou credencial alheia. Nenhuma conexão nova/DDL; servidor indisponível não comprovado. [Comparação e resultado](orchestracao/.runtime/d23-comparacao-fontes-autenticacao.md).

**Histórico D23 — acesso então bloqueado em 06/10/2026:** Lucas autorizou bootstrap/upgrade real DEV → PROD pelo agente, sem configurador/prompts, com identidade Windows corrente ou fonte protegida WMS existente. Probe SqlClient confirmou TLS -2146893019; pin público privado WMS verificado resolveu esse bloqueio no sqlcmd Go. SQL Server então recusou logon de ROD-SRVW-001\suporte. Credencial protegida WMS AUSENTE. Nenhuma leitura sys.databases/histórico/schema ou DDL concluída; permissões não determinadas. Única ação indispensável: disponibilizar autenticação SQL válida já autorizada em canal protegido exclusivo WMS, fora do chat, sem grants ou alterações globais. D22 abaixo é histórico local, não conclusão da execução real D23.

**Histórico D22 em 06/10/2026:** D22 BE03/BE15 concluído localmente: BAT normal automático sem senha/confirmação, DPAPI WMS exclusiva e configurador UMA VEZ. 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão Vigia favorável. Credencial real AUSENTE por metadados, provisionamento pelo operador pendente; nenhum segredo real/SQL pelo agente. [Resultado38](docs/38-launcher-automatico-e-credencial-protegida.md) e [recibo WMS](database/evidencias/d22-relatorio-final.md). D20/D21 abaixo são históricos preservados.

Atualizado em06/10/2026. **D21 BE03/BE15 concluída localmente:** BAT de bootstrap/upgrade completo WMS_DEV e WMS_PROD, DEV primeiro, pela sequência vigente do Flyway. 41/41 fixtures,672/672 checks de pacote,25/25 fixtures Maven,cmd.exe real offline0/0 e inválidos2/2;675 hashes database e84 backend conferidos, revisão Vigia favorável. V1–V9 e históricos D20 preservados. Nenhum SQL pelo agente; ensaio real será manual pelo operador. [Resultado37](docs/37-bootstrap-e-upgrade-manual-database.md) e [recibo WMS](orchestracao/.runtime/d21-resultado-final.md).

## Estado atual

| Frente | Situação comprovada |
| --- | --- |
| Documentação | PDF e Word preservados; respostas e regras consolidadas; arquitetura e cenários documentados |
| Backend | D26 em execução: jornadas HTTP autenticadas com SQL DEV, matriz de cobertura e correções proporcionais. D25 preservada: 408/0/0/0 e JAR local 6/6; esses resultados não substituem a persistência SQL/API D26. |
| Frontend | Somente orientação de estrutura; sem aplicação React/TypeScript, dependências, build ou testes executados |
| Banco | D24 executada no SQL Server real: WMS_DEV e WMS_PROD criados/migrados, nove migrations, 64 tabelas, 687 colunas e catálogo completo por banco. Credencial comum protegida; SQL Server 2022 Standard preservado. [Evidência](docs/39-conexao-compartilhada-e-sql-real.md) |
| Ambiente e piloto | Sem aplicação publicada; equipamentos, infraestrutura e recuperação ainda sem validação no WMS |
| Escopo autorizado agora | D26: provisionamento restrito WMSDEV/credencial protegida exclusiva e ensaio HTTP real populando apenas WMS_DEV com dados fictícios; atestação antes API/IT. Toda escrita de negócio via API. Matriz de requisitos/endpoints/perfis e provas GET/SQL/auditoria; lacunas explícitas. Sem PROD/reset/DELETE/DDL da aplicação/sa API/sharedruntime/servidor/terceiros/publicação/frontend. |

Base definida: React/TypeScript, Java/Spring com MVC convencional e SQL Server existente. Primeiro piloto em Osasco, depois Castro/Tigre; Caio valida a operação. A necessidade informada de iniciar em 13/10/2026 não representa estimativa técnica nem promessa de entrega.

## Onde consultar e como retomar

- [AGENTS.md](AGENTS.md): orientações de trabalho e de atualização deste arquivo.
- [Decisões](docs/06-decisoes-e-pendencias.md): escolhas do responsável e estado dos temas Q01 a Q27.
- [Respostas](docs/10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](docs/11-alinhamentos-apos-respostas.md): comportamento recebido e interpretações/propostas AC01 a AC16.
- [Arquitetura](docs/02-arquitetura.md), [fluxos](docs/04-fluxos-operacionais.md) e [cenários](docs/08-cenarios-de-validacao.md): responsabilidades, sequência operacional e verificações V01 a V48.
- [Plano geral](docs/07-plano-de-entregas.md) e [continuidade](docs/09-continuidade.md): visão da entrega e contexto resumido.

Escopo local autorizado **D19 concluído**, com evidência nos [28](docs/28-validacao-separacao-retirada-retornos.md), [30](docs/30-validacao-servicos-e-calculo.md) e [32](docs/32-validacao-fechamento-e-contingencia.md), matriz BE01–BE16 no [34](docs/34-matriz-e-validacao-final-backend.md) e modelo integrado no [35](docs/35-modelo-integrado-e-jornadas-backend.md). Base D18/197 preservada. D24 comprovou conexão e migrations; D25 executou testes novos e leitura JDBC DEV. Próximo ensaio da aplicação depende da identidade restrita, como detalhado no [40](docs/40-testes-backend-e-leitura-sql-dev.md); homologação, publicação e frontend permanecem pendentes.

**Próximo passo vigente D26:** executar o provisionamento mínimo de WMSDEV e atestar seus direitos; ensaiar sqlserver-it antes da população se precondições respeitadas, então JAR/HTTP/SQL com matriz de cobertura real. A pendência de autorização registrada na D25 foi resolvida por Lucas na D26; identidade administrativa continua separada da aplicação. D25/40 mantêm a evidência histórica.

**Histórico D20, substituído por D21 no launcher:** alvo confirmado TCP `127.0.0.1:1433`, SQL auth `sa`, conexão inicial `master`. Lucas passou à execução **manual**: `iniciar-bancos.bat` deve criar ambos os nomes ausentes, DEV primeiro, preservar/conferir existentes e permitir retomada; PROD sem migrations/cargas. Senha oculta local em memória e confirmação de identidade/alvo na interação. Nenhum SQL pelo agente; TCP alcançável/MSSQLSERVER Running não comprovam identidade SQL. Regra imutável do servidor em AGENTS/D20; guia compartilhado sem senha autorizado, sem ler `.env`/credenciais/dados/ETL nem alterar terceiros. Falta de credencial no canal do agente é observação histórica, sem impedir o launcher.

**Organização da mesma D20 entregue:**34 movimentos byte a byte,489 originais preservados, raiz database somente README curto e BAT. Scripts/docs/config nas pastas próprias; históricos, fontes e manifestos anteriores intactos; V1–V9 sem renomeação/alteração. Caminhos/BATs/links e testes conferidos após os movimentos, manifesto corrente722 hashes e revisão Vigia favorável ao layout/launcher. Sem SQL pelo agente ou novo build backend integral.

**Ressalva VIG07 do runtime:** guarda enumera recusas, não certifica mínimo privilégio geral. Antes do uso/ensaio do backend, exigir atestação completa das identidades/permissões próprias. Java/JAR396/404 são histórico D20; nenhuma atestação SQL executada. Não condiciona o bootstrap/migrations administrativo manual D21 com sa a novos grants/conta de aplicação.

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

**Status:** Em validação externa; D24 concluiu conexão, criação, migrations e catálogo real DEV/PROD. **Dependências:** BE01, BE02; faltam identidade própria/atestação do backend, operações com dados, concorrência e recuperação. Administração sa autorizada para o bootstrap.

- [x] Definir esquema WMS, ferramenta/procedimento de migrations, proteção de alvo e separação de ambientes em arquivos.
- [x] Lucas informar nomes WMS_DEV/WMS_PROD e alvo TCP127.0.0.1:1433, sa/master administrativo; execução manual definida na D20.
- [x] Conferir identidade/TLS/defaults/versão/collation reais no bootstrap administrativo D24.
- [ ] Definir e atestar identidades WMS próprias da aplicação e suas permissões efetivas.
- [x] Preparar V1–V9, vínculos, restrições, histórico e procedimentos em arquivos, sem aplicar SQL.
- [x] Entregar launcher manual, organizar database com preservação e conferir fixtures/cmd.exe/caminhos/manifesto/revisão local D20.
- [x] Verificar criação, aplicação V1–V9 e retomada do BAT no SQL Server real, com DEV antes de PROD (D24).
- [ ] Ensaiar evolução com dados, falhas de operação e recuperação por backup/restauração.
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

**Evidência D25 de BE03:** catálogo e histórico DEV reobservados por leitura SqlClient/JDBC protegida: 1.295/1.295 itens, 64 tabelas/687 colunas, 64 vazias e nove migrations iguais à D24. Nenhum DDL/DML/migration/reparo; identidade própria ausente, JPA validate não executado. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).

### BE04 Identidade, permissões e auditoria

**Status:** Em validação externa; acesso local aceito. **Dependências:** BE02, BE03.

- [x] Implementar Resource Server RS256 e validação de claims/configuração conforme contrato local.
- [ ] Validar provedor, contas, login, renovação, revogação/rotação e contenção no ambiente real.
- [x] Aplicar Gestor, Supervisor e Operação, com alcance por cliente/armazém e proteção em cada ação relevante.
- [x] Registrar usuário, momento, motivo e origem/destino das operações críticas; verificar recusas de acesso.
- [x] Implementar o recorte cadastral: JWT assinado com emissor/audiência/prazo, três perfis, alcance por cliente/armazém e auditoria atômica das alterações.

**Conclusão:** acesso indevido é recusado mesmo sem usar a tela; justificativa e autorização ficam distinguíveis; o histórico permite atribuir ações. **Base:** Q20, AC11, PR06, V16 e V21.

**Evidência D12:** contrato de identidade/auditoria no [documento 14](docs/14-cadastros-acesso-e-persistencia.md), testes RSA/escopos/recusas/rollback no documento 15. **Pendente:** provedor real, ciclo de contas/login/renovação/revogação, validação de permissões no SQL Server e integração do provedor real com as ações operacionais implementadas. **Ligação FE:** contrato para FE03; login/telas ainda não implementados. O backend não emite tokens nem possui usuários padrão.


**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Perfis/alcances/resolução antes do replay e auditoria/rollback verificados nos serviços, incluindo BE14. Ausência de provedor/política externa não foi preenchida com emissor ou blacklist inventados. **Ligação FE:** FE03; login/telas não implementados.

### BE05 Cadastros, contratos e posições

**Status:** Em validação externa; código local aceito. **Dependências:** BE03, BE04; recortes técnicos disponíveis e testados localmente, validações reais pendentes.

- [x] Implementar clientes, armazéns, endereços, produtos, embalagens/DUN, lotes/validade aplicáveis e dados fiscais de referência.
- [x] Implementar importação de endereços com conferência de erros e duplicidades, posições especiais e limites de ocupação.
- [x] Manter tabelas/serviços, unidades de cobrança, vigências e parâmetros contratuais; aplicar encerramento/inativação conforme AC12.
- [x] Entregar API de cliente/armazém/produto/embalagem/endereço, consultas paginadas, revisão, auditoria e solicitação de encerramento/reativação.

**Conclusão:** cadastros respeitam proprietário e armazém; não há posição ambígua nem alteração silenciosa de preço histórico. **Base:** RN04 a RN09, AC05, AC06, AC10 a AC12 e AC16.

**Evidência D12/D13:** cinco conjuntos de models/repositories/services/controllers/DTOs; [contratos e limites](docs/14-cadastros-acesso-e-persistencia.md),52 testes no15. **D19:** importação Excel, tabelas/serviços/valores/vigências e complemento fiscal aceitos localmente no29/30,292 testes. **Complemento final D19:** inativação definitiva com revalidação de compromissos BE13/BE14 e avisos de validade entregues/testados localmente. Flags de lote/validade/limites cadastrados; ocupação/estoque entregues em BE08. **Ligação FE:** contratos FE04, nenhuma tela/M01 entregue.

**D19 aceito localmente no recorte:** complemento fiscal, prévia/confirmar Excel e configuração comercial do [contrato29](docs/29-cadastros-servicos-e-calculo.md); clean verify292, revisão favorável Vigia e V7/JPA compatíveis, conforme [30](docs/30-validacao-servicos-e-calculo.md). Tabelas/serviços/vigências entregues; inativação complementada no fecho D19, conforme32. Referências fiscais não comprovam autorização externa.


**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Inativação definitiva dos sete tipos revalida compromissos físicos/financeiros na transação; resolução específica de Gestor não reativa cadastro nem dá disponibilidade geral. Encerramento de vigência conserva o passado. **Externos:** Caio/gestor/Natalina e dados reais.

### BE06 Recebimento, XML e divergências

**Status:** Em validação externa; código local aceito. **Dependências:** recortes de acesso/cadastros BE04/BE05 disponíveis; SQL Server em BE03 e homologação operacional ainda pendentes.

**Macrobloco autorizado em 05/10/2026:** implementar pedido/nota manual ou XML, chegadas, conferência, quarentena, correção por estorno e efetivação única da quantidade física. Registro de entrada em triagem/quarentena prepara BE07/BE08; não representa saldo disponível, endereçamento ou cobrança. Aplicar AC03 como proposta identificada.

- [x] Criar pedido manual ou por XML, validar arquivo no formato operacional suportado e associar proprietário, notas, SKUs e quantidades, sem duplicar importação.
- [x] Registrar chegadas em partes, primeira data FIFO e conferência física; preservar previsto, recebido e divergente.
- [x] Bloquear a carga conforme AC03, registrar tratativa/liberação e efetivar uma única vez a quantidade real aceita.

**Conclusão:** nota em dois dias não duplica previsão nem libera saldo incompleto; liberação tem responsável e justificativa. **Base:** RN12 a RN17, AC03, V01 a V05 e V26 a V29.

**Evidência D15:** [modelo/API](docs/18-recebimento-e-conferencia.md), seis tabelas em V2 e [86 testes totais](docs/19-validacao-recebimento-backend.md), incluindo 28 cenários de recebimento e seis de segurança XML. **Pendente de validação:** persistência/concorrência no SQL Server e documentos/cenários operacionais reais com Caio. O extrator não valida fiscalmente assinatura, XSD completo ou autorização SEFAZ. **Entregas distintas:** unitização disponível em BE07 após D16; endereçamento/disponibilidade ainda em BE08 e correções após efetivação em BE11. **Ligação FE:** contratos FE05 disponíveis, tela não implementada. M02 não concluído.

### BE07 Unidades logísticas e etiquetas

**Status:** Em validação externa; código local aceito. **Dependências:** recortes cadastrais BE05 e entradas conferidas BE06 disponíveis; SQL Server e homologação operacional pendentes.

**Macrobloco autorizado D16 em 05/10/2026:** unitização por entrada conferida, identidade permanente, divisão/reagrupamento rastreáveis e dados para etiqueta. Endereçamento e disponibilidade continuam em BE08; impressão/leitura física dependem de FE06 e equipamentos.

- [x] Unitizar quantidades conferidas com ID permanente e vínculo de origem; impedir mistura de SKU/lote/data não permitida.
- [x] Representar quantidade de produto e embalagem separadamente; manter vínculos em divisão ou reagrupamento autorizado.
- [x] Fornecer dados da etiqueta com SKU, ID, código de leitura, data, nota e quantidade; reimprimir preservando identidade.

**Conclusão:** quantidades unitizadas fecham com o recebido e etiquetas não criam saldo; mudanças de conteúdo permanecem rastreáveis. **Base:** RN10, RN11, RN15, RN16, AC10, V02 a V04, V09 e V25.

**Evidência D16:** [modelo/API e limites](docs/20-unidades-logisticas-e-etiquetas.md), V3 e [122 testes totais na entrega original](docs/21-validacao-unidades-logisticas.md), com 36 casos novos de conservação, permissões, repetição, concorrência e rollback. **Ampliação D17:** BE08 calcula disponibilidade e separa revisão da etiqueta; divisão/reagrupamento restringidos a unidades não endereçadas e sem bloqueio. **Pendente de validação:** persistência/locks no SQL Server e cenários físicos com Caio. **Ligação FE:** contratos FE06 disponíveis; layout, simbologia, impressão Tanca e leitura física ainda não implementados/validados. M02 não concluído.

### BE08 Endereçamento, movimentação e saldo

**Status:** Em validação externa; código local aceito. **Dependências:** cadastros, recebimento e unidades de BE05 a BE07 disponíveis; validações externas permanecem próprias.

**Macrobloco D17 autorizado em 05/10/2026:** capacidade física, conjuntos de duas posições, confirmação de leitura/endereço, movimentos atômicos, bloqueio/liberação e consulta de disponibilidade. Preservar AC04/AC05 como propostas e manter reserva, saída e cobrança nas etapas próprias.

- [x] Confirmar unidade/endereço, validar capacidade e ocupar conjuntamente as posições necessárias.
- [x] Centralizar alterações de estoque e localização, com proteção contra concorrência, repetição e gravação incompleta.
- [x] Expor físico, disponível e bloqueado; registrar primeiro endereço, início de armazenagem, equivalência e histórico. D18 integra reservado e avaria posterior conforme o documento 24.
- [ ] Conferir V4, locks, isolamento do saldo e permissões no SQL Server; validar capacidade real e leituras com a operação.

**Conclusão:** duas operações não ocupam indevidamente a mesma posição; remanejamento não cria entrada nem perde histórico. **Base:** RN18 a RN21, PR01 a PR07, AC04/AC05, V08, V14/V15, V23 e V30.

**Evidência D17:** [contratos e limites](docs/22-enderecamento-movimentacao-e-estoque.md), V4 preparada e [153 testes totais](docs/23-validacao-estoque-backend.md), com 31 novos de saldo, capacidade, concorrência entre clientes, quarentena, permissões, repetição e rollback. JAR local conferido e encerrado naquela entrega. **Integração D18:** saldo reservado, bloqueio por avaria posterior e lock do armazém antes de encerrar endereço; os 31 testes BE08 e os demais anteriores passaram no [build com 197 testes](docs/25-validacao-pedido-saida-e-reserva.md). **Pendente de validação:** itens externos acima. AC04/AC05 continuam propostas. **Ligação FE:** contratos FE07/FE08 disponíveis; telas/coletor não implementados. M02 não concluído.

### BE09 Pedido de saída, FIFO e reserva

**Status:** Em validação externa; código local aceito. **Dependências:** recorte local de BE04 e disponibilidade de BE08 disponíveis; provedor real e SQL Server continuam em validação própria. **Autorização:** D18.

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

**Status:** Em validação externa; D24 concluiu conexão administrativa, migrations e catálogo SQL real, incluindo reexecução do BAT. **Dependências:** BE02 a BE14 para validação integrada final; identidade própria da aplicação, recuperação e frontend para validação conjunta. **Autorização:** D19–D25 nos respectivos escopos; testes SQL D25 exclusivamente DEV.

**Preparação D19 observada:** Prumo preparou [configuração externa, diagnóstico e planos de recuperação/ensaio](docs/33-preparacao-tecnica-local-backend.md) em arquivos. O diagnóstico final real conferiu194/194 condições estáticas, incluindo as56 originais; fecho22/22,307 hashes e240 links conferidos por Farol. Vigia favorável às06:09, sem achado material no novo pacote; preparo local em arquivos aceito. Não consulta ambiente, rede ou SQL nem executa JVM/build. Testes integrados finais de367 cenários, artefato e revisão local concluídos no32; ensaios externos continuam pendentes.

- [x] Conferir regras, permissões, concorrência, repetições, falhas fiscais e cálculo nos testes locais adequados.
- [x] Validar conexão administrativa, migrations e catálogo no SQL Server existente (D24).
- [ ] Validar operações integradas e concorrência da aplicação no SQL Server com identidade própria.
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

**Status:** Planejado. **Dependências:** DOC01 a DOC04; alinhamento com BE01.

- [ ] Desenhar jornadas por perfil: cadastro, entrada, conferência, etiqueta, endereço, consulta, reserva, separação, retirada e fechamento.
- [ ] Definir campos, ações, bloqueios, mensagens e navegação; destacar quais tarefas usam coletor.
- [ ] Alinhar contratos e exemplos fictícios com BE01, incluindo erro, vazio, carregamento, conflito e indisponibilidade.

**Conclusão:** cada ação de tela tem responsável, condição e resultado de negócio; a jornada não inventa liberações ou cálculos locais. **Base:** RN30, AC01 a AC16 e FE/BE conforme as entregas seguintes.

### FE02 Base React e componentes comuns

**Status:** Planejado. **Dependências:** FE01 e contrato inicial de BE02.

- [ ] Definir ferramentas e organização interna, criar base React/TypeScript e procedimento de execução local.
- [ ] Preparar navegação, formulários, tabelas, diálogos, mensagens e comunicação centralizada com a API.
- [ ] Implementar tratamento comum de carregamento, erros, conflito e repetição de envio; preparar verificação de tipagem e construção.

**Conclusão:** estrutura é utilizável em telas administrativas e coletor, com confirmação explícita e sem dependência de dados produtivos. **Base:** D02, RN30, PR02 e T02.

### FE03 Acesso e navegação por perfil

**Status:** Planejado. **Dependências:** FE02, BE04.

- [ ] Implementar entrada/saída de sessão e apresentação das ações autorizadas para Gestor, Supervisor e Operação.
- [ ] Exibir contexto de cliente/armazém e tratar sessão expirada, acesso recusado e troca de contexto sem reutilizar dados indevidos.
- [ ] Disponibilizar gestão de usuários e atribuições conforme o contrato aprovado em BE04.

**Conclusão:** interface e API concordam quanto ao acesso; ocultar botão não é a única proteção. **Base:** Q20, AC11 e V16.

### FE04 Cadastros e importação de endereços

**Status:** Planejado. **Dependências:** FE03, BE05.

- [ ] Criar telas de cliente, armazém, posição, produto, embalagem/DUN e informações fiscais necessárias ao fluxo.
- [ ] Permitir conferência da importação de endereços, com erros identificáveis e resultado sem duplicidade.
- [ ] Apresentar tabelas/serviços, vigências e parâmetros; mostrar impedimentos de inativação/encerramento e histórico pertinente.

**Conclusão:** usuário distingue campos obrigatórios, erros e efeito das alterações; valores reais não são preenchidos com exemplos de outro cliente. **Base:** RN04 a RN09 e AC05/AC06/AC10/AC12.

### FE05 Entrada, conferência e quarentena

**Status:** Planejado. **Dependências:** FE04, BE06.

- [ ] Criar lista/detalhe e pedido manual/XML, com notas, itens e chegadas em partes.
- [ ] Registrar contagem, falta, sobra, avaria e tratativa, mostrando diferença entre previsto e recebido.
- [ ] Exibir bloqueio da carga e permitir liberação somente ao perfil autorizado, com confirmação do servidor.

**Conclusão:** operador entende o que está em conferência, bloqueado ou liberado; importar XML não aparece como recebimento físico concluído. **Base:** AC03, V01 e V26 a V29.

**Backend disponível após D15:** rotas, DTOs, permissões, estados e regras de repetição no [documento 18](docs/18-recebimento-e-conferencia.md). FE05 permanece planejada, sem aplicação/tela ou teste de integração frontend.

### FE06 Unitização, etiquetas e impressão

**Status:** Planejado. **Dependências:** FE05, BE07.

- [ ] Distribuir quantidades em unidades, mostrar identidade/origem e apresentar divergências da soma.
- [ ] Exibir e imprimir etiqueta no formato definido, com SKU, ID, data, nota, quantidade e código legível, sem endereço fixo.
- [ ] Reimprimir sem criar unidade e substituir etiqueta de quantidade desatualizada após retirada parcial.

**Conclusão:** etiqueta e registro correspondem; impressão/leitura são verificadas nos equipamentos disponibilizados por Mickael antes do piloto. **Base:** RN10/RN11/RN16, AC10, V02 a V04, V09 e V43.

**Backend disponível após D16:** contratos de unitização, divisão/reagrupamento, progresso, leitura por UUID e dados atuais da etiqueta no [documento 20](docs/20-unidades-logisticas-e-etiquetas.md). FE06 permanece planejada: não há tela, layout de impressão, PDF/ZPL ou envio à impressora. Reimpressão consulta a identidade existente; frontend deve substituir etiqueta cujo conteúdo mudou.

### FE07 Coletor para endereçamento e movimentação

**Status:** Planejado. **Dependências:** FE03, FE06, BE08.

**Contrato disponível D17:** capacidade, conjuntos, leitura/posição, bloqueio/liberação e repetição em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md). Sem tela implementada ou leitura no coletor real.

- [ ] Criar sequência simples de leitura da unidade e posição, com foco adequado e ações acessíveis.
- [ ] Mostrar armazém, destino, limites, posições necessárias e recusa de leitura/capacidade incompatível.
- [ ] Aguardar confirmação do servidor antes de exibir sucesso; tratar leitura repetida, perda de resposta e tentativa concorrente.

**Conclusão:** operador identifica o movimento confirmado e consegue corrigir a leitura sem duplicar operação; verificar no coletor real. **Base:** RN18/RN30, AC05, V08, V14/V15, V23 e V30.

### FE08 Estoque, filtros e rastreabilidade

**Status:** Planejado. **Dependências:** FE03, BE08 e contratos de consulta de BE14.

**Contrato disponível D17/D18:** lista, saldo por produto e histórico em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md); reservado e avaria posterior complementados no [documento 24](docs/24-pedido-saida-fifo-e-reserva.md). Indicadores financeiros seguem suas etapas; nenhuma tela entregue.

- [ ] Exibir saldo físico, disponível, reservado, quarentena e avaria, com filtros por cliente, SKU, unidade, armazém, endereço e situação.
- [ ] Mostrar origem, localização, movimentos e datas FIFO/chegada/armazenagem sem confundi-las.
- [ ] Apresentar indicadores essenciais e aviso de validade quando aplicável, sem tratar erro de consulta como estoque vazio.

**Conclusão:** quantidades e condições reconciliam com o backend e respeitam o alcance do usuário. **Base:** RN20/RN28, Q01/Q18/Q21, V07, V17, V37 e V38.

### FE09 Saída, FIFO, reserva e separação

**Status:** Planejado. **Dependências:** FE06, FE08, BE09 e etapa de separação de BE10.

**Contrato disponível D18:** criação, consulta, sugestão FIFO, justificativa/autorização, reserva, reversão/cancelamento e revalidação em [documento24](docs/24-pedido-saida-fifo-e-reserva.md). **Complemento D19:** leitura/separação BE10 no [documento27](docs/27-separacao-retirada-retornos-e-avaria.md), aceito localmente com testes/build e revisão após correção temporal no [28](docs/28-validacao-separacao-retirada-retornos.md). Frontend não iniciado.

- [ ] Criar pedido, mostrar sugestão FIFO e confirmar reserva; permitir justificativa/autorização de exceção por perfil.
- [ ] Exibir reserva sem vencimento, pendência fiscal, indisponibilidade concorrente e avaria posterior.
- [ ] Conferir leitura para separação, incluindo parcial de pallet com pedido integral e etiqueta correta do remanescente.

**Conclusão:** a interface não promete saldo antes da confirmação nem conclui parte do pedido; erros de concorrência são compreensíveis. **Base:** AC10/AC11, V06/V07, V13/V17, V24 e V31/V32.

### FE10 Fiscal, retirada, cancelamento e devolução

**Status:** Planejado. **Dependências:** FE09, BE10, BE11.

- [ ] Vincular/exibir documentos, sua cobertura do pedido e pendências do NOTAZZ no procedimento inicial.
- [ ] Separar visualmente nota emitida de retirada confirmada e limitar a conclusão a Supervisor/Gestor.
- [ ] Guiar cancelamento, retorno à posição, devolução por nova entrada e avaria, mostrando consequências e serviços preservados.

**Conclusão:** operador sabe o que falta para retirar e o que um cancelamento desfaz; documento simbólico não é mostrado como expedição física. **Base:** AC01/AC02/AC08, V10/V11, V33/V34 e V39.

### FE11 Serviços, armazenagem e fechamento

**Status:** Planejado. **Dependências:** FE04, FE10, BE12, BE13.

- [ ] Registrar serviços, quantidades e origem; disponibilizar liberação financeira ao gestor sem duplicar lançamento.
- [ ] Exibir memória de diárias, posições equivalentes, vigência, mínimo, avaria e ajustes, distinguindo ocupação física de cobrança.
- [ ] Conferir/aprovar o ciclo inteiro, reabrir quando permitido e apresentar demonstrativo para ESL e referência da NFS-e.

**Conclusão:** cada valor é explicável e a interface exibe cálculo do servidor; exemplo documental não aparece como contrato real confirmado. **Base:** AC04 a AC09, V12, V19/V20, V35/V36, V40 e V45 a V48.

### FE12 Contagem, contingência e relatórios

**Status:** Planejado. **Dependências:** FE08, FE11, BE14.

- [ ] Apoiar contagem simples e eventual carga inicial, com diferença, justificativa e aprovação do ajuste.
- [ ] Apoiar lançamento/conferência dos registros de contingência, identificando conciliados e conflitos sem criar sincronização automática sem rede.
- [ ] Disponibilizar consultas e relatórios essenciais de estoque, ocupação, movimentos, valor, pedidos e cobrança com o mesmo alcance de acesso.

**Conclusão:** registros podem ser conferidos pelo supervisor/gestor; diferença ou falha não desaparece da tela nem altera saldo sem confirmação. **Base:** AC13 a AC15, V21, V37 e V44.

### FE13 Validação das jornadas e preparação do piloto

**Status:** Planejado. **Dependências:** FE02 a FE12; BE15 para fechamento conjunto.

- [ ] Verificar tipagem, comportamento, contratos e construção; validar teclado, legibilidade, tamanhos de tela e feedback de erro/sucesso.
- [ ] Executar percurso completo com API, impressora/coletor e dados de validação, registrando falhas de conexão e repetição.
- [ ] Preparar artefato frontend, instruções para a operação e evidências da versão candidata, distinguindo simulação de teste real.

**Conclusão:** jornadas funcionam com o backend e nos dispositivos do piloto; versão construída e versão publicada possuem registros separados. **Base:** RN30, AC14 a AC16 e V01 a V48 conforme a jornada.

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
| Banco/esquema WMS, acesso e ambientes SQL Server | Lucas/TI | BE03, BE15 | D24 validou conexão administrativa e schemas DEV/PROD; identidade da aplicação e recuperação pendentes |
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
