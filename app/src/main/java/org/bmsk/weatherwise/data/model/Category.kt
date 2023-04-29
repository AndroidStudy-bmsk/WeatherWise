package org.bmsk.weatherwise.data.model

import com.google.gson.annotations.SerializedName

enum class Category {
    @SerializedName("POP")
    POP,    // 강수 확률

    @SerializedName("PTY")
    PTY,    // 강수 상태

    @SerializedName("SKY")
    SKY,    // 하늘 상태

    @SerializedName("TMP")
    TMP,    // 1시간 기온
}

const val PTY_EMPTY = 0
const val PTY_RAIN = 1
const val PTY_RAIN_SNOW = 2
const val PTY_SNOW = 3
const val PTY_SHOWER = 4

const val SKY_SUNNY = 1
const val SKY_OVERCAST = 3
const val SKY_CLOUDY = 4