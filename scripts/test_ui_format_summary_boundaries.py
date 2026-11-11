from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1];P=R/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/configure'
class FormatSummaryBoundaries(unittest.TestCase):
 def test_pinned_bar_uses_same_actual_selected_list(self):
  s=(P/'FormatPage.kt').read_text();bar=s.split('bottomBar = {',1)[1].split(') { paddingValues ->',1)[0]
  self.assertIn('val streams = formatList.map',bar)
  self.assertIn('it.fileSize, it.fileSizeApprox, it.ext, it.vcodec, it.acodec',bar)
  self.assertIn('selectedStreamBytes(streams, isClippingVideo || isSplittingVideo)',bar)
  self.assertIn('formatList = formatList',bar)
  self.assertIn('navigationBarsPadding()',bar)
  self.assertNotIn('floatingActionButton = {',s)
 def test_no_new_download_or_silent_selection(self):
  s=(P/'FormatPage.kt').read_text();bar=s.split('bottomBar = {',1)[1].split(') { paddingValues ->',1)[0]
  self.assertIn('enabled = isSuggestedFormatSelected || formatList.isNotEmpty()',bar)
  self.assertEqual(1,bar.count('onDownloadPressed('))
  for token in ['isSuggestedFormatSelected =','selectedAudioOnlyFormats.add','enqueue(','http','fileSizeApprox =']:
   self.assertNotIn(token,bar)
 def test_context_and_actual_config_preserved(self):
  s=(P/'FormatPage.kt').read_text()
  for token in ['videoClips =','splitByChapter = isSplittingVideo','newTitle = videoTitle','selectedSubtitles = selectedSubtitles','selectedAutoCaptions = selectedAutoCaptions','R.string.seali_stream_size_unknown']:
   self.assertIn(token,s)
if __name__=='__main__':unittest.main()
