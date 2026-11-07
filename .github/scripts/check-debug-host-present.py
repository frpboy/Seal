#!/usr/bin/env python3
"""Require the fixture in the debug target app, not merely in its test APK."""
from pathlib import Path
import zipfile

hosts = ['com.junkfood.seal.SafTestHostActivity', 'com.junkfood.seal.DirectorySettingsTestActivity']
paths = sorted(Path('app/build/outputs/apk/generic/debug').glob('*x86_64*.apk'))
assert len(paths) == 1, paths
with zipfile.ZipFile(paths[0]) as z:
    manifest = z.read('AndroidManifest.xml')
    for host in hosts:
        assert any(host.encode(e) in manifest for e in ['utf-8', 'utf-16le']), f'Debug manifest lacks {host}'
        descriptor = ('L' + host.replace('.', '/') + ';').encode()
        assert any(descriptor in z.read(n) for n in z.namelist() if n.endswith('.dex')), f'Debug DEX lacks {host}'
print(f'Debug target host manifest+DEX present: {paths[0]}')
