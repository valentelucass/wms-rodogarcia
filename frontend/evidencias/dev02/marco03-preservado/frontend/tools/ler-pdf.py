from pathlib import Path
import fitz, json, hashlib

root = Path(__file__).resolve().parents[2]
out = root / 'frontend/evidencias/referencia-pdf'
out.mkdir(parents=True, exist_ok=True)
pdf = root / 'docs/referencias/Especificacao_Funcional_WMS_Rodogarcia.pdf'
doc = fitz.open(pdf)
texts = []
thumbs = []
for i, page in enumerate(doc):
    texts.append(f'\n--- PÁGINA {i+1} ---\n' + page.get_text())
    pix = page.get_pixmap(matrix=fitz.Matrix(1.4, 1.4))
    pix.save(out / f'pagina-{i+1:02}.png')
    thumbs.append(page)
    if i % 4 == 0:
        sheet = fitz.open()
        canvas = sheet.new_page(width=1190, height=1684)
    canvas.show_pdf_page(fitz.Rect((i%2)*595, ((i%4)//2)*842, (i%2+1)*595, ((i%4)//2+1)*842), doc, i)
    if i % 4 == 3 or i == len(doc)-1:
        canvas.get_pixmap(matrix=fitz.Matrix(0.85, 0.85)).save(out / f'contato-{i//4+1:02}.png')
(out/'texto.txt').write_text(''.join(texts), encoding='utf-8')
(out/'inventario.json').write_text(json.dumps({'arquivo':str(pdf), 'sha256':hashlib.sha256(pdf.read_bytes()).hexdigest(), 'paginas':len(doc), 'originaisAlterados':False}, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'{len(doc)} páginas extraídas e renderizadas em {out}')
