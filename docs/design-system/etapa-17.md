# CAD-EDIT01 / FE06-ETIQUETA01 — correção cadastral e etiqueta

Pedido de Lucas em 14/10/2026: permitir corrigir os dados informados no cadastro e reproduzir a hierarquia visual da etiqueta enviada. Esta etapa altera frontend e backend; o comportamento antigo de editar somente nome/descrição é histórico.

## Campos e acesso

| Cadastro | Campos editáveis |
| --- | --- |
| Cliente | Nome e CPF/CNPJ |
| Armazém | Nome, CPF/CNPJ, cidade e UF |
| Produto | SKU, descrição, unidade, tipo/precisão de quantidade, controle de lote/validade e antecedência |
| Embalagem | Código DUN, descrição e quantidade de produto por DUN |
| Posição | Código, descrição, rua, nível, posição, tipo, sequência de coleta e limites físicos do cadastro |

Somente **Gestor**, conforme o alcance de administração cadastral vigente. A permissão é conferida nos serviços; administrador de usuários não ganha implicitamente esse perfil. Motivo e revisão atual continuam obrigatórios. Cadastro e snapshots anterior/posterior da auditoria são gravados na mesma transação. Unicidades de documento/SKU/DUN/código/coordenadas continuam protegidas pelas constraints existentes, com erro de integridade padronizado.

Os novos campos do PUT são opcionais para compatibilidade com clientes antigos que enviam somente nome/descrição. A tela carrega todos os valores da consulta atual. Campos técnicos e códigos internos automáticos não são entradas livres. Cliente proprietário do produto, produto da embalagem e armazém da posição permanecem vínculos imutáveis; a correção não transfere mercadoria/propriedade. Complementos fiscais continuam em sua edição própria.

Proteções de histórico: unidade/tipo/precisão/controles do produto só mudam antes de existirem dependências; SKU/descrição/antecedência continuam corrigíveis. A conversão de embalagem usada por unidade logística não pode ser reinterpretada; DUN/descrição continuam corrigíveis. Rua/nível/posição/tipo/limites exigem endereço desocupado e sem conjunto ativo; código/descrição/ordem de coleta podem ser corrigidos. O lock de armazém antecede o da posição. Nenhuma mudança recalcula estoque, reservas ou cobrança fechada.

Na edição completa de produto, desligar validade permite limpar antecedência. Na edição completa de endereço (`tipo` informado), limites opcionais podem ser removidos; isso representa limite não configurado, nunca capacidade infinita. Comandos antigos sem os campos novos preservam os valores existentes.

## Etiqueta

SKU grande, descrição, cliente, notas/quantidade, chegada física em destaque, conferente, Code128 do UUID, DUN/conversão e faixa inferior com QR do mesmo UUID, numeração e tipo (bobina/palete). A ajuda fica no catálogo `information.ts`. O aviso fictício aparece apenas em exercício, inclusive impresso. UUID inválido e QR ainda não gerado impedem o botão de impressão. Mudança de unidade descarta o QR anterior.

`EtiquetaService` enriquece a consulta autorizada, sem escrita: notas distintas das origens; soma dos itens do mesmo produto nessas notas, sem somar novamente uma origem repetida; autor da primeira operação que gerou o UUID, resolvido pelo nome do usuário. A leitura histórica extrai somente o código do snapshot, sem exigir campos adicionados posteriormente ao DTO. Autor não localizado é explicitamente não identificado; não se atribui a etiqueta ao usuário da reimpressão.

Data destacada é **chegada real**, em Brasília, distinta de FIFO/início da cobrança. Quantidade das notas é distinta da quantidade atual da unidade. Numeração é a posição por ID entre unidades **ativas do pedido na consulta**, com total dessas unidades; pode mudar após novas unitizações/divisões/reagrupamentos. Múltiplas notas são apresentadas quando constam das origens; a mudança visual não amplia a regra de reagrupamento entre notas.

Impressão pelo navegador, preto e branco, faixa preta preservada inclusive no tema escuro; o modal e seus outros controles ficam fora da impressão. Largura inicial de apresentação impressa: **150 mm**, escolha técnica ainda dependente do papel/impressora. Não houve impressão nem leitura em equipamento físico. Code128 via JsBarcode 3.12.3 e QR via dependência existente, sem geração por IA.

## Ativação e permissões SQL

**DEV aplicado após autorização específica; PROD preparado para execução pelo operador.** A baseline preservada D29 recusava UPDATE em 17 colunas que agora passam a ser corrigíveis. O SQL preparado em memória pelo Hibernate confirmou que elas integram os UPDATEs estáticos, inclusive de operações cadastrais que alterem apenas outro campo. A aplicação DEV recebeu o delta em transação, com guarda antes/depois e sem mudar a baseline.

| Tabela | Delta mínimo de UPDATE por coluna |
| --- | --- |
| `cliente` | `documento_fiscal` |
| `armazem` | `documento_fiscal` |
| `produto` | `sku`, `unidade_medida`, `tipo_quantidade`, `precisao_quantidade`, `controla_lote`, `controla_validade`, `antecedencia_aviso_dias` |
| `embalagem` | `codigo_dun`, `quantidade_produto` |
| `endereco` | `codigo`, `rua`, `nivel`, `posicao`, `tipo`, `sequencia_coleta` |

`infra/cadastros/contrato-edicao.ps1` estende somente o contrato esperado em memória. As guardas DEV/PROD confrontam esse contrato antes de ativar a nova fonte, recusando os direitos antigos. Não altera a baseline nem concede acesso. IDs de coluna seguem a ordem capturada no catálogo imutável; discrepância real será recusada pelo confronto exato. Teste local cobre os dois contratos, preservação, delta, idempotência e recusa de coluna ausente.

Lucas autorizou “pode aplicar”, seguido do pedido para deixar produção preparada para ele atualizar. `infra/cadastros/aplicar-permissoes-dev.ps1` executou somente WMS_DEV; `aplicar-permissoes-prod.ps1` foi validado em Plan local, sem conexão. O comando do operador `pm2-wms.bat atualizar` chama o ajuste PROD idempotente antes do preparo/troca das instâncias. Nenhuma migration, novo schema ou escrita SQL de negócio integra o ajuste; são somente os GRANTs listados, com direitos completos, catálogo e histórico comparados dentro da transação. A guarda preliminar possui sinal próprio e `guardaRealAprovada=false`, portanto nunca libera backend. Após o delta, a guarda normal precisa passar. Falha posterior de build pode manter os direitos já aplicados, preservando a instância antiga e registrando essa distinção. Sem execução PROD pelo agente.

## Validação

Evidências em `orchestracao/.runtime/cadastros-etiqueta-20261014/`: **139 testes puros backend e 650 testes frontend aprovados**, tipagem, lint e builds isolados aprovados. Cobertura de serviços (normalização, auditoria, revisão, autorização e impedimentos), enriquecimento da etiqueta sem conexão; Chrome fictício com edição/confirmação dos cinco cadastros, mobile 390 px sem overflow, temas e impressão do modal. Imagens `cliente-desktop.png`, `armazem-desktop.png`, `posicao-mobile.png`, `etiqueta-desktop.png`, `etiqueta-mobile.png`, `etiqueta-impressao.png`.

Os 650 testes frontend e 139 testes puros backend continuam separados do ensaio real posterior: **73 verificações integradas**, 44 chamadas API registradas e cinco submissões pelos formulários oficiais. Cliente81 (reutilizado após corrigir a expectativa de normalização), cliente82 para duplicidade, armazém45, produto87, embalagem59 e posição152 foram criados/corrigidos somente por API e preservados. Leitura da etiqueta usou unidade fictícia preexistente sem alterar estoque; autor real do histórico identificado. Recibo `resultado-ativacao.json`, prova `integracao-real-02.json`, telas `real-edicao-*.png` e `etiqueta-sql-real.png`. A primeira falha do teste e a guarda de partida permanecem históricas; a continuação teve guarda suplementar nova vinculada a fonte/pacote/PIDs/JDBC atuais. Dez testes locais dos limites PM2 e Plan PROD passaram sem conectar produção. Impressão/leitor físico, perfis Supervisor/Operação autenticados no ensaio real e concorrência operacional ampla não foram comprovados por esse recorte; a autorização por perfil tem cobertura pura. Sem commit/push.
