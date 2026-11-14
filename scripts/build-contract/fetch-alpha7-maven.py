#!/usr/bin/env python3
"""Stage the exact published Maven Central bytes bound by alpha7-maven-lock.json.

Downloads only the locked file set from the pinned Central prefix, enforces
per-file byte count and sha256 before writing, never overwrites, and never
fetches a path outside the lock. Verification of the staged set is owned by
verify-alpha7-maven.py; this fetcher is a pinned, hash-checked transport only.
"""
import argparse, hashlib, json, pathlib, urllib.request, urllib.error

CENTRAL = 'https://repo1.maven.org/maven2/'
STAGE_PREFIX = '.qualification/maven/'
MAX_BYTES = 100_000_000


def fetch(root, lock):
    root = root.resolve(strict=True)
    staged = []
    for row in lock['artifacts']:
        for name, entry in sorted(row['files'].items()):
            relative = pathlib.PurePosixPath(name)
            if relative.is_absolute() or '..' in relative.parts or str(relative) != name:
                raise ValueError('unsafe lock path')
            if not name.startswith(STAGE_PREFIX):
                raise ValueError('locked path outside the staging area: ' + name)
            url = CENTRAL + name[len(STAGE_PREFIX):]
            request = urllib.request.Request(url, headers={'User-Agent': 'seali-build-contract'})
            try:
                response = urllib.request.urlopen(request, timeout=60)
            except urllib.error.URLError as e:
                raise ValueError('Central fetch failed for ' + name + ': ' + str(e))
            with response:
                data = response.read(MAX_BYTES + 1)
            if len(data) > MAX_BYTES:
                raise ValueError('oversize artifact: ' + name)
            if len(data) != entry['bytes']:
                raise ValueError('byte count differs from reviewed lock: ' + name)
            if hashlib.sha256(data).hexdigest() != entry['sha256']:
                raise ValueError('digest differs from reviewed lock: ' + name)
            dest = root.joinpath(*relative.parts)
            for parent in [dest, *dest.parents]:
                if parent == root.parent:
                    break
                if parent.is_symlink():
                    raise ValueError('symlink destination')
            dest.parent.mkdir(parents=True, exist_ok=True)
            with dest.open('xb') as output:
                output.write(data)
            staged.append(name)
    return {'staged': len(staged), 'source': CENTRAL, 'hash_checked': True}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--root', type=pathlib.Path, required=True)
    p.add_argument('--lock', type=pathlib.Path, default=pathlib.Path(__file__).with_name('alpha7-maven-lock.json'))
    a = p.parse_args()
    print(json.dumps(fetch(a.root, json.loads(a.lock.read_text()))))
