$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$inventario=Get-Content -LiteralPath "$backend/evidencias/d26-inventario.json" -Raw -Encoding UTF8|ConvertFrom-Json
$todos=New-Object 'Collections.Generic.List[object]'
$rodadas=New-Object 'Collections.Generic.List[object]'
foreach($file in Get-ChildItem "$backend/evidencias/d26-*-http.json"|Sort-Object Name){
 $d=Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8|ConvertFrom-Json
 if($d.fase -like 'offline*' -or -not $d.ids.jarPid){continue}
 $rodadas.Add([pscustomobject]@{rodada=$d.rodada;fase=$d.fase;bloqueio=$d.bloqueio;casos=@($d.cases).Count;jarSha256=$d.ids.jarSha256;jarEncerrado=$d.ids.jarEncerrado;baseRodada=$d.ids.baseRodada})
 $n=0;foreach($c in $d.cases){$n++;if(-not $c.metodo){continue};$todos.Add([pscustomobject]@{rodada=$d.rodada;caso=$c.caso;numero=$n;metodo=$c.metodo;rota=($c.rota -split '\?')[0];perfil=$c.perfil;expected=$c.expected;actual=$c.actual;estado=$c.estado;cookiePresente=$c.cookiePresente;segredoPresente=$c.segredoPresente;requestIdPresente=$c.requestIdPresente;arquivo=$file.Name;query=$c.rota})}
}
$rows=New-Object 'Collections.Generic.List[object]'
$md=New-Object 'Collections.Generic.List[string]'
$md.Add('# D26 — matriz objetiva das 160 rotas')
$md.Add('')
$md.Add('Fotografia gerada dos JSONs reais do JAR; tentativas e recusas anteriores preservadas. HTTP 2xx autenticado é cobertura positiva da rota, não aprovação de todas as regras ou homologação. GET vazio prova contrato/acesso, não uma transição de negócio. HTTP401 anônimo fica separado. Cada caso completo contém payload fictício, expected/actual, perfil, GETs/assertivas e IDs. SQL é prova independente nos recortes de Prumo; nunca inferido de H2.')
$md.Add('')
$md.Add('| ID / fonte | Método e rota | Perfis exercitados (incluem recusas) | 2xx / recusa autenticada / anônimo | Estado e provas HTTP |')
$md.Add('| --- | --- | --- | --- | --- |')
foreach($e in $inventario){
 $pattern='^'+([regex]::Escape($e.rota) -replace '\\\{[^}]+}','[^/]+')+'$'
 $hits=@($todos|Where-Object {$_.metodo -ceq $e.metodo -and $_.rota -cmatch $pattern})
 $positive=@($hits|Where-Object {$_.perfil -cne 'anonimo' -and $_.actual -is [ValueType] -and $_.actual -ge 200 -and $_.actual -lt 300 -and $_.estado -ceq 'aprovado'})
 $negative=@($hits|Where-Object {$_.perfil -cne 'anonimo' -and $_.actual -is [ValueType] -and $_.actual -ge 400 -and $_.estado -ceq 'aprovado'})
 $anon=@($hits|Where-Object {$_.perfil -ceq 'anonimo' -and $_.estado -ceq 'aprovado'})
 $status=if($positive.Count){'testado HTTP; regras/persistência por caso'}elseif($negative.Count){'recusa autenticada comprovada; positivo pendente'}else{'negócio não executado; barreira anônima separada'}
 $reason=if($e.id -eq 'E052'){'BLOQUEADO: ajuste gera AJUSTE_ESTOQUE ausente do CHECK aplicado de fato_permanencia; 409 real D2694ED1B4D. Migration futura separada, não aplicada.'}elseif(-not $positive.Count){'Não executado positivamente: conferir motivo concreto na matriz de regras; nenhum aceite inferido de 401.'}else{'Cobertura HTTP delimitada aos casos abaixo. Matriz de regras d26-regras.md relaciona GET/SQL exato e limites; nenhuma certificação SQL automática por rota.'}
 if($e.id -eq 'E052'){$status='falhou no SQL real; bloqueado por schema'}
 $profiles=(@($hits|Where-Object {$_.perfil -cne 'anonimo'}|Select-Object -ExpandProperty perfil -Unique) -join ', ')
 $refs=@($positive|Select-Object -First 2|ForEach-Object {"[$($_.rodada):$($_.numero)]($($_.arquivo)) $($_.expected)/$($_.actual)"}) -join '; '
 if(-not $refs){$refs=@($negative|Select-Object -First 1|ForEach-Object {"[$($_.rodada):$($_.numero)]($($_.arquivo)) $($_.expected)/$($_.actual)"}) -join '; '}
 $rows.Add([pscustomobject]@{id=$e.id;controller=$e.controller;metodo=$e.metodo;rota=$e.rota;documentos=$e.docs;regras=$e.regras;perfisExercitados=$profiles;perfisCom2xx=@($positive|Select-Object -ExpandProperty perfil -Unique);perfisCom403=@($hits|Where-Object {$_.actual -eq 403 -and $_.perfil -cne 'anonimo'}|Select-Object -ExpandProperty perfil -Unique);positivoHTTP=$positive.Count -gt 0;negativaAutenticada=$negative.Count -gt 0;barreiraAnonima=$anon.Count -gt 0;estado=$status;motivo=$reason;casos=$hits;GET_SQL_auditoria='Ver d26-regras.md/json e recorte por família/IDs/instante; sem atestação automática por endpoint.'})
 $md.Add("| $($e.id) docs$($e.docs) $($e.regras) | $($e.metodo) $($e.rota) | $profiles | $($positive.Count) / $($negative.Count) / $($anon.Count) | $status. $refs |")
}
$summary=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');endpoints=$rows.Count;comPositivoHTTP=@($rows|Where-Object positivoHTTP).Count;comRecusaAutenticada=@($rows|Where-Object negativaAutenticada).Count;semPositivoHTTP=@($rows|Where-Object {-not $_.positivoHTTP}).Count;backendTotalAprovado=$false;rodadas=$rodadas;matriz=$rows}
[IO.File]::WriteAllText("$backend/evidencias/d26-matriz.json",($summary|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$backend/evidencias/d26-matriz.md",($md -join "`n"),[Text.UTF8Encoding]::new($false))
[pscustomobject]@{endpoints=$rows.Count;positivos=$summary.comPositivoHTTP;semPositivo=$summary.semPositivoHTTP}|ConvertTo-Json -Compress
