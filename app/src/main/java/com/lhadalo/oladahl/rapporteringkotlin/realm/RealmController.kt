package com.lhadalo.oladahl.rapporteringkotlin.realm

import android.util.Log
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Contact
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Email
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Phone
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmList
import io.realm.RealmResults

class RealmController(val callback: RealmTransactionChange) {
    private val mRealm: Realm by lazy { Realm.getDefaultInstance() }

    interface RealmTransactionChange {
        fun onContactAdded(contact: Contact)
        fun onContactDeleted(contact: Contact)
    }

    companion object {
        const val TAG = "RealmController"
        fun create(callback: RealmTransactionChange): RealmController = RealmController(callback)
    }

    fun closeRealm() = mRealm.close()

    fun getAllContacts(): RealmResults<Contact> = mRealm.where(Contact::class.java).findAll()

    fun addContact(id: String, displayName: String, numbers: RealmList<Phone>, emails: RealmList<Email>) {
        mRealm.executeTransaction { realm ->
            val contact = Contact(id = id, displayName = displayName)
            numbers.forEach { number ->
                contact.phonenumbers.add(number)
            }
            emails.forEach { email ->
                contact.emailAdresses.add(email)
            }

            if (realm.where(Contact::class.java).equalTo("id", id).findFirst() == null) {
                realm.copyToRealm(contact)
                callback.onContactAdded(contact)
            } else {
                realm.copyToRealmOrUpdate(contact)
            }
        }
    }

    fun deleteContact(contact: Contact) {
        mRealm.executeTransaction {
            contact.deleteFromRealm()
        }
        Log.d(TAG, "Contact deleted")

    }
}
