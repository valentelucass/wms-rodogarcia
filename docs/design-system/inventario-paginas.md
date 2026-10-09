# Inventário de páginas e ações — FE02-REG01

Fonte: definições reais das jornadas e contratos frontend/backend. [Padrão e limites](etapa-06.md).

As 12 áreas contêm 50 páginas e 171 ocorrências de ações. Antes, cada etapa apresentava abas de ação, normalmente abrindo a primeira consulta sem execução automática. Agora, cada página abre a fonte abaixo; as demais capacidades têm destino explícito. As páginas operacionais previamente planejadas nas jornadas existentes estão contempladas; novas entidades/APIs fora dessas definições não são inventadas nesta entrega.

## Cadastros

Ligação: `BE05` → `FE04`. Páginas: 9.

### cadastros-1 — Clientes

Visão principal: `ClienteController.listar`. Detalhe: `ClienteController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`ClienteController.listar` | visão principal | `GET /api/v1/clientes`<br>`sem corpo` → `PaginaResponse<ClienteDto.Resposta>` | OPERACAO | nenhuma |
| Consultar detalhe<br>`ClienteController.consultar` | detalhe do registro | `GET /api/v1/clientes/{id}`<br>`sem corpo` → `ClienteDto.Resposta` | OPERACAO | id |
| Novo registro<br>`ClienteController.criar` | cabeçalho | `POST /api/v1/clientes`<br>`ClienteDto.Criar` → `ClienteDto.Resposta` | GESTOR | nenhuma |
| Corrigir descrição<br>`ClienteController.alterar` | contexto do registro / etapa operacional | `PUT /api/v1/clientes/{id}`<br>`ClienteDto.Alterar` → `ClienteDto.Resposta` | GESTOR | id |
| Solicitar encerramento<br>`ClienteController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/clientes/{id}/encerramento`<br>`RevisaoCadastroRequest` → `ClienteDto.Resposta` | GESTOR | id |
| Reativar cadastro<br>`ClienteController.reativar` | contexto do registro / etapa operacional | `POST /api/v1/clientes/{id}/reativacao`<br>`RevisaoCadastroRequest` → `ClienteDto.Resposta` | GESTOR | id |

Estados, validação e histórico: Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.

- Formulário `ClienteDto.Criar`: `codigo*` (`String`), `nome*` (`String`), `documentoFiscal*` (`String`).
- Formulário `ClienteDto.Alterar`: `versao*` (`Long`), `nome*` (`String`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ClienteController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ClienteController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#alterar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#encerrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ClienteService.java#reativar`.

### cadastros-2 — Armazéns

Visão principal: `ArmazemController.listar`. Detalhe: `ArmazemController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`ArmazemController.listar` | visão principal | `GET /api/v1/armazens`<br>`sem corpo` → `PaginaResponse<ArmazemDto.Resposta>` | OPERACAO | nenhuma |
| Consultar detalhe<br>`ArmazemController.consultar` | detalhe do registro | `GET /api/v1/armazens/{id}`<br>`sem corpo` → `ArmazemDto.Resposta` | OPERACAO | id |
| Novo registro<br>`ArmazemController.criar` | cabeçalho | `POST /api/v1/armazens`<br>`ArmazemDto.Criar` → `ArmazemDto.Resposta` | GESTOR | nenhuma |
| Corrigir descrição<br>`ArmazemController.alterar` | contexto do registro / etapa operacional | `PUT /api/v1/armazens/{id}`<br>`ArmazemDto.Alterar` → `ArmazemDto.Resposta` | GESTOR | id |
| Solicitar encerramento<br>`ArmazemController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/armazens/{id}/encerramento`<br>`RevisaoCadastroRequest` → `ArmazemDto.Resposta` | GESTOR | id |
| Reativar cadastro<br>`ArmazemController.reativar` | contexto do registro / etapa operacional | `POST /api/v1/armazens/{id}/reativacao`<br>`RevisaoCadastroRequest` → `ArmazemDto.Resposta` | GESTOR | id |

Estados, validação e histórico: Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.

- Formulário `ArmazemDto.Criar`: `codigo*` (`String`), `nome*` (`String`), `documentoFiscal*` (`String`), `cidade*` (`String`), `uf*` (`String`).
- Formulário `ArmazemDto.Alterar`: `versao*` (`Long`), `nome*` (`String`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ArmazemController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ArmazemController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#alterar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#encerrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ArmazemService.java#reativar`.

### cadastros-3 — Produtos / SKU

Visão principal: `ProdutoController.listar`. Detalhe: `ProdutoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`ProdutoController.listar` | visão principal | `GET /api/v1/produtos`<br>`sem corpo` → `PaginaResponse<ProdutoDto.Resposta>` | OPERACAO | clienteId |
| Consultar detalhe<br>`ProdutoController.consultar` | detalhe do registro | `GET /api/v1/produtos/{id}`<br>`sem corpo` → `ProdutoDto.Resposta` | OPERACAO | id |
| Novo registro<br>`ProdutoController.criar` | cabeçalho | `POST /api/v1/produtos`<br>`ProdutoDto.Criar` → `ProdutoDto.Resposta` | GESTOR | nenhuma |
| Corrigir descrição<br>`ProdutoController.alterar` | contexto do registro / etapa operacional | `PUT /api/v1/produtos/{id}`<br>`ProdutoDto.Alterar` → `ProdutoDto.Resposta` | GESTOR | id |
| Solicitar encerramento<br>`ProdutoController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/produtos/{id}/encerramento`<br>`RevisaoCadastroRequest` → `ProdutoDto.Resposta` | GESTOR | id |
| Reativar cadastro<br>`ProdutoController.reativar` | contexto do registro / etapa operacional | `POST /api/v1/produtos/{id}/reativacao`<br>`RevisaoCadastroRequest` → `ProdutoDto.Resposta` | GESTOR | id |

Estados, validação e histórico: Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.

- Formulário `ProdutoDto.Criar`: `clienteId*` (`Long`), `sku*` (`String`), `descricao*` (`String`), `unidadeMedida*` (`String`), `tipoQuantidade*` (`TipoQuantidade`), `precisaoQuantidade*` (`Integer`), `controlaLote*` (`Boolean`), `controlaValidade*` (`Boolean`), `antecedenciaAvisoDias` (`Integer`).
- Formulário `ProdutoDto.Alterar`: `versao*` (`Long`), `descricao*` (`String`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ProdutoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ProdutoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#alterar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#encerrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ProdutoService.java#reativar`.

### cadastros-4 — Embalagens / DUN

Visão principal: `EmbalagemController.listar`. Detalhe: `EmbalagemController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`EmbalagemController.listar` | visão principal | `GET /api/v1/embalagens`<br>`sem corpo` → `PaginaResponse<EmbalagemDto.Resposta>` | OPERACAO | produtoId |
| Consultar detalhe<br>`EmbalagemController.consultar` | detalhe do registro | `GET /api/v1/embalagens/{id}`<br>`sem corpo` → `EmbalagemDto.Resposta` | OPERACAO | id |
| Novo registro<br>`EmbalagemController.criar` | cabeçalho | `POST /api/v1/embalagens`<br>`EmbalagemDto.Criar` → `EmbalagemDto.Resposta` | GESTOR | nenhuma |
| Corrigir descrição<br>`EmbalagemController.alterar` | contexto do registro / etapa operacional | `PUT /api/v1/embalagens/{id}`<br>`EmbalagemDto.Alterar` → `EmbalagemDto.Resposta` | GESTOR | id |
| Solicitar encerramento<br>`EmbalagemController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/embalagens/{id}/encerramento`<br>`RevisaoCadastroRequest` → `EmbalagemDto.Resposta` | GESTOR | id |
| Reativar cadastro<br>`EmbalagemController.reativar` | contexto do registro / etapa operacional | `POST /api/v1/embalagens/{id}/reativacao`<br>`RevisaoCadastroRequest` → `EmbalagemDto.Resposta` | GESTOR | id |

Estados, validação e histórico: Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.

- Formulário `EmbalagemDto.Criar`: `produtoId*` (`Long`), `codigoDun*` (`String`), `descricao*` (`String`), `quantidadeProduto*` (`BigDecimal`).
- Formulário `EmbalagemDto.Alterar`: `versao*` (`Long`), `descricao*` (`String`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EmbalagemController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EmbalagemController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#alterar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#encerrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EmbalagemService.java#reativar`.

### cadastros-5 — Endereços

Visão principal: `EnderecoController.listar`. Detalhe: `EnderecoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`EnderecoController.listar` | visão principal | `GET /api/v1/enderecos`<br>`sem corpo` → `PaginaResponse<EnderecoDto.Resposta>` | OPERACAO | armazemId |
| Consultar detalhe<br>`EnderecoController.consultar` | detalhe do registro | `GET /api/v1/enderecos/{id}`<br>`sem corpo` → `EnderecoDto.Resposta` | OPERACAO | id |
| Novo registro<br>`EnderecoController.criar` | cabeçalho | `POST /api/v1/enderecos`<br>`EnderecoDto.Criar` → `EnderecoDto.Resposta` | GESTOR | nenhuma |
| Corrigir descrição<br>`EnderecoController.alterar` | contexto do registro / etapa operacional | `PUT /api/v1/enderecos/{id}`<br>`EnderecoDto.Alterar` → `EnderecoDto.Resposta` | GESTOR | id |
| Solicitar encerramento<br>`EnderecoController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/enderecos/{id}/encerramento`<br>`RevisaoCadastroRequest` → `EnderecoDto.Resposta` | GESTOR | id |
| Reativar cadastro<br>`EnderecoController.reativar` | contexto do registro / etapa operacional | `POST /api/v1/enderecos/{id}/reativacao`<br>`RevisaoCadastroRequest` → `EnderecoDto.Resposta` | GESTOR | id |

Estados, validação e histórico: Consulte o cadastro antes de alterar. Revisão e motivo preservam o histórico; inativação definitiva exige verificar impedimentos.

- Formulário `EnderecoDto.Criar`: `armazemId*` (`Long`), `codigo*` (`String`), `rua*` (`String`), `nivel*` (`Integer`), `posicao*` (`String`), `descricao*` (`String`), `tipo*` (`TipoEndereco`), `capacidadePesoKg` (`BigDecimal`), `alturaMetros` (`BigDecimal`), `larguraMetros` (`BigDecimal`), `profundidadeMetros` (`BigDecimal`), `empilhamentoMaximo` (`Integer`), `sequenciaColeta*` (`Integer`).
- Formulário `EnderecoDto.Alterar`: `versao*` (`Long`), `descricao*` (`String`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EnderecoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EnderecoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#alterar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#encerrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EnderecoService.java#reativar`.

### cadastros-6 — Capacidade e conjuntos

Visão principal: `CapacidadeController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`CapacidadeController.listar` | visão principal | `GET /api/v1/conjuntos-posicoes`<br>`sem corpo` → `PaginaResponse<CapacidadeDto.Conjunto>` | OPERACAO | armazemId |
| Configurar<br>`CapacidadeController.configurar` | cabeçalho | `PUT /api/v1/enderecos/{id}/capacidade`<br>`CapacidadeDto.ConfigurarEndereco` → `EnderecoDto.Resposta` | GESTOR | id |
| Novo registro<br>`CapacidadeController.criar` | cabeçalho | `POST /api/v1/conjuntos-posicoes`<br>`CapacidadeDto.CriarConjunto` → `CapacidadeDto.Conjunto` | GESTOR | nenhuma |
| Solicitar encerramento<br>`CapacidadeController.encerrar` | contexto do registro / etapa operacional | `POST /api/v1/conjuntos-posicoes/{id}/encerramento`<br>`RevisaoCadastroRequest` → `CapacidadeDto.Conjunto` | GESTOR | id |

Estados, validação e histórico: Limites ausentes significam não configurados. O backend confere as duas posições juntas.

- Formulário `CapacidadeDto.ConfigurarEndereco`: `versao*` (`Long`), `tipoUnidadePermitido*` (`TipoUnidadeLogistica`), `limites*` (`CapacidadeDto.Limites`), `motivo*` (`String`).
- Formulário `CapacidadeDto.CriarConjunto`: `armazemId*` (`Long`), `codigo*` (`String`), `enderecoAId*` (`Long`), `enderecoBId*` (`Long`), `limites*` (`CapacidadeDto.Limites`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/CapacidadeController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/CapacidadeController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/CapacidadeService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CapacidadeService.java#configurar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CapacidadeService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CapacidadeService.java#encerrar`.

### cadastros-7 — Importar endereços Excel

Visão principal: `ImportacaoEnderecoController.consultar`. Detalhe: `ImportacaoEnderecoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Conferir prévia Excel<br>`ImportacaoEnderecoController.previa` | cabeçalho | `POST /api/v1/armazens/{id}/importacoes-enderecos/previa`<br>`ImportacaoEnderecoDto.Previa` → `ImportacaoEnderecoDto.Resultado` | GESTOR | id |
| Consultar detalhe<br>`ImportacaoEnderecoController.consultar` | visão principal | `GET /api/v1/importacoes-enderecos/{id}`<br>`sem corpo` → `ImportacaoEnderecoDto.Resultado` | GESTOR | id |
| Confirmar<br>`ImportacaoEnderecoController.confirmar` | contexto do registro / etapa operacional | `POST /api/v1/importacoes-enderecos/{id}/confirmacao`<br>`ImportacaoEnderecoDto.Confirmar` → `ImportacaoEnderecoDto.Resultado` | GESTOR | id |

Estados, validação e histórico: Selecione .xlsx, confira cada linha na prévia e confirme a importação completa pelo hash retornado.

- Formulário `ImportacaoEnderecoDto.Previa`: `operacaoId*` (`UUID`), `motivo*` (`String`).
- Formulário `ImportacaoEnderecoDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `arquivoHash*` (`String`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ImportacaoEnderecoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ImportacaoEnderecoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ImportacaoEnderecoService.java#previa`, `backend/src/main/java/br/com/rodogarcia/wms/services/ImportacaoEnderecoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ImportacaoEnderecoService.java#confirmar`.

### cadastros-8 — Dados fiscais cadastrais

Visão principal: `ClienteController.listar`. Detalhe: `FiscalCadastroController.cliente`.

Tipos reais de registro: Clientes (`ClienteController.listar` → `FiscalCadastroController.cliente`); Armazéns (`ArmazemController.listar` → `FiscalCadastroController.armazem`); Produtos (`ProdutoController.listar` → `ProdutoController.consultar`).

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar fiscal do cliente<br>`FiscalCadastroController.cliente` | detalhe do tipo de cadastro escolhido | `GET /api/v1/clientes/{id}/complemento-fiscal`<br>`sem corpo` → `FiscalCadastroDto.Complemento` | OPERACAO | id |
| Complementar fiscal do cliente<br>`FiscalCadastroController.complementarCliente` | contexto do registro / etapa operacional | `POST /api/v1/clientes/{id}/complemento-fiscal`<br>`FiscalCadastroDto.Complementar` → `FiscalCadastroDto.Complemento` | GESTOR | id |
| Consultar fiscal do armazém<br>`FiscalCadastroController.armazem` | detalhe do tipo de cadastro escolhido | `GET /api/v1/armazens/{id}/complemento-fiscal`<br>`sem corpo` → `FiscalCadastroDto.Complemento` | OPERACAO | id |
| Complementar fiscal do armazém<br>`FiscalCadastroController.complementarArmazem` | contexto do registro / etapa operacional | `POST /api/v1/armazens/{id}/complemento-fiscal`<br>`FiscalCadastroDto.Complementar` → `FiscalCadastroDto.Complemento` | GESTOR | id |
| Consultar referências fiscais do SKU<br>`FiscalCadastroController.referencias` | contexto do registro / etapa operacional | `GET /api/v1/produtos/{id}/referencias-fiscais`<br>`sem corpo` → `List<FiscalCadastroDto.Referencia>` | OPERACAO | id |
| Registrar referência fiscal<br>`FiscalCadastroController.referenciar` | contexto do registro / etapa operacional | `POST /api/v1/produtos/{id}/referencias-fiscais`<br>`FiscalCadastroDto.Referenciar` → `FiscalCadastroDto.Referencia` | GESTOR | id |

Estados, validação e histórico: Referências de cliente/armazém/produto; configuração não emite documento.

- Formulário `FiscalCadastroDto.Complementar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `dados*` (`FiscalCadastroDto.Dados`), `motivo*` (`String`).
- Formulário `FiscalCadastroDto.Complementar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `dados*` (`FiscalCadastroDto.Dados`), `motivo*` (`String`).
- Formulário `FiscalCadastroDto.Referenciar`: `operacaoId*` (`UUID`), `armazemId*` (`Long`), `operacao*` (`String`), `versao*` (`Long`), `ncm` (`String`), `cfop` (`String`), `cest` (`String`), `enquadramento` (`String`), `aliquotaIcms` (`BigDecimal`), `aliquotaIpi` (`BigDecimal`), `fonte*` (`String`), `conferidaPor` (`String`), `conferidaEm` (`Instant`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FiscalCadastroController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FiscalCadastroController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#cliente`, `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#complementarCliente`, `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#armazem`, `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#complementarArmazem`, `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#referencias`, `backend/src/main/java/br/com/rodogarcia/wms/services/FiscalCadastroService.java#referenciar`.

### cadastros-9 — Encerramento e impedimentos

Visão principal: `ClienteController.listar`. Detalhe: `EncerramentoController.consultar`.

Tipos reais de registro: Clientes (`ClienteController.listar` → `EncerramentoController.consultar`); Armazéns (`ArmazemController.listar` → `EncerramentoController.consultar`); Produtos (`ProdutoController.listar` → `EncerramentoController.consultar`); Embalagens (`EmbalagemController.listar` → `EncerramentoController.consultar`); Endereços (`EnderecoController.listar` → `EncerramentoController.consultar`).

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar detalhe<br>`EncerramentoController.consultar` | detalhe do tipo de cadastro escolhido | `GET /api/v1/encerramentos/{tipo}/{id}/impedimentos`<br>`sem corpo` → `EncerramentoDto.Resultado` | GESTOR | tipo, id |
| Solicitar encerramento definitivo<br>`EncerramentoController.solicitar` | contexto do registro / etapa operacional | `POST /api/v1/encerramentos/{tipo}/{id}/solicitar`<br>`EncerramentoDto.Confirmar` → `EncerramentoDto.Resultado` | GESTOR | tipo, id |
| Confirmar inativação<br>`EncerramentoController.inativar` | contexto do registro / etapa operacional | `POST /api/v1/encerramentos/{tipo}/{id}/inativar`<br>`EncerramentoDto.Confirmar` → `EncerramentoDto.Resultado` | GESTOR | tipo, id |
| Resolver estoque remanescente<br>`EncerramentoController.remanescente` | contexto do registro / etapa operacional | `POST /api/v1/encerramentos/remanescente`<br>`EncerramentoDto.Remanescente` → `PedidoSaidaDto.Confirmacao` | GESTOR | nenhuma |
| Encerrar vigência identificada<br>`EncerramentoController.vigencia` | contexto do registro / etapa operacional | `POST /api/v1/encerramentos/vigencias/{tipo}/{id}/encerrar`<br>`EncerramentoDto.EncerrarVigencia` → `EncerramentoDto.Vigencia` | GESTOR | tipo, id |

Estados, validação e histórico: Consulte os compromissos antes de solicitar ou confirmar inativação. Não apague estoque ou histórico.

- Formulário `EncerramentoDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).
- Formulário `EncerramentoDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).
- Formulário `EncerramentoDto.Remanescente`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `referencia*` (`String`), `unidades*` (`List<EncerramentoDto.Unidade>`), `cargaInicialId` (`Long`), `etiquetas*` (`List<String>`), `justificativaFifo` (`String`), `motivo*` (`String`).
- Formulário `EncerramentoDto.EncerrarVigencia`: `operacaoId*` (`UUID`), `versao*` (`Long`), `corte*` (`LocalDate`), `resolucao*` (`FechamentoCobrancaDto.Resolucao`), `motivo*` (`String`).

Dependências de referência: [["Prévia Excel","ImportacaoEnderecoDto.Resultado"],["Encerramento","EncerramentoDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EncerramentoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EncerramentoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EncerramentoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EncerramentoService.java#solicitar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EncerramentoService.java#inativar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#resolverRemanescente`, `backend/src/main/java/br/com/rodogarcia/wms/services/EncerramentoService.java#encerrarVigencia`.

## Entrada e conferência

Ligação: `BE06` → `FE05`. Páginas: 4.

### entrada-1 — 1. Pedido de entrada

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`PedidoEntradaController.listar` | visão principal | `GET /api/v1/pedidos-entrada`<br>`sem corpo` → `PaginaResponse<PedidoEntradaDto.Resumo>` | OPERACAO | clienteId, armazemId |
| Consultar detalhe<br>`PedidoEntradaController.consultar` | detalhe do registro | `GET /api/v1/pedidos-entrada/{id}`<br>`sem corpo` → `PedidoEntradaDto.Detalhe` | OPERACAO | id |
| Novo registro<br>`PedidoEntradaController.criar` | cabeçalho | `POST /api/v1/pedidos-entrada`<br>`PedidoEntradaDto.Criar` → `PedidoEntradaDto.Resumo` | OPERACAO | nenhuma |

Estados, validação e histórico: Crie o pedido no cliente e armazém corretos. Selecione uma linha para continuar com o seu ID e revisão.

- Formulário `PedidoEntradaDto.Criar`: `clienteId*` (`Long`), `armazemId*` (`Long`), `referencia*` (`String`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#criar`.

### entrada-2 — 2. Notas e itens

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Registrar nota manual<br>`PedidoEntradaController.nota` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/notas`<br>`PedidoEntradaDto.NotaManual` → `PedidoEntradaDto.Resumo` | OPERACAO | id |
| Importar XML existente<br>`PedidoEntradaController.xml` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/notas/xml`<br>`PedidoEntradaDto.ImportarXml` → `PedidoEntradaDto.Resumo` | OPERACAO | id |

Estados, validação e histórico: Informe a nota manual ou importe o XML existente. Importação fiscal não confirma chegada física.

- Formulário `PedidoEntradaDto.NotaManual`: `versao*` (`Long`), `serie*` (`Integer`), `numero*` (`Long`), `emissao*` (`LocalDate`), `chaveAcesso` (`String`), `itens*` (`List<PedidoEntradaDto.ItemNota>`).
- Formulário `PedidoEntradaDto.ImportarXml`: `versao*` (`Long`), `xml*` (`String`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/NotaEntradaService.java#adicionar`, `backend/src/main/java/br/com/rodogarcia/wms/services/NotaEntradaService.java#importar`.

### entrada-3 — 3. Conferência e chegadas

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Iniciar conferência<br>`PedidoEntradaController.iniciar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/iniciar-conferencia`<br>`RevisaoCadastroRequest` → `PedidoEntradaDto.Resumo` | OPERACAO | id |
| Registrar chegada física<br>`PedidoEntradaController.chegada` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/chegadas`<br>`RecebimentoDto.RegistrarChegada` → `PedidoEntradaDto.Resumo` | OPERACAO | id |
| Consultar chegadas<br>`PedidoEntradaController.chegadas` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-entrada/{id}/chegadas`<br>`sem corpo` → `PaginaResponse<RecebimentoDto.Chegada>` | OPERACAO | id |
| Consultar detalhe<br>`PedidoEntradaController.consultar` | detalhe do registro | `GET /api/v1/pedidos-entrada/{id}`<br>`sem corpo` → `PedidoEntradaDto.Detalhe` | OPERACAO | id |

Estados, validação e histórico: Inicie a conferência, registre cada chegada real e compare previsto, recebido bom, avaria e diferença.

- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RecebimentoDto.RegistrarChegada`: `versao*` (`Long`), `operacaoId*` (`UUID`), `chegouEm*` (`Instant`), `observacao*` (`String`), `itens*` (`List<RecebimentoDto.Item>`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#iniciar`, `backend/src/main/java/br/com/rodogarcia/wms/services/RecebimentoService.java#registrarChegada`, `backend/src/main/java/br/com/rodogarcia/wms/services/RecebimentoService.java#listarChegadas`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#consultar`.

### entrada-4 — 4. Divergência e efetivação

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Efetivar carga integral<br>`PedidoEntradaController.efetivar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/efetivacao`<br>`PedidoEntradaDto.Efetivar` → `PedidoEntradaDto.Resumo` | SUPERVISOR | id |
| Estornar chegada<br>`PedidoEntradaController.estornar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/chegadas/{chegadaId}/estorno`<br>`RevisaoCadastroRequest` → `PedidoEntradaDto.Resumo` | SUPERVISOR | id, chegadaId |
| Registrar cancelamento<br>`PedidoEntradaController.cancelar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{id}/cancelamento`<br>`RevisaoCadastroRequest` → `PedidoEntradaDto.Resumo` | SUPERVISOR | id |
| Consultar entradas conferidas<br>`PedidoEntradaController.entradas` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-entrada/{id}/entradas`<br>`sem corpo` → `PaginaResponse<RecebimentoDto.Entrada>` | OPERACAO | id |

Estados, validação e histórico: Supervisor/Gestor registra a tratativa e efetiva a carga integral. Quarentena permanece indisponível; o servidor confirma o resultado.

- Formulário `PedidoEntradaDto.Efetivar`: `versao*` (`Long`), `aceitarDivergencias*` (`Boolean`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).
- Formulário `RevisaoCadastroRequest`: `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/RecebimentoService.java#efetivar`, `backend/src/main/java/br/com/rodogarcia/wms/services/RecebimentoService.java#estornar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#cancelar`, `backend/src/main/java/br/com/rodogarcia/wms/services/RecebimentoService.java#listarEntradas`.

## Unidades e etiquetas

Ligação: `BE07` → `FE06`. Páginas: 3.

### unidades-1 — 1. Distribuir quantidades

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

Leituras relacionadas: `PedidoEntradaController.entradas`, `UnidadeLogisticaController.progresso`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar progresso<br>`UnidadeLogisticaController.progresso` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-entrada/{pedidoId}/unitizacao`<br>`sem corpo` → `UnidadeLogisticaDto.Progresso` | OPERACAO | pedidoId |
| Distribuir em unidades<br>`UnidadeLogisticaController.unitizar` | cabeçalho | `POST /api/v1/pedidos-entrada/{pedidoId}/entradas/{entradaId}/unitizacao`<br>`UnidadeLogisticaDto.Unitizar` → `UnidadeLogisticaDto.Resultado` | OPERACAO | pedidoId, entradaId |
| Consultar lista<br>`UnidadeLogisticaController.listar` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-entrada/{pedidoId}/unidades`<br>`sem corpo` → `PaginaResponse<UnidadeLogisticaDto.Resumo>` | OPERACAO | pedidoId |

Estados, validação e histórico: Use as entradas conferidas. Quantidade de produto, DUN e identidade da unidade são campos distintos. O backend confere a soma.

- Formulário `UnidadeLogisticaDto.Unitizar`: `operacaoId*` (`UUID`), `versaoPedido*` (`Long`), `motivo*` (`String`), `unidades*` (`List<UnidadeLogisticaDto.NovaUnidade>`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#progresso`, `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#unitizar`, `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#listar`.

### unidades-2 — 2. Corrigir organização física

Visão principal: `EstoqueController.listar`. Detalhe: `UnidadeLogisticaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar detalhe<br>`UnidadeLogisticaController.consultar` | detalhe do registro | `GET /api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}`<br>`sem corpo` → `UnidadeLogisticaDto.Detalhe` | OPERACAO | pedidoId, unidadeId |
| Dividir unidade<br>`UnidadeLogisticaController.dividir` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}/divisao`<br>`UnidadeLogisticaDto.Dividir` → `UnidadeLogisticaDto.Resultado` | SUPERVISOR | pedidoId, unidadeId |
| Reagrupar unidades<br>`UnidadeLogisticaController.reagrupar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}/reagrupamento`<br>`UnidadeLogisticaDto.Reagrupar` → `UnidadeLogisticaDto.Resultado` | SUPERVISOR | pedidoId, unidadeId |

Estados, validação e histórico: Divisão e reagrupamento exigem Supervisor/Gestor e motivo. Preserve a origem e consulte novamente a etiqueta.

- Formulário `UnidadeLogisticaDto.Dividir`: `operacaoId*` (`UUID`), `versao*` (`Long`), `quantidadeNovaUnidade*` (`BigDecimal`), `motivo*` (`String`).
- Formulário `UnidadeLogisticaDto.Reagrupar`: `operacaoId*` (`UUID`), `versaoDestino*` (`Long`), `origens*` (`List<UnidadeLogisticaDto.RevisaoUnidade>`), `motivo*` (`String`).

Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#dividir`, `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#reagrupar`.

### unidades-3 — 3. Ler e reimprimir etiqueta

Visão principal: `EstoqueController.listar`. Detalhe: `UnidadeLogisticaController.lerCodigo`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Ler UUID da unidade<br>`UnidadeLogisticaController.lerCodigo` | detalhe do registro | `GET /api/v1/unidades-logisticas/{codigo}`<br>`sem corpo` → `UnidadeLogisticaDto.Detalhe` | OPERACAO | codigo |
| Consultar / reimprimir etiqueta<br>`UnidadeLogisticaController.etiqueta` | contexto do registro / etapa operacional | `GET /api/v1/unidades-logisticas/{codigo}/etiqueta`<br>`sem corpo` → `UnidadeLogisticaDto.Etiqueta` | OPERACAO | codigo |

Estados, validação e histórico: A etiqueta usa o UUID existente e conteúdo atual, sem endereço fixo. Impressão local é prévia; formato/equipamento do piloto permanecem pendentes.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/UnidadeLogisticaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#lerCodigo`, `backend/src/main/java/br/com/rodogarcia/wms/services/UnidadeLogisticaService.java#etiqueta`.

## Estoque e rastreabilidade

Ligação: `BE08 / BE14` → `FE08`. Páginas: 4.

### estoque-1 — Unidades e saldo por produto

Visão principal: `EstoqueController.listar`. Detalhe: `EstoqueController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`EstoqueController.listar` | visão principal | `GET /api/v1/estoque`<br>`sem corpo` → `PaginaResponse<EstoqueDto.Unidade>` | OPERACAO | clienteId, armazemId |
| Consultar saldo do SKU<br>`EstoqueController.saldo` | contexto do registro / etapa operacional | `GET /api/v1/estoque/saldo`<br>`sem corpo` → `EstoqueDto.Saldo` | OPERACAO | clienteId, armazemId, produtoId |
| Consultar detalhe<br>`EstoqueController.consultar` | detalhe do registro | `GET /api/v1/unidades-logisticas/{codigo}/estoque`<br>`sem corpo` → `EstoqueDto.Unidade` | OPERACAO | codigo |
| Consultar movimentos<br>`EstoqueController.historico` | contexto do registro / etapa operacional | `GET /api/v1/unidades-logisticas/{codigo}/movimentos`<br>`sem corpo` → `PaginaResponse<EstoqueDto.Movimento>` | OPERACAO | codigo |

Estados, validação e histórico: Consulte físico, disponível, reservado, triagem, quarentena e avaria. Um erro de consulta não significa saldo zero.


Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#saldo`, `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#historico`.

### estoque-2 — Bloqueio e liberação

Visão principal: `EstoqueController.listar`. Detalhe: `EstoqueController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Bloquear preventivamente<br>`EstoqueController.bloquear` | contexto do registro / etapa operacional | `POST /api/v1/unidades-logisticas/{codigo}/bloqueio`<br>`EstoqueDto.Bloqueio` → `EstoqueDto.Confirmacao` | OPERACAO | codigo |
| Confirmar liberação<br>`EstoqueController.liberar` | contexto do registro / etapa operacional | `POST /api/v1/unidades-logisticas/{codigo}/liberacao`<br>`EstoqueDto.Bloqueio` → `EstoqueDto.Confirmacao` | SUPERVISOR | codigo |
| Registrar bloqueio por avaria<br>`EstoqueController.avariar` | contexto do registro / etapa operacional | `POST /api/v1/unidades-logisticas/{codigo}/avaria`<br>`EstoqueDto.Bloqueio` → `EstoqueDto.Confirmacao` | OPERACAO | codigo |

Estados, validação e histórico: Bloqueio preventivo não altera saldo nem cancela reserva. Liberação é confirmada no backend por Supervisor/Gestor.

- Formulário `EstoqueDto.Bloqueio`: `operacaoId*` (`UUID`), `versaoUnidade*` (`Long`), `motivo*` (`String`).
- Formulário `EstoqueDto.Bloqueio`: `operacaoId*` (`UUID`), `versaoUnidade*` (`Long`), `motivo*` (`String`).
- Formulário `EstoqueDto.Bloqueio`: `operacaoId*` (`UUID`), `versaoUnidade*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/MovimentacaoEstoqueService.java#bloquear`, `backend/src/main/java/br/com/rodogarcia/wms/services/MovimentacaoEstoqueService.java#liberar`, `backend/src/main/java/br/com/rodogarcia/wms/services/MovimentacaoEstoqueService.java#avariar`.

### estoque-3 — Avarias e reparos

Visão principal: `EstoqueController.listar`. Detalhe: `EstoqueController.consultar`.

Leituras relacionadas: `AvariaController.listar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`AvariaController.listar` | contexto do registro / etapa operacional | `GET /api/v1/estoque/unidades/{codigo}/avarias`<br>`sem corpo` → `List<AvariaDto.Ocorrencia>` | OPERACAO | codigo |
| Registrar fato<br>`AvariaController.registrar` | cabeçalho | `POST /api/v1/estoque/unidades/{codigo}/avarias`<br>`AvariaDto.Registrar` → `AvariaDto.Confirmacao` | SUPERVISOR | codigo |
| Reconhecer responsabilidade<br>`AvariaController.reconhecer` | contexto do registro / etapa operacional | `POST /api/v1/estoque/unidades/{codigo}/avarias/{id}/responsabilidade`<br>`AvariaDto.Reconhecer` → `AvariaDto.Confirmacao` | GESTOR | codigo, id |
| Registrar reparo<br>`AvariaController.reparar` | contexto do registro / etapa operacional | `POST /api/v1/estoque/unidades/{codigo}/avarias/{id}/reparo`<br>`AvariaDto.Reparar` → `AvariaDto.Confirmacao` | SUPERVISOR | codigo, id |

Estados, validação e histórico: Registre o fato, quantidade afetada e destino. Reconhecimento de responsabilidade é do Gestor; preserve datas e reservas.

- Formulário `AvariaDto.Registrar`: `operacaoId*` (`UUID`), `versaoUnidade*` (`Long`), `quantidade*` (`BigDecimal`), `ocorridaEm*` (`Instant`), `conjuntoId` (`Long`), `destinos*` (`List<EstoqueDto.Destino>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).
- Formulário `AvariaDto.Reconhecer`: `operacaoId*` (`UUID`), `versao*` (`Long`), `responsabilidade*` (`String`), `motivo*` (`String`).
- Formulário `AvariaDto.Reparar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `versaoUnidade*` (`Long`), `conjuntoId` (`Long`), `destinos*` (`List<EstoqueDto.Destino>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/AvariaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/AvariaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/AvariaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/AvariaService.java#registrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/AvariaService.java#reconhecer`, `backend/src/main/java/br/com/rodogarcia/wms/services/AvariaService.java#reparar`.

### estoque-4 — Indicadores e validade

Visão principal: `IndicadorEstoqueController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`IndicadorEstoqueController.listar` | visão principal | `GET /api/v1/indicadores-estoque`<br>`sem corpo` → `PaginaResponse<IndicadorEstoqueDto.Resultado>` | OPERACAO | clienteId, armazemId, fuso |
| Consultar detalhe<br>`IndicadorEstoqueController.consultar` | contexto do registro / etapa operacional | `GET /api/v1/avisos-validade`<br>`sem corpo` → `IndicadorEstoqueDto.Configuracao` | OPERACAO | clienteId, armazemId |
| Configurar<br>`IndicadorEstoqueController.configurar` | cabeçalho | `PUT /api/v1/avisos-validade`<br>`IndicadorEstoqueDto.ConfigurarAviso` → `IndicadorEstoqueDto.Configuracao` | GESTOR | nenhuma |

Estados, validação e histórico: Valor é consultado somente por Supervisor/Gestor. FIFO, chegada real e início da cobrança são datas diferentes.

- Formulário `IndicadorEstoqueDto.ConfigurarAviso`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `versao*` (`Long`), `diasAntecedencia*` (`Integer`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/IndicadorEstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/IndicadorEstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/IndicadorEstoqueService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/IndicadorEstoqueService.java#configuracao`, `backend/src/main/java/br/com/rodogarcia/wms/services/IndicadorEstoqueService.java#configurar`.

## Saída, FIFO e reserva

Ligação: `BE09 / BE10` → `FE09`. Páginas: 4.

### saida-1 — 1. Pedido integral

Visão principal: `PedidoSaidaController.listar`. Detalhe: `PedidoSaidaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`PedidoSaidaController.listar` | visão principal | `GET /api/v1/pedidos-saida`<br>`sem corpo` → `PaginaResponse<PedidoSaidaDto.Detalhe>` | OPERACAO | clienteId, armazemId |
| Consultar detalhe<br>`PedidoSaidaController.consultar` | detalhe do registro | `GET /api/v1/pedidos-saida/{id}`<br>`sem corpo` → `PedidoSaidaDto.Detalhe` | OPERACAO | id |
| Novo registro<br>`PedidoSaidaController.criar` | cabeçalho | `POST /api/v1/pedidos-saida`<br>`PedidoSaidaDto.Criar` → `PedidoSaidaDto.Confirmacao` | OPERACAO | nenhuma |
| Criar pedido por XML<br>`PedidoSaidaController.importarXml` | cabeçalho | `POST /api/v1/pedidos-saida/xml`<br>`PedidoSaidaDto.ImportarXml` → `PedidoSaidaDto.ConfirmacaoXml` | OPERACAO | nenhuma |

Estados, validação e histórico: Crie manualmente ou importe XML existente. Não há liberação parcial do pedido.

- Formulário `PedidoSaidaDto.Criar`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `referencia*` (`String`), `itens*` (`List<PedidoSaidaDto.ItemCriar>`), `motivo*` (`String`).
- Formulário `PedidoSaidaDto.ImportarXml`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `xml*` (`String`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaXmlService.java#importar`.

### saida-2 — 2. Sugestão FIFO e reserva

Visão principal: `PedidoSaidaController.listar`. Detalhe: `PedidoSaidaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar sugestão FIFO<br>`PedidoSaidaController.sugerir` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-saida/{id}/fifo`<br>`sem corpo` → `PedidoSaidaDto.Sugestao` | OPERACAO | id |
| Justificar exceção FIFO<br>`PedidoSaidaController.justificar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/justificativas-fifo`<br>`PedidoSaidaDto.Justificar` → `PedidoSaidaDto.Confirmacao` | OPERACAO | id |
| Confirmar reserva integral<br>`PedidoSaidaController.reservar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/reserva`<br>`PedidoSaidaDto.Reservar` → `PedidoSaidaDto.Confirmacao` | OPERACAO | id |
| Revalidar disponibilidade<br>`PedidoSaidaController.revalidar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/revalidacao`<br>`sem corpo` → `PedidoSaidaDto.Detalhe` | OPERACAO | id |

Estados, validação e histórico: Consulte a sugestão do backend e transcreva/seleciona as unidades. A reserva só existe após confirmação. Exceção FIFO exige justificativa e Supervisor/Gestor.

- Formulário `PedidoSaidaDto.Justificar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `selecoes*` (`List<PedidoSaidaDto.Selecao>`), `motivo*` (`String`).
- Formulário `PedidoSaidaDto.Reservar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `justificativaId` (`UUID`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#sugerir`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#justificar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#reservar`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#revalidar`.

### saida-3 — 3. Leitura e separação

Visão principal: `PedidoSaidaController.listar`. Detalhe: `ExpedicaoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar detalhe<br>`ExpedicaoController.consultar` | detalhe do registro | `GET /api/v1/pedidos-saida/{id}/expedicao`<br>`sem corpo` → `ExpedicaoDto.Detalhe` | OPERACAO | id |
| Conferir leitura da reserva<br>`ExpedicaoController.ler` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/leituras`<br>`ExpedicaoDto.Leitura` → `ExpedicaoDto.Confirmacao` | OPERACAO | id |
| Confirmar separação<br>`ExpedicaoController.separar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/separacoes`<br>`ExpedicaoDto.Separar` → `ExpedicaoDto.Confirmacao` | OPERACAO | id |

Estados, validação e histórico: Confirme UUID e revisão do conteúdo da etiqueta. Destino de separação e remanescente são explícitos. Avaria pode bloquear a expedição.

- Formulário `ExpedicaoDto.Leitura`: `operacaoId*` (`UUID`), `versao*` (`Long`), `reservaId*` (`Long`), `codigoLido*` (`UUID`), `revisaoConteudo*` (`Long`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).
- Formulário `ExpedicaoDto.Separar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `destinacao*` (`ExpedicaoDto.Destinacao`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#ler`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#separar`.

### saida-4 — 4. Reversão e cancelamento

Visão principal: `PedidoSaidaController.listar`. Detalhe: `PedidoSaidaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Reverter reserva<br>`PedidoSaidaController.reverter` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/reversao-reserva`<br>`PedidoSaidaDto.Comando` → `PedidoSaidaDto.Confirmacao` | SUPERVISOR | id |
| Registrar cancelamento<br>`PedidoSaidaController.cancelar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/cancelamento`<br>`PedidoSaidaDto.Comando` → `PedidoSaidaDto.Confirmacao` | SUPERVISOR | id |

Estados, validação e histórico: A reserva não vence automaticamente. Consulte consequências antes de cancelar ou reverter.

- Formulário `PedidoSaidaDto.Comando`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).
- Formulário `PedidoSaidaDto.Comando`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#reverter`, `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#cancelar`.

## Fiscal e retirada

Ligação: `BE10 / BE11` → `FE10`. Páginas: 3.

### fiscal-1 — 1. Documento de mercadoria

Visão principal: `PedidoSaidaController.listar`. Detalhe: `ExpedicaoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar detalhe<br>`ExpedicaoController.consultar` | detalhe do registro | `GET /api/v1/pedidos-saida/{id}/expedicao`<br>`sem corpo` → `ExpedicaoDto.Detalhe` | OPERACAO | id |
| Registrar documento existente<br>`ExpedicaoController.documento` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/documentos`<br>`ExpedicaoDto.Documento` → `ExpedicaoDto.Confirmacao` | SUPERVISOR | id |
| Registrar cancelamento<br>`ExpedicaoController.cancelar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/documentos/{documentoId}/cancelamento`<br>`ExpedicaoDto.CancelarDocumento` → `ExpedicaoDto.Confirmacao` | SUPERVISOR | id, documentoId |

Estados, validação e histórico: NOTAZZ registra nota/protocolo/cobertura; XML é alternativa de registro. Nenhuma ação aqui emite nota no sistema externo.

- Formulário `ExpedicaoDto.Documento`: `operacaoId*` (`UUID`), `versao*` (`Long`), `origem*` (`String`), `natureza*` (`String`), `nota` (`ExpedicaoDto.Nota`), `protocolo*` (`String`), `xml` (`String`), `coberturas*` (`List<ExpedicaoDto.Cobertura>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).
- Formulário `ExpedicaoDto.CancelarDocumento`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Documento fiscal","ExpedicaoDto.DocumentoRegistrado"],["Baixa de origem","ExpedicaoDto.Baixa"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#registrarDocumento`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#cancelarDocumento`.

### fiscal-2 — 2. Retirada física integral

Visão principal: `PedidoSaidaController.listar`. Detalhe: `ExpedicaoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Confirmar retirada física<br>`ExpedicaoController.retirar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/retirada`<br>`ExpedicaoDto.Retirar` → `ExpedicaoDto.Confirmacao` | SUPERVISOR | id |
| Consultar fatos físicos<br>`ExpedicaoController.fatos` | contexto do registro / etapa operacional | `GET /api/v1/pedidos-saida/{id}/fatos`<br>`sem corpo` → `List<ExpedicaoDto.Fato>` | OPERACAO | id |

Estados, validação e histórico: Supervisor/Gestor confirma retirada real, com XMLs comprovantes e destinos de todos os remanescentes. Documento registrado e retirada são fatos diferentes.

- Formulário `ExpedicaoDto.Retirar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `xmls*` (`List<String>`), `remanescentes*` (`List<ExpedicaoDto.Destinacao>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Documento fiscal","ExpedicaoDto.DocumentoRegistrado"],["Baixa de origem","ExpedicaoDto.Baixa"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#retirar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#consultar`.

### fiscal-3 — 3. Retorno e devolução

Visão principal: `PedidoSaidaController.listar`. Detalhe: `ExpedicaoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Retornar às posições<br>`ExpedicaoController.retornar` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/retorno-interno`<br>`ExpedicaoDto.Retornar` → `ExpedicaoDto.Confirmacao` | SUPERVISOR | id |
| Registrar devolução / nova entrada<br>`ExpedicaoController.devolver` | contexto do registro / etapa operacional | `POST /api/v1/pedidos-saida/{id}/devolucoes`<br>`ExpedicaoDto.Devolver` → `ExpedicaoDto.Confirmacao` | SUPERVISOR | id |

Estados, validação e histórico: Retorno interno exige destinos e documento cancelado. Devolução após saída cria nova entrada ligada à baixa original, preservando FIFO.

- Formulário `ExpedicaoDto.Retornar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `unidades*` (`List<ExpedicaoDto.Destinacao>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).
- Formulário `ExpedicaoDto.Devolver`: `operacaoId*` (`UUID`), `versao*` (`Long`), `referencia*` (`String`), `nota*` (`ExpedicaoDto.Nota`), `chegadaReal*` (`Instant`), `itens*` (`List<ExpedicaoDto.ItemDevolucao>`), `motivo*` (`String`), `resolverPendentes` (`Boolean`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Documento fiscal","ExpedicaoDto.DocumentoRegistrado"],["Baixa de origem","ExpedicaoDto.Baixa"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ExpedicaoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#retornarInterno`, `backend/src/main/java/br/com/rodogarcia/wms/services/ExpedicaoService.java#devolver`.

## Serviços e tabelas

Ligação: `BE05 / BE12` → `FE04 / FE11`. Páginas: 5.

### precos-1 — Serviços contratados

Visão principal: `ConfiguracaoCobrancaController.servicos`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar serviços<br>`ConfiguracaoCobrancaController.servicos` | visão principal | `GET /api/v1/servicos-cobranca`<br>`sem corpo` → `PaginaResponse<ConfiguracaoCobrancaDto.Servico>` | SUPERVISOR | nenhuma |
| Cadastrar serviço<br>`ConfiguracaoCobrancaController.criarServico` | cabeçalho | `POST /api/v1/servicos-cobranca`<br>`ConfiguracaoCobrancaDto.CriarServico` → `ConfiguracaoCobrancaDto.Servico` | GESTOR | nenhuma |

Estados, validação e histórico: Preços e parâmetros são configuração. Exemplos fictícios não são tabela comercial aprovada.

- Formulário `ConfiguracaoCobrancaDto.CriarServico`: `operacaoId*` (`UUID`), `codigo*` (`String`), `descricao*` (`String`), `tipo*` (`String`), `unidade*` (`String`), `motivo*` (`String`).

Dependências de referência: [["Serviço configurado","ConfiguracaoCobrancaDto.Servico"],["Tabela","ConfiguracaoCobrancaDto.Tabela"],["Vínculo","ConfiguracaoCobrancaDto.Vinculo"],["Contrato","ConfiguracaoCobrancaDto.Contrato"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#servicos`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#criarServico`.

### precos-2 — Tabelas e vigências

Visão principal: `ConfiguracaoCobrancaController.tabelas`. Detalhe: `ConfiguracaoCobrancaController.tabela`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar tabelas<br>`ConfiguracaoCobrancaController.tabelas` | visão principal | `GET /api/v1/tabelas-cobranca`<br>`sem corpo` → `List<ConfiguracaoCobrancaDto.Tabela>` | SUPERVISOR | armazemId |
| Consultar tabela<br>`ConfiguracaoCobrancaController.tabela` | detalhe do registro | `GET /api/v1/tabelas-cobranca/{id}`<br>`sem corpo` → `ConfiguracaoCobrancaDto.Tabela` | SUPERVISOR | id |
| Criar tabela e itens<br>`ConfiguracaoCobrancaController.criarTabela` | cabeçalho | `POST /api/v1/tabelas-cobranca`<br>`ConfiguracaoCobrancaDto.CriarTabela` → `ConfiguracaoCobrancaDto.Tabela` | GESTOR | nenhuma |
| Encerrar vigência da tabela<br>`ConfiguracaoCobrancaController.encerrarTabela` | contexto do registro / etapa operacional | `POST /api/v1/tabelas-cobranca/{id}/encerramento`<br>`ConfiguracaoCobrancaDto.Encerrar` → `ConfiguracaoCobrancaDto.Tabela` | GESTOR | id |

Estados, validação e histórico: Preserve histórico e intervalos de vigência. Não preencha parâmetro ausente com zero.

- Formulário `ConfiguracaoCobrancaDto.CriarTabela`: `operacaoId*` (`UUID`), `armazemId*` (`Long`), `clienteId` (`Long`), `codigo*` (`String`), `descricao*` (`String`), `tipo*` (`String`), `vigenciaInicio*` (`LocalDate`), `vigenciaFim` (`LocalDate`), `itens*` (`List<ConfiguracaoCobrancaDto.Item>`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `ConfiguracaoCobrancaDto.Encerrar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `vigenciaFim*` (`LocalDate`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Serviço configurado","ConfiguracaoCobrancaDto.Servico"],["Tabela","ConfiguracaoCobrancaDto.Tabela"],["Vínculo","ConfiguracaoCobrancaDto.Vinculo"],["Contrato","ConfiguracaoCobrancaDto.Contrato"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#tabelas`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#tabela`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#criarTabela`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#encerrarTabela`.

### precos-3 — Vínculos por cliente

Visão principal: `ConfiguracaoCobrancaController.vinculos`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar vínculos<br>`ConfiguracaoCobrancaController.vinculos` | visão principal | `GET /api/v1/vinculos-tabela`<br>`sem corpo` → `List<ConfiguracaoCobrancaDto.Vinculo>` | SUPERVISOR | clienteId, armazemId |
| Vincular tabela ao cliente<br>`ConfiguracaoCobrancaController.vincular` | cabeçalho | `POST /api/v1/vinculos-tabela`<br>`ConfiguracaoCobrancaDto.Vincular` → `ConfiguracaoCobrancaDto.Vinculo` | GESTOR | nenhuma |
| Encerrar vínculo<br>`ConfiguracaoCobrancaController.encerrarVinculo` | contexto do registro / etapa operacional | `POST /api/v1/vinculos-tabela/{id}/encerramento`<br>`ConfiguracaoCobrancaDto.Encerrar` → `ConfiguracaoCobrancaDto.Vinculo` | GESTOR | id |

Estados, validação e histórico: Vincule uma tabela no contexto correto. Encerramento não apaga cálculo anterior.

- Formulário `ConfiguracaoCobrancaDto.Vincular`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `tabelaId*` (`Long`), `vigenciaInicio*` (`LocalDate`), `vigenciaFim` (`LocalDate`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `ConfiguracaoCobrancaDto.Encerrar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `vigenciaFim*` (`LocalDate`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Serviço configurado","ConfiguracaoCobrancaDto.Servico"],["Tabela","ConfiguracaoCobrancaDto.Tabela"],["Vínculo","ConfiguracaoCobrancaDto.Vinculo"],["Contrato","ConfiguracaoCobrancaDto.Contrato"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#vinculos`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#vincular`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#encerrarVinculo`.

### precos-4 — Contrato de fechamento

Visão principal: `ConfiguracaoCobrancaController.contratos`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar contratos<br>`ConfiguracaoCobrancaController.contratos` | visão principal | `GET /api/v1/contratos-cobranca`<br>`sem corpo` → `List<ConfiguracaoCobrancaDto.Contrato>` | SUPERVISOR | clienteId, armazemId |
| Configurar<br>`ConfiguracaoCobrancaController.configurar` | cabeçalho | `POST /api/v1/contratos-cobranca`<br>`ConfiguracaoCobrancaDto.ConfigurarContrato` → `ConfiguracaoCobrancaDto.Contrato` | GESTOR | nenhuma |
| Encerrar contrato<br>`ConfiguracaoCobrancaController.encerrarContrato` | contexto do registro / etapa operacional | `POST /api/v1/contratos-cobranca/{id}/encerramento`<br>`ConfiguracaoCobrancaDto.Encerrar` → `ConfiguracaoCobrancaDto.Contrato` | GESTOR | id |

Estados, validação e histórico: Configure corte, mínimo e GRIS somente com dados do contrato. Convenções AC04–08 permanecem propostas para validação comercial.

- Formulário `ConfiguracaoCobrancaDto.ConfigurarContrato`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `vigenciaInicio*` (`LocalDate`), `vigenciaFim` (`LocalDate`), `fuso*` (`String`), `moeda*` (`String`), `modalidadeCiclo*` (`String`), `diaCorte` (`Integer`), `duracaoDias` (`Integer`), `minimoModo*` (`String`), `grisModo*` (`String`), `minimoValor` (`BigDecimal`), `minimoProporcao` (`String`), `servicosMinimo*` (`List<Long>`), `grisPercentual` (`BigDecimal`), `grisBase` (`String`), `grisPeriodicidade` (`String`), `grisProporcao` (`String`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `ConfiguracaoCobrancaDto.Encerrar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `vigenciaFim*` (`LocalDate`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Serviço configurado","ConfiguracaoCobrancaDto.Servico"],["Tabela","ConfiguracaoCobrancaDto.Tabela"],["Vínculo","ConfiguracaoCobrancaDto.Vinculo"],["Contrato","ConfiguracaoCobrancaDto.Contrato"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ConfiguracaoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#contratos`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#configurar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java#encerrarContrato`.

### precos-5 — Histórico do vínculo

Visão principal: `AuditoriaController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`AuditoriaController.listar` | visão principal | `GET /api/v1/auditoria`<br>`sem corpo` → `PaginaResponse<AuditoriaResponse>` | GESTOR | tipo, registroId |

Estados, validação e histórico: Gestor consulta o vínculo selecionado. Legado ambíguo tem consulta separada e não é atribuído automaticamente ao vínculo.


Dependências de referência: [["Serviço configurado","ConfiguracaoCobrancaDto.Servico"],["Tabela","ConfiguracaoCobrancaDto.Tabela"],["Vínculo","ConfiguracaoCobrancaDto.Vinculo"],["Contrato","ConfiguracaoCobrancaDto.Contrato"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/AuditoriaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/AuditoriaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/AuditoriaService.java#listar`.

## Serviços e cálculo

Ligação: `BE12` → `FE11`. Páginas: 3.

### cobranca-1 — 1. Fatos e origem

Visão principal: `FatoServicoController.listar`. Detalhe: `FatoServicoController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`FatoServicoController.listar` | visão principal | `GET /api/v1/fatos-servico`<br>`sem corpo` → `PaginaResponse<FatoServicoDto.Fato>` | SUPERVISOR | clienteId, armazemId |
| Consultar sugestões de serviço<br>`FatoServicoController.sugestoes` | contexto do registro / etapa operacional | `GET /api/v1/fatos-servico/sugestoes`<br>`sem corpo` → `PaginaResponse<FatoServicoDto.Sugestao>` | SUPERVISOR | clienteId, armazemId, servicoId |
| Consultar detalhe<br>`FatoServicoController.consultar` | detalhe do registro | `GET /api/v1/fatos-servico/{id}`<br>`sem corpo` → `FatoServicoDto.Fato` | SUPERVISOR | id |
| Registrar fato<br>`FatoServicoController.registrar` | cabeçalho | `POST /api/v1/fatos-servico`<br>`FatoServicoDto.Registrar` → `FatoServicoDto.Fato` | SUPERVISOR | nenhuma |
| Anular fato identificado<br>`FatoServicoController.anular` | contexto do registro / etapa operacional | `POST /api/v1/fatos-servico/{id}/anulacao`<br>`FatoServicoDto.Anular` → `FatoServicoDto.Fato` | GESTOR | id |

Estados, validação e histórico: Consulte sugestões e registre execução real. Adicional pode ser associado posteriormente ao pedido pertinente; não crie saída fictícia.

- Formulário `FatoServicoDto.Registrar`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `servicoId*` (`Long`), `origem*` (`String`), `unidadeId` (`Long`), `pedidoEntradaId` (`Long`), `pedidoSaidaId` (`Long`), `produtoId` (`Long`), `referenciaExecucao` (`String`), `executadoEm` (`Instant`), `quantidade` (`BigDecimal`), `categoria*` (`String`), `valorBase` (`BigDecimal`), `cotas*` (`List<FatoServicoDto.Cota>`), `criterioRateio*` (`String`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FatoServicoDto.Anular`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Fato","FatoServicoDto.Fato"],["Origem consultada","FatoServicoDto.Sugestao"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FatoServicoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FatoServicoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FatoServicoService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FatoServicoService.java#sugestoes`, `backend/src/main/java/br/com/rodogarcia/wms/services/FatoServicoService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FatoServicoService.java#registrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FatoServicoService.java#anular`.

### cobranca-2 — 2. Avaria financeira

Visão principal: `FatoServicoController.marcos`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar marcos financeiros<br>`FatoServicoController.marcos` | visão principal | `GET /api/v1/avarias/{id}/marcos-financeiros`<br>`sem corpo` → `List<FatoServicoDto.MarcoResposta>` | SUPERVISOR | id |
| Registrar marco financeiro<br>`FatoServicoController.marco` | cabeçalho | `POST /api/v1/avarias/{id}/marcos-financeiros`<br>`FatoServicoDto.Marco` → `FatoServicoDto.MarcoResposta` | GESTOR | id |

Estados, validação e histórico: Gestor registra marcos identificados. Condição física e suspensão financeira são informações distintas.

- Formulário `FatoServicoDto.Marco`: `operacaoId*` (`UUID`), `fatoPermanenciaId*` (`Long`), `quantidadeAfetada*` (`BigDecimal`), `motivo*` (`String`).

Dependências de referência: [["Fato","FatoServicoDto.Fato"],["Origem consultada","FatoServicoDto.Sugestao"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FatoServicoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FatoServicoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/MarcoFinanceiroAvariaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/MarcoFinanceiroAvariaService.java#registrar`.

### cobranca-3 — 3. Memória do servidor

Visão principal: `CalculoCobrancaController.listar`. Detalhe: `CalculoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`CalculoCobrancaController.listar` | visão principal | `GET /api/v1/calculos-cobranca`<br>`sem corpo` → `PaginaResponse<CalculoCobrancaDto.Resultado>` | SUPERVISOR | clienteId, armazemId |
| Consultar detalhe<br>`CalculoCobrancaController.consultar` | detalhe do registro | `GET /api/v1/calculos-cobranca/{id}`<br>`sem corpo` → `CalculoCobrancaDto.Resultado` | SUPERVISOR | id |
| Solicitar cálculo ao servidor<br>`CalculoCobrancaController.calcular` | cabeçalho | `POST /api/v1/calculos-cobranca`<br>`CalculoCobrancaDto.Calcular` → `CalculoCobrancaDto.Resultado` | SUPERVISOR | nenhuma |

Estados, validação e histórico: Solicite cálculo e confira origens, diárias, pico cobrável, vigências, mínimo, GRIS e pendências. O navegador não recalcula valores.

- Formulário `CalculoCobrancaDto.Calcular`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `periodoInicio*` (`LocalDate`), `periodoFim*` (`LocalDate`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Fato","FatoServicoDto.Fato"],["Origem consultada","FatoServicoDto.Sugestao"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/CalculoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/CalculoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java#calcular`.

## Fechamentos e ESL

Ligação: `BE13` → `FE11`. Páginas: 5.

### fechamento-1 — 1. Ciclo e versões

Visão principal: `FechamentoCobrancaController.listar`. Detalhe: `FechamentoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`FechamentoCobrancaController.listar` | visão principal | `GET /api/v1/fechamentos-cobranca`<br>`sem corpo` → `PaginaResponse<FechamentoCobrancaDto.Fechamento>` | SUPERVISOR | clienteId, armazemId |
| Consultar detalhe<br>`FechamentoCobrancaController.consultar` | detalhe do registro | `GET /api/v1/fechamentos-cobranca/{id}`<br>`sem corpo` → `FechamentoCobrancaDto.Fechamento` | SUPERVISOR | id |
| Preparar<br>`FechamentoCobrancaController.preparar` | cabeçalho | `POST /api/v1/fechamentos-cobranca`<br>`FechamentoCobrancaDto.Preparar` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | nenhuma |
| Consultar versões<br>`FechamentoCobrancaController.versoes` | contexto do registro / etapa operacional | `GET /api/v1/fechamentos-cobranca/{id}/versoes`<br>`sem corpo` → `List<FechamentoCobrancaDto.Versao>` | SUPERVISOR | id |
| Consultar versão e hash<br>`FechamentoCobrancaController.versao` | contexto do registro / etapa operacional | `GET /api/v1/fechamentos-cobranca/{id}/versoes/{numero}`<br>`sem corpo` → `FechamentoCobrancaDto.Versao` | SUPERVISOR | id, numero |
| Baixar demonstrativo guardado<br>`FechamentoCobrancaController.demonstrativo` | contexto do registro / etapa operacional | `GET /api/v1/fechamentos-cobranca/{id}/versoes/{numero}/demonstrativo`<br>`sem corpo` → `FechamentoCobrancaDto.Demonstrativo` | SUPERVISOR | id, numero |

Estados, validação e histórico: Prepare o ciclo com cálculo identificado. Consulte a versão e baixe os bytes do demonstrativo; consulta não registra entrega.

- Formulário `FechamentoCobrancaDto.Preparar`: `operacaoId*` (`UUID`), `calculoId*` (`Long`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#preparar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#versoes`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#versao`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#demonstrativo`.

### fechamento-2 — 2. Decisão integral do Gestor

Visão principal: `FechamentoCobrancaController.listar`. Detalhe: `FechamentoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Aprovar ciclo integral<br>`FechamentoCobrancaController.aprovar` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/aprovacao`<br>`FechamentoCobrancaDto.Decidir` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |
| Rejeitar ciclo integral<br>`FechamentoCobrancaController.rejeitar` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/rejeicao`<br>`FechamentoCobrancaDto.Decidir` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |
| Reabrir ciclo<br>`FechamentoCobrancaController.reabrir` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/reabertura`<br>`FechamentoCobrancaDto.Reabrir` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |

Estados, validação e histórico: Aprove/rejeite o ciclo inteiro. Reabertura exige condições confirmadas pelo servidor; preserve a versão antiga.

- Formulário `FechamentoCobrancaDto.Decidir`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FechamentoCobrancaDto.Decidir`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FechamentoCobrancaDto.Reabrir`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `calculoId*` (`Long`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#aprovar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#rejeitar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#reabrir`.

### fechamento-3 — 3. Entrega manual e NFS-e

Visão principal: `FechamentoCobrancaController.listar`. Detalhe: `FechamentoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Registrar entrega manual<br>`FechamentoCobrancaController.entregar` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/entregas`<br>`FechamentoCobrancaDto.Entregar` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |
| Registrar declaração de não emissão<br>`FechamentoCobrancaController.confirmarNaoEmissao` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/confirmacoes-externas`<br>`FechamentoCobrancaDto.Confirmar` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |
| Registrar referência NFS-e<br>`FechamentoCobrancaController.registrarNfse` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/referencias-nfse`<br>`FechamentoCobrancaDto.Nfse` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |
| Resolver saldo financeiro<br>`FechamentoCobrancaController.resolverSaldo` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/resolucao-financeira`<br>`FechamentoCobrancaDto.Resolver` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |

Estados, validação e histórico: Registre entrega efetuada fora do WMS, declaração de não emissão ou referência NFS-e existente. Não há envio ao ESL nem emissão automática.

- Formulário `FechamentoCobrancaDto.Entregar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `layoutVersao*` (`int`), `arquivoHash*` (`String`), `destinoReferencia*` (`String`), `entregueEm*` (`Instant`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FechamentoCobrancaDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `entregaId` (`Long`), `fonte*` (`String`), `confirmadaPor*` (`String`), `confirmadaEm*` (`Instant`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FechamentoCobrancaDto.Nfse`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `emissorDocumento*` (`String`), `referenciaExterna*` (`String`), `numeroDocumento` (`String`), `serie` (`String`), `emitidaEm*` (`Instant`), `fonte*` (`String`), `conferidaPor*` (`String`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).
- Formulário `FechamentoCobrancaDto.Resolver`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numero*` (`int`), `referenciaExterna*` (`String`), `fonte*` (`String`), `confirmadaPor*` (`String`), `confirmadaEm*` (`Instant`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#entregar`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#confirmarNaoEmissao`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#registrarNfse`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#resolverSaldo`.

### fechamento-4 — 4. Conflitos e ajustes

Visão principal: `FechamentoCobrancaController.listar`. Detalhe: `FechamentoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar tratativas<br>`FechamentoCobrancaController.tratativas` | contexto do registro / etapa operacional | `GET /api/v1/fechamentos-cobranca/{id}/tratativas-externas`<br>`sem corpo` → `List<FechamentoCobrancaDto.Tratativa>` | SUPERVISOR | id |
| Registrar tratativa externa<br>`FechamentoCobrancaController.tratar` | contexto do registro / etapa operacional | `POST /api/v1/fechamentos-cobranca/{id}/tratativas-externas`<br>`FechamentoCobrancaDto.Tratar` → `FechamentoCobrancaDto.ConfirmacaoComando` | GESTOR | id |

Estados, validação e histórico: Trate conflito externo e ajustes por origem/base/delta sem alterar documento emitido ou cálculo fechado.

- Formulário `FechamentoCobrancaDto.Tratar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `numeroResultado*` (`int`), `resultado*` (`String`), `referencias*` (`List<FechamentoCobrancaDto.DocumentoConferido>`), `fonte*` (`String`), `conferidaPor*` (`String`), `conferidaEm*` (`Instant`), `motivo*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`), `destinoAjustesId` (`Long`), `versaoDestinoAjustes` (`Long`), `regularizacaoOrigem` (`FechamentoCobrancaDto.RegularizacaoOrigem`).

Dependências de referência: [["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/FechamentoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#tratativas`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#tratarExterno`.

### fechamento-5 — Ajustes de ciclo

Visão principal: `AjusteFechamentoController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`AjusteFechamentoController.listar` | visão principal | `GET /api/v1/ajustes-fechamento`<br>`sem corpo` → `List<FechamentoCobrancaDto.Ajuste>` | SUPERVISOR | clienteId, armazemId |
| Registrar ajuste<br>`AjusteFechamentoController.ajustar` | cabeçalho | `POST /api/v1/ajustes-fechamento`<br>`FechamentoCobrancaDto.Ajustar` → `FechamentoCobrancaDto.Ajuste` | GESTOR | nenhuma |

Estados, validação e histórico: Ajuste é identificado e mantém a origem. O servidor calcula/verifica consequências.

- Formulário `FechamentoCobrancaDto.Ajustar`: `operacaoId*` (`UUID`), `origemVersaoId*` (`Long`), `destinoFechamentoId*` (`Long`), `versaoDestino*` (`Long`), `calculoCorrigidoId*` (`Long`), `motivo*` (`String`), `evidencia*` (`String`), `resolucao` (`FechamentoCobrancaDto.Resolucao`).

Dependências de referência: [["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/AjusteFechamentoController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/AjusteFechamentoController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#ajustes`, `backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java#ajustar`.

## Contagem e carga inicial

Ligação: `BE14` → `FE12`. Páginas: 3.

### contagem-1 — Consultar unidade para contagem

Visão principal: `EstoqueController.listar`. Detalhe: `EstoqueController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar detalhe<br>`EstoqueController.consultar` | detalhe do registro | `GET /api/v1/unidades-logisticas/{codigo}/estoque`<br>`sem corpo` → `EstoqueDto.Unidade` | OPERACAO | codigo |

Estados, validação e histórico: Leia o UUID e consulte a unidade/versão no estoque antes de registrar a observação. Essa consulta não exige inventar um pedido de entrada. Depois abra Contagem física.


Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Contagem","ContagemDto.Resultado"],["Carga inicial","CargaInicialDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#consultar`.

### contagem-2 — Contagem física

Visão principal: `ContagemController.listar`. Detalhe: `ContagemController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`ContagemController.listar` | visão principal | `GET /api/v1/contagens`<br>`sem corpo` → `PaginaResponse<ContagemDto.Resultado>` | OPERACAO | clienteId, armazemId |
| Consultar detalhe<br>`ContagemController.consultar` | detalhe do registro | `GET /api/v1/contagens/{id}`<br>`sem corpo` → `ContagemDto.Resultado` | OPERACAO | id |
| Registrar contagem física<br>`ContagemController.contar` | cabeçalho | `POST /api/v1/contagens`<br>`ContagemDto.Contar` → `ContagemDto.Resultado` | OPERACAO | nenhuma |
| Consultar revisões<br>`ContagemController.revisoes` | contexto do registro / etapa operacional | `GET /api/v1/contagens/{id}/revisoes`<br>`sem corpo` → `PaginaResponse<ContagemDto.Resultado>` | OPERACAO | id |
| Aplicar ajuste conferido<br>`ContagemController.aplicar` | contexto do registro / etapa operacional | `POST /api/v1/contagens/{id}/aplicar`<br>`ContagemDto.Aplicar` → `ContagemDto.Resultado` | SUPERVISOR | id |

Estados, validação e histórico: Leia a unidade e sua versão, registre instante e quantidade observada. Consulte esperado, contado, diferença, reservado, origens e impedimento retornados. Contar não aplica ajuste. Somente Supervisor/Gestor confirma ajuste com revisão, origens/deltas, causa, destino e comprovação. Exercício preparado: leitura 8 sem reserva; leitura 7 retorna impedimento de reserva. O navegador não calcula diferença.

- Formulário `ContagemDto.Contar`: `operacaoId*` (`UUID`), `codigoUnidade*` (`UUID`), `versaoUnidade*` (`Long`), `contado*` (`BigDecimal`), `observadoEm*` (`Instant`), `motivo*` (`String`).
- Formulário `ContagemDto.Aplicar`: `operacaoId*` (`UUID`), `revisao*` (`Integer`), `versaoUnidade*` (`Long`), `motivo*` (`String`), `causa*` (`String`), `destino*` (`String`), `comprovacao*` (`String`), `origens*` (`List<ContagemDto.DeltaOrigem>`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Contagem","ContagemDto.Resultado"],["Carga inicial","CargaInicialDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ContagemController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ContagemController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ContagemEstoqueService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContagemEstoqueService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContagemEstoqueService.java#contar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContagemEstoqueService.java#revisoes`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContagemEstoqueService.java#aplicar`.

### contagem-3 — Carga inicial excepcional

Visão principal: `CargaInicialController.listar`. Detalhe: `CargaInicialController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`CargaInicialController.listar` | visão principal | `GET /api/v1/cargas-iniciais`<br>`sem corpo` → `PaginaResponse<CargaInicialDto.Resultado>` | SUPERVISOR | clienteId, armazemId |
| Consultar detalhe<br>`CargaInicialController.consultar` | detalhe do registro | `GET /api/v1/cargas-iniciais/{id}`<br>`sem corpo` → `CargaInicialDto.Resultado` | SUPERVISOR | id |
| Novo registro<br>`CargaInicialController.criar` | cabeçalho | `POST /api/v1/cargas-iniciais`<br>`CargaInicialDto.Criar` → `CargaInicialDto.Resultado` | SUPERVISOR | nenhuma |
| Consultar revisões<br>`CargaInicialController.revisoes` | contexto do registro / etapa operacional | `GET /api/v1/cargas-iniciais/{id}/revisoes`<br>`sem corpo` → `PaginaResponse<CargaInicialDto.Revisao>` | SUPERVISOR | id |
| Revisar dados levantados<br>`CargaInicialController.revisar` | contexto do registro / etapa operacional | `POST /api/v1/cargas-iniciais/{id}/revisoes`<br>`CargaInicialDto.Revisar` → `CargaInicialDto.Resultado` | SUPERVISOR | id |
| Preparar<br>`CargaInicialController.preparar` | contexto do registro / etapa operacional | `POST /api/v1/cargas-iniciais/{id}/preparar`<br>`CargaInicialDto.Confirmar` → `CargaInicialDto.Resultado` | SUPERVISOR | id |
| Confirmar<br>`CargaInicialController.confirmar` | contexto do registro / etapa operacional | `POST /api/v1/cargas-iniciais/{id}/confirmar`<br>`CargaInicialDto.Confirmar` → `CargaInicialDto.Resultado` | SUPERVISOR | id |
| Registrar cancelamento<br>`CargaInicialController.cancelar` | contexto do registro / etapa operacional | `POST /api/v1/cargas-iniciais/{id}/cancelar`<br>`CargaInicialDto.Cancelar` → `CargaInicialDto.Resultado` | GESTOR | id |
| Resolver cancelamento pendente<br>`CargaInicialController.resolverCancelamento` | contexto do registro / etapa operacional | `POST /api/v1/cargas-iniciais/{id}/resolver-cancelamento`<br>`CargaInicialDto.ResolverCancelamento` → `CargaInicialDto.Resultado` | GESTOR | id |

Estados, validação e histórico: Declare ausências sem inventar nota, FIFO ou valor. Criar mantém PENDENTE. O roteiro fictício conhecido usa a entrada 201 na revisão; outras origens continuam pendentes. Confira versão, revisão e hash devolvidos ao preparar. PREPARADA possui entrada e etiquetas, mas não libera saída. Confirme leitura da etiqueta fornecida e todas as etiquetas devolvidas, depois consulte REGULARIZADA. Exemplos não comprovam carga real.

- Formulário `CargaInicialDto.Criar`: `operacaoId*` (`UUID`), `clienteId*` (`Long`), `armazemId*` (`Long`), `produtoId*` (`Long`), `referencia*` (`String`), `etiquetaFornecida` (`String`), `quantidade*` (`BigDecimal`), `dados*` (`CargaInicialDto.Dados`), `motivo*` (`String`).
- Formulário `CargaInicialDto.Revisar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `dados*` (`CargaInicialDto.Dados`), `motivo*` (`String`).
- Formulário `CargaInicialDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `revisao*` (`Integer`), `conteudoHash*` (`String`), `leitura*` (`String`), `etiquetasUnidades*` (`List<String>`), `motivo*` (`String`).
- Formulário `CargaInicialDto.Confirmar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `revisao*` (`Integer`), `conteudoHash*` (`String`), `leitura*` (`String`), `etiquetasUnidades*` (`List<String>`), `motivo*` (`String`).
- Formulário `CargaInicialDto.Cancelar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `motivo*` (`String`).
- Formulário `CargaInicialDto.ResolverCancelamento`: `operacaoId*` (`UUID`), `versao*` (`Long`), `pedidoResolucaoId` (`Long`), `contagensIds*` (`List<Long>`), `motivo*` (`String`).

Dependências de referência: [["Unidade logística","UnidadeLogisticaDto.Resumo"],["Contagem","ContagemDto.Resultado"],["Carga inicial","CargaInicialDto.Resultado"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/CargaInicialController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/CargaInicialController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#criar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#revisoes`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#revisar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#preparar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#confirmar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#cancelar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CargaInicialService.java#resolverCancelamento`.

## Contingência

Ligação: `BE14` → `FE12`. Páginas: 1.

### contingencia-1 — Registrar fatos da planilha

Visão principal: `ContingenciaController.listar`. Detalhe: `ContingenciaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`ContingenciaController.listar` | visão principal | `GET /api/v1/contingencias`<br>`sem corpo` → `PaginaResponse<ContingenciaDto.Resultado>` | SUPERVISOR | clienteId, armazemId |
| Consultar detalhe<br>`ContingenciaController.consultar` | detalhe do registro | `GET /api/v1/contingencias/{id}`<br>`sem corpo` → `ContingenciaDto.Resultado` | SUPERVISOR | id |
| Registrar fato<br>`ContingenciaController.registrar` | cabeçalho | `POST /api/v1/contingencias`<br>`ContingenciaDto.Registrar` → `ContingenciaDto.Resultado` | SUPERVISOR | nenhuma |
| Conciliar fato manualmente<br>`ContingenciaController.conciliar` | contexto do registro / etapa operacional | `POST /api/v1/contingencias/{id}/conciliar`<br>`ContingenciaDto.Conciliar` → `ContingenciaDto.Resultado` | SUPERVISOR | id |

Estados, validação e histórico: Fluxo manual após recuperação da rede: registre identidade do fato, instante real, operador, fonte, dependências e dados de CHEGADA pelo contrato. Consulte versão/hash antes de conciliar. EXECUTAR ou VINCULAR com prova da operação original são comandos explícitos. Dependência mantém pendência; conteúdo divergente retorna conflito. Repetição conserva a confirmação original; consulte o estado atual. Não há fila automática offline nem efeito real neste exercício.

- Formulário `ContingenciaDto.Registrar`: `operacaoId*` (`UUID`), `identidadeFato*` (`String`), `clienteId*` (`Long`), `armazemId*` (`Long`), `tipo*` (`TipoContingencia`), `ocorridaEm*` (`Instant`), `operador*` (`String`), `fonte*` (`String`), `efeitoRegistradoNoWms*` (`Boolean`), `dependencias*` (`List<String>`), `dados*` (`Map<String,Object>`), `motivo*` (`String`).
- Formulário `ContingenciaDto.Conciliar`: `operacaoId*` (`UUID`), `versao*` (`Long`), `modo*` (`String`), `prova` (`ContingenciaDto.Prova`), `motivo*` (`String`).

Dependências de referência: [["Fato manual","ContingenciaDto.Resultado"],["Unidade logística","UnidadeLogisticaDto.Resumo"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/ContingenciaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/ContingenciaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/ContingenciaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContingenciaService.java#consultar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContingenciaService.java#registrar`, `backend/src/main/java/br/com/rodogarcia/wms/services/ContingenciaService.java#conciliar`.

## Consultas e relatórios

Ligação: `BE08 / BE12–14` → `FE08 / FE12`. Páginas: 6.

### relatorios-1 — Estoque e movimentos

Visão principal: `EstoqueController.listar`. Detalhe: `EstoqueController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`EstoqueController.listar` | visão principal | `GET /api/v1/estoque`<br>`sem corpo` → `PaginaResponse<EstoqueDto.Unidade>` | OPERACAO | clienteId, armazemId |
| Consultar saldo do SKU<br>`EstoqueController.saldo` | contexto do registro / etapa operacional | `GET /api/v1/estoque/saldo`<br>`sem corpo` → `EstoqueDto.Saldo` | OPERACAO | clienteId, armazemId, produtoId |
| Consultar movimentos<br>`EstoqueController.historico` | contexto do registro / etapa operacional | `GET /api/v1/unidades-logisticas/{codigo}/movimentos`<br>`sem corpo` → `PaginaResponse<EstoqueDto.Movimento>` | OPERACAO | codigo |

Estados, validação e histórico: Consultas paginadas no mesmo alcance do usuário. Sem total global extrapolado de uma página.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/EstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#saldo`, `backend/src/main/java/br/com/rodogarcia/wms/services/EstoqueService.java#historico`.

### relatorios-2 — Valor e validade

Visão principal: `IndicadorEstoqueController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`IndicadorEstoqueController.listar` | visão principal | `GET /api/v1/indicadores-estoque`<br>`sem corpo` → `PaginaResponse<IndicadorEstoqueDto.Resultado>` | OPERACAO | clienteId, armazemId, fuso |

Estados, validação e histórico: Valores são retornados pelo backend; null é não informado.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/IndicadorEstoqueController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/IndicadorEstoqueController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/IndicadorEstoqueService.java#listar`.

### relatorios-3 — Entrada e saída

Visão principal: `PedidoEntradaController.listar`. Detalhe: `PedidoEntradaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`PedidoEntradaController.listar` | visão principal | `GET /api/v1/pedidos-entrada`<br>`sem corpo` → `PaginaResponse<PedidoEntradaDto.Resumo>` | OPERACAO | clienteId, armazemId |

Estados, validação e histórico: Consulte pedidos do contexto. Filtros adicionais inexistentes no contrato não são enviados.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoEntradaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoEntradaService.java#listar`.

### relatorios-4 — Pedidos de saída

Visão principal: `PedidoSaidaController.listar`. Detalhe: `PedidoSaidaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`PedidoSaidaController.listar` | visão principal | `GET /api/v1/pedidos-saida`<br>`sem corpo` → `PaginaResponse<PedidoSaidaDto.Detalhe>` | OPERACAO | clienteId, armazemId |

Estados, validação e histórico: Reservado, separado e retirado permanecem distintos.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/PedidoSaidaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/PedidoSaidaService.java#listar`.

### relatorios-5 — Serviços e valores

Visão principal: `CalculoCobrancaController.listar`. Detalhe: `CalculoCobrancaController.consultar`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`CalculoCobrancaController.listar` | visão principal | `GET /api/v1/calculos-cobranca`<br>`sem corpo` → `PaginaResponse<CalculoCobrancaDto.Resultado>` | SUPERVISOR | clienteId, armazemId |
| Consultar detalhe<br>`CalculoCobrancaController.consultar` | detalhe do registro | `GET /api/v1/calculos-cobranca/{id}`<br>`sem corpo` → `CalculoCobrancaDto.Resultado` | SUPERVISOR | id |

Estados, validação e histórico: Memória e histórico financeiro do servidor.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/CalculoCobrancaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/CalculoCobrancaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java#listar`, `backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java#consultar`.

### relatorios-6 — Auditoria

Visão principal: `AuditoriaController.listar`. Detalhe: `dados da consulta`.

| Aba/comando anterior | Destino | Contrato | Perfil mínimo | Referências obrigatórias |
| --- | --- | --- | --- | --- |
| Consultar lista<br>`AuditoriaController.listar` | visão principal | `GET /api/v1/auditoria`<br>`sem corpo` → `PaginaResponse<AuditoriaResponse>` | GESTOR | tipo, registroId |

Estados, validação e histórico: Somente Gestor. Registros preservados; não há edição/exclusão.


Dependências de referência: [["Pedido de entrada","PedidoEntradaDto.Resumo"],["Entrada conferida","RecebimentoDto.Entrada"],["Unidade logística","UnidadeLogisticaDto.Resumo"],["Pedido de saída","PedidoSaidaDto.Detalhe"],["Reserva","PedidoSaidaDto.Reserva"],["Cálculo","CalculoCobrancaDto.Resultado"],["Fechamento","FechamentoCobrancaDto.Fechamento"],["Versão financeira","FechamentoCobrancaDto.Versao"]].

Fontes de comandos: [backend/src/main/java/br/com/rodogarcia/wms/controllers/AuditoriaController.java](../../backend/src/main/java/br/com/rodogarcia/wms/controllers/AuditoriaController.java).

Regras e permissões: `backend/src/main/java/br/com/rodogarcia/wms/services/AuditoriaService.java#listar`.

## Limites comuns

Filtros, enums, restrições de cada campo e os esquemas completos permanecem no [inventário estruturado](../../frontend/evidencias/record-pages-inventario.json) e nos contratos. Revisões, motivos e confirmações são preservados; permissões do backend são a autoridade final. Busca local identifica que opera apenas sobre a resposta atual. Sem endpoint global, a visão inicial solicita a referência obrigatória. Cidade/vínculos cliente–armazém e integração real têm os limites registrados em etapa-06.md.
