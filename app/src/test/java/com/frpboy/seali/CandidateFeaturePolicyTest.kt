package com.frpboy.seali

import com.frpboy.seali.util.CandidateFeaturePolicy
import org.junit.Assert.*
import org.junit.Test

class CandidateFeaturePolicyTest {
    @Test fun `source-limited candidate does not enable commands or app updates`() {
        assertFalse(CandidateFeaturePolicy.CUSTOM_COMMANDS_AVAILABLE)
        assertFalse(CandidateFeaturePolicy.APP_UPDATES_AVAILABLE)
    }
    @Test fun `saved custom true and false remain stored but cannot disable normal candidate downloads`() {
        for (saved in listOf(false, true)) {
            assertFalse(CandidateFeaturePolicy.customCommandsEnabled(saved))
        }
    }
    @Test fun `update attempt fails explicitly before selection download or install`() {
        try {
            CandidateFeaturePolicy.requireAppUpdatesAvailable()
            fail("Candidate must not offer an unqualified app update")
        } catch (e: IllegalStateException) {
            assertEquals(CandidateFeaturePolicy.UPDATE_UNAVAILABLE, e.message)
            assertTrue(e.message!!.contains("signing certificate"))
            assertTrue(e.message!!.contains("version-code"))
        }
    }
}
