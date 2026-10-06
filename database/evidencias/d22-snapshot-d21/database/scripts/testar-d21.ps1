[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$Db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $PSScriptRoot 'iniciar-bancos.ps1')
$Casos=New-Object 'Collections.Generic.List[object]'
function Caso([string]$Nome,[scriptblock]$Teste){$erro=$null;try{& $Teste}catch{$erro=$_.Exception.Message};$Casos.Add([pscustomobject]@{caso=$Nome;aprovado=($null -eq $erro);erro=$erro})}
function Exigir([bool]$Ok){if(-not $Ok){throw 'D21_FIXTURE_DIVERGENTE'}}
function Recusa([scriptblock]$Acao,[string]$Codigo){$falhou=$false;try{& $Acao}catch{$falhou=$_.Exception.Message -like ('*'+$Codigo+'*')};Exigir $falhou}
function NovaSenha {$s=New-Object Security.SecureString;foreach($c in [Guid]::NewGuid().ToString('N').ToCharArray()){$s.AppendChar($c)};$s}
function Disposto([Security.SecureString]$S){try{$x=$S.Copy();$x.Dispose();$false}catch{$e=$_.Exception;while($e.InnerException){$e=$e.InnerException};$e -is [ObjectDisposedException]}}
$Toy=@([pscustomobject]@{arquivo='V1__fixture.sql';texto='CREATE TABLE wms.ficticio (id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_ficticio PRIMARY KEY, quantidade decimal(19,6) NOT NULL, CONSTRAINT ck_ficticio CHECK (quantidade > 0));'})
$CatalogoToy=@(Get-WmsD21CatalogoEsperado $Toy)
function NovaFixture([string[]]$Existentes=@(),[string]$Falha='', [string]$Motivo='HISTORICO_CHECKSUM_OU_MIGRATION',[bool]$Confirmar=$true){
    $f=@{senha=(NovaSenha);copias=(New-Object 'Collections.Generic.List[object]');eventos=(New-Object 'Collections.Generic.List[string]');
        existentes=$Existentes;falha=$Falha;motivo=$Motivo;confirmar=$Confirmar;ddl=0;fatos=42;catalogo=$CatalogoToy;
        fontes=@([pscustomobject]@{arquivo='migrations/V1__fixture.sql';sha256='FICTICIO'});aplicadas=@{WMS_DEV=0;WMS_PROD=0};pendentes=9;migrations=0;leiturasFonte=0;parcial=0}
    $a=@{}
    $a.Mostrar={param($Texto)}
    $a.Ferramentas={if($f.falha -ceq 'FERRAMENTAS'){throw 'D21_PROFILE_CEDRO_AUSENTE'}}.GetNewClosure()
    $a.Fontes={$f.leiturasFonte++;if($f.falha -ceq 'FONTES' -and $f.leiturasFonte -gt 3){return @([pscustomobject]@{arquivo='outra';sha256='ALTERADO'})};$f.fontes}.GetNewClosure()
    $a.Expectativa={param($Fs) $f.catalogo}.GetNewClosure()
    $a.Senha={$f.senha}.GetNewClosure()
    $a.Inspecao={param($Senha)
        $f.eventos.Add('Inspect');$f.copias.Add($Senha)
        if($f.falha -ceq 'TLS'){throw 'D21_TLS_INSPECAO_FALHOU'}
        $bs=@($f.existentes|ForEach-Object {[pscustomobject]@{banco=$_;estado=0;online=$true}})
        $info=[pscustomobject]@{endpoint='127.0.0.1:1433';bancoInicial='master';loginCriacao='sa';servidorReal='FICTICIO';existentes=$bs}
        if($f.falha -ceq 'IDENTIDADE'){$info.loginCriacao='outro'}
        if($f.falha -ceq 'ESTADO'){$info.existentes[0].estado=1;$info.existentes[0].online=$false}
        $info
    }.GetNewClosure()
    $a.Confirmar={param($Info) $f.eventos.Add('Confirm');$f.confirmar}.GetNewClosure()
    $a.Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
        $f.eventos.Add($Acao+':'+$Banco);$f.copias.Add($Senha);Exigir ($Senha.Length -gt 0 -and $f.senha.Length -gt 0 -and $Servidor -ceq 'FICTICIO' -and $Alvo -ceq ('127.0.0.1:1433/'+$Banco))
        if($f.falha -ceq ($Banco+'_CreateCheck')){throw 'D21_PRIMITIVE_FALHOU'}
        $Acao=Get-WmsD20AcaoObservada $Acao ($Banco -cin $f.existentes -or $f.falha -ceq 'CORRIDA')
        if($Acao -ceq 'Create'){$f.ddl++;$f.existentes+=@($Banco)}
        [pscustomobject]@{banco=$Banco;resultado=if($Acao -ceq 'Check'){'EXISTENTE_PRESERVADO_CONFERIDO'}else{'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'};
            novo=($Acao -ceq 'Create');ddlSolicitado=($Acao -ceq 'Create');conteudoExistenteAlterado=$false;grantsExecutados=$false}
    }.GetNewClosure()
    $a.Flyway={param($Banco,$Servidor,$Senha,$Etapa)
        $f.eventos.Add($Etapa+':'+$Banco);$f.copias.Add($Senha);Exigir ($Senha.Length -gt 0 -and $f.senha.Length -gt 0)
        if($f.falha -ceq ($Banco+'_'+$Etapa)){
            if($Etapa -ceq 'Migrate' -and $f.parcial -gt 0){$f.aplicadas[$Banco]+=$f.parcial;$f.migrations+=$f.parcial}
            return [pscustomobject]@{exitCode=1;resultado=$f.motivo;saidaBrutaDescartada=$true}
        }
        if($Etapa -ceq 'Migrate'){$f.migrations+=($f.pendentes-$f.aplicadas[$Banco]);$f.aplicadas[$Banco]=$f.pendentes}
        [pscustomobject]@{exitCode=0;resultado='GOAL_OK';saidaBrutaDescartada=$true}
    }.GetNewClosure()
    $a.Schema={param($Banco,$Servidor,$Senha)
        $f.eventos.Add('Schema:'+ $Banco);$f.copias.Add($Senha)
        $cat=@($f.catalogo|ForEach-Object {[pscustomobject]@{categoria=$_.categoria;tabela=$_.tabela;nome=$_.nome;valor=$_.valor}})
        $r=[pscustomobject]@{banco=$Banco;metadataCompleta=1;schemaWms=1;tabelas=1;colunas=2;historico=$f.aplicadas[$Banco];falhasHistorico=0;restricoesInvalidas=0;indicesDesabilitados=0;catalogo=$cat}
        if($f.falha -ceq 'CATALOGO'){$r.catalogo=@($cat|Where-Object {$_.nome -cne 'quantidade'})}
        if($f.falha -ceq 'METADATA'){$r.metadataCompleta=0}
        if($f.falha -ceq 'RESTRICAO'){$r.restricoesInvalidas=1}
        $r
    }.GetNewClosure()
    @{estado=$f;adaptadores=$a}
}
function Executar($F){Invoke-WmsD21InicioManual -Adaptadores $F.adaptadores}
function Descartes($F){Exigir (Disposto $F.estado.senha);foreach($c in $F.estado.copias){Exigir (Disposto $c)}}
function SemProd($F){Exigir (@($F.estado.eventos|Where-Object {$_ -match 'WMS_PROD'}).Count -eq 0)}
Caso 'Offline nao toca fonte ambiente prompt segredo SQL ou Maven' {$r=Invoke-WmsD21InicioManual -Offline -Adaptadores @{Ferramentas={throw 'indevido'};Senha={throw 'indevido'}};Exigir ($r.resultado -ceq 'OFFLINE_OK' -and $r.senhaLida -eq $false -and $r.ddl -eq 0 -and $r.migrationsExecutadas -eq 0)}
Caso 'Ausentes CREATE e upgrades ambos DEV completo antes PROD' {$f=NovaFixture;$r=Executar $f;Exigir ($r.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.ddl -eq 2 -and $f.estado.migrations -eq 18);Exigir (($f.estado.eventos -join ',') -ceq 'Inspect,Confirm,Create:WMS_DEV,PreValidate:WMS_DEV,Migrate:WMS_DEV,PostValidate:WMS_DEV,Info:WMS_DEV,Schema:WMS_DEV,Create:WMS_PROD,PreValidate:WMS_PROD,Migrate:WMS_PROD,PostValidate:WMS_PROD,Info:WMS_PROD,Schema:WMS_PROD');Descartes $f}
Caso 'Existentes preservados e pendentes sem CREATE' {$f=NovaFixture @('WMS_DEV','WMS_PROD');$f.estado.aplicadas.WMS_DEV=7;$f.estado.aplicadas.WMS_PROD=8;$r=Executar $f;Exigir ($r.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.ddl -eq 0 -and $f.estado.fatos -eq 42 -and $f.estado.migrations -eq 3);Descartes $f}
Caso 'DEV existe PROD falta retomada' {$f=NovaFixture @('WMS_DEV');$f.estado.aplicadas.WMS_DEV=9;$r=Executar $f;Exigir ($r.devConcluido -and $f.estado.ddl -eq 1 -and $f.estado.migrations -eq 9)}
Caso 'PROD existe DEV falta DEV primeiro sem recriar PROD' {$f=NovaFixture @('WMS_PROD');$r=Executar $f;Exigir ($r.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.ddl -eq 1)}
Caso 'Reexecucao sem pendentes sem novo CREATE' {$f=NovaFixture;$r=Executar $f;$orig=$f.estado.senha;$f.estado.senha=NovaSenha;$r2=Executar $f;Exigir ($r2.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.ddl -eq 2 -and $f.estado.migrations -eq 18 -and (Disposto $orig));Descartes $f}
Caso 'Queda DEV parcial persistida reexecuta restante nova senha PROD intocado na falha' {
    $f=NovaFixture -Falha WMS_DEV_Migrate -Motivo CONEXAO_INTERROMPIDA_OU_RECUSADA;$f.estado.parcial=4
    $r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $f.estado.aplicadas.WMS_DEV -eq 4 -and $f.estado.aplicadas.WMS_PROD -eq 0 -and $f.estado.ddl -eq 1);SemProd $f
    $orig=$f.estado.senha;$corte=$f.estado.eventos.Count;$f.estado.senha=NovaSenha;$f.estado.falha='';$f.estado.parcial=0
    $r2=Executar $f;Exigir ($r2.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.aplicadas.WMS_DEV -eq 9 -and $f.estado.aplicadas.WMS_PROD -eq 9 -and $f.estado.migrations -eq 18 -and $f.estado.ddl -eq 2 -and (Disposto $orig))
    Exigir (@($f.estado.eventos|Select-Object -Skip $corte|Where-Object {$_ -ceq 'Create:WMS_DEV'}).Count -eq 0)
    Descartes $f
}
Caso 'DEV completo PROD parcial mesma situacao retoma restante sem CREATE' {
    $f=NovaFixture -Falha WMS_PROD_Migrate -Motivo CONEXAO_INTERROMPIDA_OU_RECUSADA;$f.estado.parcial=2
    $r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $r.devConcluido -and $f.estado.aplicadas.WMS_DEV -eq 9 -and $f.estado.aplicadas.WMS_PROD -eq 2 -and $f.estado.ddl -eq 2)
    $orig=$f.estado.senha;$corte=$f.estado.eventos.Count;$f.estado.senha=NovaSenha;$f.estado.falha='';$f.estado.parcial=0
    $r2=Executar $f;Exigir ($r2.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.migrations -eq 18 -and $f.estado.ddl -eq 2 -and $f.estado.fatos -eq 42 -and (Disposto $orig))
    Exigir (@($f.estado.eventos|Select-Object -Skip $corte|Where-Object {$_ -like 'Create:*'}).Count -eq 0)
    Descartes $f
}
Caso 'Corrida vira Check ddl0' {$f=NovaFixture -Falha CORRIDA;$r=Executar $f;Exigir ($r.devConcluido -and $f.estado.ddl -eq 0 -and $r.etapas[0].etapa -ceq 'Check')}
Caso 'Declinou somente leitura nenhum DDL migrate' {$f=NovaFixture -Confirmar $false;$r=Executar $f;Exigir ($r.resultado -ceq 'CANCELADO_SEM_DDL' -and $f.estado.ddl -eq 0 -and $f.estado.migrations -eq 0);Descartes $f}
foreach($falha in @('WMS_DEV_CreateCheck','WMS_DEV_PreValidate','WMS_DEV_Migrate','WMS_DEV_PostValidate','WMS_DEV_Info','CATALOGO','METADATA','RESTRICAO','TLS','IDENTIDADE','FONTES')){
    Caso ('Falha '+$falha+' bloqueia qualquer PROD') {$f=NovaFixture @('WMS_DEV') -Falha $falha;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and -not $r.devConcluido);SemProd $f;Descartes $f}
}
Caso 'Estado existente divergente sem DDL' {$f=NovaFixture @('WMS_DEV') -Falha ESTADO;$r=Executar $f;Exigir ($r.codigo -ceq 'D21_EXISTENTE_NOME_ESTADO_DIVERGENTE' -and $f.estado.ddl -eq 0);SemProd $f}
Caso 'Erro PROD preserva sucesso DEV sem clean retry' {$f=NovaFixture -Falha WMS_PROD_Migrate;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $r.devConcluido -and $f.estado.migrations -eq 9 -and $r.repair -eq $false -and $r.clean -eq $false)}
Caso 'Perfil falha antes senha ou SQL' {$f=NovaFixture -Falha FERRAMENTAS;$r=Executar $f;Exigir ($r.fase -ceq 'PREFLIGHT_LOCAL' -and $f.estado.eventos.Count -eq 0);$f.estado.senha.Dispose()}
foreach($motivo in @('HISTORICO_CHECKSUM_OU_MIGRATION','TLS_NAO_VALIDADO','CONEXAO_INTERROMPIDA_OU_RECUSADA')){
    Caso ('Erro Flyway sanitizado '+$motivo) {$f=NovaFixture -Falha WMS_DEV_PreValidate -Motivo $motivo;$r=Executar $f;Exigir ($r.resultado -ceq 'FALHA' -and $f.estado.migrations -eq 0);SemProd $f}
}
Caso '13 copias independentes senha original descartada ao fim' {$f=NovaFixture;$r=Executar $f;Exigir ($f.estado.copias.Count -eq 13 -and -not [object]::ReferenceEquals($f.estado.copias[0],$f.estado.copias[1]));Descartes $f}
Caso 'IN NOT IN BETWEEN formas conhecidas SQL Server' {
    Exigir ((Get-WmsD21Expressao "situacao IN ('A','B')") -ceq (Get-WmsD21Expressao "([situacao]='B' OR [situacao]='A')"))
    Exigir ((Get-WmsD21Expressao "x NOT IN (1,2)") -ceq (Get-WmsD21Expressao "([x]<>(2) AND [x]<>(1))"))
    Exigir ((Get-WmsD21Expressao 'x BETWEEN 1 AND 9') -ceq (Get-WmsD21Expressao '([x]>=(1) AND [x]<=(9))'))
}
Caso 'Agrupamento AND OR divergente' {Exigir ((Get-WmsD21Expressao '(a=1 OR b=2) AND c=3') -cne (Get-WmsD21Expressao 'a=1 OR (b=2 AND c=3)'))}
Caso 'CHECK alterado detectado' {$x=@($CatalogoToy|ForEach-Object {[pscustomobject]@{categoria=$_.categoria;tabela=$_.tabela;nome=$_.nome;valor=$_.valor}});($x|Where-Object categoria -eq CHECK).valor=Get-WmsD21Expressao 'quantidade >= 0';Recusa {Assert-WmsD21Catalogo $CatalogoToy $x} 'D21_CATALOGO_DIVERGENTE'}
Caso 'Parser desconhecido recusado' {Recusa {Get-WmsD21CatalogoEsperado @([pscustomobject]@{arquivo='V10__fixture.sql';texto='EXEC procedimento_desconhecido;'})} 'D21_FONTE_PARSER_NAO_SUPORTADA'}
Caso 'Alvos exatos initSQL identidade NULL e SETs' {foreach($b in @('WMS_DEV','WMS_PROD')){$s=Get-WmsD21InitSql $b "FICTICIO'LOCAL";Exigir ($s.Contains("FICTICIO''LOCAL") -and $s.Contains("N'$b'") -and $s.Contains('ORIGINAL_LOGIN() IS NULL') -and $s.Contains('DB_NAME() IS NULL') -and ([regex]::Matches($s,'SET ').Count -eq 7))};Recusa {Get-WmsD21InitSql 'wms_dev' 'FICTICIO'} 'D21_BANCO_NAO_AUTORIZADO'}
Caso 'Overrides Flyway Java Maven bootstrap recusados' {foreach($n in @('FLYWAY_URL','MAVEN_OPTS','WMS_DB_BOOTSTRAP_GUARD','JAVA_TOOL_OPTIONS')){Recusa {Assert-WmsD21Ambiente @{$n='FICTICIO'}} 'RECUSAD'}}
Caso 'Args pre pending apenas e pos explicit EMPTY sem segredo' {
    foreach($etapa in @('PreValidate','Migrate','PostValidate','Info')){
        $psi=New-WmsD21StartInfo $Db 'WMS_PROD' 'FICTICIO' $etapa
        Exigir ($psi.Arguments -match '-Pbootstrap-local -Dwms.bootstrap.skip=false' -and $psi.Arguments -notmatch 'password|repair|clean|baseline|target=' -and $null -eq $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_PASSWORD'] -and $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_NAME'] -ceq 'WMS_PROD')
        if($etapa -ceq 'PreValidate'){Exigir ($psi.Arguments.Contains('-Dflyway.ignoreMigrationPatterns=*:pending'))}else{Exigir ($psi.Arguments.Contains('-Dflyway.ignoreMigrationPatterns= validate'))}
        $psi.EnvironmentVariables.Clear()
    }
}
$FixtureRoot=Join-Path $Db ('evidencias/fixtures/d21-'+[Guid]::NewGuid().ToString('N'))
$fixtureDb=Join-Path $FixtureRoot 'database'
$null=New-Item -ItemType Directory -Path (Join-Path $fixtureDb 'migrations'),(Join-Path $fixtureDb 'evidencias') -Force
Copy-Item -LiteralPath (Join-Path $Db 'evidencias/d20-baseline.json') -Destination (Join-Path $fixtureDb 'evidencias/d20-baseline.json')
foreach($f in Get-ChildItem -LiteralPath (Join-Path $Db 'migrations') -File -Filter '*.sql'){Copy-Item -LiteralPath $f.FullName -Destination (Join-Path $fixtureDb 'migrations')}
$V10=Join-Path $fixtureDb 'migrations/V10__somente_fixture_nao_aplicar.sql'
[IO.File]::WriteAllText($V10,'ALTER TABLE wms.cliente ADD d21_ficticio varchar(12) NULL;',[Text.Encoding]::UTF8)
Caso 'Filesystem V10 sem freeze latest9 expectativa inclui coluna e mock pendentes' {
    $src=@(Get-WmsD21Fontes $fixtureDb);Exigir ($src.Count -eq 10)
    $fs=@($src|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;texto=[IO.File]::ReadAllText((Join-Path $fixtureDb $_.arquivo))}})
    $cat=@(Get-WmsD21CatalogoEsperado $fs);Exigir (@($cat|Where-Object {$_.tabela -ceq 'cliente' -and $_.nome -ceq 'd21_ficticio'}).Count -eq 1)
    $f=NovaFixture @('WMS_DEV','WMS_PROD');$f.estado.fontes=$src;$f.estado.catalogo=$cat;$f.estado.pendentes=10;$f.estado.aplicadas.WMS_DEV=9;$f.estado.aplicadas.WMS_PROD=9
    $r=Executar $f;Exigir ($r.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $r.fontes.Count -eq 10 -and $f.estado.migrations -eq 2)
}
$Filho=Join-Path $FixtureRoot 'filho-ficticio.ps1'
$childText=@('param([int]$Codigo=0,[switch]$Lento)','if($Lento){Start-Sleep -Seconds 5}','Write-Output $env:WMS_DB_BOOTSTRAP_PASSWORD','[Console]::Error.WriteLine(''checksum ''+$env:WMS_DB_BOOTSTRAP_PASSWORD)','exit $Codigo') -join [Environment]::NewLine
[IO.File]::WriteAllText($Filho,$childText,(New-Object Text.UTF8Encoding($true)))
foreach($exitCode in @(0,3)){
    Caso ('Filho REAL ficticio exit '+$exitCode+' stdout stderr pai protegidos') {
        $antes=[Environment]::GetEnvironmentVariable('WMS_DB_BOOTSTRAP_PASSWORD','Process')
        $psi=New-WmsD21StartInfo $Db 'WMS_DEV' 'FICTICIO' PreValidate
        $psi.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
        $psi.Arguments='-NoProfile -NonInteractive -File "'+$Filho+'" -Codigo '+$exitCode
        $senha=NovaSenha
        try{$r=Invoke-WmsD21Processo $psi $senha 10000;Exigir ($r.exitCode -eq $exitCode -and $r.saidaBrutaDescartada -eq $true -and $psi.EnvironmentVariables.Count -eq 0 -and [Environment]::GetEnvironmentVariable('WMS_DB_BOOTSTRAP_PASSWORD','Process') -ceq $antes)}finally{$senha.Dispose()}
    }
}
Caso 'Timeout filho ficticio propria arvore limpeza finally' {
    $psi=New-WmsD21StartInfo $Db 'WMS_DEV' 'FICTICIO' PreValidate
    $psi.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $psi.Arguments='-NoProfile -NonInteractive -File "'+$Filho+'" -Lento'
    $senha=NovaSenha
    try{Recusa {Invoke-WmsD21Processo $psi $senha 100} 'D21_FLYWAY_TIMEOUT';Exigir ($psi.EnvironmentVariables.Count -eq 0)}finally{$senha.Dispose()}
}
Caso 'POM Cedro contrato conferido sem Maven' {Assert-WmsD21Pom (Join-Path $Db '../backend/pom.xml')}
Caso '45 fontes e manifestos snapshot D20 conferidos' {
    $snap=Get-Content -LiteralPath (Join-Path $Db 'evidencias/d21-snapshot-d20.json') -Raw -Encoding UTF8|ConvertFrom-Json
    $raiz=Split-Path $Db -Parent
    foreach($f in $snap.copias){Exigir ((Get-FileHash -LiteralPath (Join-Path $raiz $f.copia) -Algorithm SHA256).Hash -ceq $f.sha256)}
}
$falhas=@($Casos|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='D21_FIXTURES_OFFLINE_SQL_NAO_EXECUTADO';utc=[DateTime]::UtcNow.ToString('o');total=$Casos.Count;aprovados=$Casos.Count-$falhas.Count;falhas=$falhas.Count;casos=$Casos.ToArray();fixture=$FixtureRoot.Substring($Db.Length+1);mavenExecutado=$false;sqlExecutado=$false;filhosReais='SOMENTE_POWERSHELL_FICTICIO_SEM_SQL'}|ConvertTo-Json -Depth 7
if($falhas.Count){exit 1}
