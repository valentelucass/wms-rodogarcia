package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.OperacaoAdministrativa;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Snapshot dos comandos administrativos concretos; autorização/locks pertencem ao serviço chamador.
 */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@Transactional(propagation = Propagation.MANDATORY)
public class OperacaoAdministrativaService {
    private final OperacaoAdministrativaRepository repository;
    private final JsonMapper mapper;
    private final AcessoService acesso;
    private final Clock clock;

    public OperacaoAdministrativaService(
            OperacaoAdministrativaRepository repository,
            JsonMapper mapper,
            AcessoService acesso,
            Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.acesso = acesso;
        this.clock = clock;
    }

    public String hash(String tipo, Object alvo, Object dados) {
        return digest(
                mapper.writeValueAsString(java.util.Arrays.asList(tipo, alvo, dados))
                        .getBytes(StandardCharsets.UTF_8));
    }

    public static String digest(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> T repetida(UUID id, String hash, Class<T> resposta) {
        var existente = repository.findByOperacaoId(id.toString()).orElse(null);
        if (existente == null) return null;
        if (!existente.getConteudoHash().equals(hash))
            throw RegraNegocioException.conflito(
                    "OPERACAO_DIVERGENTE", "Chave já utilizada com outro conteúdo ou alvo.");
        return mapper.readValue(existente.getResultado(), resposta);
    }

    public void salvar(
            UUID id,
            String tipo,
            Cliente cliente,
            Armazem armazem,
            Long recurso,
            String hash,
            Object resultado) {
        repository.saveAndFlush(
                new OperacaoAdministrativa(
                        id.toString(),
                        tipo,
                        cliente,
                        armazem,
                        recurso,
                        hash,
                        mapper.writeValueAsString(resultado),
                        acesso.usuario(),
                        Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
    }
}
