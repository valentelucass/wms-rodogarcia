# Gera documentos somente a partir da auditoria congelada e repositorios atuais.
[CmdletBinding()]
param()
$WmsD20Pathgerard20matrizes=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $WmsD20Pathgerard20matrizes '..'))
$a=Get-Content -LiteralPath (Join-Path $WmsD20Pathgerard20matrizes 'evidencias/d20-auditoria-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
if(@($a.divergenciasJpa).Count){throw 'D20_AUDITORIA_COM_DIVERGENCIAS'}
$perm=@('# D20 — matriz de UPDATE por coluna','',
'Fonte: [auditoria efetiva](../evidencias/d20-auditoria-final.json) e [SQL preparado pelo Hibernate](../../backend/evidencias/d20-hibernate-update-sql.json). Correspondência exata: 34 statements de UPDATE e64 entidades. Hibernate preparou os statements com SQLServerDialect sem executar JDBC; não é SQL observado no servidor nem GRANT validado. Herança, embedded, updatable=false e @Version incluídos.','',
'SELECT/INSERT para as64 tabelas de domínio conforme operações existentes. UPDATE somente nas colunas da tabela abaixo, inclusive inalteradas presentes no statement estático. Nenhum DELETE/DDL/CONTROL/db_owner/sysadmin para aplicação, nenhum GRANT neste pacote. Acesso de diagnóstico ao histórico Flyway deve ser separado, não ampliado por conveniência.','',
'| Tabela | Colunas no UPDATE preparado | Proteção |','| --- | --- | --- |')
$idx=@('# D20 — índices existentes e consultas','',
'Fonte: [schema efetivo](../evidencias/d20-auditoria-final.json), reconstruído após80 ALTERs.83 índices explícitos (5 filtrados),61 UNIQUE e64 PK. Nenhum índice acrescentado/removido. A tabela liga todos os índices explícitos ao repositório da entidade e lista seus métodos concretos; isso localiza consultas, não comprova que o otimizador usa o índice. FKs/direções sem consulta seletiva direta devem ser medidas no SQL Server antes de propor mudança.','',
'[Ligação focal por predicado/ordem/limite de cobertura](d20-consultas.md) detalha as consultas críticas atuais, inclusive filtros ATIVA e ordenação de reservas.','',
'| Índice | Tabela / chaves / filtro | Métodos de consulta existentes | Fonte |','| --- | --- | --- | --- |')
foreach($m in $a.matrizUpdate){
    $cols=if($m.colunasSqlPreparado.Count){'`'+($m.colunasSqlPreparado -join '`, `')+'`'}else{'Nenhuma'}
    $perm+='| '+$m.tabela+' | '+$cols+' | '+$(if($m.colunasSqlPreparado.Count){'Demais colunas sem UPDATE'}else{'Histórico somente INSERT/leitura'})+' |'
    $repo=$m.classe+'Repository.java';$path=Join-Path $raiz ('backend/src/main/java/br/com/rodogarcia/wms/repositories/'+$repo)
    $methods=@()
    if(Test-Path -LiteralPath $path){
        $txt=[IO.File]::ReadAllText($path)
        $methods=@([regex]::Matches($txt,'(?m)^\s*[A-Za-z][^\r\n";{}=]*\s+(?<nome>[a-z]\w*)\s*\(')|ForEach-Object {$_.Groups['nome'].Value}|Sort-Object -Unique)
    }
    $source=if(Test-Path -LiteralPath $path){'['+$repo+'](../../backend/src/main/java/br/com/rodogarcia/wms/repositories/'+$repo+')'}else{'Sem repositório próprio identificado'}
    $queries=if($methods.Count){$methods -join ', '}else{'CRUD herdado; conferir joins/chamadores e plano real'}
    $t=$a.schema.tabelas.PSObject.Properties[$m.tabela].Value
    foreach($i in $t.indices.PSObject.Properties.Value){
        $idx+='| '+$i.nome+' | '+$m.tabela+' (`'+($i.colunas -join ', ')+'`) '+$i.filtro+' | '+$queries+' | '+$source+' |'
    }
}
$perm+=@('','As permissões por coluna não impõem a situação da linha nem alcance por cliente/armazém. Services mantêm permissões do operador, locks, contexto, revisão e atomicidade. Na mudança de contrato ou versão Hibernate, regenerar/confrontar o SQL antes de implantar permissões. Não conceder UPDATE geral para resolver incompatibilidade.','',
'Correções D20 de Cedro: item_chegada e operacao_administrativa sem UPDATE; chegada somente estorno; pedido entrada somente situação/revisão/alteração/efetivação/motivo; nota somente XML/hash/chave; item nota somente valor_mercadoria. Origem/quantidade/data/identidades protegidas. @DynamicUpdate não introduzido.','',
'[Microsoft GRANT](https://learn.microsoft.com/en-us/sql/t-sql/statements/grant-transact-sql?view=sql-server-ver17): permissões herdadas e exceções por coluna devem ser auditadas; DENY na tabela não garante sobrepor GRANT em coluna. [Hibernate](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#pc-managed-state): statement estático pode incluir todas as colunas updatable. Ensaio positivo/negativo e comandos efetivos no SQL Server continuam pendentes.')
$idx+=@('','PKs id bigint identity são acessos por findById/buscarParaAtualizar/joins e não precisam de um segundo índice id. UNIQUEs indexam identidades e idempotência; seu inventário completo está no JSON, inclusive chaves de operação/nota/versão/dia/fato.','',
'A reserva usa buscarCandidatas/disponibilidade por cliente/armazém/produto e revalida sob locks, com FIFO ordenado no serviço. ix_unidade_pedido/produto, ix_pedido_entrada_escopo, ix_ocupacao_unidade e índices/UNIQUEs de contagem/carga apoiam caminhos existentes. Não propor índice de data_fifo por suposição: medição do SQL/plano/cardinalidade deve demonstrar necessidade.','',
'O índice filtrado de reserva impõe exclusividade ATIVA, não a soma nem correspondência ponte/pedido. Nota/documento com chave NULL, regularização sem tratativa e carga sem entrada usam filtros para múltiplos NULL. SETs são necessários em cada sessão. Predicados parametrizados podem exigir outro plano; nenhum desempenho/lock real foi observado.')
$perm|Set-Content -LiteralPath (Join-Path $WmsD20Pathgerard20matrizes 'docs/permissoes-d20.md') -Encoding UTF8
$idx|Set-Content -LiteralPath (Join-Path $WmsD20Pathgerard20matrizes 'docs/indices-d20.md') -Encoding UTF8
