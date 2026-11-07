#!/usr/bin/env python3
"""Check actual JUnit XML, not a green task label. Fail on missing/empty evidence."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path('app/build/test-results/testGenericDebugUnitTest')
files = sorted(root.glob('TEST-*.xml'))
assert files, f'No JVM XML under {root}'
summary = []
for path in files:
    suite = ET.parse(path).getroot()
    cases = suite.findall('testcase')
    row = dict(name=suite.attrib.get('name'), tests=len(cases),
               failures=sum(c.find('failure') is not None for c in cases),
               errors=sum(c.find('error') is not None for c in cases),
               skipped=sum(c.find('skipped') is not None for c in cases))
    assert int(suite.attrib['tests']) == len(cases), f'Count mismatch: {path}'
    summary.append(row)
print(json.dumps(summary, indent=2), flush=True)
matched = [r for r in summary if r['name'] == 'com.junkfood.seal.util.YtDlpInstallResolutionTest']
assert len(matched) == 1, 'Missing or duplicate migration JVM suite'
assert matched[0]['tests'] == 20 and not matched[0]['skipped'], matched
assert all(not r['failures'] and not r['errors'] for r in summary), summary
