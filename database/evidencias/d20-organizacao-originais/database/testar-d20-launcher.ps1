[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'iniciar-bancos.ps1')
$casos=New-Object 'Collections.Generic.List[object]'
function Caso([string]$Nome,[scriptblock]$Teste){
    $erro=$null;try{& $Teste}catch{$erro=$_.Exception.Message}
    $casos.Add([pscustomobject]@{caso=$Nome;aprovado=($null -eq $erro);erro=$erro})
}
function Exigir([bool]$Condicao,[string]$Mensagem='Fixture divergente'){if(-not $Condicao){throw $Mensagem}}
function NovaFixture([string[]]$Existentes=@(),[string]$Falha='', [bool]$Confirmar=$true,[string]$Alteracao=''){
    $s=New-Object Security.SecureString;foreach($n in @(70,73,67,84,73,67,73,79)){$s.AppendChar([char]$n)}
    $f=@{senha=$s;existentes=$Existentes;falha=$Falha;confirmar=$Confirmar;alteracao=$Alteracao;
        eventos=(New-Object 'Collections.Generic.List[string]');copias=(New-Object 'Collections.Generic.List[object]');ddlSimulado=0;dadosExistentes=42}
    $a=@{}
    $a.Senha={ $f.senha }.GetNewClosure()
    $a.Inspecao={param($Senha)
        $f.eventos.Add('INSPECAO');$f.copias.Add($Senha);Exigir ($Senha.Length -gt 0)
        if($f.falha -eq 'CONEXAO'){throw 'D20_INSPECAO_FALHOU_TLS_ACESSO_IDENTIDADE_CONFERIR_SEM_BYPASS'}
        if($f.falha -eq 'TLS'){throw 'D20_TLS_FALHOU_SEM_BYPASS'}
        $bs=@();foreach($n in $f.existentes){$bs+=[pscustomobject]@{banco=$n;estado=0;online=$true}}
        $i=[pscustomobject]@{endpoint='127.0.0.1:1433';bancoInicial='master';loginCriacao='sa';servidorReal='FICTICIO';existentes=$bs}
        switch($f.alteracao){'IDENTIDADE'{$i.loginCriacao='FICTICIO'};'ESTADO'{$i.existentes[0].online=$false;$i.existentes[0].estado=1};'METADATA'{$i.existentes[0].estado=$null};'SERVIDOR'{$i.servidorReal=''}}
        $i
    }.GetNewClosure()
    $a.Confirmar={param($Info) $f.eventos.Add('CONFIRMACAO');$f.confirmar}.GetNewClosure()
    $a.Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
        $f.eventos.Add($Acao+':'+$Banco);$f.copias.Add($Senha)
        Exigir ($Senha.Length -gt 0 -and $f.senha.Length -gt 0 -and $Servidor -ceq 'FICTICIO' -and $Alvo -ceq ('127.0.0.1:1433/'+$Banco))
        if($f.falha -eq $Banco){throw 'D20_BANCO_FALHOU_CREATE_GUARDA_OU_CONFIRMACAO'}
        $Acao=Get-WmsD20AcaoObservada $Acao ($Banco -cin $f.existentes -or $f.falha -eq 'CORRIDA')
        $m=[pscustomobject]@{banco=$Banco;metadataCompleta=1;objetos=64;tipos=1;schemas=1;principais=2;membros=2;permissoes=12}
        if($f.falha -eq 'METADADOS'){$m.metadataCompleta=0}
        if($Acao -eq 'Check'){Assert-WmsD20MetadadosExistente $m $Banco}
        if($Acao -eq 'Create'){$f.ddlSimulado++}
        [pscustomobject]@{banco=$Banco;resultado=if($Acao -eq 'Check'){'EXISTENTE_PRESERVADO_CONFERIDO'}else{'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'};
            migrationsExecutadas=$false;cargasExecutadas=$false;grantsExecutados=$false;novo=($Acao -eq 'Create');ddlSolicitado=($Acao -eq 'Create');conteudoExistenteAlterado=$false}
    }.GetNewClosure()
    $a.Mostrar={param($Texto)}
    @{estado=$f;adaptadores=$a}
}
function Executar($Fixture){Invoke-WmsD20InicioManual -Adaptadores $Fixture.adaptadores}
function Disposto([Security.SecureString]$S){try{$tmp=$S.Copy();$tmp.Dispose();return $false}catch{$e=$_.Exception;while($e.InnerException){$e=$e.InnerException};return ($e -is [ObjectDisposedException])}}
function ConferirDescarte($F){Exigir (Disposto $F.estado.senha);foreach($s in $F.estado.copias){Exigir (Disposto $s)}}
Caso 'offline nao chama prompt segredo conector ou DDL' {
    $x=Invoke-WmsD20InicioManual -Offline -Adaptadores @{Senha={throw 'chamada indevida'};Inspecao={throw 'chamada indevida'};Banco={throw 'chamada indevida'}}
    Exigir ($x.resultado -ceq 'OFFLINE_OK' -and $x.senhaLida -eq $false -and $x.conexoes -eq 0 -and $x.ddl -eq 0)
}
Caso 'sucesso DEV e PROD inexistentes' {$f=NovaFixture;$r=Executar $f;Exigir ($r.resultado -ceq 'BANCOS_CONFIRMADOS_NOVOS_VAZIOS_EXISTENTES_PRESERVADOS' -and $f.estado.ddlSimulado -eq 2);ConferirDescarte $f}
Caso 'ordem inspecao confirmacao DEV primeiro PROD depois' {$f=NovaFixture;$r=Executar $f;Exigir (($f.estado.eventos -join ',') -ceq 'INSPECAO,CONFIRMACAO,Create:WMS_DEV,Create:WMS_PROD')}
Caso 'ambos existentes com schema e dados Check preserva sem CREATE' {$f=NovaFixture @('WMS_DEV','WMS_PROD');$r=Executar $f;Exigir ($r.etapas.Count -eq 2 -and $f.estado.ddlSimulado -eq 0 -and $f.estado.dadosExistentes -eq 42 -and ($f.estado.eventos -join ',') -ceq 'INSPECAO,CONFIRMACAO,Check:WMS_DEV,Check:WMS_PROD');ConferirDescarte $f}
Caso 'retoma DEV com schema e dados PROD faltante' {$f=NovaFixture @('WMS_DEV');$r=Executar $f;Exigir ($r.etapas.Count -eq 2 -and $f.estado.ddlSimulado -eq 1 -and $f.estado.dadosExistentes -eq 42 -and ($f.estado.eventos -join ',') -ceq 'INSPECAO,CONFIRMACAO,Check:WMS_DEV,Create:WMS_PROD');ConferirDescarte $f}
Caso 'PROD existente DEV faltante preserva PROD' {$f=NovaFixture @('WMS_PROD');$r=Executar $f;Exigir (($f.estado.eventos -join ',') -ceq 'INSPECAO,CONFIRMACAO,Create:WMS_DEV,Check:WMS_PROD')}
Caso 'conexao falha antes de DDL' {$f=NovaFixture -Falha CONEXAO;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $r.fase -ceq 'INSPECAO_LEITURA' -and $f.estado.ddlSimulado -eq 0);ConferirDescarte $f}
Caso 'TLS falha sem fallback' {$f=NovaFixture -Falha TLS;$r=Executar $f;Exigir ($r.codigo -ceq 'D20_TLS_FALHOU_SEM_BYPASS' -and $f.estado.eventos.Count -eq 1 -and $f.estado.ddlSimulado -eq 0);ConferirDescarte $f}
Caso 'operador declina depois de somente leitura' {$f=NovaFixture -Confirmar $false;$r=Executar $f;Exigir ($r.resultado -ceq 'CANCELADO_SEM_DDL' -and $f.estado.eventos.Count -eq 2 -and $f.estado.ddlSimulado -eq 0);ConferirDescarte $f}
Caso 'falha DEV impede PROD' {$f=NovaFixture -Falha WMS_DEV;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $r.etapas.Count -eq 0 -and $f.estado.eventos.Count -eq 3);ConferirDescarte $f}
Caso 'falha PROD conserva sucesso DEV sem limpeza ou retry' {$f=NovaFixture -Falha WMS_PROD;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $r.etapas.Count -eq 1 -and $f.estado.ddlSimulado -eq 1 -and $f.estado.eventos.Count -eq 4);ConferirDescarte $f}
Caso 'existente metadados insuficientes preserva e interrompe' {$f=NovaFixture @('WMS_DEV') -Falha METADADOS;$r=Executar $f;Exigir ($r.codigo -ceq 'D20_METADADOS_INSUFICIENTES' -and $f.estado.ddlSimulado -eq 0 -and $r.explicacao -match 'preservado');ConferirDescarte $f}
foreach($alteracao in @('IDENTIDADE','ESTADO','METADATA','SERVIDOR')){
    Caso ('inspecao divergente '+$alteracao+' sem DDL') {$f=NovaFixture @('WMS_DEV') -Alteracao $alteracao;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $f.estado.eventos.Count -eq 1 -and $f.estado.ddlSimulado -eq 0);ConferirDescarte $f}
}
Caso 'nome existente com caixa divergente preserva e recusa' {$f=NovaFixture @('wms_dev');$r=Executar $f;Exigir ($r.codigo -ceq 'D20_EXISTENTE_NOME_OU_ESTADO_DIVERGENTE' -and $f.estado.ddlSimulado -eq 0)}
Caso 'copia propria em cada etapa e original descartado somente ao fim' {$f=NovaFixture;$r=Executar $f;Exigir ($f.estado.copias.Count -eq 3);Exigir (-not [object]::ReferenceEquals($f.estado.copias[0],$f.estado.copias[1]));Exigir (-not [object]::ReferenceEquals($f.estado.copias[1],$f.estado.copias[2]));ConferirDescarte $f}
Caso 'corrida nomes aparecem segunda consulta Create vira Check ddl0' {$f=NovaFixture -Falha CORRIDA;$r=Executar $f;Exigir ($r.resultado -ceq 'BANCOS_CONFIRMADOS_NOVOS_VAZIOS_EXISTENTES_PRESERVADOS' -and $f.estado.ddlSimulado -eq 0 -and $r.etapas[0].acao -ceq 'Check' -and $r.etapas[1].acao -ceq 'Check');ConferirDescarte $f}
Caso 'bat nao relaxa politica TLS ou instala' {$s=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'iniciar-bancos.bat'));Exigir ($s -notmatch 'ExecutionPolicy|Bypass|install|sqlcmd' -and $s -match '--offline' -and $s -match 'pause');foreach($nome in @('criar-bancos.ps1','verificar-alvo-criacao.ps1')){$t=[IO.File]::ReadAllText((Join-Path $PSScriptRoot $nome));Exigir ($t -match 'Encrypt=\$true' -and $t -match 'TrustServerCertificate=\$false')}}
Caso 'primitivo Check nao alcança ExecuteNonQuery fora da guarda Create' {
    $tokens=$null;$erros=$null;$ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'criar-bancos.ps1'),[ref]$tokens,[ref]$erros)
    $writes=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.InvokeMemberExpressionAst] -and $n.Member.Value -ceq 'ExecuteNonQuery'},$true))
    Exigir ($writes.Count -eq 1)
    foreach($w in $writes){$p=$w.Parent;$guardado=$false;while($p){if($p -is [Management.Automation.Language.IfStatementAst] -and $p.Clauses[0].Item1.Extent.Text -match '\$Action -eq ''Create'''){$guardado=$true;break};$p=$p.Parent};Exigir $guardado}
    $s=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'criacao/verificar-vazio.sql')) -replace '(?m)--.*$',''
    Exigir ($s -notmatch '\b(CREATE|ALTER|DROP|INSERT|UPDATE|DELETE|GRANT|RESTORE)\b')
}
$falhas=@($casos|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='FIXTURES_ORQUESTRACAO_SEM_SEGREDO_REAL_SEM_SQL';total=$casos.Count;aprovados=$casos.Count-$falhas.Count;falhas=$falhas.Count;casos=$casos.ToArray()}|ConvertTo-Json -Depth 6
if($falhas.Count){exit 1}
