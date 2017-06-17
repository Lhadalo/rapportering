package com.lhadalo.oladahl.rapporteringkotlin.utilities

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity

/**
 * Created by oladahl on 2017-06-01.
 */

object PreferencesKey {
    val REPORTER_NAME = "reporter_name"
    val PREFERENCE_FILE = "preferences"
}

class PreferenceUtil(val context: Context) {
    val sharedPreferences: SharedPreferences by lazy { context.getSharedPreferences(PreferencesKey.PREFERENCE_FILE, Context.MODE_PRIVATE) }

    var reporter: String
        get() = sharedPreferences.getString(PreferencesKey.REPORTER_NAME, "")
        set(value) {
            sharedPreferences.edit().putString(PreferencesKey.REPORTER_NAME, value).apply()
        }
}