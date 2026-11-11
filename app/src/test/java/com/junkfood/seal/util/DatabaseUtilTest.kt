package com.junkfood.seal.util

import com.junkfood.seal.database.objects.CommandTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseUtilTest {
    @Test
    fun importsOnlyTemplatesMissingFromTheDatabase() {
        val existing = CommandTemplate(id = 1, name = "existing", template = "-f best")
        val newTemplate = CommandTemplate(id = 2, name = "new", template = "--write-info-json")

        assertEquals(
            listOf(newTemplate.copy(id = 0)),
            templatesToImport(listOf(existing, newTemplate), listOf(existing)),
        )
    }
}
