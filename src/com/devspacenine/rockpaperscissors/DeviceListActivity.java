package com.devspacenine.rockpaperscissors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    // Member fields
    private TextView noneFoundText;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mDevicesArrayAdapter;
    private List<String> available_devices = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bluetooth_device_list);
        noneFoundText = (TextView) findViewById(R.id.no_devices_found);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize array adapter
        mDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.available_devices);
        newDevicesListView.setAdapter(mDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Perform device discovery
        doDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discovery with the BluetoothAdapter
     */
    private void doDiscovery() {

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        Log.d("DSN Debug", "Starting device discovery");
        mBluetoothAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBluetoothAdapter.cancelDiscovery();
            Log.d("DSN Debug", "Canceling device discovery because item was clicked");
            // Create the result Intent
            Intent intent = new Intent();

            // Get the device MAC address, which is the last 17 chars in the View, and attach it to the
            // intent as a String extra
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            intent.putExtra(RockPaperScissorsGame.DEVICE_ADDRESS, address);
            
            // Check to see if the device is paired and attach the result to the intent as an boolean extra
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0) {
            	for(BluetoothDevice device : pairedDevices) {
            		if(device.getAddress().equals(address)) intent.putExtra(RockPaperScissorsGame.DEVICE_PAIRED, true);
            	}
            	if(!intent.hasExtra(RockPaperScissorsGame.DEVICE_PAIRED)) intent.putExtra(RockPaperScissorsGame.DEVICE_PAIRED, false);
            }else{
            	intent.putExtra(RockPaperScissorsGame.DEVICE_PAIRED, false);
            }
            
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device, add it to the list
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent and list it
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!available_devices.contains(device.getAddress())) {
	                available_devices.add(device.getAddress());
	            	noneFoundText.setVisibility(View.GONE);
	                mDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                // If no devices were found return to the title screen
                if (mDevicesArrayAdapter.getCount() == 0) {
                	finish();
                }
            }
        }
    };

}