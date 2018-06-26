/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.os.Bundle;

import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;




/**
 * @author Falling
 * 
 */
public class SNSActivity extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		// why are we called?
		if (this.getIntent().getAction() == Space.ACTION_ALARM)
		{

		}

		setContentView( R.layout.alarm );
	}


	@Override
	protected void onStart ()
	{
		super.onStart();
	}


	@Override
	protected void onStop ()
	{
		super.onStop();
	}


	@Override
	protected void onDestroy ()
	{
		super.onDestroy();
	}


	@Override
	protected void onResume ()
	{
		super.onResume();
	}


	@Override
	protected void onPause ()
	{
		super.onPause();
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();
	}
}
