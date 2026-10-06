package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.ImportacaoEnderecoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.ImportacaoEndereco;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.ImportacaoEnderecoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ImportacaoEnderecoService {
    private final ExcelEnderecoService excel;
    private final ImportacaoEnderecoRepository repository;
    private final ArmazemRepository armazens;
    private final EnderecoRepository enderecos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;

    public ImportacaoEnderecoService(
            ExcelEnderecoService excel,
            ImportacaoEnderecoRepository repository,
            ArmazemRepository armazens,
            EnderecoRepository enderecos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock) {
        this.excel = excel;
        this.repository = repository;
        this.armazens = armazens;
        this.enderecos = enderecos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ImportacaoEnderecoDto.Resultado previa(
            @NotNull @Positive Long armazemId,
            @NotNull byte[] arquivo,
            @NotNull @Valid ImportacaoEnderecoDto.Previa d) {
        acesso.exigirGestor();
        acesso.armazem(armazemId);
        var a =
                armazens.buscarParaAtualizar(armazemId)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String fileHash = OperacaoAdministrativaService.digest(arquivo);
        String hash = operacoes.hash("PREVIA_ENDERECOS", armazemId, List.of(d, fileHash));
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ImportacaoEnderecoDto.Resultado.class);
        if (replay != null) return replay;
        CadastroSupport.ativo(a);
        var lote = excel.ler(armazemId, arquivo);
        var erros = new ArrayList<>(lote.erros());
        erros.addAll(duplicidades(armazemId, lote.linhas()));
        var i =
                repository.saveAndFlush(
                        new ImportacaoEndereco(
                                a,
                                fileHash,
                                1,
                                lote.quantidade(),
                                erros.isEmpty() ? "PREVIA" : "COM_ERROS",
                                mapper.writeValueAsString(lote.linhas()),
                                mapper.writeValueAsString(erros),
                                Instant.now(clock)
                                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                                null,
                                acesso.usuario()));
        var resposta = resultado(i, List.of());
        auditoria.registrar(
                "ARMAZEM",
                armazemId,
                "PREVIA_ENDERECOS",
                CadastroSupport.motivo(d.motivo()),
                null,
                resposta);
        operacoes.salvar(d.operacaoId(), "PREVIA_ENDERECOS", null, a, i.getId(), hash, resposta);
        return resposta;
    }

    public ImportacaoEnderecoDto.Resultado consultar(@NotNull @Positive Long id) {
        acesso.exigirGestor();
        Long aid = repository.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(aid);
        return resultado(
                repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado),
                List.of());
    }

    @Transactional
    public ImportacaoEnderecoDto.Resultado confirmar(
            @NotNull @Positive Long id, @NotNull @Valid ImportacaoEnderecoDto.Confirmar d) {
        acesso.exigirGestor();
        Long aid = repository.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(aid);
        var a = armazens.buscarParaAtualizar(aid).orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("IMPORTACAO_ENDERECOS", id, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ImportacaoEnderecoDto.Resultado.class);
        if (replay != null) return replay;
        CadastroSupport.ativo(a);
        var i = repository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        if (i.getVersao() != d.versao())
            throw RegraNegocioException.conflito("VERSAO_DESATUALIZADA", "Prévia alterada.");
        if (!i.getSituacao().equals("PREVIA") || !i.getArquivoHash().equals(d.arquivoHash()))
            throw RegraNegocioException.conflito(
                    "PREVIA_INVALIDA", "Prévia com erros, já confirmada ou hash divergente.");
        var linhas = linhas(i);
        var conflitos = duplicidades(aid, linhas);
        if (!conflitos.isEmpty())
            throw RegraNegocioException.conflito(
                    "LOTE_DIVERGENTE",
                    "Cadastro mudou após prévia; gere nova prévia para conferir duplicidades.");
        var antes = resultado(i, List.of());
        var criados = new ArrayList<EnderecoDto.Resposta>();
        for (var l : linhas) {
            var e = l.endereco();
            var entidade =
                    new Endereco(
                            a,
                            CadastroSupport.codigo(e.codigo()),
                            CadastroSupport.codigo(e.rua()),
                            e.nivel(),
                            CadastroSupport.codigo(e.posicao()),
                            CadastroSupport.texto(e.descricao()),
                            e.tipo(),
                            e.capacidadePesoKg(),
                            e.alturaMetros(),
                            e.larguraMetros(),
                            e.profundidadeMetros(),
                            e.empilhamentoMaximo(),
                            e.sequenciaColeta(),
                            Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            if (l.tipoUnidadePermitido() != null)
                entidade.configurarFisico(
                        l.tipoUnidadePermitido(),
                        e.capacidadePesoKg(),
                        e.alturaMetros(),
                        e.larguraMetros(),
                        e.profundidadeMetros(),
                        e.empilhamentoMaximo(),
                        Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            enderecos.saveAndFlush(entidade);
            var r = EnderecoDto.Resposta.de(entidade);
            criados.add(r);
            auditoria.registrar(
                    "ENDERECO",
                    entidade.getId(),
                    "CRIACAO",
                    CadastroSupport.motivo(d.motivo()),
                    null,
                    r);
        }
        i.confirmar(Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        repository.flush();
        var resposta = resultado(i, List.copyOf(criados));
        auditoria.registrar(
                "ARMAZEM",
                aid,
                "IMPORTACAO_ENDERECOS",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(d.operacaoId(), "IMPORTACAO_ENDERECOS", null, a, id, hash, resposta);
        return resposta;
    }

    private List<ImportacaoEnderecoDto.Erro> duplicidades(
            Long aid, List<ImportacaoEnderecoDto.Linha> linhas) {
        var codigos = new HashSet<String>();
        var posicoes = new HashSet<String>();
        for (var e : enderecos.findByArmazemIdOrderByIdAsc(aid)) {
            codigos.add(e.getCodigo());
            posicoes.add(chave(e.getRua(), e.getNivel(), e.getPosicao()));
        }
        var erros = new ArrayList<ImportacaoEnderecoDto.Erro>();
        for (var l : linhas) {
            var e = l.endereco();
            if (!codigos.add(CadastroSupport.codigo(e.codigo())))
                erros.add(
                        new ImportacaoEnderecoDto.Erro(
                                l.linha(),
                                "codigo",
                                "CODIGO_DUPLICADO",
                                "Código duplicado no lote ou no armazém."));
            if (!posicoes.add(chave(e.rua(), e.nivel(), e.posicao())))
                erros.add(
                        new ImportacaoEnderecoDto.Erro(
                                l.linha(),
                                "posicao",
                                "POSICAO_DUPLICADA",
                                "Rua/nível/posição duplicada no lote ou no armazém."));
        }
        return erros;
    }

    private static String chave(String rua, int nivel, String posicao) {
        return CadastroSupport.codigo(rua) + ":" + nivel + ":" + CadastroSupport.codigo(posicao);
    }

    private List<ImportacaoEnderecoDto.Linha> linhas(ImportacaoEndereco i) {
        return mapper.readValue(
                i.getLinhasJson(), new TypeReference<List<ImportacaoEnderecoDto.Linha>>() {});
    }

    private ImportacaoEnderecoDto.Resultado resultado(
            ImportacaoEndereco i, List<EnderecoDto.Resposta> criados) {
        return new ImportacaoEnderecoDto.Resultado(
                i.getId(),
                i.getVersao(),
                i.getArmazem().getId(),
                i.getArquivoHash(),
                i.getLayoutVersao(),
                i.getSituacao(),
                i.getQuantidadeLinhas(),
                linhas(i),
                mapper.readValue(
                        i.getErrosJson(), new TypeReference<List<ImportacaoEnderecoDto.Erro>>() {}),
                criados,
                i.getCriadaEm(),
                i.getConfirmadaEm());
    }
}
