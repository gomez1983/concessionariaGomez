"""Baixa imagens selecionadas do Commons e registra créditos. Não altera a API/banco."""
import html
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODELS = [
    ('chevette', 'Chevrolet', 'Chevette', 'Chevrolet Chevette SL front.jpg'),
    ('fiesta', 'Ford', 'Fiesta', "'08-'10 Ford Fiesta Hatchback -- Front.JPG"),
    ('cruze', 'Chevrolet', 'Cruze', 'Chevrolet Cruze 2014 facelift IMG001.jpg'),
    ('tracker', 'Chevrolet', 'Tracker', 'Chevrolet Tracker 1.8 LT 2015 (15187405424).jpg'),
    ('ecosport', 'Ford', 'EcoSport', '2014 Ford EcoSport 2.0 in Mars Red, Front Left, 05-17-2022.jpg'),
    ('f250', 'Ford', 'F-250', '2004 Ford F-250 XLT (4X4) RN extended cab.jpg'),
    ('fusion', 'Ford', 'Fusion', '2018 Ford Fusion Hybrid Titanium.jpg'),
    ('passat', 'Volkswagen', 'Passat', 'Volkswagen Passat (B8) – front view (2017).jpg'),
    ('tiguan', 'Volkswagen', 'Tiguan', '2021 Volkswagen Tiguan R 2.0 Front.jpg'),
    ('polo', 'Volkswagen', 'Polo', '2019 Volkswagen Polo 1.6 MSi Highline.jpg'),
    ('jetta', 'Volkswagen', 'Jetta', '2019 Volkswagen Jetta 1.4L front 3.22.19.jpg'),
    ('pajero-tr4', 'Mitsubishi', 'Pajero TR4', '2004 MMC Pajero TR4.jpg'),
    ('eclipse-cross', 'Mitsubishi', 'Eclipse Cross', '2020 Mitsubishi Eclipse Cross 1.5 SEL in Octane Blue Pearl, front left.jpg'),
    ('asx', 'Mitsubishi', 'ASX', '2018 Mitsubishi ASX (XE) LS wagon (2018-03-28) 01.jpg'),
]
HEADERS = {'User-Agent': 'GomezLocalPrototype/1.0 (reference-image catalog)'}

def get(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=30) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code not in (429, 503) or attempt == 2:
                raise
            time.sleep(min(int(error.headers.get('Retry-After', '10')), 30))

def clean(value):
    return html.unescape(re.sub('<[^>]+>', '', value)).strip()

def main():
    cache = ROOT / 'target/reference-photo-metadata.json'
    if cache.exists():
        response = json.loads(cache.read_text(encoding='utf-8'))
    else:
        params = dict(action='query', format='json', titles='|'.join('File:' + row[3] for row in MODELS),
                      prop='imageinfo', iiprop='url|extmetadata|size', iiurlwidth=1280)
        response = json.loads(get('https://commons.wikimedia.org/w/api.php?' + urllib.parse.urlencode(params)))
        cache.parent.mkdir(parents=True, exist_ok=True)
        cache.write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding='utf-8')
    pages = {page['title']: page for page in response['query']['pages'].values()}
    output = ROOT / 'frontend/public/images/vehicles'
    output.mkdir(parents=True, exist_ok=True)
    manifest = []
    for slug, marca, modelo, filename in MODELS:
        page = pages.get('File:' + filename)
        if not page or not page.get('imageinfo'):
            raise RuntimeError(f'Arquivo não encontrado no Commons: {filename}')
        info = page['imageinfo'][0]
        metadata = info['extmetadata']
        license_name = metadata.get('LicenseShortName', {}).get('value', '')
        if not license_name.startswith(('CC BY', 'Public domain', 'CC0')):
            raise RuntimeError(f'Licença não selecionada para reutilização: {license_name}')
        local = output / f'{slug}.jpg'
        if not local.exists():
            url = info.get('thumburl', info['url']).split('?')[0]
            data = get(url)
            if not data.startswith(b'\xff\xd8'):
                raise RuntimeError(f'O download não retornou um JPEG: {slug}')
            local.write_bytes(data)
            time.sleep(1)
        entry = dict(marca=marca, modelo=modelo, image=f'/images/vehicles/{slug}.jpg',
                     title=filename, author=clean(metadata.get('Artist', {}).get('value', 'Wikimedia Commons')),
                     license=license_name, licenseUrl=metadata.get('LicenseUrl', {}).get('value', ''),
                     sourceUrl=info['descriptionurl'])
        manifest.append(entry)
        print(f'{slug}: {local.stat().st_size} bytes; {license_name}', flush=True)
    target = ROOT / 'frontend/src/data/referencePhotos.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    credits = ['<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Créditos das fotos · Gomez</title>',
               '<style>body{font:16px/1.7 system-ui;max-width:900px;margin:40px auto;padding:0 24px;color:#294435}article{padding:18px 0;border-bottom:1px solid #ddd}a{color:#24684b}img{width:180px;max-height:120px;object-fit:cover;border-radius:8px}</style>',
               '<h1>Fotos de referência do catálogo</h1><p>Imagens ilustrativas dos modelos, obtidas no Wikimedia Commons. Ano, versão e cor podem variar em relação ao veículo cadastrado. Fotos enviadas pelo usuário têm prioridade. Imagens redimensionadas pelo Wikimedia, sem alterações locais; o card aplica apenas um recorte de apresentação em CSS.</p>']
    for entry in manifest:
        e = {key: html.escape(value, quote=True) for key, value in entry.items()}
        license_link = f'<a href="{e["licenseUrl"]}">{e["license"]}</a>' if e['licenseUrl'] else e['license']
        credits.append(f'<article><img src="{e["image"]}" alt="{e["marca"]} {e["modelo"]}"><h2>{e["marca"]} {e["modelo"]}</h2><p>Foto: {e["author"]} · {license_link}<br><a href="{e["sourceUrl"]}">{e["title"]}</a></p></article>')
    credits.append('</html>')
    (ROOT / 'frontend/public/creditos-fotos.html').write_text('\n'.join(credits), encoding='utf-8')

if __name__ == '__main__':
    main()
