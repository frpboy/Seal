#!/usr/bin/env python3
"""Adversarial source-only fixture tests. No target code is executed."""
import hashlib
import importlib.util
import io
import json
import subprocess
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('producer', HERE / 'seali-prepare-owned-producer-inputs.py')
producer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(producer)


def record(path):
    return {'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'bytes': path.stat().st_size}


class PreparationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.bundle = self.root / 'bundle'
        for d in ['python-baseline/recipe', 'python-baseline/archives', 'python-baseline/notices',
                  'python-refresh/recipe', 'python-refresh/archives']:
            (self.bundle / d).mkdir(parents=True)
        self.ca = self.bundle / 'python-baseline/notices/cacert.pem'
        self.ca.write_text('synthetic fixture CA, not a trust store')
        self.recipe = self.bundle / 'python-refresh/recipe/README.md'
        self.recipe.write_text('synthetic source recipe')
        self.archive = self.bundle / 'python-baseline/archives/termux-9115a276.tar.gz'
        self.metadata = self.bundle / 'python-baseline/recipe/producer-source-inputs.json'
        self.members = [('root/readme', 'file', b'source'), ('root/arch/a', 'sym', '../readme')]
        self.write_tar()
    def tearDown(self):
        self.tmp.cleanup()
    def write_tar(self):
        with tarfile.open(self.archive, 'w:gz') as t:
            for name, kind, value in self.members:
                m = tarfile.TarInfo(name)
                if kind == 'file':
                    m.size = len(value)
                    t.addfile(m, io.BytesIO(value))
                else:
                    m.type = {'sym': tarfile.SYMTYPE, 'hard': tarfile.LNKTYPE, 'dir': tarfile.DIRTYPE,
                              'fifo': tarfile.FIFOTYPE}[kind]
                    m.linkname = value or ''
                    t.addfile(m)
        self.metadata.write_text(json.dumps([{'file': p.name, **record(p), 'url': 'https://example.invalid/source'} for p in [self.archive, self.ca]]))
        self.lock = {'metadata': {'python-baseline/recipe/producer-source-inputs.json': {
            'sha256': record(self.metadata)['sha256'], 'names': sorted([self.archive.name, self.ca.name])}},
            'inputs': {str(p.relative_to(self.bundle)): {**record(p), 'name': p.name}
                       for p in [self.archive, self.ca]}, 'recipe': {'README.md': record(self.recipe)}}
    def run_prepare(self, success=False):
        if success:
            return producer.prepare(self.bundle, self.root / 'output', self.lock)
        with self.assertRaises((ValueError, OSError)):
            producer.prepare(self.bundle, self.root / 'output', self.lock)
        self.assertFalse((self.root / 'output').exists())
        self.assertFalse(list(self.root.glob('.output-*')), 'partial staging leaked')
    def add_bad(self, members):
        self.members += members
        self.write_tar()
        self.run_prepare()
    def test_valid_internal_relative_link(self):
        self.run_prepare(True)
        self.assertEqual((self.root / 'output/termux-source/root/arch/a').read_bytes(), b'source')
    def test_valid_later_symlink_chain(self):
        self.members.append(('root/deep/chain', 'sym', '../alias'))
        self.members.append(('root/alias', 'sym', 'readme'))
        self.write_tar()
        self.run_prepare(True)
        self.assertEqual((self.root / 'output/termux-source/root/deep/chain').read_bytes(), b'source')
    def test_valid_shallower_symlink_then_parent(self):
        self.members += [('root/deep/s', 'sym', '..'), ('root/b', 'sym', 'deep/s/arch/../readme')]
        self.write_tar()
        self.run_prepare(True)
        self.assertEqual((self.root / 'output/termux-source/root/b').read_bytes(), b'source')
    def test_valid_hardlink_prior_regular(self):
        self.members.append(('root/hard', 'hard', 'root/readme'))
        self.write_tar()
        self.run_prepare(True)
        a, b = self.root / 'output/termux-source/root/readme', self.root / 'output/termux-source/root/hard'
        self.assertEqual(a.stat().st_ino, b.stat().st_ino)
    def test_absolute_member(self):
        self.add_bad([('/escape', 'file', b'bad')])
    def test_traversal_member(self):
        self.add_bad([('../escape', 'file', b'bad')])
    def test_absolute_symlink(self):
        self.add_bad([('root/s', 'sym', '/etc/passwd')])
    def test_escaping_symlink(self):
        self.add_bad([('root/arch/s', 'sym', '../../../escape')])
    def test_shallower_symlink_chain(self):
        self.add_bad([('root/deep/shallower', 'sym', '..'),
                      ('root/s', 'sym', 'deep/shallower/../../escape')])
    def test_hardlink_via_shallower_symlink(self):
        self.add_bad([('root/deep/shallower', 'sym', '..'),
                      ('root/h', 'hard', 'root/deep/shallower/../../escape')])
    def test_hardlink_to_symlink(self):
        self.add_bad([('root/h', 'hard', 'root/arch/a')])
    def test_missing_hardlink(self):
        self.add_bad([('root/h', 'hard', 'root/missing')])
    def test_later_hardlink(self):
        self.add_bad([('root/h', 'hard', 'root/later'), ('root/later', 'file', b'late')])
    def test_duplicate(self):
        self.add_bad([('root/readme', 'file', b'other')])
    def test_special(self):
        self.add_bad([('root/fifo', 'fifo', '')])
    def test_symlink_ancestor_late(self):
        self.add_bad([('root/s/file', 'file', b'bad'), ('root/s', 'sym', '..')])
    def test_symlink_cycle(self):
        self.add_bad([('root/c1', 'sym', 'c2'), ('root/c2', 'sym', 'c1')])
    def test_oversized(self):
        old = producer.MAX_FILE
        producer.MAX_FILE = 2
        try:
            self.run_prepare()
        finally:
            producer.MAX_FILE = old
    def test_member_count(self):
        old = producer.MAX_MEMBERS
        producer.MAX_MEMBERS = 1
        try:
            self.run_prepare()
        finally:
            producer.MAX_MEMBERS = old
    def test_total_size(self):
        old = producer.MAX_BYTES
        producer.MAX_BYTES = 2
        try:
            self.run_prepare()
        finally:
            producer.MAX_BYTES = old
    def test_bad_hash(self):
        with self.archive.open('ab') as f:
            f.write(b'corruption')
        self.run_prepare()
    def test_changed_recipe(self):
        self.recipe.write_text('changed')
        self.run_prepare()
    def test_extra_recipe(self):
        self.recipe.with_name('extra').write_text('extra')
        self.run_prepare()
    def test_symlink_recipe(self):
        self.recipe.unlink()
        self.recipe.symlink_to(self.ca)
        self.run_prepare()
    def test_missing_row(self):
        self.metadata.write_text('[]')
        self.lock['metadata']['python-baseline/recipe/producer-source-inputs.json']['sha256'] = record(self.metadata)['sha256']
        self.run_prepare()
    def test_extra_row(self):
        rows = json.loads(self.metadata.read_text())
        rows.append({'file': 'extra', 'sha256': '0' * 64})
        self.metadata.write_text(json.dumps(rows))
        self.lock['metadata']['python-baseline/recipe/producer-source-inputs.json']['sha256'] = record(self.metadata)['sha256']
        self.run_prepare()
    def test_metadata_hash_drift(self):
        rows = json.loads(self.metadata.read_text())
        rows[0]['sha256'] = '0' * 64
        self.metadata.write_text(json.dumps(rows))
        self.lock['metadata']['python-baseline/recipe/producer-source-inputs.json']['sha256'] = record(self.metadata)['sha256']
        self.run_prepare()
    def test_metadata_size_drift(self):
        rows = json.loads(self.metadata.read_text())
        rows[0]['bytes'] += 1
        self.metadata.write_text(json.dumps(rows))
        self.lock['metadata']['python-baseline/recipe/producer-source-inputs.json']['sha256'] = record(self.metadata)['sha256']
        self.run_prepare()
    def test_metadata_fields_and_modes_preserved(self):
        self.run_prepare(True)
        out = self.root / 'output'
        rows = json.loads((out / 'inputs/source-inputs.json').read_text())
        self.assertTrue(all(x['url'] == 'https://example.invalid/source' for x in rows))
        self.assertEqual(out.stat().st_mode & 0o777, 0o700)
        self.assertEqual((out / 'recipe/README.md').stat().st_mode & 0o777, 0o600)
    def test_symlink_input(self):
        self.archive.rename(self.root / 'outside.tar.gz')
        self.archive.symlink_to(self.root / 'outside.tar.gz')
        self.run_prepare()
    def test_existing_output_preserved(self):
        out = self.root / 'output'
        out.mkdir()
        (out / 'keep').write_text('keep')
        with self.assertRaises(ValueError):
            producer.prepare(self.bundle, out, self.lock)
        self.assertEqual((out / 'keep').read_text(), 'keep')
    def test_optimized_checks_still_enforced(self):
        with self.archive.open('ab') as f:
            f.write(b'corruption')
        lock = self.root / 'synthetic-lock.json'
        lock.write_text(json.dumps(self.lock))
        code = 'import importlib.util,json,sys; s=importlib.util.spec_from_file_location("p",sys.argv[1]); m=importlib.util.module_from_spec(s);s.loader.exec_module(m);m.prepare(sys.argv[2],sys.argv[3],json.load(open(sys.argv[4])))'
        r = subprocess.run([sys.executable, '-O', '-c', code, str(HERE / 'seali-prepare-owned-producer-inputs.py'),
                            str(self.bundle), str(self.root / 'output'), str(lock)], capture_output=True)
        self.assertNotEqual(r.returncode, 0)
        self.assertIn(b'input size differs', r.stderr)


if __name__ == '__main__':
    unittest.main(verbosity=2)
