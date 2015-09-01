package systems.movingdata.blemeshmessage;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Ap on 8/31/2015.
 */
class MessageBeacon {

    /* Full Bluetooth UUID that defines the Health Thermometer Service */
    public static final ParcelUuid SERVICE =   ParcelUuid.fromString("0003cbbb-0000-1000-8000-00805f9b0131");
    public static final ParcelUuid SERVICE_M = ParcelUuid.fromString("00011111-0000-0000-0000-000000000000");
    /* Short-form UUID that defines the Health Thermometer service */
    public static final ParcelUuid UUID_Message =  ParcelUuid.fromString("0003cbb1-0000-1000-8000-00805f9b0131");
    public static final ParcelUuid UUID_Message_Count =  ParcelUuid.fromString("0003cbb2-0001-0008-0000-0805f9b01310");
    public String mName;
    public String CurrentMsg;
    //Device metadata
    public int mSignal;
    public String mAddress;
    private static final String TAG = "BLEDevice";
    MainActivity fthis;

    /* Builder for Lollipop+ */
    public MessageBeacon(ScanRecord record, String deviceAddress, int rssi, BluetoothDevice mDevice, MainActivity _fthis) {
        mSignal = rssi;
        mAddress = deviceAddress;

        mName = record.getDeviceName();
        fthis = _fthis;

        if ((fthis.mConnGatt == null) && (fthis.mStatus == BluetoothProfile.STATE_DISCONNECTED)) {
            // try to connect
            fthis.mConnGatt = mDevice.connectGatt(fthis, false, mGattcallback);
            fthis.mStatus = BluetoothProfile.STATE_CONNECTING;
        } else {
            if (fthis.mConnGatt != null) {
                // re-connect and re-discover Services
                fthis.mConnGatt.connect();
                fthis.mConnGatt.discoverServices();
            } else {
                Log.e(TAG, "state error");
                return;
            }
        }
    }

    public String getName() {
        return mName;
    }

    public int getSignal() {
        return mSignal;
    }

    public String getCurrentMsg() {
        return CurrentMsg;
    }

    public String getAddress() {
        return mAddress;
    }

    @Override
    public String toString() {
        return String.format("%s (%ddBm): %s", mName, mSignal, CurrentMsg);
    }





    private final BluetoothGattCallback mGattcallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                fthis.mStatus = newState;
                fthis.mConnGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                fthis.mStatus = newState;

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            for (BluetoothGattService service : gatt.getServices()) {
                if ((service == null) || (service.getUuid() == null)) {
                    continue;
                }
                if (MessageBeacon.SERVICE.toString().equalsIgnoreCase(service.getUuid().toString())) {
                    fthis.mConnGatt.readCharacteristic(service.getCharacteristic(UUID.fromString(MessageBeacon.UUID_Message.toString())));
                }
            }
        }

        @Override
        public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic,int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (MessageBeacon.UUID_Message.toString().equalsIgnoreCase(characteristic.getUuid().toString())) {
                    final String name = characteristic.getStringValue(0);
                    Log.d(TAG, "onBatchScanResults: "+name);
                    fthis.runOnUiThread(new Runnable() {
                        public void run() {
                            CurrentMsg= name;
                            fthis.mHandler.sendMessage(Message.obtain(null, 0, MessageBeacon.this));
                        }
                    });
                } else if (MessageBeacon.UUID_Message_Count.toString().equalsIgnoreCase(characteristic.getUuid().toString())) {
                    final String name = characteristic.getStringValue(0);
                    Log.d(TAG, "onBatchScanResults: "+name);
                }

            }
        }

        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic,int status){
        }
        ;
    };
}
