#!/usr/bin/env python3
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
class AppearanceBoundaries(unittest.TestCase):
    def setUp(self): self.s=(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/settings/appearance/AppearancePreferences.kt').read_text()
    def test_existing_switch_guard_and_confirm_preserved(self):
        self.assertIn('InterfaceSwitchGuard.hasActiveDownloads(',self.s)
        self.assertIn('else if (selected != currentUiStyle)',self.s)
        self.assertIn('TextButton(onClick = { onSelect(selected) })',self.s)
        self.assertIn('(context as? Activity)?.recreate()',self.s)
    def test_opaque_seeds_opt_in_existing_style_path(self):
        self.assertIn('Color(0xFF007F86)',self.s)
        self.assertIn('Color(0xFF3867D6)',self.s)
        self.assertIn('PreferenceUtil.modifyThemeSeedColor(seed.toArgb(), STYLE_TONAL_SPOT)',self.s)
        helper=self.s.split('private fun CuratedAccent(',1)[1].split('@Composable',1)[0]
        self.assertEqual(helper.count('PreferenceUtil.switchDynamicColor(enabled = false)'),1)
        self.assertIn('onClick = {',helper)
    def test_large_target_radio_whole_row_and_monochrome_bounds(self):
        helper=self.s.split('private fun InterfaceChoice(',1)[1]
        self.assertIn('heightIn(min = 64.dp)',helper)
        self.assertIn('role = Role.RadioButton',helper)
        self.assertIn('RadioButton(selected = selected, onClick = null)',helper)
        self.assertIn('if (LocalPaletteStyleIndex.current == STYLE_MONOCHROME) pageCount - 1',self.s)
    def test_xml_unique_across_default_values(self):
        names=[]
        for path in (ROOT/'app/src/main/res/values').glob('*.xml'):
            names += [e.attrib['name'] for e in ET.parse(path).getroot() if e.tag=='string']
        self.assertEqual(len(names),len(set(names)))
if __name__=='__main__':unittest.main()
