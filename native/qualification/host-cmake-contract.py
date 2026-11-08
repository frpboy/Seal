#!/usr/bin/env python3
"""Fail closed around one explicit disposable-host CMake symlink adapter."""
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys


SOURCE = Path('/usr/local/bin/cmake')
TARGET = Path('/usr/bin/cmake')
RECEIPT = Path('qualification-evidence/host-cmake-contract.json')


def trusted_chain(path, executable=True):
    # Reject every lexical symlink before execution. The one target adapter
    # link is checked separately and never passed through this helper.
    if not path.is_absolute() or '..' in path.parts:
        raise ValueError('Absolute normalized tool path required')
    for item in [path, *path.parents]:
        info = os.lstat(item)
        if stat.S_ISLNK(info.st_mode):
            raise ValueError('Symlink in source tool chain: ' + str(item))
        if info.st_uid != 0 or info.st_mode & 0o022:
            raise ValueError('Untrusted tool path ownership/permissions: ' + str(item))
        if item != path or not executable:
            if not stat.S_ISDIR(info.st_mode):
                raise ValueError('Tool ancestor must be directory')
        elif not stat.S_ISREG(info.st_mode) or not os.access(item, os.X_OK):
            raise ValueError('Tool must be executable regular file')
    return path


def main():
    phase = sys.argv[1]
    if phase not in ('before', 'after'):
        raise ValueError('Unknown phase')
    source, target = SOURCE, TARGET
    executable = trusted_chain(source)
    trusted_chain(target.parent, executable=False)
    version = subprocess.check_output([str(executable), '--version'], text=True).splitlines()[0]
    if version != 'cmake version 3.31.6':
        raise ValueError('Unexpected preinstalled CMake version: ' + version)
    digest = hashlib.sha256(executable.read_bytes()).hexdigest()
    receipt = RECEIPT
    if phase == 'before':
        if os.path.lexists(target):
            raise ValueError('Adapter target already exists; refusing replacement')
        receipt.write_text(json.dumps({'source': str(source), 'resolved': str(executable),
                                      'version': version, 'sha256': digest}) + '\n')
    else:
        if not target.is_symlink() or os.readlink(target) != str(source):
            raise ValueError('Exact adapter link missing')
        if os.lstat(target).st_uid != 0:
            raise ValueError('Adapter link is not root-owned')
        # Exact link text above points directly to fully checked lexical source.
        trusted_chain(source)
        previous = json.loads(receipt.read_text())
        if previous != {'source': str(source), 'resolved': str(executable),
                        'version': version, 'sha256': digest}:
            raise ValueError('CMake identity changed during adapter operation')
    print('HOST_CMAKE_CONTRACT ' + phase + ' ' + json.dumps({
        'source': str(source), 'resolved': str(executable), 'version': version,
        'sha256': digest, 'root_owned_not_group_world_writable': True}))


if __name__ == '__main__':
    main()
