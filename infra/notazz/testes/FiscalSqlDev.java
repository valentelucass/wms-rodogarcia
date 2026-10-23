import br.com.rodogarcia.wms.WmsApplication;
import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.services.NotazzGateway;
import java.io.DataInputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Runner externo: SQL Server real e transporte fiscal controlado. Nunca integra o JAR. */
public class FiscalSqlDev {
    static final String EMITENTE = "00000000090701";
    static Path area;
    static final JsonMapper JSON = new JsonMapper();

    static String frame(DataInputStream in, int max, boolean empty) throws Exception {
        int n = in.readInt();
        if (n < (empty ? 0 : 1) || n > max) throw new IllegalStateException("FRAME_INVALIDO");
        byte[] b = in.readNBytes(n);
        try {
            if (b.length != n) throw new IllegalStateException("FRAME_INCOMPLETO");
            return new String(b, StandardCharsets.UTF_8);
        } finally { Arrays.fill(b, (byte) 0); }
    }

    public static void main(String[] args) throws Exception {
        // Todos os argumentos sao publicos. Material privado somente pelo stdin herdado.
        area = Path.of(args[0]).toRealPath();
        var gate = JSON.readTree(Files.readString(area.resolve("gate.json")));
        if (!gate.path("allowed").asBoolean() || !gate.path("database").asText().equals("WMS_DEV")
                || !gate.path("login").asText().equals("WMSDEV")
                || gate.path("externalCalls").asInt() != 0) throw new IllegalStateException("GATE_INVALIDO");
        var input = new DataInputStream(System.in);
        if (input.readInt() != 0x574d5301) throw new IllegalStateException("FRAME_INVALIDO");
        System.setProperty("wms.database.password", frame(input, 4096, false));
        System.setProperty("wms.auth.private-key", frame(input, 16384, false));
        if (!frame(input, 0, true).isEmpty() || input.read() != -1) throw new IllegalStateException("FRAME_EXTRA");
        System.setProperty("wms.auth.bootstrap-hash", "");
        System.setProperty("wms.database.tls.trust-store-password", "projetos-public-cert");
        var app = new SpringApplication(WmsApplication.class, Controlado.class);
        var arguments = new ArrayList<String>(List.of(
                "--spring.profiles.active=sqlserver-dev",
                "--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties",
                "--spring.config.additional-location=" + Path.of(args[1]).toUri(),
                "--server.address=127.0.0.1", "--server.port=25582",
                "--wms.database.host=127.0.0.1", "--wms.database.port=1433",
                "--wms.database.name=WMS_DEV", "--wms.database.user=WMSDEV",
                "--wms.database.confirmed-target=127.0.0.1:1433/WMS_DEV",
                "--wms.database.confirmed-server=" + gate.path("server").asText(),
                "--wms.database.tls.certificate-host=" + gate.path("certificateHost").asText(),
                "--wms.database.tls.trust-store=" + gate.path("truststore").asText(),
                "--wms.identity.issuer=https://wms.localhost",
                "--wms.identity.jwk-set-uri=https://wms.localhost/api/auth/jwks",
                "--wms.identity.audience=wms-dev", "--wms.auth.enabled=true",
                "--wms.auth.origin=http://localhost:25581", "--wms.auth.proxy-origin=",
                "--wms.auth.secure-cookie=false", "--wms.sql01.observation.enabled=false",
                "--wms.notazz.enabled=false", "--wms.notazz.production-enabled=false",
                "--logging.level.root=OFF", "--spring.main.banner-mode=off"));
        try (var context = app.run(arguments.toArray(String[]::new))) {
            try (var c = context.getBean(DataSource.class).getConnection();
                    var s = c.createStatement();
                    var r = s.executeQuery("SELECT DB_NAME(), ORIGINAL_LOGIN(), USER_NAME()")) {
                if (!r.next() || !"WMS_DEV".equals(r.getString(1))
                        || !"WMSDEV".equals(r.getString(2)) || !"WMSDEV".equals(r.getString(3)))
                    throw new IllegalStateException("IDENTIDADE_INVALIDA");
            }
            Files.writeString(area.resolve("server-ready.json"), "{\"sql\":\"WMS_DEV/WMSDEV\",\"port\":25582,\"externalCalls\":0}", StandardOpenOption.CREATE_NEW);
            var until = Instant.now().plusSeconds(900);
            while (!Files.exists(area.resolve("fixture-plan.json"))) {
                if (Instant.now().isAfter(until)) throw new IllegalStateException("FIXTURE_AUSENTE");
                Thread.sleep(200);
            }
            var plan = JSON.readTree(Files.readString(area.resolve("fixture-plan.json")));
            long id = plan.path("armazemId").asLong();
            // Verificacao independente antes de habilitar somente o emitente ficticio deste runner.
            try (var c = context.getBean(DataSource.class).getConnection();
                    var s = c.prepareStatement("SELECT codigo,documento_fiscal FROM wms.armazem WHERE id=?")) {
                s.setLong(1, id);
                try (var r = s.executeQuery()) {
                    if (!r.next() || !r.getString(1).startsWith("NFH23-")
                            || !EMITENTE.equals(r.getString(2))) throw new IllegalStateException("FIXTURE_INVALIDA");
                }
            }
            var properties = context.getBean(NotazzProperties.class);
            var e = new NotazzProperties.Emitente();
            e.setCnpj(EMITENTE); e.setAmbiente(2); e.setApiKey("FICTICIA_SEM_ACESSO_EXTERNO");
            e.setApiKeySha256Conferida(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(e.getApiKey().getBytes(StandardCharsets.UTF_8))));
            e.setConfiguracaoConferida(true); e.setFinalidade("4");
            e.setNaturezaOperacao("RETORNO FICTICIO DE DEPOSITO - TESTE INTERNO");
            e.setFinalidadeEntrada("4"); e.setNaturezaEntrada("ENTRADA FICTICIA DE REGULARIZACAO - TESTE INTERNO");
            properties.setCnpjsHomologacaoPermitidos(Set.of(EMITENTE));
            properties.setEmitentes(new java.util.concurrent.ConcurrentHashMap<>(Map.of(id,e)));
            properties.setEnabled(true);
            Files.writeString(area.resolve("provider-ready.json"), "{\"transport\":\"CONTROLADO_SEM_REDE\",\"externalCalls\":0}", StandardOpenOption.CREATE_NEW);
            while (!Files.exists(area.resolve("stop"))) {
                if (Instant.now().isAfter(until)) throw new IllegalStateException("PRAZO_EXCEDIDO");
                Thread.sleep(200);
            }
        } finally {
            for (var k : List.of("wms.database.password","wms.auth.private-key","wms.auth.bootstrap-hash","wms.database.tls.trust-store-password")) System.clearProperty(k);
        }
    }

    @Configuration(proxyBeanMethods=false)
    public static class Controlado {
        @Bean @Primary
        NotazzGateway emissorControlado(FiscalNfeRepository repository, NotazzProperties properties) {
            return new Emissor(repository, properties);
        }
    }

    static final class Emissor extends NotazzGateway {
        final FiscalNfeRepository repository;
        final Map<String,Integer> criacoes = new LinkedHashMap<>();
        final Set<String> canceladas = new HashSet<>();
        Emissor(FiscalNfeRepository repository, NotazzProperties properties) {
            super(HttpClient.newHttpClient(),JSON,properties); this.repository=repository;
            var ledger=area.resolve("provider-ledger.json");
            if(Files.exists(ledger)) {
                try {
                    var old=JSON.readTree(Files.readString(ledger));
                    old.path("criacoes").properties().forEach(x->criacoes.put(x.getKey(),x.getValue().asInt()));
                    for(var id:old.path("canceladas"))canceladas.add(id.asText());
                } catch(java.io.IOException ex) {throw new IllegalStateException("LEDGER_INDISPONIVEL");}
            }
        }
        @Override public synchronized JsonNode chamar(NotazzProperties.Emitente e, String method, Map<String,Object> body) {
            if (TransactionSynchronizationManager.isActualTransactionActive() || !EMITENTE.equals(e.getCnpj())
                    || e.getAmbiente()!=2 || !"FICTICIA_SEM_ACESSO_EXTERNO".equals(e.getApiKey()))
                throw new IllegalStateException("TRANSPORTE_FICTICIO_RECUSADO");
            String id = Objects.toString(body.get("EXTERNAL_ID"), "");
            if (id.isEmpty()) id=Objects.toString(body.get("DOCUMENT_ID"), "").replace("ficticio-", "");
            var s=repository.consultar(id,false);
            if (s==null) throw new IllegalStateException("ENVIO_ANTES_DA_PERSISTENCIA");
            var doc=JSON.readValue(s.documentoJson(),FiscalNfeDto.DocumentoPlanejado.class);
            if (!EMITENTE.equals(doc.emitenteCnpj()) || !doc.observacao().contains("NFH23-"))
                throw new IllegalStateException("DOCUMENTO_FORA_DA_FIXTURE");
            try {
                if (method.equals("create_nfe_55")) {
                    criacoes.merge(id,1,Integer::sum);
                    Files.writeString(area.resolve("provider-ledger.json"),JSON.writeValueAsString(Map.of("criacoes",criacoes,"canceladas",canceladas,"externalCalls",0)));
                    // Aceitou o documento, mas a resposta foi perdida. Criacao nao pode ser repetida.
                    throw new ResultadoDesconhecido();
                }
                if (!Set.of("consult_nfe_55","cancel_nfe_55").contains(method)) throw new IllegalStateException("METODO_INVALIDO");
                if (method.equals("cancel_nfe_55")) canceladas.add(id);
                var release=area.resolve("liberar-"+id);
                if (!Files.exists(release)) return JSON.valueToTree(Map.of("codigoProcessamento","000","statusProcessamento","sucesso","id","ficticio-"+id,"external_id",id,"statusNota","Pendente"));
                long number=1_000_000L+Math.floorMod(id.hashCode(),800_000_000);
                String key=chave("352610"+EMITENTE+"55"+"999"+String.format("%09d",number)+"1"+"00000001");
                var out=new LinkedHashMap<String,Object>();
                out.put("codigoProcessamento","000"); out.put("statusProcessamento","sucesso");
                out.put("id","ficticio-"+id); out.put("external_id",id);
                out.put("statusNota",canceladas.contains(id)?"Cancelada":"Autorizada");
                out.put("numero",Long.toString(number)); out.put("chave",key);
                out.put("xmlBase64",Base64.getEncoder().encodeToString(xml(doc,key,number).getBytes(StandardCharsets.UTF_8)));
                if (canceladas.contains(id)) out.put("xmlCancelamentoBase64",Base64.getEncoder().encodeToString(cancelamento(key).getBytes(StandardCharsets.UTF_8)));
                Files.writeString(area.resolve("provider-ledger.json"),JSON.writeValueAsString(Map.of("criacoes",criacoes,"canceladas",canceladas,"externalCalls",0)));
                return JSON.valueToTree(out);
            } catch (java.io.IOException ex) { throw new IllegalStateException("EVIDENCIA_INDISPONIVEL"); }
        }
    }
    static String chave(String base) {
        int sum=0; for(int i=0;i<43;i++)sum+=(base.charAt(42-i)-'0')*(2+i%8);
        int d=11-sum%11; return base+(d>=10?0:d);
    }
    static String esc(String x) {return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    static String xml(FiscalNfeDto.DocumentoPlanejado d,String key,long number) {
        var t=OffsetDateTime.now(ZoneOffset.ofHours(-3));
        if(d.observacao().contains("-AGED"))t=t.minusHours(25);
        var b=new StringBuilder("<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\"><NFe><infNFe versao=\"4.00\" Id=\"NFe"+key+"\"><ide><mod>55</mod><serie>999</serie><nNF>"+number+"</nNF><dhEmi>"+t+"</dhEmi><tpAmb>2</tpAmb><tpNF>"+(d.natureza().equals("REGULARIZACAO_ENTRADA")?0:1)+"</tpNF><finNFe>"+d.finalidade()+"</finNFe><natOp>"+esc(d.naturezaOperacao())+"</natOp>");
        for(var k:d.chavesReferenciadas())b.append("<NFref><refNFe>"+k+"</refNFe></NFref>");
        b.append("</ide><emit><CNPJ>"+EMITENTE+"</CNPJ></emit><dest><CNPJ>"+d.destinatarioDocumento()+"</CNPJ></dest>");
        int n=0; for(var i:d.itens()) b.append("<det nItem=\""+(++n)+"\"><prod><cProd>"+esc(i.sku())+"</cProd><uCom>"+esc(i.unidade())+"</uCom><NCM>"+i.ncm()+"</NCM><CFOP>"+i.cfop()+"</CFOP><qCom>"+i.quantidade()+"</qCom><vProd>"+i.valorTotal()+"</vProd></prod></det>");
        return b+"</infNFe></NFe><protNFe versao=\"4.00\"><infProt><tpAmb>2</tpAmb><chNFe>"+key+"</chNFe><nProt>123456789012345</nProt><cStat>100</cStat><dhRecbto>"+t+"</dhRecbto></infProt></protNFe></nfeProc>";
    }
    static String cancelamento(String key) {
        return "<procEventoNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><evento><infEvento><CNPJ>"+EMITENTE+"</CNPJ><chNFe>"+key+"</chNFe><tpEvento>110111</tpEvento></infEvento></evento><retEvento><infEvento><tpAmb>2</tpAmb><chNFe>"+key+"</chNFe><tpEvento>110111</tpEvento><cStat>135</cStat></infEvento></retEvento></procEventoNFe>";
    }
}
