# V7 — preparação em arquivos de 06/10/2026

Prumo. **V7 preparada conforme doc29 atualizado, sem divergências na comparação contrato/SQL. JPA final aguarda freeze.** Não é migration aplicada, aceite BE05/BE12 ou aprovação de regra comercial. Farol informou aceite local BE10/BE11 após os outputs V6/Vigia; SQL Server permanece sem execução.

## Arquivos e resultado observado

- [V7 SQL Server](../migrations/V7__cadastros_servicos_e_calculo.sql): 15 tabelas/172 colunas, 26 extensões opcionais, 36 FKs sem cascata, 13 chaves únicas, 25 índices explícitos e 75 CHECKs nas tabelas novas. Sem preço/fiscal/corte real, DEFAULT/backfill, seed, DML ou grants.
- [Transcrição técnica do doc29](../contratos/v7-schema-doc29.json) e [leitor estático](../verificar-schema-v7.ps1): nomes/tipos/tamanhos/nulos/precisões/IDENTITY/FKs/chaves/enums/CHECKs/índices, domínios que cabem nas colunas e preservação de migrations/auditoria cotejados. A leitura manual da tabela integral do doc29 complementa essa transcrição.
- [Output datado](d19-v7-preparacao-2026-10-06.json): saída 0, **494/494 verificações locais**, sem divergências. Modo JPA não solicitado; backend ainda em escrita. O leitor só lê arquivos e não executa JPA, H2, SQL, Maven/Flyway ou validação de dialeto.
- [Manifesto de hashes](d19-v7-preparacao-2026-10-06.sha256): 17 arquivos iguais entre 2026-10-06T04:00:06.2003379Z e 2026-10-06T04:02:14.4593842Z. V1–V6 coincidem com baseline; outputs antigos de 72/74 colunas e p2-final preservados sem sobrescrita.
- [Procedimento V7](../migrations/README.md) e [matriz de permissões](../permissoes-minimas.md) atualizados. [Doc33](../../docs/33-preparacao-tecnica-local-backend.md) registra preparo/limites/próximo passo. Infra não precisou de mudança.

## Alinhamentos e limites

Foi encontrada incompatibilidade material no contrato inicial: `servico_cobranca.situacao VARCHAR(16)` não comportava ENCERRAMENTO_PENDENTE (21 caracteres). Reportada no ask próprio; Farol confirmou VARCHAR(24), Cedro formalizou e Prumo reconferiu doc29 completo. SQL/transcrição usam 24, preservando enum e padrão cadastral. Nenhuma alteração de backend/contrato central por Prumo.

Auditoria passa de oito para 14 tipos e de 36 para 47 ações, conservando os anteriores e o CHECK de pareamento V6. Pares V7 seguem doc29; CRIACAO existente é aceita para serviço/tabela e na operação administrativa. Alvo/tipo contextual fazem parte do hash. Nenhuma dessas ações foi adicionada a movimento_estoque. A importação cadastral não cria ocupação/movimento físico.

Minimo/GRIS aplicáveis exigem parâmetros; nos outros modos esses parâmetros são NULL. Categoria sem tarifa diferenciada é vazio explícito fornecido pelo serviço, sem DEFAULT. Item tem preço OU percentual; correspondência com catálogo é entre tabelas e fica no serviço. Vigência fim NULL ou maior que início; sobreposição/contexto/fronteira são revalidados sob locks. Fato deduplica execução pela chave canônica; UUID HTTP não basta. Rateio divide cotas/notas e valor em centavos sem replicar total; CHECK/FK não provam soma/correspondência.

Marco de avaria conserva par único avaria/fato e quantidade afetada até a base; mesma unidade, instante, continuidade, origem e Gestor requerem serviço/revisão. Cálculo COMPLETO exige configuração/componentes/total e array de pendências vazio. JSON/erros vazios permitem os espaços JSON usuais; isso não comprova ausência de uma pendência financeira omitida pelo serviço. Snapshots imutáveis exigem JPA/permissões/serviços coerentes; não há trigger nem permissões aplicadas.

Apontamento Lume/Vigia recebido por Farol: memoria_diaria continua UNIQUE(calculo_id,data), com segmentos/picos/valores por regra/categoria. A coluna tarifa já opcional fica NULL nas múltiplas regras conforme complemento do doc29; nenhuma nova tabela/coluna presumida. GRIS DIARIA sem dupla proporção e agregado temporal de avarias inconsistente como pendência são assuntos de serviço/contrato. Cedro/Vigia conferem memória/cálculo/testes; esta preparação não os aceita nem os testa.

## Hashes principais

- V7: `6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328`
- Doc29 reconferido: `BC913E6C42D42A1605734287BE71A71C70BB053C842328C946434BA42F3EA573`
- Transcrição técnica: `6B3629DD8E779B7B8D88B03735C4CCF815C93AD58C1BD5DEE946ADF92105EB92`
- Leitor estático: `7F3BE06A9605BDF46DAD15B9B2EB4F15AE88815042EEB2BDBBBC1817C3EC9891`

Hashes V1–V6 e evidências protegidas estão no manifesto/output, separados de checksum/histórico de execução Flyway.

## Pendências e reporte

Aguardar freeze de Cedro/Farol para reconferir JPA final e qualquer ajuste formal de schema. Modo `-CompararJpa` é somente leitura e ainda não foi executado; guardar output posterior sem sobrescrever esta evidência. V8/schema31 não foram antecipados.

Alvo/versão/collation/TLS/identidades/permissões reais, planos/locks/pool/constraints e recuperação exigem ensaio SQL Server externo autorizado. H2/build de Cedro não executam estes arquivos; Prumo não executou SQL Server (nem Info/Validate), migration, H2 ou build, não criou cobrança/fiscal real nem iniciou publicação/git/rotinas/Hermes/ETL/frontend. V1–V6, backend/BE08/contratos/centrais/Graphify preservados.

Reporte neste ask próprio/arquivos para WMS - Farol realizar check com seu ambiente. CLI maestri fora do PATH e MAESTRI_CLI próprio ausente: list/mensagem não executados; sem identidade/env de terceiros ou raw.
