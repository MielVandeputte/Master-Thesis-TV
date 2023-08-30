package com.masterproef.model

import android.bluetooth.le.ScanResult
import com.masterproef.model.DistanceCalculator.calculateDistance
import com.welie.blessed.BluetoothPeripheral

data class Peripheral(private val peripheral: BluetoothPeripheral, private val scanResult: ScanResult) {

    var deviceName: String = peripheral.name
    var deviceAddress: String = peripheral.address

    var deviceId: Int? = null

    // Advertisement data
    var lastAdvertisementTimestamp: Long = scanResult.timestampNanos
    var txPower: Int = scanResult.txPower
    var rssi: Int = scanResult.rssi
    var distance: Double = calculateDistance(scanResult.txPower, scanResult.rssi)

    // Ultrasonic detection
    var ultrasonicDetected: Boolean? = null

    // PP data
    var lastPeakToPeakTimestamp: Long? = null
    var peakToPeakValueArray = mutableListOf<Float>()
    var arrayLength: Int = 0
}