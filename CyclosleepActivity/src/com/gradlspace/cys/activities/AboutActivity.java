/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;




/**
 * @author Falling
 * 
 */
public class AboutActivity extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.about );

		findViewById( R.id.lblCyclo ).setOnLongClickListener( new OnLongClickListener() {

			public boolean onLongClick (View v)
			{
				// int mh = 0;
				// if (3 / mh < 0)
				// return true;
				Space.error( "About\nAP radios: "
						+ android.provider.Settings.System.getString(	Space.get().getContentResolver(),
																		android.provider.Settings.System.AIRPLANE_MODE_RADIOS )
						+ "\n" + DeviceTestActivity.enumSensors( Space.get() ) );

				// Space.startActivity( AboutActivity.this, DeviceTestActivity.class );
				return true;
			}
		} );

		PackageInfo info;
		try
		{
			info = getPackageManager().getPackageInfo( getPackageName(), 0 );

			// Version info
			final TextView lblVersion = (TextView) findViewById( R.id.lblVersion );
			if (Space.sIsWakeCrippledDevice)
				lblVersion.setText( info.versionName + " wcr" );
			else
				lblVersion.setText( info.versionName );
		}
		catch (NameNotFoundException e)
		{
			Space.log( "We are REALLY lost! :(" );
			e.printStackTrace();
		}
	}


	public void onClickEula (View v)
	{
		Intent intent = new Intent( Intent.ACTION_VIEW );
		intent.setData( Uri.parse( "http://gradlspace.com/cys/eula" ) );
		startActivity( intent );
	}


	public void onClickTutorial (View v)
	{
		Space.startActivity( this, IntroActivity.class );
	}


	public void onClickContact (View v)
	{
		Intent intent = new Intent( Intent.ACTION_VIEW );
		intent.setData( Uri.parse( "http://gradlspace.com/cys/contact" ) );
		startActivity( intent );

		// if (Space.startSendActivity( this,
		// Space.SEND_TYPE_EMAIL,
		// this.getString( R.string.textCysSupport ),
		// "Cyclosleep Support Request",
		// "------\nPlease enter your support request before this text and leave the lower part unchanged.\n\n"
		// + Guardian.collectSupportInfo( this ),
		// null ) != 0)
		// {
		// Space.showToast( this, R.string.errorNoEmailClient );
		// }
	}
}
