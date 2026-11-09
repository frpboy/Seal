#!/usr/bin/env python3
"""Source-boundary assertions supplement real helper JUnit tests, not Android UI tests."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1] / "app/src/main/java/com/frpboy/seali"
class SafeReportRouting(unittest.TestCase):
    def test_shared_formatter_routes_without_nullable_message(self):
        text = (ROOT / "util/TextUtil.kt").read_text()
        section = text.split("fun getErrorReport(", 1)[1].split("@Deprecated", 1)[0]
        self.assertIn("SafeErrorReport.copyReport(App.getVersionReport(), th, url)", section)
        self.assertNotIn("th.message", section)
    def test_queue_and_classic_keep_shared_route(self):
        for name in ["DownloadPageV2.kt", "classic/ClassicHomePage.kt"]:
            text = (ROOT / "ui/page/downloadv2" / name).read_text()
            self.assertIn("getErrorReport(action.throwable, task.url)", text)
    def test_legacy_error_state_uses_safe_summary(self):
        text = (ROOT / "Downloader.kt").read_text()
        self.assertEqual(2, text.count("errorReport = SafeErrorReport.describe(th)"))
        self.assertNotIn("th.message.toString()", text)
    def test_configuration_display_and_copy_route(self):
        text = (ROOT / "ui/page/downloadv2/configure/DownloadDialogV2.kt").read_text()
        self.assertIn("SafeErrorReport.describe(state.throwable)", text)
        self.assertIn("SafeErrorReport.copyReport(App.getVersionReport(), state.throwable, url)", text)
        self.assertNotIn("state.throwable.message", text)
if __name__ == "__main__":
    unittest.main()
