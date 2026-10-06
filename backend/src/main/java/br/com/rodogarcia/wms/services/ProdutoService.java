package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
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
public class ProdutoService {
    private final ProdutoRepository repository;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;
    private final ClienteRepository clientes;

    public ProdutoService(
            ProdutoRepository repository,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock,
            ClienteRepository clientes) {
        this.repository = repository;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
        this.clientes = clientes;
    }

    public ProdutoDto.Resposta consultar(@NotNull @Positive Long id) {
        Produto entidade =
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(entidade.getCliente().getId());
        return ProdutoDto.Resposta.de(entidade);
    }

    public PaginaResponse<ProdutoDto.Resposta> listar(
            @NotNull @Positive Long clienteId, int pagina, int tamanho) {
        acesso.cliente(clienteId);
        return PaginaResponse.de(
                repository.findByClienteId(clienteId, CadastroSupport.pagina(pagina, tamanho)),
                ProdutoDto.Resposta::de);
    }

    @Transactional
    public ProdutoDto.Resposta criar(@NotNull @Valid ProdutoDto.Criar dados) {
        acesso.exigirGestor();
        if (dados.tipoQuantidade() == TipoQuantidade.CONTAGEM && dados.precisaoQuantidade() != 0) {
            throw CadastroSupport.invalido("Produto contado exige precisão zero.");
        }
        if (dados.controlaValidade() != (dados.antecedenciaAvisoDias() != null)) {
            throw CadastroSupport.invalido(
                    "Informe antecedência somente quando houver controle de validade.");
        }
        Cliente cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(cliente);
        Produto entidade =
                repository.saveAndFlush(
                        new Produto(
                                cliente,
                                CadastroSupport.codigo(dados.sku()),
                                CadastroSupport.texto(dados.descricao()),
                                CadastroSupport.codigo(dados.unidadeMedida()),
                                dados.tipoQuantidade(),
                                dados.precisaoQuantidade(),
                                dados.controlaLote(),
                                dados.controlaValidade(),
                                dados.antecedenciaAvisoDias(),
                                Instant.now(clock)));
        var resposta = ProdutoDto.Resposta.de(entidade);
        auditoria.registrar(
                "PRODUTO", entidade.getId(), "CRIACAO", "Cadastro inicial", null, resposta);
        return resposta;
    }

    @Transactional
    public ProdutoDto.Resposta alterar(
            @NotNull @Positive Long id, @NotNull @Valid ProdutoDto.Alterar dados) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Produto entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var antes = ProdutoDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.descricao()), Instant.now(clock));
        repository.flush();
        var depois = ProdutoDto.Resposta.de(entidade);
        auditoria.registrar("PRODUTO", id, "ALTERACAO", motivo, antes, depois);
        return depois;
    }

    @Transactional
    public ProdutoDto.Resposta encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(
                id, dados, SituacaoCadastro.ENCERRAMENTO_PENDENTE, "SOLICITAR_ENCERRAMENTO");
    }

    @Transactional
    public ProdutoDto.Resposta reativar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(id, dados, SituacaoCadastro.ATIVO, "REATIVACAO");
    }

    private ProdutoDto.Resposta mudarSituacao(
            Long id, RevisaoCadastroRequest dados, SituacaoCadastro destino, String acao) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Produto entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        CadastroSupport.transicao(entidade, destino);
        var antes = ProdutoDto.Resposta.de(entidade);
        entidade.alterarSituacao(destino, Instant.now(clock));
        repository.flush();
        var depois = ProdutoDto.Resposta.de(entidade);
        auditoria.registrar("PRODUTO", id, acao, motivo, antes, depois);
        return depois;
    }
}
