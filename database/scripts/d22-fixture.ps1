# Canal ficticio explicito: chama o MESMO runner automatico. Nenhuma conexao SQL/Maven.
function New-WmsD22LauncherFixture([string]$Falha=''){
    $base=Join-Path ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))) ('evidencias/fixtures/d22-'+[Guid]::NewGuid().ToString('N'))
    $null=New-Item -ItemType Directory -Path $base -Force
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $contexto=[pscustomobject]@{base=$base;wms=(Join-Path $base 'Rodogarcia/WMS');pasta=(Join-Path $base 'Rodogarcia/WMS/database-runner');arquivo=(Join-Path $base 'Rodogarcia/WMS/database-runner/credencial-sa.clixml');sid=$sid}
    $s=New-Object Security.SecureString;foreach($c in [Guid]::NewGuid().ToString('N').ToCharArray()){$s.AppendChar($c)}
    try{if($Falha -cne 'AUSENTE'){$null=Save-WmsD22Credencial $contexto $s}}finally{$s.Dispose()}
    $catalogo=@([pscustomobject]@{categoria='T';tabela='ficticio';nome='';valor=''})
    $f=@{contexto=$contexto;falha=$Falha;eventos=(New-Object 'Collections.Generic.List[string]');copias=(New-Object 'Collections.Generic.List[object]');senha=$null;ddl=0;sql=0;catalogo=$catalogo}
    $a=@{}
    $a.Mostrar={param($Texto)}
    $a.Senha={
        $f.eventos.Add('Credencial')
        $f.senha=Read-WmsD22Credencial $f.contexto
        $f.senha
    }.GetNewClosure()
    $a.Ferramentas={$f.eventos.Add('Preflight')}.GetNewClosure()
    $a.Fontes={@([pscustomobject]@{arquivo='migrations/V1__fixture.sql';sha256='FICTICIO'})}
    $a.Expectativa={param($Fontes) $f.catalogo}.GetNewClosure()
    $a.Inspecao={param($Senha)
        $f.eventos.Add('Inspect');$f.copias.Add($Senha)
        if($f.falha -ceq 'IDENTIDADE'){throw 'D21_INSPECAO_IDENTIDADE_DIVERGENTE'}
        [pscustomobject]@{endpoint='127.0.0.1:1433';bancoInicial='master';loginCriacao='sa';servidorReal='FICTICIO';existentes=@()}
    }.GetNewClosure()
    $a.Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
        $f.eventos.Add($Acao+':'+$Banco);$f.copias.Add($Senha);$f.ddl++
        [pscustomobject]@{banco=$Banco;resultado='CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO';novo=$true;ddlSolicitado=$true;conteudoExistenteAlterado=$false;grantsExecutados=$false}
    }.GetNewClosure()
    $a.Flyway={param($Banco,$Servidor,$Senha,$Etapa)
        $f.eventos.Add($Etapa+':'+$Banco);$f.copias.Add($Senha)
        if($f.falha -ceq 'DEV' -and $Banco -ceq 'WMS_DEV' -and $Etapa -ceq 'Migrate'){return [pscustomobject]@{exitCode=1;resultado='HISTORICO_CHECKSUM_OU_MIGRATION';saidaBrutaDescartada=$true}}
        [pscustomobject]@{exitCode=0;resultado='GOAL_OK';saidaBrutaDescartada=$true}
    }.GetNewClosure()
    $a.Schema={param($Banco,$Servidor,$Senha)
        $f.eventos.Add('Schema:'+ $Banco);$f.copias.Add($Senha)
        [pscustomobject]@{banco=$Banco;metadataCompleta=1;schemaWms=1;tabelas=1;colunas=1;historico=1;falhasHistorico=0;restricoesInvalidas=0;indicesDesabilitados=0;catalogo=$f.catalogo}
    }.GetNewClosure()
    @{adaptadores=$a;estado=$f}
}
