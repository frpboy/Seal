package com.frpboy.seali

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Prepared upgrade contract checks. This does not claim an installed-upgrade run. */
@RunWith(AndroidJUnit4::class)
class NamespaceComponentResolutionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Suppress("DEPRECATION")
    @Test fun newAndLegacyReceiverAreRealNonExportedComponents() {
        val manager=context.packageManager
        val current=manager.getReceiverInfo(ComponentName(context, NotificationActionReceiver::class.java),0)
        val legacy=manager.getReceiverInfo(ComponentName(context.packageName,"com.junkfood.seal.NotificationActionReceiver"),0)
        assertFalse(current.exported)
        assertFalse(legacy.exported)
        assertTrue(NotificationActionReceiver::class.java.isAssignableFrom(Class.forName(legacy.name)))
    }
    @Suppress("DEPRECATION")
    @Test fun legacyActivityNamesResolveToNewImplementationsWithoutExtraLaunchFilters() {
        val manager=context.packageManager
        listOf("MainActivity","QuickDownloadActivity").forEach { name ->
            val alias=manager.getActivityInfo(ComponentName(context.packageName,"com.junkfood.seal.$name"),0)
            assertEquals("com.frpboy.seali.$name",alias.targetActivity)
            assertTrue(alias.exported)
        }
        val launcher=manager.getLaunchIntentForPackage(context.packageName)
        assertEquals("com.frpboy.seali.MainActivity",launcher?.component?.className)
    }
    @Suppress("DEPRECATION")
    @Test fun appAndFileProviderKeepInstalledIdentityInsteadOfSourceNamespaceIdentity() {
        assertTrue(context.packageName.startsWith("com.frpboy.seali"))
        val provider=context.packageManager.resolveContentProvider(context.packageName+".provider",0)
        assertNotNull(provider)
        assertFalse(provider!!.exported)
        assertTrue(provider.grantUriPermissions)
        assertEquals("com.frpboy.seali.App",context.applicationInfo.className)
    }
}
