from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
class PresetResolution(unittest.TestCase):
 def test_material3_readonly_anchored_labeled_selector(self):
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/settings/format/FormatSettingDialogs.kt').read_text()
  s=s.split('fun VideoResolutionSelectField(',1)[1].split('fun VideoFormatPreferenceSelectField(',1)[0]
  for t in ['OutlinedTextField(', 'readOnly = true','MenuAnchorType.PrimaryNotEditable','R.string.video_resolution','ExposedDropdownMenu(','RES_HIGHEST..RES_LOWEST','onSelect(i)','expanded = false']:
   self.assertIn(t,s)
  self.assertNotIn('SealTextField(',s)
 def test_existing_draft_save_and_cancel_not_altered(self):
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/configure/DownloadDialogV2.kt').read_text()
  for t in ['remember(preferences) { mutableIntStateOf(preferences.videoResolution) }','onResolutionSelect = { res = it }','VIDEO_QUALITY.updateInt(res)','onDismissRequest = { showVideoPresetDialog = false }']:
   self.assertIn(t,s)
if __name__=='__main__':unittest.main()
