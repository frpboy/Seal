package com.junkfood.seal

import org.junit.Assert.assertTrue

/** Always-on test oracles, shared by positive verification and negative control. */
internal object ContinuityOracles {
    fun versionExceedsAlpha5(versionCode: Long) {
        assertTrue("versionCode $versionCode not greater than alpha5 baseline", versionCode > 200_000_150L)
    }

    fun historyContainsMarker(paths: List<String>, marker: String) {
        assertTrue("Room download history lost across upgrade", paths.any { it == marker })
    }
}
