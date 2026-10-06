# Plano de entregas do WMS Rodogarcia

Este plano é uma proposta de execução atualizada em 05/10/2026, sem esforço ou prazo técnico estimado. O questionário informa necessidade de iniciar em 13/10/2026; essa data ainda não é um compromisso de entrega validado. A sequência parte da recomendação do PDF, página 10, e antecipa a análise fiscal, o histórico e os fatos de cobrança porque eles influenciam o funcionamento do núcleo.

Em 05/10/2026, D11 iniciou a base BE02 e D12 ampliou o backend para cadastros, persistência, acesso e auditoria. Os testes locais não concluem as validações no ambiente real. As etapas abaixo mantêm a visão geral; critérios e resultados de cada entrega ficam no acompanhamento oficial.

D13 acrescentou BE16 para padronização em `.properties` e verificações de engenharia. Os [padrões](16-padroes-de-engenharia-backend.md) acompanham as próximas entregas, sem substituir os critérios de negócio e piloto.

D15 entregou o [recebimento](18-recebimento-e-conferencia.md), D16 acrescentou [unidades logísticas e dados de etiquetas](20-unidades-logisticas-e-etiquetas.md) e D17 implementou [endereçamento e estoque](22-enderecamento-movimentacao-e-estoque.md), com evidência histórica no [documento23](23-validacao-estoque-backend.md). D18 acrescentou pedido/FIFO/reserva, conforme [25](25-validacao-pedido-saida-e-reserva.md); BE09 deixou de ser próximo trabalho.

Em06/10, D19 autoriza concluir continuamente o backend local BE01–BE16. Expedição/retornos, serviços/cálculo e fechamento já têm aceite local nos [28](28-validacao-separacao-retirada-retornos.md), [30](30-validacao-servicos-e-calculo.md) e [32](32-validacao-fechamento-e-contingencia.md). Contagem/carga/contingência/inativação e a jornada integrada concluídas localmente após testes/revisão, conforme32/34; preparação SQL/configuração/recuperação permanece em arquivos. O estado oficial e o restante efetivo estão no states, sem nova autorização por macrobloco. SQL Server, identidade real, fiscal/comercial, equipamentos, frontend e homologação continuam com evidência própria, sem piloto presumido.

O detalhamento executável do plano está no [states.md](../states.md), com trilhas backend e frontend, dependências, critérios e evidências. Este documento mantém a visão geral; consultar e atualizar o estado de cada entrega na trilha, sem duplicar seu status aqui.

## Etapa 1 Definições funcionais

As Q01 a Q27 já receberam respostas. Usar as [regras consolidadas](11-alinhamentos-apos-respostas.md) para detalhar estados, modelo conceitual e exemplos completos. A orientação é resolver lacunas pelo raciocínio e apresentar resultados concretos para validação, sem repetir o questionário. Preços reais e condições de ambiente entram como tarefas de implantação.

Resultado esperado: escopo inicial identificado, regras confirmadas e pendências delimitadas. Validar pelo menos um caso completo de entrada, saída e cálculo financeiro, incluindo exceções reais.

## Etapa 2 Preparação técnica

Confirmar versões, ferramentas, pacote da aplicação, ambientes, acesso ao SQL Server, emissor fiscal, coletor, impressora e rede. Manter React/TypeScript, Java/Spring e MVC convencional, conforme as decisões do responsável.

Resultado esperado: viabilidade das integrações e dispositivos verificada em ambiente de validação, com caminho de acesso e recuperação definido. Não é necessário escolher soluções para necessidades ainda não confirmadas.

## Etapa 3 Base da aplicação e fluxo completo

Com a implementação autorizada, usar as camadas convencionais para construir um percurso utilizável: cadastro, entrada, conferência, identificação, endereçamento, reserva, separação, tratamento fiscal e retirada.

Resultado esperado: uma mercadoria pode percorrer o ciclo com identidade preservada, saldo coerente e histórico. Contas, permissões, registro das operações e fatos cobrados devem acompanhar esse percurso desde o início.

Os cadastros e telas podem ser entregues progressivamente. O piloto só deve assumir operações reais quando as etapas necessárias do percurso estiverem disponíveis e verificadas.

## Etapa 4 Cobrança e consultas essenciais

Implementar as fórmulas confirmadas, tabelas com vigência, serviços, memória de cálculo, fechamento e relatórios necessários para conferir a operação.

Resultado esperado: valores de exemplos reais conferem com o contrato; cada valor tem origem identificável; períodos podem ser recuperados sem duplicar cobrança. Disponibilizar consultas de estoque, localização, bloqueios e histórico para suporte ao piloto.

## Etapa 5 Validação e piloto controlado

O piloto informado é Osasco, cliente escrito Bracel nas respostas e Brasel no PDF; cadastro empresarial deve resolver a grafia. Caio acompanha testes e confirma a operação. O recorte funcional proposto está em AC16; NOTAZZ e ESL podem usar procedimento manual intermediário conforme AC02.

Antes de assumir a operação, conferir estoque inicial, permissões, dispositivos, integração fiscal, cálculo, recuperação e os [cenários de validação](08-cenarios-de-validacao.md). Definir quem acompanha o piloto e como registrar e corrigir diferenças.

Resultado esperado: responsáveis conseguem verificar recebimentos, reservas, saídas e cobranças e explicar qualquer diferença. A expansão depende desses resultados, não de uma data presumida.

## Etapa 6 Evolução

Ampliar clientes e armazéns conforme validação. Dashboard elaborado, mapa visual, integrações adicionais e inventário completo podem ser priorizados posteriormente. Transferências entre armazéns foram excluídas da primeira versão. Devoluções por nova entrada e retirada parcial de pallet fazem parte da base funcional recebida; pedido de saída permanece integral.

## Critérios propostos para começar a operação real

- Estoque inicial conferido e rastreável, quando houver mercadoria já armazenada.
- Percurso operacional e fiscal aplicável executado de ponta a ponta.
- Reserva simultânea e repetição de solicitações verificadas.
- Quarentena impedindo seleção e expedição indevida.
- Histórico suficiente para explicar alterações e correções.
- Fórmulas e exemplos financeiros conferidos com as regras confirmadas.
- Permissões por função verificadas.
- Impressão, leitura e confirmação testadas nos dispositivos escolhidos.
- Falhas de rede e integrações com tratamento compreensível.
- Recuperação de dados testada e responsáveis conhecidos.

A necessidade em 13/10 deve orientar o planejamento com equipe e disponibilidade reais. Dez usuários/quatro simultâneos são a estimativa informada; volumes ainda serão levantados. Conferir percurso e demonstrativo, dispositivos e recuperação antes da liberação por Caio, sem prometer viabilidade apenas pela data desejada.
