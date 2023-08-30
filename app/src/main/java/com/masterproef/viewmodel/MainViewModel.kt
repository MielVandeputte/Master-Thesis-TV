package com.masterproef.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.masterproef.model.PeripheralRepository
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {

    private val peripheralRepository = PeripheralRepository()
    val peripherals = peripheralRepository.peripherals

    val isListeningState = MutableStateFlow(false)

    fun enableBle(context: Context) {
        isListeningState.value = true
        peripheralRepository.enableBle(context)
    }

    fun disableBle() {
        isListeningState.value = false
        peripheralRepository.disableBle()
    }
}