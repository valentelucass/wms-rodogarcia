import datetime
import hashlib
import json
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

backend = Path(__file__).resolve().parents[1]
repo = backend.parent
evidencias = backend / 'evidencias'
prefix = 'd29-retomada-prio06-'


def utc():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def referencia(path):
    dados = path.read_bytes()
    return {'arquivo': path.relative_to(repo).as_posix(),
            'sha256': hashlib.sha256(dados).hexdigest().upper(), 'bytes': len(dados)}


def ler(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def salvar(path, dados):
    with path.open('x', encoding='utf-8') as output:
        json.dump(dados, output, ensure_ascii=False, indent=2)


manifest_path = evidencias / (prefix + 'manifesto-build01.json')
preservation_path = evidencias / (prefix + 'preservacao-antes.json')
if sys.argv[1] == 'freeze':
    local5_path = evidencias / 'd29-retomada-validacao-local.json'
    original486_path = repo / 'orchestracao/.runtime/d29-build-fronteiras-conferido-farol.json'
    local5, original486 = ler(local5_path), ler(original486_path)
    refs = {local5_path, original486_path}
    for doc in (local5, original486):
        refs.add(repo / doc['JAR']['arquivo'])
        for report in doc['XMLs']:
            refs.add(repo / report['arquivo'])
            if 'original' in report:
                refs.add(repo / report['original'])
    refs.add(repo / local5['manifestoAntesBuild']['arquivo'])
    salvar(preservation_path, {'observadoUtc': utc(), 'local5Totais': local5['totais'],
                              'original486Totais': original486['totais'],
                              'arquivos': [referencia(p) for p in sorted(refs)]})
    archive = evidencias / (prefix + 'fontes-build01.zip')
    sources = sorted(p for p in (backend / 'src').rglob('*') if p.is_file())
    sources += [backend / 'pom.xml', backend / 'mvnw.cmd']
    sources += sorted(p for p in (backend / '.mvn').rglob('*') if p.is_file())
    sources += sorted((repo / 'database/migrations').glob('*.sql'))
    rows = []
    with zipfile.ZipFile(archive, 'x', compression=zipfile.ZIP_DEFLATED) as frozen:
        for source in sources:
            dados = source.read_bytes()
            relative = source.relative_to(repo).as_posix()
            frozen.writestr(relative, dados)
            rows.append({'arquivo': relative, 'entradaZIP': relative,
                         'sha256': hashlib.sha256(dados).hexdigest().upper(), 'bytes': len(dados)})
    salvar(manifest_path, {'demanda': 'MESMA_D29_PRIO06', 'agente': 'WMS - Cedro',
                         'congeladoAntesBuildUtc': utc(), 'fontesZIP': referencia(archive),
                         'propostaAntesEdicao': referencia(evidencias / (prefix + 'proposta.md')),
                         'preservacaoAntesBuild': referencia(preservation_path),
                         'arquivos': rows,
                         'limite': 'Fontes lidas e arquivadas; migrations somente fontes, nunca executadas. ZIP evita indexar novas copias Java no Graphify.'})
    print(json.dumps({'manifesto': referencia(manifest_path), 'fontes': len(rows),
                      'arquivosPreservados': len(refs)}))
elif sys.argv[1] == 'conferir':
    manifest, preservation = ler(manifest_path), ler(preservation_path)
    checks = []
    with zipfile.ZipFile(repo / manifest['fontesZIP']['arquivo']) as frozen:
        for row in manifest['arquivos']:
            checks.append({'arquivo': row['arquivo'],
                           'fonteAtualConfere': referencia(repo / row['arquivo'])['sha256'] == row['sha256'],
                           'bytesArquivadosConferem': hashlib.sha256(frozen.read(row['entradaZIP'])).hexdigest().upper() == row['sha256']})
    previous_checks = [{**row, 'preservado': referencia(repo / row['arquivo'])['sha256'] == row['sha256']}
                       for row in preservation['arquivos']]
    if any(not c['fonteAtualConfere'] or not c['bytesArquivadosConferem'] for c in checks):
        raise RuntimeError('Divergencia das fontes congeladas: nao publicar aceite de build.')
    if not all(c['preservado'] for c in previous_checks):
        raise RuntimeError('Artefato anterior mudou: reconciliar antes de publicar.')
    reports = []
    totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for path in sorted((backend / 'target-d29-retomada/surefire-reports').glob('TEST-*.xml')):
        archive = evidencias / (prefix + 'build01-' + path.name)
        with archive.open('xb') as output:
            output.write(path.read_bytes())
        suite = ET.parse(path).getroot()
        counts = {key: int(suite.get(key, '0')) for key in totals}
        for key, value in counts.items():
            totals[key] += value
        reports.append({**referencia(archive), 'classe': suite.get('name'), 'totais': counts,
                        'testcases': [case.get('name') for case in suite.findall('testcase')]})
    build_exit = int((evidencias / (prefix + 'clean-verify-01.exit')).read_text())
    if totals != {'tests': 14, 'failures': 0, 'errors': 0, 'skipped': 0} or build_exit != 0:
        raise RuntimeError('Build correspondente nao verde14: classificar red sem enfraquecer oraculos.')
    jar = backend / 'target-d29-retomada/wms-backend-0.0.1-SNAPSHOT.jar'
    jar_archive = evidencias / (prefix + 'build01-wms-backend.jar')
    with jar_archive.open('xb') as output:
        output.write(jar.read_bytes())
    local5 = ler(evidencias / 'd29-retomada-validacao-local.json')
    old_cases = [case for report in local5['XMLs'] for case in report['testcases']]
    new_cases = [case for report in reports for case in report['testcases']]
    if not set(old_cases).issubset(new_cases):
        raise RuntimeError('Testcase anterior ausente do recorte correspondente.')
    if sum(case.startswith('prio06ContratoAceitaLimitesPublicosEPreservaReplay') for case in new_cases) != 3:
        raise RuntimeError('Tres invocacoes positivas PRIO06 nao identificadas.')
    if sum(case.startswith('prio06ContratoRecusaCamposInvalidosSemAlterarSnapshot') for case in new_cases) != 6:
        raise RuntimeError('Seis invocacoes negativas PRIO06 nao identificadas.')
    source_before = ler(evidencias / 'd29-retomada-manifesto-build04.json')
    business_preserved = []
    for row in source_before['arquivos']:
        if row['arquivo'].startswith('backend/src/main/'):
            business_preserved.append({'arquivo': row['arquivo'], 'preservado':
                                       referencia(repo / row['arquivo'])['sha256'] == row['sha256']})
    if not all(row['preservado'] for row in business_preserved):
        raise RuntimeError('Fonte de negocio mudou: reconciliar dono antes de aceite.')
    jar_ref = referencia(jar_archive)
    if jar_ref['sha256'] != local5['JAR']['sha256']:
        raise RuntimeError('JAR main mudou em complemento somente de testes.')
    doc = {'demanda': 'MESMA_D29_PRIO06_LOCAL', 'agente': 'WMS - Cedro', 'observadoUtc': utc(),
           'estado': 'CLEAN_VERIFY14_VERDE_PARA_REVISAO_INDEPENDENTE', 'totais': totals,
           'escopoEditado': 'Somente backend/src/test/java/br/com/rodogarcia/wms/D29FinanceiroRetomadaTest.java; demais gravacoes apenas evidencias proprias/checkpoint e mapa AST obrigatorio.',
           'vinculoCuradoria': {'prioridade': 'D29-L-RET-PRIO-06',
                               'fonte': 'orchestracao/.runtime/d29-lume-retomada-prioridades.md',
                               'contrato': 'docs/29-cadastros-servicos-e-calculo.md:90',
                               'DTO': 'backend/src/main/java/br/com/rodogarcia/wms/dto/ConfiguracaoCobrancaDto.java:174',
                               'servico': 'backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java:434',
                               'partes': 'Configuracao publica duracao1..366, MESdia31, exclusao e ausencia de campo por modalidade.',
                               'limite': 'Nao fecha pais RN09/CT29/V37 nem F31/F32; Vigia decide equivalencias e vinculos por predicado. O complemento local agora existe e nao deve ser rotulado exclusivamente externo.'},
           'exitCode': build_exit, 'XMLs': reports, 'JAR': jar_ref, 'JARigual472e486': True,
           'propostaAntesEdicao': manifest['propostaAntesEdicao'], 'manifestoAntesBuild': referencia(manifest_path),
           'fontesConferidas': checks, 'artefatos5e486Preservados': previous_checks,
           'fontesNegocioPreservadas': business_preserved,
           'formatacao': referencia(evidencias / (prefix + 'formatacao-01.log')),
           'log': referencia(evidencias / (prefix + 'clean-verify-01.log')),
           'comando': 'mvnw.cmd -B -ntp -Dwms.build.directory=target-d29-retomada -DargLine=-Xmx768m -Dtest=D29FinanceiroRetomadaTest,D29XmlFronteirasRetomadaTest -DspotlessFiles=.*D29.*RetomadaTest[.]java -Dwms.migrations.skip=true -Dwms.bootstrap.skip=true -P!sqlserver-it,!migrations,!bootstrap-local,!migrations-wrapper-confirmado,!bootstrap-local-wrapper-confirmado clean verify',
           'casosPRIO06': [
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': None, 'duracaoDias': 1, 'HTTP': 200},
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': None, 'duracaoDias': 366, 'HTTP': 200},
               {'modalidade': 'MES_DIA_FIXO', 'diaCorte': 31, 'duracaoDias': None, 'HTTP': 200},
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': None, 'duracaoDias': 0, 'HTTP': 400, 'campo': 'duracaoDias', 'restricao': 'Min'},
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': None, 'duracaoDias': 367, 'HTTP': 400, 'campo': 'duracaoDias', 'restricao': 'Max'},
               {'modalidade': 'MES_DIA_FIXO', 'diaCorte': 31, 'duracaoDias': 1, 'HTTP': 400, 'restricao': 'Exclusao'},
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': 31, 'duracaoDias': 1, 'HTTP': 400, 'restricao': 'Exclusao'},
               {'modalidade': 'MES_DIA_FIXO', 'diaCorte': None, 'duracaoDias': None, 'HTTP': 400, 'restricao': 'diaCorte obrigatorio'},
               {'modalidade': 'DIAS_CORRIDOS', 'diaCorte': None, 'duracaoDias': None, 'HTTP': 400, 'restricao': 'duracaoDias obrigatoria'}],
           'oraculoNegativo': 'Contrato valido preexistente; seis400 DADOS_INVALIDOS no predicado exigido; snapshot integral contratos/servicos_minimo/operacoes/auditorias imediatamente apos recusa e UUID recusado ausente.',
           'oraculoPositivo': 'Tres200 criam exatamente contrato/operacao/auditoria; modalidade e campos independentes retornados/persistidos/GET; replay200 mesmo resultado sem nova linha ou alteracao de conteudo.',
           'testcasesAnteriores5Preservados': old_cases,
           'testcasesAnteriores5PresentesNoBuild14': True,
           'redProduto': False, 'correcaoJavaNegocio': False,
           'SQLServer': 0, 'APIWMSDEV': 0, 'fixturesDEV': 0, 'revisaoIndependentePendente': True,
           'limites': ['H2/HTTP local exclusivo, sem prova de SQLServer/DEV/corte03Z/comercial/fiscal.',
                       'Build correspondente14, nao repeticao da suite486 original nem fecho D29.',
                       'PRIO06 limites/exclusao/ausencia agora possuem prova local; F31/F32 e curadoria geral continuam separados.'],
           'fechoD29': False}
    target = evidencias / (prefix + 'validacao-local.json')
    salvar(target, doc)
    print(json.dumps({'recibo': referencia(target), 'totais': totals, 'fontes': len(checks),
                      'divergenciasFontes': 0, 'artefatosAnterioresPreservados': len(previous_checks),
                      'JARigual472e486': True}))
else:
    raise RuntimeError('Etapa desconhecida')
