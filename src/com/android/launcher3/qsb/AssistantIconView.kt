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

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView

import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.internal.util.android.Utils

class AssistantIconView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ImageView(context, attrs, defStyle) {

    private val isGoogleInstalled = Utils.isPackageInstalled(context, Utilities.GSA_PACKAGE)

    init {
        apply {
            scaleType = ScaleType.CENTER
            isFocusable = true
            setImageResource(R.drawable.ic_mic_color)
            visibility = if (isGoogleInstalled) VISIBLE else GONE
            setOnClickListener { launchAssistant() }
        }
    }

    private fun launchAssistant() {
        if (!isGoogleInstalled) return
        val intent = Intent("android.intent.action.VOICE_ASSIST").apply {
            putExtra("onesearch_request_type", "TapMicIcon")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            setPackage(Utilities.GSA_PACKAGE)
        }
        runCatching { context.startActivity(intent) }
    }
}

