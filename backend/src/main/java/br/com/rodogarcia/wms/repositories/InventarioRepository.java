package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.models.InventarioItem;
import br.com.rodogarcia.wms.models.InventarioSessao;
import br.com.rodogarcia.wms.models.TipoInventario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** SQL Server explícito. A estrutura proposta só é usada após ativação do recurso. */
@Repository
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class InventarioRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final JsonMapper mapper;
    private static final String CABECALHO =
            "select i.id,i.versao,i.cliente_id,i.armazem_id,i.tipo,i.produto_id,i.endereco_id,i.situacao,i.iniciado_em,i.encerrado_em,i.usuario,i.motivo,i.divergencias, (select count_big(*) from wms.inventario_item it where it.inventario_id=i.id and it.contado is not null) unidades_lidas from wms.inventario i ";

    public InventarioRepository(NamedParameterJdbcTemplate jdbc, JsonMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    private static MapSqlParameterSource parametros(Object... pares) {
        var p = new MapSqlParameterSource();
        for (int i = 0; i < pares.length; i += 2) p.addValue((String) pares[i], pares[i + 1]);
        return p;
    }

    private InventarioSessao cabecalho(ResultSet r, int linha) throws SQLException {
        var fim = r.getTimestamp("encerrado_em");
        return new InventarioSessao(
                r.getLong("id"),
                r.getLong("versao"),
                r.getLong("cliente_id"),
                r.getLong("armazem_id"),
                TipoInventario.valueOf(r.getString("tipo")),
                r.getObject("produto_id", Long.class),
                r.getObject("endereco_id", Long.class),
                r.getString("situacao"),
                r.getTimestamp("iniciado_em").toInstant(),
                fim == null ? null : fim.toInstant(),
                r.getString("usuario"),
                r.getString("motivo"),
                r.getLong("unidades_lidas"),
                r.getObject("divergencias", Integer.class));
    }

    public InventarioSessao consultar(Long id, boolean bloquear) {
        var sql = CABECALHO + (bloquear ? "with (updlock,holdlock) " : "") + "where i.id=:id";
        var rows = jdbc.query(sql, parametros("id", id), this::cabecalho);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public String relatorio(Long id) {
        return jdbc.queryForObject(
                "select relatorio_json from wms.inventario where id=:id",
                parametros("id", id),
                String.class);
    }

    public long totalMercadorias(Long id) {
        return jdbc.queryForObject(
                "select count_big(*) from wms.inventario_item where inventario_id=:id",
                parametros("id", id),
                Long.class);
    }

    public PaginaResponse<InventarioSessao> listar(
            Long cliente, Long armazem, int pagina, int tamanho) {
        var p =
                parametros(
                        "cliente",
                        cliente,
                        "armazem",
                        armazem,
                        "offset",
                        pagina * tamanho,
                        "tamanho",
                        tamanho);
        String where = "where i.cliente_id=:cliente and i.armazem_id=:armazem ";
        long total =
                jdbc.queryForObject(
                        "select count_big(*) from wms.inventario i " + where, p, Long.class);
        var itens =
                jdbc.query(
                        CABECALHO
                                + where
                                + "order by i.id desc offset :offset rows fetch next :tamanho rows only",
                        p,
                        this::cabecalho);
        return new PaginaResponse<>(
                itens, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }

    public Long criar(InventarioDto.Iniciar d, String usuario, Instant agora) {
        return jdbc.queryForObject(
                """
                insert into wms.inventario(versao,cliente_id,armazem_id,tipo,produto_id,endereco_id,
                situacao,iniciado_em,usuario,motivo) output inserted.id
                values(0,:cliente,:armazem,:tipo,:produto,:endereco,'EM_ANDAMENTO',:agora,:usuario,:motivo)
                """,
                parametros(
                        "cliente",
                        d.clienteId(),
                        "armazem",
                        d.armazemId(),
                        "tipo",
                        d.tipo().name(),
                        "produto",
                        d.produtoId(),
                        "endereco",
                        d.enderecoId(),
                        "agora",
                        Timestamp.from(agora),
                        "usuario",
                        usuario,
                        "motivo",
                        d.motivo().strip()),
                Long.class);
    }

    public void guardarBase(Long id, List<InventarioItem.Base> bases) {
        for (int inicio = 0; inicio < bases.size(); inicio += 500) {
            var lote =
                    bases.subList(inicio, Math.min(inicio + 500, bases.size())).stream()
                            .map(
                                    b ->
                                            parametros(
                                                    "id",
                                                    id,
                                                    "codigo",
                                                    b.codigoUnidade(),
                                                    "json",
                                                    mapper.writeValueAsString(b)))
                            .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate(
                    "insert into wms.inventario_item(inventario_id,codigo_unidade,base_json,prevista) values(:id,:codigo,:json,1)",
                    lote);
        }
    }

    private InventarioItem item(ResultSet r, int linha) throws SQLException {
        var data = r.getTimestamp("lido_em");
        return new InventarioItem(
                mapper.readValue(r.getString("base_json"), InventarioItem.Base.class),
                r.getBoolean("prevista"),
                r.getBigDecimal("contado"),
                r.getObject("endereco_lido", Long.class),
                r.getString("posicao_lida"),
                data == null ? null : data.toInstant(),
                r.getString("usuario"));
    }

    public InventarioItem item(Long id, String codigo) {
        var rows =
                jdbc.query(
                        "select * from wms.inventario_item where inventario_id=:id and codigo_unidade=:codigo",
                        parametros("id", id, "codigo", codigo),
                        this::item);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<InventarioItem> itens(Long id) {
        return jdbc.query(
                "select * from wms.inventario_item where inventario_id=:id order by codigo_unidade",
                parametros("id", id),
                this::item);
    }

    public List<InventarioItem> ultimasLeituras(Long id) {
        return jdbc.query(
                "select top (100) * from wms.inventario_item where inventario_id=:id and contado is not null order by lido_em desc,codigo_unidade",
                parametros("id", id),
                this::item);
    }

    public void ler(
            Long id,
            InventarioItem.Base base,
            InventarioDto.Ler d,
            String posicao,
            String usuario,
            Instant agora) {
        var p =
                parametros(
                        "id",
                        id,
                        "codigo",
                        base.codigoUnidade(),
                        "contado",
                        d.contado(),
                        "endereco",
                        d.enderecoId(),
                        "posicao",
                        posicao,
                        "usuario",
                        usuario,
                        "agora",
                        Timestamp.from(agora),
                        "json",
                        mapper.writeValueAsString(base));
        if (item(id, base.codigoUnidade()) == null) {
            jdbc.update(
                    "insert into wms.inventario_item(inventario_id,codigo_unidade,base_json,prevista) values(:id,:codigo,:json,0)",
                    p);
        }
        jdbc.update(
                "update wms.inventario_item set contado=:contado,endereco_lido=:endereco,posicao_lida=:posicao,usuario=:usuario,lido_em=:agora where inventario_id=:id and codigo_unidade=:codigo",
                p);
        jdbc.update("update wms.inventario set versao=versao+1 where id=:id", p);
    }

    public void encerrar(
            Long id, String situacao, Instant agora, Integer divergencias, Object relatorio) {
        jdbc.update(
                "update wms.inventario set versao=versao+1,situacao=:situacao,encerrado_em=:agora,divergencias=:divergencias,relatorio_json=:relatorio where id=:id",
                parametros(
                        "id",
                        id,
                        "situacao",
                        situacao,
                        "agora",
                        Timestamp.from(agora),
                        "divergencias",
                        divergencias,
                        "relatorio",
                        relatorio == null ? null : mapper.writeValueAsString(relatorio)));
    }

    /** Uma mercadoria aparece uma única vez, inclusive quando ocupa duas posições. */
    public PaginaResponse<InventarioDto.Visao> visao(
            Long cliente,
            Long armazem,
            Long produto,
            Long endereco,
            String busca,
            boolean porPosicao,
            int pagina,
            int tamanho) {
        var p =
                parametros(
                        "cliente",
                        cliente,
                        "armazem",
                        armazem,
                        "produto",
                        produto,
                        "endereco",
                        endereco,
                        "busca",
                        "%"
                                + busca.replace("!", "!!")
                                        .replace("%", "!%")
                                        .replace("_", "!_")
                                        .replace("[", "![")
                                + "%",
                        "offset",
                        pagina * tamanho,
                        "tamanho",
                        tamanho);
        String from =
                """
                 from wms.unidade_logistica u join wms.pedido_entrada pe on pe.id=u.pedido_id
                 join wms.produto p on p.id=u.produto_id
                 where u.ativa=1 and pe.cliente_id=:cliente and pe.armazem_id=:armazem
                 and (:produto is null or p.id=:produto)
                 and (:endereco is null or exists(select 1 from wms.ocupacao_endereco o where o.unidade_id=u.id and o.endereco_id=:endereco))
                 and (p.sku like :busca escape '!' or p.descricao like :busca escape '!')
                """;
        String select =
                porPosicao
                        ? "select p.id,p.sku,p.descricao,p.unidade_medida,u.quantidade,cast(1 as bigint) mercadorias,coalesce((select string_agg(cast(e.codigo as nvarchar(max)),N' + ') within group(order by e.codigo) from wms.ocupacao_endereco o join wms.endereco e on e.id=o.endereco_id where o.unidade_id=u.id),N'Sem endereço') posicoes"
                        : "select p.id,p.sku,p.descricao,p.unidade_medida,sum(u.quantidade) quantidade,count_big(*) mercadorias,cast(null as nvarchar(max)) posicoes";
        String group = porPosicao ? "" : " group by p.id,p.sku,p.descricao,p.unidade_medida";
        String order = porPosicao ? " order by posicoes,p.sku,u.id" : " order by p.sku,p.id";
        long total =
                jdbc.queryForObject(
                        "select count_big(*) from (" + select + from + group + ") q",
                        p,
                        Long.class);
        var rows =
                jdbc.query(
                        select
                                + from
                                + group
                                + order
                                + " offset :offset rows fetch next :tamanho rows only",
                        p,
                        (r, n) ->
                                new InventarioDto.Visao(
                                        r.getLong("id"),
                                        r.getString("sku"),
                                        r.getString("descricao"),
                                        r.getString("unidade_medida"),
                                        r.getBigDecimal("quantidade"),
                                        r.getLong("mercadorias"),
                                        r.getString("posicoes")));
        return new PaginaResponse<>(
                rows, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }
}
