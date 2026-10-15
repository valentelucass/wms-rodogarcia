[CmdletBinding()]
param([ValidateSet('DEV','PROD')][string]$Ambiente='DEV',
    [ValidateSet('Plan','Apply')][string]$Modo='Plan')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'iniciar-bancos.ps1')

function Invoke-WmsEnvironmentPreparation {
    param([ValidateSet('DEV','PROD')][string]$Ambiente,
        [ValidateSet('Plan','Apply')][string]$Modo='Plan',[hashtable]$Adaptadores)
    $real=$null -eq $Adaptadores
    $root=[IO.Path]::GetFullPath((Join-Path $WmsD21InicioRoot '..'))
    $targets=@(if($Ambiente -ceq 'DEV'){'WMS_DEV'}else{'WMS_DEV';'WMS_PROD'})
    $report=[ordered]@{natureza='WMS_PREPARO_AMBIENTE';ambiente=$Ambiente;modo=$Modo;
        inicioUtc=[DateTime]::UtcNow.ToString('o');estado='INTERROMPIDO';fase='PREFLIGHT_LOCAL';
        codigo=$null;bancos=$targets;bootstrap=$null;permissoes=@();fontes=@();recibo=$null;
        processosEncerrados=0;clean=$false;repair=$false;recapturaTLS=$false;trava=$null}
    if($real){
        $Adaptadores=@{
            Fontes={Get-WmsD21Fontes $WmsD21InicioRoot}
            Catalogo={param($Fontes)
                $fs=@($Fontes|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;texto=[IO.File]::ReadAllText((Join-Path $WmsD21InicioRoot $_.arquivo))}})
                $null=Get-WmsD21CatalogoEsperado $fs
            }
            Bootstrap={param($Bancos) Invoke-WmsD22InicioAutomatico -Bancos $Bancos -SomenteExistentes 6>$null}
            Permissoes={param($Banco,$Acao)
                $suffix=if($Banco -ceq 'WMS_DEV'){'dev'}else{'prod'}
                $helper=Join-Path $root ('infra/cadastros/aplicar-permissoes-'+$suffix+'.ps1')
                $info=[Diagnostics.ProcessStartInfo]::new()
                $info.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
                $info.Arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$helper+'" -Modo '+$Acao
                $info.WorkingDirectory=$root;$info.UseShellExecute=$false;$info.CreateNoWindow=$true
                $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
                $p=[Diagnostics.Process]::new();$p.StartInfo=$info
                try{
                    if(-not $p.Start()){throw 'WMS_PREP_PERMISSOES_NAO_INICIADAS'}
                    $out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync()
                    $p.WaitForExit();$null=$err.Result
                    $result=$out.Result|ConvertFrom-Json
                    if($p.ExitCode -ne 0 -or $result.estado -ceq 'INTERROMPIDO'){
                        if($result.erro.codigo -cmatch '^(CAD|SQL|PROD|DEV02)_[A-Z0-9_]+$'){throw $result.erro.codigo}
                        throw 'WMS_PREP_PERMISSOES_RECUSADAS'
                    }
                    $result
                }finally{$p.Dispose()}
            }
        }
    }
    $lease=$null
    try{
        if($real -and $Modo -ceq 'Apply'){
            $lease=Enter-WmsDatabasePreparationLock
            $report.trava=Get-WmsDatabasePreparationLockReport $lease
            if(-not $lease.acquired){throw 'D21_PREPARO_BANCO_EM_ANDAMENTO'}
        }
        $report.fontes=@(& $Adaptadores.Fontes)
        if(-not $report.fontes.Count){throw 'D21_MIGRATIONS_AUSENTES'}
        & $Adaptadores.Catalogo $report.fontes
        # Os planos validam tambem os hashes/contratos revisados antes de qualquer SQL.
        foreach($banco in $targets){
            $plan=& $Adaptadores.Permissoes $banco 'Plan'
            if($plan.estado -cne 'PLANO_LOCAL_VALIDADO'){throw 'WMS_PREP_PLANO_PERMISSOES_RECUSADO'}
        }
        Assert-WmsD21FontesIguais $report.fontes @(& $Adaptadores.Fontes)
        if($Modo -ceq 'Apply'){
            $report.fase='MIGRATIONS'
            $report.bootstrap=& $Adaptadores.Bootstrap $targets
            if($report.bootstrap.resultado -cnotin @('DEV_BOOTSTRAP_UPGRADE_CONCLUIDO','DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO')){
                if($report.bootstrap.codigo -cmatch '^(D20|D21|D22)_[A-Z0-9_]+$'){throw $report.bootstrap.codigo}
                throw 'WMS_PREP_MIGRATIONS_RECUSADAS'
            }
            Assert-WmsD21FontesIguais $report.fontes @(& $Adaptadores.Fontes)
            $report.fase='PERMISSOES'
            foreach($banco in $targets){
                $r=& $Adaptadores.Permissoes $banco 'Apply'
                $expected=if($banco -ceq 'WMS_DEV'){'DIREITOS_DEV_APLICADOS_E_CONFERIDOS'}else{'DIREITOS_PROD_APLICADOS_E_CONFERIDOS'}
                if($r.estado -cne $expected){throw 'WMS_PREP_PERMISSOES_NAO_CONFIRMADAS'}
                $report.permissoes+=@{banco=$banco;estado=$r.estado;recibo=$r.recibo;grantsAplicados=$r.grantsAplicados}
                Assert-WmsD21FontesIguais $report.fontes @(& $Adaptadores.Fontes)
            }
        }
        $report.fase='CONCLUIDO';$report.estado=if($Modo -ceq 'Plan'){'PLANO_SEM_SQL'}else{'AMBIENTE_ATUALIZADO'}
    }catch{
        $e=$_.Exception;while($e.InnerException){$e=$e.InnerException}
        $report.codigo=if($e.Message -cmatch '^(D20|D21|D22|CAD|SQL|PROD|DEV02|WMS_PREP)_[A-Z0-9_]+$'){$e.Message}else{'WMS_PREP_FALHA_CONTROLADA'}
    }finally{
        Exit-WmsDatabasePreparationLock $lease
        $report['fimUtc']=[DateTime]::UtcNow.ToString('o')
    }
    [pscustomobject]$report
}

if($MyInvocation.InvocationName -ne '.'){
    $r=Invoke-WmsEnvironmentPreparation -Ambiente $Ambiente -Modo $Modo
    $area=Join-Path $WmsD21InicioRoot ('../orchestracao/.runtime/preparo-ambiente/'+[guid]::NewGuid().ToString('N'))
    $null=New-Item -ItemType Directory -Path $area
    $r.recibo=[IO.Path]::GetFullPath((Join-Path $area 'resultado.json'))
    $json=$r|ConvertTo-Json -Depth 15
    [IO.File]::WriteAllText($r.recibo,$json,[Text.UTF8Encoding]::new($false))
    [pscustomobject]@{estado=$r.estado;ambiente=$r.ambiente;codigo=$r.codigo;recibo=$r.recibo;trava=$r.trava}|ConvertTo-Json
    if($r.estado -ceq 'INTERROMPIDO'){exit 20}
}
