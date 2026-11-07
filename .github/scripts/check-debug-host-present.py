#!/usr/bin/env python3
"""Require the fixture in the debug target app, not merely in its test APK."""
from pathlib import Path
import zipfile

host = 'com.junkfood.seal.SafTestHostActivity'
paths = sorted(Path('app/build/outputs/apk/generic/debug').glob('*x86_64*.apk'))
assert len(paths) == 1, paths
with zipfile.ZipFile(paths[0]) as z:
    manifest = z.read('AndroidManifest.xml')
    assert any(host.encode(e) in manifest for e in ['utf-8', 'utf-16le']), 'Debug manifest lacks host'
    assert any(b'Lcom/junkfood/seal/SafTestHostActivity;' in z.read(n) for n in z.namelist() if n.endswith('.dex')), 'Debug DEX lacks host'
print(f'Debug target host manifest+DEX present: {paths[0]}')
