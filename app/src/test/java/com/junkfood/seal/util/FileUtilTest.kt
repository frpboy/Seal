package com.junkfood.seal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilTest {
    @Test
    fun taskConfigFilesAreStableAndTaskSpecific() {
        val first = FileUtil.taskConfigFileName("first-task")

        assertTrue(first.matches(Regex("config-[0-9a-f]{64}\\.txt")))
        assertEquals(first, FileUtil.taskConfigFileName("first-task"))
        assertNotEquals(first, FileUtil.taskConfigFileName("second-task"))
    }
}
