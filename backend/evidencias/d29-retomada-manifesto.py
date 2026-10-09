import datetime
import hashlib
import json
import shutil
import sys
from pathlib import Path

backend = Path(__file__).resolve().parents[1]
repo = backend.parent
tag = sys.argv[1]
stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
directory = backend / 'evidencias' / ('d29-retomada-fontes-' + tag + '-' + stamp)
directory.mkdir()
paths = sorted((backend / 'src').rglob('*'))
paths += [backend / 'pom.xml', backend / 'mvnw.cmd']
paths += sorted((backend / '.mvn').rglob('*'))
paths += sorted((repo / 'database/migrations').glob('*.sql'))
rows = []
for path in paths:
    if not path.is_file():
        continue
    relative = path.relative_to(repo)
    destination = directory / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, destination)
    original_hash = hashlib.sha256(path.read_bytes()).hexdigest().upper()
    copied_hash = hashlib.sha256(destination.read_bytes()).hexdigest().upper()
    if original_hash != copied_hash:
        raise RuntimeError('Fonte mudou durante copia: ' + str(relative))
    rows.append({'arquivo': relative.as_posix(), 'sha256': original_hash,
                 'arquivoArquivado': destination.relative_to(repo).as_posix(),
                 'bytes': path.stat().st_size})
manifest = {'demanda': 'MESMA_D29', 'agente': 'Cedro_retornada', 'etapa': tag,
            'congeladoAntesBuildUtc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'pasta': directory.relative_to(repo).as_posix(), 'arquivos': rows,
            'limite': 'Snapshot das fontes atuais antes do build local, sem SQL/HTTP DEV/migrations aplicadas. Arquivos originais apenas lidos.'}
target = backend / 'evidencias' / ('d29-retomada-manifesto-' + tag + '.json')
with target.open('x', encoding='utf-8') as out:
    json.dump(manifest, out, ensure_ascii=False, indent=2)
print(json.dumps({'arquivo': target.relative_to(repo).as_posix(), 'fontes': len(rows),
                  'pasta': directory.relative_to(repo).as_posix()}))
