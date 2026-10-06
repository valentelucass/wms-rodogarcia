# Validação do bloco de cadastros

Registro da entrega D13. A ampliação posterior de recebimento e o build atual estão no [documento 19](19-validacao-recebimento-backend.md); os números abaixo preservam a evidência desta entrega.

Entrega de 05/10/2026, referente a recortes de BE01/BE03/BE04/BE05 descritos no [documento 14](14-cadastros-acesso-e-persistencia.md), com a ampliação D13/BE16 do [documento 16](16-padroes-de-engenharia-backend.md). Validação local em Windows, JDK 21.0.12.1 e Maven Wrapper 3.9.16.

## Resultado executado

Comandos finais em `backend`, com `JAVA_HOME` no JDK 21. Depois da conversão para properties, da validação dos serviços e dos ajustes de imports/formatação, o build limpo passou:

```powershell
./mvnw.cmd -B -ntp spotless:apply
./mvnw.cmd -B -ntp clean verify
```

**BUILD SUCCESS: 52 testes, zero falhas, zero erros, zero ignorados.** Enforcer e Spotless aprovados na fase `validate`. JAR gerado em `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`. Relatórios locais em `backend/target/surefire-reports` e saída em `backend/target/verify-be16.log`; arquivos gerados não são versionados. O resultado anterior de D12 era 40 testes; D13 acrescentou 12.

| Grupo | Testes | O que comprova |
| --- | --- | --- |
| [CadastrosIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/CadastrosIntegrationTest.java) | 28 | HTTP real, JPA/H2, cinco cadastros, JWT RSA, perfis/alcances, duplicidade, precisão, validade, endereço, encerramento/reativação, revisão concorrente, rollback, auditoria e validação/permissão em chamadas diretas aos serviços |
| [SqlServerConfigTest](../backend/src/test/java/br/com/rodogarcia/wms/config/SqlServerConfigTest.java) | 4 | Recusa de alvo não conferido, credenciais ausentes da representação textual, bloqueio de DDL e scripts automáticos antes de abrir conexão |
| [ApiHttpIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/ApiHttpIntegrationTest.java) | 4 | Base HTTP com persistência desligada, status e bloqueios originais |
| [AmbienteConfigTest](../backend/src/test/java/br/com/rodogarcia/wms/config/AmbienteConfigTest.java) | 3 | Perfis aceitos/recusados e prevenção de combinação acidental |
| [ApiExceptionHandlerTest](../backend/src/test/java/br/com/rodogarcia/wms/exceptions/ApiExceptionHandlerTest.java) | 6 | JSON/DTO inválido, tratamento seguro de falha e violação do contrato do serviço, códigos HTTP e cabeçalhos |
| [ArquiteturaTest](../backend/src/test/java/br/com/rodogarcia/wms/ArquiteturaTest.java) | 5 | Dependências entre camadas, isolamento de controllers/models/DTOs e ausência de injeção por campo com `@Autowired` no código principal |
| [ConfiguracaoPropertiesTest](../backend/src/test/java/br/com/rodogarcia/wms/config/ConfiguracaoPropertiesTest.java) | 2 | Perfil local sem persistência; perfil SQL substitui exclusões, resolve variáveis fictícias e mantém proteções, sem criar conexão |

Resultados relevantes: dois updates simultâneos com a mesma versão produziram um sucesso e um conflito, com apenas uma auditoria de alteração; falha forçada na tabela de auditoria desfez também a criação do cliente; usuário sem alcance não recebeu registros de outro cliente/armazém; assinatura adulterada, emissor/audiência errados e token expirado foram recusados. Teste de duplicidade confere também ausência do documento fiscal e SQL nos logs/resposta. Um teste mantém o lock do cliente, encerra o produto enquanto a criação da embalagem aguarda e verifica a recusa do novo vínculo após liberar o lock.

O aviso de depreciação JSON da base anterior foi corrigido. Permanece aviso do Mockito sobre instrumentação automática no Java 21, sem falha nos testes; nenhuma chave RSA de teste é persistida.

## Artefato e preparação de migração

O JAR final de D13 foi executado separadamente em perfil local padrão, porta aleatória: HTTP 200 em `/api/v1/status`, listener em `127.0.0.1` e persistência desligada. O processo foi encerrado depois da conferência. O JAR contém os três arquivos `application*.properties` de execução; não contém YAML de configuração, H2, ArchUnit nem configuração/chaves dos testes.

`database/migrate.ps1` passou pela análise sintática do PowerShell, sem execução do script. Artefatos Flyway 12.4.0 e seu módulo SQL Server foram encontrados no Maven Central. V1 foi preparada e revisada junto aos mapeamentos; **não foi executada** em SQL Server ou em H2. H2 cria sua estrutura somente para testes a partir do JPA.

Conferidos UTF-8 e referências locais de 26 documentos de trabalho, codificação de 64 arquivos Java e 6 properties (incluindo o wrapper), ausência de imports Java com `*` e de YAML em resources. São 16 IDs BE e 13 FE únicos; BE16 foi acrescentado após BE15, preservando os anteriores. As fontes originais foram preservadas.

## Limites

- Nenhuma conexão ao SQL Server da empresa, migration aplicada, GRANT ou alteração externa.
- H2 comprova os comportamentos exercitados nesse banco, não o dialeto, locks, constraints de V1 ou recuperação do SQL Server.
- Tokens dos testes usam chave RSA efêmera. Emissor real, contas, expiração/renovação/revogação e integração de login não foram homologados.
- Não foram implementados/testados estoque, recebimento, reserva, saída, fiscal, cobrança, frontend ou equipamentos.
- Nenhuma publicação ou serviço WMS deixado em execução.

Próximos passos no [states.md](../states.md): avançar contratos operacionais/recebimento e partes restantes de BE05; validar banco/identidade quando o alvo e o provedor forem definidos. Não marcar BE03, BE04, BE05 ou M01 como concluídos por este resultado local.
