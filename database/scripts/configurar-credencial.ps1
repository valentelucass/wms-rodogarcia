[CmdletBinding()]
param([switch]$Offline)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd22-credencial.ps1')
if($Offline){[pscustomobject]@{natureza='D22_SETUP_OFFLINE_SEM_SEGREDO_SEM_ARQUIVO_PRIVADO';resultado='OFFLINE_OK';sqlExecutado=$false;credencialGravada=$false}|ConvertTo-Json;return}
$senha=$null
try{
    $contexto=Get-WmsD22Contexto
    Assert-WmsD22Caminho $contexto
    Write-Host 'WMS: configurar uma vez credencial sa deste usuario Windows nesta maquina.'
    Write-Host 'DPAPI + ACL usuario/SYSTEM. Nenhuma conexao SQL sera feita pelo configurador.'
    $senha=Read-Host 'Senha sa autorizada (oculta; nao cole em chat ou arquivo)' -AsSecureString
    Save-WmsD22Credencial $contexto $senha|ConvertTo-Json
    Write-Host 'Pronto. Agora executar database/iniciar-bancos.bat, sem perguntas.'
}catch{
    $codigo=if($_.Exception.Message -cmatch '^D22_[A-Z0-9_]+$'){$_.Exception.Message}else{'D22_SETUP_FALHOU_SEM_EXIBIR_DETALHES_PRIVADOS'}
    [Console]::Error.WriteLine($codigo+'; credencial antiga preservada quando disponivel; conferir owner/ACL/politica local.')
    exit 1
}finally{if($null -ne $senha){$senha.Dispose()}}
