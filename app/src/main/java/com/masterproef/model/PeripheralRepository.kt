package com.masterproef.model

import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.masterproef.model.DistanceCalculator.calculateDistance
import com.welie.blessed.BluetoothPeripheral

// Main class of the model
class PeripheralRepository: BleCallback {

    // List of all recently discovered devices, advertising the identification-service
    // Key is the MAC-address of said device, which is present in the advertisement packets and when a value is received
    val peripherals = mutableStateMapOf<String, Peripheral>()

    private var bleManager: BleManager? = null
    private val ultrasonicGenerator = UltrasonicGenerator()

    /*  Called when an advertisement packet is discovered
    *   If the device is already in the list, update the RSSI and txPower values (these can only be gotten from advertisements)
    *   If the device wasn't already in the list, add it
    *   Whole objects are removed and added back so the UI properly updates, even when a value inside an object changes
    * */
    override fun onPeripheralDiscovered(bluetoothPeripheral: BluetoothPeripheral, scanResult: ScanResult) {
        var peripheral = peripherals[bluetoothPeripheral.address]

        if (peripheral != null) {
            peripheral.rssi = scanResult.rssi
            peripheral.lastAdvertisementTimestamp = scanResult.timestampNanos

            peripheral.txPower = scanResult.txPower
            peripheral.distance = calculateDistance(scanResult.txPower, scanResult.rssi)

            peripherals.remove(bluetoothPeripheral.address)
        } else {
            peripheral = Peripheral(bluetoothPeripheral, scanResult)
        }

        peripherals[bluetoothPeripheral.address] = peripheral
    }

    // Called when a device disconnects
    override fun onPeripheralDisconnected(bluetoothPeripheral: BluetoothPeripheral) {
        peripherals.remove(bluetoothPeripheral.address)
        checkAndDisableUltrasonic()
    }

    // Called when an object connects
    // Ultrasonic pattern starts playing
    override fun onPeripheralConnected(bluetoothPeripheral: BluetoothPeripheral) {
        ultrasonicGenerator.startPlaying()
    }

    // Called when a deviceId value is received
    override fun onDeviceIdReceived(bluetoothPeripheral: BluetoothPeripheral, deviceId: Int) {
        val peripheral = peripherals[bluetoothPeripheral.address]

        if(peripheral != null) {
            peripherals.remove(bluetoothPeripheral.address)

            /* MAC-addresses can change. deviceId is seen as a constant id, unique to every device
            * If a deviceId is received that is already the id of another device, it is assumed that
            * both devices are actually the same device and that that device changed its MAC-address
            * The already present object with that deviceId is removed from the list.
            * */
            peripherals.forEach { (key, peripheral) ->
                if (peripheral.deviceId == deviceId) { peripherals.remove(key) }
            }

            peripheral.deviceId = deviceId
            peripherals[bluetoothPeripheral.address] = peripheral
        }
    }

    // Called when a peak-to-peak heart rate variability value is received
    // Value gets added to the list of that device
    override fun onPeakToPeakReceived(bluetoothPeripheral: BluetoothPeripheral, timestamp: Long, pp: Float) {
        val peripheral = peripherals[bluetoothPeripheral.address]

        if(peripheral != null) {
            if((peripheral.lastPeakToPeakTimestamp != null) && (timestamp - peripheral.lastPeakToPeakTimestamp!! < 1000)) {
                peripheral.peakToPeakValueArray = mutableListOf(pp)
            }else{
                peripheral.peakToPeakValueArray.add(pp)
            }

            peripheral.lastPeakToPeakTimestamp = timestamp
            peripheral.arrayLength = peripheral.peakToPeakValueArray.size

            peripherals.remove(bluetoothPeripheral.address)
            peripherals[bluetoothPeripheral.address] = peripheral
        }
    }

    // Called when an ultrasonic value is received, indicating that either the pattern was detected or the timer ran out on the wearable
    override fun onUltrasonicDetectedReceived(bluetoothPeripheral: BluetoothPeripheral, ultrasonicDetected: Boolean) {
        val peripheral = peripherals[bluetoothPeripheral.address]

        if(peripheral != null) {
            peripheral.ultrasonicDetected = ultrasonicDetected

            peripherals.remove(bluetoothPeripheral.address)
            peripherals[bluetoothPeripheral.address] = peripheral
        }

        checkAndDisableUltrasonic()
    }

    /*  Checks if there are any devices that have not had onUltrasonicDetectedReceived called on them,
    * indicating that these are still listening for a pattern
    * If there are none, prematurely stops the ultrasonic pattern
    * */
    private fun checkAndDisableUltrasonic() {
        var nullPresent = false

        peripherals.forEach { (_, peripheral) ->
            if (peripheral.ultrasonicDetected == null) { nullPresent = true }
        }

        if (!nullPresent) {
            ultrasonicGenerator.stopPlaying()
        }
    }

    fun enableBle(context: Context) {
        if (bleManager == null) { bleManager = BleManager(context, this) }
        bleManager!!.startScanning()
    }

    fun disableBle() {
        bleManager?.disableBle()
        ultrasonicGenerator.stopPlaying()
    }
}