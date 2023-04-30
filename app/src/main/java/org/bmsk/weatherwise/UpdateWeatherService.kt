package org.bmsk.weatherwise

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import org.bmsk.weatherwise.data.repository.WeatherRepository
import org.bmsk.weatherwise.ui.SettingActivity

class UpdateWeatherService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // notification channel
        createChannel()
        startForeground(FOREGROUND_ID, createNotification())

        val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val pendingIntent: PendingIntent = Intent(this, SettingActivity::class.java).let {
                PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE)
            }

            RemoteViews(packageName, R.layout.widget_weather).apply {
                setTextViewText(R.id.temperatureTextView, getString(R.string.no_permission))
                setTextViewText(R.id.weatherTextView, "")
                setOnClickPendingIntent(R.id.temperatureTextView, pendingIntent)
            }.also { remoteViews ->
                val appWidgetName = ComponentName(this, WeatherAppWidgetProvider::class.java)
                appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
            }

            stopSelf()

            return super.onStartCommand(intent, flags, startId)
        }
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {

            WeatherRepository.getVillageForecast(
                longitude = it.longitude,
                latitude = it.latitude,
                successCallback = { forecastList ->

                    val pendingServiceIntent: PendingIntent =
                        Intent(this, UpdateWeatherService::class.java)
                            .let { intent ->
                                PendingIntent.getService(
                                    this,
                                    1,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            }

                    val currentForecast = forecastList.first()

                    RemoteViews(packageName, R.layout.widget_weather).apply {
                        setTextViewText(
                            R.id.temperatureTextView,
                            getString(R.string.temperature_text, currentForecast.temperature)
                        )
                        setTextViewText(
                            R.id.weatherTextView,
                            currentForecast.weather
                        )
                        setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                    }.also { remoteViews ->
                        val appWidgetName =
                            ComponentName(this, WeatherAppWidgetProvider::class.java)
                        appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                    }

                    stopSelf()
                },
                failureCallback = {
                    val pendingServiceIntent: PendingIntent =
                        Intent(this, UpdateWeatherService::class.java)
                            .let { intent ->
                                PendingIntent.getService(
                                    this,
                                    1,
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE
                                )
                            }

                    RemoteViews(packageName, R.layout.widget_weather).apply {
                        setTextViewText(
                            R.id.temperatureTextView,
                            getString(R.string.error)
                        )
                        setTextViewText(
                            R.id.weatherTextView,
                            ""
                        )
                        setOnClickPendingIntent(R.id.temperatureTextView, pendingServiceIntent)
                    }.also { remoteViews ->
                        val appWidgetName =
                            ComponentName(this, WeatherAppWidgetProvider::class.java)
                        appWidgetManager.updateAppWidget(appWidgetName, remoteViews)
                    }

                    stopSelf()
                }
            )
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            getString(R.string.widget_refresh_channel),
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = getString(R.string.guid_widget_channel)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, getString(R.string.widget_refresh_channel))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.update_weather))
            .build()
    }

    companion object {
        const val FOREGROUND_ID = 1
    }
}