package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ClienteService {
    private final ClienteRepository repository;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ClienteService(
            ClienteRepository repository,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock) {
        this.repository = repository;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public ClienteDto.Resposta consultar(@NotNull @Positive Long id) {
        acesso.cliente(id);
        Cliente entidade =
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        return ClienteDto.Resposta.de(entidade);
    }

    public PaginaResponse<ClienteDto.Resposta> listar(int pagina, int tamanho) {
        var paginacao = CadastroSupport.pagina(pagina, tamanho);
        if (acesso.gestor()) {
            return PaginaResponse.de(repository.findAll(paginacao), ClienteDto.Resposta::de);
        }
        var ids = acesso.clientes();
        return PaginaResponse.de(
                ids.isEmpty()
                        ? org.springframework.data.domain.Page.<Cliente>empty(paginacao)
                        : repository.findByIdIn(ids, paginacao),
                ClienteDto.Resposta::de);
    }

    @Transactional
    public ClienteDto.Resposta criar(@NotNull @Valid ClienteDto.Criar dados) {
        acesso.exigirGestor();
        Cliente entidade =
                repository.saveAndFlush(
                        new Cliente(
                                CadastroSupport.codigo(dados.codigo()),
                                CadastroSupport.texto(dados.nome()),
                                CadastroSupport.documento(dados.documentoFiscal()),
                                Instant.now(clock)));
        var resposta = ClienteDto.Resposta.de(entidade);
        auditoria.registrar(
                "CLIENTE", entidade.getId(), "CRIACAO", "Cadastro inicial", null, resposta);
        return resposta;
    }

    @Transactional
    public ClienteDto.Resposta alterar(
            @NotNull @Positive Long id, @NotNull @Valid ClienteDto.Alterar dados) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Cliente entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var antes = ClienteDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.nome()), Instant.now(clock));
        repository.flush();
        var depois = ClienteDto.Resposta.de(entidade);
        auditoria.registrar("CLIENTE", id, "ALTERACAO", motivo, antes, depois);
        return depois;
    }

    @Transactional
    public ClienteDto.Resposta encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(
                id, dados, SituacaoCadastro.ENCERRAMENTO_PENDENTE, "SOLICITAR_ENCERRAMENTO");
    }

    @Transactional
    public ClienteDto.Resposta reativar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(id, dados, SituacaoCadastro.ATIVO, "REATIVACAO");
    }

    private ClienteDto.Resposta mudarSituacao(
            Long id, RevisaoCadastroRequest dados, SituacaoCadastro destino, String acao) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Cliente entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        CadastroSupport.transicao(entidade, destino);
        var antes = ClienteDto.Resposta.de(entidade);
        entidade.alterarSituacao(destino, Instant.now(clock));
        repository.flush();
        var depois = ClienteDto.Resposta.de(entidade);
        auditoria.registrar("CLIENTE", id, acao, motivo, antes, depois);
        return depois;
    }
}
