[CmdletBinding()]
param([switch]$Offline)
$WmsD20Pathiniciarbancos=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathiniciarbancos 'scripts/d20-guardas.ps1')

function Invoke-WmsD20InicioManual {
    param([switch]$Offline,[hashtable]$Adaptadores)
    if($Offline){
        return [pscustomobject]@{natureza='LAUNCHER_OFFLINE_SEM_SEGREDO_SEM_SQL';endpoint='127.0.0.1:1433';
            bancos=@('WMS_DEV','WMS_PROD');senhaLida=$false;conexoes=0;ddl=0;migrations=0;
            roteiro=@('SENHA_OCULTA_MEMORIA','INSPECAO_MASTER_LEITURA_TLS','CONFIRMACAO_INTERATIVA','DEV_CHECK_OU_CREATE','PROD_CHECK_OU_CREATE');resultado='OFFLINE_OK'}
    }
    if($null -eq $Adaptadores){
        $Adaptadores=@{
            Senha={Read-Host 'Senha sa WMS autorizada (oculta, somente memoria)' -AsSecureString}
            Inspecao={param($Senha) & (Join-Path $WmsD20Pathiniciarbancos 'scripts/verificar-alvo-criacao.ps1') -Action Inspect -SenhaLocal $Senha | ConvertFrom-Json}
            Confirmar={param($Info)
                Write-Host ('Servidor real: '+$Info.servidorReal+' | master | sa | TLS validado')
                foreach($b in @($Info.existentes)){Write-Host ('Existente: '+$b.banco+' | ONLINE='+$b.online)}
                Write-Host 'WMS_DEV e WMS_PROD: existentes serao apenas conferidos; inexistentes criados vazios. Sem migrations/cargas.'
                (Read-Host 'Digite 127.0.0.1:1433 para confirmar este alvo e continuar') -ceq '127.0.0.1:1433'
            }
            Banco={param($Acao,$Banco,$Servidor,$Alvo,$Senha)
                & (Join-Path $WmsD20Pathiniciarbancos 'scripts/criar-bancos.ps1') -Action $Acao -Database $Banco -ServidorConfirmado $Servidor -AlvoConfirmado $Alvo -SenhaLocal $Senha | ConvertFrom-Json
            }
            Mostrar={param($Texto) Write-Host $Texto}
        }
    }
    $senha=$null;$etapas=New-Object 'Collections.Generic.List[object]';$fase='ENTRADA';$codigo=$null;$resultado='FALHA'
    try{
        & $Adaptadores.Mostrar 'TCP127.0.0.1:1433 | WMS_DEV e WMS_PROD | senha oculta | TLS obrigatorio'
        $senha=& $Adaptadores.Senha
        if($null -eq $senha -or $senha -isnot [Security.SecureString] -or $senha.Length -eq 0){throw 'D20_CREDENCIAL_CRIACAO_PROTEGIDA_AUSENTE_SEM_CONEXAO'}
        $fase='INSPECAO_LEITURA';$copia=$senha.Copy()
        try{$info=& $Adaptadores.Inspecao $copia}finally{$copia.Dispose()}
        if($null -eq $info -or $info.endpoint -cne '127.0.0.1:1433' -or $info.bancoInicial -cne 'master' -or $info.loginCriacao -cne 'sa' -or
            [string]::IsNullOrWhiteSpace($info.servidorReal) -or $info.servidorReal -match '[\x00-\x1f]' -or $null -eq $info.PSObject.Properties['existentes']){throw 'D20_INSPECAO_IDENTIDADE_OU_METADADOS_DIVERGENTES'}
        $vistos=@()
        foreach($b in @($info.existentes)){
            if($b.banco -cin $vistos -or $null -eq $b.estado -or $b.estado -ne 0 -or $b.online -ne $true){throw 'D20_EXISTENTE_NOME_OU_ESTADO_DIVERGENTE'}
            Assert-WmsD20BancoExistente $b.banco $b.banco $b.online;$vistos+=$b.banco
        }
        $fase='CONFIRMACAO'
        if(-not (& $Adaptadores.Confirmar $info)){$resultado='CANCELADO_SEM_DDL'}
        else{
            foreach($banco in @('WMS_DEV','WMS_PROD')){
                $acao=if($banco -cin $vistos){'Check'}else{'Create'}
                $fase=$banco+'_'+$acao.ToUpperInvariant()
                & $Adaptadores.Mostrar ($banco+': '+$(if($acao -eq 'Check'){'preservar schema/dados e conferir identidade/ONLINE/metadados'}else{'CREATE somente se inexistente; comprovar vazio'}))
                $copia=$senha.Copy()
                try{$r=& $Adaptadores.Banco $acao $banco $info.servidorReal ('127.0.0.1:1433/'+$banco) $copia}finally{$copia.Dispose()}
                $efetiva=if($r.resultado -ceq 'EXISTENTE_PRESERVADO_CONFERIDO'){'Check'}else{'Create'}
                $esperado=if($efetiva -eq 'Check'){'EXISTENTE_PRESERVADO_CONFERIDO'}else{'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'}
                if($null -eq $r -or $r.banco -cne $banco -or $r.resultado -cne $esperado -or $r.migrationsExecutadas -ne $false -or $r.cargasExecutadas -ne $false -or $r.grantsExecutados -ne $false -or
                    ($acao -eq 'Check' -and $efetiva -ne 'Check') -or $r.novo -ne ($efetiva -eq 'Create') -or $r.ddlSolicitado -ne ($efetiva -eq 'Create') -or $r.conteudoExistenteAlterado -ne $false){throw 'D20_ETAPA_NAO_CONFIRMADA'}
                $etapas.Add([pscustomobject]@{banco=$banco;acaoSolicitada=$acao;acao=$efetiva;resultado=$r.resultado})
                & $Adaptadores.Mostrar ($banco+': '+$r.resultado)
            }
            $resultado='BANCOS_CONFIRMADOS_NOVOS_VAZIOS_EXISTENTES_PRESERVADOS';$fase='CONCLUIDO'
        }
    }catch{
        $msg=$_.Exception.Message
        $codigo=if($msg -cmatch '^D20_[A-Z0-9_]+$'){$msg}else{'D20_ENTRADA_OU_CONEXAO_FALHOU'}
    }finally{if($null -ne $senha -and $senha -is [Security.SecureString]){$senha.Dispose()}}
    $explicacao=if($resultado -eq 'CANCELADO_SEM_DDL'){'Confirmacao recusada. Nenhum CREATE solicitado.'}
        elseif($resultado -ne 'FALHA'){'Bancos confirmados. Novos vazios; existentes preservados com seu schema/dados. Migration DEV e acao posterior separada.'}
        elseif($codigo -match 'CONTEUDO_OU_ACESSO|METADADOS|VAZIO'){'Metadados insuficientes ou objetos/acessos inesperados. Banco preservado; nao limpar/recriar/alterar. Conferir evidencia.'}
        elseif($codigo -match 'EXISTENTE|IDENTIDADE|ETAPA_NAO_CONFIRMADA'){'Identidade, nome, estado ou resultado divergente. Preservar bancos e conferir alvo/metadados; sem sobrescrita.'}
        else{'Falha de entrada, acesso ou TLS. Nao houve permissao para bypass TLS, instalacao ou mudanca global. Conferir evidencia da fase; preservar bancos criados.'}
    & $Adaptadores.Mostrar ($explicacao+' Fase: '+$fase)
    [pscustomobject]@{natureza='ORQUESTRACAO_MANUAL';endpoint='127.0.0.1:1433';resultado=$resultado;fase=$fase;codigo=$codigo;
        explicacao=$explicacao;etapas=$etapas.ToArray();migrations=0;cargas=0;grants=0}
}

if($MyInvocation.InvocationName -ne '.'){
    $r=Invoke-WmsD20InicioManual -Offline:$Offline
    if(-not $Offline){
        $p=Join-Path $WmsD20Pathiniciarbancos ('evidencias/d20-manual-'+[Guid]::NewGuid().ToString('N')+'.json')
        $r|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $p -Encoding UTF8
    }
    $r|ConvertTo-Json -Depth 6
    if($r.resultado -eq 'FALHA'){exit 1}
    if($r.resultado -eq 'CANCELADO_SEM_DDL'){exit 2}
}
