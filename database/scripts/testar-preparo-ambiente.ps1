# Fronteiras ficticias: nao abre SQL nem importa credencial real.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'preparar-ambiente.ps1')
. (Join-Path $PSScriptRoot 'd22-fixture.ps1')
$cases=[Collections.Generic.List[object]]::new()
function Check([string]$Name,[scriptblock]$Test){try{& $Test;$cases.Add(@{caso=$Name;passou=$true})}catch{$cases.Add(@{caso=$Name;passou=$false;erro=$_.Exception.Message})}}
function Need([bool]$Ok){if(-not $Ok){throw ('PREPARO_TESTE_FALHOU_LINHA_'+(Get-PSCallStack)[1].ScriptLineNumber)}}
function Fixture([string]$Fail=''){
    $s=@{events=[Collections.Generic.List[string]]::new();fail=$Fail;reads=0}
    $ad=@{
        Fontes={
            $s.reads++
            $sha=if($s.fail -ceq 'FONTE' -and $s.reads -gt 2){'ALTERADA'}else{'FIXTURE'}
            [pscustomobject]@{arquivo='migrations/V1__ficticia.sql';sha256=$sha}
        }.GetNewClosure()
        Catalogo={param($Fs) $s.events.Add('Catalogo');if($s.fail -ceq 'PARSER'){throw 'D21_FONTE_PARSER_NAO_SUPORTADA'}}.GetNewClosure()
        Bootstrap={param($Bancos)
            $s.events.Add('Bootstrap:'+($Bancos -join ','))
            [pscustomobject]@{resultado=if($s.fail -ceq 'MIGRATION'){'FALHA'}elseif($Bancos.Count -eq 1){'DEV_BOOTSTRAP_UPGRADE_CONCLUIDO'}else{'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO'};codigo='D21_CONEXAO_FLYWAY_INTERROMPIDA'}
        }.GetNewClosure()
        Permissoes={param($Banco,$Modo)
            $s.events.Add($Modo+':'+$Banco)
            if($s.fail -ceq ($Modo+':'+$Banco)){throw 'CAD_GUARDA_ANTES_RECUSADA'}
            [pscustomobject]@{estado=if($Modo -ceq 'Plan'){'PLANO_LOCAL_VALIDADO'}elseif($Banco -ceq 'WMS_DEV'){'DIREITOS_DEV_APLICADOS_E_CONFERIDOS'}else{'DIREITOS_PROD_APLICADOS_E_CONFERIDOS'};recibo='ficticio';grantsAplicados=$false}
        }.GetNewClosure()
    }
    @{state=$s;ad=$ad}
}
Check 'DEV prepara somente DEV; migrations antes de permissoes' {
    $f=Fixture;$r=Invoke-WmsEnvironmentPreparation DEV Apply $f.ad
    Need ($r.estado -ceq 'AMBIENTE_ATUALIZADO' -and ($f.state.events -join '|') -ceq 'Catalogo|Plan:WMS_DEV|Bootstrap:WMS_DEV|Apply:WMS_DEV')
}
Check 'PROD exige DEV completo primeiro e ambas permissoes' {
    $f=Fixture;$r=Invoke-WmsEnvironmentPreparation PROD Apply $f.ad
    Need ($r.estado -ceq 'AMBIENTE_ATUALIZADO' -and ($f.state.events -join '|') -ceq 'Catalogo|Plan:WMS_DEV|Plan:WMS_PROD|Bootstrap:WMS_DEV,WMS_PROD|Apply:WMS_DEV|Apply:WMS_PROD')
}
Check 'Plan nao invoca migrations nem Apply' {
    $f=Fixture;$r=Invoke-WmsEnvironmentPreparation PROD Plan $f.ad
    Need ($r.estado -ceq 'PLANO_SEM_SQL' -and ($f.state.events -join '|') -ceq 'Catalogo|Plan:WMS_DEV|Plan:WMS_PROD')
}
foreach($fault in @('PARSER','Plan:WMS_DEV','Plan:WMS_PROD','MIGRATION','FONTE','Apply:WMS_DEV','Apply:WMS_PROD')){
    Check ('falha '+$fault+' interrompe sem repetir') {
        $f=Fixture $fault;$r=Invoke-WmsEnvironmentPreparation PROD Apply $f.ad
        Need ($r.estado -ceq 'INTERROMPIDO' -and $r.processosEncerrados -eq 0)
        Need (@($f.state.events|Where-Object {$_ -like 'Bootstrap:*'}).Count -le 1)
        if($fault -in @('PARSER','Plan:WMS_DEV','Plan:WMS_PROD','MIGRATION','FONTE')){Need (@($f.state.events|Where-Object {$_ -like 'Apply:*'}).Count -eq 0)}
        if($fault -ceq 'Apply:WMS_DEV'){Need (-not $f.state.events.Contains('Apply:WMS_PROD'))}
    }
}
Check 'bootstrap DEV isolado preserva PROD e banco existente' {
    $f=New-WmsD22LauncherFixture
    $f.adaptadores.Inspecao={param($Senha) [pscustomobject]@{endpoint='127.0.0.1:1433';bancoInicial='master';loginCriacao='sa';servidorReal='FICTICIO';existentes=@([pscustomobject]@{banco='WMS_DEV';estado=0;online=$true})}}
    $events=$f.estado.eventos
    $f.adaptadores.Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
        $events.Add($Acao+':'+$Banco)
        [pscustomobject]@{banco=$Banco;resultado='EXISTENTE_PRESERVADO_CONFERIDO';novo=$false;ddlSolicitado=$false;conteudoExistenteAlterado=$false;grantsExecutados=$false}
    }.GetNewClosure()
    $r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores -Bancos WMS_DEV -SomenteExistentes
    Need ($r.resultado -ceq 'DEV_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $events.Contains('Check:WMS_DEV') -and @($events|Where-Object {$_ -match 'PROD|Create'}).Count -eq 0)
}
Check 'partida nao cria banco ausente' {
    $f=New-WmsD22LauncherFixture;$r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores -Bancos WMS_DEV -SomenteExistentes
    Need ($r.codigo -ceq 'D21_BANCO_EXISTENTE_NECESSARIO' -and $f.estado.ddl -eq 0)
}
Check 'TLS inconclusivo interrompe antes da abertura sem repeticao' {
    $f=New-WmsD22LauncherFixture;$f.adaptadores.AntesSql={throw 'D21_TLS_PROCESSO_ATUAL_DIVERGENTE'}
    $r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Need ($r.codigo -ceq 'D21_TLS_PROCESSO_ATUAL_DIVERGENTE' -and -not $f.estado.eventos.Contains('Inspect'))
}
Check 'PROD isolado e ordem inversa recusados sem adaptadores' {
    foreach($banks in @(@('WMS_PROD'),@('WMS_PROD','WMS_DEV'))){
        $refused=$false;try{$null=Invoke-WmsD22InicioAutomatico -Bancos $banks -Offline}catch{$refused=$_.Exception.Message -ceq 'D21_ORDEM_BANCOS_RECUSADA'};Need $refused
    }
}
Check 'catalogo V1-V13 deriva coluna/default/indice filtrado sem perder chaves' {
    $fs=@(Get-WmsD21Fontes $WmsD21InicioRoot|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;texto=[IO.File]::ReadAllText((Join-Path $WmsD21InicioRoot $_.arquivo))}})
    $cat=@(Get-WmsD21CatalogoEsperado $fs)
    Need (@($cat|Where-Object {$_.categoria -ceq 'COL' -and $_.tabela -ceq 'item_tabela_cobranca' -and $_.nome -ceq 'ativo' -and $_.valor -ceq 'bit|0|0|0|0'}).Count -eq 1)
    Need (@($cat|Where-Object {$_.nome -ceq 'df_item_tabela_cobranca_ativo' -and $_.valor -ceq 'ativo|1'}).Count -eq 1)
    Need (@($cat|Where-Object {$_.nome -ceq 'uk_item_tabela_cobranca_ativo' -and $_.valor -ceq '1|tabela_id,servico_id,categoria|0,0,0||=(1,ativo)'}).Count -eq 1)
    Need (@($cat|Where-Object {$_.nome -ceq 'uk_item_tabela_cobranca_1'}).Count -eq 0)
    Need ((Get-FileHash -LiteralPath (Join-Path $WmsD21InicioRoot 'migrations/V13__historico_itens_tabela_cobranca.sql')).Hash -ceq '3794C8638C4A3F2B823B76288967EE0E03C81E804879F15F4A4986BECFB7DD28')
    $v13=($fs|Where-Object {$_.arquivo -like '*V13__*'}).texto
    Need ($v13 -match '(?s)DEFAULT \(1\) WITH VALUES;\s*\r?\nGO\s*\r?\n.*CREATE UNIQUE INDEX')
}
Check 'parser recusa extensao SQL desconhecida' {
    $bad=@([pscustomobject]@{arquivo='V1__ficticia.sql';texto='EXEC procedimento_nao_autorizado;'})
    $refused=$false;try{$null=Get-WmsD21CatalogoEsperado $bad}catch{$refused=$_.Exception.Message -ceq 'D21_FONTE_PARSER_NAO_SUPORTADA'};Need $refused
}
Check 'erro SQL da migration nao vira erro de checksum nem publica texto bruto' {
    $r=Get-WmsD21FlywayFailure 1 "[ERROR] Migration V13__historico_itens_tabela_cobranca.sql failed`n[ERROR] SQL State : S0001`n[ERROR] Error Code : 207`n[ERROR] Message : FICTICIO_SIGILOSO"
    Need ($r.resultado -ceq 'MIGRATION_SQL_FALHOU' -and $r.diagnostico.sqlNumber -eq 207 -and $r.diagnostico.version -ceq '13')
    Need (($r|ConvertTo-Json -Depth 5) -notmatch 'FICTICIO_SIGILOSO')
}
Check 'checksum TLS e transporte permanecem causas distintas' {
    Need ((Get-WmsD21FlywayFailure 1 'Validate failed: checksum mismatch').resultado -ceq 'HISTORICO_CHECKSUM_OU_MIGRATION')
    Need ((Get-WmsD21FlywayFailure 1 'Migration failed: PKIX certificate validation failed').resultado -ceq 'TLS_NAO_VALIDADO')
    Need ((Get-WmsD21FlywayFailure 1 'Migration failed: connection refused').resultado -ceq 'CONEXAO_INTERROMPIDA_OU_RECUSADA')
    Need ((Get-WmsD21FlywayFailure 0 'Migration checksums valid').resultado -ceq 'GOAL_OK')
}
$failed=@($cases|Where-Object {-not $_.passou})
[pscustomobject]@{total=$cases.Count;aprovados=$cases.Count-$failed.Count;falhas=$failed.Count;casos=$cases;SQL=$false;credencialReal=$false}|ConvertTo-Json -Depth 5
if($failed.Count){exit 1}
