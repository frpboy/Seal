#!/usr/bin/env python3
"""Read-only producer input checks, including exact path sets and modes."""
import hashlib
import json
import os
import platform
import shutil
import stat
import subprocess
import zipfile
from pathlib import Path

NDK_SHA256 = 'dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f'
NDK_BYTES = 722261334
STAGE_LOCK_SHA256 = 'a7ea11bc136c397017b6048d96c261dda66c59ac92ba6245cae89e3993c0a3c0'


def require(ok, why):
    if not ok:
        raise ValueError(why)


def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def inventory(root):
    rows = []
    for path in sorted(root.rglob('*')):
        row = {'path': str(path.relative_to(root)), 'mode': path.lstat().st_mode & 0o777}
        if path.is_symlink():
            row.update(kind='symlink', target=os.readlink(path))
        elif path.is_dir():
            row['kind'] = 'directory'
        elif path.is_file():
            row.update(kind='file', bytes=path.stat().st_size, sha256=sha(path))
        else:
            raise ValueError('special tree input: ' + str(path))
        rows.append(row)
    return rows


def verify_manifest(root, rows):
    require(root.is_dir() and not root.is_symlink(), 'regular tree root required')
    actual = inventory(root)
    require(actual == rows, 'tree path set, mode, link or content differs: ' + str(root))
    return len(rows)


def verify_ndk(root, archive, expected_hash=NDK_SHA256, expected_size=NDK_BYTES):
    require(archive.stat().st_size == expected_size and sha(archive) == expected_hash, 'complete NDK archive differs')
    expected = set()
    regular_count = 0
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            parts = item.filename.split('/', 1)
            require(len(parts) == 2 and parts[0] == 'android-ndk-r28c', 'unexpected NDK root')
            rel = parts[1].rstrip('/')
            if not rel:
                continue
            require(rel not in expected and all(p not in ('', '.', '..') for p in rel.split('/')), 'duplicate or unsafe NDK path')
            expected.add(rel)
            path = root / rel
            mode = item.external_attr >> 16
            require((path.lstat().st_mode & 0o777) == (mode & 0o777), 'NDK execution/permission mode differs: ' + rel)
            if stat.S_ISLNK(mode):
                require(path.is_symlink() and os.readlink(path).encode() == z.read(item), 'NDK symlink differs: ' + rel)
            elif item.is_dir():
                require(path.is_dir() and not path.is_symlink(), 'NDK directory differs: ' + rel)
            else:
                require(path.is_file() and not path.is_symlink(), 'NDK regular file missing: ' + rel)
                require(path.stat().st_size == item.file_size and sha(path) == hashlib.sha256(z.read(item)).hexdigest(), 'NDK bytes differ: ' + rel)
                regular_count += 1
    actual = {str(p.relative_to(root)) for p in root.rglob('*')}
    require(actual == expected, 'NDK tree path set differs; extra=' + repr(sorted(actual - expected)[:4]))
    return {'entries': len(expected), 'regular_files': regular_count}


def verify_stage(root, lock):
    require(sha(lock) == STAGE_LOCK_SHA256, 'qualified full-stage lock differs')
    data = json.loads(lock.read_text())
    return verify_manifest(root, data['files'])


def host_tools():
    tools = {}
    names = ['bash', 'make', 'gcc', 'g++', 'ar', 'ld', 'perl', 'cmake', 'patch', 'sed',
             'tar', 'pkg-config', 'gzip', 'bzip2', 'xz', 'find', 'uname', 'env', 'cp',
             'ln', 'timeout', 'readlink', 'python3', 'unshare']
    for name in names:
        path = shutil.which(name, path='/usr/bin:/bin')
        require(path is not None, 'host tool absent: ' + name)
        result = subprocess.run([path, '--version'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                text=True, timeout=10, env={'PATH': '/usr/bin:/bin', 'LANG': 'C.UTF-8'})
        tools[name] = {'path': path, 'resolved_path': str(Path(path).resolve()), 'sha256': sha(Path(path)),
                       'version_exit': result.returncode, 'version': result.stdout[:8192]}
    return {'tools': tools, 'os_release': Path('/etc/os-release').read_text(), 'uname': list(platform.uname())}


def network_probe():
    parent = os.readlink('/proc/self/ns/net')
    code = """import errno,fcntl,json,os,socket,struct
interfaces=[]
for index,name in socket.if_nameindex():
    with socket.socket(socket.AF_INET,socket.SOCK_DGRAM) as s:
        flags=struct.unpack('H',fcntl.ioctl(s.fileno(),0x8913,struct.pack('256s',name.encode()))[16:18])[0]
    interfaces.append({'index':index,'name':name,'flags':flags,'up':bool(flags&1)})
connections=[]
for family,address in [(socket.AF_INET,('198.51.100.1',9)),(socket.AF_INET6,('2001:db8::1',9,0,0))]:
    with socket.socket(family,socket.SOCK_STREAM) as s:
        s.settimeout(1)
        connections.append({'family':int(family),'connect_errno':s.connect_ex(address)})
print(json.dumps({'namespace':os.readlink('/proc/self/ns/net'),'interfaces':interfaces,'routes':open('/proc/net/route').read(),'ipv6_routes':open('/proc/net/ipv6_route').read(),'connections':connections}))
"""
    result = subprocess.run(['/usr/bin/unshare', '-Urn', '--', '/usr/bin/python3', '-c', code],
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=10,
                            env={'PATH': '/usr/bin:/bin', 'LANG': 'C.UTF-8'})
    if result.returncode:
        return {'available': False, 'exit_code': result.returncode, 'error': result.stderr}
    data = json.loads(result.stdout)
    available = (data['namespace'] != parent and not any(row['up'] for row in data['interfaces'])
                 and all(row['connect_errno'] in (101, 99) for row in data['connections']))
    return {'available': available, 'parent_namespace': parent, 'probe': data,
            'command_prefix': ['/usr/bin/unshare', '-Urn', '--'],
            'limits': 'Network namespace isolation only. Not a hard RAM/cgroup or descendant lifetime boundary; detached sessions can escape process-group cleanup.'}
