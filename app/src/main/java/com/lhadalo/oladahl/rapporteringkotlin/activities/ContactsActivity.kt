package com.lhadalo.oladahl.rapporteringkotlin.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.lhadalo.oladahl.rapporteringkotlin.R
import com.lhadalo.oladahl.rapporteringkotlin.realm.RealmController
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Contact
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Email
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Phone
import com.lhadalo.oladahl.rapporteringkotlin.utilities.PreferenceUtil
import com.lhadalo.oladahl.rapporteringkotlin.utilities.forEach
import io.realm.RealmList
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_contacts.*
import kotlinx.android.synthetic.main.dialog_layout.*
import kotlinx.android.synthetic.main.dialog_layout.view.*


class ContactsActivity : AppCompatActivity(), RealmController.RealmTransactionChange {
    val realmController: RealmController by lazy { RealmController.create(this) }
    val prefs: PreferenceUtil by lazy { PreferenceUtil(this) }
    private val adapter: ArrayAdapter<Contact> by lazy { ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts) }
    private val contacts: ArrayList<Contact> = ArrayList()
    val CONTACT_PICKER_RESULT = 100
    val READ_CONTACTS_REQUEST = 101

    companion object {
        val TAG = ContactsActivity::class.java.simpleName
        fun newIntent(context: Context): Intent = Intent(context, ContactsActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        this.title = getString(R.string.persons_title)
        realmController.getAllContacts().forEach { contact -> contacts.add(contact) }
        list.adapter = adapter
        list.setOnItemClickListener { parent, view, position, id ->
            showContactInfo(contacts[position], position)
        }
    }

    override fun onResume() {
        super.onResume()
        checkOrRequestPermission()
        et_reporter.setText(prefs.reporter)
    }

    override fun onPause() {
        super.onPause()
        prefs.reporter = et_reporter.text.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_contacts, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.action_add_contact -> {
                    if (checkOrRequestPermission()) {
                        startActivityForResult(
                                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                                CONTACT_PICKER_RESULT)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onContactAdded(contact: Contact) {
        contacts.add(contact)
        adapter.notifyDataSetChanged()
    }

    override fun onContactDeleted(contact: Contact) {

    }

    fun showContactInfo(contact: Contact, listIndex: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(contact.displayName)
        val parent = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null) as LinearLayout

        if (contact.phonenumbers.size > 0) {
            contact.phonenumbers.forEach { number ->
                val textView = TextView(this)
                textView.text = number.phoneNumber
                parent.phone_container.addView(textView)
            }
        } else {
            parent.dialog_title_phone.visibility = View.GONE
            parent.phone_container.visibility = View.GONE
        }

        if (contact.emailAdresses.size > 0) {
            contact.emailAdresses.forEach { adress ->
                val textView = TextView(this)
                textView.text = adress.emailAddress
                parent.email_container.addView(textView)
            }
        }

        builder.setView(parent)


        builder.setNeutralButton("Radera", {dialog, which ->
            contacts.remove(contact)
            adapter.notifyDataSetChanged()
            realmController.deleteContact(contact)
            dialog.dismiss()
        })

        builder.setPositiveButton("OK", {dialog, which ->

        })

        builder.create().show()

    }

    fun showContactsPicker(id: String, name: String, numbers: RealmList<Phone>, emails: RealmList<Email>) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(name)

        val parent = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null) as LinearLayout

        if (numbers.size > 0) {
            numbers.forEach { number ->
                val checkbox = CheckBox(this)
                checkbox.text = number.phoneNumber
                parent.phone_container.addView(checkbox)
            }
        } else {
            parent.dialog_title_phone.visibility = View.GONE
            parent.phone_container.visibility = View.GONE
        }

        if (emails.size > 0) {
            emails.forEach { email ->
                val checkbox = CheckBox(this)
                checkbox.text = email.emailAddress
                parent.email_container.addView(checkbox)
            }
        } else {
            parent.dialog_title_email.visibility = View.GONE
            parent.email_container.visibility = View.GONE
        }

        builder.setView(parent)

        builder.setPositiveButton("Lägg till", { d, w ->
            val phoneChoices = RealmList<Phone>()
            parent.phone_container.forEach { view, i ->
                if ((view as CheckBox).isChecked) {
                    phoneChoices.add(numbers[i])
                }
            }
            val emailChoices = RealmList<Email>()
            parent.email_container.forEach { view, i ->
                if ((view as CheckBox).isChecked) {
                    emailChoices.add(emails[i])
                }
            }

            realmController.addContact(id, name, phoneChoices, emailChoices)
        })

        builder.setNegativeButton("Avbryt", { dialog, _ -> dialog.dismiss() })

        builder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CONTACT_PICKER_RESULT -> {
                    if (result != null) {
                        val id = result.data.lastPathSegment
                        val name = getName(result.data)
                        val numbers = getNumbers(id)
                        val emails = getEmails(id)

                        //Ifall det finns fler element än 1, välj vilka vi vill ha
                        if (numbers.size > 1 || emails.size > 1) {
                            showContactsPicker(id, name, numbers, emails)
                        } else {
                            //Lägg till kontakt direkt
                            realmController.addContact(
                                    id,
                                    name,
                                    numbers,
                                    emails
                            )
                        }
                    }
                }
            }
        }
    }

    fun getName(contactURI: Uri): String {
        val contactCursor = contentResolver.query(contactURI, null, null, null, null)
        var name: String = ""
        if (contactCursor != null && contactCursor.moveToFirst()) {
            name = contactCursor.getString(contactCursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.DISPLAY_NAME))
        }
        contactCursor.close()

        return name
    }

    fun getNumbers(id: String): RealmList<Phone> {
        val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                arrayOf(id),
                null
        )

        val numbers: RealmList<Phone> = RealmList<Phone>()
        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {
                numbers.add(Phone(phoneCursor.getString(
                        phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA))))
            }
        }
        phoneCursor.close()

        return numbers
    }

    fun getEmails(id: String): RealmList<Email> {
        val emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                arrayOf(id),
                null
        )

        val emails: RealmList<Email> = RealmList<Email>()
        if (emailCursor != null) {
            while (emailCursor.moveToNext()) {
                emails.add(Email(emailCursor.getString(
                        emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)))
                )
            }
        }
        emailCursor.close()

        return emails
    }

    fun checkOrRequestPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS),
                    READ_CONTACTS_REQUEST)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {

        when (requestCode) {
            READ_CONTACTS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    checkOrRequestPermission()
                }
            }
        }
    }
}