/**
 * 
 */
package com.gradlspace.cys.activities;


import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.gradlspace.cys.AudioHandler;
import com.gradlspace.cys.Cyops;
import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.Space;
import com.gradlspace.cys.TriggerHandler;

import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.Plot2D;
import de.lme.plotview.PlotView;
import de.lme.plotview.PlotView.Flags;




/**
 * @author Falling
 * 
 */
public class AlarmActivity extends Activity implements TextToSpeech.OnInitListener
{
	public static final int			RATE_SLEEP			= 1;

	public static volatile boolean	sIsRunning			= false;

	private String					mAction				= null;

	private TextView				m_lblClock			= null;

	private BroadcastReceiver		mTimeReceiver		= new TimeTickReceiver();
	private static IntentFilter		sTimeIntentFilter	= new IntentFilter( Intent.ACTION_TIME_TICK );


	/**
	 * AlarmActivity's internal CountDownTimer implementation that is started when the activity is called with
	 * ACTION_ALARM intent. It aquires a wakelock for dim screen and wakeup and counts down to the pref-set timeout.
	 * During each ticks it increases mediaplayer volume or vibration, depending on user settings.
	 * 
	 * @author Stefan Gradl
	 * 
	 */
	class AlarmTicker extends CountDownTimer
	{

		private WakeLock		mWakeLock		= null;
		private Context			mContext		= null;
		private long			mStartMillis	= 0;
		transient private long	mPassedMillis	= 0;

		private int				mTriggerId		= -1;

		protected TextToSpeech	mTts			= null;
		protected boolean		mTtsAnnounce	= false;
		protected boolean		mTtsMessage		= false;
		protected long			mTtsInterval	= 0;

		protected boolean		m_playSounds	= true;

		private boolean			doRiot			= true;


		/**
		 * @param millisInFuture
		 * @param countDownInterval
		 */
		public AlarmTicker (Context con, int triggerId)
		{
			super( Long.parseLong( CyopsString.ALARM_TIMEOUT.get() ), 1000 );

			Space.log( "aticker init" );

			mTriggerId = triggerId;

			mContext = con;
			mStartMillis = Long.parseLong( CyopsString.ALARM_TIMEOUT.get() );
			mTtsAnnounce = CyopsBoolean.SPEECH_ATIME.get();

			m_playSounds = AudioHandler.confirmLoud( con );

			doRiot = CyopsBoolean.ALARM_RIOT.isEnabled();

			// only enable message speaking if there is a message
			if (TriggerHandler.getTrigger( mTriggerId ).text != null)
				mTtsMessage = CyopsBoolean.SPEECH_MSG.get();

			mTtsInterval = Long.parseLong( CyopsString.SPEECH_INTERVAL.get() ) * 60000;
			if (mTtsInterval < 1000)
				mTtsInterval = 30000;

			// create wakelock object for our alarm timer with screen on and wakeup flags
			mWakeLock = Space.getPowerManager().newWakeLock(	PowerManager.SCREEN_DIM_WAKE_LOCK
																		| PowerManager.ACQUIRE_CAUSES_WAKEUP,
																getClass().getName() );
			mWakeLock.acquire();
		}


		public void cleanup ()
		{
			if (mContext != null)
			{
				AudioHandler.vibrate( mContext, 0 );
				AudioHandler.stopSong();

				AudioHandler.cleanSignal();
			}
			if (mTts != null)
			{
				mTts.shutdown();
				mTts = null;
			}
			if (mWakeLock != null)
			{
				mWakeLock.release();
				mWakeLock = null;
			}
			mContext = null;
		}


		/* (non-Javadoc)
		 * @see android.os.CountDownTimer#onFinish()
		 */
		@Override
		public void onFinish ()
		{
			Space.log( "finish" );
			cleanup();
		}


		/* (non-Javadoc)
		 * @see android.os.CountDownTimer#onTick(long)
		 */
		@Override
		public void onTick (long millisUntilFinished)
		{
			mPassedMillis = mStartMillis - millisUntilFinished;

			try
			{
				Space.log( "mils: " + millisUntilFinished + "  -  " + (mStartMillis * 0.25) );

				// riot?
				if (doRiot && millisUntilFinished < mStartMillis * 0.25)
				{
					if (millisUntilFinished < mStartMillis * 0.14)
					{
						AudioHandler.playTone( ToneGenerator.TONE_DTMF_D, 800 );
					}

					AudioHandler.startSong( mContext, "riot", 0.5f, false );
				}

				// check TTS
				if (mTts != null && m_playSounds)
				{
					if (mPassedMillis > 10000 && mPassedMillis % mTtsInterval < 1000)
					{
						if (mTtsMessage)
						{
							mTts.speak( TriggerHandler.getTrigger( mTriggerId ).text, TextToSpeech.QUEUE_ADD, null );
						}

						if (mTtsAnnounce)
						{
							Time t = new Time();
							t.setToNow();
							mTts.speak( t.format( mContext.getString( R.string.prefSpeechTextFormat ) ),
										TextToSpeech.QUEUE_ADD,
										null );
						}
					}
				}

				// check vibrate timeout
				if (mPassedMillis > 360000)
				{
					AudioHandler.vibrate( mContext, 0 );
				}

				// increase volume
				else if (mPassedMillis > 1000)
				{
					// increase volume , start vibrations
					AudioHandler.setVolume( mContext, (mPassedMillis * 0.000005f) );

					if (mPassedMillis > 120000)
						AudioHandler.vibrate( mContext, (int) (mPassedMillis * 0.01) );
				}
			}
			catch (NullPointerException e)
			{
				Space.log( "NullPointer in AlarmTicker::onTick" );
				e.printStackTrace();
			}

		}

	}


	/**
	 * The AlarmTicker class instance, there should always be only one alarm running, so it's static. The instance runs
	 * in a separate thread.
	 */
	private static AlarmTicker	mTimer	= null;


	class TimeTickReceiver extends BroadcastReceiver
	{
		private transient String	tAction	= null;


		@Override
		public void onReceive (Context context, Intent intent)
		{
			if (intent == null)
				return;

			tAction = intent.getAction();
			if (tAction != null && tAction.equals( Intent.ACTION_TIME_TICK ) && AlarmActivity.this.m_lblClock != null)
			{
				AlarmActivity.this.m_lblClock.setText( DateUtils.formatDateTime(	context,
																					System.currentTimeMillis(),
																					DateUtils.FORMAT_SHOW_TIME ) );
			}
		}
	}


	/**
	 * Dismiss the alarm committing the associated trigger. Stops the sounds and the vibrator.
	 * 
	 * @param force
	 *            if set to true everything will canceled
	 * @return true if caller should continue finishing the activity or not
	 */
	public synchronized boolean dismiss (boolean force)
	{
		Space.log( "Alarm dismiss @ " + force );

		if (!force)
		{
			String str = CyopsString.ALARM_COMMIT.get();

			if (!str.equals( "none" ))
			{
				showDialog( 0 );
				return false;
			}
		}

		AudioHandler.vibrate( this, 0 );
		AudioHandler.stopSong();

		if (mTimer != null)
		{
			mTimer.cancel();
			mTimer.cleanup();
			mTimer = null;
		}


		if (!force && !this.isFinishing())
		{
			if (rateSleep() == false)
				return false;
		}


		// // close monitor
		// if (MonitorActivity.sThis != null)
		// {
		// MonitorActivity.sThis.finish();
		// }


		// the alarm only really ends if true is returned
		AlarmActivity.sIsRunning = false;

		try
		{
			// perform cleanup task
			if (CyopsBoolean.DATA_AUTO_CLEAN.isEnabled())
			{
				FileManager.workThread = new Thread( new Runnable() {

					public void run ()
					{
						FileManager.purgeOldFiles( AlarmActivity.this );
					}
				} );
				FileManager.workThread.start();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return true;
	}


	public boolean rateSleep ()
	{
		if (mAction != null && mAction.equals( Space.ACTION_ALARM ))
		{
			// only rate if there was some sleep
			if (SPTracker.requiresRating())
			{
				Intent in = new Intent( this, RateActivity.class );
				// in.putExtra( AlarmEditActivity.EXTRA_ALARM_ID, mApref2.getAlarmId() );
				startActivityForResult( in, RATE_SLEEP );
				return false;
			}
			else
			{
				SPTracker.save();
			}
		}

		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == RATE_SLEEP)
		{
			finish();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}


	public void onClickDismiss (View v)
	{
		if (dismiss( false ))
			finish();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed ()
	{
		if (dismiss( false ))
			super.onBackPressed();
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		AlarmActivity.sIsRunning = true;

		setContentView( R.layout.alarm );

		m_lblClock = (TextView) findViewById( R.id.lblClock );

		try
		{
			// set the alarm activity in front of the locked screen
			getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );
			// this.requestWindowFeature( featureId )

			// getWindow().addFlags( WindowManager.LayoutParams.TYPE_SYSTEM_ALERT );
			// TODO: check behavior
		}
		catch (BadTokenException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}

		mAction = this.getIntent().getAction();
	}


	@Override
	protected void onDestroy ()
	{
		dismiss( true );
		super.onDestroy();
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();
	}


	@Override
	protected void onPause ()
	{
		if (mTimeReceiver != null)
		{
			unregisterReceiver( mTimeReceiver );
		}

		if (this.isFinishing())
		{
			dismiss( true );
		}
		super.onPause();
	}


	@Override
	protected void onResume ()
	{
		try
		{
			// set static screen orientation so alarm will never be restarted unintentionally based on screen rotations
			this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );
			// TODO: check if there are other "easy" config changes that restart the activity
			// FIXME: produces exceptions, somehow when display is rotated while activity is currently starting


			if (m_lblClock != null)
				m_lblClock.setText( DateUtils.formatDateTime( this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME ) );

			if (mTimeReceiver != null)
			{
				registerReceiver( mTimeReceiver, sTimeIntentFilter );
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		super.onResume();
	}


	@Override
	protected void onStart ()
	{
		super.onStart();

		// don't start again if alarm is already running. e.g. if the user pressed the home button and returned.
		if (mTimer != null)
		{
			return;
		}

		final TextView lblMain = (TextView) findViewById( R.id.lblAlarmLarge );
		final TextView lblMed = (TextView) findViewById( R.id.lblAlarmMedium );

		// why are we called?
		if (mAction != null && mAction.equals( Space.ACTION_ALARM ))
		{
			// set alarm timer
			mTimer = new AlarmTicker( this, (int) this.getIntent().getIntExtra( Space.EXTRA_INT, 0 ) );

			// end sleep / reactivate radios, etc
			Space.sleepEnd( false );

			// start music
			AudioHandler.startSong( this, TriggerHandler.getTrigger( mTimer.mTriggerId ).getAlarmSound(), 0.005f, true );

			lblMain.setText( TriggerHandler.getTrigger( mTimer.mTriggerId ).getFireString() );
			lblMed.setText( TriggerHandler.getTrigger( mTimer.mTriggerId ).text );

			// prepare speech engine?
			if (mTimer.mTtsAnnounce || mTimer.mTtsMessage)
			{
				mTimer.mTts = new TextToSpeech( this, this );
			}

			// init done - start alarm
			mTimer.start();

			// alarm started, make sure TriggerHandlers wakelock is released
			LockAuthority.releaseSecure();
		}

		else if (mAction != null && mAction.equals( Space.ACTION_QUICKTIMER ) == true)
		{
			// acquire and release a wakelock to turn the screen on
			WakeLock wakeLock = Space.getPowerManager().newWakeLock(	PowerManager.FULL_WAKE_LOCK
																				| PowerManager.ACQUIRE_CAUSES_WAKEUP
																				| PowerManager.ON_AFTER_RELEASE,
																		getClass().getName() );
			wakeLock.acquire( 12000 );

			// QUICK TIMER
			AudioHandler.startSong( this, Cyops.getTriggerSound( -1 ), 0.15f, true );
			AudioHandler.vibrate( this, 4000 );

			lblMed.setText( R.string.textQuickTimerAlert );
			lblMain.setText( R.string.textQuickTimerInfo );
		}
	}


	@Override
	protected void onStop ()
	{
		super.onStop();
	}


	/* (non-Javadoc)
	 * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
	 */
	public void onInit (int status)
	{
		if (status == TextToSpeech.SUCCESS)
		{
			// mTts.setLanguage( Locale.GERMAN );
		}
		else
		{
			Log.e( "AlarmActivity", "TTS init error." );
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;

		if (id == 0)
		{
			// test dialog
			dialog = new Dialog( this );
			dialog.setContentView( R.layout.alarm_dismiss );
			dialog.setTitle( R.string.textAlarmDismiss );


			String str = CyopsString.ALARM_COMMIT.get();

			final SeekBar seeker = ((SeekBar) dialog.findViewById( R.id.sliderAlarm ));
			final PlotView plotView = ((PlotView) dialog.findViewById( R.id.plotViewCount ));
			final TextView lblMath = ((TextView) dialog.findViewById( R.id.lblAlarmMath ));
			final Spinner spinnerAlarm = ((Spinner) dialog.findViewById( R.id.spinnerAlarm ));

			if (str.equals( "slider" ))
			{
				seeker.setMax( 10 );
				seeker.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

					public void onStopTrackingTouch (SeekBar seekBar)
					{}


					public void onStartTrackingTouch (SeekBar seekBar)
					{}


					public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser)
					{
						if (progress >= 9)
						{
							AlarmActivity.this.dismissDialog( 0 );
							AlarmActivity.this.rateSleep();
							if (AlarmActivity.this.dismiss( true ))
								AlarmActivity.this.finish();
						}
					}
				} );

				seeker.setVisibility( View.VISIBLE );
				plotView.setVisibility( View.GONE );
				lblMath.setVisibility( View.GONE );
				spinnerAlarm.setVisibility( View.GONE );
			}
			else if (str.equals( "count" ))
			{
				seeker.setVisibility( View.GONE );
				plotView.setVisibility( View.VISIBLE );
				lblMath.setVisibility( View.GONE );
				spinnerAlarm.setVisibility( View.VISIBLE );

				plotView.removeFlag( Flags.DRAW_AXES );
				plotView.removeFlag( Flags.ENABLE_GESTURES );
				// plotView.removeFlag( Flags.DRAW_GRID );
				final Plot2D plot = new Plot2D( "Count", Plot.generatePlotPaint(), PlotStyle.RECT_VALUE_FILLED, 32 );
				// plot.setAxis( "t", "s", 1f, "a", "g", 1f );
				plotView.attachPlot( plot );

				// plot.setViewport( 100, 100, 200, 200 );

				ArrayAdapter< CharSequence > adapter = new ArrayAdapter< CharSequence >( this,
						android.R.layout.simple_spinner_item );
				adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
				spinnerAlarm.setAdapter( adapter );

				Random rnd = new Random();
				final int randsize = 4 + rnd.nextInt( 12 );
				for (int i = 1; i <= randsize; ++i)
				{
					plot.addValue( Color.RED, 50 + rnd.nextInt( 150 ), 50 + rnd.nextInt( 150 ) );
				}

				int upper = 2 + rnd.nextInt( 5 );
				for (int i = randsize - 2 - rnd.nextInt( 5 ); i < randsize + upper; ++i)
				{
					adapter.add( Integer.toString( i ) );
				}

				spinnerAlarm.setOnItemSelectedListener( new OnItemSelectedListener() {

					public void onItemSelected (AdapterView< ? > parent, View view, int pos, long arg3)
					{
						// Space.log( "pos " + pos + "   " + randsize );
						// if (pos + 1 == randsize)
						if (Integer.toString( randsize ).equals( parent.getItemAtPosition( pos ).toString() ))
						{
							AlarmActivity.this.dismissDialog( 0 );
							AlarmActivity.this.rateSleep();
							if (AlarmActivity.this.dismiss( true ))
								AlarmActivity.this.finish();
						}
					}


					public void onNothingSelected (AdapterView< ? > arg0)
					{}
				} );


			}
			else if (str.equals( "math" ))
			{
				seeker.setVisibility( View.GONE );
				plotView.setVisibility( View.GONE );
				lblMath.setVisibility( View.VISIBLE );
				spinnerAlarm.setVisibility( View.VISIBLE );

				Random rnd = new Random();
				final int a = 3 + rnd.nextInt( 120 );
				final int b = 4 + rnd.nextInt( 250 );
				final int res = a + b;

				lblMath.setText( String.format( "%d + %d = ?", a, b ) );

				ArrayAdapter< CharSequence > adapter = new ArrayAdapter< CharSequence >( this,
						android.R.layout.simple_spinner_item );
				adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
				spinnerAlarm.setAdapter( adapter );

				int upper = 2 + rnd.nextInt( 5 );
				for (int i = res - 2 - rnd.nextInt( 5 ); i < res + upper; ++i)
				{
					adapter.add( Integer.toString( i ) );
				}

				spinnerAlarm.setOnItemSelectedListener( new OnItemSelectedListener() {

					public void onItemSelected (AdapterView< ? > parent, View view, int pos, long arg3)
					{
						// Space.log( "pos " + pos + "   " + randsize );
						if (Integer.toString( res ).equals( parent.getItemAtPosition( pos ).toString() ))
						{
							AlarmActivity.this.dismissDialog( 0 );
							AlarmActivity.this.rateSleep();
							if (AlarmActivity.this.dismiss( true ))
								AlarmActivity.this.finish();
						}
					}


					public void onNothingSelected (AdapterView< ? > arg0)
					{}
				} );
			}
			else
			{
				if (dismiss( true ))
					finish();
			}


		}

		return dialog;
	}

}
