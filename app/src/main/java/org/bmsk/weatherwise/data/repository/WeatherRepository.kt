package org.bmsk.weatherwise.data.repository

import android.util.Log
import org.bmsk.weatherwise.BASE_URL
import org.bmsk.weatherwise.R
import org.bmsk.weatherwise.SERVICE_KEY
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
import org.bmsk.weatherwise.data.service.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherService: WeatherService = retrofit.create(WeatherService::class.java)

    fun getVillageForecast(
        longitude: Double,
        latitude: Double,
        successCallback: (List<Forecast>) -> Unit,
        failureCallback: (Throwable) -> Unit,
    ) {
        val baseDateTime = BaseDateTime.getBaseDateTime()
        val converter = GeoPointConverter()
        val point = converter.convert(lat = latitude, lon = longitude)

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

                if (sortedList.isEmpty()) {
                    failureCallback(NullPointerException())
                } else {
                    successCallback(sortedList)
                }
            }

            override fun onFailure(call: Call<WeatherEntity>, t: Throwable) {
                failureCallback(t)
            }
        })
    }

    private fun transformRainType(forecast: ForecastEntity) =
        when (forecast.forecastValue.toInt()) {
            PTY_EMPTY -> "없음"
            PTY_RAIN -> "비"
            PTY_RAIN_SNOW -> "비/눈"
            PTY_SNOW -> "눈"
            PTY_SHOWER -> "소나기"
            else -> ""
        }

    private fun transformSky(forecast: ForecastEntity) =
        when (forecast.forecastValue.toInt()) {
            SKY_SUNNY -> "맑음"
            SKY_OVERCAST -> "구름많음"
            SKY_CLOUDY -> "흐림"
            else -> ""
        }
}