from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1];S=R/'app/src/main/java/com/frpboy/seali'
class HistoryAlignment(unittest.TestCase):
 def test_uniform_history_thumbnail_does_not_change_other_media_image(self):
  s=(S/'ui/component/VideoListItem.kt').read_text();history,other=s.split('fun MediaImage(',1)
  self.assertIn('Modifier.width(96.dp).aspectRatio(16f / 9f)',history)
  self.assertIn('contentScale = ContentScale.Crop',history)
  self.assertIn('if (!isAudio) 16f / 9f else 1f',other)
 def test_menu_has_reserved_row_space_and_accessible_label(self):
  s=(S/'ui/component/VideoListItem.kt').read_text()
  self.assertNotIn('Alignment.BottomEnd',s)
  self.assertNotIn('clearAndSetSemantics',s)
  self.assertIn('Modifier.padding(horizontal = 12.dp).weight(1f)',s)
  self.assertIn('R.string.show_more_actions',s)
 def test_no_duration_guess_or_db_change(self):
  s=(S/'ui/component/VideoListItem.kt').read_text()
  self.assertIn('historyFileExtension(videoPath)',s)
  self.assertNotIn('duration',s.lower())
  self.assertIn('else stringResource(R.string.unavailable)',s)
 def test_provider_display_only(self):
  s=(S/'ui/page/videolist/VideoListPage.kt').read_text()
  self.assertIn('label = historyProviderLabel(filterSet.elementAt(i))',s)
  self.assertIn('(this.extractor == extractor)',s)
if __name__=='__main__':unittest.main()
