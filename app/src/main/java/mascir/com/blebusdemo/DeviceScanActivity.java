package mascir.com.blebusdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    ArrayList<String> list;
    MediaPlayer mp;
    int volume_level = 1, volume_incr = 10;
    boolean done;
    AlertDialog.Builder builder;
    private LocationManager manager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 123;
    private static final int REQUEST_ENABLE_BT = 124;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        builder = new AlertDialog.Builder(DeviceScanActivity.this);

        list = new ArrayList<String>();
        //list.add("Personne A");
        //list.add("Personne B");
        //list.add("Personne C");
        //list.add("24:6F:28:B5:48:6E");
        //list.add("24:6F:28:B5:43:A6");
        list.add("AC:23:3F:8B:24:DA");
        list.add("AC:23:3F:8B:24:DB");
        list.add("AC:23:3F:8B:24:DC");
        list.add("AC:23:3F:AB:98:A6");
        list.add("AC:23:3F:AB:98:A7");
        list.add("AC:23:3F:AB:98:A8");


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (manager != null) {
            String provider = manager.getBestProvider(new Criteria(), false);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Make sure we have access fine location and bluetooth enabled, if not, prompt the user to enable it
        getLocationPermission();
        getBluetoothPermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        /*if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }*/
        //if(!isLocationGranted()) requestLocation();

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

        if(mBluetoothAdapter.isEnabled() && isLocationGranted() && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            scanLeDevice(true);
        }
        //else
         //   Toast.makeText(this, "Veuillez activer Bluetooth & GPS", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothDevice> mLeDevices;
        private final LayoutInflater mInflator;
        private final Map<String,String> listDevices;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
            listDevices = new HashMap<String, String>();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            String state;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.state = (TextView) view.findViewById(R.id.state);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            listDevices.put("AC:23:3F:8B:24:DA", "Personne A");
            listDevices.put("AC:23:3F:8B:24:DB", "Personne B");
            listDevices.put("AC:23:3F:8B:24:DC", "Personne C");
            listDevices.put("AC:23:3F:AB:98:A6", "Personne D");
            listDevices.put("AC:23:3F:AB:98:A7", "Personne E");
            listDevices.put("AC:23:3F:AB:98:A8", "Personne F");

            BluetoothDevice device = mLeDevices.get(i);
            String deviceAddress = device.getAddress();
            String deviceName = "";

            for (Map.Entry<String, String> set :
                    listDevices.entrySet()) {
                if(set.getKey().equals(deviceAddress)){
                    deviceName = set.getValue();
                }
                // Printing all elements of a Map
               // System.out.println(set.getKey() + " = "
                 //       + set.getValue());
            }
            //= device.getName();
            //if(deviceAddress.equals("24:6F:28:B5:48:6E")) deviceName = "Personne A";
            //else if(deviceAddress.equals("24:6F:28:B5:43:A6")) deviceName = "Personne B";
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(deviceAddress);

            if(listDevices.containsKey(deviceAddress)){
                state = "Autorisé";
                viewHolder.state.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_verified_user, 0);
            }else {
                state = "Non autorisé";
                viewHolder.state.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_unauthorized_user, 0);
                //startAlarm(deviceName+" n'est pas autorisé à monter à ce bus!!");
            }

            viewHolder.state.setText(state);

            return view;
        }
}

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(getDistance(rssi) < 1.5) {
                        //if (list.contains(device.getAddress())) {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        //}
                    }
                }
            });
        }
    };

    private double getDistance(int rssi) {
        int txPower = -63; //hard coded power value. Usually ranges between -59 to -65
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * n = 2 (in free space)
         *
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 4));
    }

    private void alertDialog(String message){
        //Uncomment the below code to Set the message and title from the strings.xml file
        //builder.setMessage("test").setTitle("Bus Alert");
        playSound(DeviceScanActivity.this, getAlarmUri());
        //Setting message manually and performing action on button click
        builder.setMessage(message)
                .setCancelable(false)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //finish();
                        dialog.cancel();
                        mp.stop();
                    }
                });

        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle("Bus Alert");
        alert.setIcon(R.drawable.ic_bus_alert);
        alert.show();
    }

    private void playSound(Context context, Uri alert) {
        mp = new MediaPlayer();
        try {
            mp.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                mp.setVolume(volume_level, volume_incr);
                mp.prepare();
                mp.start();
            }
        } catch (IOException e) {
            System.out.println("OOPS");
        }
    }

    private Uri getAlarmUri() {

        Uri alert = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (alert == null) {
            alert = RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alert == null) {
                alert = RingtoneManager
                        .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }
        return alert;
    }



    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView state;
    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            ActivityCompat.requestPermissions(DeviceScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);

        } else {
            // Permission has already been granted, check if its enabled
            statusCheck();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                //Toast.makeText(this, "Maintenant, vous êtes en sécurité!", Toast.LENGTH_LONG).show();
                statusCheck();
            } else {
                // permission denied
                Toast.makeText(DeviceScanActivity.this, "Vous devez accepter les autorisations!!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void statusCheck() {

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
        builder.setMessage("Permettre à Safety Tracker d'accéder à votre position GPS ?")
                .setCancelable(false)
                .setPositiveButton("Autoriser", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Refuser", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void getBluetoothPermission() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Votre appareil ne supporte pas Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public boolean isLocationGranted() {
        // Check if Permission has already been granted
        return ContextCompat.checkSelfPermission(DeviceScanActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}