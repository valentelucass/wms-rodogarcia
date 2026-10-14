# FE06-REFERENCIAS01 — nomes e códigos nas telas operacionais

Pedido de Lucas em 14/10/2026, a partir do relato do gestor: apresentar nomes/códigos reais no lugar de IDs internos em todas as telas semelhantes, reorganizar a unitização e entregar imagens de antes/depois.

## Apresentação

- Listas, detalhes, confirmações, formulários e referências compartilhadas usam cliente/armazém por nome e código, produto por SKU, pedido por referência, nota por número/série e unidade pelo código da etiqueta existente. Não gerar novos códigos a partir de IDs ou retirar zeros do SKU.
- IDs de persistência e versões técnicas deixam de ocupar colunas e títulos. Continuam intactos no estado e nos comandos, inclusive valores maiores que o limite seguro do JavaScript. Números de versão financeira e códigos de etiqueta continuam sendo informações do negócio.
- Entradas conferidas são cartões de nota/item, SKU, triagem, quarentena e lote. Datas ficam em uma seção expansível; horários em Brasília e quantidades em português, preservando casas significativas. Selecionar um cartão mantém o registro exato recebido.
- Seletores de referências oferecem nomes/códigos, busca na página atual e paginação. Mudança de cliente ou armazém descarta a página anterior. Carregamento, falha, consulta vazia e contexto incompleto têm apresentação explícita.
- Administração de usuários oferece clientes/armazéns por nome. Vínculos preexistentes ausentes da consulta são preservados, sem revogação silenciosa.

## Resolução e limites

O frontend usa primeiro as respostas tipadas já consultadas no contexto da jornada. Para entidades com consulta individual suportada, resolve a referência por GET autorizado, com correspondência exata de tipo/ID, cache restrito à sessão/escopo e no máximo três consultas simultâneas. Troca de escopo descarta respostas antigas; erros não provocam repetição automática. O catálogo de nomes não autoriza selecionar produtos de outro cliente.

Uma referência ausente ou sem acesso aparece como **Referência indisponível**; não é substituída por um nome inventado ou por ID numérico. Entidades dependentes de uma coleção relacionada usam a coleção consultada na jornada. Os seletores não criam cadastros, não mudam autorização e não substituem a validação transacional do backend.

A organização também alcança recebimento, estoque, saída/expedição, cobrança, auditoria, etiquetas e usuários por meio dos componentes compartilhados. Ajuda da unitização permanece no catálogo único `frontend/src/content/information.ts`.

## Evidências e entrega

Testes e capturas desta etapa ficam em `orchestracao/.runtime/reference-names-20261014/`. Regressão final: **637/637 testes em 63 arquivos**, incluindo 12 novos, além de tipagem, lint, build isolado e diff check aprovados. Chrome verificou desktop 1366×1000 e celular 390×844, temas claro/escuro, rastreabilidade por Enter, foco no modal e fechamento por Escape com proteção de alterações. Sem erros JavaScript ou overflow horizontal do modal.

As imagens de desktop usam os componentes reais da versão anterior e da fonte alterada, com os mesmos dados fictícios de pedido, nota e entrada. Não representam movimentação em produção.

A validação local de apresentação, comandos e isolamento de escopo usa transporte fictício explícito. Tipagem, lint e build são conferências separadas. Este incremento não executa SQL, migrations, escrita operacional, reinício DEV/PROD nem publicação. Resultado final e contagens constam do recibo `resultado.json`; integração autenticada e versão operacional ativa não são atestadas por esta evidência local.
