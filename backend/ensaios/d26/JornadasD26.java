import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

final class JornadasD26 {
    static Map<String,Object> m(Object...pairs){return EnsaioD26.m(pairs);}
    final EnsaioD26 x;int number=100;
    final String G="GESTOR",S="SUPERVISOR",O="OPERACAO";
    JornadasD26(EnsaioD26 x){this.x=x;}
    JsonNode get(String path)throws Exception{return x.call("consulta "+path,"GET",path,null,O,200);}
    JsonNode post(String name,String path,Object body,String role,int expected)throws Exception{return x.call(name,"POST",path,body,role,expected);}
    String pe(long id){return "/api/v1/pedidos-entrada/"+id;}
    String ps(JsonNode p){return "/api/v1/pedidos-saida/"+p.get("id").longValue();}
    String unit(JsonNode u){return "/api/v1/unidades-logisticas/"+u.get("codigo").asString();}
    long pv(long id)throws Exception{return get(pe(id)).get("pedido").get("versao").longValue();}
    void run()throws Exception{
        auth();cadastros();recebimentoEstoqueSaida();financeiro();contagemContingencia();
    }
    void auth()throws Exception{
        x.phase="auth";var inventory=x.json.readTree(java.nio.file.Files.readString(x.evidence.resolve("d26-inventario.json")));
        for(var e:inventory){String path=e.get("rota").asString().replaceAll("\\{[^}]+}","1");String method=e.get("metodo").asString();x.call("anonimo "+e.get("id").asString(),method,path,method.equals("GET")?null:Map.of(),null,path.equals("/api/v1/status")?200:401);}
        var bad=List.of(m("aud",List.of("outro")),m("iss","https://invalido.local"),m("exp",Date.from(Instant.now().minusSeconds(120))),m("iat",Date.from(Instant.now().plusSeconds(300))),m("wms_perfil","ADMIN"),m("wms_clientes",List.of("0")),m("wms_armazens",List.of(1)),m("sub",""));
        int n=0;for(var b:bad)x.callToken("JWT claims invalido "+(++n),"GET","/api/v1/clientes",null,G,x.token(G,b),401);
        String token=x.token(G,Map.of());int at=token.lastIndexOf('.')+1;String altered=token.substring(0,at)+(token.charAt(at)=='A'?'B':'A')+token.substring(at+1);
        x.callToken("JWT assinatura invalida","GET","/api/v1/clientes",null,G,altered,401);
        x.call("rota inexistente","GET","/api/v1/rota-d26-inexistente",null,G,403);
    }
    void cadastros()throws Exception{
        x.precheck("cadastros",false);
        var c=m("codigo",x.round+"C","nome",x.round+" Cliente ficticio Ação","documentoFiscal",x.round+"C");
        post("cliente operador recusado","/api/v1/clientes",c,O,403);
        var client=post("cliente criado","/api/v1/clientes",c,G,201);x.clientId=client.get("id").longValue();x.ids.put("clienteId",x.clientId);
        post("cliente duplicado sem nova linha","/api/v1/clientes",c,G,409);
        x.call("cliente validacao","POST","/api/v1/clientes",m("codigo","","nome","","documentoFiscal",""),G,400);
        var wh=post("armazem criado","/api/v1/armazens",m("codigo",x.round+"A","nome",x.round+" Armazem ficticio","documentoFiscal",x.round+"A","cidade","Osasco","uf","SP"),G,201);x.warehouseId=wh.get("id").longValue();x.ids.put("armazemId",x.warehouseId);
        for(String role:List.of(G,S,O))x.call("JWT real perfil "+role,"GET","/api/v1/clientes/"+x.clientId,null,role,200);
        x.callToken("cliente fora alcance","GET","/api/v1/clientes/"+x.clientId,null,O,x.token(O,m("wms_clientes",List.of())),403);
        var filtered=x.callToken("lista filtra alcance","GET","/api/v1/clientes",null,O,x.token(O,m("wms_clientes",List.of())),200);x.assertion("lista fora alcance vazia",filtered.get("itens").isEmpty(),0,filtered.get("itens").size());
        x.call("cliente inexistente","GET","/api/v1/clientes/9223372036854775806",null,G,404);
        x.call("paginacao invalida","GET","/api/v1/clientes?pagina=-1&tamanho=1001",null,G,400);
        x.call("cliente atualizado colunas permitidas","PUT","/api/v1/clientes/"+x.clientId,m("versao",client.get("versao").longValue(),"nome",x.round+" Cliente revisado Ação","motivo",x.round+" revisao ficticia"),G,200);
        x.product=post("produto criado","/api/v1/produtos",m("clienteId",x.clientId,"sku",x.round+"SKU","descricao",x.round+" Produto ficticio","unidadeMedida","UN","tipoQuantidade","CONTAGEM","precisaoQuantidade",0,"controlaLote",false,"controlaValidade",false),G,201);
        x.pack=post("DUN criado","/api/v1/embalagens",m("produtoId",x.product.get("id").longValue(),"codigoDun",x.round+"DUN","descricao",x.round+" Configuracao DUN ficticia","quantidadeProduto",10),G,201);x.ids.put("produtoId",x.product.get("id").longValue());x.ids.put("embalagemId",x.pack.get("id").longValue());x.save();
        x.call("produto filtro paginado","GET","/api/v1/produtos?clienteId="+x.clientId+"&pagina=0&tamanho=1",null,O,200);
        x.call("DUN filtro paginado","GET","/api/v1/embalagens?produtoId="+x.product.get("id").longValue()+"&pagina=0&tamanho=1",null,O,200);
    }
    JsonNode address(String suffix,String type)throws Exception{
        var e=post("endereco "+type,"/api/v1/enderecos",m("armazemId",x.warehouseId,"codigo",x.round+suffix,"rua",x.round,"nivel",1,"posicao",suffix,"descricao",x.round+" Posicao ficticia","tipo",type,"sequenciaColeta",++number),G,201);
        return x.call("capacidade "+type,"PUT","/api/v1/enderecos/"+e.get("id").longValue()+"/capacidade",m("versao",e.get("versao").longValue(),"tipoUnidadePermitido","PALLET","limites",m("pesoKg",1000,"alturaMetros",2,"larguraMetros",2,"profundidadeMetros",2,"empilhamentoMaximo",2),"motivo",x.round+" capacidade ficticia"),G,200);
    }
    long entry(String suffix,int quantity)throws Exception{
        long id=post("pedido entrada "+suffix,"/api/v1/pedidos-entrada",m("clienteId",x.clientId,"armazemId",x.warehouseId,"referencia",x.round+suffix),O,201).get("id").longValue();
        post("nota manual ficticia",pe(id)+"/notas",m("versao",pv(id),"serie",1,"numero",++number,"emissao",LocalDate.now().minusDays(3).toString(),"itens",List.of(m("numeroItem",1,"produtoId",x.product.get("id").longValue(),"quantidadePrevista",quantity,"valorMercadoria",quantity*10))),O,200);
        post("iniciar conferencia",pe(id)+"/iniciar-conferencia",m("versao",pv(id),"motivo",x.round+" conferencia fisica"),O,200);return id;
    }
    void arrival(long id,int quantity,Instant when)throws Exception{
        long item=get(pe(id)).get("notas").get(0).get("itens").get(0).get("id").longValue();
        var body=m("versao",pv(id),"operacaoId",UUID.randomUUID().toString(),"chegouEm",when.toString(),"observacao",x.round+" chegada ficticia","itens",List.of(m("itemNotaId",item,"quantidadeBoa",quantity,"quantidadeAvariada",0)));
        var a=post("chegada fisica",pe(id)+"/chegadas",body,O,200);var replay=post("chegada replay",pe(id)+"/chegadas",body,O,200);x.assertion("chegada replay igual",a.equals(replay),a,replay);
    }
    JsonNode unitize(long pe,long entry,int quantity)throws Exception{
        var cmd=m("operacaoId",UUID.randomUUID().toString(),"versaoPedido",pv(pe),"motivo",x.round+" unitizacao ficticia","unidades",List.of(m("embalagemId",x.pack.get("id").longValue(),"tipo","PALLET","condicao","BOA","quantidade",quantity)));
        var a=post("unitizar",pe(pe)+"/entradas/"+entry+"/unitizacao",cmd,O,200);var b=post("unitizacao replay",pe(pe)+"/entradas/"+entry+"/unitizacao",cmd,O,200);x.assertion("unitizacao replay igual",a.equals(b),a,b);return a.get("unidades").get(0).get("unidade");
    }
    JsonNode actual(JsonNode u)throws Exception{return get(unit(u)).get("unidade");}
    List<Map<String,Object>> destinations(JsonNode e){return List.of(m("enderecoId",e.get("id").longValue(),"codigoLido",e.get("codigo").asString()));}
    Map<String,Object> move(JsonNode u,JsonNode e){var d=x.command();d.put("versaoUnidade",u.get("versao").longValue());d.put("destinos",destinations(e));d.put("medidas",m("pesoKg",500,"alturaMetros",1,"larguraMetros",1,"profundidadeMetros",1,"empilhamento",1,"posicoesNecessarias",1));return d;}
    JsonNode order(String suffix,int quantity)throws Exception{var d=x.command();d.putAll(m("clienteId",x.clientId,"armazemId",x.warehouseId,"referencia",x.round+suffix,"itens",List.of(m("produtoId",x.product.get("id").longValue(),"quantidade",quantity))));return post("pedido saida "+suffix,"/api/v1/pedidos-saida",d,O,201).get("pedido");}
    JsonNode balance()throws Exception{return get("/api/v1/estoque/saldo?clienteId="+x.clientId+"&armazemId="+x.warehouseId+"&produtoId="+x.product.get("id").longValue());}
    JsonNode remaining,stored;long entryId,noteId;JsonNode storageA,storageB;
    void recebimentoEstoqueSaida()throws Exception{
        x.precheck("recebimento",false);entryId=entry("ENT",100);x.ids.put("pedidoEntradaId",entryId);
        arrival(entryId,50,Instant.now().minusSeconds(172800));arrival(entryId,50,Instant.now().minusSeconds(86400));
        var ef=m("versao",pv(entryId),"motivo",x.round+" conferencia integral","aceitarDivergencias",false);
        post("efetivacao operador negada",pe(entryId)+"/efetivacao",ef,O,403);post("efetivacao supervisor",pe(entryId)+"/efetivacao",ef,S,200);
        var entries=get(pe(entryId)+"/entradas").get("itens");x.assertion("duas entradas parciais",entries.size()==2,2,entries.size());
        var a=unitize(entryId,entries.get(0).get("id").longValue(),50);var b=unitize(entryId,entries.get(1).get("id").longValue(),50);
        noteId=get(pe(entryId)).get("notas").get(0).get("id").longValue();x.ids.put("notaId",noteId);x.ids.put("unidades",List.of(a,b));x.save();
        var label=get(unit(b)+"/etiqueta");x.assertion("etiqueta reimpressao sem mutacao",label.equals(get(unit(b)+"/etiqueta")),label,"mesmo conteudo");
        storageA=address("ARM1","ARMAZENAGEM");storageB=address("ARM2","ARMAZENAGEM");
        var movement=move(a,storageA);var mv=post("enderecar A",unit(a)+"/movimentos",movement,O,200);var rep=post("movimento replay",unit(a)+"/movimentos",movement,O,200);x.assertion("movimento replay igual",mv.equals(rep),mv,rep);
        post("enderecar B",unit(b)+"/movimentos",move(b,storageB),O,200);a=actual(a);b=actual(b);
        x.assertion("saldo fisico 100",balance().get("fisicoTotal").decimalValue().intValueExact()==100,100,balance().get("fisicoTotal"));
        var insufficient=order("SEM-SALDO",101);post("saldo insuficiente integral",ps(insufficient)+"/reserva",x.version(insufficient),O,409);x.assertion("recusa nao reserva",balance().get("reservado").decimalValue().signum()==0,0,balance().get("reservado"));
        concurrency();
        var p=order("RET",60);get(ps(p)+"/fifo");var rc=x.version(p);var r=post("reserva parcial pallet integral pedido",ps(p)+"/reserva",rc,O,200);var rr=post("reserva replay",ps(p)+"/reserva",rc,O,200);x.assertion("reserva replay igual",r.equals(rr),r,rr);p=r.get("pedido");
        x.assertion("reserva FIFO 50+10",p.get("reservas").size()==2&&p.get("reservas").get(0).get("quantidade").decimalValue().intValueExact()==50,2,p.get("reservas"));
        var sepA=address("SEP1","SEPARACAO");var sepB=address("SEP2","SEPARACAO");p=separate(p,a,sepA);p=separate(p,b,sepB);
        String xml=xml(60,"1");var doc=x.version(p);var covers=new ArrayList<Map<String,Object>>();for(var reserve:p.get("reservas"))covers.add(m("reservaId",reserve.get("id").longValue(),"notaOrigemId",noteId,"sku",x.product.get("sku").asString(),"quantidade",reserve.get("quantidade").decimalValue()));doc.putAll(m("origem","XML","natureza","RETORNO_MERCADORIA","xml",xml,"protocolo",x.round+" documento FICTICIO","coberturas",covers));
        p=post("cobertura fiscal ficticia",ps(p)+"/documentos",doc,S,200).get("expedicao").get("pedido");
        var withdraw=x.version(p);long reserveB=reserve(p,b);withdraw.putAll(m("xmls",List.of(xml),"remanescentes",List.of(m("reservaId",reserveB,"destinos",destinations(storageB)))));
        post("retirada operador negada",ps(p)+"/retirada",withdraw,O,403);var w=post("retirada integral supervisor",ps(p)+"/retirada",withdraw,S,200);var wrep=post("retirada replay",ps(p)+"/retirada",withdraw,S,200);x.assertion("retirada replay igual",w.equals(wrep),w,wrep);
        p=w.get("expedicao").get("pedido");x.ids.put("pedidoSaidaId",p.get("id").longValue());x.ids.put("baixas",w.get("expedicao").get("baixas"));
        remaining=actual(b);stored=remaining;x.assertion("remanescente conserva codigo",remaining.get("codigo").equals(b.get("codigo")),b.get("codigo"),remaining.get("codigo"));x.assertion("saldo remanescente 40",balance().get("fisicoTotal").decimalValue().intValueExact()==40,40,balance());x.save();
    }
    long reserve(JsonNode p,JsonNode u){for(var r:p.get("reservas"))if(r.get("unidadeId").longValue()==u.get("id").longValue())return r.get("id").longValue();throw new IllegalStateException("Reserva fixture ausente");}
    JsonNode separate(JsonNode p,JsonNode u,JsonNode e)throws Exception{var d=x.version(p);d.putAll(m("reservaId",reserve(p,u),"codigoLido",u.get("codigo").asString(),"revisaoConteudo",get(unit(u)+"/etiqueta").get("versaoConteudo").longValue()));p=post("leitura etiqueta",""+ps(p)+"/leituras",d,O,200).get("expedicao").get("pedido");d=x.version(p);d.put("destinacao",m("reservaId",reserve(p,u),"destinos",destinations(e)));return post("separacao fisica",ps(p)+"/separacoes",d,O,200).get("expedicao").get("pedido");}
    void concurrency()throws Exception{
        x.precheck("concorrencia",false);var p=order("CONC1",100);var q=order("CONC2",100);var go=new CountDownLatch(1);var ready=new CountDownLatch(2);
        // Mesma quantidade cobre todo saldo. Barreira do cliente inicia ambos sem depender de vencedor.
        try(var pool=Executors.newFixedThreadPool(2)){
            var futures=new ArrayList<Future<Integer>>();for(var order:List.of(p,q))futures.add(pool.submit(()->{var d=x.version(order);String jwt=x.token(O,Map.of());var req=java.net.http.HttpRequest.newBuilder(java.net.URI.create(x.base+ps(order)+"/reserva")).timeout(Duration.ofSeconds(40)).header("Content-Type","application/json").header("Authorization","Bearer "+jwt).POST(java.net.http.HttpRequest.BodyPublishers.ofString(x.json.writeValueAsString(d))).build();ready.countDown();go.await();var res=x.http.send(req,java.net.http.HttpResponse.BodyHandlers.ofString());synchronized(x){x.cases.add(m("caso","concorrencia reserva barreira","metodo","POST","rota",ps(order)+"/reserva","perfil",O,"expected","um 200 outro 409","actual",res.statusCode(),"resposta",x.json.readTree(res.body()),"estado","confrontar conjunto"));x.save();}return res.statusCode();}));
            if(!ready.await(10,TimeUnit.SECONDS))throw new IllegalStateException("D26_BARREIRA_CLIENTE_FALHOU");go.countDown();var statuses=new ArrayList<Integer>();for(var f:futures)statuses.add(f.get(45,TimeUnit.SECONDS));Collections.sort(statuses);x.assertion("concorrencia somente um integral",statuses.equals(List.of(200,409)),List.of(200,409),statuses);
        }finally{go.countDown();}
        for(var order:List.of(p,q)){var state=get(ps(order)).get("pedido");if(state.get("situacao").asString().equals("RESERVADO"))post("reversao vencedor concorrencia",ps(state)+"/reversao-reserva",x.version(state),S,200);else post("cancelar perdedor concorrencia",ps(state)+"/cancelamento",x.version(state),O,200);}
        x.assertion("reversao restaura reserva zero",balance().get("reservado").decimalValue().signum()==0,0,balance());
    }
    String xml(int quantity,String digit){return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe Id=\"NFe"+digit.repeat(44)+"\" versao=\"4.00\"><ide><mod>55</mod><serie>1</serie><nNF>"+(++number)+"</nNF><dhEmi>"+Instant.now().minusSeconds(3600)+"</dhEmi></ide><emit><CNPJ>98765432000188</CNPJ></emit><det nItem=\"1\"><prod><cProd>"+x.product.get("sku").asString()+"</cProd><uCom>UN</uCom><qCom>"+quantity+"</qCom><vProd>"+quantity*10+".00</vProd></prod></det></infNFe></NFe>";}
    void financeiro()throws Exception { /* cenarios definidos abaixo antes da fase SQL */ }
    void contagemContingencia()throws Exception { /* cenarios definidos abaixo antes da fase SQL */ }
}
