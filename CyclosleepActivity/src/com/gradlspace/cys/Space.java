/**
 * 
 */
package com.gradlspace.cys;


import java.io.File;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.TextView;
import android.widget.Toast;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsLong;
import com.gradlspace.cys.activities.AlarmActivity;
import com.gradlspace.cys.activities.CyclosleepActivity;
import com.gradlspace.cys.activities.ErrorActivity;
import com.gradlspace.cys.activities.MonitorActivity;
import com.gradlspace.cys.activities.ViewerActivity;




/**
 * Helper class for organizing all SharedPreferences calls and keeping the default values.
 * 
 * @author Falling
 * 
 */
public class Space
{
	public static final String		TAG							= "cys";

	public static final int			CLIENT						= 34;
	public static final int			ENSLAVED					= 1 << 4;
	public static final int			MASTER						= 1 << 5;

	public static final String		ACTION_ALARM				= "com.gradlspace.cys.alarmcall";
	public static final String		ACTION_QUICKTIMER			= "com.gradlspace.cys.quicktimer";
	public static final String		ACTION_EVENT				= "com.gradlspace.cys.eventcall";
	public static final String		ACTION_MONITOR				= "com.gradlspace.cys.moncall";
	public static final String		ACTION_MONITOR_FORCE		= "com.gradlspace.cys.monforce";
	public static final String		ACTION_MONITOR_PRESTART		= "com.gradlspace.cys.monprestart";
	public static final String		ACTION_MONITOR_CALIBRATE	= "com.gradlspace.cys.moncal";

	public static final String		EXTRA_STRING				= "com.gradlspace.cys.extrastring";
	public static final String		EXTRA_INT					= "com.gradlspace.cys.extraint";

	public static final String		SEND_TYPE_EMAIL				= "message/rfc822";
	public static final String		SEND_TYPE_GENERIC			= "*/*";


	private static final int		NOTIFY_ID					= 1;
	private static Notification		sNotification				= null;
	private static PendingIntent	sNotifyIntent;

	public static boolean			sendReport					= true;

	/** Is device that can't deliver sensor changes without activated screen */
	public static boolean			sIsWakeCrippledDevice		= false;

	private static Context			appContext					= null;

	public static volatile String	logBuffer					= "n/a";

	public static CysInternalData	s_dbData					= null;


	public static Context get ()
	{
		if (appContext == null)
		{
			// serious error
			SystemClock.sleep( 4000 );
			System.exit( 1 );
		}
		return appContext;
	}


	/**
	 * Should be called after any trigger preference update. installs/uninstalls the tick and the notify icon
	 * 
	 * @param con
	 */
	public static void doTriggerUpdate (Context con, boolean silent, boolean disableAll)
	{
		if (disableAll)
		{
			TriggerHandler.disableAllTriggers( con );
			showNotify( con, "del", 1 );
			TriggerHandler.cancelSystemAlarm( con );
			return;
		}

		int num = TriggerHandler.loadTriggers( con );

		if (num <= 0)
		{
			showNotify( con, "del", 1 );
			TriggerHandler.cancelSystemAlarm( con );
			return;
		}

		TriggerHandler.installSystemAlarm();

		sIsNotifyValid = false;


		String bla = con.getString( R.string.textNextAlarm ) + " " + TriggerHandler.getNextTriggerEta( con );
		if (!silent && num > 0)
		{
			showToast( con, bla );
		}

		doNotifyUpdate( con, bla, num );
	}


	private static boolean	sIsNotifyValid	= false;


	/**
	 * Updates the notify area entry
	 * 
	 * @param con
	 * @param str
	 * @param num
	 */
	public static void doNotifyUpdate (Context con, String str, int num)
	{
		if (sIsNotifyValid)
			return;

		if (str == null)
		{
			str = con.getString( R.string.textNextAlarm ) + " " + TriggerHandler.getNextTriggerEta( con );
			num = TriggerHandler.getNumTriggers( true );
		}
		Space.showNotify( con, str, num );

		sIsNotifyValid = true;
	}


	/**
	 * Returns the OS Alarm Service
	 * 
	 * @param con
	 * @return
	 */
	public static AlarmManager getAlarmManager ()
	{
		return (AlarmManager) get().getSystemService( Context.ALARM_SERVICE );
	}


	public static AudioManager getAudioManager (Context con)
	{
		return (AudioManager) con.getSystemService( Context.AUDIO_SERVICE );
	}


	public static PowerManager getPowerManager ()
	{
		return (PowerManager) get().getSystemService( Context.POWER_SERVICE );
	}


	public static Vibrator getVibrator (Context con)
	{
		return (Vibrator) con.getSystemService( Context.VIBRATOR_SERVICE );
	}


	public static WindowManager getWindowManager (Context con)
	{
		return (WindowManager) con.getSystemService( Context.WINDOW_SERVICE );
	}


	/**
	 * Global static class initializers. Should be called ASAP from the main Activity.
	 * 
	 * @param con
	 */
	public static void initSpace (Context con)
	{
		if (appContext != null)
			return;

		appContext = con.getApplicationContext();

		Guardian.setHandler( con );
		LockAuthority.loadFlags();

		s_dbData = new CysInternalData();

		SPTracker.init();
		doTriggerUpdate( get(), true, false );

		sendReport = Cyops.CyopsBoolean.REPORT_BUG.isEnabled();
		// sendReport = spref().getBoolean( "pk_g_report", true );

		AudioHandler.registerMediaScannerListener();


	}


	/**
	 * Makes sure all app-wide static variables that could block normal operations are reset.
	 */
	public static void resetOperations ()
	{
		// MonitorActivity.sCalibrationMode = Calibrator.NULL;
		SPTracker.isPrestart = false;
		// MonitorActivity.sThis = null;
		AlarmActivity.sIsRunning = false;
	}


	/**
	 * So far unused method to hardcore kill the app (due to license problems)
	 * 
	 * @param con
	 * @param reason
	 */
	public static void killForLicense (Context con, String reason)
	{
		doUninstall();

		SystemClock.sleep( 150 );
		Runtime.getRuntime().exit( 1 );
	}


	public static void doUninstall ()
	{
		Space.doTriggerUpdate( get(), false, true );
	}


	public static synchronized void log (String text)
	{
		Log.d( "cys_Debug", text );
		logBuffer = text;
	}


	public static synchronized void logRelease (String text)
	{
		Log.i( TAG, text );
		logBuffer = text;
	}


	/**
	 * Manages the notification icon. update == "del" removes the icon.
	 * 
	 * @param con
	 * @param update
	 * @param number
	 */
	public static synchronized void showNotify (Context con, String update, int number)
	{
		NotificationManager notificationManager = (NotificationManager) con.getSystemService( Context.NOTIFICATION_SERVICE );

		if ( (update != null && update.equals( "del" )) || Cyops.CyopsBoolean.PERSISTENT_ICON.isNotEnabled())
		{
			notificationManager.cancel( NOTIFY_ID );
		}
		else
		{

			if (sNotification == null)
			{
				Intent notificationIntent = new Intent( con, CyclosleepActivity.class );
				sNotifyIntent = PendingIntent.getActivity( con, 0, notificationIntent, 0 );

				sNotification = new Notification( R.drawable.ic_launcher, "Cyclosleep - active", System.currentTimeMillis() );
				sNotification.flags |= Notification.FLAG_NO_CLEAR;
				sNotification.flags |= Notification.FLAG_ONGOING_EVENT;
			}

			if (sNotification != null)
			{
				sNotification.number = number;
				sNotification.setLatestEventInfo( con, con.getString( R.string.textNotifyActive ), update, sNotifyIntent );
			}

			notificationManager.notify( NOTIFY_ID, sNotification );
		}
	}


	/**
	 * Displays an toast notification with short duration.
	 * 
	 * @param text
	 *            Text to show
	 */
	public static void showToast (Context con, String text)
	{
		showToast( con, text, Toast.LENGTH_SHORT );
	}


	public static void showToast (Context con, int resId)
	{
		showToast( con, con.getString( resId ), Toast.LENGTH_LONG );
	}


	public static void showHint (Context con, int resId)
	{
		if (CyopsBoolean.SHOW_HINTS.get() == false)
			return;

		showToast( con, con.getString( resId ), Toast.LENGTH_LONG );
	}


	/**
	 * Displays an toast notification with the specified duration.
	 * 
	 * @param con
	 * @param text
	 * @param duration
	 */
	public static void showToast (Context con, String text, int duration)
	{
		logRelease( text );

		if (Activity.class.isInstance( con ))
		{
			LayoutInflater inflater = (LayoutInflater) con.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			View layout = inflater.inflate( R.layout.toast_layout,
											(ViewGroup) ((Activity) con).findViewById( R.id.toast_layout_root ) );

			// ImageView image = (ImageView) layout.findViewById( R.id.image );
			// image.setImageResource( R.drawable.android );
			TextView lblText = (TextView) layout.findViewById( R.id.text );
			lblText.setText( text );

			Toast toast = new Toast( con );
			toast.setGravity( Gravity.CENTER, 0, 0 );
			toast.setDuration( duration );
			toast.setView( layout );
			toast.show();
		}
		else
			Toast.makeText( con, text, duration ).show();
	}


	public static boolean isAirplaneModeOn ()
	{
		return Settings.System.getInt( Space.get().getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, 0 ) != 0;
	}


	public static void setAirplaneMode (boolean status)
	{
		boolean isAirplaneModeOn = isAirplaneModeOn();

		// Editor ed = Space.spref().edit();

		if ( (isAirplaneModeOn && status) || (!isAirplaneModeOn && !status))
		{
			// ed.putBoolean( PREF_APM_WAS_SET, false );
			CyopsBoolean.APMODE_WAS_SET.set( false );
		}
		else if (isAirplaneModeOn && !status)
		{
			// ed.putBoolean( PREF_APM_WAS_SET, false );
			CyopsBoolean.APMODE_WAS_SET.set( false );

			// s_apThread = new Thread( null, new Space.AirplaneTask( false ), "AirplaneModeOffTask" );
			// s_apThread.start();

			Settings.System.putInt( Space.get().getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, 0 );
			Intent intent = new Intent( android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED );
			intent.putExtra( "state", false );
			Space.get().sendBroadcast( intent );
		}
		else if (!isAirplaneModeOn && status)
		{
			// ed.putBoolean( PREF_APM_WAS_SET, true );
			CyopsBoolean.APMODE_WAS_SET.set( true );

			// s_apThread = new Thread( null, new Space.AirplaneTask( true ), "AirplaneModeOnTask" );
			// s_apThread.start();

			Settings.System.putInt( Space.get().getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, 1 );
			Intent intent = new Intent( android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED );
			intent.putExtra( "state", true );
			Space.get().sendBroadcast( intent );
		}

		// ed.commit();
	}


	/**
	 * Called on end of sleep, when an alarm is fired
	 * 
	 * @param con
	 */
	public static synchronized void sleepEnd (boolean calibrationMode)
	{
		if (CyopsBoolean.SLEEPING.get() == false)
			return;

		// restore settings
		// Editor ed = Space.spref().edit();
		// ed.putBoolean( PREF_SLEEPING, false );
		CyopsBoolean.SLEEPING.set( false );

		// ==============================================
		// == disable auto screen rotation if requested and save restore value
		// ====>
		if (CyopsBoolean.AUTOROT.get() && CyopsBoolean.AUTOROT_WAS_SET.get() == true
				&& Settings.System.getInt( Space.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1 ) == 0)
		{
			Settings.System.putInt( Space.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1 );
			// ed.putBoolean( PREF_AR_WAS_SET, false );
			CyopsBoolean.AUTOROT_WAS_SET.set( false );
		}
		// <====
		// ==============================================

		if (!calibrationMode)
		{
			// ==============================================
			// == AIRPLANE MODE: disable airplane mode if requested
			// ====>
			if (CyopsBoolean.APMODE_WAS_SET.get() == true)
			{
				setAirplaneMode( false );
			}
			// <====
			// ==============================================
		}

		// ed.commit();
	}


	public static class AirplaneTask implements Runnable
	{
		private boolean	m_enable	= false;


		public AirplaneTask (boolean enable)
		{
			m_enable = enable;
		}


		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run ()
		{
			Settings.System.putInt( Space.get().getContentResolver(),
									android.provider.Settings.System.AIRPLANE_MODE_ON,
									m_enable ? 1 : 0 );
			Intent intent = new Intent( android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED );
			intent.putExtra( "state", m_enable );
			Space.get().sendBroadcast( intent );
		}

	}


	// private static Thread s_apThread = null;


	/**
	 * Called on pressing "SleepNow" or when sleep is detected
	 * 
	 * @param con
	 */
	public static synchronized void sleepStart (boolean calibrationMode)
	{
		// Editor ed = Space.spref().edit();
		// ed.putBoolean( PREF_SLEEPING, true );
		CyopsBoolean.SLEEPING.set( true );

		// disable auto screen rotation if requested and save restore value
		if (CyopsBoolean.AUTOROT.get()
				&& Settings.System.getInt( Space.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1 ) == 1)
		{
			Settings.System.putInt( Space.get().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0 );
			// ed.putBoolean( PREF_AR_WAS_SET, true );
			CyopsBoolean.AUTOROT_WAS_SET.set( true );
		}
		else
			// ed.putBoolean( PREF_AR_WAS_SET, false );
			CyopsBoolean.AUTOROT_WAS_SET.set( false );


		if (!calibrationMode)
		{
			// enable airplane mode if requested
			if (CyopsBoolean.APMODE.get())
			{
				setAirplaneMode( true );
			}
		}

		// save current timestamp
		// ed.putLong( "pk_sm_starttime", System.currentTimeMillis() );
		CyopsLong.SM_START_TIME.set( System.currentTimeMillis() );


		// ed.commit();

		Space.showHint( Space.get(), R.string.textSleepStart );
	}


	public static int startActivity (Context con, Class< ? > cls)
	{
		return startActivity( con, cls, null, null, null );
	}


	/**
	 * Start the intentAction activity.
	 * 
	 * @param con
	 * @param intentAction
	 * @param paramString
	 * @param paramLong
	 * @return
	 */
	public static synchronized int startActivity (Context con, Class< ? > cls, String intentAction, String paramString,
			Integer paramInt)
	{
		int res = 0;

		// if an alarm is running, we don't start ANYTHING
		if (AlarmActivity.sIsRunning)
		{
			return 1;
		}

		Intent in = new Intent( get(), cls );

		// add params to intent
		if (paramInt != null)
			in.putExtra( EXTRA_INT, paramInt );
		if (paramString != null)
			in.putExtra( EXTRA_STRING, paramString );
		if (intentAction != null)
			in.setAction( intentAction );


		if (cls == AlarmActivity.class)
		{
			con = get();
			AlarmActivity.sIsRunning = true;
			in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		}
		else if (cls == MonitorActivity.class)
		{
			// if (MonitorActivity.sThis != null)
			// return 1;

			con = get();
			in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		}

		if (con == null)
			con = get();


		try
		{
			con.startActivity( in );
		}
		catch (ActivityNotFoundException e)
		{
			e.printStackTrace();
			error( e.getLocalizedMessage() );
			res = 2;
		}
		catch (BadTokenException e)
		{
			e.printStackTrace();
			res = 3;
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
			res = 4;
		}


		return res;
	}


	/**
	 * Start email or generic send activity.
	 * 
	 * @param type
	 * @param subject
	 * @param text
	 * @param attachPath
	 * @return
	 */
	public static int startSendActivity (Context con, String type, String toAddress, String subject, String text,
			String attachPath)
	{
		Intent i = new Intent( Intent.ACTION_SEND );
		i.setType( type );

		// recipient given?
		if (toAddress != null)
		{
			i.putExtra( Intent.EXTRA_EMAIL, new String[] { toAddress } );
		}

		// subject & text
		if (subject != null)
			i.putExtra( Intent.EXTRA_SUBJECT, subject );
		if (text != null)
			i.putExtra( Intent.EXTRA_TEXT, text );

		// attach a file?
		if (attachPath != null)
		{
			if (CyopsBoolean.DATA_ZIP.isEnabled())
			{
				attachPath = FileManager.compressFile( attachPath );
			}

			i.putExtra( Intent.EXTRA_STREAM, Uri.fromFile( new File( attachPath ) ) );
		}

		try
		{
			// Intent.createChooser( i, "Send crash report?" ) not working?
			con.startActivity( i );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 1;
		}

		return 0;
	}


	public static int startViewerActivity (Context con, String filepath)
	{
		if (filepath == null || con == null)
			return 2;

		File f = new File( filepath );
		if (f == null || f.isFile() == false)
			return 2;

		Intent i = new Intent( Intent.ACTION_VIEW, Uri.fromFile( f ), Space.get(), ViewerActivity.class );
		try
		{
			// Intent.createChooser( i, "Send crash report?" ) not working?
			con.startActivity( i );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return 1;
		}

		return 0;
	}


	/**
	 * Starts the error (report) activity
	 * 
	 * @param con
	 * @param text
	 */
	public static void error (String text)
	{
		Intent in = new Intent( get(), ErrorActivity.class );
		in.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP ).putExtra( EXTRA_STRING, text );
		get().startActivity( in );
	}


	public static void test (Context con)
	{
		Space.startActivity( con, AlarmActivity.class, Space.ACTION_ALARM, null, 0 );
		// Space.setFirstStart( con, true );
		// Space.setEulaAccepted( con, false );
	}


	/**
	 * Protect constructor since this is a static only class.
	 */
	protected Space ()
	{}
}
