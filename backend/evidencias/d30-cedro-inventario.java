import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.*;

/** Analise javac estatica. Nao executa classes analisadas, processors, aplicacao ou conexoes. */
class D30Inventario {
    static Path root;
    static Trees trees;
    static SourcePositions positions;
    static Elements elements;
    static final IdentityHashMap<Element,String> ids = new IdentityHashMap<>();
    static final List<Map<String,Object>> definitions = new ArrayList<>();
    static final List<Map<String,Object>> uses = new ArrayList<>();
    static final List<Map<String,Object>> calls = new ArrayList<>();
    static final List<Map<String,Object>> conversions = new ArrayList<>();
    static final List<Map<String,Object>> assignments = new ArrayList<>();
    static final Map<String,List<String>> fieldIdsByType = new LinkedHashMap<>();
    static final List<TypeElement> sourceTypes = new ArrayList<>();
    static final List<Map<String,Object>> recordAliases = new ArrayList<>();

    static Map<String,Object> map(Object... values) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i=0;i<values.length;i+=2) result.put(values[i].toString(),values[i+1]);
        return result;
    }
    static String json(Object x) {
        if (x==null) return "null";
        if (x instanceof Number || x instanceof Boolean) return x.toString();
        if (x instanceof Map<?,?> m) {
            StringJoiner j = new StringJoiner(",","{","}");
            for (var e:m.entrySet()) j.add(json(e.getKey().toString())+":"+json(e.getValue()));
            return j.toString();
        }
        if (x instanceof Iterable<?> c) {
            StringJoiner j = new StringJoiner(",","[","]"); for (var e:c) j.add(json(e)); return j.toString();
        }
        String s=x.toString(); StringBuilder b=new StringBuilder("\"");
        for (char c:s.toCharArray()) {
            switch(c) {
                case '\\': b.append("\\\\"); break;
                case '"': b.append("\\\""); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default: if(c<32) b.append(String.format("\\u%04x",(int)c)); else b.append(c);
            }
        }
        return b.append('"').toString();
    }
    static String symbol(Element e) {
        if(e==null) return null;
        if(e instanceof TypeElement t) return t.getQualifiedName().toString();
        return symbol(e.getEnclosingElement())+"#"+e;
    }
    static List<Object> annotations(Element e) {
        List<Object> out=new ArrayList<>();
        if(e!=null) for(var a:e.getAnnotationMirrors()) {
            Map<String,Object> values=new LinkedHashMap<>();
            for(var v:a.getElementValues().entrySet()) values.put(v.getKey().getSimpleName().toString(),v.getValue().toString());
            Map<String,Object> defaults=new LinkedHashMap<>();
            for(var v:elements.getElementValuesWithDefaults(a).entrySet()) defaults.put(v.getKey().getSimpleName().toString(),v.getValue().toString());
            out.add(map("tipo",a.getAnnotationType().toString(),"valoresExplicitos",values,"valoresComDefaults",defaults));
        }
        return out;
    }
    static String file(CompilationUnitTree u) {
        return root.relativize(Path.of(u.getSourceFile().toUri())).toString().replace('\\','/');
    }
    static long pos(CompilationUnitTree u, Tree t) { return positions.getStartPosition(u,t); }
    static Map<String,Object> where(CompilationUnitTree u,Tree t) {
        long p=pos(u,t);
        return map("arquivo",file(u),"linha",p<0?-1:u.getLineMap().getLineNumber(p),
            "coluna",p<0?-1:u.getLineMap().getColumnNumber(p),"inicio",p,"fim",positions.getEndPosition(u,t));
    }
    static String type(TreePath p) {
        try { TypeMirror t=trees.getTypeMirror(p); return t==null?null:t.toString(); }
        catch(RuntimeException e) {return "INDETERMINADO:"+e.getClass().getSimpleName();}
    }
    static String id(CompilationUnitTree u,Tree t,Element e) {
        return "D30-JAVA::"+file(u)+"::"+(e==null?t.getKind():e.getKind())+"::"+symbol(e)+"@"+pos(u,t);
    }
    static abstract class Scanner extends TreePathScanner<Void,Void> {
        final CompilationUnitTree unit;
        Scanner(CompilationUnitTree u) {unit=u;}
        Element el() {return trees.getElement(getCurrentPath());}
        String context() {
            TreePath p=getCurrentPath();
            while(p!=null) {
                if(p.getLeaf() instanceof MethodTree || p.getLeaf() instanceof ClassTree) {
                    Element e=trees.getElement(p); if(ids.containsKey(e)) return ids.get(e);
                }
                p=p.getParentPath();
            }
            return file(unit);
        }
        Map<String,Object> base(Tree t,Element e) {
            Map<String,Object> r=where(unit,t);
            r.put("id",id(unit,t,e)); r.put("simbolo",symbol(e));
            r.put("tipoDeclaracao",e==null?t.getKind().toString():e.getKind().toString());
            r.put("anotacoes",annotations(e));
            r.put("modificadores",e==null?List.of():e.getModifiers().stream().map(Object::toString).sorted().toList());
            if(e!=null) ids.putIfAbsent(e,r.get("id").toString());
            r.put("disposicao","ESTRUTURA_RESOLVIDA_NAO_E_PROVA_COMPORTAMENTAL; VINCULAR_CASO_OU_LACUNA");
            return r;
        }
    }
    static class Definitions extends Scanner {
        Definitions(CompilationUnitTree u) {super(u);}
        public Void visitClass(ClassTree t,Void v) {
            Element e=el();
            if(pos(unit,t)>=0) {
                var r=base(t,e); r.put("nome",t.getSimpleName().toString());
                r.put("heranca",t.getExtendsClause()==null?null:t.getExtendsClause().toString());
                r.put("interfaces",t.getImplementsClause().stream().map(Object::toString).toList());
                if(e instanceof TypeElement te) {
                    sourceTypes.add(te);
                    r.put("tipoQualificado",te.getQualifiedName().toString());
                    r.put("constantesEnum",te.getEnclosedElements().stream().filter(x->x.getKind()==ElementKind.ENUM_CONSTANT).map(x->x.getSimpleName().toString()).toList());
                    r.put("componentesRecord",te.getRecordComponents().stream().map(c->map("nome",c.getSimpleName().toString(),"tipo",c.asType().toString(),"anotacoes",annotations(c))).toList());
                }
                definitions.add(r);
            }
            return super.visitClass(t,v);
        }
        public Void visitMethod(MethodTree t,Void v) {
            Element e=el();
            if(pos(unit,t)>=0) {
                var r=base(t,e); r.put("nome",t.getName().toString());
                r.put("retorno",t.getReturnType()==null?null:type(new TreePath(getCurrentPath(),t.getReturnType())));
                r.put("parametros",t.getParameters().stream().map(Object::toString).toList());
                r.put("throws",t.getThrows().stream().map(Object::toString).toList());
                r.put("valorDefault",t.getDefaultValue()==null?null:t.getDefaultValue().toString());
                r.put("geradoOuSintetico",positions.getEndPosition(unit,t)<0);
                definitions.add(r);
            }
            return super.visitMethod(t,v);
        }
        public Void visitVariable(VariableTree t,Void v) {
            Element e=el();
            if(pos(unit,t)>=0) {
                var r=base(t,e); r.put("nome",t.getName().toString());
                r.put("tipoFonte",t.getType()==null?null:t.getType().toString());
                r.put("tipoResolvido",e==null?type(getCurrentPath()):e.asType().toString());
                r.put("inicializador",t.getInitializer()==null?null:t.getInitializer().toString());
                r.put("tipoInicializador",t.getInitializer()==null?null:type(new TreePath(getCurrentPath(),t.getInitializer())));
                r.put("anotacoesFonte",t.getModifiers().getAnnotations().stream().map(Object::toString).toList());
                r.put("constante",e instanceof VariableElement ve&&ve.getConstantValue()!=null?ve.getConstantValue().toString():null);
                r.put("nullabilidadeJava",e!=null&&e.asType().getKind().isPrimitive()?"PRIMITIVO_NAO_NULL":"REFERENCIA_PODE_NULL; CONFERIR_CONTRATO_VALIDACOES_USOS");
                r.put("defaultJava",t.getInitializer()!=null?"EXPLICITO_INICIALIZADOR":e!=null&&e.getKind()==ElementKind.FIELD?"JVM_DEFAULT_0_FALSE_OU_NULL; CONSTRUTOR_PODE_SUBSTITUIR":"NAO_APLICAVEL_SEM_INICIALIZADOR_DE_CAMPO");
                definitions.add(r);
                if(e!=null&&(e.getKind()==ElementKind.FIELD||e.getKind()==ElementKind.ENUM_CONSTANT))
                    fieldIdsByType.computeIfAbsent(symbol(e.getEnclosingElement()),x->new ArrayList<>()).add(r.get("id").toString());
            }
            return super.visitVariable(t,v);
        }
    }
    static class References extends Scanner {
        References(CompilationUnitTree u) {super(u);}
        Map<String,Object> argument(ExpressionTree a) {
            var r=where(unit,a);
            r.put("expressao",a.toString());r.put("tipo",type(new TreePath(getCurrentPath(),a)));
            r.put("tipoAST",a.getKind().toString());
            if(a instanceof ConditionalExpressionTree c) {
                r.put("condicao",where(unit,c.getCondition()));r.put("condicaoTexto",c.getCondition().toString());
                r.put("ramoTrue",where(unit,c.getTrueExpression()));r.put("ramoTrueTexto",c.getTrueExpression().toString());
                r.put("ramoFalse",where(unit,c.getFalseExpression()));r.put("ramoFalseTexto",c.getFalseExpression().toString());
            }
            return r;
        }
        void use(Tree t) {
            if(pos(unit,t)<0) return;
            Element e=el();
            if(e!=null&&ids.containsKey(e)) {
                var r=where(unit,t);r.put("definicao",ids.get(e));r.put("contexto",context());
                r.put("expressao",t.toString());r.put("tipoUso",t.getKind().toString());
                r.put("contextoAST",getCurrentPath().getParentPath()==null?null:getCurrentPath().getParentPath().getLeaf().getKind().toString());
                uses.add(r);
            }
        }
        public Void visitIdentifier(IdentifierTree t,Void v) {use(t);return super.visitIdentifier(t,v);}
        public Void visitMemberSelect(MemberSelectTree t,Void v) {use(t);return super.visitMemberSelect(t,v);}
        public Void visitMethodInvocation(MethodInvocationTree t,Void v) {
            if(pos(unit,t)>=0) {
                Element e=trees.getElement(new TreePath(getCurrentPath(),t.getMethodSelect()));
                var r=where(unit,t);r.put("contexto",context());r.put("destino",ids.get(e));r.put("simbolo",symbol(e));
                r.put("expressao",t.toString());r.put("tipoRetorno",type(getCurrentPath()));
                r.put("argumentos",t.getArguments().stream().map(this::argument).toList());
                r.put("parametrosDestino",e instanceof ExecutableElement ex?ex.getParameters().stream().map(a->map("nome",a.getSimpleName().toString(),"tipo",a.asType().toString(),"anotacoes",annotations(a))).toList():List.of());
                r.put("externo",!ids.containsKey(e));calls.add(r);
            }
            return super.visitMethodInvocation(t,v);
        }
        public Void visitNewClass(NewClassTree t,Void v) {
            if(pos(unit,t)>=0) {
                Element e=el();var r=where(unit,t);r.put("contexto",context());r.put("destino",ids.get(e));r.put("simbolo",symbol(e));
                r.put("expressao",t.toString());r.put("tipoRetorno",type(getCurrentPath()));r.put("construtor",true);
                r.put("argumentos",t.getArguments().stream().map(this::argument).toList());
                r.put("parametrosDestino",e instanceof ExecutableElement ex?ex.getParameters().stream().map(a->map("nome",a.getSimpleName().toString(),"tipo",a.asType().toString(),"anotacoes",annotations(a))).toList():List.of());
                r.put("externo",!ids.containsKey(e));calls.add(r);
            }
            return super.visitNewClass(t,v);
        }
        public Void visitTypeCast(TypeCastTree t,Void v) {
            if(pos(unit,t)>=0) {var r=where(unit,t);r.put("contexto",context());r.put("de",type(new TreePath(getCurrentPath(),t.getExpression())));r.put("para",type(getCurrentPath()));r.put("expressao",t.toString());conversions.add(r);}
            return super.visitTypeCast(t,v);
        }
        public Void visitAssignment(AssignmentTree t,Void v) {
            if(pos(unit,t)>=0) {
                var r=where(unit,t);TreePath lhs=new TreePath(getCurrentPath(),t.getVariable());
                r.put("destino",ids.get(trees.getElement(lhs)));r.put("contexto",context());
                r.put("de",type(new TreePath(getCurrentPath(),t.getExpression())));r.put("para",type(lhs));
                r.put("expressao",t.toString());assignments.add(r);
            }
            return super.visitAssignment(t,v);
        }
    }
    public static void main(String[] args) throws Exception {
        root=Path.of(args[0]).toAbsolutePath().normalize(); Path output=Path.of(args[2]);
        List<Path> files;
        try(var s=Files.walk(root.resolve("backend/src/main/java"))) {files=new ArrayList<>(s.filter(p->p.toString().endsWith(".java")).sorted().toList());}
        if(args.length>3 && args[3].equals("include-tests")) {
            try(var s=Files.walk(root.resolve("backend/src/test/java"))) {files.addAll(s.filter(p->p.toString().endsWith(".java")).sorted().toList());}
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diag=new DiagnosticCollector<>();
        try(StandardJavaFileManager fm=compiler.getStandardFileManager(diag,Locale.ROOT,StandardCharsets.UTF_8)) {
            var task=(JavacTask)compiler.getTask(null,fm,diag,List.of("--release","21","-proc:none","-encoding","UTF-8","-classpath",args[1]),null,fm.getJavaFileObjectsFromPaths(files));
            var units=new ArrayList<CompilationUnitTree>();task.parse().forEach(units::add);task.analyze();
            trees=Trees.instance(task);positions=trees.getSourcePositions();elements=task.getElements();
            for(var u:units) new Definitions(u).scan(u,null);
            // Accessors implicitos dos records nao aparecem como MethodTree de fonte.
            // Usos sao associados ao componente/campo por Element do compilador, nao pelo nome global.
            for(var te:sourceTypes) for(var rc:te.getRecordComponents()) {
                var field=te.getEnclosedElements().stream().filter(x->x.getKind()==ElementKind.FIELD && x.getSimpleName().contentEquals(rc.getSimpleName())).findFirst().orElseThrow();
                String fid=ids.get(field);
                if(fid!=null) {
                    ids.put(rc.getAccessor(),fid);
                    ids.put(rc,fid);
                    recordAliases.add(map("componente",fid,"tipoRecord",te.getQualifiedName().toString(),"acessor",symbol(rc.getAccessor()),"tipo",rc.asType().toString(),"anotacoesAccessor",annotations(rc.getAccessor()),"criterio","ALIAS_COMPILER_ELEMENT_RECORD_ACCESSOR_TO_COMPONENT; NAO_OCORRENCIA_LEXICAL"));
                }
            }
            for(var u:units) new References(u).scan(u,null);
            var diagnostics=diag.getDiagnostics().stream().map(d->map("tipo",d.getKind().toString(),"linha",d.getLineNumber(),"arquivo",d.getSource()==null?null:d.getSource().getName(),"mensagem",d.getMessage(Locale.ROOT))).toList();
            var result=map("mainArquivos",files.stream().filter(p->p.toString().contains("src\\main\\java")).count(),"arquivosAnalisados",files.size(),"definicoes",definitions,"usosResolvidos",uses,"chamadas",calls,"aliasesRecord",recordAliases,"castsExplicitos",conversions,"atribuicoes",assignments,"camposPorTipo",fieldIdsByType,"diagnosticos",diagnostics,"limite","Javac parse/analyze com-proc:none; tipos/simbolos de fonte produtiva e testes indicados. Resolucao estatica nao prova regra/comportamento/conexao/SQL. Sinteticos/record aliases discriminados; copiar node do historico nao entra.");
            Files.writeString(output,json(result)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            long errors=diag.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count();
            System.out.println("files="+files.size()+" defs="+definitions.size()+" uses="+uses.size()+" calls="+calls.size()+" errors="+errors);
            if(errors!=0) System.exit(2);
        }
    }
}
