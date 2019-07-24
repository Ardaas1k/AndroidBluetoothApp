package com.example.bluetoothapp

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

public class deviceAdapter(val items: ArrayList<parcelabelBluetoothDevices>, val context: Context) : RecyclerView.Adapter<MainActivity.ViewHolder>() {
    override fun onBindViewHolder(holder: MainActivity.ViewHolder, position: Int){
        holder.deviceNameTxt.text = items.get(position).deviceNameLine
        holder.deviceAddressTxt.text=items.get(position).deviceAddressLine
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainActivity.ViewHolder {
        return MainActivity.ViewHolder(LayoutInflater.from(context).inflate(R.layout.activity_list, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

}