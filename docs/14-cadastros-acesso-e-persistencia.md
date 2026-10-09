# Cadastros, acesso e persistência do backend

Entrega de 05/10/2026, após D12: ampliar o backend em um bloco maior. Abrange recortes de **BE01, BE03, BE04 e BE05**, mantendo Spring MVC por camadas. Nenhuma dessas etapas completas é encerrada apenas por esta entrega.

D13/BE16 acrescentou configuração em `.properties`, validação das entradas dos serviços e verificações automáticas de engenharia, descritas no [documento 16](16-padroes-de-engenharia-backend.md). Os contratos cadastrais abaixo são preservados.

## Modelo implementado e origem

| Modelo | Relações e regras implementadas | Fonte |
| --- | --- | --- |
| Cliente | Código e documento fiscal normalizados/únicos; nome; situação e revisão | RN06; identidade/unicidade são escolhas técnicas deste recorte |
| Armazém | Código único; nome, documento fiscal, cidade e UF | RN04; referência fiscal inicial, sem configuração tributária |
| Produto | Proprietário imutável; SKU único por cliente; unidade, contagem/medida e precisão de 0 a 6 casas | RN06, Q01, interpretação AC10 |
| Embalagem | Pertence ao produto; DUN único nesse produto; quantidade positiva na precisão do produto | Q01/Q02, AC10; escopo de unicidade é escolha técnica |
| Endereço | Armazém imutável; código único e combinação rua/nível/posição única nesse armazém | RN04/RN05, Q06 |
| Auditoria de cadastro | Tipo/ID, ação, sujeito autenticado, instante UTC, motivo, ID da operação e snapshots anterior/posterior | AC11/AC12, PR06 e proteção de histórico |

Identificadores são `bigint`; entidades usam `@Version`, e DTOs têm contratos próprios. String de documento fiscal é normalizada para letras/números; não valida dígito verificador nem situação fiscal. Não foram determinados CFOP, tributos ou dados reais. Armazéns podem compartilhar a referência fiscal; seu código identifica o estabelecimento operacional nesta etapa.

Produto contado exige precisão zero. Produto medido aceita a precisão cadastrada, até seis casas. Quantidade por embalagem usa decimal(19,6) e não pode introduzir fração proibida pelo produto. O controle de validade exige antecedência configurada; essa configuração não implementa ainda avisos ou bloqueios operacionais. Não se inventa lote para produto sem controle de lote.

DUN é configuração de embalagem e não ID de pallet/bobina. A consulta de DUN exige contexto do produto; não há promessa de leitura global pelo DUN. Nenhum cadastro de embalagem cria estoque ou unidade logística.

Endereços possuem tipo ARMAZENAGEM, TRIAGEM, QUARENTENA ou SEPARACAO, sequência de coleta e limites opcionais de peso, dimensões e empilhamento. Limite ausente significa **não configurado**, não capacidade infinita. A validação de ocupação, uma unidade por posição e o conjunto de duas posições de AC05 pertencem a BE08. A importação Excel de endereços permanece em BE05.

D17/BE08 acrescentou `tipoUnidadePermitido` à resposta e o PUT específico `/enderecos/{id}/capacidade`: Gestor pode configurar limites em endereço ativo, livre e sem conjunto ativo. O PUT cadastral acima continua restrito à descrição. Ocupação, conjuntos e disponibilidade estão no [documento 22](22-enderecamento-movimentacao-e-estoque.md); a ausência de perfil completo impede endereçamento.

## API entregue para FE04

Prefixo `/api/v1`. Nas cinco coleções abaixo:

- `POST /<colecao>` cria e retorna 201 com `Location`.
- `GET /<colecao>` lista com `pagina` (início 0) e `tamanho` (padrão 20, limite 100), ordem por ID crescente.
- `GET /<colecao>/{id}` consulta um cadastro.
- `PUT /<colecao>/{id}` altera somente nome/descrição; exige `versao` e `motivo` de 5 a 500 caracteres.
- `POST /<colecao>/{id}/encerramento` e `/reativacao` exigem `versao` e `motivo`.

| Coleção | Dados de criação | Filtro de vínculo obrigatório para listar | Campo editável pelo PUT |
| --- | --- | --- | --- |
| `clientes` | `codigo`, `nome`, `documentoFiscal` | Nenhum; backend filtra pelo escopo | `nome` |
| `armazens` | `codigo`, `nome`, `documentoFiscal`, `cidade`, `uf` | Nenhum; backend filtra pelo escopo | `nome` |
| `produtos` | `clienteId`, `sku`, `descricao`, `unidadeMedida`, `tipoQuantidade`, `precisaoQuantidade`, `controlaLote`, `controlaValidade`, `antecedenciaAvisoDias` quando aplicável | `clienteId` | `descricao` |
| `embalagens` | `produtoId`, `codigoDun`, `descricao`, `quantidadeProduto` | `produtoId` | `descricao` |
| `enderecos` | `armazemId`, `codigo`, `rua`, `nivel`, `posicao`, `descricao`, `tipo`, `sequenciaColeta`; opcionais `capacidadePesoKg`, `alturaMetros`, `larguraMetros`, `profundidadeMetros`, `empilhamentoMaximo` | `armazemId` | `descricao` |

DTOs de criação e resposta estão em `backend/src/main/java/br/com/rodogarcia/wms/dto`. Códigos/SKU/DUN aceitam letras ASCII, números e `._/-`, sendo convertidos para maiúsculas; unidade/UF também são normalizadas. Nomes e descrições preservam acentos. O endereço completo é explícito: não derivar um código ambíguo apenas concatenando seus componentes.

Resposta paginada: `itens`, `pagina`, `tamanho`, `totalItens`, `totalPaginas`. Deslocamentos acima do limite inteiro do JPA são recusados com 400. Respostas de cadastro acrescentam `id`, `versao`, `situacao`, `criadoEm`, `alteradoEm` e os campos do cadastro; relacionamentos usam IDs. Listas fora do alcance não vazam registros. Consulta de objeto fora do alcance retorna 403. Campos JSON desconhecidos são recusados, inclusive tentativa de editar campos imutáveis.

Exemplo fictício de alteração:

```json
{"versao": 0, "nome": "Cliente de demonstração", "motivo": "Correção do nome cadastrado"}
```

Identidade, proprietário, SKU, unidade, precisão, conversão e localização estrutural são imutáveis neste recorte para preservar seu significado histórico. Uma futura correção desses campos exige fluxo específico, revisão de dependências e auditoria; o PUT não os ignora silenciosamente.

## Encerramento e concorrência

Estados implementados: ATIVO e ENCERRAMENTO_PENDENTE. O encerramento preserva o registro e seu histórico, bloqueando a criação de produto em cliente encerrando, embalagem em produto/cliente encerrando e endereço em armazém encerrando. Reativação é explícita e auditada. Consultas e correção de descrição continuam permitidas.

A inativação definitiva não é exposta: faltam os módulos para conferir estoque, pedidos e cobrança conforme a proposta AC12. Não há DELETE. Situações de cliente/armazém/produto devem ser revalidadas pelos futuros serviços operacionais; sua mudança não cancela ou libera reservas silenciosamente.

Versão desatualizada retorna 409, sem sobrescrever a alteração anterior. Locks transacionais serializam a alteração e a criação dos vínculos afetados; no caminho da embalagem, cliente é bloqueado antes de produto. Restrições únicas no banco protegem contra duas criações equivalentes. Cadastro e auditoria são gravados na mesma transação; falha na auditoria desfaz a alteração. Isso foi verificado em H2, sem comprovar o comportamento dos locks SQL Server.

## Identidade e alcance para FE03

Escolha técnica: API como **OAuth2 Resource Server**, recebendo JWT RS256 assinado de um provedor externo. Spring Security valida assinatura e emissor; a aplicação exige audiência WMS, sujeito, emissão, expiração, perfil e listas de alcance. O intervalo entre emissão e expiração é limitado a 15 minutos, com a tolerância padrão do validador Spring para diferença de relógios. Não há endpoint próprio de emissão de tokens, senha, usuário padrão ou chave privada no backend.

| Claim | Contrato |
| --- | --- |
| `iss`, `aud`, `sub`, `iat`, `exp` | Emissor/audiência configurados, sujeito até 200 caracteres, emissão/expiração válidas |
| `wms_perfil` | Exatamente GESTOR, SUPERVISOR ou OPERACAO |
| `wms_clientes` | Lista de IDs positivos como strings, até 500; vazia significa sem alcance para perfis limitados |
| `wms_armazens` | Mesma regra para armazéns |

GESTOR consulta e altera todos os cadastros e consulta auditoria. SUPERVISOR e OPERACAO apenas consultam cadastros dentro dos clientes/armazéns atribuídos. As permissões operacionais futuras não estão implementadas. O backend verifica o alcance nos services, inclusive nas consultas de produto/embalagem/endereço pelo ID.

`Authorization: Bearer <token>` é o único meio de autenticação nesta API; cookies não autenticam. Não há sessão ou login local. CSRF fica desabilitado somente no modo de API Bearer; o modo básico local mantém seu bloqueio original. Frontend futuro deverá obter o token por fluxo do provedor e não persistir segredo de cliente no navegador. CORS segue sem origens externas liberadas.

O provedor real, mapeamento de contas, emissão/renovação/revogação, políticas de login e integração FE03 ainda exigem configuração/validação. Revogação de alcance não é instantânea em JWT já emitido: sua validade máxima limita a janela. BE04 permanece em andamento.

Base técnica consultada: [Spring Security Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html). Nos testes, chaves RSA são geradas em memória e tokens são assinados/verificados de verdade; nenhum provedor é acessado.

## Auditoria e erros

`GET /api/v1/auditoria?tipo=CLIENTE&registroId=<id>&pagina=0&tamanho=20`, somente GESTOR. Tipos: CLIENTE, ARMAZEM, PRODUTO, EMBALAGEM, ENDERECO. Snapshots `dadosAntes`/`dadosDepois` são strings JSON; criação tem `dadosAntes` nulo. Não há atualização/exclusão de auditoria por API.

Erros mantêm ProblemDetail, `codigo`, `idOperacao` e `X-Request-Id`. Exemplos: 400 DADOS_INVALIDOS; 401 NAO_AUTENTICADO; 403 ACESSO_NEGADO; 404 CADASTRO_NAO_ENCONTRADO; 409 CONFLITO_DE_INTEGRIDADE, VERSAO_DESATUALIZADA, CONFLITO_CONCORRENTE ou CADASTRO_EM_ENCERRAMENTO. SQL e valores rejeitados não entram nas respostas; mensagens de erro JDBC que poderiam conter valores são suprimidas, com aviso seguro de integridade no handler.

## Persistência e ligações futuras

JPA/Hibernate, driver Microsoft e V1 em [migrations](../database/migrations/README.md). Perfil padrão `local` continua sem conexão e com apenas status. `sqlserver-dev` exige alvo confirmado e configuração externa de banco/identidade. Hibernate valida, sem criar/atualizar esquema; migrations só pelo procedimento separado. H2 é dependência exclusiva de teste e não acompanha o JAR.

Cliente/armazém/produto/embalagem/endereço fornecem referências para futuros pedidos, notas, unidades e movimentos. A unidade logística guardará ID permanente, nota de origem, quantidade, lote aplicável e datas separadas; não substituir esses campos por IDs do cadastro. Reserva, localização, condição, fiscal e cobrança continuam distintas. Nada neste recorte efetiva estoque, conclui pedido ou calcula valor faturável.

Próximos recortes independentes: contratos operacionais de BE01 e preparação de recebimento BE06; importação de endereços/tabelas de BE05. SQL Server, provedor de identidade e frontend precisam de validação própria antes de M01/piloto. Resultados desta entrega em [validação do bloco](15-validacao-cadastros-backend.md).

## Adenda D30 — limite de transporte do JWT,07/10/2026

A configuração da aplicação passa a definir `server.max-http-request-header-size=64KB`. Esse limite é do transporte HTTP; as regras de domínio permanecem: sujeito até200 caracteres, duração até900s, listas de alcance até500 IDs positivos como strings e domínioLong preservado. A propriedade não configura um provedor real, servidor compartilhado ou frontend.

O [recibo do autor](../backend/evidencias/d30-cedro-header-recibo.json) conserva o red TCP local: JWT RS256 fictício válido de30.220bytes recebeu400 com limite efetivo8192bytes. Após a correção apenas em `application.properties`, o contexto local semDataSource/EMF comprovou65536bytes no ServerProperties e connector, resposta200 e uma chamada ao colaborador de negócio. Um cabeçalho adicional de131072bytes recebeu400 sem chamada adicional. O token/sujeito/alcances concretos do teste delimitam essa prova; não aceitam todascombinações, provedor real ou SQLServer. A revisão independente e a regressão final D30 continuam necessárias. Uma recusa no connector antecede o controller; este recibo não prova o formato ProblemDetails para essa recusa.
