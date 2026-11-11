#!/usr/bin/env python3
# Synthetic parser/guard fixtures ONLY. No Android build, native execution, real signer or runtime proof.
import hashlib,json,os,pathlib,runpy,tempfile,unittest,zipfile
from unittest.mock import patch
SCRIPT=pathlib.Path(__file__).with_name('verify-compatible-alpha7.py')
CERT='88774e5211b61f7827db0ba6991a668000ceab9a9e9490648054b8a19b6e070f'
class Guards(unittest.TestCase):
 def fixture(self,change):
  with tempfile.TemporaryDirectory() as d:
   old=os.getcwd();os.chdir(d)
   try:
    p=pathlib.Path('app/build/outputs/apk/generic/release');p.mkdir(parents=True)
    data={'lib/arm64-v8a/libqjs.so':b'qjs','assets/functional-engine/yt-dlp':b'engine','res/raw/ytdlp':b'engine'}
    if change=='renamed':data['res/Nc']=data.pop('res/raw/ytdlp')
    if change=='wrongraw':data['res/raw/ytdlp']=b'wrong'
    if change=='missingraw':data.pop('res/raw/ytdlp')
    if change=='extra':data['lib/x86/libqjs.so']=b'other'
    if change=='noqjs':data.pop('lib/arm64-v8a/libqjs.so')
    with zipfile.ZipFile(p/'fixture.apk','w') as z:
     for n,v in data.items():z.writestr(n,v)
    pathlib.Path('functional-alpha-payload-manifest.json').write_text(json.dumps({'files':[{'path':'libqjs.so','sha256':hashlib.sha256(b'qjs').hexdigest()}] if change!='noqjs' else []}))
    pathlib.Path('functional-alpha-platform-jni.json').write_text('[]')
    bad="package: name='com.frpboy.seali' versionCode='200000172' versionName='2.0.0-alpha.7'\nsdkVersion:'24'\ntargetSdkVersion:'36'\nnative-code: 'arm64-v8a'"
    if change=='oldcode':bad=bad.replace('200000172','200000162')
    if change=='debug':bad=bad.replace("name='com.frpboy.seali'","name='com.frpboy.seali.debug'")
    manifest='extractNativeLibs(0x010104ea)=(type 0x12)0xffffffff'
    cert='Signer #1 certificate SHA-256 digest: '+(CERT if change!='wrongcert' else '0'*64)+'\nVerified using v2 scheme (APK Signature Scheme v2): true'
    resources='  type raw id=0f entryCount=1\n    resource 0x7f0f0000 raw/ytdlp\n      () (file) '+('res/Nc' if change=='renamed' else 'res/raw/ytdlp')+'\n'
    if change=='duplicateraw':resources+=resources
    if change=='alternate':resources+='      (en) (file) res/other\n'
    if change=='noresource':resources=''
    def output(args,**kw):return resources if args[0].endswith('aapt2') else cert if args[0].endswith('apksigner') else manifest if 'xmltree' in args else bad
    # The retained realengine pin is replaced only in this in-memory fixturecode, never in production verifier.
    code=SCRIPT.read_text().replace('e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3',hashlib.sha256(b'engine').hexdigest())
    with patch.dict(os.environ,{'CUSTODY_ROOT':d,'ANDROID_HOME':'/fake'}),patch('subprocess.check_output',side_effect=output),patch('subprocess.run'):
     exec(compile(code,str(SCRIPT),'exec'),{})
    return json.loads(pathlib.Path('evidence/alpha7-static-check.json').read_text())
   finally:os.chdir(old)
 def test_positive_static_only(self):
  r=self.fixture('ok');self.assertFalse(r['runtime_tested']);self.assertFalse(r['upgrade_tested']);self.assertFalse(r['public_release_ready'])
 def test_optimized_raw_path(self):
  self.assertEqual(self.fixture('renamed')['raw_engine_zip_path'],'res/Nc')
 def test_rejects_wrong_identities_and_missing_payload(self):
  for c in ['oldcode','debug','wrongcert','noqjs','extra','wrongraw','duplicateraw','alternate','noresource']:
   with self.subTest(c=c),self.assertRaises(AssertionError):self.fixture(c)
if __name__=='__main__':unittest.main()
