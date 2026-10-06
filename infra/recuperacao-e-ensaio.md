# Recuperação, contingência e ensaio SQL Server — BE15

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Plano local D19, não executado. Fontes: Q27 no [documento 10](../docs/10-respostas-recebidas-2026-10-05.md), propostas AC14/AC15 no [documento 11](../docs/11-alinhamentos-apos-respostas.md) e [ordem D19](../docs/26-execucao-continua-backend.md). A parada tolerada informada de 36h não autoriza perder 36h de movimentos nem comprova prazo de recuperação. Nenhuma rotina, servidor ou provedor foi configurado.

## Complemento BE14/V9 — ensaio futuro, somente planejado

[V9/matriz](../database/contratos/v9-matriz-cobertura.md) parte do complemento31 SHA996750EA…, com resolver-cancelamento/carga_id e efeitoRegistradoNoWms formalizados. JPA/guardas aguardam freeze. Sem SQL/restore/conexão/rotina executados. V1–V8 e provas aceitas localmente preservadas, sem repetir revisão333. [Procedimento V9](../database/docs/procedimento-v9.md) separa preparação/ensaio/autorização, [acessos](../database/docs/permissoes-minimas.md) são proposta.

Após autorização específica, Lucas/TI/DBA ensaiam destino isolado, sequência incremental, constraints confiáveis/custos WITH CHECK, sessão do índice filtrado e identidade global conforme collation definida. Caio/Supervisor/Gestor conciliam físico/reservas/origens/pendências; parâmetros/documentos reais continuam com responsáveis externos. Sucesso H2 não comprova dialeto/locks/permissões/índice NULL filtrado/desempenho/restauração SQL Server.

Contingência conserva identidade/contexto/tipo/instante real e prova, incluindo dependências ausentes no envelope/hash originais. FKs quando materializáveis; ausência não autoriza inventar linha/origem/data, ciclos incluem identidades originais. efeitoRegistradoNoWms true só VINCULAR efeito comprovado; false admite reconstrução temporal do fato ainda sem registro, com história suficiente, pendência em ausência/contradição e sem trocar timestamp/hash antigo. Cada efeito/marca/auditoria atômicos, dependência conciliada antes; não conciliar ignorando cadeia.

Após restauração, conferir revisões/impedimento próprio de contagem, origem/quantidade/reserva/ocupação/permanência, entrada ligada/etiquetas de carga e resposta original. Preparar entrada/unitizar uma vez; confirmação lê TODAS etiquetas. PREPARADA soma físico uma vez mas não disponibiliza saída; resolver-cancelamento exige zero integral/ref comprovada e mantém entrada histórica em CANCELADA, sem liberar conteúdo positivo pela marca isolada. Conferir carga_id/pedido integral reservado/etiquetas autorizadas, contingências/avisos e matriz física/financeira completa sob locks antes de INATIVO.

Recuperação não limpa notas/revisões/movimentos/auditoria/cobranças. Correção por serviço/fato identificado, conservando histórico. Ensaio precisa medir repetição/concorrência/rollback/restauração; RPO/RTO, alvo/frequência/retenção/automação continuam decisões externas. Nenhuma rotina configurada.

## Donos e decisões externas

Consolidação BE15 em06/10 no [pacote V1–V9](preparacao-tecnica-final.md): etapas/critério/evidência/dono para RPO/RTO/volumes, backup/cadeia/chaves, restauração isolada, evolução SQL/permissões/concorrência e contingência. DBA a identificar nominalmente por Lucas/TI; papel de referência não comprova nomeação. Retenção/frequência/RPO/RTO/volumetria não preenchidos. [Diagnóstico final](verificar-preparacao-final-local.ps1) e [relatório](evidencias/d19-preparacao-tecnica-final-2026-10-06.md) são arquivos, sem SQL/backup/RESTORE/JVM/build; artefato final ainda será recebido. Documento anterior copiado/preservado.

Na evolução V3→V4, ensaiar o UPDATE técnico já existente que inicializa revisao_conteudo a partir de versao: comparar linhas/versões antes/depois e preservar origem/quantidade/FIFO. Não foi executado nesta preparação. Falha entre lotes exige diagnóstico do estado confirmado/histórico antes de repetir, não suprimir backfill ou inventar dados para passar.

| Assunto | Dono de referência | Evidência esperada antes do uso real |
| --- | --- | --- |
| Banco, versão, collation, TLS, armazenamento e perfis | Lucas/TI e DBA | Alvo confirmado e ambientes separados, sem credenciais no relatório |
| Objetivo de perda de dados e prazo de recuperação | Lucas/TI/DBA com Caio e responsável | RPO/RTO acordados e tempo/perda medidos no ensaio |
| Backup, guarda, retenção e restauração | Lucas/TI e DBA | Cadeia verificável, cópia protegida e recuperação em destino isolado |
| Conciliação física e planilha de contingência | Caio/operação e Supervisor/Gestor | Quantidades/reservas conciliadas sem duplicidade |
| Documentos reais/regularização NOTAZZ/ESL | Natalina/Controladoria | Tratamento definido para documentos existentes; sem emissão nesta etapa |
| Coletor, impressão e rede | Mickael/TI | Equipamentos e cobertura aferidos no armazém |

Não inventar frequência, retenção, volume ou RPO/RTO. Dez usuários/quatro simultâneos são estimativas recebidas; a carga e a duração do ensaio precisam ser acordadas com a operação. O prazo desejado de piloto não é evidência de prontidão.

## Backup e restauração preparados

1. DBA define o modelo de recuperação a partir do RPO/RTO, capacidade e operação. Backup completo e eventual diferencial compõem a base; se usar FULL/BULK_LOGGED, definir também a cadeia de logs. SIMPLE não oferece backup de log nem recuperação até um instante arbitrário; BULK_LOGGED tem limitações adicionais. Conferir o cenário escolhido, sem mudar o modelo automaticamente. [Modelos de recuperação Microsoft](https://learn.microsoft.com/en-us/sql/relational-databases/backup-restore/recovery-models-sql-server?view=sql-server-ver17).
2. Registrar quem executa, quem verifica falha, localização protegida, acesso mínimo, retenção e recuperação das chaves/certificados necessários. Nenhuma cópia de produção ou segredo vai para este repositório.
3. Ensaiar recuperação em destino WMS isolado, identificado e autorizado, sem sobrescrever o banco operacional. Registrar conjunto de backups, sequência/instante recuperável e compatibilidade de versão. Não copiar permissões ou acesso externo inadvertidamente para o destino do ensaio.
4. Verificar legibilidade e integridade do backup, depois restaurar e conferir estrutura/dados. `RESTORE VERIFYONLY` não restaura nem verifica toda a estrutura dos dados; sucesso nesse comando não prova recuperação. [Microsoft VERIFYONLY](https://learn.microsoft.com/en-us/sql/t-sql/statements/restore-statements-verifyonly-transact-sql?view=sql-server-ver17).
5. Conferir integridade pelo procedimento do DBA, schema/histórico Flyway, IDs, quantidades, origens, datas, reservas, operações e auditoria. Abrir a aplicação apenas no ambiente autorizado e comparar os saldos/pedidos esperados com o estado restaurado.
6. Medir duração e perda/reconstrução real; reconciliar operações confirmadas e registros de contingência. Caio/Supervisor validam o físico e TI valida o ambiente. Evidência registra resultado, divergências, correção e aceite responsável; não marcar restauração concluída por existência do arquivo de backup.

## Matriz de ensaio SQL — execução futura

| Ensaio | Critério observável | Responsáveis |
| --- | --- | --- |
| Criação limpa e evolução por migration | Histórico/checksums corretos, constraints confiáveis, JPA `validate` compatível; dados antigos preservados | DBA, Cedro/Prumo por Farol |
| Banco/servidor incorretos | Procedimento recusa alvo antes de DDL; TLS não aceita certificado indevido | DBA/TI |
| Precisão, datas e Unicode | Quantidades/valores sem arredondamento silencioso, UTC/microssegundos e texto longo/acento preservados | Cedro/DBA |
| Constraints/índices filtrados | Quantidade/estado inválidos recusados; nulos opcionais permitidos; duas reservas ATIVAS impedidas, nova após reversão permitida | Cedro/DBA |
| Reserva/movimento/bloqueio simultâneos | Nenhuma promessa/posição duplicada, sem saldo negativo; conflitos claros e histórico coerente | Cedro, Caio e DBA |
| Falha e repetição | Falha na auditoria reverte efeitos; mesma operação recupera confirmação original; payload diferente conflita | Cedro/DBA |
| Saldo e duas posições | Unidade contada uma vez; físico = pendente + unitizado; unitizado = disponível + reservado + bloqueado | Cedro/Caio |
| Permissões e pool | DML autorizado funciona e excessos são recusados; opções JDBC/isolamento/timeout compatíveis | DBA/TI/Cedro |
| Queda e recuperação | Sem operação pela metade, backup restaurável e registros faltantes conciliados; RPO/RTO medidos | DBA/TI/Caio |
| Novos blocos de D19 | Separação/retirada/retorno, cálculo/ciclo/ajuste e contagem acrescentados apenas conforme contratos aprovados para implementação | Cedro/Caio/Farol |

O ensaio deve observar o SQL Server escolhido, inclusive deadlocks, timeout, escalonamento/ordem de locks, isolamento e planos das consultas reais. H2 não comprova esses resultados. Conflito não autoriza repetição automática com nova chave: consultar/reutilizar a operação original quando a intenção for a mesma.

## Falha de migration e recuperação

Antes da aplicação futura, guardar artefato, conjunto de scripts/checksums e backup recuperável. Na falha, suspender novas escritas, registrar etapa e erro seguro e inspecionar o histórico/estado real com o DBA. Não executar `repair`, `baseline`, `clean`, exclusão de tabelas, restauração ou nova migration automaticamente. O DBA e Farol definem repetição, correção incremental ou restauração em plano concreto, preservando dados e compatibilidade com a versão do backend. Restaurar banco e escolher artefato são ações distintas; downgrade de aplicação sem compatibilidade verificada não é recuperação demonstrada.

## Contingência e retomada

Na falha isolada do coletor, a atividade afetada aguarda equipamento funcional. Na indisponibilidade geral, Caio/Supervisor coordenam a planilha e uma lista única de unidades já comprometidas. O aplicativo inicial depende de conexão; não há sincronização automática offline proposta nesta entrega.

Cada fato da contingência deve registrar identificação estável, instante real, operador, cliente/armazém, pedido, nota/origem, unidade, produto/lote aplicável, quantidade/unidade de medida, origem/destino e motivo. Identificar o que ficou confirmado antes da queda e o que está pendente, sem fabricar documento, FIFO, estoque ou operação. Não guardar senha/token na planilha.

Após TI recuperar o serviço, conferir físico e estado persistido, separar duplicidade de fato novo e lançar pelos comandos operacionais autorizados, com a mesma chave para a mesma intenção. A planilha não concede liberação de quarentena, troca de reserva, baixa por XML ou ajuste sem permissão. Divergência que afete reserva sinaliza/bloqueia a saída até solução identificada. Correções são ajuste/reversão auditados; não editar tabelas diretamente para fechar saldo.

Supervisor/Gestor validam cada conciliação e registram conflitos restantes; Natalina trata documentos sem confundir presença física com emissão/cancelamento. A retomada exige evidência de quantidades/origens/reservas conciliadas e autorização operacional. Nenhum procedimento de contingência, backup ou restauração foi executado nesta preparação.

## Complemento de ensaio futuro V8 p2-origem

[Fonte/transcrição31](../database/contratos/v8-schema-doc31.json) e [procedimento V8](../database/migrations/README.md#v8--fechamento-versões-e-registros-externos-be13) preparados somente em arquivos. Cópias do freeze251/328 e outputs490/132 preservam o pareamento histórico; preparo p2-origem469/469 não é execução/aceite. JPA/guardas finais dependem de novo freeze. Antes de qualquer aplicação, DBA/TI confirmam alvo WMS/versão/opções de sessão, autorização, artefato/hashes e backup/restauração comprovados em destino isolado; não houve conexão nesta rodada.

Ensaio estrutural futuro confere FK cíclica criada após as tabelas, CHECKs confiáveis, múltiplos ajustes comuns com tratativa_origem_id NULL e recusa da segunda regularização para a mesma tratativa pelo índice único filtrado. Tipo/FK, par base anterior/JSON, JSON inválido e referência inexistente devem ser recusados. Observar criação e DML pelo pool com opções de sessão exigidas pelo índice filtrado e mínimo privilégio; nenhum resultado real foi observado.

Cedro/Vigia deverão comprovar TODOS os originados/todas versões, lista/destino/versão/valor esperados e atomicidade sob locks/replay. Anterior100−selecionada80=+20 conserva -20: se aplicado em B, não mover/desfazer, registrar +20 em C e manter pendência até sua aplicação comprovada. Exercitar também -20 VALIDADO, composição dos dois deltas, cadeia comum80→70=-10 sem incluir regularizações, hash repetido em outra versão, período incorreto antes do replay, auditoria identificada e falha de auditoria com rollback completo. Natalina/Controladoria conferem somente fatos externos conhecidos, sem emissão/cancelamento real.

Recuperação conserva versões/JSON/bytes/hashes, referências, dependências, ajustes/marcas/aplicações e resposta de repetição. Após falha, DBA/Farol inspecionam histórico e efeitos reais antes de decidir correção incremental ou restauração autorizada; não remover FK/índice, editar deltas aplicados, executar repair/clean ou compensar automaticamente. H2 e leitura de arquivos não comprovam dialeto, locks, opções de sessão, permissões, RPO/RTO ou recuperação SQL Server.

Reconferência final333/251:523/523 e163/163 somente arquivos, V1–V8 preservadas, [parecer](../database/evidencias/d19-v8-2026-10-06-p2-origem-final.md). JPA nullable unique e V8 filtrada foram comparados como representações da mesma chave; SQL emitido/semântica de NULL/opções JDBC não observados. DEFAULT Java/SQL e dois pareamentos @Check conferidos, sem executar26 CHECKs/ISJSON SQL Server. A preparação técnica não altera donos/autorizações do ensaio nem representa aceite de negócio/BE13 ou recuperação comprovada.
