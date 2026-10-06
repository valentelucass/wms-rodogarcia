package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
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
public class EnderecoService {
    private final EnderecoRepository repository;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;
    private final ArmazemRepository armazens;

    public EnderecoService(
            EnderecoRepository repository,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock,
            ArmazemRepository armazens) {
        this.repository = repository;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
        this.armazens = armazens;
    }

    public EnderecoDto.Resposta consultar(@NotNull @Positive Long id) {
        Endereco entidade =
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(entidade.getArmazem().getId());
        return EnderecoDto.Resposta.de(entidade);
    }

    public PaginaResponse<EnderecoDto.Resposta> listar(
            @NotNull @Positive Long armazemId, int pagina, int tamanho) {
        acesso.armazem(armazemId);
        return PaginaResponse.de(
                repository.findByArmazemId(armazemId, CadastroSupport.pagina(pagina, tamanho)),
                EnderecoDto.Resposta::de);
    }

    @Transactional
    public EnderecoDto.Resposta criar(@NotNull @Valid EnderecoDto.Criar dados) {
        acesso.exigirGestor();
        Armazem armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(armazem);
        Endereco entidade =
                repository.saveAndFlush(
                        new Endereco(
                                armazem,
                                CadastroSupport.codigo(dados.codigo()),
                                CadastroSupport.codigo(dados.rua()),
                                dados.nivel(),
                                CadastroSupport.codigo(dados.posicao()),
                                CadastroSupport.texto(dados.descricao()),
                                dados.tipo(),
                                dados.capacidadePesoKg(),
                                dados.alturaMetros(),
                                dados.larguraMetros(),
                                dados.profundidadeMetros(),
                                dados.empilhamentoMaximo(),
                                dados.sequenciaColeta(),
                                Instant.now(clock)));
        var resposta = EnderecoDto.Resposta.de(entidade);
        auditoria.registrar(
                "ENDERECO", entidade.getId(), "CRIACAO", "Cadastro inicial", null, resposta);
        return resposta;
    }

    @Transactional
    public EnderecoDto.Resposta alterar(
            @NotNull @Positive Long id, @NotNull @Valid EnderecoDto.Alterar dados) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Endereco entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var antes = EnderecoDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.descricao()), Instant.now(clock));
        repository.flush();
        var depois = EnderecoDto.Resposta.de(entidade);
        auditoria.registrar("ENDERECO", id, "ALTERACAO", motivo, antes, depois);
        return depois;
    }

    @Transactional
    public EnderecoDto.Resposta encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(
                id, dados, SituacaoCadastro.ENCERRAMENTO_PENDENTE, "SOLICITAR_ENCERRAMENTO");
    }

    @Transactional
    public EnderecoDto.Resposta reativar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(id, dados, SituacaoCadastro.ATIVO, "REATIVACAO");
    }

    private EnderecoDto.Resposta mudarSituacao(
            Long id, RevisaoCadastroRequest dados, SituacaoCadastro destino, String acao) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        // Mesmo lock da reserva/movimento; projeção escalar não carrega estado antes do lock.
        Long armazemId =
                repository.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(armazemId);
        armazens.buscarParaAtualizar(armazemId).orElseThrow(RegraNegocioException::naoEncontrado);
        Endereco entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        CadastroSupport.transicao(entidade, destino);
        var antes = EnderecoDto.Resposta.de(entidade);
        entidade.alterarSituacao(destino, Instant.now(clock));
        repository.flush();
        var depois = EnderecoDto.Resposta.de(entidade);
        auditoria.registrar("ENDERECO", id, acao, motivo, antes, depois);
        return depois;
    }
}
