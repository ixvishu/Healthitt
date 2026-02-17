package com.example.healthitt.health

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BleWatchManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices

    // Standard UUIDs for heart rate and battery
    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            try {
                // Safely access device name
                val name = device.name
                if (name != null && !foundDevices.value.any { it.address == device.address }) {
                    _foundDevices.value = _foundDevices.value + device
                    Log.d("BleWatchManager", "Found device: $name at ${device.address}")
                }
            } catch (e: SecurityException) {
                Log.e("BleWatchManager", "SecurityException during scan: ${e.message}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleWatchManager", "Scan failed with error: $errorCode")
        }
    }

    fun startDiscovery() {
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e("BleWatchManager", "Bluetooth is disabled")
            return
        }
        _foundDevices.value = emptyList()
        Log.d("BleWatchManager", "Starting BLE Scan...")
        bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopDiscovery() {
        Log.d("BleWatchManager", "Stopping BLE Scan")
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            Log.d("BleWatchManager", "onConnectionStateChange: status=$status, newState=$newState for $deviceAddress")
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BleWatchManager", "GATT Error: status $status. Disconnecting...")
                _connectionState.value = "Error ($status)"
                gatt.close()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = "Connected"
                Log.i("BleWatchManager", "Successfully connected to $deviceAddress. Discovering services...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = "Disconnected"
                Log.i("BleWatchManager", "Disconnected from $deviceAddress")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleWatchManager", "Services discovered. Setting up notifications...")
                setupNotifications(gatt)
            } else {
                Log.w("BleWatchManager", "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                HEART_RATE_MEASUREMENT_CHAR_UUID -> {
                    val flag = characteristic.properties
                    val format = if (flag and 0x01 != 0) BluetoothGattCharacteristic.FORMAT_UINT16 else BluetoothGattCharacteristic.FORMAT_UINT8
                    val hr = characteristic.getIntValue(format, 1) ?: 0
                    _heartRate.value = hr
                    Log.d("BleWatchManager", "Heart Rate Update: $hr BPM")
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                val level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: 0
                _batteryLevel.value = level
                Log.d("BleWatchManager", "Battery Level: $level%")
            }
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        if (bluetoothAdapter?.isEnabled != true) return
        
        stopDiscovery()
        _connectionState.value = "Connecting..."
        Log.d("BleWatchManager", "Attempting connection to ${device.address}")
        
        // Use TRANSPORT_LE for modern watches to ensure BLE connection
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    private fun setupNotifications(gatt: BluetoothGatt) {
        // Try to enable Heart Rate notifications
        val hrService = gatt.getService(HEART_RATE_SERVICE_UUID)
        val hrChar = hrService?.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)
        
        if (hrChar != null) {
            gatt.setCharacteristicNotification(hrChar, true)
            val descriptor = hrChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Log.i("BleWatchManager", "Heart Rate Notifications Enabled")
            }
        } else {
            Log.w("BleWatchManager", "Heart Rate Service not found on this device")
        }

        // Read Battery Level
        val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
        val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
        if (batteryChar != null) {
            gatt.readCharacteristic(batteryChar)
        }
    }

    fun disconnect() {
        Log.d("BleWatchManager", "Manual Disconnect requested")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = "Disconnected"
    }
}
