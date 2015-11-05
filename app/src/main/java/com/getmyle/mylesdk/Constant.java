package com.getmyle.mylesdk;

import java.util.UUID;

public interface Constant {
    enum ConnectState {
        BLE_CONNECT_SUCCESS,
        BLE_CONNECT_FAIL
    }

    String DEVICE_PARAM_RECLN = "RECLN";
    String DEVICE_PARAM_PAUSELEVEL = "PAUSELEVEL";
    String DEVICE_PARAM_PAUSELEN = "PAUSELEN";
    String DEVICE_PARAM_ACCELERSENS = "ACCELERSENS";
    String DEVICE_PARAM_BTLOC = "BTLOC";
    String DEVICE_PARAM_MIC = "MIC";
    String DEVICE_PARAM_VERSION = "VERSION";
    String DEVICE_PARAM_UUID = "UUID";

    interface RECEIVE_MODE {
        int RECEIVE_AUDIO_FILE = 30;
        int RECEIVE_LOG_FILE = 31;
        int RECEIVE_NONE = 32;
    }

    interface SharedPrefencesKeyword {
        String LAST_CONNECTED_TAP_UUID = "LAST_CONNECTED_TAP_UUID";
        String LAST_CONNECTED_TAP_PASS = "LAST_CONNECTED_TAP_PASS";
    }

    UUID SERVICE_UUID = UUID.fromString("14839ac4-7d7e-415c-9a42-167340cf2339");
    UUID CHARACTERISTIC_UUID_TO_WRITE = UUID.fromString("ba04c4b2-892b-43be-b69c-5d13f2195392");
    UUID CHARACTERISTIC_UUID_TO_READ = UUID.fromString("0734594a-a8e7-4b1a-a6b1-cd5243059a57");
    String CHARACTERISTIC_UUID_CONFIG1 = "e06d5efb-4f4a-45c0-9eb1-371ae5a14ad4";
    String CHARACTERISTIC_UUID_CONFIG2 = "8b00ace7-eb0b-49b0-bbe9-9aee0a26e1a3";
    String NOTIFICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    String PARAMETER_CHARACTERISTIC = "00002a04-0000-1000-8000-00805f9b34fb";

    String BATTERY_LEVEL_UUID = "00002a19-0000-1000-8000-00805f9b34fb";
    String BATTERY_SERVICE_UUID = "0000180F-0000-1000-8000-00805f9b34fb";

    String TAP_NOTIFICATION_SCAN = "tap-notification-scan";
    String TAP_NOTIFICATION_TAP_CONNECTED = "tap-notification-tap-connected";
    String TAP_NOTIFICATION_TAP_CONNECTED_PARAM = "tap-notification-tap-connected-param";
    String TAP_NOTIFICATION_TAP_DISCONNECTED = "tap-notification-tap-disconnected";
    String TAP_NOTIFICATION_TAP_DISCONNECTED_PARAM = "tap-notification-tap-disconnected-param";

    byte[] MESSAGE_CONNECTED = "CONNECTED".getBytes();
    byte[] MESSAGE_FILE_AUDIO = "55040".getBytes();
    byte[] MESSAGE_FILE_LOG = "55041".getBytes();
    byte[] MESSAGE_RECLN = ("5503" + DEVICE_PARAM_RECLN).getBytes();
    byte[] MESSAGE_PAUSELEVEL = ("5503PAUSELEVEL" + DEVICE_PARAM_RECLN).getBytes();
    byte[] MESSAGE_PAUSELEN = ("5503PAUSELEN" + DEVICE_PARAM_PAUSELEN).getBytes();
    byte[] MESSAGE_ACCELERSENS = ("5503ACCELERSENS" + DEVICE_PARAM_ACCELERSENS).getBytes();
    byte[] MESSAGE_BTLOC = ("5503BTLOC" + DEVICE_PARAM_BTLOC).getBytes();
    byte[] MESSAGE_MIC = ("5503MIC" + DEVICE_PARAM_MIC).getBytes();
    byte[] MESSAGE_VERSION = ("5503VERSION" + DEVICE_PARAM_VERSION).getBytes();
    byte[] MESSAGE_UUID = ("5503UUID" + DEVICE_PARAM_UUID).getBytes();
}
