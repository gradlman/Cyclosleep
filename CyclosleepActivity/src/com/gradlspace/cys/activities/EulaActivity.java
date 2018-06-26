/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.gradlspace.cys.Cyops;
import com.gradlspace.cys.R;




/**
 * @author Falling
 * 
 */
public class EulaActivity extends Activity
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.eula );

		final Button btnAccept = (Button) findViewById( R.id.btnEulaAccept );
		btnAccept.setOnClickListener( new View.OnClickListener() {
			public void onClick (View v)
			{
				// CyopsBoolean.EULA_ACCEPTED.set( true );
				Cyops.setEulaAccepted( true );
				EulaActivity.this.setResult( RESULT_OK );
				EulaActivity.this.finish();
			}
		} );
		final Button btnReject = (Button) findViewById( R.id.btnEulaReject );
		btnReject.setOnClickListener( new View.OnClickListener() {
			public void onClick (View v)
			{
				Cyops.setEulaAccepted( false );
				EulaActivity.this.setResult( RESULT_CANCELED );
				EulaActivity.this.finish();
			}
		} );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed ()
	{
		//
		// super.onBackPressed();
	}


}
