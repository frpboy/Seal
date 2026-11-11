#!/usr/bin/env python3
"""Static QuickJS target acceptance candidate. Never executes an ELF. Not build admission."""
import argparse, hashlib, json, pathlib, re, struct, subprocess

def elf_layout(data, machine):
    assert len(data) >= 64 and data[:7] == b'\x7fELF\x02\x01\x01', 'ELF64 little-endian required'
    hdr = struct.unpack_from('<16sHHIQQQIHHHHHH', data)
    assert hdr[1] == 3 and hdr[2] == machine, 'PIE ET_DYN and exact ABI required'
    phoff, size, count = hdr[5], hdr[9], hdr[10]
    assert size == 56 and 0 < count <= 128 and phoff + size*count <= len(data)
    loads, relro, interpreter = [], [], []
    for i in range(count):
        t, flags, offset, vaddr, _, filesz, memsz, align = struct.unpack_from('<IIQQQQQQ', data, phoff+i*size)
        assert offset + filesz <= len(data), 'segment outside file'
        if t == 1:
            assert filesz <= memsz and align >= 16384 and align & (align-1) == 0
            assert offset % 16384 == vaddr % 16384, '16KB LOAD congruence'
            assert flags & 3 != 3, 'writable executable LOAD rejected'
            loads.append({'offset':offset,'vaddr':vaddr,'filesz':filesz,'memsz':memsz,'align':align})
        elif t == 3:
            interpreter.append(data[offset:offset+filesz])
        elif t == 0x6474e552:
            relro.append((vaddr, memsz))
        elif t == 0x6474e551:
            assert not flags & 1, 'executable stack rejected'
    assert loads and interpreter == [b'/system/bin/linker64\0'], 'Android PIE interpreter required'
    assert len(relro) == 1 and relro[0][0] % 16384 == 0, '16KB RELRO start alignment required'
    # Strict acceptance candidate: RELRO end must also be page aligned.
    assert (relro[0][0] + relro[0][1]) % 16384 == 0, '16KB RELRO end alignment required'
    return {'loads':loads,'relro':relro}

def symbols(text):
    rows=[]
    for line in text.splitlines():
        m=re.match(r'^\s*\d+:\s+[0-9a-fA-F]+\s+\d+\s+\S+\s+(\S+)\s+\S+\s+(\S+)\s+(\S+)',line)
        if m:rows.append((m[1],m[2],m[3].split('@')[0]))
    return rows

def main():
    p=argparse.ArgumentParser();p.add_argument('--elf',required=True);p.add_argument('--ndk',required=True);p.add_argument('--abi',choices=['arm64-v8a','x86_64'],required=True);p.add_argument('--out',required=True);a=p.parse_args()
    elf=pathlib.Path(a.elf);ndk=pathlib.Path(a.ndk);out=pathlib.Path(a.out)
    assert elf.is_file() and not elf.is_symlink()
    assert 'Pkg.Revision = 28.2.13676358' in (ndk/'source.properties').read_text()
    triple,machine={'arm64-v8a':('aarch64-linux-android',183),'x86_64':('x86_64-linux-android',62)}[a.abi]
    tc=ndk/'toolchains/llvm/prebuilt/linux-x86_64';readelf=tc/'bin/llvm-readelf'
    def read(*args):return subprocess.check_output([str(readelf),*args],text=True,timeout=30)
    layout=elf_layout(elf.read_bytes(),machine)
    dynamic=read('-d','--wide',str(elf))
    needed=re.findall(r'\(NEEDED\).*\[([^\]]+)\]',dynamic)
    assert needed and len(needed)==len(set(needed)) and set(needed)<={'libc.so','libm.so','libdl.so'}, needed
    assert not re.search(r'\((?:RPATH|RUNPATH|TEXTREL)\)',dynamic)
    assert 'BIND_NOW' in dynamic or re.search(r'\(FLAGS_1\).*\bNOW\b',dynamic)
    assert re.search(r'\(FLAGS_1\).*\bPIE\b',dynamic), 'DT_FLAGS_1 PIE required'
    symbol_rows=symbols(read('--dyn-symbols','--wide',str(elf)))
    imports={name for bind,ndx,name in symbol_rows if ndx=='UND' and bind=='GLOBAL'}
    weak_imports={name for bind,ndx,name in symbol_rows if ndx=='UND' and bind=='WEAK'}
    exports=set();stub_hashes={}
    for name in needed:
        stub=tc/'sysroot/usr/lib'/triple/'24'/name
        assert stub.is_file(), 'verified NDK API24 stub missing'
        stub_hashes[name]=hashlib.sha256(stub.read_bytes()).hexdigest()
        exports.update(name for bind,ndx,name in symbols(read('--dyn-symbols','--wide',str(stub))) if ndx!='UND' and bind in ['GLOBAL','WEAK'])
    assert not imports-exports, 'API24 strong imports unavailable: '+str(sorted(imports-exports))
    out.mkdir(parents=True,exist_ok=False)
    (out/'readelf.txt').write_text(read('-h','-l','-d','-V','--dyn-symbols','--wide',str(elf)))
    (out/'static-target-check.json').write_text(json.dumps({'elf_sha256':hashlib.sha256(elf.read_bytes()).hexdigest(),'abi':a.abi,'api':24,'needed':needed,'strong_imports':sorted(imports),'weak_undefined_imports':sorted(weak_imports),'symbol_version_check':'name-only API24 stub availability; version compatibility requires independent ELF review','api24_stub_sha256':stub_hashes,'layout':layout,'runtime_tested':False,'source_security_accepted':False,'public_release_ready':False},indent=2)+'\n')
if __name__=='__main__':main()
