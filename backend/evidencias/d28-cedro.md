# D28 — entrega Cedro

Executado no JAR atual `4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1`: build novo clean verify sem sqlserver-it, 420 testes sem falhas, erros ou ignorados. Foram 761 chamadas HTTP reais e 211 assertivas nas 17 rodadas, incluindo reds preservados. Fixtures offline não somam HTTP. Seis famílias D28 fictícias, negócio somente por HTTP e WMSDEV/WMS_DEV confirmado antes de operar.

As 29 linhas estão vinculadas em `d28-cobertura.json/md`: caso, índice, UUID, perfil, IDs, RequestId, hash e assertivas/fotos SQL novas. R17 mantém o limite material SQL300. `d28-perfis.json` deriva perfis e respostas dos casos reais. Não é homologação do backend inteiro.

R13: AST e mock Query-Sql, 6/0. R23: 20/0 offline de opt-in, ticket, mutex entre processos, prazo, estado final e saldo antes do gate, sem SQL/credencial/JVM. R24 Prumo: 16/0 + 6/0 KEY/PID. R16: entrada 28 efetivada com aceite explícito fictício da falta de uma unidade; físico104, pendente4, unitizado100, disponível100. Modo fresco bloqueado no launcher e helper; fixture isolada3/0.

R16 green D284B2EA65C tem SELECT34/0 e focal8/0. GETs após replay dos pedidos 46/47 e entrada 27 estão em D282DDA1365, somente leitura, SELECT68/0 e focal5/0. As reservas originais não foram repetidas. O par real200/409 tem duas threads HTTP/JDBC na JVM e SELECT posterior; vencedor47/perdedor46 foram derivados da prova.

Financeiro aprovado não zero: mínimo50 em ciclo fechado real06→07, reabertura/v2/v1SUPERADA e memória anterior intacta, sem história fabricada ou NFS-e. Variantes54/PENDENTE/53.75 são previsões em corte aberto. Ajustes, RN22/extremos/científico, filtros/FIFO/quarentena/avaria/GRIS/vigência constam nos casos e SQL novos.

| Red / achado | Classificacao | Green / conclusao |
| --- | --- | --- |
| D284DC609CC | roteiro/fixture | D288163F01B criou recebimento100 por HTTP na mesma familia36 e positivo201/replay soma5 |
| ['D28079AE038', 'D28EFFF454F'] | helper/oraculo GET final | Filtro actual200+DTO/ID valido; D28E89EBAE4 listas, D282DDA1365 saidas46/47 apos replay+entrada27 |
| D289A0CF1ED | oraculo/precondicao R16 | D284B2EA65C retomou somente entrada28 com aceite explicito motivo ficticioAC03, efetivou4;104/pendente4/100unitizado. Fonte final bloqueia modo fresco; fixture offline3/0 e launcher negativo sem SQL/HTTP |
| prefixo inicial d28-select-liberar e guardaDB_NAME/login antigos | helper gerador/contrato | gerador+runner usam prefixo d28-prumo-select-liberar e envelopealvo.banco/login/usuario; guardas12/0 recusam envelope antigo |
| reextracao JAR lib em uso falhou por lock | helper compilacao | extrair somente ausente, conferir comprimento de lib preexistente; compilacao final sem substituir lib aberta |
| bind imediato portFree false apos fechamento, inclusive failureXML | captura de encerramento | sem corrigir flags historicas; Win32_Process/Get-NetTCPConnectionconfirmam0 listeners e0 APIs/helpers nas17 rodadas |
| d28-aguardador-red.md | teste/auxiliar offline aguardador | posicionar operador no fim linha;20/0 com mutex real entre processos, sem SQL/credencial/JVM |
| d28-cobertura-red.md | recibo/consolidador | decodificacao explicita por BOM mantendo bytes;29 linhas causais |
| GET historicopreunitizacao vsfoto atual; array vazioNULL; zero movimentos global depois fixture; avaria5 vs20/reservaREVERTIDA vsATIVA | oraculos SQL Prumo, autoria independente | SELECT/focais corrigidos novos; reds de Prumo preservados nos arquivos -antes/tentativas |

Fontes/classes finais arquivadas por hash `312DE2BB58BE17C5D5F64669502F67495691B91ECDA2CC9FADD84A0DFC0A2F75`. H8 executado:7B0FA59855F40B7C8A7B6AB49F02ACE977EDAFEE966426108DB4B92EDA7FECBB preservado; JAR de aplicação inalterado. Manifestos `d28-helper-versao-*.json` e cópias `target-d28-helper/versionados/`. A compilação final não inicia jornadas. H1–H6 não tiveram todos os compilados intermediários arquivados: limite explícito, sem hashes retrospectivos inventados.

Encerramento nativo em 2026-10-07T13:11:34.8569301Z: zero JARs próprios, helpers e listeners nas portas das 17 rodadas. Inclui failureXML com flag histórica portFree=false preservada. Cada hold encerrou após sinal Prumo. Nenhum processo alheio ou terminal foi encerrado ou resetado.

Conferência dos recibos: 11 checks, 0 falhas. Fontes D27 derivadas preservadas por hash. Preexistentes/dirty preservados, sem alterar backend/src, database, frontend ou registros centrais. Sem DDL/PROD/launcherDEV-PROD/commit/push/Hermes/ETL.

Entrega de Cedro concluída. Farol consolida matriz/recibo central e Vigia encerra parecer. Nenhuma jornada/API de negócio nova será iniciada. Mensagens de preflight antigas são notificações já atendidas.
