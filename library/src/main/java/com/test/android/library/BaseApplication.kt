package com.test.android.library

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * Created on 2021/9/25.
 *
 * @author Jack Chen
 * @email zhenchen@tubi.tv
 */
open class BaseApplication : Application() {
    companion object {
        private const val TAG = "BaseApplication"
        private fun init(context: Context) {
            Log.i(TAG, "init:$context")
        }
    }

    override fun onCreate() {
        super.onCreate()
        init(this)
    }
}