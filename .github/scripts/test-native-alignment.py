#!/usr/bin/env python3
import importlib.util
import io
from pathlib import Path
import struct
import stat
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('alignment', Path(__file__).with_name('check-native-alignment.py'))
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


def elf(alignment=16384, offset=0, vaddr=0):
    data = bytearray(120)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 1)
    struct.pack_into('<I', data, 64, 1)
    struct.pack_into('<QQ', data, 72, offset, vaddr)
    struct.pack_into('<Q', data, 112, alignment)
    return bytes(data)


def archive(entries):
    result = io.BytesIO()
    with zipfile.ZipFile(result, 'w') as z:
        for name, data in entries.items():

            if isinstance(data, tuple):
                info = zipfile.ZipInfo(name)
                info.create_system = 3
                info.external_attr = (stat.S_IFLNK | 0o777) << 16
                z.writestr(info, data[0])
            else:
                z.writestr(name, data)
    return result.getvalue()


class NativeAlignmentTests(unittest.TestCase):
    def test_direct_valid(self):
        self.assertTrue(check.inspect_payload(elf(), 'libfoo.so')[0]['passes'])

    def test_direct_4k(self):
        self.assertFalse(check.inspect_payload(elf(4096), 'libfoo.so')[0]['passes'])

    def test_incongruent_load(self):
        self.assertFalse(check.inspect_payload(elf(offset=4096), 'libfoo.so')[0]['passes'])

    def test_non_power_two(self):
        self.assertFalse(check.inspect_payload(elf(24576), 'libfoo.so')[0]['passes'])

    def test_nested_valid_and_text_symlink(self):
        data = archive({'usr/lib/real.so.1': elf(), 'usr/lib/link.so': (b'real.so.1',), 'data.txt': b'text'})
        found = check.inspect_payload(data, 'libfoo.zip.so')
        self.assertEqual(len(found), 1)
        self.assertTrue(found[0]['passes'])

    def test_nested_4k_preserved(self):
        data = archive({'good.so': elf(), 'bad.so': elf(4096)})
        self.assertEqual([r['passes'] for r in check.inspect_payload(data, 'libfoo.zip.so')], [True, False])

    def test_malformed_elf_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(b'\x7fELF', 'libfoo.so')

    def test_unknown_payload_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(b'not ELF', 'libfoo.so')

    def test_empty_nested_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'text': b'text'}), 'libfoo.zip.so')

    def test_invalid_nested_elf_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'bad': b'\x7fELF'}), 'libfoo.zip.so')

    def test_zip_expansion_ceiling(self):
        old = check.LIMIT
        try:
            check.LIMIT = 4
            with self.assertRaises(ValueError):
                check.inspect_payload(archive({'x.so': elf()}), 'libfoo.zip.so')
        finally:
            check.LIMIT = old

    def test_regular_native_text_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'bad.so': b'not ELF or symlink'}), 'libfoo.zip.so')

    def test_nested_zip_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'hidden.zip.so': archive({'bad.so': elf(4096)})}), 'libfoo.zip.so')

    def test_missing_symlink_target(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'bad.so': (b'absent.so',)}), 'libfoo.zip.so')

    def test_symlink_cycle(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'a.so': (b'b.so',), 'b.so': (b'a.so',)}), 'libfoo.zip.so')

    def test_symlink_escape(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'bad.so': (b'../escape.so',)}), 'libfoo.zip.so')

    def test_symlink_text_target_rejected(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), 'text': b'text', 'bad.so': (b'text',)}), 'libfoo.zip.so')

    def test_aggregate_byte_budget(self):
        data = archive({'good.so': elf()})
        budget = check.Budget(byte_limit=200)
        check.inspect_payload(data, 'libfoo.zip.so', budget)
        with self.assertRaises(ValueError):
            check.inspect_payload(data, 'libbar.zip.so', budget)

    def test_aggregate_entry_budget(self):
        data = archive({'good.so': elf()})
        budget = check.Budget(entry_limit=1)
        check.inspect_payload(data, 'libfoo.zip.so', budget)
        with self.assertRaises(ValueError):
            check.inspect_payload(data, 'libbar.zip.so', budget)

    def test_bad_archive_path(self):
        with self.assertRaises(ValueError):
            check.inspect_payload(archive({'good.so': elf(), '../bad': b'text'}), 'libfoo.zip.so')

    def test_abi_selection_and_aab_path(self):
        data = archive({'base/lib/x86/libold.so': b'ELF32 out of scope', 'base/lib/x86_64/libfoo.zip.so': archive({'nested.so': elf(4096)})})
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'app.aab'
            path.write_bytes(data)
            result = check.inspect_artifact(path)
            self.assertEqual(result['packaged_abis'], ['x86', 'x86_64'])
            self.assertEqual(len(result['elf64']), 1)
            self.assertFalse(result['elf64'][0]['passes'])


if __name__ == '__main__':
    unittest.main()
