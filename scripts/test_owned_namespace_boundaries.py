#!/usr/bin/env python3
from pathlib import Path
import hashlib
import json
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
A='{http://schemas.android.com/apk/res/android}'
class OwnedNamespaceBoundaries(unittest.TestCase):
    def test_owned_source_and_gradle_namespaces_moved(self):
        for source_set in ['main','test','androidTest']:
            for p in (ROOT/f'app/src/{source_set}/java').rglob('*.kt'):
                s=p.read_text()
                self.assertNotIn('import com.junkfood.seal',s,str(p))
                if 'package com.junkfood.seal' in s:
                    self.assertEqual(p.relative_to(ROOT).as_posix(),'app/src/main/java/com/junkfood/seal/NotificationActionReceiver.kt')
                    self.assertIn('class NotificationActionReceiver : com.frpboy.seali.NotificationActionReceiver()',s)
        app=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('namespace = "com.frpboy.seali"',app)
        self.assertIn('applicationId = "com.frpboy.seali"',app)
        self.assertIn('applicationIdSuffix = ".debug"',app)
        self.assertIn('applicationIdSuffix = ".preview"',app)
        self.assertIn('versionCode = 200_000_160',app)
        self.assertIn('namespace = "com.frpboy.seali.color"',(ROOT/'color/build.gradle.kts').read_text())
    def test_real_legacy_receiver_and_supported_activity_aliases(self):
        app=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot().find('application')
        self.assertEqual(app.attrib[A+'name'],'.App')
        receivers={e.attrib[A+'name']:e for e in app.findall('receiver')}
        for name in ['.NotificationActionReceiver','com.junkfood.seal.NotificationActionReceiver']:
            self.assertEqual(receivers[name].attrib[A+'exported'],'false')
        aliases={e.attrib[A+'name']:e for e in app.findall('activity-alias')}
        for name in ['MainActivity','QuickDownloadActivity']:
            alias=aliases['com.junkfood.seal.'+name]
            self.assertEqual(alias.attrib[A+'targetActivity'],'.'+name)
            self.assertEqual(alias.findall('intent-filter'),[])
        self.assertEqual(app.find('provider').attrib[A+'authorities'],'${applicationId}.provider')
    def test_queue_backup_write_gate_and_visible_error(self):
        s=(ROOT/'app/src/main/java/com/frpboy/seali/util/PreferenceUtil.kt').read_text()
        self.assertIn('taskBackupGuard.encodeForStorage(map)',s)
        self.assertIn('.onSuccess { kv.encode(TASK_LIST, it) }',s)
        self.assertIn('taskBackupGuard.restore(kv.decodeString(TASK_LIST))',s)
        self.assertIn('R.string.queue_restore_failed_preserved',s)
        self.assertNotIn('kv.removeValueForKey(TASK_LIST)',s)
        self.assertIn('DATABASE_NAME = "app_database"',(ROOT/'app/src/main/java/com/frpboy/seali/util/DatabaseUtil.kt').read_text())
    def test_schema_history_and_dependency_coordinates_retained(self):
        manifest=json.loads((ROOT/'docs/migration/schema-custody.json').read_text())
        for name,expected in manifest.items():
            self.assertEqual(hashlib.sha256((ROOT/'app/schemas/com.frpboy.seali.database.AppDatabase'/name).read_bytes()).hexdigest(),expected)
        self.assertFalse((ROOT/'app/schemas/com.junkfood.seal.database.AppDatabase').exists())
        self.assertIn('io.github.junkfood02.youtubedl-android',(ROOT/'gradle/libs.versions.toml').read_text())
        self.assertIn('Based on Seal by JunkFood02',(ROOT/'app/src/main/java/com/frpboy/seali/ui/page/settings/about/AboutPage.kt').read_text())
if __name__=='__main__':unittest.main()
