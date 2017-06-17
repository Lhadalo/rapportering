package com.lhadalo.oladahl.rapporteringkotlin.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.lhadalo.oladahl.rapporteringkotlin.R
import com.lhadalo.oladahl.rapporteringkotlin.realm.RealmController
import com.lhadalo.oladahl.rapporteringkotlin.realm.model.Contact
import com.lhadalo.oladahl.rapporteringkotlin.utilities.PreferenceUtil
import com.lhadalo.oladahl.rapporteringkotlin.utilities.forEach
import com.lhadalo.oladahl.rapporteringkotlin.weather.LocationConstant.LOCATION_REQUEST
import com.lhadalo.oladahl.rapporteringkotlin.weather.LocationHelper
import com.lhadalo.oladahl.rapporteringkotlin.weather.WeatherFetcher
import com.rengwuxian.materialedittext.MaterialEditText
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), WeatherFetcher.WeatherFetchFinished, RealmController.RealmTransactionChange {
    private val TAG = "MainActivity"
    val prefs: PreferenceUtil by lazy { PreferenceUtil(this) }
    val locHelper: LocationHelper by lazy { LocationHelper(this) }
    val realmController: RealmController = RealmController.create(this)

    private val SMS = 1
    private val EMAIL = 2
    private val SMS_REQUEST = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.title = "Parkteatern"
        val format = SimpleDateFormat.getDateInstance()
        et_date.setText(format.format(Calendar.getInstance().time))

        listeners()
    }

    override fun onStart() {
        super.onStart()
        locHelper.doConnect(true)
    }

    override fun onStop() {
        super.onStop()
        locHelper.doConnect(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        realmController.closeRealm()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        startActivity(ContactsActivity.newIntent(this))
        return super.onOptionsItemSelected(item)
    }

    fun listeners() {
        btn_send_sms.setOnClickListener {
            val report = getReport()
            if (report.isNotBlank()) {

            }
        }
    }

    fun createSendDialog(source: Int, report: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Skicka rapport?")
        builder.setMessage("Är du säker?")

        builder.setPositiveButton("Skicka", { dialog, which ->
            when (source) {
                SMS -> {
                    val smsManager = SmsManager.getDefault()
                    val smsBodyParts = smsManager.divideMessage(report)
                    if (requestSmsPermission()) {
                        val contacts = realmController.getAllContacts()
                        contacts.forEach { contact ->
                            contact.phonenumbers.forEach { number ->
                                smsManager.sendMultipartTextMessage(number.phoneNumber, null, smsBodyParts, null, null)
                            }
                        }
                        Toast.makeText(this, "SMS skickades!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    fun getReport(): String {
        var resultOk = true
        var report = ""

        linear_layout.forEach { view ->
            if (view is MaterialEditText) {
                if (view.text.isBlank()) {
                    resultOk = false
                    view.error = getString(R.string.error_et_empty)
                } else report += "${view.hint}: ${view.text}\n"
            }
        }

        if (resultOk) return report
        else return ""
    }

    private fun requestSmsPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_REQUEST)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            results: IntArray) {
        when (requestCode) {
            LOCATION_REQUEST -> {
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                    locHelper.doConnect(true)
                }
            }

            SMS_REQUEST -> {
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO Skicka meddelande
                } else {
                    Toast.makeText(this, "Du måste godkänna för att skicka SMS", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onWeatherFetched(temperature: Int, description: String) {
        et_weather_description.setText(description.capitalize())
        et_weather_temperature.setText(MessageFormat.format("{0} {1}", temperature.toString(), getString(R.string.degree_celsius)))
    }

    override fun onContactAdded(contact: Contact) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
