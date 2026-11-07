#!/usr/bin/env python3
"""Inspect named per-stage evidence before upload; don't hide JUnit failure."""
import hashlib
import json
from pathlib import Path
import tarfile

root = Path('settings-evidence')
report = []
evidence_ok = True
for label, method in [('fresh', 'actualSdcardSettingsPersistsFreshGrant'),
                      ('recreated', 'pendingSdcardResultRoutesAfterRecreation')]:
    result = (root / f'{label}-result.txt').read_text()
    passed = '\nOK (1 test)\n' in result
    stage = {'stage': label, 'method': method, 'junit_passed': passed,
             'files': [], 'missing': [], 'errors': []}
    destination = root / label
    destination.mkdir(exist_ok=True)
    try:
        with tarfile.open(root / f'{label}-files.tar') as archive:
            members = {Path(m.name).name: m for m in archive.getmembers()
                       if m.isfile() and (Path(m.name).name.startswith(f'settings-{label}-') or
                                         Path(m.name).name == 'settings-host-lifecycle.txt')}
            for name, member in members.items():
                if member.size > 5 * 1024 * 1024:
                    stage['errors'].append(f'oversize:{name}')
                    continue
                content = archive.extractfile(member).read()
                (destination / name).write_bytes(content)
                stage['files'].append({'name': name, 'bytes': len(content),
                                       'sha256': hashlib.sha256(content).hexdigest()})
            required = [f'settings-{label}-provenance.txt', 'settings-host-lifecycle.txt']
            if passed:
                required += [f'settings-{label}-{name}' for name in [
                    'picker.png', 'picker.xml', 'outcome.txt', 'after.png', 'after.xml',
                    'before-root-selection-windows.txt', 'before-folder-confirmation-windows.txt',
                    'return-windows.txt', 'render-capture.txt', 'capture-windows.txt', 'pre-render.png']]
                if label == 'recreated':
                    required += [f'settings-{label}-pending.png', f'settings-{label}-recreated-before-selection-windows.txt']
            else:
                required += [f'settings-{label}-CURRENT-failure{ext}' for ext in
                             ['.xml', '.png', '-windows.txt', '-outcome.txt']]
            for name in required:
                p = destination / name
                if not p.exists() or p.stat().st_size == 0:
                    stage['missing'].append(name)
            provenance = destination / f'settings-{label}-provenance.txt'
            if provenance.exists() and f'method={method}\n' not in provenance.read_text():
                stage['errors'].append('method provenance mismatch')
    except Exception as error:
        stage['errors'].append(f'{type(error).__name__}:{error}')
    if stage['missing'] or stage['errors']:
        evidence_ok = False
    report.append(stage)
(root / 'evidence-inspection.json').write_text(json.dumps(report, indent=2)+'\n')
# Original product/harness method outcomes remain in separate result files.
print(json.dumps(report, indent=2))
raise SystemExit(0 if evidence_ok else 1)
