import tools.jackson.databind.JsonNode;
import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.w3c.dom.*;

/** Doc24:101: documentos bem formados, limites medidos antes do HTTP; sem fiscal externo. */
final class XmlFronteirasD29 {
 final EnsaioD29 x; final JornadasD29 j; final PendenciasD29 p;
 XmlFronteirasD29(EnsaioD29 x){this.x=x;j=new JornadasD29(x);p=new PendenciasD29(x);}
 static Map<String,Object> m(Object...v){return EnsaioD29.m(v);}
 static int depth(Node n){int max=0;for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling())if(c instanceof Element)max=Math.max(max,depth(c));return 1+max;}
 void fixture(String name,String xml,int bytes,int levels,int lines)throws Exception {
  var f=DocumentBuilderFactory.newDefaultInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  var root=f.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
  var metrics=m("UTF8bytes",xml.getBytes(StandardCharsets.UTF_8).length,"caracteresUTF16",xml.length(),"profundidade",depth(root),"linhas",root.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe","det").getLength(),"bemFormado",true);
  x.assertion("D29 precondicao XML independente "+name,(int)metrics.get("UTF8bytes")==bytes&&(int)metrics.get("profundidade")==levels&&(int)metrics.get("linhas")==lines,m("bytes",bytes,"depth",levels,"linhas",lines),metrics);
 }
 void run()throws Exception {
  if(x.ids.containsKey("retomadaDe"))throw new IllegalStateException("D29_XML_FRONTEIRAS_RETOMADA_EXIGE_FASE_EXPLICITA");
  x.ids.put("oraculosSQL",m("fontes",List.of("docs/24-pedido-saida-fifo-e-reserva.md:99,101","D29-VIG-P11","D29-L-P08"),"esperado","Nova familia MEDIDA precisao6 com500 confirmados/unitizados/disponiveis. XML bem formado:1000000bytesUTF8/depth64/200linhas aceitos201; margem+1 recusa400 sem novo pedido. nfeProc aceito; decimal6 soma3.000003; decimal7/soma alem DECIMAL19,6 recusados400; maximo exato numericamente valido recusa409 saldo insuficiente, nunca alegado positivo fisico. Nenhum XML altera estoque/reserva.","limite","Nao verifica assinatura/SEFAZ, arquivo ou rede externa; nota/importacao documental nao movimenta fisico."));x.save();j.cadastros();
  x.ids.put("produtoContagemAuxiliar",x.product);x.ids.put("embalagemContagemAuxiliar",x.pack);
  x.product=p.post("D29 SKU MEDIDA precisao6","/api/v1/produtos",m("clienteId",x.clientId,"sku",x.round+"DEC6","descricao",x.round+" Produto ficticio decimal6","unidadeMedida","UN","tipoQuantidade","MEDIDA","precisaoQuantidade",6,"controlaLote",false,"controlaValidade",false),"GESTOR",201);
  x.pack=p.post("D29 DUN MEDIDA","/api/v1/embalagens",m("produtoId",x.product.path("id").asLong(),"codigoDun",x.round+"DEC6DUN","descricao",x.round+" DUN ficticio","quantidadeProduto",10),"GESTOR",201);x.ids.put("produtoId",x.product.path("id").asLong());x.ids.put("embalagemId",x.pack.path("id").asLong());x.save();
  x.precheck("XML-fronteiras-fixture-real",false);long pe=j.entry("XML-FRONTEIRAS",500);j.arrival(pe,500,Instant.now());p.post("D29 XML base efetivar500",j.pe(pe)+"/efetivacao",m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" base decimal integral"),"SUPERVISOR",200);long en=j.get(j.pe(pe)+"/entradas").path("itens").get(0).path("id").asLong();var u=j.unitize(pe,en,500);var address=j.address("XML500","ARMAZENAGEM");p.post("D29 XML base endereco500",j.unit(u)+"/movimentos",j.move(u,address),"OPERACAO",200);x.ids.put("pedidoEntradaId",pe);x.ids.put("unidadeXMLBase",j.actual(u));x.save();var stock=j.balance();x.assertion("D29 XML base500 livre",stock.path("fisicoTotal").decimalValue().intValueExact()==500&&stock.path("disponivel").decimalValue().intValueExact()==500,"500livres",stock);
  for(String name:List.of("UTF8exato1000000","UTF8mais1","profundidade64","profundidade65","linhas200","linhas201","nfeProc","decimal6","decimal7","somaOverflow","maximoValidoSaldoInsuficiente")) {
   x.precheck("XML-fronteira-"+name,false);var d=p.xmlPayload(x.nextDocumentNumber(),"2","3");String xml=d.get("xml").toString();int expected=201,levels=5,lines=2;
   if(name.startsWith("UTF8")){int target=name.equals("UTF8exato1000000")?1000000:1000001;String tags="<infAdic><infCpl></infCpl></infAdic>";int pad=target-xml.getBytes(StandardCharsets.UTF_8).length-tags.length();String text="é".repeat(pad/2)+(pad%2==1?"x":"");xml=xml.replace("</infNFe>","<infAdic><infCpl>"+text+"</infCpl></infAdic></infNFe>");if(name.endsWith("mais1"))expected=400;}
   if(name.startsWith("profundidade")){levels=name.endsWith("64")?64:65;xml=xml.replace("<ide>","<extra>".repeat(levels-2)+"ok"+"</extra>".repeat(levels-2)+"<ide>");if(levels==65)expected=400;}
   if(name.startsWith("linhas")){lines=name.endsWith("200")?200:201;int start=xml.indexOf("<det "),end=xml.indexOf("</det>",start)+6;String det=xml.substring(start,end).replace("<qCom>2</qCom>","<qCom>1</qCom>");StringBuilder all=new StringBuilder();for(int n=1;n<=lines;n++)all.append(det.replace("nItem='1'","nItem='"+n+"'"));xml=xml.substring(0,start)+all+"</infNFe></NFe>";if(lines==201)expected=400;}
   if(name.equals("nfeProc")){xml="<nfeProc xmlns='http://www.portalfiscal.inf.br/nfe' versao='4.00'>"+xml+"</nfeProc>";levels=6;}
   if(name.equals("decimal6"))xml=xml.replace("<qCom>2</qCom>","<qCom>1.000001</qCom>").replace("<qCom>3</qCom>","<qCom>2.000002</qCom>");
   if(name.equals("decimal7")){xml=xml.replace("<qCom>2</qCom>","<qCom>1.0000001</qCom>");expected=400;}
   if(name.equals("somaOverflow")||name.equals("maximoValidoSaldoInsuficiente")){xml=xml.replace("<qCom>2</qCom>",name.equals("somaOverflow")?"<qCom>9999999999999.999999</qCom>":"<qCom>9999999999999.999998</qCom>").replace("<qCom>3</qCom>","<qCom>0.000001</qCom>");expected=name.equals("somaOverflow")?400:409;}
   fixture(name,xml,xml.getBytes(StandardCharsets.UTF_8).length,levels,lines);d.put("xml",xml);var before=p.get("D29 XML lista antes "+name,"/api/v1/pedidos-saida"+p.ctx()+"&tamanho=100","OPERACAO");var response=p.post("D29 XML fronteira "+name,"/api/v1/pedidos-saida/xml",d,"OPERACAO",expected);var after=p.get("D29 XML lista depois "+name,"/api/v1/pedidos-saida"+p.ctx()+"&tamanho=100","OPERACAO");
   if(expected==201){String qty=name.equals("decimal6")?"3.000003":name.equals("linhas200")?"200":"5";x.assertion("D29 XML efeito documental exato "+name,response.path("documento").path("itens").size()==lines&&response.path("pedido").path("itens").size()==1&&response.path("pedido").path("itens").get(0).path("quantidade").decimalValue().compareTo(new java.math.BigDecimal(qty))==0&&after.path("totalItens").asLong()==before.path("totalItens").asLong()+1,m("linhas",lines,"soma",qty,"novoPedido",1),response);}else{x.assertion("D29 XML recusa semefeito "+name,before.equals(after),before,after);p.code("D29 XML codigo "+name,response,expected==409?"SALDO_INSUFICIENTE":"DADOS_INVALIDOS");}
   x.assertion("D29 XML nao cria fisico nem reserva "+name,stock.equals(j.balance()),stock,j.balance());
  }
 }
}
