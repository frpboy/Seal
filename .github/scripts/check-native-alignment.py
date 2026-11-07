#!/usr/bin/env python3
"""Inspect direct and ZIP-packaged ELF64 payloads without extracting files.

Reports ELF PT_LOAD constraints only. Does not establish runtime, APK ZIP
alignment, Android page size, or Play acceptance.
"""
from pathlib import Path
from zipfile import ZipFile, is_zipfile
from io import BytesIO
import hashlib
import json
import struct
import stat
import posixpath
import re

LIMIT = 512 * 1024 * 1024
ABIS = {'arm64-v8a', 'x86_64'}


class Budget:
    def __init__(self, byte_limit=2 * 1024 ** 3, entry_limit=50000):
        self.bytes = byte_limit
        self.entries = entry_limit

    def charge(self, infos):
        self.bytes -= sum(x.file_size for x in infos)
        self.entries -= len(infos)
        if self.bytes < 0 or self.entries < 0:
            raise ValueError('Aggregate expanded bytes/entry ceiling exceeded')


def native_name(name):
    return bool(re.search(r'\.so(?:\.|$)', name))


def safe_name(name):
    return bool(name) and not name.startswith('/') and '..' not in name.split('/') and '\\' not in name and '\x00' not in name


def inspect_elf(data, name):
    if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01':
        raise ValueError(f'{name}: not little-endian ELF64')
    offset = struct.unpack_from('<Q', data, 32)[0]
    size, count = struct.unpack_from('<HH', data, 54)
    if size < 56 or not count or offset + size * count > len(data):
        raise ValueError(f'{name}: invalid program-header table')
    loads = []
    for i in range(count):
        ph = offset + i * size
        if struct.unpack_from('<I', data, ph)[0] != 1:
            continue
        file_offset, vaddr = struct.unpack_from('<QQ', data, ph + 8)
        alignment = struct.unpack_from('<Q', data, ph + 48)[0]
        loads.append({'alignment': alignment, 'offset': file_offset, 'vaddr': vaddr})
    if not loads:
        raise ValueError(f'{name}: no PT_LOAD')
    return {
        'entry': name,
        'sha256': hashlib.sha256(data).hexdigest(),
        'loads': loads,
        'passes': all(x['alignment'] >= 16384 and
                      x['alignment'] & (x['alignment'] - 1) == 0 and
                      x['offset'] % 16384 == x['vaddr'] % 16384 for x in loads),
    }


def inspect_payload(data, name, budget=None):
    budget = budget if budget is not None else Budget()
    if data.startswith(b'\x7fELF'):
        return [inspect_elf(data, name)]
    if not name.endswith('.zip.so') or not is_zipfile(BytesIO(data)):
        raise ValueError(f'{name}: unknown native payload')
    results = []
    with ZipFile(BytesIO(data)) as archive:
        infos = archive.infolist()
        if sum(x.file_size for x in infos) > LIMIT:
            raise ValueError(f'{name}: expanded ZIP exceeds inspection ceiling')
        budget.charge(infos)
        by_name = {x.filename: x for x in infos}
        if len(by_name) != len(infos) or any(not safe_name(x.filename) for x in infos):
            raise ValueError(f'{name}: duplicate or unsafe ZIP path')
        for info in infos:
            if info.is_dir():
                continue
            entry = archive.read(info)
            if stat.S_ISLNK(info.external_attr >> 16):
                # Only actual Unix symlink metadata qualifies. Resolve in-memory
                # within this ZIP, reject absent targets, cycles and escape paths.
                current = info
                seen = set()
                while stat.S_ISLNK(current.external_attr >> 16):
                    if current.filename in seen or len(seen) >= 32:
                        raise ValueError(f'{name}: symlink cycle/depth')
                    seen.add(current.filename)
                    target = archive.read(current).decode('utf-8')
                    if not safe_name(target):
                        raise ValueError(f'{name}: unsafe symlink target')
                    target = posixpath.normpath(posixpath.join(posixpath.dirname(current.filename), target))
                    if target not in by_name:
                        raise ValueError(f'{name}: missing symlink target')
                    current = by_name[target]
                if current.is_dir() or (native_name(info.filename) and not archive.read(current).startswith(b'\x7fELF')):
                    raise ValueError(f'{name}: invalid native symlink target')
                continue
            if entry.startswith(b'PK\x03\x04') or is_zipfile(BytesIO(entry)):
                raise ValueError(f'{name}!{info.filename}: unsupported nested ZIP')
            if entry.startswith(b'\x7fELF'):
                results.append(inspect_elf(entry, name + '!' + info.filename))
            elif native_name(info.filename):
                raise ValueError(f'{name}!{info.filename}: unknown regular native payload')
    if not results:
        raise ValueError(f'{name}: ZIP contains no ELF64')
    return results


def inspect_artifact(path, budget=None):
    budget = budget if budget is not None else Budget()
    entries = []
    abis = set()
    with ZipFile(path) as archive:
        infos = archive.infolist()
        budget.charge(infos)
        if len({x.filename for x in infos}) != len(infos):
            raise ValueError(f'{path}: duplicate ZIP entry')
        for info in infos:
            parts = info.filename.split('/')
            if not info.filename.endswith('.so') or 'lib' not in parts:
                continue
            index = parts.index('lib')
            if index + 2 >= len(parts):
                raise ValueError(f'{path}: malformed native path {info.filename}')
            abi = parts[index + 1]
            abis.add(abi)
            if abi not in ABIS:
                continue
            if info.file_size > LIMIT:
                raise ValueError(f'{path}: payload exceeds inspection ceiling')
            entries.extend(inspect_payload(archive.read(info), info.filename, budget))
    if not abis or (abis & ABIS and not entries):
        raise ValueError(f'{path}: missing native libraries/ELF64')
    return {'artifact': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'packaged_abis': sorted(abis), 'elf64': entries}


def main():
    paths = sorted(Path('app/build/outputs/apk/generic/release').glob('*.apk'))
    paths += sorted(Path('app/build/outputs/bundle/playRelease').glob('*.aab'))
    if not any(p.suffix == '.apk' for p in paths) or not any(p.suffix == '.aab' for p in paths):
        raise SystemExit('Missing APK/AAB outputs')
    budget = Budget()
    results = [inspect_artifact(path, budget) for path in paths]
    Path('native-alignment.json').write_text(json.dumps(results, indent=2) + '\n')
    failures = [(r['artifact'], e['entry']) for r in results for e in r['elf64'] if not e['passes']]
    if failures:
        raise SystemExit(f'ELF64 PT_LOAD constraints failed: {failures}')
    print('All inspected direct/nested ELF64 LOAD constraints pass; no runtime/ZIP alignment claim')


if __name__ == '__main__':
    main()
