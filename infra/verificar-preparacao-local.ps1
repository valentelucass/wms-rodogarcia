# Diagnostico estatico D19: somente arquivos conhecidos, sem ambiente ou conexoes.
# Nao inicia aplicacao, Maven, Flyway ou outro processo e nao escreve arquivos.
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$raizProjeto = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$utf8Estrito = New-Object System.Text.UTF8Encoding($false, $true)
$conferencias = New-Object 'System.Collections.Generic.List[object]'

function Ler-Arquivo([string] $relativo) {
    [System.IO.File]::ReadAllText((Join-Path $raizProjeto $relativo), $utf8Estrito)
}

function Ler-Propriedades([string] $relativo) {
    $valores = @{}
    foreach ($linha in ((Ler-Arquivo $relativo) -split '\r?\n')) {
        if ($linha -match '^\s*([^#!\s][^=]*)=(.*)$') {
            $valores[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $valores
}

function Conferir([string] $id, [bool] $atendido) {
    $conferencias.Add([pscustomobject][ordered]@{ id = $id; atendido = $atendido })
}

try {
    $aplicacao = Ler-Propriedades 'backend/src/main/resources/application.properties'
    $declarados = [ordered]@{
        'spring.application.name' = 'wms-rodogarcia'
        'spring.profiles.default' = 'local'
        'server.address' = '127.0.0.1'
        'spring.jpa.open-in-view' = 'false'
        'spring.jpa.generate-ddl' = 'false'
        'spring.jpa.hibernate.ddl-auto' = 'validate'
        'spring.sql.init.mode' = 'never'
        'spring.flyway.enabled' = 'false'
        'spring.jpa.properties.hibernate.jdbc.time_zone' = 'UTC'
        'spring.mvc.log-request-details' = 'false'
        'server.error.include-message' = 'never'
        'server.error.include-binding-errors' = 'never'
        'server.error.include-stacktrace' = 'never'
        'server.error.include-exception' = 'false'
        'logging.level.org.hibernate.orm.jdbc.error' = 'OFF'
        'wms.cadastros.enabled' = 'false'
    }
    foreach ($chave in $declarados.Keys) {
        Conferir ("fonte.propriedade.$chave") ($aplicacao[$chave] -ceq $declarados[$chave])
    }
    Conferir 'fonte.local.sem_datasource' ($aplicacao['spring.autoconfigure.exclude'].Contains('DataSourceAutoConfiguration'))

    $sqlDev = Ler-Propriedades 'backend/src/main/resources/application-sqlserver-dev.properties'
    $externas = [ordered]@{
        'wms.database.host' = '${WMS_DB_HOST}'
        'wms.database.port' = '${WMS_DB_PORT:1433}'
        'wms.database.name' = '${WMS_DB_NAME}'
        'wms.database.user' = '${WMS_DB_USER}'
        'wms.database.password' = '${WMS_DB_PASSWORD}'
        'wms.database.confirmed-target' = '${WMS_DB_CONFIRMED_TARGET}'
        'wms.identity.issuer' = '${WMS_OIDC_ISSUER}'
        'wms.identity.jwk-set-uri' = '${WMS_OIDC_JWK_SET_URI}'
        'wms.identity.audience' = '${WMS_OIDC_AUDIENCE}'
    }
    foreach ($chave in $externas.Keys) {
        Conferir ("fonte.externa.$chave") ($sqlDev[$chave] -ceq $externas[$chave])
    }

    $configSql = Ler-Arquivo 'backend/src/main/java/br/com/rodogarcia/wms/config/SqlServerConfig.java'
    Conferir 'fonte.sql.tls_sem_confianca_implicita' ($configSql.Contains(';encrypt=true;trustServerCertificate=false'))
    Conferir 'fonte.sql.guarda_antes_datasource' (
        $configSql.IndexOf('properties.validarAlvo();') -ge 0 -and
        $configSql.IndexOf('properties.validarAlvo();') -lt $configSql.IndexOf('new HikariDataSource(config)') -and
        $configSql.Contains('spring.jpa.hibernate.ddl-auto') -and
        $configSql.Contains('spring.jpa.generate-ddl') -and
        $configSql.Contains('spring.sql.init.mode') -and
        $configSql.Contains('spring.flyway.enabled'))
    $alvo = Ler-Arquivo 'backend/src/main/java/br/com/rodogarcia/wms/config/SqlServerProperties.java'
    Conferir 'fonte.sql.alvo_explicito' ($alvo.Contains('confirmedTarget.equals(host + ":" + port + "/" + name)'))
    Conferir 'fonte.sql.to_string_protegido' ($alvo.Contains('return "SqlServerProperties[configuracao protegida]";'))

    $identidade = Ler-Arquivo 'backend/src/main/java/br/com/rodogarcia/wms/config/IdentidadeProperties.java'
    Conferir 'fonte.jwt.issuer_jwk_https' ([regex]::Matches($identidade, '@Pattern\(regexp = "https://').Count -eq 2)
    $seguranca = Ler-Arquivo 'backend/src/main/java/br/com/rodogarcia/wms/config/CadastrosSegurancaConfig.java'
    Conferir 'fonte.jwt.jwk_issuer_audience' (
        $seguranca.Contains('NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())') -and
        $seguranca.Contains('JwtValidators.createDefaultWithIssuer(properties.issuer())') -and
        $seguranca.Contains('new JwtWmsValidator(properties.audience())'))
    Conferir 'fonte.jwt.sem_sessao_login_basic' (
        $seguranca.Contains('SessionCreationPolicy.STATELESS') -and
        $seguranca.Contains('.formLogin(AbstractHttpConfigurer::disable)') -and
        $seguranca.Contains('.httpBasic(AbstractHttpConfigurer::disable)') -and
        $seguranca.Contains('.requestCache(AbstractHttpConfigurer::disable)'))
    $claims = Ler-Arquivo 'backend/src/main/java/br/com/rodogarcia/wms/config/JwtWmsValidator.java'
    Conferir 'fonte.jwt.perfis_e_alcances' (
        $claims.Contains('Set.of("GESTOR", "SUPERVISOR", "OPERACAO")') -and
        $claims.Contains('"wms_clientes"') -and $claims.Contains('"wms_armazens"') -and
        $claims.Contains('ids.size() > 500') -and $claims.Contains('Long.parseLong(s)'))
    Conferir 'fonte.jwt.prazo_sujeito_audiencia' (
        $claims.Contains('Duration.ofMinutes(15)') -and $claims.Contains('s.length() > 200') -and
        $claims.Contains('jwt.getAudience().contains(audience)') -and
        $claims.Contains('inicio == null') -and $claims.Contains('fim == null'))

    [xml] $pom = Ler-Arquivo 'backend/pom.xml'
    $perfilMigration = @($pom.project.profiles.profile | Where-Object { $_.id -eq 'migrations' })
    Conferir 'fonte.flyway.perfil_unico' ($perfilMigration.Count -eq 1)
    if ($perfilMigration.Count -eq 1) {
        $pluginMigration = @($perfilMigration[0].build.plugins.plugin | Where-Object { $_.artifactId -eq 'flyway-maven-plugin' })
        Conferir 'fonte.flyway.plugin_unico' ($pluginMigration.Count -eq 1)
        if ($pluginMigration.Count -eq 1) {
            $configMigration = $pluginMigration[0].configuration
            Conferir 'fonte.flyway.clean_baseline_nomes' (
                $configMigration.cleanDisabled -ceq 'true' -and
                $configMigration.baselineOnMigrate -ceq 'false' -and
                $configMigration.validateMigrationNaming -ceq 'true')
            Conferir 'fonte.flyway.schema_placeholder_tls' (
                $configMigration.defaultSchema -ceq 'wms' -and
                $configMigration.placeholders.wmsDatabase -ceq '${env.WMS_DB_NAME}' -and
                $configMigration.url.Contains(';encrypt=true;trustServerCertificate=false'))
            Conferir 'fonte.flyway.sem_execucao_vinculada' ($null -eq $pluginMigration[0].SelectSingleNode('*[local-name()="executions"]'))
        }
    }

    $baseline = (Ler-Arquivo 'database/evidencias/d19-v1-v5.sha256') -split '\r?\n' | Where-Object { $_.Trim().Length -gt 0 }
    Conferir 'preservacao.baseline_cinco_scripts' (@($baseline).Count -eq 5)
    $versaoEsperada = 1
    foreach ($linha in $baseline) {
        if ($linha -notmatch '^([0-9A-F]{64})  (V[1-5]__[A-Za-z0-9_]+\.sql)$') {
            Conferir 'preservacao.formato_baseline' $false
            continue
        }
        $hashEsperado = $Matches[1]
        $nomeScript = $Matches[2]
        Conferir ("preservacao.ordem.V$versaoEsperada") ($nomeScript.StartsWith("V${versaoEsperada}__"))
        $versaoEsperada++
        $arquivoScript = Join-Path $raizProjeto ("database/migrations/$nomeScript")
        Conferir ("preservacao.hash.$nomeScript") ((Get-FileHash -LiteralPath $arquivoScript -Algorithm SHA256).Hash -ceq $hashEsperado)
        Conferir ("fonte.guarda.$nomeScript") ((Ler-Arquivo "database/migrations/$nomeScript").Contains("IF DB_NAME() <> N'" + '${wmsDatabase}' + "'"))
    }

    $falhas = @($conferencias | Where-Object { -not $_.atendido })
    [pscustomobject][ordered]@{
        formato = 'wms-diagnostico-estatico-v1'
        natureza = 'Somente arquivos; sem ambiente, rede, SQL, JVM, Maven ou Flyway'
        total = $conferencias.Count
        atendidos = $conferencias.Count - $falhas.Count
        falhas = $falhas.Count
        conferencias = @($conferencias.ToArray())
    } | ConvertTo-Json -Depth 5
    if ($falhas.Count -gt 0) { exit 1 }
}
catch {
    # Nunca devolver caminho, conteudo, valor, stack ou mensagem da excecao.
    '{"formato":"wms-diagnostico-estatico-v1","status":"BLOQUEADO_LEITURA_OU_FORMATO","sem_detalhes_sensiveis":true}'
    exit 1
}
