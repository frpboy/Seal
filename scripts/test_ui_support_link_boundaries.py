from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1];S=R/'app/src/main/java/com/frpboy/seali/ui/page/settings'
class SupportLinks(unittest.TestCase):
 def test_own_routes_and_upstream_credits_are_distinct(self):
  s=(S/'about/AboutPage.kt').read_text()
  self.assertIn('"https://github.com/frpboy/Seali/releases"',s)
  self.assertIn('"https://github.com/frpboy/Seali"',s)
  self.assertNotIn('githubIssueUrl',s);self.assertNotIn('githubSponsor',s)
  self.assertIn('"https://github.com/JunkFood02/Seal"',s)
  self.assertIn('Based on Seal by JunkFood02',s)
  self.assertIn('onNavigateToDonatePage()',s)
 def test_display_only_url_labels_preserve_destinations(self):
  s=(S/'about/AboutPage.kt').read_text()
  for name in ['portfolioUrl','githubProfileUrl','linkedinUrl','supportGroupUrl']:
   self.assertIn('aboutLinkLabel('+name+')',s)
   self.assertIn('openUrl('+name+')',s)
  self.assertIn('icon = Icons.Outlined.Inventory2',s)
  self.assertIn('AnnotatedString(context.packageName)',s)
 def test_technical_references_remain_not_mislabeled_as_own_tracker(self):
  s=(S/'troubleshooting/TroubleshootingPage.kt').read_text()
  for url in ['https://github.com/frpboy/Seali/issues','https://github.com/JunkFood02/Seal/issues/1399','https://github.com/yt-dlp/yt-dlp/issues/3766']:
   self.assertIn(url,s)
  for resource in ['seali_issue_tracker','seali_upstream_known_issues','seali_ytdlp_known_issues']:
   self.assertIn('R.string.'+resource,s)
 def test_current_interaction_summary_and_unique_strings(self):
  s=(S/'SettingsPage.kt').read_text()
  self.assertIn('R.string.seali_interaction_summary',s)
  root=ET.parse(R/'app/src/main/res/values/strings.xml').getroot();names=[e.attrib.get('name') for e in root if e.tag=='string']
  self.assertEqual(len(names),len(set(names)))
if __name__=='__main__':unittest.main()
