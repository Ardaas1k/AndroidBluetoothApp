@file:Suppress("DEPRECATION")

package com.example.bluetoothapp


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.util.Log.d
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_list.view.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.Charset.defaultCharset
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {

        private val REQUEST_ENABLE_BT = 0
        var arrDeviceList: ArrayList<parcelabelBluetoothDevices> = ArrayList()
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        private val appName = "MYAPP"
        var MY_INSECUREUUID:UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var mBTDevice: BluetoothDevice? = null
        var uuids: ArrayList<ParcelUuid>? = null
        var deviceUUID: UUID? = null
        const val TAG = "MY_APP_DEBUG_TAG"
        lateinit var mConnectedThread: ConnectedThread

    }

    private val receiver = object : BroadcastReceiver(){
        @SuppressLint("NewApi")
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {

                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    var deviceName = device?.name
                    if(device?.name==null){
                        deviceName="N/A"
                    }
                    val deviceHardwareAddress = device?.address // MAC address
                    val newDevice = parcelabelBluetoothDevices(deviceName.toString(),
                        deviceHardwareAddress.toString()
                    )
                    insertItem(newDevice)
                }
            }
        }
    }

    private val receiver2 = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val mDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (mDevice!!.bondState == BluetoothDevice.BOND_BONDED) {
                    d(TAG, "BroadcastReceiver: BOND_BONDED.")
                    statusTxt.text="BroadcastReceiver: BOND_BONDED."
                }

                if (mDevice.bondState == BluetoothDevice.BOND_BONDING) {
                    d(TAG, "BroadcastReceiver: BOND_BONDING.")
                    statusTxt.text="BroadcastReceiver: BOND_BONDING."
                }

                if (mDevice.bondState == BluetoothDevice.BOND_NONE) {
                    d(Companion.TAG, "BroadcastReceiver: BOND_NONE.")
                    statusTxt.text="BroadcastReceiver: BOND_NONE."
                }
            }
        }
    }

    private val receiver3 = object : BroadcastReceiver() {

        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {

                val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)

                when (mode) {
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ->{
                        d(TAG, "BroadcastReceiver: BOND_NONE.")
                        statusTxt.text="Discoverability Enabled."
                    }

                    BluetoothAdapter.SCAN_MODE_CONNECTABLE ->{
                        d(TAG, "BroadcastReceiver: Discoverability Disabled. Able to receive connections.")
                        statusTxt.text=" Discoverability Disabled. Able to receive connections."
                    }


                    BluetoothAdapter.SCAN_MODE_NONE ->{
                        d(TAG, "BroadcastReceiver: Discoverability Disabled. Not able to receive connections.")
                        statusTxt.text="Discoverability Disabled. Not able to receive connections."
                        }


                    BluetoothAdapter.STATE_CONNECTING ->{
                        d(TAG, "BroadcastReceiver: Connecting....")
                        statusTxt.text="Connecting...."
                        }


                    BluetoothAdapter.STATE_CONNECTED ->{
                        d(TAG, "BroadcastReceiver: Connected.")
                        statusTxt.text="Connected."
                        }

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterReceiver(receiver2)
        unregisterReceiver(receiver3)
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arrDeviceList=ArrayList()
        setListeners()
        setRegisters()

    }

    private fun insertItem(newDevice: parcelabelBluetoothDevices) {
        arrDeviceList.add(newDevice)
        arrDeviceList.distinct()
        list_Devices.layoutManager = LinearLayoutManager(this)
        list_Devices.adapter = deviceAdapter(arrDeviceList, this)
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

                val isBonded: Boolean?
                isBonded = createBond(btDevice)
                d(TAG, "The bond is created: " + isBonded)
                if (isBonded==false){
                    statusTxt.text="Bluetooth Device Already Paired"
                }

            }
        }.start()
    }

    @SuppressLint("NewApi", "SetTextI18n")
    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.M)
    fun setListeners(){

        list_Devices.addOnItemTouchListener(RecyclerItemClickListenr(this, list_Devices, object : RecyclerItemClickListenr.OnItemClickListener {
            @SuppressLint("SetTextI18n")
            @TargetApi(Build.VERSION_CODES.M)
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onItemClick(view: View, position: Int) {
                bluetoothAdapter?.cancelDiscovery()
                d(Companion.TAG, "onItemClick: You Clicked on a device.")
                val deviceName = arrDeviceList.get(position).deviceNameLine
                val deviceAddress = arrDeviceList.get(position).deviceAddressLine
                d(Companion.TAG, "onItemClick: deviceName = $deviceName")
                d(Companion.TAG, "onItemClick: deviceAddress = $deviceAddress")

                checkBTPermissions()

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    d(TAG, "Trying to pair with $deviceAddress")
                    statusTxt.text="Trying to pair with $deviceAddress"
                    val device = bluetoothAdapter?.getRemoteDevice(arrDeviceList.get(position).deviceAddressLine)
                    callThread(device!!)
                    val bondStateChangedIntent = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                    registerReceiver(receiver2, bondStateChangedIntent)

                    mBTDevice=device
                    mBTDevice?.uuids
                    deviceUUID= UUID.fromString(mBTDevice?.uuids?.get(0).toString())

                }
            }
            override fun onItemLongClick(view: View?, position: Int) {
                println("long")
            }
        }))

        searchDeviceBtn.setOnClickListener {
            if(bluetoothAdapter?.isEnabled==false){
                statusTxt.text="please enable bluetooth from blow"
            }else{
                statusTxt.text="Searching Started"
                if (bluetoothAdapter!!.isDiscovering) {
                    arrDeviceList.clear()
                    statusTxt.text="Searching Started Again"
                    bluetoothAdapter.cancelDiscovery()
                    this.checkBTPermissions()
                    bluetoothAdapter.startDiscovery()
                    val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(receiver, discoverDevicesIntent)
                }
                if (!bluetoothAdapter.isDiscovering) {
                    arrDeviceList.clear()
                    statusTxt.text="Searching Started Again"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        this.checkBTPermissions()
                    }
                    bluetoothAdapter.startDiscovery()
                    val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
                    registerReceiver(receiver, discoverDevicesIntent)

                }
            }
        }

        sendBtn.setOnClickListener {
            val bytes:ByteArray=sendTxt.text.toString().toByteArray(defaultCharset())
            mConnectedThread.write(bytes)

        }

        discoverableBtn.setOnClickListener {

            d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.")

            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)

            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            registerReceiver(receiver3, intentFilter)
        }

        connectionBtn.setOnClickListener {
            ConnectThread().start()
        }

        listenBtn.setOnClickListener {
            AcceptThread().start()
        }

        statusBtn.setOnClickListener {
            bluetoothAdapter?.cancelDiscovery()
        }

        stop_SearchBtn.setOnClickListener {
            bluetoothAdapter!!.cancelDiscovery()
            statusTxt.text="Searching Stopped"
            arrDeviceList.clear()
            list_Devices.layoutManager = LinearLayoutManager(this)
            list_Devices.adapter = deviceAdapter(arrDeviceList, this)

        }

        enableBtn.setOnClickListener {
            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

     fun setRegisters(){
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        val filter2= IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(receiver2,filter2)
    }

    @SuppressLint("SetTextI18n")
    fun checkPairedDeviceStatus(){
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            d(TAG, "PairedDevice: deviceName = $deviceName")
            d(TAG, "PairedDevice: deviceAddress = $deviceHardwareAddress")
        }
    }

    inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(appName, MY_INSECUREUUID)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.e("","Listening")
                    mmServerSocket?.accept()


                } catch (e: IOException) {
                    Log.e(Companion.TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {

                    mmServerSocket?.close()
                    shouldLoop = false

                }
                mConnectedThread=ConnectedThread(socket)
                mConnectedThread.start()
            }
            Log.e("","Connected")

        }
        // Closes the connect socket and causes the thread to finish.
    }

    inner class ConnectThread : Thread() {

        var mmSocket: BluetoothSocket? = null

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()
                try {
                    mmSocket = mBTDevice?.createInsecureRfcommSocketToServiceRecord(MY_INSECUREUUID)
                    mmSocket?.connect()
                    Log.e("","Connected")


                } catch (e:Exception ) {
                    Log.e("","Error creating socket")
                }
            mConnectedThread=ConnectedThread(mmSocket)
            mConnectedThread.start()
            }
    }

    inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        val mmSocket=socket
        val mmInStream: InputStream? = mmSocket?.inputStream
        val mmOutStream: OutputStream? = mmSocket?.outputStream
        val mmBuffer = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var bytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    bytes = mmInStream!!.read(mmBuffer)
                    val incomingMessage = String(mmBuffer,0,bytes)
                    statusTxt.text=incomingMessage
                }catch (ex:IOException){
                    Log.e(TAG, "write: Error reading Input Stream. " + ex.message );
                    break;
                }

            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            val text:String= String(bytes, Charset.defaultCharset())
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream?.write(bytes)

            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

            }
            Log.d(TAG, "write: Write Called.")

        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}













