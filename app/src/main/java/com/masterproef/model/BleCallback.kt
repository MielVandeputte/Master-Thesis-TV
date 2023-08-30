package com.masterproef.model

import android.bluetooth.le.ScanResult
import com.welie.blessed.BluetoothPeripheral

interface BleCallback {
    fun onPeripheralDiscovered(bluetoothPeripheral: BluetoothPeripheral, scanResult: ScanResult)
    fun onPeripheralConnected(bluetoothPeripheral: BluetoothPeripheral)
    fun onPeripheralDisconnected(bluetoothPeripheral: BluetoothPeripheral)
    fun onDeviceIdReceived(bluetoothPeripheral: BluetoothPeripheral, deviceId: Int)
    fun onUltrasonicDetectedReceived(bluetoothPeripheral: BluetoothPeripheral, ultrasonicDetected: Boolean)
    fun onPeakToPeakReceived(bluetoothPeripheral: BluetoothPeripheral, timestamp: Long, pp: Float)
}