from pathlib import Path
import xml.etree.ElementTree as ET
import json
src = Path('app/src/main/res/values/strings.xml')
root = ET.parse(src).getroot()
items=[]
for child in root:
    tag=child.tag.rsplit('}',1)[-1]
    name=child.attrib.get('name')
    if tag == 'string':
        if child.attrib.get('{http://schemas.android.com/tools}ignore') == 'true':
            continue
        if child.attrib.get('translatable') == 'false':
            continue
        items.append({'id':name,'kind':'string','quantity':None,'text':child.text or ''})
    elif tag == 'plurals':
        for item in child:
            if item.tag.rsplit('}',1)[-1] == 'item':
                items.append({'id':name,'kind':'plural','quantity':item.attrib['quantity'],'text':item.text or ''})
Path('/tmp/lumi_strings_to_translate.json').write_text(json.dumps(items, ensure_ascii=False, indent=2))
print(f'prepared {len(items)} translatable values')
