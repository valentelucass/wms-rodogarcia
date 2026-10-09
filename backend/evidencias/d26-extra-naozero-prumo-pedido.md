# D26 — SELECT focal do extra financeiro após corte real

Novo extra por HTTP no JAR412/SHA524FBE7E…0DC2195, somente cliente15/armazém7. Usar a rodada com extraClockUTC/extraCiclosIntermediarios/extraCalculoAtual/extraFechamento no publisher d26-persistencia-pedido.json; JSON completo preservado. Não refazer a fotografia236/0 histórica nem outras famílias.

O relógio real passou07/10 UTC; contrato3 encerra07/10, contrato4 começa07/10. Fato3 (quantidade4×tarifa5) foi anulado explicitamente antes do fechamento em D265CE7E9F0. O snapshot cálculo5=20 permanece histórico; recálculo deve refletir a situação atual. Não reativar fato, trocar Clock ou inventar execução anterior para produzir20.

Recorte somente SELECT WMSDEV/DEV após fase concluída: confirmar fato3 ANULADO; cálculo5/memória20 preservados; novo cálculo06→07 e exclusão da memória do fato anulado; cortes intermediários legítimos derivados do fim28/09 até06/10 sem duplicar dias, seus IDs/operacoes/revisões/auditorias; fechamento06→07/versão/demonstrativo/saldo e aprovação ou recusa CALCULO_DESATUALIZADO segundo HTTP. Se recusado, EM_REVISAO sem decisor/aprovação/entrega/NFS-e, replay da preparação único e negativas sem efeitos. Conferir somente IDs novos e invariantes de referências antigas necessários ao extra.

Sem DML/DDL/grants/PROD/reset/observador administrativo ou nova suíte. Resultado APROVADO20 só se efetivamente obtido; guarda concreta da anulação é bloqueio do caso positivo, não conclusão fictícia. Finais anteriores arquivados em d26-preextra-naozero; suas fontes SQL236/0 e builds permanecem intactas.
