# Preparo técnico BE03/BE04/BE15 — fecho local em06/10/2026

## Fecho com complemento formal V9

Durante este preparo Farol/Cedro comunicaram a fonte31 SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C. [Pareamento/SQL/transcrição/leitor V9](../../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md):424/424, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/10 índices comuns/um único filtrado e seis CHECKs cumulativos substituídos mais um novo de pares. ENTRADA_CONTINGENCIA só no tipo administrativo; ação auditoria PEDIDO_ENTRADA/ENTRADA_EFETIVADA preservada; AJUSTE APLICACAO_CONTAGEM/AJUSTE_ESTOQUE sem literal adicional. VARCHAR24 do serviço mantido, DROP/ADD WITH CHECK reafirma os três estados já presentes em V7.

[Antes do complemento](../../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar-antes.json) e cópias preservam V9 SHA518596FB…/transcrição/leitor/matriz/procedimento e este parecer/pacote/baseline técnicos. V9 atual SHA17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8 é atualização preparada autorizada, sem execução. V1–V8,413/manifesto250 e diagnósticos56/191/194/194 anteriores intocados. Não declarar toda V9 imutável nem tratar os hashes/documents antigos como atuais.

[Diagnóstico técnico posterior ao complemento194/194](d19-preparacao-tecnica-final-2026-10-06-diagnostico-complemento-v9.json) e [metadados reais](d19-preparacao-tecnica-final-2026-10-06-execucao-complemento-v9.json) usam baseline SQL atual e preservam os outputs anteriores. Leitura da V9 e técnico é lexical/estrutural, não JPA BE14, execução SQL ou aceite de negócio. O fecho194 abaixo é histórico anterior ao complemento; o novo output integral é o resultado atual. LASTEXITCODE null continua sem promessa de saída nativa0.

As dez fontes técnicas ficaram estáveis durante a leitura. Nove também coincidem com a baseline inicial; backend/pom.xml mudou externamente de B25C5E149B740305B71EC3B13ECB731D804903AD996DD48D0293D69DCF01AA4C para4E279E66CE1D3A640A9DE5DD152861F7F6A7FFC84C9135165A31E0B935B7D030 antes do último leitor. Ambos os hashes ficam registrados, sem atualizar automaticamente a baseline ou editar backend. Os controles estáticos conferidos seguem194/194; esse resultado não avalia as demais alterações de Cedro, dependências em execução ou artefato final. Sem presumir freeze global.


[Diagnóstico de fecho194/194](d19-preparacao-tecnica-final-2026-10-06-diagnostico-fecho.json), sem divergências, inclui56/56 verificações originais. Trata apenas arquivos/fontes/inventário/planos e preservação; não comprova artefato final, JPA BE14, permissões/locks/dialeto SQL Server, provedor/conta real, restauração ou homologação externa. Artefato final segue em construção por Cedro. Farol já recebeu V9/fonte413/checks18/manifesto250; revisão333 não repetida. JPA BE14 será tarefa separada após freeze.

## Entrega

| Arquivo | Resultado local |
| --- | --- |
| [Pacote integrado](../preparacao-tecnica-final.md) | Plano V1–V9, alvos/segregação/contas/JWT/ensaios/backup/restauração/contingência e donos/pendências externos, sem parâmetros inventados |
| [Novo diagnóstico](../verificar-preparacao-final-local.ps1) / [contrato/baseline](../contratos/preparacao-final-local.json) | Leitura com caminhos fixos, hashes V1–V9,56 original preservado, inventário64 tabelas/documentos, controles declarados e estabilidade das dez fontes técnicas |
| [Configuração externa](../configuracao-externa.md) | Perfis/variáveis só por nome; ciclo de conta/renovação/revogação/rotação/indisponibilidade; sem fornecedor/conta/credencial coletados |
| [Permissões](../../database/permissoes-minimas.md) / [migrations](../../database/migrations/README.md) |64 tabelas citadas; aplicação/migration/DBA/consulta separadas, histórico mínimo, SETs/NULL filtrado/SQL emitido a ensaiar; nenhum grant executado; somente V9 preparada atualizada pelo complemento formal |
| [Recuperação](../recuperacao-e-ensaio.md) / [doc33](../../docs/33-preparacao-tecnica-local-backend.md) / READMEs database/infra | Etapas/evidências/donos externos e limites separados, sem afirmar prontidão do artefato em construção |

V1–V8 SQL permanecem byte a byte. V9 preparada foi atualizada pelo complemento formal autorizado com cópia/hash anteriores: conjunto de nove arquivos,64 tabelas distintas. Cobertura textual de permissões não prova privilégios efetivos ou regra de UPDATE por estado/alcance. Os mapeamentos/DDL/grants efetivos, transações/concorrência e restauração pertencem ao ensaio real futuro e à revisão backend estável.

## Achado estático e correções do leitor

O [primeiro resultado191/194](d19-preparacao-tecnica-final-2026-10-06-diagnostico.json) e [metadados](d19-preparacao-tecnica-final-2026-10-06-execucao.json) permanecem: LASTEXITCODE observado1, três checks falsos. Dois eram limitações de regex com texto acentuado. O terceiro refutou a premissa ampla de ausência de DML: V4 já contém `UPDATE wms.unidade_logistica SET revisao_conteudo = versao`, depois das colunas/lote GO e antes dos CHECKs de revisão/medidas. É backfill técnico preexistente; inventariado exatamente, sem executá-lo/suprimi-lo ou editar V4. DBA deverá ensaiar linhas/versões/custos/locks/lotes/transação/histórico na evolução; nenhuma quantidade/nota/FIFO/preço ou fato fictício criado.

[Leitor inicial](d19-preparacao-tecnica-final-2026-10-06-leitor-inicial.ps1), [contrato inicial](d19-preparacao-tecnica-final-2026-10-06-contrato-inicial.json) e [pacote inicial](d19-preparacao-tecnica-final-2026-10-06-pacote-inicial.md) copiados antes de corrigir só o diagnóstico/documentação. [194/194 após correção](d19-preparacao-tecnica-final-2026-10-06-diagnostico-final.json) também preservado; fecho separado após atualização documental. Não converter falha de premissa/regex em falha de Java/SQL, nem checks estáticos em resultado SQL executado.

## Comando, metadados e preservação

Comando autorizado executado na raiz WMS: `./infra/verificar-preparacao-final-local.ps1`. Ele executa apenas leitura PowerShell, inclusive o diagnóstico56 conhecido/hash-preservado; POM com DTD/entidade externa seria bloqueado antes do parser original. Não invoca processo JVM/Maven/Flyway/H2, conexão, URL/TLS/JWK/status, ambiente ou segredo. Bloqueio retorna código seguro sem raw/exceção/conteúdo. [Metadados de fecho](d19-preparacao-tecnica-final-2026-10-06-execucao-fecho.json): saída integral/PowerShell normal/LASTEXITCODE null; não declarar saída0 nativa. Original56 também concluiu com LASTEXITCODE null nesta leitura; seu script/output históricos não foram sobrescritos.

[Antes251 arquivos](d19-preparacao-tecnica-final-2026-10-06-antes.json), [depois](d19-preparacao-tecnica-final-2026-10-06-depois.json), [checks locais](d19-preparacao-tecnica-final-2026-10-06-checks-locais.json), [metadados consolidados](d19-preparacao-tecnica-final-2026-10-06-metadados.json) e [manifesto de fecho](d19-preparacao-tecnica-final-2026-10-06.sha256) permitem check de Farol. Os sete documentos modificados já constavam de manifestos anteriores; foram copiados literalmente com hash antes da edição. Manifestos antigos ficam intactos e identificam a revisão anterior/cópias, não os documentos atualizados. Todas as evidências/fontes/drafts/outputs antigos permanecem; nenhum novo texto foi gravado no backend/contratos/estados/centrais/Graphify.

Hashes do leitor/contrato/diagnóstico de fecho e dos nove SQLs constam do manifesto e dos outputs. Dez fontes técnicas ficaram estáveis durante a leitura final; hashes antes/baseline/depois são observações dessa preparação, sem presumir freeze global de Cedro.

## Riscos/donos e limites

Lucas/TI define ambiente/identidade/hospedagem e nomeia DBA. DBA efetiva privilégios/alvo/TLS/versão/collation/índices/permissões/sessão/backup/restauração com autorização específica; ainda precisa ser identificado nominalmente. Caio/Supervisor/Gestor conciliam físico/compromissos e função/alcance; Gestor/comercial fornece parâmetros; Natalina/Controladoria responde por documentos/fiscal; Mickael/TI equipamentos/cobertura. Referências não foram usadas como identidades de execução.

JWT já emitido pode conservar alcance antigo; duração nominal15min/guarda de emissão futura na fonte não prova revogação imediata, janela efetiva/cache/rotação/TLS ou provedor disponível. Login/renovação/revogação/contas reais e contenção exigem decisão/ensaio externos. Wrapper migrate.ps1 conecta mesmo em Info/Validate e não foi executado; proteção de formato/confirmed-target do backend não equivale à identidade real do servidor. VERIFYONLY não é restauração; perda/tempo/backup/cadeia/chaves e conciliação precisam ser medidos em destino autorizado. Contingência não permite inventar origens/instantes nem duplicar efeito WMS já existente.

Não foram inventados RPO/RTO, frequência/retenção/volumetria/provedor/credenciais/preços/cutoff/fiscalização. Não executados: rede/valores de ambiente/segredos/SQL Server/Info/Validate/Flyway/JVM/Hibernate/H2/build/backup/restore/grants/rotinas/publicação/Git/fiscal/cobrança/FE. H2 e testes anteriores de Cedro são outra evidência, sem equivalência com scripts, locks, collation, filtros, permissões ou recuperação SQL Server.

Próximo passo local: receber freeze e tarefa JPA BE14 separada de Farol, depois artefato final/integrado com evidência Cedro/Vigia/Farol. Donos externos recebem procedimentos concretos para decisões/ensaios futuros, sem usar ausência de dados para bloquear preparo independente ou autorizar conexão. CLI próprio ausente conforme contexto; retorno neste ask/arquivos, sem ambiente/identidade alheios ou mensagem por raw.
