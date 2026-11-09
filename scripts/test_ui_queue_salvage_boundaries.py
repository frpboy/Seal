#!/usr/bin/env python3
from pathlib import Path
import hashlib
import unittest
ROOT=Path(__file__).resolve().parents[1]
class QueueSalvageBoundaries(unittest.TestCase):
    def test_exact_current_reviewed_predicate_salvaged_without_workflow(self):
        path='app/src/main/java/com/junkfood/seal/ui/page/downloadv2/DownloadPageV2.kt'
        source=(ROOT/path).read_text()
        start='    fun predict(entry: Pair<Task, Task.State>)'
        self.assertEqual(hashlib.sha256(source.split(start,1)[1].split('sealed interface UiAction',1)[0].encode()).hexdigest(),'dae32a2355704b5b991b8b7d0334c5661a269214124831a5cc831f24f4dfdc1d')
    def test_exact_unit_tests_preserved(self):
        path='app/src/test/java/com/junkfood/seal/ui/page/downloadv2/QueueFilterTest.kt'
        self.assertEqual(hashlib.sha256((ROOT/path).read_bytes()).hexdigest(),'58a8a87d53c30dc200b20462e972543c99f7c1c167437bfa9a86bd484f6da778')
    def test_finished_points_to_session_and_saved_downloads(self):
        source=(ROOT/'app/src/main/java/com/junkfood/seal/ui/page/downloadv2/QueueEmptyState.kt').read_text()
        self.assertIn('Filter.Finished -> R.string.queue_no_finished_this_session',source)
        self.assertIn('Filter.Finished -> R.string.queue_saved_history_hint',source)
if __name__=='__main__':unittest.main()
