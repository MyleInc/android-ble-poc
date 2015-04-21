# android-ble-poc
A simple program to communicate with board.


+++ April, 21, 2015 +++

1. Tested device:
    + Galaxy S3 (Android 4.3): work
    + Galaxy S5 (Androdi 4.4): work
    + LG G2 (Android 4.3): work
    
    + All device runs >= Android 5.0 doesn't work. [Nexus 5 (Android 5.1), Nexus 7 (Android 5.0), Galaxy S3 (Cynamon 5.0)]
    
        Bugs:
        
        + "BluetoothGatt.connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback)" always has status is 133 (GATT_ERROR).

        + Maybe TI firmware doesn't support Android 5.0.
    
    
2. Max speed: 300 bytes/second

3. Can't change connection parameter due to api BluetoothGatt.requestConnectionPriority() only
    available from SDK 21 (Android 5.0)
