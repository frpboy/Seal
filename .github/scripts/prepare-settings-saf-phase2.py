#!/usr/bin/env python3
"""Produce a workflow-only local proposal; never push or dispatch."""
from pathlib import Path
import re
import sys

if len(sys.argv) != 3 or not re.fullmatch(r'[0-9a-f]{40}', sys.argv[1]):
    raise SystemExit('Usage: prepare-settings-saf-phase2.py VERIFIED_STAGING_SHA OUTPUT_PATH')
sha = sys.argv[1]
p = Path('.github/workflows/android_ci.yml')
s = p.read_text()
a, b = s.split('  settings-saf:\n', 1)
assert '    if: ${{ false }}' in b
condition = ("    if: >-\n"
    "      github.event_name == 'pull_request' && github.event.action == 'synchronize' &&\n"
    "      github.event.number == 25 && github.run_attempt == 1 &&\n"
    "      github.repository == 'frpboy/Seal' &&\n"
    "      github.event.pull_request.head.repo.full_name == 'frpboy/Seal' &&\n"
    "      github.event.pull_request.head.ref == 'candidate/security-baseline' &&\n"
    f"      github.event.before == '{sha}'")
b = b.replace('    if: ${{ false }}', condition, 1)
b = b.replace('          persist-credentials: false',
              f'          persist-credentials: false\n          ref: {sha}', 1)
# Skip unrelated build/signing/history ONLY on this exact single transition.
# Future PR synchronizes resume their normal jobs. Settings remains one-shot.
skip = "    if: >-\n      ${{ !(" + condition.split("    if: >-\n", 1)[1].strip() + ") }}\n"
for job in ['build', 'history-ui']:
    marker = f'  {job}:\n'
    assert a.count(marker) == 1
    a = a.replace(marker, marker + skip, 1)
Path(sys.argv[2]).write_text(a+'  settings-saf:\n'+b)
print('Created local phase2 proposal only. Missing before fails comparison; no dispatch. Review exact diff before write.')
