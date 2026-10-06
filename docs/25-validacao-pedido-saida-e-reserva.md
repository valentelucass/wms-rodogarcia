# Validação do pedido de saída, FIFO e reserva — D18

Executada em 05/10/2026, somente em desenvolvimento local. Recorte BE01/BE09 e complemento de saldo BE14, com contratos para FE08/FE09 no [documento 24](24-pedido-saida-fifo-e-reserva.md). FE09 não foi iniciado. O [states.md](../states.md) mantém BE09 em validação externa; esta evidência não comprova SQL Server ou operação no armazém.

## Autorização, divisão e preservação

Hermes WMS encaminhou a autorização registrada em D18. Farol conferiu `maestri list`: WMS - Cedro, WMS - Prumo e WMS - Vigia com os papéis esperados. Cedro escreveu `backend/**` e documento 24; Prumo escreveu somente V5 e README de migrations; Vigia revisou em leitura; Farol consolidou estados, decisões, continuidade, índice e mapa. Lume não recebeu implementação frontend.

Git conferido antes e depois: branch `main`, zero commits; o conteúdo preexistente já aparecia como não rastreado. Não houve commit/push, reset, stash ou remoção de arquivos. Comparação SHA-256 de V1–V4 com a base inicial confirmou sua preservação. Alterações BE08 se limitaram à integração de reserva, saldo e avaria; seus 31 testes e as demais suítes anteriores passaram no build completo. Configurações Spring continuam em properties, com MVC por camadas e sem novas dependências.

## Comandos e saída real

JDK Eclipse Adoptium 21.0.12.1 e Maven Wrapper 3.9.16, conferidos também por Farol com `./mvnw.cmd -v`. Cedro executou a partir de `backend`, com JAVA_HOME no JDK 21:

```powershell
./mvnw.cmd -B -ntp spotless:apply
./mvnw.cmd -B -ntp -Dtest=PedidoSaidaIntegrationTest test
./mvnw.cmd -B -ntp clean verify
```

Formatação: **BUILD SUCCESS**, às 22:02:12, duração de 3,463 s; 129 arquivos Java conferidos. [Saída completa](../backend/evidencias/d18-spotless-final.log).

Build: **BUILD SUCCESS**, às **22:04:17**, horário de São Paulo; duração de **2min01s**. **197 testes, zero falhas, zero erros e zero ignorados**. Enforcer, Spotless e ArchUnit aprovados. [Saída completa](../backend/evidencias/d18-clean-verify.log). Farol leu esse output e os 12 relatórios XML de `target/surefire-reports`; a soma coincide com o log.

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
| Estoque BE08 | 31 |
| Pedido de saída — novos | 44 |
| **Total** | **197** |

A bateria isolada dos 44 testes também passou, às 22:01:00, antes do build completo: [log](../backend/evidencias/d18-testes-saida-ajustes.log). A primeira execução identificou comparações de representação decimal e instrumentação de spy/transação inadequadas; foram corrigidas e a execução repetida. O [log inicial](../backend/evidencias/d18-testes-saida-inicial.log) permanece como histórico, não como aprovação.

## Comportamentos conferidos

- Criação valida proprietário, armazém, SKU, precisão e saldo disponível por item; insuficiência reverte pedido, itens, auditoria e operação. Criar não reserva: confirmação revalida a disponibilidade.
- Pedidos concorrentes não reservam a mesma unidade nem excedem o saldo; duas chaves do mesmo pedido não duplicam reserva. Mesma chave/conteúdo confirma uma vez; payload diferente conflita. A confirmação original é preservada após reversão; GET/revalidação mostram o estado atual.
- Pedido com vários itens reserva tudo ou reverte tudo. Parcial de pallet conserva físico, composição, origem, FIFO, posição e etiqueta. O remanescente fica protegido conforme proposta AC10; saldo reconcilia físico, disponível, reservado e bloqueado, sem duplicar unidade em duas posições.
- FIFO prioriza data original antes da chegada real/localização e aplica desempate documentado. Qualquer perfil com alcance pode justificar; Supervisor/Gestor autorizam. Seleção parcial, duplicada, sem motivo, antiga ou indisponível é recusada. Bobinas 4 e 6 para pedido 6 podem atender por exceção integral com a bobina de 6; não se fraciona bobina.
- Triagem, quarentena, condição avariada, bloqueio e cadastros encerrados ficam indisponíveis. Bloqueio ou avaria posterior mantêm reserva e físico, sinalizam unidades impedidas no pedido e recusam prosseguimento; liberação preventiva preserva reserva e não repara avaria.
- Testes usam transações concorrentes distintas. O cenário de encerramento pausa a reserva após a leitura de candidatas e verifica que o encerramento do endereço aguarda seu commit. Disputa entre reserva e dano não libera saldo silenciosamente.
- Cancelamento/reversão são integrais e preservam histórico; nova reserva após reversão usa outro UUID. Avaria não vira estoque disponível ao cancelar. Falhas forçadas na auditoria ou gravação da resposta idempotente revertem linhas, ponte, versões, estado e saldo.
- Perfis e os dois escopos são conferidos nos serviços, inclusive repetição; testes também exercitam validação sem controller, autenticação HTTP e contratos inválidos.

Dados fictícios, HTTP em porta aleatória, JWT com chave RSA efêmera e JPA/H2 isolado. A suíte cria a estrutura pelos models; não executa V1–V5.

## Arquivos entregues

Novo fluxo MVC: `PedidoSaidaController`, `PedidoSaidaService`, `PedidoSaidaDto`, quatro models/repositories de pedido, item, reserva e operação e dois enums de situação. Testes em [PedidoSaidaIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/PedidoSaidaIntegrationTest.java). A integração altera unidade/repository, estoque/DTO/controller, movimentos, auditoria, segurança e encerramento de endereço. Inventário completo e resultados de Cedro no [relatório de entrega](../backend/evidencias/d18-entrega.txt).

Contrato no documento 24; [V5](../database/migrations/V5__pedidos_saida_fifo_e_reservas.sql) e procedimento de migrations por Prumo. Farol mantém esta evidência, states, decisão D18, continuidade, índice e Graphify. Não houve escrita concorrente nesses recortes.

## Revisão e artefato

Vigia identificou e revisou três correções: saldo na criação (Q07), seleção excepcional integral de bobinas e proteção compartilhada do armazém no encerramento do endereço. Parecer final pelo terminal WMS, às 22:04: **favorável ao recorte local D18, sem novos achados materiais**. Conferiu código, testes, logs, JPA/V5, contrato e integração BE08, sem editar ou executar builds.

Prumo conferiu 30 colunas novas e duas extensões da unidade contra os models, tipos, nulabilidade, FKs, estados, referências e codificação. Preservou sete tipos e 19 ações anteriores da auditoria. V5 acrescenta índice filtrado de reserva ativa e CHECKs não reproduzidos integralmente pelo H2. [Migration e procedimento](../database/migrations/README.md).

JAR gerado: `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`.

SHA-256: `61C3FCD165442F7B5F6BB1061DA4DCC576CBCF82D11DB5A14C4154FF157464C3`.

Não houve execução independente desse JAR como aplicação nesta entrega; os fluxos HTTP foram exercitados nos testes. Artefato construído, teste local, homologação e versão publicada permanecem resultados distintos.

## Mapa e limites externos

Farol executou `graphify update .` após o código: extração AST sem LLM, **2002 nós, 7466 relações e 109 comunidades** naquela passagem. Complementou a extração semântica de nove arquivos de contrato, migration/procedimento, evidência e registros de continuidade, preservando o histórico. Executou `graphify cluster-only . --no-label` e consulta de BE09/FIFO/reserva/197 testes; JSON, HTML e relatório atualizados, com zero relações apontando para nó ausente. Custo de API externo zero; tokens da extração pelo agente não medidos. A conferência documental verificou links, codificação e coerência de status, sem novas execuções da aplicação.

Nenhuma conexão ou migration SQL Server real foi executada. Faltam criação/evolução V1–V5, validação JPA no alvo, índice filtrado/CHECKs, opções JDBC, permissões, isolamento, deadlocks, contenção, recuperação, collation e desempenho reais. H2 comprova somente os cenários locais executados; não substitui esses ensaios.

Também faltam provedor real, homologação operacional com Caio, frontend/coletor, impressão e leitura física, separação e retirada BE10, tratamento completo de avaria/retorno BE11 e fiscal/cobrança. Não houve emissão fiscal, cobrança real, publicação, rotinas, alteração de perfis Hermes ou uso do ETL.

Próximo bloco proposto: BE01/BE10, separação, documentos e confirmação física, após nova demanda. Os contratos FE08/FE09 estão disponíveis; nenhuma tela foi entregue. AC10/AC11 continuam identificadas como propostas/interpretações, sem aprovação comercial presumida.
