package com.lhadalo.oladahl.rapporteringkotlin.activities

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.TimePicker
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
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(),
        WeatherFetcher.WeatherFetchFinished,
        RealmController.RealmTransactionChange {
    private val TAG = "MainActivity"
    val prefs: PreferenceUtil by lazy { PreferenceUtil(this) }
    val locHelper: LocationHelper by lazy { LocationHelper(this) }
    val dateFormat: DateFormat by lazy { SimpleDateFormat.getDateInstance() }
    val realmController: RealmController = RealmController.create(this)

    private val SEND_SMS = 1
    private val SEND_EMAIL = 2
    private val SMS_REQUEST = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.title = getString(R.string.app_name)
        this.setSupportActionBar(toolbar)
        et_date.setText(dateFormat.format(Calendar.getInstance().time))

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
        /*
        btn_send_sms.setOnClickListener {
            val report = getReport()
            if (report.isNotBlank()) {
                createSendDialog(SEND_SMS, report)
            }
        }

        btn_send_email.setOnClickListener {
            val report = getReport()
            createSendDialog(SEND_EMAIL, "Testing")
        }
        */
        /*
        btn_send_sms.setOnClickListener {
            val report = getReport()
            if (report.isNotBlank()) {
                createSendDialog(SEND_SMS, report)
            }
        }
        */

        btn_send.setOnClickListener {
            val report = getReport()
            if (report.isNotBlank()) {
                createSendDialog(SEND_SMS, report)
            }
        }

        et_date.setOnClickListener {
            val picker = DatePickerFragment({ year, month, day ->
                val date = Calendar.getInstance()
                date.set(year, month, day)
                et_date.setText(dateFormat.format(date.time))
            })

            picker.show(supportFragmentManager, "")
        }

        et_planned_starttime.setOnClickListener {
            TimePickerFragment({ hour, minute ->
                et_planned_starttime.setText(formatTime(hour, minute))
            }).show(supportFragmentManager, "")
        }

        et_actual_starttime.setOnClickListener {
            TimePickerFragment({ hour, minute ->
                et_actual_starttime.setText(formatTime(hour, minute))
            }).show(supportFragmentManager, "")
        }

        et_stoptime.setOnClickListener {
            TimePickerFragment({ hour, minute ->
                et_stoptime.setText(formatTime(hour, minute))
            }).show(supportFragmentManager, "")
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Calendar.getInstance()
        date.set(Calendar.HOUR_OF_DAY, hour)
        date.set(Calendar.MINUTE, minute)

        return timeFormat.format(date.time)
    }

    fun createSendDialog(source: Int, report: String) {
        val contacts = realmController.getAllContacts()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Skicka rapport?")
        builder.setMessage("Är du säker?")

        builder.setPositiveButton("Skicka", { dialog, which ->
            when (source) {
                SEND_SMS -> {
                    val smsManager = SmsManager.getDefault()
                    val smsBodyParts = smsManager.divideMessage(report)
                    if (requestSmsPermission()) {
                        contacts.forEach { contact ->
                            contact.phonenumbers.forEach { number ->
                                smsManager.sendMultipartTextMessage(number.phoneNumber, null, smsBodyParts, null, null)
                            }
                        }
                        Toast.makeText(this, getString(R.string.feedback_sms_sent), Toast.LENGTH_SHORT).show()
                    }
                }

                SEND_EMAIL -> {

                    //TODO Få email-address att komma med
                    //Hämta alla adresser och spara i array


                    val emailList = ArrayList<String>()
                    contacts.forEach { contact ->
                        contact.emailAdresses.forEach { email ->
                            emailList.plus(email.emailAddress)
                        }
                    }

                    val list = arrayOfNulls<String>(emailList.size)
                    emailList.toArray(list)

                    Log.d(TAG, list.toString())
                    /*
                    val emailIntent = Intent(Intent.ACTION_SENDTO)

                    emailIntent.setData(Uri.parse("mailto:"))
                  //  emailIntent.putExtra(Intent.EXTRA_EMAIL, list)
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Rapport")
                    emailIntent.putExtra(Intent.EXTRA_TEXT, report)

                    try {
                        startActivity(emailIntent)
                    } catch (ex: android.content.ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, "There are no email clients installed.",
                                Toast.LENGTH_SHORT).show()
                    }
                    */

                }
            }
        })

        builder.create().show()
    }

    fun getReport(): String {
        var resultOk = true
        var report = ""

        linear_layout.forEach { view ->
            if (view is LinearLayout) {
                view.forEach { view ->
                    if (view is MaterialEditText) {
                        if (view.text.isBlank()) {
                            resultOk = false
                            view.error = getString(R.string.error_et_empty)
                        } else {
                            report += "${view.hint}: ${view.text}\n"
                        }
                    }
                }
            } else if (view is MaterialEditText) {
                if (view.text.isBlank()) {
                    resultOk = false
                    view.error = getString(R.string.error_et_empty)
                } else {
                    report += "${view.hint}: ${view.text}\n"
                }
            }
        }

        if (resultOk) {
            if (prefs.reporter.isBlank()) {
                Toast.makeText(this, "Du måste fylla i rapportör", Toast.LENGTH_SHORT).show()
                startActivity(ContactsActivity.newIntent(this))
            } else {
                report += "${getString(R.string.et_reporter)}: ${prefs.reporter}"
                return report
            }
        }

        return ""
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
                    createSendDialog(SEND_SMS, getReport())
                } else {
                    Toast.makeText(this, "Du måste godkänna för att skicka SEND_SMS", Toast.LENGTH_LONG).show()
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

    override fun onContactDeleted(contact: Contact) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class TimePickerFragment(val listener: (Int, Int) -> Unit) : DialogFragment(), TimePickerDialog.OnTimeSetListener {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val cal = Calendar.getInstance()
            return TimePickerDialog(activity, this,
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE), true)
        }

        override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
            listener(hourOfDay, minute)
        }
    }

    class DatePickerFragment(val listener: (Int, Int, Int) -> Unit): DialogFragment(), DatePickerDialog.OnDateSetListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val cal = Calendar.getInstance()
            return DatePickerDialog(activity, this,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH))
        }

        override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
            listener(year, month, dayOfMonth)
        }
    }
}
