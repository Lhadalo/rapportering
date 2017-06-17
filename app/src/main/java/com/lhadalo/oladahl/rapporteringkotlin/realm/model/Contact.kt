package com.lhadalo.oladahl.rapporteringkotlin.realm.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Contact(
        @PrimaryKey
        var id: String = "",

        var displayName: String = "",

        var phonenumbers: RealmList<Phone> = RealmList<Phone>(),

        var emailAdresses: RealmList<Email> = RealmList<Email>()

) : RealmObject() {
        override fun toString(): String {
                return displayName
        }
}
