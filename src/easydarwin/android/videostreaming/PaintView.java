package easydarwin.android.videostreaming;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class PaintView extends View implements OnTouchListener{

	Paint mPaint;
	float mX;
	float mY;	
	
	public PaintView(Context context,AttributeSet attributeSet){
		super(context,attributeSet);
	
		/** Initializing the variables */
		mPaint = new Paint();
		mX = mY = -100;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {		
		super.onDraw(canvas);
		
		// Setting the color of the circle
		mPaint.setColor(Color.YELLOW);
		mPaint.setStyle(Style.STROKE);
//        RectF oval = new RectF(mX, mY, 60, 80);  
//        canvas.drawOval(oval, mPaint);  
		// Draw the circle at (x,y) with radius 60
		canvas.drawCircle(mX, mY, 60, mPaint);	

		// Redraw the canvas
		invalidate();			
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(event.getAction()){
			// When user touches the screen
			case MotionEvent.ACTION_DOWN:
				// Getting X,Y coordinate
				mX = event.getX();
				mY = event.getY();
				break;
		}
		return true;
	}
}