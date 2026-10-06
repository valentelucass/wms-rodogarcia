# Preparação técnica integrada — BE03/BE04/BE15, V1–V9

## Correção documental posterior ao456 — 06/10/2026

[Relatório da correção](evidencias/d19-correcao-identidade-ciclos-2026-10-06.md): a atualização367 reutilizou objetos do contrato dentro do histórico360, contaminando manifesto/fonte/JAR e conservando resultados904/122 antigos. Restituída a identidade360 exata da cópia anterior (422845/B459/JAR829A,78.524.597 bytes) por cópia profunda; atual367A257/0C57/JAR352211,78.526.951 bytes separado. Outputs238/67/904/129 e manifesto456 preservados; não detectaram essa mistura. Nova guarda68 relações/fixtures18 e diagnóstico306/306 só de arquivos, antes de ler código corrente, recusam mistura entre ciclos. Procedimento: não guardar `$c.freezeFinal`/`$c.artefatoFinal` diretamente no histórico; copiar por serialização JSON, fixar hash da origem e conferir cada par de fonte/resultado/JAR. Snapshot histórico, estado e aceite pertencem à sua rodada. Nenhum JPA, build, JVM ou banco repetido; homologação externa continua pendente.

## Fonte e artefato FINAL p2-locks367 — 06/10/2026

Nova tarefa explicita de Farol autoriza a reconferencia em arquivos: fonte31 SHA0C57C902AF67645A3949CF136993B0FE1EBADDDAA4CEC2DD3173CC637E96D879, manifesto289 SHAA257476BCC711BCB14A42A27FAE72C092E68F85460700D758B8BC9C70CE38233. JAR target-be14/wms-backend-0.0.1-SNAPSHOT.jar FINAL367,78.526.951 bytes,SHA352211B7A38EFE0E80A034656B41D40CEEDDE0D988FE5AF130E56BF2C1FA13E5. Fonte/models/DDL/literais sem novo schema; apenas servico/teste/doc31 alterados. Refere-se ao novo freeze estavel, nao ao estado em escrita descrito no historico360 abaixo. Aceite local continua com Farol; revisao Vigia e externa separadas.

Diagnostico integrado com -DiretorioArtefato target-be14 agora usa contrato atual, valida hashes do manifesto/fontes antes das leituras, compara resultado367/17 XML/JAR sem executar codigo. JSON/links/AST/metadados sao locais. Original56 e baseline194/360 permanecem preservados, inclusive pom antigo; nao atualizar baseline tecnica historica silenciosamente. Nao executar JAR, SQL, Info/Validate, JVM/H2/build, rede ou ambiente. [Relatorio desta tarefa](evidencias/d19-preparacao-p2-locks367-2026-10-06.md) registra resultados e limites.

## Referência recebida e retenção BE14 — 06/10

O artefato360 agora tem [referência estática conferida e snapshot histórico](evidencias/d19-be14-360-historico-2026-10-06.md): target-be14/wms-backend-0.0.1-SNAPSHOT.jar,78.524.597 bytes,SHA829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13; log final-camadas/17 XML360/0/0/0 de Cedro. Nenhum JAR/build/JVM foi executado por Prumo. [Leitor histórico52/52](verificar-artefato-historico-local.ps1) não lê Java corrente. Diretório opcional no diagnóstico integrado foi preparado; não executado nesta dependência.

Freeze360/289/B459 e comparação histórica904/122 não são aceite: Vigia/Farol07:27 retiveram BE14 por P2 locks CONTAGEM (prelock código raiz versus DTO aninhado). Backend/31 em correção; final depende de nova tarefa com manifesto p2-locks exato. Leitores bloqueiam fase histórica; nenhum DDL novo presumido/V1–V9 intactas. As afirmações de artefato em construção/JPA futuro abaixo descrevem a etapa194/307 anterior preservada; fonte/baseline técnica antiga não foi substituída automaticamente. Homologação externa segue não executada.

## Baseline e complemento formal V9 nesta rodada

Farol/Cedro comunicaram novo Pareamento complementar antes de Java, fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C. [Evidência V9](../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md) registra424/424: ENTRADA_CONTINGENCIA somente no tipo administrativo VARCHAR32, auditoria PEDIDO_ENTRADA/ENTRADA_EFETIVADA existente; AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE. CHECK de situação do serviço reafirma ATIVO/ENCERRAMENTO_PENDENTE/INATIVO em VARCHAR24 já existente em V7, sem coluna/tabela/enum novo. Seis CHECKs cumulativos substituídos; demais tabelas/FKs/chaves/JSON/índices e pares mantidos.

Baseline anteriorV9 SHA518596FB9AEE595E54EDBE13A8E6A1837354EC050AD9F7845A297B6473123CF5 e diagnóstico194/194 de fecho foram preservados. V9 atual SHA17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8 está no contrato técnico com histórico/cópia e autorização de atualização. V1–V8 permanecem intocadas; não afirmar toda V9 imutável. Diagnóstico novo usa essa baseline atual, sem apagar191/194,194/194,56 original,413 ou manifesto250. JPA/guardas só após freezeBE14 separado; nenhum SQL ou regra física executado.


Preparação local D19 em06/10/2026, por Prumo. Escrita somente database/infra/doc33; leitura de fontes locais. Farol recebeu V9/fonte413,18 checks e250 hashes; JPA BE14 depende de freeze e tarefa separada. O artefato final do backend ainda está em construção por Cedro: nenhum JAR atual, presença de target ou teste anterior é tratado aqui como entrega final. Estados/aceites permanecem com Farol.

| Nível de evidência | O que este preparo entrega | O que não comprova |
| --- | --- | --- |
| Preparo local | V1–V8 preservadas e V9 preparada atualizada pelo complemento formal com cópia/hash anteriores, inventário64 tabelas, controles declarados em fontes/properties, plano de acessos/identidade/ensaio/recuperação, diagnóstico estático e hashes | Código BE14/JPA final, execução de SQL, permissões/locks reais, conta/provedor ou equipamento |
| Artefato final | Procedimento de receber manifesto, logs/resultado de Cedro e revisão Vigia/Farol | Ainda não recebido/conferido nesta tarefa; não executar build paralelo ou repetir333 |
| Homologação externa | Casos, donos, registros e critérios de encaminhamento preparados | Alvo SQL Server/provedor/contas/TLS/rede/restauração/RPO/RTO/fiscal/comercial/operacional reais |

## Diagnóstico reproduzível sem ambiente ou rede

[Leitor final](verificar-preparacao-final-local.ps1) e [contrato técnico](contratos/preparacao-final-local.json) usam caminhos fixos deste repositório. O leitor preserva/reutiliza o [diagnóstico56 original](verificar-preparacao-local.ps1) como leitura de fontes, compara nove hashes/guardas e inventário de migrations, cobertura documental das64 tabelas, planos e estabilidade das fontes técnicas ao ler. Não avalia variáveis WMS_DB/WMS_OIDC, não consulta URLs/JWK/TLS/status, não abre SQL ou JVM/Flyway/Maven/H2 e não escreve arquivo. Bloqueio de leitura/formato retorna código seguro, sem raw/stack/conteúdo ou segredo. DTD/entidades XML externas são recusadas antes da leitura original do POM.

Comando autorizado nesta rodada, na raiz WMS:

```powershell
./infra/verificar-preparacao-final-local.ps1
```

Quem coleta a evidência escolhe sufixo novo, verifica que destino não existe e guarda output integral/AST/comando/início/fim/hashes e resultado PowerShell/LASTEXITCODE imediatamente após invocação. LASTEXITCODE nulo não prova saída0 nativa. Não alterar política/confiança para executar script bloqueado; informar o bloqueio seguro e encaminhar a TI/Farol. Não usar raw, outro terminal/env/identidade ou leitura de segredo para diagnóstico. Fontes técnicas podem mudar enquanto Cedro implementa: registrar diferença/hash, sem presumir freeze ou alterar backend.

[Configuração externa](configuracao-externa.md), [permissões](../database/docs/permissoes-minimas.md), [migrations](../database/migrations/README.md), [ensaio/recuperação](recuperacao-e-ensaio.md) e [doc33](../docs/33-preparacao-tecnica-local-backend.md) são os procedimentos detalhados. Nenhum valor sensível deverá ser colocado neste pacote/evidência.

## BE03 — alvo e mínimo privilégio

Perfis/properties atuais separam local sem DataSource, test isolado e sqlserver-dev que efetivamente conecta. Default local/127.0.0.1, validate sem geração automática, SQL init never/Flyway desabilitado no startup e proteção de logs são declarações conferidas em arquivos, sem aplicação iniciada. Database.migrate.ps1 lê ambiente e conecta também em Info/Validate; foi apenas lido e não é diagnóstico permitido nesta rodada.

Lucas/TI com DBA a identificar nominalmente definem banco/schema WMS exclusivo, instância/versão/collation/TLS, topologia/ambientes, credenciais externas, quem autoriza cada conexão/aplicação e janela de manutenção. Não usar banco de outro projeto nem inferir alvo de variável encontrada em outro terminal. O backend atual valida formato/confirmed-target antes do DataSource; o wrapper consulta identidade real do servidor/banco por TLS. Uma validação não prova a outra; requer ensaio externo do caminho completo.

V1–V9 formam sequência incremental em arquivos,64 CREATE TABLE distintos. V4 já contém backfill técnico `UPDATE wms.unidade_logistica SET revisao_conteudo = versao` após adicionar colunas, antes dos CHECKs de revisão/medidas; inventariado e preservado, sem executá-lo ou alterar V4. DBA deve ensaiar esse passo com versões/linhas existentes, locks/custo e transação/histórico na evolução; não é quantidade/nota/FIFO/preço ou fato físico inventado. Demais migrations não têm DML de INSERT/UPDATE/DELETE/MERGE observado como statement. V1 exige esquema novo; não usar baseline/repair/clean para passar por conflito. Identidades aplicação/migration/DBA/consulta separadas, sem db_owner/sysadmin/db_datawriter. Aplicação só SELECT/INSERT/UPDATE necessários por objeto/coluna; fatos/operações/auditoria/revisões/snapshots imutáveis sem UPDATE/DELETE. Conferir SQL emitido/colunas @Version no ensaio: anotações/GRANT não comprovam condição de linha/estado/escopo/rollback.

DBA verifica permissões efetivas/herdadas, acesso a metadados/histórico Flyway e sessão de criação/DML dos índices filtrados. Conferir várias linhas NULL permitidas, chave preenchida única, ISJSON/checks/FKs confiáveis, contexto controlado em serviço e preservação de dados na evolução. Não presumir collation/case/normalização da identidade global; confirmar com regra canônica e dados fictícios no alvo autorizado. Nenhum grant/alvo foi criado.

## BE04 — contas e ciclo do JWT externo

Contrato existente: Resource Server JWT RS256, assinatura/emissor/audiência/sujeito/prazo/perfil/alcances; somente Bearer, sem login/emissão/refresh/sessão local. Prazo nominal entre emissão/expiração até15 minutos; fonte também guarda emissão futura além de60 segundos. Tolerância de relógio/cache e indisponibilidade reais ainda exigem ensaio, sem promessa de revogação imediata.

| Processo | Dono e decisão externa | Evidência futura sanitizada |
| --- | --- | --- |
| Provedor/login/fluxo de renovação | Equipe técnica com Lucas, sem fornecedor escolhido neste preparo | Conta fictícia de ensaio, issuer/audience/claims conformes; registrar caso/código/instante, nunca token |
| Conceder/mudar/desligar conta e alcance | TI administra identidade; Gestor aprova função/contextos com Caio/Supervisor; responsáveis nominais a confirmar por Lucas | Aprovação/mudança rastreável, novo token com alcance correto e tentativa recusada fora do alcance |
| Renovar/recuperar sessão | Equipe de identidade define armazenamento protegido/expiração/recuperação no provedor e cliente futuros | Token novo/expirado e falha de refresh; WMS não passa a emitir tokens por esse plano |
| Revogar conta/alcance/token | TI/identidade com Gestor define prazo e contenção exigidos | Comparar token já emitido e token novo após bloqueio; registrar janela residual e tolerâncias; sem afirmar invalidação instantânea |
| Rotacionar chaves/TLS e tratar indisponibilidade | TI/identidade com Lucas | Chave antiga/nova/desconhecida, JWKS inacessível/certificado rejeitado; erro seguro e sem abrir operação não autenticada |

JWT já emitido pode conservar autorização antiga até validade/tolerâncias aplicáveis. Desligar conta no provedor não comprova rejeição imediata no Resource Server. Se o prazo requerido exigir mecanismo adicional, TI/Cedro/Farol registram lacuna e contrato antes de implementar; não inventar introspecção, blacklist, Redis, fornecedor ou novo campo. Política de contenção e suporte externa ainda deve ser definida por TI. Lista vazia não concede alcance a perfil limitado; papel técnico SQL não equivale a função do operador. Guarda de serviço permanece obrigatória, inclusive replay e confirmação.

Segredos por mecanismo protegido fora do repositório/log/argumentos, com responsáveis, acesso mínimo e rotação. WMS usa chaves públicas, não recebe chave privada do emissor. Em diagnóstico usar IDs/correlação técnica/código/caso/instante e métricas autorizadas; sem header Bearer, claims brutos, XML/JSON/provas/nota/valor/driver/connection string. Erro seguro não autoriza raw ou confiança/certificado bypass.

## BE15 — ensaio, backup/restauração e contingência

| Etapa futura | Critério de prova | Responsável de referência |
| --- | --- | --- |
| Definir RPO/RTO/volumes/retenção/frequência | Decisão explícita considerando operação, capacidade e perda tolerável; não derivar RPO das36h informadas | Lucas/TI/DBA com Caio e responsável |
| Preparar backup/cadeia/chaves e destino isolado | Modelo de recuperação/compatibilidade/cadeia protegidos e restauráveis; retenção/criptografia/chaves externas definidas | DBA a identificar por Lucas/TI |
| Restaurar e conferir integridade | Restauração real em destino autorizado, tempo/instante recuperável/perda medidos; VERIFYONLY isolado não prova restauração | DBA/TI; Caio/Supervisor conciliam operação |
| Aplicar/evoluir V1–V9 | Hash/histórico/constraints/indexes/permissões/sessão/dados preservados, rollback/falha/retomada medidos; sem repair/clean automático | DBA/TI, com contratos/artefato estáveis de Cedro/Farol |
| Concorrência/privacidade | Reserva×contagem×retirada×avaria, preparo/replay, compromisso×inativação e auditoria tardia, sem efeito parcial/duplicado ou log sensível | Cedro/Vigia com Caio/DBA no ensaio autorizado |
| Recuperar fatos na contingência | Identidade global/conteúdo/hash/instante/dependências/prova, efeitos já WMS vinculados, ausências pendentes; saldo/reserva/história reconciliados | Caio/Supervisor/Gestor, com TI/DBA |

Não executar ensaio nesta rodada. Conservar evidências antes/depois; operação parcialmente confirmada ou timeout exige consulta idempotente/resultado original, não novo UUID para duplicar efeito. Contingência identifica fatos físicos ocorridos e se há efeito registrado WMS; não inventa nota/FIFO/origem/instante para fechar histórico. Restauração não exclui reserva/movimento/auditoria/revisão/documento/cobrança e não autoriza fiscal/billing real. Regularização de documentos existentes com Natalina/Controladoria fica separada do físico e do procedimento DBA.

Alvo ou backup sem prova impede aquela etapa externa, não desfaz trabalho local independente. Falha de migration exige conservar logs sanitizados/histórico/estado e diagnosticar DDL efetivamente confirmado antes de repetir; rollback transacional ou restauração não são pressupostos. Escolher correção incremental ou restauração autorizada conforme estado/impacto, com comparação de saldos/UUIDs/operações após retomada. Não configurar rotinas nem automação de restauração.

## Donos e próximos encaminhamentos

Lucas/TI responde pela definição do ambiente/identidade/hospedagem e nomeação do DBA. DBA executa privilégios/SQL/backup/restauração somente com alvo/autorização próprios. Caio e Supervisor/Gestor conciliam fluxo físico e compromissos; Gestor/comercial fornece preços/mínimo/GRIS/corte reais, sem valores nesta entrega. Natalina/Controladoria define regularização fiscal/documentos existentes; Mickael/TI valida coletor/impressora/cobertura. Nenhum desses nomes foi usado como identidade de execução pelo agente.

Cedro continua código/testes/artefato final; Vigia revisão; Farol mantém estados/contratos centrais/aceite/mapa e encaminha freeze BE14 para tarefa JPA separada. Prumo entrega somente preparação/diagnóstico/evidência local nesta rodada. FE03/login, equipamentos, implantação, comercial/fiscal e SQL real são validações externas/futuras, sem frontend ou publicação. [Relatório desta entrega](evidencias/d19-preparacao-tecnica-final-2026-10-06.md) identifica comandos, resultados, hashes, preservação e limites reais.
