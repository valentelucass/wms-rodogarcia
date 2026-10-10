# Padrões de engenharia do backend

Registrado em 05/10/2026, conforme D13 e BE16. O responsável pediu configuração em `.properties` e boas práticas de engenharia. As ferramentas abaixo são escolhas técnicas para tornar esses padrões verificáveis no backend atual; não representam homologação dos módulos futuros.

## Organização e responsabilidades

- Manter Spring MVC nas pastas por camada definidas em AGENTS.md. Controllers traduzem HTTP/DTOs e chamam serviços; não acessam repositories, entidades ou JDBC diretamente.
- Serviços coordenam regras, permissões e transações. Receber dependências pelo construtor, com campos `final`; não instanciar manualmente serviços gerenciados pelo Spring. Seus proxies aplicam transações e validação de parâmetros.
- Repositories concentram acesso persistente. Models não dependem de controllers, serviços, DTOs ou configuração da aplicação. DTOs delimitam entradas e saídas; entidades JPA não são respostas HTTP.
- Usar Bean Validation nos DTOs e nas entradas públicas dos cinco serviços cadastrais (`@Validated`, `@Valid`, `@NotNull` e IDs positivos). Invariantes entre campos continuam nos serviços; restrições relacionais e unicidade também ficam no banco.
- Escritas têm transação explícita. Auditoria participa da mesma transação, obrigatoriamente. Versão recebida e locks protegem alterações concorrentes; conferir os testes antes de mudar a ordem dos locks ou antecipar consultas de entidades gerenciadas.
- Métodos de leitura são `readOnly`; `open-in-view` permanece desligado. Mapear DTOs dentro da transação e limitar consultas com paginação. Precisões e quantidades usam `BigDecimal`, sem conversão financeira para `double`.
- Para processamento de dados e redução de hidratação/consultas repetidas, aplicar a [decisão QUAL-CONF01-PERF01-DEC01](06-decisoes-e-pendencias.md#qual-conf01-perf01-dec01--processamento-adequado-no-banco-10102026): preferir execução adequada no banco com equivalência funcional e benefício medidos, mantendo coordenação, autorização e transação nos serviços.
- Preservar erros padronizados, correlação por operação e logs sem valores sensíveis. Autorização deve existir nos serviços, além da proteção HTTP. Não confiar em IDs ou funções informados pelo navegador.
- Evitar herança de CRUD e interfaces sem necessidade concreta. Compartilhar apenas comportamentos realmente comuns, como paginação, revisão e auditoria; regras próprias continuam em serviços explícitos.

## Configuração e construção

Configuração comum em `application.properties`; diferenças em `application-local.properties`, `application-sqlserver-dev.properties` e `application-test.properties`. A configuração H2/identidade fictícia permanece em `src/test/resources/cadastros-test.properties`. Não misturar YAML com properties para a configuração Spring.

O perfil local continua sem banco. A lista de exclusões de autoconfiguração é uma propriedade única, substituída por valor vazio no perfil SQL Server. Um teste carrega os perfis, resolve variáveis fictícias e confere essa precedência sem criar DataSource. O perfil SQL mantém `validate`, scripts automáticos desligados, alvo confirmado e segredos por variáveis externas. Migrações seguem o procedimento próprio em `database`.

| Verificação | Escolha técnica | Aplicação |
| --- | --- | --- |
| Ferramentas | Maven Enforcer 3.6.3 | Fase `validate`: JDK 21 e Maven de 3.9.16 até antes de 4; usar o wrapper fixado em 3.9.16 com SHA-256 |
| Formatação | Spotless 3.10.3 + google-java-format 1.28.0, estilo AOSP | Java principal e testes; quatro espaços, UTF-8, LF, imports explícitos/sem sobras; `validate` confere e falha sem editar arquivos |
| Editor | `.editorconfig` e `.gitattributes` | Consistência de codificação, espaços e finais de linha; scripts `.cmd` usam CRLF |
| Arquitetura | ArchUnit 1.5.1, somente em testes | Fronteiras entre controllers, services, repositories, models e DTOs; proibição de injeção por campo com `@Autowired` no código principal |

Versões de bibliotecas de aplicação continuam geridas pelo BOM do Spring Boot. Versões adicionais estão fixadas no `pom.xml`. H2 e ArchUnit têm escopo de teste e não integram o JAR operacional. Ferramentas de formatação não reescrevem regras de negócio.

Na pasta `backend`, com `JAVA_HOME` apontando para JDK 21:

```powershell
./mvnw.cmd -B -ntp spotless:apply
./mvnw.cmd -B -ntp clean verify
```

O primeiro comando aplica a formatação quando houver alterações de Java. O segundo confere ferramentas e formato, compila, executa testes e empacota. A verificação não deve editar código nem executar migrações. Alteração somente documental continua exigindo apenas conferência proporcional.

## Evidências e continuidade

A execução inicial de BE16 está no [registro de validação D13](15-validacao-cadastros-backend.md); o [registro D15](19-validacao-recebimento-backend.md) inclui recebimento e o [registro D16](21-validacao-unidades-logisticas.md), unidades logísticas. Os testes conferem os perfis properties, dependências entre camadas, validação/permissão em chamadas diretas aos serviços e resposta segura para violações de contrato.

O padrão precisa acompanhar cada entrega: testar os riscos reais alterados, atualizar contratos e estados e corrigir qualquer falha do build. SQL Server, identidade real, integrações fiscais, impressão, recuperação e desempenho no ambiente real continuam com validações próprias; estas ferramentas não os comprovam.

Referências técnicas consultadas: [Spotless para Maven](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md), [Enforcer](https://maven.apache.org/enforcer/maven-enforcer-plugin/usage.html), [ArchUnit](https://www.archunit.org/userguide/html/000_Index.html), [validação de métodos no Spring](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html) e [google-java-format 1.28.0](https://github.com/google/google-java-format/releases/tag/v1.28.0).
