# Prazos do cliente WMS DEV. Nao conecta, nao inicia processos e nao le segredos.
function Get-WmsDevConnectionWaitPolicy {
    [pscustomobject]@{
        ConnectTimeoutSeconds = 120
        CommandTimeoutSeconds = 30
        GuardTimeoutSeconds = 240
        ProgressIntervalSeconds = 10
    }
}

function Get-WmsDevGuardFailureMessage($Guard, $Policy) {
    $refused='A verificacao de seguranca ou estrutura do WMS_DEV nao foi aprovada. Consulte o recibo da guarda.'
    if (!$Guard -or !$Guard.PSObject.Properties['erro'] -or !$Guard.erro -or
        !$Guard.erro.PSObject.Properties['marcadores'] -or !$Guard.erro.marcadores) { return $refused }
    $markers=$Guard.erro.marcadores
    if ($markers.PSObject.Properties['timeout'] -and $markers.timeout -eq $true) {
        if ($Guard.PSObject.Properties['aberturaConcluida'] -and $Guard.aberturaConcluida -eq $true) {
            $commandTimeout=$Policy.CommandTimeoutSeconds
            if ($Guard.PSObject.Properties['politica'] -and $Guard.politica -and
                $Guard.politica.PSObject.Properties['commandTimeout'] -and
                $Guard.politica.commandTimeout -is [ValueType] -and $Guard.politica.commandTimeout -gt 0 -and
                $Guard.politica.commandTimeout -le $Policy.GuardTimeoutSeconds) { $commandTimeout=$Guard.politica.commandTimeout }
            $phase=if($Guard.erro.PSObject.Properties['fase']){[string]$Guard.erro.fase}else{''}
            $query=switch($phase){
                'IDENTIDADE' {'identificacao do WMS_DEV'}
                'PERMISSOES' {'permissoes do WMS_DEV'}
                'CATALOGO_HISTORICO' {'estrutura e historico do WMS_DEV'}
                default {'verificacao do WMS_DEV'}
            }
            return "A conexao SQL foi aberta, mas a consulta de $query falhou por timeout (limite de ${commandTimeout}s). O banco nao foi liberado para uso."
        }
        return ('A abertura da conexao com o SQL Server falhou por timeout (limite de '+$Policy.ConnectTimeoutSeconds+'s). O banco nao foi liberado para uso.')
    }
    if ($markers.PSObject.Properties['certificado'] -and $markers.certificado -eq $true) {
        return 'Nao foi possivel validar o certificado da conexao com o SQL Server.'
    }
    if ($markers.PSObject.Properties['autenticacao'] -and $markers.autenticacao -eq $true) {
        return 'O SQL Server recusou o acesso da conta tecnica WMSDEV.'
    }
    return $refused
}

function Wait-WmsDevGuardExit {
    param(
        [Parameter(Mandatory = $true)][Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)][TimeSpan]$Timeout,
        [Parameter(Mandatory = $true)][TimeSpan]$ProgressInterval,
        [scriptblock]$OnProgress
    )
    if ($Timeout.TotalMilliseconds -le 0 -or $Timeout.TotalMilliseconds -gt [int]::MaxValue -or
        $ProgressInterval.TotalMilliseconds -le 0) { throw 'DEV_WAIT_INTERVAL_INVALID' }

    $watch = [Diagnostics.Stopwatch]::StartNew()
    try {
        while ($true) {
            $remaining = $Timeout.TotalMilliseconds - $watch.Elapsed.TotalMilliseconds
            if ($remaining -le 0) { return $Process.HasExited }
            $slice = [int][Math]::Ceiling([Math]::Min($remaining, $ProgressInterval.TotalMilliseconds))
            if ($Process.WaitForExit($slice)) { return $true }
            if ($watch.Elapsed -ge $Timeout) { return $false }
            if ($OnProgress) { $null = & $OnProgress $watch.Elapsed.TotalSeconds $Timeout.TotalSeconds }
        }
    } finally {
        $watch.Stop()
    }
}
