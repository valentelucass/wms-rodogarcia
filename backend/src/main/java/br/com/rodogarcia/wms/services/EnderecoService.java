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
                        Instant.now(clock));
        entidade.alterarTipoUnidadePermitido(dados.tipoUnidadePermitido(), Instant.now(clock));
        entidade = repository.saveAndFlush(entidade);
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
        Long armazemId =
                repository.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(armazemId).orElseThrow(RegraNegocioException::naoEncontrado);
        Endereco entidade =
                repository
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.versao(entidade, dados.versao());
        var codigo =
                dados.codigo() == null
                        ? entidade.getCodigo()
                        : CadastroSupport.codigo(dados.codigo());
        var rua = dados.rua() == null ? entidade.getRua() : CadastroSupport.codigo(dados.rua());
        var nivel = dados.nivel() == null ? entidade.getNivel() : dados.nivel();
        var posicao =
                dados.posicao() == null
                        ? entidade.getPosicao()
                        : CadastroSupport.codigo(dados.posicao());
        var tipo = dados.tipo() == null ? entidade.getTipo() : dados.tipo();
        var tipoUnidadePermitido =
                dados.tipoUnidadePermitido() == null
                        ? entidade.getTipoUnidadePermitido()
                        : dados.tipoUnidadePermitido();
        var capacidadePesoKg =
                dados.tipo() == null ? entidade.getCapacidadePesoKg() : dados.capacidadePesoKg();
        var alturaMetros = dados.tipo() == null ? entidade.getAlturaMetros() : dados.alturaMetros();
        var larguraMetros =
                dados.tipo() == null ? entidade.getLarguraMetros() : dados.larguraMetros();
        var profundidadeMetros =
                dados.tipo() == null
                        ? entidade.getProfundidadeMetros()
                        : dados.profundidadeMetros();
        var empilhamentoMaximo =
                dados.tipo() == null
                        ? entidade.getEmpilhamentoMaximo()
                        : dados.empilhamentoMaximo();
        var sequenciaColeta =
                dados.sequenciaColeta() == null
                        ? entidade.getSequenciaColeta()
                        : dados.sequenciaColeta();
        boolean estrutura =
                !java.util.Objects.equals(rua, entidade.getRua())
                        || !java.util.Objects.equals(nivel, entidade.getNivel())
                        || !java.util.Objects.equals(posicao, entidade.getPosicao())
                        || !java.util.Objects.equals(tipo, entidade.getTipo())
                        || !java.util.Objects.equals(
                                tipoUnidadePermitido, entidade.getTipoUnidadePermitido())
                        || !mesmoDecimal(capacidadePesoKg, entidade.getCapacidadePesoKg())
                        || !mesmoDecimal(alturaMetros, entidade.getAlturaMetros())
                        || !mesmoDecimal(larguraMetros, entidade.getLarguraMetros())
                        || !mesmoDecimal(profundidadeMetros, entidade.getProfundidadeMetros())
                        || !java.util.Objects.equals(
                                empilhamentoMaximo, entidade.getEmpilhamentoMaximo());
        if (estrutura && repository.contarUsoAtual(id) > 0)
            throw RegraNegocioException.conflito(
                    "ENDERECO_EM_USO",
                    "Desocupe a posição e encerre os conjuntos ativos antes de mudar rua, nível, posição, tipo de área, tipo de unidade permitido ou limites. Código e descrição continuam editáveis.");
        var antes = EnderecoDto.Resposta.de(entidade);
        entidade.alterarDescricao(CadastroSupport.texto(dados.descricao()), Instant.now(clock));

        entidade.corrigirCadastro(
                codigo,
                rua,
                nivel,
                posicao,
                tipo,
                capacidadePesoKg,
                alturaMetros,
                larguraMetros,
                profundidadeMetros,
                empilhamentoMaximo,
                sequenciaColeta,
                Instant.now(clock));
        entidade.alterarTipoUnidadePermitido(tipoUnidadePermitido, Instant.now(clock));
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

    private static boolean mesmoDecimal(java.math.BigDecimal a, java.math.BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
