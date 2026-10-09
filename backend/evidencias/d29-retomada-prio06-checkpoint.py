import datetime
import hashlib
import json
import os
from pathlib import Path

backend = Path(__file__).resolve().parents[1]
repo = backend.parent
evidencias = backend / 'evidencias'
checkpoint = repo / 'orchestracao/.runtime/d29-cedro-retomada-checkpoint.json'


def referencia(path):
    dados = path.read_bytes()
    return {'arquivo': path.relative_to(repo).as_posix(),
            'sha256': hashlib.sha256(dados).hexdigest().upper(), 'bytes': len(dados)}


def ler(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


before = checkpoint.read_bytes()
previous = referencia(checkpoint)
doc = json.loads(before.decode('utf-8-sig'))
if 'complementoPRIO06' not in doc:
    if previous['sha256'] != '6F6B890036E7EA5FC04BBFAD203409BAF40540679656B89811F93D8BD49B077E':
        raise RuntimeError('Checkpoint mudou desde ultimo reconhecimento; reconciliar sem sobrescrever.')
    backup = evidencias / ('d29-retomada-prio06-checkpoint-anterior-' + previous['sha256'] + '.json')
    with backup.open('xb') as output:
        output.write(before)
    previous_preserved = referencia(backup)
else:
    previous_preserved = doc['complementoPRIO06']['checkpointAnteriorPreservado']

receipt_path = evidencias / 'd29-retomada-prio06-validacao-local.json'
receipt = ler(receipt_path)
resources_path = evidencias / 'd29-retomada-prio06-processos-fim.json'
graph_exit = evidencias / 'd29-retomada-prio06-graphify-final.exit'
graph_done = graph_exit.exists() and int(graph_exit.read_text().strip()) == 0
stamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
doc['observadoUtc'] = stamp
doc['estado'] = 'PRIO06_LOCAL14_VERDE_PARA_REVISAO_INDEPENDENTE_DEV_RETIDO'
doc['prontidao']['PRIO06LocalConcluida'] = True
doc['prontidao']['revisaoIndependentePendente'] = True
doc['prontidao']['mapaASTConcluido'] = graph_done
doc['complementoPRIO06'] = {
    'observadoUtc': stamp, 'estado': 'PROVA_LOCAL14_DISPONIVEL_REVISAO_PENDENTE',
    'propostaAntesEdicao': referencia(evidencias / 'd29-retomada-prio06-proposta.md'),
    'evidencia': referencia(receipt_path), 'manifestoAntesBuild': receipt['manifestoAntesBuild'],
    'checkpointAnteriorPreservado': previous_preserved,
    'fonteEditada': 'backend/src/test/java/br/com/rodogarcia/wms/D29FinanceiroRetomadaTest.java',
    'testcases': {'totais': receipt['totais'], 'PRIO06Novos': 9, 'anteriores5ReexecutadosEPreservados': True,
                  'positivos200': 3, 'negativos400DADOS_INVALIDOS': 6},
    'fontesCongeladas': len(receipt['fontesConferidas']), 'divergenciasFonteOuZIP': 0,
    'artefatos5e486Preservados': len(receipt['artefatos5e486Preservados']),
    'JARigual472e486': receipt['JARigual472e486'], 'JavaNegocioAlterado': False,
    'guardasLocais': 'H2 real jdbc:h2:mem:wms-d29-retomada e produtoH2 comprovados em BeforeEach antes fixtures; JWT ficticio/contextos unicos.',
    'casos': receipt['casosPRIO06'], 'oraculoNegativo': receipt['oraculoNegativo'],
    'oraculoPositivo': receipt['oraculoPositivo'], 'vinculoCuradoria': receipt['vinculoCuradoria'],
    'recursosProprios': {'evidencia': referencia(resources_path), **ler(resources_path)},
    'SQLServer': 0, 'APIWMSDEV': 0, 'migrationsAplicadas': 0,
    'redProduto': False, 'limites': receipt['limites'],
    'revisao': {'estado': 'SOLICITAR_A_FAROL_REVISAO_VIGIA_DO_RECORTE14',
                'aceiteIndependente': False, 'reciboGeralSomenteFarol': True},
    'mapa': {'estado': 'AST_CONCLUIDO' if graph_done else 'ATUALIZACAO_EM_CURSO',
             'exitCode': 0 if graph_done else None,
             'log': 'backend/evidencias/d29-retomada-prio06-graphify-final.log',
             'limite': 'ZIP novo evita novas copias Java indexadas; snapshots antigos continuam preservados no mapa. Contagens nao representam testes/cobertura.'},
    'proximo': 'Farol encaminha a Vigia os predicados PRIO06 e provas F33/F34/XML atuais. Curadoria decide equivalencias por parte, sem aceitar pais completos. DEV retido SQL_-2; nenhum fallback ou repeticao de familias.',
    'fechoD29': False}

if not any(row.get('id') == 'D29-L-RET-PRIO-06-LOCAL' for row in doc['lacunasExatasConfirmadas']):
    doc['lacunasExatasConfirmadas'].append({
        'id': 'D29-L-RET-PRIO-06-LOCAL', 'estado': 'PROVA_LOCAL14_ENTREGUE_REVISAO_PENDENTE',
        'faltanteAnterior': 'Configuracao HTTP1/366/MES31, recusas0/367/exclusao/ausencia e snapshot imediato sem efeitos.',
        'provaNova': 'backend/evidencias/d29-retomada-prio06-validacao-local.json',
        'pendenteAtual': 'Revisao independente e curadoria por predicado; nao rotular a falta de prova local como externo. SQLDEV/fiscal/comercial continuam separados.'})
doc['fechoD29'] = False
doc['homologacaoBackendTotal'] = False
temporary = checkpoint.with_name('d29-cedro-retomada-checkpoint.prio06.tmp')
with temporary.open('x', encoding='utf-8') as output:
    json.dump(doc, output, ensure_ascii=False, indent=2)
if checkpoint.read_bytes() != before:
    raise RuntimeError('Checkpoint alterado durante preparo; tmp preservado, nada sobrescrito.')
os.replace(temporary, checkpoint)
print(json.dumps({'checkpoint': referencia(checkpoint), 'estado': doc['estado'],
                  'mapaASTConcluido': graph_done, 'totais': receipt['totais']}))
