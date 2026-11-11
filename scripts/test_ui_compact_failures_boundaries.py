from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
S=R/'app/src/main/java/com/frpboy/seali'
class CompactFailureBoundaries(unittest.TestCase):
 def test_layout_default_and_explicit_persistence(self):
  s=(S/'ui/page/downloadv2/DownloadPageV2.kt').read_text()
  self.assertIn('rememberSaveable',s)
  self.assertIn('QUEUE_GRID_VIEW.getBoolean(default = windowWidthSizeClass != WindowWidthSizeClass.Compact)',s)
  self.assertIn('QUEUE_GRID_VIEW.updateBoolean(isGridView)',s)
 def test_error_wiring_both_layouts(self):
  s=(S/'ui/page/downloadv2/DownloadPageV2.kt').read_text()
  self.assertEqual(2,s.count('CompactQueueErrorCard(task,'))
  self.assertIn('R.string.queue_failed_or_canceled',s)
  self.assertNotIn('viewState = this,',s)
  self.assertNotIn('CompactQueueErrorCard(task, this,',s)
  self.assertIn('taskDownloadStateMap.count { filter.predict(it.toPair()) }',s)
 def test_error_controls_and_no_thumbnail_network(self):
  s=(S/'ui/page/downloadv2/CompactQueueErrorCard.kt').read_text()
  for text in ['UiAction.Resume','UiAction.CopyErrorReport(error.throwable)','onShowActions','SafeErrorReport.reason(error.throwable)']:
   self.assertIn(text,s)
  for text in ['AsyncImage','throwable.message','stackTrace','http://','https://']:
   self.assertNotIn(text,s)
 def test_settings_entry_remains_section_renamed(self):
  s=(S/'ui/page/NavigationDrawer.kt').read_text()
  self.assertIn('label = { Text(stringResource(R.string.settings)) }',s)
  self.assertEqual(1,s.count('R.string.seali_quick_settings'))
if __name__=='__main__':unittest.main()
