# WMS Rodogarcia

Projeto do sistema de gestão de armazenagem da Rodogarcia. A estrutura e a documentação começaram em 03/10/2026. O backend possui cadastros, recebimento manual/XML, unidades/etiquetas, estoque, pedido/FIFO/reserva, separação/documentos/retirada, retornos/avaria, serviços/cálculo e fechamento, usando Spring MVC, JPA, permissões e auditoria. Escopo local BE01–BE16 concluído em D19: 367 testes aprovados, revisão favorável e V1–V9 preparadas em arquivos, conforme [evidência32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Inclui contagem/carga/contingência/inativação e jornada integrada. SQL Server real, fiscal/cobrança reais, homologação e frontend permanecem separados.

A operação da Brasel, com bobinas, é o primeiro caso descrito na especificação. O sistema deverá comportar outros clientes, armazéns e tipos de unidade logística.

## Tecnologias definidas

| Parte | Decisão do responsável pelo projeto |
| --- | --- |
| Frontend | React com TypeScript |
| Backend | Java com Spring |
| Organização do backend | MVC convencional, com controllers, services, repositories, models e DTOs |
| Banco de dados | SQL Server já disponível na empresa |

Versões, Maven Wrapper e pacote Java estão nas [escolhas da base](docs/12-base-e-contratos-backend.md). O [bloco cadastral](docs/14-cadastros-acesso-e-persistencia.md) acrescenta JPA, Flyway e validação de tokens assinados. Alvo SQL Server, provedor real de identidade, hospedagem e frontend ainda precisam de definição/validação. Nenhuma conexão com o banco da empresa foi realizada.

O [recebimento](docs/18-recebimento-e-conferencia.md) fornece contratos FE05; as [unidades e etiquetas](docs/20-unidades-logisticas-e-etiquetas.md), contratos FE06; [endereçamento e estoque](docs/22-enderecamento-movimentacao-e-estoque.md), contratos FE07/FE08. Pedido/expedição nos contratos24/27; serviços/cálculo/fechamento nos29/31. Evidências e limites atuais em [validação D19](docs/32-validacao-fechamento-e-contingencia.md) e na [matriz BE01–BE16](docs/34-matriz-e-validacao-final-backend.md). A API de etiquetas entrega dados, sem impressão física validada.

## Pastas

| Pasta | Finalidade |
| --- | --- |
| [backend](backend/README.md) | Backend Java/Spring e orientação sobre suas camadas |
| [frontend](frontend/README.md) | Interface React/TypeScript administrativa e para coletores |
| [database](database/README.md) | Evolução futura da estrutura do SQL Server e dados de referência |
| [infra](infra/README.md) | Planejamento dos ambientes e da operação da aplicação |
| [docs](docs/README.md) | Regras, arquitetura, decisões, dúvidas e planejamento |

## Por onde continuar

1. Ler as [orientações de trabalho](AGENTS.md) e a [trilha de implementação](states.md), separada em backend e frontend.
2. Consultar o [índice da documentação](docs/README.md) e o [contexto para continuidade](docs/09-continuidade.md).
3. Distinguir as [decisões e pendências](docs/06-decisoes-e-pendencias.md).
4. Ler as [respostas recebidas em 05/10](docs/10-respostas-recebidas-2026-10-05.md) e as [regras consolidadas pelo raciocínio](docs/11-alinhamentos-apos-respostas.md). O questionário original permanece como histórico; não deve ser reenviado como pendente.

Para executar e testar, consultar [backend/README.md](backend/README.md). Os [resultados atuais](docs/32-validacao-fechamento-e-contingencia.md) distinguem testes HTTP/JPA/H2 de SQL Server, identidade e equipamentos reais. Próxima etapa: definir insumos externos e autorizar seus ensaios, conforme o preparo33 e a matriz34. O `states.md` mantém as dependências e validações externas delimitadas.
