#!/usr/bin/env python3
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class SelectionWiring(unittest.TestCase):
    def test_visible_selection_is_dispatched_not_initial(self):
        source=(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/configure/DownloadDialogV2.kt').read_text()
        sheet=source.split('fun ConfigurePagePlaylistVariant(',1)[1].split('@Composable\nprivate fun AdditionalSettings',1)[0]
        self.assertIn('onSelect = { selectedType = it }',sheet)
        self.assertIn('submitPlaylistSelection(selectedType, onDownload, onDismissRequest)',sheet)
        self.assertNotIn('onDownload(initialDownloadType)',sheet)
        self.assertEqual(sheet.count('submitPlaylistSelection('),1)
    def test_both_consumers_use_dispatched_audio_flag(self):
        folder=ROOT/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/configure'
        source=(folder/'DownloadDialogV2.kt').read_text()
        multi=source.split('ConfigurePagePlaylistVariant(',1)[1].split('is Error ->',1)[0]
        self.assertIn('urlList = state.urlList',multi)
        self.assertIn('preferences.copy(extractAudio = it == Audio)',multi)
        playlist=(folder/'PlaylistSelectionPage.kt').read_text().split('ConfigurePagePlaylistVariant(',1)[1]
        self.assertIn('preferences.copy(extractAudio = it == Audio)',playlist)
        self.assertIn('.forEach(downloader::enqueue)',playlist)
if __name__=='__main__': unittest.main()
