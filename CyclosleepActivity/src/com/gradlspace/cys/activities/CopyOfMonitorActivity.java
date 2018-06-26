/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.gradlspace.cys.AudioHandler;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.SleepStage.SleepPhase;
import com.gradlspace.cys.Space;
import com.gradlspace.cys.TimeTrigger;
import com.gradlspace.cys.TriggerHandler;
import com.gradlspace.widgets.HypnogramView;

import de.lme.plotview.LmeFilter.AccuFilter;
import de.lme.plotview.LmeFilter.ImpDerivativeFilter;
import de.lme.plotview.LmeFilter.MeanFilter;
import de.lme.plotview.LmeFilter.SavGolayFilter;
import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.PlotView;
import de.lme.plotview.SamplingPlot;




/**
 * @author Falling
 * 
 */
public class CopyOfMonitorActivity extends Activity implements SensorEventListener
{

	/**
	 * Receiver for android time updates
	 * 
	 * @author Falling
	 * 
	 */
	class MonitorBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive (Context context, Intent intent)
		{
			if (intent == null)
				return;

			tAction = intent.getAction();
			if (tAction != null && tAction.equals( Intent.ACTION_BATTERY_CHANGED ))
			{
				// if (MonitorActivity.this.lblStatus != null)
				{
					SPTracker.batteryLevel = intent.getIntExtra( BatteryManager.EXTRA_LEVEL, -1 );
					if (SPTracker.batteryLevel == -1)
						SPTracker.batteryLevel = 100;

					SPTracker.batteryTemp = intent.getIntExtra( BatteryManager.EXTRA_TEMPERATURE, -1 );

					if (SPTracker.batteryLevelStart == -1)
						SPTracker.batteryLevelStart = SPTracker.batteryLevel;
				}
			}

			if (CopyOfMonitorActivity.this != null)
				CopyOfMonitorActivity.this.updateStatus();
		}
	}


	public enum Calibrator
	{
		NULL, INIT, EXPLAIN, WAIT_FOR_PREPARE, PLAY_SIGNAL, WAIT_FOR_MOVEMENT, FAILED, FINISHED;
	}


	public static volatile Calibrator		sCalibrationMode		= Calibrator.NULL;
	public static volatile long				sCalibrateNextTime		= -1;
	public static volatile CopyOfMonitorActivity	sThis					= null;

	private boolean							m_forceMonitor			= false;

	public static final int					DIALOG_EXIT				= 1;

	private PlotView						mPlotView				= null;

	private SamplingPlot					mActPlot				= null;
	private SamplingPlot					mActPlotx				= null;
	private SamplingPlot					mActPloty				= null;
	private SamplingPlot					mActPlotz				= null;
	private SamplingPlot					mActPlotUtil			= null;

	private HypnogramView					mHypnoView				= null;
	private TextView						lblStatus				= null;
	private LinearLayout					m_layoutInfo			= null;
	private Button							m_btnInfo				= null;
	private TextView						m_lblInfo				= null;
	private TextView						m_lblPrefire			= null;
	private SeekBar							m_sliderInfo			= null;

	private boolean							mIsDisplayOn			= true;
	private MonitorBroadcastReceiver		mBroadcastReceiver		= null;
	private static IntentFilter				sTimeIntentFilter		= new IntentFilter( Intent.ACTION_TIME_TICK );


	private static IntentFilter				sBatteryIntentFilter	= new IntentFilter( Intent.ACTION_BATTERY_CHANGED );

	private Sensor							mSensor					= null;

	private SensorManager					mSensorMan				= null;


	private transient String				tAction					= null;


	private long							m_gravFilteredMillis	= 0;
	private boolean							m_gravFiltered			= false;

	private long							mNextShortMillis		= 0;
	private long							mNextMediumMillis		= 0;
	private long							mNextLongMillis			= 0;

	private static final long				INTERVAL_SHORT			= 1000;
	private static final long				INTERVAL_MEDIUM			= 6000;
	private static final long				INTERVAL_LONG			= 60000;


	// private transient int mNumPoints = 0;
	// private transient double mAvg = 0d;

	private transient long					t_fireReturn			= -1;


	private transient double				diffOp[]				= new double[ 3 ];

	TimeTrigger								mTimeTrigger			= null;


	private void finalCleanup ()
	{
		stopUI();
		stopReceivers();
		stopSensor();

		if (sCalibrationMode == Calibrator.NULL)
		{
			SPTracker.saveFinalStream();
		}

		AudioHandler.cleanSignal();
	}


	/**
	 * Inits persistent objects.
	 * 
	 * @param nonPersistent
	 * @param semiPersistent
	 * @param persistent
	 */
	private void finalInit ()
	{
		String action = this.getIntent().getAction();
		if (action != null && action.equals( Space.ACTION_MONITOR_CALIBRATE ))
		{
			if (sCalibrationMode == Calibrator.NULL)
				sCalibrationMode = Calibrator.INIT;
		}
		else if (action != null && action.equals( Space.ACTION_MONITOR_PRESTART ))
		{
			sCalibrationMode = Calibrator.NULL;
			SPTracker.isPrestart = true;
		}
		else if (action != null && action.equals( Space.ACTION_MONITOR_FORCE ))
		{
			sCalibrationMode = Calibrator.NULL;
			SPTracker.isPrestart = false;
			m_forceMonitor = true;
		}
		else
		{
			sCalibrationMode = Calibrator.NULL;
			SPTracker.isPrestart = false;
		}


		if (mPlotView != null && mPlotView.getNumPlots() <= 0)
		{
			if (mActPlot == null)
			{
				mActPlot = new SamplingPlot( "Activity", Plot.generatePlotPaint( 1f, 255, 38, 126, 202 ), PlotStyle.LINE,
						16 * 120 );
				mActPlot.setAxis( "t", "s", 1f, "a", "g", 1f );
				mActPlot.setViewport( 60, 30 );

				mActPlotx = new SamplingPlot( "x", Plot.generatePlotPaint( 1f, 255, 192, 192, 192 ), PlotStyle.LINE, 16 * 120 );
				mActPlotx.setViewport( 60, 30 );

				mActPloty = new SamplingPlot( "y", Plot.generatePlotPaint( 1f, 255, 202, 38, 126 ), PlotStyle.LINE, 16 * 120 );
				mActPloty.setViewport( 60, 30 );

				mActPlotz = new SamplingPlot( "z", Plot.generatePlotPaint( 1f, 255, 126, 202, 38 ), PlotStyle.LINE, 16 * 120 );
				mActPlotz.setViewport( 60, 30 );

				mActPlotUtil = new SamplingPlot( "util", Plot.generatePlotPaint( 1f, 255, 128, 216, 216 ), PlotStyle.LINE,
						16 * 120 );
				mActPlotUtil.setViewport( 60, 30 );
			}
			mPlotView.attachPlot( mActPlot );
			mPlotView.attachPlot( mActPlotx );
			mPlotView.attachPlot( mActPloty );
			mPlotView.attachPlot( mActPlotz );
			mPlotView.attachPlot( mActPlotUtil );

			// mPlotView.removeFlag( Flags.DRAW_AXES );
		}


		if (sCalibrationMode != Calibrator.NULL)
		{
			mHypnoView.setEnabled( false );
			mHypnoView.setVisibility( View.GONE );

			m_layoutInfo.setVisibility( View.VISIBLE );

			m_lblInfo.setText( R.string.calibrateIntro );
			m_btnInfo.setText( R.string.textStart );

			m_lblPrefire.setVisibility( View.GONE );
			m_sliderInfo.setVisibility( View.GONE );

			SPTracker.calibrate = true;
		}
		else
		{
			SPTracker.calibrate = false;

			mHypnoView.setEnabled( true );
			mHypnoView.setVisibility( View.VISIBLE );
			m_btnInfo.setText( R.string.textStopMon );

			m_lblInfo.setText( R.string.textPrefireMon );
			m_lblPrefire.setText( CyopsString.PREFIRE_TIME.get() );

			m_sliderInfo.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {

				public void onStopTrackingTouch (SeekBar seekBar)
				{}


				public void onStartTrackingTouch (SeekBar seekBar)
				{}


				public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser)
				{
					CyopsString.PREFIRE_TIME.setInt( progress * 5 );
					m_lblPrefire.setText( CyopsString.PREFIRE_TIME.get() );
					updateStatus();
				}
			} );
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	public void onAccuracyChanged (Sensor sensor, int accuracy)
	{
		// ignored
		Space.log( "acc-changed: " + accuracy );
	}


	public void onClickCalibrate (View v)
	{
		if (sCalibrationMode != Calibrator.NULL)
		{
			switch (sCalibrationMode)
			{
				case FINISHED:
					sCalibrationMode = Calibrator.NULL;
					// Space.spref().edit().putString( "pk_spt_threshold", Float.toString( SPTracker.thresholdMultiplier
					// ) )
					// .commit();
					finish();
					break;

				case INIT:
				case FAILED:
					sCalibrationMode = Calibrator.EXPLAIN;
					break;

				case EXPLAIN:
					finish();
					break;
			}

			changeCalibrationMode( sCalibrationMode );
		}
		else
		{
			showDialog( DIALOG_EXIT );
		}
	}


	public void changeCalibrationMode (Calibrator newMode)
	{
		sCalibrationMode = newMode;

		switch (sCalibrationMode)
		{
			case INIT:
				break;
			case EXPLAIN:
				m_lblInfo.setText( R.string.calibrateExplain );
				m_btnInfo.setText( R.string.textClose );
				break;

			case WAIT_FOR_PREPARE:
				// m_lblInfo.setText( getString( R.string.calibrateWaiting ) + "25" );
				// m_btnInfo.setEnabled( false );
				// m_btnInfo.setVisibility( View.INVISIBLE );
				break;

			case FAILED:
				m_btnInfo.setEnabled( true );
				m_btnInfo.setVisibility( View.VISIBLE );
				m_lblInfo.setText( R.string.calibrateFailed );
				m_btnInfo.setText( R.string.textGo );
				break;

			case FINISHED:
				// SPTracker.thresholdMultiplier = (float) (mAvg * 3 / (double) SPTracker.maxSensorValue);
				//
				// // if (mAvg )
				//
				// if (SPTracker.thresholdMultiplier > 0.5f)
				// SPTracker.thresholdMultiplier = 0.5f;
				// else if (SPTracker.thresholdMultiplier < 0.1f)
				// SPTracker.thresholdMultiplier = 0.1f;
				//
				// m_btnInfo.setEnabled( true );
				// m_btnInfo.setVisibility( View.VISIBLE );
				// Space.log( String.format( "mult: %.2f - max: %d - avg: %.2f",
				// SPTracker.thresholdMultiplier,
				// SPTracker.maxSensorValue,
				// mAvg ) );
				// m_lblInfo.setText( String.format( getString( R.string.calibrateFinish ),
				// SPTracker.thresholdMultiplier ) );
				// m_btnInfo.setText( R.string.textSave );
				break;
		}
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.monitor );

		this.setTitle( this.getString( R.string.app_name ) + " - " + this.getString( R.string.textMonitor ) );

		// try
		// {
		// // set the activity in front of the locked screen
		// getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );
		// }
		// catch (BadTokenException e)
		// {
		// e.printStackTrace();
		// }
		// catch (SecurityException e)
		// {
		// e.printStackTrace();
		// }


		mPlotView = (PlotView) findViewById( R.id.plotMonitor1 );
		mHypnoView = (HypnogramView) findViewById( R.id.plotMonitor2 );

		// mPlotView.removeFlag( Flags.DRAW_X_AXIS );
		// mPlotView.removeFlag( Flags.DRAW_Y_AXIS );

		lblStatus = (TextView) findViewById( R.id.lblMonitorStatus );

		m_lblInfo = (TextView) findViewById( R.id.lblMonInfoText );
		m_lblPrefire = (TextView) findViewById( R.id.lblPrefire );
		m_btnInfo = (Button) findViewById( R.id.btnMonInfoButton );
		m_layoutInfo = (LinearLayout) findViewById( R.id.layoutMonInfo );
		m_sliderInfo = (SeekBar) findViewById( R.id.seekPrefire );

		// ACCEL STREAM
		SPTracker.initTracking();

		finalInit();
	}


	/* (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu) */
	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.monitormenu, menu );
		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy ()
	{
		super.onDestroy();

		this.stopSensor();
		this.stopReceivers();

		if (this.isFinishing())
		{
			Space.log( "Monitor::onDestroy for finish" );
			this.finalCleanup();
			LockAuthority.releaseNormal();
		}
		else
		{
			Space.log( "Monitor::onDestroy for reconfig" );
		}
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();
		// mMonitorView.stopSimulation();
	}


	private static int	s_sensCycle	= 0;


	/* (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem) */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		if (mSensorMan == null)
		{
			mSensorMan = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
		}


		// Handle item selection
		switch (item.getItemId())
		{
		// case R.id.sens_dec:
		// mSensorMan.unregisterListener( this );
		// mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
		// mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_UI );
		//
		// updateStatus();
		// return true;
		//
		// case R.id.sens_inc:
		// mSensorMan.unregisterListener( this );
		// mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
		// mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_FASTEST );
		//
		// updateStatus();
		// return true;
		//
		// case R.id.calibrate:
		// mSensorMan.unregisterListener( this );
		// mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
		// mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_GAME );
		//
		// updateStatus();
		// return true;

			case R.id.help:
				// Space.showToast( this, R.string.helpMonitor );

				mSensorMan.unregisterListener( this );
				mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );

				switch (s_sensCycle)
				{
					case 0:
						mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_GAME );
						s_sensCycle = 1;
						break;

					case 1:
						mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_FASTEST );
						s_sensCycle = 2;
						break;

					default:
					case 2:
						mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_UI );
						s_sensCycle = 0;
						break;
				}

				updateStatus();

				return true;

			default:
				return super.onOptionsItemSelected( item );
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;
		if (id == DIALOG_EXIT)
		{
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textStopMon ).setMessage( R.string.textStopMonQuestion )
					.setPositiveButton( R.string.textYes, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							Space.sleepEnd( false );

							if (SPTracker.requiresRating())
							{
								Intent in = new Intent( CopyOfMonitorActivity.this, RateActivity.class );
								startActivity( in );
							}

							CopyOfMonitorActivity.this.finish();
						}
					} ).setNegativeButton( R.string.textNo, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// do nothing
						}
					} ).create();
		}

		return dialog;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed ()
	{
		if (sCalibrationMode == Calibrator.NULL)
			showDialog( DIALOG_EXIT );
		// super.onBackPressed();
	}


	@Override
	protected void onPause ()
	{
		super.onPause();

		// update next trigger
		mTimeTrigger = TriggerHandler.getNextTimeTrigger( true );

		this.stopUI();

		if (this.isFinishing())
		{
			Space.log( "Monitor::onPause for finish" );
			this.stopSensor();
			this.stopReceivers();

			sCalibrationMode = Calibrator.NULL;

			sThis = null;

			LockAuthority.releaseNormal();
		}
		else
		{
			Space.log( "Monitor::onPause for pause" );

			if (AlarmActivity.sIsRunning)
			{
				Space.log( "finishing monitor for alarm" );
				this.stopSensor();
				this.stopReceivers();
				sCalibrationMode = Calibrator.NULL;
				LockAuthority.releaseNormal();
				this.finish();
			}
		}
	}


	// private long mNext

	@Override
	protected void onResume ()
	{
		super.onResume();

		sThis = this;

		this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );

		mTimeTrigger = TriggerHandler.getNextTimeTrigger( true );

		this.startUI();

		Space.log( "Monitor::onResume" );
	}


	private transient ImpDerivativeFilter	dX		= null;
	private transient ImpDerivativeFilter	dY		= null;
	private transient ImpDerivativeFilter	dZ		= null;

	private transient SavGolayFilter		hX		= new SavGolayFilter( 1 );
	private transient SavGolayFilter		hY		= new SavGolayFilter( 1 );
	private transient SavGolayFilter		hZ		= new SavGolayFilter( 1 );

	// private transient HannFilter hann = new HannFilter();
	private transient AccuFilter			wndint	= new AccuFilter( 16 );
	private transient MeanFilter			mean	= new MeanFilter( 512 );


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged (SensorEvent event)
	{
		// if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		// {
		// get current time

		// abort if stream is invalid
		if (SPTracker.stream == null)
			return;

		if (dX == null)
		{
			// first call, initialize derivative value
			dX = new ImpDerivativeFilter( 0.065f, hX.next( event.values[ 0 ] ) );
			dY = new ImpDerivativeFilter( 0.065f, hY.next( event.values[ 1 ] ) );
			dZ = new ImpDerivativeFilter( 0.065f, hZ.next( event.values[ 2 ] ) );

			return;
		}

		// high-pass-derivative operator
		diffOp[ 0 ] = dX.next( hX.next( event.values[ 0 ] ) );
		diffOp[ 1 ] = dY.next( hY.next( event.values[ 1 ] ) );
		diffOp[ 2 ] = dZ.next( hZ.next( event.values[ 2 ] ) );

		// squaring & hann filter & wnd integrator
		SPTracker.curSensorValue = (int) wndint.next( FloatMath.sqrt( (float) (diffOp[ 0 ] * diffOp[ 0 ] + diffOp[ 1 ]
				* diffOp[ 1 ] + diffOp[ 2 ] * diffOp[ 2 ]) ) );


		// add stream values and update timestamp!!
		SPTracker.stream.add( event.timestamp, event.values[ 0 ], event.values[ 1 ], event.values[ 2 ] );
		SPTracker.millisNow = SPTracker.stream.m_millisNow;


		// calc current time based on event timestamp
		// SPTracker.millisNow = SPTracker.stream.m_milliStart
		// + (long) ( (event.timestamp - SPTracker.stream.m_nanoStart) * 0.000001);

		// don't do any further processing within the first few seconds after start so most of the gravity noise can be
		// removed FIXME
		if (m_gravFiltered == false)
		{
			if (SPTracker.millisNow > m_gravFilteredMillis)
			{
				m_gravFiltered = true;
			}
			else
				return;
		}

		// calc mean value
		SPTracker.meanSensorValue = (float) mean.next( SPTracker.curSensorValue );

		// Space.log( "tt " + SPTracker.millisNow + "  " + event.timestamp );


		// process signal
		SPTracker.onSensorUpdate();


		// display on or calibration mode? then update plots
		if (sCalibrationMode != Calibrator.NULL || (mIsDisplayOn && mPlotView != null && mActPlot != null))
		{
			mActPlot.addValue( SPTracker.curSensorValue, SPTracker.millisNow );

			mActPlotx.addValue( (float) hX.y[ 0 ], SPTracker.millisNow );
			mActPloty.addValue( (float) hY.y[ 0 ], SPTracker.millisNow );
			// mActPlotz.addValue( hY.y[ 0 ], SPTracker.millisNow );
			mActPlotz.addValue( SPTracker.runAct, SPTracker.millisNow );

			mActPlotUtil.addValue( SPTracker.meanSensorValue, SPTracker.millisNow );
		}


		// ==============> Calibration Mode
		if (sCalibrationMode == Calibrator.EXPLAIN)
		{
			if (SPTracker.peaked)
			{
				// AudioHandler.playSignal( this, InternalAlarm.BELL_SOFT );
				AudioHandler.playTone( ToneGenerator.TONE_DTMF_D, 150 );
				SPTracker.peaked = false;
				SPTracker.fireOnNext = false;

				SPTracker.runAct = 0;
			}
		}
		// <=============


		// Log.d( "sensor", String.format( "%d  v %d %d %d", (mMillisNow - mLastMillis), mSensorValue,
		// stream.avg_long[ stream.pos - 1 ], stream.avg_short[ stream.pos - 1 ] ) );


		if (sCalibrationMode == Calibrator.NULL)
		{
			// ==============> short-term processing
			if (SPTracker.millisNow > mNextShortMillis)
			{
				mNextShortMillis = SPTracker.millisNow + INTERVAL_SHORT;

				// process values
				SPTracker.minute -= INTERVAL_SHORT;
				if (SPTracker.minute <= 0)
				{
					// cut ppm
					SPTracker.runAct -= (SPTracker.runAct >>> 2);
					SPTracker.minute = SPTracker.PPM_DURATION;
				}

				SPTracker.transitPhase( SPTracker.millisNow, SPTracker.evaluateValue( SPTracker.runAct ) );
			}
			// <=============


			// ==============> medium-term processing
			if (SPTracker.millisNow > mNextMediumMillis)
			{
				mNextMediumMillis = SPTracker.millisNow + INTERVAL_MEDIUM;

				// check for alarm to wake up to
				if (mTimeTrigger != null && SPTracker.fireOnNext)
				{
					t_fireReturn = mTimeTrigger.onFire( this, true );
					if (t_fireReturn == 0)
					{
						if (sCalibrationMode == Calibrator.NULL)
						{
							SPTracker.saveFinalStream();
							this.finish();
						}
					}
					else if (t_fireReturn < 600000)
					{
						if (SPTracker.minute >= SPTracker.PPM_DURATION * 0.9 && SPTracker.varThreshold > 150)
							SPTracker.varThreshold *= 0.9;
					}
				}
				SPTracker.fireOnNext = false;
			}
			// <=============
		}


		// ==============> long-term processing
		if (SPTracker.millisNow > mNextLongMillis)
		{
			mNextLongMillis = SPTracker.millisNow + INTERVAL_LONG;

			// check battery & temp levels
			if (SPTracker.batteryLevel < 7 || SPTracker.batteryTemp > 630)
			{
				this.stopReceivers();
				this.stopSensor();
				this.stopUI();

				StringBuilder stat = new StringBuilder( 128 );

				if (SPTracker.batteryTemp > 630)
				{
					stat.append( getString( R.string.errorBatteryHigh ) ).append( "\nBattery: " ).append( SPTracker.batteryLevel )
							.append( "% @ " ).append( (SPTracker.batteryTemp * 0.1f) ).append( "° " );
				}
				else
				{
					stat.append( getString( R.string.errorBatteryLow ) ).append( "\nBattery: " ).append( SPTracker.batteryLevel )
							.append( "% @ " ).append( (SPTracker.batteryTemp * 0.1f) ).append( "° " );
				}

				if (lblStatus != null)
				{
					lblStatus.setText( stat.toString() );
				}

				// display error activity
				Space.error( stat.toString() );

				LockAuthority.releaseNormal();

				return;
			}


			if (!m_forceMonitor && sCalibrationMode == Calibrator.NULL)
			{
				if (mTimeTrigger == null || mTimeTrigger.enabled == false
						|| mTimeTrigger.getFireTime() - SPTracker.millisNow > 72000000)
				{
					// next trigger is disabled or too far away (20 hours), we probably got stuck somehow. finish
					// activity
					Space.logRelease( "Lost monitor finish!" );
					this.stopReceivers();
					this.stopSensor();
					this.stopUI();
					LockAuthority.releaseNormal();
					this.finish();
				}
			}

		}
		// <=============
	}


	@Override
	protected void onStart ()
	{
		super.onStart();

		if (Space.sIsWakeCrippledDevice)
		{
			// don't acquire wakelock if we are on a crippled device and this is not the prestart
			if (SPTracker.isPrestart || sCalibrationMode != Calibrator.NULL
					|| CyopsString.CRIPPLE_WORKAROUND.get().equals( "power" ))
				LockAuthority.acquireNormal();
		}
		else
			LockAuthority.acquireNormal();


		this.startSensor();
		this.startReceivers();

		Space.log( "starting Monitor" );
	}


	@Override
	protected void onStop ()
	{
		super.onStop();

		if (m_forceMonitor)
		{
			this.stopSensor();
			this.stopReceivers();
			LockAuthority.releaseNormal();
			this.finish();
		}
	}


	private synchronized void startReceivers ()
	{
		// BATTERY & TIME receiver
		if (mBroadcastReceiver == null)
		{
			mBroadcastReceiver = new MonitorBroadcastReceiver();
			registerReceiver( mBroadcastReceiver, sTimeIntentFilter );
			registerReceiver( mBroadcastReceiver, sBatteryIntentFilter );
		}

		// SPTRACKER
		SPTracker.setOnPhaseChangeListener( new SPTracker.OnSleepPhaseChangeListener() {
			/* (non-Javadoc)
			 * @see com.gradlspace.cys.SPTracker.OnSleepPhaseChangeListener#onPhaseChanged(com.gradlspace.cys.SleepStage.SleepPhase, com.gradlspace.cys.SleepStage.SleepPhase)
			 */
			@Override
			public void onPhaseChanged (SleepPhase from, SleepPhase to)
			{
				if (CopyOfMonitorActivity.this.mHypnoView != null)
					CopyOfMonitorActivity.this.mHypnoView.invalidate();
				super.onPhaseChanged( from, to );
			}

		} );

	}


	private synchronized void startSensor ()
	{
		// SENSOR
		if (mSensor == null)
		{
			if (mSensorMan == null)
			{
				mSensorMan = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
			}


			mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
			// mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_UI );
			mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_GAME );

			m_gravFilteredMillis = System.currentTimeMillis() + 2300;
			m_gravFiltered = false;
		}
	}


	private void startUI ()
	{
		finalInit();

		if (mPlotView != null)
		{
			// UI
			mPlotView.setEnabled( true );

			if (sCalibrationMode == Calibrator.NULL)
			{
				mHypnoView.setEnabled( true );
				mHypnoView.setHypnogram( SPTracker.getHypnogram() );
			}
			else
			{
				changeCalibrationMode( sCalibrationMode );
			}

			mIsDisplayOn = true;

			updateStatus();
		}
	}


	private synchronized void stopReceivers ()
	{
		// BATTERY & TIME receiver
		if (mBroadcastReceiver != null)
		{
			unregisterReceiver( mBroadcastReceiver );
			mBroadcastReceiver = null;
		}

		// SPTRACKER
		SPTracker.setOnPhaseChangeListener( null );
	}


	private synchronized void stopSensor ()
	{
		// SENSOR
		if (mSensor != null)
		{
			if (mSensorMan == null)
			{
				mSensorMan = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
			}

			mSensorMan.unregisterListener( this );
			mSensor = null;

			mSensorMan = null;
		}
	}


	private void stopUI ()
	{
		if (mPlotView != null)
		{
			mPlotView.setEnabled( false );
			mHypnoView.setEnabled( false );
			mIsDisplayOn = false;
		}
	}


	StringBuilder	t_sbStat	= new StringBuilder( 128 );


	public void updateStatus ()
	{
		// STATUS
		if (lblStatus != null && mIsDisplayOn)
		{
			t_sbStat.setLength( 0 );

			t_sbStat.append( getString( R.string.textBattery ) ).append( ": " ).append( SPTracker.batteryLevel ).append( "% @ " )
					.append( (SPTracker.batteryTemp * 0.1f) ).append( "°  " );


			if (sCalibrationMode == Calibrator.NULL)
			{
				if (SPTracker.stream != null)
				{
					t_sbStat.append( String.format( "T[ %d ]", SPTracker.varThreshold ) );

					if (SPTracker.millisNow < SPTracker.millisStartTime)
					{
						t_sbStat.append( String.format( "  TTL[ %d s ]",
														(long) ( (SPTracker.millisStartTime - SPTracker.millisNow) * 0.001) ) );
					}
				}

				if (mTimeTrigger != null)
				{
					t_sbStat.append( "\n" ).append( mTimeTrigger.getPrefireWindowString( this ) );
				}

				// stat.append( "  ^" ).append( maxSensorValue ).append( " " ).append( " ~" ).append( varThreshold );
			}
			else
			{
				t_sbStat.append( "\n" ).append( getString( R.string.textCalibrationMode ) );
			}


			// if (sCalibrationMode)
			//


			// EVENTS
			// String eventtext = "Events: " + mCurSize + " [" + (mCurSize * 12) / 1024 + "kB]";

			lblStatus.setText( t_sbStat.toString() );
		}
	}

}
