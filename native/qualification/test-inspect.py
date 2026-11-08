#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import struct
import unittest
spec = importlib.util.spec_from_file_location('inventory', Path(__file__).with_name('inspect-apk.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

def elf(bits, machine, align):
    b = bytearray(128)
    b[:6] = b'\x7fELF' + bytes([bits//32, 1])
    struct.pack_into('<H', b, 18, machine)
    if bits == 32:
        struct.pack_into('<I', b, 28, 52)
        struct.pack_into('<HH', b, 42, 32, 1)
        struct.pack_into('<8I', b, 52, 1, 0, 0, 0, 0, 0, 0, align)
    else:
        struct.pack_into('<Q', b, 32, 64)
        struct.pack_into('<HH', b, 54, 56, 1)
        struct.pack_into('<I', b, 64, 1)
        struct.pack_into('<Q', b, 112, align)
    return bytes(b)

class InventoryTests(unittest.TestCase):
    def test_arm64(self):
        self.assertTrue(m.inspect_elf(elf(64,183,16384),'lib/arm64-v8a/x.so')['passes'])
    def test_x64_bad_load(self):
        self.assertFalse(m.inspect_elf(elf(64,62,4096),'lib/x86_64/x.so')['passes'])
    def test_arm32_inventory(self):
        self.assertEqual(m.inspect_elf(elf(32,40,4096),'lib/armeabi-v7a/x.so')['elf_class'],32)
    def test_x86_inventory(self):
        self.assertEqual(m.inspect_elf(elf(32,3,4096),'lib/x86/x.so')['machine'],3)
    def test_abi_mismatch(self):
        with self.assertRaises(ValueError):m.inspect_elf(elf(32,40,4096),'lib/x86/x.so')
    def test_truncated(self):
        with self.assertRaises(ValueError):m.inspect_elf(b'\x7fELF\x01\x01','lib/x86/x.so')

unittest.main()
