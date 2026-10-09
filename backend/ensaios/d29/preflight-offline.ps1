$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
. "$PSScriptRoot/guardas.ps1"
$source=Join-Path $root 'orchestracao/.runtime/d29-prumo-preflight.json'
$public=Get-Content -LiteralPath $source -Raw -Encoding UTF8|ConvertFrom-Json
$sourceHash=(Get-FileHash -LiteralPath $source).Hash
$cases=New-Object Collections.Generic.List[object]
Assert-D29Preflight $public
$cases.Add([ordered]@{caso='controle publico D29 atual';esperado='aceito';obtido='aceito';aprovado=$true})
$variants=@(
 @{path='database';value='WMS_PROD'},@{path='database';value='OUTRO'},@{path='identity';value='sa'},@{path='identity';value='wmsdev'},
 @{path='alvo.banco';value='WMS_PROD'},@{path='alvo.login';value='sa'},@{path='alvo.usuario';value='dbo'},@{path='alvo.servidor';value='OUTRO'},
 @{path='guardasValidas';value=$false},@{path='tls.mandatory';value=$false},@{path='tls.trustServerCertificate';value=$true},@{path='prodConectado';value=$true},
 @{path='database';value=$null},@{path='identity';value=$null},@{path='guardasValidas';value=$null}
)
foreach($v in $variants){
 $fixture=($public|ConvertTo-Json -Depth 40)|ConvertFrom-Json
 $parts=$v.path.Split('.');$obj=$fixture
 for($i=0;$i -lt $parts.Length-1;$i++){$obj=$obj.($parts[$i])}
 $obj.($parts[-1])=$v.value
 $got='SEM_RECUSA';try{Assert-D29Preflight $fixture}catch{$got=$_.Exception.Message}
 $pass=$got -ceq 'D29_PREFLIGHT_PRUMO_RECUSADO'
 $cases.Add([ordered]@{caso=$v.path;insumoFicticio=$v.value;esperado='D29_PREFLIGHT_PRUMO_RECUSADO';obtido=$got;aprovado=$pass})
 if(-not $pass){throw 'D29_OFFLINE_PREFLIGHT_VARIANTE_FALHOU'}
}
$old=($public|ConvertTo-Json -Depth 40)|ConvertFrom-Json
$auth=(Get-Item -LiteralPath (Join-Path $root 'orchestracao/.runtime/d29-autorizacao-e-distribuicao.md')).LastWriteTimeUtc
$old.observadoEm=$auth.AddSeconds(-1).ToString('o')
$got='SEM_RECUSA';try{Assert-D29Preflight $old}catch{$got=$_.Exception.Message}
if($got -cne 'D29_PREFLIGHT_ANTERIOR_AUTORIZACAO_RECUSADO'){throw 'D29_OFFLINE_PREFLIGHT_ANTIGO_FALHOU'}
$cases.Add([ordered]@{caso='notificacao anterior nao inicia D29';esperado='D29_PREFLIGHT_ANTERIOR_AUTORIZACAO_RECUSADO';obtido=$got;aprovado=$true})
$legacy=[pscustomobject]@{DB_NAME='WMS_DEV';login='WMSDEV';guardasValidas=$true}
$got='SEM_RECUSA';try{Assert-D29Preflight $legacy}catch{$got=$_.Exception.Message}
if($got -cne 'D29_PREFLIGHT_PRUMO_RECUSADO'){throw 'D29_OFFLINE_PREFLIGHT_ESQUEMA_LEGADO_FALHOU'}
$cases.Add([ordered]@{caso='esquema legado sem alvo completo';esperado='D29_PREFLIGHT_PRUMO_RECUSADO';obtido=$got;aprovado=$true})
$version=Get-Content -LiteralPath (Join-Path $root 'backend/target-d29-helper/versao-atual.json') -Raw -Encoding UTF8|ConvertFrom-Json
$doc=[ordered]@{demanda='D29';observadoUtc=[DateTime]::UtcNow.ToString('o');tipo='OFFLINE_PUBLICO';helperHash=$version.hashConjunto;preflightFonteSHA256=$sourceHash;checks=$cases.Count;falhas=0;SQL=$false;HTTP=$false;segredos=$false;listener=$false;cases=$cases}
[IO.File]::WriteAllText((Join-Path $root "backend/evidencias/d29-preflight-offline-$($version.hashConjunto).json"),($doc|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "D29 preflight offline $($cases.Count)/0; sem SQL/HTTP"
