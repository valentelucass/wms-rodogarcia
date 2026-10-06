# V9 BE14 — procedimento de preparação e ensaio futuro

## Freeze atual p2-locks367 — 06/10

Nova tarefa formal recebida: manifesto backend/evidencias/d19-bloco4-freeze-p2-locks.sha256 SHAA257476… e fonte31SHA0C57C902…; fontes copiadas/hash antes/depois. [Parecer/outputs904/129](../evidencias/d19-v9-2026-10-06-p2-locks-final.md) conferidos somente por arquivos; contrato aponta fase autorizada/hash exato, sem DDL/model/literal novo. V1–V9 preservadas. Tentativa128/129 e transcrição/leitores anteriores copiados antes de ajuste. Gates recusam manifesto/fonte diferente ou mudando; autorização do parâmetro depende da tarefa explícita, sem selecionar outro freeze automaticamente.

Comandos executados nesta tarefa: verificar-schema-v9.ps1 -CompararJpa -ManifestoFreeze backend/evidencias/d19-bloco4-freeze-p2-locks.sha256 e verificar-v9-freeze-suplementar.ps1 com o mesmo manifesto. Outputs/metadata em sufixos p2-locks-final; LASTEXITCODE null não é saída nativa0. Nenhum SQL/Flyway/JVM/H2/build; procedimento de ensaio/privilegios abaixo continua futuro. Vigia semântica/Farol aceite. Se fonte mudar após esse freeze, bloquear e aguardar novo contrato/tarefa; não inferir DDL. Seção360 seguinte é histórico retido, substituída apenas por esta nova tarefa.

## Dependência vigente — freeze360 histórico/P2 locks

[Leitura histórica904/122 e correções do parser](../evidencias/d19-v9-freeze-2026-10-06-final-historico-p2-locks.md) não aceitam BE14: Farol/Vigia07:27 retiveram por prelock CONTAGEM raiz versus DTO aninhado. Leitores correntes bloqueiam antes de Java enquanto a transcrição marca HISTORICO/AGUARDA_NOVA_TAREFA. Fonte31 capturadaB459 e manifesto360/289 preservados; não trocar pela fonte em escrita nem pelo manifesto antigo sem nova tarefa formal p2-locks. SQL V9 intocado SHA17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8, V1–V8 intocadas. Não presumir DDL novo.

Após nova tarefa: copiar/hash fonte/leitores/transcrição atuais; reconciliar fonte formal completa, configurar manifesto exato/fase autorizada; coletar novos outputs/metadados sem sobrescrever904/122/tentativas; comparar hashes antes/depois e informar limites/achados. Comparação/suplemento não foram repetidos depois do aviso recebido neste ask. Nullable/filtro/ISJSON/CHECK/H2 mantêm limites do ensaio SQL Server abaixo; nenhuma execução SQL/Flyway/Hibernate/JVM/H2/build.

## Suporte posterior ao307 aceito — aguarda freeze BE14

[Protocolo de comparação JPA e suplemento](../contratos/v9-leitor-jpa.md) preparado somente em database: leitor -CompararJpa/-ManifestoFreeze, parser textual e suplemento de imutabilidade/guardas. Oito models/carga_id/legado/literais cumulativos serão conferidos somente após mensagem explícita de freeze. Nesta preparação não rodar modo JPA ou suplemento, não declarar compatibilidade; apenas AST/JSON/regex/links/hashes e fixtures fictícias. Leitor/contrato/matriz/procedimento anteriores copiados; V1–V9 e outputs424/194/22/307 preservados. [Evidência da preparação](../evidencias/d19-v9-leitor-jpa-preparo-2026-10-06.md).

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](../evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Fonte [doc31](../../docs/31-fechamento-contagem-e-contingencia.md), rodapé BE14/complementos e delta final SHA52D6071C…, copiados em [evidências](../evidencias/d19-v9-preparacao-2026-10-06-fonte-doc31-vigencia.md). [V9](../migrations/V9__contagem_carga_contingencia_e_encerramento.sql), [transcrição](../contratos/v9-schema-doc31.json) e [matriz](../contratos/v9-matriz-cobertura.md) são preparação derivada. Farol informou aceite LOCAL BE13/333; essa revisão não foi repetida.

Complemento996750 formalizou os residuais antes do Java: resolver-cancelamento com Gestor/prova integral/zero e entrada histórica conservada; resolucao_remanescente.carga_id opcional/FK/índice e nome final efeitoRegistradoNoWms true só VINCULAR/false reconstrução temporal. O quadro e o complemento são cumulativos: PREPARADA e REMANEJAMENTO constam no complemento, carga_id também. Sem literal/tabela extra. Arquivos/drafts383/395/residuais copiados antes das alterações; preparação estrutural alinhada à fonte, JPA BE14 só após freeze explícito.

Na leitura posterior52D607, encerramento de vigência também audita CONTRATO_COBRANCA; literal ENCERRAMENTO_VIGENCIA já existia. Ampliar o CHECK financeiro nos dois sentidos desse par, sem coluna/tabela/literal adicional, preservando TABELA/vínculo e demais pares históricos. Fonte996750/407 foi copiada antes; o output407 registra a mudança da fonte canônica durante a execução, sem ocultar esse limite.

Fonte final desta preparação1D6413AF…: [cópia](../evidencias/d19-v9-preparacao-2026-10-06-fonte-doc31-solicitacao.md) e [delta](../evidencias/d19-v9-preparacao-2026-10-06-fonte-diferencas-solicitacao.json). Solicitar encerramento por Gestor nos sete cadastros acrescenta literal exato SOLICITACAO_ENCERRAMENTO em auditoria/admin, sem coluna/tabela/enum. CHECK financeiro permite serviço/solicitação; não remover SOLICITAR_ENCERRAMENTO histórico ou qualquer par anterior. Output413/413;410 e15/16 de arquivos preservados antes da correção da fonte corrente.

## Conferência autorizada agora

Na raiz WMS, o comando só lê arquivos e emite JSON:

```powershell
./database/scripts/verificar-schema-v9.ps1
# Fonte literal preservada, identificada no output:
./database/scripts/verificar-schema-v9.ps1 -FonteDoc31 database/evidencias/d19-v9-preparacao-2026-10-06-fonte-doc31-solicitacao.md
```

Leitor não usa ambiente/segredos/JDBC/Flyway, não conecta banco e não inicia JPA/JVM/H2/build. Compara campos/tipos/nulos/precisões/FKs/chaves/CHECKs/ISJSON/índices, preservação V1–V8, literal da fonte e tabela simbólica do subconjunto AND/OR/igualdade/diferença/IN dos pares tipo/ação. Não executa SQL. Preparação concluída no output é somente estrutural; não equivale a JPA/código/regra validada, aceite BE14 ou SQL Server homologado.

Guardar output/metadados em arquivo novo, nunca redirecionar sobre evidência existente. Registrar início/fim/comando e hashes imediatamente antes/depois, `$?` e LASTEXITCODE observado logo após invocação. LASTEXITCODE nulo em script PowerShell normal não prova saída nativa0; não confundir código do processo externo com código nativo do leitor. Registrar falha de parser/reader como tal, preservando arquivo/output antes da correção; não é falha SQL ou Java. [Parecer](../evidencias/d19-v9-preparacao-2026-10-06.md) registra resultados e limites.

Antes da comparação final, Cedro/Farol fornecem schema e freeze/manifesto BE14 estáveis. Ampliar o leitor às anotações dos oito models, enums, mutabilidade, literais realmente persistidos e repasses, sem iniciar Hibernate. Conferir todos os tipos/ações administrativos cumulativos, AJUSTE_ESTOQUE só movimento/auditoria e saída/inativação. Revisão de guardas/rollback/replay/histórico é de Vigia/Cedro; nenhum parecer estrutural aceita sozinho negócio.

## Execução SQL futura com autorização específica

1. Lucas/TI/DBA definem alvo WMS isolado, versão/collation/TLS/identidades, permissões reais e backup/restauração comprovados. Procedimento geral e ferramenta em [migrations/README](../migrations/README.md). Nenhum Flyway Info/Validate está autorizado agora.
2. Confirmar hashes V1–V9 e sequência incremental/histórico, constraints existentes confiáveis e pares cumulativos. V9 não preenche dados antigos, estoque, preços ou parâmetro comercial. WITH CHECK revalida domínios; ensaiar custos/locks/permissões na evolução, sem assumir resultado pela leitura.
3. `entrada_id` opcional tem UNIQUE filtrado WHERE NOT NULL, não UNIQUE comum. Conferir sessão criação/DML: ANSI_NULLS, QUOTED_IDENTIFIER, ANSI_PADDING, ANSI_WARNINGS, CONCAT_NULL_YIELDS_NULL, ARITHABORT ON e NUMERIC_ROUNDABORT OFF constam somente do arquivo. Índice nullable JPA pode não expressar filtro; não substituir SQL ou gerar DDL automaticamente.
4. Conferir identidade global VARCHAR200/normalização/hash segundo collation definida, sem nome/collation inventados. ISJSON não prova estrutura, bytes/hash, lista completa, referências/contexto ou regras. FKs são simples e indexadas; não substituem escopo/locks/somas/imutabilidade.
5. Ensaiar preparo concorrente/replay/leitura de todas etiquetas, reserva×contagem×retirada e novo compromisso×inativação sob locks compartilhados. Dependência ausente/materialização/ciclo e fora de ordem; VINCULAR comprovado/EXECUTAR temporal com história suficiente; falta de prova mantém pendência, sem mudar data/hash do comando antigo.
6. Auditoria tardia precisa reverter efeito/marca/UUID/resposta juntos; contagem zero fecha ocupação/permanência sem retirada fiscal. Resolver-cancelamento PREPARADA exige todas origens/unidades zero, sem ocupação/reserva ativa/unitização pendente e pedido RETIRADO integral ou contagens APLICADAS a zero comprovadas. entrada_id permanece em CANCELADA; falha audita reverte só a transição/chave, preservando resolução já existente. Vínculo carga_id autoriza só o pedido identificado/reservado após leitura exaustiva de etiquetas, não disponibilidade comum. Validar guardas reais no freeze, não pela situação isolada.
7. Medir restauração e conciliação com operação; recuperação não limpa notas, movimentos, revisões ou cobranças. Correção por operação identificada ou nova migration incremental após diagnóstico e autorização. Sem down destrutivo automatizado.

[Permissões](permissoes-minimas.md), [recuperação/contingência](../../infra/recuperacao-e-ensaio.md) e [doc33](../../docs/33-preparacao-tecnica-local-backend.md) mantêm donos externos e limites. Não executados: SQL/Flyway/conexão/Hibernate/JVM/H2/build, concessões/rotinas, fiscal/cobrança real, frontend, publicação ou Git. H2 de Cedro é evidência local distinta de scripts, dialeto/locks/permissões ou homologação SQL Server.
