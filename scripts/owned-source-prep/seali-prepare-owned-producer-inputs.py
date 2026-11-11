#!/usr/bin/env python3
"""Prepare locked source inputs only. No downloads, compilation or target execution."""
import argparse
import hashlib
import json
import os
import shutil
import stat
import tarfile
import tempfile
from pathlib import Path, PurePosixPath

LOCK_SHA256 = '5c6244cb912a2548959adb951f4f527e91045abab37b5c878f4e9d2a3eacc4f5'
MAX_MEMBERS = 20000
MAX_BYTES = 128 * 1024 * 1024
MAX_FILE = 32 * 1024 * 1024


def require(condition, message):
    if not condition:
        raise ValueError(message)


def regular_inside(root, relative):
    parts = PurePosixPath(relative).parts
    require(parts and not PurePosixPath(relative).is_absolute() and all(x not in ('.', '..', '') for x in relative.split('/')), 'unsafe input path')
    p = root
    for part in parts:
        p = p / part
        require(not p.is_symlink(), 'symlink input component')
    require(p.is_file(), 'missing regular input: ' + relative)
    p.resolve().relative_to(root.resolve())
    return p


def verify_file(path, record):
    require(path.stat().st_size == record['bytes'], 'input size differs: ' + str(path))
    require(hashlib.sha256(path.read_bytes()).hexdigest() == record['sha256'], 'input hash differs: ' + str(path))


def safe_unpack(archive, destination):
    """Validate the entire graph first; never link to a path outside that graph."""
    with tarfile.open(archive) as t:
        members = []
        by_name = {}
        total = 0
        root_name = None
        previous_regular = set()
        for m in t:
            require(len(members) < MAX_MEMBERS, 'too many archive members')
            parts = m.name.rstrip('/').split('/')
            require(parts and all(x not in ('', '.', '..') for x in parts) and not m.name.startswith('/'), 'unsafe archive member')
            name = '/'.join(parts)
            require(name not in by_name, 'duplicate archive member')
            require(m.isdir() or m.isfile() or m.issym() or m.islnk(), 'special archive member')
            require(0 <= m.size <= MAX_FILE, 'oversized archive member')
            total += m.size
            require(total <= MAX_BYTES, 'archive expands beyond bound')
            root_name = root_name or parts[0]
            require(parts[0] == root_name, 'multiple archive roots')
            require(len(name) <= 4096, 'archive path too long')
            if m.issym():
                require(m.linkname and not m.linkname.startswith('/') and len(m.linkname) <= 4096, 'unsafe symlink target')
            if m.islnk():
                require(m.linkname in previous_regular, 'hardlink must name an earlier regular archive file exactly')
                require(all(x not in ('', '.', '..') for x in m.linkname.split('/')), 'unsafe hardlink target')
            if m.isfile():
                previous_regular.add(name)
            by_name[name] = m
            members.append((name, m))
        require(members, 'empty archive')
        # No member, including a hardlink source, can have a link/file ancestor.
        for name, m in members:
            parts = name.split('/')
            for i in range(1, len(parts)):
                ancestor = by_name.get('/'.join(parts[:i]))
                require(ancestor is None or ancestor.isdir(), 'non-directory member ancestor')

        def resolve_virtual(parts):
            current = []
            pending = list(parts)
            expansions = 0
            while pending:
                part = pending.pop(0)
                if part in ('', '.'):
                    continue
                if part == '..':
                    require(len(current) > 1, 'link escapes archive root')
                    current.pop()
                    continue
                current.append(part)
                require(current[0] == root_name, 'link changes archive root')
                item = by_name.get('/'.join(current))
                if item is not None and item.issym():
                    expansions += 1
                    require(expansions <= 40, 'symlink cycle or excessive chain')
                    current.pop()
                    pending = item.linkname.split('/') + pending
                elif pending and item is not None:
                    require(item.isdir(), 'link traverses non-directory')
            return current

        for name, m in members:
            if m.issym():
                resolve_virtual(name.split('/')[:-1] + m.linkname.split('/'))
        destination.mkdir()
        # Files/directories first, so no writes can pass through links.
        for name, m in members:
            dest = destination / name
            if m.isdir():
                dest.mkdir(parents=True, exist_ok=True)
            elif m.isfile():
                dest.parent.mkdir(parents=True, exist_ok=True)
                with t.extractfile(m) as src, dest.open('xb') as dst:
                    shutil.copyfileobj(src, dst)
                dest.chmod(m.mode & 0o777)
        for name, m in members:
            if m.islnk():
                source = regular_inside(destination, m.linkname)
                require(stat.S_ISREG(source.lstat().st_mode), 'hardlink source not regular')
                dest = destination / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                os.link(source, dest, follow_symlinks=False)
        for name, m in members:
            if m.issym():
                # The complete graph was resolved before any link creation.
                dest = destination / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.symlink_to(m.linkname)
        for name, m in members:
            if m.issym():
                (destination / name).resolve().relative_to(destination.resolve())
        return root_name


def prepare(bundle, output, lock):
    bundle, output = Path(bundle).resolve(), Path(output).absolute()
    require(not output.exists() and not output.is_symlink(), 'fresh output required')
    require(output.parent.is_dir() and not output.parent.is_symlink(), 'existing regular output parent required')
    # All expected metadata, input sets and recipe files checked before copying.
    metadata_rows = {}
    for rel, record in lock['metadata'].items():
        p = regular_inside(bundle, rel)
        data = p.read_bytes()
        require(hashlib.sha256(data).hexdigest() == record['sha256'], 'metadata hash differs')
        rows = json.loads(data)
        names = [x['file'] for x in rows]
        require(len(names) == len(set(names)) and sorted(names) == record['names'], 'metadata input set differs')
        category = rel.split('/')[0]
        for row in rows:
            matching = [r for path, r in lock['inputs'].items()
                        if path.startswith(category + '/') and r['name'] == row['file']]
            require(len(matching) == 1, 'metadata input missing from lock')
            require(row['sha256'] == matching[0]['sha256'] and row.get('bytes') == matching[0]['bytes'],
                    'metadata hash/size differs from input lock')
            if row['file'] in metadata_rows:
                require(metadata_rows[row['file']]['sha256'] == row['sha256'], 'metadata source conflict')
            metadata_rows[row['file']] = dict(row)

    combined = {}
    for rel, record in lock['inputs'].items():
        source = regular_inside(bundle, rel)
        verify_file(source, record)
        name = record['name']
        require(name and PurePosixPath(name).name == name and name not in ('.', '..'), 'unsafe flat input name')
        require(name not in combined or combined[name][1]['sha256'] == record['sha256'], 'conflicting source name')
        combined[name] = (source, record)
    recipe_root = bundle / 'python-refresh/recipe'
    require(not recipe_root.is_symlink(), 'recipe symlink')
    actual = set()
    for p in recipe_root.rglob('*'):
        require(not p.is_symlink(), 'recipe symlink')
        if p.is_file():
            actual.add(str(p.relative_to(recipe_root)))
        else:
            require(p.is_dir(), 'recipe special input')
    require(actual == set(lock['recipe']), 'recipe file set differs')
    for rel, record in lock['recipe'].items():
        verify_file(regular_inside(recipe_root, rel), record)
    require(set(metadata_rows) == set(combined), 'metadata union differs from input lock')
    temp = Path(tempfile.mkdtemp(prefix='.' + output.name + '-', dir=output.parent))
    temp.chmod(0o700)
    try:
        inputs = temp / 'inputs'
        inputs.mkdir()
        rows = []
        for name, (source, record) in combined.items():
            shutil.copyfile(source, inputs / name)
            verify_file(inputs / name, record)
            (inputs / name).chmod(0o600)
            rows.append(metadata_rows[name])
        (inputs / 'source-inputs.json').write_text(json.dumps(rows, indent=2) + '\n')
        root_name = safe_unpack(inputs / 'termux-9115a276.tar.gz', temp / 'termux-source')
        recipe = temp / 'recipe'
        recipe.mkdir()
        for rel, record in lock['recipe'].items():
            dest = recipe / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(regular_inside(recipe_root, rel), dest)
            verify_file(dest, record)
            dest.chmod(0o600)
        shutil.copyfile(inputs / 'cacert.pem', temp / 'cacert.pem')
        (temp / 'cacert.pem').chmod(0o600)
        require(not output.exists() and not output.is_symlink(), 'output appeared during preparation')
        # Same-filesystem rename, only after every validation succeeds.
        temp.rename(output)
        return {'status': 'locked source inputs prepared; no compilation or target execution',
                'input_files': len(combined), 'inputs': str(output / 'inputs'),
                'termux_source': str(output / 'termux-source' / root_name),
                'recipe': str(output / 'recipe'), 'ca': str(output / 'cacert.pem')}
    finally:
        if temp.exists():
            shutil.rmtree(temp)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--bundle', required=True)
    p.add_argument('--output', required=True)
    a = p.parse_args()
    lock_path = Path(__file__).with_name('seali-owned-producer-lock.json')
    data = lock_path.read_bytes()
    require(hashlib.sha256(data).hexdigest() == LOCK_SHA256, 'producer lock differs')
    print(json.dumps(prepare(a.bundle, a.output, json.loads(data)), indent=2))


if __name__ == '__main__':
    main()
