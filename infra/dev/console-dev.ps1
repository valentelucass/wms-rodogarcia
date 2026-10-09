# Apresentacao somente. Nao inicia processos, nao conecta e nao le configuracoes.
function Initialize-WmsDevConsole {
    $script:WmsDevConsoleWatch=[Diagnostics.Stopwatch]::StartNew()
    $script:WmsDevConsoleStage='Preparacao'
    $script:WmsDevConsoleStageStarted=0.0
    Write-Host ''
    Write-Host ('  '+('='*64)) -ForegroundColor Cyan
    Write-Host '    WMS RODOGARCIA                         DESENVOLVIMENTO' -ForegroundColor Cyan
    Write-Host ('  '+('='*64)) -ForegroundColor Cyan
    Write-Host '    Ambiente local  |  Banco WMS_DEV  |  Dados reais de DEV' -ForegroundColor DarkGray
    Write-Host ''
}

function Write-WmsDevConsoleStep([int]$Number,[string]$Message,[switch]$Done) {
    $time=Get-Date -Format 'HH:mm:ss'
    if($Done){
        $elapsed=$script:WmsDevConsoleWatch.Elapsed.TotalSeconds-$script:WmsDevConsoleStageStarted
        Write-Host ('  {0}  [ OK ]  {1}  ({2:N1}s)' -f $time,$Message,$elapsed) -ForegroundColor Green
    } else {
        $script:WmsDevConsoleStage=$Message
        $script:WmsDevConsoleStageStarted=$script:WmsDevConsoleWatch.Elapsed.TotalSeconds
        Write-Host ('  {0}  [{1}/4]  {2}' -f $time,$Number,$Message) -ForegroundColor Cyan
    }
}

function Write-WmsDevConsoleReady([string]$FrontendUrl,[int]$BackendPort,[string]$ReceiptPath,[switch]$TunnelMode,[int]$FrontendPort) {
    Write-Host ''
    Write-Host ('  '+('='*64)) -ForegroundColor Green
    Write-Host '    SISTEMA PRONTO PARA ACESSAR' -ForegroundColor Green
    Write-Host ('  '+('='*64)) -ForegroundColor Green
    Write-Host ''
    Write-Host '    ABRA NO NAVEGADOR' -ForegroundColor White
    if($TunnelMode) {
        Write-Host ('    Use o link HTTPS atual da porta '+$FrontendPort+' no painel Portas do VS Code.') -ForegroundColor Cyan
        Write-Host '    O link pode mudar; nao e necessario editar a configuracao.' -ForegroundColor DarkGray
    } else {
        Write-Host ('    '+$FrontendUrl) -ForegroundColor Cyan
    }
    Write-Host ''
    Write-Host '    Frontend   ONLINE' -ForegroundColor Green
    Write-Host ('    Backend    ONLINE  |  porta '+$BackendPort) -ForegroundColor Green
    Write-Host '    Banco      WMS_DEV |  conexao segura verificada' -ForegroundColor Green
    Write-Host ('    Inicio     {0:N1} segundos' -f $script:WmsDevConsoleWatch.Elapsed.TotalSeconds) -ForegroundColor DarkGray
    Write-Host ''
    Write-Host '    Entre com seu e-mail e sua senha na tela de login.'
    Write-Host '    Mantenha esta janela aberta enquanto usa o sistema.'
    Write-Host '    Ctrl+C encerra esta instancia de desenvolvimento.' -ForegroundColor Yellow
    Write-Host ''
    Write-Host '    Registro desta execucao:' -ForegroundColor DarkGray
    Write-Host ('    '+$ReceiptPath) -ForegroundColor DarkGray
    Write-Host ''
}

function Write-WmsDevConsoleFailure([string]$Reason,[string]$ReceiptPath) {
    $message=switch -Regex ($Reason) {
        '^PORT_OCCUPIED_' { 'A porta ja esta em uso. Confira se o WMS ja esta aberto em outro console.';break }
        '^AUTH_NATIVE_CONFIGURATION_REQUIRED$' { 'Falta a configuracao inicial do login. Use infra\auth\configurar-login-dev.bat.';break }
        '^AUTH_' { 'A configuracao de login precisa ser conferida.';break }
        '^GUARD_' { 'A verificacao do banco nao foi aprovada. Confira a orientacao acima.';break }
        '^BACKEND_NOT_READY$' { 'O backend nao ficou disponivel. O registro abaixo contem o diagnostico.';break }
        '^FRONTEND_' { 'A interface nao ficou disponivel. Confira as dependencias e o registro abaixo.';break }
        '^PREPARATION_ARTIFACT_MISSING$' { 'Faltam arquivos preparados para iniciar o sistema.';break }
        default { 'A inicializacao foi interrompida. Consulte o registro abaixo.' }
    }
    Write-Host ''
    Write-Host ('  '+('='*64)) -ForegroundColor Red
    Write-Host '    NAO FOI POSSIVEL INICIAR O WMS' -ForegroundColor Red
    Write-Host ('  '+('='*64)) -ForegroundColor Red
    Write-Host ('    Etapa: '+$script:WmsDevConsoleStage) -ForegroundColor Yellow
    Write-Host ('    '+$message) -ForegroundColor Yellow
    Write-Host ('    Codigo: '+$Reason) -ForegroundColor DarkGray
    Write-Host '    Registro para diagnostico:' -ForegroundColor DarkGray
    Write-Host ('    '+$ReceiptPath) -ForegroundColor DarkGray
    Write-Host ''
}
