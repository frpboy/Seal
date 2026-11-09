#!/usr/bin/env python3
"""Source-boundary checks, not Android execution or network/UI qualification."""
import pathlib
import unittest
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / 'app/src/main/java/com/junkfood/seal'

class RuntimeConsentBoundaryTests(unittest.TestCase):
    def test_central_gate_precedes_external_update(self):
        text = (SRC / 'util/UpdateUtil.kt').read_text()
        self.assertIn('manualConsent: Boolean = false', text)
        self.assertLess(text.index('RuntimeUpdatePolicy.mayDownload'), text.index('.updateYoutubeDL('))
        self.assertIn('automaticConsent = FDROID_RUNTIME_AUTO_UPDATE_CONSENT.getBoolean()', text)

    def test_old_auto_preference_does_not_create_consent(self):
        text = (SRC / 'util/PreferenceUtil.kt').read_text()
        self.assertIn('FDROID_RUNTIME_AUTO_UPDATE_CONSENT to false', text)
        self.assertIn('YT_DLP_AUTO_UPDATE to true', text)  # other flavors unchanged

    def test_empty_version_bootstrap_is_after_fdroid_opt_in_gate(self):
        text = (SRC / 'ui/page/YtdlpUpdater.kt').read_text()
        self.assertLess(text.index('!FDROID_RUNTIME_AUTO_UPDATE_CONSENT.getBoolean()'),
                        text.index('YT_DLP_VERSION.getString().isNotEmpty()'))
        self.assertLess(text.index('!FDROID_RUNTIME_AUTO_UPDATE_CONSENT.getBoolean()'),
                        text.index('isNetworkAvailableForDownload()'))

    def test_all_manual_callers_prompt_before_the_download(self):
        callers = []
        for path in SRC.rglob('*.kt'):
            text = path.read_text()
            if 'UpdateUtil.updateYtDlp(' in text:
                callers.append(path.relative_to(SRC).as_posix())
                if 'manualConsent = true' in text:
                    self.assertLess(text.index('if (!requestRuntimeConsent(false)) return@launch'),
                                    text.index('UpdateUtil.updateYtDlp(manualConsent = true)'))
        self.assertEqual(sorted(callers), sorted([
            'ui/page/YtdlpUpdater.kt',
            'ui/page/settings/general/GeneralDownloadPreferences.kt',
            'ui/page/settings/troubleshooting/TroubleshootingPage.kt',
        ]))

    def test_disclosure_and_equal_refusal_controls_are_present(self):
        xml = ET.parse(ROOT / 'app/src/main/res/values/classic_strings.xml')
        strings = {el.attrib.get('name'): el.text for el in xml.getroot()}
        for key in ['runtime_update_consent_once', 'runtime_update_consent_automatic']:
            for phrase in ['executable yt-dlp code', 'yt-dlp-nightly-builds', 'F-Droid review',
                           'keep using the bundled version']:
                # Automatic wording says 'execution of yt-dlp code', checked separately.
                if key.endswith('automatic') and phrase == 'executable yt-dlp code':
                    phrase = 'execution of yt-dlp code'
                self.assertIn(phrase, strings[key])
        text = (SRC / 'ui/page/settings/general/RuntimeUpdateConsent.kt').read_text()
        self.assertEqual(text.count('TextButton(onClick = { finish('), 2)
        self.assertIn('onDismissRequest = { finish(false) }', text)
        self.assertIn('onDispose { pending?.complete(false) }', text)

if __name__ == '__main__':
    unittest.main()
