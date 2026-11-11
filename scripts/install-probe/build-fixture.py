#!/usr/bin/env python3
"""Build harmless same-byte-count install fixture; no Seali code/data/product signer."""
import pathlib,os,subprocess,zipfile,hashlib,json,re,time
root=pathlib.Path(os.environ['RUNNER_TEMP'])/'seali-sdk-diagnostics';work=root/'fixture-private';assert not work.exists();work.mkdir(mode=0o700);e=root/'evidence';sdk=pathlib.Path(os.environ['ANDROID_HOME']);tools=sdk/'build-tools/36.0.0';deadline=time.monotonic()+150
target=57361877;package='com.frpboy.syntheticinstallprobe';receipt={'package':package,'target_final_bytes':target,'attempts':[],'scope':'synthetic no-code no-permissions no-components fixture; not Seali or product signer'}
def run(args,name=None):
 remaining=deadline-time.monotonic();assert remaining>0
 try:r=subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=min(30,remaining))
 except subprocess.TimeoutExpired as error:
  if name:(e/name).write_bytes((error.stdout or b'')[-65536:])
  raise
 if name:(e/name).write_bytes(r.stdout[-65536:])
 assert r.returncode==0,'fixture command failed '+str(args[0]);return r.stdout
try:
 manifest=work/'AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'+package+'" android:versionCode="1" android:versionName="1"><uses-sdk android:minSdkVersion="24" android:targetSdkVersion="34"/><application android:hasCode="false" android:allowBackup="false" android:label="Synthetic install probe"/></manifest>')
 (e/'synthetic-manifest.xml').write_bytes(manifest.read_bytes())
 run([tools/'aapt2','link','-o',work/'base.apk','-I',sdk/'platforms/android-34/android.jar','--manifest',manifest],name='fixture-link.txt')
 # Fixture-only disposable key, public conventional debug password. No login/production secret.
 run(['keytool','-genkeypair','-keystore',work/'fixture.jks','-storepass','android','-keypass','android','-alias','fixture','-keyalg','RSA','-keysize','2048','-validity','1','-dname','CN=Synthetic Install Fixture'],name='fixture-keygen-result.txt')
 # Stored deterministic pseudo-random bytes, capped57.4MB. Never upload asset/APK/key.
 chunk=b''.join(hashlib.sha256(b'seali-synthetic-install-fixture-v1'+i.to_bytes(4,'big')).digest() for i in range(32768))
 asset_length=target-16384;name_padding=0
 for attempt in range(4):
  assert 0<asset_length<=target and 0<=name_padding<=4095
  unsigned=work/'unsigned.apk'
  with zipfile.ZipFile(work/'base.apk') as source,zipfile.ZipFile(unsigned,'w',compression=zipfile.ZIP_STORED) as z:
   for n in source.namelist():z.writestr(n,source.read(n))
   info=zipfile.ZipInfo('assets/'+('p'*name_padding)+'padding.bin');info.compress_type=zipfile.ZIP_STORED
   with z.open(info,'w') as f:
    left=asset_length
    while left:n=min(left,len(chunk));f.write(chunk[:n]);left-=n

  run([tools/'zipalign','-f','4',unsigned,work/'aligned.apk'])
  run([tools/'apksigner','sign','--ks',work/'fixture.jks','--ks-key-alias','fixture','--ks-pass','pass:android','--key-pass','pass:android','--v1-signing-enabled','false','--v2-signing-enabled','true','--v3-signing-enabled','false','--v4-signing-enabled','false','--out',work/'fixture.apk',work/'aligned.apk'])
  size=(work/'fixture.apk').stat().st_size;delta=target-size;receipt['attempts'].append({'asset_bytes':asset_length,'asset_filename_padding_bytes':name_padding,'actual_signed_bytes':size});(e/'fixture-iterations.json').write_text(json.dumps(receipt,indent=2))
  if delta==0:break
  # apksigner strips ZIP comments and page-pads its signing block. The central
  # directory's filename length controls the final byte residue without modifying
  # signed bytes; adjust asset for whole-page change, filename for residue.
  residue=delta%4096
  name_padding+=residue
  asset_length+=delta-residue-4096 if residue else delta
 else:raise RuntimeError('bounded exact signed-size construction failed; no install')
 fixture=work/'fixture.apk';assert fixture.stat().st_size==target
 run([tools/'apksigner','verify','--verbose','--print-certs',fixture],name='fixture-signature.txt')
 run([tools/'zipalign','-c','-v','4',fixture],name='fixture-alignment.txt')
 badging=run([tools/'aapt2','dump','badging',fixture],name='fixture-badging.txt').decode();assert "package: name='"+package+"' versionCode='1' versionName='1'" in badging
 xml=run([tools/'aapt2','dump','xmltree',fixture,'--file','AndroidManifest.xml'],name='fixture-binary-manifest.txt').decode()
 assert re.search(r'android:hasCode[^\n]*(?:false|0x0\b)',xml),'hasCode false not verified'
 assert not re.search(r'\bE: (?:activity|activity-alias|service|receiver|provider|uses-permission|uses-permission-sdk-23|instrumentation)\b',xml)
 with zipfile.ZipFile(fixture) as z:
  assert set(z.namelist())<={'AndroidManifest.xml','resources.arsc','assets/'+('p'*name_padding)+'padding.bin'}
  assert 'assets/'+('p'*name_padding)+'padding.bin' in z.namelist();assert not any(n.endswith('.dex') or n.startswith('lib/') for n in z.namelist())
 receipt.update({'actual_final_bytes':fixture.stat().st_size,'actual_sha256':hashlib.sha256(fixture.read_bytes()).hexdigest(),'manifest_package_version_verified':True,'no_components_permissions_dex_native_verified':True,'signature_alignment_verified':True,'fixture_apk_not_uploaded':True,'fixture_key_not_uploaded':True})
 (e/'fixture-receipt.json').write_text(json.dumps(receipt,indent=2))
except BaseException as error:
 receipt['error_type']=type(error).__name__;raise
finally:
 (e/'fixture-receipt.json').write_text(json.dumps(receipt,indent=2))
