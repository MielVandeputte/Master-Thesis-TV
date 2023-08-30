package com.masterproef.model

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.welie.blessed.*
import com.welie.blessed.ConnectionState.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Class hides all BLE logic away and informs other functions of events using the passed in callback object
class BleManager(context: Context, callback: BleCallback) {

    // Object containing callback functions related to scanning / discovering devices
    private val scanningCallback: BluetoothCentralManagerCallback = object : BluetoothCentralManagerCallback() {

            // Called when a device connects, information is passed to PeripheralRepository and
            // startScanning is called again to resume scanning
            override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
                Log.i("BluetoothCentralManagerCallback", "Connected to ${peripheral.name}")
                callback.onPeripheralConnected(peripheral)
                startScanning()
            }

            // Called when a connection attempt failed, information is passed to PeripheralRepository and
            // startScanning is called again to resume scanning
            override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
                Log.i("BluetoothCentralManagerCallback", "Connection ${peripheral.name} failed with status $status")
                callback.onPeripheralDisconnected(peripheral)
                startScanning()
            }

            // Called when a device disconnects, information is passed to PeripheralRepository
            override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: HciStatus) {
                Log.i("BluetoothCentralManagerCallback", "Disconnected ${peripheral.name} with status $status")
                callback.onPeripheralDisconnected(peripheral)
            }

            // Called when an advertisement packet is discovered. Connect to it if not already connected / connecting / disconnecting
            // Scanning is stopped during the connection phase because issues were observed
            override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
                Log.i("BluetoothCentralManagerCallback", "Found peripheral ${peripheral.name}")
                callback.onPeripheralDiscovered(peripheral, scanResult)

                if ((peripheral.state != CONNECTED) and (peripheral.state != CONNECTING) and (peripheral.state != DISCONNECTING)) {
                    bluetoothCentralManager.stopScan()
                    bluetoothCentralManager.connectPeripheral(peripheral, connectionCallback)
                }
            }
    }

    // Object containing callback functions related to connected devices
    private val connectionCallback: BluetoothPeripheralCallback = object : BluetoothPeripheralCallback() {

        /* Called after connection is established
        *  Characteristic deviceId is read if available
        *  Characteristics ultrasonicDetected and ppIntervalCharacteristic are subscribed to if available
        * */
        override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
            val deviceIdCharacteristic = peripheral.getCharacteristic(
                Identifiers.IDENTIFICATION_SERVICE_UUID,
                Identifiers.DEVICE_ID_CHARACTERISTIC_UUID
            )

            if (deviceIdCharacteristic != null) {
                peripheral.readCharacteristic(deviceIdCharacteristic)
            }

            val ultrasonicDetectedCharacteristic = peripheral.getCharacteristic(
                Identifiers.IDENTIFICATION_SERVICE_UUID,
                Identifiers.ULTRASONIC_DETECTED_CHARACTERISTIC_UUID
            )

            if (ultrasonicDetectedCharacteristic != null) {
                peripheral.setNotify(ultrasonicDetectedCharacteristic, true)
            }

            val ppIntervalCharacteristic = peripheral.getCharacteristic(
                Identifiers.HRV_SERVICE_UUID,
                Identifiers.PP_INTERVAL_CHARACTERISTIC_UUID
            )

            if (ppIntervalCharacteristic != null) {
                peripheral.setNotify(ppIntervalCharacteristic, true)
            }
        }

        // Called when a characteristic-value is received, either from a read operation or a value that's subscribed to
        // Received value is passed to the PeripheralRepository
        override fun onCharacteristicUpdate(peripheral: BluetoothPeripheral, value: ByteArray, characteristic: BluetoothGattCharacteristic, status: GattStatus) {
            when (characteristic.uuid) {
                Identifiers.DEVICE_ID_CHARACTERISTIC_UUID -> {
                    callback.onDeviceIdReceived(peripheral, ByteBuffer.wrap(value).int)
                }

                Identifiers.ULTRASONIC_DETECTED_CHARACTERISTIC_UUID -> {
                    val ultrasonicDetected = value[0] == 1.toByte()
                    callback.onUltrasonicDetectedReceived(peripheral, ultrasonicDetected)
                }

                Identifiers.PP_INTERVAL_CHARACTERISTIC_UUID -> {
                    val buffer = ByteBuffer.wrap(value)
                    buffer.order(ByteOrder.LITTLE_ENDIAN)
                    callback.onPeakToPeakReceived(peripheral, buffer.long, buffer.float)
                }
            }
        }
    }

    private var bluetoothCentralManager: BluetoothCentralManager = BluetoothCentralManager(context, scanningCallback, Handler(Looper.getMainLooper()))

    // Start scanning for the identification-service UUID
    fun startScanning() {
        if (!bluetoothCentralManager.isScanning) {
            bluetoothCentralManager.scanForPeripheralsWithServices(arrayOf(Identifiers.IDENTIFICATION_SERVICE_UUID))
        }
    }

    // Called when closing the app
    // Unsubscribe from all values and terminate all connections
    fun disableBle() {
        bluetoothCentralManager.stopScan()

        for (peripheral in bluetoothCentralManager.connectedPeripherals) {
            val ppIntervalCharacteristic = peripheral.getCharacteristic(
                Identifiers.HRV_SERVICE_UUID,
                Identifiers.PP_INTERVAL_CHARACTERISTIC_UUID
            )

            if (ppIntervalCharacteristic != null) { peripheral.setNotify(ppIntervalCharacteristic, false) }

            val ultrasonicDetectedCharacteristic = peripheral.getCharacteristic(
                Identifiers.IDENTIFICATION_SERVICE_UUID,
                Identifiers.ULTRASONIC_DETECTED_CHARACTERISTIC_UUID
            )
            if (ultrasonicDetectedCharacteristic != null) { peripheral.setNotify(ultrasonicDetectedCharacteristic, false) }

            bluetoothCentralManager.cancelConnection(peripheral)
        }
    }
}