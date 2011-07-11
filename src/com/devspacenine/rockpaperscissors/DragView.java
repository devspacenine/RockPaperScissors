package com.devspacenine.rockpaperscissors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * A DragView is a special view used by a DragController. During a drag operation, what is actually moving
 * on the screen is a DragView. A DragView is constructed using a bitmap of the view the user really
 * wants to move.
 *
 */
public class DragView extends View 
{
    // Number of pixels to add to the dragged item for scaling.  Should be even for pixel alignment.
    private static final int DRAG_SCALE = 0;   // In Launcher, value is 40
    private Bitmap mBitmap;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;
    private float mAnimationScale = 1.0f;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;

    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param context A context
     * @param bitmap The view that we're dragging around.  We scale it up when we draw it.
     * @param registrationX The x coordinate of the registration point.
     * @param registrationY The y coordinate of the registration point.
     */
    public DragView(Context context, Bitmap bitmap, int registrationX, int registrationY,
            int left, int top, int width, int height) {
        super(context);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);        
  
        Matrix scale = new Matrix();
        float scaleFactor = width;
        scaleFactor = (scaleFactor + DRAG_SCALE) / scaleFactor;
        scale.setScale(scaleFactor, scaleFactor);
        mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height, scale, true);

        // The point in our scaled bitmap that the touch events are located
        mRegistrationX = registrationX + (DRAG_SCALE / 2);
        mRegistrationY = registrationY + (DRAG_SCALE / 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float scale = mAnimationScale;
        if (scale < 0.999f) { // allow for some float error
            float width = mBitmap.getWidth();
            float offset = (width-(width*scale))/2;
            canvas.translate(offset, offset);
            canvas.scale(scale, scale);
        }
        canvas.drawBitmap(mBitmap, 0.0f, 0.0f, mPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBitmap.recycle();
    }

    public void setPaint(Paint paint) {
        mPaint = paint;
        invalidate();
    }

    public void setScale (float scale) {
        if (scale > 1.0f) mAnimationScale = 1.0f;
        else mAnimationScale = scale;
        invalidate();
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void show(IBinder windowToken, int touchX, int touchY) {
        WindowManager.LayoutParams lp;
        int pixelFormat;

        pixelFormat = PixelFormat.TRANSLUCENT;

        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                touchX-mRegistrationX, touchY-mRegistrationY,
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                pixelFormat);
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.token = windowToken;
        lp.setTitle("DragView");
        mLayoutParams = lp;

        mWindowManager.addView(this, lp);

    }
    
    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    void move(int touchX, int touchY) {
        WindowManager.LayoutParams lp = mLayoutParams;
        lp.x = touchX - mRegistrationX;
        lp.y = touchY - mRegistrationY;
        mWindowManager.updateViewLayout(this, lp);
    }

    void remove() {
        mWindowManager.removeView(this);
    }
}