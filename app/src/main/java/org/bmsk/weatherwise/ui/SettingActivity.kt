package org.bmsk.weatherwise.ui

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.bmsk.weatherwise.R
import org.bmsk.weatherwise.UpdateWeatherService
import org.bmsk.weatherwise.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val locationPermissionRequest = getLocationPermissionRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissionRequest.launch(
                arrayOf(
                    ACCESS_BACKGROUND_LOCATION,
                    POST_NOTIFICATIONS
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionRequest.launch(
                arrayOf(ACCESS_BACKGROUND_LOCATION)
            )
        }
    }

    private fun getLocationPermissionRequest() = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(ACCESS_BACKGROUND_LOCATION, false) -> {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, UpdateWeatherService::class.java)
                )
            }

            else -> {
                Toast.makeText(
                    this,
                    getString(R.string.need_location_permission),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}