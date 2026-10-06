[CmdletBinding()]
param([switch]$Offline,[switch]$Fixture)
$WmsD21InicioRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd21-flyway.ps1')
. (Join-Path $PSScriptRoot 'd22-credencial.ps1')
function Invoke-WmsD22InicioAutomatico {
    param([switch]$Offline,[hashtable]$Adaptadores)
    if($Offline){
        return [pscustomobject]@{natureza='D22_LAUNCHER_OFFLINE_SEM_SEGREDO_SEM_SQL';endpoint='127.0.0.1:1433';
            bancos=@('WMS_DEV','WMS_PROD');senhaLida=$false;conexoes=0;ddl=0;migrationsExecutadas=0;
            fonte='FLYWAY_FILESYSTEM_DINAMICO';perfil='bootstrap-local';prod='BOOTSTRAP_UPGRADE_AUTOMATICO_AUTORIZADO';
            roteiro=@('CREDENCIAL_DPAPI_AUTOMATICA','PREFLIGHT_LOCAL','INSPECAO_MASTER_TLS_LEITURA',
                'DEV_CREATE_OU_CHECK_VALIDATE_MIGRATE_VALIDATE_INFO_CATALOGO','SOMENTE_APOS_DEV_OK_PROD_MESMO_FLUXO');resultado='OFFLINE_OK'}
    }
    if($null -eq $Adaptadores){
        $Adaptadores=@{
            Ferramentas={Assert-WmsD21Ferramentas $WmsD21InicioRoot}
            Fontes={Get-WmsD21Fontes $WmsD21InicioRoot}
            Expectativa={param($Fontes)
                $fs=@($Fontes|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;texto=[IO.File]::ReadAllText((Join-Path $WmsD21InicioRoot $_.arquivo))}})
                Get-WmsD21CatalogoEsperado $fs
            }
            Senha={Read-WmsD22Credencial (Get-WmsD22Contexto)}
            Inspecao={param($Senha) & (Join-Path $WmsD21InicioRoot 'scripts/verificar-alvo-criacao.ps1') -Action Inspect -SenhaLocal $Senha|ConvertFrom-Json}
            Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
                & (Join-Path $WmsD21InicioRoot 'scripts/criar-bancos.ps1') -Action $Acao -Database $Banco -ServidorConfirmado $Servidor -AlvoConfirmado $Alvo -SenhaLocal $Senha|ConvertFrom-Json
            }
            Flyway={param($Banco,$Servidor,$Senha,$Etapa) Invoke-WmsD21Flyway $Banco $Servidor $Senha $Etapa}
            Schema={param($Banco,$Servidor,$Senha) Read-WmsD21Schema $Banco $Servidor $Senha}
            Mostrar={param($Texto) Write-Host $Texto}
        }
    }
    $senha=$null;$etapas=New-Object 'Collections.Generic.List[object]';$fase='PREFLIGHT_LOCAL';$codigo=$null;$resultado='FALHA';$devOk=$false;$fontes=@()
    try{
        & $Adaptadores.Mostrar 'D22 automatico | TCP127.0.0.1:1433 | WMS_DEV -> WMS_PROD | bootstrap/upgrade | TLS obrigatorio'
        $fase='CREDENCIAL_DPAPI';$senha=& $Adaptadores.Senha
        if($null -eq $senha -or $senha -isnot [Security.SecureString] -or $senha.Length -eq 0){throw 'D22_CREDENCIAL_INVALIDA'}
        $fase='PREFLIGHT_LOCAL'
        & $Adaptadores.Ferramentas
        $fontes=@(& $Adaptadores.Fontes)
        if(-not $fontes.Count){throw 'D21_MIGRATIONS_AUSENTES'}
        $catalogoEsperado=@(& $Adaptadores.Expectativa $fontes)
        if(-not $catalogoEsperado.Count){throw 'D21_CATALOGO_EXPECTATIVA_AUSENTE'}
        $fase='INSPECAO_MASTER_LEITURA';$copia=$senha.Copy()
        try{$info=& $Adaptadores.Inspecao $copia}finally{$copia.Dispose()}
        if($null -eq $info -or $info.endpoint -cne '127.0.0.1:1433' -or $info.bancoInicial -cne 'master' -or $info.loginCriacao -cne 'sa' -or
            [string]::IsNullOrWhiteSpace($info.servidorReal) -or $info.servidorReal -match '[\x00-\x1f]' -or $null -eq $info.PSObject.Properties['existentes']){throw 'D21_INSPECAO_IDENTIDADE_DIVERGENTE'}
        $vistos=@()
        foreach($b in @($info.existentes)){
            if($b.banco -cin $vistos -or $null -eq $b.estado -or $b.estado -ne 0 -or $b.online -ne $true){throw 'D21_EXISTENTE_NOME_ESTADO_DIVERGENTE'}
            Assert-WmsD20BancoExistente $b.banco $b.banco $b.online;$vistos+=$b.banco
        }
        & $Adaptadores.Mostrar 'Identidade/alvo fixos conferidos automaticamente. DEV completo antes PROD.'
            foreach($banco in @('WMS_DEV','WMS_PROD')){
                if($banco -ceq 'WMS_PROD' -and -not $devOk){throw 'D21_DEV_NAO_CONCLUIDO_PROD_BLOQUEADO'}
                Assert-WmsD21FontesIguais $fontes @(& $Adaptadores.Fontes)
                $acao=if($banco -cin $vistos){'Check'}else{'Create'}
                $fase=$banco+'_'+$acao.ToUpperInvariant()
                & $Adaptadores.Mostrar ($banco+': '+$acao+' protegido, sem recriar existente')
                $copia=$senha.Copy()
                try{$r=& $Adaptadores.Banco $acao $banco $info.servidorReal ('127.0.0.1:1433/'+$banco) $copia}finally{$copia.Dispose()}
                $efetiva=if($r.resultado -ceq 'EXISTENTE_PRESERVADO_CONFERIDO'){'Check'}else{'Create'}
                $esperado=if($efetiva -eq 'Check'){'EXISTENTE_PRESERVADO_CONFERIDO'}else{'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'}
                if($null -eq $r -or $r.banco -cne $banco -or $r.resultado -cne $esperado -or
                    ($acao -eq 'Check' -and $efetiva -ne 'Check') -or $r.novo -ne ($efetiva -eq 'Create') -or $r.ddlSolicitado -ne ($efetiva -eq 'Create') -or
                    $r.conteudoExistenteAlterado -ne $false -or $r.grantsExecutados -ne $false){throw 'D21_CREATE_CHECK_NAO_CONFIRMADO'}
                $etapas.Add([pscustomobject]@{banco=$banco;etapa=$efetiva;resultado=$r.resultado})
                foreach($etapa in @('PreValidate','Migrate','PostValidate','Info')){
                    Assert-WmsD21FontesIguais $fontes @(& $Adaptadores.Fontes)
                    $fase=$banco+'_'+$etapa.ToUpperInvariant()
                    & $Adaptadores.Mostrar ($banco+': Flyway '+$etapa+' (saida sanitizada)')
                    $copia=$senha.Copy()
                    try{$f=& $Adaptadores.Flyway $banco $info.servidorReal $copia $etapa}finally{$copia.Dispose()}
                    if($null -eq $f -or $null -eq $f.PSObject.Properties['exitCode'] -or $f.exitCode -ne 0 -or $f.resultado -cne 'GOAL_OK' -or $f.saidaBrutaDescartada -ne $true){
                        if($null -ne $f){switch($f.resultado){'HISTORICO_CHECKSUM_OU_MIGRATION'{throw 'D21_HISTORICO_CHECKSUM_FLYWAY_FALHOU'};'TLS_NAO_VALIDADO'{throw 'D21_TLS_FLYWAY_FALHOU'};'CONEXAO_INTERROMPIDA_OU_RECUSADA'{throw 'D21_CONEXAO_FLYWAY_INTERROMPIDA'}}}
                        throw 'D21_FLYWAY_ETAPA_FALHOU'
                    }
                    $etapas.Add([pscustomobject]@{banco=$banco;etapa=$etapa;exitCode=0;resultado='GOAL_OK'})
                }
                $fase=$banco+'_SCHEMA_REAL_LEITURA';$copia=$senha.Copy()
                try{$schema=& $Adaptadores.Schema $banco $info.servidorReal $copia}finally{$copia.Dispose()}
                Assert-WmsD21Schema $schema $banco
                if($null -eq $schema.PSObject.Properties['catalogo']){throw 'D21_CATALOGO_METADADOS_AUSENTES'}
                Assert-WmsD21Catalogo $catalogoEsperado @($schema.catalogo)
                Assert-WmsD21FontesIguais $fontes @(& $Adaptadores.Fontes)
                $etapas.Add([pscustomobject]@{banco=$banco;etapa='SchemaReal';resultado='CATALOGO_CONFRONTADO_FONTE';
                    tabelas=$schema.tabelas;colunas=$schema.colunas;historico=$schema.historico;itensCatalogo=@($schema.catalogo).Count})
                if($banco -ceq 'WMS_DEV'){$devOk=$true}
                & $Adaptadores.Mostrar ($banco+': BOOTSTRAP_UPGRADE_CONCLUIDO')
            }
            $resultado='DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO';$fase='CONCLUIDO'
    }catch{
        $e=$_.Exception;while($e.InnerException){$e=$e.InnerException}
        $codigo=if($e.Message -cmatch '^(D20|D21|D22)_[A-Z0-9_]+$'){$e.Message}else{'D21_ENTRADA_CONEXAO_FERRAMENTA_FALHOU'}
    }finally{if($null -ne $senha -and $senha -is [Security.SecureString]){$senha.Dispose()}}
    $explicacao=if($resultado -ne 'FALHA'){'DEV e PROD concluidos: historico/checksums validados, pendentes aplicadas e catalogo real confrontado com fonte.'}
        elseif($fase -eq 'CREDENCIAL_DPAPI'){'Credencial WMS ausente/invalida neste usuario/maquina. Execute database/configurar-credencial.bat. Nenhuma senha/confirmacao sera pedida; nenhuma conexao SQL solicitada.'}
        else{'Falha nesta fase: interrompido, sem clean/repair/baseline/recriacao. Erro DEV bloqueia PROD. Preservar bancos/historico, corrigir causa e reexecutar; Flyway determina pendentes. TLS obrigatorio.'}
    & $Adaptadores.Mostrar ($explicacao+' Fase: '+$fase)
    [pscustomobject]@{natureza='D22_ORQUESTRACAO_AUTOMATICA';endpoint='127.0.0.1:1433';resultado=$resultado;fase=$fase;codigo=$codigo;
        explicacao=$explicacao;devConcluido=$devOk;etapas=$etapas.ToArray();fontes=$fontes;grants=0;baselineAutomatico=$false;repair=$false;clean=$false}
}
if($MyInvocation.InvocationName -ne '.'){
    if($Offline -and $Fixture){throw 'D22_MODOS_INCOMPATIVEIS'}
    $ad=$null
    if($Fixture){. (Join-Path $PSScriptRoot 'd22-fixture.ps1');$ad=(New-WmsD22LauncherFixture).adaptadores}
    $r=Invoke-WmsD22InicioAutomatico -Offline:$Offline -Adaptadores $ad
    if(-not $Offline -and -not $Fixture){$p=Join-Path $WmsD21InicioRoot ('evidencias/d22-automatico-'+[Guid]::NewGuid().ToString('N')+'.json');$r|ConvertTo-Json -Depth 8|Set-Content -LiteralPath $p -Encoding UTF8}
    $r|ConvertTo-Json -Depth 8
    if($r.resultado -eq 'FALHA'){exit 1}
}
