# Testes isolados: listeners, CIM e handles simulados; nenhum servico ou SQL real.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'reiniciar-instancia.ps1')
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$run = Join-Path $project ('orchestracao/.runtime/reinicio-dev/' + [guid]::NewGuid().ToString('N'))
$repo = Join-Path $run 'projeto ficticio'
$runs = Join-Path $repo 'orchestracao/.runtime/frontend-integracao-dev-runs'
$fixture = Join-Path $runs 'anterior/launcher.json'
$null = New-Item -ItemType Directory -Path (Split-Path $fixture) -Force
$cases = [Collections.Generic.List[object]]::new()

function Get-WmsDevListeners { $script:listeners }
function Get-CimInstance($ClassName, $Filter, $ErrorAction) {
    $id = [int]($Filter -replace '^ProcessId=','')
    $script:metadata[$id]
}
function Get-Process($Id, $ErrorAction) { $script:processes[[int]$Id] }
function Save-Fixture {
    [IO.File]::WriteAllText($fixture, ($script:receipt | ConvertTo-Json -Depth 12), [Text.UTF8Encoding]::new($false))
}
function New-Scenario {
    $script:killed = @(); $script:stopped = @()
    $script:processes = @{}; $script:metadata = @{}; $script:listeners = @()
    foreach ($port in @(25580,25581)) {
        $id = $port + 10000
        $process = [pscustomobject]@{Id=$id;StartTime=[DateTime]::UtcNow;HasExited=$false;Handle=1;Disposed=$false}
        $process | Add-Member ScriptMethod Kill {
            $script:killed += $this.Id
            $this.HasExited = $true
            $script:listeners = @($script:listeners | Where-Object { $_.OwningProcess -ne $this.Id })
        }
        $process | Add-Member ScriptMethod WaitForExit { param($timeout) $this.HasExited }
        $process | Add-Member ScriptMethod Dispose { $this.Disposed=$true }
        $script:processes[$id] = $process
        $script:listeners += [pscustomobject]@{LocalPort=$port;LocalAddress='127.0.0.1';OwningProcess=$id}
    }
    $java='C:\Ferramentas\jdk 21\bin\java.exe'; $node='C:\Ferramentas\node.exe'
    $jar=Join-Path $repo 'backend/target/wms.jar'
    $config=Join-Path $repo 'backend/src/main/resources/application-frontend-dev.properties'
    # Mesmo formato do ProcessStartInfo de start-backend.ps1, com caminhos contendo espacos.
    $script:metadata[35580]=[pscustomobject]@{ExecutablePath=$java;CommandLine=('"'+$java+'" -jar "'+$jar+'" --spring.profiles.active=sqlserver-dev '+
        '--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties '+
        '--spring.config.additional-location="'+([Uri]$config).AbsoluteUri+'" '+
        '--server.address=127.0.0.1 --server.port=25580 --wms.cadastros.enabled=true '+
        '--spring.autoconfigure.exclude= --spring.jpa.hibernate.ddl-auto=validate --spring.jpa.generate-ddl=false '+
        '--spring.sql.init.mode=never --spring.flyway.enabled=false --spring.liquibase.enabled=false '+
        '--wms.database.name=WMS_DEV --wms.database.user=WMSDEV')}
    $script:metadata[35581]=[pscustomobject]@{ExecutablePath=$node;CommandLine=('"'+$node+'" "'+(Join-Path $repo 'frontend/node_modules/vite/bin/vite.js')+'" --config vite.dev.config.ts')}
    $script:receipt=@{
        projectPath=$repo;natureza='D31_DEV02_LAUNCHER_REAL';modo='real';database='WMS_DEV';login='WMSDEV';backendPort=25580;frontendPort=25581
        backend=@{natureza='D31_DEV02_BACKEND_READINESS';profile='sqlserver-dev';database='WMS_DEV';login='WMSDEV';java=@{path=$java};artifact=@{path=$jar}
            backend=@{pid=35580;startUtc=$script:processes[35580].StartTime.ToString('o');port=25580;listenerOwned=$true}}
        frontend=@{pid=35581;startUtc=$script:processes[35581].StartTime.ToString('o');mode='real';listenerOwned=$true}
    }
    Save-Fixture
}
function Plan { @(Get-WmsDevRestartPlan $repo 25580 25581) }
function Stop-Plan($Targets) { Stop-WmsDevRestartPlan $Targets $repo {param($item) $script:stopped += $item} }
function Expect([bool]$Condition) { if (-not $Condition) { throw 'ASSERTION_FAILED' } }
function Refused([scriptblock]$Action, [string]$Code='DEV_RESTART_OWNER_NOT_CONFIRMED') {
    $result='NO_ERROR'
    try { & $Action } catch { $result=$_.Exception.Message }
    Expect ($result -ceq $Code)
    Expect ($script:killed.Count -eq 0)
}
function Case([string]$Name, [scriptblock]$Action) {
    New-Scenario
    try { & $Action; $cases.Add(@{caso=$Name;aprovado=$true}) }
    catch { $cases.Add(@{caso=$Name;aprovado=$false;erro=$_.Exception.Message}) }
}

Case 'reinicia somente FE e BE comprovados, nesta ordem' {
    $script:listeners += [pscustomobject]@{LocalPort=25591;LocalAddress='127.0.0.1';OwningProcess=999}
    $plan=Plan; Expect ($plan.Count -eq 2); Stop-Plan $plan
    Expect (($script:killed -join ',') -eq '35581,35580')
    Expect ($script:stopped.Count -eq 2 -and $script:listeners.Count -eq 1 -and $script:listeners[0].OwningProcess -eq 999)
}
Case 'sem instancia segue sem encerramento' { $script:listeners=@(); $plan=Plan; Stop-Plan $plan; Expect ($script:killed.Count -eq 0) }
Case 'somente backend existente' { $script:listeners=@($script:listeners | Where-Object LocalPort -eq 25580); Stop-Plan (Plan); Expect (($script:killed -join ',') -eq '35580') }
Case 'recusa porta de producao' { Refused { Get-WmsDevRestartPlan $repo 25590 25591 } 'DEV_RESTART_PORTS_NOT_ALLOWED' }
Case 'recusa frontend configurado fora DEV' { Refused { Get-WmsDevRestartPlan $repo 25580 3000 } 'DEV_RESTART_PORTS_NOT_ALLOWED' }
Case 'recusa projeto alheio mesmo PID' { $script:receipt.projectPath=Join-Path $run 'alheio'; Save-Fixture; Refused { Plan } }
Case 'recusa PID reutilizado' { $script:receipt.frontend.startUtc=[DateTime]::UtcNow.AddDays(-1).ToString('o'); Save-Fixture; Refused { Plan } }
Case 'recusa WMS_PROD' { $script:receipt.database='WMS_PROD'; Save-Fixture; Refused { Plan } }
Case 'recusa perfil producao' { $script:receipt.backend.profile='sqlserver-prod'; Save-Fixture; Refused { Plan } }
Case 'recusa comando com parametro extra' { $script:metadata[35580].CommandLine+=' --server.port=25590'; Refused { Plan } }
Case 'recusa vite de outro projeto' { $script:metadata[35581].CommandLine=$script:metadata[35581].CommandLine.Replace($repo,(Join-Path $run 'alheio')); Refused { Plan } }
Case 'recusa executavel alheio' { $script:metadata[35581].ExecutablePath='C:\outro.exe'; Refused { Plan } }
Case 'recusa JAR fora do projeto' { $script:receipt.backend.artifact.path='C:\outro\wms.jar'; Save-Fixture; Refused { Plan } }
Case 'recusa dono sem recibo' { [IO.File]::WriteAllText($fixture,'{}'); Refused { Plan } }
Case 'recusa recibo invalido' { [IO.File]::WriteAllText($fixture,'invalido'); Refused { Plan } }
Case 'recusa comando ilegivel' { $script:metadata[35581].CommandLine=''; Refused { Plan } }
Case 'recusa processo que tambem escuta outra porta' { $script:listeners += [pscustomobject]@{LocalPort=25591;LocalAddress='127.0.0.1';OwningProcess=35580}; Refused { Plan } }
Case 'recusa escuta externa' { $script:listeners[0].LocalAddress='0.0.0.0'; Refused { Plan } }
Case 'recusa mais de um dono na porta' { $script:listeners += [pscustomobject]@{LocalPort=25581;LocalAddress='127.0.0.1';OwningProcess=999}; Refused { Plan } }
Case 'revalida todas as origens antes do primeiro encerramento' { $plan=Plan; $script:metadata[35580].CommandLine+=' --extra'; Refused { Stop-Plan $plan } }
Case 'recusa troca do dono durante preparo' { $plan=Plan; $script:listeners[0].OwningProcess=999; Refused { Stop-Plan $plan } 'DEV_RESTART_OWNER_CHANGED' }
Case 'recusa nova escuta externa antes do encerramento' { $plan=Plan; $script:listeners[0].LocalAddress='0.0.0.0'; Refused { Stop-Plan $plan } 'DEV_RESTART_OTHER_LISTENER_REFUSED' }
Case 'nao encerra PID reaproveitado apos termino original' { $plan=Plan; $script:processes[35580].HasExited=$true; Refused { Stop-Plan $plan } 'DEV_RESTART_OWNER_CHANGED' }
Case 'ignora processo original que ja terminou' {
    $plan=Plan; $script:processes[35580].HasExited=$true
    $script:listeners=@($script:listeners | Where-Object LocalPort -eq 25581)
    Stop-Plan $plan; Expect (($script:killed -join ',') -eq '35581')
}
Case 'falha posterior nao encerra novo dono da porta' {
    $plan=Plan
    $script:processes[35581] | Add-Member ScriptMethod Kill -Force {
        $script:killed += $this.Id; $this.HasExited=$true
        $script:listeners=@([pscustomobject]@{LocalPort=25581;LocalAddress='127.0.0.1';OwningProcess=999})
        $script:processes[35580].HasExited=$true
    }
    $result=''; try { Stop-Plan $plan } catch { $result=$_.Exception.Message }
    Expect ($result -eq 'DEV_RESTART_PORT_NOT_RELEASED' -and ($script:killed -join ',') -eq '35581')
}
Case 'limpeza simultanea pelo launcher anterior' {
    $plan=Plan
    $script:processes[35580] | Add-Member ScriptMethod Kill -Force {
        $this.HasExited=$true
        $script:listeners=@($script:listeners | Where-Object LocalPort -ne 25580)
        throw 'PROCESS_ALREADY_EXITED'
    }
    Stop-Plan $plan; Expect (($script:killed -join ',') -eq '35581')
}
Case 'falha de encerramento interrompe sem matar outro processo' {
    $plan=Plan
    $script:processes[35581] | Add-Member ScriptMethod Kill -Force { throw 'ACCESS_DENIED' }
    Refused { Stop-Plan $plan } 'DEV_RESTART_STOP_FAILED'
}
$report=@{natureza='REINICIO_DEV_TESTES_ISOLADOS';sqlServer=$false;processosReaisEncerrados=0;total=$cases.Count;aprovados=@($cases | Where-Object aprovado).Count;casos=$cases.ToArray()}
$reportPath=Join-Path $run 'resultado.json'
[IO.File]::WriteAllText($reportPath,($report | ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
$cases | Where-Object { -not $_.aprovado } | ConvertTo-Json -Depth 3
Write-Host ($report.aprovados.ToString()+'/'+$report.total+' aprovados. '+$reportPath)
if ($report.aprovados -ne $report.total) { throw 'REINICIO_DEV_TESTES_FALHARAM' }
