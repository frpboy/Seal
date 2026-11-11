#!/usr/bin/env python3
"""Synthetic ELF-layout/parser guard tests only. No real target binary or ABI proof."""
import importlib.util,pathlib,struct,unittest
p=pathlib.Path(__file__).with_name('verify-quickjs-target.py');s=importlib.util.spec_from_file_location('target_check',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
def fixture(machine=183,align=16384,flags=5,relro_start=16384,relro_size=16384):
 d=bytearray(512);ident=b'\x7fELF\x02\x01\x01'+b'\0'*9
 struct.pack_into('<16sHHIQQQIHHHHHH',d,0,ident,3,machine,1,0,64,0,0,64,56,4,0,0,0)
 for i,v in enumerate([(1,flags,0,0,0,512,32768,align),(3,4,400,400,0,21,21,1),(0x6474e552,4,0,relro_start,0,0,relro_size,1),(0x6474e551,6,0,0,0,0,0,16)]):struct.pack_into('<IIQQQQQQ',d,64+56*i,*v)
 d[400:421]=b'/system/bin/linker64\0';return d
class Layout(unittest.TestCase):
 def test_accepts_synthetic_arm64_and_x86(self):
  m.elf_layout(fixture(),183);m.elf_layout(fixture(machine=62),62)
 def test_rejects_wrong_abi_small_alignment_wx_relro(self):
  for d in [fixture(machine=62),fixture(align=4096),fixture(flags=7),fixture(relro_start=4096),fixture(relro_size=4096)]:
   with self.assertRaises(AssertionError):m.elf_layout(d,183)
 def test_symbol_parser(self):
  self.assertEqual(m.symbols('  1: 0000000000000000 0 FUNC GLOBAL DEFAULT UND malloc@LIBC (2)\n  2: 0000000000000050 20 FUNC WEAK DEFAULT 5 free@@LIBC'),[('GLOBAL','UND','malloc'),('WEAK','5','free')])
if __name__=='__main__':unittest.main()
