# D26: biblioteca sem execução SQL ao importar. Segredo somente PSCredential/SecureString.
$d26Raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'd22-credencial.ps1')
function Get-WmsDevCredentialContext {
    $base=[Environment]::GetFolderPath('LocalApplicationData')
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $pasta=Join-Path $base 'Rodogarcia/WMS/api-dev'
    [pscustomobject]@{pasta=$pasta;arquivo=(Join-Path $pasta 'wmsdev-app.clixml');sid=$sid}
}
function Assert-WmsDevCredentialPath($Contexto) {
    foreach($p in @([Environment]::GetFolderPath('LocalApplicationData'),
        (Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia'),
        (Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/WMS'),$Contexto.pasta,$Contexto.arquivo)) {
        if(Test-Path -LiteralPath $p) {
            $i=Get-Item -LiteralPath $p -Force
            if(($i.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0){throw 'D26_CREDENCIAL_REPARSE_RECUSADO'}
            if(($p -ceq $Contexto.arquivo) -eq $i.PSIsContainer){throw 'D26_CREDENCIAL_TIPO_CAMINHO_INVALIDO'}
        }
    }
}
function Get-WmsDevCredentialMetadata {
    $ctx=Get-WmsDevCredentialContext
    Assert-WmsDevCredentialPath $ctx
    $itens=@();$valida=$true
    foreach($p in @($ctx.pasta,$ctx.arquivo)) {
        if(-not (Test-Path -LiteralPath $p)){$valida=$false;continue}
        $a=Get-WmsD22AclMetadata $p $ctx.sid ($p -ceq $ctx.pasta)
        $itens+=@([pscustomobject]@{caminho=$p;acl=$a})
        if(-not $a.valida){$valida=$false}
    }
    [pscustomobject]@{arquivo=$ctx.arquivo;sidCorrente=$ctx.sid;existe=(Test-Path -LiteralPath $ctx.arquivo);aclValida=$valida;itens=$itens;conteudoExibido=$false}
}
function Get-WmsDevApplicationCredential {
    $original=$null;$copia=$null
    try {
        $ctx=Get-WmsDevCredentialContext;$antes=Get-WmsDevCredentialMetadata
        if(-not $antes.existe -or -not $antes.aclValida){throw 'D26_CREDENCIAL_PROPRIA_AUSENTE_OU_ACL_INVALIDA'}
        $original=Import-Clixml -LiteralPath $ctx.arquivo -ErrorAction Stop
        if($original -isnot [Management.Automation.PSCredential] -or $original.UserName -cne 'WMSDEV' -or $original.Password.Length -lt 32){throw 'D26_CREDENCIAL_LOGIN_OU_TIPO_INVALIDO'}
        $depois=Get-WmsDevCredentialMetadata
        if(-not $depois.aclValida){throw 'D26_CREDENCIAL_ACL_MUDOU'}
        $copia=$original.Password.Copy();$copia.MakeReadOnly()
        New-Object Management.Automation.PSCredential('WMSDEV',$copia)
    } catch {
        if($copia){$copia.Dispose()}
        if($_.Exception.Message -cmatch '^D26_[A-Z0-9_]+$'){throw $_.Exception.Message}
        throw 'D26_CREDENCIAL_DPAPI_USUARIO_MAQUINA_RECUSADA'
    } finally {if($original -is [Management.Automation.PSCredential]){$original.Password.Dispose()}}
}
function Save-WmsDevNewApplicationCredential([Security.SecureString]$Password) {
    $ctx=Get-WmsDevCredentialContext;Assert-WmsDevCredentialPath $ctx
    if(Test-Path -LiteralPath $ctx.arquivo){throw 'D26_CREDENCIAL_EXISTENTE_PRESERVADA'}
    if(Test-Path -LiteralPath $ctx.pasta) {
        if(-not (Get-WmsD22AclMetadata $ctx.pasta $ctx.sid $true).valida){throw 'D26_PASTA_PRIVADA_EXISTENTE_DIVERGENTE_PRESERVADA'}
    } else {$null=[IO.Directory]::CreateDirectory($ctx.pasta,(New-WmsD22Security $ctx.sid $true))}
    $copia=$Password.Copy();$temp=Join-Path $ctx.pasta ('wmsdev-'+[Guid]::NewGuid().ToString('N')+'.tmp.clixml')
    try {
        $f=[IO.File]::Open($temp,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None);$f.Dispose()
        [IO.File]::SetAccessControl($temp,(New-WmsD22Security $ctx.sid $false))
        $cred=New-Object Management.Automation.PSCredential('WMSDEV',$copia)
        $cred|Export-Clixml -LiteralPath $temp -Depth 3 -ErrorAction Stop
        if(-not (Get-WmsD22AclMetadata $temp $ctx.sid $false).valida){throw 'D26_CREDENCIAL_ACL_TEMP_INVALIDA'}
        Assert-WmsDevCredentialPath $ctx
        [IO.File]::Move($temp,$ctx.arquivo) # Create-only: jamais substitui arquivo.
        $m=Get-WmsDevCredentialMetadata
        if(-not $m.aclValida){throw 'D26_CREDENCIAL_ACL_FINAL_INVALIDA'}
        $m
    } finally {
        $copia.Dispose()
        if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp -Force}
    }
}
function Get-WmsDevApplicationProfile {
    Import-Module ([IO.Path]::GetFullPath((Join-Path $d26Raiz '../.runtime/sql-server/SqlServerProjetos.psm1'))) -Force
    $p=Get-ProjetosSqlProfile
    [pscustomobject]@{server=$p.server;host=$p.host;port=$p.port;database='WMS_DEV';login='WMSDEV';
        javaHome=$p.javaHome;truststore=$p.truststore;certificateHost=$p.certificateHost;
        jdbcUrl=($p.jdbcBaseUrl+';databaseName=WMS_DEV');trustStorePassword='projetos-public-cert';
        encrypt=$true;trustServerCertificate=$false}
}
function New-WmsDevApplicationConnection([Management.Automation.PSCredential]$Credential,[string]$ApplicationName='WMS-D26-DEV') {
    if($Credential.UserName -cne 'WMSDEV'){throw 'D26_CONEXAO_SOMENTE_WMSDEV'}
    Import-Module ([IO.Path]::GetFullPath((Join-Path $d26Raiz '../.runtime/sql-server/SqlServerProjetos.psm1'))) -Force
    New-ProjetosSqlConnection -Database WMS_DEV -Credential $Credential -ApplicationName $ApplicationName
}
