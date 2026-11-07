#!/usr/bin/env python3
"""ALWAYS finalizer. Raw evidence is NEVER an upload target. No network/write outside output."""
import hashlib
import json
import os
import re
from pathlib import Path
import shutil
import stat

RAW = Path('settings-evidence')
OUT = Path('settings-upload')
CAP = 20 * 1024 * 1024
TEXT_CAP = 64 * 1024
# Fixed names, not suffix-only recursion. No APKs/tars/database/unknown files.
TOP = ['core-runtime-resolution.txt', 'fresh-result.txt', 'recreated-result.txt', 'summary.txt', 'identity.txt',
       'runner-final.txt', 'permission-after-revoke.txt', 'evidence-inspection.json',
       'fresh-lifecycle.txt', 'recreated-lifecycle.txt', 'fresh-activities.txt',
       'recreated-activities.txt', 'fresh-windows.txt', 'recreated-windows.txt', 'runner.log']
NAMED = ['provenance.txt', 'picker.png', 'picker.xml', 'outcome.txt', 'after.png', 'after.xml',
         'picker-open-windows.txt', 'before-root-selection-windows.txt',
         'before-folder-confirmation-windows.txt', 'return-windows.txt', 'pending.png',
         'recreated-before-selection-windows.txt', 'CURRENT-failure.xml',
         'CURRENT-failure.png', 'CURRENT-failure-windows.txt', 'CURRENT-failure-outcome.txt']
# Fixed directory count bounds even malicious diagnostics can only fill these.
SELECT = TOP + [f'{label}/{name}' for label in ['fresh', 'recreated']
               for name in ['settings-host-lifecycle.txt'] + [f'settings-{label}-{n}' for n in NAMED]]
if OUT.exists():
    if OUT.is_symlink():
        OUT.unlink()
    else:
        shutil.rmtree(OUT)
OUT.mkdir()
manifest = {'cap_bytes': CAP, 'files': [], 'incomplete': [], 'method_status': {}}
for label in ['fresh', 'recreated']:
    path = RAW / f'{label}-result.txt'
    status = {'result': 'unavailable', 'success_marker': False, 'failure_marker': False,
              'stage_adjudication': 'unavailable', 'transport_rc': None}
    if path.exists() and not path.is_symlink() and path.is_file():
        # Scan bounded chunks without holding unbounded output in RAM; record
        # both markers, never turn a truncation into method success.
        tail = b''
        with path.open('rb') as f:
            while chunk := f.read(65536):
                check = tail + chunk
                status['success_marker'] |= b'OK (1 test)' in check
                status['failure_marker'] |= any(x in check for x in [b'FAILURES', b'INSTRUMENTATION_FAILED', b'shortMsg=', b'INSTRUMENTATION_ABORTED']) or bool(re.search(rb'INSTRUMENTATION_STATUS_CODE: -[234]\b', check))
                tail = check[-128:]
        status['result'] = 'failure-marker-observed' if status['failure_marker'] else ('success-marker-only-not-adjudicated' if status['success_marker'] else 'indeterminate')
    # Runner adjudication and transport are independent from textual markers.
    summary = RAW / 'summary.txt'
    if summary.is_file() and not summary.is_symlink():
        with summary.open('rb') as f:
            for line in f:
                match = re.fullmatch(rb'stage=' + label.encode() + rb' (PASSED|FAILED) transport_rc=([0-9]+)\n?', line[:1024])
                if match:
                    status['stage_adjudication'] = match[1].decode()
                    status['transport_rc'] = int(match[2])
    manifest['method_status'][label] = status
used = 0
for relative in SELECT:
    source = RAW / relative
    # Refuse symlinks in every path component and nonregular source objects.
    if any(p.is_symlink() for p in [RAW, source.parent, source]):
        manifest['incomplete'].append(f'selected-symlink-rejected:{relative}')
        continue
    if not source.exists():
        if relative in ['fresh-result.txt', 'recreated-result.txt', 'identity.txt']:
            manifest['incomplete'].append(f'missing-or-symlink:{relative}')
        continue
    info = source.stat()
    if not stat.S_ISREG(info.st_mode):
        manifest['incomplete'].append(f'nonregular:{relative}')
        continue
    size = info.st_size
    is_image = source.suffix == '.png'
    limit = min(2 * 1024 * 1024 if is_image else TEXT_CAP, CAP - used - 128 * 1024)
    if limit <= 0:
        manifest['incomplete'].append(f'cap-omitted:{relative}')
        continue
    if is_image and size > limit:
        manifest['incomplete'].append(f'oversize-image-omitted:{relative}:{size}')
        continue
    with source.open('rb') as f:
        data = f.read(limit)
    if len(data) < size:
        manifest['incomplete'].append(f'truncated:{relative}:{size}->{len(data)}')
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    used += len(data)
    manifest['files'].append({'name': relative, 'source_bytes': size, 'upload_bytes': len(data),
                              'sha256': hashlib.sha256(data).hexdigest()})
# Fixed metadata preserves statuses even if raw method result was truncated.
(OUT / 'upload-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
(OUT / 'INCOMPLETE.txt').write_text(
    'Evidence is incomplete: inspect upload-manifest.json. Original method status is separate.\n'
    if manifest['incomplete'] else 'No sanitizer omission/truncation. Named-file inspection still determines completeness.\n')
actual = 0
for p in OUT.rglob('*'):
    if p.is_symlink() or (not p.is_dir() and not p.is_file()):
        raise SystemExit('Unsafe output type; upload blocked')
    if p.is_file():
        actual += p.stat().st_size
assert actual <= CAP, f'Actual second size check failed: {actual}>{CAP}; upload blocked'
print(f'Validated upload directory {OUT}: actual_bytes={actual}, cap={CAP}, incomplete={bool(manifest["incomplete"])}')
