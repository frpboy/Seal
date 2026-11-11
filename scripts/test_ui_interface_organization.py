from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1];S=R/'app/src/main/java/com/frpboy/seali/ui/page/settings'
class InterfaceOrganization(unittest.TestCase):
 def test_one_selector_with_existing_guard_and_confirm(self):
  i=(S/'interaction/InterfaceStylePreference.kt').read_text();a=(S/'appearance/AppearancePreferences.kt').read_text()
  self.assertIn('InterfaceStylePreference()', (S/'interaction/InteractionPreferencePage.kt').read_text())
  self.assertIn('onNavigateTo(Route.INTERACTION)',a)
  self.assertNotIn('InterfaceSwitchGuard.hasActiveDownloads',a)
  for t in ['InterfaceSwitchGuard.hasActiveDownloads(', 'downloader.getTaskStateMap().values.map { it.downloadState }', 'PreferenceUtil.updateUiStyle(selected)', '(context as? Activity)?.recreate()', 'TextButton(onClick = { onSelect(selected) })', 'rememberSaveable(current)', 'onClick = null']:
   self.assertIn(t,i)
 def test_current_setting_value_and_no_forced_update(self):
  i=(S/'interaction/InterfaceStylePreference.kt').read_text()
  self.assertIn('val currentUiStyle = remember { PreferenceUtil.getUiStyle() }',i)
  self.assertEqual(1,i.count('PreferenceUtil.updateUiStyle(selected)'))
  self.assertIn('else if (selected != currentUiStyle)',i)
  self.assertIn('onDismissRequest = onDismiss',i)
if __name__=='__main__':unittest.main()
