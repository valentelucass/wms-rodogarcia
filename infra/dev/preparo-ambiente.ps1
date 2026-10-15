# Executa o preparador em processo proprio; nunca propaga saida bruta de SQL/segredo.
function Start-WmsEnvironmentPreparation([string]$Repository,[ValidateSet('DEV','PROD')][string]$Ambiente){
    $helper=Join-Path $Repository 'database/scripts/preparar-ambiente.ps1'
    $hash=(Get-FileHash -LiteralPath $helper).Hash
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $info.Arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$helper+'" -Ambiente '+$Ambiente+' -Modo Apply'
    $info.WorkingDirectory=$Repository;$info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $p=[Diagnostics.Process]::new();$p.StartInfo=$info
    try{
        Write-Host ('[WMS '+$Ambiente+'] Outra preparacao pode ser aguardada por ate 180s, antes de acessar o banco.')
        if(-not $p.Start()){throw 'WMS_PREP_PROCESSO_NAO_INICIADO'}
        $stdout=$p.StandardOutput.ReadToEndAsync();$stderr=$p.StandardError.ReadToEndAsync()
        while(-not $p.WaitForExit(10000)){Write-Host ('[WMS '+$Ambiente+'] Preparando migrations e permissoes; instancia atual preservada.')}
        $p.WaitForExit();$null=$stderr.Result
        $r=$stdout.Result|ConvertFrom-Json
        if($hash -ine (Get-FileHash -LiteralPath $helper).Hash){throw 'WMS_PREP_FONTE_ALTERADA'}
        if($p.ExitCode -ne 0 -or $r.estado -cne 'AMBIENTE_ATUALIZADO'){
            if($r.codigo -cmatch '^(D20|D21|D22|CAD|SQL|PROD|DEV02|WMS_PREP)_[A-Z0-9_]+$'){
                Write-Host ('[WMS '+$Ambiente+'] Preparo interrompido: '+$r.codigo)
            }
            if($r.codigo -ceq 'D21_PREPARO_BANCO_EM_ANDAMENTO' -and $r.trava){
                $owner=if($r.trava.pidObservado){[string][long]$r.trava.pidObservado}else{'nao disponivel'}
                Write-Host ('[WMS '+$Ambiente+'] Limite de espera atingido. PID observado da outra preparacao: '+$owner+'. Nenhuma conexao SQL desta partida.')
            }
            if($r.recibo){Write-Host ('[WMS '+$Ambiente+'] Recibo do preparo: '+$r.recibo)}
            throw 'WMS_PREP_AMBIENTE_RECUSADO'
        }
        $r
    }finally{$p.Dispose()}
}
