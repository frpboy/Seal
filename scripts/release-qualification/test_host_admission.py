import pathlib
import os
import tempfile
import stat
import unittest
from unittest.mock import Mock, patch
from host_admission import admitted_kvm, valid_sha, main


class HostAdmissionTests(unittest.TestCase):
    def test_source_requires_full_lowercase_sha(self):
        self.assertTrue(valid_sha('a' * 40))
        for value in ['', 'a' * 7, 'a' * 41, 'A' * 40, 'z' * 40]:
            self.assertFalse(valid_sha(value))

    def test_regular_file_cannot_impersonate_kvm(self):
        path = Mock()
        path.stat.return_value.st_mode = stat.S_IFREG | 0o666
        self.assertFalse(admitted_kvm(path, True, True))

    def test_character_device_requires_both_accesses(self):
        path = Mock()
        path.stat.return_value.st_mode = stat.S_IFCHR | 0o666
        for r, w in [(False, False), (False, True), (True, False)]:
            self.assertFalse(admitted_kvm(path, r, w))
        self.assertTrue(admitted_kvm(path, True, True))

    def test_environment_cannot_redirect_emulator_executable(self):
        env = dict(EXPECTED_SOURCE_SHA='a' * 40, GITHUB_SHA='a' * 40,
                   GITHUB_REPOSITORY='frpboy/Seali', REPOSITORY_PRIVATE='false',
                   GITHUB_EVENT_NAME='workflow_dispatch', ANDROID_HOME='/tmp/attacker',
                   ANDROID_SDK_ROOT='/tmp/attacker')
        previous = os.getcwd()
        with tempfile.TemporaryDirectory() as temp:
            try:
                os.chdir(temp)
                with patch.dict(os.environ, env, clear=True), \
                     patch('host_admission.admitted_kvm', return_value=True), \
                     patch('pathlib.Path.is_file', return_value=True), \
                     patch('subprocess.run') as run:
                    run.return_value.returncode = 0
                    self.assertEqual(main(), 0)
                    run.assert_called_once_with(
                        ['/usr/local/lib/android/sdk/emulator/emulator', '-accel-check'],
                        shell=False, capture_output=True, timeout=30, check=False)
            finally:
                os.chdir(previous)

    def test_missing_or_unstatable_device_fails(self):
        path = Mock()
        path.stat.side_effect = FileNotFoundError()
        self.assertFalse(admitted_kvm(path, True, True))


if __name__ == '__main__':
    unittest.main()
