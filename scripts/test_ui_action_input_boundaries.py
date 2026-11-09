from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
S=R/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2'
class ActionInputBoundaries(unittest.TestCase):
 def test_safe_reason_no_raw_log_preview(self):
  s=(S/'ActionSheet.kt').read_text()
  self.assertIn('SafeErrorReport.reason(downloadState.throwable)',s)
  for raw in ['throwable.message','stackTrace','takeLast(3)','FontFamily.Monospace']:
   self.assertNotIn(raw,s)
 def test_primary_error_controls_not_hidden_in_scroller(self):
  s=(S/'ActionSheet.kt').read_text()
  region=s.split('if (downloadState is Error) item {',1)[1].split('LazyRow(',1)[0]
  self.assertIn('UiAction.Resume',region)
  self.assertIn('UiAction.CopyErrorReport(downloadState.throwable)',region)
  self.assertEqual(2,region.count('FilledTonalButton('))
  self.assertIn('includeErrorPrimary = false',s)
  self.assertIn('is Error -> if (includeErrorPrimary)',s)
  self.assertIn('contentPadding = PaddingValues(horizontal = 20.dp)',s)
  for action in ['UiAction.Delete','UiAction.CopyVideoURL','UiAction.OpenVideoURL','UiAction.OpenThumbnailURL']:
   self.assertIn(action,s)
 def test_header_uses_display_fallback_media_keeps_exact_original(self):
  s=(S/'ActionSheet.kt').read_text()
  self.assertIn('queueTaskDisplayTitle(task.url, viewState.title)',s)
  self.assertIn('Text(text = url, style = MaterialTheme.typography.bodySmall)',s)
 def test_explicit_paste_preserves_multiple_selection_and_lines(self):
  s=(S/'configure/InputUrlDialog.kt').read_text()
  region=s.split('trailingIcon = {',1)[1].split('LazyRow(',1)[0]
  self.assertIn('PasteFromClipBoardButton { text ->',region)
  self.assertIn('findURLsFromString(text).distinct()',region)
  self.assertIn('if (links.size > 1)',region)
  self.assertIn('explicitlyPastedUrls = links',region)
  self.assertIn('showPasteDialog = true',region)
  self.assertIn('urlListFromClipboard = explicitlyPastedUrls ?: urlListFromClipboard',s)
  self.assertIn('maxLines = 3',s)
  self.assertNotIn('singleLine = true',s)
  self.assertIn('Action.ProceedWithURLs(it)',s)
 def test_preset_edit_icon_and_existing_template_scroller(self):
  s=(S/'configure/DownloadDialogV2.kt').read_text()
  region=s.split('Crossfade(showEditIcon',1)[1].split('}',1)[0]
  self.assertIn('imageVector = Icons.Outlined.Edit',region)
  self.assertNotIn('Icons.Outlined.MoreVert',s)
  self.assertIn('LazyRow(modifier = Modifier)',s)
if __name__=='__main__':unittest.main()
