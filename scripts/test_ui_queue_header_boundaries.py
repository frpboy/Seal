#!/usr/bin/env python3
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
class QueueHeaderBoundaries(unittest.TestCase):
    def setUp(self):self.s=(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/DownloadPageV2.kt').read_text()
    def test_count_is_current_filter_and_outside_lazy_grid(self):
        call=self.s.index('                    SubHeader(')
        self.assertLess(call,self.s.index('else LazyVerticalGrid('))
        self.assertIn('filterLabel = activeFilter.label()',self.s)
        self.assertIn('taskCount = filteredMap.size',self.s)
        self.assertEqual(self.s.count('SubHeader('),2)
    def test_empty_queue_fab_label_uses_whole_queue_not_empty_filter(self):
        self.assertIn('expandForEmptyQueue = taskDownloadStateMap.isEmpty()',self.s)
        self.assertIn('val expanded = expandForEmptyQueue ||',self.s)
    def test_view_switch_accessible_and_dead_menu_removed(self):
        header=self.s.split('fun SubHeader(',1)[1].split('internal class',1)[0]
        self.assertIn('Modifier.size(48.dp)',header)
        self.assertIn('R.string.queue_show_list else R.string.queue_show_grid',header)
        self.assertNotIn('clearAndSetSemantics',header)
        self.assertNotIn('onShowMenu',header)
        self.assertNotIn('Not implemented yet!',self.s)
    def test_default_strings_unique(self):
        root=ET.parse(ROOT/'app/src/main/res/values/strings.xml').getroot()
        names=[(e.tag,e.attrib.get('name')) for e in root if e.tag in ('string','plurals')]
        self.assertEqual(len(names),len(set(names)))
if __name__=='__main__':unittest.main()
