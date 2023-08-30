package com.masterproef.model

import kotlin.math.pow

object DistanceCalculator {
    fun calculateDistance(txPower: Int, rssi: Int): Double {
        return 10.0.pow(((-txPower - rssi) / (10 * 4).toDouble()))
    }
}