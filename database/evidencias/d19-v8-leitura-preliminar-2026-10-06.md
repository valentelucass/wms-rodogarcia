# V8 — leitura preliminar do schema BE13, 06/10/2026

Prumo. **Somente leitura/planejamento. Preparação de migration suspensa até retomada por Farol após P2 temporal BE08 e revisão contratual.** A autorização mais recente de Farol prevalece sobre o convite anterior do doc31 para preparar V8. Nenhuma migration, contrato transcrito de V8, model Java ou SQL foi criado/executado.

Fonte relida: [doc31](../../docs/31-fechamento-contagem-e-contingencia.md), SHA256 `90F629BBA3A780FB194E33B1AD2B8EF5969B8DC06BBF8F3305413CC819B9C338`, capturaUTC 2026-10-06T05:12:10.2808906Z. É desenho/schema anterior ao Java e sujeito ao apoio/revisão Lume/Vigia; não é aceite BE13. V8 existente na pasta migrations: 0.

## Estrutura recebida, sem DDL

As dez tabelas do quadro BE13 são: fechamento_cobranca, versao_fechamento, dia_fechamento, fato_fechamento, ajuste_fechamento, ajuste_versao_fechamento, entrega_esl, confirmacao_externa_fechamento, referencia_nfse, resolucao_financeira_fechamento. O quadro usa id BIGINT IDENTITY, instantes DATETIME2(6), calendário DATE, valores DECIMAL(19,2), enums/códigos VARCHAR, textos NVARCHAR e JSON NVARCHAR(MAX)/ISJSON. As FKs são sem cascata; uniques preservam identidade de ciclo, dias por contexto, fato único, correção por origem/hash, tentativas de entrega e referências externas. Não há schema BE14 pronto nesse quadro; não presumir contagem/carga inicial/contingência/indicador ou novos campos financeiros.

O futuro leitor deve cotejar colunas/nulos/precisões/FKs/uniques/CHECKs/índices com o contrato estável e depois com JPA, preservar os hashes V1–V7 e não copiar automaticamente o erro de confundir tipo administrativo com ação de auditoria. DDL só depois de Farol retomar a preparação e encaminhar os ajustes exatos do contrato. JSON/hash/snapshots continuam sem preço/cutoff/tributo externo inventado.

## Enums e ações recebidos

Estados do fechamento/versão/externo usam VARCHAR24; os maiores literais recebidos NAO_EMITIDO_CONFIRMADO, FINALIZADO_SEM_EMISSAO e FINALIZADA_SEM_EMISSAO têm22 caracteres e cabem24. Natureza DEBITO/ZERO/CREDITO cabe VARCHAR8; VALIDADO/APLICADO cabe VARCHAR16; SALDO_CREDOR/SALDO_ZERO cabe VARCHAR24. Essa é apenas conferência de comprimento em texto, sem CHECK SQL preparado.

Tipo de auditoria proposto FECHAMENTO_COBRANCA tem19 caracteres; o doc31 informa18 por contagem manual. Cabe no VARCHAR24 existente, sem mudança estrutural necessária por esse detalhe; registrar a correção documental para Farol/Cedro na retomada. Nove ações propostas: PREPARACAO_FECHAMENTO, APROVACAO_FECHAMENTO, REJEICAO_FECHAMENTO, REABERTURA_FECHAMENTO, ENTREGA_ESL, CONFIRMACAO_EXTERNA, REFERENCIA_NFSE, RESOLUCAO_FINANCEIRA, AJUSTE_FECHAMENTO. Devem ampliar cumulativamente os tipos/ações e operacao_administrativa.tipo VARCHAR32 existente após o contrato confirmar os pares e literais efetivamente salvos pelo Java; não alterar ações V7 e não inferir movimento_estoque dessas ações. Nenhuma delas excede32 caracteres. O schema em planejamento não ganha comandos para emitir NFS-e ou enviar conteúdo ao ESL.

## Fronteiras a preservar na retomada

- Dia/fato pertencem à identidade do fechamento, não à versão; reabertura/rejeição não os libera para outro ciclo. UNIQUE protege repetição simples; integridade de contexto/intervalo e conjuntos completos permanece no serviço.
- FK individual não comprova que cliente/armazém/contrato/calculo/entrega/referência pertencem ao mesmo contexto. Contrato deve especificar a revalidação sob locks e o ensaio de concorrência/rollback posterior.
- Aprovação e resolução exigem cálculo completo/saldo conhecido. Colunas de estado/decisão são distintas de JSON/hash/valor original imutável; comparar mutabilidade e mínimo privilégio contra os models reais na retomada.
- Saldo e diferença assinados conservam DEBITO/ZERO/CREDITO; não aplicar CHECK>=0 a delta/saldo. Ajuste tem base cronológica e destino únicos sem reescrever versão emitida.
- Estado externo DESCONHECIDO não prova ausência de emissão; referências existentes/confirmacões/entregas são registros identificados, sem integração fiscal real.
- Calendário/corte29–31/bissexto/âncora, aplicação de ajustes e temporalidade do dano são regras de serviço a testar por Cedro/Vigia. P2 BE08 impede declarar a versão anterior encerrada ou aprovada.

## Procedimento e limites

O [procedimento](../migrations/README.md) e [doc33](../../docs/33-preparacao-tecnica-local-backend.md) já registram a suspensão V8/novo freeze P2. Na retomada: reler doc31 e complementos, esclarecer somente lacuna estrutural material, preparar migration/metadata/leitor em arquivos, comparar depois do freeze Java e publicar nova evidência sem sobrescrever esta leitura. Sem iniciar V8 enquanto a fonte estiver suspensa; BE14 aguarda quadro próprio.

Nenhum SQL Server inclusive Info/Validate, JVM/H2/build, fiscal/NFS-e/cobrança real, publicação/git/rotina/Hermes/ETL/frontend, segredos ou identidade/env alheios. Reporte próprio/artefato para check WMS - Farol; CLI Maestri próprio ausente. Leitura independente não aceita BE13 nem libera escrita Java.
