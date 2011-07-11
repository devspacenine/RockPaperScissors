package com.devspacenine.rockpaperscissors;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private SharedPreferences settings;
	private BluetoothAdapter mBluetoothAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.registerOnSharedPreferenceChangeListener(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	// OnSharedPreferenceChangeListener interface methods
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals("name_preference")) {
			String new_name = settings.getString(key, "");
			if(!new_name.equals("")) {
				mBluetoothAdapter.setName(new_name);
			}
		}
	}
}