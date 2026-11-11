#!/usr/bin/env python3
"""Read-only recursive ELF page audit. Does not execute or extract payloads."""
import argparse, hashlib, io, json, struct, zipfile
from pathlib import Path, PurePosixPath

def elf(data, name, page=16384, canonical=False):
    errors=[];warnings=[]
    if len(data)<64 or data[:6]!=b'\x7fELF\x02\x01':
        return {'path':name,'errors':['not ELF64 little-endian']}
    kind,machine=struct.unpack_from('<HH',data,16)
    off=struct.unpack_from('<Q',data,32)[0]
    size,count=struct.unpack_from('<HH',data,54)
    if size<56 or count==0 or off+size*count>len(data):
        return {'path':name,'errors':['invalid program-header bounds']}
    loads=[];relros=[]
    for i in range(count):
        ph=struct.unpack_from('<IIQQQQQQ',data,off+i*size)
        typ,flags,p_off,addr,_,filesz,memsz,align=ph
        if typ==0x6474e552:
            relros.append({'vaddr':addr,'memsz':memsz})
            if addr%page or (addr+memsz)%page:
                (errors if canonical else warnings).append('noncanonical RELRO start/end alignment')
        if typ==0x6474e551 and flags&1: errors.append('executable GNU_STACK')
        if typ!=1: continue
        loads.append({'flags':flags,'offset':p_off,'vaddr':addr,'filesz':filesz,'memsz':memsz,'align':align})
        if align<page or align&(align-1): errors.append('LOAD alignment below page or not power of two')
        if p_off%page!=addr%page: errors.append('LOAD offset/vaddr not page congruent')
        if filesz>memsz or p_off+filesz>len(data): errors.append('invalid LOAD bounds')
        if flags&3==3: errors.append('writable executable LOAD')
    if not loads: errors.append('no LOAD segments')
    if not relros: errors.append('missing GNU_RELRO')
    for relro in relros:
        start=relro['vaddr'];end=start+relro['memsz'];lo=start//page*page;hi=(end+page-1)//page*page
        if not any(l['flags']&2 and l['vaddr']<=start and end<=l['vaddr']+l['memsz'] for l in loads): errors.append('RELRO not enclosed by writable LOAD')
        for l in loads:
            left=max(lo,l['vaddr']);right=min(hi,l['vaddr']+l['memsz'])
            if left>=right:continue
            if l['flags']&1:errors.append('rounded RELRO protection overlaps executable LOAD bytes')
            if l['flags']&2 and (left<start or right>end):errors.append('rounded RELRO protection overlaps non-RELRO writable LOAD bytes')
    if machine!=183: errors.append('not AArch64')
    if kind not in (2,3): errors.append('not executable/shared ELF')
    return {'path':name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'machine':machine,'loads':loads,'relro':relros,'warnings':warnings,'errors':sorted(set(errors))}

def inspect(data,name,page=16384,depth=0,canonical=False):
    if data.startswith(b'\x7fELF'): return [elf(data,name,page,canonical)]
    if not zipfile.is_zipfile(io.BytesIO(data)): return []
    if depth>3: raise ValueError('nested ZIP depth exceeded')
    rows=[]
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        if len(z.infolist())>20000: raise ValueError('entry bound exceeded')
        names=set();expanded=0
        for i in z.infolist():
            p=PurePosixPath(i.filename)
            if i.filename in names or p.is_absolute() or '..' in p.parts: raise ValueError('duplicate/unsafe ZIP path')
            names.add(i.filename)
            if (i.external_attr>>16)&0o170000==0o120000: continue # never follow or interpret links
            expanded+=i.file_size
            if expanded>1024**3 or i.file_size>256*1024**2: raise ValueError('expanded size bound exceeded')
            if i.is_dir(): continue
            rows+=inspect(z.read(i),name+'!/'+i.filename,page,depth+1,canonical)
    return rows

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('input');p.add_argument('--page',type=int,default=16384);p.add_argument('--output',required=True);p.add_argument('--canonical',action='store_true')
    a=p.parse_args()
    if a.page<4096 or a.page&(a.page-1): p.error('invalid page')
    data=Path(a.input).read_bytes();rows=inspect(data,Path(a.input).name,a.page,canonical=a.canonical)
    out={'input_sha256':hashlib.sha256(data).hexdigest(),'page':a.page,'elf_count':len(rows),'failed_count':sum(bool(r['errors']) for r in rows),'canonical_policy':a.canonical,'elf':rows,'limits':'ELF layout only, not dynamic-link/import/source/runtime/ZIP alignment qualification'}
    Path(a.output).write_text(json.dumps(out,indent=2)+'\n')
    print(json.dumps({k:v for k,v in out.items() if k!='elf'}))
    raise SystemExit(1 if not rows or out['failed_count'] else 0)
