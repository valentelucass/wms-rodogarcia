# Manutenção dos avisos Java — 06/10/2026

Recorte BE16 posterior ao aceite local D19, solicitado pelos diagnósticos do editor. Não altera contratos HTTP, regras de negócio, migrations ou telas frontend.

## Alterações

- Substituídas as três anotações Hibernate `@Check` por `@Table(check = @CheckConstraint(...))` da Jakarta Persistence. As expressões SQL foram comparadas com o estado anterior e permanecem idênticas.
- Substituídas 60 chamadas Jackson `asText()` por `asString()`, em produção e testes. São aliases na versão utilizada.
- Atualizada a consulta de vínculos externos do POI para `getExternalLinksTables()`, preservando a recusa de planilhas com esses vínculos.
- Removidos campos/injeções sem uso, 12 auxiliares privados de testes e variáveis locais não utilizadas. Chamadas que preparam estoque, fatos e fechamentos continuam executadas; nenhum cenário de teste foi removido.
- Declarado `serialVersionUID` nas duas exceções que apresentaram esse aviso durante a conferência Eclipse JDT.
- Configurada atualização automática Java/Maven em [settings.json](../../.vscode/settings.json). A primeira gravação do POM com conteúdo idêntico não resolveu os diagnósticos informados pelo responsável; a intervenção posterior no servidor Java está registrada abaixo.

Referências das substituições: [Hibernate Check](https://docs.hibernate.org/orm/7.3/javadocs/org/hibernate/annotations/Check.html), [Jackson JsonNode](https://raw.githubusercontent.com/FasterXML/jackson-databind/3.x/src/main/java/tools/jackson/databind/JsonNode.java) e [Apache POI 5.5.1](https://raw.githubusercontent.com/apache/poi/REL_5_5_1/poi-ooxml/src/main/java/org/apache/poi/xssf/usermodel/XSSFWorkbook.java).

## Validação executada

Com JDK 21, na pasta `backend`:

```powershell
.\mvnw.cmd '-Dwms.build.directory=target-avisos' '-Dmaven.compiler.showDeprecation=true' spotless:apply clean verify
```

- **BUILD SUCCESS**, em 06/10/2026 às 11:49:49 -03:00: **367 testes, zero falhas, zero erros e zero ignorados**, conferidos em 17 XMLs do Surefire. Inclui formatação, arquitetura e testes unitários/HTTP com banco H2 isolado e dados fictícios.
- Compilação adicional dos 276 fontes Java principais/de teste com Eclipse JDT 3.46.100, JDK 21 e dependências locais do projeto: **zero avisos e zero erros**, incluindo depreciações, imports, variáveis e membros privados sem uso. Argumentos e log locais em `backend/target-avisos-baseline/`.
- `graphify update .` concluído após as alterações Java. As regras de exclusão também retiraram arquivos gerados do mapa.
- JAR e relatórios novos em `backend/target-avisos/`, log em `backend-avisos-verify.log`; gerados ignorados pelo Git. SHA-256 do JAR: `47A9F56693D1B2DC80C965E5D3F602F34AF2F88A81A0D1741624B905AC068CF2`.

O JAR D19 de `backend/target-be14/` foi preservado e seu hash histórico conferido. Esta validação de manutenção complementa as evidências anteriores; não reapresenta o artefato antigo como se contivesse as mudanças atuais. SQL Server, identidade real, equipamentos, fiscal/comercial e homologação continuam pendentes de validação externa.

## Complemento: sincronização do editor e teste D20

Em 06/10/2026, o responsável informou que os dois avisos do POM permaneciam no painel Problems. Foi reiniciado somente o processo JDT Language Server ligado ao workspace WMS; a extensão Java efetuou o reinício automático. O log do servidor confirmou importação Maven às 12:01:17, término dos builds às 12:01:30 e atualizações de `wms-backend` às 12:02:48, 12:05:12 e 12:09:19 (-03:00). O nível dos diagnósticos não foi reduzido. O comentário de reimportação e o rastreamento temporários foram retirados; a configuração final contém apenas a atualização automática. Alterações concorrentes do perfil `sqlserver-it` de D20 foram preservadas. O controle de janela estava indisponível; a confirmação obtida é dos registros do editor, sem inspeção visual do painel.

O aviso adicional em `SqlServerUpdateMappingTest` foi corrigido substituindo `forEachOperation(...)` pelo percurso indexado com `getNumberOfOperations()` e `getOperation(indice)`, conforme a [API Hibernate](https://docs.hibernate.org/orm/7.3/javadocs/org/hibernate/sql/model/MutationOperationGroup.html). Nenhuma asserção foi removida por esta manutenção.

- Compilação adicional Eclipse JDT: zero avisos/erros, registrada em `backend/target-avisos-baseline/ecj-editor.log`.
- Validação final direcionada em `backend/target-avisos-editor/`: **1 teste aprovado, zero falhas/erros/ignorados**, BUILD SUCCESS às 12:09:42. Comando: `mvnw.cmd -Dwms.build.directory=target-avisos-editor -DspotlessFiles=src/test/java/br/com/rodogarcia/wms/SqlServerUpdateMappingTest.java -Dmaven.compiler.showDeprecation=true -Dtest=SqlServerUpdateMappingTest spotless:apply test`. Log: `backend-avisos-editor-test-final.log`.
- Tentativas anteriores encontraram modelos D20 ainda em edição e formatação pendente. A execução final usou os modelos atualizados e limitou a formatação ao teste solicitado, sem alterar as regras dos modelos.
- Graphify atualizado. Não houve nova execução completa dos 367 testes nem conexão JDBC neste complemento; o teste inspeciona SQL gerado pelo dialeto SQL Server sem banco externo.
