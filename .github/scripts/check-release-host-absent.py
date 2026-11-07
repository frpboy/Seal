#!/usr/bin/env python3
"""Inspect both release artifact families built by Android CI, fail if missing."""
from pathlib import Path
import zipfile

needles = ['com.junkfood.seal.SafTestHostActivity', 'Lcom/junkfood/seal/SafTestHostActivity;']
def check_bytes(data, source):
    assert not any(n.encode(enc) in data for n in needles for enc in ['utf-8', 'utf-16le']), source

families = {
    'genericRelease APK': sorted(Path('app/build/outputs/apk/generic/release').glob('*.apk')),
    'playRelease AAB': sorted(Path('app/build/outputs/bundle/playRelease').glob('*.aab')),
}
for family, paths in families.items():
    assert paths, f'Missing {family} family'
    for path in paths:
        with zipfile.ZipFile(path) as z:
            manifests = [n for n in z.namelist() if n.endswith('AndroidManifest.xml')]
            dex = [n for n in z.namelist() if n.endswith('.dex')]
            assert manifests and dex, f'Missing manifest/DEX in {path}'
            for name in manifests + dex:
                check_bytes(z.read(name), (path, name))
        print(f'No SAF debug host declaration/class reference in {path}')

# Pre-R8 manifests prevent obfuscation from hiding a leaked host. Require BOTH
# built variant families, not merely one manifest somewhere under this tree.
root = Path('app/build/intermediates/merged_manifests')
for variant in ['genericRelease', 'playRelease']:
    paths = sorted((root / variant).rglob('AndroidManifest.xml'))
    assert paths, f'Missing {variant} merged manifest family'
    for path in paths:
        check_bytes(path.read_bytes(), path)
        print(f'No SAF debug host in merged manifest {path}')
