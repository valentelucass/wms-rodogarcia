from pathlib import Path
import json,hashlib,re
root=Path(__file__).resolve().parents[3];b=root/'backend';out=b/'ensaios/d29';e=b/'evidencias';items=[]
names=['EnsaioD28.java','FixtureHttps.java','JornadasD28.java','PendenciasD28.java','VariantesD28.java','RegressaoD28.java','guardas.ps1','executar.ps1','versionar.ps1']
for n in names:
 p=b/'ensaios/d28'/n;dst=out/n.replace('D28','D29');s=p.read_text(encoding='utf-8').replace('D28','D29').replace('d28','d29')
 if n=='EnsaioD28.java':
  start=s.index('    public static void main(');end=s.index('    boolean portFree(',start)
  s=s[:start]+'''    public static void main(String[] args) throws Exception {
        var x=new EnsaioD29();
        x.ids.put("helperPid",ProcessHandle.current().pid());
        x.ids.put("etapa",args[0]);
        try {
            if(args.length>2){
                if(!args[2].matches("D29[A-F0-9]{8}"))throw new IllegalStateException("D29_BASE_INVALIDA");
                var prior=x.json.readTree(Files.readAllBytes(x.evidence.resolve("d29-"+args[2]+"-http.json")));
                x.ids.putAll(x.json.convertValue(prior.path("ids"),new tools.jackson.core.type.TypeReference<Map<String,Object>>(){}));
                x.ids.put("retomadaDe",args[2]);x.clientId=((Number)x.ids.get("clienteId")).longValue();x.warehouseId=((Number)x.ids.get("armazemId")).longValue();
            }
            x.ids.put("helperPid",ProcessHandle.current().pid());x.ids.put("etapa",args[0]);
            x.ids.put("helperVersao","D29-H1");x.ids.put("holdPrazoMinutos",120);
            x.ids.put("helperFontesCompilados",x.json.readTree(Files.readAllBytes(x.backend.resolve("target-d29-helper/versao-atual.json"))));
            x.precheck("inicio",false);
            if(!args[0].equals("precheck")){
                var work=x.backend.resolve("target-d29-helper/"+x.round);Files.createDirectories(work);x.startJar(Path.of(args[1]),work);
                D29Suite.run(x,args[0]);x.finalReads();x.holdForSelect();x.precheck("fim",false);
            }
            x.phase="concluido";
        }catch(Exception err){x.blocker=err instanceof CaseFailure||err instanceof IllegalStateException&&err.getMessage()!=null&&err.getMessage().startsWith("D29_")?err.getMessage():"D29_FALHA_VER_EVIDENCIA";x.phase="falhou";}
        finally{x.stopApp();if(x.fixture!=null)x.fixture.close();x.ids.put("jarEncerrado",x.app==null||!x.app.isAlive());x.ids.put("issuerEncerrado",true);x.ids.put("portasLivres",x.portFree("porta")&&x.portFree("issuerPorta"));x.save();}
        System.out.println("D29 rodada="+x.round+" fase="+x.phase+" bloqueio="+(x.blocker==null?"nenhum":"EVIDENCIA_PRESERVADA"));
        if(x.blocker!=null)System.exit(1);
    }

''' +s[end:]
  s=s.replace('"cases",\n                        cases,','"cases",\n                        cases,')
  s=s.replace('"metodo",\n                        method,','"instanteRespostaUtc",\n                        Instant.now().toString(),\n                        "metodo",\n                        method,')
 if n=='PendenciasD28.java':
  start=s.index('    void concorrencia()');end=s.index('    JsonNode concurrentCall(',start);s=s[:start]+s[end:]
  s=s.replace('case "concorrencia" -> concorrencia();','case "concorrencia" -> throw new IllegalStateException("D29_NATIVE_NAO_EXECUTAVEL");')
 if n=='executar.ps1':
  s=re.sub(r"\[ValidateSet\([^\n]+?\)\]\[string\]\$Etapa='listas'","[ValidateSet('precheck','entrada','estoque','saida','fiscal-servicos','carga-contagem','financeiro','concorrencia','get-final')][string]$Etapa='precheck'",s)
  s='\n'.join(l for l in s.splitlines() if 'D29_R16_MODO_FRESCO_BLOQUEADO' not in l)+'\n'
 if n in ['JornadasD28.java','VariantesD28.java','RegressaoD28.java']:
  # Fatos operacionais novos recebem instante atual. Cortes futuros sao somente previsoes.
  s=re.sub(r'Instant\.now\(\)\.minusSeconds\([^()]+\)', 'Instant.now()',s)
 if dst.exists():raise RuntimeError('PRESERVAR_D29_PREEXISTENTE:'+dst.name)
 dst.write_text(s,encoding='utf-8');items.append({'origem':p.relative_to(root).as_posix(),'origemSHA256':hashlib.sha256(p.read_bytes()).hexdigest().upper(),'novo':dst.relative_to(root).as_posix(),'novoSHA256':hashlib.sha256(dst.read_bytes()).hexdigest().upper()})
(e/'d29-fontes-derivacao.json').write_text(json.dumps(items,indent=2),encoding='utf-8')
print('D29 fontes derivadas sem modificar D28; native gate removido; dispatcher novo pendente')
