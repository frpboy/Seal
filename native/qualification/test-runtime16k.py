#!/usr/bin/env python3
"""Tiny mocked controller fixtures only. Never Android/runtime evidence."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock, patch
os.environ.setdefault('ANDROID_HOME','/nonexistent-fixture-sdk')
spec=importlib.util.spec_from_file_location('runtime16k',Path(__file__).with_name('runtime16k.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

class Controller(unittest.TestCase):
    def test_main_focus(self):
        self.assertTrue(m.focus_is_main('mResumedActivity: '+m.PKG+'/com.junkfood.seal.MainActivity',
                                       'mCurrentFocus='+m.PKG+'/com.junkfood.seal.MainActivity'))
    def test_crash_focus_rejected_same_process(self):
        self.assertFalse(m.focus_is_main('mResumedActivity: '+m.PKG+'/com.junkfood.seal.CrashReportActivity',
                                        'mCurrentFocus='+m.PKG+'/com.junkfood.seal.MainActivity'))
    def test_missing_focus_rejected(self):
        self.assertFalse(m.focus_is_main('',''))
    def test_hash_exact(self):
        self.assertEqual(m.verify_hash_output({'/fixture/a':'abc'},'abc  /fixture/a\n'),1)
    def test_hash_drift(self):
        with self.assertRaises(RuntimeError):m.verify_hash_output({'/fixture/a':'abc'},'def /fixture/a\n')
    def test_hash_duplicate(self):
        with self.assertRaises(RuntimeError):m.verify_hash_output({'/fixture/a':'abc'},'abc /fixture/a\nabc /fixture/a\n')
    def cleanup_fixture(self,emulator,fail=False):
        with tempfile.TemporaryDirectory() as d, patch.object(m,'OUT',Path(d)), patch.object(m,'emulator',emulator), patch.object(m,'report',{'status':'failure'}), patch.object(m,'adb',side_effect=RuntimeError('fixture') if fail else None):
            m.finish();return json.loads((Path(d)/'runtime16k.json').read_text())
    def test_cleanup_success_receipt(self):
        self.assertIn('owned emulator exited',self.cleanup_fixture(Mock())['cleanup'])
    def test_cleanup_failure_unknown_receipt(self):
        self.assertIn('UNKNOWN',self.cleanup_fixture(Mock(),True)['cleanup'])
    def test_no_emulator_receipt(self):
        self.assertEqual(self.cleanup_fixture(None)['status'],'failure')

if __name__=='__main__':unittest.main()
