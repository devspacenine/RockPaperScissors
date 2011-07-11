package com.devspacenine.rockpaperscissors;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public class DragAndDropGame extends RockPaperScissorsGame implements View.OnTouchListener, DragController.DragListener {
	private DragController mDragController;
	private DragLayer mDragLayer;
	private ImageView rock_img;
	private ImageView paper_img;
	private ImageView scissors_img;
	
	/** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDragController = new DragController(this);
		mDragController.setDragListener(this);
		((ViewStub) findViewById(R.id.game_images)).inflate();
		setupViews();
	}
    
    @Override
    public void playSubmitSound() {
    	SoundEffectManager.playSound(1, 1f);
    }
	
	//View.OnTouchListener interface methods
	/*
	 * Starts the drag event in DragController when a draggable view is clicked.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		if(action == MotionEvent.ACTION_DOWN && v.isClickable()) {
			// Let the DragController initiate a drag-drop sequence.
			// Use the dragInfo to pass along the object being dragged.
			player_img.setImageDrawable(res.getDrawable(R.drawable.blank));
			opponent_img.setImageDrawable(res.getDrawable(R.drawable.blank));
			Object dragInfo = v;
			mDragController.startDrag(v, mDragLayer, dragInfo, DragController.DRAG_ACTION_MOVE);
			return true;
		}else
			return false;
	}
	
	/*
	 * Assigns a DragController to the outer DragLayer, registers Views that can accept
	 * a drop event with the DragController, and sets the onTouchListeners for
	 * draggable Views.
	 */
	private void setupViews() {
		DragController dragController = mDragController;
		mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
		mDragLayer.setDragController(dragController);
	    dragController.addDropTarget((DropTargetView) findViewById(R.id.player_choice));

	    rock_img = (ImageView) findViewById(R.id.rock_img);
	    rock_img.setClickable(true);
	    paper_img = (ImageView) findViewById(R.id.paper_img);
	    paper_img.setClickable(true);
	    scissors_img = (ImageView) findViewById(R.id.scissors_img);
	    scissors_img.setClickable(true);
	    
	    rock_img.setOnTouchListener(this);
	    paper_img.setOnTouchListener(this);
	    scissors_img.setOnTouchListener(this);
	}
	
	// DragController.DragListener interface methods
	/**
     * A drag has begun
     * 
     * @param source An object representing where the drag originated
     * @param info The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link DragController#DRAG_ACTION_MOVE}
     *        or {@link DragController#DRAG_ACTION_COPY}
     */
	public void onDragStart(DragSource source, Object info, int dragAction) {
		player_img.setImageDrawable(res.getDrawable(R.drawable.highlight));
	}
	
	/**
     * The drag has ended
     */
	public void onDragEnd() {
	}
	
	/*
	 * There was a successful drag and drop
	 */
	public void onSuccessfulDrop(View v) {
		rock_img.setClickable(false);
		paper_img.setClickable(false);
		scissors_img.setClickable(false);
		choice_made = true;
		submitChoice(v);
	}
	
	/*
	 * There was an unsuccessful drag and drop
	 */
	public void onUnsuccessfulDrop(View v) {
		player_img.setImageDrawable(res.getDrawable(R.drawable.blank));
	}
	
	/*
	 * Disables buttons so that they cannot be clicked and starts the end game sequence.
	 */
	@Override
	public void continueGame() {
		rock_img.setClickable(true);
		paper_img.setClickable(true);
		scissors_img.setClickable(true);
	}
}