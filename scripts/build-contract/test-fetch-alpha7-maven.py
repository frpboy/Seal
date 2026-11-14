#!/usr/bin/env python3
"""Offline contract tests for fetch-alpha7-maven.py. No network."""
import hashlib, importlib.util, io, json, pathlib, tempfile, unittest

spec = importlib.util.spec_from_file_location('fetch_alpha7_maven', pathlib.Path(__file__).with_name('fetch-alpha7-maven.py'))
f = importlib.util.module_from_spec(spec); spec.loader.exec_module(f)

LOCK = {'artifacts': [{'module': 'common', 'coordinate': 'g:common:v', 'aar': '.qualification/maven/c.aar',
                       'files': {'.qualification/maven/c.aar': {'bytes': 4, 'sha256': hashlib.sha256(b'good').hexdigest()}}}]}

class FakeResponse(io.BytesIO):
    def __enter__(self): return self
    def __exit__(self, *a): return False

class Contract(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.root = pathlib.Path(self.tmp.name)
    def tearDown(self): self.tmp.cleanup()
    def run_fetch(self, lock, payload):
        orig = f.urllib.request.urlopen
        f.urllib.request.urlopen = lambda req, timeout=0: FakeResponse(payload)
        try: return f.fetch(self.root, lock)
        finally: f.urllib.request.urlopen = orig
    def test_exact_bytes_stage(self):
        out = self.run_fetch(LOCK, b'good')
        self.assertEqual(out['staged'], 1)
        self.assertEqual((self.root/'.qualification/maven/c.aar').read_bytes(), b'good')
    def test_tampered_bytes_rejected_and_not_written(self):
        with self.assertRaises(ValueError): self.run_fetch(LOCK, b'evil')
        self.assertFalse((self.root/'.qualification/maven/c.aar').exists())
    def test_no_overwrite(self):
        self.run_fetch(LOCK, b'good')
        with self.assertRaises(FileExistsError): self.run_fetch(LOCK, b'good')
    def test_unsafe_lock_path_rejected(self):
        bad = {'artifacts': [{'module': 'm', 'coordinate': 'g:m:v', 'aar': '../x', 'files': {'../x': {'bytes': 1, 'sha256': '0'*64}}}]}
        with self.assertRaises(ValueError): self.run_fetch(bad, b'x')
    def test_outside_stage_prefix_rejected(self):
        bad = {'artifacts': [{'module': 'm', 'coordinate': 'g:m:v', 'aar': 'elsewhere/a', 'files': {'elsewhere/a': {'bytes': 1, 'sha256': hashlib.sha256(b'x').hexdigest()}}}]}
        with self.assertRaises(ValueError): self.run_fetch(bad, b'x')

if __name__ == '__main__':
    unittest.main()
