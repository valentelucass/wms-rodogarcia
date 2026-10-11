# Selecao local do pacote cuja partida/JDBC/leituras SQL foram comprovadas.
# Nao abre SQL, HTTP, DPAPI, processos de aplicacao ou migracoes.
function Get-WmsSqlValidatedCandidate([string]$Repository,[string]$ExecutionPath,[string]$ExecutionSha256) {
    . (Join-Path $Repository 'infra/dev/artefato-backend.ps1')
    $sqlArea=[IO.Path]::GetFullPath((Join-Path $Repository 'orchestracao/.runtime/qual-conf01/sql01'))
    function Read-ValidatedReference($Reference) {
        $path=[IO.Path]::GetFullPath($Reference.path)
        if(!$path.StartsWith($sqlArea+'\',[StringComparison]::OrdinalIgnoreCase) -or $Reference.sha256 -notmatch '^[a-fA-F0-9]{64}$'){throw 'SQL01_PROMOTION_REFERENCE_REFUSED'}
        $cursor=$path
        while($cursor.Length -ge $sqlArea.Length){if((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'SQL01_PROMOTION_LINK_REFUSED'};$cursor=[IO.Path]::GetDirectoryName($cursor)}
        if((Get-FileHash -LiteralPath $path).Hash -ine $Reference.sha256){throw 'SQL01_PROMOTION_HASH_REFUSED'}
        Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    }
    $execution=Read-ValidatedReference @{path=$ExecutionPath;sha256=$ExecutionSha256}
    $uuid=[guid]::Empty
    if($execution.id -cne 'QUAL-CONF01-SQL01' -or ![guid]::TryParseExact($execution.runId,'D',[ref]$uuid) -or
        [IO.Path]::GetFullPath($ExecutionPath) -ine (Join-Path $sqlArea ('prumo/runs/'+$execution.runId+'/execution-result.json')) -or
        $execution.exit -ne 0 -or $execution.stage -cne 'REAL_SQL_FUNCTIONAL_SUBSET_APPROVED' -or
        $execution.ownBackendStopped -ne $true -or $execution.existingProcessesPreserved -ne $true -or $execution.SQLGuardAttempts -ne 1 -or
        $execution.ddl -ne 0 -or $execution.migrations -ne 0 -or $execution.PROD -ne 0 -or $execution.sa -ne 0){throw 'SQL01_REAL_VALIDATION_REQUIRED'}
    $runtime=Read-ValidatedReference $execution.runtime
    $candidate=Read-ValidatedReference $runtime.candidate
    $build=Read-ValidatedReference $candidate.build
    $review=Read-ValidatedReference $runtime.review
    $guard=Read-ValidatedReference $runtime.guard
    $jdbc=Read-ValidatedReference $execution.jdbc
    $http=Read-ValidatedReference $execution.functional
    $own=Join-Path $sqlArea ('cedro/'+$execution.runId)
    $authPath=Join-Path $own 'http/auth-result.json'
    $auth=Get-Content -LiteralPath $authPath -Raw -Encoding UTF8|ConvertFrom-Json
    if($runtime.runId -cne $execution.runId -or $runtime.code -cne 'SQL01_OWN_LISTENER_READY' -or $runtime.backend.listenerOwned -ne $true -or
        $runtime.database -cne 'WMS_DEV' -or $runtime.login -cne 'WMSDEV' -or $runtime.bootstrapDisabled -ne $true -or
        $build.exit -ne 0 -or $build.scope -cne 'PURE_NO_DATABASE' -or $build.sourceHash -cne $candidate.sourceHash -or
        $runtime.sourceHash -cne $candidate.sourceHash -or $runtime.artifact.sha256 -ine $candidate.jar.sha256 -or
        $review.SQLLaunchApproved -ne $true -or $review.owner -cne 'WMS - Vigia' -or $review.candidate.sha256 -ine $runtime.candidate.sha256 -or
        $review.sourceHash -cne $candidate.sourceHash -or $review.jarSha256 -ine $candidate.jar.sha256){throw 'SQL01_PROMOTION_CANDIDATE_BINDING_REFUSED'}
    if($guard.guardaRealAprovada -ne $true -or $guard.alvo.banco -cne 'WMS_DEV' -or $guard.alvo.login -cne 'WMSDEV' -or
        $guard.tls.trustServerCertificate -ne $false -or $guard.tls.encrypt -cne 'Mandatory' -or @($guard.checks).Count -ne 26 -or
        @($guard.checks|Where-Object {$_.passou -ne $true}).Count -ne 0 -or $jdbc.approved -ne $true -or $jdbc.database -cne 'WMS_DEV' -or
        $jdbc.login -cne 'WMSDEV' -or $jdbc.user -cne 'WMSDEV' -or $jdbc.provider -cne 'SQLServer' -or $jdbc.runId -cne $runtime.runId -or
        $jdbc.pid -ne $runtime.backend.pid -or $jdbc.declaredJarSha256 -ine $candidate.jar.sha256 -or $jdbc.declaredSourceHash -cne $candidate.sourceHash -or
        $jdbc.connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse -ne $true){throw 'SQL01_PROMOTION_SQL_PROOF_REFUSED'}
    if($auth.runId -cne $execution.runId -or $auth.legitimate -ne $true -or $auth.state -cne 'AUTHENTICATED' -or
        $auth.accountId -cne '89c0340f-7ad7-45b9-8a46-0fc2409d4459' -or $auth.trocarSenha -ne $false -or
        $http.runId -cne $execution.runId -or $http.exit -ne 0 -or $http.selected -ne 3 -or $http.passed -ne 3 -or $http.failures -ne 0 -or
        $http.fixturesCreated -ne 0 -or $http.businessMutations -ne 0 -or $http.cleanup -ne 0){throw 'SQL01_PROMOTION_AUTH_HTTP_REQUIRED'}
    foreach($endpoint in @('/api/v1/produtos','/api/v1/visao-operacao')){
        $cases=@($http.cases|Where-Object {$_.endpoint -ceq $endpoint -and $_.passed -eq $true -and $_.status -eq 200})
        if($cases.Count -ne 1 -or $cases[0].statementsInspectedPrepared -lt 1 -or @($cases[0].assertions.PSObject.Properties|Where-Object {$_.Value -ne $true}).Count){throw 'SQL01_PROMOTION_FUNCTIONAL_CASE_REFUSED'}
        $trace=Read-ValidatedReference $cases[0].observation
        if($trace.httpStatus -ne 200 -or $trace.routePattern -cne $endpoint -or $trace.chainCompleted -ne $true -or $trace.statementsInspectedPrepared -lt 1){throw 'SQL01_PROMOTION_OBSERVATION_REFUSED'}
    }
    $null=Assert-WmsDevApplicationArtifact $Repository $runtime.candidate.path
    if($candidate.jar.path -ine (Join-Path $build.target 'wms-backend-0.0.1-SNAPSHOT.jar')){throw 'SQL01_PROMOTION_MANUAL_JAR_REFUSED'}
    $reports=@(Get-ChildItem -LiteralPath (Join-Path $build.target 'surefire-reports') -File -Filter 'TEST-*.xml')
    if(!$reports.Count){throw 'SQL01_PROMOTION_PURE_REPORTS_MISSING'}
    $pureTests=0
    foreach($report in $reports){
        [xml]$suite=Get-Content -LiteralPath $report.FullName -Raw -Encoding UTF8
        if([int]$suite.testsuite.failures -ne 0 -or [int]$suite.testsuite.errors -ne 0 -or [int]$suite.testsuite.skipped -ne 0){throw 'SQL01_PROMOTION_PURE_REPORTS_REFUSED'}
        $pureTests+=[int]$suite.testsuite.tests
    }
    foreach($ref in @($candidate.java,$candidate.config)){
        if((Get-FileHash -LiteralPath $ref.path).Hash -ine $ref.sha256){throw 'SQL01_PROMOTION_TOOL_CONFIG_REFUSED'}
    }
    return @{candidate=$candidate;runtime=$runtime;execution=$execution;executionReference=@{path=[IO.Path]::GetFullPath($ExecutionPath);sha256=$ExecutionSha256};authReference=@{path=$authPath;sha256=(Get-FileHash $authPath).Hash.ToLowerInvariant()};pureTests=$pureTests;functionalBusinessReads=2;identityReads=1}
}

function Select-WmsSqlValidatedCandidate([string]$Repository,[string]$ExecutionPath,[string]$ExecutionSha256) {
    . (Join-Path $Repository 'infra/dev/artefato-backend.ps1')
    $validated=Get-WmsSqlValidatedCandidate $Repository $ExecutionPath $ExecutionSha256
    $candidate=$validated.candidate
    $run=Join-Path $Repository ('orchestracao/.runtime/qual-conf01/sql01/prumo/promocao-'+[guid]::NewGuid().ToString('N').Substring(0,8))
    $null=New-Item -ItemType Directory -Path $run
    $files=@();$updated=@()
    foreach($name in @('login-d32-preparo.json','frontend-dev02-preparo.json')){
        $active=Join-Path $Repository ('backend/evidencias/'+$name);$backup=Join-Path $run ('anterior-'+$name);$prepared=Join-Path $run ('novo-'+$name)
        $oldHash=(Get-FileHash $active).Hash.ToLowerInvariant()
        [IO.File]::Copy($active,$backup,$false)
        if((Get-FileHash $backup).Hash -ine $oldHash){throw 'SQL01_PROMOTION_BACKUP_REFUSED'}
        $value=@{natureza='SQL01_PACOTE_VALIDADO_SQL_REAL';observadoUtc=[DateTime]::UtcNow.ToString('o');sourceHash=$candidate.sourceHash;java=$candidate.java;jar=$candidate.jar;config=$candidate.config;validacaoSql=$validated.executionReference;verificacao=@{ambiente='SQLServer WMS_DEV/WMSDEV';testesPuros=$validated.pureTests;leiturasFuncionais=2;leiturasIdentidade=1;sqlServer=$true;integracaoSqlObrigatoria=$true;integracaoIntegral=$false};linhagem=@{reciboAnterior=$backup;sha256=$oldHash;preservado=$true}}
        $stream=[IO.File]::Open($prepared,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
        try{$bytes=[Text.UTF8Encoding]::new($false).GetBytes(($value|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
        $null=Assert-WmsDevApplicationArtifact $Repository $prepared
        $files+=@{active=$active;backup=$backup;prepared=$prepared;replaceBackup=(Join-Path $run ('replace-backup-'+$name));previousSha256=$oldHash;preparedSha256=(Get-FileHash $prepared).Hash.ToLowerInvariant()}
    }
    $receipt=@{id='QUAL-CONF01-SQL01';runId=$validated.execution.runId;sourceHash=$candidate.sourceHash;jarSha256=$candidate.jar.sha256;validation=$validated.executionReference;files=$files;activeReceiptsChanged=$false;SQLConnections=0;HTTP=0;processes=0;existingProcessesAltered=$false;exit=1}
    $promotionPhase='VALIDATED_BACKUPS_READY';$promotionFile=$null
    try {
        foreach($file in $files){
            $promotionFile=$file.active;$promotionPhase='ACTIVE_REVALIDATION'
            if((Get-FileHash $file.active).Hash -ine $file.previousSha256){throw 'SQL01_PROMOTION_ACTIVE_CHANGED'}
            $promotionPhase='FILE_REPLACE'
            if(Test-Path -LiteralPath $file.replaceBackup){throw 'SQL01_PROMOTION_REPLACE_BACKUP_EXISTS'}
            # PowerShell 5.1 converte $null para string vazia nesta assinatura.
            # Um destino novo concreto evita esse erro e preserva copia atomica.
            [IO.File]::Replace($file.prepared,$file.active,$file.replaceBackup)
            $updated+=$file.active
            $promotionPhase='WRITTEN_HASH_REVALIDATION'
            if((Get-FileHash $file.active).Hash -ine $file.preparedSha256 -or (Get-FileHash $file.replaceBackup).Hash -ine $file.previousSha256){throw 'SQL01_PROMOTION_WRITE_REFUSED'}
        }
        $receipt.exit=0;$receipt.state='SELECTED_SQL_VALIDATED_CANDIDATE'
    } catch {
        $chain=@();$exception=$_.Exception
        while($exception -and $chain.Count -lt 8){$chain+=@{type=$exception.GetType().FullName;hresult=$exception.HResult};$exception=$exception.InnerException}
        $receipt.error=@{phase=$promotionPhase;path=$promotionFile;chain=$chain;messagesIncluded=$false;retry=$false;rollback=$false}
        $receipt.state=if($updated.Count){'PARTIAL_PROMOTION_STOPPED'}else{'PROMOTION_STOPPED'}
        throw 'SQL01_PROMOTION_FAILED_SEE_SANITIZED_RECEIPT'
    } finally {
        $receipt.activeReceiptsChanged=($updated.Count -gt 0)
        $receipt.updated=$updated;$receipt.finishedUtc=[DateTime]::UtcNow.ToString('o')
        [IO.File]::WriteAllText((Join-Path $run 'resultado.json'),($receipt|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
    }
    Write-Host ('Pacote DEV atual selecionado, com prova SQL real: '+(Join-Path $run 'resultado.json'))
}
