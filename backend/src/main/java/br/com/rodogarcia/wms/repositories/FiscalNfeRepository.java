package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas parametrizadas; escritas e locks são coordenados pelo serviço fiscal. */
@Repository
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalNfeRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public FiscalNfeRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static MapSqlParameterSource p(Object... pairs) {
        var p = new MapSqlParameterSource();
        for (int i = 0; i < pairs.length; i += 2) p.addValue((String) pairs[i], pairs[i + 1]);
        return p;
    }
    private SolicitacaoNfe row(ResultSet r, int index) throws SQLException {
        return new SolicitacaoNfe(r.getString("id"), r.getString("plano_id"), r.getString("operacao_id"),
                r.getLong("pedido_id"), r.getString("grupo"), r.getString("natureza"), r.getLong("versao_pedido"),
                r.getString("hash_confirmacao"), r.getString("corpo_json"), r.getString("documento_json"),
                r.getString("emitente_cnpj"), r.getInt("ambiente"), r.getString("situacao"),
                r.getString("notazz_id"), r.getObject("documento_id", Long.class), r.getObject("documento_origem_id", Long.class),
                r.getString("numero"), r.getString("serie"), r.getString("chave_acesso"), r.getString("protocolo"),
                r.getString("xml"), r.getString("mensagem"), r.getString("usuario"),
                r.getTimestamp("criada_em").toInstant(), r.getTimestamp("atualizada_em").toInstant());
    }
    public boolean disponivel() {
        return Boolean.TRUE.equals(jdbc.getJdbcTemplate().queryForObject(
                "select cast(case when object_id(N'wms.solicitacao_nfe') is null then 0 else 1 end as bit)", Boolean.class));
    }
    public SolicitacaoNfe consultar(String id, boolean lock) {
        var rows = jdbc.query("select * from wms.solicitacao_nfe " + (lock ? "with(updlock,holdlock) " : "") + "where id=:id", p("id",id), this::row);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public List<SolicitacaoNfe> doPedido(long id) {
        return jdbc.query("select * from wms.solicitacao_nfe where pedido_id=:id order by criada_em,id", p("id",id), this::row);
    }
    public List<SolicitacaoNfe> daOperacao(String id) {
        return jdbc.query("select * from wms.solicitacao_nfe where operacao_id=:id order by grupo", p("id",id), this::row);
    }
    public List<SolicitacaoNfe> ativas(long id, String natureza) {
        return jdbc.query("select * from wms.solicitacao_nfe where pedido_id=:id and natureza=:natureza and ativa=1", p("id",id,"natureza",natureza), this::row);
    }
    public void inserir(SolicitacaoNfe s) {
        jdbc.update("""
                insert into wms.solicitacao_nfe(id,plano_id,operacao_id,pedido_id,grupo,natureza,versao_pedido,hash_confirmacao,
                  corpo_json,documento_json,emitente_cnpj,ambiente,situacao,documento_origem_id,mensagem,usuario,criada_em,atualizada_em,ativa)
                values(:id,:plano,:operacao,:pedido,:grupo,:natureza,:versao,:hash,:corpo,:documento,:cnpj,:ambiente,'PREPARADA',:origem,:mensagem,:usuario,:agora,:agora,1)
                """,p("id",s.id(),"plano",s.planoId(),"operacao",s.operacaoId(),"pedido",s.pedidoId(),"grupo",s.grupo(),"natureza",s.natureza(),
                "versao",s.versaoPedido(),"hash",s.hashConfirmacao(),"corpo",s.corpoJson(),"documento",s.documentoJson(),"cnpj",s.emitenteCnpj(),
                "ambiente",s.ambiente(),"origem",s.documentoOrigemId(),"mensagem",s.mensagem(),"usuario",s.usuario(),"agora",Timestamp.from(s.criadaEm())));
    }
    /** Lease compartilhada entre processos, sem manter transação durante I/O. */
    public boolean ocuparCanal(String cnpj, int ambiente, String dono, Instant agora, Instant ate) {
        var args=p("cnpj",cnpj,"ambiente",ambiente,"dono",dono,"agora",Timestamp.from(agora),"ate",Timestamp.from(ate));
        jdbc.update("""
                if not exists(select 1 from wms.notazz_canal with(updlock,holdlock) where emitente_cnpj=:cnpj and ambiente=:ambiente)
                  insert into wms.notazz_canal(emitente_cnpj,ambiente,ocupado_ate,dono) values(:cnpj,:ambiente,:agora,:dono)
                """, args);
        return jdbc.update("update wms.notazz_canal set dono=:dono,ocupado_ate=:ate where emitente_cnpj=:cnpj and ambiente=:ambiente and ocupado_ate<=:agora",args)==1;
    }
    public void liberarCanal(String cnpj, int ambiente, String dono, Instant proxima) {
        jdbc.update("update wms.notazz_canal set ocupado_ate=:ate where emitente_cnpj=:cnpj and ambiente=:ambiente and dono=:dono",
                p("cnpj",cnpj,"ambiente",ambiente,"dono",dono,"ate",Timestamp.from(proxima)));
    }
    public void estado(String id,String estado,String mensagem,Instant agora) {
        jdbc.update("update wms.solicitacao_nfe set situacao=:estado,mensagem=:mensagem,atualizada_em=:agora where id=:id",
                p("id",id,"estado",estado,"mensagem",mensagem,"agora",Timestamp.from(agora)));
    }
    public void resultado(String id,String estado,String remoto,String numero,String serie,String chave,String protocolo,String xml,Long documento,String mensagem,Instant agora) {
        jdbc.update("""
                update wms.solicitacao_nfe set situacao=:estado,notazz_id=coalesce(:remoto,notazz_id),numero=coalesce(:numero,numero),
                  serie=coalesce(:serie,serie),chave_acesso=coalesce(:chave,chave_acesso),protocolo=coalesce(:protocolo,protocolo),
                  xml=coalesce(:xml,xml),documento_id=coalesce(:documento,documento_id),mensagem=:mensagem,atualizada_em=:agora,
                  ativa=case when :estado='CANCELADA' then 0 else ativa end where id=:id
                """,p("id",id,"estado",estado,"remoto",remoto,"numero",numero,"serie",serie,"chave",chave,"protocolo",protocolo,"xml",xml,"documento",documento,"mensagem",mensagem,"agora",Timestamp.from(agora)));
    }
    public void evento(String id,String tipo,String usuario,String motivo,Instant agora) {
        jdbc.update("insert into wms.evento_nfe(solicitacao_id,tipo,usuario,motivo,registrado_em) values(:id,:tipo,:usuario,:motivo,:agora)",
                p("id",id,"tipo",tipo,"usuario",usuario,"motivo",motivo,"agora",Timestamp.from(agora)));
    }
    public java.math.BigDecimal quantidadeComprometida(Long itemNotaId) {
        return jdbc.queryForObject("""
                select
                  (select coalesce(sum(pn.quantidade),0) from wms.parcela_nfe pn join wms.solicitacao_nfe sn on sn.id=pn.solicitacao_id
                    where pn.item_nota_id=:item and sn.ativa=1 and sn.natureza='RETORNO_MERCADORIA')
                  + (select coalesce(sum(b.quantidade),0) from wms.baixa_saida b join wms.entrada_conferida e on e.id=b.entrada_origem_id
                    join wms.item_chegada ic on ic.id=e.item_chegada_id where ic.item_nota_id=:item
                      and not exists(select 1 from wms.parcela_nfe pn join wms.solicitacao_nfe sn on sn.id=pn.solicitacao_id
                        where pn.reserva_id=b.reserva_id and sn.ativa=1 and sn.natureza='RETORNO_MERCADORIA'))
                """,p("item",itemNotaId),java.math.BigDecimal.class);
    }
    public void parcela(String id,Long reserva,Long entrada,Long item,java.math.BigDecimal quantidade,java.math.BigDecimal valor) {
        jdbc.update("insert into wms.parcela_nfe(solicitacao_id,reserva_id,entrada_id,item_nota_id,quantidade,valor_total) values(:id,:reserva,:entrada,:item,:q,:valor)",
                p("id",id,"reserva",reserva,"entrada",entrada,"item",item,"q",quantidade,"valor",valor));
    }
    public List<String> eventosComando(String id,String operacao) {
        return jdbc.queryForList("select motivo from wms.evento_nfe where solicitacao_id=:id and tipo=:tipo",p("id",id,"tipo","CANCELAR:"+operacao),String.class);
    }
    public boolean regularizada(String id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select cast(case when regularizada_em is null then 0 else 1 end as bit) from wms.solicitacao_nfe where id=:id",p("id",id),Boolean.class));
    }
    public void concluirRegularizacao(String id,String operacao,Long entrada,Instant agora) {
        if(jdbc.update("update wms.solicitacao_nfe set regularizacao_operacao_id=:operacao,pedido_entrada_id=:entrada,regularizada_em=:agora where id=:id and regularizada_em is null and situacao='AUTORIZADA' and natureza='REGULARIZACAO_ENTRADA'",
                p("id",id,"operacao",operacao,"entrada",entrada,"agora",Timestamp.from(agora)))!=1) throw new IllegalStateException("Regularização fiscal já concluída ou indisponível.");
    }
    public List<Map<String,Object>> fila(Long cliente,Long armazem,String fiscal,String busca,int pagina,int tamanho) {
        return jdbc.queryForList(FILA+" where x.cliente_id=:cliente and x.armazem_id=:armazem and (:fiscal='TODOS' or x.situacao_fiscal=:fiscal) and (:busca='' or x.referencia like :buscaLike) order by x.criado_em desc,x.id desc offset :offset rows fetch next :tamanho rows only",
                p("cliente",cliente,"armazem",armazem,"fiscal",fiscal,"busca",busca,"buscaLike","%"+busca.replace("[","[[]").replace("%","[%]").replace("_","[_]")+"%","offset",(long)pagina*tamanho,"tamanho",tamanho));
    }
    public long total(Long cliente,Long armazem,String fiscal,String busca) {
        return jdbc.queryForObject("select count_big(*) from ("+FILA+") q where q.cliente_id=:cliente and q.armazem_id=:armazem and (:fiscal='TODOS' or q.situacao_fiscal=:fiscal) and (:busca='' or q.referencia like :buscaLike)",
                p("cliente",cliente,"armazem",armazem,"fiscal",fiscal,"busca",busca,"buscaLike","%"+busca.replace("[","[[]").replace("%","[%]").replace("_","[_]")+"%"),Long.class);
    }
    public String situacaoFiscal(Long pedido) {
        return jdbc.queryForObject("select situacao_fiscal from ("+FILA+") q where q.id=:id",p("id",pedido),String.class);
    }
    private static final String FILA="""
            select x.* from (
              select p.id,p.versao,p.referencia,p.criado_em,p.cliente_id,c.nome cliente,p.armazem_id,p.situacao,
                case when (
                  (exists(select 1 from wms.solicitacao_nfe sn where sn.pedido_id=p.id and sn.ativa=1 and sn.natureza='RETORNO_SIMBOLICO' and sn.situacao='AUTORIZADA')
                    and not exists(select 1 from wms.solicitacao_nfe sn where sn.pedido_id=p.id and sn.ativa=1 and sn.natureza='RETORNO_MERCADORIA'))
                  or (exists(select 1 from wms.reserva_saida r where r.pedido_id=p.id and r.situacao<>'CANCELADA')
                  and not exists(select 1 from wms.reserva_saida r where r.pedido_id=p.id and r.situacao<>'CANCELADA'
                    and r.quantidade<>(select coalesce(sum(cd.quantidade),0) from wms.cobertura_documento_saida cd join wms.documento_saida d on d.id=cd.documento_id
                      where cd.reserva_id=r.id and d.situacao='AUTORIZADO' and d.natureza='RETORNO_MERCADORIA'))))
                  and not exists(select 1 from wms.solicitacao_nfe sn where sn.pedido_id=p.id and sn.ativa=1 and sn.natureza<>'REGULARIZACAO_ENTRADA' and sn.situacao<>'AUTORIZADA')
                  then 'FINALIZADO' else 'PENDENTE' end situacao_fiscal,
                (select string_agg(cast(d.numero as varchar(max)),', ') within group(order by d.id) from wms.documento_saida d
                  where d.pedido_id=p.id and d.situacao='AUTORIZADO') numeros
              from wms.pedido_saida p join wms.cliente c on c.id=p.cliente_id
            ) x
            """;
}
