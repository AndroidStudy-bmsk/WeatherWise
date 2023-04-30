package org.bmsk.weatherwise.data.model

data class Forecast(
    val forecastDate: String,
    val forecastTime: String,

    var temperature: Double = 0.0,
    var sky: String = "",
    var precipitation: Int = 0,
    var precipitationType: String = "",
) {
    val weather: String
        get() = if (precipitationType == "" || precipitationType == "없음") {
            sky
        } else {
            precipitationType
        }

    val convertedTime: String
        get() {
            val hour = forecastTime.substring(0, 2).toInt()
            val minute = forecastTime.substring(2, 4)

            return if (hour >= 12) {
                "오후 ${(hour - 12)}시 ${minute}분"
            } else {
                "오전 ${hour}시 ${minute}분"
            }
        }
}
