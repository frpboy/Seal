#!/usr/bin/env python3
"""Keep the in-app release checker pointed at the Seali repository."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/frpboy/seali/util/UpdateUtil.kt"
PLAN = ROOT / "scripts/release-qualification/PLAN.md"


class ReleaseRepositoryTarget(unittest.TestCase):
    def test_release_queries_target_seali(self):
        source = SOURCE.read_text()
        self.assertIn('private const val OWNER = "frpboy"', source)
        self.assertIn('private const val REPO = "Seali"', source)
        self.assertIn('https://api.github.com/repos/${OWNER}/${REPO}/releases/latest', source)
        self.assertIn('https://api.github.com/repos/${OWNER}/${REPO}/releases', source)
        self.assertNotIn('https://api.github.com/repos/frpboy/Seal/', source)

    def test_upgrade_plan_uses_private_alpha6_baseline(self):
        plan = PLAN.read_text()
        self.assertIn("exact recovered private alpha.6 APK", plan)
        self.assertIn("Synthetic Room/schema/backup tests do not prove this installed alpha.6 upgrade.", plan)
        self.assertNotIn("8. Upgrade: install alpha.5", plan)


if __name__ == "__main__":
    unittest.main()
