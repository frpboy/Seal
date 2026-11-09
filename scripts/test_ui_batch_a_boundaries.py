#!/usr/bin/env python3
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/junkfood/seal"
class BatchABoundaries(unittest.TestCase):
    def test_all_empty_filters_have_honest_distinct_messages(self):
        text=(SRC/"ui/page/downloadv2/QueueEmptyState.kt").read_text()
        for name in ["Filter.All", "Filter.Downloading", "Filter.Canceled", "Filter.Finished"]:
            self.assertEqual(2,text.count(name + " ->"))
        self.assertIn("R.drawable.ic_launcher_foreground",text)
        self.assertIn("verticalScroll",text)
        self.assertIn("FilledTonalButton(onClick = onAddLink)",text)
        self.assertNotIn("Clipboard",text)
    def test_empty_state_uses_existing_link_action(self):
        text=(SRC/"ui/page/downloadv2/DownloadPageV2.kt").read_text()
        self.assertIn("onAddLink = downloadCallback",text)
        self.assertNotIn("fillMaxHeight(0.4f)",text)
        self.assertNotIn("DownloadQueuePlaceholder",text)
    def test_drawer_brand_grouping_and_version_about(self):
        drawer=(SRC/"ui/page/NavigationDrawer.kt").read_text()
        self.assertIn("R.drawable.ic_launcher_foreground",drawer)
        self.assertIn("R.string.seali_support_and_donate",drawer)
        self.assertNotIn("Spacer(Modifier.height(72.dp))",drawer)
        app=(SRC/"ui/page/AppEntry.kt").read_text()
        footer=app.split("footer = {",1)[1].split("NavHost",1)[0]
        self.assertIn("TextButton",footer)
        self.assertIn("navController.navigate(Route.ABOUT)",footer)
        self.assertNotIn('+ "\\n" + currentRoute',footer)
    def test_default_strings_are_unique_and_xml_valid(self):
        elements=ET.parse(ROOT/"app/src/main/res/values/strings.xml").getroot()
        names=[e.attrib.get("name") for e in elements if e.tag=="string"]
        self.assertEqual(len(names),len(set(names)))
        text={e.attrib.get("name"):e.text for e in elements}
        self.assertEqual("No canceled or failed downloads",text["seali_empty_canceled"])
if __name__=="__main__":unittest.main()
