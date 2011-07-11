package com.devspacenine.rockpaperscissors;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class RockPaperScissorsGame extends Activity implements OnDismissListener, OnCancelListener {
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_CONNECT_SUCCESSFUL = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECT_FAILED = 6;
    public static final int MESSAGE_DISCONNECTED = 7;
    public static final int MESSAGE_READY_FOR_DISCONNECT = 8;
    
    // Dialog ids for the onCreateDialog method
    public static final int DIALOG_WAITING_FOR_CONNECTION = 1;
    
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
		setContentView(R.layout.game);
		
		// Initialize variables
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		player_name = settings.getString("name_preference", "");
		sender_intent = getIntent();
		opponent_choice = Opponent.valueOf(sender_intent.getStringExtra("opponent_choice"));
		res = getResources();            	
		ctx = getApplicationContext();
		opponent_img = (ImageView) findViewById(R.id.opponent_choice);
		player_img = (ImageView) findViewById(R.id.player_choice);
		player_win_count_txt = (TextView) findViewById(R.id.player_win_count);
		opponent_win_count_txt = (TextView) findViewById(R.id.opponent_win_count);
		wins_needed = sender_intent.getIntExtra("wins_needed", 1);
		wins_needed_txt = (TextView) findViewById(R.id.wins_needed);
		wins_needed_txt.setText("Wins Needed: " + wins_needed);
		choice_made = false;
		opponent_choice_made = false;
		synced = false;
		opponent_ready_to_disconnect = false;
		player_ready_to_disconnect = false;
		current_toast = Toast.makeText(ctx, "Good Luck!", Toast.LENGTH_SHORT);
		
		// Register for broadcasts when this device ends discoverability
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mReceiver, filter);
		
		// Check if bluetooth is required
		if(opponent_choice == Opponent.FRIEND) {
			// Get local Bluetooth adapter
			bluetooth_role = sender_intent.getStringExtra("bluetooth_role");
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "Bluetooth is not supported on your device. Ending game...", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }else{
	        	// If BT is not on, request that it be enabled.
	        	if (!mBluetoothAdapter.isEnabled()) {
	                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	            }else{
		            if(bluetooth_role.equals("join")) {
		            	Log.d("DSN Debug", "This is the join device");
		            	// Launch the DeviceListActivity to see devices and do scan
		            	Intent connectIntent = new Intent(this, DeviceListActivity.class);
		            	startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
		            }else{
		            	Log.d("DSN Debug", "This is the create device");
		            	waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.waiting_for_bluetooth), true, true);
		            	waiting_dialog.setOnCancelListener(this);
		            	waiting_dialog.setOnDismissListener(this);
		            }
	            }
	        }
		}
		current_toast.show();
		
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
    	
    	// If BT is not on, request that it be enabled.
        // setupGame() will then be called during onActivityResult
    	if(opponent_choice == Opponent.FRIEND){ 
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        // Otherwise, setup the chat session
	        } else {
	            if (mGameService == null) setupGame();
	        }
    	}else{
    		setupGame();
    	}
    }
    
    @Override
    public synchronized void onResume() {
    	Log.d("DSN Debug", "Calling onResume");
    	super.onResume();
    	
    	// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mGameService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mGameService.getState() == BluetoothGameService.STATE_NONE && bluetooth_role.equals("create")) {
              // Start the Bluetooth game services
              mGameService.start();
            }
        }
    }
    
    protected void setupGame() {
    	Log.d("DSN Debug", "Calling setupGame");
    	if(opponent_choice == Opponent.FRIEND){ 
			// Initialize the BluetoothGameService to perform bluetooth connections
	        mGameService = new BluetoothGameService(this, mHandler, player_name);
	        
	        if(bluetooth_role.equals("create")) {
	        	mGameService.start();
	        }
		}
    }
    
    @Override
    public void onStop() {
    	Log.d("DSN Debug", "Calling onStop");
    	super.onStop();
    	finish();
    }
    
    @Override
    public void onDestroy() {
    	Log.d("DSN Debug", "Calling onDestroy");
        super.onDestroy();
        // Stop the Bluetooth game services
        if (mGameService != null) {
        	mGameService.stop();
        	mGameService = null;
        }
        try{
        	unregisterReceiver(mReceiver);
        }catch(Exception e) {
        	Log.d("DSN Debug", "Could not unregister the Broadcast receiver: " + e.getMessage());
        }
    }
    
    //OnDismissListener interface methods
    public void onDismiss(DialogInterface dialog) {
    	
    }
    
    //OnCancelListener interface methods
    public void onCancel(DialogInterface dialog) {
    	if(mGameService != null) mGameService.stop();
    	finish();
    }
    
    /**
     * Ensures that this device is discoverable to other bluetooth devices
     */
    protected void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Changes the sub-title at the top of the activity
     * 
     * @param subTitle - String to append to the title
     */
    protected final void setStatus(CharSequence subTitle) {
        setTitle(res.getString(R.string.app_name) + " - " + subTitle);
    }
    
    protected void playSubmitSound() {
    }
    
 // The Handler that gets information back from the BluetoothGameService
    protected final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
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
            	Log.d("DSN Debug", "Received message: MESSAGE_WRITE");
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                if(bluetooth_role.equals("create") && !synced) {
                	synced = true;
                }
                break;
            // This message means the local connectedThread has received a message from a remote device
            case MESSAGE_READ:
            	Log.d("DSN Debug", "Received message: MESSAGE_READ");
                // Split the message string into its pieces
                String readString = (String) msg.obj;
                String[] readArray = readString.split(":");
                String msgTag = readArray[0];
                String msgValue = readArray[1];
                // This is a play choice message
                if(msgTag.equals("Play")) {
	                opponent_play_choice = PlayChoice.valueOf(msgValue);
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
                } // This is a game synchronization message
                else if(msgTag.equals("Sync")) {
                	if(!synced) {
	                	wins_needed = Integer.parseInt(msgValue);
	                	wins_needed_txt.setText("Wins Needed: " + wins_needed);
	                	synced = true;
                	}
                }
                break;
            case MESSAGE_CONNECT_SUCCESSFUL:
            	Log.d("DSN Debug", "Received message: MESSAGE_CONNECT_SUCCESSFUL");
                // save the connected device's name and update the status
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                setStatus(getString(R.string.title_bluetooth_connected) + " " + mConnectedDeviceName);
                
                // save the device's address
                mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
                if(waiting_dialog != null) {
                	waiting_dialog.dismiss();
                }
                if(connecting_dialog != null) {
                	connecting_dialog.dismiss();
                }
                if(bluetooth_role.equals("create") && !synced) {
                	String syncMsg = "Sync:" + wins_needed;
                	mGameService.write(syncMsg.getBytes());
                }
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName + ": " + mConnectedDeviceAddress, Toast.LENGTH_SHORT).show();
                break;
            // A message handler for notifying the user
            case MESSAGE_TOAST:
            	Log.d("DSN Debug", "Received message: MESSAGE_TOAST");
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_CONNECT_FAILED:
            	Log.d("DSN Debug", "Received message: MESSAGE_CONNECT_FAILED");
            	Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
            	if(connecting_dialog != null) {
            		connecting_dialog.dismiss();
            	}
            	finish();
            	break;
            case MESSAGE_DISCONNECTED:
            	Log.d("DSN Debug", "Received message: MESSAGE_DISCONNECTED");
            	Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
            	finish();
            	break;
            case MESSAGE_READY_FOR_DISCONNECT:
            	Log.d("DSN Debug", "Received message: MESSAGE_READY_FOR_DISCONNECT");
            	delayHandler.postDelayed(new Runnable(){public void run(){finish();}}, 3000);
            	break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            	if(data.getBooleanExtra(DEVICE_PAIRED, false)) {
            		connecting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.connecting), true, true);
            	}else{
            		connecting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.connecting_unpaired), true, true);
            	}
                connectDevice(data);
            }else{
            	Log.d("DSN Debug", "REQUEST_CONNECT_DEVICE failed");
            	finish();
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupGame();
                if(bluetooth_role.equals("join")) {
	            	// Launch the DeviceListActivity to see devices and do scan
	            	Intent connectIntent = new Intent(this, DeviceListActivity.class);
	            	startActivityForResult(connectIntent, REQUEST_CONNECT_DEVICE);
	            }else{
	            	waiting_dialog = ProgressDialog.show(RockPaperScissorsGame.this, "", getString(R.string.waiting_for_bluetooth), true, true);
	            	waiting_dialog.setOnCancelListener(this);
	            	waiting_dialog.setOnDismissListener(this);
	            }
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, "Bluetooth must be activated to play with a friend. Ending game...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    protected void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getStringExtra(DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mGameService.connect(device);
    }
    
    /*
     * Determines the player's and the opponent's play choice, plays a
     * sound effect, and then calls a Runnable after a delay to calculate 
     * the outcome.
     */
    protected void submitChoice(View v) {
		ImageView img = (ImageView) v;
		player_play_choice = PlayChoice.valueOf(v.getTag().toString());
		player_img.setImageDrawable(img.getDrawable());
		if(!opponent_choice_made) {
			opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
		}
		switch(opponent_choice) {
		case FRIEND:
			if(mGameService != null && mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
				String playMsg = "Play:" + v.getTag().toString();
				mGameService.write(playMsg.getBytes());
				if(opponent_choice_made) {
					if(settings.getBoolean("sounds_preference", true)) {
						playSubmitSound();
					}
					delayHandler.postDelayed(runMatch, 500);
				}
			}
			break;
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
			
			// Set the images for each players play choice
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
			
			// Check if the game is finished
			if(opponent_win_count != wins_needed && player_win_count != wins_needed) { // The game is not finished
				current_toast.cancel();
				current_toast = Toast.makeText(ctx, text, Toast.LENGTH_SHORT);
				current_toast.show();
				continueGame();
			}else if(opponent_win_count == wins_needed) { // Game over, opponent wins
				endGame(R.string.game_lose, 3);
			}else if(player_win_count == wins_needed) { // Game over, player wins
				endGame(R.string.game_win, 2);
			}
		}
	};
	
	/**
	 * Resets variables when a game is not finished and must continue to the next round
	 */
	protected void continueGame() {
	}
	
	/*
	 * Disables buttons so that they cannot be clicked and starts the end game sequence.
	 */
	protected void endGame(int messageResource, int soundIndex) {
		if(settings.getBoolean("sounds_preference", true)) {
			SoundEffectManager.playSound(soundIndex, 1f);
		}
		current_toast.cancel();
		current_toast = Toast.makeText(ctx, messageResource, Toast.LENGTH_SHORT);
		current_toast.show();
		if(mGameService != null) {
			if(mGameService.getState() == BluetoothGameService.STATE_CONNECTED) {
				if(bluetooth_role.equals("create")) {
					Log.d("DSN Debug", "Ending game from endGame method");
					String disconnectString = "Disconnect:initiate";
					mGameService.write(disconnectString.getBytes());
				}
			}
    	}else{
    		delayHandler.postDelayed(new Runnable(){public void run(){finish();}}, 3000);
    	}
	}
	
	// The BroadcastReceiver that listens for this device's discoverability to end
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	        	int prev_scan_mode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE);
	        	
	        	switch(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_NONE)) {
	        	case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
	        	case BluetoothAdapter.SCAN_MODE_NONE:
	        		// When this device has ended discoverability and no connection has been made, return
	        		// to the title screen.
	        		if(prev_scan_mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
	        			Toast.makeText(ctx, "No devices tried to connect", Toast.LENGTH_SHORT).show();
	        			finish();
	        		}
	        		break;
	        	default:
	        		break;
	        	}
        	}
        }
    };
}