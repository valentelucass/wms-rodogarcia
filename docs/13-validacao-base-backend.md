# Validação da base do backend

Executada em 05/10/2026, para **BE02**, no Windows local com Temurin JDK 21.0.12.1, Maven Wrapper e as versões registradas em [base técnica](12-base-e-contratos-backend.md).

## Build e testes

Comando executado em `backend`, com `JAVA_HOME` apontando para o JDK 21:

```powershell
./mvnw.cmd -B -ntp clean verify
```

Resultado: **BUILD SUCCESS**. JAR gerado em `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`. **12 testes executados, zero falhas, zero erros, zero ignorados**.

| Grupo | Quantidade | Evidência de comportamento |
| --- | --- | --- |
| [ApiHttpIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/ApiHttpIntegrationTest.java) | 4 | Servidor HTTP real em porta aleatória; status 200; acesso negado 403 sem login/sessão; escrita recusada; UUID gerado e distinto por solicitação |
| [ApiExceptionHandlerTest](../backend/src/test/java/br/com/rodogarcia/wms/exceptions/ApiExceptionHandlerTest.java) | 5 | JSON inválido; validação de DTO; falha interna sem dados sensíveis na resposta/log; 405 preservando `Allow`; 415 para formato incompatível |
| [AmbienteConfigTest](../backend/src/test/java/br/com/rodogarcia/wms/config/AmbienteConfigTest.java) | 3 | Perfil local permitido; perfil operacional recusado; combinação de perfis recusada |

Relatórios locais em `backend/target/surefire-reports`. `target` é gerado e ignorado no versionamento; `clean` remove esses relatórios. Este registro resume o resultado observado, sem substituir a repetição do build em outra máquina.

O build apresentou avisos de API de JSON depreciada nos testes e de instrumentação automática do Mockito no Java 21. Não impediram compilação, testes ou empacotamento; são pontos de manutenção das dependências de teste.

## Execução do artefato

Executado o JAR empacotado com `--server.port=0` e sem perfil explícito. Observado:

- Perfil padrão `local`.
- Listener somente em `127.0.0.1`, confirmado pelo processo no sistema operacional.
- `GET /api/v1/status` retornou 200, `DISPONIVEL`, JSON e `X-Request-Id`.
- Nova execução com `--spring.profiles.active=prod` foi recusada pela proteção de ambiente e encerrou com código diferente de zero.
- Ambos os processos de verificação foram encerrados; não há publicação ou serviço WMS mantido em execução por esta entrega.

A distribuição Maven foi conferida com SHA-512 publicado no Maven Central; seu SHA-256 foi fixado no wrapper. Não foi necessário instalar Maven global nem alterar o Java padrão da máquina.

Conferidos também: JAR sem drivers/ORM/migrations ou controllers de teste; referências locais e UTF-8 dos 21 documentos de trabalho; manutenção dos 15 IDs BE e 13 FE, sem duplicação. Arquivos originais de referências foram preservados.

## Limites e próximo passo

Sem conexão ao SQL Server, migration, persistência simulada, autenticação de usuários, teste de estoque/fiscal/cobrança, frontend ou equipamentos. Não há homologação nem versão operacional publicada. O status confirma somente o processo HTTP.

BE02 atende à base local prevista. BE01 permanece parcial: detalhar modelo e contratos de cliente/armazém/produto/embalagem, mantendo a ligação com o restante do ciclo operacional. O alvo SQL Server e o procedimento de migração continuam em BE03, e identidade/permissões em BE04. Estados oficiais em [states.md](../states.md).
