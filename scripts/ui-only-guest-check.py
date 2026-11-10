#!/usr/bin/env python3
"""Check real APK inventory and manifest before installation. No APK/key log output."""
import hashlib,json,os,pathlib,re,subprocess,zipfile
root=pathlib.Path('.')
apps=list(root.glob('app/build/outputs/apk/uiOnly/debug/*.apk'))
tests=list(root.glob('app/build/outputs/apk/androidTest/uiOnly/debug/*.apk'))
assert len(apps)==1 and len(tests)==1, 'Exactly one universal app and test APK required'
aapt=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0/aapt'
cert=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0/apksigner'
app=apps[0]
manifest=subprocess.check_output([str(aapt),'dump','xmltree',str(app),'AndroidManifest.xml'],text=True)
badging=subprocess.check_output([str(aapt),'dump','badging',str(app)],text=True)
assert "package: name='com.frpboy.seali.uipreview'" in badging
assert "application-label:'Seali UI Preview'" in badging
assert "launchable-activity: name='com.frpboy.seali.UiPreviewActivity'" in badging
assert 'com.frpboy.seali.UiPreviewApplication' in manifest
assert 'android.permission.INTERNET' not in manifest
assert not re.search(r'android\.permission\.(?:READ_|WRITE_|MANAGE_|REQUEST_INSTALL)',manifest)
assert not re.search(r'E: (?:service|receiver|provider)\b',manifest)
assert 'QuickDownloadActivity' not in manifest and 'com.frpboy.seali.MainActivity' not in manifest
with zipfile.ZipFile(app) as z:
 assert z.testzip() is None
 entries=z.namelist()
 forbidden=re.compile(r'(?:libpython|libffmpeg|libffprobe|libaria2|libavcodec|libavformat|libavutil|res/raw/ytdlp)',re.I)
 assert not any(forbidden.search(n) for n in entries)
 libs=[n for n in entries if n.startswith('lib/') and n.endswith('.so')]
 assert libs and all(pathlib.PurePosixPath(n).name in {'libmmkv.so','libandroidx.graphics.path.so'} for n in libs),libs
 assert {n.split('/')[1] for n in libs}=={'arm64-v8a','x86_64'},libs
print('UI_APK_IDENTITY',json.dumps({'app':str(app),'bytes':app.stat().st_size,'sha256':hashlib.sha256(app.read_bytes()).hexdigest(),'native_entries':libs,'test_sha256':hashlib.sha256(tests[0].read_bytes()).hexdigest()}))
print(subprocess.check_output([str(cert),'verify','--print-certs',str(app)],text=True))
print('UI_STATIC_GATE=PASS')
