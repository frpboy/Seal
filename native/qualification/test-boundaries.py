#!/usr/bin/env python3
import copy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('metadata', HERE / 'verify-metadata.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

class Boundaries(unittest.TestCase):
    def test_terminal_status_and_stage(self):
        for stage, code in [('selection',0),('selection',7),('packaging',0),('packaging',124)]:
            with self.subTest(stage=stage,code=code), tempfile.TemporaryDirectory() as tmp:
                Path(tmp,'qualification-evidence').mkdir()
                env = dict(os.environ, SEALI_RECEIPT_FIXTURE='1', SEALI_FIXTURE_STAGE=stage, SEALI_FIXTURE_EXIT=str(code))
                r = subprocess.run(['bash', str(HERE / 'run-phases.sh')],cwd=tmp,env=env,capture_output=True,text=True)
                self.assertEqual(r.returncode,code,r.stderr)
                receipt=json.loads(Path(tmp,'qualification-evidence/gradle-terminal.json').read_text())
                self.assertEqual((receipt['stage'],receipt['exit_code']),(stage,code))
                self.assertTrue(receipt['cleanup'].startswith('UNKNOWN'))
    def test_apk_hash_binding(self):
        # Execute actual main via a private fixture root, no APK build.
        import shutil
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); script=root/'native/qualification/verify-metadata.py'
            script.parent.mkdir(parents=True);shutil.copyfile(HERE/'verify-metadata.py',script)
            out=root/'qualification-evidence';out.mkdir();apk=root/'fixture.apk';apk.write_bytes(b'fixture bytes')
            (out/'native-inventory.json').write_text(json.dumps({'artifact':str(apk),'sha256':hashlib.sha256(apk.read_bytes()).hexdigest()}))
            # Mismatch rejects before any metadata read.
            apk.write_bytes(b'changed fixture bytes')
            r=subprocess.run(['python3',str(script)],capture_output=True,text=True)
            self.assertNotEqual(r.returncode,0);self.assertIn('APK changed after native inspection',r.stderr)
    def test_packaged_metadata(self):
        exp={'package':'com.frpboy.seali.nativequalification','version_name':'2.0.0-alpha.6-native-qualification','version_code':200000162}
        bad="package: name='com.frpboy.seali.nativequalification' versionCode='200000162' versionName='2.0.0-alpha.6-native-qualification'\napplication-debuggable\n"
        meta={'applicationId':exp['package'],'variantName':'genericNativeQualification','elements':[{'type':'SINGLE','filters':[],'versionCode':exp['version_code'],'versionName':exp['version_name'],'outputFile':'fixture.apk'}]}
        self.assertTrue(m.verify(bad,meta,exp,'fixture.apk','ab')['debuggable'])
        variants=[]
        variants += [(bad.replace(exp['package'],'com.frpboy.seali'),meta)]
        variants += [(bad.replace('200000162','200000160'),meta)]
        variants += [(bad.replace(exp['version_name'],'2.0.0-alpha.6'),meta)]
        variants += [(bad.replace('application-debuggable',''),meta)]
        for key,val in [('filters',[{'filterType':'ABI','value':'arm64-v8a'}]),('outputFile','wrong.apk'),('versionCode',200000160),('type','ONE_OF_MANY')]:
            mm=copy.deepcopy(meta);mm['elements'][0][key]=val;variants.append((bad,mm))
        mm=copy.deepcopy(meta);mm['elements']*=2;variants.append((bad,mm))
        for b,mm in variants:
            with self.subTest(b=b,meta=mm),self.assertRaises(ValueError):m.verify(b,mm,exp,'fixture.apk','ab')

unittest.main()
