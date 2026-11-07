#!/usr/bin/env python3
"""Check PT_LOAD alignment of every 64-bit .so in final APK/AAB outputs.
ELF alignment only: not runtime page-size, ZIP alignment or Play acceptance.
"""
from pathlib import Path
from zipfile import ZipFile
import json, struct, hashlib
paths = sorted(Path('app/build/outputs/apk/generic/release').glob('*.apk'))
paths += sorted(Path('app/build/outputs/bundle/playRelease').glob('*.aab'))
assert any(p.suffix == '.apk' for p in paths)
assert any(p.suffix == '.aab' for p in paths)
results=[]
for path in paths:
    entries=[]
    packaged_abis=set()
    with ZipFile(path) as z:
        for name in z.namelist():
            parts=name.split('/')
            if not name.endswith('.so') or 'lib' not in parts: continue
            abi=parts[parts.index('lib')+1]
            packaged_abis.add(abi)
            if abi not in ['arm64-v8a','x86_64']: continue
            d=z.read(name)
            assert d[:6] == b'\x7fELF\x02\x01', (path,name,'not little-endian ELF64')
            off=struct.unpack_from('<Q',d,32)[0]
            size,count=struct.unpack_from('<HH',d,54)
            align=[]
            for i in range(count):
                ph=off+i*size
                if struct.unpack_from('<I',d,ph)[0] != 1: continue
                offset,vaddr=struct.unpack_from('<QQ',d,ph+8)
                alignment=struct.unpack_from('<Q',d,ph+48)[0]
                assert alignment>=16384 and offset%16384==vaddr%16384,(path,name,alignment)
                align.append(alignment)
            assert align,(path,name,'no LOAD')
            entries.append({'entry':name,'sha256':hashlib.sha256(d).hexdigest(),'load_alignments':align})
    assert packaged_abis, (path,'missing native libraries')
    if packaged_abis & {'arm64-v8a','x86_64'}: assert entries,(path,'missing ELF64')
    results.append({'artifact':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'packaged_abis':sorted(packaged_abis),'elf64':entries})
Path('native-alignment.json').write_text(json.dumps(results,indent=2)+'\n')
print('All packaged ELF64 LOAD alignments >= 16KiB; no runtime/ZIP alignment claim')
