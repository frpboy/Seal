package com.frpboy.seali
import com.frpboy.seali.ui.page.settings.about.aboutLinkLabel
import org.junit.Assert.*
import org.junit.Test
class AboutDisplayLabelsTest {
 @Test fun cosmeticLabelsDoNotChangeSourceString() {
  val original="https://www.example.com/profile/"
  assertEquals("example.com/profile",aboutLinkLabel(original))
  assertEquals("https://www.example.com/profile/",original)
 }
 @Test fun onlyLiteralPrefixesRemoved() {
  assertEquals("t.me/Group",aboutLinkLabel("https://t.me/Group"))
  assertEquals("http://www.example.com",aboutLinkLabel("http://www.example.com/"))
 }
}
