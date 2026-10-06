param([string]$Resultado = 'backend/evidencias/d26-inventario.json')
$ErrorActionPreference='Stop'
$raiz = (Resolve-Path "$PSScriptRoot/../../..").Path
$itens = @()
$mapa = @{
 Cliente='14|RN06,AC12'; Armazem='14|RN04'; Produto='14|RN06,AC12'; Embalagem='14,20|RN15,AC10'; Endereco='14,22|RN05,RN18'; FiscalCadastro='29|RN07,AC02'; ImportacaoEndereco='29|RN05'; Auditoria='14|RN29';
 PedidoEntrada='18|RN12,RN13,RN14,AC03'; UnidadeLogistica='20|RN10,RN11,RN15,RN16,AC10'; Capacidade='22|RN18,AC04,AC05'; Estoque='22|RN19,RN20,RN21,AC04'; IndicadorEstoque='22,35|RN28'; PedidoSaida='24|RN22,RN23,RN24,AC10,AC11'; Expedicao='27|RN25,RN26,AC01,AC02'; Avaria='27|RN17,RN29,AC06'; ConfiguracaoCobranca='29|RN08,RN27,AC04,AC07,AC08'; FatoServico='29|RN09,RN27,AC06'; CalculoCobranca='29|RN27,AC04,AC07,AC08'; FechamentoCobranca='31|RN27,RN29,AC02'; AjusteFechamento='31|RN27,RN29'; Contagem='31|RN20,RN29'; Contingencia='31,35|RN29'; CargaInicial='31|RN02,RN29'; Encerramento='31|RN29,AC12'; Status='33|BE15'
}
foreach($arquivo in Get-ChildItem "$raiz/backend/src/main/java/br/com/rodogarcia/wms/controllers/*Controller.java" | Sort-Object Name) {
 $fonte=[IO.File]::ReadAllText($arquivo.FullName)
 $base=[regex]::Match($fonte,'@RequestMapping\("([^"]+)"\)').Groups[1].Value
 foreach($m in [regex]::Matches($fonte,'@(Get|Post|Put|Delete|Patch)Mapping(?:\((?<args>[\s\S]*?)\))?\s*(?:@ResponseStatus\([^)]*\)\s*)?(?<decl>public[\s\S]*?)\{')) {
  $suffix=[regex]::Match($m.Groups['args'].Value,'"([^"]*)"').Groups[1].Value
  $decl=$m.Groups['decl'].Value.Trim() -replace '\s+',' '
  $controller=$arquivo.BaseName -replace 'Controller$',''
  $refs=$mapa[$controller].Split('|')
  $itens += [ordered]@{id=('E{0:D3}' -f ($itens.Count+1));metodo=$m.Groups[1].Value.ToUpper();rota=$base+$suffix;controller=$arquivo.BaseName;linha=1+($fonte.Substring(0,$m.Index).Split("`n").Count-1);contrato=$decl;docs=$refs[0];regras=$refs[1];perfil='Conferir filtro JWT e restricao no service por caso';status='nao executado';motivo='Ensaio D26 ainda nao executado; depende identidade atestada/IT/JAR';casos=@();persistencia='GET/SQL/auditoria pendentes'}
 }
}
if($itens.Count -ne 160){throw "Inventario diverge: $($itens.Count)"}
$saida=Join-Path $raiz $Resultado
[IO.File]::WriteAllText($saida,($itens|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$md=@('# D26 - inventario e matriz de endpoints','',"160 metodos em 26 controllers. Fonte: codigo atual. RN/AC apontam requisitos relacionados; AC continuam propostas. Aprovacao requer casos reais, nao apenas existencia da rota.",'','| ID | Metodo / rota | Docs / RN / AC | Contrato (arquivo:linha) | Estado / motivo |','| --- | --- | --- | --- | --- |')
foreach($e in $itens){$md+="| $($e.id) | $($e.metodo) $($e.rota) | $($e.docs) / $($e.regras) | $($e.controller):$($e.linha) | $($e.status): $($e.motivo) |"}
[IO.File]::WriteAllLines((Join-Path $raiz 'backend/evidencias/d26-matriz.md'),$md,[Text.UTF8Encoding]::new($false))
Write-Output "Inventario: $($itens.Count) metodos / $(($itens.controller|Select-Object -Unique).Count) controllers"
