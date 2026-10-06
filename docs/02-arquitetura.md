# Arquitetura do WMS Rodogarcia

Este documento registra as tecnologias definidas pelo responsável e propostas de organização, incorporando o questionário. A base inicial BE02 está no [documento 12](12-base-e-contratos-backend.md); cadastros, JPA, acesso JWT e auditoria implementados após D12 estão no [documento 14](14-cadastros-acesso-e-persistencia.md), com seus limites.

D13 determina configuração Spring em `.properties` e boas práticas de engenharia. As convenções e verificações automáticas estão no [documento 16](16-padroes-de-engenharia-backend.md), preservando a organização MVC abaixo.

D15 implementa o [recebimento](18-recebimento-e-conferencia.md) com pedido/notas, chegadas e quantidades conferidas. D16 acrescenta [unidades logísticas](20-unidades-logisticas-e-etiquetas.md), composição de origem, transformações e dados de etiquetas. Fatos físicos, origem fiscal, estado do pedido, condição e identidade permanecem separados; endereçamento e estoque disponível continuam em BE08.

## Base definida pelo responsável

| Componente | Decisão |
| --- | --- |
| Frontend | React com TypeScript |
| Backend | Java com Spring |
| Organização do backend | MVC convencional com pastas por camada |
| Banco | SQL Server já disponível na empresa |

O React apresentará as telas. Os controllers do Spring receberão suas solicitações. A estrutura convencional foi escolhida para facilitar a compreensão e a manutenção.

## Camadas do backend

| Camada | Função |
| --- | --- |
| controllers | Receber pedidos da interface e encaminhar a execução aos serviços |
| services | Aplicar regras e coordenar alterações relacionadas |
| repositories | Consultar e gravar informações no SQL Server |
| models | Representar entidades e relacionamentos |
| dto | Definir dados de entrada e saída das operações |
| config | Organizar configuração, segurança e integrações |
| exceptions | Padronizar tratamento de falhas e mensagens compreensíveis |

As áreas de negócio serão representadas por serviços com responsabilidades distintas dentro dessa organização. A proposta não exige pacotes separados por domínio nem substitui as pastas convencionais solicitadas.

Pacote Java, versões, ferramenta de construção e Spring Boot foram escolhidos tecnicamente em BE02, após o pedido D11. Essas escolhas estão no documento 12 e mantêm a organização definida pelo responsável.

## Responsabilidades propostas

As decisões desta seção são propostas de desenho, registradas como P01 a P07 no controle de decisões.

| Área | Responsabilidade proposta |
| --- | --- |
| Cadastros e contratos | Dados de clientes, produtos, estabelecimentos, endereços e tabelas com vigência |
| Recebimento | Previsão, conferência, diferenças, liberação e formação das unidades |
| Estoque | Efetivação de mudanças de quantidade, disponibilidade, reserva e localização |
| Saída | Coordenação do pedido, solicitação de reserva, separação e confirmação da retirada |
| Fiscal | Envio ao emissor e acompanhamento dos resultados |
| Faturamento | Cálculo a partir dos fatos operacionais e dos contratos, com memória de cálculo |
| Consultas | Visibilidade sobre os registros oficiais da operação |

A proposta inicial é manter uma aplicação backend organizada em serviços. O serviço responsável pelo estoque deve centralizar suas alterações. Entrada e saída solicitam mudanças a ele; relatórios e faturamento não mantêm um saldo operacional independente.

## Informações que precisam permanecer distintas

| Informação | Exemplo |
| --- | --- |
| Situação do pedido | Em conferência, pendente ou concluído |
| Quantidades do pedido | Prevista, recebida, reservada e expedida |
| Condição da mercadoria | Liberada ou em quarentena |
| Compromisso com uma saída | Sem reserva ou vinculada a um pedido |
| Localização física | Triagem, posição de armazenagem ou área de separação |
| Situação fiscal | Pendente, autorizada, rejeitada ou cancelada |
| Situação da cobrança | Registrada, incluída no fechamento ou ajustada |

Essa separação é proposta para evitar que um único status esconda informações simultâneas. Uma unidade pode estar endereçada e em quarentena. Uma nota pode estar autorizada sem que a retirada física tenha ocorrido.

Os nomes finais dos estados e transições serão detalhados com base nas respostas: entregas em partes com efetivação única, retirada parcial de pallet permitida e pedido de saída atendido integralmente. Quantidade de produto, identidade da unidade e quantidade pedida são medidas distintas.

## Rastreabilidade proposta

- Manter vínculo entre pedido, documento de origem, conferência e unidades criadas.
- Registrar a quantidade recebida mesmo quando ainda estiver em triagem ou quarentena, vinculando-a posteriormente à identificação definitiva.
- Preservar origem de quantidades na divisão permitida e no reagrupamento de mesmo SKU/lote/data; a proteção adicional de mesma nota está proposta em AC10.
- Guardar usuário, data e hora, origem, destino e motivo das operações críticas.
- Registrar correções relevantes como ações rastreáveis, sem apagar silenciosamente o fato anterior.
- Separar reimpressão de etiqueta de criação de unidade logística.

## Consistência e concorrência propostas

Uma operação indivisível deve ser confirmada por completo ou não ser confirmada. Por exemplo, a reserva e seu vínculo com o pedido precisam ser gravados juntos no SQL Server. A escolha do mecanismo técnico será feita durante a implementação.

Quando dois operadores tentarem reservar a mesma unidade, a disponibilidade deve ser verificada na confirmação e apenas um pedido pode obter a reserva. A sugestão do FIFO pode ficar desatualizada entre a consulta e a confirmação.

Solicitações repetidas por duplo clique, repetição de leitura ou recuperação de conexão devem ter tratamento que impeça executar duas vezes o mesmo fato. Isso não deve bloquear entregas diferentes legitimamente vinculadas à mesma nota, permitidas em Q03.

Uma unidade com impedimentos sobrepostos, como reserva e avaria posterior, não pode voltar ao saldo disponível por uma conta que subtraia ou desfaça bloqueios incorretamente.

## Integração fiscal proposta

- Manter situação fiscal independente da posição física e da reserva.
- Guardar referências suficientes para acompanhar a solicitação no emissor.
- Diferenciar rejeição conhecida de ausência de resposta.
- Consultar um resultado desconhecido antes de tentar nova emissão equivalente.
- Preservar a reserva sem vencimento durante pendência fiscal, conforme Q09, sem concluir a baixa por emissão rejeitada.
- Não interpretar cancelamento fiscal como prova de devolução física.

Q13 identifica NOTAZZ e emissão após separação; Q17 identifica ESL para NFS-e de serviços. AC02 propõe emissão externa com registro/importação inicial e integração automática posterior. AC01 mantém documento fiscal e confirmação de retirada física distintos. Regras fiscais variáveis são parametrizáveis conforme o PDF, mas seus conteúdos precisam ser fornecidos ou validados pela área fiscal. Este documento não determina CFOPs ou tratamentos tributários.

## Segurança e continuidade propostas

- Contas individuais e permissões verificadas no backend para cada operação e objeto acessado.
- Escopo de acesso por armazém e cliente conforme as atribuições reais; a Q20 define os responsáveis.
- Verificação dos dados importados e associação explícita a clientes e produtos conhecidos.
- Proteção das conexões e dos documentos, com banco sem acesso direto dos coletores.
- Histórico operacional protegido contra alterações comuns da aplicação.
- Cópias de segurança com restauração testada e responsáveis conhecidos.
- Alertas para pendências fiscais, falhas de cobrança e operações interrompidas.
- Confirmação visível do servidor nas ações que comprometem estoque.

Q27 permite planilha durante indisponibilidade geral, com posterior lançamento. Isso não exige aplicativo com sincronização automática sem conexão; AC14 descreve a reconciliação proposta. Falha isolada de coletor segue espera/reimpressão da Q26. Parada tolerada informada: 36h; capacidade de recuperação deve ser testada com Lucas e não equivale a aceitar perda de 36h de dados.

Q26 identifica os tipos de equipamento e o responsável; compatibilidade e cobertura ainda exigem teste local. Q27 informa a contingência e o limite de parada, sem comprovar capacidade de recuperação. Q25 fornece o recorte do piloto e a estimativa de usuários; volumes de mercadorias ainda serão levantados.

## Faturamento proposto

Registrar os fatos cobrados desde o primeiro piloto. Cada cálculo deve preservar cliente, origem, quantidade ou ocupação, período, tabela aplicável, regra e valor. Uma tabela nova não deve alterar silenciosamente cobranças históricas.

Se o processamento atrasar, os períodos pendentes devem poder ser recuperados com o histórico e a vigência correspondente ao fato ou período contratual, sem duplicidade. Q14 a Q17 definem posição-dia, calendário, mesmo dia zero, pico, vigência e ciclos por cliente. AC04 a AC08 propõem a combinação calculável e exemplos; preços e parâmetros reais entram no cadastro, sem copiar a tabela de outro cliente.

A Q23 detalha autorização e vínculo dos serviços feitos durante a armazenagem. As Q21, Q22 e Q24 complementam, respectivamente, a referência do FIFO, a confirmação física de saída e as operações permitidas sobre cadastros inativos.

## Escolhas técnicas ainda abertas

Permanecem abertos o provedor real/ciclo de identidade, contratos das integrações, hospedagem, alvo/validação SQL Server, organização/ferramentas React e formato físico das etiquetas. Cadastros, recebimento, unidades e V1/V2/V3 estão preparados; JWT/alcances foram implementados e testados localmente. Estruturas e contratos de localização/disponibilidade, reserva, saída e cobrança continuam futuros.

## Consequências das regras consolidadas

- Separar produto, configuração de embalagem, unidade física e posições ocupadas. Uma unidade pode consumir duas posições; a reserva e liberação do conjunto são indivisíveis.
- Registrar reserva por quantidade e bloquear a unidade física durante a separação parcial, conforme proposta AC10.
- Guardar data FIFO original, chegadas reais e início da armazenagem separadamente.
- Distinguir ocupação física de posições equivalentes cobráveis durante separação/quarentena. Guardar alterações e memória da convenção usada.
- Preservar notas de origem, vínculo de devolução e versões de etiqueta sem duplicar unidades.
- Serviços operacionais têm lançamento único, aprovação e vínculo com origem; sugestão automática não duplica lançamento manual.
- Manter vigência de preços e versões de fechamento. Documento já faturado não é reescrito por ajuste posterior.
- Aplicar permissões de Gestor, Supervisor e Operação no backend; configuração de cliente/armazém limita o alcance das contas.

Essas responsabilidades cabem em controllers, services, repositories, models, dto, config e exceptions. Não exigem trocar a estrutura escolhida nem criar vários sistemas. Detalhamento e classificação das propostas em [regras consolidadas](11-alinhamentos-apos-respostas.md).
