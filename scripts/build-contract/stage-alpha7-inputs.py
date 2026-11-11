#!/usr/bin/env python3
"""Verify and stage a fixed data-only ARM64 archive. Never downloads or executes target code."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import tarfile


def stage(archive, root, lock):
    raw = archive.read_bytes()
    if len(raw) != lock['archive']['bytes'] or hashlib.sha256(raw).hexdigest() != lock['archive']['sha256']:
        raise ValueError('input archive identity mismatch')
    expected = lock['files']
    for name, row in lock.get('tracked_files', {}).items():
        tracked = root / name
        if not tracked.is_file() or tracked.is_symlink() or len(tracked.read_bytes()) != row['bytes'] or hashlib.sha256(tracked.read_bytes()).hexdigest() != row['sha256']:
            raise ValueError('tracked file mismatch: ' + name)
    pending = {}
    with tarfile.open(archive, 'r:') as stream:
        for member in stream:
            name = member.name
            if name not in expected or name in pending or not member.isfile() or member.issym() or member.islnk():
                raise ValueError('unexpected, duplicate or nonregular member')
            parts = PurePosixPath(name).parts
            if name.startswith('/') or '..' in parts or str(PurePosixPath(name)) != name:
                raise ValueError('unsafe member path')
            row = expected[name]
            if member.size != row['bytes'] or member.size > 25_000_000:
                raise ValueError('member size mismatch')
            data = stream.extractfile(member).read()
            if hashlib.sha256(data).hexdigest() != row['sha256']:
                raise ValueError('member digest mismatch')
            dest = root.joinpath(*parts)
            # Inputs must not overwrite tracked or previously staged files.
            for parent in [dest, *dest.parents]:
                if parent == root.parent:
                    break
                if parent.is_symlink():
                    raise ValueError('symlink destination')
            if dest.exists():
                raise ValueError('input destination already exists: ' + name)
            pending[name] = data
    if set(pending) != set(expected):
        raise ValueError('missing archive member')
    for name, data in pending.items():
        dest = root / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        with dest.open('xb') as output:
            output.write(data)
    return {'archive_sha256': lock['archive']['sha256'], 'files_staged': len(pending), 'target_executed': False}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--archive', type=Path, required=True)
    p.add_argument('--root', type=Path, required=True)
    p.add_argument('--lock', type=Path, required=True)
    a = p.parse_args()
    print(json.dumps(stage(a.archive, a.root.resolve(strict=True), json.loads(a.lock.read_text()))))
