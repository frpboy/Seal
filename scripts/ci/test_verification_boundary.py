"""Source-policy checks only. These do not execute GitHub Actions or Kotlin tests."""
from pathlib import Path
import unittest

WORKFLOW = Path(__file__).resolve().parents[2] / ".github/workflows/android_ci.yml"
SIGNING = "(github.event_name == 'workflow_dispatch' || github.event_name == 'workflow_call') && inputs.build_release == true"


class VerificationBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.source = WORKFLOW.read_text()

    def test_manual_release_input_is_opt_in(self):
        section = self.source.split("      build_release:\n", 1)[1].split("      run_history_ui:", 1)[0]
        self.assertIn("type: boolean", section)
        self.assertIn("default: false", section)

    def test_reusable_release_inputs_default_to_safe_values(self):
        section = self.source.split("  workflow_call:\n", 1)[1].split("\npermissions:", 1)[0]
        fields = (
            section.split("      build_release:\n", 1)[1].split("      run_history_ui:\n", 1)[0],
            section.split("      run_history_ui:\n", 1)[1].split("      publish_apk_artifacts:\n", 1)[0],
            section.split("      publish_apk_artifacts:\n", 1)[1],
        )
        for field in fields:
            self.assertIn("type: boolean", field)
            self.assertIn("default: false", field)

    def test_key_staging_requires_explicit_release_opt_in(self):
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
        self.assertLess(self.source.index("testGenericDebugUnitTest"), self.source.index("id: signing"))
        self.assertLess(self.source.index("./gradlew -p buildSrc test"), self.source.index("id: signing"))
        self.assertIn("persist-credentials: false", self.source)
        self.assertIn("./gradlew -PfunctionalAlphaCandidate testGenericDebugUnitTest", self.source)
        self.assertIn("python3 scripts/test_owned_namespace_boundaries.py", self.source)
        self.assertIn("python3 scripts/owned-source-prep/test-seali-prepare-owned-producer-inputs.py", self.source)
        self.assertIn("python3 scripts/release-qualification/test-compatible-alpha7-verifier.py", self.source)

    def test_maven_consumer_replaces_local_task_without_claiming_producer_coverage(self):
        root=WORKFLOW.parents[2]
        for name in ['android_ci.yml','alpha7-candidate.yml','classic-history-ui.yml']:
            text=(root/'.github/workflows'/name).read_text()
            self.assertNotIn(':frpboy-library:testDebugUnitTest',text)
            self.assertIn('38 owned-library producer units moved to',text)
            self.assertIn('Consumer contract CI staging awaits a source-bound producer artifact route.',text)
        self.assertNotIn('python3 scripts/build-contract/test-maven-inputs.py',self.source.split('  maven-consumer-qualification:',1)[0])

    def test_isolated_maven_workflow_is_manual_source_only(self):
        text=self.source.split('  maven-consumer-qualification:\n',1)[1]
        self.assertTrue(text.startswith("    if: github.event_name == 'workflow_dispatch' && inputs.qualify_maven_consumer == true\n"))
        self.assertIn("if: github.event_name != 'workflow_dispatch' || inputs.qualify_maven_consumer != true",self.source)
        self.assertIn('inputs.run_history_ui == true && inputs.qualify_maven_consumer != true',self.source)
        self.assertIn('ref: 486a5462431c4ba81a1e80f30dcb01952ec43c4c',text)
        self.assertIn('python3 scripts/build-contract/test-maven-inputs.py',text)
        self.assertIn(':app:compileGenericDebugKotlin :app:testGenericDebugUnitTest',text)
        for name in ['BUILD_RELEASE','PUBLISH_APK','HISTORY_UI']:
            self.assertIn('test "$'+name+'" = false',text)
        self.assertIn('packages: platform-tools',text)
        self.assertIn('expected_qualification_sha',self.source)
        self.assertNotIn('secrets.',text)
        self.assertNotIn('actions/upload-artifact',text)
        self.assertNotIn(':app:assemble',text)

    def test_upload_does_not_silently_enable_release(self):
        self.assertIn("inputs.publish_apk_artifacts == true && inputs.build_release != true", self.source)
        self.assertEqual(self.source.count("inputs.publish_apk_artifacts == true\n        uses: actions/upload-artifact@v4"), 6)
        self.assertIn("name: seali-checksums", self.source)
        self.assertIn("sha256sum -- *.apk > SHA256SUMS", self.source)


if __name__ == "__main__":
    unittest.main()
