package com.frpboy.seali
import com.frpboy.seali.ui.page.downloadv2.configure.*
import org.junit.Assert.*
import org.junit.Test
class FormatSelectionSummaryTest {
 private fun stream(exact:Double?=null,approx:Double?=null,c:String?=null,v:String?=null,a:String?=null)=SelectedStreamSummary(exact,approx,c,v,a)
 @Test fun exactAndApproximateFiniteSizesSumOnlyCompleteSelection(){
  assertEquals(350L,selectedStreamBytes(listOf(stream(100.0),stream(250.0,999.0)),false))
  assertEquals(300L,selectedStreamBytes(listOf(stream(100.0),stream(null,200.0)),false))
 }
 @Test fun missingInvalidOrOverflowCannotBecomePlausibleTotal(){
  for(value in listOf(null,0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))
   assertNull(selectedStreamBytes(listOf(stream(100.0),stream(value)),false))
  assertNull(selectedStreamBytes(listOf(stream(Double.MAX_VALUE),stream(Double.MAX_VALUE)),false))
  assertNull(selectedStreamBytes(emptyList(),false))
 }
 @Test fun invalidExactCanUseValidApproximation(){assertEquals(200L,selectedStreamBytes(listOf(stream(Double.NaN,200.0)),false))}
 @Test fun clippingOrChapterSplitNeverPretendsFullStreamIsOutput(){assertNull(selectedStreamBytes(listOf(stream(1000.0)),true))}
 @Test fun badgesOnlyLiteralActualTokensNoUrlOrNotes(){
  assertEquals(listOf("webm","vp09.00.40.08","opus"),selectedStreamBadges(listOf(stream(c="webm",v="vp09.00.40.08",a="opus"),stream(c="webm",v="none"))))
  assertTrue(selectedStreamBadges(listOf(stream(c="https://host/token",v="cookie=secret",a="raw note with spaces"))).isEmpty())
 }
 @Test fun badgesBoundedAndNoInference(){
  assertTrue(selectedStreamBadges(listOf(stream())).isEmpty())
  assertEquals(8,selectedStreamBadges((1..20).map { stream(c="codec$it") }).size)
 }
}
