/**
 * 
 */
package com.gradlspace.cys;


import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.gradlspace.cys.Cyops.CyopsInt;




/**
 * Handles the WakeLocks used by all Cys modules.
 * 
 * There are secure Locks and normal Locks. The flags should be determined the first time the user ever starts the app.
 * 
 * @author Falling
 * 
 */
public class LockAuthority
{
	public enum LockMode
	{
		DEFAULT, NOSENSOR
	}


	private static final int			DEFAULT_SECURE_FLAGS	= PowerManager.PARTIAL_WAKE_LOCK;
	private static final int			DEFAULT_NORMAL_FLAGS	= PowerManager.PARTIAL_WAKE_LOCK;

	private static final int			NOSENSOR_SECURE_FLAGS	= PowerManager.SCREEN_DIM_WAKE_LOCK
																		| PowerManager.ACQUIRE_CAUSES_WAKEUP
																		| PowerManager.ON_AFTER_RELEASE;
	private static final int			NOSENSOR_NORMAL_FLAGS	= PowerManager.SCREEN_DIM_WAKE_LOCK
																		| PowerManager.ACQUIRE_CAUSES_WAKEUP
																		| PowerManager.ON_AFTER_RELEASE;

	private static volatile WakeLock	s_normalLock			= null;
	private static volatile WakeLock	s_secureLock			= null;

	private static int					s_secureFlags			= 0;
	private static int					s_normalFlags			= 0;


	private static LockMode				s_mode					= LockMode.DEFAULT;


	/**
	 * Load lock flags from preferences.
	 * 
	 * @param con
	 */
	public static synchronized void loadFlags ()
	{
		s_secureFlags = CyopsInt.LAF_SECURE.get();
		s_normalFlags = CyopsInt.LAF_NORMAL.get();
	}


	public static synchronized void setMode (LockMode mode)
	{
		s_mode = mode;

		switch (s_mode)
		{
			case DEFAULT:
				s_secureFlags = DEFAULT_SECURE_FLAGS;
				s_normalFlags = DEFAULT_NORMAL_FLAGS;
				break;

			case NOSENSOR:
				s_secureFlags = NOSENSOR_SECURE_FLAGS;
				s_normalFlags = NOSENSOR_NORMAL_FLAGS;
				break;
		}

		Cyops.spref().edit().putInt( CyopsInt.LAF_SECURE.key, s_secureFlags ).putInt( CyopsInt.LAF_NORMAL.key, s_normalFlags )
				.commit();
	}


	public static synchronized void reset ()
	{
		s_secureFlags = 0;
		s_normalFlags = 0;
	}


	public static synchronized LockMode getMode ()
	{
		if (s_normalFlags == DEFAULT_NORMAL_FLAGS)
			s_mode = LockMode.DEFAULT;
		else
			s_mode = LockMode.NOSENSOR;
		return s_mode;
	}


	public static boolean isValid ()
	{
		return (s_secureFlags != 0);
	}


	/**
	 * @param con
	 */
	public static synchronized void acquireSecure ()
	{
		if (s_secureLock == null)
		{
			if (s_secureFlags == 0)
				loadFlags();

			if (s_secureFlags == 0)
				s_secureFlags = DEFAULT_SECURE_FLAGS;

			s_secureLock = Space.getPowerManager().newWakeLock( s_secureFlags, "cys.lock.secure" );
		}

		s_secureLock.acquire();
		if (!s_secureLock.isHeld())
		{
			Log.w( Space.TAG, "SecureLock error ... Trying normal lock ..." );
			acquireNormal();
		}
	}


	/**
	 * 
	 */
	public static synchronized void releaseSecure ()
	{
		if (s_secureLock != null)
		{
			s_secureLock.release();
			s_secureLock = null;
		}
	}


	/**
	 * Acquires global wakelock that will only be released by calling release
	 * 
	 * @param con
	 */
	public static synchronized void acquireNormal ()
	{
		// Space.log( "acquire " + (sWakeLockFlags & PowerManager.FULL_WAKE_LOCK) );

		if (s_normalLock == null)
		{
			if (s_secureFlags == 0)
				loadFlags();

			if (s_normalFlags == 0)
				s_normalFlags = DEFAULT_NORMAL_FLAGS;

			s_normalLock = Space.getPowerManager().newWakeLock( s_normalFlags, "cys.lock.normal" );
		}

		s_normalLock.acquire();
		if (!s_normalLock.isHeld())
		{
			Log.w( Space.TAG, "Permanent lock error... normal operation impossible." );
		}
	}


	/**
	 * 
	 */
	public static synchronized void releaseNormal ()
	{
		if (s_normalLock != null)
		{
			s_normalLock.release();
			s_normalLock = null;
		}
	}
}
