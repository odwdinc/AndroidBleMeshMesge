package systems.movingdata.blemeshmessage;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "BeaconActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    /* Collect unique devices discovered, keyed by address */
    private HashMap<String,MessageBeacon> mBeacons;


    public BeaconAdapter mAdapter;
    public BluetoothGatt mConnGatt = null;
    public int mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminate(true);

        /*
         * We are going to display all the device beacons that we discover
         * in a list, using a custom adapter implementation
         */
        ListView list = new ListView(this);
        mAdapter = new BeaconAdapter(this);
        list.setAdapter(mAdapter);
        setContentView(list);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBeacons = new HashMap<String, MessageBeacon>();
        mStatus = BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Begin scanning for LE devices
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Cancel scan in progress
        if (mConnGatt != null) {
            if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
                    && (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
                mConnGatt.disconnect();
            }
            while (mStatus != BluetoothProfile.STATE_DISCONNECTED){

            }
            mConnGatt.close();
            mConnGatt = null;
        }
        stopScan();
    }


    private void startScan() {
        //Scan for devices advertising the thermometer service
        ScanFilter beaconFilter = new ScanFilter.Builder()
                //.setServiceUuid(MessageBeacon.THERM_SERVICE, MessageBeacon.THERM_SERVICE_M)
                .setDeviceName("Titan")
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(beaconFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }

    private void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: "+results.size()+" results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "LE Scan Failed: "+errorCode);
        }





        private void processResult(ScanResult result) {


            /*
             * Create a new beacon from the list of obtains AD structures
             * and pass it up to the main thread
             */

                mStatus = BluetoothProfile.STATE_DISCONNECTED;

                MessageBeacon beacon = new MessageBeacon(result.getScanRecord(),
                        result.getDevice().getAddress(),
                        result.getRssi(),result.getDevice(),MainActivity.this);


            //
        }
    };

    int FM_NOTIFICATION_ID = 0;
    public void showNotification(String Text_Msg) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                getApplicationContext()).setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle("BLE_Msg")
                .setContentText(Text_Msg)
                .setContentIntent(resultPendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(FM_NOTIFICATION_ID, mBuilder.build());
    }

    /*
     * We have a Handler to process scan results on the main thread,
     * add them to our list adapter, and update the view
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MessageBeacon beacon = (MessageBeacon) msg.obj;
            MessageBeacon beaconcpy = (MessageBeacon) msg.obj;
            mBeacons.put(beacon.getName(), beacon);
            showNotification(beacon.getCurrentMsg());

                    //mBeacons.put(beacon.getCurrentMsg(), beacon);

            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
            mAdapter.addAll(mBeacons.values());
            mAdapter.notifyDataSetChanged();





            if (mConnGatt != null) {
                if ((mStatus != BluetoothProfile.STATE_DISCONNECTING)
                        && (mStatus != BluetoothProfile.STATE_DISCONNECTED)) {
                    mConnGatt.disconnect();
                }
                while (mStatus != BluetoothProfile.STATE_DISCONNECTED){

                }
                mConnGatt.close();
                mConnGatt = null;
            }

        }
    };

    /*
     * A custom adapter implementation that displays the MessageBeacon
     * element data in columns, and also varies the text color of each row
     * by the temperature values of the beacon
     */
    private static class BeaconAdapter extends ArrayAdapter<MessageBeacon> {

        public BeaconAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_beacon_list, parent, false);
            }

            MessageBeacon beacon = getItem(position);
            //Set color based on temperature
            final int textColor = getTemperatureColor(0);

            TextView nameView = (TextView) convertView.findViewById(R.id.text_name);
            if(beacon.getName() == null){
                nameView.setVisibility(View.GONE);
            }else{
                nameView.setVisibility(View.VISIBLE);
                nameView.setText(beacon.getName());
                nameView.setTextColor(textColor);
            }

            TextView tempView = (TextView) convertView.findViewById(R.id.editText);
            if(beacon.getCurrentMsg() == null){
                tempView.setVisibility(View.GONE);
            }else{
                tempView.setVisibility(View.VISIBLE);
                tempView.setText(beacon.getCurrentMsg());
                tempView.setTextColor(textColor);
            }



            TextView addressView = (TextView) convertView.findViewById(R.id.text_address);
            if(beacon.getAddress() == null){
                addressView.setVisibility(View.GONE);
            }else{
                addressView.setVisibility(View.VISIBLE);
                addressView.setText(beacon.getAddress());
                addressView.setTextColor(textColor);
            }

            TextView rssiView = (TextView) convertView.findViewById(R.id.text_rssi);

            if(beacon.getSignal() == 0){
                rssiView.setVisibility(View.GONE);
            }else{
                rssiView.setVisibility(View.VISIBLE);
                rssiView.setText(String.format("%ddBm", beacon.getSignal()));
                rssiView.setTextColor(textColor);
            }

            return convertView;
        }

        private int getTemperatureColor(float temperature) {
            //Color range from 0 - 40 degC
            float clipped = Math.max(0f, Math.min(40f, temperature));

            float scaled = ((40f - clipped) / 40f) * 255f;
            int blue = Math.round(scaled);
            int red = 255 - blue;

            return Color.rgb(red, 0, blue);
        }
    }
}
