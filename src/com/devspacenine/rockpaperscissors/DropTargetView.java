package com.devspacenine.rockpaperscissors;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class DropTargetView extends ImageView implements DropTarget
{
	/**
     * Used to create a new DropTargetView from code.
     *
     * @param context The application's context.
     */
	public DropTargetView(Context context) {
		super(context);
	}
	
	/**
     * Used to create a new DropTargetView from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
	public DropTargetView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
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
	    ImageView v = (ImageView) dragInfo;
	    setImageDrawable(v.getDrawable());
	}
	
	/*
	 * Handle a dragged object entering the DropTarget.
	 */
	public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	/*
	 * Handle an object being dragged over the DropTarget.
	 */
	public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
	        DragView dragView, Object dragInfo)
	{
	}
	
	/*
	 * Handle a dragged object exiting the DropTarget.
	 */
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
}