/**
 * 
 */
package com.gradlspace.widgets;


import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.gradlspace.cys.Hypnogram;
import com.gradlspace.cys.SleepStage;




/**
 * @author Falling
 * 
 */
public class HypnogramView extends View
{
	private Hypnogram				mHypnogram	= null;
	private boolean					mDrawAxes	= true;

	private GestureDetector			mGestureDetector;
	private ScaleGestureDetector	mScaleGeDet;
	private float					mScaleLeftRight	= 1f, mScaleUpDown = 1f, mScaleInOut = 1f;


	class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		@Override
		public boolean onScale (ScaleGestureDetector detector)
		{
			mScaleInOut *= detector.getScaleFactor();
			invalidate();

			return true;
		}
	}


	class GestureListener extends GestureDetector.SimpleOnGestureListener
	{
		@Override
		public boolean onSingleTapUp (MotionEvent ev)
		{
			return true;
		}


		@Override
		public void onShowPress (MotionEvent ev)
		{}


		@Override
		public void onLongPress (MotionEvent ev)
		{
			// reset
			mScaleInOut = mScaleLeftRight = mScaleUpDown = 1f;
			invalidate();
		}


		@Override
		public boolean onDoubleTapEvent (MotionEvent ev)
		{
			// reset
			mScaleInOut = mScaleLeftRight = mScaleUpDown = 1f;
			invalidate();
			return true;
		}


		@Override
		public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
		{
			// int dist = (int) (m_wndStart + distanceX * 2.5f);
			//
			// if (dist + m_wndWidth < mHypnogram.mStages.size() && dist > 0)
			// {
			// m_wndStart = dist;
			// invalidate();
			// }

			mScaleLeftRight += distanceX * 0.5f;
			if (mScaleLeftRight < -64)
				mScaleLeftRight = -64f;
			else if (mScaleLeftRight > 190)
				mScaleLeftRight = 190f;


			mScaleUpDown += distanceY * 0.5f;
			if (mScaleUpDown < -128)
				mScaleUpDown = -128f;
			else if (mScaleUpDown > 127)
				mScaleUpDown = 127f;


			invalidate();

			// Space.Log( "scroll " + distanceX + " " + mScaleLeftRight + "  " + distanceY );

			return true;
		}


		@Override
		public boolean onDown (MotionEvent ev)
		{
			return true;
		}


		@Override
		public boolean onFling (MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
		{

			// Space.Log( "fling", "vel " + velocityX );

			// if (velocityX > 5000.0f || velocityX < 5000.0f)
			// onScroll( e1, e2, (-velocityX), 0 );
			return true;
		}
	}


	/**
	 * @param context
	 */
	public HypnogramView (Context context)
	{
		super( context );

		// gestures control the opacity of various elements of the graph
		mGestureDetector = new GestureDetector( new GestureListener() );
		mScaleGeDet = new ScaleGestureDetector( context, new ScaleListener() );
	}


	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public HypnogramView (Context context, AttributeSet attrs, int defStyle)
	{
		super( context, attrs, defStyle );

		mGestureDetector = new GestureDetector( new GestureListener() );
		mScaleGeDet = new ScaleGestureDetector( context, new ScaleListener() );
	}


	/**
	 * @param context
	 * @param attrs
	 */
	public HypnogramView (Context context, AttributeSet attrs)
	{
		super( context, attrs );

		mGestureDetector = new GestureDetector( new GestureListener() );
		mScaleGeDet = new ScaleGestureDetector( context, new ScaleListener() );
	}


	@Override
	public boolean onTouchEvent (MotionEvent event)
	{
		if (mGestureDetector.onTouchEvent( event ))
			return true;
		else if (mScaleGeDet.onTouchEvent( event ))
			return true;
		else
			return false;
	}


	public void setHypnogram (Hypnogram hypno)
	{
		mHypnogram = hypno;
		invalidate();
	}


	public void setDrawAxes (boolean drawThem)
	{
		mDrawAxes = drawThem;
	}


	Paint	bgShader	= null;
	Paint	paintFrame	= null;


	@Override
	protected void onDraw (Canvas canvas)
	{
		if (this.isEnabled() == false)
		{
			return;
		}

		canvas.drawColor( Color.argb( 255, 32, 32, 32 ) );

		if (mHypnogram == null)
		{
			return;
		}


		if (bgShader == null)
		{
			bgShader = new Paint();
			int[] _color = new int[ 2 ];
			// _color[ 0 ] = Color.argb( 255, 48, 48, 48 );
			// _color[ 1 ] = Color.argb( 255, 16, 16, 16 );
			_color[ 0 ] = Color.argb( 255, 52, 52, 64 );
			_color[ 1 ] = Color.argb( 255, 2, 2, 5 );
			bgShader.setShader( new LinearGradient( 0, 0, 0, getHeight(), _color, null, Shader.TileMode.MIRROR ) );
		}
		canvas.drawRect( 1, 0, getWidth() - 1, getHeight() - 1, bgShader );

		if (paintFrame == null)
		{
			paintFrame = new Paint();
			paintFrame.setColor( Color.argb( 255, 92, 92, 92 ) );
			paintFrame.setStyle( Paint.Style.STROKE );
		}
		canvas.drawRect( 1, 0, getWidth() - 1, getHeight() - 1, paintFrame );


		drawRegion( canvas, 5, getWidth() - 5, 5, getHeight() - 5 );

		// and make sure to redraw asap - not required when nothing changes!
		// invalidate();
	}


	/* (non-Javadoc)
	 * 
	 * @see android.view.View#onSizeChanged(int, int, int, int) */
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh)
	{
		mShader = null;
		super.onSizeChanged( w, h, oldw, oldh );
	}


	private transient Paint	mShader			= null;
	private transient Paint	mGraphPaint		= null;
	private transient Paint	mAvgPaint		= null;
	private transient Paint	mTextPaint		= null;
	private transient Paint	mStageTextPaint	= null;


	private transient int	_size			= 0;


	public void drawRegion (Canvas can, float left, float right, float top, float bottom)
	{
		_size = mHypnogram.stages.size();

		if (_size <= 0)
			return;

		// Log.d( "t", "test" + _size );

		// make sure the last entry has a duration
		mHypnogram.fillLastDuration( System.currentTimeMillis() );

		// initialize the first time or after size changes
		if (mShader == null)
		{
			int[] col = new int[ 2 ];
			col[ 0 ] = Color.argb( 128, 92, 128, 92 );
			col[ 1 ] = Color.argb( 128, 12, 16, 12 );
			mShader = new Paint();
			mShader.setShader( new LinearGradient( left, top, left, bottom, col, null, Shader.TileMode.MIRROR ) );
		}

		// recalc all stats
		mHypnogram.calculateStats();

		long cspan = 0;

		if (mHypnogram.stats.timeSpan <= 0)
		{
			return;
		}

		// calculate units
		float xunit = (right - left) / mHypnogram.stats.timeSpan;
		float yunit = (bottom - top) / 5;

		// initialize the first time
		if (mTextPaint == null || mStageTextPaint == null)
		{
			mTextPaint = new Paint();
			mTextPaint.setTextAlign( Paint.Align.LEFT );
			mTextPaint.setARGB( 225, 163, 163, 163 );
			mTextPaint.setShadowLayer( 4.0f, 2, 2, Color.BLACK );
			mTextPaint.setAntiAlias( true );
			mTextPaint.setTypeface( Typeface.DEFAULT_BOLD );
			mTextPaint.setTextSize( 12f );

			mStageTextPaint = new Paint();
			mStageTextPaint.setTextAlign( Paint.Align.CENTER );
			mStageTextPaint.setARGB( 128, 128, 128, 128 );
			mStageTextPaint.setAntiAlias( true );
			mStageTextPaint.setTextSize( 16f );

			mGraphPaint = new Paint();
			mGraphPaint.setARGB( 128, 128, 128, 192 );
			mGraphPaint.setAntiAlias( true );
			mGraphPaint.setStyle( Paint.Style.STROKE );
			mGraphPaint.setStrokeWidth( 2.0f );
			// mGraphPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
			mGraphPaint.setPathEffect( new CornerPathEffect( 2.0f ) );

			mAvgPaint = new Paint();
			mAvgPaint.setARGB( 124, 192, 128, 128 );
			mAvgPaint.setAntiAlias( true );
			mAvgPaint.setStyle( Paint.Style.STROKE );
			mAvgPaint.setStrokeWidth( 2.0f );
			// mHannPaint.setShadowLayer( 4.0f, 2, 2, Color.LTGRAY );
			float intervals[] = { 1.0f, 3.0f };
			mAvgPaint.setPathEffect( new DashPathEffect( intervals, 1.0f ) );
		}

		RectF barRect = null;
		Path bezier = new Path();
		ArrayList< RectF > ctrlRects = new ArrayList< RectF >( _size );


		// draw hypnogram
		for (SleepStage css : mHypnogram.stages)
		{
			barRect = new RectF();
			barRect.left = left + xunit * cspan;
			barRect.top = top + yunit * ((float) css.phase.ordinalRev() - 0.7f);
			barRect.right = left + xunit * (cspan + css.durationMillis);
			barRect.bottom = bottom;

			ctrlRects.add( barRect );

			// draw graph-rect
			can.drawRect( barRect, mShader );
			cspan += css.durationMillis;
		}

		if (_size > 2)
		{
			// ==============> Average Line
			mAvgPaint.setARGB( 192, 164, 128, 128 );
			float avg = top + yunit * (6.5f - mHypnogram.stats.averagePhase);
			can.drawLine( left, avg, right, avg, mAvgPaint );
			// <=============

			// bezier curve
			float yMidpoint;

			for (int i = 0; i < _size - 1; i++)
			{
				if (i == 0)
				{
					bezier.moveTo( ctrlRects.get( i ).left, ctrlRects.get( i ).top );
				}

				// yMidpoint between thisY and nextY
				// yMidpoint = ctrlRects.get( i ).top - ( (ctrlRects.get( i ).top - ctrlRects.get( i + 1 ).top) * 0.5f);
				//
				// bezier.quadTo( ctrlRects.get( i ).left, ctrlRects.get( i ).top, ctrlRects.get( i ).centerX(),
				// ctrlRects.get( i ).top );
				// bezier.quadTo( ctrlRects.get( i ).right, ctrlRects.get( i ).top, ctrlRects.get( i ).right, yMidpoint
				// );

				bezier.lineTo( ctrlRects.get( i ).left, ctrlRects.get( i ).top );
				bezier.lineTo( ctrlRects.get( i ).right, ctrlRects.get( i ).top );

				if (i == _size - 2)
				{
					bezier.setLastPoint( ctrlRects.get( i + 1 ).right, ctrlRects.get( i + 1 ).top );
				}
			}

			mGraphPaint.setARGB( (int) (128 + mScaleUpDown), 128, 128, 192 );
			can.drawPath( bezier, mGraphPaint );
		}
		else
		{

		}

		// we only draw a timeline, timestamps really get messed up
		if (mDrawAxes)
		{
			Time tt = new Time();

			// ==============> Start time
			mTextPaint.setTextAlign( Paint.Align.LEFT );
			long starttime = mHypnogram.stages.get( 0 ).startMillis;
			tt.set( starttime );
			can.drawText( tt.format( "%H:%M" ), left, bottom - mTextPaint.descent(), mTextPaint );
			// <=============

			if (_size > 1)
			{
				// ==============> End Time
				mTextPaint.setTextAlign( Paint.Align.RIGHT );
				tt.set( mHypnogram.stages.get( _size - 1 ).startMillis + mHypnogram.stages.get( _size - 1 ).durationMillis );
				can.drawText( tt.format( "%H:%M" ), right, bottom - mTextPaint.descent(), mTextPaint );
				// <=============

				// ==============> Additional timestamps (4) in between
				if (_size > 3)
				{
					mTextPaint.setTextAlign( Paint.Align.CENTER );
					float divPx = (right - left) * 0.2f;
					long divTime = (long) (mHypnogram.stats.timeSpan * 0.2f);
					cspan = divTime;
					for (int i = 1; i < 5; i++)
					{
						tt.set( starttime + cspan );
						can.drawText( tt.format( "%H:%M" ), left + divPx * i, bottom - mTextPaint.descent(), mTextPaint );
						cspan += divTime;
					}
				}
				// <=============
			}

		}

		// draw y-axis
		if (mDrawAxes)
		{
			mStageTextPaint.setARGB( (int) (64 + mScaleLeftRight), 128, 128, 128 );

			// ==============> DRAW date and total time slept
			Time tt = new Time();
			tt.set( mHypnogram.stages.get( 0 ).startMillis );
			mTextPaint.setTextAlign( Paint.Align.LEFT );
			can.drawText( String.format(	"%s [%.1f h / %.1f h]",
											tt.format( "%Y-%m-%d" ),
											(float) mHypnogram.stats.sleepSpan / 3600000,
											(float) mHypnogram.stats.timeSpan / 3600000 ), left + 30, top - mTextPaint.ascent()
					+ mTextPaint.descent() + 2, mTextPaint );
			// <=============

			// ==============> DRAW sleep stages
			drawStage( SleepStage.SleepPhase.AWAKE, can, left, right, top, bottom, yunit );
			drawStage( SleepStage.SleepPhase.DAWN, can, left, right, top, bottom, yunit );
			drawStage( SleepStage.SleepPhase.REM, can, left, right, top, bottom, yunit );
			drawStage( SleepStage.SleepPhase.TWILIGHT, can, left, right, top, bottom, yunit );
			drawStage( SleepStage.SleepPhase.DEEP, can, left, right, top, bottom, yunit );
			// <=============

		}
	}


	/**
	 * Draws a certain stage's text and stats.
	 * 
	 * @param phase
	 * @param can
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 * @param yunit
	 */
	private void drawStage (SleepStage.SleepPhase phase, Canvas can, float left, float right, float top, float bottom, float yunit)
	{
		mStageTextPaint.setTextAlign( Paint.Align.CENTER );

		float y = top - mStageTextPaint.ascent() + yunit * phase.ordinalRev() - yunit * 0.5f;

		// draw stage text
		can.drawText( phase.getText( getResources() ), left + (right - left) * 0.5f, y, mStageTextPaint );

		// draw sleep "histogram"
		can.drawRect(	left,
						y + mStageTextPaint.descent(),
						(float) (left + (mHypnogram.stats.phaseDistrib.get( phase ) * (right - left))),
						y + mStageTextPaint.ascent(),
						mStageTextPaint );

		// draw percent
		mTextPaint.setTextAlign( Paint.Align.LEFT );
		can.drawText( String.format( "%.2f %%", mHypnogram.stats.phaseDistrib.get( phase ) * 100 ), left, y, mTextPaint );
	}
}
