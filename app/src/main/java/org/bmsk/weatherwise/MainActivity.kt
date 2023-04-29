package org.bmsk.weatherwise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val service = weatherService

        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
        val point = converter.convert(lat = 37.5519, lon = 126.9918)
        Log.d("MainActivity", "${point.nx} ${point.ny}")
        service.getVillageForecast(
            serviceKey = SERVICE_KEY,
            baseDate = baseDateTime.baseDate,
            baseTime = baseDateTime.baseTime,
            nx = point.nx,
            ny = point.ny
        ).enqueue(object : Callback<WeatherEntity> {
            override fun onResponse(call: Call<WeatherEntity>, response: Response<WeatherEntity>) {
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
            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                t.printStackTrace()
            }
        })
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
}