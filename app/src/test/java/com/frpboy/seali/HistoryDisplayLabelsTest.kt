package com.frpboy.seali

import com.frpboy.seali.ui.component.historyFileExtension
import com.frpboy.seali.ui.component.historyProviderLabel
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class HistoryDisplayLabelsTest {
 @Test fun onlyLiteralFileExtensionShown() {
  assertEquals("MP4",historyFileExtension("/storage/private/video.Mp4"))
  assertEquals("M4A",historyFileExtension("/storage/private/audio.m4a"))
  assertNull(historyFileExtension("/private/noextension"))
 }
 @Test fun invalidExtensionsAndQueryWithheld() {
  for(path in listOf("/private/file.mp4?token=secret","/private/file.verylongextension","/private/file.mp4\nsecret","/private/file.","/private/folder/"))
   assertNull(historyFileExtension(path))
 }
 @Test fun displayOnlyExactYouTubeBrandAndOtherExtractorUntouched() {
  assertEquals("YouTube",historyProviderLabel("Youtube"))
  assertEquals("YouTube",historyProviderLabel("youtube"))
  for(label in listOf("youtube:tab","Unknown","Example site"))assertEquals(label,historyProviderLabel(label))
 }
 @Test fun stableAcrossLocale() {
  val previous=Locale.getDefault()
  try { Locale.setDefault(Locale.forLanguageTag("tr-TR"));assertEquals("AVI",historyFileExtension("a.avi")) }
  finally { Locale.setDefault(previous) }
 }
}
