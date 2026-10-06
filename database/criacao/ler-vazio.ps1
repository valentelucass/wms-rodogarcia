# Recebe sessao existente; nao abre conexao. Uso exclusivamente pelo executor CREATE.
function Read-WmsD20Vazio($Conexao,[string]$Banco) {
    $Conexao.ChangeDatabase($Banco)
    $cmd=$Conexao.CreateCommand();$cmd.CommandTimeout=15
    $cmd.CommandText=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'verificar-vazio.sql'))
    try{
        $r=$cmd.ExecuteReader()
        try{
            if(-not $r.Read()){throw 'D20_METADADOS_INSUFICIENTES'}
            $e=[ordered]@{}
            for($i=0;$i -lt $r.FieldCount;$i++){$e[$r.GetName($i)]=if($r.IsDBNull($i)){$null}else{$r.GetValue($i)}}
            [pscustomobject]$e
        }finally{$r.Close()}
    }finally{$cmd.Dispose();$Conexao.ChangeDatabase('master')}
}
