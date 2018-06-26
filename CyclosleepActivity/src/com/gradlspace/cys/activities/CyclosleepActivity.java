package com.gradlspace.cys.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import com.gradlspace.cys.Cyops;
import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsInt;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.FauLink;
import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.Guardian;
import com.gradlspace.cys.LicenseManager;
import com.gradlspace.cys.LicenseManager.StaticLicenseCheckerCallback;
import com.gradlspace.cys.LicenseManager.StaticReturnCode;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.Space;
import com.gradlspace.cys.TimeTrigger;
import com.gradlspace.cys.TriggerHandler;
import com.gradlspace.widgets.TimeEdit;




/**
 * Colors: #19273B -> #37769A
 * 
 * @author Falling
 * 
 */
public class CyclosleepActivity extends Activity
{


	private class CysLicenseCheckerCallback extends StaticLicenseCheckerCallback
	{
		public void allow (int reason)
		{
			super.allow( reason );

			// Should allow user access.
			displayResult( StaticReturnCode.OK, "license: ok" );
		}


		public void applicationError (int errorCode)
		{
			super.applicationError( errorCode );

			// This is a polite way of saying the developer made a mistake
			// while setting up or calling the license checker library.
			// Please examine the error code and fix the error.
			String result = String.format( getString( R.string.application_error ), errorCode );
			displayResult( StaticReturnCode.ERROR, result );
		}


		public void dontAllow (int reason)
		{
			super.dontAllow( reason );

			displayResult( StaticReturnCode.DENIED, "license: denied" );
			// Should not allow access. In most cases, the app should assume
			// the user has access unless it encounters this. If it does,
			// the app should inform the user of their unlicensed ways
			// and then either shut down the app or limit the user to a
			// restricted set of features.
			// In this example, we show a dialog that takes the user to Market.
		}
	}

	/**
	 * CountDownTimer override to handle our ui widgets and a wakelock
	 * 
	 * @author Falling
	 * 
	 */
	class QuickUiTimer extends CountDownTimer
	{

		transient private TextView		mLabel		= null;


		transient private Button		mButton		= null;


		transient private TimePicker	mPicker		= null;


		private WakeLock				mWakeLock	= null;
		public boolean					isRunning	= false;


		/**
		 * @param millisInFuture
		 * @param countDownInterval
		 */
		public QuickUiTimer (long millisInFuture, long countDownInterval)
		{
			super( millisInFuture, countDownInterval );
		}


		public void changeState (boolean start)
		{
			if (mLabel == null)
			{
				Log.e( Space.TAG, "Can't change QuickUiTimer state!" );
				return;
			}

			isRunning = start;

			if (start)
			{
				mWakeLock = Space.getPowerManager().newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, getClass().getName() );
				mWakeLock.acquire();
				mButton.setText( R.string.textStop );
				mPicker.setEnabled( false );
			}
			else
			{
				release();
				mButton.setText( R.string.textStart );
				mLabel.setText( R.string.textQuickTimer );
				mPicker.setEnabled( true );
			}
		}


		/* (non-Javadoc)
		 * @see android.os.CountDownTimer#onFinish()
		 */
		@Override
		public void onFinish ()
		{
			Space.startActivity( null, AlarmActivity.class, Space.ACTION_QUICKTIMER, null, 0 );
			isRunning = false;

			if (mLabel != null)
			{
				mButton.setText( R.string.textStart );
				mLabel.setText( R.string.textQuickTimer );
				mPicker.setEnabled( true );
				release();
			}
		}


		/* (non-Javadoc)
		 * @see android.os.CountDownTimer#onTick(long)
		 */
		@Override
		public void onTick (long millisUntilFinished)
		{
			if (mLabel != null)
			{
				mPicker.setCurrentHour( (int) (millisUntilFinished / 3600000) );
				mPicker.setCurrentMinute( (int) ( (millisUntilFinished % 3600000) / 60000) );
				mLabel.setText( getResources().getText( R.string.textQuickTimer ) + " ["
						+ DateUtils.formatElapsedTime( (long) (millisUntilFinished * 0.001) ) + "]" );
			}
		}


		/**
		 * releases the wakelock, if acquired
		 */
		public void release ()
		{
			if (mWakeLock != null)
			{
				mWakeLock.release();
				mWakeLock = null;
			}
		}


		/**
		 * Updates the internal references to the display widgets
		 * 
		 * @param label
		 * @param button
		 * @param picker
		 * @param start
		 *            true to aquire wakelock, false to release it
		 */
		public void updateWidgets (TextView label, Button button, TimePicker picker)
		{
			mLabel = label;
			mButton = button;
			mPicker = picker;
		}
	}


	/**
	 * Receiver for android time updates
	 * 
	 * @author Falling
	 * 
	 */
	class TimeTickReceiver extends BroadcastReceiver
	{
		private int	m_deadCount	= 0;


		@Override
		public void onReceive (Context context, Intent intent)
		{
			PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
			if (!pm.isScreenOn())
			{
				if (m_deadCount > 1)
					CyclosleepActivity.this.unregisterReceiver( this );
				else
					++m_deadCount;
			}
			else
				m_deadCount = 0;

			if (intent == null)
				return;


			tAction = intent.getAction();
			if (tAction != null && tAction.equals( Intent.ACTION_BATTERY_CHANGED ))
			{
				// if (MonitorActivity.this.lblStatus != null)
				{
					mBatteryLevel = intent.getIntExtra( BatteryManager.EXTRA_LEVEL, -1 );
					if (mBatteryLevel == -1)
						mBatteryLevel = 100;

					mBatteryTemp = intent.getIntExtra( BatteryManager.EXTRA_TEMPERATURE, -1 );

					mBatteryHealth = intent.getIntExtra( BatteryManager.EXTRA_HEALTH, -1 );

					// mBatteryIsPlugged = (intent.getIntExtra( BatteryManager.EXTRA_PLUGGED, 0 ) != 0);

					mBatteryVoltage = intent.getIntExtra( BatteryManager.EXTRA_VOLTAGE, 1 );

					// mBatteryIconId = intent.getIntExtra( BatteryManager.EXTRA_ICON_SMALL, -1 );
				}

				// just update our widgets displaying time to triggers
				updateUi();
			}
			else if (tAction != null && tAction.equals( Intent.ACTION_TIME_TICK ))
			{
				Space.doNotifyUpdate( CyclosleepActivity.this, null, 0 );

				// just update our widgets displaying time to triggers
				updateUi();
			}


		}
	}


	private static final int	DIALOG_LICENSE			= 0;

	private static final int	DIALOG_LICENSE_ERROR	= 1;
	private static final int	DIALOG_ALARM			= 2;
	private static final int	DIALOG_ASK_MONITOR		= 3;
	private static final int	DIALOG_EXIT				= 4;
	private static final int	DIALOG_CRIPPLE			= 5;


	// A handler on the UI thread.
	private Handler				mHandler				= null;


	private String				mError					= "null";
	private TextView			mLblStatus				= null;
	private SeekBar				mSeekerPreset			= null;


	protected TimeEdit			mTxtTime				= null;
	protected EditText			mTxtHours				= null;


	private static QuickUiTimer	mTimer					= null;


	private BroadcastReceiver	mTimeReceiver			= null;
	private static IntentFilter	sTimeIntentFilter		= new IntentFilter( Intent.ACTION_TIME_TICK );


	private static IntentFilter	sBatteryIntentFilter	= new IntentFilter( Intent.ACTION_BATTERY_CHANGED );
	private transient String	tAction					= null;
	private ProgressBar			mBarStats				= null;
	private Drawable			m_barDrawableNormal		= null;
	private Drawable			m_barDrawableRed		= null;
	private ToggleButton		mBtnSleepNow			= null;

	private LinearLayout		m_frmFaup				= null;
	private CheckBox			m_btnFaupEnabled		= null;
	private TextView			m_lblFaupStatus			= null;
	private Button				m_btnFaupDispatch		= null;

	private TextView			mLblActiveTrigger		= null;
	private TextView			mLblMainStats			= null;

	private TextView			mLblNextTrigger			= null;
	private int					mBatteryLevel			= 100;
	private int					mBatteryLastUsage		= 25;
	private int					mBatteryTemp			= 0;
	private int					mBatteryHealth			= BatteryManager.BATTERY_HEALTH_UNKNOWN;
	// private int mBatteryIconId = 0;
	private int					mBatteryVoltage			= 1;


	// private static boolean m_isFirstStart = false;


	private void displayResult (final StaticReturnCode code, final String result)
	{
		if (mHandler == null)
			return;

		mHandler.post( new Runnable() {
			public void run ()
			{
				mError = result;
				// mStatusText.setText(result);

				Space.log( result );
				setProgressBarIndeterminateVisibility( false );

				// mCheckLicenseButton.setEnabled(true);

				// if (!CyclosleepActivity.this.isFinishing())
				// {
				// if (code == StaticReturnCode.DENIED)
				// {
				// showDialog( CyclosleepActivity.DIALOG_LICENSE );
				// }
				// else if (code == StaticReturnCode.ERROR)
				// {
				// showDialog( CyclosleepActivity.DIALOG_LICENSE_ERROR );
				// }
				// else
				// {
				// if (notify_license)
				// {
				// Space.showToast( CyclosleepActivity.this, R.string.license_ok );
				// notify_license = false;
				// }
				// }
				// }
			}
		} );
	}


	public void exitApp ()
	{
		stopQuickTimer();
		Space.doUninstall();
		updateUi();
		finish();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1 && resultCode == RESULT_CANCELED)
		{
			this.finish();
		}
		super.onActivityResult( requestCode, resultCode, data );
	};


	public void onClickAlarmButton (View v)
	{
		if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
		{
			Space.showToast( this, R.string.unlicensed_dialog_title );
			return;
		}

		if (mTxtTime != null)
		{
			mTxtTime.clearFocus();
			mTxtHours.clearFocus();
			Time t = mTxtTime.getTime();
			TriggerHandler.setAlarm( this, 0, t.hour, t.minute );
			updateUi();
		}
	}


	public void onClickFm1 (View v)
	{
		if ( ((ToggleButton) v).isChecked())
		{
			android.provider.Settings.System.putInt(	Space.get().getContentResolver(),
														android.provider.Settings.System.AIRPLANE_MODE_ON,
														1 );

			Intent intent = new Intent( android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED );
			intent.putExtra( "state", new Boolean( true ) );
			// con.sendBroadcast( intent );
			PendingIntent pIntent = PendingIntent.getBroadcast( Space.get(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT );
			try
			{
				pIntent.send();
				// con.sendStickyBroadcast( intent );
			}
			catch (CanceledException e)
			{
				e.printStackTrace();
				Space.error( "Airplane Mode Intent Cancelled\n" + e.getMessage() );
			}
		}
		else
		{
			android.provider.Settings.System.putInt(	Space.get().getContentResolver(),
														android.provider.Settings.System.AIRPLANE_MODE_ON,
														0 );

			Intent intent = new Intent( android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED );
			intent.putExtra( "state", new Boolean( false ) );
			// con.sendBroadcast( intent );
			PendingIntent pIntent = PendingIntent.getBroadcast( Space.get(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT );
			try
			{
				pIntent.send();
				// con.sendStickyBroadcast( intent );
			}
			catch (CanceledException e)
			{
				e.printStackTrace();
				Space.error( "Airplane Mode Intent Cancelled\n" + e.getMessage() );
			}
		}
	}


	public void onClickFm2 (View v)
	{
		if ( ((ToggleButton) v).isChecked())
		{
			Space.setAirplaneMode( true );
		}
		else
		{
			Space.setAirplaneMode( false );
		}
	}


	private static Thread	s_apThread	= null;


	public void onClickFm3 (View v)
	{
		if ( ((ToggleButton) v).isChecked())
		{
			s_apThread = new Thread( null, new Space.AirplaneTask( true ), "AirplaneModeToggleTask" );
			s_apThread.start();
		}
		else
		{
			s_apThread = new Thread( null, new Space.AirplaneTask( false ), "AirplaneModeToggleTask" );
			s_apThread.start();
		}

	}


	public void onClickFm5 (View v)
	{
		if ( ((ToggleButton) v).isChecked())
		{
			android.provider.Settings.System.putInt( Space.get().getContentResolver(), "airplane_mode_on", 1 );

			Intent intent = new Intent( "android.intent.action.AIRPLANE_MODE" );
			intent.putExtra( "state", new Boolean( true ) );
			// con.sendBroadcast( intent );
			PendingIntent pIntent = PendingIntent.getBroadcast( Space.get(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT );
			try
			{
				pIntent.send();
				// con.sendStickyBroadcast( intent );
			}
			catch (CanceledException e)
			{
				e.printStackTrace();
				Space.error( "Airplane Mode Intent Cancelled\n" + e.getMessage() );
			}
		}
		else
		{
			android.provider.Settings.System.putInt( Space.get().getContentResolver(), "airplane_mode_on", 0 );

			Intent intent = new Intent( "android.intent.action.AIRPLANE_MODE" );
			intent.putExtra( "state", new Boolean( false ) );
			// con.sendBroadcast( intent );
			PendingIntent pIntent = PendingIntent.getBroadcast( Space.get(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT );
			try
			{
				pIntent.send();
				// con.sendStickyBroadcast( intent );
			}
			catch (CanceledException e)
			{
				e.printStackTrace();
				Space.error( "Airplane Mode Intent Cancelled\n" + e.getMessage() );
			}
		}
	}


	public void onClickQuickTimer (View v)
	{
		((ViewSwitcher) findViewById( R.id.switcherTimer )).showNext();
	}


	public void onClickQuickTimerPreset (View v)
	{
		long timerCd = 10000;

		if (v == this.findViewById( R.id.btnQuickTimer2min ))
		{
			timerCd = 120000;
		}
		else if (v == this.findViewById( R.id.btnQuickTimer10min ))
		{
			timerCd = 600000;
		}
		else if (v == this.findViewById( R.id.btnQuickTimer15min ))
		{
			timerCd = 900000;
		}
		else if (v == this.findViewById( R.id.btnQuickTimer30min ))
		{
			timerCd = 1800000;
		}

		if (mTimer != null)
		{
			mTimer.release();
			mTimer.cancel();
			mTimer = null;
		}

		// set up the quick timer
		mTimer = new QuickUiTimer( timerCd, 1000 );
		mTimer.updateWidgets(	(TextView) findViewById( R.id.lblQuickTimer ),
								(Button) findViewById( R.id.btnQuickTimer ),
								(TimePicker) findViewById( R.id.timeQuickTimer ) );
		mTimer.changeState( true );
		mTimer.start();
	}


	public void onClickFaupEnable (View v)
	{
		if ( ((CheckBox) v).isChecked())
		{
			CyopsBoolean.FAUP_ENABLED.set( true );
			FauLink.update();
		}
		else
		{
			CyopsBoolean.FAUP_ENABLED.set( false );
		}
		updateUi();
	}


	public void onClickFaupDispatch (View v)
	{

	}


	public void onClickSleepNow (View v)
	{
		if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
		{
			Space.showToast( this, R.string.unlicensed_dialog_title );
			return;
		}

		// Space.test( this );
		if ( ((ToggleButton) v).isChecked())
		{

			if (Space.sIsWakeCrippledDevice)
			{
				Space.showToast(	this,
									"This device has a bugged firmware. Please consult the FAQ for additional information!",
									Toast.LENGTH_LONG );
			}


			// String str = Space.spref().getString( "pk_sm_action", "ask" );
			String str = CyopsString.SM_ACTION.get();

			// check for the next fire time, if its prestart is in the past, start the monitor in prestart mode
			// immediately
			TimeTrigger tt = TriggerHandler.getNextTimeTrigger( true );
			if (tt != null)
			{
				// No, just ask as well...
				// if (System.currentTimeMillis() >= tt.getFireTime() - Space.sPrestartMillis)
				// {
				// Space.startActivity( getApplicationContext(),
				// Space.ACTION_MONITOR_PRESTART,
				// null,
				// 0 );
				// return;
				// }
				// else
				{
					if (CyopsBoolean.FAUP_ENABLED.isEnabled())
					{
						// Faup enabled supersedes other options since we always want to track the entire night
						Space.sleepStart( false );
						Space.startActivity( this, MonitorActivity.class, Space.ACTION_MONITOR, null, 0 );
					}
					else
					{
						if (str.equals( "ask" ))
						{
							showDialog( DIALOG_ASK_MONITOR );
							return;
						}

						Space.sleepStart( false );

						if (str.equals( "mon" ))
						{
							Space.startActivity( this, MonitorActivity.class, Space.ACTION_MONITOR, null, 0 );
						}
						else if (str.equals( "hide" ))
						{
							finish();
						}
						else if (str.equals( "none" ))
						{
							// none
						}
					}
				}
			}
			else
			{
				Space.showHint( this, R.string.textNeedAlarms );
				((ToggleButton) v).setChecked( false );
			}
		}
		else
			Space.sleepEnd( false );

	}


	public void onClickTimerButton (View v)
	{
		TimePicker picker = (TimePicker) findViewById( R.id.timeQuickTimer );

		if (mTimer == null)
		{
			picker.clearFocus();

			// set up the quick timer
			long timerCd = picker.getCurrentHour() * 3600000L;
			timerCd += picker.getCurrentMinute() * 60000L;

			if (timerCd <= 0)
			{
				timerCd = 30000;
			}
			mTimer = new QuickUiTimer( timerCd, 1000 );
		}

		if (mTimer.isRunning == false)
		{

			mTimer.updateWidgets(	(TextView) findViewById( R.id.lblQuickTimer ),
									(Button) findViewById( R.id.btnQuickTimer ),
									picker );
			mTimer.changeState( true );
			mTimer.start();
		}
		else
		{
			stopQuickTimer();
		}

	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		// load basic settings
		Space.initSpace( this );

		// set contentview
		requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
		setContentView( R.layout.main );

		// LicenseManager.resetCache();

		mHandler = new Handler();

		mLblNextTrigger = (TextView) findViewById( R.id.lblNextTrigger );
		mLblActiveTrigger = (TextView) findViewById( R.id.lblActiveTriggers );
		mBtnSleepNow = (ToggleButton) findViewById( R.id.btnSleepNow );

		mBarStats = (ProgressBar) findViewById( R.id.progressBarStats );

		m_barDrawableNormal = getResources().getDrawable( R.drawable.progress_horizontal );
		mBarStats.setProgressDrawable( m_barDrawableNormal );
		m_barDrawableRed = getResources().getDrawable( R.drawable.progress_horizontal_red );

		mLblStatus = (TextView) findViewById( R.id.lblStats );
		mLblMainStats = (TextView) findViewById( R.id.lblMainStats );

		mTxtTime = (TimeEdit) findViewById( R.id.txtMainAlarm );
		mTxtHours = (EditText) findViewById( R.id.txtMainHours );
		mSeekerPreset = (SeekBar) findViewById( R.id.seekerMainAlarm );

		mTxtTime.setHourSlave( mTxtHours );
		mTxtTime.setSeekerSlave( mSeekerPreset );

		m_frmFaup = (LinearLayout) findViewById( R.id.frameFaup );
		m_btnFaupEnabled = (CheckBox) findViewById( R.id.btnFaupEnabled );
		m_lblFaupStatus = (TextView) findViewById( R.id.lblFaupStatus );
		m_btnFaupDispatch = (Button) findViewById( R.id.btnFaupDispatch );


		// ==============> Quick Timer
		if (CyopsBoolean.QUICKTIMER.isNotEnabled())
		{
			findViewById( R.id.frameTimer ).setVisibility( View.GONE );
		}
		else
		{
			TimePicker picker = (TimePicker) findViewById( R.id.timeQuickTimer );
			picker.setIs24HourView( true );
			picker.setCurrentHour( 0 );
			picker.setCurrentMinute( 5 );
		}
		// <=============
	}


	private boolean	notify_license	= false;


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;
		if (id == DIALOG_LICENSE)
		{
			// no valid license
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.unlicensed_dialog_title )
					.setMessage( R.string.unlicensed_dialog_body )
					.setPositiveButton( R.string.textBuy, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// start market
							Intent marketIntent = new Intent( Intent.ACTION_VIEW, Uri
									.parse( "http://market.android.com/details?id=" + getPackageName() ) );
							startActivity( marketIntent );
							// CyclosleepActivity.mLastLicenseCheck = -1;
							exitApp();
						}
					} ).setNegativeButton( R.string.textExit, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// FIXME
							exitApp();
						}
					} ).setNeutralButton( R.string.textRetry, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							CyclosleepActivity.this.setProgressBarIndeterminateVisibility( true );
							notify_license = true;
							LicenseManager.checkLicense( new CysLicenseCheckerCallback() );
						}
					} ).setCancelable( false ).create();
		}
		else if (id == DIALOG_LICENSE_ERROR)
		{
			// problems with license checking
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textError )
					.setMessage( mError + "\n" + getString( R.string.textErrorSendReport ) )
					.setPositiveButton( R.string.textReport, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// delete notify
							Guardian.dispatchReport( "License Error: " + mError );
							exitApp();
						}
					} ).setNeutralButton( R.string.textExit, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// delete notify
							// Guardian.dispatchReport( "License Error: " + mError );
							exitApp();
						}
					} ).setCancelable( false ).create();
		}
		else if (id == DIALOG_ALARM)
		{
			// test dialog
			dialog = new Dialog( this );
			dialog.setContentView( R.layout.alarm_edit );
			dialog.setTitle( "Custom Dialog" );

		}
		else if (id == DIALOG_ASK_MONITOR)
		{
			// SLEEP NOW ask dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textDialogAskMonitorTitle )
					.setMessage( R.string.textDialogAskMonitor )
					.setPositiveButton( R.string.textYes, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							Space.sleepStart( false );
							Space.startActivity( CyclosleepActivity.this, MonitorActivity.class, Space.ACTION_MONITOR, null, 0 );
						}
					} ).setNeutralButton( R.string.textNo, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							Space.sleepStart( false );
							CyclosleepActivity.this.updateUi();
							// CyclosleepActivity.this.finish();
						}
					} ).setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// do nothing
							((ToggleButton) CyclosleepActivity.this.findViewById( R.id.btnSleepNow )).setChecked( false );
						}
					} ).create();
		}
		else if (id == DIALOG_EXIT)
		{
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textExit ).setMessage( R.string.textReallyExit )
					.setPositiveButton( R.string.textYes, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							exitApp();
						}
					} ).setNeutralButton( R.string.textClose, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							CyclosleepActivity.this.finish();
						}
					} ).setNegativeButton( R.string.textBack, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// do nothing
						}
					} ).create();
		}
		else if (id == DIALOG_CRIPPLE)
		{
			// problems with license checking
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textError ).setMessage( R.string.errorCrippleDevice )
					.setNeutralButton( R.string.textOk, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							Space.startActivity( CyclosleepActivity.this, IntroActivity.class );
						}
					} ).create();
		}

		return dialog;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.mainmenu, menu );
		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy ()
	{
		LicenseManager.unregisterCallback();

		super.onDestroy();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.lasthypno:
				if (Space.startViewerActivity( this, FileManager.getLastHypnoFile( this, 0 ) ) != 0)
				{
					Space.showToast( this, R.string.errorNoFile );
				}
				return true;

			case R.id.monitor:
				if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
				{
					Space.showToast( this, R.string.unlicensed_dialog_title );
					return true;
				}
				Space.startActivity( this, MonitorActivity.class, Space.ACTION_MONITOR_FORCE, null, 0 );
				return true;

			case R.id.viewer:
				if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
				{
					Space.showToast( this, R.string.unlicensed_dialog_title );
					return true;
				}
				Space.startActivity( this, SleepLogActivity.class );
				return true;

			case R.id.stats:
				if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
				{
					Space.showToast( this, R.string.unlicensed_dialog_title );
					return true;
				}
				Space.startActivity( this, StatsActivity.class );
				return true;

			case R.id.disableall:
				Space.doTriggerUpdate( this, false, true );
				this.updateUi();
				return true;

			case R.id.options:
				if (LicenseManager.sLastReturnCode == StaticReturnCode.DENIED)
				{
					Space.showToast( this, R.string.unlicensed_dialog_title );
					return true;
				}
				Space.startActivity( this, OptionsActivity.class );
				return true;

			case R.id.about:
				Space.startActivity( this, AboutActivity.class );
				return true;

			case R.id.exit:
				showDialog( DIALOG_EXIT );

				return true;

			case R.id.help:
				startActivity( new Intent( Intent.ACTION_VIEW ).setData( Uri.parse( "http://gradlspace.com/cys/faq" ) ) );

				// Space.startActivity( this, Space.ACTION_ALARM, null, 3 );
				// Space.showHint( this, R.string.textHelpMain );
				// Space.getPowerManager( this )
				// .newWakeLock( PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
				// "cys.lock.global" ).acquire( 5000 );
				return true;

			default:
				return super.onOptionsItemSelected( item );
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause ()
	{
		if (mTimeReceiver != null)
		{
			unregisterReceiver( mTimeReceiver );
			mTimeReceiver = null;
		}

		super.onPause();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume ()
	{
		super.onResume();

		// since we are here, we can assume all operations are offline
		Space.resetOperations();


		if (CyopsBoolean.EULA_ACCEPTED.get() == false)
		{
			// delete notify
			Space.showNotify( this, "del", 0 );

			// show eula activity
			this.startActivityForResult(	new Intent( Space.get(), EulaActivity.class )
													.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP ),
											1 );
			return;
		}

		if (mTimeReceiver == null)
		{
			mTimeReceiver = new TimeTickReceiver();
		}
		// registerReceiver( mTimeReceiver, mTimeIntentFilter );
		registerReceiver( mTimeReceiver, sTimeIntentFilter );
		registerReceiver( mTimeReceiver, sBatteryIntentFilter );


		if (CyopsBoolean.EULA_ACCEPTED.get())
		{
			if (CyopsBoolean.IS_FIRST_START.get())
			{
				// set first start static.
				// m_isFirstStart = true;

				// if (Space.sIsWakeCrippledDevice)
				// showDialog( DIALOG_CRIPPLE );
				// else
				// {
				// if first start and eula accepted > display intro
				Space.startActivity( this, IntroActivity.class );
				return;
				// }
			}
		}


		if (!LockAuthority.isValid())
		{
			Space.startActivity( this, DeviceTestActivity.class );
			return;
		}

		// license check
		if ( (Space.CLIENT & Space.MASTER) == Space.MASTER)
		{
			if (!LicenseManager.s_checkInProgress)
			{
				// this.setProgressBarIndeterminateVisibility( true );
				// this.setp
				LicenseManager.checkLicense( new CysLicenseCheckerCallback() );
			}
		}

		mBatteryLastUsage = CyopsInt.BATT_USAGE.get();

		// set alarm time edit control
		if (mTxtTime != null)
		{
			Time t = new Time();
			t.set( ((TimeTrigger) TriggerHandler.getTrigger( 0 )).getFireTime() );
			mTxtTime.setTime( t, null );
		}

		// set avg sleep duration
		if (mLblMainStats != null)
		{
			mLblMainStats.setText( String.format( "%.2f h", SPTracker.avgSleepHours ) );
		}

		// check if the next alarm is > 12h, and disable sleep mode (if enabled)
		TimeTrigger tt = TriggerHandler.getNextTimeTrigger( true );
		if (tt == null || tt.getEta( System.currentTimeMillis() ) > Cyops.MILLIS_12H)
		{
			Space.sleepEnd( false );
		}

		updateUi();

		// make sure to remove any locks that may persisted from somewhere
		LockAuthority.releaseNormal();
		LockAuthority.releaseSecure();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart ()
	{
		super.onStart();

		notify_license = false;

		if (CyopsBoolean.FAUP_ENABLED.isEnabled())
		{
			FauLink.update();
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop ()
	{
		super.onStop();

		// this.dismissDialog( DIALOG_LICENSE );
		// this.dismissDialog( DIALOG_LICENSE_ERROR );
	}


	/**
	 * Stops the quick timer
	 */
	public void stopQuickTimer ()
	{
		if (mTimer != null)
		{
			mTimer.cancel();
			mTimer.updateWidgets(	(TextView) findViewById( R.id.lblQuickTimer ),
									(Button) findViewById( R.id.btnQuickTimer ),
									(TimePicker) findViewById( R.id.timeQuickTimer ) );
			mTimer.changeState( false );
			mTimer = null;
		}
	}


	/**
	 * Update all UI components that require repeating updates
	 */
	public void updateUi ()
	{
		if (mLblNextTrigger != null)
		{
			mLblNextTrigger.setText( TriggerHandler.getNextTriggerEta( this ) );
		}

		if (mLblActiveTrigger != null)
			mLblActiveTrigger.setText( Integer.toString( TriggerHandler.getNumTriggers( true ) ) );


		if (mLblStatus != null)
		{
			StringBuilder stat = new StringBuilder( 128 );
			stat.append( mBatteryLevel ).append( "% | " ).append( (mBatteryTemp * 0.1f) ).append( "° | " )
					.append( String.format( "%.3f", mBatteryVoltage * 0.001 ) ).append( " V | " )
					.append( mBatteryHealth == BatteryManager.BATTERY_HEALTH_GOOD ? "ok" : ":-(" ).append( " | [-" )
					.append( mBatteryLastUsage ).append( "%]" );

			mLblStatus.setText( stat.toString() );

			if (mBarStats != null)
			{
				mBarStats.setMax( 100 );
				mBarStats.setProgress( mBatteryLevel - mBatteryLastUsage );
				mBarStats.setSecondaryProgress( mBatteryLevel );

				if (mBatteryLevel - mBatteryLastUsage < 6)
				{
					if (mBarStats.getProgressDrawable() == m_barDrawableNormal)
					{
						mBarStats.setProgressDrawable( m_barDrawableRed );
					}
				}
				else
				{
					if (mBarStats.getProgressDrawable() == m_barDrawableRed)
					{
						mBarStats.setProgressDrawable( m_barDrawableNormal );
					}
				}
			}

			// animate battery icon (disabled)
			//
			// if (mImageBattery != null)
			// {
			// try
			// {
			// mImageBattery.setImageResource( mBatteryIconId );
			// mImageBattery.setImageLevel( mBatteryLevel );
			// // if (mBatteryIconId > 0)
			// // {
			// // LevelListDrawable batteryLevel = (LevelListDrawable) getResources().getDrawable(
			// // mBatteryIconId );
			// // batteryLevel.setLevel( mBatteryLevel );
			// //
			// // mImageBattery.setBackgroundDrawable( batteryLevel );
			// // }
			// }
			// catch (NotFoundException e)
			// {
			// }
			// catch (NullPointerException e)
			// {
			//
			// }
			// }
		}


		// set button status based on saved prefs
		if (mBtnSleepNow != null)
		{
			mBtnSleepNow.setChecked( CyopsBoolean.SLEEPING.isEnabled() );
			if (CyopsBoolean.IS_SIMPLE_MODE.isEnabled())
			{
				mBtnSleepNow.setEnabled( false );
			}
		}

		if (mTimer != null)
			mTimer.updateWidgets(	(TextView) findViewById( R.id.lblQuickTimer ),
									(Button) findViewById( R.id.btnQuickTimer ),
									(TimePicker) findViewById( R.id.timeQuickTimer ) );


		// faup
		if (CyopsBoolean.SCIENCE_ENABLED.isEnabled())
		{
			m_frmFaup.setVisibility( View.VISIBLE );

			if (CyopsBoolean.FAUP_ENABLED.isEnabled())
			{
				m_btnFaupEnabled.setChecked( true );
				m_lblFaupStatus.setVisibility( View.VISIBLE );

				// status text
				m_lblFaupStatus.setText( FauLink.getStatusText() );

				// upload button
				if (FauLink.isReadyForUpload())
				{
					m_btnFaupDispatch.setVisibility( View.VISIBLE );
				}
				else
				{
					m_btnFaupDispatch.setVisibility( View.GONE );
				}
			}
			else
			{
				m_btnFaupEnabled.setChecked( false );
				m_lblFaupStatus.setVisibility( View.GONE );
				m_btnFaupDispatch.setVisibility( View.GONE );
			}
		}
		else
		{
			m_frmFaup.setVisibility( View.GONE );
		}
	}
}