package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.UsuarioAcessoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EtiquetaService {
    private final UnidadeLogisticaService acessoUnidade;
    private final UnidadeLogisticaRepository unidades;
    private final ConteudoUnidadeRepository conteudos;
    private final ItemNotaEntradaRepository itens;
    private final OperacaoUnidadeRepository operacoes;
    private final UsuarioAcessoRepository usuarios;
    private final JsonMapper mapper;

    public EtiquetaService(
            UnidadeLogisticaService acessoUnidade,
            UnidadeLogisticaRepository unidades,
            ConteudoUnidadeRepository conteudos,
            ItemNotaEntradaRepository itens,
            OperacaoUnidadeRepository operacoes,
            UsuarioAcessoRepository usuarios,
            JsonMapper mapper) {
        this.acessoUnidade = acessoUnidade;
        this.unidades = unidades;
        this.conteudos = conteudos;
        this.itens = itens;
        this.operacoes = operacoes;
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    public UnidadeLogisticaDto.Etiqueta consultar(UUID codigo) {
        // Reuse the authoritative scope and active-unit validation before any enrichment.
        acessoUnidade.etiqueta(codigo);
        var u =
                unidades.findByCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var notas = new LinkedHashMap<Long, UnidadeLogisticaDto.NotaEtiqueta>();
        for (var origem : conteudos.buscarOrigens(u.getId())) {
            var n = origem.getEntrada().getItemChegada().getItemNota().getNota();
            notas.putIfAbsent(
                    n.getId(), new UnidadeLogisticaDto.NotaEtiqueta(n.getSerie(), n.getNumero()));
        }
        if (notas.isEmpty()) {
            var n = u.getNota();
            notas.put(n.getId(), new UnidadeLogisticaDto.NotaEtiqueta(n.getSerie(), n.getNumero()));
        }
        String conferente = null;
        for (var op :
                operacoes.findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(
                        u.getPedido().getId(),
                        List.of("UNIDADES_CRIADAS", "UNIDADE_DIVIDIDA", "UNIDADES_REAGRUPADAS"))) {
            // Historic snapshots may precede fields added to the current DTO.
            boolean gerouUnidade = false;
            for (var detalhe : mapper.readTree(op.getResultado()).path("unidades")) {
                if (codigo.toString().equals(detalhe.path("unidade").path("codigo").asString())) {
                    gerouUnidade = true;
                    break;
                }
            }
            if (gerouUnidade) {
                conferente = usuarios.buscarNome(op.getUsuario()).orElse(null);
                break;
            }
        }
        return UnidadeLogisticaDto.Etiqueta.de(
                u,
                conferente,
                itens.quantidadeNasNotas(List.copyOf(notas.keySet()), u.getProduto().getId()),
                List.copyOf(notas.values()),
                unidades.countByPedidoIdAndAtivaTrueAndIdLessThanEqual(
                        u.getPedido().getId(), u.getId()),
                unidades.countByPedidoIdAndAtivaTrue(u.getPedido().getId()));
    }
}
