# Validação do macrobloco de recebimento

Registro histórico D15. O estado posterior, com unitização, está na [validação D16](21-validacao-unidades-logisticas.md).

Entrega D15 de 05/10/2026. Escopo, contratos e limites em [recebimento e conferência](18-recebimento-e-conferencia.md). Executado localmente em Windows, JDK 21.0.12.1 e Maven Wrapper 3.9.16. Testes HTTP/JPA usam H2 isolado e tokens assinados com chave efêmera.

## Construção e testes executados

Na pasta `backend`, com `JAVA_HOME` no JDK 21:

```powershell
./mvnw.cmd -B -ntp spotless:apply
./mvnw.cmd -B -ntp clean verify
```

**BUILD SUCCESS: 86 testes, zero falhas, zero erros, zero ignorados.** Enforcer, formatação, compilação, testes de arquitetura e empacotamento aprovados. O resultado anterior de D13 era 52 testes; este bloco acrescentou 34. A primeira execução dirigida de recebimento passou com 26 casos; a suíte final inclui mais dois cenários de serviço/concorrência e seis testes de XML.

| Grupo | Quantidade | Evidência |
| --- | --- | --- |
| Base, cadastros, configuração, erros e arquitetura | 52 | Suíte anterior preservada, descrita no documento 15; regras ArchUnit também passaram com as novas classes |
| [RecebimentoIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/RecebimentoIntegrationTest.java) | 28 | HTTP real, JPA, permissões, vários dias/notas, divergências, estornos, XML, concorrência, repetição, validação direta de serviço e rollback |
| [NfeXmlServiceTest](../backend/src/test/java/br/com/rodogarcia/wms/services/NfeXmlServiceTest.java) | 6 | DTD/entidades externas e internas, ausência de acesso HTTP externo, profundidade/bytes, formato/namespace/campos duplicados e dois envelopes aceitos |

Resultados conferidos:

- Nota recebida em dois dias conserva o FIFO da primeira chegada, a data real de cada parcela e apenas uma efetivação; todas as entradas continuam indisponíveis para saída.
- 98 ou 102 recebidas para 100 previstas exigem aceite; quantidade real e diferença permanecem identificáveis.
- 95 boas e 5 avariadas bloqueiam o pedido inteiro; após tratativa, os 5 avariados permanecem em quarentena.
- Estorno preserva o fato original, impede estorno repetido e recalcula FIFO com as chegadas ativas; não permite editar uma entrada já efetivada.
- Duas chamadas simultâneas com a mesma chave de chegada gravam um fato e uma auditoria. Chaves diferentes com a mesma versão produzem um sucesso e um conflito. Duas efetivações simultâneas produzem uma única entrada.
- Falha forçada na auditoria desfaz entradas, situação e versão; a tentativa posterior consegue concluir após retirar a falha de teste.
- Permissões exigem simultaneamente cliente e armazém; Operação recebe, mas não efetiva, estorna ou cancela. Chamada direta ao serviço também recusa permissão/dados inválidos.
- Produto em encerramento impede efetivação. Itens de outro pedido/cliente, quantidade negativa/zero/fracionária incompatível, falta de lote/validade e data futura são recusados sem deixar chegada incompleta.
- XML não produz chegada ou estoque, não duplica nota e pode complementar valor desconhecido de nota manual sem mudar recebimento. Incompatibilidade de emitente, SKU, unidade, quantidade ou valor conhecido é recusada.
- Um servidor HTTP temporário, exclusivamente em loopback, recebeu **zero acessos** durante o teste de entidade externa e foi encerrado. Erros não expõem o endereço ou conteúdo do XML.

Saída local em `backend/target/recebimento-verify.log`; relatórios em `backend/target/surefire-reports`. JAR em `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`. Esses arquivos são gerados e ignorados pelo versionamento. Permanece o aviso de instrumentação automática do Mockito/JDK 21 já registrado em D13, sem falha nos testes.

## Banco e limites da comprovação

O JAR final foi executado separadamente no perfil local padrão, em porta aleatória: `/api/v1/status` respondeu 200, listener somente em `127.0.0.1`, sem DataSource/JPA inicializados. O módulo de recebimento está empacotado, com configuração properties e sem H2/ArchUnit/configuração de testes no artefato. O processo de verificação foi encerrado.

SHA-256 do JAR conferido: `6E53417CBBBDA9FFD8AF7C82B145CC45B68CA393D8E4F22C4D0B3612324CC975`. Na conferência documental, passaram referências locais/UTF-8, 87 arquivos Java, seis properties e unicidade dos IDs BE/FE/OR e D01 a D15. A documentação paralela de orquestração (D14/OR01/documento 17) foi preservada; esta entrega usa D15/documentos 18/19.

V2 foi preparada com seis tabelas, FKs, restrições, índices e evolução dos tipos/ações de auditoria. V1 foi preservada. **Nenhuma V1/V2 foi executada no SQL Server ou em H2:** o banco H2 de cada suíte é criado pelo mapeamento JPA somente para teste.

Não foram validados dialeto, concorrência, locks, índices filtrados, permissões, evolução com dados existentes ou recuperação no SQL Server. Esses pontos estão no procedimento de BE03. Não houve conexão ao banco da empresa, GRANT, emissão fiscal, envio externo ou publicação.

Os XMLs de teste são exemplos sintéticos dos campos usados pelo extrator; não são documentos fiscais autorizados nem comprovação de conformidade integral com o XSD. Faltam documentos reais de homologação, validação fiscal completa e integração com provedores reais. O modo de autenticação permanece o contrato existente, sem contas/chaves reais nos testes.

BE07/BE08 ainda precisam unitizar, identificar, endereçar e produzir disponibilidade de estoque; reserva, saída, cobrança e equipamentos continuam futuros. FE05 não foi implementada. O resultado comprova o recorte backend local, sem declarar o módulo conjunto ou piloto homologado.
