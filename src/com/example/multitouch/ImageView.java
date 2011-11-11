package com.example.multitouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * This class provides a panning and zooming view of a bitmap. 
 */
public class ImageView extends View implements OnTouchListener {
	
	public static final int MODE_STOP = 0;
	public static final int MODE_PAN = 1; 
	public static final int MODE_ZOOM = 2; 
	public static final int MODE_RUBBER = 3; 
	
	private int mode = MODE_STOP; 
	private Bitmap main; 
	private PointF center, oldpoint = new PointF(); 
	private float dist = 0F; 
	private Rect src;
	private RectF dst, min, max;
	
	private Runnable looper = new Runnable() {
		public void run() {
			switch(mode) {
			case MODE_RUBBER :
				Rubber();
				break; 
			}
			if(mode != MODE_STOP) postDelayed(this, 50);
		}
	};

	public void setImage(String path) {
		setImage(BitmapFactory.decodeFile(path));
	}
	
	public void setImage(Bitmap bitmap) {
		main = bitmap;
		reset();
		requestLayout();
		invalidate();
	}

	public ImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this); 
		setFocusable(true);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (main == null) return; 
		src = new Rect(0, 0, main.getWidth(), main.getHeight());
		max = new RectF(0, 0, main.getWidth(), main.getHeight());
		dst = fitRect(src, getWidth(), getHeight());
		min = new RectF(dst);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (main == null) return; 
		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		canvas.drawBitmap(main, src, dst, paint);
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		if(main == null) return true; 
		int code = event.getAction() & MotionEvent.ACTION_MASK;
		switch (code) {
		case 0 : // pointer 1 down
			mode = MODE_PAN; 
			reset();
			break; 
		case 1 : // pointer 1 up
			mode = MODE_RUBBER; 
			reset();
			post(looper);
			break; 
		case 5 : // pointer 2 down
			mode = MODE_ZOOM; 
			reset();
			break; 
		case 6 : // pointer 2 up
			mode = MODE_PAN; 
			reset();
			break; 
		case 2 : // move
			if (mode == MODE_PAN)
				Pan(event);
			if (mode == MODE_ZOOM)
				Zoom(event);
			break;			
		}
		return true;
	}
	
	/**
	 * Reset the properties that are manipulated for pan and zoom.
	 */
	private void reset() {
		oldpoint.x = 0;
		oldpoint.y = 0;
		dist = 0F;
	}
	
	/**
	 *  Re-center the destination rectangle on the screen if it's hanging off center. 
	 */
	private void Rubber() {
		// TODO add code to re-center image if it's smaller than the screen
		float cx = 0; 
		float cy = 0; 
		if (dst.top > min.top + 1) cy = (min.top - dst.top) / 3;
		if (dst.bottom < min.bottom - 1) cy = (min.bottom - dst.bottom) / 3;
		if (dst.left > min.left + 1) cx = (min.left - dst.left) / 3;
		if (dst.right < min.right - 1) cx = (min.right - dst.right) / 3; 
		if(cx == 0 && cy == 0) mode = MODE_STOP;
		dst.top += cy; 
		dst.bottom += cy;
		dst.left += cx; 
		dst.right += cx; 	
		invalidate();
	}
	
	/**
	 * Zoom the destination rectangle on the screen based on a 
	 * pinching motion.
	 * @param event the current MotionEvent
	 */
	private void Zoom(MotionEvent event) {
		float newdist = getDistance(event);
		float scale = newdist / dist; 
		if(dist != 0F) {
			center = getCenter(event);
			float cx = (dst.width() * scale) - dst.width(); 
			float cy = (dst.height() * scale) - dst.height(); 
			
			if((dst.width() > min.width() && scale < 1) || (dst.width() <= max.width() && scale > 1)) {
				dst.left -= cx * ((center.x - dst.left) / dst.width());
				dst.top -= cy * ((center.y - dst.top) / dst.height());
				dst.right += cx * ((dst.right - center.x) / dst.width());
				dst.bottom += cy * ((dst.bottom - center.y) / dst.height());
			}
			// TODO add a more sophisticated lower bound mechanism
			if(dst.width() < min.width()) dst = new RectF(min);
		}
		dist = newdist;
		invalidate(); 
	}
	
	/**
	 * Pan the destination rectangle around the screen based on a 
	 * dragging motion. 
	 * @param event the current MotionEvent
	 */
	private void Pan(MotionEvent event) {
		if(oldpoint.x != 0) {
			float dx = (event.getX(0) - oldpoint.x);
			float dy = (event.getY(0) - oldpoint.y);
			dst.left += dx;
			dst.right += dx; 
			// vert pan only if the image is shorter than the screen
			if(dst.top <= -1 || dst.bottom >= getHeight() + 1) {
				dst.top  += dy;
				dst.bottom += dy; 
			}
			invalidate(); 
		}
		oldpoint.x = event.getX(0);
		oldpoint.y = event.getY(0);
	}
	
	/**
	 * Generate a new rectangle that fits inside of width and height
	 * while preserving the aspect ratio of the src rectangle
	 * @param src a source rectangle 
	 * @param width bounding width
	 * @param height bounding height
	 * @return new rectangle fitting width and height
	 */
	private RectF fitRect(Rect src, int width, int height) {
		float sx = (float)width / (float)src.width(); 
		float sy = (float)height / (float)src.height(); 
		float scale = (sx < sy) ? sx : sy;
		float w = src.width() * scale;
		float h = src.height() * scale;
		float x = (width - w)/2;
		float y = (height - h)/2;
		
		return new RectF(x, y, x+w, y+h);
	}
	
	/**
	 * Get the distance between two points of a motion event for a 
	 * pinch operation.
	 * @param event the current MotionEvent
	 * @return linear distance between two points
	 */
	private float getDistance(MotionEvent event) {
		float dx = event.getX(0) - event.getX(1);
		float dy = event.getY(0) - event.getY(1);
		return (float) Math.sqrt( (dx * dx) + (dy * dy) );
	}

	/**
	 * Find the center point of a pinch operation. 
	 * @param event the current MotionEvent
	 * @return a new point at the center of the pinch
	 */
	private PointF getCenter(MotionEvent event) {
		PointF center = new PointF();
		center.x = ((event.getX(0) + event.getX(1)) / 2F);
		center.y = ((event.getY(0) + event.getY(1)) / 2F);
		return center; 
	}
}
