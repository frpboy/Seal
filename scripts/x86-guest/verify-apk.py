#!/usr/bin/env python3
"""Byte-exact APK payload check before retaining any candidate for review."""
import pathlib,zipfile,json,hashlib,subprocess,os
root=pathlib.Path(os.environ['CUSTODY_ROOT']);out=root/'evidence';files=list(pathlib.Path('app/build/outputs/apk/generic/debug').glob('*.apk'));assert len(files)==1,files;apk=files[0]
payload=json.loads(pathlib.Path('functional-alpha-payload-manifest.json').read_text());platform=json.loads(pathlib.Path('functional-alpha-platform-jni.json').read_text());rows=[]
with zipfile.ZipFile(apk) as z:
 assert len(z.namelist())==len(set(z.namelist())) and z.testzip() is None
 native={n for n in z.namelist() if n.startswith('lib/')}
 want={'lib/x86_64/'+pathlib.Path(r['path']).name:r['sha256'] for r in payload['files']}
 want.update({r['apk_entry']:r['sha256'] for r in platform})
 assert native==set(want),(native,set(want))
 for name,h in want.items():
  got=hashlib.sha256(z.read(name)).hexdigest();assert got==h,(name,got,h);rows.append({'path':name,'sha256':got})
 for name in ['assets/functional-engine/yt-dlp','res/raw/ytdlp']:
  assert hashlib.sha256(z.read(name)).hexdigest()=='e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3',name
bt=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
badging=subprocess.check_output([str(bt/'aapt'),'dump','badging',str(apk)],text=True)
assert "package: name='com.frpboy.seali.debug' versionCode='200000164' versionName='2.0.0-alpha.6-debug'" in badging,badging[:300]
manifest=subprocess.check_output([str(bt/'aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True)
assert 'extractNativeLibs(0x010104ea)=(type 0x12)0xffffffff' in manifest
assert 'debuggable(0x0101000f)=(type 0x12)0xffffffff' in manifest
cert=subprocess.check_output([str(bt/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
(out/'apk-badging.txt').write_text(badging);(out/'apk-manifest.txt').write_text(manifest);(out/'apk-debug-cert.txt').write_text(cert)
(out/'apk-payload-check.json').write_text(json.dumps({'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'bytes':apk.stat().st_size,'native_byte_exact':rows,'engine_raw_and_asset_sha256':'e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3','identity':'com.frpboy.seali.debug / 200000164 / 2.0.0-alpha.6-debug','target_runtime_tested':False,'public_release_ready':False},indent=2))

import re
certs=re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)$',cert,re.M)
assert len(certs)==1,cert
tests=list(pathlib.Path('app/build/outputs/apk/androidTest/generic/debug').glob('*.apk'));assert len(tests)==1,tests;test=tests[0]
with zipfile.ZipFile(test) as z:
 assert len(z.namelist())==len(set(z.namelist())) and z.testzip() is None
test_badging=subprocess.check_output([str(bt/'aapt'),'dump','badging',str(test)],text=True)
assert "package: name='com.frpboy.seali.debug.test'" in test_badging
test_manifest=subprocess.check_output([str(bt/'aapt'),'dump','xmltree',str(test),'AndroidManifest.xml'],text=True)
instrumentation_blocks=[]
lines=test_manifest.splitlines()
for i,line in enumerate(lines):
 if line.strip().startswith('E: instrumentation '):
  indent=len(line)-len(line.lstrip());block=[]
  for child in lines[i+1:]:
   if child.strip() and len(child)-len(child.lstrip())<=indent:break
   block.append(child)
  instrumentation_blocks.append('\n'.join(block))
assert len(instrumentation_blocks)==1,test_manifest
instrumentation=instrumentation_blocks[0]
assert re.search(r'A: android:name\([^)]*\)="androidx\.test\.runner\.AndroidJUnitRunner"',instrumentation),instrumentation
assert re.search(r'A: android:targetPackage\([^)]*\)="com\.frpboy\.seali\.debug"',instrumentation),instrumentation
assert 'debuggable(0x0101000f)=(type 0x12)0xffffffff' in test_manifest
test_cert=subprocess.check_output([str(bt/'apksigner'),'verify','--verbose','--print-certs',str(test)],text=True)
test_certs=re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)$',test_cert,re.M)
assert len(test_certs)==1 and test_certs==certs,(certs,test_certs)
(out/'test-apk-badging.txt').write_text(test_badging);(out/'test-apk-manifest.txt').write_text(test_manifest);(out/'test-apk-cert.txt').write_text(test_cert)
(out/'app-test-pair-check.json').write_text(json.dumps({'app_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'test_sha256':hashlib.sha256(test.read_bytes()).hexdigest(),'exactly_one_apk_each':True,'both_crc_unique_zip_entries':True,'both_debuggable':True,'one_signer_each_equal_sha256':certs[0],'test_package':'com.frpboy.seali.debug.test','target_package':'com.frpboy.seali.debug','runner':'androidx.test.runner.AndroidJUnitRunner','instrumentation_executed':False},indent=2))
