[CmdletBinding()]
param([ValidateSet('Plan','Metadados','Constraints')][string]$Action='Plan')
$WmsD20Pathensaiolocald20=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathensaiolocald20 'scripts/d20-guardas.ps1')
if($Action -eq 'Plan'){
    [pscustomobject]@{natureza='PLANO_OFFLINE';alvo='WMS_DEV_LOOPBACK_ISOLADO_FICTICIO';
        metadados='ensaios/d20-metadados.sql';constraints='ensaios/d20-constraints.sql';
        ddl=$false;grants=$false;dadosConfirmados=$false;sqlExecutado=$false}|ConvertTo-Json;return
}
foreach($n in @('WMS_DB_HOST','WMS_DB_PORT','WMS_DB_NAME','WMS_DB_USER','WMS_DB_PASSWORD',
    'WMS_DB_CONFIRMED_TARGET','WMS_DB_CONFIRMED_SERVER','WMS_D20_ISOLADO')){
    if([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($n))){throw "D20_VARIAVEL_AUSENTE_$n"}
}
Assert-WmsD20Alvo $env:WMS_DB_HOST $env:WMS_DB_PORT $env:WMS_DB_NAME $env:WMS_DB_CONFIRMED_TARGET $env:WMS_DB_CONFIRMED_SERVER 'DiagnosticarLocal'
if($env:WMS_D20_ISOLADO -cne 'WMS_DEV_LOCAL_SOMENTE_FICTICIO'){throw 'D20_ISOLAMENTO_NAO_CONFIRMADO'}
$b=New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$b.DataSource="$($env:WMS_DB_HOST),$($env:WMS_DB_PORT)";$b.InitialCatalog='WMS_DEV'
$b.UserID=$env:WMS_DB_USER;$b.Password=$env:WMS_DB_PASSWORD;$b.Encrypt=$true;$b.TrustServerCertificate=$false;$b.ConnectTimeout=10
$c=New-Object System.Data.SqlClient.SqlConnection($b.ConnectionString)
$fase='CONEXAO';$falha=$null;$datasets=@()
try{
    $c.Open();$cmd=$c.CreateCommand();$cmd.CommandTimeout=30
    $cmd.CommandText=(Get-WmsD20SessaoSql)+" SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')),DB_NAME(),ORIGINAL_LOGIN()"
    $r=$cmd.ExecuteReader();if(-not $r.Read()){throw 'D20_IDENTIDADE_AUSENTE'}
    $srv=$r.GetString(0);$db=$r.GetString(1);$login=$r.GetString(2);$r.Close()
    Assert-WmsD20Identidade $srv $db $env:WMS_DB_CONFIRMED_SERVER 'WMS_DEV'
    if($login -cne $env:WMS_DB_USER){throw 'D20_LOGIN_REAL_DIVERGENTE'}
    if($Action -eq 'Constraints'){
        $fase='PREFLIGHT_APP_FICTICIA_VAZIA'
        $cmd.CommandText="SELECT IS_SRVROLEMEMBER(N'sysadmin'),IS_MEMBER(N'db_owner'),COUNT_BIG(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history'"
        $r=$cmd.ExecuteReader();if(-not $r.Read() -or $r.IsDBNull(0) -or $r.IsDBNull(1) -or $r.GetInt32(0) -ne 0 -or $r.GetInt32(1) -ne 0 -or $r.GetInt64(2) -ne 64){throw 'D20_SCHEMA_OU_IDENTIDADE_INAPTO'};$r.Close()
        $a=Get-Content -LiteralPath (Join-Path $WmsD20Pathensaiolocald20 'evidencias/d20-auditoria-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
        $guardas=@()
        foreach($t in $a.schema.tabelas.PSObject.Properties.Name){
            if($t -notmatch '^[a-z][a-z0-9_]*$'){throw 'D20_SCHEMA_INVENTARIO_INVALIDO'}
            $guardas+="IF EXISTS(SELECT 1 FROM wms.[$t]) THROW 51033,'D20 exige todas tabelas ficticias vazias.',1;"
        }
        if($guardas.Count -ne 64){throw 'D20_INVENTARIO_INCOMPLETO'}
        $cmd.CommandText=$guardas -join ' ';$null=$cmd.ExecuteNonQuery()
    }
    $fase='SQL_'+$Action
    $cmd.CommandText=[IO.File]::ReadAllText((Join-Path $WmsD20Pathensaiolocald20 ('ensaios/d20-'+$(if($Action -eq 'Metadados'){'metadados'}else{'constraints'})+'.sql')))
    if($Action -eq 'Metadados'){$null=$cmd.Parameters.Add('@ServidorEsperado',[System.Data.SqlDbType]::NVarChar,128);$cmd.Parameters['@ServidorEsperado'].Value=$env:WMS_DB_CONFIRMED_SERVER}
    $r=$cmd.ExecuteReader()
    do{
        $rows=@()
        while($r.Read()){
            $row=[ordered]@{}
            for($i=0;$i -lt $r.FieldCount;$i++){$row[$r.GetName($i)]=if($r.IsDBNull($i)){$null}else{$r.GetValue($i)}}
            $rows+=[pscustomobject]$row
        }
        $datasets+=,[object[]]$rows
    }while($r.NextResult());$r.Close()
    if($Action -eq 'Constraints'){
        $fase='POS_ROLLBACK_SEM_DADOS';$cmd.CommandText=$guardas -join ' ';$null=$cmd.ExecuteNonQuery()
    }
}catch{
    $e=$_.Exception;while($e.InnerException){$e=$e.InnerException}
    $falha=if($e -is [System.Data.SqlClient.SqlException]){$e.Number}else{'GUARDA_OU_CONFIRMACAO'}
}finally{$c.Dispose();$b.Password=''}
$relato=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');natureza='ENSAIO_SQL_SERVER_LOCAL_REAL';
    acao=$Action;banco='WMS_DEV';fase=$fase;falha=$falha;datasets=$datasets;ddl=$false;grants=$false}
$p=Join-Path $WmsD20Pathensaiolocald20 ('evidencias/d20-sql-local-'+[Guid]::NewGuid().ToString('N')+'.json')
$relato|ConvertTo-Json -Depth 14|Set-Content -Encoding UTF8 -LiteralPath $p
if($null -ne $falha){throw 'D20_ENSAIO_FALHOU_CONFERIR_EVIDENCIA'}
[pscustomobject]@{resultado='CONCLUIDO';evidencia=$p}|ConvertTo-Json
