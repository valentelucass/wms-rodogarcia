package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ArmazemDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
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
public class ArmazemService {
    private final ArmazemRepository repository;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ArmazemService(
            ArmazemRepository repository,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock) {
        this.repository = repository;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    public ArmazemDto.Resposta consultar(@NotNull @Positive Long id) {
        acesso.armazem(id);
        Armazem entidade =
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        return ArmazemDto.Resposta.de(entidade);
    }

    public PaginaResponse<ArmazemDto.Resposta> listar(int pagina, int tamanho) {
        var paginacao = CadastroSupport.pagina(pagina, tamanho);
        if (acesso.gestor()) {
            return PaginaResponse.de(repository.findAll(paginacao), ArmazemDto.Resposta::de);
        }
        var ids = acesso.armazens();
        return PaginaResponse.de(
                ids.isEmpty()
                        ? org.springframework.data.domain.Page.<Armazem>empty(paginacao)
                        : repository.findByIdIn(ids, paginacao),
                ArmazemDto.Resposta::de);
    }

    @Transactional
    public ArmazemDto.Resposta criar(@NotNull @Valid ArmazemDto.Criar dados) {
        acesso.exigirGestor();
        Armazem entidade =
                repository.saveAndFlush(
                        new Armazem(
                                CadastroSupport.codigo(dados.codigo()),
                                CadastroSupport.texto(dados.nome()),
                                CadastroSupport.documento(dados.documentoFiscal()),
                                CadastroSupport.texto(dados.cidade()),
                                CadastroSupport.codigo(dados.uf()),
                                Instant.now(clock)));
        var resposta = ArmazemDto.Resposta.de(entidade);
        auditoria.registrar(
                "ARMAZEM", entidade.getId(), "CRIACAO", "Cadastro inicial", null, resposta);
        return resposta;
    }

    @Transactional
    public ArmazemDto.Resposta alterar(
            @NotNull @Positive Long id, @NotNull @Valid ArmazemDto.Alterar dados) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Armazem entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var antes = ArmazemDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.nome()), Instant.now(clock));
        repository.flush();
        var depois = ArmazemDto.Resposta.de(entidade);
        auditoria.registrar("ARMAZEM", id, "ALTERACAO", motivo, antes, depois);
        return depois;
    }

    @Transactional
    public ArmazemDto.Resposta encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(
                id, dados, SituacaoCadastro.ENCERRAMENTO_PENDENTE, "SOLICITAR_ENCERRAMENTO");
    }

    @Transactional
    public ArmazemDto.Resposta reativar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(id, dados, SituacaoCadastro.ATIVO, "REATIVACAO");
    }

    private ArmazemDto.Resposta mudarSituacao(
            Long id, RevisaoCadastroRequest dados, SituacaoCadastro destino, String acao) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Armazem entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        CadastroSupport.transicao(entidade, destino);
        var antes = ArmazemDto.Resposta.de(entidade);
        entidade.alterarSituacao(destino, Instant.now(clock));
        repository.flush();
        var depois = ArmazemDto.Resposta.de(entidade);
        auditoria.registrar("ARMAZEM", id, acao, motivo, antes, depois);
        return depois;
    }
}
