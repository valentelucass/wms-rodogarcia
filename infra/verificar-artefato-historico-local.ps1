# Leitor independente do artefato360 HISTORICO. Nao le Java/estado corrente nem executa JAR.
[CmdletBinding()]
param([ValidatePattern('^target(?:-[A-Za-z0-9-]+)?$')][string]$DiretorioArtefato='target-be14')
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$utf8=New-Object Text.UTF8Encoding($false,$true)
$checks=[ordered]@{};$hashes=@()
function LerHistorico([string]$p){[IO.File]::ReadAllText((Join-Path $script:raiz $p),$script:utf8)}
function HashHistorico([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function CheckHistorico([string]$id,[bool]$ok){$script:checks[$id]=$ok}
try {
    $c=(LerHistorico 'database/evidencias/d19-v9-2026-10-06-p2-locks-final-antes-infra-contratos-preparacao-final-local.json')|ConvertFrom-Json
    $a=$c.artefatoFinal;$f=$c.freezeFinal
    if($f.estado -cne 'HISTORICO_ACEITE_RETIDO_P2_LOCKS' -or $DiretorioArtefato -cne $a.diretorio){throw 'BLOQUEADO_CONTEXTO_HISTORICO_OU_DIRETORIO'}
    foreach($p in @($f.arquivo,$f.fonteSnapshot,$a.resumo,$a.log)){$hashes+=[pscustomobject]@{arquivo=$p;antes=HashHistorico $p}}
    CheckHistorico 'manifesto360.hash_exato' ((HashHistorico $f.arquivo) -ceq $f.sha256)
    CheckHistorico 'fonte31.snapshot_B459_exato' ((HashHistorico $f.fonteSnapshot) -ceq $f.doc31Sha256)
    $jar='infra/evidencias/d19-be14-360-historico-2026-10-06-jar-snapshot.jar';$hash=HashHistorico $jar;$bytes=(Get-Item -LiteralPath (Join-Path $raiz $jar) -ErrorAction Stop).Length
    CheckHistorico 'jar.bytes_78524597' ($bytes -eq $a.bytes)
    CheckHistorico 'jar.sha256_829A' ($hash -ceq $a.sha256)
    $resumo=(LerHistorico $a.resumo)|ConvertFrom-Json;$log=LerHistorico $a.log
    CheckHistorico 'resumo.360_0_0_0_17' ($resumo.testes -eq 360 -and $resumo.falhas -eq 0 -and $resumo.erros -eq 0 -and $resumo.ignorados -eq 0 -and $resumo.quantidadeXml -eq 17)
    CheckHistorico 'resumo.diretorio_target_be14_log_camadas' ($resumo.comando -match '(?:^|\s)-Dwms.build.directory=target-be14(?:\s|$)' -and $resumo.log -ceq [IO.Path]::GetFileName($a.log))
    CheckHistorico 'log.build_success_360_071212' ($log -match '\[INFO\] BUILD SUCCESS' -and $log -match 'Tests run: 360, Failures: 0, Errors: 0, Skipped: 0' -and $log -match '2026-10-06T07:12:12-03:00')
    $xmls=@(Get-ChildItem -LiteralPath (Join-Path $raiz $a.xmls) -Filter '*.xml' -File);$total=0;$falhas=0;$erros=0;$ignorados=0
    CheckHistorico 'xml.17_copias' ($xmls.Count -eq 17 -and @($resumo.suites).Count -eq 17)
    foreach($x in $xmls){
        $p=$a.xmls+'/'+$x.Name;$h=HashHistorico $p;$e=@($resumo.suites|Where-Object {$_.xml -ceq $x.Name});$hashes+=[pscustomobject]@{arquivo=$p;antes=$h}
        CheckHistorico ('xml.hash.'+$x.Name) ($e.Count -eq 1 -and $h -ceq $e[0].sha256)
        $settings=New-Object Xml.XmlReaderSettings;$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit;$settings.XmlResolver=$null
        $xr=[Xml.XmlReader]::Create($x.FullName,$settings)
        try{$doc=New-Object Xml.XmlDocument;$doc.XmlResolver=$null;$doc.Load($xr)}finally{$xr.Dispose()}
        $s=$doc.DocumentElement;if($s.LocalName -cne 'testsuite'){throw 'BLOQUEADO_XML_SUITE_FORMATO'}
        $total+=[int]$s.GetAttribute('tests');$falhas+=[int]$s.GetAttribute('failures');$erros+=[int]$s.GetAttribute('errors');$ignorados+=[int]$s.GetAttribute('skipped')
    }
    CheckHistorico 'xml.soma_360_0_0_0' ($total -eq 360 -and $falhas -eq 0 -and $erros -eq 0 -and $ignorados -eq 0)
    foreach($e in $c.evidenciasJpaFinal){
        $v=(LerHistorico $e.arquivo)|ConvertFrom-Json;$n=if($e.tipo -ceq 'principal'){$v.totalChecks}else{$v.total};$freeze=if($e.tipo -ceq 'principal'){$v.comparacaoJpa.freeze}else{$v.freeze}
        CheckHistorico ('output.historico.'+$e.tipo) ($e.estado -ceq 'HISTORICO' -and $n -eq $e.total -and $v.aprovados -eq $e.total -and @($v.divergencias).Count -eq 0 -and $freeze.sha256 -ceq $f.sha256)
        $hashes+=[pscustomobject]@{arquivo=$e.arquivo;antes=HashHistorico $e.arquivo}
    }
    CheckHistorico 'jar.hash_estavel' ((HashHistorico $jar) -ceq $hash)
    foreach($h in $hashes){$h|Add-Member -NotePropertyName depois -NotePropertyValue (HashHistorico $h.arquivo);CheckHistorico ('preservacao.'+$h.arquivo) ($h.antes -ceq $h.depois)}
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{capturadoEm=(Get-Date).ToString('o');natureza='Referencia ESTATICA do artefato360 HISTORICO; aceite retido P2 locks, nao reconferencia do backend em escrita';total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;artefato=[pscustomobject]@{arquivo=$jar;bytes=$bytes;sha256=$hash;executado=$false};buildCedro=[pscustomobject]@{log=$a.log;testes=$total;falhas=$falhas;erros=$erros;ignorados=$ignorados;xmls=$xmls.Count;executadoPorEsteLeitor=$false};fontes=$hashes;semLeituraJavaAtual=$true;semSqlJvmH2BuildRedeAmbienteSegredos=$true;pendente='Nova tarefa e manifesto exato p2-locks; nao aceita BE14/guardas nem homologa SQL Server.'}|ConvertTo-Json -Depth 12
    if($div.Count){exit 1}
}catch{
    $codigo='BLOQUEADO_LEITURA_HISTORICA_OU_HASH';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{bloqueio=$codigo;rawExposto=$false;semSqlJvmBuildRedeAmbiente=$true;comparacaoBackend='NAO_EXECUTADA'}|ConvertTo-Json
    exit 1
}
