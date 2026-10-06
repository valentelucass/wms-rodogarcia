# Banco WMS — D21 manual

Duplo clique em **`database/iniciar-bancos.bat`**, ou CMD na raiz:

```bat
database\iniciar-bancos.bat
database\iniciar-bancos.bat --offline
```

Alvo fixo **TCP127.0.0.1:1433, sa/master**. Senha oculta, somente memória; inspeção TLS de master mostra ServerName e bancos existentes. Digite o endpoint no console para confirmar **bootstrap/upgrade de WMS_DEV e WMS_PROD**.

**DEV completo primeiro:** criar ausente ou conferir existente, Flyway validate → migrate pendentes → validate estrito → info → confrontar catálogo real com a expectativa das migrations. Só então repetir em PROD. Qualquer erro/checksum/queda/metadado/catalogo divergente em DEV bloqueia PROD. Existentes não são recriados; dados/histórico permanecem. Reexecutar após corrigir a causa: Flyway determina pendentes, incluindo futuras V10+. Sem clean/repair/baseline automático, DROP, grants ou configurações globais.

Requisitos: Windows PowerShell/política existente, JDK 21 e distribuição/dependências Maven já disponíveis em cache (execução `-o`, sem instalação/download), perfil backend `bootstrap-local` integrado; credencial sa autorizada e certificados compatíveis com 127.0.0.1 já confiáveis no SqlClient/Windows **e JDBC/JDK**. TLS exige encrypt=true/trustServerCertificate=false; nenhuma validação real é alegada pelo modo offline, nenhum pin/perfil privado/trust de referência é herdado.

Senha vai somente no ambiente do filho efêmero, nunca argumento/arquivo/log/ambiente pai/User/Machine. Saída bruta Maven stdout/stderr é descartada; resultado/fase/código sanitizados ficam em `evidencias/d21-manual-*.json`. Console permanece aberto ao terminar. Exit 0 sucesso/offline; 1 falha; 2 argumento inválido/confirmação recusada. `--offline` não lê segredo, ambiente de conexão nem conecta.

Parser estrutural acompanha a fonte canônica; sintaxe não suportada interrompe antes de conectar. A comparação cobre tabelas/colunas/tipos/nulos/identity/chaves/FKs/CHECKs/defaults/índices, não drift universal, permissões ou propriedades físicas. Nenhuma carga fictícia é feita. Conta sa serve ao bootstrap administrativo; identidade do backend continua distinta/restrita.

[Contrato Flyway](docs/d21-contrato-flyway.md), [resultado/testes/freeze](evidencias/d21-relatorio-final.md). Fontes SQL únicas em migrations/, PS1 em scripts/, XML em config/. [Snapshot D20](evidencias/d21-snapshot-d20.json) e manifestos anteriores preservados; os limites D20 de PROD vazio e migration DEV separada são históricos, supersedidos por D21. Nenhum SQL pelo agente.