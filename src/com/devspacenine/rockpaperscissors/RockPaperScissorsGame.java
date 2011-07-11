package com.devspacenine.rockpaperscissors;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;

public class RockPaperScissorsGame extends Activity implements OnDismissListener, OnCancelListener {
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_PLAY = 2;
    public static final int MESSAGE_SYNC = 3;
    public static final int MESSAGE_REPLAY = 4;
    public static final int MESSAGE_WRITE = 5;
    public static final int MESSAGE_CONNECT_SUCCESSFUL = 6;
    public static final int MESSAGE_TOAST = 7;
    public static final int MESSAGE_CONNECT_FAILED = 8;
    public static final int MESSAGE_DISCONNECTED = 9;
    public static final int MESSAGE_READY_FOR_DISCONNECT = 10;
    
    // Dialog ids for the onCreateDialog method
    public static final int DIALOG_REPLAY = 1;
    public static final int DIALOG_BEST_OF = 2;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_PAIRED = "device_paired";
    public static final String TOAST = "toast";

    // Intent request codes
    protected static final int REQUEST_CONNECT_DEVICE = 1;
    protected static final int REQUEST_ENABLE_BT = 2;
    
    // Name of the connected device
    protected String mConnectedDeviceName = null;
    protected String mConnectedDeviceAddress = null;
    
    // Enums for game options
    protected enum Opponent {
		COMPUTER,
		FRIEND,
		STRANGER;
	}
    protected enum PlayChoice {
		ROCK,
		PAPER,
		SCISSORS;
		
		private static final List<PlayChoice> VALUES =
			Collections.unmodifiableList(Arrays.asList(values()));
		private static final int SIZE = VALUES.size();
		private static final Random RANDOM = new Random();
		
		public static PlayChoice randomPlayChoice() {
			return VALUES.get(RANDOM.nextInt(SIZE));
		}
	}
    
    // Local Bluetooth adapter
    protected BluetoothAdapter mBluetoothAdapter = null;
    
    // Member object for the game services
    protected BluetoothGameService mGameService = null;
    
	// Parent intent
    protected Intent sender_intent;
    
    // Game counts
	
    protected class MatchUp{
		private final PlayChoice mPlayer;
		private final PlayChoice mOpponent;
		
		private final int HASH_PRIME = 1000003;
		
		public MatchUp(PlayChoice p, PlayChoice o){
			mPlayer = p;
			mOpponent = o;
		}
		
		public boolean equals(Object o){
			if(this == o)
				return true;
			if(o == null || !(o instanceof MatchUp))
				return false;
			MatchUp mu = (MatchUp) o;
			return mu.mPlayer == mPlayer && mu.mOpponent == mOpponent;
		}
		
		public int hashCode(){
			int result = 0;
			result = HASH_PRIME * result + mPlayer.hashCode();
			result = HASH_PRIME * result + mOpponent.hashCode();
			return result;
		}
	}
	
    protected class ChoiceData{
		public final String mChoiceString;
		public final Integer mChoiceImage;
		public final Integer mLoseText;
		public final Integer mWinText;
		
		public ChoiceData(String choiceString, Integer choiceImg, Integer loseTxt, Integer winTxt){
			mChoiceString = choiceString;
			mChoiceImage = choiceImg;
			mLoseText = loseTxt;
			mWinText = winTxt;
		}
		
	}
    protected int wins_needed;
    protected int player_win_count = 0;
    protected int opponent_win_count = 0;
	
    protected Handler delayHandler = new Handler();
    protected Resources res;
    protected Toast current_toast;
    protected Context ctx;
    protected PlayChoice player_play_choice;
    protected PlayChoice opponent_play_choice;
    protected Opponent opponent_choice;
    protected ImageView player_img;
    protected ImageView opponent_img;
    protected TextView player_win_count_txt;
    protected TextView opponent_win_count_txt;
    protected TextView wins_needed_txt;
    protected ProgressDialog waiting_dialog;
    protected ProgressDialog connecting_dialog;
    protected String bluetooth_role;
    protected String player_name;
    protected boolean synced;
    protected boolean choice_made;
    protected boolean opponent_choice_made;
    protected boolean opponent_ready_to_disconnect;
    protected boolean player_ready_to_disconnect;
    protected boolean replay_message_received;
    protected boolean replay_message_sent;
    protected boolean opponent_will_replay;
    protected boolean player_will_replay;
	
    protected HashMap<MatchUp, Boolean> mOutcomeMap;
    protected EnumMap<PlayChoice, ChoiceData> mChoiceMap;
	
    protected StatsDbAdapter mStatsDbAdapter;
    protected long mGameId;
    
    protected SharedPreferences settings;
    
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	Log.d("DSN Debug", "Calling onCreate");
		super.onCreate(savedInstanceState);
		
		// Add test devices for development so we don't look fishy
		AdRequest request = new AdRequest();
		request.addTestDevice(AdRequest.TEST_EMULATOR);
		request.addTestDevice("HT07NHL01089"); // My Evo test phone
		request.addTestDevice("3332BBFD4E5200EC"); // My Samsung tablet
		
		setContentView(R.layout.game);
		
		// Initialize variables
		// Shared application settings
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		player_name = settings.getString("name_preference", "");
		
		// Intent that started this activity
		sender_intent = getIntent();
		
		// Single player or bluetooth game
		opponent_choice = Opponent.valueOf(sender_intent.getStringExtra("opponent_choice"));
		
		// Application context and resources
		res = getResources();            	
		ctx = getApplicationContext();
		
		// Views that may need to be modified at some point
		opponent_img = (ImageView) findViewById(R.id.opponent_choice);
		player_img = (ImageView) findViewById(R.id.player_choice);
		player_win_count_txt = (TextView) findViewById(R.id.player_win_count);
		opponent_win_count_txt = (TextView) findViewById(R.id.opponent_win_count);
		wins_needed_txt = (TextView) findViewById(R.id.wins_needed);
        
		// Retrieve number of wins needed for a victory and update the layout
		wins_needed = sender_intent.getIntExtra("wins_needed", 1);
		wins_needed_txt.setText("Wins Needed: " + wins_needed);
		
		// Initialize message booleans
		choice_made = false;
		opponent_choice_made = false;
		synced = false;
		opponent_ready_to_disconnect = false;
		player_ready_to_disconnect = false;
		replay_message_received = false;
		replay_message_sent = false;
		opponent_will_replay = false;
		player_will_replay = false;
		current_toast = Toast.makeText(ctx, "", Toast.LENGTH_SHORT);
		
		// Register for broadcasts when this device ends discoverability
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mReceiver, filter);
		
		// Check if bluetooth is required
		if(opponent_choice == Opponent.FRIEND) {
			// Get local Bluetooth adapter
			bluetooth_role = sender_intent.getStringExtra("bluetooth_role");
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	        // If the adapter is null, then Bluetooth is not supported. End the game
	        if (mBluetoothAdapter == null) {
	            showNotification("Bluetooth is not supported on your device. Ending game...");
	            finish();
	            return;
	        }else{
	        	// If Bluetooth is not on, request that it be enabled.
	        	if (!mBluetoothAdapter.isEnabled()) {
	                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	            }else{
		            if(bluetooth_role.equals("client")) {
		            	// This is a client device. Display a list of available bluetooth devices to choose
		            	// from
		            	Log.d("DSN Debug", "This is the client device");
		            	Intent connectIntent = new Intent(this, DeviceListActivity.class);
		            	startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
		            }else{
		            	// This is the host device. Display a waiting dialog until a remote device connects
		            	Log.d("DSN Debug", "This is the host device");
		            	waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.waiting_for_bluetooth), true, true);
		            	waiting_dialog.setOnCancelListener(this);
		            	waiting_dialog.setOnDismissListener(this);
		            }
	            }
	        }
		}
		
		mOutcomeMap = new HashMap<MatchUp, Boolean>();
		mOutcomeMap.put(new MatchUp(PlayChoice.ROCK, PlayChoice.SCISSORS), true);
		mOutcomeMap.put(new MatchUp(PlayChoice.SCISSORS, PlayChoice.PAPER), true);
		mOutcomeMap.put(new MatchUp(PlayChoice.PAPER, PlayChoice.ROCK), true);
		
		mChoiceMap = new EnumMap<PlayChoice, ChoiceData>(PlayChoice.class);
		mChoiceMap.put(PlayChoice.ROCK, new ChoiceData("ROCK", R.drawable.rock, R.string.game_lose_rock, R.string.game_win_rock));
		mChoiceMap.put(PlayChoice.PAPER, new ChoiceData("PAPER", R.drawable.paper, R.string.game_lose_paper, R.string.game_win_paper));
		mChoiceMap.put(PlayChoice.SCISSORS, new ChoiceData("SCISSORS", R.drawable.scissors, R.string.game_lose_scissors, R.string.game_win_scissors));
		
		mStatsDbAdapter = new StatsDbAdapter(this);
		mStatsDbAdapter.open();
		mGameId = mStatsDbAdapter.createGame("C", wins_needed);
	}
	
	@Override
    public void onStart() {
    	Log.d("DSN Debug", "Calling onStart");
    	super.onStart();
    	
    	if(opponent_choice == Opponent.FRIEND){
    		// This is a bluetooth game. Make sure the bluetooth service is turned on
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        } else {
	        	// Bluetooth is on. Setup the game if it hasn't been already
	            if (mGameService == null) setupGame();
	        }
    	}
    }
    
    @Override
    public synchronized void onResume() {
    	Log.d("DSN Debug", "Calling onResume");
    	super.onResume();
    	
    	// Performing this check in onResume() covers the case in which Bluetooth was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    	// Make sure the game service is set up first.
        if (mGameService != null) {
            // If the state of the game service is STATE_NONE then it needs to be started. Only
        	// the host device can start the game server
            if (mGameService.getState() == BluetoothGameService.STATE_NONE && bluetooth_role.equals("host")) {
              mGameService.start();
            }
        }
    }
    
    protected void setupGame() {
    	Log.d("DSN Debug", "Calling setupGame");
    	
    	if(opponent_choice == Opponent.FRIEND){ 
			// This is a bluetooth game. Initialize a BluetoothGameService to handle
    		// communication between other bluetooth devices
	        mGameService = new BluetoothGameService(this, mHandler, player_name);
	        
	        // Only the host device can start the game server
	        if(bluetooth_role.equals("host")) {
	        	mGameService.start();
	        }
		}
    }
    
    @Override
    public void onStop() {
    	Log.d("DSN Debug", "Calling onStop");
    	super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	Log.d("DSN Debug", "Calling onDestroy");
        super.onDestroy();
        
        // Stop the game service and release its resources
        if (mGameService != null) {
        	mGameService.stop();
        	mGameService = null;
        }
        // Unregister any BroadcastReceivers that are still listening
        try{
        	unregisterReceiver(mReceiver);
        }catch(Exception e) {
        	Log.d("DSN Debug", "Could not unregister the Broadcast receiver: " + e.getMessage());
        }
    }
    
    /**
     * Builds alert dialogs when they are requested
     * @param id The id of the dialog to build
     */
    protected Dialog onCreateDialog(int id) {
    	Dialog dialog;
    	switch(id) {
    	// This dialog asks the player if he/she wants to play another game or return to the main menu
    	case DIALOG_REPLAY:
    		// Set the dialog's title, options, and OnClickListener
    		final String[] play_again_items = {"Start A New Match", "Return to Main Menu"};
    		AlertDialog.Builder play_again_builder = new AlertDialog.Builder(this);
    		play_again_builder.setTitle("Rematch?");
    		play_again_builder.setItems(play_again_items, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				String replayString;
    				if(play_again_items[item].equals("Start A New Match")) {
    					// The player chose to play again
    					if(opponent_choice == Opponent.FRIEND){
    						// This is a bluetooth game. Send a ready message to the opponent's device and
    						// dismiss this dialog
	    					player_will_replay = true;
	    					replayString = "Replay:agree";
	    					mGameService.write(replayString.getBytes());
	    					replay_message_sent = true;
	    					dialog.dismiss();
	    					// If the opponent's continue decision has been received then reset the game
	    					if(replay_message_received) {
	    						if(opponent_will_replay) resetGame();
	    					}else{
	    						// Still waiting on the opponent. Display a waiting dialog until a message
	    						// is received
	    						waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "",
	    								getString(R.string.waiting_for_replay), true, true);
	    					}
    					}else{
    						// This is a single player game. Dismiss this dialog and reset the game
    						dialog.dismiss();
    						resetGame();
    					}
    				}else{
    					// The player chose to return to the main menu
    					if(opponent_choice == Opponent.FRIEND) {
    						// This is a bluetooth game. Send a decline message to the opponent's device,
    						// dismiss this dialog, and end the game
	    					player_will_replay = false;
	    					replayString = "Replay:disagree";
	    					mGameService.write(replayString.getBytes());
	    					replay_message_sent = true;
	    					dialog.dismiss();
	    					showNotification("Disconnecting from opponent's device");
	    					// Delay the endGame() call to ensure the OutputStream isn't overloaded by
	    					// calling write() too fast consecutively
	    					delayHandler.postDelayed(new Runnable(){public void run(){endGame();}}, 500);
    					}else{
    						// This is a single player game. Dismiss this dialog and end the game
    						dialog.dismiss();
    						endGame();
    					}
    				}
    			}
    		});
    		dialog = play_again_builder.create();
    		break;
    	// This dialog ask the host to choose how many games to play the best of
    	case DIALOG_BEST_OF:
    		// Set the dialog's title, options, and OnClickListener
    		final String[] best_of_items = {"1", "3", "5", "7", "9"};
    		AlertDialog.Builder best_of_builder = new AlertDialog.Builder(this);
    		best_of_builder.setTitle("Best of how many games?");
    		best_of_builder.setItems(best_of_items, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				int best_of = Integer.parseInt(best_of_items[item]);
    				// Determine how many wins are needed for a victory and dismiss this dialog
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
    				wins_needed_txt.setText("Wins Needed: " + wins_needed);
    				if(opponent_choice == Opponent.FRIEND) {
    					// This is a bluetooth game. Send a synchronization message to the client
    					// device with game settings
	    				String syncMsg = "Sync:" + wins_needed;
	                	mGameService.write(syncMsg.getBytes());
    				}
                	continueGame();
    			}
    		});
    		dialog = best_of_builder.create();
    		break;
    	default:
    		dialog = null;
    	}
    	return dialog;
    }
    
    //OnDismissListener interface methods
    public void onDismiss(DialogInterface dialog) {
    	Log.d("DSN Debug", "A dialog was dismissed");
    }
    
    //OnCancelListener interface methods
    public void onCancel(DialogInterface dialog) {
    	Log.d("DSN Debug", "A dialog was canceled");
    	if(mGameService != null) mGameService.stop();
    	finish();
    }
    
    /**
     * Ensures that this device is discoverable to other bluetooth devices
     */
    protected void ensureDiscoverable() {
    	// If the device isn't already discoverable then start discoverability for 30 seconds
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(discoverableIntent);
        }
    }
    
    /**
     * Displays a short notification at the bottom of the layout
     * 
     * @param msg - The String to be displayed as a notification
     */
    protected synchronized void showNotification(String msg) {
    	current_toast.cancel();
    	current_toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
    	current_toast.show();
    }

    /**
     * Changes the sub-title at the top of the activity
     * 
     * @param subTitle - String to append to the title
     */
    protected final void setStatus(CharSequence subTitle) {
        setTitle(res.getString(R.string.app_name) + " - " + subTitle);
    }
    
    /**
     * Subclasses must override this to play a sound when a player's choice is submitted
     */
    protected void playSubmitSound() {
    }
    
    // The Handler that gets information back from the BluetoothGameService
    protected final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // This message means the state of the game service has changed
            case MESSAGE_STATE_CHANGE:
            	Log.d("DSN Debug", "Received message: MESSAGE_STATE_CHANGED - " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothGameService.STATE_CONNECTED:
                	unregisterReceiver(mReceiver);
                    break;
                case BluetoothGameService.STATE_CONNECTING:
                    setStatus(getString(R.string.connecting));
                    break;
                case BluetoothGameService.STATE_LISTEN:
                case BluetoothGameService.STATE_NONE:
                    setStatus(getString(R.string.title_not_connected));
                    break;
                }
                break;
                
            // This message means the local connectedThread has sent a message out
            case MESSAGE_WRITE:
                String writeString = (String) msg.obj;
                Log.d("DSN Debug", "Received message: MESSAGE_WRITE - " + writeString);
                String[] writeArray = writeString.split(":");
                String writeTag = writeArray[0];
                if(writeTag.equals("Sync") && !synced) {
                	synced = true;
                }
                break;
                
            // This message means the opponent has sent his/her play choice
            case MESSAGE_PLAY:
                opponent_play_choice = PlayChoice.valueOf((String) msg.obj);
                opponent_choice_made = true;
                // If the player has made a choice then the outcome is ready to be calculated,
                // otherwise just set the opponent image to waiting and wait for player input
                if(choice_made) {
                	playSubmitSound();
					delayHandler.postDelayed(runMatch, 500);
                }else{
                	opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank_waiting));
                	if(choice_made) Log.d("DSN Debug", "choice_made did not set true in time");
                }
                break;
                
            // This message means the host is synchronizing game settings with this device
            case MESSAGE_SYNC:
            	if(!synced) {
                	wins_needed = Integer.parseInt((String) msg.obj);
                	wins_needed_txt.setText("Wins Needed: " + wins_needed);
                	synced = true;
                	// If this is a reset game there will be a waiting dialog that needs to be
                	// dismissed
                	if(waiting_dialog != null) {
                		waiting_dialog.dismiss();
                	}
                	continueGame();
            	}
            	break;
            	
            // This message means the opponent has decided whether or not to play again
            case MESSAGE_REPLAY:
            	replay_message_received = true;
            	if(msg.obj.equals("agree")) {
            		// The opponent wishes to play again. Check if the player has submitted
            		// his/her choice
            		opponent_will_replay = true;
            		if(replay_message_sent) {
            			if(player_will_replay) {
            				// Everyone wants to play again. Dismiss waiting dialogs and reset
            				// the game variables
            				if(waiting_dialog != null) {
            					waiting_dialog.dismiss();
            				}
            				resetGame();
            			}
            		}
            	}else{
            		// The opponent does not want to play again. Remove the player's replay
            		// dialog and end the game.
            		opponent_will_replay = false;
            		removeDialog(DIALOG_REPLAY);
            		Log.d("DSN Debug", "Your opponent has left the game");
            		showNotification(getString(R.string.replay_declined));
            		endGame();
            	}
                break;
                
            // This message means this device has successfully connected to another device via bluetooth
            case MESSAGE_CONNECT_SUCCESSFUL:
            	Log.d("DSN Debug", "Received message: MESSAGE_CONNECT_SUCCESSFUL");
                // Save the connected device's name and address and update the activities status
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                setStatus(getString(R.string.title_bluetooth_connected) + " " + mConnectedDeviceName);
                // Dismiss any waiting dialogs that are still up
                if(waiting_dialog != null) waiting_dialog.dismiss();
                if(connecting_dialog != null) connecting_dialog.dismiss();
                // If this is the host device then send a synchronization message (wins needed) to the
                // client device if they are not already synced
                if(bluetooth_role.equals("host") && !synced) {
                	String syncMsg = "Sync:" + wins_needed;
                	mGameService.write(syncMsg.getBytes());
                }
                showNotification("Connected to " + mConnectedDeviceName + ": "
                		+ mConnectedDeviceAddress);
                break;
                
            // This message means that the device failed to connect to a host
            case MESSAGE_CONNECT_FAILED:
            	Log.d("DSN Debug", "Received message: MESSAGE_CONNECT_FAILED");
            	showNotification((String) msg.obj);
            	// Dismiss the connecting dialog and end the activity
            	if(connecting_dialog != null) {
            		connecting_dialog.dismiss();
            	}
            	finish();
            	break;
            	
            // This message means the opponent's device has disconnected unexpectedly
            case MESSAGE_DISCONNECTED:
            	Log.d("DSN Debug", "Received message: MESSAGE_DISCONNECTED");
            	showNotification((String) msg.obj);
            	// End the game
            	finish();
            	break;
            	
            // This message means that this device has disconnected gracefully and is ready to end the activity
            case MESSAGE_READY_FOR_DISCONNECT:
            	Log.d("DSN Debug", "Received message: MESSAGE_READY_FOR_DISCONNECT");
            	delayHandler.postDelayed(new Runnable(){public void run(){finish();}}, 1000);
            	break;
            }
        }
    };
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        // The bluetooth device selector dialog has returned
        case REQUEST_CONNECT_DEVICE:
        	// If a device was selected try to connect to it
            if (resultCode == Activity.RESULT_OK) {
            	if(data.getBooleanExtra(DEVICE_PAIRED, false)) {
            		connecting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.connecting), true, true);
            	}else{
            		// The host device is not paired. Add help text to the connecting dialog for accepting a pair
            		connecting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.connecting_unpaired), true, true);
            	}
                connectDevice(data);
            }else{ // No device was selected so return to the main menu
            	Log.d("DSN Debug", "REQUEST_CONNECT_DEVICE failed");
            	finish();
            }
            break;
        // The request for bluetooth activation has returned
        case REQUEST_ENABLE_BT:
        	// If bluetooth was turned on set up the game
            if (resultCode == Activity.RESULT_OK) {
                setupGame();
                // If this is a client device bring up an available devices list
                if(bluetooth_role.equals("client")) {
	            	Intent connectIntent = new Intent(this, DeviceListActivity.class);
	            	startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
	            }else{ // This device is a host so wait for a client to try and connect
	            	waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.waiting_for_bluetooth), true, true);
	            	waiting_dialog.setOnCancelListener(this);
	            	waiting_dialog.setOnDismissListener(this);
	            }
            } else {
                // The player did not enable Bluetooth or an error occurred. Return to the main menu
                showNotification("Bluetooth must be activated to play with a friend. Ending game...");
                finish();
            }
        }
    }
    
    /**
     * This method uses device data from an intent to connect to a remote bluetooth host
     * @param hostData An intent with the host device's information attached
     */
    protected void connectDevice(Intent hostData) {
        // Get the device MAC address
        String address = hostData.getStringExtra(DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mGameService.connect(device);
    }
    
    /**
     * Determines the player's and the opponent's play choice, plays a
     * sound effect, and then calls a Runnable after a delay to calculate 
     * the outcome.
     * @param v The ImageView that was selected by the player
     */
    protected void submitChoice(View v) {
		ImageView img = (ImageView) v;
		player_play_choice = PlayChoice.valueOf(img.getTag().toString());
		// Set the player's image as the image that was chosen
		player_img.setImageDrawable(img.getDrawable());
		// If the opponent has not submitted a choice make sure the opponent's image is blank
		if(!opponent_choice_made) {
			opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
		}
		
		switch(opponent_choice) {
		// This is a bluetooth game. Make sure the devices are connected and send a message to the opponent
		// device with the player's choice
		case FRIEND:
			if(mGameService != null && mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
				String playMsg = "Play:" + v.getTag().toString();
				mGameService.write(playMsg.getBytes());
				// If the opponent's decision has already been received then play the appropriate sound
				// and calculate the outcome of the match
				if(opponent_choice_made) {
					if(settings.getBoolean("sounds_preference", true)) {
						playSubmitSound();
					}
					delayHandler.postDelayed(runMatch, 500);
				}
			}
			break;
			
		// This is a single player game. Roll a random play choice for the opponent then play the appropriate
		// sound and calculate the outcome of the match
		case COMPUTER:
		default:
			opponent_play_choice = PlayChoice.randomPlayChoice();
			if(settings.getBoolean("sounds_preference", true)) {
				playSubmitSound();
			}
			// Give enough delay to allow the sound effect to play through
			delayHandler.postDelayed(runMatch, 500);
		}
	}
    
    /*
     * A Runnable that calculates the outcome of a Paper Rock Scissors match
     * and checks to see if either player has won enough rounds to be victorious.
     * If there is a winner, then input is frozen and endGame is called.
     */
    protected Runnable runMatch = new Runnable() {
		public void run() {
			String text;
			String result;
			
			// Set the images for each player's play choice
			player_img.setImageDrawable(res.getDrawable(mChoiceMap.get(player_play_choice).mChoiceImage));
			opponent_img.setImageDrawable(res.getDrawable(mChoiceMap.get(opponent_play_choice).mChoiceImage));
			
			if(player_play_choice == opponent_play_choice){ // The match is a draw
				text = res.getString(R.string.game_draw);
				result = "D";
			} else { // The match is not a draw, determine the winner and set the result and Toast text
				if (mOutcomeMap.containsKey(new MatchUp(player_play_choice, opponent_play_choice))) {
					player_win_count++;
					result = "W";
					text = res.getString(mChoiceMap.get(player_play_choice).mWinText);
				} else {
					opponent_win_count++;
					result = "L";
					text = res.getString(mChoiceMap.get(player_play_choice).mLoseText);
				}
			}
			// Update the statistics database
			mStatsDbAdapter.insertRound((int)mGameId, mChoiceMap.get(player_play_choice).mChoiceString, 
					mChoiceMap.get(opponent_play_choice).mChoiceString, result);
			
			// Update the win-count text on the screen
			opponent_win_count_txt.setText(Integer.toString(opponent_win_count));
			player_win_count_txt.setText(Integer.toString(player_win_count));
			
			// Reset each player's choice_made boolean
			choice_made = false;
			opponent_choice_made = false;
			
			// If the game is not finished then call continueGame() to reset variables for the next round
			if(opponent_win_count != wins_needed && player_win_count != wins_needed) {
				showNotification(text);
				continueGame();
			}else{
				// The game is over. Determine the winner and play the appropriate sounds. Then display
				// a play again dialog to the user
				if(opponent_win_count == wins_needed) { // Opponent wins
					if(settings.getBoolean("sounds_preference", true)) {
						SoundEffectManager.playSound(3, 1f);
					}
					showNotification(getString(R.string.game_lose));
				}else{ // Player wins
					if(settings.getBoolean("sounds_preference", true)) {
						SoundEffectManager.playSound(2, 1f);
					}
					showNotification(getString(R.string.game_win));
				}
				showDialog(DIALOG_REPLAY);
			}
		}
	};
	
	/**
	 * Resets variables when a game is not finished and must continue to the next round
	 */
	protected void continueGame() {
	}
	
	/**
	 * Resets variables to their initial values and restarts the game
	 * 
	 * @param messageResource
	 * @param soundIndex
	 */
	protected void resetGame() {
		Log.d("DSN Debug", "Resetting the game");
		// Reset the win counters and images for each player
		player_win_count = 0;
		player_win_count_txt.setText("0");
		player_img.setImageDrawable(res.getDrawable(R.drawable.blank));
		opponent_win_count = 0;
		opponent_win_count_txt.setText("0");
		opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
		// Reset message booleans
		synced = false;
		opponent_ready_to_disconnect = false;
		player_ready_to_disconnect = false;
		replay_message_received = false;
		replay_message_sent = false;
		opponent_will_replay = false;
		player_will_replay = false;
		// If this is a bluetooth game then only the host will be able to choose game settings
		if(opponent_choice == Opponent.FRIEND) {
			if(bluetooth_role.equals("host")) {
				showDialog(DIALOG_BEST_OF);
			}else{ // Display a waiting dialog for client devices until the host has chosen settings
				waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "",
						getString(R.string.waiting_for_reset), true, true);
			}
		}else{
			// This is a single player game. Display a dialog for the next match's settings
			showDialog(DIALOG_BEST_OF);
		}
	}
	
	/**
	 * Disables buttons so that they cannot be clicked and starts the end-game sequence.
	 */
	protected void endGame() {
		// If this is a bluetooth game and this is the host device, send a disconnect message to the
		// client device to initiate a graceful disconnect
		if(mGameService != null) {
			if(mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
				if(bluetooth_role.equals("host")) {
					Log.d("DSN Debug", "Ending game from endGame method");
					String disconnectString = "Disconnect:initiate";
					mGameService.write(disconnectString.getBytes());
				}
			}
    	}else{
    		// This is a single player game. To clean up finish the activity
    		finish();
    	}
	}
	
	// The BroadcastReceiver that listens for this device's discoverability to end. This BroadcastReceiver
	// is only registered until a remote device successfully connects to this device
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	
        	// This device's bluetooth scan mode has changed
        	if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	        	int prev_scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE,
	        			BluetoothAdapter.SCAN_MODE_NONE);
	        	switch(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE)) {
	        	// Bluetooth is turned on but discoverability has ended
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
	        	case BluetoothAdapter.SCAN_MODE_NONE:
	        		// When this device has ended discoverability and no connection has been made, return
	        		// to the title screen.
	        		if(prev_scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	        			showNotification("No devices tried to connect");
	        			finish();
	        		}
	        		break;
	        	default:
	        		break;
	        	}
        	}
        }
    };
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }
}