package com.darktower.treadmillapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

interface BtHandler {
    fun onConnected()
    fun onDisconnected()
}

class MainActivity : ComponentActivity(), BtHandler {
    private var connectBtn : ImageButton? = null
    private var startBtn : ImageButton? = null
    private var stopBtn : ImageButton? = null

    private var btService : BluetoothService = BluetoothService()

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
                btService.startScanning()
        }

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result: ActivityResult ->
        if(result.resultCode == RESULT_OK){
            btService.startScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        btService.init(this, this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectBtn = findViewById(R.id.btnConnect)
        startBtn = findViewById(R.id.btnStart)
        stopBtn = findViewById(R.id.btnStop)
        val speedText = findViewById<TextView>(R.id.speedText)

        val speedSeekBar = findViewById<SeekBar>(R.id.speedSeekBar)
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                speedText.text = (progress/10.0).toString()
                btService.setSpeed(progress/10.0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = btManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    fun onScan(view : View) {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = btManager.adapter
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PERMISSION_GRANTED
            ) {
                btService.startScanning()
            } else {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    fun onStart(view : View) {
        btService.startTreadMill()
    }

    fun onStop(view : View) {
        btService.stopTreadMill()
    }

    override fun onConnected() {
        runOnUiThread {
            connectBtn?.isClickable = false
            connectBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.green))
            startBtn?.isClickable = true
            startBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.white))
            stopBtn?.isClickable = true
            stopBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.white))
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            connectBtn?.isClickable = true
            connectBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.white))
            startBtn?.isClickable = false
            stopBtn?.isClickable = false
            startBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.gray))
            stopBtn?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.gray))
        }
    }
}