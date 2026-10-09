import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.Query;
import tools.jackson.databind.json.JsonMapper;

/** Traducao semantica local: nenhuma sessao, JDBC, query executada ou DDL. */
class D30CedroJpqlEscopo {
    static final Map<String, Integer> LINHAS = Map.of(
        "buscarElegiveisSaida", 90, "buscarElegiveis", 107, "buscarCandidatas", 113,
        "buscarDisponiveis", 116, "consultarEstoque", 125, "filtrarEstoque", 137,
        "somarDisponivel", 169);
    static final class SemConexao implements ConnectionProvider {
        int tentativas;
        public Connection getConnection() { tentativas++; throw new AssertionError("JDBC proibido"); }
        public void closeConnection(Connection c) { throw new AssertionError("Conexao inexistente"); }
        public boolean supportsAggressiveRelease() { return false; }
        public boolean isUnwrappableAs(Class<?> c) { return c.isInstance(this); }
        public <T> T unwrap(Class<T> c) { return c.cast(this); }
    }
    static Field campo(Class<?> c, String nome) throws Exception {
        for (Class<?> atual = c; atual != null; atual = atual.getSuperclass()) {
            try { return atual.getDeclaredField(nome); } catch (NoSuchFieldException e) { }
        }
        throw new NoSuchFieldException(c.getName() + "." + nome);
    }
    static class Leitor extends BaseSemanticQueryWalker {
        int proximo;
        final IdentityHashMap<SqmRoot<?>, String> escopos = new IdentityHashMap<>();
        final List<Map<String, Object>> ocorrencias = new ArrayList<>();
        public Object visitQuerySpec(SqmQuerySpec<?> spec) {
            String escopo = "subquery-" + (++proximo);
            for (var root : spec.getFromClause().getRoots()) escopos.put(root, escopo);
            return super.visitQuerySpec(spec);
        }
        public Object visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
            var root = path.findRoot();
            if (root != null && "c".equals(root.getExplicitAlias())) {
                var partes = new ArrayList<String>();
                for (SqmPath<?> p = path; p != null && p != root; p = p.getLhs())
                    partes.addFirst(p.getReferencedPathSource().getPathName());
                var expressao = "c." + String.join(".", partes);
                if (Set.of("c.situacao", "c.unidade.id", "c.impedimento", "c.entrada.id").contains(expressao)) {
                    try {
                        Class<?> modelo = root.getModel().getBindableJavaType();
                        var cadeia = new ArrayList<Map<String, Object>>();
                        Class<?> atual = modelo;
                        for (String nome : partes) {
                            Field field = campo(atual, nome);
                            var coluna = field.getAnnotation(Column.class);
                            var join = field.getAnnotation(JoinColumn.class);
                            var tabela = atual.getAnnotation(Table.class);
                            var item = new LinkedHashMap<String, Object>();
                            item.put("modelo", atual.getName());
                            item.put("declaradoEm", field.getDeclaringClass().getName());
                            item.put("propriedade", nome);
                            item.put("tipo", field.getType().getName());
                            item.put("tabela", tabela == null ? "herdada" : tabela.name());
                            item.put("coluna", join != null ? join.name() : coluna != null && !coluna.name().isEmpty() ? coluna.name() : nome);
                            item.put("associacao", join != null);
                            cadeia.add(item); atual = field.getType();
                        }
                        var registro = new LinkedHashMap<String, Object>();
                        registro.put("escopo", escopos.get(root));
                        registro.put("alias", root.getExplicitAlias());
                        registro.put("expressao", expressao);
                        registro.put("raizTipadaSQM", modelo.getName());
                        registro.put("tipoResultadoSQM", path.getReferencedPathSource().getBindableJavaType().getName());
                        registro.put("cadeia", cadeia);
                        ocorrencias.add(registro);
                    } catch (Exception e) { throw new IllegalStateException(e); }
                }
            }
            return super.visitBasicValuedPath(path);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Destino proprio obrigatorio");
        Path destino = Path.of(args[0]);
        if (!destino.getFileName().toString().startsWith("d30-cedro-")) throw new AssertionError("Destino alheio");
        var bloqueio = new SemConexao();
        var registry = new StandardServiceRegistryBuilder()
            .addService(ConnectionProvider.class, bloqueio)
            .applySettings(Map.of("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false", "hibernate.hbm2ddl.auto", "none",
                "jakarta.persistence.schema-generation.database.action", "none",
                "jakarta.persistence.schema-generation.scripts.action", "none"))
            .build();
        var saida = new LinkedHashMap<String, Object>();
        var consultas = new ArrayList<Map<String, Object>>();
        try {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var entidades = scanner.findCandidateComponents("br.com.rodogarcia.wms.models");
            if (entidades.size() != 64) throw new AssertionError("Inventario entidades divergente");
            for (var e : entidades) sources.addAnnotatedClass(Class.forName(e.getBeanClassName()));
            try (var factory = (SessionFactoryImplementor) sources.buildMetadata().buildSessionFactory()) {
                for (var method : UnidadeLogisticaRepository.class.getDeclaredMethods()) {
                    if (!LINHAS.containsKey(method.getName())) continue;
                    String hql = method.getAnnotation(Query.class).value();
                    var arvore = factory.getQueryEngine().getHqlTranslator().translate(hql, null);
                    var leitor = new Leitor(); arvore.accept(leitor);
                    var registro = new LinkedHashMap<String, Object>();
                    registro.put("id", "Q:UnidadeLogisticaRepository." + method.getName() + "@" + LINHAS.get(method.getName()));
                    registro.put("consultaIntegral", hql);
                    registro.put("subqueriesComRaiz", leitor.proximo);
                    registro.put("ocorrencias", leitor.ocorrencias);
                    if (leitor.ocorrencias.isEmpty()) throw new AssertionError("Nenhum alias resolvido");
                    consultas.add(registro);
                }
            }
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
        if (consultas.size() != 7 || bloqueio.tentativas != 0) throw new AssertionError("Limite falhou");
        saida.put("metodo", "Hibernate HQL/SQM real, aliases ligados a cada raiz de subquery; sem executar consulta");
        saida.put("tentativasJDBC", bloqueio.tentativas);
        saida.put("queriesExecutadas", 0);
        saida.put("entidadesMetadados", 64);
        saida.put("consultas", consultas);
        Files.writeString(destino, JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(saida),
            StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}
