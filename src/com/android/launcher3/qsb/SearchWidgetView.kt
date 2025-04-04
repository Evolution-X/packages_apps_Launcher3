/*
 * Copyright (C) 2025 AxionAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.qsb

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView

import androidx.core.content.ContextCompat

import com.android.launcher3.R
import com.android.launcher3.DeviceProfile
import com.android.launcher3.LauncherPrefChangeListener
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.util.Themes
import com.android.launcher3.Utilities
import com.android.launcher3.views.ActivityContext

import com.android.internal.util.android.Utils

class SearchWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), LauncherPrefChangeListener {

    private lateinit var searchIcon: ImageView
    private lateinit var lensIcon: ImageButton
    private lateinit var micIcon: ImageView

    private val dp = (ActivityContext.lookupContext(context) as ActivityContext).getDeviceProfile()
    private val isGoogleInstalled = Utils.isPackageInstalled(context, Utilities.GSA_PACKAGE)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        searchIcon = findViewById(R.id.g_icon)
        lensIcon = findViewById(R.id.lens_icon)
        micIcon = findViewById(R.id.mic_icon)

        lensIcon.visibility = if (isGoogleInstalled) View.VISIBLE else View.GONE

        setClickListeners()
        LauncherPrefs.Companion.get(context).addListener(this, LauncherPrefs.THEMED_ICONS)
        updateColors()
    }

    override fun onPrefChanged(str: String) {
        if ("themed_icons".equals(str)) {
            updateColors()
        }
    }

    fun updateColors() {
        val attrColor: Int
        val style: Int
        val themedContext: Context

        if (Themes.isThemedIconEnabled(context)) {
            attrColor = Themes.getAttrColor(context, R.attr.qsbFillColorThemedAllApps)
            style = R.style.QsbIconTint_Themed
        } else {
            attrColor = Themes.getAttrColor(context, R.attr.qsbFillColor)
            style = R.style.QsbIconTint
        }

        themedContext = ContextThemeWrapper(context, style)

        background?.setTint(attrColor)

        val searchDrawable = ContextCompat.getDrawable(themedContext, if (isGoogleInstalled) R.drawable.ic_super_g_color else R.drawable.ic_allapps_search)!!
        searchIcon.setImageDrawable(searchDrawable)

        val lensDrawable = ContextCompat.getDrawable(themedContext, R.drawable.ic_lens_color)!!
        lensIcon.setImageDrawable(lensDrawable)

        val micDrawable = ContextCompat.getDrawable(themedContext, R.drawable.ic_mic_color)!!
        micIcon.setImageDrawable(micDrawable)
    }

    override fun onDetachedFromWindow() {
        LauncherPrefs.Companion.get(context).removeListener(this, LauncherPrefs.THEMED_ICONS)
    }

    private fun setClickListeners() {
        setOnClickListener { launchSearch() }
        searchIcon.setOnClickListener { launchSearch() }
        lensIcon.setOnClickListener { launchLensSearch() }
    }

    private fun launchSearch() {
        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (isGoogleInstalled) {
                action = "android.search.action.GLOBAL_SEARCH"
                setPackage(Utilities.GSA_PACKAGE)
            } else {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://www.google.com/search?q=")
            }
        }

        runCatching { context.startActivity(intent) }
    }

    private fun launchLensSearch() {
        val lensIntent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            data = Uri.parse(Utilities.LENS_URI)
            putExtra("LensHomescreenShortcut", true)
        }

        runCatching { context.startActivity(lensIntent) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hotseatPadding = dp.getHotseatLayoutPadding(context)
        val width = dp.availableWidthPx - hotseatPadding.left - hotseatPadding.right

        val horizontalInset = if (dp.isQsbInline) {
            dp.getAllAppsIconStartMargin(context)
        } else {
            val cellWidth = DeviceProfile.calculateCellWidth(
                width, dp.hotseatBorderSpace, dp.numShownHotseatIcons
            )
            (cellWidth - (dp.iconSizePx * 0.92f)).toInt()
        }
        
        val qsbWidth = if (dp.isTablet) {
            width
        } else {
            width - horizontalInset
        }

        val height = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(qsbWidth , height)

        for (i in 0 until childCount) {
            getChildAt(i)?.let { child ->
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            }
        }
    }

    fun getTopInset(): Int = if (dp.isTablet) {
        resources.getDimensionPixelOffset(R.dimen.qsb_margin_top_adjusting_large)
    } else {
        resources.getDimensionPixelSize(R.dimen.qsb_margin_top_adjusting)
    }
}
