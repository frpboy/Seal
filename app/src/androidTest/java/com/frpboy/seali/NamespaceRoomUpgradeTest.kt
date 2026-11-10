package com.frpboy.seali

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frpboy.seali.database.AppDatabase
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Opens actual pre-move exported v5 schema with the new Room class, no destructive fallback. */
@RunWith(AndroidJUnit4::class)
class NamespaceRoomUpgradeTest {
    @Test fun movedClassOpensExistingSchemaAndPreservesRows() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="namespace-upgrade-fixture.db"
        context.deleteDatabase(name)
        try {
            val schema=JSONObject(context.assets.open("migration/legacy-room-v5.json").bufferedReader().use { it.readText() }).getJSONObject("database")
            context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null).use { old ->
                val entities=schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity=entities.getJSONObject(i)
                    old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
                }
                val setup=schema.getJSONArray("setupQueries")
                for(i in 0 until setup.length()) old.execSQL(setup.getString(i))
                old.execSQL("INSERT INTO CommandTemplate (id,name,template) VALUES (7,'fixture','--fixture')")
                old.version=5
            }
            val room=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
            try {
                room.openHelper.writableDatabase.query("SELECT name,template FROM CommandTemplate WHERE id=7").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("fixture",cursor.getString(0))
                    assertEquals("--fixture",cursor.getString(1))
                }
                assertEquals(5,room.openHelper.writableDatabase.version)
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
