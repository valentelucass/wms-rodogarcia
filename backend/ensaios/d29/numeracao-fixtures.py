from pathlib import Path
base=Path(__file__).resolve().parent
for p in base.glob('*.java'):
    s=p.read_text(encoding='utf-8')
    s=s.replace('++j.number','x.nextDocumentNumber()')
    if p.name=='JornadasD29.java':s=s.replace('++number','x.nextDocumentNumber()')
    if p.name=='PendenciasD29.java':s=s.replace('xmlPayload(++number,','xmlPayload(number = x.nextDocumentNumber(),')
    p.write_text(s,encoding='utf-8')
print('D29 numeracao documentos ficticios compartilhada por familia/helper; retomada recupera maior numero capturado')
