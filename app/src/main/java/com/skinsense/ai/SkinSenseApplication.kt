package com.skinsense.ai

import android.app.Application
import android.content.Context
import com.skinsense.ai.utils.LocaleHelper

class SkinSenseApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
}
