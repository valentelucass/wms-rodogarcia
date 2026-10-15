# TAB-EDIT01 — edição das tabelas de cobrança, 15/10/2026

Pedido expresso de Lucas: editar tabelas já criadas, incluindo valores e inclusão/remoção de serviços, e retirar a etapa lateral duplicada “Vínculos por cliente”. O pedido autoriza a implementação; não representa aplicação de migration, grants ou publicação.

## Comportamento entregue

“Tabelas e vigências” apresenta o botão Editar para Gestor e carrega os dados consultados da tabela. O formulário permite alterar descrição, cliente, armazém, tipo, vigência e itens (serviço, categoria, preço ou percentual). Valores decimais e identificadores mantêm a precisão. Código e ID da tabela permanecem estáveis. A tabela deve conservar ao menos um serviço.

A navegação passa a quatro etapas. A manutenção dos vínculos permanece dentro de “Tabelas e vigências”, na visão “Vínculos das tabelas”, sem etapa lateral própria. Consulta, inclusão, encerramento e histórico do vínculo continuam disponíveis no alcance autorizado. Os seletores de serviço apresentam descrição e código.

O novo `PUT /api/v1/tabelas-cobranca/{id}` exige Gestor, alcance, revisão atual, operação identificada e motivo. O serviço valida novamente contexto, vigência, duplicatas após normalização, serviço ativo e compatibilidade entre preço/percentual e unidade de cobrança. Cliente e armazém precisam estar ativos. Tabela encerrada não recebe edição por este comando.

## Proteção do histórico

Escolha de implementação para atender à preservação financeira: um item removido ou substituído fica inativo; seu ID, serviço, categoria, preço e percentual não são sobrescritos nem apagados. Um novo valor cria outro item ativo. Itens sem alteração conservam seus IDs. Consulta da tabela e cálculos novos usam somente itens ativos; memórias antigas continuam ligadas aos itens originais. Salvar não recalcula períodos nem muda fechamentos.

Cliente, armazém e tipo podem ser corrigidos enquanto não houver vínculo ou cálculo. Depois disso, a mudança exige outra tabela para o novo contexto. Vigência já utilizada em cálculo não é reescrita; vigência sem cálculo precisa continuar abrangendo todos os vínculos existentes. O encerramento de vigência mantém seu comando e suas proteções anteriores.

A transação bloqueia clientes em ordem, depois armazéns e tabela; revalida a revisão antes de alterar os itens. Os efeitos, auditoria antes/depois e recibo de repetição pertencem à mesma transação. Auditoria usa a ação `ALTERACAO`; o recibo administrativo usa `CONFIGURACAO`, com hash específico da edição. A revisão avança também quando a mudança atinge somente serviços/valores.

## Estrutura e permissões preparadas

[V13](../../database/migrations/V13__historico_itens_tabela_cobranca.sql) acrescenta `item_tabela_cobranca.ativo`, preenchendo os registros anteriores com verdadeiro. Substitui a unicidade antiga por índice único filtrado dos itens ativos e estende somente a combinação TABELA_COBRANCA/ALTERACAO no CHECK financeiro da auditoria. Não elimina registros nem altera V1–V12.

São seis permissões adicionais de UPDATE por coluna:

| Tabela | Colunas adicionais |
| --- | --- |
| `tabela_cobranca` | `armazem_id`, `cliente_id`, `descricao`, `tipo`, `vigencia_inicio` |
| `item_tabela_cobranca` | `ativo` |

As 17 permissões cadastrais anteriores são preservadas. O preparador existente passa a preparar 23 no total, idempotentemente. A guarda preliminar reconhece apenas os estados exatos anterior, cadastral com 17 e completo com 23; não libera o backend. A guarda normal continua exigindo V13 e direitos completos, além de alvo/identidade/TLS/catálogo/histórico atuais. A V10 específica de PROD permanece distinta da V10 DEV.

## Validação e ativação

Evidências e capturas em `orchestracao/.runtime/tab-edit01/`; logs finais adjacentes `tab-edit01-*.log`. Testes sem banco, compilação, navegador fictício e planos de permissões não comprovam persistência SQL real. Os testes novos verificam preservação das tarifas, remoção/inclusão, zero, escala decimal, duplicata, Gestor, revisão antiga, repetição e proteção da vigência. O teste de mapeamento gera SQL pelo dialeto SQL Server sem conexão e confere que um item só pode atualizar `ativo`.

**Ativação pendente:** liberar/aplicar a V13 no alvo específico pelo executor WMS, adequar as seis colunas com guarda antes/depois e validar o fluxo real exclusivamente em WMS_DEV/WMSDEV. Somente então atualizar a instância pelo BAT DEV ou pelo fluxo PM2 do operador no alvo correspondente. A aplicação não executa migrations automaticamente. Nenhuma alteração em banco, processo operacional ou produção foi executada nesta entrega.
