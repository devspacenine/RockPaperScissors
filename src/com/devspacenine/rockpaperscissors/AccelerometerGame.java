package com.devspacenine.rockpaperscissors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.ViewStub;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class AccelerometerGame extends RockPaperScissorsGame implements SensorEventListener, RadioGroup.OnCheckedChangeListener {
	private enum ShootCount {
		ONETWOSHOOT,
		ONETWOTHREESHOOT;
	}
	
	private boolean select_checked = false;
	private int shake_count = 0;
	private int max_shake_count;
	private float magnitude;
	private RadioGroup choice_group;
	private RadioButton rock_select;
	private RadioButton paper_select;
	private RadioButton scissors_select;
	private RadioButton empty_select;
	private RadioButton chosen_select;
	private TextView action_text;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Vibrator mVibrator;
	
	// Acceleration detection variables
	private long now = 0;
	private long timeDiff = 0;
	private long lastShake = 0;
	
	/** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	((ViewStub) findViewById(R.id.game_radio_images)).inflate();
    	mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    	mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    	mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		choice_group = (RadioGroup) findViewById(R.id.choice_group);
		choice_group.setOnCheckedChangeListener(this);
		rock_select = (RadioButton) findViewById(R.id.rock_select);
		paper_select = (RadioButton) findViewById(R.id.paper_select);
		scissors_select = (RadioButton) findViewById(R.id.scissors_select);
		empty_select = (RadioButton) findViewById(R.id.empty_select);
		chosen_select = empty_select;
		switch(ShootCount.valueOf(settings.getString("accelerometer_count_preference", "ONETWOSHOOT"))) {
		case ONETWOSHOOT:
			max_shake_count = 3;
			break;
		case ONETWOTHREESHOOT:
			max_shake_count = 4;
			break;
		}
		if(max_shake_count == 4) {
			action_text = (TextView) findViewById(R.id.accelerometer_action);
			action_text.setText(R.string.accelerometer_action_two);
		}
	}
    
    @Override
    public synchronized void onResume() {
    	super.onResume();
        
        if(select_checked) {
    		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    	}
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if(select_checked) {
    		mSensorManager.unregisterListener(this);
    	}
    }
    
    @Override
    public void playSubmitSound() {
    }
    
	/*
	 * Disables buttons so that they cannot be clicked and starts the end game sequence.
	 */
    @Override
	public void endGame() {
		rock_select.setClickable(false);
		paper_select.setClickable(false);
		scissors_select.setClickable(false);
		
		super.endGame();
	}
    
    // SensorEventListener interface methods
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    
    public void onSensorChanged(SensorEvent event) {
		magnitude = (float) Math.sqrt((event.values[0]*event.values[0])+(event.values[1]*event.values[1])+(event.values[2]*event.values[2]));
		magnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
		
		now = event.timestamp;
		if(lastShake == 0) {
			lastShake = now;
		}else{
			timeDiff = now - lastShake;
			if(timeDiff > 300000000 && magnitude > 14) {
				if(shake_count == 0) {
					opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
					
				}
				if(settings.getBoolean("sounds_preference", true)) {
					SoundEffectManager.playSound(4, 1f);
				}
				mVibrator.vibrate(100);
				shake_count = shake_count + 1;
				lastShake = now;
			}
		}
		if(shake_count == max_shake_count) {
			shake_count = 0;
			mSensorManager.unregisterListener(this);
			choice_made = true;
			submitChoice(player_img);
		}
    }
    
    // RadioGroup.OnCheckedChangeListener interface methods
    public void onCheckedChanged(RadioGroup group, int checkedId) {
    	chosen_select = (RadioButton) findViewById(checkedId);
    	if(!opponent_choice_made) {
    		opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
    	}
    	switch(PlayChoice.valueOf(chosen_select.getTag().toString())) {
		case ROCK:
			player_img.setImageDrawable(res.getDrawable(R.drawable.rock));
			player_img.setTag("ROCK");
			break;
		case PAPER:
			player_img.setImageDrawable(res.getDrawable(R.drawable.paper));
			player_img.setTag("PAPER");
			break;
		case SCISSORS:
		default:
			player_img.setImageDrawable(res.getDrawable(R.drawable.scissors));
			player_img.setTag("SCISSORS");
    	}
    	if(!select_checked) {
    		select_checked = true;
    		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    	}
    }
    
    @Override
    protected void resetGame() {
    	rock_select.setChecked(false);
    	paper_select.setChecked(false);
    	scissors_select.setChecked(false);
    	chosen_select = empty_select;
    	select_checked = false;
    	mSensorManager.unregisterListener(this);
    	super.resetGame();
    }
    
    @Override
    protected void continueGame() {
    	super.continueGame();
    	rock_select.setClickable(true);
		paper_select.setClickable(true);
		scissors_select.setClickable(true);
		if(select_checked) {
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		}
    }
}