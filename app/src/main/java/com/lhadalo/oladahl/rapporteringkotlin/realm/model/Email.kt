package com.lhadalo.oladahl.rapporteringkotlin.realm.model

import io.realm.RealmObject

open class Email constructor(
        var emailAddress: String = ""
) : RealmObject()
