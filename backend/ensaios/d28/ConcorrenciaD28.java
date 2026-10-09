import tools.jackson.databind.JsonNode;
import java.time.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Par HTTP e testemunho da JVM própria. Não afirma acesso a DMV nem witness SQL online. */
final class ConcorrenciaD28 {
    final EnsaioD28 x;
    final PendenciasD28 p;
    final JornadasD28 j;
    ConcorrenciaD28(EnsaioD28 x) { this.x=x; p=new PendenciasD28(x); j=p.j; }
    void run() throws Exception {
        j.cadastros(); j.prepararRecebimento();
        var cancel=j.order("CANCELPERFIL",1);
        var cancelCommand=x.version(cancel);
        p.post("D28 cancelamento Operação recusado",j.ps(cancel)+"/cancelamento",cancelCommand,"OPERACAO",403);
        x.callToken("D28 cancelamento Supervisor sem alcance recusado","POST",j.ps(cancel)+"/cancelamento",cancelCommand,"SUPERVISOR",x.token("SUPERVISOR",EnsaioD28.m("wms_clientes",List.of())),403);
        var cancelled=p.post("D28 cancelamento Supervisor permitido",j.ps(cancel)+"/cancelamento",cancelCommand,"SUPERVISOR",200);
        x.assertion("D28 cancelamento replay autorizado único",cancelled.equals(p.post("D28 cancelamento replay Supervisor",j.ps(cancel)+"/cancelamento",cancelCommand,"SUPERVISOR",200)),"mesmo snapshot",cancelled);
        p.post("D28 cancelamento replay não permite Operação",j.ps(cancel)+"/cancelamento",cancelCommand,"OPERACAO",403);
        x.ids.put("pedidoCancelamentoPerfisId",cancel.get("id").longValue());
        var a=j.order("CONCA",80); var b=j.order("CONCB",80);
        var pa=x.version(a); var pb=x.version(b);
        x.ids.put("concorrenciaPedidos",List.of(a.get("id").longValue(),b.get("id").longValue()));
        x.ids.put("concorrenciaOperacoes",List.of(pa.get("operacaoId"),pb.get("operacaoId")));
        x.ids.put("witnessSQLOnline",false);
        x.ids.put("limiteWitness","DMV WMSDEV recusada SQL300; observador nativo não autorizado no escopo atual. Par HTTP/JVM e SELECT posterior são provas separadas.");
        x.save();
        var exec=Executors.newFixedThreadPool(2);
        JsonNode ar,br;
        try(var gate=x.sqlDataSource().getConnection()) {
            try(var st=gate.createStatement();var r=st.executeQuery("SELECT DB_NAME(),ORIGINAL_LOGIN(),USER_NAME()")) {
                if(!r.next() || !"WMS_DEV".equals(r.getString(1)) || !"WMSDEV".equals(r.getString(2)) || !"WMSDEV".equals(r.getString(3))) throw new IllegalStateException("D28_GATE_ALVO_RECUSADO");
            }
            gate.setAutoCommit(false);
            try(var st=gate.createStatement();var r=st.executeQuery("SELECT id FROM wms.cliente WITH(UPDLOCK,HOLDLOCK) WHERE id="+x.clientId)) {
                if(!r.next()) throw new IllegalStateException("D28_GATE_CLIENTE_AUSENTE");
            }
            x.ids.put("gateAtivoUtc",Instant.now().toString()); x.save();
            String ta=x.token("OPERACAO",Map.of()),tb=x.token("OPERACAO",Map.of());
            var fa=exec.submit(()->p.concurrentCall("reserva concorrente A",j.ps(a)+"/reserva",pa,ta));
            var fb=exec.submit(()->p.concurrentCall("reserva concorrente B",j.ps(b)+"/reserva",pb,tb));
            boolean observed=false;
            long until=System.nanoTime()+Duration.ofSeconds(12).toNanos();
            while(System.nanoTime()<until && !fa.isDone() && !fb.isDone()) {
                var proc=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","jcmd.exe").toString(),Long.toString(x.app.pid()),"Thread.print").start();
                String dump=EnsaioD28.processOutputLimited(proc,3000);
                if(!x.secret.isEmpty()) dump=dump.replace(x.secret,"[SEGREDO_OMITIDO]");
                Files.writeString(x.evidence.resolve("d28-"+x.round+"-jvm-concorrencia.txt"),dump);
                int count=0;
                for(String thread:dump.split("(?m)^\\\"")) if(thread.startsWith("http-nio-") && thread.contains("com.microsoft.sqlserver.jdbc") && thread.contains("PedidoSaidaService.reservar")) count++;
                x.ids.put("threadsHttpJdbcReserva",count);
                if(count>=2) { observed=true; break; }
                Thread.sleep(150);
            }
            x.ids.put("witnessJVM",observed); x.save();
            gate.rollback(); x.ids.put("gateLiberadoUtc",Instant.now().toString());
            ar=fa.get(45,TimeUnit.SECONDS); br=fb.get(45,TimeUnit.SECONDS);
        } finally { exec.shutdownNow(); }
        var sa=j.get(j.ps(a)); var sb=j.get(j.ps(b));
        boolean aw=sa.get("situacao").asString().equals("RESERVADO"),bw=sb.get("situacao").asString().equals("RESERVADO");
        var statuses=x.cases.stream().filter(c->List.of("reserva concorrente A","reserva concorrente B").contains(c.get("caso"))).map(c->((Number)c.get("actual")).intValue()).sorted().toList();
        var saldo=j.balance();
        x.assertion("D28 JVM própria mostra duas requests JDBC reserva sob gate",Boolean.TRUE.equals(x.ids.get("witnessJVM")),"duas threads HTTP/JDBC reservando na JVM própria; não witness SQL",x.ids.get("threadsHttpJdbcReserva"));
        x.assertion("D28 concorrência um200/outro409 e perdedor sem reserva",statuses.equals(List.of(200,409)) && aw!=bw && (aw?sa:sb).get("reservas").size()==2 && (aw?sb:sa).get("reservas").isEmpty() && saldo.get("fisicoTotal").decimalValue().intValueExact()==100 && saldo.get("reservado").decimalValue().intValueExact()==80,"par200/409,fisico100,reservado80,perdedorRASCUNHO",EnsaioD28.m("a",sa,"b",sb,"saldo",saldo));
        var winner=aw?a:b; var loser=aw?b:a;
        var replay=p.post("D28 concorrência vencedor replay",j.ps(winner)+"/reserva",aw?pa:pb,"OPERACAO",200);
        p.post("D28 concorrência perdedor replay",j.ps(loser)+"/reserva",aw?pb:pa,"OPERACAO",409);
        x.assertion("D28 concorrência replay preserva saldo",saldo.equals(j.balance()),saldo,j.balance());
        x.ids.put("concorrenciaResultado",EnsaioD28.m("a",ar,"b",br,"getA",sa,"getB",sb,"vencedorId",winner.get("id").longValue(),"perdedorId",loser.get("id").longValue(),"saldo",j.balance(),"replayVencedor",replay));
        x.save();
    }
}
