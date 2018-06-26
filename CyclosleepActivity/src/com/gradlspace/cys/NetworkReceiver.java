/**
 * 
 */
package com.gradlspace.cys;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.Time;




/**
 * @author Falling
 * 
 */
public class NetworkReceiver extends BroadcastReceiver
{
	private String	mAction	= null;


	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive (Context context, Intent intent)
	{
		mAction = intent.getAction();
		if (mAction != null && mAction.equals( ConnectivityManager.CONNECTIVITY_ACTION ))
		{
			Space.initSpace( context );


			// NetworkInfo info = ((ConnectivityManager) context.getSystemService( CONNECTIVITY_SERVICE ))
			// .getActiveNetworkInfo();
			NetworkInfo info = intent.getParcelableExtra( ConnectivityManager.EXTRA_NETWORK_INFO );

			Space.log( "Received conn broadcast" );
			if (info.isAvailable())
			{
				Time t = new Time();
				t.set( LicenseManager.sLastKnownTryUntil );

				Space.log( "Conn available! " + t.format3339( false ) );

				// Space.log( "License Answer: " + mCheckerPolicy.getLastResponse().name() + " valid " + t.format3339(
				// false ) );


				// if (mCheckerPolicy.getLastResponse() == LicenseResponse.LICENSED)

				if (LicenseManager.sLastKnownTryUntil != -1)
				{
					if (LicenseManager.sLastKnownTryUntil - System.currentTimeMillis() < LicenseManager.CRITICAL_LICENSE_DIFF)
					{
						LicenseManager.checkLicense( null );
					}
				}
			}
			// mTypeName = info.getTypeName();
			// mSubtypeName = info.getSubtypeName();
			// mAvailable = info.isAvailable();
			// Log.i( "net", "Network Type: " + mTypeName + ", subtype: " + mSubtypeName + ", available: " +
			// mAvailable );
		}
	}
}
