/**
 * 
 */
package com.gradlspace.cys;


import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.activities.AlarmActivity;




/**
 * @author Falling
 * 
 */
public class TriggerHandler extends BroadcastReceiver
{
	/**
	 * Maximum number of built-in alarms
	 */
	public static final byte			NUM_ALARMS		= 4;
	/**
	 * Maximum number of triggers + alarms
	 */
	public static final int				MAX_TRIGGERS	= 512;

	/**
	 * Array of all loaded triggers
	 */
	private static ArrayList< Trigger >	sTriggers		= new ArrayList< Trigger >( MAX_TRIGGERS );


	public static int add (Trigger trigger, Context con)
	{
		int id = sTriggers.size();

		if (id >= MAX_TRIGGERS - 2)
		{
			Log.e( "TriggerHandler::add", "Too many triggers installed! size = " + id );
			return -1;
		}

		trigger.assign( id );
		sTriggers.add( trigger );
		trigger.onInit( con );

		return id;
	}


	/**
	 * Loads all triggers from shared prefs, db or other settings Returns the number of active triggers.
	 * 
	 * @param con
	 */
	public static synchronized int loadTriggers (Context con)
	{
		sTriggers.clear();

		// load all built-in alarms
		TimeTrigger t = null;
		int numActive = 0;
		for (int i = 0; i < NUM_ALARMS; i++)
		{
			t = new TimeTrigger();

			add( t, con );

			if (t.enabled)
				numActive++;
		}

		// load all variable triggers
		// implement them...? :>

		Space.logRelease( "loadTriggers: ...done [" + numActive + "]" );

		return numActive;
	}


	/**
	 * Setup a specific alarm.
	 * 
	 * @param con
	 * @param id
	 *            trigger-id
	 * @param hour
	 * @param minute
	 */
	public static void setAlarm (Context con, int id, int hour, int minute)
	{
		TimeTrigger tt = (TimeTrigger) TriggerHandler.getTrigger( id );
		tt.setHourMin( hour, minute );
		tt.enabled = true;
		tt.recurrence = "daily";
		tt.setNextOccurrence( -1 );

		tt.persistValues( con, null );

		Space.doTriggerUpdate( con, false, false );
	}


	/**
	 * Retrieves the requested trigger.
	 * 
	 * @param id
	 * @return the trigger requested or null if idx is invalid
	 */
	public static Trigger getTrigger (int idx)
	{
		if (idx < 0 || idx >= sTriggers.size())
			return null;

		return sTriggers.get( idx );
	}


	/**
	 * Returns a reference to the TimeTrigger that will fire next. If no match is found, null is returned.
	 * 
	 * @param onlyActive
	 *            considers only active TimeTriggers
	 * @return a reference to the next TimeTrigger, or null if none is available
	 */
	public static TimeTrigger getNextTimeTrigger (boolean onlyActive)
	{
		long next = Long.MAX_VALUE;
		long millis = 0;
		long now = System.currentTimeMillis();
		TimeTrigger tt = null;

		for (Trigger t : sTriggers)
		{
			if (onlyActive && t.enabled == false)
				continue;

			if (t.getClass() == TimeTrigger.class)
			{
				millis = ((TimeTrigger) t).getFireTime();
				// check if the returned time is in the future and sooner than the last saved value
				if (millis - now > 0 && millis < next)
				{
					next = millis;
					tt = (TimeTrigger) t;
				}
			}
		}

		return tt;
	}


	/**
	 * Returns a formatted string representing the time until the next trigger fires.
	 * 
	 * @param con
	 * @return
	 */
	public static CharSequence getNextTriggerEta (Context con)
	{
		if (sTriggers.size() <= 0)
		{
			return new String( "" );
		}
		TimeTrigger tt = TriggerHandler.getNextTimeTrigger( true );
		if (tt != null)
		{
			return DateUtils.getRelativeDateTimeString( con,
														tt.getFireTime(),
														DateUtils.MINUTE_IN_MILLIS,
														DateUtils.WEEK_IN_MILLIS,
														0 );
		}

		return "n/a";
		// return DateUtils.getRelativeTimeSpanString( TriggerHandler.getNextTimeTrigger( false, true ).getFireTime(
		// false ),
		// System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_NUMERIC_DATE );
	}


	/**
	 * Returns the number of currently active (will fire somewhen) triggers.
	 * 
	 * @return
	 */
	public static int getNumTriggers (boolean onlyActive)
	{
		if (onlyActive == false)
			return sTriggers.size();

		int num = 0;
		for (Trigger t : sTriggers)
		{
			if (t.enabled == false)
				continue;
			num++;
		}
		return num;
	}


	public static void disableAllTriggers (Context con)
	{
		for (Trigger t : sTriggers)
		{
			if (t.enabled == true)
				t.enabled = false;
			t.persistValues( con, null );
		}
	}


	public static void fireTimeTriggers (Context con)
	{
		for (Trigger t : sTriggers)
		{
			if (t.getClass() == TimeTrigger.class)
			{
				if (t.enabled)
					((TimeTrigger) t).onFire( con, false );
			}
		}
	}


	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive (Context context, Intent intent)
	{
		Space.initSpace( context );

		// the lock has to be released by the alarm activity
		LockAuthority.acquireSecure();

		Space.log( "TriggerHandler::onReceive" );

		fireTimeTriggers( context );

		// make sure we leave some time space
		SystemClock.sleep( 16 );

		// install next trigger
		installSystemAlarm();

		// check if this is the boot receiver
		tAction = intent.getAction();
		if (tAction != null && tAction.equals( Intent.ACTION_BOOT_COMPLETED ))
		{
			Space.doTriggerUpdate( context, true, false );
		}

		if (AlarmActivity.sIsRunning == false)
		{
			// if alarm is not running, we are responsible for releasing our lock
			LockAuthority.releaseSecure();
		}
	}


	private transient String		tAction		= null;
	private static PendingIntent	mPendingInt	= null;


	/**
	 * Installs the next trigger event as alarm to the Android Alarm Table.
	 * 
	 * @param con
	 */
	public static void installSystemAlarm ()
	{
		TimeTrigger tt = getNextTimeTrigger( true );
		if (tt != null)
		{
			if (mPendingInt == null)
			{
				// intent with new task flag
				Intent in = new Intent( Space.get(), TriggerHandler.class );
				in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
				mPendingInt = PendingIntent.getBroadcast( Space.get(), 0, in, 0 );
			}

			long fireTime = tt.getFireTime();
			long curTime = System.currentTimeMillis();

			if (CyopsBoolean.IS_SIMPLE_MODE.isNotEnabled() && curTime < fireTime - Cyops.MILLIS_PRESTART - 70000)
			{
				// prestart is in the future, we set the prestart time as first fire time
				fireTime -= Cyops.MILLIS_PRESTART;
			}

			Space.logRelease( "install ... " + fireTime + "  ... " + curTime );
			Space.getAlarmManager().set( AlarmManager.RTC_WAKEUP, fireTime, mPendingInt );
		}
	}


	/**
	 * Removes us from Android Alarm Table
	 * 
	 * @param con
	 */
	public static void cancelSystemAlarm (Context con)
	{
		if (mPendingInt == null)
		{
			// intent with new task flag
			Intent in = new Intent( con, TriggerHandler.class );
			in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
			mPendingInt = PendingIntent.getBroadcast( con, 0, in, 0 );
		}

		Space.getAlarmManager().cancel( mPendingInt );
	}


}
