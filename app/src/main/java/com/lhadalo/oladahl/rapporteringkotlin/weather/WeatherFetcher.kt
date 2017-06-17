package com.lhadalo.oladahl.rapporteringkotlin.weather

import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.logging.Handler
import kotlin.concurrent.thread

class WeatherFetcher constructor(val context: Context) {
    val TAG = "WeatherFetcher"
    val callback = context as WeatherFetchFinished

    interface WeatherFetchFinished {
        fun onWeatherFetched(temperature: Int, description: String)
    }

    companion object Factory {
        fun newInstance(context: Context): WeatherFetcher = WeatherFetcher(context)
    }

    fun fetchWeather(location: Location) {
        val queue = Volley.newRequestQueue(context)

        val request = StringRequest(Request.Method.GET, getOpenWeatherMapUrl(location), { response ->
            parseWeatherDataFromJson(response)
            //Hämta data som behövs

        }) { error ->
            //Error
        }

        queue.add(request)
    }

    fun getOpenWeatherMapUrl(location: Location): String {
        val appId = "a47cee4cdf64cdfacb45b2433441eaf6"
        val FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/weather?"
        val LAT_PARAM = "lat"
        val LON_PARAM = "lon"
        val APP_ID = "appid"
        val UNIT_PARAM = "units"
        val LANG_PARAM = "lang"


        return Uri.parse(FORECAST_BASE_URL).buildUpon()
                .appendQueryParameter(UNIT_PARAM, "metric")
                .appendQueryParameter(LAT_PARAM, location.latitude.toString())
                .appendQueryParameter(LON_PARAM, location.longitude.toString())
                .appendQueryParameter(LANG_PARAM, "se")
                .appendQueryParameter(APP_ID, appId)
                .build()
                .toString()
    }

    fun parseWeatherDataFromJson(forecastJson: String): Unit {
        doAsync {
            val temperature = JSONObject(forecastJson)
                    .getJSONObject("main")
                    .getDouble("temp")
            val desc = JSONObject(forecastJson)
                    .getJSONArray("weather")
                    .getJSONObject(0).getString("description")

            uiThread {
                callback.onWeatherFetched(temperature.toInt(), desc)
            }
        }
    }
}
