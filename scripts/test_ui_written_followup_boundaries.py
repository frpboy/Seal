from pathlib import Path
import unittest,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1];S=R/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2'
class WrittenFollowup(unittest.TestCase):
 def test_open_does_not_read_clipboard(self):
  s=(S/'configure/InputUrlDialog.kt').read_text()
  self.assertNotIn('clipboardManager.getText()',s)
  self.assertNotIn('LaunchedEffect(Unit)',s)
  self.assertIn('PasteFromClipBoardButton { text ->',s)
  self.assertIn('explicitlyPastedText.orEmpty()',s)
 def test_secondary_paste_is_explicit_and_not_configuration_bypass(self):
  s=(S/'DownloadPageV2.kt').read_text();empty=(S/'QueueEmptyState.kt').read_text()
  self.assertIn('TextButton(onClick = onPasteLink)',empty)
  self.assertIn('onPasteLink = pasteCallback',s)
  paste=s.split('pasteCallback = {',1)[1].split('onMenuOpen',1)[0]
  self.assertIn('clipboardManager.getText()',paste)
  self.assertIn('Action.ShowSheet(explicitlyPastedText = text)',paste)
  self.assertNotIn('ProceedWithURLs',paste)
  vm=(S/'configure/DownloadDialogViewModel.kt').read_text()
  self.assertIn('SheetState.InputUrl(action.explicitlyPastedText)',vm)
 def test_single_invalid_and_multiple_paste_remain_reviewable(self):
  s=(S/'configure/InputUrlDialog.kt').read_text()
  self.assertIn('urlListFromClipboard.singleOrNull() ?: initialPastedText.orEmpty()',s)
  self.assertIn('mutableStateOf(urlListFromClipboard.size > 1)',s)
  self.assertIn('urlListFromClipboard = explicitlyPastedUrls ?: urlListFromClipboard',s)
  self.assertIn('Action.ProceedWithURLs(it)',s)
 def test_decoration_and_active_emphasis_only(self):
  s=(S/'QueueEmptyState.kt').read_text();v=(S/'DownloadPageV2.kt').read_text()
  self.assertIn('filter == Filter.All && !compact',s);self.assertIn('contentDescription = null',s)
  self.assertIn('verticalScroll(rememberScrollState())',s)
  self.assertEqual(2,v.count('.queueActiveEmphasis(state.downloadState is Running)'))
  self.assertIn('if (!running) this else this',v)
  self.assertEqual(2,v.count('sortedBy { (_, state) -> state.downloadState }'))
  ET.parse(R/'app/src/main/res/drawable/seali_queue_phone.xml')
if __name__=='__main__':unittest.main()
