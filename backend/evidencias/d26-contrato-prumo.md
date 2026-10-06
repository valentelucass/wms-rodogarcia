# D26 — contrato pequeno Cedro/Prumo

Aguardando `orchestracao/.runtime/d26-prumo-pronto.json`, sem conexão antes de atestação. Preciso do nome/caminho público da API PowerShell que retorna PSCredential WMSDEV por DPAPI, sem parâmetro de senha/configurador, e do comando protegido de leitura/precheck pela própria identidade. Não enviarei nem gravarei segredo. Consumo de Get-ProjetosSqlProfile somente metadados/confiança privada do cliente; nunca Get-ProjetosSqlAdminCredential no runner.

O sinal deve vincular WMS_DEV, WMSDEV, 127.0.0.1:1433, ROD-SRVW-001, TLS privado e atestação efetiva por objeto/coluna/recusas, sem DDL/DELETE/roles administrativas/PROD. Informar contagens de vazio/histórico e acesso ao catálogo necessário para Hibernate validate e IT.

Primeiro farei sete ITs se banco vazio e precondições válidas. A fixture de cliente do lock será confirmada e preservada. Depois popularei via HTTP. Antes de cada fase publicarei/conferirei alvo pela identidade própria. `backend/evidencias/d26-persistencia-pedido.json` será atualizado com rodada/fase/IDs gerados e relações/expectativas; Prumo poderá publicar SELECTs sanitizados por IDs em sua área. Não ler segredo de `.clixml` diretamente fora da API autorizada, nem arquivo privado/config alheia. Matriz/casos/GETs evidenciarão separadamente o que foi ou não comprovado no SQL físico/financeiro/documental/auditoria.
