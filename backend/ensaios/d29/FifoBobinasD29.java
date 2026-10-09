import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;

/** Doc24:23/68/89 e VigiaP12: bobinas indivisiveis e autorizacao da excecao. */
final class FifoBobinasD29 {
 final EnsaioD29 x; final JornadasD29 j; final PendenciasD29 p;
 FifoBobinasD29(EnsaioD29 x){this.x=x;j=new JornadasD29(x);p=new PendenciasD29(x);}
 static Map<String,Object> m(Object...v){return EnsaioD29.m(v);}
 JsonNode post(String n,String r,Object d,String role,int code)throws Exception{return x.call(n,"POST",r,d,role,code);}
 JsonNode bobina(String suffix,int qty)throws Exception {
  x.precheck("fifo-bobina"+qty,false);long pe=j.entry(suffix,qty);j.arrival(pe,qty,Instant.now());
  post("D29 bobina efetivar"+qty,j.pe(pe)+"/efetivacao",m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" bobina integral"),"SUPERVISOR",200);
  long en=j.get(j.pe(pe)+"/entradas").path("itens").get(0).path("id").asLong();
  var cmd=x.command();cmd.putAll(m("versaoPedido",j.pv(pe),"unidades",List.of(m("embalagemId",x.pack.path("id").asLong(),"tipo","BOBINA","condicao","BOA","quantidade",qty))));
  var u=post("D29 unitizacao BOBINA"+qty,j.pe(pe)+"/entradas/"+en+"/unitizacao",cmd,"OPERACAO",200).path("unidades").get(0).path("unidade");
  var e=j.address("B"+qty,"ARMAZENAGEM");
  e=x.call("D29 capacidade BOBINA"+qty,"PUT","/api/v1/enderecos/"+e.path("id").asLong()+"/capacidade",m("versao",e.path("versao").asLong(),"tipoUnidadePermitido","BOBINA","limites",m("pesoKg",1000,"alturaMetros",2,"larguraMetros",2,"profundidadeMetros",2,"empilhamentoMaximo",2),"motivo",x.round+" capacidade bobina ficticia"),"GESTOR",200);
  post("D29 enderecar BOBINA"+qty,j.unit(u)+"/movimentos",j.move(u,e),"OPERACAO",200);u=j.actual(u);x.ids.put("bobina"+qty,u);x.ids.put("pedidoEntradaBobina"+qty,pe);x.save();return u;
 }
 void balance(String n,int reserved,int available)throws Exception {var b=j.balance();x.assertion(n,b.path("fisicoTotal").decimalValue().intValueExact()==10&&b.path("reservado").decimalValue().intValueExact()==reserved&&b.path("disponivel").decimalValue().intValueExact()==available&&b.path("bloqueado").decimalValue().signum()==0,"fisico10/res"+reserved+"/disp"+available+"/bloq0",b);}
 void run()throws Exception {
  if(x.ids.containsKey("retomadaDe"))throw new IllegalStateException("D29_FIFO_RETOMADA_EXIGE_FASE_EXPLICITA");
  x.ids.put("oraculosSQL",m("fontes",List.of("docs/24-pedido-saida-fifo-e-reserva.md:23,68,89","D29-VIG-P12"),"esperado","Chegadas reais ordenadas: BOBINA4 anterior a6. FIFO pedido6 recusa409 sem prefixo reservado; pedido4 escolhe4, selecionar4 de6 recusa400; Operacao justifica6 sem reserva e nao autoriza; Supervisor reserva6; reversao explicita libera6; novo pedido10 FIFO4+6 reserva integral10. Recusas nao mudam versoes/quantidades/reservas.","limite","Nao prova empate de datas, lock nativo, coletor ou dispositivo fisico."));x.save();j.cadastros();var b4=bobina("ANTIGA4",4);var b6=bobina("RECENTE6",6);
  x.assertion("D29 FIFO datas reais estritamente ordenadas",Instant.parse(b4.path("dataFifo").asString()).isBefore(Instant.parse(b6.path("dataFifo").asString())),"4 antes6 sem retrodata",m("4",b4,"6",b6));balance("D29 bobinas agregado10 livre",0,10);
  var six=j.order("SEIS",6);var four=j.order("QUATRO",4);x.ids.put("pedidoBobina6",six.path("id").asLong());x.ids.put("pedidoBobina4",four.path("id").asLong());x.save();
  p.code("D29 FIFO6 conflito integral",x.call("D29 FIFO greedy4+6 nao fecha6","GET",j.ps(six)+"/fifo",null,"OPERACAO",409),"SALDO_INSUFICIENTE");var before=j.get(j.ps(six));p.code("D29 reserva6 mesma recusa",post("D29 reserva6 sem excecao",j.ps(six)+"/reserva",x.version(six),"OPERACAO",409),"SALDO_INSUFICIENTE");x.assertion("D29 recusa6 nao reserva prefixo",before.equals(j.get(j.ps(six))),before,j.get(j.ps(six)));balance("D29 recusas6 preservam10",0,10);
  var fifo4=j.get(j.ps(four)+"/fifo");x.assertion("D29 FIFO4 bobina4 inteira",fifo4.path("selecoes").size()==1&&fifo4.path("selecoes").get(0).path("unidadeId").equals(b4.path("id"))&&fifo4.path("selecoes").get(0).path("quantidade").decimalValue().intValueExact()==4,"bobina4 inteira",fifo4);
  var partial=x.version(four);partial.put("selecoes",List.of(m("unidadeId",b6.path("id").asLong(),"quantidade",4)));var before4=j.get(j.ps(four));p.code("D29 BOBINA6 nao fraciona4",post("D29 selecao parcial bobina400",j.ps(four)+"/justificativas-fifo",partial,"SUPERVISOR",400),"DADOS_INVALIDOS");x.assertion("D29 recusa parcial sem efeito",before4.equals(j.get(j.ps(four))),before4,j.get(j.ps(four)));
  var manual=x.version(six);manual.put("selecoes",List.of(m("unidadeId",b6.path("id").asLong(),"quantidade",6)));var justified=post("D29 Operacao registra proposta excepcional6",j.ps(six)+"/justificativas-fifo",manual,"OPERACAO",200);x.assertion("D29 justificativa nao e reserva",justified.path("excecaoFifo").asBoolean()&&justified.path("pedido").path("reservas").isEmpty(),"excecao/RASCUNHO/0res",justified);balance("D29 proposta nao altera saldo",0,10);
  var reserve=x.version(justified.path("pedido"));reserve.put("justificativaId",manual.get("operacaoId"));var snap=j.get(j.ps(six));post("D29 Operacao nao autoriza excecao",j.ps(six)+"/reserva",reserve,"OPERACAO",403);x.assertion("D29 permissao recusada sem efeito",snap.equals(j.get(j.ps(six))),snap,j.get(j.ps(six)));
  var ok=post("D29 Supervisor reserva bobina6 integral",j.ps(six)+"/reserva",reserve,"SUPERVISOR",200);x.assertion("D29 autorizacao excepcional6",ok.path("excecaoFifo").asBoolean()&&ok.path("pedido").path("reservas").size()==1&&ok.path("pedido").path("reservas").get(0).path("unidadeId").equals(b6.path("id")),"bobina6 integral/excecao",ok);post("D29 replay excecao Operacao403",j.ps(six)+"/reserva",reserve,"OPERACAO",403);balance("D29 reserva6 resto4",6,4);
  var reversed=post("D29 reversao explicita bobina6",j.ps(six)+"/reversao-reserva",x.version(ok.path("pedido")),"SUPERVISOR",200);x.ids.put("reversaoBobina6",reversed);balance("D29 reversao6 livre10",0,10);
  var ten=j.order("DEZ",10);x.ids.put("pedidoBobina10",ten.path("id").asLong());x.save();var fifo10=j.get(j.ps(ten)+"/fifo");x.assertion("D29 FIFO10 ordem4mais6",fifo10.path("selecoes").size()==2&&fifo10.path("selecoes").get(0).path("unidadeId").equals(b4.path("id"))&&fifo10.path("selecoes").get(1).path("unidadeId").equals(b6.path("id")),"4+6 nestaordem",fifo10);balance("D29 sugestao10 sem reserva",0,10);
  var tenOk=post("D29 pedido10 integral duasbobinas",j.ps(ten)+"/reserva",x.version(ten),"OPERACAO",200);x.assertion("D29 reserva10 sem excecao",!tenOk.path("excecaoFifo").asBoolean()&&tenOk.path("pedido").path("reservas").size()==2,"2reservas/semexcecao",tenOk);balance("D29 final fisico10 reserva10",10,0);x.ids.put("reservaBobina10",tenOk);x.save();
 }
}
