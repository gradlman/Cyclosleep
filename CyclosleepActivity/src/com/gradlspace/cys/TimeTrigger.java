/**
 * 
 */
package com.gradlspace.cys;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.text.format.DateUtils;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.activities.AlarmActivity;
import com.gradlspace.cys.activities.MonitorActivity;




/**
 * @author Falling
 * 
 */
public class TimeTrigger extends Trigger
{
	/** time "margin" in milliseconds within which another time is still considered EQUAL */
	protected static final long	MILLI_MARGIN	= 9000;

	public static final byte	BEFORE			= -1;
	public static final byte	AFTER			= 1;
	public static final byte	EQUAL			= 0;

	/** the calendar representing the trigger time */
	private GregorianCalendar	mCalendar		= new GregorianCalendar();

	/** recurrence pattern */
	public String				recurrence		= "daily";

	/** last time the trigger fired */
	private long				mLastFireTime	= -1;

	private boolean				mHasPrefired	= false;

	private transient long		t_millisTime	= 0;


	/**
	 * sets this alarm instance time to the next valid occurrence from nowTime, according to the recurPattern. if
	 * nowTime <= 0, the current system time is used.
	 * 
	 * @param nowTime
	 *            can be <= 0, then the current system time is used as nowTime
	 */
	public void setNextOccurrence (long nowTime)
	{
		// invalid nowTime, set to current time
		if (nowTime <= 0)
			nowTime = System.currentTimeMillis();

		// check if time is in the future or right now AND lastfire is way in the past so we shouldn't set next
		// occurrence since it still needs to fire
		if ( (compareTo( nowTime ) == AFTER && recurrence.contains( "daily" ))
				|| (compareTo( nowTime ) == EQUAL && mLastFireTime < nowTime - 70000))
		{
			// we don't set a next occurrence
			return;
		}

		// at this point we will change the fire time, therefore we reset the fire-related states
		mHasPrefired = false;
		mLastFireTime = -1;

		if (recurrence.contains( "once" ))
		{
			// there is no next occurrence
			return;
		}

		// make sure we don't iterate forever. if time is over 34 days past
		if (mCalendar.getTimeInMillis() < nowTime - 3000000000L)
		{
			// use same time, but set year and month to the current values
			GregorianCalendar now = new GregorianCalendar();
			mCalendar.set( Calendar.YEAR, now.get( Calendar.YEAR ) );
			mCalendar.set( Calendar.MONTH, now.get( Calendar.MONTH ) );
		}


		// at this point we have the following scenarios:
		// 1. If there was an alarm in the last 70 secs, it fired
		// 2. Alarm time is certainly in the past or within 70 secs from now, but already fired
		if (recurrence.contains( "daily" ))
		{
			// daily
			while (compareTo( nowTime ) == BEFORE)
			{
				mCalendar.add( Calendar.DAY_OF_MONTH, 1 );
			}
		}
		else
		{
			// weekly recurrence
			boolean mon, tue, wed, thu, fri, sat, sun;
			mon = tue = wed = thu = fri = sat = sun = false;

			if (recurrence.contains( "mon" ))
				mon = true;
			if (recurrence.contains( "tue" ))
				tue = true;
			if (recurrence.contains( "wed" ))
				wed = true;
			if (recurrence.contains( "thu" ))
				thu = true;
			if (recurrence.contains( "fri" ))
				fri = true;
			if (recurrence.contains( "sat" ))
				sat = true;
			if (recurrence.contains( "sun" ))
				sun = true;

			int day = mCalendar.get( Calendar.DAY_OF_WEEK );

			// TODO: this can be done more elegantly with sets...

			// if triggertime is in the past or now OR
			// triggertime is in the future but not on any of the weekdays set as reccurrence
			if ( (compareTo( nowTime ) <= EQUAL)
					|| ( (compareTo( nowTime ) == AFTER) && ( (!mon && day == Calendar.MONDAY)
							|| (!tue && day == Calendar.TUESDAY) || (!wed && day == Calendar.WEDNESDAY)
							|| (!thu && day == Calendar.THURSDAY) || (!fri && day == Calendar.FRIDAY)
							|| (!sat && day == Calendar.SATURDAY) || (!sun && day == Calendar.SUNDAY))))
			{
				for (int i = 0; i < 60; ++i)
				{
					mCalendar.add( Calendar.DAY_OF_MONTH, 1 );
					day = mCalendar.get( Calendar.DAY_OF_WEEK );
					if (mon && day == Calendar.MONDAY)
						break;
					if (tue && day == Calendar.TUESDAY)
						break;
					if (wed && day == Calendar.WEDNESDAY)
						break;
					if (thu && day == Calendar.THURSDAY)
						break;
					if (fri && day == Calendar.FRIDAY)
						break;
					if (sat && day == Calendar.SATURDAY)
						break;
					if (sun && day == Calendar.SUNDAY)
						break;
				}
			}
		}

		Space.logRelease( "<-- " + getDateTimeString() );
	}


	/**
	 * Compares the current millis to thisMillis, respecting the MILLI_MARGIN. This should only be used for probing the
	 * time. The actual alarm management has to use the exact values!
	 * 
	 * @param thisMillis
	 * @return -1 if current millis is before thisMillis; +1 if current millis is after thisMillis; 0 if they are
	 *         "equal"
	 */
	public byte compareTo (long thisMillis)
	{
		long t = mCalendar.getTimeInMillis();

		if (t < thisMillis - MILLI_MARGIN)
			return BEFORE;

		if (t > thisMillis + MILLI_MARGIN)
			return AFTER;

		return EQUAL;
	}


	/**
	 * sets the time to trigger the alarm. if it is in the past it will be set to the next occurrence.
	 * 
	 * @param millis
	 *            Millisecond timestamp
	 */
	public void setTriggerTime (long millis)
	{
		mCalendar.setTimeInMillis( millis );
		mCalendar.set( Calendar.SECOND, 0 );

		setNextOccurrence( -1 );
	}


	/**
	 * Sets the trigger time to the hour and minute based on the current time.
	 * 
	 * @param hour
	 * @param minute
	 */
	public void setHourMin (int hour, int minute)
	{
		mCalendar.setTimeInMillis( System.currentTimeMillis() );
		mCalendar.set( Calendar.HOUR_OF_DAY, hour );
		mCalendar.set( Calendar.MINUTE, minute );
		mCalendar.set( Calendar.SECOND, 0 );
		setNextOccurrence( -1 );
	}


	/**
	 * returns only the time of the day as formatted String
	 * 
	 * @return Formatted String
	 */
	public String getTimeString ()
	{
		return SimpleDateFormat.getTimeInstance( SimpleDateFormat.SHORT ).format( mCalendar.getTime() );
	}


	/**
	 * returns the date and time as String
	 * 
	 * @return
	 */
	public String getDateTimeString ()
	{
		return SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT )
				.format( mCalendar.getTime() );
	}


	/* (non-Javadoc)
	 * @see com.gradlspace.cys.Trigger#getConfigString(android.content.Context)
	 */
	public String getConfigString (Context con)
	{
		return new StringBuilder( getTimeString() ).append( "  " ).append( getName() ).toString();
	}


	/**
	 * Calculates the milliseconds to the triggertime
	 * 
	 * @param currentMillis
	 * @return
	 */
	public long getEta (long currentMillis)
	{
		return (mCalendar.getTimeInMillis() - currentMillis);
	}


	/* (non-Javadoc)
	 * @see com.gradlspace.cys.Trigger#getFireString()
	 */
	public String getFireString ()
	{
		return new StringBuilder( 128 ).append( getDateTimeString() ).append( "\n" ).append( getName() ).toString();
	}


	/**
	 * Returns the timestamp for the next time-to-fire.
	 * 
	 * @return millisecond timestamp
	 */
	public long getFireTime ()
	{
		return mCalendar.getTimeInMillis();
	}


	/**
	 * @param con
	 * @return Timestamp [millis] of the start of the prefire window.
	 */
	public long getPreFireTime (Context con)
	{
		return mCalendar.getTimeInMillis() - (Long.parseLong( CyopsString.PREFIRE_TIME.get() ) * 60000);
	}


	/**
	 * @param con
	 * @return String for the alarm trigger window.
	 */
	public String getPrefireWindowString (Context con)
	{
		StringBuilder str = new StringBuilder( 128 );

		t_millisTime = mCalendar.getTimeInMillis();

		str.append( con.getResources().getString( R.string.textTriggerWnd ) )
				.append( "  " )
				.append( DateUtils.formatDateRange( con, t_millisTime
						- (Long.parseLong( CyopsString.PREFIRE_TIME.get() ) * 60000), t_millisTime, DateUtils.FORMAT_SHOW_TIME ) );

		return str.toString();

	}


	@Override
	public void onInit (Context con)
	{
		super.onInit( con );
		setNextOccurrence( -1 );
	}


	/* (non-Javadoc)
	 * @see com.gradlspace.cys.Trigger#retrieveValues(android.content.Context)
	 */
	@Override
	public void retrieveValues (Context con)
	{
		recurrence = Cyops.spref().getString( Cyops.TRIGGER_RECC + id, "daily" );
		setTriggerTime( Cyops.spref().getLong( Cyops.TRIGGER_TIME + id, 0 ) );

		super.retrieveValues( con );
	}


	/* (non-Javadoc)
	 * @see com.gradlspace.spc.Trigger#onFire()
	 */
	@Override
	public synchronized long onFire (Context con, boolean forceFire)
	{
		// check if enabled
		if (this.enabled == false)
			return -1;

		// get timestamps / diff
		long now = System.currentTimeMillis();
		long diff = mCalendar.getTimeInMillis() - now;

		// get prefire time pref
		long preFire = Long.parseLong( CyopsString.PREFIRE_TIME.get() ) * 60000;
		if (preFire <= MILLI_MARGIN)
			preFire = MILLI_MARGIN + 1;

		// check for simple mode
		// if now OR ( force AND within prefire time AND forcefire not occurred yet )
		if (diff < MILLI_MARGIN || (!mHasPrefired && forceFire && diff < preFire && CyopsBoolean.IS_SIMPLE_MODE.get() == false))
		{
			mLastFireTime = now;

			Space.logRelease( "Alarm " + id + " fires @ " + now );

			// aquire authority lock to ensure alarm activity startup
			LockAuthority.acquireSecure();

			// Alarm reached or passed, start alarm activity
			Space.startActivity( null, AlarmActivity.class, Space.ACTION_ALARM, null, id );

			// either this is a forced prefire or a regular timed fire
			// there is only one case where we don't want to set the next occurrence:
			// when two alarms are requested and this was the forced fire
			if (CyopsBoolean.ONLY_ONCE.isNotEnabled() && forceFire)
			{
				// this is the only case where the alarm can repeatedly refire during pretime!
				// So we set a prefire stop boolean. TODO: Re-implement the timeout value?
				mHasPrefired = true;
			}
			else
			{
				// set next occurrence AFTER the occurrence "approaching"
				setNextOccurrence( mCalendar.getTimeInMillis() + MILLI_MARGIN + 70000 );
				// save new values
				persistValues( con, null );
			}

			return 0;
		}
		// if not forced and far away but within prestart time
		else if (!forceFire && diff > 10000 && diff <= Cyops.MILLIS_PRESTART + MILLI_MARGIN)
		{
			// Monitor prestart
			Space.startActivity( null, MonitorActivity.class, Space.ACTION_MONITOR_PRESTART, null, null );
		}

		return diff;
	}


	/* (non-Javadoc)
	 * @see com.gradlspace.cys.Trigger#persistValues(android.content.Context, android.content.SharedPreferences.Editor)
	 */
	@Override
	public void persistValues (Context con, Editor ed)
	{
		if (ed == null)
			ed = Cyops.spref().edit();

		ed.putLong( Cyops.TRIGGER_TIME + id, getFireTime() );
		ed.putString( Cyops.TRIGGER_RECC + id, recurrence );

		super.persistValues( con, ed );
	}


}
