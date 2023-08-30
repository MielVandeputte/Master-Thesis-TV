package com.masterproef.presentation

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.masterproef.model.Peripheral
import com.masterproef.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }

        setContent { PhoneApp(viewModel, this.applicationContext) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.disableBle()
    }
}

@Composable
fun PhoneApp(viewModel: MainViewModel, context: Context) {
    val isListening by viewModel.isListeningState.collectAsState()

    // Show button if isListening is false, show list if isListening is true
    if (!(isListening)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { viewModel.enableBle(context) }) {
                Text(text = "Start scanning")
            }
        }
    } else {
        PeripheralList(viewModel = viewModel)
    }
}

@Composable
fun PeripheralList(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())) {
        for (peripheral in viewModel.peripherals.values) {
            PeripheralElement(peripheral)
        }
    }
}

@Composable
fun PeripheralElement(peripheral: Peripheral) {
    Card(elevation = 5.dp, modifier = Modifier.width(500.dp).fillMaxHeight().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = peripheral.deviceName, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.padding(bottom = 16.dp))

            TitleRow("Basic Info")
            InfoRow("Device address", peripheral.deviceAddress)
            InfoRow("Device id", peripheral.deviceId.toString())

            TitleRow("Raw Presence Data")
            InfoRow("Last Advertisement timestamp", peripheral.lastAdvertisementTimestamp.toString())
            InfoRow("RSSI", peripheral.rssi.toString())
            InfoRow("Tx power", peripheral.txPower.toString())
            InfoRow("Distance", peripheral.distance.toString() + " meters")
            InfoRow("Ultrasonic detected", peripheral.ultrasonicDetected.toString())

            TitleRow("Raw HRV Data")
            InfoRow("Last timestamp", peripheral.lastPeakToPeakTimestamp.toString())
            InfoRow("Array Size", peripheral.arrayLength.toString())
            InfoRow("Last PP value", if (peripheral.peakToPeakValueArray.size > 0) { peripheral.peakToPeakValueArray.last().toString() } else { "Empty" })
        }
    }
}

@Composable
fun TitleRow(title: String) {
    Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black), modifier = Modifier.padding(bottom = 8.dp, top = 16.dp))
}

@Composable
fun InfoRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Gray), modifier = Modifier.width(150.dp))
        Text(text = value, style = TextStyle(fontWeight = FontWeight.Normal, color = Color.Black))
    }
}
