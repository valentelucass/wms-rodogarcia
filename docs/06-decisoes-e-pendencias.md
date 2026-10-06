# Decisões e pendências do WMS Rodogarcia

Registro iniciado em 03/10/2026. As decisões do responsável, os requisitos da especificação e as propostas de análise têm origens diferentes. Em 05/10/2026 foi recebido o questionário com respostas aos 110 subitens. As interpretações decorrentes estão identificadas e permitem avançar no desenho sem nova rodada extensa de perguntas.

## Decisões do responsável pelo projeto

| ID | Data | Decisão | Consequência |
| --- | --- | --- | --- |
| D01 | 03/10/2026 | Discutir regras de negócio e arquitetura em linguagem simples, antes de código. | Orientou a fase documental inicial; D11 posteriormente autorizou começar o backend. |
| D02 | 03/10/2026 | Frontend React com TypeScript. | Registrar essa base e preparar a implementação futura. |
| D03 | 03/10/2026 | Backend Java com Spring. | Manter o ecossistema escolhido. |
| D04 | 03/10/2026 | Banco SQL Server já disponível na empresa. | Levantar ambiente e acesso; não substituir o banco escolhido. |
| D05 | 03/10/2026 | MVC convencional com repository, models, DTOs e demais camadas usuais. | Organizar o backend por camadas e separar responsabilidades dos serviços. |
| D06 | 03/10/2026 | Analisar o PDF antes de perguntar; o responsável não saberá responder às regras operacionais. | Consolidar lacunas reais em linguagem simples para o gestor. |
| D07 | 03/10/2026 | Criar o projeto em `C:\Users\suporte\Documents\projetos` e documentar em `docs`. | Criada a pasta `wms-rodogarcia`, nome derivado da especificação. |
| D08 | 03/10/2026 | Trazer todas as lacunas identificadas em perguntas fáceis de responder pelo gestor. | Ampliado o roteiro para 27 temas com 110 subitens, preservando Q01 a Q20 e acrescentando Q21 a Q27. Nenhuma regra de negócio nova foi aprovada por essa revisão. |
| D09 | 05/10/2026 | Usar as informações recebidas e o raciocínio para resolver as pontas restantes. | Formular uma base de trabalho para as ambiguidades. Distinguir resposta literal, interpretação e proposta; não exigir nova resposta para cada detalhe nem inventar dados comerciais. |
| D10 | 05/10/2026 | Consultar AGENTS.md e states.md dos outros projetos e montar os arquivos do WMS, com trilha separada em backend e frontend. | AGENTS.md atualizado e states.md criado na raiz; 15 etapas BE e 13 FE com dependências e critérios. Pedido documental, sem início de implementação. |
| D11 | 05/10/2026 | Ler AGENTS.md e states.md para iniciar o backend, mantendo MVC padrão com repository, models e demais camadas, e escolher uma frente. | Escolhida BE02: base executável local, com contrato técnico inicial de BE01. Autoriza código nesse escopo; não autoriza conexão ao ambiente real, migração ou publicação. |
| D12 | 05/10/2026 | Fazer um bloco maior do backend. | Ampliado para cliente, armazém, produto, embalagem e endereço, com JPA, API, permissões JWT, auditoria e V1 SQL Server preparada. Recortes de BE01/BE03/BE04/BE05; testes locais com H2 e identidade efêmera. Nenhuma conexão/migration no SQL Server ou publicação. |
| D13 | 05/10/2026 | Usar `.properties` em resources e seguir boas práticas de engenharia de software. | BE16 acrescenta padronização de configuração, verificação de ferramentas/formato/camadas e validação nas entradas dos serviços. Convenções e escolhas técnicas no documento 16; permanece MVC convencional. |
| D14 | 05/10/2026 | Preparar no Maestri aberto uma orquestração WMS no mesmo canvas, ao lado do ETL v2, com Hermes próprio; preservar os terminais existentes e não conectar projetos. | Hermes WMS com perfil, memória e sessões próprios, mais cinco agentes WMS com papéis exclusivos. Conexões e nota somente da equipe WMS. Preparação/ativação em OR01 e documento 17; não inicia novo bloco da aplicação nem reconfigura o gateway, a ponte ou o perfil antigo. |
| D15 | 05/10/2026 | Aplicar outro macrobloco do backend. | Escolhido recebimento (BE01/BE06): pedido, notas manual/XML, chegadas, quarentena, estorno e efetivação única da quantidade física conferida. V2 preparada; contrato/limites no documento 18. Unitização e disponibilidade pertencem a BE07/BE08. Não autoriza conexão SQL Server, emissão fiscal ou publicação. |
| D16 | 05/10/2026 | Avançar mais no backend. | Escolhido BE01/BE07: unidades logísticas, identidade permanente, divisão/reagrupamento e dados das etiquetas, mantendo MVC e properties. AC10 continua identificado como proposta; endereçamento/disponibilidade em BE08 e validação de equipamentos em FE06. Autoriza código e testes locais, sem conexão SQL Server ou publicação. |
| D17 | 05/10/2026 | Aplicar o próximo macrobloco proposto, BE08. | Entregues endereçamento, capacidade, movimentos, posições conjuntas, bloqueio/liberação e disponibilidade; consultas iniciais BE14 para FE07/FE08. V4 preparada e 153 testes aprovados; contratos/limites nos documentos 22/23. Mantém MVC/properties e propostas AC04/AC05 identificadas; não autoriza conexão SQL Server, migração, cobrança real ou publicação. |
| D18 | 05/10/2026 | Avançar BE01/BE09 pela orquestração WMS: pedido de saída integral, FIFO, reserva atômica sem vencimento, parcial de pallet, exceção por perfil, saldo reservado e bloqueio por avaria posterior. | Autoriza código, contratos, migration SQL Server somente em arquivos e testes locais isolados. Cedro escreve backend/contrato, Prumo migration/procedimento, Vigia revisa em leitura e Farol consolida os registros. Preservar BE08 e alterações existentes. Exige formatação, clean verify com saída real e atualização Graphify. Proíbe conexão/migration SQL Server real, emissão fiscal, cobrança real, publicação, commit/push, rotinas, alteração dos perfis Hermes e uso do ETL. AC10/AC11 continuam interpretações/propostas identificadas; frontend não iniciado. |
| D19 | 05/10/2026 | Continuar autonomamente até concluir todo o escopo local restante BE01–BE16, sem autorização por macrobloco e sem parar após BE10. | Autoriza backend, contratos, SQL/migrations somente em arquivos, procedimentos e testes isolados fictícios: BE10/BE11, BE05/BE12, BE13, BE14 e integração/preparação BE01/BE03/BE04/BE15. Ordem e arquivos no [documento 26](26-execucao-continua-backend.md). Cedro backend/contratos, Prumo migrations/infra/procedimentos, Vigia leitura e Farol registros centrais; Lume somente apoio de contratos em leitura. Preserva MVC/properties, histórico e propostas AC01–AC16. Exige testes reais por bloco, correção dos achados, clean verify final, Graphify e matriz BE01–BE16. Proíbe SQL Server real, emissão fiscal/NFS-e, cobrança real, publicação, commit/push, rotinas, perfis Hermes e ETL. Dados externos não bloqueiam trabalho independente; registrar item e dono, sem declarar homologação. |

O nome da pasta foi escolhido na execução do pedido com base no nome do projeto. O pacote Java e as ferramentas foram escolhidos tecnicamente em BE02 e registrados no [documento 12](12-base-e-contratos-backend.md); não são escolhas expressas de versão/pacote pelo responsável. D12 acrescentou decisões técnicas descritas no [documento 14](14-cadastros-acesso-e-persistencia.md): JPA/Hibernate, Flyway, esquema `wms`, API Bearer/JWT, normalização/unicidade e campos inicialmente imutáveis. Nome de banco, provedor real de identidade e publicação continuam indefinidos. Essas escolhas não convertem AC01 a AC16 em aprovação do gestor.

### D20 — macrobloco local de database, 06/10/2026

Lucas autorizou, após o fecho local D19, executar BE03/BE15 com auditoria integrada de V1–V9, modelos, consultas e permissões; implementar melhorias locais justificadas, automações, testes isolados fictícios e procedimentos de engenharia de database. Não recriar o modelo nem reabrir Q01–Q27. Migrations congeladas devem ser preservadas; eventual correção exige justificativa, impacto e revisão registrados. Farol mantém states, decisões, continuidade, índice e mapa; Prumo escreve database/infra, Cedro somente a integração backend necessária e Vigia revisa em leitura independente. Resultado pelo ask recebido e registro exclusivo em `orchestracao/.runtime/`, sem callback a Hermes WMS.

O pedido não autoriza conexão/aplicação no SQL Server da empresa, provisionamento externo, grants reais, produção, fiscal/cobrança, frontend, publicação, commit/push, rotinas, perfis Hermes ou ETL. Ausência de host/banco/versão/collation/TLS/identidades não impede o pacote local. H2 e leitura estática não comprovam SQL Server; listar os insumos indispensáveis para o ensaio separado. IDs D01–D19 e BE/FE permanecem estáveis. Execução e evidências no [documento 36](36-database-local-engenharia-e-validacao.md).

**Complemento da mesma D20, 06/10/2026:** Lucas definiu nomes exatos `WMS_DEV` e `WMS_PROD` e autorizou iniciar sua criação somente quando alvo real e acesso autorizado forem inequivocamente confirmados, sem sobrescrever existentes. DEV primeiro; nenhuma migration/carga em PROD nesta rodada. Jamais alterar instalação/configuração global, versão/edição, serviços, instância, collation do servidor, compatibilidade/configurações de bancos existentes, outros bancos/acessos/rotinas; não instalar/atualizar SQL Server. Usar ambiente/padrões já existentes. “System admin, o mesmo que todos os outros estão usando” não identifica host/instância e não autoriza sysadmin à aplicação. Segredos e conexões de outros agentes/projetos não podem ser lidos/usados. Regra durável registrada no AGENTS canônico WMS. Alvo exato ausente na documentação WMS; solicitado somente servidor/instância ou host:porta, sem senha. Demanda local original continua, sem duplicar tarefa ou renumerar IDs.

## Base funcional

A especificação versão 1.0 é a fonte funcional inicial. Os requisitos RN01 a RN30 estão em [regras de negócio](03-regras-de-negocio.md), com seção e página. As interpretações I01 a I07 estão identificadas no mesmo arquivo. O Word recebido em 05/10 complementa essa base; o resumo está em [respostas recebidas](10-respostas-recebidas-2026-10-05.md) e as conciliações em [regras consolidadas](11-alinhamentos-apos-respostas.md).

## Propostas de análise

| ID | Proposta | Situação |
| --- | --- | --- |
| P01 | Manter uma aplicação backend e responsabilidades de negócio separadas dentro das camadas convencionais. | Proposta compatível com a estrutura solicitada; implantação ainda a definir. |
| P02 | Centralizar alterações do estoque, reserva e localização nos serviços responsáveis pelo estoque. | Proposta documentada. |
| P03 | Separar situação do pedido, condição da mercadoria, reserva, localização, fiscal e cobrança. | Proposta documentada; transições ainda serão detalhadas. |
| P04 | Aplicar proteções de concorrência, repetição e registro conjunto de alterações relacionadas. | Proposta de proteção; mecanismos técnicos ainda não escolhidos. |
| P05 | Preservar origem de quantidades, histórico e correções rastreáveis. | Proposta documentada. |
| P06 | Validar integração fiscal cedo e registrar fatos cobrados desde o primeiro piloto. | Proposta que antecipa dependências da sequência do PDF. |
| P07 | Planejar acesso, recuperação, confirmação nos coletores e piloto controlado desde o início. | Proposta documentada; responsáveis e parâmetros pendentes. |

As proteções PR01 a PR11 detalham essas propostas. Não há aprovação tácita por ausência de comentários.

## Situação dos temas após as respostas

Todos os temas receberam respostas. A coluna de tratamento aponta a regra explícita ou a interpretação/proposta que completa o funcionamento. Receber uma resposta não equivale a comprovar preços, ambiente ou desempenho real.

| ID | Tema | Resultado e tratamento | Detalhamento |
| --- | --- | --- | --- |
| Q01 | Dados da mercadoria | Bobinas; peso da nota; SKU do cliente; aviso de validade. Parâmetros por cadastro. | AC10, AC16 |
| Q02 | Divisão e reagrupamento | Parcial do pallet permitido; mesmo SKU/lote/data para reunir. Tratamento de ID e origem proposto. | AC10 |
| Q03 | Entregas em partes | Várias chegadas e notas por pedido; uma nota em um pedido; efetivação única. | AC03 |
| Q04 | Divergências e avaria | Carga em quarentena; tratativa com cliente; supervisor libera. Limite por pedido proposto. | AC03, AC08 |
| Q05 | Triagem | Não atende saída; disponibilidade após liberação e endereçamento. | AC03 |
| Q06 | Posições | Uma unidade por posição; duas para unidade grande. Conciliação com Q14 proposta. | AC05 |
| Q07 | Saída incompleta | Pedido atendido inteiro; mudança exige cancelar e recriar. | AC10 |
| Q08 | FIFO e exceções | Nota específica permite exceção; justificativa e autorização são ações distintas. | AC11 |
| Q09 | Reserva | Sem vencimento; erro fiscal mantém; urgência exige cancelar pedido anterior. | AC01, AC11 |
| Q10 | Cancelamento e devolução | Cancelar documentos/pedido; retornar separado à posição; devolução física por nova entrada. | AC01, AC02 |
| Q11 | Transferência | Fora da primeira versão; transporte no TMS. | AC16 |
| Q12 | Propriedade | Um proprietário por pedido; emitente da entrada é o cliente; sem troca interna de dono. | AC03 |
| Q13 | Fiscal | Rodogarcia emite no NOTAZZ; várias notas possíveis; Natalina valida. Piloto manual proposto. | AC01, AC02 |
| Q14 | Critérios de cobrança | Entrada, posição-dia e saída; parâmetros contratuais por cliente. | AC04, AC05, AC06 |
| Q15 | Diárias | Calendário, mesmo dia zero, pico e vigência nova. Combinação matemática proposta com exemplos. | AC04, AC06 |
| Q16 | Término da cobrança | Saída física encerra; reserva/separação/quarentena continuam; avaria imputável suspende. | AC01, AC04, AC08 |
| Q17 | Fechamento | Ciclo por cliente, corte sem repetir dias, aprovação integral, ESL e ajustes após nota. | AC02, AC07 |
| Q18 | Valor do estoque | Valor da nota proporcional ao saldo; exclui avaria do indicador, não do controle físico. | AC08 |
| Q19 | Estoque inicial e contagem | Planilha se existir saldo; etiquetas conferidas; correção operacional rastreável proposta. | AC03, AC13 |
| Q20 | Perfis | Gestor, Supervisor e Operação; sem obrigatoriedade de segunda pessoa. | AC11 |
| Q21 | Data FIFO | Chegada física; primeira entrega da nota; devolução preserva original; sem reunir datas diferentes. | AC10, AC11 |
| Q22 | Confirmação da saída | Leitura de cada unidade; XML e supervisor/gestor. Conciliação fiscal/físico proposta. | AC01 |
| Q23 | Serviços | Registro manual, supervisor e liberação do gestor; vínculo e duplicidade resolvidos por proposta. | AC09 |
| Q24 | Inativos | Encerramento de compromissos antes da inativação definitiva proposto. | AC12 |
| Q25 | Piloto | Osasco; Caio; dez usuários/quatro simultâneos estimados; necessidade em 13/10. | AC16 |
| Q26 | Equipamentos | Web, Tanca, Mickael; impressão/leitura/rede requerem teste local. | AC14, AC16 |
| Q27 | Continuidade | Planilha, até 36h de parada, backups; Lucas e Caio. Recuperação a comprovar tecnicamente. | AC14, AC15 |

Os identificadores Q01 a Q27 e seus 110 subitens permanecem no questionário histórico. AC01 a AC16 não são outra lista de perguntas: registram soluções derivadas da leitura e do raciocínio. A emissão versus retirada, as duas posições e a combinação pico/diária merecem conferência por exemplos antes do uso real, sem impedir o avanço do estudo.

Os dados que não podem ser deduzidos são preços contratados, parâmetros fiscais concretos, cadastros reais e capacidades do ambiente. Eles entram como tarefas de implantação com responsáveis conhecidos. As propostas não substituem autorização fiscal ou homologação de valores.

## Levantamento técnico

O acompanhamento de execução está no [states.md](../states.md). Esta seção registra os assuntos a definir; os status de implementação ficam nas etapas BE e FE correspondentes, sem repetir uma segunda trilha aqui.

| ID | Ponto ainda aberto | Encaminhamento previsto |
| --- | --- | --- |
| T01 | Versões e ferramentas do frontend/SQL Server; ambiente operacional | Base Java/Spring/Maven escolhida e validada localmente em BE02; ver documento 12. React, SQL Server e ambiente operacional continuam a definir. |
| T02 | Organização interna e dependências do frontend | Pacote e dependências backend definidos tecnicamente em BE02, conforme documento 12; frontend permanece futuro. |
| T03 | Banco, acessos, versão e validação SQL Server | Esquema técnico `wms`, JPA e Flyway escolhidos; V1/procedimento preparados. Nome de banco/host/credenciais e aplicação real ainda dependem do alvo de desenvolvimento. H2 usado somente em testes. |
| T04 | Interface do emissor, retorno, autenticação e ambiente de validação | NOTAZZ identificado; Natalina é referência fiscal. Para o piloto, registro manual/importação proposto; futura API e entrega ao ESL serão levantadas. |
| T05 | Coletor, impressora, etiqueta e código físico de leitura | D16 escolheu UUID permanente e contrato de dados da etiqueta no documento 20. Coletor web e impressora Tanca informados; formato/simbologia, modelos, amostras e leitura continuam para validação com Mickael. |
| T06 | Rede, usuários simultâneos, volume e quantidade de armazéns | Osasco primeiro, dez usuários/quatro simultâneos estimados. Volumes e cobertura serão medidos; não presumir desempenho validado. |
| T07 | Provedor de identidade, implantação, monitoramento e gestão de segredos | Resource Server JWT implementado para cadastros, com perfis/alcances e auditoria. Falta definir/configurar emissor real, contas, ciclo de tokens e integração de login. Hospedagem e condições operacionais continuam a definir. |
| T08 | Recuperação, cópias de segurança e tolerância à indisponibilidade | Lucas em TI e Caio na operação; 36h de parada informadas. Definir e testar capacidade real de recuperação, sem equiparar parada a perda aceitável de dados. |

Os dados funcionais recebidos orientam esses levantamentos; verificações técnicas ainda não executadas permanecem futuras. Nenhuma solicitação foi enviada a terceiros e não há credenciais registradas.

## Como registrar novas decisões

Para cada resposta ou mudança, acrescentar data, quem decidiu ou qual fonte respondeu, IDs afetados, decisão, motivo e impacto. Atualizar o estado da pendência e os documentos envolvidos. Se a resposta contradizer o PDF, registrar o conflito e a solução proposta com fundamento; avançar no estudo sem apresentar a proposta como aprovação do gestor. Validar seus efeitos por exemplos antes do uso real.

## Histórico

| Data | Registro |
| --- | --- |
| 03/10/2026 | Criada a estrutura local e preservada a especificação original. Registradas as decisões D01 a D07, propostas P01 a P07 e questões Q01 a Q20. Aplicação ainda sem implementação. |
| 03/10/2026 | Registrado o pedido D08. Questionário ampliado para Q01 a Q27, com 110 subitens curtos e condicionais. Atualizados índice, continuidade e vínculos com os fluxos. Todas as respostas seguem pendentes. |
| 05/10/2026 | Recebido e preservado o Word com respostas aos 110 subitens e 12 imagens distintas. Registrada D09; consolidadas regras explícitas e 16 tratamentos por raciocínio, sem reenviar questionário. Atualizados arquitetura, fluxos, cenários e continuidade. Sem implementação, acesso ao banco ou mensagem a terceiros. |
| 05/10/2026 | Registrada D10. Consultados os padrões de satelite-tms-api, dashboards-etl e avaliacao-desempenho-competencias. Adaptado AGENTS.md e criado states.md com trilhas backend/frontend e marcos conjuntos. Índices e orientações de retomada sincronizados; sem copiar configurações de outros sistemas ou implementar a aplicação. |
| 05/10/2026 | Registrada D11 e iniciada implementação. Escolhas técnicas de BE02 no documento 12; build, 12 testes e execução local no documento 13. BE01 parcial; SQL Server, autenticação e frontend permanecem etapas futuras. Nenhuma proposta de negócio foi convertida em aprovação do gestor. |
| 05/10/2026 | Registrada D12. Entregues cinco cadastros com API/JPA, controle de acesso, auditoria atômica, revisão e encerramento pendente. V1 SQL Server preparada; 40 testes aprovados em ambiente isolado. Escopo/limites no documento 14 e validação no 15. BE03/BE04/BE05 não concluídos no ambiente real. |
| 05/10/2026 | Registrada D13 e concluída BE16: resources em properties, validação nos serviços, formatação e verificações de ferramentas/arquitetura. Build limpo com 52 testes; JAR local conferido. Padrões no documento 16 e evidências no 15. Sem alteração no SQL Server ou publicação. |
| 05/10/2026 | D15 entrega recebimento manual/XML, chegadas, quarentena, estorno e efetivação única. V2 preparada e 86 testes aprovados; documentos 18/19. Preservados D14/OR01 e os arquivos da preparação de orquestração paralela. Sem migração, emissão fiscal ou publicação nesta implementação. |
| 05/10/2026 | D16 entrega unidades logísticas, composição de origem, divisão/reagrupamento e dados de etiquetas. V3 preparada; 122 testes aprovados, sendo 36 novos. Contrato e validação nos documentos 20/21; próximo código BE08. SQL Server, frontend e impressão física permanecem separados. |
| 05/10/2026 | D17 entrega endereçamento, capacidade, conjuntos de posições, movimentos/bloqueios e consultas de estoque. V4 preparada; 153 testes aprovados, sendo 31 novos. Contratos/evidências nos documentos 22/23. BE08 em validação externa e BE14 parcial; próximo código BE09. Sem SQL Server, frontend, cobrança ou publicação. |
| 05/10/2026 | D18 entregue localmente pela equipe WMS: pedido integral por cliente/armazém, FIFO, reserva sem vencimento, parcial de pallet, exceção por perfil, saldo reservado e avaria posterior. Contratos no [documento 24](24-pedido-saida-fifo-e-reserva.md), V5 preparada e [validação](25-validacao-pedido-saida-e-reserva.md): spotless/clean verify com 197 testes, 44 novos, revisão favorável de Vigia e Graphify atualizado. BE09 em validação externa, BE01/BE14 parciais; BE10/BE11 completos fora do recorte. AC10/AC11 continuam propostas identificadas. Git zero commits, alterações preexistentes preservadas. Sem ações externas proibidas; aguardar nova demanda. |
| 05/10/2026 | Registrada autorização expressa D19 para concluir continuamente o backend local restante BE01–BE16, sem autorização por macrobloco. Ordem/divisão no documento 26; não autoriza homologação ou as ações externas proibidas. |
| 06/10/2026 | Primeiro bloco D19 aceito localmente: BE10/BE11 e complemento BE01, [contrato 27](27-separacao-retirada-retornos-e-avaria.md) e [evidência 28](28-validacao-separacao-retirada-retornos.md). Após achados/P2 temporal, spotless/clean verify com 240 testes, Vigia favorável e V6 compatível em arquivos. Base197, V1–V5, origem e histórico preservados. Segue BE05/BE12 autonomamente. SQL Server, fiscal, equipamentos e conferência operacional/comercial reais permanecem externos; AC01–AC16 não convertidas em aprovação. |
| 06/10/2026 | Recorte local BE01/BE05/BE12 D19 aceito após P2 financeiro BE08: clean verify292/0/0/0,14 XMLs/JAR/222 hashes conferidos, Vigia favorável02:33 e V7/JPA518/518 mais67/67 suplementares compatíveis. [Contrato29](29-cadastros-servicos-e-calculo.md)/[evidência30](30-validacao-servicos-e-calculo.md). Inativação definitiva BE05 permanece após BE13/BE14; BE13 liberado para refinar31 antes de Java/V8. Continuar autonomamente, sem emissão, SQL Server real, cobrança a clientes ou homologação comercial/fiscal presumida. |
| 06/10/2026 | BE01/BE13 D19 aceito localmente após três P2: clean verify333/0/0/0,15 XMLs/JAR/251 hashes conferidos, Vigia favorável04:29, V8/JPA523/523 e163/163 em arquivos. [Contrato31](31-fechamento-contagem-e-contingencia.md)/[evidência32](32-validacao-fechamento-e-contingencia.md). Regularização de origem conserva deltas/versões, período é conferido antes do replay e auditoria recupera a resolução; externo fiscal/SQL/comercial não homologado. Segue BE14/inativação definitiva BE05 e integração final, sem nova autorização, preservando todas as evidências anteriores. |


**Fecho local D19 em06/10/2026:** escopo BE01–BE16 entregue/testado/revisado após P2-locks:367/0/0/0,17 XMLs/JAR/289 hashes conferidos, Vigia favorável e V9/JPA904/904+129/129 em arquivos. Matriz [34](34-matriz-e-validacao-final-backend.md), evidência [32](32-validacao-fechamento-e-contingencia.md) e modelo [35](35-modelo-integrado-e-jornadas-backend.md) sincronizados; Graphify atualizado. Não é aprovação comercial/fiscal/homologação/piloto/publicação. AC01–AC16 e todos os limites/donos externos preservados. Sem nova autorização criada; conclui o trabalho local autorizado por D19.
