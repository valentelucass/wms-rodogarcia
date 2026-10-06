# Configuração externa preparada — BE03/BE04/BE15

**D20 corrente:** [prontidão](d20-prontidao.md) e [contrato atual](../database/d20-procedimentos.md) prevalecem sobre a configuração histórica D19 abaixo. Aplicação Cedro agora inicializa SETs e confirma ServerName/banco em cada conexão; migration usa WMS_DB_MIGRATION_USER/PASSWORD distintos e initSql externo transitório do wrapper. sa somente CREATE separado, via SecureString/PSCredential local; nenhum segredo persistido ou SQL executado. HOST confirmado não elimina o bloqueio de credencial protegida.

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Preparação local D19 de 05/10/2026. Este arquivo descreve entradas e conferências; não configura servidor, fornecedor, conta, certificado ou segredo. Configuração Spring continua em `.properties` no backend. Fontes: [contrato de identidade](../docs/14-cadastros-acesso-e-persistencia.md), [engenharia](../docs/16-padroes-de-engenharia-backend.md) e [migrations](../database/migrations/README.md).

## Perfis e alvo

Complemento final de preparo em06/10 no [pacote integrado](preparacao-tecnica-final.md): ciclo de contas/renovação/revogação, token antigo após alcance revogado, caches/tolerâncias e chaves indisponíveis, com donos externos Lucas/TI/Gestor. Fonte JwtWmsValidator guarda duração nominal15min/emissão futura além de60s; não prova revogação imediata ou disponibilidade. Novo [diagnóstico](verificar-preparacao-final-local.ps1) preserva56 e cobre V1–V9 sem acessar valores de ambiente/JWK/TLS/status/segredos. Artefato final/JPA BE14 continuam pendentes; fonte lida não é aplicação iniciada.

O perfil padrão é `local`, com endereço HTTP `127.0.0.1`, sem persistência. `test` é isolado; H2 não acompanha o JAR. `sqlserver-dev` abre conexão e exige alvo exclusivo, identidade externa e estrutura previamente preparada. A aplicação aceita exatamente um desses perfis; homologação/produção/hospedagem continuam a definir. Este preparo não autoriza iniciar `sqlserver-dev` nem executar `database/migrate.ps1`, inclusive `Info`/`Validate`.

| Entrada externa | Uso atual | Quem define/fornece |
| --- | --- | --- |
| `WMS_DB_HOST`, `WMS_DB_NAME` | Host/banco exclusivos do WMS; formato limitado pela configuração atual | Lucas/TI e DBA |
| `WMS_DB_PORT` | Porta; aplicação assume 1433 se ausente, mas o procedimento de migrations exige valor explícito | Lucas/TI e DBA |
| `WMS_DB_USER`, `WMS_DB_PASSWORD` | Identidade técnica; valores diferentes para aplicação e migration, em contextos de execução separados | DBA/TI por mecanismo protegido |
| `WMS_DB_CONFIRMED_TARGET` | Conferência exata `host:porta/banco`; não substitui identidade real do servidor | Lucas/TI e DBA |
| `WMS_DB_CONFIRMED_SERVER` | Nome real esperado pelo script de migrations; a aplicação atual não consulta essa variável | DBA/TI |
| `WMS_OIDC_ISSUER` | Emissor HTTPS do JWT | Equipe técnica com Lucas |
| `WMS_OIDC_JWK_SET_URI` | Endpoint HTTPS de chaves públicas do emissor | Equipe técnica com Lucas |
| `WMS_OIDC_AUDIENCE` | Audiência destinada ao WMS | Equipe técnica com Lucas |
| `WMS_PORT` | Porta HTTP local, opcional; padrão 8080 | Equipe técnica |

Nenhum valor real dessas entradas foi coletado. Não preencher este arquivo com valores. Entregar segredos por mecanismo externo protegido, com acesso mínimo, rotação e responsáveis definidos por TI. Não colocar senha/token em argumentos, exemplos, dump de ambiente, URL, log, relatório, planilha ou memória dos agentes. O WMS verifica JWT com chaves públicas; não recebe a chave privada do emissor.

O script de migration confirma banco e `SERVERPROPERTY('ServerName')` por TLS antes do Flyway. O backend atual confirma o formato do alvo configurado e usa TLS, mas não executa essa mesma consulta de identidade. Esta diferença deve constar do ensaio com TI; não declarar equivalência entre as duas verificações.

## Contrato de identidade e ciclo de contas

O backend é Resource Server, sem login próprio ou emissão de tokens. `CadastrosSegurancaConfig` usa JWK e validadores de emissor e claims; `JwtWmsValidator` exige audiência, sujeito até 200 caracteres, início/fim válidos, duração de até 15 minutos, perfil GESTOR/SUPERVISOR/OPERACAO e listas de IDs positivos como strings, até 500 por cliente/armazém. Lista vazia não concede alcance a perfil limitado. A referência atual usa RS256, conforme [Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

GESTOR possui alcance geral definido no contrato; SUPERVISOR/OPERACAO exigem cliente e armazém simultaneamente nos serviços. Perfis do JWT não são permissões SQL individuais: a identidade técnica da aplicação é separada, e os serviços verificam o operador. Consulta de auditoria continua restrita ao Gestor. Não usar perfis de outro projeto nem criar usuários padrão.

Plano a validar com a equipe de identidade, sem escolher fornecedor:

1. Documentar emissor, audiência, origem das chaves públicas, certificado e mapeamento das contas reais para perfil/alcance.
2. Definir criação, alteração, desligamento, recuperação de conta e responsáveis pelas concessões; conferir acessos com o supervisor da operação.
3. Ensaiar assinatura válida/inválida, emissor/audiência incorretos, token expirado, perfil inválido, alcance insuficiente e tentativa direta ao serviço/API.
4. Ensaiar rotação de chave pública, chave desconhecida e indisponibilidade do endpoint, com erros seguros. Não inferir disponibilidade do provedor pela execução local.
5. Definir login, renovação e revogação no provedor. JWT já emitido pode conservar alcance até expirar; a duração máxima limita a janela, mas não comprova revogação imediata.

Não armazenar tokens de operadores como evidência. Registrar somente caso, resultado, código HTTP/código de erro, correlação e instante. Cookies não autenticam esta API; o modo JWT é sem sessão. Não abrir CORS ou exposição externa nesta preparação. Login/telas são trabalho separado, sem frontend nesta entrega.

## Diagnóstico em arquivos

[verificar-preparacao-local.ps1](verificar-preparacao-local.ps1) lê somente arquivos conhecidos do repositório, confere os controles declarados e hashes de V1–V5. Não lê ambiente/segredos, não acessa URL, não cria DataSource, não chama Java/Maven/Flyway e não escreve arquivos. Seu resultado é estático: não comprova disponibilidade, token real, TLS, permissões ou SQL Server.

Uma execução autorizada somente desse diagnóstico pode usar `./infra/verificar-preparacao-local.ps1`. Se a política local impedir scripts, registrar o bloqueio; não alterar política/confiança nem usar mecanismos de bypass. Não confundir essa leitura com `migrate.ps1`, que conecta ao servidor mesmo em consultas.

Diagnóstico futuro da aplicação: conferir artefato/perfil e `/api/v1/status` no ambiente autorizado; um status HTTP não comprova banco, identidade ou integridade do estoque. Correlacionar falhas por `X-Request-Id`, sem copiar payloads, XML, valores rejeitados, SQL com dados, cabeçalhos Bearer ou mensagens completas de driver. Logs detalhados e `help:effective-pom` não são meio de coleta de configuração.
