package org.bmsk.weatherwise

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import org.bmsk.weatherwise.Network.weatherService
import org.bmsk.weatherwise.data.GeoPointConverter
import org.bmsk.weatherwise.data.model.BaseDateTime
import org.bmsk.weatherwise.data.model.Category
import org.bmsk.weatherwise.data.model.Forecast
import org.bmsk.weatherwise.data.model.ForecastEntity
import org.bmsk.weatherwise.data.model.PTY_EMPTY
import org.bmsk.weatherwise.data.model.PTY_RAIN
import org.bmsk.weatherwise.data.model.PTY_RAIN_SNOW
import org.bmsk.weatherwise.data.model.PTY_SHOWER
import org.bmsk.weatherwise.data.model.PTY_SNOW
import org.bmsk.weatherwise.data.model.SKY_CLOUDY
import org.bmsk.weatherwise.data.model.SKY_OVERCAST
import org.bmsk.weatherwise.data.model.SKY_SUNNY
import org.bmsk.weatherwise.data.model.WeatherEntity
import org.bmsk.weatherwise.databinding.ActivityMainBinding
import org.bmsk.weatherwise.databinding.ItemForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val locationPermissionRequest = getLocationPermissionRequest()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationPermissionRequest.launch(arrayOf(ACCESS_COARSE_LOCATION))
    }

    private fun transformRainType(forecast: ForecastEntity) =
        when (forecast.forecastValue.toInt()) {
            PTY_EMPTY -> getString(R.string.pty_empty)
            PTY_RAIN -> getString(R.string.pty_rain)
            PTY_RAIN_SNOW -> getString(R.string.pty_rain_snow)
            PTY_SNOW -> getString(R.string.pty_snow)
            PTY_SHOWER -> getString(R.string.pty_shower)
            else -> ""
        }

    private fun transformSky(forecast: ForecastEntity) =
        when (forecast.forecastValue.toInt()) {
            SKY_SUNNY -> getString(R.string.sky_sunny)
            SKY_OVERCAST -> getString(R.string.sky_overcast)
            SKY_CLOUDY -> getString(R.string.sky_cloudy)
            else -> ""
        }

    private fun getLocationPermissionRequest() = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(ACCESS_COARSE_LOCATION, false) -> {
                updateLocation()
            }

            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.need_location_permission),
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("pakage", packageName, null)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updateLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(ACCESS_COARSE_LOCATION))
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener {

            Thread {
                try {
                    val addressList = Geocoder(this, Locale.KOREA).getFromLocation(
                        it.latitude,
                        it.longitude,
                        1
                    )
                    runOnUiThread {
                        binding.locationTextView.text = addressList?.get(0)?.thoroughfare.orEmpty()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }.start()


            val baseDateTime = BaseDateTime.getBaseDateTime()
            val converter = GeoPointConverter()
            val point = converter.convert(lat = it.latitude, lon = it.longitude)
            Log.d("MainActivity", "${it.latitude} ${it.longitude}")
            Log.d("MainActivity", "${point.nx} ${point.ny}")
            weatherService.getVillageForecast(
                serviceKey = SERVICE_KEY,
                baseDate = baseDateTime.baseDate,
                baseTime = baseDateTime.baseTime,
                nx = point.nx,
                ny = point.ny
            ).enqueue(object : Callback<WeatherEntity> {
                override fun onResponse(
                    call: Call<WeatherEntity>,
                    response: Response<WeatherEntity>
                ) {

                    val forecastDateTimeMap = mutableMapOf<String, Forecast>()

                    val forecastList =
                        response.body()?.response?.body?.items?.forecastEntities.orEmpty()

                    forecastList.forEach { forecast ->
                        val fKey = "${forecast.forecastDate}/${forecast.forecastTime}"

                        forecastDateTimeMap[fKey] = forecastDateTimeMap.getOrDefault(
                            fKey, Forecast(
                                forecastDate = forecast.forecastDate,
                                forecastTime = forecast.forecastTime
                            )
                        ).apply {
                            when (forecast.category) {
                                Category.POP -> precipitation = forecast.forecastValue.toInt()
                                Category.PTY -> precipitationType = transformRainType(forecast)
                                Category.SKY -> sky = transformSky(forecast)
                                Category.TMP -> temperature = forecast.forecastValue.toDouble()
                                else -> {}
                            }
                        }
                    }

                    val sortedList = forecastDateTimeMap.values.toMutableList().apply {
                        sortWith(compareBy { f ->
                            "${f.forecastDate}/${f.forecastTime}"
                        })
                    }

                    val currentForecast = sortedList.first()

                    binding.temperatureTextView.text =
                        getString(R.string.temperature_text, currentForecast.temperature)
                    binding.skyTextView.text = currentForecast.weather
                    binding.precipitationTextView.text =
                        getString(R.string.precipitation_text, currentForecast.precipitation)
                    binding.childForecastLayout.apply {
                        sortedList.forEachIndexed { index, f ->
                            if (index == 0) return@forEachIndexed

                            val itemView = ItemForecastBinding.inflate(layoutInflater)
                            itemView.timeTextView.text = f.convertedTime
                            itemView.weatherTextView.text = f.weather
                            itemView.temperatureTextView.text =
                                getString(R.string.temperature_text, f.temperature)

                            addView(itemView.root)
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        }
    }
}