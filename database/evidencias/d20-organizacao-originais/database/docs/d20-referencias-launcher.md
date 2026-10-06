# D20 — referências públicas do launcher manual

Lucas autorizou somente estes seis arquivos para organização. Lidos em06/10; SHA/bytes em [evidência](evidencias/d20-fontes-launcher.json). Não foram lidos config/.env/credenciais/histórico/ponte/rotinas dos projetos, nem executado qualquer comando desses runners.

| Fonte exata | Padrão aproveitado no WMS |
| --- | --- |
| `../satelite-tms-api/database/README.md` | Comando e requisitos antes de detalhes da estrutura. |
| `../satelite-tms-api/database/subir_database.bat` | Caminho `%~dp0`, allowlist e parada por falha. |
| `../dashboards-etl/database/README.md` | Separação entre criação/preparo, migrations e verificação. |
| `../dashboards-etl/database/executar_database.bat` | Modos explícitos, raiz relativa, códigos de saída. |
| `../avaliacao-desempenho-competencias/database/README.md` | Guia direto, repetição preservando existente/histórico. |
| `../avaliacao-desempenho-competencias/database/executar-database.bat` | DEV antes PROD e falha DEV interrompe a sequência. |

Diferenças justificadas: BAT WMS apenas chama um orquestrador PS que reutiliza os primitivos; não replica SQL nem importa a complexidade do runner Avaliação. Endpoint/names exclusivos fixos da autorização WMS, sem opção de banco customizado ou nomes/acessos copiados. Configuração de migration continua externa e separada; CREATE administrativo usa prompt oculto SecureString em memória, sem importar config/.env/arquivo de credencial de referência. TLS encrypt=true/trustServerCertificate=false, sem -C, bypass de execução, instalação ou mudança SQL/global. Compatibilidade/cadeia/certificado para127.0.0.1 dependem da confiança **já existente**: falha encerra e encaminha, não altera confiança/certificados.

A referência Avaliação README110–114 descreve pin público via sqlcmd -J e perfil privado. Isso não comprova cadeia OS/SqlClient do WMS; não foi lido/importado seu perfil, certificado, recibo ou credencial. Compatibilidade TLS real só na execução manual, sem bypass/installs/config compartilhada. Duplo clique mantém console e resultado. `--offline` é verificável e não chama prompt/segredo/conector; argumentos inválidos preservam console e exit2. Novos recebem apenas CREATE vazio; existentes ONLINE com metadados completos preservam schema/dados, mesmo não vazios. Não há DROP/recriação/GRANT/migration/carga no launcher. Migration V1–V9 tem entrada manual distinta, somente DEV, sem sa; PROD jamais migrado automaticamente.
