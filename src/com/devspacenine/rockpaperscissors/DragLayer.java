package com.devspacenine.rockpaperscissors;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A ViewGroup that coordinates dragging across its descendants.
 *
 * <p> This class used DragLayer in the Android Launcher activity as a model.
 * It is a bit different in several respects:
 * (1) It extends MyAbsoluteLayout rather than FrameLayout; (2) it implements DragSource and DropTarget methods
 * that were done in a separate Workspace class in the Launcher.
 */
public class DragLayer extends FrameLayout implements DragSource, DropTarget
{
    private DragController mDragController;

    /**
     * Used to create a new DragLayer from code.
     *
     * @param context The application's context.
     */
    public DragLayer(Context context)
    {
    	super(context);
    }
    
    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public DragLayer(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setDragController(DragController controller)
    {
        mDragController = controller;
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
    	if(mDragController != null) {
    		return mDragController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    	}else{
    		return false;
    	}
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
    	if(mDragController != null) {
    		return mDragController.onInterceptTouchEvent(ev);
    	}else{
    		return false;
    	}
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
    	if(mDragController != null) {
    		return mDragController.onTouchEvent(ev);
    	}else{
    		return false;
    	}
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction)
    {
    	if(mDragController != null) {
    		return mDragController.dispatchUnhandledMove(focused, direction);
    	}else{
    		return false;
    	}
    }

    // DragSource interface methods
	/**
	 * onDropCompleted
	 *
	 */
	public void onDropCompleted (View target, boolean success)
	{
	}

	// DropTarget interface implementation
	/**
	 * Handle an object being dropped on the DropTarget.
	 * This is where a dragged view gets repositioned at the end of a drag.
	 * 
	 * @param source DragSource where the drag started
	 * @param x X coordinate of the drop location
	 * @param y Y coordinate of the drop location
	 * @param xOffset Horizontal offset with the object being dragged where the original
	 *          touch happened
	 * @param yOffset Vertical offset with the object being dragged where the original
	 *          touch happened
	 * @param dragView The DragView that's being dragged around on screen.
	 * @param dragInfo Data associated with the object being dragged
	 * 
	 */
	public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	/**
	 * Check if a drop action can occur at, or near, the requested location.
	 * This may be called repeatedly during a drag, so any calls should return
	 * quickly.
	 * 
	 * @param source DragSource where the drag started
	 * @param x X coordinate of the drop location
	 * @param y Y coordinate of the drop location
	 * @param xOffset Horizontal offset with the object being dragged where the
	 *            original touch happened
	 * @param yOffset Vertical offset with the object being dragged where the
	 *            original touch happened
	 * @param dragView The DragView that's being dragged around on screen.
	 * @param dragInfo Data associated with the object being dragged
	 * @return True if the drop will be accepted, false otherwise.
	 */
	public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	    return true;
	}
	
	/**
	 * Estimate the surface area where this object would land if dropped at the
	 * given location.
	 * 
	 * @param source DragSource where the drag started
	 * @param x X coordinate of the drop location
	 * @param y Y coordinate of the drop location
	 * @param xOffset Horizontal offset with the object being dragged where the
	 *            original touch happened
	 * @param yOffset Vertical offset with the object being dragged where the
	 *            original touch happened
	 * @param dragView The DragView that's being dragged around on screen.
	 * @param dragInfo Data associated with the object being dragged
	 * @param recycle {@link Rect} object to be possibly recycled.
	 * @return Estimated area that would be occupied if object was dropped at
	 *         the given location. Should return null if no estimate is found,
	 *         or if this target doesn't provide estimations.
	 */
	public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
	            DragView dragView, Object dragInfo, Rect recycle)
	{
	    return null;
	}
} // end class