# Leitura PUBLICA minima. Nao importa credencial, nao chama guard/profile/SQL/rede.
$ErrorActionPreference='Stop'
$publicGuardPath=Join-Path $PSScriptRoot 'frontend-prumo-dev02-guarda-lucas-20261008.json'
$publicGuard=Get-Content -LiteralPath $publicGuardPath -Encoding UTF8 -Raw|ConvertFrom-Json
$publicReceiptPath=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer/tls.json'
$publicReceipt=Get-Content -LiteralPath $publicReceiptPath -Encoding UTF8 -Raw|ConvertFrom-Json
$publicCert=New-Object Security.Cryptography.X509Certificates.X509Certificate2($publicReceipt.certificate)
$publicSha=[Security.Cryptography.SHA256]::Create()
try {
    $derSha256=([BitConverter]::ToString($publicSha.ComputeHash($publicCert.GetRawCertData()))).Replace('-','').ToLowerInvariant()
    $publicCertMetadata=[ordered]@{
        uso='Arquivo PEM local passado ao SqlConnectionStringBuilder.ServerCertificate, nao certificado peer observado nesta tentativa'
        arquivoPEMSha256=(Get-FileHash -LiteralPath $publicReceipt.certificate -Algorithm SHA256).Hash
        certificadoDERSha256=$derSha256;DERCorrespondeReciboProtegido=($derSha256 -ceq $publicReceipt.certificateSHA256)
        thumbprintSHA1=$publicCert.Thumbprint;subject=$publicCert.Subject;issuer=$publicCert.Issuer
        notBefore=$publicCert.NotBefore.ToUniversalTime().ToString('o');notAfter=$publicCert.NotAfter.ToUniversalTime().ToString('o')
        extensoesOID=@($publicCert.Extensions|ForEach-Object {$_.Oid.Value})
        origemReciboTLS=[ordered]@{updatedAt=$publicReceipt.updatedAt;PIDRegistradoHistorico=$publicReceipt.pid;startedRegistradoHistorico=$publicReceipt.started;certificateHost=$publicReceipt.certificateHost;PIDAtualVerificado=$false}
    }
} finally {$publicCert.Dispose();$publicSha.Dispose()}
$publicAnalysis=[ordered]@{
    demanda='D31-DEV02';agente='WMS - Prumo';observadoEm=[DateTime]::UtcNow.ToString('o')
    guardaAtual=[ordered]@{arquivo='frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json';sha256=(Get-FileHash -LiteralPath $publicGuardPath -Algorithm SHA256).Hash;tentativaEm=$publicGuard.aberturaInicioUtc;guardaRealAprovada=$false;SQLSELECTs=0}
    codigoTLS=[ordered]@{decimal=-2146762480;hex='0x800B0110';fonteInterpretacao='ComponentModel.Win32Exception / mensagem do Windows instalado';descricao=(New-Object ComponentModel.Win32Exception(-2146762480)).Message;fase='ABERTURA';SqlClientVersion=$publicGuard.cliente.assembly;erroAtualDeTimeout=$false}
    clienteConfigurado=[ordered]@{Encrypt='Mandatory';TrustServerCertificate=$false;ServerCertificate='arquivoPEM local existente';retry=0;pooling=$false;timeoutSegundos=5}
    certificadoConfiguradoCliente=$publicCertMetadata
    certificadoServidorPeerAtual=[ordered]@{naoCapturado=$true;SHA256=$null;correspondenciaComArquivoClienteNaoComprovada=$true;handshakeConcluido=$false;identidadeSQLRealConfirmada=$false}
    provado=@('A tentativa unica atual recusou validacao TLS com codigo0x800B0110 antes de qualquer SELECT.','O arquivo PEM existente e o certificado DER foram identificados localmente; arquivo/ACL/validade/recibo protegidos passaram antes da Open.','O helper configurou criptografia obrigatoria, TrustServerCertificatefalse e ServerCertificate com aquele arquivo; nenhuma configuracao global foi mudada.')
    naoProvado=@('Que o servidor peer atual tenha apresentado o mesmo certificado do arquivo cliente/reciboTLS de06outubro.','Qual certificado/cadeia/politica interna levou a recusa de uso; excecao sanitizada nao identifica esse elemento.','Que ausencia de extensoes EKU/SAN no arquivo local seja a causa; ausencia isolada nao basta para esse diagnostico.','Que o incidente timeout D29 esteja resolvido ou que seja a causa desta falha TLS distinta.','Banco/identidade/permissoes/schema/historico reais nesta tentativa.')
    decisaoMaterial='Encaminhar ao responsavel TLS o codigo atual e a proveniencia do certificado cliente. Se a solucao exigir mudar certificado/trust/servidor/acessos/sharedruntime, requer decisao material especifica; Prumo nao executou. Nenhuma correcao cliente concreta foi comprovada que permita liberar sem nova prova.'
    limites=[ordered]@{novaOpen=0;recapturaCertificado=0;rede=0;importCredencial=0;profileRuntime=0;runtimeUpdate=0;globalTrust=0;bypass=0;servidorAcessoAlterados=0}
    contratoHash='Campo tls.certificateSha256 da guarda identifica SHA256 dos bytes do arquivo PEM; certificado DER tem SHA256 separado nesta analise. Nao e SHA peer atual.'
}
$publicOutputPath=Join-Path $PSScriptRoot 'prumo-dev02-tls-publico-lucas-20261008.json'
if(Test-Path -LiteralPath $publicOutputPath){throw 'DEV02_ANALISE_PUBLICA_EXISTENTE_PRESERVADA'}
[IO.File]::WriteAllText($publicOutputPath,($publicAnalysis|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$publicOutputSha=(Get-FileHash -LiteralPath $publicOutputPath -Algorithm SHA256).Hash
$publicMdPath=Join-Path $PSScriptRoot 'frontend-prumo-dev02-guarda-lucas-20261008.md'
[IO.File]::Copy($publicMdPath,(Join-Path $PSScriptRoot 'prumo-dev02-guarda-md-antes-analise-publica.md'),$false)
$publicAppend=@"


## Complemento publico solicitado por Farol

O codigo atual 0x800B0110 foi interpretado pela mensagem nativa do Windows: certificado invalido para o uso solicitado. A origem exata da recusa nao foi provada. Nao inferir que ausencia de EKU/SAN seja a causa so porque o arquivo existente nao tem extensoes.

**Cliente e peer sao evidencias diferentes.** O campo tls.certificateSha256 no JSON identifica SHA256 dos bytes do PEM cliente passado a ServerCertificate: $($publicCertMetadata.arquivoPEMSha256). SHA256 do certificado DER local: $($publicCertMetadata.certificadoDERSha256), correspondente ao recibo protegido. Nenhum deles foi observado como certificado do peer nesta tentativa; handshakeConcluido=false. EncryptMandatory e TrustServerCertificatefalse sao configuracao comprovada, nao prova de handshake aprovado.

O recibo publico TLS tem updatedAt=$($publicReceipt.updatedAt), PID historico=$($publicReceipt.pid), started historico=$($publicReceipt.started). Nao foi verificado PID atual, nem recapturado certificado. Subject e issuer do arquivo local CN=SSL_Self_Signed_Fallback; validade vigente e nenhuma extensao identificada. Isso descreve o arquivo cliente, sem certificar origem atual do servidor.

Nao foi comprovado ajuste concreto do cliente que elimine a falha. Encaminhamento: responsavel TLS analisar recusa/proveniencia. Se a solucao exigir certificado/trust/servidor/acessos/sharedruntime, falta decisao material especifica; nenhum bypass ou globaltrust foi aplicado. STOP preservado, sem nova sonda. Analise publica: prumo-dev02-tls-publico-lucas-20261008.json, SHA $publicOutputSha. MD anterior preservado em prumo-dev02-guarda-md-antes-analise-publica.md.
"@
[IO.File]::AppendAllText($publicMdPath,$publicAppend,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{analisePublicaSha256=$publicOutputSha;mdFinalSha256=(Get-FileHash -LiteralPath $publicMdPath -Algorithm SHA256).Hash;novasOpen=0;rede=0;arquivoPEMSha256=$publicCertMetadata.arquivoPEMSha256;certificadoDERSha256=$derSha256}|ConvertTo-Json -Compress
