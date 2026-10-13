# Etapa 12 — correspondência de SKU sem zeros à esquerda

SKU-ZEROS01, pedido do gestor encaminhado por Lucas em 13/10/2026. O produto já cadastrado com zeros permanece válido e não precisa ser alterado.

## Comportamento e parâmetro

Na importação XML de entrada, o backend aceita diferenças apenas nos zeros de preenchimento à esquerda de um SKU inteiramente numérico, para os clientes configurados. Exemplo: `600366`, `000600366` e `000000000000600366`. Zeros internos e finais são preservados. Não converte códigos para números, evitando perda de precisão; códigos alfanuméricos e compostos somente de zeros continuam literais.

Parâmetro de aplicação: `wms.recebimento.xml.sku-sem-zeros-clientes`, lista de CNPJ/CPF sem pontuação separados por vírgulas. Vazio desativa a regra. Padrão autorizado neste recorte: `53943098000187` (Bracell). O perfil DEV acrescenta exclusivamente `00000000090601` para validação fictícia. Configuração em `.properties`; esta entrega não cria um controle editável na tela de clientes. O launcher captura a configuração na versão preparada; alterar o arquivo requer preparar e ativar a nova versão.

A consulta permanece restrita ao cliente autorizado, com variações de até 40 caracteres e retorno de no máximo dois IDs. Se houver duas correspondências, inclusive uma exata ou um cadastro inativo, a prévia exige associação manual; nunca escolhe um arbitrariamente. Um único cadastro inativo permanece impedido pelo controle existente. Associação manual continua exigindo produto do mesmo cliente. A confirmação recalcula a prévia e recusa revisão desatualizada.

XML original, SKU cadastrado, produtos existentes e histórico são preservados. Não há migration, alteração de estoque ou limpeza de cadastros. A regra de uma bobina por item também reconhece essas variações somente para o emitente/SKU previamente configurado, sem ampliar produtos ou unidades de medida.

## Interface e verificação

A prévia apresenta um aviso quando o parâmetro do cliente está ativo. A linha continua mostrando o código original da nota e o produto associado. Ambiguidades aparecem como pendência na mesma tela, usando o componente de avisos já existente. Não há nova regra de comparação no frontend.

**Concluído no DEV:** 108 testes puros aprovados (17 novos), sem H2; build, formatação e guarda SQL atual 26/26 aprovados. Quinze verificações integradas autenticadas: busca nos dois sentidos, código original, aviso, alfanuméricos, ambiguidade mesmo com match exato, revisão antiga recusada, associação manual, confirmação/repetição sem duplicação e XML salvo íntegro. Chrome desktop/mobile 390 px conferido, sem overflow; última sessão visual encerrada por logout 204.

DEV atualizado pelo BAT: run `467776d9-683d-4a05-95bc-7bd0b6f985b4`, backend 47272/25580 e frontend 54680/25581. Fixtures preservadas: produtos 83–85, pedido 89, nota 80, cliente 78/armazém 44; sem chegada física ou criação de estoque. [Recibo](../../orchestracao/.runtime/sku-zeros/resultado.json).

**Publicado em produção após autorização expressa de Lucas:** o candidato `554545971e4a4abb9c08330c245cde7d` teve sua integridade e fonte conferidas antes da interrupção. Somente os processos PROD anteriores 16776/5016 foram encerrados após atestação de propriedade. O BAT normal recompilou e ativou o run `8e2ad08966344568a19be1a2158ebdd5`, backend 57608/25590 e frontend 54916/25591. JAR com o mesmo hash do candidato preparado; 311 arquivos da fonte atual, classe e configuração Bracell confirmados.

Guarda PROD atual 26/26, frontend/API/relay HTTPS HTTP 200 e formulário de login sem erros JavaScript. Não foi repetido login autenticado nem executado teste de negócio ou fixture em PROD. DEV observado durante a publicação permaneceu nos PIDs 39564/43424. Sem DDL/migration, mudança de SKU existente ou estoque. [Publicação](../../orchestracao/.runtime/sku-zeros/publicacao.json), [HTTPS](../../orchestracao/.runtime/sku-zeros/https-publicacao.json). A sessão do launcher deve permanecer aberta; não há pendência de publicação deste recorte.
