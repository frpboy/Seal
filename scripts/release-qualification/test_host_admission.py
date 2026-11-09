import pathlib
import stat
import unittest
from unittest.mock import Mock
from host_admission import admitted_kvm, valid_sha


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

    def test_missing_or_unstatable_device_fails(self):
        path = Mock()
        path.stat.side_effect = FileNotFoundError()
        self.assertFalse(admitted_kvm(path, True, True))


if __name__ == '__main__':
    unittest.main()
