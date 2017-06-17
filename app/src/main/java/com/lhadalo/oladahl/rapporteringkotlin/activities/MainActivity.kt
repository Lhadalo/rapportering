package com.lhadalo.oladahl.rapporteringkotlin.activities

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
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
    val TAG = "MainActivity"
    val prefs: PreferenceUtil by lazy { PreferenceUtil(this) }
    val locHelper: LocationHelper by lazy { LocationHelper(this) }
    val realmController: RealmController = RealmController.create(this)

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

    fun listeners(): Unit {

    }

    fun getText(): String {
        var resultOk = true
        var report = ""

        constraint_layout.forEach { view ->
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            results: IntArray) {
        when (requestCode) {
            LOCATION_REQUEST -> {
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                    locHelper.doConnect(true)
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
