package com.junkfood.seal

import com.junkfood.seal.util.RuntimeUpdatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeUpdatePolicyTest {
    @Test fun `all boolean policy combinations enforce explicit F-Droid consent`() {
        for (play in listOf(false, true))
            for (fdroid in listOf(false, true))
                for (enabled in listOf(false, true))
                    for (consent in listOf(false, true))
                        for (manual in listOf(false, true)) {
                            val expected = when {
                                play -> false
                                !fdroid -> true
                                manual -> true
                                !enabled -> false
                                else -> consent
                            }
                            assertEquals(expected, RuntimeUpdatePolicy.mayDownload(
                                play, fdroid, enabled, consent, manual))
                        }
    }
    @Test fun `legacy auto true is not F-Droid consent`() {
        assertFalse(RuntimeUpdatePolicy.mayDownload(false, true, true, false, false))
    }
    @Test fun `manual one shot works without automatic consent`() {
        assertTrue(RuntimeUpdatePolicy.mayDownload(false, true, false, false, true))
        assertFalse(RuntimeUpdatePolicy.mayDownload(false, true, false, false, false))
    }
    @Test fun `turning automatic updates off stops stored consent from admitting automatic downloads`() {
        assertFalse(RuntimeUpdatePolicy.mayDownload(false, true, false, true, false))
    }
}
