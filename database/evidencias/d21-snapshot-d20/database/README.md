# Banco de dados do WMS Rodogarcia

## Uso manual D20 — bancos locais

Duplo clique em **`database/iniciar-bancos.bat`**, ou na raiz pelo CMD:

```bat
database\iniciar-bancos.bat
database\iniciar-bancos.bat --offline
```

Alvo exclusivo **TCP127.0.0.1:1433, sa/master**, bancos exatos **WMS_DEV → WMS_PROD**. Senha oculta no próprio console, somente memória; inspeção master em leitura mostra ServerName/estado e pede confirmação do endpoint. Existentes ONLINE com metadados completos preservam schema/dados; DEV existente permite retomar PROD faltante. Novos exigem model sem conteúdo/acessos inesperados e confirmação de vazio. Falha DEV impede PROD; nenhuma sobrescrita/limpeza. Console fica aberto ao terminar.

Requisitos: Windows PowerShell existente/política local permitindo o script, credencial sa autorizada no operador e TLS/certificado compatível com 127.0.0.1 já confiável no SqlClient/Windows. O pin público sqlcmd -J da referência Avaliação não prova confiança da cadeia OS deste provider; WMS não herda seu perfil privado. Compatibilidade real só na execução manual. Sem instalação, bypass, mudança SQL/global ou senha em arquivo/env persistente/log. Erros de identidade/estado/metadados/TLS preservam bancos e explicam a fase; evidência sanitizada em `evidencias/d20-manual-*.json`. Exit 0 sucesso/offline; exit 1 falha; exit 2 confirmação recusada/argumento inválido. `--offline` não lê segredo nem conecta.

**Migration é outra ação manual, exclusivamente WMS_DEV**, após contas app/migration distintas/restritas e configuração externa protegida própria (nunca sa). O BAT cria/confere bancos; migrations e cargas são separadas. Entrada na raiz:

```powershell
powershell.exe -NoProfile -File database/scripts/migrate.ps1 -Action Plan
# Somente com contexto DEV/credenciais protegido e alvo confirmado:
powershell.exe -NoProfile -File database/scripts/migrate.ps1 -Action Migrate
```

WMS_PROD não recebe migrations/cargas. [Procedimento](docs/d20-procedimentos.md), [fontes seis/diferenças](docs/d20-referencias-launcher.md), [testes/freeze](evidencias/d20-relatorio-reorganizacao.md). Agente executou somente testes offline/fixtures, sem SQL. Histórico integral preservado em [README anterior](docs/README-historico-d20.md); [de-para e hashes](docs/d20-organizacao.md). Estrutura: scripts/ para PS1, docs/ técnicos, config/ XML; SQL/migrations/contratos/evidencias preservados.
