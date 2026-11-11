#!/usr/bin/env python3
"""Source wiring checks only. Actual dialog events/pixels require Android qualification."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
class NightlyContract(unittest.TestCase):
 def test_default_and_effective_consumer(self):
  s=(R/'app/src/main/java/com/frpboy/seali/util/PreferenceUtil.kt').read_text()
  self.assertIn('YT_DLP_UPDATE_CHANNEL to YT_DLP_STABLE',s)
  self.assertIn('kv.encode(YT_DLP_CHANNEL_SELECTION, NightlyChannelPolicy.encode(selection))',s)
  self.assertIn('when (PreferenceUtil.effectiveYtdlpUpdateChannel())',(R/'app/src/main/java/com/frpboy/seali/util/UpdateUtil.kt').read_text())
 def test_no_warning_callback_persists_choice(self):
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/settings/general/YtdlpUpdateDialog.kt').read_text()
  warning=s[s.index('    if (showNightlyWarning)'):s.index('    val scope =')]
  self.assertNotIn('updateBoolean',warning)
  self.assertNotIn('saveYtdlpChannelSelection',warning)
  self.assertIn('NightlyChannelSelection(YT_DLP_NIGHTLY, true, dontWarnPending)',warning)
  self.assertIn('onDismissRequest = { denyNightly() }',warning)
  self.assertEqual(s.count('PreferenceUtil.saveYtdlpChannelSelection(selection)'),1)
 def test_denial_resets_only_pending_state(self):
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/settings/general/YtdlpUpdateDialog.kt').read_text()
  denial=s[s.index('    fun denyNightly()'):s.index('    if (showNightlyWarning)')]
  self.assertIn('selection = NightlyChannelSelection()',denial)
  self.assertIn('dontWarnPending = false',denial)
  self.assertNotIn('PreferenceUtil',denial)
 def test_strings_checkbox_accessibility(self):
  values={e.attrib.get('name'):e.text for e in ET.parse(R/'app/src/main/res/values/strings.xml').getroot()}
  for key in ['runtime_nightly_warning_title','runtime_nightly_warning_body','runtime_nightly_dont_show_again','runtime_nightly_save_hint','runtime_nightly_allow','runtime_channel_pending_hint']:self.assertTrue(values[key])
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/settings/general/YtdlpUpdateDialog.kt').read_text()
  self.assertIn('heightIn(min = 48.dp).toggleable(',s)
  self.assertIn('role = Role.Checkbox',s)
  self.assertIn('Checkbox(checked = dontWarnPending, onCheckedChange = null)',s)
if __name__=='__main__':unittest.main()
