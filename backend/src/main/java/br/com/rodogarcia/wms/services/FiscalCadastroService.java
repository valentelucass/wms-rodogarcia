package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ReferenciaFiscalProduto;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReferenciaFiscalProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalCadastroService {
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final ReferenciaFiscalProdutoRepository referencias;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final OperacaoAdministrativaService operacoes;
    private final Clock clock;

    public FiscalCadastroService(
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            ReferenciaFiscalProdutoRepository referencias,
            AcessoService acesso,
            AuditoriaService auditoria,
            OperacaoAdministrativaService operacoes,
            Clock clock) {
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.referencias = referencias;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.operacoes = operacoes;
        this.clock = clock;
    }

    public FiscalCadastroDto.Complemento cliente(@NotNull @Positive Long id) {
        acesso.cliente(id);
        var c = clientes.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        return new FiscalCadastroDto.Complemento(
                id,
                c.getVersao(),
                c.getCodigo(),
                c.getDocumentoFiscal(),
                FiscalCadastroDto.Dados.de(
                        c.getComplementoFiscal(),
                        c.getCidade(),
                        c.getUf(),
                        c.getFaturamentoEmail(),
                        c.getFaturamentoReferencia()));
    }

    public FiscalCadastroDto.Complemento armazem(@NotNull @Positive Long id) {
        acesso.armazem(id);
        var a = armazens.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        return new FiscalCadastroDto.Complemento(
                id,
                a.getVersao(),
                a.getCodigo(),
                a.getDocumentoFiscal(),
                FiscalCadastroDto.Dados.de(
                        a.getComplementoFiscal(), a.getCidade(), a.getUf(), null, null));
    }

    @Transactional
    public FiscalCadastroDto.Complemento complementarCliente(
            @NotNull @Positive Long id, @NotNull @Valid FiscalCadastroDto.Complementar d) {
        acesso.exigirGestor();
        acesso.cliente(id);
        var c = clientes.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("COMPLEMENTO_FISCAL", List.of("CLIENTE", id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FiscalCadastroDto.Complemento.class);
        if (replay != null) return replay;
        CadastroSupport.versao(c, d.versao());
        CadastroSupport.ativo(c);
        var antes = cliente(id);
        var x = d.dados();
        c.complementar(
                x.modelo(),
                x.cidade(),
                x.uf(),
                x.faturamentoEmail(),
                x.faturamentoReferencia(),
                Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        clientes.flush();
        var resposta = cliente(id);
        auditoria.registrar(
                "CLIENTE",
                id,
                "COMPLEMENTO_FISCAL",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(d.operacaoId(), "COMPLEMENTO_FISCAL", c, null, id, hash, resposta);
        return resposta;
    }

    @Transactional
    public FiscalCadastroDto.Complemento complementarArmazem(
            @NotNull @Positive Long id, @NotNull @Valid FiscalCadastroDto.Complementar d) {
        acesso.exigirGestor();
        acesso.armazem(id);
        var a = armazens.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("COMPLEMENTO_FISCAL", List.of("ARMAZEM", id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FiscalCadastroDto.Complemento.class);
        if (replay != null) return replay;
        CadastroSupport.versao(a, d.versao());
        CadastroSupport.ativo(a);
        var x = d.dados();
        if (x.cidade() == null
                || x.cidade().isBlank()
                || x.uf() == null
                || x.faturamentoEmail() != null
                || x.faturamentoReferencia() != null)
            throw CadastroSupport.invalido(
                    "Cidade/UF obrigatórias; faturamento pertence ao cliente.");
        var antes = armazem(id);
        a.complementar(
                x.modelo(),
                x.cidade().strip(),
                x.uf(),
                null,
                null,
                Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        armazens.flush();
        var resposta = armazem(id);
        auditoria.registrar(
                "ARMAZEM",
                id,
                "COMPLEMENTO_FISCAL",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(d.operacaoId(), "COMPLEMENTO_FISCAL", null, a, id, hash, resposta);
        return resposta;
    }

    public List<FiscalCadastroDto.Referencia> referencias(@NotNull @Positive Long id) {
        Long cliente =
                produtos.buscarClienteId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(cliente);
        return referencias.findByProdutoIdOrderByIdAsc(id).stream()
                .filter(r -> acesso.gestor() || acesso.armazens().contains(r.getArmazem().getId()))
                .map(FiscalCadastroDto.Referencia::de)
                .toList();
    }

    @Transactional
    public FiscalCadastroDto.Referencia referenciar(
            @NotNull @Positive Long id, @NotNull @Valid FiscalCadastroDto.Referenciar d) {
        acesso.exigirGestor();
        var cid = produtos.buscarClienteId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(cid);
        acesso.armazem(d.armazemId());
        var c = clientes.buscarParaAtualizar(cid).orElseThrow(RegraNegocioException::naoEncontrado);
        var a =
                armazens.buscarParaAtualizar(d.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var p = produtos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("REFERENCIA_FISCAL", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FiscalCadastroDto.Referencia.class);
        if (replay != null) return replay;
        CadastroSupport.ativo(c);
        CadastroSupport.ativo(a);
        CadastroSupport.ativo(p);
        if ((d.conferidaPor() == null) != (d.conferidaEm() == null)
                || d.conferidaEm() != null
                        && d.conferidaEm()
                                .isAfter(
                                        Instant.now(clock)
                                                .truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
            throw CadastroSupport.invalido(
                    "Conferência externa exige responsável/data não futura.");
        var r =
                referencias
                        .findByProdutoIdAndArmazemIdAndOperacao(
                                id, a.getId(), CadastroSupport.codigo(d.operacao()))
                        .orElse(null);
        var antes = r == null ? null : FiscalCadastroDto.Referencia.de(r);
        if (d.versao() != (r == null ? 0 : r.getVersao()))
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA", "Consulte a referência vigente.");
        Instant agora = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (r == null)
            r =
                    referencias.save(
                            new ReferenciaFiscalProduto(
                                    p,
                                    a,
                                    CadastroSupport.codigo(d.operacao()),
                                    d.ncm(),
                                    d.cfop(),
                                    d.cest(),
                                    d.enquadramento(),
                                    d.aliquotaIcms(),
                                    d.aliquotaIpi(),
                                    CadastroSupport.texto(d.fonte()),
                                    d.conferidaPor(),
                                    d.conferidaEm(),
                                    agora,
                                    agora));
        else
            r.revisar(
                    d.ncm(),
                    d.cfop(),
                    d.cest(),
                    d.enquadramento(),
                    d.aliquotaIcms(),
                    d.aliquotaIpi(),
                    CadastroSupport.texto(d.fonte()),
                    d.conferidaPor(),
                    d.conferidaEm(),
                    agora);
        referencias.flush();
        var resposta = FiscalCadastroDto.Referencia.de(r);
        auditoria.registrar(
                "PRODUTO",
                id,
                "REFERENCIA_FISCAL",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(d.operacaoId(), "REFERENCIA_FISCAL", c, a, r.getId(), hash, resposta);
        return resposta;
    }
}
