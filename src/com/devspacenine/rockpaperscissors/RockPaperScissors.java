package com.devspacenine.rockpaperscissors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ads.AdRequest;

public class RockPaperScissors extends Activity implements View.OnTouchListener, OnDismissListener {
	// Dialog ids
	public static final int CREATE_OR_JOIN_DIALOG = 1;
	public static final int BEST_OF_DIALOG = 2;
	
	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 0;
	private static final int REQUEST_BT_DISCOVERABLE = 2;
	
	// Preferences File names
	public static final String MAIN_PREFERENCES = "MainPreferencesFile";
	
	private enum Opponent {
		COMPUTER,
		FRIEND,
		STRANGER;
	}
	private enum GameType {
		ACCELEROMETER,
		DRAGANDDROP,
		BUTTON;
	}
	private Resources res;
	private Opponent opponent_choice;
	private BluetoothAdapter mBluetoothAdapter;
	private ImageView robot_button;
	private ImageView bluetooth_button;
	private int best_of;
	private int wins_needed;
	private Intent gameIntent;
	private SharedPreferences settings;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add test devices for development so we don't look fishy
		AdRequest request = new AdRequest();
		request.addTestDevice(AdRequest.TEST_EMULATOR);
		request.addTestDevice("HT07NHL01089"); // My Evo test phone
		request.addTestDevice("3332BBFD4E5200EC"); // My Samsung tablet
        
        setContentView(R.layout.main);
        
        // Setup the sound manager
        SoundEffectManager.getInstance();
        SoundEffectManager.initSounds(this);
        SoundEffectManager.loadSounds();
        
        // Load game settings
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        res = getResources();
        robot_button = (ImageView) findViewById(R.id.robot_img);
        robot_button.setOnTouchListener(this);
        bluetooth_button = (ImageView) findViewById(R.id.bt_img);
        bluetooth_button.setOnTouchListener(this);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode , resultCode, data);
    	switch(requestCode) {
    	case REQUEST_BT_DISCOVERABLE:
			if(resultCode != RESULT_CANCELED) {
				startActivity(gameIntent);
			}
			break;
    	case REQUEST_ENABLE_BT:
			if(resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Bluetooth must be enabled to play with a friend.", Toast.LENGTH_SHORT).show();
			}else{
				switch(GameType.valueOf(settings.getString("game_type_preference", "ACCELEROMETER"))) {
		        case ACCELEROMETER:
		        	gameIntent = new Intent(this, AccelerometerGame.class);
		        	break;
		        case DRAGANDDROP:
		        	gameIntent = new Intent(this, DragAndDropGame.class);
		        	break;
		        case BUTTON:
		        	gameIntent = new Intent(this, ButtonGame.class);
		        	break;
		        }
				gameIntent.putExtra("opponent_choice", "FRIEND");
				showDialog(CREATE_OR_JOIN_DIALOG);
			}
			break;
    	}
    }
    
    public void playGame(View v) {
    	opponent_choice = Opponent.valueOf(v.getTag().toString());
    	if(opponent_choice == Opponent.FRIEND) {
    		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		if(mBluetoothAdapter == null) {
    			Toast.makeText(this, "Your device does not have Bluetooth support.", Toast.LENGTH_SHORT).show();
    		}else{
    			if(!mBluetoothAdapter.isEnabled()) {
    				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    			}else{
    				switch(GameType.valueOf(settings.getString("game_type_preference", "ACCELEROMETER"))) {
    		        case ACCELEROMETER:
    		        	gameIntent = new Intent(this, AccelerometerGame.class);
    		        	break;
    		        case DRAGANDDROP:
    		        	gameIntent = new Intent(this, DragAndDropGame.class);
    		        	break;
    		        case BUTTON:
    		        	gameIntent = new Intent(this, ButtonGame.class);
    		        	break;
    		        }
    				gameIntent.putExtra("opponent_choice", "FRIEND");
    				showDialog(CREATE_OR_JOIN_DIALOG);
    			}
    		}
    	}else{
    		switch(GameType.valueOf(settings.getString("game_type_preference", "ACCELEROMETER"))) {
            case ACCELEROMETER:
            	gameIntent = new Intent(this, AccelerometerGame.class);
            	break;
            case DRAGANDDROP:
            	gameIntent = new Intent(this, DragAndDropGame.class);
            	break;
            case BUTTON:
            	gameIntent = new Intent(this, ButtonGame.class);
            	break;
            }
    		gameIntent.putExtra("opponent_choice", "COMPUTER");
    		showDialog(BEST_OF_DIALOG);
    	}
    }
    
    public void viewStats(View v) {
    	Intent intent = new Intent(this, Statistics.class);
    	startActivity(intent);
    }
    
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	switch(id) {
    	case CREATE_OR_JOIN_DIALOG:
    		final String[] multiplayer_items = {"Create Game", "Join Game"};
    		
    		AlertDialog.Builder multiplayer_builder = new AlertDialog.Builder(this);
    		multiplayer_builder.setTitle("Create or join a game?");
    		multiplayer_builder.setItems(multiplayer_items, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				if(multiplayer_items[item] == "Join Game") {
    					gameIntent.putExtra("wins_needed", 1);
    					gameIntent.putExtra("bluetooth_role", "client");
    					dialog.dismiss();
    					startActivity(gameIntent);
    				}else{
    					gameIntent.putExtra("bluetooth_role", "host");
    					dialog.dismiss();
    					showDialog(BEST_OF_DIALOG);
    				}
    			}
    		});
    		dialog = multiplayer_builder.create();
    		break;
    	case BEST_OF_DIALOG:
    		final String[] best_of_items = {"1", "3", "5", "7", "9"};
    		
    		AlertDialog.Builder best_of_builder = new AlertDialog.Builder(this);
    		best_of_builder.setTitle("Best of how many games?");
    		best_of_builder.setItems(best_of_items, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				best_of = Integer.parseInt(best_of_items[item]);
					switch(best_of) {
					case 1:
						wins_needed = 1;
						break;
					case 3:
						wins_needed = 2;
						break;
					case 5:
						wins_needed = 3;
						break;
					case 7:
						wins_needed = 4;
						break;
					case 9:
						wins_needed = 5;
						break;
					default:
						wins_needed = 1;
					}
    				dialog.dismiss();
    				gameIntent.putExtra("wins_needed", wins_needed);
    				if(opponent_choice == Opponent.COMPUTER) { 
    					startActivity(gameIntent);
    				}else{
    					Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    					discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
    					startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABLE);
    				}
    			}
    		});
    		dialog = best_of_builder.create();
    		break;
    	default:
    		dialog = null;
    	}
    	return dialog;
    }
    
    // OnDismissListener interface methods
    public void onDismiss(DialogInterface dialog) {
    }
    
    // View.OnTouchListener interface methods
    public boolean onTouch(View v, MotionEvent event) {
    	int action = event.getAction();
    	ImageView img = (ImageView) v;
    	switch(action) {
    	case MotionEvent.ACTION_DOWN:
    		if(v.getTag().toString().equals("COMPUTER")) {
    			img.setImageDrawable(res.getDrawable(R.drawable.robot_button_pressed));
    		}else{
    			img.setImageDrawable(res.getDrawable(R.drawable.bluetooth_button_pressed));
    		}
    		break;
    	case MotionEvent.ACTION_UP:
    		if(v.getTag().toString().equals("COMPUTER")) {
    			img.setImageDrawable(res.getDrawable(R.drawable.robot_button));
    		}else{
    			img.setImageDrawable(res.getDrawable(R.drawable.bluetooth_button));
    		}
    		break;
    	}
    	return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.settings:
    		Intent settingsIntent = new Intent(this, Settings.class);
    		startActivity(settingsIntent);
    		return true;
    	case R.id.help:
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }
}
