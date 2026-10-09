package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.config.JsonInteirosModule;
import br.com.rodogarcia.wms.exceptions.ApiExceptionHandler;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Contratos técnicos fixados no índice independente. Não substitui regras/efeitos dos serviços. */
class D30ContratosComponentesTest {
    private static final Path ORACULOS =
            Path.of("evidencias/d30-cedro-contratos-componentes-v13-oraculos-antes.json");
    private static final String SHA =
            "2F4E99672747766719D99D7D862B0AF5B2A4A8D25258593EBD245FA97CE0A614";
    private static final Path DEFAULTS =
            Path.of("evidencias/d30-cedro-defaults-v15-oraculos-antes.json");
    private static final String DEFAULTS_SHA =
            "56572BDC0772EE7A49A8C9E73E1EFC96D731E82C2B6D1420C20A9AF7A9DB193D";
    private static final Path ENVELOPES =
            Path.of("evidencias/d30-cedro-envelopes-v23-oraculos-antes.json");
    private static final String ENVELOPES_SHA =
            "27EB9049E35DD92F5C4968C118236ED2776E39E3E618621F814D3F65A0175508";
    private final Map<String, JsonNode> atributos = new LinkedHashMap<>();
    private final Map<String, JsonNode> defaults = new LinkedHashMap<>();
    private final Map<String, List<Canal>> canais = new HashMap<>();
    private final List<Map<String, Object>> provas = new ArrayList<>();
    private final Set<String> positivosConferidos = new java.util.HashSet<>();
    private JsonMapper mapper;
    private Validator validator;

    private record Canal(String id, String metodo, String rota, MockMvc mvc, Object[] servicos) {}

    @Test
    void componentesConservamTiposEValidamFronteirasComOraculosFixadosAntesDosRetornos()
            throws Exception {
        byte[] bytes = Files.readAllBytes(ORACULOS);
        assertThat(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                                .toUpperCase())
                .isEqualTo(SHA);
        var ref = new AtomicReference<JsonMapper>();
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(JsonInteirosModule.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            ref.set(context.getBean(JsonMapper.class));
                        });
        mapper = ref.get();
        byte[] defaultsBytes = Files.readAllBytes(DEFAULTS);
        assertThat(
                        HexFormat.of()
                                .formatHex(
                                        MessageDigest.getInstance("SHA-256").digest(defaultsBytes))
                                .toUpperCase())
                .isEqualTo(DEFAULTS_SHA);
        for (var esperado : mapper.readTree(defaultsBytes).get("defaults"))
            defaults.put(esperado.get("id").asString(), esperado);
        assertThat(defaults).hasSize(8);
        var plano = mapper.readTree(bytes);
        for (var a : plano.get("atributos"))
            atributos.put(a.get("record").asString() + "#" + a.get("campo").asString(), a);
        assertThat(atributos).hasSize(1373);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
            prepararCanais(plano.get("rotas"));
            var individuais =
                    Path.of(System.getProperty("wms.test.evidencias.dir"))
                            .resolve("d30-cedro-componentes-" + UUID.randomUUID());
            Files.createDirectories(individuais);
            for (var a : atributos.values()) {
                confrontar(a);
                Files.writeString(
                        individuais.resolve(a.get("id").asString() + ".json"),
                        mapper.writeValueAsString(provas.getLast()),
                        StandardOpenOption.CREATE_NEW);
            }
            conferirCascatas();
        }
        assertThat(provas).hasSize(1373);
        var destino = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(destino);
        Files.writeString(
                destino.resolve("d30-cedro-componentes-tipados-" + UUID.randomUUID() + ".json"),
                mapper.writeValueAsString(
                        Map.of(
                                "demanda",
                                "D30",
                                "oraculosSHA",
                                SHA,
                                "provas",
                                provas,
                                "limite",
                                "DTO/mapper/validator/controllers standalone/services mocks; sem aceite de regra de negocio, calculo, permissoes, SQL ou transformacao de origem somente por roundtrip")),
                StandardOpenOption.CREATE_NEW);
    }

    private void confrontar(JsonNode a) throws Exception {
        var tipo = classe(a.get("record").asString());
        String campo = a.get("campo").asString();
        var componente =
                Arrays.stream(tipo.getRecordComponents())
                        .filter(c -> c.getName().equals(campo))
                        .findFirst()
                        .orElseThrow();
        var original = instancia(tipo, null, null);
        var antes = componente.getAccessor().invoke(original);
        var json = mapper.writeValueAsString(original);
        var depois = componente.getAccessor().invoke(mapper.readValue(json, tipo));
        assertThat(depois).as(a.get("id").asString() + " conserva campo tipado").isEqualTo(antes);
        var resultados = new ArrayList<Map<String, Object>>();
        var restricoes = a.get("restricoes");
        for (var r : restricoes) {
            String regra = r.get("tipo").asString();
            if (Set.of("NotNull", "NotBlank").contains(regra)) {
                checar(a, tipo, campo, null, regra, true, "NULL", resultados);
                if (regra.equals("NotBlank")) {
                    checar(a, tipo, campo, "", regra, true, "VAZIO", resultados);
                    checar(a, tipo, campo, "   ", regra, true, "ESPACOS", resultados);
                }
            } else if (regra.equals("Size")) {
                int min = inteiro(r, "min", 0), max = inteiro(r, "max", Integer.MAX_VALUE);
                if (min > 0)
                    checar(
                            a,
                            tipo,
                            campo,
                            tamanho(antes, min - 1),
                            regra,
                            true,
                            "MIN_MENOS_1",
                            resultados);
                checar(
                        a,
                        tipo,
                        campo,
                        tamanho(antes, Math.max(min, 1)),
                        regra,
                        false,
                        "MIN_VALIDO",
                        resultados);
                if (max != Integer.MAX_VALUE) {
                    checar(
                            a,
                            tipo,
                            campo,
                            tamanho(antes, max),
                            regra,
                            false,
                            "MAX_DECLARADO",
                            resultados);
                    checar(
                            a,
                            tipo,
                            campo,
                            tamanho(antes, max + 1),
                            regra,
                            true,
                            "MAX_MAIS_1",
                            resultados);
                }
            } else if (Set.of(
                            "Min", "Max", "Positive", "PositiveOrZero", "DecimalMin", "DecimalMax")
                    .contains(regra)) {
                var limite = new BigDecimal(valor(r, "value", "0"));
                boolean minimo =
                        Set.of("Min", "Positive", "PositiveOrZero", "DecimalMin").contains(regra);
                boolean inclusivo =
                        !regra.equals("Positive") && !valor(r, "inclusive", "true").equals("false");
                checar(
                        a,
                        tipo,
                        campo,
                        numero(componente.getType(), limite),
                        regra,
                        !inclusivo,
                        "LIMITE_EXATO",
                        resultados);
                var vizinho = limite.add(minimo ? BigDecimal.ONE.negate() : BigDecimal.ONE);
                checar(
                        a,
                        tipo,
                        campo,
                        numero(componente.getType(), vizinho),
                        regra,
                        true,
                        "VIZINHO_FORA",
                        resultados);
                if (minimo && !inclusivo)
                    checar(
                            a,
                            tipo,
                            campo,
                            numero(componente.getType(), limite.add(BigDecimal.ONE)),
                            regra,
                            false,
                            "POSITIVO",
                            resultados);
            } else if (regra.equals("Digits")) {
                int integer = inteiro(r, "integer", 1), fraction = inteiro(r, "fraction", 0);
                BigDecimal max =
                        new BigDecimal(
                                "9".repeat(integer)
                                        + (fraction == 0 ? "" : "." + "9".repeat(fraction)));
                checar(
                        a,
                        tipo,
                        campo,
                        max,
                        regra,
                        false,
                        "PRECISAO_ESCALA_MAX_DECLARADOS",
                        resultados);
                checar(
                        a,
                        tipo,
                        campo,
                        BigDecimal.TEN.pow(integer),
                        regra,
                        true,
                        "EXCESSO_INTEIRO",
                        resultados);
                checar(
                        a,
                        tipo,
                        campo,
                        new BigDecimal("1." + "0".repeat(fraction) + "1"),
                        regra,
                        true,
                        "EXCESSO_FRACAO",
                        resultados);
                checar(a, tipo, campo, BigDecimal.ZERO, regra, false, "ZERO_DIGITS", resultados);
            } else if (regra.equals("Pattern") || regra.equals("Email")) {
                checar(a, tipo, campo, antes, regra, false, "TEXTO_VALIDO", resultados);
                checar(
                        a,
                        tipo,
                        campo,
                        regra.equals("Email") ? "email-invalido@" : "!",
                        regra,
                        true,
                        "TEXTO_FORA_DOMINIO",
                        resultados);
            } else if (!Set.of("Valid", "JsonInclude").contains(regra))
                throw new IllegalStateException("Oraculo D30 sem caso: " + regra);
        }
        if (Set.of(Long.class, long.class, Integer.class, int.class).contains(componente.getType()))
            conferirInteirosPorCanal(a, tipo, campo, componente.getType(), resultados);
        if (!componente.getType().isPrimitive()) {
            var nulo = instancia(tipo, campo, null);
            var copiado = mapper.readValue(mapper.writeValueAsString(nulo), tipo);
            var esperadoDefault = defaults.get(a.get("id").asString());
            Object esperado = esperadoDefault == null ? null : Boolean.FALSE;
            assertThat(componente.getAccessor().invoke(nulo)).isEqualTo(esperado);
            assertThat(componente.getAccessor().invoke(copiado)).isEqualTo(esperado);
            resultados.add(
                    Map.of(
                            "caso",
                            esperadoDefault == null
                                    ? "NULL_TIPADO_SEM_DEFAULT_INVENTADO"
                                    : "NULL_CONSTRUTOR_COMPACTO_FALSE",
                            "resultado",
                            esperadoDefault == null ? "NULL_PRESERVADO" : "FALSE_EXPLICITO",
                            "limite",
                            "Nao aceita NULL no negocio apenas por preservacao do mapper"));
            if (esperadoDefault != null) {
                var originalJson = mapper.valueToTree(original);
                for (String entrada : List.of("NULL", "AUSENTE", "FALSE", "TRUE")) {
                    var body = (tools.jackson.databind.node.ObjectNode) originalJson.deepCopy();
                    if (entrada.equals("AUSENTE")) body.remove(campo);
                    else if (entrada.equals("NULL")) body.putNull(campo);
                    else body.put(campo, entrada.equals("TRUE"));
                    boolean valorEsperado = entrada.equals("TRUE");
                    assertThat(componente.getAccessor().invoke(mapper.treeToValue(body, tipo)))
                            .isEqualTo(valorEsperado);
                    var retornos = new ArrayList<Map<String, Object>>();
                    for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of())) {
                        reset(canal.servicos());
                        var resposta =
                                canal.mvc()
                                        .perform(
                                                request(
                                                                HttpMethod.valueOf(canal.metodo()),
                                                                canal.rota())
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(mapper.writeValueAsString(body)))
                                        .andReturn()
                                        .getResponse();
                        assertThat(resposta.getStatus())
                                .as(canal.id() + " default " + entrada)
                                .isEqualTo(200);
                        var argumentosDto =
                                Arrays.stream(canal.servicos())
                                        .flatMap(s -> mockingDetails(s).getInvocations().stream())
                                        .flatMap(i -> Arrays.stream(i.getArguments()))
                                        .filter(tipo::isInstance)
                                        .toList();
                        assertThat(argumentosDto).hasSize(1);
                        assertThat(componente.getAccessor().invoke(argumentosDto.getFirst()))
                                .isEqualTo(valorEsperado);
                        retornos.add(
                                Map.of(
                                        "rotaID",
                                        canal.id(),
                                        "HTTP",
                                        200,
                                        "valorEfetivoDTO",
                                        valorEsperado,
                                        "chamadasComDTO",
                                        1));
                    }
                    assertThat(retornos)
                            .as(a.get("id").asString() + " canal real standalone")
                            .isNotEmpty();
                    resultados.add(
                            Map.of(
                                    "caso",
                                    "DEFAULT_ENTRADA_" + entrada,
                                    "esperadoEfetivo",
                                    valorEsperado,
                                    "retornosHTTP",
                                    retornos,
                                    "oraculoSHA",
                                    DEFAULTS_SHA,
                                    "limite",
                                    "Binding controller/mock nao prova permissoes ou ramos do servico"));
                }
            }
        }
        if (componente.getType().isEnum()) {
            for (var e : componente.getType().getEnumConstants()) {
                var dto = instancia(tipo, campo, e);
                assertThat(
                                componente
                                        .getAccessor()
                                        .invoke(
                                                mapper.readValue(
                                                        mapper.writeValueAsString(dto), tipo)))
                        .isEqualTo(e);
                if (validator.validate(dto).isEmpty())
                    for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of()))
                        conferirPositivo(canal, dto);
            }
            for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of())) {
                conferirPositivo(canal, original);
                var invalido =
                        (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(original);
                invalido.put(campo, "D30_ENUM_INEXISTENTE");
                reset(canal.servicos());
                var resposta =
                        canal.mvc()
                                .perform(
                                        request(HttpMethod.valueOf(canal.metodo()), canal.rota())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(mapper.writeValueAsString(invalido)))
                                .andReturn()
                                .getResponse();
                assertThat(resposta.getStatus())
                        .as(a.get("id").asString() + " enum desconhecido")
                        .isEqualTo(400);
                verifyNoInteractions(canal.servicos());
                var problema = mapper.readTree(resposta.getContentAsString());
                assertThat(problema.get("codigo").asString()).isEqualTo("HTTP_400");
                assertThat(problema.get("idOperacao").asString()).isNotBlank();
                assertThat(resposta.getContentAsString())
                        .doesNotContain("jdbc:", "SQLException", "org.h2");
            }
            resultados.add(
                    Map.of(
                            "caso",
                            "ENUM_TODOS_VALORES_SERIALIZADOS",
                            "constantes",
                            componente.getType().getEnumConstants().length));
        }
        provas.add(
                Map.of(
                        "id",
                        a.get("id").asString(),
                        "record",
                        tipo.getCanonicalName(),
                        "campo",
                        campo,
                        "roundtripTipado",
                        true,
                        "casos",
                        resultados,
                        "fonte",
                        mapper.convertValue(a.get("fonte"), Map.class),
                        "estado",
                        "PROVA_CONTRATO_TECNICO_DELIMITADA; transformacao/caller de negocio conserva disposicao propria"));
    }

    private void checar(
            JsonNode a,
            Class<?> tipo,
            String campo,
            Object valor,
            String regra,
            boolean recusa,
            String caso,
            List<Map<String, Object>> resultados)
            throws Exception {
        var dto = instancia(tipo, campo, valor);
        var erros = validator.validateProperty(dto, campo);
        boolean recusouRegra =
                erros.stream()
                        .anyMatch(
                                e ->
                                        e.getConstraintDescriptor()
                                                .getAnnotation()
                                                .annotationType()
                                                .getSimpleName()
                                                .equals(regra));
        assertThat(recusouRegra)
                .as(a.get("id").asString() + " " + caso + " " + regra)
                .isEqualTo(recusa);
        var transportes = new ArrayList<String>();
        var positivos = new ArrayList<String>();
        if (!recusa && validator.validate(dto).isEmpty())
            for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of())) {
                conferirPositivo(canal, dto);
                positivos.add(canal.id());
            }
        if (recusa)
            for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of())) {
                conferirPositivo(canal, instancia(tipo, null, null));
                reset(canal.servicos());
                var resultadoMvc =
                        canal.mvc()
                                .perform(
                                        request(HttpMethod.valueOf(canal.metodo()), canal.rota())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(mapper.writeValueAsString(dto)))
                                .andReturn();
                var resposta = resultadoMvc.getResponse();
                assertThat(resposta.getStatus())
                        .as(canal.id() + " " + a.get("id").asString() + " " + caso)
                        .isEqualTo(400);
                verifyNoInteractions(canal.servicos());
                var problema = mapper.readTree(resposta.getContentAsString());
                assertThat(problema.get("status").intValue()).isEqualTo(400);
                assertThat(problema.get("codigo").asString()).isIn("DADOS_INVALIDOS", "HTTP_400");
                assertThat(problema.get("idOperacao").asString()).isNotBlank();
                Files.writeString(
                        Path.of(System.getProperty("wms.test.evidencias.dir"))
                                .resolve(
                                        "d30-cedro-componente-canal-diagnostico-"
                                                + UUID.randomUUID()
                                                + ".json"),
                        mapper.writeValueAsString(
                                Map.of(
                                        "id",
                                        a.get("id").asString(),
                                        "canal",
                                        canal.id(),
                                        "record",
                                        tipo.getCanonicalName(),
                                        "campo",
                                        campo,
                                        "caso",
                                        caso,
                                        "restricao",
                                        regra,
                                        "problema",
                                        problema,
                                        "causaTecnica",
                                        resultadoMvc.getResolvedException() == null
                                                ? "AUSENTE"
                                                : resultadoMvc
                                                        .getResolvedException()
                                                        .getClass()
                                                        .getName())),
                        StandardOpenOption.CREATE_NEW);
                conferirCampoProblema(problema, campo, regra, resultadoMvc.getResolvedException());
                assertThat(resposta.getContentAsString())
                        .doesNotContain("org.h2", "jdbc:", "SQLException");
                transportes.add(canal.id());
            }
        resultados.add(
                Map.of(
                        "caso",
                        caso,
                        "restricao",
                        regra,
                        "recusaDaRestricao",
                        recusa,
                        "routes400ZeroChamadasNegocio",
                        transportes,
                        "routesPositivasFixtureValidaBinding",
                        positivos,
                        "outrasRestricoesDoCampo",
                        erros.stream()
                                .map(
                                        e ->
                                                e.getConstraintDescriptor()
                                                        .getAnnotation()
                                                        .annotationType()
                                                        .getSimpleName())
                                .distinct()
                                .sorted()
                                .toList(),
                        "limite",
                        "Limite declarado pode ser estreitado por outra restricao; aceita-lo neste predicado nao aceita combinacao invalida"));
    }

    private void conferirInteirosPorCanal(
            JsonNode a,
            Class<?> tipo,
            String campo,
            Class<?> numeroTipo,
            List<Map<String, Object>> resultados)
            throws Exception {
        var retornos = new ArrayList<Map<String, Object>>();
        BigDecimal max =
                new BigDecimal(
                        numeroTipo == Long.class || numeroTipo == long.class
                                ? "9223372036854775807"
                                : "2147483647");
        for (var canal : canais.getOrDefault(tipo.getCanonicalName(), List.of())) {
            var original = instancia(tipo, null, null);
            conferirPositivo(canal, original);
            for (String literal : List.of("1.9", "0.9", max.add(BigDecimal.ONE).toPlainString())) {
                reset(canal.servicos());
                var body = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(original);
                body.put(campo, "D30_NUMERO_LITERAL");
                String wire =
                        mapper.writeValueAsString(body).replace("\"D30_NUMERO_LITERAL\"", literal);
                var resposta =
                        canal.mvc()
                                .perform(
                                        request(HttpMethod.valueOf(canal.metodo()), canal.rota())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(wire))
                                .andReturn()
                                .getResponse();
                assertThat(resposta.getStatus())
                        .as(a.get("id").asString() + " " + canal.id() + " " + literal)
                        .isEqualTo(400);
                verifyNoInteractions(canal.servicos());
                var problema = mapper.readTree(resposta.getContentAsString());
                assertThat(problema.get("status").intValue()).isEqualTo(400);
                assertThat(problema.get("codigo").asString()).isEqualTo("HTTP_400");
                assertThat(problema.get("idOperacao").asString()).isNotBlank();
                assertThat(resposta.getContentAsString())
                        .doesNotContain("jdbc:", "SQLException", "org.h2");
                retornos.add(
                        Map.of(
                                "rotaID",
                                canal.id(),
                                "entradaNumber",
                                literal,
                                "HTTP",
                                400,
                                "chamadasNegocio",
                                0));
            }
            for (BigDecimal n : List.of(BigDecimal.ZERO, max)) {
                var dto = instancia(tipo, campo, numero(numeroTipo, n));
                if (!validator.validate(dto).isEmpty()) continue;
                var body = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(dto);
                body.put(campo, "D30_NUMERO_LITERAL");
                String literal = n.toPlainString() + ".0";
                String wire =
                        mapper.writeValueAsString(body).replace("\"D30_NUMERO_LITERAL\"", literal);
                reset(canal.servicos());
                var resposta =
                        canal.mvc()
                                .perform(
                                        request(HttpMethod.valueOf(canal.metodo()), canal.rota())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(wire))
                                .andReturn()
                                .getResponse();
                assertThat(resposta.getStatus())
                        .as(a.get("id").asString() + " exato " + literal)
                        .isBetween(200, 299);
                var argumentos =
                        Arrays.stream(canal.servicos())
                                .flatMap(s -> mockingDetails(s).getInvocations().stream())
                                .flatMap(i -> Arrays.stream(i.getArguments()))
                                .filter(tipo::isInstance)
                                .toList();
                assertThat(argumentos)
                        .isNotEmpty()
                        .allSatisfy(arg -> assertThat(arg).isEqualTo(dto));
                retornos.add(
                        Map.of(
                                "rotaID",
                                canal.id(),
                                "entradaNumber",
                                literal,
                                "HTTP",
                                resposta.getStatus(),
                                "inteiroExatoNoDTO",
                                n.toPlainString()));
            }
        }
        resultados.add(
                Map.of(
                        "caso",
                        "INTEIROS_CALLERS_DA_ULTIMA_FONTE",
                        "retornos",
                        retornos,
                        "oraculo",
                        "LongMAX9223372036854775807/IntegerMAX2147483647; fracao1.9/0.9 e MAX+1 recusados; MAX.0/0.0 exatos quando fixture valida",
                        "limite",
                        "Canal tecnico do campo/raiz; nao aceita caller de negocio pela largura Java"));
    }

    private Object instancia(Class<?> tipo, String campo, Object substituto) throws Exception {
        var componentes = tipo.getRecordComponents();
        Object[] valores = new Object[componentes.length];
        for (int i = 0; i < componentes.length; i++) {
            var c = componentes[i];
            valores[i] =
                    c.getName().equals(campo)
                            ? substituto
                            : semente(
                                    c.getGenericType(),
                                    atributos.get(tipo.getCanonicalName() + "#" + c.getName()));
        }
        return tipo.getDeclaredConstructor(
                        Arrays.stream(componentes)
                                .map(RecordComponent::getType)
                                .toArray(Class<?>[]::new))
                .newInstance(valores);
    }

    private Object semente(Type tipo, JsonNode atributo) throws Exception {
        if (tipo instanceof ParameterizedType p) {
            var raw = (Class<?>) p.getRawType();
            if (List.class.isAssignableFrom(raw)) {
                int min = 1;
                if (atributo != null)
                    for (var r : atributo.get("restricoes"))
                        if (r.get("tipo").asString().equals("Size"))
                            min = Math.max(min, inteiro(r, "min", 0));
                var lista = new ArrayList<Object>();
                for (int i = 0; i < min; i++)
                    lista.add(semente(p.getActualTypeArguments()[0], null));
                return lista;
            }
            if (Map.class.isAssignableFrom(raw)) return Map.of("D30", "FICTICIO");
            throw new IllegalStateException("Tipo generico D30 sem fixture: " + tipo);
        }
        if (!(tipo instanceof Class<?> c)) return "D30-FICTICIO";
        if (c.isRecord()) return instancia(c, null, null);
        if (c.isEnum()) return c.getEnumConstants()[0];
        if (c == Instant.class) return Instant.parse("2026-09-01T12:00:00.123456Z");
        if (c == LocalDate.class) return LocalDate.of(2026, 9, 1);
        if (c == UUID.class) return UUID.fromString("00000000-0000-0000-0000-000000000001");
        if (c == boolean.class || c == Boolean.class) return true;
        if (c == String.class) {
            String texto = "D30-PROVA";
            if (atributo != null)
                for (var r : atributo.get("restricoes")) {
                    String regra = r.get("tipo").asString();
                    if (regra.equals("Email")) texto = "d30@example.invalid";
                    if (regra.equals("Pattern")) {
                        String padrao = valor(r, "regexp", "");
                        if (padrao.equals("[a-fA-F0-9]{64}")) texto = "a".repeat(64);
                        else if (padrao.startsWith("[0-9]{"))
                            texto =
                                    "1"
                                            .repeat(
                                                    Integer.parseInt(
                                                            padrao.substring(6, padrao.indexOf('}'))
                                                                    .split(",")[0]));
                        else if (padrao.equals("[0-9A-Z]{14}")) texto = "1".repeat(14);
                        else if (padrao.equals("[A-Z]{2}") || padrao.equals("[A-Za-z]{2}"))
                            texto = "SP";
                        else if (padrao.equals("[A-Za-z]{1,8}")) texto = "UN";
                        else if (padrao.startsWith("[A-Za-z0-9]")) texto = "D30";
                        else texto = padrao.split("\\|")[0];
                    }
                }
            if (atributo != null)
                for (var r : atributo.get("restricoes"))
                    if (r.get("tipo").asString().equals("Size")) {
                        int min = inteiro(r, "min", 0), max = inteiro(r, "max", Integer.MAX_VALUE);
                        if (texto.length() < min) texto = "A".repeat(min);
                        if (texto.length() > max) texto = texto.substring(0, max);
                    }
            return texto;
        }
        if (c == long.class
                || c == Long.class
                || c == int.class
                || c == Integer.class
                || c == BigDecimal.class) {
            if (c == BigDecimal.class) {
                int casas = 6;
                if (atributo != null)
                    for (var r : atributo.get("restricoes"))
                        if (r.get("tipo").asString().equals("Digits"))
                            casas = inteiro(r, "fraction", 0);
                return casas == 0 ? BigDecimal.ONE : new BigDecimal("1." + "2".repeat(casas));
            }
            return numero(c, BigDecimal.ONE);
        }
        if (c == Object.class) return "D30-FICTICIO";
        throw new IllegalStateException("Tipo D30 sem fixture: " + tipo);
    }

    private static Object numero(Class<?> tipo, BigDecimal n) {
        if (tipo == int.class || tipo == Integer.class) return n.intValueExact();
        if (tipo == long.class || tipo == Long.class) return n.longValueExact();
        return n;
    }

    private static Object tamanho(Object exemplo, int tamanho) {
        if (exemplo instanceof String) return "A".repeat(tamanho);
        if (exemplo instanceof List<?> l) {
            var resultado = new ArrayList<Object>(tamanho);
            for (int i = 0; i < tamanho; i++) resultado.add(l.getFirst());
            return resultado;
        }
        if (exemplo instanceof Map<?, ?>) {
            var resultado = new LinkedHashMap<String, Object>();
            for (int i = 0; i < tamanho; i++) resultado.put("D30-" + i, "FICTICIO");
            return resultado;
        }
        throw new IllegalStateException("Tamanho D30 sem fixture: " + exemplo);
    }

    private static String valor(JsonNode r, String chave, String padrao) {
        var v = r.get("valores").get(chave);
        String texto = v == null ? padrao : v.asString().replaceAll("^\"|\"$", "");
        return texto.matches("-?[0-9]+L") ? texto.substring(0, texto.length() - 1) : texto;
    }

    private static int inteiro(JsonNode r, String chave, int padrao) {
        return Integer.parseInt(valor(r, chave, String.valueOf(padrao)));
    }

    private static Class<?> classe(String nome) throws ClassNotFoundException {
        String prefixo = "br.com.rodogarcia.wms.dto.";
        assertThat(nome).startsWith(prefixo);
        return Class.forName(prefixo + nome.substring(prefixo.length()).replace('.', '$'));
    }

    private void conferirPositivo(Canal canal, Object dto) throws Exception {
        var erros = validator.validate(dto);
        assertThat(erros)
                .as(canal.id() + " fixture positiva integral BeanValidation: " + erros)
                .isEmpty();
        String json = mapper.writeValueAsString(dto);
        String hash =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                json.getBytes(
                                                        java.nio.charset.StandardCharsets.UTF_8)));
        if (!positivosConferidos.add(canal.id() + "#" + hash)) return;
        reset(canal.servicos());
        var resposta =
                canal.mvc()
                        .perform(
                                request(HttpMethod.valueOf(canal.metodo()), canal.rota())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                        .andReturn()
                        .getResponse();
        assertThat(resposta.getStatus())
                .as(canal.id() + " controle positivo causal, mocks sem banco")
                .isBetween(200, 299);
        assertThat(
                        Arrays.stream(canal.servicos())
                                .flatMap(s -> mockingDetails(s).getInvocations().stream())
                                .flatMap(i -> Arrays.stream(i.getArguments()))
                                .filter(dto.getClass()::isInstance)
                                .count())
                .as(canal.id() + " DTO valido chegou ao servico mock")
                .isGreaterThanOrEqualTo(1);
        var argumentos =
                Arrays.stream(canal.servicos())
                        .flatMap(s -> mockingDetails(s).getInvocations().stream())
                        .flatMap(i -> Arrays.stream(i.getArguments()))
                        .filter(dto.getClass()::isInstance)
                        .toList();
        assertThat(argumentos).allSatisfy(a -> assertThat(a).isEqualTo(dto));
    }

    private void conferirCampoProblema(
            JsonNode problema, String path, String regra, Exception causa) {
        var campos = new ArrayList<String>();
        if (problema.get("codigo").asString().equals("DADOS_INVALIDOS")) {
            for (var erro : problema.get("campos"))
                campos.add(erro.get("campo").asString() + "#" + erro.get("codigo").asString());
        } else {
            assertThat(problema.get("codigo").asString()).isEqualTo("HTTP_400");
            assertThat(causa)
                    .isInstanceOf(
                            org.springframework.web.method.annotation
                                    .HandlerMethodValidationException.class);
            var validacao =
                    (org.springframework.web.method.annotation.HandlerMethodValidationException)
                            causa;
            assertThat(validacao.isForReturnValue()).isFalse();
            for (var parametro : validacao.getParameterValidationResults())
                for (var erro : parametro.getResolvableErrors()) {
                    assertThat(erro).isInstanceOf(org.springframework.validation.FieldError.class);
                    var campo = (org.springframework.validation.FieldError) erro;
                    campos.add(campo.getField() + "#" + campo.getCode());
                }
        }
        assertThat(campos).contains(path + "#" + regra);
    }

    private record Envelope(Object positivo, Object negativo, String path, List<Canal> canais) {}

    private List<Envelope> envelopes(
            Object positivo, Object negativo, String path, Set<Class<?>> visitados)
            throws Exception {
        Class<?> tipo = positivo.getClass();
        if (visitados.contains(tipo)) return List.of();
        var proximos = new java.util.HashSet<>(visitados);
        proximos.add(tipo);
        var encontrados = new ArrayList<Envelope>();
        var diretos = canais.getOrDefault(tipo.getCanonicalName(), List.of());
        if (!diretos.isEmpty()) encontrados.add(new Envelope(positivo, negativo, path, diretos));
        for (var a : atributos.values()) {
            boolean cascata = false;
            for (var r : a.get("restricoes"))
                if (r.get("tipo").asString().equals("Valid")) cascata = true;
            if (!cascata) continue;
            var pai = classe(a.get("record").asString());
            String campo = a.get("campo").asString();
            var componente =
                    Arrays.stream(pai.getRecordComponents())
                            .filter(c -> c.getName().equals(campo))
                            .findFirst()
                            .orElseThrow();
            if (componente.getType() == tipo) {
                encontrados.addAll(
                        envelopes(
                                instancia(pai, campo, positivo),
                                instancia(pai, campo, negativo),
                                campo + "." + path,
                                proximos));
            }
        }
        return encontrados;
    }

    private void conferirCascatas() throws Exception {
        byte[] bytes = Files.readAllBytes(ENVELOPES);
        assertThat(
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                                .toUpperCase())
                .isEqualTo(ENVELOPES_SHA);
        var destino = Path.of(System.getProperty("wms.test.evidencias.dir"));
        for (var esperado : mapper.readTree(bytes).get("cascatas")) {
            String id = esperado.get("id").asString();
            var pai = classe(esperado.get("record").asString());
            var filho = classe(esperado.get("filhoRecord").asString());
            String campo = esperado.get("campo").asString(),
                    leaf = esperado.get("filhoCampo").asString();
            var componente =
                    Arrays.stream(filho.getRecordComponents())
                            .filter(c -> c.getName().equals(leaf))
                            .findFirst()
                            .orElseThrow();
            Object valor =
                    esperado.get("valorInvalido").isNull()
                            ? null
                            : componente.getType() == String.class
                                    ? esperado.get("valorInvalido").asString()
                                    : numero(
                                            componente.getType(),
                                            new BigDecimal(
                                                    esperado.get("valorInvalido").asString()));
            var positivo = instancia(pai, null, null);
            var negativo = instancia(pai, campo, instancia(filho, leaf, valor));
            String path = esperado.get("path").asString(),
                    regra = esperado.get("restricao").asString();
            assertThat(validator.validate(positivo)).as(id + " envelope positivo").isEmpty();
            assertThat(
                            validator.validate(negativo).stream()
                                    .map(
                                            v ->
                                                    v.getPropertyPath()
                                                            + "#"
                                                            + v.getConstraintDescriptor()
                                                                    .getAnnotation()
                                                                    .annotationType()
                                                                    .getSimpleName())
                                    .toList())
                    .as(id + " cascata causal no pai")
                    .contains(path + "#" + regra);
            var retornos = new ArrayList<Map<String, Object>>();
            for (var envelope : envelopes(positivo, negativo, path, Set.of()))
                for (var canal : envelope.canais()) {
                    conferirPositivo(canal, envelope.positivo());
                    reset(canal.servicos());
                    var resultadoMvc =
                            canal.mvc()
                                    .perform(
                                            request(
                                                            HttpMethod.valueOf(canal.metodo()),
                                                            canal.rota())
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(
                                                            mapper.writeValueAsString(
                                                                    envelope.negativo())))
                                    .andReturn();
                    var resposta = resultadoMvc.getResponse();
                    assertThat(resposta.getStatus())
                            .as(id + " " + canal.id() + " filho invalido")
                            .isEqualTo(400);
                    verifyNoInteractions(canal.servicos());
                    var problema = mapper.readTree(resposta.getContentAsString());
                    conferirCampoProblema(
                            problema, envelope.path(), regra, resultadoMvc.getResolvedException());
                    assertThat(problema.get("idOperacao").asString()).isNotBlank();
                    assertThat(resposta.getContentAsString())
                            .doesNotContain("jdbc:", "SQLException", "org.h2");
                    retornos.add(
                            Map.of(
                                    "rotaID",
                                    canal.id(),
                                    "path",
                                    envelope.path(),
                                    "HTTP",
                                    400,
                                    "positivoCausalConferido",
                                    true,
                                    "chamadasNegocio",
                                    0));
                }
            Files.writeString(
                    destino.resolve("d30-cedro-cascata-" + id + "-" + UUID.randomUUID() + ".json"),
                    mapper.writeValueAsString(
                            Map.of(
                                    "id",
                                    id,
                                    "oraculoSHA",
                                    ENVELOPES_SHA,
                                    "path",
                                    path,
                                    "restricao",
                                    regra,
                                    "validatorCascata",
                                    true,
                                    "retornos",
                                    retornos,
                                    "limite",
                                    retornos.isEmpty()
                                            ? "Tipo interno sem RequestBody raiz; prova Validator, caller/parser conserva disposicao propria"
                                            : "Binding/cascata no pai real e servicos mocks; nao aceita transformacao/regra de negocio")),
                    StandardOpenOption.CREATE_NEW);
        }
    }

    private void prepararCanais(JsonNode rotas) throws Exception {
        var construidos = new HashMap<Class<?>, Canal>();
        for (var rota : rotas) {
            String[] handler = rota.get("handler").asString().split("\\.");
            var controlador = Class.forName("br.com.rodogarcia.wms.controllers." + handler[0]);
            var metodo =
                    Arrays.stream(controlador.getDeclaredMethods())
                            .filter(m -> m.getName().equals(handler[1]))
                            .findFirst()
                            .orElseThrow();
            var body =
                    Arrays.stream(metodo.getParameters())
                            .filter(
                                    p ->
                                            p.isAnnotationPresent(RequestBody.class)
                                                    && p.getType().isRecord())
                            .findFirst();
            if (body.isEmpty()) continue;
            var canal = construidos.get(controlador);
            if (canal == null) {
                var construtor = controlador.getConstructors()[0];
                var mocks =
                        Arrays.stream(construtor.getParameterTypes())
                                .map(
                                        t ->
                                                mock(
                                                        t,
                                                        invocation -> {
                                                            var retorno =
                                                                    invocation
                                                                            .getMethod()
                                                                            .getReturnType();
                                                            if (retorno.isRecord())
                                                                return instancia(
                                                                        retorno, null, null);
                                                            return org.mockito.Answers
                                                                    .RETURNS_DEFAULTS
                                                                    .answer(invocation);
                                                        }))
                                .toArray();
                var controller = construtor.newInstance(mocks);
                var mvc =
                        MockMvcBuilders.standaloneSetup(controller)
                                .setValidator(new SpringValidatorAdapter(validator))
                                .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                                .setControllerAdvice(
                                        new ApiExceptionHandler(new ProblemasApi(mapper)))
                                .addFilters(new IdentificacaoOperacaoFilter())
                                .build();
                canal = new Canal("", "", "", mvc, mocks);
                construidos.put(controlador, canal);
            }
            String path = rota.get("rota").asString();
            for (var p : metodo.getParameters()) {
                var pathVariable =
                        p.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                if (pathVariable == null) continue;
                String nome = pathVariable.value().isEmpty() ? p.getName() : pathVariable.value();
                String literal =
                        p.getType() == UUID.class
                                ? "00000000-0000-0000-0000-000000000001"
                                : p.getType().isEnum()
                                        ? ((Enum<?>) p.getType().getEnumConstants()[0]).name()
                                        : "1";
                path = path.replace("{" + nome + "}", literal);
            }
            assertThat(path).doesNotContain("{");
            canais.computeIfAbsent(body.get().getType().getCanonicalName(), k -> new ArrayList<>())
                    .add(
                            new Canal(
                                    rota.get("id").asString(),
                                    rota.get("metodoHTTP").asString(),
                                    path,
                                    canal.mvc(),
                                    canal.servicos()));
        }
    }
}
