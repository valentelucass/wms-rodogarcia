# FE05-DATA-PESO01 — calendário e quantidades no recebimento

Pedido de Lucas em 13/10/2026: escolher data e horário da chegada física por calendário e apresentar `1820.000000` KG como `1.820,00`.

## Comportamento

Campo compartilhado para `Instant` usa `datetime-local`, com calendário nativo, teclado, label, obrigatoriedade, bloqueio e descrição acessível. A escolha representa horário de Brasília (`America/Sao_Paulo`) e é convertida para instante UTC explícito na requisição. O fuso do computador não decide o fato. Exemplo: `13/10/2026 08:45` envia `2026-10-13T11:45:00Z`. Nenhuma data/hora real é preenchida automaticamente. Datas inválidas e horários inexistentes na mudança histórica de fuso são recusados. Instante já carregado conserva seu valor e precisão originais até uma edição; o controle nativo permite edição até milissegundos. Os demais formulários com o mesmo tipo usam o mesmo componente.

Quantidade exibida usa ponto nos milhares e vírgula nos decimais. KG tem pelo menos duas casas: `1820.000000` → `1.820,00`; `-1820.000000` → `-1.820,00`; `12.5` → `12,50`. Precisão adicional significativa permanece visível, sem arredondamento: `0.125000` → `0,125` e `0.000001` → `0,000001`. Contagens inteiras permanecem inteiras. Formatação trabalha com lexemas exatos, sem conversão para Number, preservando valores acima de 2^53.

Aplicado ao resumo/conferência, tabela de itens, quantidades da prévia XML e pesos dos volumes da nota. SKU, IDs, valores do contrato, documentos e cálculos não são alterados. Colunas numéricas alinhadas à direita, sem quebra no número, com algarismos tabulares. Tabela mantém rolagem própria no celular. Orientação de data/fuso centralizada em `frontend/src/content/information.ts`, conforme AGENTS.md; padrões 15.3, 24 e 34 do design system.

## Validação e versão em execução

23 testes novos cobrem o seletor, conversão/virada de dia/fuso histórico, obrigatoriedade/bloqueio, precisão original, ausentes, frações, negativos e números grandes. Chrome com locale pt-BR e computador em UTC confirmou escolha local, envio UTC e peso exato; desktop e celular 390 px, claro/escuro, sem erro JavaScript e sem overflow da página. Campo mobile tem altura de 44 px. Jornada do navegador usa transporte fictício explícito em memória, sem backend/SQL.

Regressão completa: **619 testes em 61 arquivos, sem falhas**; tipagem, lint e build final aprovados. Resultados e lint no recibo `orchestracao/.runtime/receiving-presentation-20261013/resultado.json`, sem somar fases. A primeira tentativa de build falhou por falta de memória da VM; navegador e servidor de exercício foram encerrados antes da repetição, com limite local de heap de 512 MB. Capturas e logs locais na mesma pasta. Fonte implementada e build local não comprovam publicação: esta demanda não reiniciou DEV/PROD nem alterou banco. Atualização operacional usa `pm2-wms.bat atualizar` no terminal administrador, com o procedimento [PM2](../45-producao-pm2.md).
