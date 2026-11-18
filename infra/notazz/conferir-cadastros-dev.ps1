[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidencePath)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$out=[IO.Path]::GetFullPath($EvidencePath)
if(!$out.StartsWith((Join-Path $root 'orchestracao\.runtime\notazz-lifecycle-20261023\'),[StringComparison]::OrdinalIgnoreCase) -or (Test-Path -LiteralPath $out)){throw 'NFH_EVIDENCIA_INVALIDA'}
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
$guard=Invoke-WmsDev02Guard
if(!$guard.guardaRealAprovada -or @($guard.checks|Where-Object {!$_.passou}).Count){throw 'NFH_GUARDA_RECUSADA'}
$credential=$null;$connection=$null
$report=[ordered]@{id='NOTAZZ-OSASCO-CADASTROS';inicio=[DateTime]::UtcNow.ToString('o');state='PREPARO';database='WMS_DEV';login='WMSDEV';dml=0;ddl=0;notazzCalls=0;erro=$null;checksGuarda=@($guard.checks).Count}
function Read-Fiscal([string]$Sql){
 if($Sql.TrimStart() -cnotmatch '^SELECT\b'){throw 'NFH_SELECT_EXIGIDO'}
 $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
 $rows=[Collections.Generic.List[object]]::new();$reader=$null
 try{$reader=$cmd.ExecuteReader();while($reader.Read()){$row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$row)};$rows.ToArray()}finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
try{
 $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
 & $module {Import-SqlClient}
 $credential=Get-WmsDevApplicationCredential
 $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
 $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV');$builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($guard.perfilProtegido.certificate);$builder.set_Pooling($false);$builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0);$builder.set_ApplicationName('WMS-NOTAZZ-OSASCO-CADASTROS-SELECT')
 $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$credential.Password)
 $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential);$connection.Open()
 $identity=@(Read-Fiscal 'SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,USER_NAME() usuario')
 if($identity.Count -ne 1 -or $identity[0].banco -cne 'WMS_DEV' -or $identity[0].login -cne 'WMSDEV' -or $identity[0].usuario -cne 'WMSDEV'){throw 'NFH_IDENTIDADE_INVALIDA'}
 $report.armazens=@(Read-Fiscal "SELECT id,codigo,nome,documento_fiscal,situacao,cidade,uf,CASE WHEN NULLIF(razao_social,'') IS NOT NULL AND NULLIF(logradouro,'') IS NOT NULL AND NULLIF(numero_endereco,'') IS NOT NULL AND NULLIF(bairro,'') IS NOT NULL AND NULLIF(cep,'') IS NOT NULL AND NULLIF(codigo_municipio,'') IS NOT NULL THEN 1 ELSE 0 END cadastroBasicoCompleto,CASE WHEN NULLIF(inscricao_estadual,'') IS NOT NULL THEN 1 ELSE 0 END inscricaoEstadualInformada FROM wms.armazem WHERE REPLACE(REPLACE(REPLACE(documento_fiscal,'.',''),'/',''),'-','')='04547874000114'")
 $report.pedidos=@(Read-Fiscal "SELECT p.id,p.referencia,p.situacao,p.cliente_id,p.armazem_id,c.codigo clienteCodigo,CASE WHEN NULLIF(c.razao_social,'') IS NOT NULL AND NULLIF(c.logradouro,'') IS NOT NULL AND NULLIF(c.numero_endereco,'') IS NOT NULL AND NULLIF(c.bairro,'') IS NOT NULL AND NULLIF(c.cep,'') IS NOT NULL AND NULLIF(c.codigo_municipio,'') IS NOT NULL AND c.indicador_ie IN(1,2,9) AND c.agrupamento_nfe IN('CONSOLIDAR','POR_NOTA') THEN 1 ELSE 0 END cadastroClienteCompleto,(SELECT COUNT(*) FROM wms.item_pedido_saida i WHERE i.pedido_id=p.id) produtos,(SELECT COUNT(*) FROM wms.item_pedido_saida i JOIN wms.referencia_fiscal_produto f ON f.produto_id=i.produto_id AND f.armazem_id=p.armazem_id AND f.operacao='RETORNO_MERCADORIA' WHERE i.pedido_id=p.id AND f.ncm IS NOT NULL AND f.cfop IS NOT NULL AND f.conferida_em IS NOT NULL AND ISJSON(f.tributacao_nfe)=1) produtosComReferenciaFiscal FROM wms.pedido_saida p JOIN wms.armazem a ON a.id=p.armazem_id JOIN wms.cliente c ON c.id=p.cliente_id WHERE REPLACE(REPLACE(REPLACE(a.documento_fiscal,'.',''),'/',''),'-','')='04547874000114' ORDER BY p.id")
 $report.origens=@(Read-Fiscal "SELECT n.id,n.pedido_id,n.serie,n.numero,n.emissao,CASE WHEN LEN(n.chave_acesso)=44 THEN 1 ELSE 0 END chaveInformada,CASE WHEN n.xml_original IS NOT NULL THEN 1 ELSE 0 END xmlDisponivel,(SELECT COUNT(*) FROM wms.item_nota_entrada i WHERE i.nota_id=n.id) itens,(SELECT COUNT(*) FROM wms.item_nota_entrada i WHERE i.nota_id=n.id AND i.valor_mercadoria IS NULL) itensSemValor FROM wms.nota_entrada n JOIN wms.pedido_entrada p ON p.id=n.pedido_id JOIN wms.armazem a ON a.id=p.armazem_id WHERE REPLACE(REPLACE(REPLACE(a.documento_fiscal,'.',''),'/',''),'-','')='04547874000114' ORDER BY n.id")
 $xmlRows=@(Read-Fiscal "SELECT n.id,n.xml_original FROM wms.nota_entrada n JOIN wms.pedido_entrada p ON p.id=n.pedido_id JOIN wms.armazem a ON a.id=p.armazem_id WHERE REPLACE(REPLACE(REPLACE(a.documento_fiscal,'.',''),'/',''),'-','')='04547874000114' AND n.xml_original IS NOT NULL ORDER BY n.id")
 $report.dadosDosXmls=@(foreach($record in $xmlRows){
  $settings=[Xml.XmlReaderSettings]::new();$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit;$settings.XmlResolver=$null;$settings.MaxCharactersInDocument=1000000
  $stringReader=[IO.StringReader]::new($record.xml_original);$xmlReader=[Xml.XmlReader]::Create($stringReader,$settings)
  try{
   $doc=[Xml.XmlDocument]::new();$doc.XmlResolver=$null;$doc.Load($xmlReader)
   $info=$doc.SelectSingleNode('//*[local-name()="infNFe"]')
   if(!$info){[pscustomobject]@{notaId=$record.id;formato=$doc.DocumentElement.LocalName;infNFeDisponivel=$false};continue}
   $parties=[ordered]@{}
   foreach($part in @('emit','dest')){
    $party=$info.SelectSingleNode('*[local-name()="'+$part+'"]');$fields=[ordered]@{}
    foreach($field in @('CNPJ','CPF','xNome','IE','CRT','indIEDest')){$node=$party.SelectSingleNode('*[local-name()="'+$field+'"]');if($node){$fields[$field]=$node.InnerText}}
    $address=$party.SelectSingleNode('*[local-name()="enderEmit" or local-name()="enderDest"]')
    if($address){foreach($field in @('xLgr','nro','xCpl','xBairro','cMun','xMun','UF','CEP','cPais')){$node=$address.SelectSingleNode('*[local-name()="'+$field+'"]');if($node){$fields[$field]=$node.InnerText}}}
    $parties[$part]=$fields
   }
   $taxes=@(foreach($item in $info.SelectNodes('*[local-name()="det"]')){
    $fields=[ordered]@{item=$item.nItem}
    foreach($field in @('cProd','NCM','CFOP','uCom','qCom','vUnCom','vProd')){$node=$item.SelectSingleNode('*[local-name()="prod"]/*[local-name()="'+$field+'"]');if($node){$fields[$field]=$node.InnerText}}
    foreach($name in @('ICMS','IPI','PIS','COFINS','IBSCBS')){
     $tax=$item.SelectSingleNode('*[local-name()="imposto"]/*[local-name()="'+$name+'"]');if($tax){$fields[$name]=$tax.InnerXml}
    }
    [pscustomobject]$fields
   })
   [pscustomobject]@{notaId=$record.id;formato=$doc.DocumentElement.LocalName;infNFeDisponivel=$true;partes=$parties;itens=$taxes;protocoloPresente=($null -ne $doc.SelectSingleNode('//*[local-name()="protNFe"]'));assinaturaPresente=($null -ne $doc.SelectSingleNode('//*[local-name()="Signature"]'))}
  }finally{$xmlReader.Dispose();$stringReader.Dispose()}
 })
 $xmlRows=$null
 $report.state='PASS_READ_ONLY';$report.armazensEncontrados=$report.armazens.Count;$report.pedidosEncontrados=$report.pedidos.Count
}catch{
 $report.state='FAIL';$ex=$_.Exception;$code='NFH_LEITURA_FALHOU';$number=$null
 while($ex){if($ex.Message -cmatch '^NFH_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
 $report.erro=@{codigo=$code;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber}
}finally{
 if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()}
 $report.fim=[DateTime]::UtcNow.ToString('o')
 [IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
 [pscustomobject]@{state=$report.state;armazens=$report.armazens;pedidos=$report.pedidos;origens=$report.origens;dadosDosXmls=$report.dadosDosXmls;erro=$report.erro;evidence=$out}|ConvertTo-Json -Depth 10
}
if($report.state -ceq 'FAIL'){exit 20}
