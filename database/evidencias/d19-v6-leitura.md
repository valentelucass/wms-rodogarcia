# V6 — evidência estática de Prumo para Farol

05/10/2026. Somente leitura de contrato/models/serviços e escrita na área de Prumo. Nenhum SQL Server, Info/Validate, H2, build, fiscal/cobrança, commit/push ou Graphify foi executado/alterado. CLI/env Maestri próprio ausente: reporte por arquivo e ask próprio, sem identidade emprestada.

## Arquivos e conferência observada em 05/10/2026

- [V6](../migrations/V6__separacao_retirada_retornos_e_avaria.sql) e [procedimento](../migrations/README.md), conforme [contrato 27](../../docs/27-separacao-retirada-retornos-e-avaria.md) e complemento de pares/movimentos/reparo.
- [Comparador em leitura](../verificar-schema-v6.ps1) executado com saída 0; [resultado observado](d19-v6-jpa-sql.json) guarda os hashes do contrato/models/script. Não é execução JPA ou SQL.
- Oito tabelas, 72 colunas, uma extensão da unidade, 14 FKs, oito chaves únicas comparadas. Tipo/tamanho/nulos/precisão/escala/IDENTITY e alvo de FK compatíveis na leitura. FKs indexadas como primeira coluna. Dois enums de pedido/reserva coincidem com os CHECKs expandidos. String de estado das outras tabelas foi conferida contra o contrato em leitura.
- Auditoria mantém 25 ações anteriores e oito tipos; acrescenta 11 ações nos pares declarados. Movimento mantém cinco anteriores e recebe somente as três ações declaradas de avaria/reconhecimento/reparo. Leitura dos serviços `ExpedicaoService`, `AvariaService` e `RegistroDevolucaoService` confirma os destinatários; não são testes desses serviços.
- Reserva ATIVA e chave histórica por operação/unidade da V5 permanecem intactas. Ponte continua no pedido. Retirada única por pedido, baixa por retirada/reserva/origem, entrada nova de devolução única e fato por unidade/operação/tipo preservam histórico sem cascata.
- Índice filtrado da chave documental permite vários NULLs; documento cancelado não libera a identidade histórica. H2/JPA UNIQUE não comprova essa semântica no SQL Server.
- V1–V5 preservadas pelos cinco hashes da [baseline](d19-v1-v5.sha256), conferidos pelo [diagnóstico local](../../infra/evidencias/d19-diagnostico-local.json): 56/56 verificações de arquivos, sem ambiente, conexão ou build.
- [Conferência de arquivos/referências](d19-arquivos-e-referencias.json): UTF-8, links locais, JSON, sintaxe PowerShell e CHECK da V4 mantido, com única expansão SEPARACAO. Não comprova execução do SQL.

## Alinhamentos incorporados nesta leitura

SEPARACAO foi acrescentada à localização sem retirar condições da V4. `avaria_inicial_reparada` BIT NN DEFAULT 0 com CHECK da condição original AVARIADA e GO; `bloqueio_previo` BIT NN da avaria conserva bloqueio anterior. Versão `-1` de leitura após reparo é permitida apenas em LEITURA; snapshot anterior com data/JSON pode permanecer durante a releitura. Não se infere reparo de unidades antigas. Equivalência da baixa é referência não somável por linha; o fato registra a linha temporal e o parcial mantém equivalência integral.

## Alinhamento formal — snapshot após reparo

O contrato declara “Snapshot imutável após separar”. `SeparacaoSaida.exigirNovaLeitura()` conserva origens/conjunto/data e muda o estado para LEITURA; `ExpedicaoService.separar()` volta a chamar `SeparacaoSaida.separar()`, que atribui novamente esses três campos na mesma linha única por reserva. Cenário: separar → avaria → reparo/retorno à armazenagem → nova leitura/separação. A V6 permite os estados reais sem impedir o reparo, mas não torna o snapshot imutável por trigger ou FK.

Atualização formal recebida de Farol/Cedro neste ask e registrada no doc27: a versão `-1` invalida o snapshot para atendimento, preservando reserva/condição e exigindo releitura/reseparação. `separacao_saida` é estado corrente; cada ciclo anterior conserva origens/conjunto/datas no resultado imutável SEPARACAO_SAIDA de `operacao_saida` e na auditoria PEDIDO_SAIDA, com UUID/usuário/instante. A V6 está compatível com essa escolha e não introduz tabela/coluna extra para ciclos de separação. Após estabilidade, conferir DTO `posicoesOrigem`/`conjuntoOrigemId`, replay e teste de reseparação que preserva o resultado anterior, sem tratar snapshot invalidado como atendimento válido. Prumo não alterou backend nem converteu essa implementação em aprovação operacional das propostas AC.

## Complemento temporal/ciclo em 06/10/2026 — antes do freeze

Doc27 e trecho atual de `AvariaEstoque` lidos: `equivalencia_base DECIMAL(19,6) NOT NULL >=0` e `ciclo_id VARCHAR(36) NOT NULL`, ambos imutáveis no JPA (`updatable=false`). Incorporados à criação de `avaria_estoque` da V6 não aplicada, sem DEFAULT/backfill. O CHECK de quantidade/base positiva permanece; acrescentado CHECK de equivalência-base não negativa. O ciclo não é FK/UNIQUE: várias ocorrências da unidade podem compartilhar o mesmo ciclo.

Índice por `(unidade_id,id)` preservado conforme `AvariaEstoqueRepository`: histórico/abertas consultados por unidade, filtro de ciclo vigente no serviço. Não criar índice adicional por hipótese nem tornar ciclo único. A leitura pontual de `AvariaService` mostra uso do mesmo ciclo das abertas e restauração de bloqueio prévio filtrada pelo ciclo da ocorrência. `ExpedicaoService` escolhe retorno à quarentena por avaria/condição efetiva/bloqueio, conservando a condição original.

Bases pertencem ao instante/fatos comprovados: ocorrência anterior à retirada 100→50 conserva base 100; posterior usa 50. Ausência/cadeia divergente gera pendência `HISTORICO_AVARIA_INSUFICIENTE`, sem denominador inventado. O SQL não comprova reconstrução temporal, destino físico ou isolamento do bloqueio antigo; essa evidência depende dos serviços/testes em correção e revisão de Vigia.

O JSON de comparação de 05/10 registra a V6 anterior com 72 colunas; seu hash não representa esta revisão. Não foi sobrescrito nem promovido a comparação final. A V6 agora contém mais duas colunas; o comparador lê as colunas mapeadas sem lista fixa e será executado somente após freeze. Nenhuma suíte de aplicação foi executada por Prumo.

Conferência pontual observada de 06/10: saída 0, 14/14 verificações atendidas. Tipos/nulos dos dois campos, CHECK de equivalência não negativa, CHECK de quantidade preservado, ausência de DEFAULT/FK/UNIQUE de ciclo e índice atual preservado. Contagem SQL: oito tabelas/74 colunas, sem executar SQL ou comparar globalmente JPA. V1–V5 coincidem com os cinco hashes da baseline. Os cinco arquivos alterados foram lidos em UTF-8 com final de linha e 33 links locais existentes; nenhum bloqueio de leitura. SHA-256 da V6 desta revisão: `EE0EEBAF5D7EA5B7329C182039A274EBE46DAFCB561EAEF5DF8846381A897D39`. O arquivo de conferência documental anterior permanece histórico de 05/10; este resultado é específico do complemento.

## Validação externa e sequência

Reconferência de 06/10 registrada em [novo parecer datado](d19-v6-freeze-2026-10-06.md), output/manifesto separados: estrutura de 74 colunas compatível, 58/58 checks locais e V1–V5 preservadas. O freeze inicial foi reaberto por P2 temporal de Vigia (divisão/reagrupamento BE07 e base histórica). Revisão de código/aceite do bloco pendentes; reconferir contrato/hash após correção e novo freeze. Evidência inicial de 72 colunas permanece preservada, como resultado histórico.

Ainda não comprovados: dialeto/aplicação Flyway, constraints confiáveis no servidor, opções do pool, unicidade sob collation real, ordem de flush, permissões, locks/deadlocks/rollback e recuperação SQL Server. Cenários e donos estão no [documento 33](../../docs/33-preparacao-tecnica-local-backend.md), procedimento e [ensaio](../../infra/recuperacao-e-ensaio.md). Repetir a comparação quando Cedro estabilizar os models; mudança de hash exige identificar o que mudou.

Farol consolida o andamento/evidência central e encaminha o próximo schema, documento 29 para BE05/BE12 e depois 31 para BE13/BE14. Não há V7 ou estrutura posterior inventada nesta entrega. Preparação BE03/BE04/BE15 independente está disponível; donos externos continuam separados de código local.
