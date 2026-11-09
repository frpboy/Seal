#!/usr/bin/env python3
"""Resource boundary checks. These do not assert Android rendering/runtime behavior."""
from pathlib import Path
import struct
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
A = "{http://schemas.android.com/apk/res/android}"
class IconResources(unittest.TestCase):
    def test_notification_vector_is_monochrome_current_chevrons(self):
        vector = ET.parse(RES / "drawable-anydpi-v24/ic_stat_seal.xml").getroot()
        self.assertEqual("24", vector.attrib[A+"viewportWidth"])
        self.assertEqual("#FFFFFF", vector.attrib[A+"tint"])
        path = vector.find("path")
        self.assertEqual(3, path.attrib[A+"pathData"].count("M "))
        self.assertEqual("#FFFFFF", path.attrib[A+"strokeColor"])
        self.assertEqual("#00000000", path.attrib[A+"fillColor"])
        self.assertEqual("round", path.attrib[A+"strokeLineCap"])
        self.assertIsNone(vector.find("group"))
    def test_png_fallback_dimensions_are_retained(self):
        for density, size in [("mdpi",24),("hdpi",36),("xhdpi",48),("xxhdpi",72)]:
            data = (RES / f"drawable-{density}/ic_stat_seal.png").read_bytes()
            self.assertEqual(b"\x89PNG\r\n\x1a\n", data[:8])
            self.assertEqual((size,size), struct.unpack(">II",data[16:24]))
        for density,size in [("mdpi",108),("hdpi",162),("xhdpi",216),("xxhdpi",324),("xxxhdpi",432)]:
            data=(RES/f"mipmap-{density}/ic_launcher_foreground.png").read_bytes()
            self.assertEqual((size,size),struct.unpack(">II",data[16:24]))
    def test_all_live_notification_builders_keep_same_resource(self):
        text=(ROOT/"app/src/main/java/com/junkfood/seal/util/NotificationUtil.kt").read_text()
        live="\n".join(line for line in text.splitlines() if not line.lstrip().startswith("//"))
        self.assertEqual(7,live.count("setSmallIcon(R.drawable.ic_stat_seal)"))
        self.assertNotIn("setLargeIcon", live)
    def test_launcher_adaptive_mapping_remains_current(self):
        for name in ["ic_launcher.xml","ic_launcher_round.xml"]:
            icon=ET.parse(RES/"mipmap-anydpi-v26"/name).getroot()
            self.assertEqual("@drawable/ic_launcher_foreground",icon.find("foreground").attrib[A+"drawable"])
            self.assertEqual("@drawable/ic_launcher_monochrome",icon.find("monochrome").attrib[A+"drawable"])
if __name__ == "__main__":
    unittest.main()
