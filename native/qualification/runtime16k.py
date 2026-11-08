#!/usr/bin/env python3
"""Disposable x86_64 16KiB app-UID codec smoke, not release qualification."""
import atexit
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import time

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'qualification-evidence'
SDK = Path(os.environ['ANDROID_HOME'])
ADB = str(SDK / 'platform-tools/adb')
SERIAL = 'emulator-5554'
PKG = 'com.frpboy.seali.nativequalification'
report = {'status': 'failure', 'abi_scope': 'x86_64 emulator ONLY',
          'release_ready': False, 'cleanup': 'UNKNOWN until emulator stop verified',
          'ffmpeg_scope': 'app-extracted FFmpeg under actual app UID; not download UI wiring'}
emulator = None


def run(args, seconds=45, data=None):
    p = subprocess.run([str(x) for x in args], input=data, capture_output=True, timeout=seconds)
    if p.returncode:
        raise RuntimeError('Command failed: ' + str(args[0]) + ' exit=' + str(p.returncode)
                           + ' stderr=' + p.stderr.decode(errors='replace')[-1800:])
    return p.stdout


def adb(*args, seconds=45, data=None):
    return run([ADB, '-s', SERIAL, *args], seconds, data)


def shell(*args, seconds=45, data=None):
    return adb('shell', shlex.join([str(x) for x in args]), seconds=seconds, data=data)


def app(*args, **kwargs):
    return shell('run-as', PKG, *args, **kwargs)


def finish():
    if emulator is not None:
        try:
            adb('emu', 'kill', seconds=10)
            emulator.wait(timeout=20)
            report['cleanup'] = 'owned emulator exited; detached descendants UNKNOWN'
        except Exception:
            report['cleanup'] = 'UNKNOWN: emulator stop unverified'
    (OUT / 'runtime16k.json').write_text(json.dumps(report, indent=2) + '\n')


atexit.register(finish)


def main():
    global emulator
    if not (os.access('/dev/kvm', os.R_OK) and os.access('/dev/kvm', os.W_OK)):
        raise RuntimeError('KVM unavailable; no permission modification or fallback allowed')
    assert not [x for x in run([ADB, 'devices']).decode().splitlines()[1:] if x.strip()], 'unexpected existing adb devices'
    image = 'system-images;android-35;google_apis_ps16k;x86_64'
    avdmanager = SDK / 'cmdline-tools/latest/bin/avdmanager'
    run([avdmanager, 'create', 'avd', '-n', 'seali16k', '-k', image, '--device', 'pixel_2'], data=b'no\n')
    work = Path(tempfile.mkdtemp(prefix='seali-runtime16k-'))
    emulator = subprocess.Popen([str(SDK/'emulator/emulator'), '-avd', 'seali16k', '-port', '5554',
        '-no-window', '-no-audio', '-no-snapshot', '-wipe-data', '-memory', '3072',
        '-gpu', 'swiftshader_indirect', '-no-boot-anim'],
        stdout=(work/'emulator.log').open('wb'), stderr=subprocess.STDOUT)
    deadline = time.monotonic() + 240
    while time.monotonic() < deadline:
        if emulator.poll() is not None:
            raise RuntimeError('Emulator exited before boot')
        try:
            if shell('getprop', 'sys.boot_completed', seconds=5).strip() == b'1':
                break
        except Exception:
            pass
        time.sleep(3)
    else:
        raise RuntimeError('Boot deadline exceeded')
    assert shell('getconf', 'PAGE_SIZE').strip() == b'16384', 'not actual16KiB kernel'
    assert shell('getprop', 'ro.product.cpu.abi').strip() == b'x86_64'
    adb('root')
    adb('wait-for-device', seconds=20)
    assert shell('id', '-u').strip() == b'0', 'owned emulator root identity required'
    # Only owned disposable guest policies: prohibit IPv4/IPv6 egress before app.
    # Host KVM policy is never modified. ADB uses the emulator transport.
    for firewall in ['iptables', 'ip6tables']:
        shell(firewall, '-P', 'OUTPUT', 'DROP')
        rules=shell(firewall, '-S', 'OUTPUT').decode().splitlines()
        assert rules == ['-P OUTPUT DROP'], 'unexpected guest egress rules'
    report['network_isolation']='guest IPv4 and IPv6 OUTPUT DROP, no allow rules'
    # Disable documented page-size compatibility, never rely on fallback loading.
    shell('setprop', 'bionic.linker.16kb.app_compat.enabled', 'false')
    shell('setprop', 'pm.16kb.app_compat.disabled', 'true')
    assert shell('getprop', 'bionic.linker.16kb.app_compat.enabled').strip() == b'false'
    assert shell('getprop', 'pm.16kb.app_compat.disabled').strip() == b'true'
    properties=(SDK/'system-images/android-35/google_apis_ps16k/x86_64/source.properties').read_text()
    assert re.search(r'^Pkg.Revision\s*=\s*5\s*$', properties, re.M), 'image revision drift'
    report.update(page_size=16384, image=image, image_source_properties=properties, page_size_compat='disabled and read back',
                  fingerprint=shell('getprop', 'ro.build.fingerprint').decode().strip())
    identity = json.loads((OUT/'verified-identity.json').read_text())
    unsigned = next((ROOT/'app/build/outputs/apk/generic/nativeQualification').glob('*.apk'))
    assert hashlib.sha256(unsigned.read_bytes()).hexdigest() == identity['sha256']
    # Fixed disposable fixture credentials, no production/vault key or continuity claim.
    key = work/'fixture.jks'
    run(['keytool', '-genkeypair', '-keystore', key, '-storepass', 'fixture-only', '-keypass', 'fixture-only',
         '-alias', 'fixture', '-dname', 'CN=Disposable Seali runtime fixture', '-keyalg', 'RSA',
         '-keysize', '2048', '-validity', '2', '-noprompt'])
    signed = work/'runtime-only.apk'
    signer = SDK/'build-tools/36.0.0/apksigner'
    run([signer, 'sign', '--ks', key, '--ks-pass', 'pass:fixture-only', '--key-pass', 'pass:fixture-only',
         '--out', signed, unsigned])
    cert = run([signer, 'verify', '--print-certs', signed]).decode()
    report.update(unsigned_sha256=identity['sha256'], runtime_signed_sha256=hashlib.sha256(signed.read_bytes()).hexdigest(),
                  certificate_sha256=re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', cert)[1],
                  signing='disposable fixture only; cannot update user installation')
    adb('install', '--no-streaming', str(signed), seconds=120)
    launch = shell('am', 'start', '-W', '-n', PKG+'/com.junkfood.seal.MainActivity').decode()
    assert 'Error' not in launch and 'Status: ok' in launch, launch
    assert shell('pidof', PKG).strip(), 'app process absent'
    package = shell('dumpsys', 'package', PKG).decode()
    assert 'primaryCpuAbi=x86_64' in package
    lib = re.search(r'\bnativeLibraryDir=(\S+)', package)[1]
    uid = app('id', '-u').decode().strip()
    codecdir = '/data/user/0/'+PKG+'/no_backup/youtubedl-android/packages/ffmpeg/usr/lib'
    deadline = time.monotonic()+45
    while time.monotonic()<deadline:
        try:
            app('test', '-f', codecdir+'/libwebp.so');break
        except Exception:
            time.sleep(1)
    else:
        raise RuntimeError('App initialization did not extract native codec payload')
    pixels = bytes((i*17+31)%256 for i in range(16*16*3))
    app('mkdir', '-p', 'files/runtime16k')
    app('sh', '-c', 'cat > files/runtime16k/input.rgb', data=pixels)
    def ffmpeg(*args):
        return app('env', 'LD_LIBRARY_PATH='+codecdir+':'+lib, lib+'/libffmpeg.so', '-nostdin', '-v', 'error', '-y', *args, seconds=60)
    ffmpeg('-f','rawvideo','-pixel_format','rgb24','-video_size','16x16','-i','files/runtime16k/input.rgb',
           '-frames:v','1','-c:v','libwebp','-lossless','1','files/runtime16k/output.webp')
    ffmpeg('-i','files/runtime16k/output.webp','-frames:v','1','-f','rawvideo','-pix_fmt','rgb24','files/runtime16k/decoded.rgb')
    decoded = app('cat','files/runtime16k/decoded.rgb')
    assert decoded == pixels, 'losslessWebP pixel roundtrip mismatch'
    encoded = app('cat','files/runtime16k/output.webp')
    assert encoded[:4]==b'RIFF' and encoded[8:12]==b'WEBP'
    report.update(app_uid=uid, app_launch='am start statusok + process present',
        actual_native_dir=lib, webp_roundtrip='16x16 RGB lossless encode/decode EXACT under appUID',
        input_sha256=hashlib.sha256(pixels).hexdigest(), decoded_sha256=hashlib.sha256(decoded).hexdigest(),
        webp_sha256=hashlib.sha256(encoded).hexdigest())
    # Pure emulator fixture pixels only. Parent must inspect these actual pixels.
    (OUT/'runtime16k.png').write_bytes(adb('exec-out','screencap','-p'))
    assert shell('pidof', PKG).strip(), 'app exited after codec operation'
    report['status']='PASS narrow x86_64 16KiB launch and appUID WebP roundtrip only'


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        report['error']=str(error)[-2200:]
        raise
