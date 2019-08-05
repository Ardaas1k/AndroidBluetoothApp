package com.example.bluetoothapp

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import java.util.*

class parcelabelBluetoothDevices(val deviceNameLine:String, val deviceAddressLine:String) : Parcelable {
    constructor(parcel: String) : this(parcel,parcel) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(deviceNameLine)
        parcel.writeString(deviceAddressLine)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<parcelabelBluetoothDevices> {
        override fun createFromParcel(parcel: Parcel): parcelabelBluetoothDevices {
            return parcelabelBluetoothDevices(parcel.toString())
        }

        override fun newArray(size: Int): Array<parcelabelBluetoothDevices?> {
            return arrayOfNulls(size)
        }
    }

}