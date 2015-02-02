package idea.bluetooth.spp.library;

import android.bluetooth.BluetoothDevice;

public interface BluetoothSPPListener {
	public void onFoundDevice(BluetoothDevice device);
	public void onReceiveData();
}
