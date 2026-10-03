"""Read source inventories again and report changes; never overwrite translation configs.
python tools/source-audit.py [module ...] [--record]
--record is for an explicitly reviewed new baseline; normal use only compares.
"""
import argparse, hashlib, json, pathlib, zipfile
PATCH = pathlib.Path(__file__).resolve().parents[1]
WORKSPACE = PATCH.parent.parent
def sha(data): return hashlib.sha256(data).hexdigest()
def read_source(spec):
    path = WORKSPACE / spec['path']
    data = path.read_bytes()
    result = {'sha256': sha(data)}
    if 'classes' in spec:
        with zipfile.ZipFile(path) as jar:
            result['entries'] = {name: sha(jar.read(name+'.class')) if name+'.class' in jar.namelist() else None for name in spec['classes']}
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
            if ae==be: print('  File bytes changed; extracted text unchanged. Review non-text changes.')
        if old==current:print(module+': unchanged')
if __name__=='__main__': main()
