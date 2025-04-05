package com.android.launcher3.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.launcher3.Utilities

object ActivityLauncher {

    @JvmStatic
    fun launchSearch(context: Context) {
        if (Utilities.isPixelSearchInstalled(context)) {
            launchPixelSearch(context)
            return
        }
        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (Utilities.isGsaInstalled(context)) {
                action = "android.search.action.GLOBAL_SEARCH"
                setPackage(Utilities.GSA_PACKAGE)
            } else {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://www.google.com/search?q=")
            }
        }

        runCatching { context.startActivity(intent) }
    }

    @JvmStatic
    fun launchLensSearch(context: Context) {
        val lensIntent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            data = Uri.parse(Utilities.LENS_URI)
            putExtra("LensHomescreenShortcut", true)
        }

        runCatching { context.startActivity(lensIntent) }
    }

    @JvmStatic
    fun launchPixelSearch(context: Context) {
        val intent = Intent().apply {
            setPackage(Utilities.PIXEL_SEARCH_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        runCatching { context.startActivity(intent) }
    }
}
