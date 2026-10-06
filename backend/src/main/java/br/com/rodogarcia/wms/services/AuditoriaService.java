package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.dto.AuditoriaResponse;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AuditoriaService {
    private final AuditoriaCadastroRepository repository;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;

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
        repository.saveAndFlush(
                new AuditoriaCadastro(
                        tipo,
                        id,
                        acao,
                        acesso.usuario(),
                        Instant.now(clock),
                        idOperacao,
                        motivo,
                        antes == null ? null : mapper.writeValueAsString(antes),
                        mapper.writeValueAsString(depois)));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaResponse> listar(String tipo, Long id, int pagina, int tamanho) {
        acesso.exigirGestor();
        if (!Set.of(
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
        return PaginaResponse.de(
                repository.findByTipoAndRegistroId(
                        tipo, id, CadastroSupport.pagina(pagina, tamanho)),
                AuditoriaResponse::de);
    }
}
