[CmdletBinding()]
param([ValidateSet('Plan','Apply')][string]$Modo='Plan',[ValidateSet('WMS_DEV')][string]$Banco='WMS_DEV')
$ErrorActionPreference='Stop'
$WmsD27ExecutorPath=$MyInvocation.MyCommand.Path
. (Join-Path $PSScriptRoot 'd27-guardas.ps1')
. (Join-Path $PSScriptRoot 'd27-preflight.ps1')
function Invoke-WmsD27Aplicacao {
    $inicio=[DateTime]::UtcNow;$cred=$null;$copy=$null;$etapas=New-Object 'Collections.Generic.List[object]';$etapa='REVISAO';$antes=$null
    try{
        $prepared=Join-Path $WmsD27Database ('docs/propostas/d27/'+$WmsD27V10);$conf=$prepared+'.conf'
        $hash=(Get-FileHash $prepared -Algorithm SHA256).Hash;$executor=(Get-FileHash $WmsD27ExecutorPath -Algorithm SHA256).Hash;$configHash=(Get-FileHash $conf -Algorithm SHA256).Hash
        $receipt=Join-Path $WmsD27Root 'orchestracao/.runtime/d27-farol-continuar.json'
        if(-not(Test-Path $receipt -PathType Leaf)){throw 'D27_REVISAO_E_SINAL_AUSENTES_OU_DIVERGENTES'}
        $revisao=Get-Content $receipt -Raw -Encoding UTF8|ConvertFrom-Json;Assert-WmsD27Revisao $revisao $hash $executor $configHash
        Assert-WmsD27Dev $Banco;Assert-WmsD21Pom (Join-Path $WmsD27Root 'backend/pom.xml')
        $fontes=@(Get-WmsD27Fontes)
        $etapa='PREFLIGHT';$antes=Invoke-WmsD27Preflight
        if($antes.estado -cne 'PREFLIGHT_ADMIN_REAL_CONFERIDO'){throw 'D27_PREFLIGHT_BLOQUEOU_MIGRATION'}
        $aplicada=@($antes.historico|Where-Object {$_.type -ceq 'SQL' -and $_.version -ceq '10'})
        if(-not(Test-WmsD27Check $antes.check.definition -Novo:($aplicada.Count -eq 1))){throw 'D27_CHECK_REAL_DIVERGENTE'}
        $target=Join-Path $WmsD27Database ('migrations/'+$WmsD27V10)
        if(Test-Path $target){if((Get-FileHash $target).Hash -cne $hash){throw 'D27_V10_ATIVA_DIVERGENTE'}}else{Copy-Item -LiteralPath $prepared -Destination $target}
        if(Test-Path ($target+'.conf')){if((Get-FileHash ($target+'.conf')).Hash -cne $configHash){throw 'D27_SIDECAR_ATIVO_DIVERGENTE'}}else{Copy-Item -LiteralPath $conf -Destination ($target+'.conf')}
        $fontes=@(Get-WmsD27Fontes)
        $perfil=Get-ProjetosSqlProfile
        if($perfil.server -cne $antes.alvo.servidor -or $perfil.host -cne '127.0.0.1' -or $perfil.port -ne 1433){throw 'D27_PERFIL_DIVERGENTE'}
        $release=Get-Content (Join-Path $perfil.javaHome 'release') -Encoding UTF8
        if(-not($release -match '^JAVA_VERSION="21[.\"]')){throw 'D27_JDK21_EXISTENTE_NECESSARIO'}
        # A ferramenta existente bloqueia distribuicao ausente e usa Maven offline.
        Assert-WmsD21Ferramentas $WmsD27Database
        $cred=Get-ProjetosSqlAdminCredential
        foreach($etapa in @('PreValidate','Migrate','PostValidate','Info')){
            Assert-WmsD21FontesIguais $fontes @(Get-WmsD27Fontes)
            Assert-WmsD27Revisao $revisao $hash $executor $configHash
            if((Get-FileHash $target).Hash -cne $hash -or (Get-FileHash ($target+'.conf')).Hash -cne $configHash){throw 'D27_V10_REVISADA_MUDOU'}
            $psi=New-WmsD27StartInfo $perfil.server $etapa $perfil.javaHome
            $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_TRUSTSTORE']=$perfil.truststore
            $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_CERTIFICATE_HOST']=$perfil.certificateHost
            $copy=$cred.Password.Copy();$copy.MakeReadOnly()
            try{$result=Invoke-WmsD21Processo $psi $copy}finally{$copy.Dispose();$copy=$null}
            $etapas.Add([pscustomobject]@{etapa=$etapa;exitCode=$result.exitCode;resultado=$result.resultado;pid=$result.pid;processoEncerrado=$result.processoEncerrado;saidaBrutaDescartada=$true})
            if($result.exitCode -ne 0){throw 'D27_FLYWAY_INTERROMPIDO_PRESERVAR'}
        }
        $etapa='POSCHECK';$depois=Invoke-WmsD27Preflight
        if($depois.estado -cne 'PREFLIGHT_ADMIN_REAL_CONFERIDO' -or -not(Test-WmsD27Check $depois.check.definition -Novo) -or @($depois.historico|Where-Object {$_.type -ceq 'SQL'}).Count -ne 10){throw 'D27_POSCONDICAO_DIVERGENTE'}
        foreach($nome in @('wmsdevIdentidade','wmsdevPermissoes','wmsdevRoles')){if(($antes.$nome|ConvertTo-Json -Depth 8 -Compress) -cne ($depois.$nome|ConvertTo-Json -Depth 8 -Compress)){throw 'D27_PERMISSOES_WMSDEV_DIVERGENTES'}}
        if(($antes.fixtures|ConvertTo-Json -Compress) -cne ($depois.fixtures|ConvertTo-Json -Compress)){throw 'D27_FIXTURES_MUDARAM_CONFERIR_SEM_RESET'}
        Assert-WmsD21FontesIguais $fontes @(Get-WmsD27Fontes)
        $r=[pscustomobject]@{estado='V10_DEV_APLICADA_E_CONFERIDA';inicioUtc=$inicio.ToString('o');fimUtc=[DateTime]::UtcNow.ToString('o');antes=$antes;depois=$depois;etapas=$etapas.ToArray();fontes=$fontes;somenteDev=$true;prodConectado=$false;businessDML=0;segredoExibido=$false}
    }catch{$pidFinal=$script:WmsD21UltimoPid;$r=[pscustomobject]@{estado='EXECUCAO_INTERROMPIDA_PRESERVAR';inicioUtc=$inicio.ToString('o');fimUtc=[DateTime]::UtcNow.ToString('o');etapa=$etapa;codigo=if($_.Exception.Message -cmatch '^(D27_|D21_|D24_|SQL_)[A-Z0-9_]+$'){$_.Exception.Message}else{'D27_EXECUTOR_FALHOU'};linha=$_.InvocationInfo.ScriptLineNumber;preflight=$antes;etapas=$etapas.ToArray();ultimoFilhoPid=$pidFinal;ultimoFilhoEncerrado=if($pidFinal){-not [bool](Get-Process -Id $pidFinal -ErrorAction SilentlyContinue)}else{$null};somenteDev=$true;prodConectado=$false;businessDML=0;segredoExibido=$false;semRepairCleanReset=$true}
    }finally{if($copy){$copy.Dispose()};if($cred){$cred.Password.Dispose()}}
    $r|ConvertTo-Json -Depth 16|Set-Content (Join-Path $WmsD27Database ('evidencias/d27-aplicacao-'+$inicio.ToString('yyyyMMddTHHmmssfff')+'.json')) -Encoding UTF8
    $r|Select-Object estado,codigo,etapa,prodConectado|ConvertTo-Json -Compress
    if($r.estado -cne 'V10_DEV_APLICADA_E_CONFERIDA'){exit 2}
}
if($MyInvocation.InvocationName -ne '.'){
    Assert-WmsD27Dev $Banco
    if($Modo -ceq 'Plan'){$f=@(Get-WmsD27Fontes);[pscustomobject]@{estado='PLAN_OFFLINE_AGUARDA_VIGIA_FAROL';banco='WMS_DEV';perfil='bootstrap-local';target='10';migrationsAtivas=$f.Count;v10Preparada='database/docs/propostas/d27/'+$WmsD27V10;ordem=@('PreValidate','Migrate','PostValidate','Info','Poscheck');senha='API_SECURESTRING_ENV_FILHO_EFEMERO';sqlExecutado=$false;prodConectado=$false}|ConvertTo-Json -Compress}else{Invoke-WmsD27Aplicacao}
}
