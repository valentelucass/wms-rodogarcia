import datetime
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

backend = Path(__file__).resolve().parents[1]
repo = backend.parent
tag = 'build04'
def referencia(path):
    return {'arquivo': path.relative_to(repo).as_posix(),
            'sha256': hashlib.sha256(path.read_bytes()).hexdigest().upper(),
            'bytes': path.stat().st_size}
manifest_path = backend / 'evidencias/d29-retomada-manifesto-build04.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
checks = []
for entry in manifest['arquivos']:
    current = repo / entry['arquivo']
    archived = repo / entry['arquivoArquivado']
    checks.append({'arquivo': entry['arquivo'],
                   'fonteAtualConfere': referencia(current)['sha256'] == entry['sha256'],
                   'bytesArquivadosConferem': referencia(archived)['sha256'] == entry['sha256']})
reports = []
totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
for path in sorted((backend / 'target-d29-retomada/surefire-reports').glob('TEST-*.xml')):
    archive = backend / 'evidencias' / ('d29-retomada-' + tag + '-' + path.name)
    with archive.open('xb') as output:
        output.write(path.read_bytes())
    suite = ET.parse(path).getroot()
    counts = {key: int(suite.get(key, '0')) for key in totals}
    for key, value in counts.items(): totals[key] += value
    reports.append({**referencia(archive), 'classe': suite.get('name'), 'totais': counts,
                    'testcases': [case.get('name') for case in suite.findall('testcase')]})
jar = backend / 'target-d29-retomada/wms-backend-0.0.1-SNAPSHOT.jar'
jar_archive = backend / 'evidencias/d29-retomada-build04-wms-backend.jar'
with jar_archive.open('xb') as output: output.write(jar.read_bytes())
owned = ['backend/src/test/java/br/com/rodogarcia/wms/D29FinanceiroRetomadaTest.java',
         'backend/src/test/java/br/com/rodogarcia/wms/D29XmlFronteirasRetomadaTest.java']
old = json.loads((repo / 'orchestracao/.runtime/d29-build-financeiro-ampliado-conferido-farol.json').read_text(encoding='utf-8'))
previous = []
for entry in old['fontesAtuais']:
    current = referencia(repo / entry['arquivo'])
    previous.append({**current, 'sha256Build472': entry['sha256'],
                     'preservado': current['sha256'] == entry['sha256']})
doc = {'demanda': 'MESMA_D29_CONTINUIDADE_LOCAL', 'agente': 'WMS - Cedro',
       'observadoUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
       'estado': 'CLEAN_VERIFY_FOCAL_CONCLUIDO_PARA_REVISAO',
       'comando': 'mvnw.cmd -B -ntp -Dwms.build.directory=target-d29-retomada -DargLine=-Xmx768m -Dtest=D29FinanceiroRetomadaTest,D29XmlFronteirasRetomadaTest -DspotlessFiles=.*D29.*RetomadaTest[.]java -Dwms.migrations.skip=true -Dwms.bootstrap.skip=true -P!sqlserver-it,!migrations,!bootstrap-local,!migrations-wrapper-confirmado,!bootstrap-local-wrapper-confirmado clean verify',
       'exitCode': int((backend / 'evidencias/d29-retomada-clean-verify-04.exit').read_text()),
       'totais': totals, 'XMLs': reports, 'JAR': referencia(jar_archive),
       'JARmainIgual472': referencia(jar_archive)['sha256'] == old['jar']['sha256'],
       'log': referencia(backend / 'evidencias/d29-retomada-clean-verify-04.log'),
       'manifestoAntesBuild': referencia(manifest_path), 'fontesConferidas': checks,
       'classesNovas': [referencia(repo / path) for path in owned],
       'fontes472Preservadas': previous,
       'nativos': {'SQLServer': 0, 'APIWMSDEV': 0, 'fixturesDEV': 0, 'migrationsAplicadas': 0},
       'casos': [
           {'id': 'D29-VIG-F33', 'testcase': 'f33ReabrirV1AprovadaPreservaMemoriaBytesHashEExigeNovaAprovacaoV2',
            'oraculo': 'V1APROVADA100, hash SHA256 independente e memoria/bytes antes e depois reabertura e apos nova aprovacaoV2; V2PENDENTE_REVISAO/decisorNULL/decididaNULL, entrega409sem efeito antes aprovarV2.',
            'escopo': 'HTTP/JPA/H2 exclusivo jdbc:h2:mem:wms-d29-retomada, ClockMockito2028 apenas neste contexto.'},
           {'id': 'D29-VIG-F34', 'testcase': 'f34RecusaDesconhecidoNaoAlteraVersoesMemoriaAuditoriaOuReplayAntesDeclaracao',
            'oraculo': 'DESCONHECIDO409EMISSAO_DESCONHECIDA antesdeclaracao: mesmosconteudos versoes/fechamento/memoria/hash/auditoria/operacoes/dias/fatos/entregas. DepoisdeclaracaoNAO_EMITIDO_CONFIRMADO aceitaV2PENDENTE_REVISAO semalterarV1.',
            'escopo': 'HTTP/JPA/H2 exclusivo; nenhum provedor/servico externo.'},
           {'id': 'D29-VIG-RET-XML-FRONTEIRAS',
            'oraculo': 'Parser publico recebe NF-e bemformada, profundidades DOM64/65,200/201itens validos e1000000/1000001bytesUTF8 ambos com menosde1000000caracteres; exatos aceitos/+1recusados.',
            'escopo': 'Trespares locais, sem API/SQLDEV e sem autenticar documento fiscal.'}],
       'redsPreservados': [
           {'build': '01', 'classificacao': 'Build inteiro interrompido antes testes por formato de arquivo D29XmlFronteirasTest.java ativo do original; arquivo nao editado por esta retomada.'},
           {'build': '02', 'totais': {'tests': 5, 'failures': 1, 'errors': 1, 'skipped': 0}, 'classificacao': 'Fixture nova Clock.withZone ausente e CNPJ repetido; nao defeito de negocio.'},
           {'build': '03', 'totais': {'tests': 5, 'failures': 1, 'errors': 0, 'skipped': 0}, 'classificacao': 'F33/XML passaram; F34negativo passou mas controle posterior usava literalNAO_EMITIDO divergente de contrato31:27. CorretoNAO_EMITIDO_CONFIRMADO, sem mudar regra/esperado de recusa.'}],
       'limites': ['Cleanverify correspondente a5casos, nao regressao integral nova472+5;472historico preservado.',
                   'Formatacao restritaas2classes novas; nao declarou formatacao global de arquivos ativos de outro dono.',
                   'H2/mocks/Clockisolado nao comprovam cicloDEVaprovado, corte08/10/03Z, SQLServer, fiscal/comercial ou recuperacao.',
                   'Preservacaofinal1895+5tardios, timeout-2/witnessSQL300 e matriz/fecho geral permanecem Farol/Prumo/Vigia.',
                   'Revisao independente deste complemento ainda solicitada ao Farol; nenhum aceite integral.'],
       'parecerIndependenteDisponivel': False, 'fechoD29': False}
target = backend / 'evidencias/d29-retomada-validacao-local.json'
with target.open('x', encoding='utf-8') as output: json.dump(doc, output, ensure_ascii=False, indent=2)
print(json.dumps({'arquivo': target.relative_to(repo).as_posix(), 'sha256': referencia(target)['sha256'],
                  'totais': totals, 'JARmainIgual472': doc['JARmainIgual472'],
                  'fonteAtualDivergencias': sum(not check['fonteAtualConfere'] for check in checks),
                  'arquivoAntesBuildDivergencias': sum(not check['bytesArquivadosConferem'] for check in checks),
                  'fontes472Preservadas': all(check['preservado'] for check in previous)}))
