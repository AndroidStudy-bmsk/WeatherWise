package org.bmsk.weatherwise.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import org.bmsk.weatherwise.R
import org.bmsk.weatherwise.data.repository.WeatherRepository
import org.bmsk.weatherwise.databinding.ActivityMainBinding
import org.bmsk.weatherwise.databinding.ItemForecastBinding
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
                    data = Uri.fromParts("package", packageName, null)
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

            WeatherRepository.getVillageForecast(
                longitude = it.longitude,
                latitude = it.latitude,
                successCallback = { list ->

                    val currentForecast = list.first()

                    binding.temperatureTextView.text =
                        getString(R.string.temperature_text, currentForecast.temperature)
                    binding.skyTextView.text = currentForecast.weather
                    binding.precipitationTextView.text =
                        getString(R.string.precipitation_text, currentForecast.precipitation)
                    binding.childForecastLayout.apply {
                        list.forEachIndexed { index, f ->
                            if (index == 0) return@forEachIndexed

                            val itemView = ItemForecastBinding.inflate(layoutInflater)
                            itemView.timeTextView.text = f.convertedTime
                            itemView.weatherTextView.text = f.weather
                            itemView.temperatureTextView.text =
                                getString(R.string.temperature_text, f.temperature)

                            addView(itemView.root)
                        }
                    }
                },
                failureCallback = {
                    it.printStackTrace()
                }
            )

        }
    }
}