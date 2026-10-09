package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.dto.AuditoriaResponse;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AuditoriaService {
    private final AuditoriaCadastroRepository repository;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final JsonMapper leitorHistorico =
            JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final String VINCULO = "VINCULO_COBRANCA";
    private static final String LEGADO = "LEGADO_NAO_ATRIBUIDO";
    private static final Set<String> CARRIERS = Set.of("CONTRATO_COBRANCA", "TABELA_COBRANCA");

    public AuditoriaService(
            AuditoriaCadastroRepository repository,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock) {
        this.repository = repository;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(
            String tipo, Long id, String acao, String motivo, Object antes, Object depois) {
        String idOperacao = MDC.get(IdentificacaoOperacaoFilter.ATRIBUTO);
        if (idOperacao == null) {
            idOperacao = UUID.randomUUID().toString();
        }
        boolean vinculo = VINCULO.equals(tipo);
        if (vinculo
                && (id == null
                        || id <= 0
                        || depois == null
                        || !Set.of("VINCULO_TABELA", "ENCERRAMENTO_VIGENCIA").contains(acao))) {
            throw CadastroSupport.invalido("Evento de vínculo incompatível com a auditoria.");
        }
        var evento =
                new AuditoriaCadastro(
                        vinculo ? "CONTRATO_COBRANCA" : tipo,
                        id,
                        acao,
                        acesso.usuario(),
                        Instant.now(clock),
                        idOperacao,
                        motivo,
                        antes == null
                                ? null
                                : mapper.writeValueAsString(
                                        vinculo ? new EnvelopeVinculo(VINCULO, id, antes) : antes),
                        mapper.writeValueAsString(
                                vinculo ? new EnvelopeVinculo(VINCULO, id, depois) : depois));
        if (vinculo && !VINCULO.equals(classificarFinanceiro(evento))) {
            throw CadastroSupport.invalido("Snapshot de vínculo incompatível com a auditoria.");
        }
        repository.saveAndFlush(evento);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> listar(String tipo, Long id, int pagina, int tamanho) {
        acesso.exigirGestor();
        if (tipo == null
                || !Set.of(
                                "CLIENTE",
                                "ARMAZEM",
                                "PRODUTO",
                                "EMBALAGEM",
                                "ENDERECO",
                                "PEDIDO_ENTRADA",
                                "PEDIDO_SAIDA",
                                "CONJUNTO_POSICOES",
                                "SERVICO_COBRANCA",
                                "TABELA_COBRANCA",
                                "CONTRATO_COBRANCA",
                                VINCULO,
                                "VINCULO_COBRANCA_LEGADO",
                                "FATO_SERVICO",
                                "AVARIA_FINANCEIRA",
                                "CALCULO_COBRANCA",
                                "CONTAGEM_ESTOQUE",
                                "CARGA_INICIAL",
                                "CONTINGENCIA",
                                "AVISO_VALIDADE",
                                "FECHAMENTO_COBRANCA")
                        .contains(tipo)) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST, "TIPO_INVALIDO", "Tipo de cadastro inválido.");
        }
        if (CARRIERS.contains(tipo)
                || VINCULO.equals(tipo)
                || "VINCULO_COBRANCA_LEGADO".equals(tipo)) {
            return listarFinanceiro(tipo, id, pagina, tamanho);
        }
        return PaginaResponse.de(
                repository.findByTipoAndRegistroId(
                        tipo, id, CadastroSupport.pagina(pagina, tamanho)),
                AuditoriaResponse::de);
    }

    private record EnvelopeVinculo(String tipoRegistro, Long registroId, Object resultado) {}

    private PaginaResponse<AuditoriaResponse> listarFinanceiro(
            String tipo, Long id, int pagina, int tamanho) {
        if (id == null || id <= 0)
            throw CadastroSupport.invalido("Registro de auditoria inválido.");
        var pageable = CadastroSupport.pagina(pagina, tamanho);
        String destino = "VINCULO_COBRANCA_LEGADO".equals(tipo) ? LEGADO : tipo;
        long offset = pageable.getOffset(), total = 0;
        var selecionados = new ArrayList<AuditoriaCadastro>(tamanho);
        // Stream de candidatos, nunca atribuição por carrier/ID. Classificar antes de count/offset.
        try (var candidatos = repository.findByTipoInAndRegistroIdOrderByIdAsc(CARRIERS, id)) {
            var iterator = candidatos.iterator();
            while (iterator.hasNext()) {
                var evento = iterator.next();
                if (!id.equals(evento.getRegistroId()) || !CARRIERS.contains(evento.getTipo())) {
                    throw RegraNegocioException.conflito(
                            "AUDITORIA_INCONSISTENTE", "Consulta de auditoria inconsistente.");
                }
                if (!destino.equals(classificarFinanceiro(evento))) continue;
                if (total >= offset && selecionados.size() < tamanho) selecionados.add(evento);
                total++;
            }
        }
        return PaginaResponse.de(
                new PageImpl<>(selecionados, pageable, total),
                e -> AuditoriaResponse.de(e, destino));
    }

    private String classificarFinanceiro(AuditoriaCadastro evento) {
        try {
            String depois = classificarDocumento(evento.getDadosDepois(), evento);
            if (LEGADO.equals(depois)) return LEGADO;
            if (evento.getDadosAntes() != null
                    && !depois.equals(classificarDocumento(evento.getDadosAntes(), evento)))
                return LEGADO;
            return depois;
        } catch (RuntimeException desconhecido) {
            // JSON inválido/duplicado/forma desconhecida não prova entidade, não registrar payload.
            return LEGADO;
        }
    }

    private String classificarDocumento(String json, AuditoriaCadastro evento) {
        var nodes = new ArrayList<Map<?, ?>>();
        lerSnapshot(json, nodes);
        boolean encerramento = "ENCERRAMENTO_VIGENCIA".equals(evento.getAcao());
        var tipos = new HashSet<String>();
        boolean idPresente = false;
        boolean envelope = false;
        for (var node : nodes) {
            if (node.containsKey("tipoRegistro") || node.containsKey("registroId")) {
                // Um marcador isolado não pode ser ignorado em favor do resultado interno.
                if (!node.containsKey("tipoRegistro")
                        || !node.containsKey("registroId")
                        || !node.containsKey("resultado")
                        || !VINCULO.equals(node.get("tipoRegistro"))
                        || !mesmoId(node.get("registroId"), evento.getRegistroId())) return LEGADO;
                tipos.add(VINCULO);
                envelope = true;
            }
            if (node.containsKey("id")) {
                if (!mesmoId(node.get("id"), evento.getRegistroId())) return LEGADO;
                idPresente = true;
            }
            if (node.containsKey("tipo")) {
                String classe =
                        switch (String.valueOf(node.get("tipo"))) {
                            case "VINCULO" -> VINCULO;
                            case "CONTRATO" -> "CONTRATO_COBRANCA";
                            case "TABELA", "PADRAO", "ESPECIFICA" -> "TABELA_COBRANCA";
                            default -> LEGADO;
                        };
                if (LEGADO.equals(classe) || !node.containsKey("id")) return LEGADO;
                tipos.add(classe);
            }
            // Ação compartilhada exige tipo/id ou envelope explícitos em CADA documento.
            // tabelaId (inclusive null) não é discriminador de um encerramento.
            if (!encerramento && node.containsKey("tabelaId")) tipos.add(VINCULO);
            if (!encerramento
                    && node.containsKey("fuso")
                    && node.containsKey("moeda")
                    && node.containsKey("modalidadeCiclo")) tipos.add("CONTRATO_COBRANCA");
        }
        if (!idPresente) return LEGADO;
        String esperado;
        if ("VINCULO_TABELA".equals(evento.getAcao())
                && "CONTRATO_COBRANCA".equals(evento.getTipo())) esperado = VINCULO;
        else if ("CONFIGURACAO".equals(evento.getAcao())
                && "CONTRATO_COBRANCA".equals(evento.getTipo())) esperado = "CONTRATO_COBRANCA";
        else if ("CRIACAO".equals(evento.getAcao()) && "TABELA_COBRANCA".equals(evento.getTipo()))
            esperado = "TABELA_COBRANCA";
        else if (encerramento && tipos.size() == 1) esperado = tipos.iterator().next();
        else return LEGADO;
        if (tipos.stream().anyMatch(t -> !esperado.equals(t))) return LEGADO;
        if (VINCULO.equals(esperado)) {
            if (envelope && !"CONTRATO_COBRANCA".equals(evento.getTipo())) return LEGADO;
        } else if (!esperado.equals(evento.getTipo())) return LEGADO;
        return esperado;
    }

    private void lerSnapshot(String json, List<Map<?, ?>> nodes) {
        Object decoded = leitorHistorico.readValue(json, Object.class);
        for (int depth = 0; depth < 3; depth++) {
            if (!(decoded instanceof Map<?, ?> node)) throw new IllegalArgumentException();
            nodes.add(node);
            if (!node.containsKey("resultado")) return;
            decoded = node.get("resultado");
        }
        throw new IllegalArgumentException();
    }

    private static boolean mesmoId(Object valor, Long registroId) {
        if (!(valor instanceof Number) && !(valor instanceof String)) return false;
        // Comparação decimal literal: não passa por double/float nem substitui audit.id/tabelaId.
        return registroId != null && registroId.toString().equals(valor.toString());
    }
}
