package com.darktower.treadmillapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class BluetoothService {
    private lateinit var handler : BtHandler
    private lateinit var btManager : BluetoothManager
    private lateinit var appContext : Context

    private lateinit var treadmillDevice : BluetoothDevice
    private val TreadmillDeviceName : String = "RZ_TreadMill"
    private lateinit var bluetoothGatt: BluetoothGatt
    private var treadMillBtCharacteristic: BluetoothGattCharacteristic? = null

    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED && result.device.name == TreadmillDeviceName) {
                treadmillDevice = result.device
                connectDevice()
            }
        }
    }

    private val bleGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    bluetoothGatt?.discoverServices()
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                handler.onDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            val service = gatt?.services?.first{ it.uuid.toString().startsWith("00001800")}
            treadMillBtCharacteristic = service?.characteristics?.first()
            if(treadMillBtCharacteristic != null){
                handler.onConnected()
            }
        }
    }

    fun init(context : Context, btHandler : BtHandler)  {
        appContext = context
        btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        handler = btHandler
    }

    private fun getBtAdapter() : BluetoothAdapter {
        val bluetoothAdapter = btManager.adapter
        return bluetoothAdapter
    }

    fun startScanning() {
        if(appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            getBtAdapter().bluetoothLeScanner.startScan(bleScanCallback)
        }
    }

    private fun connectDevice() {
        if(appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            getBtAdapter().bluetoothLeScanner.stopScan(bleScanCallback)
        }

        if(treadmillDevice != null) {
            if (ActivityCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothGatt = treadmillDevice.connectGatt(appContext, false, bleGattCallback)
            }
        }
    }

    fun startTreadMill() {
        if(treadMillBtCharacteristic != null && ActivityCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val data = ubyteArrayOf(0xFBU, 0x07U, 0xA2U, 0x01U, 0x01U, 0x05U, 0x00U, 0xB0U, 0xFCU).toByteArray()
                treadMillBtCharacteristic?.let { it.value = data }
                bluetoothGatt.writeCharacteristic(treadMillBtCharacteristic)
            }
    }

    fun stopTreadMill() {
        if(treadMillBtCharacteristic != null && ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val data = ubyteArrayOf(0xFBU, 0x07U, 0xA2U, 0x04U, 0x01U, 0x00U, 0x00U, 0xAEU, 0xFCU).toByteArray()
            treadMillBtCharacteristic?.let { it.value = data }
            bluetoothGatt.writeCharacteristic(treadMillBtCharacteristic)
        }
    }

    fun setSpeed(speed: Double) {
        if(treadMillBtCharacteristic != null && ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val speedValue = (speed * 10).toInt().toUByte();
            val value = (0xABU + speedValue).toUByte();
            val data = ubyteArrayOf(
                0xFBU, 0x07U, 0xA1U, 0x02U, 0x01U, speedValue, 0x00U, value, 0xFCU
            ).toByteArray()
            treadMillBtCharacteristic?.let { it.value = data }
            bluetoothGatt.writeCharacteristic(treadMillBtCharacteristic)
        }
    }
}