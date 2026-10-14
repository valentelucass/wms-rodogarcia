# Executa apenas funcoes/ramo AST com objetos ficticios. Zero SQL/DPAPI/HTTP/processos.
[CmdletBinding()]
param([switch]$Antes,[Parameter(Mandatory=$true)][string]$OutputPath)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $repo 'orchestracao/.runtime/be02-dev-fonte01/guarda-timeout02'
if(Test-Path -LiteralPath $OutputPath){throw 'PURE_OUTPUT_EXISTS'}
. (Join-Path $PSScriptRoot 'espera-conexao.ps1')
$policy=Get-WmsDevConnectionWaitPolicy
$guardSource=if($Antes){Join-Path $area 'guarda-wmsdev-before.ps1'}else{Join-Path $repo 'infra/dev02/guarda-wmsdev.ps1'}
$launcherSource=if($Antes){Join-Path $area 'iniciar-dev-before.ps1'}else{Join-Path $PSScriptRoot 'iniciar-dev.ps1'}
$cases=New-Object 'Collections.Generic.List[object]'
function Check-Pure([string]$Name,[bool]$Passed){$cases.Add(@{name=$Name;passed=$Passed})}
function Parse-PureSource([string]$Path){
    $tokens=$null;$errors=$null
    $ast=[Management.Automation.Language.Parser]::ParseFile($Path,[ref]$tokens,[ref]$errors)
    if(@($errors).Count){throw 'PURE_SOURCE_AST_INVALID'}
    return $ast
}

# A funcao Select real deve configurar o comando pelo prazo da politica.
$guardAst=Parse-PureSource $guardSource
$select=$guardAst.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -ceq 'Select-Dev02'},$true)
if(!$select){throw 'PURE_SELECT_AST_MISSING'}
. ([scriptblock]::Create($select.Extent.Text))
$command=[pscustomobject]@{CommandText='';CommandTimeout=0;Disposed=$false;ExecuteReaderCalls=0}
$command|Add-Member ScriptMethod ExecuteReader {$this.ExecuteReaderCalls++;throw 'PURE_READER_STOP_NO_NETWORK'}
$command|Add-Member ScriptMethod Dispose {$this.Disposed=$true}
$connection=[pscustomobject]@{Command=$command}
$connection|Add-Member ScriptMethod CreateCommand {return $this.Command}
$commandTimeoutSeconds=$policy.CommandTimeoutSeconds
$identityConfirmed=$true
$report=@{queriesGuarda=0;queriesMetadados=0}
$queries=New-Object 'Collections.Generic.List[object]'
try {Select-Dev02 'PURE_IDENTITY' 'SELECT DB_NAME() banco' -Identity;throw 'PURE_READER_SHOULD_STOP'}catch{
    if($_.Exception.ToString() -notmatch 'PURE_READER_STOP_NO_NETWORK'){throw}
}
Check-Pure 'real_Select_assigns_policy_to_command' ($command.CommandTimeout -eq $policy.CommandTimeoutSeconds -and $command.CommandTimeout -gt 3)
Check-Pure 'real_Select_disposes_command_and_records_failed_query' ($command.Disposed -and $command.ExecuteReaderCalls -eq 1 -and $queries.Count -eq 1 -and !$queries[0].concluida -and $report.queriesGuarda -eq 1)

# O ramo real do launcher deve informar a fase e ainda executar o STOP20.
$launcherAst=Parse-PureSource $launcherSource
$failureBranch=$launcherAst.Find({param($node) $node -is [Management.Automation.Language.IfStatementAst] -and $node.Extent.Text.StartsWith('if ($guardProcess.ExitCode -ne 0)')},$true)
if(!$failureBranch){throw 'PURE_LAUNCHER_BRANCH_MISSING'}
$failureAction=[scriptblock]::Create($failureBranch.Extent.Text)
$consoleMessages=New-Object 'Collections.Generic.List[string]'
function Write-Host {param([Parameter(ValueFromRemainingArguments=$true)][object[]]$Object) $consoleMessages.Add(($Object -join ' '))}
function Fail-Startup([int]$Code,[string]$Reason){throw ('PURE_STOP_'+$Code+'_'+$Reason)}
$guardProcess=[pscustomobject]@{ExitCode=20}
$guardReceiptPath='PURE_PUBLIC_RECEIPT_PATH'
$connectionWaitPolicy=$policy
$userReceipt=Join-Path $repo 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs/8ccd961e-0d74-4a01-9245-868781089f5d/guarda.json'
$original=Get-Content -LiteralPath $userReceipt -Raw -Encoding UTF8|ConvertFrom-Json
$fixtures=@(
    @{name='real_user_identity_timeout_after_open';value=$original;expected='consulta de identificacao do WMS_DEV';limit='3s';reject='conexao em 120s'},
    @{name='opening_timeout';value=@{aberturaConcluida=$false;erro=@{fase='ABERTURA';marcadores=@{timeout=$true}}};expected='abertura da conexao';limit='120s';reject='consulta de'},
    @{name='permissions_timeout_after_open';value=@{aberturaConcluida=$true;politica=@{commandTimeout=30};erro=@{fase='PERMISSOES';marcadores=@{timeout=$true}}};expected='consulta de permissoes';limit='30s';reject='conexao em 120s'},
    @{name='catalog_timeout_after_open';value=@{aberturaConcluida=$true;politica=@{commandTimeout=30};erro=@{fase='CATALOGO_HISTORICO';marcadores=@{timeout=$true}}};expected='consulta de estrutura e historico';limit='30s';reject='conexao em 120s'},
    @{name='certificate_refusal';value=@{erro=@{marcadores=@{certificado=$true}}};expected='validar o certificado';limit='';reject='conexao em 120s'},
    @{name='login_refusal';value=@{erro=@{marcadores=@{autenticacao=$true}}};expected='recusou o acesso';limit='';reject='conexao em 120s'},
    @{name='auxiliary_error_shape_without_markers';value=@{erro=@{codigo='DEV02_AUXILIAR_OU_SAIDA_RECUSADA'}};expected='seguranca ou estrutura';limit='';reject='conexao em 120s'}
    @{name='migration_inventory_refusal_before_sql';value=@{aberturaTentativas=0;checks=@(@{criterio='inventario local completo conforme contrato versionado';passou=$false});erro=@{codigo='DEV02_GUARDA_RECUSADA';fase='PREPARO_LOCAL'}};expected='migrations locais divergem do contrato';limit='';reject='timeout'}
)
foreach($fixture in $fixtures){
    if($fixture.value -is [hashtable] -and $fixture.value.erro.ContainsKey('marcadores')){
        foreach($name in @('timeout','certificado','autenticacao','prelogin','handshake')){
            if(!$fixture.value.erro.marcadores.ContainsKey($name)){$fixture.value.erro.marcadores[$name]=$false}
        }
    }
    $guard=$fixture.value|ConvertTo-Json -Depth 18|ConvertFrom-Json
    $consoleMessages.Clear();$stopped=$false
    try {& $failureAction}catch{
        if($_.Exception.Message -ceq 'PURE_STOP_20_GUARD_CURRENT_BLOCKED'){$stopped=$true}
    }
    $message=$consoleMessages -join "`n"
    $messageCorrect=($message -match [regex]::Escape($fixture.expected) -and (!$fixture.limit -or $message.Contains($fixture.limit)) -and !$message.Contains($fixture.reject))
    Check-Pure $fixture.name ($messageCorrect -and $stopped)
}
$guardProcess.ExitCode=0;$consoleMessages.Clear();$passedThrough=$true
try {& $failureAction}catch{$passedThrough=$false}
Check-Pure 'success_does_not_run_failure_branch' ($passedThrough -and $consoleMessages.Count -eq 0)
Check-Pure 'bounded_independent_deadlines_no_infinite_timeout' ($policy.ConnectTimeoutSeconds -eq 120 -and $policy.CommandTimeoutSeconds -eq 30 -and $policy.GuardTimeoutSeconds -eq 240)
$receipt=@{scope='PURE_DIAGNOSTICS_ACTUAL_AST_FAKE_COMMAND_NO_SQL_NO_DPAPI_NO_HTTP_NO_PROCESS';phase=if($Antes){'RED_BEFORE'}else{'GREEN_AFTER'};selected=$cases.Count;passed=@($cases|Where-Object {$_.passed}).Count;failed=@($cases|Where-Object {!$_.passed}).Count;SQLConnections=0;HTTP=0;DPAPI=0;applicationProcesses=0;commandTimeoutAssigned=$command.CommandTimeout;cases=$cases.ToArray();sources=@(foreach($p in @($guardSource,$launcherSource,(Join-Path $PSScriptRoot 'espera-conexao.ps1'))){@{path=$p;sha256=(Get-FileHash $p).Hash.ToLowerInvariant()}})}
$stream=[IO.File]::Open([IO.Path]::GetFullPath($OutputPath),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
try{$bytes=[Text.Encoding]::UTF8.GetBytes(($receipt|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
$receipt|ConvertTo-Json -Depth 5
if($receipt.failed){exit 1}
