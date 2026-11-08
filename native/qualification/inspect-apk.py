#!/usr/bin/env python3
"""Unsigned qualification APK inventory. ELF32 recorded; 16KiB gate applies to ELF64."""
import importlib.util
import hashlib
import json
from pathlib import Path
import struct
import sys
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('reviewed_native', ROOT / '.github/scripts/check-native-alignment.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)
original_elf = checker.inspect_elf


def inspect_elf(data, name):
    if data[:4] != b'\x7fELF' or data[5:6] != b'\x01':
        raise ValueError(f'{name}: unsupported ELF encoding')
    bits = data[4] * 32
    if bits == 64:
        result = original_elf(data, name)
    elif bits == 32:
        if len(data) < 52:
            raise ValueError(f'{name}: truncated ELF32')
        offset = struct.unpack_from('<I', data, 28)[0]
        size, count = struct.unpack_from('<HH', data, 42)
        if size < 32 or not count or offset + size * count > len(data):
            raise ValueError(f'{name}: invalid ELF32 program headers')
        loads = []
        for i in range(count):
            fields = struct.unpack_from('<8I', data, offset + i * size)
            if fields[0] == 1:
                loads.append({'offset': fields[1], 'vaddr': fields[2], 'alignment': fields[7]})
        if not loads:
            raise ValueError(f'{name}: missing ELF32 PT_LOAD')
        result = {'entry': name, 'sha256': hashlib.sha256(data).hexdigest(),
                  'loads': loads, 'passes': True}
    else:
        raise ValueError(f'{name}: unsupported ELF class')
    abi = name.split('/')[1]
    machine = struct.unpack_from('<H', data, 18)[0]
    expected = {'arm64-v8a': (64, 183), 'x86_64': (64, 62),
                'armeabi-v7a': (32, 40), 'x86': (32, 3)}
    if expected.get(abi) != (bits, machine):
        raise ValueError(f'{name}: ABI/class/machine mismatch {bits}/{machine}')
    result.update(elf_class=bits, machine=machine,
                  alignment_gate='16KiB PT_LOAD' if bits == 64 else 'inventory only, not 16KiB gate')
    return result


def main():
    paths = list((ROOT / 'app/build/outputs/apk/generic/nativeQualification').glob('*.apk'))
    if len(paths) != 1:
        raise ValueError(f'Expected exactly one universal qualification APK, found {len(paths)}')
    path = paths[0]
    if b'APK Sig Block 42' in path.read_bytes():
        raise ValueError('Unexpected APK signing block')
    with ZipFile(path) as archive:
        for item in archive.infolist():
            if item.filename.upper().startswith('META-INF/') and item.filename.upper().endswith(('.RSA', '.DSA', '.EC', '.SF')):
                raise ValueError('Unexpected JAR signing entry')
            if not checker.safe_name(item.filename):
                raise ValueError('Unsafe APK path')
            if checker.native_name(item.filename) and (not item.filename.startswith('lib/') or not item.filename.endswith('.so')):
                raise ValueError(f'Uninventoried native location: {item.filename}')
    checker.ABIS = {'arm64-v8a', 'x86_64', 'armeabi-v7a', 'x86'}
    checker.inspect_elf = inspect_elf
    result = checker.inspect_artifact(path)
    if set(result['packaged_abis']) != checker.ABIS:
        raise ValueError('Expected all four packaged ABIs')
    # Historical key retained by the reviewed container walker; contents now include ELF32.
    result['native_elf'] = result.pop('elf64')
    result['size_bytes'] = path.stat().st_size
    result['signing'] = 'unsigned: no JAR signature entries or APK signing block found'
    result['signing_certificate'] = None
    result['qualification'] = 'unsigned diagnostic build only; no Android runtime, install, provider, release or AAB proof'
    out = ROOT / 'qualification-evidence/native-inventory.json'
    out.write_text(json.dumps(result, indent=2) + '\n')
    if not all(e['passes'] for e in result['native_elf']):
        raise ValueError('Packaged ELF64 PT_LOAD gate failed; see inventory')
    print(f'PASS {len(result["native_elf"])} direct/nested ELF inventory; ELF64 alignment only')


if __name__ == '__main__':
    main()
