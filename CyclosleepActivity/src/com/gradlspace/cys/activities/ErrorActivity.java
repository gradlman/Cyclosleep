/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gradlspace.cys.Guardian;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;




/**
 * @author Falling
 * 
 */
public class ErrorActivity extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.error );

		TextView lblDetails = (TextView) findViewById( R.id.lblDetails );

		lblDetails.setText( getIntent().getStringExtra( Space.EXTRA_STRING ) );
	}


	public void onReportClick (View v)
	{
		DeviceTestActivity.enumSensors( this );
		Guardian.clearReport();
		Guardian.dispatchReport( getIntent().getStringExtra( Space.EXTRA_STRING ) );
	}


	public void onOkClick (View v)
	{
		finish();
	}
}
