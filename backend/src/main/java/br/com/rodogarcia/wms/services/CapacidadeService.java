package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.CapacidadeDto;
import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class CapacidadeService {
    private final EnderecoRepository enderecos;
    private final ArmazemRepository armazens;
    private final ConjuntoPosicoesRepository conjuntos;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final UnidadeLogisticaRepository unidades;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public CapacidadeService(
            EnderecoRepository enderecos,
            ArmazemRepository armazens,
            ConjuntoPosicoesRepository conjuntos,
            OcupacaoEnderecoRepository ocupacoes,
            UnidadeLogisticaRepository unidades,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock) {
        this.enderecos = enderecos;
        this.armazens = armazens;
        this.conjuntos = conjuntos;
        this.ocupacoes = ocupacoes;
        this.unidades = unidades;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    public EnderecoDto.Resposta configurar(
            @NotNull @Positive Long id, @NotNull @Valid CapacidadeDto.ConfigurarEndereco dados) {
        acesso.exigirGestor();
        Long armazemId =
                enderecos.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(
                armazens.buscarParaAtualizar(armazemId)
                        .orElseThrow(RegraNegocioException::naoEncontrado));
        var e = enderecos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(e);
        CadastroSupport.versao(e, dados.versao());
        if (ocupacoes.existsByEnderecoIdAndUnidadeIsNotNull(id)
                || conjuntos.contarAtivosDoEndereco(id) > 0)
            throw RegraNegocioException.conflito(
                    "ENDERECO_EM_USO",
                    "Desocupe o endereço e encerre os conjuntos ativos antes de alterar sua capacidade.");
        var antes = EnderecoDto.Resposta.de(e);
        var l = dados.limites();
        e.configurarFisico(
                dados.tipoUnidadePermitido(),
                l.pesoKg(),
                l.alturaMetros(),
                l.larguraMetros(),
                l.profundidadeMetros(),
                l.empilhamentoMaximo(),
                agora());
        enderecos.flush();
        var depois = EnderecoDto.Resposta.de(e);
        auditoria.registrar(
                "ENDERECO",
                id,
                "CONFIGURAR_CAPACIDADE",
                CadastroSupport.motivo(dados.motivo()),
                antes,
                depois);
        return depois;
    }

    @Transactional
    public CapacidadeDto.Conjunto criar(@NotNull @Valid CapacidadeDto.CriarConjunto dados) {
        acesso.exigirGestor();
        if (dados.enderecoAId().equals(dados.enderecoBId()))
            throw CadastroSupport.invalido("Informe duas posições diferentes.");
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(armazem);
        var a =
                enderecos
                        .buscarParaAtualizar(Math.min(dados.enderecoAId(), dados.enderecoBId()))
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var b =
                enderecos
                        .buscarParaAtualizar(Math.max(dados.enderecoAId(), dados.enderecoBId()))
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(a);
        CadastroSupport.ativo(b);
        CapacidadeSupport.configurado(a);
        CapacidadeSupport.configurado(b);
        if (!a.getArmazem().getId().equals(armazem.getId())
                || !b.getArmazem().getId().equals(armazem.getId())
                || a.getTipo() != b.getTipo()
                || a.getTipoUnidadePermitido() != b.getTipoUnidadePermitido())
            throw CadastroSupport.invalido(
                    "As posições devem ter mesmo armazém, área e tipo de unidade compatível.");
        var l = dados.limites();
        if (l.pesoKg().compareTo(a.getCapacidadePesoKg().add(b.getCapacidadePesoKg())) > 0
                || l.larguraMetros().compareTo(a.getLarguraMetros().add(b.getLarguraMetros())) > 0
                || l.alturaMetros().compareTo(a.getAlturaMetros().min(b.getAlturaMetros())) > 0
                || l.profundidadeMetros()
                                .compareTo(a.getProfundidadeMetros().min(b.getProfundidadeMetros()))
                        > 0
                || l.empilhamentoMaximo()
                        > Math.min(a.getEmpilhamentoMaximo(), b.getEmpilhamentoMaximo()))
            throw CadastroSupport.invalido(
                    "Limites do conjunto excedem os limites de suas posições.");
        var c =
                conjuntos.saveAndFlush(
                        new ConjuntoPosicoes(
                                armazem,
                                CadastroSupport.codigo(dados.codigo()),
                                a,
                                b,
                                l.pesoKg(),
                                l.alturaMetros(),
                                l.larguraMetros(),
                                l.profundidadeMetros(),
                                l.empilhamentoMaximo(),
                                agora()));
        var resposta = CapacidadeDto.Conjunto.de(c);
        auditoria.registrar(
                "CONJUNTO_POSICOES",
                c.getId(),
                "CRIACAO",
                CadastroSupport.motivo(dados.motivo()),
                null,
                resposta);
        return resposta;
    }

    public PaginaResponse<CapacidadeDto.Conjunto> listar(
            @NotNull @Positive Long armazemId, int pagina, int tamanho) {
        acesso.armazem(armazemId);
        return PaginaResponse.de(
                conjuntos.findByArmazemId(armazemId, CadastroSupport.pagina(pagina, tamanho)),
                CapacidadeDto.Conjunto::de);
    }

    @Transactional
    public CapacidadeDto.Conjunto encerrar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        acesso.exigirGestor();
        Long armazemId =
                conjuntos.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(armazemId).orElseThrow(RegraNegocioException::naoEncontrado);
        var c = conjuntos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(c, dados.versao());
        CadastroSupport.transicao(c, SituacaoCadastro.ENCERRAMENTO_PENDENTE);
        CadastroSupport.ativo(c);
        if (unidades.existsByConjuntoAtualId(id))
            throw RegraNegocioException.conflito(
                    "CONJUNTO_OCUPADO", "Remova a ocupação antes de encerrar o conjunto.");
        var antes = CapacidadeDto.Conjunto.de(c);
        c.alterarSituacao(SituacaoCadastro.ENCERRAMENTO_PENDENTE, agora());
        conjuntos.flush();
        var depois = CapacidadeDto.Conjunto.de(c);
        auditoria.registrar(
                "CONJUNTO_POSICOES",
                id,
                "SOLICITAR_ENCERRAMENTO",
                CadastroSupport.motivo(dados.motivo()),
                antes,
                depois);
        return depois;
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
