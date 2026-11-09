#!/usr/bin/env python3
"""Read-only candidate-host admission. Never installs, builds, or qualifies an APK."""
import json
import os
import pathlib
import re
import stat
import subprocess
import sys


def admitted_kvm(path, readable, writable):
    try:
        return stat.S_ISCHR(path.stat().st_mode) and readable and writable
    except OSError:
        return False


def valid_sha(value):
    return bool(re.fullmatch(r'[0-9a-f]{40}', value))


def main():
    expected = os.environ.get('EXPECTED_SOURCE_SHA', '')
    actual = os.environ.get('GITHUB_SHA', '')
    source_matches = valid_sha(expected) and expected == actual
    public_owner = (os.environ.get('GITHUB_REPOSITORY') == 'frpboy/Seali'
                    and os.environ.get('REPOSITORY_PRIVATE') == 'false')
    manual = os.environ.get('GITHUB_EVENT_NAME') == 'workflow_dispatch'
    kvm = pathlib.Path('/dev/kvm')
    accessible = admitted_kvm(kvm, os.access(kvm, os.R_OK), os.access(kvm, os.W_OK))
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or ''
    emulator = pathlib.Path(sdk) / 'emulator/emulator'
    accelerator_ok = False
    if source_matches and public_owner and manual and accessible and emulator.is_file():
        try:
            # No sudo, chmod, module loading, device creation, emulator boot or network.
            result = subprocess.run([str(emulator), '-accel-check'],
                                    capture_output=True, timeout=30, check=False)
            accelerator_ok = result.returncode == 0
        except (OSError, subprocess.TimeoutExpired):
            pass
    checks = dict(exact_source=source_matches, public_expected_repository=public_owner,
                  manual_workflow=manual, kvm_rw_character_device=accessible,
                  emulator_present=emulator.is_file(), emulator_accel_check=accelerator_ok)
    report = dict(schema=1, source_sha=actual if valid_sha(actual) else None,
                  image_os=os.environ.get('ImageOS'),
                  image_version=os.environ.get('ImageVersion'), checks=checks,
                  host_admitted=all(checks.values()), runtime_pass=False,
                  release_qualified=False, permission_changes=False,
                  packages_installed=False)
    pathlib.Path('host-admission.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, sort_keys=True))
    return 0 if report['host_admitted'] else 78


if __name__ == '__main__':
    sys.exit(main())
