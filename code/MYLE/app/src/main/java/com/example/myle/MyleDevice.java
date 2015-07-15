package com.example.myle;

import android.bluetooth.BluetoothDevice;

public class MyleDevice {
	private BluetoothDevice device;
	private String name;
	
	public MyleDevice(BluetoothDevice device, String name) {
		this.device = device;
		this.name = name;
	}
	
	public BluetoothDevice getDevice() {
		return this.device;
	}
	
	public String getName() {
		return this.name;
	}
}
