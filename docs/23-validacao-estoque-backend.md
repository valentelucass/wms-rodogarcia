# Validação do endereçamento e estoque — D17

Executada em 05/10/2026, em desenvolvimento local. Escopo funcional e contratos no [documento 22](22-enderecamento-movimentacao-e-estoque.md). Evidência de implementação de BE08 e consultas iniciais de BE14; não comprova homologação do armazém, frontend ou SQL Server.

## Build e testes

JDK Eclipse Temurin 21.0.12.1 e Maven Wrapper 3.9.16. A partir de `backend`, com `JAVA_HOME` apontando para JDK 21:

```powershell
./mvnw.cmd -B spotless:apply clean verify
```

Resultado: **BUILD SUCCESS**, concluído às 21:18:58, horário de São Paulo; duração de 1min32s. **153 testes, zero falhas, zero erros e zero ignorados**. Enforcer, Spotless e regras ArchUnit aprovados. Resources continuam em `.properties`.

| Suíte | Testes |
| --- | ---: |
| HTTP/base | 4 |
| Arquitetura | 5 |
| Cadastros | 28 |
| Configuração de ambiente | 3 |
| Configuração properties | 2 |
| Proteções SQL Server | 4 |
| Tratamento de erros | 6 |
| Recebimento | 28 |
| Extração XML | 6 |
| Unidades logísticas | 36 |
| **Estoque — novos** | **31** |
| **Total** | **153** |

O teste de estoque usa HTTP real em porta aleatória, JWT assinado por chave RSA efêmera e JPA/H2 isolado. As unidades de cada cenário vêm do fluxo HTTP de recebimento/efetivação/unitização, com dados fictícios. O H2 cria estrutura a partir dos models; **não executa V1 a V4**.

## Comportamentos conferidos

- Quantidade pendente de unitização e quantidade unitizada reconciliam sem dupla contagem; saldo permanece dez durante unitização concorrente. Unidades sem endereço aparecem como indisponíveis, inclusive no filtro negativo.
- Primeiro endereço e remanejamento preservam UUID, origem, FIFO, início de armazenagem e etiqueta; posição anterior fica livre e o histórico conserva antes/depois.
- Mesma chave concorrente confirma uma única vez; chaves diferentes com a mesma revisão produzem uma confirmação e um conflito. Repetição posterior recupera a resposta original; outro conteúdo com a mesma chave é recusado.
- Unidades de clientes/pedidos diferentes disputam uma posição: somente uma ocupa. Conjunto ocupa duas posições atomicamente, permite destino parcialmente sobreposto e mantém equivalência/datas. Se uma posição estiver ocupada, a outra não é ocupada parcialmente.
- Cada limite de peso, altura, largura, profundidade e empilhamento é validado. Código lido, tipo, área, medidas iniciais, perfil ausente, revisão, posições repetidas, conjunto incorreto e outro armazém são recusados conforme o contrato.
- Triagem não disponibiliza nem inicia armazenagem. Quarentena estabelece bloqueio persistente; mover para armazenagem exige liberação expressa. Avariada só ocupa quarentena e não pode ser liberada como boa.
- Liberação revalida endereço; encerramento não remove bloqueio silenciosamente. Encerramento de endereço preserva a ocupação e retira disponibilidade. Produto em encerramento impede posicionamento, mas permite bloqueio preventivo.
- Operação não configura capacidade nem libera; Supervisor não configura capacidade. Consultas, histórico, comandos e repetição exigem os dois escopos. Sem JWT retorna 401; paginação/dados inválidos são recusados; validação do serviço independe do controller.
- Capacidade não muda em posição ocupada ou vinculada a conjunto ativo. Conjunto incompatível/excedido é recusado e conjunto ocupado não pode ser encerrado.
- Divisão pelo fluxo anterior é recusada após endereçamento ou bloqueio, preservando posição e quantidade.
- Falhas forçadas na auditoria e no histórico revertem ocupação, medidas, datas, revisões da unidade/pedido, auditoria e chave de repetição. Corrigida a falha, a mesma solicitação pode ser confirmada uma vez.

## Artefato e inicialização

JAR: `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`.

SHA-256: `A45322A1613106A9EB95F2A101EB5FFCE73545D18125B8D2B5A2C2090516125E`.

O JAR foi iniciado com perfil `local`, endereço `127.0.0.1` e porta aleatória (49999 neste ensaio). `/api/v1/status` retornou a aplicação `wms-rodogarcia` e `DISPONIVEL`; `/actuator/health` retornou 403, conforme bloqueio do perfil, sem expor administração. Não houve datasource/conexão no perfil local. O processo foi encerrado após a conferência. Esse ensaio verifica o artefato e a inicialização; os fluxos persistentes foram exercitados nos testes HTTP/JPA acima.

## O que permanece sem comprovação

V4 foi preparada e revisada contra models, constraints anteriores e procedimento Flyway. Nenhuma migration/conexão foi executada no SQL Server. Faltam validação de dialeto e `ddl-auto=validate`, evolução V3→V4 com etiquetas preexistentes, TLS/alvo/permissões, locks, isolamento de saldo, contenção, recuperação e desempenho no banco real.

Também faltam capacidade efetiva dos endereços/conjuntos, validação operacional com Caio, frontend FE07/FE08, coletor, impressão/leitura física, provedor real e piloto. Reservado permanece zero até BE09; não há saída, cobrança, integração fiscal, emissão ou publicação. As propostas AC04/AC05 continuam identificadas e precisam de exemplos reais antes de uso operacional/cobrança.

Registros anteriores 13/15/19/21 permanecem como evidências históricas. O [states.md](../states.md) separa o código entregue, a validação externa e a próxima frente BE09.
