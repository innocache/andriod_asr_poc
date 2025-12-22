package com.k2fsa.sherpa.onnx.vad.asr

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineOnlyManifestTest {

    @Test
    fun appDoesNotRequestInternetPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager

        @Suppress("DEPRECATION")
        val pkgInfo = packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        val requested = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertFalse(
            "Offline-only PoC must not request android.permission.INTERNET",
            requested.contains("android.permission.INTERNET")
        )
    }
}
