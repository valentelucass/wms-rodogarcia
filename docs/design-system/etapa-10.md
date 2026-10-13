# FE05-XML-BOBINAS01 — XML por peso e conferência de bobinas

Pedido de Lucas em 13/10/2026: concluir e testar o fluxo aprovado pelo gestor. Complementa a etapa 09; mantém os componentes, tokens e estados de confirmação existentes.

## Fluxo implementado

1. Importar o XML e conferir documento, cliente proprietário, armazém e produtos. A quantidade comercial original permanece no documento, ao lado da quantidade prevista no estoque. O backend converte exatamente TO/T/TON ↔ KG; outras unidades só passam quando iguais. Não há arredondamento silencioso nem conversão de peso para contagem.
2. Se faltar produto, o Gestor pode cadastrá-lo dentro da prévia, com SKU/descrição sugeridos pelo XML. Massa sugere KG, MEDIDA e três casas decimais; controles de lote/validade continuam sujeitos à conferência do cadastro. O cadastro usa o serviço e a confirmação existentes. Ao retornar, a associação é preenchida e uma nova prévia é obrigatória antes de criar o pedido. O arquivo e as demais escolhas permanecem.
3. Na seção Conferência, Registrar chegada permite informar a contagem física de bobinas boas/avariadas, separada das quantidades de produto. O backend exige coerência entre presença de peso e presença de bobinas em cada condição, de 1 a 100 bobinas por item (limite já existente da distribuição). Para outros tipos de mercadoria ou registros históricos, a contagem permanece ausente, sem valor presumido.
4. Após efetivação, a distribuição sugere uma linha por bobina contada, com condição e tipo preenchidos; pesos individuais e embalagem continuam a informar. O backend confere tanto a contagem por condição como a soma das quantidades. Cada unidade conserva sua origem e identidade. Reimpressão não cria estoque.

## Persistência e compatibilidade

A contagem integra o comando e o snapshot imutável de CHEGADA_REGISTRADA, já gravado na mesma transação dos itens. A leitura usa o pedido autorizado, o ID da chegada e sua sequência, com uma consulta de snapshots por pedido, limitado a 100 chegadas. Não altera schema, permissões ou migrations. A informação acompanha as consultas de chegadas/entradas e a validação da unitização. Estorno e novo registro corrigem a conferência; o lançamento original permanece no histórico.

O campo opcional `bobinas` é omitido quando nulo para preservar o hash de repetição dos comandos antigos. Chegadas históricas sem contagem continuam distribuíveis com a regra de soma anterior. UUID, versões, autorização por cliente/armazém e transações permanecem nos serviços. Produtos já cadastrados não são alterados pela importação. O XML original continua armazenado sem substituir unidades/quantidades fiscais pelo valor convertido.

## Validação

Concluído em 13/10/2026: 581 testes frontend em 57 arquivos, tipagem/lint/build e 86 testes backend puros/build aprovados. Contratos gerados e 183 fontes do manifesto conferidos; 308 arquivos backend coincidem com o fingerprint do artefato ativo. Testes cobrem conversão, limites, condições, snapshots, serialização antiga, cadastro assistido/permissão, preservação do arquivo e preparação da distribuição.

Após guarda atual 26/26 exclusivamente WMS_DEV/WMSDEV/TLS obrigatório, uma execução autorizada de `iniciar-dev.bat` consumiu a fonte atual. Chrome autenticado com API real validou XML → produto → prévia de 0,012 TO/12 KG → pedido → chegada com duas bobinas → efetivação → unidades de 5/7 KG. Consultas posteriores comprovaram contagem persistida, XML original intacto, etiquetas/origens, duas unidades e saldo de 12 KG indisponível antes do endereçamento. Foram aprovadas 33 verificações integradas, incluindo repetição simultânea sem duplicidade, UUID com conteúdo diferente, revisão antiga, soma/contagem divergentes, limites e importação em pedido manual. Desktop/mobile, temas e teclado conferidos; screenshots inspecionadas.

O navegador encontrou duas falhas corrigidas: lote opcional vazio em produto sem controle de lote e seleção da entrada que não atualizava o parâmetro junto das linhas sugeridas. Regressões e jornada real passaram após as correções. [Resultado e limites](../../orchestracao/.runtime/fe05-xml-bobinas01/resultado.json), [jornada com IDs](../../orchestracao/.runtime/fe05-xml-bobinas01/jornada.json). Fixtures `TESTE-FE05-1791915369920` preservadas: pedidos 85/86, chegada 93, entrada 102, unidades 1345/1346. Pedido 85 efetivado/unitizado, sem endereço; pedido 86 em conferência sem chegada. Não recriar ao retomar.

DEV mantido aberto: backend PID 9004/25580, frontend PID 22364/25581, run `d1277d90-a1dc-4fee-9f7c-c8fc28651972`. Sessão final própria encerrada por logout. Sem segredos nos recibos, H2, DDL, migrations ou produção. Este aceite cobre o recorte; não substitui impressora/coletor físico, SEFAZ ou regressão dos 879 casos históricos.

A NF 32325 continua somente como referência analisada. Sua amostra incompatível e a diferença de 60 kg não são corrigidas por suposição. Testes usam XML e mercadorias fictícios; não há emissão fiscal externa.
