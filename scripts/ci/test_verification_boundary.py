"""Source-policy checks only. These do not execute GitHub Actions or Kotlin tests."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[2] / ".github/workflows/android_ci.yml"
SIGNING = "github.event_name == 'workflow_dispatch' && inputs.build_release == true"


class VerificationBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.source = WORKFLOW.read_text()

    def test_manual_release_input_is_opt_in(self):
        section = self.source.split("      build_release:\n", 1)[1].split("      run_history_ui:", 1)[0]
        self.assertIn("type: boolean", section)
        self.assertIn("default: false", section)

    def test_key_staging_requires_manual_opt_in(self):
        section = self.source.split("      - name: Validate and stage private Play upload key\n", 1)[1]
        self.assertTrue(section.startswith("        if: " + SIGNING + "\n"))
        self.assertNotIn('if [[ "$GITHUB_EVENT_NAME" == "pull_request"', section)

    def test_every_signing_secret_is_in_a_guarded_step(self):
        steps = self.source.split("      - ")
        secret_steps = [step for step in steps if "secrets.SEALI_UPLOAD_" in step]
        self.assertEqual(len(secret_steps), 4)
        for step in secret_steps:
            self.assertTrue(
                "if: " + SIGNING in step or "if: steps.signing.outputs.available == 'true'" in step
            )

    def test_release_builds_depend_on_successful_signing(self):
        for name in ["Build generic release APK variants", "Build Google Play App Bundle"]:
            section = self.source.split("      - name: " + name + "\n", 1)[1]
            self.assertTrue(section.startswith("        if: steps.signing.outputs.available == 'true'\n"))

    def test_private_key_cleanup_is_scoped_and_unconditional_after_opt_in(self):
        section = self.source.split("      - name: Remove private signing material\n", 1)[1]
        self.assertTrue(section.startswith("        if: always() && " + SIGNING + "\n"))

    def test_actual_unit_commands_remain_before_key_staging(self):
        self.assertLess(self.source.index("./gradlew testGenericDebugUnitTest"), self.source.index("id: signing"))
        self.assertLess(self.source.index("./gradlew -p buildSrc test"), self.source.index("id: signing"))
        self.assertIn("persist-credentials: false", self.source)

    def test_upload_does_not_silently_enable_release(self):
        self.assertIn("inputs.publish_apk_artifacts == true && inputs.build_release != true", self.source)
        self.assertEqual(self.source.count("inputs.publish_apk_artifacts == true\n        uses: actions/upload-artifact@v4"), 5)


if __name__ == "__main__":
    unittest.main()
