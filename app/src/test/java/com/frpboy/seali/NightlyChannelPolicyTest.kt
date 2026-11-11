package com.frpboy.seali

import com.frpboy.seali.util.NightlyChannelSelection
import com.frpboy.seali.util.NightlyChannelPolicy
import com.frpboy.seali.util.YT_DLP_NIGHTLY
import com.frpboy.seali.util.YT_DLP_STABLE
import org.junit.Assert.*
import org.junit.Test

class NightlyChannelPolicyTest {
    @Test fun inheritedNightlyDoesNotCountAsConsent() {
        assertEquals(YT_DLP_STABLE, NightlyChannelPolicy.effectiveChannel(YT_DLP_NIGHTLY, false))
    }
    @Test fun stableRemainsStableRegardlessOfOldConsent() {
        for (allowed in listOf(true, false)) assertEquals(YT_DLP_STABLE, NightlyChannelPolicy.effectiveChannel(YT_DLP_STABLE, allowed))
    }
    @Test fun onlyExplicitAllowedNightlyIsEffective() {
        assertEquals(YT_DLP_NIGHTLY, NightlyChannelPolicy.effectiveChannel(YT_DLP_NIGHTLY, true))
    }
    @Test fun unknownSavedChannelFallsBackStable() {
        assertEquals(YT_DLP_STABLE, NightlyChannelPolicy.effectiveChannel(99, true))
    }
    @Test fun warningRepeatsUnlessAllowedSuppressionWasSaved() {
        assertTrue(NightlyChannelPolicy.needsWarning(false))
        assertFalse(NightlyChannelPolicy.needsWarning(true))
    }
    @Test fun absentOrMalformedRecordIsStableWithoutConsent() {
        for (raw in listOf(null, "", "{broken")) assertEquals(NightlyChannelSelection(), NightlyChannelPolicy.decode(raw))
    }
    @Test fun channelAllowAndSuppressionRoundTripTogether() {
        val allowed = NightlyChannelSelection(YT_DLP_NIGHTLY, true, true)
        assertEquals(allowed, NightlyChannelPolicy.decode(NightlyChannelPolicy.encode(allowed)))
    }
    @Test fun cannotSaveNightlyOrSuppressionWithoutAllow() {
        assertTrue(runCatching { NightlyChannelPolicy.encode(NightlyChannelSelection(YT_DLP_NIGHTLY)) }.isFailure)
        assertTrue(runCatching { NightlyChannelPolicy.encode(NightlyChannelSelection(dontWarnAgain = true)) }.isFailure)
    }
    @Test fun inconsistentStoredRecordCannotEnableNightlyOrSuppressWarning() {
        val decoded = NightlyChannelPolicy.decode("""{"channel":1,"nightlyAllowed":false,"dontWarnAgain":true}""")
        assertEquals(NightlyChannelSelection(), decoded)
    }
}
