#!/usr/bin/env python3
"""Fail-closed comparison of an actual resolved runtime graph report against the reviewed baseline lock.

Lock and report share one shape: {configuration, components, artifacts}.
components: sorted identity strings for every resolved component (module gav,
project path, other) - any selection/version/added/removed difference fails.
artifacts: module artifacts with group/name/version/file/sha256/bytes - any
identity or byte difference fails. Nothing is assumed or allowlisted.
"""
import argparse, json, pathlib, sys

EXPECTED_KEYS = {'configuration', 'components', 'artifacts'}

def load(path):
    doc = json.loads(pathlib.Path(path).read_text())
    if set(doc) != EXPECTED_KEYS: raise ValueError('unexpected graph document keys: ' + str(sorted(doc)))
    if sorted(doc['components']) != doc['components'] or len(set(doc['components'])) != len(doc['components']):
        raise ValueError('components must be sorted and unique')
    rows = {}
    for row in doc['artifacts']:
        key = f"{row['group']}:{row['name']}:{row['version']}:{row['file']}"
        if key in rows: raise ValueError('duplicate artifact identity: ' + key)
        rows[key] = row
    # every module artifact must come from a component present in the graph
    comp = set(doc['components'])
    for key in rows:
        g, n, v, _f = key.split(':', 3)
        if f'module:{g}:{n}:{v}' not in comp: raise ValueError('artifact without resolved component: ' + key)
    return doc['configuration'], doc['components'], rows

def verify(lock_path, report_path):
    lock_conf, lock_comp, lock = load(lock_path)
    report_conf, report_comp, report = load(report_path)
    if lock_conf != report_conf: raise ValueError(f'configuration differs: lock={lock_conf} report={report_conf}')
    problems = []
    for c in sorted(set(lock_comp) - set(report_comp)): problems.append('component missing from actual graph: ' + c)
    for c in sorted(set(report_comp) - set(lock_comp)): problems.append('component not in reviewed baseline: ' + c)
    for key in sorted(set(lock) - set(report)): problems.append('artifact missing from actual graph: ' + key)
    for key in sorted(set(report) - set(lock)): problems.append('artifact not in reviewed baseline: ' + key)
    for key in sorted(set(lock) & set(report)):
        want, got = lock[key], report[key]
        for field in ('sha256', 'bytes'):
            if want[field] != got.get(field):
                problems.append(f"{field} differs for {key}: baseline={want[field]} actual={got.get(field)}")
    if problems:
        raise ValueError('resolved runtime graph differs from reviewed baseline:\n' + '\n'.join(problems))
    return {'configuration': lock_conf, 'components_checked': len(lock_comp), 'artifacts_checked': len(lock),
            'scope': 'resolved graph identity/bytes only, not runtime, signing or release approval'}

if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--lock', required=True); p.add_argument('--report', required=True)
    a = p.parse_args()
    try:
        print(json.dumps(verify(a.lock, a.report), indent=2))
    except ValueError as e:
        print(str(e), file=sys.stderr); sys.exit(1)
