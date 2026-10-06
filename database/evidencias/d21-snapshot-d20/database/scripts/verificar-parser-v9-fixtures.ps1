
$WmsD20Pathverificarparserv9fixtures=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
# Auto-check de fixtures textuais ficticias. NAO le/pareia backend/doc31/V9 ou freeze real.
$ErrorActionPreference='Stop'
try {
    . (Join-Path $WmsD20Pathverificarparserv9fixtures 'scripts/leitores/v9-java-arquivos.ps1')
    $checks=[ordered]@{}
    function CheckV9([string]$id,[bool]$ok){$script:checks[$id]=$ok}
    function BloqueiaV9([scriptblock]$f){try{& $f|Out-Null;return $false}catch{return $true}}
    $s='class F { String url="https://exemplo.invalid//x"; /* fake(); */ void f(){real("x,y", Map.of("k", List.of(1,2)));} }'
    $sem=V9SemComentarios $s
    CheckV9 'comentarios_preservam_offset_e_literal' ($sem.Length -eq $s.Length -and $sem.Contains('https://exemplo.invalid//x') -and -not $sem.Contains('fake();'))
    $ch=@(V9Chamadas $s '\breal');CheckV9 'argumentos_balanceados_com_string_e_lista' ($ch.Count -eq 1 -and $ch[0].argumentos.Count -eq 2 -and $ch[0].argumentos[0] -eq '"x,y"')
    CheckV9 'delimitador_incompleto_bloqueia' (BloqueiaV9 {V9Delimitado '(x' 0})
    CheckV9 'delimitador_incompativel_bloqueia' (BloqueiaV9 {V9Delimitado '(x]' 0})
    $j=@'
@Entity
@Table(schema="wms", name="fixture", uniqueConstraints=@UniqueConstraint(columnNames={"cliente_id","ref"},name="uk_f"))
public class F extends Base {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,precision=19,scale=6,updatable=false) private BigDecimal quantidade;
 @Nationalized @Column(name="prova_json",columnDefinition="nvarchar(max)",updatable=false) private String prova;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private Estado situacao;
 @ManyToOne(optional=false) @JoinColumn(name="cliente_id",nullable=false,updatable=false) private Cliente cliente;
 @JoinColumn(name="entrada_id",unique=true) private Entrada entrada;
 @Version @Column(nullable=false) private long versao;
 @Transient private String transitorio;
 private static final long serial = 1;
 /* @Column private String falsa; */
 @Column private String mensagem = "private String falsa; ops.salvar(";
}
'@
    $m=V9ModelTexto $j
    CheckV9 'table_anotacao_aninhada_ordem_atributos' ($m.tabela -eq 'fixture' -and $m.schema -eq 'wms')
    CheckV9 'heranca_identificada' ($m.base -eq 'Base')
    CheckV9 'campos_estaticos_transientes_e_comentados_ignorados' ($m.campos.Count -eq 8 -and 'falsa' -cnotin $m.campos.campoJava -and 'transitorio' -cnotin $m.campos.campoJava)
    $q=@($m.campos|Where-Object {$_.nome -eq 'quantidade'})[0]
    CheckV9 'decimal_precisao_e_imutabilidade' ($q.tipo -eq 'decimal(19,6)' -and -not $q.nulo -and -not $q.mutavel)
    $json=@($m.campos|Where-Object {$_.nome -eq 'prova_json'})[0];CheckV9 'unicode_max_nullable' ($json.tipo -eq 'nvarchar(max)' -and $json.nulo -and -not $json.mutavel)
    $fk=@($m.campos|Where-Object {$_.nome -eq 'cliente_id'})[0];CheckV9 'FK_obrigatoria' ($fk.relacao -and $fk.optionalFalse -and -not $fk.nulo -and -not $fk.mutavel)
    $id=@($m.campos|Where-Object {$_.nome -eq 'id'})[0];CheckV9 'id_identity_implicita_nao_mutavel' ($id.id -and $id.identity -and -not $id.mutavel)
    CheckV9 'unica_composta_e_nullable_sem_filtro_presumido' ($m.unicas.Count -eq 2 -and ($m.unicas[0].colunas -join ',') -eq 'cliente_id,ref' -and -not $m.unicas[1].filtrada)
    $sit=@($m.campos|Where-Object {$_.nome -eq 'situacao'})[0];CheckV9 'enum_STRING_declarado' ($sit.enumString -and $sit.tipo -eq 'varchar(24)')
    $ordinal=V9ModelTexto ($j.Replace('EnumType.STRING','EnumType.ORDINAL'));CheckV9 'enum_ORDINAL_nao_aprovado_como_STRING' (-not @($ordinal.campos|Where-Object {$_.nome -eq 'situacao'})[0].enumString)
    $base=V9ModelTexto '@MappedSuperclass public abstract class Base { @Column(nullable=false,length=24) private String situacao; }'
    CheckV9 'mapped_superclass_reconhecida' ($base.mappedSuperclass -and $base.campos.Count -eq 1)
    CheckV9 'override_nao_suportado_bloqueia' (BloqueiaV9 {V9ModelTexto '@AttributeOverride(name="x",column=@Column(name="y")) class X {}'})
    CheckV9 'decimal_sem_precisao_bloqueia' (BloqueiaV9 {V9ModelTexto 'class X { @Column private BigDecimal valor; }'})
    CheckV9 'enum_literais_exatos' ((@(V9ValoresEnum 'enum Estado { ATIVO, INATIVO }' 'Estado') -join ',') -eq 'ATIVO,INATIVO')
    CheckV9 'enum_com_construtor_nao_silenciado' (BloqueiaV9 {V9ValoresEnum 'enum Estado { ATIVO(1); }' 'Estado'})
    $s=@'
class S {
 public void run() { salvar("LEITURA_CONTAGEM", Map.of("a",1)); String falso="ops.salvar("; }
 private void salvar(String acao, Object dados) { ops.salvar(id, acao, a, b, r, hash, dados); }
 private void mudar(boolean fim) { String acao = fim ? "INATIVACAO_DEFINITIVA" : "SOLICITACAO_ENCERRAMENTO"; ops.salvar(id, acao, a, b, r, hash, dados); }
}
'@
    $metodos=@(V9Metodos $s);$chamadas=@(V9Chamadas $s '\bops\.salvar')
    CheckV9 'metodos_e_chamadas_nao_pegam_texto_string' ($metodos.Count -eq 3 -and $chamadas.Count -eq 2)
    CheckV9 'repasse_literal_por_helper' ((@(V9ResolverLiteral $s $metodos 'acao' $chamadas[0].indice @{}) -join ',') -eq 'LEITURA_CONTAGEM')
    CheckV9 'ternario_local_duas_alternativas' ((@(V9ResolverLiteral $s $metodos 'acao' $chamadas[1].indice @{}) -join ',') -eq 'INATIVACAO_DEFINITIVA,SOLICITACAO_ENCERRAMENTO')
    $mutado=$s.Replace('ops.salvar(id, acao','acao = receber(); ops.salvar(id, acao');$mm=@(V9Metodos $mutado);$cc=@(V9Chamadas $mutado '\bops\.salvar')
    CheckV9 'reatribuicao_nao_silenciada' (BloqueiaV9 {V9ResolverLiteral $mutado $mm 'acao' $cc[1].indice @{}})
    CheckV9 'parametro_reatribuido_nao_silenciado' (BloqueiaV9 {V9ResolverLiteral $mutado $mm 'acao' $cc[0].indice @{}})
    $genericos=@(V9Metodos 'class G { private void usar(Map<Long, Conteudo> mapa, Set<String> vistos) { usarInterno(); } }')
    CheckV9 'parametros_genericos_com_virgula_interna' ($genericos.Count -eq 1 -and $genericos[0].parametros.Count -eq 2 -and $genericos[0].parametros[0].tipo -eq 'Map')
    $varargs=@(V9Metodos 'class V { private static void conferir(CadastroBase... cadastros) { validar(); } }')
    CheckV9 'parametro_varargs_sem_presumir_enum_singular' ($varargs.Count -eq 1 -and $varargs[0].parametros[0].tipo -ceq 'CadastroBase...')
    $lista=New-Object System.Collections.Generic.List[object];$lista.Add([pscustomobject]@{arquivo='fixture.java'})
    $listaJson=([pscustomobject]@{fontes=$lista.ToArray()}|ConvertTo-Json -Depth 4)|ConvertFrom-Json
    CheckV9 'lista_fontes_serializada_por_ToArray' (@($listaJson.fontes).Count -eq 1 -and $listaJson.fontes[0].arquivo -ceq 'fixture.java')
    $checkTexto='@org.hibernate.annotations.Check(constraints="situacao in (''ATIVO'',''ENCERRAMENTO_PENDENTE'',''INATIVO'')") class T {}'
    CheckV9 'String_Check_dominio_literal_exato' ((@(V9DominioStringCheck $checkTexto 'situacao') -join ',') -ceq 'ATIVO,ENCERRAMENTO_PENDENTE,INATIVO')
    CheckV9 'String_Check_ausente_bloqueia' (BloqueiaV9 {V9DominioStringCheck 'class T {}' 'situacao'})
    CheckV9 'String_Check_expressao_extra_nao_silenciada' (BloqueiaV9 {V9DominioStringCheck ($checkTexto.Replace("'INATIVO')","'INATIVO') OR true")) 'situacao'})
    $dominio=@((@('PENDENTE','PREPARADA','CANCELADA')+@('PREPARADA'))|Sort-Object -Unique)
    CheckV9 'quadro_com_complemento_nao_duplica_literal' ($dominio.Count -eq 3 -and 'PREPARADA' -cin $dominio)
    $enumRepasse='class E { public void gravar(Tipo tipo) { auditoria.registrar(tipo.name(), id, "SOLICITACAO_ENCERRAMENTO", motivo, antes, depois); } }'
    $em=@(V9Metodos $enumRepasse);$ec=@(V9Chamadas $enumRepasse '\bauditoria\.registrar')
    CheckV9 'enum_repasse_finito_explicitado' ((@(V9ResolverLiteral $enumRepasse $em 'tipo.name()' $ec[0].indice @{Tipo=@('CLIENTE','SERVICO_COBRANCA')}) -join ',') -eq 'CLIENTE,SERVICO_COBRANCA')
    CheckV9 'expressao_dinamica_nao_silenciada' (BloqueiaV9 {V9ResolverLiteral $s $metodos 'calcular()' $chamadas[0].indice @{}})
    $hash='A'*64;$mf=V9ManifestoTexto ("D19 bloco4 - freeze BE14 - SHA-256`n"+$hash+'  backend/src/main/java/F.java')
    CheckV9 'manifesto_cabecalho_e_hash_puros' ($mf.mapa.Count -eq 1 -and $mf.cabecalho -like '*BE14*')
    CheckV9 'manifesto_caminho_traversal_bloqueia' (BloqueiaV9 {V9ManifestoTexto ($hash+'  backend/../segredo')})
    CheckV9 'manifesto_duplicado_bloqueia' (BloqueiaV9 {V9ManifestoTexto ($hash+"  backend/src/F.java`n"+$hash+'  backend/src/F.java')})
    CheckV9 'freeze_ausente_bloqueia_antes_leitura' (BloqueiaV9 {V9Freeze 'C:\fixture' ''})
    $falhas=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{capturadoEm=(Get-Date).ToString('o');natureza='Auto-check textual ficticio do parser; nao compara backend/JPA/schema31/V9 real';total=$checks.Count;aprovados=$checks.Count-$falhas.Count;divergencias=$falhas;checks=$checks;backendLido=$false;comparacaoJpaExecutada=$false;semSqlJvmH2BuildRedeAmbienteSegredos=$true}|ConvertTo-Json -Depth 8
    if($falhas.Count){exit 1}
} catch {
    $codigo='BLOQUEADO_FIXTURE_OU_PARSER'
    if($_.Exception.Message -match '^(?:PARSER|BLOQUEADO)_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{bloqueio=$codigo;checksAnteriores=$checks;rawExposto=$false;comparacaoJpaExecutada=$false}|ConvertTo-Json -Depth 6
    exit 1
}
