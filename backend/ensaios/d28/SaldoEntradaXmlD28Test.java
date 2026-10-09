import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Fixture isolada de recusa R16; nenhuma conexao, token, issuer ou HTTP. */
public class SaldoEntradaXmlD28Test {
    public static void main(String[] args) throws Exception {
        var x=new EnsaioD28("");
        var cases=new ArrayList<Map<String,Object>>();
        for (String base:List.of("", "D28FFFFFFFF")) {
            x.ids.clear();if(!base.isEmpty())x.ids.put("retomadaDe",base);
            boolean refused=false;
            try{SaldoEntradaXmlD28.run(x);}catch(IllegalStateException e){refused=e.getMessage().equals("D28_R16_MODO_FRESCO_BLOQUEADO_SEM_HTTP_SQL");}
            cases.add(EnsaioD28.m("caso",base.isEmpty()?"modo fresco recusado antes efeitos":"base diversa recusada antes efeitos","passou",refused&&x.cases.isEmpty()&&x.app==null&&x.fixture==null));
        }
        SaldoEntradaXmlD28.validateBase(EnsaioD28.m("retomadaDe","D289A0CF1ED"));
        cases.add(EnsaioD28.m("caso","base pendente explicita reconhecida so pela guarda; run nao invocado","passou",true));
        var out=EnsaioD28.m("incremento","D28","utc",Instant.now().toString(),"checks",cases.size(),"falhas",cases.stream().filter(c->!Boolean.TRUE.equals(c.get("passou"))).count(),"HTTPExecutado",false,"SQLExecutado",false,"credencialCarregada",false,"JARIniciado",false,"casos",cases);
        Files.writeString(Path.of("evidencias/d28-saldo-xml-guarda-offline.json"),x.json.writeValueAsString(out));
        System.out.println("D28 R16 guarda offline "+cases.size()+"/"+out.get("falhas"));
        if(!out.get("falhas").equals(0L))System.exit(1);
    }
}
