from pathlib import Path
import json,hashlib,unittest
R=Path(__file__).resolve().parents[1];N=R/'app/src/main/assets/functional-notices'
class NoticeAssets(unittest.TestCase):
 def test_credits_reader_stays_path_lines(self):
  s=(R/'app/src/main/java/com/frpboy/seali/ui/page/settings/about/CreditsPage.kt').read_text()
  self.assertIn('open(base + "notice-index.txt").bufferedReader().use { it.readLines() }',s)
  self.assertIn('context.assets.open(base + name)',s)
 def test_every_index_line_is_existing_text_asset(self):
  names=(N/'notice-index.txt').read_text().splitlines();self.assertEqual(names,sorted(set(names)));self.assertTrue(names)
  for name in names:
   p=Path(name);self.assertFalse(p.is_absolute());self.assertNotIn('..',p.parts);self.assertTrue((N/name).is_file());(N/name).read_text()
 def test_hash_inventory_matches_ui_paths_and_bytes(self):
  rows=json.loads((N/'notice-inventory.json').read_text());self.assertEqual([r['path'] for r in rows],(N/'notice-index.txt').read_text().splitlines())
  for row in rows:
   b=(N/row['path']).read_bytes();self.assertEqual(len(b),row['bytes']);self.assertEqual(hashlib.sha256(b).hexdigest(),row['sha256'])
 def test_schema2_notice_references_and_limits(self):
  x=json.loads((N/'source-ledger.json').read_text());self.assertEqual(x['schema'],2);self.assertFalse(x['qualification']['notices_complete']);self.assertFalse(x['qualification']['source_delivery_complete'])
  for component in x['components']:
   for row in component['notices']:
    b=(N/row['path']).read_bytes();self.assertEqual(len(b),row['bytes']);self.assertEqual(hashlib.sha256(b).hexdigest(),row['sha256'])
if __name__=='__main__':unittest.main()
