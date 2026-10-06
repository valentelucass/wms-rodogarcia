[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'leitores/d20-schema-efetivo.ps1')
$base='CREATE TABLE wms.a (id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_a PRIMARY KEY, situacao varchar(8) NOT NULL, CONSTRAINT ck_a CHECK (situacao IN (''ATIVO'')));'
$casos=New-Object 'Collections.Generic.List[object]'
function Caso([string]$Nome,[string]$Texto,[string]$Erro='', [scriptblock]$Conferir={}){
    $falha=$null
    try{$s=D20SchemaEfetivo @([pscustomobject]@{arquivo='FICTICIO.sql';texto=$Texto}); & $Conferir $s}catch{$falha=$_.Exception.Message}
    $ok=if($Erro){$falha -like "$Erro*"}else{$null -eq $falha}
    $casos.Add([pscustomobject]@{caso=$Nome;aprovado=$ok;erro=$falha})
}
Caso 'ALTER substitui CHECK anterior' ($base+"ALTER TABLE wms.a DROP CONSTRAINT ck_a; ALTER TABLE wms.a WITH CHECK ADD CONSTRAINT ck_a CHECK(situacao IN ('ATIVO','INATIVO'));") '' {param($s) if($s.tabelas.a.constraints.ck_a.definicao -notmatch 'INATIVO' -or $s.tabelas.a.constraints.Count -ne 2){throw 'nao aplicou ALTER'}}
Caso 'FK alvo correto' ($base+'CREATE TABLE wms.b (id bigint NOT NULL CONSTRAINT pk_b PRIMARY KEY, a_id bigint NOT NULL CONSTRAINT fk_b REFERENCES wms.a(id));')
Caso 'FK tipo incorreto' ($base+'CREATE TABLE wms.b (id bigint NOT NULL CONSTRAINT pk_b PRIMARY KEY, a_id int NOT NULL CONSTRAINT fk_b REFERENCES wms.a(id));') 'D20_FK_TIPO_INVALIDO'
Caso 'FK alvo sem chave' ($base+'CREATE TABLE wms.b (id bigint NOT NULL CONSTRAINT pk_b PRIMARY KEY, a_id varchar(8) NOT NULL CONSTRAINT fk_b REFERENCES wms.a(situacao));') 'D20_FK_ALVO_SEM_CHAVE'
Caso 'DROP desconhecido' ($base+'ALTER TABLE wms.a DROP CONSTRAINT ausente;') 'D20_DROP_INEXISTENTE'
Caso 'coluna duplicada' ($base+'ALTER TABLE wms.a ADD id bigint NULL;') 'D20_COLUNA_DUPLICADA'
Caso 'indice coluna inexistente' ($base+'CREATE INDEX ix_a ON wms.a(ausente);') 'D20_INDICE_COLUNA_AUSENTE'
Caso 'ALTER desconhecido' ($base+'ALTER TABLE wms.a ALTER COLUMN situacao varchar(99);') 'D20_STATEMENT_NAO_SUPORTADO'
Caso 'SQL incompleto' ($base+'ALTER TABLE wms.a ADD CONSTRAINT ck_b CHECK (id>0;') 'D20_SQL_INCOMPLETO'
Caso 'sem PK' 'CREATE TABLE wms.a (id bigint NOT NULL);' 'D20_PK_INCORRETA'
Caso 'literal escapa ; -- parenteses' "CREATE TABLE wms.a (id bigint NOT NULL CONSTRAINT pk_a PRIMARY KEY, s varchar(40) NOT NULL, CONSTRAINT ck_a CHECK (s IN ('a;--()','a''b')));"
Caso 'GO e comentario nao alteram schema' ("-- CREATE TABLE wms.falsa`n"+$base+"`nGO`n/* DROP TABLE wms.a; */") '' {param($s) if($s.tabelas.Count -ne 1){throw 'comentario executado'}}
Caso 'indice filtrado reconhecido' ($base+"CREATE UNIQUE INDEX ux_a ON wms.a(situacao) INCLUDE (id) WHERE situacao='ATIVO';") '' {param($s) if(-not $s.tabelas.a.indices.ux_a.unique -or -not $s.tabelas.a.indices.ux_a.filtro){throw 'filtro perdido'}}
$falhas=@($casos|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='FIXTURES_PARSER_OFFLINE_NAO_SQL_SERVER';total=$casos.Count;aprovados=$casos.Count-$falhas.Count;falhas=$falhas.Count;casos=$casos.ToArray()}|ConvertTo-Json -Depth 6
if($falhas.Count){exit 1}
