package com.example.bluetoothapp

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_list.view.deviceAddressLine
import kotlinx.android.synthetic.main.activity_list.view.deviceNameLine
import android.bluetooth.BluetoothSocket
import androidx.fragment.app.FragmentActivity
import java.io.IOException


class MainActivity : AppCompatActivity() {


    private val TAG = "MainActivity"

    private val REQUEST_ENABLE_BT = 0

    var arrDeviceList: ArrayList<parcelabelBluetoothDevices> = ArrayList()
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()


    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        arrDeviceList=ArrayList<parcelabelBluetoothDevices>()

        setListeners()
        setRegisters()
        //checkPairedDeviceStatus()

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if(device.bondState!=BluetoothDevice.BOND_BONDED){
                        var deviceName = device.name
                        if(device.name==null){
                            deviceName="N/A"
                        }
                        val deviceHardwareAddress = device.address // MAC address
                        val newDevice = parcelabelBluetoothDevices(deviceName, deviceHardwareAddress)
                        insertItem(newDevice)

                    }

                }
            }
        }
    }

    private val receiver2 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val mDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                //3 cases:
                //case1: bonded already
                if (mDevice!!.bondState == BluetoothDevice.BOND_BONDED) {
                    d(TAG, "BroadcastReceiver: BOND_BONDED.")
                    textView.text="BroadcastReceiver: BOND_BONDED."
                }
                //case2: creating a bone
                if (mDevice.bondState == BluetoothDevice.BOND_BONDING) {
                    d(TAG, "BroadcastReceiver: BOND_BONDING.")
                    textView.text="BroadcastReceiver: BOND_BONDING."
                }
                //case3: breaking a bond
                if (mDevice.bondState == BluetoothDevice.BOND_NONE) {
                    d(TAG, "BroadcastReceiver: BOND_NONE.")
                    textView.text="BroadcastReceiver: BOND_NONE."
                }
            }
        }
    }

    private fun insertItem(newDevice: parcelabelBluetoothDevices) {
        arrDeviceList.add(newDevice)
        arrDeviceList.distinct()
        list_Devices.layoutManager = LinearLayoutManager(this) as RecyclerView.LayoutManager?
        list_Devices.adapter = deviceAdapter(arrDeviceList, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterReceiver(receiver2)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceNameTxt = view.deviceNameLine
        val deviceAddressTxt = view.deviceAddressLine
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            var permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.BLUETOOTH_ADMIN")
            if (permissionCheck != 0) {
                this.requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ), 1001
                ) //Any number
            }
        }
    }

    @Throws(Exception::class)
    fun createBond(btDevice: BluetoothDevice): Boolean {
        val class1 = Class.forName("android.bluetooth.BluetoothDevice")
        val createBondMethod = class1.getMethod("createBond")
        val returnValue = createBondMethod.invoke(btDevice) as Boolean
        return returnValue
    }

    private fun callThread(btDevice: BluetoothDevice) {
        object : Thread() {
            override fun run() {

                var isBonded: Boolean? = false
                    isBonded = createBond(btDevice)

                connect(btDevice);
                Log.i("Log", "The bond is created: " + isBonded)
            }
        }.start()
    }

    private fun connect(btDevice: BluetoothDevice): Boolean? {
        var bool: Boolean? = false
        try {
            Log.i("Log", "service method is called ")
            val cl = Class.forName("android.bluetooth.BluetoothDevice")
            val par = arrayOf<Class<*>>()
            val method = cl.getMethod("createBond", *par)
            val args = arrayOf<Any>()
            bool = method.invoke(btDevice) as Boolean
        } catch (e: Exception) {
            Log.i("Log", "Inside catch of serviceFromDevice Method")
            e.printStackTrace()
        }

        return bool
    }
    
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.M)
    public fun setListeners(){
        list_Devices.addOnItemTouchListener(RecyclerItemClickListenr(this, list_Devices, object : RecyclerItemClickListenr.OnItemClickListener {
            @SuppressLint("SetTextI18n")
            @TargetApi(Build.VERSION_CODES.M)
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onItemClick(view: View, position: Int) {
                bluetoothAdapter?.cancelDiscovery()
                d(TAG, "onItemClick: You Clicked on a device.")
                val deviceName = arrDeviceList.get(position).deviceNameLine
                val deviceAddress = arrDeviceList.get(position).deviceAddressLine
                d(TAG, "onItemClick: deviceName = $deviceName")
                d(TAG, "onItemClick: deviceAddress = $deviceAddress")
                checkBTPermissions()
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    d(TAG, "Trying to pair with $deviceAddress")
                    textView.text="Trying to pair with $deviceAddress"
                    val device = bluetoothAdapter?.getRemoteDevice(arrDeviceList.get(position).deviceAddressLine)
                    if (device != null) {
                        callThread(device)
                    }
                    val bondStateChangedIntent = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    registerReceiver(receiver2, bondStateChangedIntent)
                }
            }
            override fun onItemLongClick(view: View?, position: Int) {
                TODO("do nothing")
            }
        }))

        searchDeviceBtn.setOnClickListener {
            textView.text="Searching Started"
            if (bluetoothAdapter!!.isDiscovering()) {
                arrDeviceList.clear()
                textView.text="Searching Started Again"
                bluetoothAdapter.cancelDiscovery()
                this.checkBTPermissions()
                bluetoothAdapter.startDiscovery()
                val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, discoverDevicesIntent)
            }
            if (!bluetoothAdapter.isDiscovering()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.checkBTPermissions()
                }
                bluetoothAdapter.startDiscovery()
                val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, discoverDevicesIntent)
            }
        }

        statusBtn.setOnClickListener {
            bluetoothAdapter?.cancelDiscovery()
            checkPairedDeviceStatus()

        }

        stop_SearchBtn.setOnClickListener {
            bluetoothAdapter!!.cancelDiscovery()
            textView.text="Searching Stopped"
            arrDeviceList.clear()
            list_Devices.layoutManager = LinearLayoutManager(this) as RecyclerView.LayoutManager?
            list_Devices.adapter = deviceAdapter(arrDeviceList, this)

        }

        enableBtn.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }


    }

    public fun setRegisters(){

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        //Check bonding status ! Bonding || None || Already Bonded
        val filter2= IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(receiver2,filter2)
    }

    @SuppressLint("SetTextI18n")
    public fun checkPairedDeviceStatus(){
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            d(TAG, "PairedDevice: deviceName = $deviceName")
            d(TAG, "PairedDevice: deviceAddress = $deviceHardwareAddress")
        }
        textView.text="$pairedDevices"
    }

}












