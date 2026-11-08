#!/usr/bin/env python3
"""Gate aapt2 badging and AGP metadata against source-derived diagnostic identity."""
import hashlib
import json
from pathlib import Path
import re
import sys


def verify(badging, metadata, expected, apk_name, apk_hash):
    match = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.M)
    if not match:
        raise ValueError('Missing APK package/version')
    package, code, name = match.groups()
    if (package, int(code), name) != (expected['package'], expected['version_code'], expected['version_name']):
        raise ValueError('Packaged identity/version differs from source expectation')
    if not re.search(r'^application-debuggable\s*$', badging, re.M):
        raise ValueError('Expected debuggable diagnostic APK')
    if metadata.get('applicationId') != package or metadata.get('variantName') != 'genericNativeQualification':
        raise ValueError('AGP metadata identity/variant differs')
    elements = metadata.get('elements', [])
    if len(elements) != 1:
        raise ValueError('Expected single universal output')
    e = elements[0]
    if e.get('type') != 'SINGLE' or e.get('filters') != []:
        raise ValueError('Output must be universal without ABI filters')
    if (e.get('outputFile'), e.get('versionCode'), e.get('versionName')) != (apk_name, int(code), name):
        raise ValueError('AGP output filename/version mismatch')
    return {'package': package, 'version_code': int(code), 'version_name': name,
            'debuggable': True, 'universal': True, 'artifact_filename': apk_name,
            'sha256': apk_hash, 'status': 'packaged metadata gate PASS'}


def main():
    root = Path(__file__).resolve().parents[2]
    out = root / 'qualification-evidence'
    inventory = json.loads((out / 'native-inventory.json').read_text())
    apk = Path(inventory['artifact'])
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    if digest != inventory['sha256']:
        raise ValueError('APK changed after native inspection')
    report = verify((out / 'apk-badging.txt').read_text(),
                    json.loads((out / 'output-metadata.json').read_text()),
                    json.loads((out / 'expected-identity.json').read_text()), apk.name, digest)
    (out / 'verified-identity.json').write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    main()
