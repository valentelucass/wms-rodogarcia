# D24 — conexão compartilhada e bancos WMS reais

Em 06/10/2026, o responsável autorizou centralizar a conexão em `projetos/.runtime` para facilitar novos projetos e executar o BAT WMS ao final. A autorização substitui a exigência anterior de fonte administrativa exclusiva WMS. A versão e a edição do SQL Server permanecem imutáveis.

## Resultado comprovado

O `database/iniciar-bancos.bat` concluiu a execução real DEV → PROD, com exit code 0. Ambos os bancos estão ONLINE, compatibilidade 160, com 64 tabelas de negócio, 687 colunas e as nove migrations SQL V1–V9 aplicadas sem falhas. O histórico Flyway passou no validate e cada catálogo real foi confrontado com os 1.295 itens derivados das fontes. A tabela técnica de histórico não entra nas 64 tabelas.

SQL Server continua **Standard Edition (64-bit), 16.0.1000.6**, no mesmo processo iniciado em 05/10/2026. Não houve reinício, atualização, mudança de edição, configuração global, collation, grants ou intervenção nos dados dos outros projetos. Nomes, estado e compatibilidade dos onze bancos anteriores foram comparados em leitura e permanecem iguais. Windows Server 2022 Standard e licenciamento não foram alterados; a conferência de edição não constitui auditoria de licenças.

Evidências: [leitura autenticada final](../database/evidencias/d24-resultado-real.json) e [execução completa pelo BAT](../database/evidencias/d22-automatico-905ef13acee141659ef35dfe3a893e66.json). Os nove arquivos de migration conservaram exatamente seus hashes anteriores.

A [segunda execução completa](../database/evidencias/d22-automatico-3e16257f12cf4beb991aad59df057900.json) também terminou com exit code 0: ambos foram tratados como existentes (`Check`), sem nova criação. A leitura antes/depois confirmou o mesmo histórico (versões, scripts, checksums e sucesso) e as mesmas contagens estruturais.

## Conexão e correções

- O runtime compartilhado fornece configuração pública, credencial administrativa DPAPI por usuário/máquina e confiança TLS privada. A fonte protegida fica em `%LOCALAPPDATA%/Rodogarcia/SqlServer`, fora dos repositórios, acessível somente ao usuário e SYSTEM. A credencial existente do Database Client foi usada para o provisionamento autorizado; o launcher não depende do VS Code em funcionamento.
- A autenticação administrativa é SQL (`sa`). A tentativa Windows `ROD-SRVW-001\suporte` era recusada. Nenhuma permissão desse login Windows foi alterada.
- PowerShell usa o SqlClient já instalado no SSMS, com certificado exato. JDBC/Flyway usa truststore privado, criptografia e validação de certificado. Não foi habilitado bypass TLS nem alterado o armazenamento global de confiança.
- O BAT seleciona Windows PowerShell 5.1 e o executor seleciona o JDK 21 somente para seus processos. As dependências do SqlClient recebem resolução de assemblies local ao processo, conforme as versões instaladas no SSMS.
- O cache Maven recebeu o Flyway 12.4.0 e dependências já declaradas. A operação normal continua offline. A opção de arquivo de configuração Flyway agora aponta para um arquivo de zero bytes conferido, evitando interpretar o diretório backend como arquivo de configuração.
- A inspeção de banco vazio passou a reconhecer somente a associação padrão `dbo` → `db_owner`; outras associações continuam bloqueadas. O modelo do SQL Server não foi alterado.
- O comparador do catálogo reconhece `IN` unitário e a transformação equivalente de `NOT IN` em `NOT (... OR ...)` feita pelo SQL Server. A normalização preserva a lógica de NULL e continua detectando remoção de termos, troca de operadores e alterações de literais. Nenhuma constraint ou migration foi modificada para contornar essa comparação.

As tentativas intermediárias foram interrompidas pelas guardas. Primeiro foi criado DEV; falhas de ferramenta/comparação impediram PROD. A retomada conferiu e preservou DEV antes de criar e migrar PROD. Não houve clean, repair, baseline, recriação ou limpeza dos bancos.

## Verificações e limites

Passaram as leituras reais pelos clientes .NET e JDBC, os 18 casos D22 com credenciais fictícias e os nove casos específicos D24 do comparador, incluindo diferenças que devem continuar bloqueadas. O teste D22 foi atualizado para permitir as alterações D24 nos adaptadores, mantendo a conferência das cópias históricas, manifestos e migrations. A suíte antiga D21 usa uma função manual removida na evolução D22; sua execução exploratória não passou e não é apresentada como validação do launcher atual. Os resultados históricos anteriores permanecem identificados como históricos.

A análise sintática dos 45 scripts PowerShell atuais não encontrou erros; `git diff --check` passou. Não foi repetida a suíte integral do backend para esta mudança de conexão e launcher.

Esta entrega valida conexão administrativa, criação/migrations e catálogo real. BE03/BE15 continuam com pendências de identidade própria da aplicação, teste integrado de operações/concorrência, backup/restauração e homologação. O backend, frontend, equipamentos e processos de negócio não foram iniciados ou homologados por este trabalho. `sa` permanece restrito ao executor administrativo; não deve ser usado pela aplicação.

Novos projetos devem seguir o [guia compartilhado](../../.runtime/sql-server/README.md). Para o WMS, a configuração já está pronta: executar [iniciar-bancos.bat](../database/iniciar-bancos.bat) quando houver migrations autorizadas, seguindo o [guia do operador](../database/README.md). Não repetir provisionamento de credencial a cada execução.
