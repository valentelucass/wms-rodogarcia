from pathlib import Path
import json, hashlib, datetime, subprocess

backend = Path(__file__).resolve().parents[2]
dest = backend / 'ensaios/d28'
mapping = {
    'EnsaioD27': 'EnsaioD28', 'JornadasD27': 'JornadasD28',
    'PendenciasD27': 'PendenciasD28', 'VariantesD27': 'VariantesD28',
    'ReadinessD27Test': 'ReadinessD28Test',
    'RespostaConcorrenteD27Test': 'RespostaConcorrenteD28Test',
    'D27': 'D28', 'd27': 'd28',
}
sources = []
for name in ['FixtureHttps.java', 'EnsaioD27.java', 'JornadasD27.java',
             'PendenciasD27.java', 'VariantesD27.java', 'ReadinessD27Test.java',
             'RespostaConcorrenteD27Test.java', 'preparar.ps1', 'executar.ps1']:
    src = backend / 'ensaios/d27' / name
    text = src.read_text(encoding='utf-8')
    for old, new in mapping.items(): text = text.replace(old, new)
    text = text.replace('target-d28-numerico-final', 'target-d28-atual')
    target_name = name
    for old, new in mapping.items(): target_name = target_name.replace(old, new)
    if name == 'EnsaioD27.java':
        text = text.replace('new PendenciasD28(x).run(args[0]);',
            'if (args[0].equals("carga-temporal")) new RegressaoD28(x).run();\n'
            '            else new PendenciasD28(x).run(args[0]);\n'
            '            x.holdForSelect();')
        text = text.replace('var x = new EnsaioD28();',
            'if (args.length == 1 && args[0].equals("fixture-processo-bloqueado")) {\n'
            '            Thread.sleep(30000); return;\n        }\n'
            '        var x = new EnsaioD28();', 1)
        text = text.replace('public static void main(String[] args)', '''void holdForSelect() throws Exception {
        phase = "aguardando-select-prumo";
        ids.put("httpTerminadoUtc", Instant.now().toString());
        ids.put("contratoSQL", "d28-select-" + round + ".json");
        save();
        var signal = backend.getParent().resolve("orchestracao/.runtime/d28-prumo-select-liberar-" + round + ".json");
        long deadline = System.nanoTime() + Duration.ofHours(2).toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(signal)) {
                var r = json.readTree(Files.readAllBytes(signal));
                if (r.path("rodada").asString().equals(round)
                        && r.path("DB_NAME").asString().equals("WMS_DEV")
                        && r.path("login").asString().equals("WMSDEV")
                        && r.path("leiturasConcluidas").asBoolean(false)) {
                    ids.put("prumoSelectSinal", r); return;
                }
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("D28_SELECT_PRUMO_PRAZO_SEM_SINAL");
    }

    public static void main(String[] args)''')
        text = text.replace('evidence.resolve("d28-persistencia-pedido.json")',
                            'evidence.resolve("d28-select-" + round + ".json")')
    if name == 'executar.ps1':
        text = text.replace("'concorrencia')", "'concorrencia','carga-temporal')")
        begin = text.index("  $readyFile=")
        end = text.index("  . (Join-Path $raiz", begin)
        text = text[:begin] + '''  $readyFile=Join-Path $raiz 'orchestracao/.runtime/d28-prumo-preflight.json'
  if(-not(Test-Path -LiteralPath $readyFile)){throw 'D28_PREFLIGHT_PRUMO_AUSENTE'}
  $r=Get-Content -LiteralPath $readyFile -Encoding UTF8 -Raw|ConvertFrom-Json
  . "$PSScriptRoot/guardas.ps1"
  Assert-D28Preflight $r
  $psi.EnvironmentVariables['WMS_D28_V10_CHECKSUM']='-562012523'
''' + text[end:]
        text = text.replace("-or $p.server -cne $r.servidor", "-or $p.server -cne 'ROD-SRVW-001'")
    target = dest / target_name
    if target.exists(): raise RuntimeError(f'preservar preexistente: {target.name}')
    target.write_text(text, encoding='utf-8')
    sources.append({'origem': str(src.relative_to(backend)), 'shaOrigem': hashlib.sha256(src.read_bytes()).hexdigest(),
                    'derivadoD28': str(target.relative_to(backend)), 'shaInicialD28': hashlib.sha256(target.read_bytes()).hexdigest()})

src = backend / 'ensaios/d26/ProcessoLimitadoD26Test.java'
text = src.read_text(encoding='utf-8').replace('D26','D28').replace('d26','d28')
(dest / 'ProcessoLimitadoD28Test.java').write_text(text, encoding='utf-8')
prep = dest / 'preparar.ps1'
text = prep.read_text(encoding='utf-8').replace('"$PSScriptRoot/ReadinessD28Test.java"',
    '"$PSScriptRoot/ReadinessD28Test.java" "$PSScriptRoot/ProcessoLimitadoD28Test.java" "$PSScriptRoot/RegressaoD28.java"')
prep.write_text(text, encoding='utf-8')
(backend / 'evidencias/d28-fontes-derivacao.json').write_text(json.dumps(sources, ensure_ascii=False, indent=2), encoding='utf-8')
status = subprocess.run(['git','status','--short'], cwd=backend.parent, capture_output=True).stdout.decode('utf-8')
(backend / 'evidencias/d28-git-baseline.txt').write_text(status, encoding='utf-8')
(backend / 'evidencias/d28-cedro-progresso.json').write_text(json.dumps({
    'incremento':'D28','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'fase':'PREPARO_OFFLINE','build':'target-d28-atual','sqlOperado':False,
    'pendencias':['preflight Prumo válido','compilar auxiliares D28','HTTP novo/SELECT novo/revisão'],
    'preservacao':'Fontes D26/D27 e dirty preexistente preservados; nenhum IT vazio.'
}, ensure_ascii=False, indent=2), encoding='utf-8')
