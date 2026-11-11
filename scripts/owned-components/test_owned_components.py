#!/usr/bin/env python3
"""Host fixtures only. Never run source downloads, compilers or Android code."""
import importlib.util
import io
import json
import os
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


def load(name):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(name+'.py'))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m


prepare=load('prepare');runner=load('run')


class Tests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
    def tearDown(self): self.temp.cleanup()
    def archive(self,name='src/file',kind='file'):
        p=self.root/'source.tar'
        with tarfile.open(p,'w') as t:
            i=tarfile.TarInfo(name)
            if kind=='link': i.type=tarfile.SYMTYPE;i.linkname='/outside';t.addfile(i)
            elif kind=='special': i.type=tarfile.FIFOTYPE;t.addfile(i)
            else: i.size=6;i.mode=0o755;t.addfile(i,io.BytesIO(b'source'))
        return p
    def test_safe_archive(self):
        root=prepare.unpack_source(self.archive(),self.root/'out')
        self.assertEqual((root/'file').read_bytes(),b'source')
        self.assertEqual((root/'file').stat().st_mode&0o777,0o755)
    def test_traversal(self):
        with self.assertRaisesRegex(ValueError,'traversal'):
            prepare.unpack_source(self.archive('../outside'),self.root/'out')
        self.assertFalse((self.root/'out').exists())
    def test_symlink(self):
        with self.assertRaisesRegex(ValueError,'link'):
            prepare.unpack_source(self.archive(kind='link'),self.root/'out')
    def test_special(self):
        with self.assertRaisesRegex(ValueError,'special'):
            prepare.unpack_source(self.archive(kind='special'),self.root/'out')
    def test_existing_output(self):
        (self.root/'out').mkdir()
        with self.assertRaisesRegex(ValueError,'fresh'):
            prepare.unpack_source(self.archive(),self.root/'out')
    def test_duplicate(self):
        p=self.archive()
        with tarfile.open(p,'a') as t:
            i=tarfile.TarInfo('src/file');i.size=1;t.addfile(i,io.BytesIO(b'x'))
        with self.assertRaisesRegex(ValueError,'duplicate'):
            prepare.unpack_source(p,self.root/'out')
    def test_file_ancestor(self):
        p=self.archive('src')
        with tarfile.open(p,'a') as t:
            i=tarfile.TarInfo('src/file');i.size=1;t.addfile(i,io.BytesIO(b'x'))
        with self.assertRaisesRegex(ValueError,'ancestor'):
            prepare.unpack_source(p,self.root/'out')
    def test_cipher_compressed_bound(self):
        with patch.object(runner,'CIPHER_MAX',1024**2+4):
            with self.assertRaisesRegex(ValueError,'bound'):
                runner.LimitedWriter(io.BytesIO()).write(b'12345')
    def test_manual_source_guard(self):
        sha='a'*40
        with patch.dict(os.environ,{'GITHUB_REPOSITORY':'frpboy/Seali','GITHUB_EVENT_NAME':'workflow_dispatch','REPOSITORY_PRIVATE':'false','GITHUB_SHA':sha}),patch.object(runner.subprocess,'check_output',return_value=sha+'\n'):
            runner.exact_source(sha)
            with self.assertRaisesRegex(ValueError,'SHA differs'): runner.exact_source('b'*40)
    def test_push_event_rejected(self):
        with patch.dict(os.environ,{'GITHUB_REPOSITORY':'frpboy/Seali','GITHUB_EVENT_NAME':'push'}):
            with self.assertRaisesRegex(ValueError,'manual'): runner.exact_source('a'*40)
    def test_private_repo_rejected(self):
        with patch.dict(os.environ,{'GITHUB_REPOSITORY':'frpboy/Seali','GITHUB_EVENT_NAME':'workflow_dispatch','REPOSITORY_PRIVATE':'true'}):
            with self.assertRaisesRegex(ValueError,'public'): runner.exact_source('a'*40)
    def test_feature_flags(self):
        s=Path(__file__).with_name('build-two-components.sh').read_text()
        for flag in ['no-module no-legacy','no-zlib no-brotli no-zstd no-quic no-siv no-cms','-DEXPAT_DTD=OFF -DEXPAT_GE=OFF -DEXPAT_NS=ON','make -j1','--parallel 1','-D__ANDROID_API__=24']:
            self.assertIn(flag,s)
        self.assertNotIn('execution_authorized',s)
    def test_no_downstream_actions(self):
        text=Path(__file__).with_name('run.py').read_text()
        self.assertNotIn('gradlew',text);self.assertNotIn('git push',text)
        self.assertIn('CIPHER_MAX=200*1024**2',text)
        self.assertIn("'publication_ready':False",text)

    def test_manual_workflow_pins_and_custody(self):
        root=Path(__file__).resolve().parents[2]
        text=(root/'.github/workflows/owned-arm64-components.yml').read_text()
        self.assertIn('actions/checkout@11d5960a326750d5838078e36cf38b85af677262',text)
        self.assertIn('actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02',text)
        self.assertIn('retention-days: 1',text)
        self.assertIn('persist-credentials: false',text)
        self.assertIn('runs-on: ubuntu-24.04',text)
        self.assertNotIn('secrets.',text)
        self.assertNotIn('pull_request:',text)
        self.assertNotIn('push:',text)

    def test_probe_precedes_download(self):
        text=Path(__file__).with_name('prepare.py').read_text()
        body=text[text.index('def prepare('):]
        self.assertLess(body.index('network=v.network_probe()'),body.index("'inputs':download("))
    def test_reduced_custody_keeps_build_not_sources(self):
        root=self.root/'job';root.mkdir()
        for name in ['sources','build','evidence','recipes','tmp','home','inputs']:
            (root/name).mkdir();(root/name/'fixture').write_text(name)
        archive=self.root/'reduced.tar.gz'
        runner.write_archive(archive,root,reduced=True)
        with tarfile.open(archive) as t:
            names=t.getnames()
        self.assertIn('build/fixture',names)
        self.assertIn('evidence/fixture',names)
        self.assertNotIn('sources/fixture',names)
        self.assertFalse(any(x.startswith('inputs') for x in names))
    def test_entropy_assertions_present(self):
        text=Path(__file__).with_name('postbuild.py').read_text()
        self.assertIn("assert 'HAVE_SYSCALL_GETRANDOM' in expat",text)
        self.assertIn("'HAVE_ARC4RANDOM','HAVE_ARC4RANDOM_BUF','HAVE_GETRANDOM','XML_DEV_URANDOM'",text)


if __name__=='__main__': unittest.main(verbosity=2)
