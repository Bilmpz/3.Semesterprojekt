"""Build 156 train transfers directly from the original dataset. Requires openpyxl."""
from pathlib import Path
from collections import Counter
from openpyxl import load_workbook, Workbook

COMPANIES = [
    ('demo500-autohuset-vestergaard', 'Autohuset Vestergaard (demo)'),
    ('demo500-aunsbjerg', 'Aunsbjerg (demo)'),
    ('demo500-lkj-biler', 'LKJ Biler (demo)'),
    ('demo500-ronald-carlsen', 'Ronald Carlsen (demo)'),
    ('demo500-henrik-christensen', 'Henrik Christensen (demo)'),
]


def build():
    folder = Path(__file__).resolve().parent
    backend = next(p for p in folder.parents if (p / 'pom.xml').is_file())
    original = load_workbook(backend / 'data/syntetisk_voice_agent_dataset_analyseklart_500.xlsx', read_only=True)
    source = iter(original['Model Input'].values)
    columns = {name: i for i, name in enumerate(next(source))}
    result = Workbook()
    sheet = result.active
    sheet.title = 'Model Input'
    sheet.append(['conversation_id', 'dataset_split', 'tools_called', 'synthetic_transcript', 'company_id', 'company_name'])
    selected = []
    for row in source:
        tools = row[columns['tools_called']] or ''
        if row[columns['dataset_split']] != 'train' or 'transfer' not in [t.strip() for t in tools.split(';')]:
            continue
        source_id = row[columns['conversation_id']]
        demo_id = 'samtaler500-' + source_id.removeprefix('SYN-')
        company_id, company_name = COMPANIES[(int(source_id.removeprefix('SYN-')) - 1) % len(COMPANIES)]
        sheet.append([demo_id, 'train', tools, row[columns['synthetic_transcript']], company_id, company_name])
        selected.append(demo_id)
    if len(selected) != 156 or len(set(selected)) != 156:
        raise ValueError('Expected exactly 156 unique train transfers')
    sheet.freeze_panes = 'A2'
    sheet.auto_filter.ref = sheet.dimensions
    result.save(folder / 'samtaler_156_pipeline.xlsx')
    original.close()
    print('Saved 156 original train transfers:', dict(Counter(r[5] for r in list(sheet.values)[1:])))


if __name__ == '__main__':
    build()
