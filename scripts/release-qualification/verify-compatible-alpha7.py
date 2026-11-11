#!/usr/bin/env python3
"""Source-only alpha7 verifier. No install, network, signing or mutation of APK bytes."""
import hashlib, json, os, pathlib, re, subprocess, zipfile
root = pathlib.Path(os.environ['CUSTODY_ROOT'])
out = root / 'evidence'
out.mkdir(exist_ok=True)
apks = list(pathlib.Path('app/build/outputs/apk/generic/release').glob('*.apk'))
assert len(apks) == 1, 'expected exactly one ARM64 generic release APK'
apk = apks[0]
bt = pathlib.Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
badging = subprocess.check_output([str(bt/'aapt'), 'dump', 'badging', str(apk)], text=True)
assert "package: name='com.frpboy.seali' versionCode='200000172' versionName='2.0.0-alpha.7'" in badging
assert re.findall(r"^native-code: (.+)$", badging, re.M) == ["'arm64-v8a'"]
assert "sdkVersion:'24'" in badging and "targetSdkVersion:'36'" in badging
assert 'application-debuggable' not in badging
manifest = subprocess.check_output([str(bt/'aapt'), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
assert 'extractNativeLibs(0x010104ea)=(type 0x12)0xffffffff' in manifest
assert 'debuggable(0x0101000f)=(type 0x12)0xffffffff' not in manifest
cert = subprocess.check_output([str(bt/'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], text=True)
assert re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)$', cert, re.M) == ['88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f']
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in cert
subprocess.run([str(bt/'zipalign'), '-c', '-P', '16', '4', str(apk)], check=True, stdout=subprocess.DEVNULL)
# X7's final native integration must extend this actual payload manifest with libqjs.so.
payload = json.loads(pathlib.Path('functional-alpha-payload-manifest.json').read_text())
platform = json.loads(pathlib.Path('functional-alpha-platform-jni.json').read_text())
want = {'lib/arm64-v8a/'+pathlib.Path(r['path']).name:r['sha256'] for r in payload['files']}
want.update({r['apk_entry']:r['sha256'] for r in platform})
assert 'lib/arm64-v8a/libqjs.so' in want, 'real reviewed QuickJS payload required; version bump is not a fix'
with zipfile.ZipFile(apk) as z:
    assert len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None
    assert {n for n in z.namelist() if n.startswith('lib/')} == set(want)
    for name, expected in want.items():
        assert hashlib.sha256(z.read(name)).hexdigest() == expected, name
    # Release resource optimization may rename the ZIP member. Resolve the exact
    # default raw/ytdlp resource through the APK table, never by scanning for a hash.
    resources = subprocess.check_output([str(bt/'aapt2'), 'dump', 'resources', str(apk)], text=True)
    matches = list(re.finditer(r'^    resource (0x[0-9a-fA-F]+) raw/ytdlp\s*\n((?:      .*\n?)*)', resources, re.M))
    assert len(matches) == 1, 'exactly one raw/ytdlp resource required'
    raw_id = int(matches[0][1], 16)
    assert raw_id >> 24 == 0x7f, 'application resource ID required'
    paths = re.findall(r'^      \(\) \(file\) (\S+)\s*$', matches[0][2], re.M)
    assert len(paths) == 1 and len(matches[0][2].strip().splitlines()) == 1, 'one default raw engine, no alternate configs'
    raw_path = paths[0]
    assert raw_path.startswith('res/') and '..' not in pathlib.PurePosixPath(raw_path).parts
    assert raw_path != 'assets/functional-engine/yt-dlp'
    for name in ['assets/functional-engine/yt-dlp', raw_path]:
        assert hashlib.sha256(z.read(name)).hexdigest() == 'e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3'
(out/'alpha7-resource-table.txt').write_text(resources)
for name, text in [('alpha7-badging.txt',badging),('alpha7-manifest.txt',manifest),('alpha7-cert.txt',cert)]:
    (out/name).write_text(text)
(out/'alpha7-static-check.json').write_text(json.dumps({'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'bytes':apk.stat().st_size,'package':'com.frpboy.seali','name':'2.0.0-alpha.7','code':200000172,'signer_sha256':'88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f','native_sha256':want,'raw_engine_resource_id':hex(raw_id),'raw_engine_zip_path':raw_path,'runtime_tested':False,'upgrade_tested':False,'download_tested':False,'public_release_ready':False},indent=2)+'\n')
