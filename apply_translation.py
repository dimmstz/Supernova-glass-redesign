from pathlib import Path
import xml.etree.ElementTree as ET
import json, re
src=Path('app/src/main/res/values/strings.xml')
out=Path('app/src/main/res/values-pt-rBR/strings.xml')
translations={int(k):v for k,v in json.loads(Path('/tmp/lumi_ptbr_translations.json').read_text()).items()}
root=ET.parse(src).getroot()
idx=0
for child in root:
    tag=child.tag.rsplit('}',1)[-1]
    if tag=='string' and child.attrib.get('translatable')!='false':
        child.text=translations[idx]; idx+=1
    elif tag=='plurals':
        for item in child:
            if item.tag.rsplit('}',1)[-1]=='item':
                item.text=translations[idx]; idx+=1
out.parent.mkdir(parents=True, exist_ok=True)
ET.indent(root, space='    ')
ET.ElementTree(root).write(out, encoding='utf-8', xml_declaration=True)
assert idx==len(translations), (idx,len(translations))

def placeholders(s):
    return sorted(re.findall(r'%(?:\d+\$)?[dsf]', s or ''))
orig=ET.parse(src).getroot(); trans=ET.parse(out).getroot()
def values(r):
    vals=[]
    for c in r:
        tag=c.tag.rsplit('}',1)[-1]
        if tag=='string': vals.append((c.attrib.get('name'),None,c.text or ''))
        elif tag=='plurals':
            for i in c:
                if i.tag.rsplit('}',1)[-1]=='item': vals.append((c.attrib.get('name'),i.attrib['quantity'],i.text or ''))
    return vals
ov,tv=values(orig),values(trans)
assert [(a,b) for a,b,_ in ov]==[(a,b) for a,b,_ in tv]
errors=[]
for (name,q,os),(name2,q2,ts) in zip(ov,tv):
    if placeholders(os)!=placeholders(ts): errors.append((name,q,placeholders(os),placeholders(ts),os,ts))
assert not errors, errors[:5]
print(f'created {out} with {idx} translated values; placeholders PASS; entries PASS')
