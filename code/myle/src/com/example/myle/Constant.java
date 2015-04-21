package com.example.myle;

public class Constant {
	public enum ConnectState {
		BLE_CONNECT_SUCCESS,
		BLE_CONNECT_FAIL
	}
	
	public class SharedPrefencesKeyword {
		public static final String PASSWORD = "PASSWORD";
		public static final String PERIPHERAL_ADDRESS = "PERIPHERAL_ADDRESS";
	}

	public static final String SERVICE_UUID                   = "14839ac4-7d7e-415c-9a42-167340cf2339";
	public static final String CHARACTERISTIC_UUID_TO_WRITE   = "ba04c4b2-892b-43be-b69c-5d13f2195392";
	public static final String CHARACTERISTIC_UUID_TO_READ    = "0734594a-a8e7-4b1a-a6b1-cd5243059a57";
	public static final String CHARACTERISTIC_UUID_CONFIG1    = "e06d5efb-4f4a-45c0-9eb1-371ae5a14ad4";
	public static final String CHARACTERISTIC_UUID_CONFIG2    = "8b00ace7-eb0b-49b0-bbe9-9aee0a26e1a3";
	public static final String NOTIFICATION_DESCRIPTOR 		  = "00002902-0000-1000-8000-00805f9b34fb";
	public static final String PARAMETER_CHARACTERISTIC 	  = "00002a04-0000-1000-8000-00805f9b34fb";
	
}
