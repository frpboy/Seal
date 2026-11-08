#!/usr/bin/env python3
"""Checksum-bound local AAR preparation. No Gradle adoption or publication."""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import hashlib
import copy
import importlib.util
import json
import subprocess
import re
import sys

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('alignment', HERE / 'check-native-alignment.py')
alignment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(alignment)
NAMES = ['libsharpyuv.so', 'libwebp.so', 'libwebpdecoder.so', 'libwebpdemux.so', 'libwebpmux.so']
PIN = '0a87ffa6cf912b0fe76c1a99b9107f543ee2f247935fae2c71f0822eb7bc5f49'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def symbols(tool, path):
    text = subprocess.check_output([str(tool), '--dyn-syms', '--wide', str(path)], text=True)
    return sorted({v[-1] for line in text.splitlines()
                   if len(v := line.split()) >= 8 and v[4] in ['GLOBAL', 'WEAK'] and v[6] != 'UND'})


def dynamic(tool, path):
    text = subprocess.check_output([str(tool), '-d', str(path)], text=True)
    return [line.strip() for line in text.splitlines() if 'NEEDED' in line or 'SONAME' in line]


def prepare(work):
    original = work / 'original.aar'
    if digest(original.read_bytes()) != PIN:
        raise ValueError('Original AAR hash mismatch')
    tool = work / 'android-ndk-r28c/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf'
    evidence = {'original_aar_sha256': PIN, 'changes': [], 'payload_checks': []}
    patched = {}
    with ZipFile(original) as aar:
        for abi in ['arm64-v8a', 'x86_64']:
            entry = f'jni/{abi}/libffmpeg.zip.so'
            output = BytesIO()
            with ZipFile(BytesIO(aar.read(entry))) as source, ZipFile(output, 'w') as dest:
                replacements = {f'usr/lib/{name}' for name in NAMES}
                if not replacements <= set(source.namelist()):
                    raise ValueError('Missing expected replacement entry')
                for info in source.infolist():
                    old = source.read(info)
                    new = old
                    if info.filename in replacements:
                        name = Path(info.filename).name
                        built = work / f'build-{abi}' / name
                        new = built.read_bytes()
                        if not alignment.inspect_elf(new, name)['passes']:
                            raise ValueError('Rebuilt ELF alignment failure')
                        old_path = work / 'audit-original' / abi / name
                        old_path.parent.mkdir(parents=True, exist_ok=True)
                        old_path.write_bytes(old)
                        old_symbols, new_symbols = symbols(tool, old_path), symbols(tool, built)
                        if old_symbols != new_symbols:
                            raise ValueError(f'{abi}/{name}: symbol/version surface differs')
                        old_dyn, new_dyn = dynamic(tool, old_path), dynamic(tool, built)
                        old_soname = re.findall(r'Library soname: \[([^]]+)\]', '\n'.join(old_dyn))
                        new_soname = re.findall(r'Library soname: \[([^]]+)\]', '\n'.join(new_dyn))
                        if old_soname != [name] or new_soname != old_soname:
                            raise ValueError('SONAME changed')
                        needed = re.findall(r'Shared library: \[([^]]+)\]', '\n'.join(new_dyn))
                        if not set(needed) <= set(NAMES + ['libc.so', 'libm.so', 'libdl.so']):
                            raise ValueError('Unexpected rebuilt dependency')
                        old_header = subprocess.check_output([str(tool), '-h', str(old_path)], text=True)
                        new_header = subprocess.check_output([str(tool), '-h', str(built)], text=True)
                        machine = 'AArch64' if abi == 'arm64-v8a' else 'Advanced Micro Devices X86-64'
                        if machine not in old_header or machine not in new_header:
                            raise ValueError('Architecture mismatch')
                        evidence['changes'].append({'abi': abi, 'entry': info.filename,
                            'old_sha256': digest(old), 'new_sha256': digest(new),
                            'exports_equal': True, 'export_count': len(old_symbols),
                            'original_dynamic': old_dyn, 'rebuilt_dynamic': new_dyn})
                    dest.writestr(copy.copy(info), new)
            candidate = output.getvalue()
            checks = alignment.inspect_payload(candidate, entry)
            if any(not item['passes'] for item in checks):
                raise ValueError('Patched payload has unaligned ELF')
            with ZipFile(BytesIO(candidate)) as result, ZipFile(BytesIO(aar.read(entry))) as source:
                if result.namelist() != source.namelist():
                    raise ValueError('Payload inventory changed')
                for name in source.namelist():
                    if name not in replacements and result.read(name) != source.read(name):
                        raise ValueError('Unrelated nested payload bytes changed')
            evidence['payload_checks'].append({'entry': entry, 'elf64_count': len(checks),
                'sha256': digest(candidate), 'all_load_constraints_pass': True})
            patched[entry] = candidate
        out = work / 'ffmpeg-0.18.1-seali-webp16k-candidate.aar'
        with ZipFile(out, 'w') as dest:
            for info in aar.infolist():
                dest.writestr(copy.copy(info), patched.get(info.filename, aar.read(info)))
        with ZipFile(out) as result:
            if result.namelist() != aar.namelist():
                raise ValueError('AAR inventory changed')
            for name in aar.namelist():
                if name not in patched and result.read(name) != aar.read(name):
                    raise ValueError('Unrelated AAR bytes changed')
    evidence['candidate_aar_sha256'] = digest(out.read_bytes())
    evidence['limitations'] = ['Not adopted into Gradle or APK', 'Android runtime untested',
        'Exports are not complete ABI/behavior proof', 'Original upstream provenance not reconstructed',
        '32-bit payloads preserved, not rebuilt', 'Does not establish APK ZIP alignment or Play acceptance']
    (work / 'native-remediation-evidence.json').write_text(json.dumps(evidence, indent=2) + '\n')
    print(json.dumps(evidence, indent=2))


if __name__ == '__main__':
    prepare(Path(sys.argv[1]).resolve())
