#!/usr/bin/env python3
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
class ConfigurationBoundaries(unittest.TestCase):
    def setUp(self):self.text=(ROOT/"app/src/main/java/com/frpboy/seali/ui/page/downloadv2/configure/DownloadDialogV2.kt").read_text()
    def test_always_visible_current_type_chips(self):
        self.assertIn("settingChips: @Composable (DownloadType?) -> Unit",self.text)
        self.assertIn("settingChips(selectedType)",self.text)
        self.assertNotIn("ExpandableTitle(expanded = expanded",self.text)
        self.assertNotIn("selectedType = config.downloadType",self.text)
        self.assertIn("if (selectedType != Command)",self.text)
    def test_custom_summary_uses_actual_preference_flags(self):
        self.assertIn("if (preferences.downloadSubtitle)",self.text)
        self.assertIn("if (preferences.createThumbnail)",self.text)
        self.assertNotIn("H.264 1080p",self.text)
    def test_loading_keeps_actual_indeterminate_progress(self):
        self.assertIn("LinearProgressIndicator(modifier = Modifier.fillMaxWidth())",self.text)
        self.assertIn("R.string.fetching_info",self.text)
        self.assertIn("R.drawable.ic_launcher_foreground",self.text)
    def test_strings_xml_unique(self):
        root=ET.parse(ROOT/"app/src/main/res/values/strings.xml").getroot()
        names=[e.attrib.get("name") for e in root if e.tag=="string"]
        self.assertEqual(len(names),len(set(names)))
if __name__=="__main__":unittest.main()
