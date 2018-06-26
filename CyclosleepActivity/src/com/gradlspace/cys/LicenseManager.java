/**
 * 
 */
package com.gradlspace.cys;


import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;




/**
 * @author Falling
 * 
 */
public class LicenseManager
{
	private static final String	BASE64_PUBLIC_KEY		= "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl5LQkS2lVyQtBwX0Mq99gDH3n36LnWdvdaVpOhKjCEzfCapQSy0FUz1tlVUMORxeEiUEeiUGwnsS1gfxSz5epJ7nsMONlnrK3CQn69uVrP+q71l7QgCqnO8ZmMjO4INGUuZ12dt1D/pDaRGYa388u2HPGDxMPmIIo7y/f5wQcQJ0DiB3VLlWxlr44sQgjXdW3iczsTcmePUQutSHstQK1Mkdh6TABUJUBIlGOahLW+TFO+voobQ92mNjm87iu3yGtzcGYG5QxuheJbh4Inth7NwrL+jV3I5t9TjT1Fn7lOsijIi9dsODOnIvqC8xjGn+PKdsb0ITekOLwK146dtbAwIDAQAB";
	private static final byte[]	SALT					= new byte[] { -50, 11, 31, -84, 22, 77, 14, -3, -125, -31, -95, 94, 44,
			26, 87, -36, -72, 5, -103, 22				};
	public static final long	CRITICAL_LICENSE_DIFF	= 2 * 24 * 3600 * 1000;


	public enum StaticReturnCode
	{
		OK, DENIED, ERROR
	}


	public static class StaticLicenseCheckerCallback implements LicenseCheckerCallback
	{
		/* (non-Javadoc)
		 * @see com.google.android.vending.licensing.LicenseCheckerCallback#allow(int)
		 */
		public void allow (int reason)
		{
			sLastReturnCode = StaticReturnCode.OK;
			Space.logRelease( "saticCallback " + sLastReturnCode.name() );

			if (m_broadcastCall)
				unregisterCallback();

			s_checkInProgress = false;
		}


		/* (non-Javadoc)
		 * @see com.google.android.vending.licensing.LicenseCheckerCallback#dontAllow(int)
		 */
		public void dontAllow (int reason)
		{
			// sLastReturnCode = StaticReturnCode.DENIED;
			Space.logRelease( "saticCallback " + sLastReturnCode.name() );

			if (m_broadcastCall)
				unregisterCallback();

			s_checkInProgress = false;

		}


		/* (non-Javadoc)
		 * @see com.google.android.vending.licensing.LicenseCheckerCallback#applicationError(int)
		 */
		public void applicationError (int errorCode)
		{
			// sLastReturnCode = StaticReturnCode.ERROR;
			Space.logRelease( "saticCallback " + sLastReturnCode.name() );

			if (m_broadcastCall)
				unregisterCallback();

			s_checkInProgress = false;
		}
	}


	private static String					mDeviceId				= null;
	private static Thread					mLicThread				= null;
	private static LicenseChecker			mChecker				= null;
	private static ServerManagedPolicy		mCheckerPolicy			= null;
	private static LicenseCheckerCallback	mLicenseCheckerCallback	= null;
	public static long						sLastKnownTryUntil		= -1;
	public static StaticReturnCode			sLastReturnCode			= StaticReturnCode.OK;
	private static boolean					m_broadcastCall			= false;

	public static boolean					s_checkInProgress		= false;


	/**
	 * checks the license via LicenseChecker & ServerManagedPolicy if those classes haven't been instantiated, they will
	 * be in a separate thread since AESObfuscator takes VERY long to create...
	 */
	public static void checkLicense (LicenseCheckerCallback callback)
	{
		Space.log( "checkLicense called" );

		// s_checkInProgress = true;

		if (s_checkInProgress)
			return;

		if (callback != null)
		{
			mLicenseCheckerCallback = callback;
		}
		else
		{
			if (mLicenseCheckerCallback == null)
			{
				mLicenseCheckerCallback = new StaticLicenseCheckerCallback();
				m_broadcastCall = true;
			}
		}


		// Try to use more data here. ANDROID_ID is a single point of attack. ( e.g. googlemail )
		if (mDeviceId == null)
			mDeviceId = Secure.getString( Space.get().getContentResolver(), Secure.ANDROID_ID );

		if (mChecker == null)
		{
			s_checkInProgress = true;

			if (callback == null)
			{
				Space.log( "checkLicense broadcast call" );

				// Construct the LicenseChecker with a policy.
				if (mCheckerPolicy == null)
					mCheckerPolicy = new ServerManagedPolicy( Space.get(), new AESObfuscator( SALT, Space.get().getPackageName(),
							mDeviceId ) );

				mChecker = new LicenseChecker( Space.get(), mCheckerPolicy, BASE64_PUBLIC_KEY );

				sLastKnownTryUntil = mCheckerPolicy.getRetryUntil();

				mChecker.checkAccess( mLicenseCheckerCallback );
			}
			else
			{
				Space.log( "checkLicense activity call" );

				// we have a callback
				// we have to instantiate the license checker and do obfuscation -> different thread
				if (mLicThread == null || (mLicThread != null && mLicThread.isAlive() == false))
				{
					// thread not running already -> create
					mLicThread = new Thread( new Runnable() {
						public void run ()
						{
							// Construct the LicenseChecker with a policy.
							if (mCheckerPolicy == null)
								mCheckerPolicy = new ServerManagedPolicy( Space.get(), new AESObfuscator( SALT, Space.get()
										.getPackageName(), mDeviceId ) );

							mChecker = new LicenseChecker( Space.get(), mCheckerPolicy, BASE64_PUBLIC_KEY );

							sLastKnownTryUntil = mCheckerPolicy.getRetryUntil();

							m_broadcastCall = false;

							if (mLicenseCheckerCallback != null)
								mChecker.checkAccess( mLicenseCheckerCallback );
						}
					} );
					mLicThread.start();
				}
			}
		}
		else
		{
			Space.log( "checkLicense existing, calling" );

			mChecker.checkAccess( mLicenseCheckerCallback );
		}
	}


	public static synchronized void unregisterCallback ()
	{
		if (mChecker != null)
		{
			mChecker.onDestroy();
			mChecker = null;
		}

		if (mLicenseCheckerCallback != null)
		{
			mLicenseCheckerCallback = null;
		}

		s_checkInProgress = false;
	}


	private static final String	PREFS_FILE	= "com.android.vending.licensing.ServerManagedPolicy";


	public static synchronized void resetCache ()
	{
		SharedPreferences sp = Space.get().getSharedPreferences( PREFS_FILE, Context.MODE_PRIVATE );
		sp.edit().clear().commit();
	}
}
