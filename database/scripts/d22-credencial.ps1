[CmdletBinding()]
param([ValidateSet('Plan','Metadados')][string]$Action='Plan')
$ErrorActionPreference='Stop'
function Get-WmsD22Contexto {
    if($env:OS -cne 'Windows_NT'){throw 'D22_EXIGE_WINDOWS_DPAPI'}
    $base=[Environment]::GetFolderPath('LocalApplicationData')
    if([string]::IsNullOrWhiteSpace($base)){throw 'D22_LOCALAPPDATA_AUSENTE'}
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    [pscustomobject]@{base=[IO.Path]::GetFullPath($base);wms=(Join-Path $base 'Rodogarcia/WMS');
        pasta=(Join-Path $base 'Rodogarcia/WMS/database-runner');arquivo=(Join-Path $base 'Rodogarcia/WMS/database-runner/credencial-sa.clixml');sid=$sid}
}
function Assert-WmsD22Caminho($Contexto){
    # Contexto prod e fixo; parametro existe somente para fixtures isoladas da biblioteca.
    if($Contexto.sid -notmatch '^S-1-\d+(?:-\d+)+$'){throw 'D22_SID_INVALIDO'}
    $base=[IO.Path]::GetFullPath($Contexto.base).TrimEnd('\')
    $wms=[IO.Path]::GetFullPath((Join-Path $base 'Rodogarcia/WMS'))
    if([IO.Path]::GetFullPath($Contexto.wms) -cne $wms -or
       [IO.Path]::GetFullPath($Contexto.pasta) -cne (Join-Path $wms 'database-runner') -or
       [IO.Path]::GetFullPath($Contexto.arquivo) -cne (Join-Path $wms 'database-runner/credencial-sa.clixml')){throw 'D22_CAMINHO_FIXO_DIVERGENTE'}
    foreach($p in @($base,(Join-Path $base 'Rodogarcia'),$Contexto.wms,$Contexto.pasta,$Contexto.arquivo)){
        if(Test-Path -LiteralPath $p){
            $f=Get-Item -LiteralPath $p -Force
            if(($f.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0){throw 'D22_REPARSE_RECUSADO'}
            $folha=($p -ceq $Contexto.arquivo)
            if($folha -eq $f.PSIsContainer){throw 'D22_TIPO_CAMINHO_DIVERGENTE'}
        }
    }
}
function Get-WmsD22AclMetadata([string]$Path,[string]$Sid,[bool]$Pasta){
    $a=Get-Acl -LiteralPath $Path
    $owner=$a.GetOwner([Security.Principal.SecurityIdentifier]).Value
    $rs=@($a.GetAccessRules($true,$true,[Security.Principal.SecurityIdentifier]))
    $valido=($owner -ceq $Sid -and $a.AreAccessRulesProtected -and $rs.Count -eq 2)
    $vistos=@()
    foreach($r in $rs){
        $n=$r.IdentityReference.Value;$vistos+=$n
        $heranca=if($Pasta){[Security.AccessControl.InheritanceFlags]'ContainerInherit,ObjectInherit'}else{[Security.AccessControl.InheritanceFlags]::None}
        if($n -cnotin @($Sid,'S-1-5-18') -or $r.IsInherited -or $r.AccessControlType -ne [Security.AccessControl.AccessControlType]::Allow -or
           $r.FileSystemRights -ne [Security.AccessControl.FileSystemRights]::FullControl -or $r.InheritanceFlags -ne $heranca -or
           $r.PropagationFlags -ne [Security.AccessControl.PropagationFlags]::None){$valido=$false}
    }
    if(@($vistos|Sort-Object -Unique).Count -ne 2){$valido=$false}
    [pscustomobject]@{ownerSid=$owner;protegida=$a.AreAccessRulesProtected;aces=$rs.Count;valida=$valido}
}
function Get-WmsD22Metadados($Contexto){
    $r=[ordered]@{natureza='D22_SOMENTE_EXISTENCIA_OWNER_ACL';arquivo=$Contexto.arquivo;sidCorrente=$Contexto.sid;
        estado='AUSENTE';codigo='D22_CREDENCIAL_AUSENTE';arquivoExiste=$false;aclValida=$false;conteudoLido=$false;credencialProvisionadaPorAgente=$false;itens=@()}
    try{
        Assert-WmsD22Caminho $Contexto
        foreach($p in @($Contexto.wms,$Contexto.pasta,$Contexto.arquivo)){
            if(-not (Test-Path -LiteralPath $p)){return [pscustomobject]$r}
            $pasta=($p -cne $Contexto.arquivo);$a=Get-WmsD22AclMetadata $p $Contexto.sid $pasta
            $r.itens+=@([pscustomobject]@{caminho=$p;acl=$a})
            if(-not $a.valida){$r.estado='ACL_OWNER_INVALIDOS';$r.codigo='D22_CREDENCIAL_ACL_OWNER_INVALIDOS';return [pscustomobject]$r}
        }
        $r.arquivoExiste=$true;$r.aclValida=$true;$r.estado='PRESENTE_ACL_COMPATIVEL_CONTEUDO_NAO_LIDO';$r.codigo=$null
    }catch{
        $r.estado='METADADOS_RECUSADOS'
        $r.codigo=if($_.Exception.Message -cmatch '^D22_[A-Z0-9_]+$'){$_.Exception.Message}else{'D22_CREDENCIAL_METADADOS_INDISPONIVEIS'}
    }
    [pscustomobject]$r
}
function Read-WmsD22Credencial($Contexto){
    $cred=$null;$senha=$null
    try{
        $m=Get-WmsD22Metadados $Contexto
        if(-not $m.aclValida -or -not $m.arquivoExiste){throw $m.codigo}
        # Import somente pelo operador no modo normal; agente usa apenas fixtures ficticias.
        $cred=Import-Clixml -LiteralPath $Contexto.arquivo -ErrorAction Stop
        if($cred -isnot [Management.Automation.PSCredential] -or $cred.UserName -cne 'sa' -or
           $null -eq $cred.Password -or $cred.Password.Length -eq 0){throw 'D22_CREDENCIAL_TIPO_LOGIN_OU_SENHA_INVALIDOS'}
        $apos=Get-WmsD22Metadados $Contexto
        if(-not $apos.aclValida -or -not $apos.arquivoExiste){throw 'D22_CREDENCIAL_METADADOS_MUDARAM'}
        $senha=$cred.Password.Copy();$senha.MakeReadOnly();$senha
    }catch{
        if($null -ne $senha){$senha.Dispose()}
        $msg=$_.Exception.Message
        if($msg -cmatch '^D22_[A-Z0-9_]+$'){throw $msg}
        throw 'D22_CREDENCIAL_DPAPI_INVALIDA_NESTE_USUARIO_MAQUINA'
    }finally{if($cred -is [Management.Automation.PSCredential]){$cred.Password.Dispose()}}
}
function New-WmsD22Security([string]$Sid,[bool]$Pasta){
    $a=if($Pasta){New-Object Security.AccessControl.DirectorySecurity}else{New-Object Security.AccessControl.FileSecurity}
    $a.SetOwner((New-Object Security.Principal.SecurityIdentifier($Sid)))
    $a.SetAccessRuleProtection($true,$false)
    $flags=if($Pasta){[Security.AccessControl.InheritanceFlags]'ContainerInherit,ObjectInherit'}else{[Security.AccessControl.InheritanceFlags]::None}
    foreach($s in @($Sid,'S-1-5-18')){
        $a.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule((New-Object Security.Principal.SecurityIdentifier($s)),[Security.AccessControl.FileSystemRights]::FullControl,$flags,[Security.AccessControl.PropagationFlags]::None,[Security.AccessControl.AccessControlType]::Allow)))
    }
    $a
}
function Initialize-WmsD22Privado($Contexto){
    Assert-WmsD22Caminho $Contexto
    $pai=Join-Path $Contexto.base 'Rodogarcia'
    if(-not (Test-Path -LiteralPath $pai)){$null=[IO.Directory]::CreateDirectory($pai)}
    foreach($p in @($Contexto.wms,$Contexto.pasta)){
        if(Test-Path -LiteralPath $p){
            $owner=(Get-Acl -LiteralPath $p).GetOwner([Security.Principal.SecurityIdentifier]).Value
            if($owner -cne $Contexto.sid){throw 'D22_SETUP_OWNER_DIVERGENTE_PRESERVADO'}
            [IO.Directory]::SetAccessControl($p,(New-WmsD22Security $Contexto.sid $true))
        }else{$null=[IO.Directory]::CreateDirectory($p,(New-WmsD22Security $Contexto.sid $true))}
        Assert-WmsD22Caminho $Contexto
    }
}
function Save-WmsD22Credencial($Contexto,[Security.SecureString]$Senha){
    # Somente configurador explicito do operador ou testes ficticios em raiz isolada.
    if($null -eq $Senha -or $Senha.Length -eq 0){throw 'D22_SETUP_SENHA_OCULTA_AUSENTE'}
    Initialize-WmsD22Privado $Contexto
    if(Test-Path -LiteralPath $Contexto.arquivo){
        if((Get-Acl -LiteralPath $Contexto.arquivo).GetOwner([Security.Principal.SecurityIdentifier]).Value -cne $Contexto.sid){throw 'D22_SETUP_OWNER_DIVERGENTE_PRESERVADO'}
        [IO.File]::SetAccessControl($Contexto.arquivo,(New-WmsD22Security $Contexto.sid $false))
    }
    $temp=Join-Path $Contexto.pasta ('credencial-'+[Guid]::NewGuid().ToString('N')+'.tmp.clixml')
    $copia=$Senha.Copy();$cred=New-Object Management.Automation.PSCredential('sa',$copia)
    try{
        $s=[IO.File]::Open($temp,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None);$s.Dispose()
        [IO.File]::SetAccessControl($temp,(New-WmsD22Security $Contexto.sid $false))
        $cred|Export-Clixml -LiteralPath $temp -Depth 3 -Force -ErrorAction Stop
        if(-not (Get-WmsD22AclMetadata $temp $Contexto.sid $false).valida){throw 'D22_SETUP_ACL_TEMP_INVALIDA'}
        Assert-WmsD22Caminho $Contexto
        if(Test-Path -LiteralPath $Contexto.arquivo){[IO.File]::Replace($temp,$Contexto.arquivo,[NullString]::Value)}
        else{[IO.File]::Move($temp,$Contexto.arquivo)}
        $m=Get-WmsD22Metadados $Contexto
        if(-not $m.aclValida){throw 'D22_SETUP_ACL_FINAL_INVALIDA'}
        [pscustomobject]@{natureza='D22_CONFIGURACAO_DPAPI_LOCAL';resultado='CONFIGURADA_USUARIO_MAQUINA_ATUAIS';arquivo=$Contexto.arquivo;aclValida=$true;sqlExecutado=$false}
    }finally{
        $copia.Dispose()
        if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp -Force}
    }
}
if($MyInvocation.InvocationName -ne '.'){
    if($Action -eq 'Metadados'){Get-WmsD22Metadados (Get-WmsD22Contexto)|ConvertTo-Json -Depth 6}
    else{[pscustomobject]@{natureza='D22_PLANO_OFFLINE_SEM_SEGREDO';arquivo='%LOCALAPPDATA%/Rodogarcia/WMS/database-runner/credencial-sa.clixml';login='sa';setup='database/configurar-credencial.bat';normal='SEM_PROMPTS';sqlExecutado=$false}|ConvertTo-Json}
}
