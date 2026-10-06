# Backend do WMS Rodogarcia

## Integração local D20 — BE03/BE15

O perfil `sqlserver-dev` aceita somente **WMS_DEV** nesta rodada. Exige `WMS_DB_CONFIRMED_SERVER`, além de host/porta/banco/usuário/senha e confirmação do destino. Cada conexão Hikari fixa os SETs de índices filtrados, confirma ServerName/DB_NAME e recusa credencial administrativa/DDL; TLS valida o certificado. Alternativas JPA de criação de esquema/conexão são recusadas antes do pool. Segredos continuam externos.

Flyway é externo à inicialização/build e desabilitado por padrão (`wms.migrations.skip=true`). A fonte padrão de initSql é um THROW; marcador ausente/divergente não ativa a fonte do wrapper. Execução autorizada deve usar somente o wrapper de Prumo, com identidade de migration distinta (`WMS_DB_MIGRATION_USER`/`WMS_DB_MIGRATION_PASSWORD`), guardas de configuração e SQL transitório escapado. Não habilitar um goal direto nem fornecer overrides Flyway. [Contrato D20 para Prumo/Farol](evidencias/d20-plano.md) detalha o marcador/fonte transitórios; POM sozinho não substitui a recusa de overrides pelo wrapper.

[Ensaio SQL Server optativo](evidencias/d20-ensaio-sqlserver.md) exige alvo loopback isolado, vazio e previamente migrado V1–V9 com conta restrita. Sua preparação não representa execução. D19/367 e evidências anteriores permanecem históricas. D20 usa `target-d20`, com logs/XMLs/resumo/hashes próprios em `evidencias/d20-*`.

Backend Java/Spring com MVC convencional por camadas. Além da base, cadastros, recebimento e unidades, implementa **BE08** (capacidade, endereçamento, movimentos, bloqueios e consultas de estoque), **BE09** (pedido integral/FIFO/reserva), **BE10/BE11** (leitura/separação, documentos existentes, retirada integral, retornos e avaria), o recorte local **BE05/BE12** (Excel, complemento fiscal, configuração, fatos e cálculo), **BE13** (fechamento integral por versão, demonstrativo imutável, referências externas locais e ajustes) e **BE14/BE05 final** (contagem, carga inicial, contingência temporal, indicadores/avisos e encerramento específico). Usa JPA, JWT, auditoria, revisão e repetição segura. Contratos em [cadastros](../docs/14-cadastros-acesso-e-persistencia.md), [recebimento](../docs/18-recebimento-e-conferencia.md), [unidades](../docs/20-unidades-logisticas-e-etiquetas.md), [estoque](../docs/22-enderecamento-movimentacao-e-estoque.md), [saída/reserva](../docs/24-pedido-saida-fifo-e-reserva.md), [expedição/retornos/avaria](../docs/27-separacao-retirada-retornos-e-avaria.md), [cadastros/serviços/cálculo](../docs/29-cadastros-servicos-e-calculo.md) e [fechamento](../docs/31-fechamento-contagem-e-contingencia.md); andamento oficial no [states.md](../states.md). BE13 foi aceito localmente por Farol após revisão Vigia/Prumo com333 testes; BE14 aguarda revisão final própria. Outputs333/251 e anteriores permanecem históricos.

## Organização

| Pasta lógica | Responsabilidade |
| --- | --- |
| controllers | Receber solicitações, encaminhar aos serviços e devolver respostas |
| services | Aplicar regras de negócio e coordenar as operações |
| repositories | JPA: consultas paginadas, locks dos cadastros/pedidos e persistência dos fatos e auditoria |
| models | Cadastros, auditoria, recebimento, unidades, composição, capacidade, ocupação, movimentos, pedidos de saída e reservas, mapeados no esquema `wms` |
| dto | Definir os dados de entrada e saída de cada operação |
| config | Configurações da aplicação, integrações e segurança |
| exceptions | Tratamento consistente de erros e mensagens para o usuário |

As camadas ficam em `src/main/java/br/com/rodogarcia/wms`. Injeção por construtor; controllers encaminham aos services, que verificam acesso, aplicam regras e coordenam transações. DTOs não expõem entidades/lazy loading. A consulta de status não precisa de repositório. Não há CRUD genérico que permita contornar as regras dos cadastros.

## Execução local

Requer **JDK 21**, com `JAVA_HOME` apontando para sua instalação. O Maven Wrapper baixa e verifica a versão fixada; dispensa Maven global. O primeiro build requer acesso ao Maven Central. No PowerShell, a partir de `backend`:

```powershell
# Ajustar ao JDK 21 desta máquina, se necessário:
# $env:JAVA_HOME = 'C:\caminho\para\jdk-21'
& "$env:JAVA_HOME/bin/java.exe" -version
./mvnw.cmd clean verify
./mvnw.cmd spring-boot:run
```

No Linux/macOS, usar `sh ./mvnw clean verify` e `sh ./mvnw spring-boot:run`.

A aplicação usa `http://127.0.0.1:8080`, perfil `local`. Em outro terminal:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/status
```

Para outra porta, definir `$env:WMS_PORT = '8081'` antes de iniciar. Encerrar com `Ctrl+C`. Também é possível executar o artefato:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar target/wms-backend-0.0.1-SNAPSHOT.jar
```

Perfis aceitos, isoladamente: `local`, `test` e `sqlserver-dev`. No padrão `local`, apenas status está habilitado, sem conexão/persistência. H2 é exclusivo dos testes e não acompanha o JAR. Homologação/produção ainda não foram preparados.

## SQL Server de desenvolvimento

Após definir e conferir o alvo exclusivo do WMS, seguir o [procedimento de migrations](../database/migrations/README.md). V1 a V8 foram conferidas em leitura nos blocos locais aceitos. V9 está preparada somente em arquivos por Prumo e aguarda comparação final com o JPA/contrato31 do BE14. Nenhuma migration foi executada em SQL Server. Hibernate usa `validate`; a configuração recusa DDL/scripts automáticos antes de abrir conexão.

As variáveis externas necessárias são `WMS_DB_HOST`, `WMS_DB_PORT` (1433 por padrão na aplicação), `WMS_DB_NAME`, `WMS_DB_USER`, `WMS_DB_PASSWORD`, `WMS_DB_CONFIRMED_TARGET` (`host:porta/banco`) e `WMS_OIDC_ISSUER`, `WMS_OIDC_JWK_SET_URI`, `WMS_OIDC_AUDIENCE`. O procedimento de migração exige também confirmação do nome real do servidor. Usar credenciais separadas para aplicação e migration, fornecidas fora dos arquivos versionados.

Com banco preparado, identidade configurada e alvo confirmado:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar target/wms-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=sqlserver-dev
```

TLS valida o certificado SQL Server. Configurar o provedor real conforme o contrato de claims no documento 14. Nenhum host, banco ou usuário real está predefinido.

## Acesso e falhas

`GET /api/v1/status` informa se a aplicação responde. Com cadastros habilitados, a API exige JWT assinado em `Authorization: Bearer`; GESTOR altera cadastros, SUPERVISOR/OPERACAO consultam conforme alcance por cliente/armazém. Não há emissão de token, senha padrão, usuário de demonstração ou sessão local. Provedor real e integração de login ainda precisam de configuração/validação em BE04/FE03.

Cada solicitação recebe `X-Request-Id` gerado pelo servidor. Falhas usam `application/problem+json`, com `codigo` e `idOperacao`. Não registrar payloads, documentos fiscais, senhas ou valores rejeitados. Testes usam dados fictícios.

Recebimento usa `/api/v1/pedidos-entrada`: todos os perfis operam conforme alcance simultâneo por cliente/armazém; Supervisor/Gestor efetivam, estornam e cancelam. Toda efetivação gera quantidades ainda indisponíveis para saída. XML é extração operacional e não validação fiscal; formato, limites, permissões e repetição estão no documento 18.

Unitização usa rotas de entradas/unidades dentro do pedido; leitura por UUID e etiqueta usam `/api/v1/unidades-logisticas/{codigo}`. Operação unitiza e consulta; Supervisor/Gestor dividem/reagrupam unidades ainda não endereçadas e sem bloqueio. Etiqueta é consulta sem criação de saldo; sua revisão não muda com localização. Campos e restrições no documento 20. Nenhum trabalho é enviado à impressora.

Estoque usa `/api/v1/estoque` e `/api/v1/estoque/saldo`; movimentos, bloqueio/liberação, avaria posterior e histórico ficam sob `/api/v1/unidades-logisticas/{codigo}`. Gestor configura capacidade/conjuntos; Operação confirma posição e ocorrências; Supervisor/Gestor liberam bloqueio preventivo após revalidação, preservando reserva ativa. Avaria não é reparada pela liberação. Saldo distingue pendente de unitização, físico unitizado, disponível, reservado e demais indisponibilidades. Contratos FE07/FE08 nos documentos 22/24.

Saída usa `/api/v1/pedidos-saida`. Todos os perfis criam/consultam e reservam por FIFO conforme alcance, com saldo validado na criação e confirmação. Qualquer perfil justifica seleção excepcional; Supervisor/Gestor confirmam a proposta, cancelam ou revertem a reserva integralmente. Parcial de pallet protege o restante da unidade contra outra separação (proposta AC10). Bloqueio/avaria/endereço encerrado sinalizam impedimentos e impedem prosseguimento, mantendo reserva sem vencimento. Repetição retorna confirmação original, inclusive após mudança posterior; GET e revalidação mostram o estado atual.

Expedição acrescenta leitura/revisão da etiqueta e separação física da unidade completa, cobertura por documentos NOTAZZ/XML existentes e retirada integral por Supervisor/Gestor. Documento não baixa estoque. Parcial conserva UUID/origens/FIFO, exige destino do remanescente e nova revisão da etiqueta; espaço remanescente mantém equivalência. Retorno interno cancela o pedido integral sem nova entrada; devolução posterior cria entrada vinculada à baixa e conserva FIFO original. Snapshot de separação permanece imutável por ciclo na operação/auditoria. Resolver compromissos de cadastro ENCERRAMENTO_PENDENTE exige Gestor antes de qualquer replay, sem reativação geral. Documentos, física e auditoria ficam separados quanto ao significado e atômicos quanto aos efeitos confirmados.

Avaria usa `/api/v1/estoque/unidades/{codigo}/avarias`: Supervisor/Gestor registra quantidade/data/destino e reparo; Gestor reconhece responsabilidade. Preserva reserva, condição original e histórico. Retorno usa condição efetiva de reparo inicial; bloqueio preventivo é restaurado somente no ciclo vigente. Quantidade/equivalência de uma ocorrência retroativa vêm da linha temporal, incluindo retirada parcial e o limite comprovado pelos snapshots de transformação BE07. Período anterior à última transformação sem base suficiente retorna HISTORICO_AVARIA_INSUFICIENTE. Nenhum preço, emissão fiscal ou cobrança real é realizado.

Importação Excel usa prévia multipart XLSX/comando e confirmação atômica com revalidação sob lock do armazém. Exige Gestor, inclusive antes de replay; arquivos até 5 MiB, requisições até 6 MiB e 5.000 linhas, sem fórmulas/macros/vínculos/hyperlinks. Posição 01 e capacidades ausentes são preservadas; ausência não significa infinito. Complemento fiscal revisa referências/dados próprios sem trocar documento/código/SKU nem inventar CFOP/NCM/alíquotas.

Financeiro usa `/servicos-cobranca`, `/tabelas-cobranca`, `/vinculos-tabela`, `/contratos-cobranca`, `/fatos-servico`, `/avarias/{id}/marcos-financeiros` e `/calculos-cobranca` sob `/api/v1`. Supervisor/Gestor registra execução e consulta cálculo no alcance; Gestor configura, anula ou valida marco. Tarifa vem da execução; chave do fato impede dupla confirmação manual/sugestão. Memória guarda pico por regra/categoria, parcelas, intervalos físicos de valor separados da armazenagem, mínimo/GRIS explícitos e pendências de origem/avaria/histórico. Cálculo é snapshot separado da decisão integral/versionamento do fechamento BE13.

Fechamento usa `/fechamentos-cobranca` e `/ajustes-fechamento`: Gestor prepara/aprova/rejeita/reabre e registra manualmente entrega/referência/declaração/tratativa externa; Supervisor/Gestor consulta no alcance. Ciclos conservam nominal29/30/31 ou âncora em dias corridos. Dias/fatos são únicos no contexto e versões guardam demonstrativo/hash UTF-8 imutáveis, com decisão/estado externo separados. Entrega local declarada fica DESCONHECIDO até prova identificada; não envia ao ESL. NFS-e tardia/múltipla é preservada com CONFLITO_EXTERNO e tratativa integral, sem emissão/cancelamento reais. ZERO/CREDITO exige resolução financeira e confirmação fiscal separadas. Correção100→80→70 gera−20/−10 no próximo ciclo, sem reescrever original ou duplicar ajuste recebido/APLICADO; composição antiga não reaplica ajuste redirecionado.

Resolução financeira de cadastro ENCERRAMENTO_PENDENTE/INATIVO recebe compromisso existente de mesmo contexto e execução/período, com Gestor antes de replay. Configuração específica/anulação/cálculo/fechamento preservam histórico e não reativam cadastro nem geram movimento. Os comandos comuns recusam destino INATIVO e transição de cadastro INATIVO. Solicitação e inativação específicas usam `/encerramentos/{tipo}/{id}`, Gestor/escopo/revisão e revalidação física/financeira conjunta; impedimentos permanecem consultáveis e comprometem a confirmação definitiva. Resolução de remanescente permite somente o pedido integral identificado, inclusive carga preparada, sem disponibilizar estoque para pedidos comuns.

A marcação antiga BE08 em `/unidades-logisticas/{codigo}/avaria` é reconciliada pelo movimento imutável AVARIA_ESTOQUE, sem usar a flag atual como único retrato passado. Falta de quantidade/período/responsabilidade produz pendência por movimento/UUID/ciclo e valor financeiro desconhecido. Somente ocorrência detalhada posterior vinculada comprova seu intervalo; dano antigo reparado não cobre nova marcação. Reparo/reconhecimento posteriores preservam lacunas anteriores e snapshots já calculados. Bloqueio, reserva, origem e validações BE08/BE07 permanecem nos serviços operacionais. Sem nova coluna/tabela para esse P2.

BE14 usa `/contagens`, `/cargas-iniciais` e `/contingencias` sob `/api/v1`. Contagem é versionada e tem impedimento próprio: leitura antiga/divergente não libera reserva, e ajuste exige Supervisor, causa/destino/prova/deltas por origem. Reserva80/contado70 mantém físico100 até resolver o pedido integral; recontagem válida substitui a diferença antiga, preservando bloqueios independentes. Recomposição limita-se ao saldo original comprovado ainda não retirado. Ajuste a zero registra fim físico datado, distinto de retirada fiscal.

Carga inicial PENDENTE conserva dados ausentes. Preparar persiste entrada/unidades uma vez; PREPARADA continua indisponível até leitura de todas as etiquetas. Confirmação não recria saldo. Cancelamento de PREPARADA exige resolução física integral identificada e mantém FK/histórico. Contingência distingue efeito já registrado no WMS (somente vínculo) de reconstrução temporal de fato físico comprovado. Identidade global, JSON/hash originais e dependências ausentes são conservados; ENTRADA/AJUSTE/REMANEJAMENTO/RETIRADA possuem caminhos temporais locais testados, sem atribuir a data de conciliação ao fato passado. Prova insuficiente fica pendente, e replay retorna confirmação original enquanto GET mostra estado atual.

Consultas BE14 são paginadas0/20/100 e escopadas: listas/detalhes/revisões, filtros atuais e impedimentos. `/indicadores-estoque` pagina por SKU e distingue físico/disponível/reservado/bloqueado/unitização/estágio, sem dupla soma. Valor desconhecido é NULL com motivo explícito; validade usa configuração Gestor por contexto e avisa sem bloqueio automático. Filtros HTTP usam DTOs e são convertidos nos serviços; ArchUnit verifica toda produção pelo diretório real de classes da aplicação.

## Verificação e limites

BE01/BE14/BE05 final: [clean verify completo](evidencias/d19-bloco4-clean-verify-final-camadas.log) **BUILD SUCCESS,360/0/0/0**,17 XMLs/JAR,3:47min,06/10/2026 às07:12:12 -03:00. Preserva333 anteriores e acrescenta27 cenários: Contingencia12, Jornada13 e Fechamento2. [Spotless final](evidencias/d19-bloco4-spotless-final-camadas.log) SUCCESS,276 Java,1.986s,07:08:18. JAR em target-be14,78.524.597bytes,SHA-256 `829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13`. [Resumo](evidencias/d19-bloco4-resumo-final.json), [preservação333](evidencias/d19-bloco4-preservacao-final.json), [manifesto](evidencias/d19-bloco4-freeze-final.sha256) e [entrega](evidencias/d19-bloco4-entrega-final.txt) ficam fora de target, com cópias dos17 XMLs. Backend/doc31 congelados para revisão Vigia/Prumo/Farol; sem aceite BE14 antecipado.

A jornada HTTP independente comprova chegada50+50/duas unidades, armazenagem, pedido inteiro60 por50+10, retirada/remanescente40, fato único, contagem identificada a zero, cálculo122 com configuração fictícia explícita, fechamento imutável e inativação após resolução integral. Os comandos temporais ENTRADA/AJUSTE/REMANEJAMENTO/RETIRADA e seus vínculos/replays foram executados separadamente, preservando data física e confirmação original. Concorrência determinística, perfis/escopos, bloqueios independentes, rollback da auditoria, consultas atuais e validade/valor desconhecido são conferidos. [Bateria117](evidencias/d19-bloco4-testes-integracao-fisico-financeira.log), [jornada13](evidencias/d19-bloco4-testes-jornada-fronteiras.log), [camadas/consultas17](evidencias/d19-bloco4-testes-camadas-consultas-import.log) e [diagnóstico das tentativas](evidencias/d19-bloco4-diagnostico-tentativas.txt) conservam os resultados reais. Tentativa290/2/OOM384 e todos os outputs históricos permanecem. H2/HTTP não validam SQL Server, fiscal/comercial, provedor ou equipamento reais.

Histórico aceito BE13 **p2-origem**: [clean verify](evidencias/d19-bloco3-clean-verify-final-p2-origem.log) BUILD SUCCESS,**333/0/0/0**,15 XMLs/JAR,3:30min,06/10/2026 às04:17:56 -03:00, JDK21/Maven3.9.16. [Bateria relevante](evidencias/d19-bloco3-testes-relevantes-p2-origem.log):93/0/0/0,2:13min,04:13:33. [Spotless final](evidencias/d19-bloco3-spotless-final-p2-origem.log):SUCCESS,238 Java,4,487s,04:14:19. [Ferramentas](evidencias/d19-bloco3-ferramentas-p2-origem.log), [resumo](evidencias/d19-bloco3-resumo-final-p2-origem.json), [preservação328](evidencias/d19-bloco3-preservacao-final-p2-origem.json), [manifesto](evidencias/d19-bloco3-freeze-final-p2-origem.sha256) e [entrega](evidencias/d19-bloco3-entrega-final-p2-origem.txt) são novos arquivos fora de target. Outputs328/251 e todos os anteriores permanecem intactos.

Três P2 corrigidos: conferir ajustes originados em todas as versões antes de trocar base financeira, exigir período histórico antes do replay da preparação e guardar compromisso/contexto/período na auditoria de configuração/cálculo/fechamento. Troca100→80 com−20 anterior exige regularização explícita+20 no ciclo apto; APLICADO não é apagado/transferido/revertido. Correções seguintes seguem a última correção comum do fechamento, excluindo regularizações. Novos testes exercitam VALIDADO/APLICADO, segunda troca com origens distintas, cadeia, replay/perfis, concorrência HTTP, rollback e preparação histórica/futura. Auditoria é consultada por HTTP Gestor e H2 nos três caminhos; falha reverte efeitos/marca de repetição. Leitura de aprovação histórica aceita o envelope novo e o JSON antigo, preservando bytes/hash/respostas anteriores. Schema31 tem quatro campos adicionais em duas tabelas, comunicado antes do JPA; sem tabela nova ou literal administrativo adicional. **Freeze333/251 histórico preservado; aceite local BE13 posterior confirmado por Farol/Vigia/Prumo. BE14 pertence à entrega própria abaixo.**

Configuração Spring em `.properties` por ambiente, conforme D13. Os [padrões de engenharia](../docs/16-padroes-de-engenharia-backend.md) detalham responsabilidades, validação dos serviços e ferramentas. `validate` confere JDK 21, Maven compatível e formatação Java; os testes ArchUnit protegem as fronteiras entre camadas. Para formatar alterações: `./mvnw.cmd -B -ntp spotless:apply`. `.editorconfig` orienta o editor.

`clean verify` compila, testa e empacota o JAR. A suíte de cadastros usa HTTP real, H2 isolado e tokens assinados com chave efêmera. Confere acesso, duplicidade, precisão, revisão simultânea e rollback da auditoria. Relatórios: `target/surefire-reports`, ignorados no versionamento. Não é necessário SQL Server ou provedor de identidade para executar os testes.

Para evitar disputa com compilação do editor, o build admite `-Dwms.build.directory=target-be14`; o padrão continua `target`. Os relatórios dessa execução ficam em `target-be14/surefire-reports`. A suíte completa BE14 usa `MAVEN_OPTS=-Xmx384m` para o Maven e `-DargLine=-Xmx768m` para o processo dos testes. O heap menor384MiB esgotou na tentativa histórica; nenhuma verificação/cenário foi removida para contornar isso. Com `JAVA_HOME` do JDK21, os comandos finais são:

```powershell
./mvnw.cmd -B -ntp -Dwms.build.directory=target-be14 spotless:apply
./mvnw.cmd -B -ntp -Dwms.build.directory=target-be14 -DargLine=-Xmx768m clean verify
```

O recorte BE05/BE12 após P2 foi aceito localmente com **292 testes aprovados**: os286 do freeze parser-final preservados e seis fronteiras novas. [Clean verify P2](evidencias/d19-bloco2-clean-verify-p2-avaria-final.log): BUILD SUCCESS,2:50min,06/10/2026 às**02:17:02 -03:00**,JAR/14 XMLs,zero falhas/erros/ignorados. [Spotless P2](evidencias/d19-bloco2-spotless-p2-avaria-final.log): SUCCESS,1,844s às02:14:01,210 Java. [Bateria relevante](evidencias/d19-bloco2-testes-p2-avaria-relevantes.log):162/0/0/0,1:54min às02:13:39. [Resumo](evidencias/d19-bloco2-resumo-p2-avaria-final.json), [entrega](evidencias/d19-bloco2-entrega-p2-avaria-final.txt), [preservação](evidencias/d19-bloco2-preservacao-p2-avaria-final.json) e [manifesto222](evidencias/d19-bloco2-freeze-p2-avaria-final.sha256) conservam o freeze histórico aceito. Farol liberou BE13 após Vigia favorável/V7 compatível; a suspensão documental daquele P2 foi encerrada.

Execução final BE13 com JDK Temurin21.0.12.1+1/Maven Wrapper3.9.16: [bateria relevante](evidencias/d19-bloco3-testes-transacoes.log)226/0/0/0,2:54min,03:34:18; [spotless:apply](evidencias/d19-bloco3-spotless-final.log) SUCCESS,238 Java,2,045s,03:34:56; [clean verify](evidencias/d19-bloco3-clean-verify-final.log) BUILD SUCCESS,**328/0/0/0**,15 XMLs,JAR,3:16min,**03:38:18 de06/10/2026 -03:00**. Os292 testes anteriores e36 novos passaram. Ferramentas/commands estão no [contrato31](../docs/31-fechamento-contagem-e-contingencia.md) e no [output real](evidencias/d19-bloco3-ferramentas-cmd-final.log). JAR78.328.376bytes,SHA-256 `F7246BEE3B66695186BD31F5D5EC9F1422AD4208A638ED2FD83DFF81CC28DA81`.

Os36 cenários BE13 cobrem calendários, aprovação/rejeição/reabertura integrais, cálculo pendente/desatualizado, snapshot/hash, identidade de dia/fato, revisão, replay/perfis/escopos, entrega desconhecida, múltiplas NFS-e/tardia, ZERO/CREDITO e cadeia de ajustes/regularização. HTTP concorrente com barreiras determinísticas verifica preparação/replay e NFS-e×reabertura. Falha de auditoria reverte criação, finalização/aplicação e tratativa/redirecionamento. Resolução histórica e guardas INATIVO foram exercitadas sem flexibilizar validações. Logs iniciais com fixture/import/relógio e o225/7 estão preservados; a falha real rollback-only foi corrigida por comparação pura ao registrar referência existente como conflito, mantendo aprovação/entrega estritas.

[Resumo15XMLs/JAR](evidencias/d19-bloco3-resumo-final.json), [entrega](evidencias/d19-bloco3-entrega-final.txt), [preservação/diferenças](evidencias/d19-bloco3-preservacao-final.json) e [manifesto final](evidencias/d19-bloco3-freeze-final.sha256) ficam fora de target. Esse freeze328/251 corresponde à revisão BE13 anterior, depois corrigida no p2-origem333 e aceita localmente. Outputs e manifestos permanecem intactos. D19 continua autorizado.

O [build286 anterior](evidencias/d19-bloco2-clean-verify-parser-final.log), [spotless anterior](evidencias/d19-bloco2-spotless-parser-final.log), [resumo anterior](evidencias/d19-bloco2-resumo-parser-final.json), [entrega anterior](evidencias/d19-bloco2-entrega-parser-final.txt) e [manifesto222 anterior](evidencias/d19-bloco2-freeze-parser-final.sha256) permanecem intactos. O esclarecimento V7 distingue tipos administrativos CRIACAO_SERVICO/CRIACAO_TABELA das ações de auditoria CRIACAO, sem alterar Java/idempotência desses comandos. As duas tentativas P2 com falha no helper/consulta de teste também foram preservadas.

BE10/BE11 foram aceitos localmente no [documento28](../docs/28-validacao-separacao-retirada-retornos.md), com [output240](evidencias/d19-bloco1-clean-verify-p2-final.log), [formatação](evidencias/d19-bloco1-spotless-p2-final.log), [resumo](evidencias/d19-bloco1-resumo-testes-p2-final.txt) e [entrega](evidencias/d19-bloco1-entrega-p2-final.txt) preservados. Builds283/284 e tentativas de fixture/instrumentação BE05/BE12 também permanecem nos logs históricos.

Os testes anteriores incluem HTTP/serviços, concorrência H2, multiitem integral, documentos/cobertura, duas posições, remanescente, 201 reservas com retorno/rollback, snapshots por ciclo, Gestor antes de replay, avaria inicial/ciclos e base temporal antes/depois de retirada/divisão/reagrupamento. O contexto de expedição compartilha relógio monotônico de dados fictícios/serviços; validações de produção continuam exigidas. BE05/BE12 confere multipart real, importação/revalidação/rollback/concorrência, referência fiscal/identidade/escopo, vigência e execução, parcelas de um centavo, SKU A/B, pico por categoria, triagem/armazenagem/valor físico, conservação na unitização, mínimo/GRIS10de30 e avaria retroativa/sobreposição/origens/marcos/snapshot/rollback. P2 acrescenta HTTP da avaria BE08 com reserva80 de100, antes/depois do marco, replay/origens/bloqueio, reparo seguido de novo dano, cobertura retroativa versus lacuna, reconhecimento posterior, valor físico sem diária e rollback da auditoria da marcação. Relógio fictício controlado apenas no contexto financeiro de teste; ledger de retirada do fixture comprova cálculo histórico, não autoriza retirada operacional de unidade bloqueada. H2 não valida SQL Server/V7, provedor real, assinatura fiscal, frontend, impressão, equipamento ou piloto. Demais blocos locais seguem autorizados pela [execução D19](../docs/26-execucao-continua-backend.md); configurações comerciais reais e emissão externa continuam com validação própria.
