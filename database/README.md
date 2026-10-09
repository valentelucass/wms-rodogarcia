# Bancos WMS — conexão compartilhada

**D27: V10 aplicada somente WMS_DEV. Não usar iniciar-bancos.bat como preparação/continuação D27:** esse launcher percorre DEV→PROD. A autorização atual não inclui aplicar V10 em PROD. [Executor e resultado DEV](docs/d27-migration-dev.md).

WMS_PROD: última evidência real D24, V1–V9. Não conectar ou atualizar PROD nesta D27. [Migration README vigente](migrations/README.md).

Plan offline do executor específico, a partir da raiz:

```powershell
powershell.exe -NoProfile -NonInteractive -File database/scripts/d27-migrate-dev.ps1
```

O procedimento e os resultados D24 abaixo são históricos; PROD não foi conectado na D27. O executor usa o padrão de [conexão dos projetos](../../.runtime/sql-server/README.md), configurado para o usuário Windows `suporte`. Comando normal de bootstrap, somente em escopo próprio autorizado:

```bat
database\iniciar-bancos.bat
```

**Validado em 06/10/2026:** execução real e reexecução concluídas pelo BAT. WMS_DEV e WMS_PROD ONLINE, cada um com nove migrations, 64 tabelas e 687 colunas; histórico e catálogo conferidos. [Resultado D24](../docs/39-conexao-compartilhada-e-sql-real.md).

O modo normal não pede senha nem confirmação repetida. Obtém a credencial administrativa DPAPI de `%LOCALAPPDATA%\Rodogarcia\SqlServer\admin.clixml`, confere a instância local e usa TLS com confiança privada. A conexão salva no VS Code não é uma dependência do launcher.

**Alvo fixo:** SQL Server 2022 Standard 16.0.1000.6, `127.0.0.1:1433`, SQL auth `sa`, bancos exclusivos `WMS_DEV` e `WMS_PROD`. Nenhuma versão/edição/configuração do servidor é alterada. O sa serve ao bootstrap; o backend precisa de identidade própria com permissões limitadas.

**DEV completo antes de PROD:** criar somente se ausente; conferir e preservar existente; validar histórico/checksums; aplicar apenas migrations pendentes; validar novamente; confrontar catálogo real com as fontes. Falha DEV bloqueia PROD. Sem DROP, clean, repair, baseline automático, grants ou cargas de teste. As migrations V1–V9 permanecem intactas; a descoberta continua aceitando futuras versões.

PowerShell usa o Microsoft.Data.SqlClient já instalado no SSMS, com correspondência exata de certificado. Java/Flyway usa o truststore privado do runtime, com `encrypt=true` e `trustServerCertificate=false`. O JDK 21 é selecionado somente no processo do launcher; JAVA_HOME global permanece intacto. O BAT também seleciona apenas para seus filhos os módulos do Windows PowerShell 5.1.

O Maven continua offline. Em 06/10/2026 foi preparado o cache do plugin Flyway 12.4.0 e de suas dependências já declaradas no POM; a execução normal não baixa nem instala ferramentas. Não foi alterada a versão do projeto, JDBC declarado, Flyway declarado ou SQL Server.

Diagnóstico sem SQL:

```bat
database\iniciar-bancos.bat --offline
database\iniciar-bancos.bat --fixture
```

`--offline` descreve o fluxo sem ler segredo/conectar; `--fixture` usa credenciais fictícias e adaptadores isolados. Esses modos não comprovam schema real. O teste da fonte compartilhada é `..\.runtime\sql-server\testar-conexao.bat`.

A configuração inicial já está feita. `database\configurar-credencial.bat` permanece como auxiliar explícito para uma troca autorizada da cópia administrativa compartilhada: entrada local oculta, DPAPI, sem mudar a senha no SQL. Como a fonte é comum, a atualização atende também aos demais executores que a adotarem. Não usar esse auxiliar como etapa obrigatória antes de cada execução.

Resultados reais ficam em `evidencias/d22-automatico-*.json`, com fase/código e sem saída bruta de credenciais/Maven. Exit code: 0 concluído; 1 falha; 2 argumento inválido. No duplo clique pelo Explorer, o console existente mantém o resultado visível.

O estado D24 em `../STATES.md` e suas evidências registram o resultado operacional atual. Documentos D20–D23 e manifestos anteriores permanecem históricos; suas afirmações de credencial exclusiva ausente não descrevem o launcher integrado.
