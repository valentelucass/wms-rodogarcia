import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Provas novas: 1000 produtos/2 IDs/3 posicoes; retirada100 de500; origem e historia. */
final class EstoqueSaidaD29 {
 final EnsaioD29 x;final JornadasD29 j;long pe;
 EstoqueSaidaD29(EnsaioD29 x){this.x=x;j=new JornadasD29(x);}
 static Map<String,Object> m(Object...a){return EnsaioD29.m(a);}
 JsonNode post(String n,String r,Object b,String role,int status)throws Exception{return x.call(n,"POST",r,b,role,status);}
 JsonNode get(String r)throws Exception{return j.get(r);}
 JsonNode current(JsonNode p)throws Exception{return get(j.ps(p));}
 void eq(String n,JsonNode a,String f,int expected)throws Exception{x.assertion(n,a.path(f).decimalValue().intValueExact()==expected,expected,a);}
 void run()throws Exception{
  x.ids.put("oraculosSQL",m("quantidade","1000total/1000unitizado; 2IDs500 ativos apos divisao100+400 e reagrupamento; identidade consumida0 inativa preservada","ocupacao","500 primeira1posicao +500 segunda2posicoes=3ocupadas, sem sobreposicao; movimento preservaetiqueta","retirada","pedido100 integral; umPALLET500->400 mesmoUUID/origem; saldo1000->900, reserva0; documento nao retira; replay nao duplica","avaria","10ocorridas reais sobre400: mantémreserva50 e fisico900, pedidoimpedido; reversaoexplicita zerareserva"));x.save();
  if(x.ids.containsKey("retomadaDe")){retomar();return;}
  j.cadastros();x.precheck("estoque-recebimento",false);pe=j.entry("EST",1000);x.ids.put("pedidoEntradaId",pe);j.arrival(pe,1000,Instant.now());
  post("D29 estoque efetivacao integral",j.pe(pe)+"/efetivacao",m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" conferir1000 ficticios"),"SUPERVISOR",200);
  long en=get(j.pe(pe)+"/entradas").path("itens").get(0).path("id").asLong();
  var units=post("D29 estoque2pallets500",j.pe(pe)+"/entradas/"+en+"/unitizacao",m("operacaoId",UUID.randomUUID().toString(),"versaoPedido",j.pv(pe),"motivo",x.round+" dois500","unidades",List.of(m("embalagemId",x.ids.get("embalagemId"),"tipo","PALLET","condicao","BOA","quantidade",500),m("embalagemId",x.ids.get("embalagemId"),"tipo","PALLET","condicao","BOA","quantidade",500))),"OPERACAO",200);
  JsonNode a=units.path("unidades").get(0).path("unidade"),b=units.path("unidades").get(1).path("unidade");x.ids.put("unidades",List.of(a,b));x.save();
  var d=x.version(a);d.put("quantidadeNovaUnidade",100);String route=j.pe(pe)+"/unidades/"+a.path("id").asLong();
  post("D29 divisaoOperacao403",route+"/divisao",d,"OPERACAO",403);
  var split=post("D29 divisao500em400e100",route+"/divisao",d,"SUPERVISOR",200);x.assertion("D29 divisao replay mesmoUUID",split.equals(post("D29 replaydivisao",route+"/divisao",d,"SUPERVISOR",200)),split,"snapshot unico");
  JsonNode child=null;for(var u:split.path("unidades")){var v=u.path("unidade");if(v.path("id").asLong()!=a.path("id").asLong())child=v;}
  a=j.actual(a);eq("D29 original400 apos divisao",a,"quantidade",400);eq("D29 novo100 aposdivisao",child,"quantidade",100);eq("D29 divisao conserva1000",j.balance(),"fisicoTotal",1000);
  d=x.command();d.putAll(m("versaoDestino",a.path("versao").asLong(),"origens",List.of(m("unidadeId",child.path("id").asLong(),"versao",child.path("versao").asLong()))));
  var joined=post("D29 reagrupamento mesmaorigem",route+"/reagrupamento",d,"SUPERVISOR",200);x.assertion("D29 reagrup replay unico",joined.equals(post("D29 replayreagrupamento",route+"/reagrupamento",d,"SUPERVISOR",200)),joined,"snapshot unico");
  a=j.actual(a);eq("D29 reagrupado500",a,"quantidade",500);var closed=j.actual(child);x.assertion("D29 filho0 encerrado permanececonsultavel",!closed.path("ativa").asBoolean()&&closed.path("quantidade").decimalValue().signum()==0,"inativo/0",closed);x.ids.put("unidadeEncerrada",closed);
  var triage=j.balance();eq("D29 triagemsemreserva",triage,"disponivel",0);
  post("D29 triagemrecusasaida","/api/v1/pedidos-saida",m("operacaoId",UUID.randomUUID().toString(),"clienteId",x.clientId,"armazemId",x.warehouseId,"referencia",x.round+"TRI","itens",List.of(m("produtoId",x.ids.get("produtoId"),"quantidade",1)),"motivo",x.round+" triagemrecusada"),"OPERACAO",409);
  x.precheck("estoque-capacidade",false);JsonNode s1=j.address("S1","ARMAZENAGEM"),s2=j.address("S2","ARMAZENAGEM"),s3=j.address("S3","ARMAZENAGEM"),s4=j.address("S4","ARMAZENAGEM");
  var group=post("D29 conjunto2posicoes","/api/v1/conjuntos-posicoes",m("armazemId",x.warehouseId,"codigo",x.round+"G2","enderecoAId",s2.path("id").asLong(),"enderecoBId",s3.path("id").asLong(),"limites",m("pesoKg",1000,"alturaMetros",2,"larguraMetros",2,"profundidadeMetros",2,"empilhamentoMaximo",2),"motivo",x.round+" conjuntoficticio"),"GESTOR",201);
  var label=get(j.unit(a)+"/etiqueta");var mv=j.move(a,s4);post("D29 mover500primeira1posicao",j.unit(a)+"/movimentos",mv,"OPERACAO",200);a=j.actual(a);
  d=j.move(b,s2);d.putAll(m("conjuntoId",group.path("id").asLong(),"destinos",List.of(m("enderecoId",s2.path("id").asLong(),"codigoLido",s2.path("codigo").asString()),m("enderecoId",s3.path("id").asLong(),"codigoLido",s3.path("codigo").asString()))));((Map<String,Object>)d.get("medidas")).put("posicoesNecessarias",2);
  post("D29 mover500segunda2posicoes",j.unit(b)+"/movimentos",d,"OPERACAO",200);b=j.actual(b);
  x.assertion("D29 duasposicoes juntas ocupadas",get(j.unit(b)+"/estoque").path("posicoes").size()==2,2,get(j.unit(b)+"/estoque"));
  var old=get(j.unit(a)+"/estoque");var occupied=j.move(a,s2);occupied.remove("medidas");post("D29 sobreposicaorecusada",j.unit(a)+"/movimentos",occupied,"OPERACAO",409);x.assertion("D29 recusaocupacao conservaestado",old.equals(get(j.unit(a)+"/estoque")),old,get(j.unit(a)+"/estoque"));
  d=x.command();d.putAll(m("versaoUnidade",a.path("versao").asLong(),"destinos",j.destinations(s1)));post("D29 remanejarsemreiniciardata",j.unit(a)+"/movimentos",d,"OPERACAO",200);a=j.actual(a);
  x.assertion("D29 etiquetaidentica apos2movimentos",label.equals(get(j.unit(a)+"/etiqueta")),label,get(j.unit(a)+"/etiqueta"));eq("D29 disponivel1000 aposenderecar",j.balance(),"disponivel",1000);
  x.ids.put("unidades",List.of(a,b,closed));x.ids.put("enderecos",List.of(s1,s2,s3,s4));x.ids.put("conjunto",group);x.save();
  retirada(a,s1);
 }
 void retomar()throws Exception{
  var prior=x.json.readTree(java.nio.file.Files.readAllBytes(x.evidence.resolve("d29-"+x.ids.get("retomadaDe")+"-http.json")));
  if(!prior.path("bloqueio").asString().startsWith("D29 sobreposicaorecusada expected=409 actual=400"))throw new IllegalStateException("D29_RETOMADA_ESTOQUE_PONTO_DIVERGENTE");
  x.product=get("/api/v1/produtos/"+x.ids.get("produtoId"));x.pack=get("/api/v1/embalagens/"+x.ids.get("embalagemId"));pe=((Number)x.ids.get("pedidoEntradaId")).longValue();var units=x.json.valueToTree(x.ids.get("unidades"));var a=j.actual(units.get(0));var b=j.actual(units.get(1));
  JsonNode s1=null,s2=null;for(var c:prior.path("cases")){var resp=c.path("resposta");if(c.path("metodo").asString().equals("POST")&&c.path("rota").asString().equals("/api/v1/enderecos")&&c.path("actual").asInt()==201){if(resp.path("codigo").asString().endsWith("S1"))s1=get("/api/v1/enderecos/"+resp.path("id").asLong());if(resp.path("codigo").asString().endsWith("S2"))s2=get("/api/v1/enderecos/"+resp.path("id").asLong());}}
  if(s1==null||s2==null)throw new IllegalStateException("D29_DESTINO_RETOMADA_AUSENTE");
  var before=get(j.unit(a)+"/estoque");var label=get(j.unit(a)+"/etiqueta");var d=j.move(a,s2);d.remove("medidas");post("D29 sobreposicao corpo valido",j.unit(a)+"/movimentos",d,"OPERACAO",409);x.assertion("D29 ocupacao recusa conservaestado",before.equals(get(j.unit(a)+"/estoque")),before,get(j.unit(a)+"/estoque"));
  d=x.command();d.putAll(m("versaoUnidade",a.path("versao").asLong(),"destinos",j.destinations(s1)));post("D29 remanejarsemreiniciardata",j.unit(a)+"/movimentos",d,"OPERACAO",200);a=j.actual(a);x.assertion("D29 etiquetaidentica aposremanejamento",label.equals(get(j.unit(a)+"/etiqueta")),label,get(j.unit(a)+"/etiqueta"));eq("D29 disponivel1000",j.balance(),"disponivel",1000);retirada(a,s1);
 }
 void retirada(JsonNode a,JsonNode storage)throws Exception{
  x.precheck("saida-fisica",false);var p=j.order("RET100",100);x.ids.put("pedidoSaidaId",p.path("id").asLong());var before=j.balance();var fifo=get(j.ps(p)+"/fifo");x.assertion("D29 somenteFIFOsemreserva",before.equals(j.balance()),before,j.balance());
  x.assertion("D29 FIFO100 seleciona primeiro500",fifo.path("selecoes").get(0).path("unidadeId").asLong()==a.path("id").asLong(),a.path("id"),fifo);
  var reserve=x.version(p);var r=post("D29 reserva100integral",j.ps(p)+"/reserva",reserve,"OPERACAO",200);post("D29 replayreserva100",j.ps(p)+"/reserva",reserve,"OPERACAO",200);p=r.path("pedido");eq("D29 reserva100fisico1000",j.balance(),"fisicoTotal",1000);eq("D29 reservado100",j.balance(),"reservado",100);eq("D29 disponivel500e400protegido",j.balance(),"disponivel",500);eq("D29 bloqueadoremanescente400",j.balance(),"bloqueado",400);
  long rid=p.path("reservas").get(0).path("id").asLong();var tag=get(j.unit(a)+"/etiqueta");var d=x.version(p);d.putAll(m("reservaId",rid,"codigoLido",a.path("codigo").asString(),"revisaoConteudo",tag.path("versaoConteudo").asLong()));p=post("D29 leituraetiqueta",j.ps(p)+"/leituras",d,"OPERACAO",200).path("expedicao").path("pedido");
  var sep=j.address("SEP","SEPARACAO");d=x.version(p);d.put("destinacao",m("reservaId",rid,"destinos",j.destinations(sep)));p=post("D29 separacaofisica",j.ps(p)+"/separacoes",d,"OPERACAO",200).path("expedicao").path("pedido");
  String xml=xml(100);d=x.version(p);d.putAll(m("origem","XML","natureza","RETORNO_MERCADORIA","xml",xml,"protocolo",x.round+" FICTICIO SEM EMISSAO EXTERNA","coberturas",List.of(m("reservaId",rid,"notaOrigemId",p.path("reservas").get(0).path("notaOrigemId").asLong(),"sku",x.product.path("sku").asString(),"quantidade",100))));
  p=post("D29 documentoexistentelocal",j.ps(p)+"/documentos",d,"SUPERVISOR",200).path("expedicao").path("pedido");eq("D29 fiscalnaoretirafisico",j.balance(),"fisicoTotal",1000);
  d=x.version(p);d.putAll(m("xmls",List.of(xml),"remanescentes",List.of(m("reservaId",rid,"destinos",j.destinations(storage)))));post("D29 retiradaOperacao403",j.ps(p)+"/retirada",d,"OPERACAO",403);var w=post("D29 retirada100depallet500",j.ps(p)+"/retirada",d,"SUPERVISOR",200);x.assertion("D29 retirada replayunico",w.equals(post("D29 replayretirada100",j.ps(p)+"/retirada",d,"SUPERVISOR",200)),w,"snapshot unico");
  var remaining=j.actual(a);eq("D29 restante400",remaining,"quantidade",400);eq("D29 fisico900",j.balance(),"fisicoTotal",900);eq("D29 reservado0retirada",j.balance(),"reservado",0);x.assertion("D29 origemFIFOidentidadepreservada",remaining.path("codigo").equals(a.path("codigo"))&&remaining.path("dataFifo").equals(a.path("dataFifo"))&&remaining.path("notaId").equals(a.path("notaId")),a,remaining);x.ids.put("retirada",w);x.ids.put("remanescente",remaining);x.save();
  avaria(remaining);
 }
 String xml(int qty)throws Exception{String key=String.format("%044d",new java.math.BigInteger(1,x.round.getBytes(java.nio.charset.StandardCharsets.UTF_8)));String emit=get("/api/v1/armazens/"+x.warehouseId).path("documentoFiscal").asString();return "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe Id='NFe"+key+"' versao='4.00'><ide><mod>55</mod><serie>1</serie><nNF>"+(x.nextDocumentNumber())+"</nNF><dhEmi>"+Instant.now()+"</dhEmi></ide><emit><CNPJ>"+emit+"</CNPJ></emit><det nItem='1'><prod><cProd>"+x.product.path("sku").asString()+"</cProd><uCom>UN</uCom><qCom>"+qty+"</qCom><vProd>1000.00</vProd></prod></det></infNFe></NFe>";}
 void avaria(JsonNode u)throws Exception{
  x.precheck("avaria-real",false);var p=j.order("AVRES50",50);p=post("D29 reserva50antesavaria",j.ps(p)+"/reserva",x.version(p),"OPERACAO",200).path("pedido");u=j.actual(u);var q=j.address("Q","QUARENTENA");var d=x.command();d.putAll(m("versaoUnidade",u.path("versao").asLong(),"quantidade",10,"ocorridaEm",Instant.now().toString(),"destinos",j.destinations(q)));
  var av=post("D29 avaria10apreserva50","/api/v1/estoque/unidades/"+u.path("codigo").asString()+"/avarias",d,"SUPERVISOR",200);x.ids.put("avaria",av);x.ids.put("pedidoReservaAvariaId",p.path("id").asLong());x.save();
  post("D29 revalidacaobloqueadaavaria",j.ps(p)+"/revalidacao",null,"OPERACAO",409);var live=current(p);x.assertion("D29 avariasinalizasemprometersaida",!live.path("podeProsseguir").asBoolean()&&live.path("reservas").get(0).path("situacao").asString().equals("ATIVA"),"impedido/reservaATIVA",live);eq("D29 avariareservapreservada50",j.balance(),"reservado",50);eq("D29 avariafisico900",j.balance(),"fisicoTotal",900);
  var rev=x.version(live);post("D29 reversaoOperacao403",j.ps(live)+"/reversao-reserva",rev,"OPERACAO",403);var rr=post("D29 reversaoexpressaavaria",j.ps(live)+"/reversao-reserva",rev,"SUPERVISOR",200);x.assertion("D29 reversaoreplayoriginal",rr.equals(post("D29 replayreversaoavaria",j.ps(live)+"/reversao-reserva",rev,"SUPERVISOR",200)),rr,"unico");eq("D29 reversaoreserva0fisico900",j.balance(),"reservado",0);
 }
}
