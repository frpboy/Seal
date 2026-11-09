#!/usr/bin/env python3
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class QueueProgressBoundaries(unittest.TestCase):
    def test_both_grid_and_list_use_actual_running_state(self):
        s=(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/DownloadPageV2.kt').read_text()
        self.assertEqual(s.count('(state.downloadState as? Running)?.let { QueueRunningProgress(it) }'),2)
    def test_renderer_uses_only_validated_fields(self):
        s=(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/downloadv2/QueueRunningProgress.kt').read_text()
        self.assertIn('measuredQueueProgress(state.progress)',s)
        self.assertIn('queueProgressTelemetry(state.progressText)',s)
        self.assertNotIn('Text(state.progressText',s)
        self.assertIn('if (progress == null) LinearProgressIndicator(',s)
        self.assertIn('telemetry.eta?.let',s)
if __name__=='__main__':unittest.main()
