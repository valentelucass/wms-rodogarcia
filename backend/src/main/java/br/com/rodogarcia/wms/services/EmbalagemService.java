package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EmbalagemDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
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
public class EmbalagemService {
    private final EmbalagemRepository repository;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;
    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;

    public EmbalagemService(
            EmbalagemRepository repository,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock,
            ProdutoRepository produtos,
            ClienteRepository clientes) {
        this.repository = repository;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
        this.produtos = produtos;
        this.clientes = clientes;
    }

    public EmbalagemDto.Resposta consultar(@NotNull @Positive Long id) {
        Embalagem entidade =
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(entidade.getProduto().getCliente().getId());
        return EmbalagemDto.Resposta.de(entidade);
    }

    public PaginaResponse<EmbalagemDto.Resposta> listar(
            @NotNull @Positive Long produtoId, int pagina, int tamanho) {
        Produto produto =
                produtos.findById(produtoId).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(produto.getCliente().getId());
        return PaginaResponse.de(
                repository.findByProdutoId(produtoId, CadastroSupport.pagina(pagina, tamanho)),
                EmbalagemDto.Resposta::de);
    }

    @Transactional
    public EmbalagemDto.Resposta criar(@NotNull @Valid EmbalagemDto.Criar dados) {
        acesso.exigirGestor();
        // Ler somente o ID evita manter uma situação antiga do produto no contexto JPA antes do
        // lock.
        Long clienteId =
                produtos.buscarClienteId(dados.produtoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        // Ordem de locks: cliente antes do produto, igual à criação/encerramento dos cadastros
        // pais.
        Cliente cliente =
                clientes.buscarParaAtualizar(clienteId)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(cliente);
        Produto produto =
                produtos.buscarParaAtualizar(dados.produtoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(produto);
        CadastroSupport.quantidade(produto, dados.quantidadeProduto());
        Embalagem entidade =
                repository.saveAndFlush(
                        new Embalagem(
                                produto,
                                CadastroSupport.codigo(dados.codigoDun()),
                                CadastroSupport.texto(dados.descricao()),
                                dados.quantidadeProduto(),
                                Instant.now(clock)));
        var resposta = EmbalagemDto.Resposta.de(entidade);
        auditoria.registrar(
                "EMBALAGEM", entidade.getId(), "CRIACAO", "Cadastro inicial", null, resposta);
        return resposta;
    }

    @Transactional
    public EmbalagemDto.Resposta alterar(
            @NotNull @Positive Long id, @NotNull @Valid EmbalagemDto.Alterar dados) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Embalagem entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var antes = EmbalagemDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.descricao()), Instant.now(clock));
        repository.flush();
        var depois = EmbalagemDto.Resposta.de(entidade);
        auditoria.registrar("EMBALAGEM", id, "ALTERACAO", motivo, antes, depois);
        return depois;
    }

    @Transactional
    public EmbalagemDto.Resposta encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(
                id, dados, SituacaoCadastro.ENCERRAMENTO_PENDENTE, "SOLICITAR_ENCERRAMENTO");
    }

    @Transactional
    public EmbalagemDto.Resposta reativar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        return mudarSituacao(id, dados, SituacaoCadastro.ATIVO, "REATIVACAO");
    }

    private EmbalagemDto.Resposta mudarSituacao(
            Long id, RevisaoCadastroRequest dados, SituacaoCadastro destino, String acao) {
        acesso.exigirGestor();
        String motivo = CadastroSupport.motivo(dados.motivo());
        Embalagem entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        CadastroSupport.transicao(entidade, destino);
        var antes = EmbalagemDto.Resposta.de(entidade);
        entidade.alterarSituacao(destino, Instant.now(clock));
        repository.flush();
        var depois = EmbalagemDto.Resposta.de(entidade);
        auditoria.registrar("EMBALAGEM", id, acao, motivo, antes, depois);
        return depois;
    }
}
