package com.lhadalo.oladahl.rapporteringkotlin.app

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class Rapportering : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config =RealmConfiguration.Builder()
                .name("rapportering.realm")
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build()

        Realm.setDefaultConfiguration(config)
    }
}
