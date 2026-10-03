"""Read source inventories again and report changes; never overwrite translation configs.
python tools/source-audit.py [module ...] [--record]
--record is for an explicitly reviewed new baseline; normal use only compares.
"""
import argparse, hashlib, json, pathlib, zipfile, struct
PATCH = pathlib.Path(__file__).resolve().parents[1]
WORKSPACE = PATCH.parent.parent
def sha(data): return hashlib.sha256(data).hexdigest()
def class_strings(data):
    """Read CONSTANT_String literals, including concatenation recipes, without loading code."""
    if data[:4] != b'\xca\xfe\xba\xbe': raise ValueError('Not a Java class')
    count=struct.unpack_from('>H',data,8)[0];offset=10;index=1;utf={};refs=[]
    sizes={3:4,4:4,5:8,6:8,7:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
    while index<count:
        tag=data[offset];offset+=1
        if tag==1:
            length=struct.unpack_from('>H',data,offset)[0];offset+=2
            # Java modified UTF-8 encodes NUL specially and supplementary characters as surrogate pairs.
            raw=data[offset:offset+length].replace(b'\xc0\x80',b'\0')
            utf[index]=raw.decode('utf-8','surrogatepass').encode('utf-16','surrogatepass').decode('utf-16')
            offset+=length
        elif tag==8: refs.append(struct.unpack_from('>H',data,offset)[0]);offset+=2
        elif tag in sizes:offset+=sizes[tag]
        else:raise ValueError(f'Unknown constant-pool tag {tag}')
        index+=2 if tag in (5,6) else 1
    return sorted({utf[ref] for ref in refs})
def read_source(spec):
    path = WORKSPACE / spec['path']
    data = path.read_bytes()
    result = {'sha256': sha(data)}
    if 'classes' in spec:
        with zipfile.ZipFile(path) as jar:
            result['entries'] = {name: sha(jar.read(name+'.class')) if name+'.class' in jar.namelist() else None for name in spec['classes']}
            result['texts'] = {name:class_strings(jar.read(name+'.class')) if name+'.class' in jar.namelist() else [] for name in spec['classes']}
    else:
        obj = json.loads(data.decode('utf-8-sig'))
        entries = {}
        def visit(value, location='$'):
            if isinstance(value, dict):
                for key, child in value.items():
                    if isinstance(child, str) and key in spec.get('fields', ['key','value','name','id']):
                        if not spec.get('prefixes') or any(child.startswith(p) for p in spec['prefixes']): entries[location+'.'+key] = child
                    elif isinstance(child,(dict,list)): visit(child,location+'.'+key)
            elif isinstance(value,list):
                for i,child in enumerate(value): visit(child,f'{location}[{i}]')
        visit(obj)
        result['entries'] = entries
    return result
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('modules',nargs='*');parser.add_argument('--record',action='store_true');args=parser.parse_args()
    registry=json.loads((PATCH/'translations/source-inputs.json').read_text('utf-8'))
    unknown=set(args.modules)-registry.keys()
    if unknown: parser.error('Unknown modules: '+', '.join(sorted(unknown)))
    for module in args.modules or registry:
        current={s['path']:read_source(s) for s in registry[module]}
        baseline=PATCH/'translations/sources'/f'{module}.json'
        if args.record:
            baseline.parent.mkdir(parents=True,exist_ok=True);baseline.write_text(json.dumps(current,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(module+': recorded reviewed baseline');continue
        if not baseline.exists():print(module+': MISSING BASELINE');continue
        old=json.loads(baseline.read_text('utf-8'))
        for path in sorted(old.keys()|current.keys()):
            a,b=old.get(path,{}),current.get(path,{})
            if a==b:continue
            print(f'{module}: SOURCE CHANGED {path}')
            ae,be=a.get('entries',{}),b.get('entries',{})
            for key in sorted(ae.keys()|be.keys()):
                if ae.get(key)!=be.get(key):print(f'  {key}: {ae.get(key)!r} -> {be.get(key)!r}')
            at,bt=a.get('texts',{}),b.get('texts',{})
            for name in sorted(at.keys()|bt.keys()):
                before,after=set(at.get(name,[])),set(bt.get(name,[]))
                for value in sorted(before-after):print(f'  {name}: REMOVED literal {value!r}')
                for value in sorted(after-before):print(f'  {name}: ADDED literal {value!r}')
            if ae==be and at==bt: print('  File bytes changed; extracted text unchanged. Review non-text changes.')
        if old==current:print(module+': unchanged')
if __name__=='__main__': main()
