package com.devspacenine.rockpaperscissors;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public class ButtonGame extends RockPaperScissorsGame implements View.OnTouchListener, View.OnClickListener {
	
	private ImageView rock_button;
	private ImageView paper_button;
	private ImageView scissors_button;
	
	/** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		((ViewStub) findViewById(R.id.game_images)).inflate();
		rock_button = (ImageView) findViewById(R.id.rock_img);
		rock_button.setOnTouchListener(this);
		rock_button.setOnClickListener(this);
		paper_button = (ImageView) findViewById(R.id.paper_img);
		paper_button.setOnTouchListener(this);
		paper_button.setOnClickListener(this);
		scissors_button = (ImageView) findViewById(R.id.scissors_img);
		scissors_button.setOnTouchListener(this);
		scissors_button.setOnClickListener(this);
	}
    
    @Override
    public void playSubmitSound() {
    	SoundEffectManager.playSound(1, 1f);
    }
    
    // View.OnTouchListener interface methods
    public boolean onTouch(View v, MotionEvent event) {
    	int action = event.getAction();
    	ImageView img = (ImageView) v;
    	if(img.isClickable()) {
	    	switch(action) {
	    	case MotionEvent.ACTION_DOWN:
	    		switch(PlayChoice.valueOf(v.getTag().toString())) {
	    		case ROCK:
	    			img.setImageDrawable(res.getDrawable(R.drawable.rock_pressed));
	    			break;
	    		case PAPER:
	    			img.setImageDrawable(res.getDrawable(R.drawable.paper_pressed));
	    			break;
	    		case SCISSORS:
	    			img.setImageDrawable(res.getDrawable(R.drawable.scissors_pressed));
	    			break;
	    		}
	    		break;
	    	case MotionEvent.ACTION_UP:
	    		switch(PlayChoice.valueOf(v.getTag().toString())) {
	    		case ROCK:
	    			img.setImageDrawable(res.getDrawable(R.drawable.rock));
	    			break;
	    		case PAPER:
	    			img.setImageDrawable(res.getDrawable(R.drawable.paper));
	    			break;
	    		case SCISSORS:
	    			img.setImageDrawable(res.getDrawable(R.drawable.scissors));
	    			break;
	    		}
	    		break;
	    	}
    	}
    	return false;
    }
    
    //View.OnClickListener interface methods
    public void onClick(View v) {
    	rock_button.setClickable(false);
		paper_button.setClickable(false);
		scissors_button.setClickable(false);
		choice_made = true;
    	submitChoice(v);
    }
	
	/*
	 * Disables buttons so that they cannot be clicked and starts the end game sequence.
	 */
	@Override
	public void continueGame() {
		rock_button.setClickable(true);
		paper_button.setClickable(true);
		scissors_button.setClickable(true);
	}
}
