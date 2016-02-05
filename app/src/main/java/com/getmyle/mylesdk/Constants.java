package com.getmyle.mylesdk;

import java.util.UUID;

public interface Constants {
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
    String DEVICE_PARAM_PASS = "PASS";

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

    UUID BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    UUID BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");


    // send on scan events
    String TAP_NOTIFICATION_SCAN = "tap-notification-scan";

    // send when tap is connected and authenticated
    String TAP_NOTIFICATION_TAP_AUTHED = "tap-notification-tap-authed";
    String TAP_NOTIFICATION_TAP_AUTHED_PARAM = "tap-notification-tap-authed-param";

    // send when tap authentication failed
    String TAP_NOTIFICATION_TAP_AUTH_FAILED = "tap-notification-tap-auth-failed";
    String TAP_NOTIFICATION_TAP_AUTH_FAILED_PARAM = "tap-notification-tap-auth-failed-param";

    // send when tap is disconnected
    String TAP_NOTIFICATION_TAP_DISCONNECTED = "tap-notification-tap-disconnected";
    String TAP_NOTIFICATION_TAP_DISCONNECTED_PARAM = "tap-notification-tap-disconnected-param";

    // send file is received
    String TAP_NOTIFICATION_FILE = "tap-notification-file";
    String TAP_NOTIFICATION_FILE_PATH_PARAM = "tap-notification-file-path-param";
    String TAP_NOTIFICATION_FILE_TIME_PARAM = "tap-notification-file-time-param";


    byte[] MESSAGE_CONNECTED = "CONNECTED".getBytes();
    byte[] MESSAGE_FILE_AUDIO = "55040".getBytes();
    byte[] MESSAGE_FILE_LOG = "55041".getBytes();

    byte[] READ_RECLN = ("5503" + DEVICE_PARAM_RECLN).getBytes();
    byte[] READ_PAUSELEVEL = ("5503" + DEVICE_PARAM_PAUSELEVEL).getBytes();
    byte[] READ_PAUSELEN = ("5503" + DEVICE_PARAM_PAUSELEN).getBytes();
    byte[] READ_ACCELERSENS = ("5503" + DEVICE_PARAM_ACCELERSENS).getBytes();
    byte[] READ_BTLOC = ("5503" + DEVICE_PARAM_BTLOC).getBytes();
    byte[] READ_MIC = ("5503" + DEVICE_PARAM_MIC).getBytes();
    byte[] READ_VERSION = ("5503" + DEVICE_PARAM_VERSION).getBytes();
    byte[] READ_UUID = ("5503" + DEVICE_PARAM_UUID).getBytes();

    String WRITE_RECLN = "5502" + DEVICE_PARAM_RECLN;
    String WRITE_PAUSELEVEL = "5502" + DEVICE_PARAM_PAUSELEVEL;
    String WRITE_PAUSELEN = "5502" + DEVICE_PARAM_PAUSELEN;
    String WRITE_ACCELERSENS = "5502" + DEVICE_PARAM_ACCELERSENS;
    String WRITE_BTLOC = "5502" + DEVICE_PARAM_BTLOC;
    String WRITE_MIC = "5502" + DEVICE_PARAM_MIC;
    String WRITE_PASSWORD = "5502" + DEVICE_PARAM_PASS;

    String AudioFileNameFormat = "yyyy-MM-dd-HH-mm-ss'.wav'";
    String LogFileNameFormat = "yyyy-MM-dd-HH-mm-ss'.log'";
}
