from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
S=R/'app/src/main/java/com/frpboy/seali'
class SettingDependencyHints(unittest.TestCase):
 def test_hints_are_visible_items_not_disabled_controls(self):
  s=(S/'ui/page/settings/format/DownloadFormatPreferences.kt').read_text()
  for predicate,resource in [
   ('!isCustomCommandEnabled && !audioSwitch','format_requires_audio_hint'),
   ('!isCustomCommandEnabled && audioSwitch && !embedMetadata','artwork_requires_metadata_hint'),
   ('!isCustomCommandEnabled && audioSwitch','video_requires_video_hint'),
   ('!isCustomCommandEnabled && !audioSwitch && isFormatSortingEnabled','video_uses_sorting_hint'),
   ('!isCustomCommandEnabled && !isFormatSelectionEnabled','advanced_requires_selection_hint')]:
   self.assertIn('if ('+predicate+') item {\n                    PreferenceInfo(text = stringResource(R.string.'+resource+'))\n                }',s)
 def test_original_restrictions_and_custom_command_hint_remain(self):
  s=(S/'ui/page/settings/format/DownloadFormatPreferences.kt').read_text()
  for predicate in ['enabled = audioSwitch && !isCustomCommandEnabled','enabled = embedMetadata && audioSwitch && !isCustomCommandEnabled','enabled = !audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled','enabled = !isCustomCommandEnabled && isFormatSelectionEnabled']:
   self.assertIn(predicate,s)
  self.assertIn('R.string.custom_command_enabled_hint',s)
 def test_download_type_uses_existing_preference_and_icon(self):
  s=(S/'ui/page/settings/interaction/InteractionPreferencePage.kt').read_text()
  self.assertIn('icon = Icons.Outlined.Download',s)
  self.assertIn('DOWNLOAD_TYPE_INITIALIZATION.updateInt(it)',s)
 def test_strings_unique_and_no_path_rewrite(self):
  root=ET.parse(R/'app/src/main/res/values/strings.xml').getroot()
  names=[e.attrib.get('name') for e in root if e.tag=='string']
  self.assertEqual(len(names),len(set(names)))
  values={e.attrib.get('name'):e.text for e in root}
  for name in ['format_requires_audio_hint','artwork_requires_metadata_hint','video_requires_video_hint','video_uses_sorting_hint','advanced_requires_selection_hint']:
   self.assertNotIn('/Download',values[name])
if __name__=='__main__':unittest.main()
