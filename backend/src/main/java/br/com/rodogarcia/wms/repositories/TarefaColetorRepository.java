package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.TarefaColetorDto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class TarefaColetorRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String FONTE =
            """
        select t.*,c.nome cliente,a.nome armazem,
          coalesce(pe.referencia,ps.referencia,concat('Inventário ',iv.id)) referencia,
          pr.sku,pr.descricao,u.codigo codigo_unidade,
          case
           when t.tipo='ENDERECAMENTO' and u.primeiro_enderecamento_em is not null then 'CONCLUIDA'
           when t.tipo='ENDERECAMENTO' and (u.ativa=0 or u.quantidade<=0) then 'CANCELADA'
           when t.tipo='SEPARACAO' and ps.situacao in ('SEPARADO','RETIRADO') then 'CONCLUIDA'
           when t.tipo='SEPARACAO' and ps.situacao='CANCELADO' then 'CANCELADA'
           when t.tipo='INVENTARIO' and iv.situacao='CONCLUIDO' then 'CONCLUIDA'
           when t.tipo='INVENTARIO' and iv.situacao='CANCELADO' then 'CANCELADA'
           when t.tipo='SEPARACAO' and ps.situacao='RASCUNHO' then 'AGUARDANDO_RESERVA'
           when t.responsavel is not null then 'EM_ANDAMENTO'
           else 'PENDENTE' end situacao
        from wms.coletor_tarefa t
        join wms.cliente c on c.id=t.cliente_id
        join wms.armazem a on a.id=t.armazem_id
        left join wms.unidade_logistica u on t.tipo='ENDERECAMENTO' and u.id=t.origem_id
        left join wms.pedido_entrada pe on pe.id=u.pedido_id
        left join wms.produto pr on pr.id=u.produto_id
        left join wms.pedido_saida ps on t.tipo='SEPARACAO' and ps.id=t.origem_id
        left join wms.inventario iv on t.tipo='INVENTARIO' and iv.id=t.origem_id
        """;

    public TarefaColetorRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource p(Object... pares) {
        var p = new MapSqlParameterSource();
        for (int i = 0; i < pares.length; i += 2) p.addValue((String) pares[i], pares[i + 1]);
        return p;
    }

    public void registrar(String tipo, Long origem, Long cliente, Long armazem, Instant agora) {
        jdbc.update(
                """
            insert into wms.coletor_tarefa(tipo,origem_id,cliente_id,armazem_id,criada_em)
            select :tipo,:origem,:cliente,:armazem,:agora
            where not exists(select 1 from wms.coletor_tarefa with(updlock,holdlock)
              where tipo=:tipo and origem_id=:origem)
            """,
                p(
                        "tipo",
                        tipo,
                        "origem",
                        origem,
                        "cliente",
                        cliente,
                        "armazem",
                        armazem,
                        "agora",
                        Timestamp.from(agora)));
    }

    private TarefaColetorDto.Tarefa tarefa(ResultSet r, String usuario) throws SQLException {
        var iniciada = r.getTimestamp("iniciada_em");
        var responsavel = r.getString("responsavel");
        String situacao = r.getString("situacao");
        return new TarefaColetorDto.Tarefa(
                r.getLong("id"),
                r.getLong("versao"),
                r.getString("tipo"),
                r.getLong("origem_id"),
                r.getLong("cliente_id"),
                r.getString("cliente"),
                r.getLong("armazem_id"),
                r.getString("armazem"),
                r.getString("referencia"),
                r.getString("sku"),
                r.getString("descricao"),
                r.getString("codigo_unidade"),
                situacao,
                "AGUARDANDO_RESERVA".equals(situacao)
                        ? "Aguarde a confirmação da reserva do pedido."
                        : null,
                r.getTimestamp("criada_em").toInstant(),
                responsavel,
                iniciada == null ? null : iniciada.toInstant(),
                usuario.equals(responsavel));
    }

    public TarefaColetorDto.Tarefa consultar(Long id, String usuario, boolean bloquear) {
        if (bloquear)
            jdbc.queryForList(
                    "select id from wms.coletor_tarefa with(updlock,holdlock) where id=:id",
                    p("id", id));
        var rows =
                jdbc.query(
                        "select * from (" + FONTE + ") q where id=:id",
                        p("id", id),
                        (r, n) -> tarefa(r, usuario));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public TarefaColetorDto.Tarefa porPedido(Long pedidoId, String usuario) {
        var rows =
                jdbc.query(
                        "select * from ("
                                + FONTE
                                + ") q where tipo='SEPARACAO' and origem_id=:pedido",
                        p("pedido", pedidoId),
                        (r, n) -> tarefa(r, usuario));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String escopo(
            MapSqlParameterSource params,
            Long cliente,
            Long armazem,
            boolean gestor,
            List<Long> clientes,
            List<Long> armazens) {
        String sql = " where situacao not in ('CONCLUIDA','CANCELADA') ";
        if (cliente != null) {
            sql += " and cliente_id=:cliente";
            params.addValue("cliente", cliente);
        }
        if (armazem != null) {
            sql += " and armazem_id=:armazem";
            params.addValue("armazem", armazem);
        }
        if (!gestor) {
            if (clientes.isEmpty() || armazens.isEmpty()) return sql + " and 1=0";
            sql += " and cliente_id in (:clientes) and armazem_id in (:armazens)";
            params.addValue("clientes", clientes).addValue("armazens", armazens);
        }
        return sql;
    }

    public TarefaColetorDto.Totais totais(
            Long cliente, Long armazem, boolean gestor, List<Long> clientes, List<Long> armazens) {
        var params = p();
        var where = escopo(params, cliente, armazem, gestor, clientes, armazens);
        var rows =
                jdbc.queryForList(
                        "select tipo,count_big(*) total from ("
                                + FONTE
                                + ") q"
                                + where
                                + " group by tipo",
                        params);
        long end = 0, sep = 0, inv = 0;
        for (var row : rows) {
            long n = ((Number) row.get("total")).longValue();
            switch ((String) row.get("tipo")) {
                case "ENDERECAMENTO" -> end = n;
                case "SEPARACAO" -> sep = n;
                case "INVENTARIO" -> inv = n;
                default -> {}
            }
        }
        return new TarefaColetorDto.Totais(end, sep, inv);
    }

    public PaginaResponse<TarefaColetorDto.Tarefa> listar(
            String tipo,
            Long cliente,
            Long armazem,
            boolean gestor,
            List<Long> clientes,
            List<Long> armazens,
            String usuario,
            int pagina,
            int tamanho) {
        var params = p("tipo", tipo, "offset", pagina * tamanho, "tamanho", tamanho);
        var where =
                escopo(params, cliente, armazem, gestor, clientes, armazens) + " and tipo=:tipo";
        long total =
                jdbc.queryForObject(
                        "select count_big(*) from (" + FONTE + ") q" + where, params, Long.class);
        var rows =
                jdbc.query(
                        "select * from ("
                                + FONTE
                                + ") q"
                                + where
                                + " order by id offset :offset rows fetch next :tamanho rows only",
                        params,
                        (r, n) -> tarefa(r, usuario));
        return new PaginaResponse<>(
                rows, pagina, tamanho, total, (int) ((total + tamanho - 1) / tamanho));
    }

    public void assumir(Long id, String usuario, Instant agora) {
        jdbc.update(
                "update wms.coletor_tarefa set responsavel=:usuario,iniciada_em=:agora,versao=versao+1 where id=:id",
                p("id", id, "usuario", usuario, "agora", Timestamp.from(agora)));
    }

    public void liberar(Long id) {
        jdbc.update(
                "update wms.coletor_tarefa set responsavel=null,iniciada_em=null,versao=versao+1 where id=:id",
                p("id", id));
    }

    public record Operacao(String hash, String usuario, String resultado) {}

    public Operacao operacao(Long id, UUID operacao) {
        var rows =
                jdbc.query(
                        "select conteudo_hash,usuario,resultado from wms.coletor_operacao where tarefa_id=:id and operacao_id=:op",
                        p("id", id, "op", operacao.toString()),
                        (r, n) -> new Operacao(r.getString(1), r.getString(2), r.getString(3)));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public void registrarOperacao(
            Long id,
            UUID op,
            String tipo,
            String usuario,
            Instant agora,
            String hash,
            String resultado) {
        jdbc.update(
                """
            insert into wms.coletor_operacao(tarefa_id,operacao_id,tipo,usuario,registrada_em,conteudo_hash,resultado)
            values(:id,:op,:tipo,:usuario,:agora,:hash,:resultado)
            """,
                p(
                        "id",
                        id,
                        "op",
                        op.toString(),
                        "tipo",
                        tipo,
                        "usuario",
                        usuario,
                        "agora",
                        Timestamp.from(agora),
                        "hash",
                        hash,
                        "resultado",
                        resultado));
    }
}
