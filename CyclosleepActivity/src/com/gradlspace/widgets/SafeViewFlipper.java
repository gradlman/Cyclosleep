package com.gradlspace.widgets;


import com.gradlspace.cys.Space;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ViewFlipper;




/**
 * Works around Android Bug 6191 by catching IllegalArgumentException after
 * detached from the window.
 * 
 * @author Eric Burke (eric@squareup.com)
 */
public class SafeViewFlipper extends ViewFlipper
{
	public SafeViewFlipper (Context context)
	{
		super( context );
	}


	public SafeViewFlipper (Context context, AttributeSet attrs)
	{
		super( context, attrs );
	}


	/**
	 * Workaround for Android Bug 6191:
	 * http://code.google.com/p/android/issues/detail?id=6191
	 * <p/>
	 * ViewFlipper occasionally throws an IllegalArgumentException after screen rotations.
	 */
	@Override
	protected void onDetachedFromWindow ()
	{
		try
		{
			super.onDetachedFromWindow();
		}
		catch (IllegalArgumentException e)
		{
			Log.d( Space.TAG, "SafeViewFlipper ignoring IllegalArgumentException" );

			// Call stopFlipping() in order to kick off updateRunning()
			stopFlipping();
		}
	}
}