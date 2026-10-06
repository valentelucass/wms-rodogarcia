# V9 — complemento formal de pareamento, preparo local em06/10/2026

[Fonte31 copiada](d19-v9-preparacao-2026-10-06-pareamento-complementar-fonte-doc31.md), SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C. Trecho Pareamento complementar anterior a Java: ENTRADA_CONTINGENCIA em operação administrativa, auditoria PEDIDO_ENTRADA/ENTRADA_EFETIVADA; AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE. CHECK de serviço de cobrança admite ATIVO/ENCERRAMENTO_PENDENTE/INATIVO em VARCHAR24. Fonte integral preservada, nenhuma edição doc31/backend/centrais.

[V9 preparada](../migrations/V9__contagem_carga_contingencia_e_encerramento.sql) atual SHA17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8. [Cópia anterior](d19-v9-preparacao-2026-10-06-pareamento-complementar-antes-v9.sql), SHA518596FB9AEE595E54EDBE13A8E6A1837354EC050AD9F7845A297B6473123CF5. Única migration autorizada a atualizar nesta rodada; V1–V8 intocadas. SQL em arquivo não foi aplicado ou validado por SQL Server.

## Mudança e leitura

ENTRADA_CONTINGENCIA acrescentado só no CHECK operacao_administrativa.tipo, total38 tipos. Mantidos19 tipos/71 ações de auditoria e seus pares; nove ações movimento. Nenhum literal novo de auditoria/movimento presumido. AJUSTE_ESTOQUE continua fora dos tipos administrativos, APLICACAO_CONTAGEM continua admitido.

V7 já contém o domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO no CHECK ck_servico_cobranca_situacao. V9 reafirma esse conjunto por DROP/ADD WITH CHECK conforme complemento autorizado; tipo herdado VARCHAR24 mantido. Não houve coluna/tabela/enum novo, DML, default, trigger, grant, cascata ou alteração V1–V8.

[Transcrição derivada](../contratos/v9-schema-doc31.json), [leitor estático](../verificar-schema-v9.ps1), [matriz](../contratos/v9-matriz-cobertura.md) e [procedimento](../procedimento-v9.md) atualizados com histórico/cópias e fonte nova. O leitor ganhou11 verificações: cinco do domínio cumulativo do serviço e seis do complemento/tipo herdado/ação de entrada existente. [Output424/424](d19-v9-preparacao-2026-10-06-pareamento-complementar-output.json), zero divergências: oito tabelas/79 colunas/18 FKs/oito constraints UNIQUE/24 CHECKs de tabela/10 índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. Avaliação simbólica de pares é lexical, sem engine SQL.

Comando local na raiz WMS:

```powershell
./database/verificar-schema-v9.ps1 -FonteDoc31 database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar-fonte-doc31.md
```

[Metadados reais](d19-v9-preparacao-2026-10-06-pareamento-complementar-execucao.json): conclusão PowerShell normal, output integral; LASTEXITCODE null não é saída0 nativa. Fonte canônica e cópia coincidiram durante esse leitor; alterações posteriores de Cedro precisam novo registro/freeze, não mudança automática de hash.

## Preservação e limites

[Antes280 arquivos](d19-v9-preparacao-2026-10-06-pareamento-complementar-antes.json) e [cópias de documentos](d19-v9-preparacao-2026-10-06-pareamento-complementar-copias-documentos.json) guardam hashes/SQL/JSON/leitores/matriz/procedimento/documentos antes de editar. Fontes1D/outputs413/18 checks/manifesto250 e drafts antigos preservados. [Fecho integrado](../../infra/evidencias/d19-preparacao-tecnica-final-2026-10-06.md), [antes/depois](../../infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-final-depois.json), [checks locais](../../infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-final-checks-locais.json) e [manifesto novo](../../infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-final.sha256) registram o conjunto atual; manifestos antigos descrevem revisões anteriores preservadas em cópias.

[Preparo técnico BE03/BE04/BE15](../../infra/preparacao-tecnica-final.md) distingue baseline anteriorV9 da atual, mantém diagnósticos56/191/194/194 anteriores e novo194/194. Mínimos privilégios/inativação não são executados nem assegurados por GRANT genérico; serviço exige contexto/revalidação/replay/locks, a conferir no freeze e ensaio autorizados.

JPA/imutabilidade/guardas BE14 só após freeze e tarefa separada de Farol. Não é aceite BE14, homologação SQL Server ou prova de efeito físico/temporal. Nenhuma JVM/Hibernate/H2/SQL/Flyway Info/Validate/conexão/rede/ambiente/segredo/build/ETL/frontend/fiscal/cobrança real/publicação/Git executada. Artefato final ainda em construção; alvos/provedor/contas/backup/restauração/permissões/locks externos seguem com donos e autorização próprios. CLI próprio ausente conforme contexto; reporte neste ask/arquivos, sem identidade ou ambiente alheios, sem raw.
