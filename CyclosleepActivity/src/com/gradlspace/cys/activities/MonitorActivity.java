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
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.gradlspace.cys.BatteryTracker;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SensorData;
import com.gradlspace.cys.SensorProcessor;
import com.gradlspace.cys.Space;

import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.PlotView;
import de.lme.plotview.SamplingPlot;




/**
 * @author Falling
 * 
 */
public class MonitorActivity extends Activity implements SensorEventListener
{
	public static final int				DIALOG_EXIT			= 1;

	private PlotView					plotViewMain		= null;
	private SamplingPlot				plotMain			= null;

	private TextView					lblStatus			= null;

	private boolean						isDisplayOn			= true;
	private MonitorBroadcastReceiver	brMonitor			= null;
	private static IntentFilter			intentFilterTime	= new IntentFilter( Intent.ACTION_TIME_TICK );
	private static IntentFilter			intentFilterBattery	= new IntentFilter( Intent.ACTION_BATTERY_CHANGED );

	private Sensor						sensor				= null;
	private SensorManager				sensorManager		= null;

	private transient String			strAction			= null;
	StringBuilder						sbStat				= new StringBuilder( 128 );

	private static int					sensorCycle			= 0;


	private/**
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

			strAction = intent.getAction();
			if (strAction != null && strAction.equals( Intent.ACTION_BATTERY_CHANGED ))
			{
				BatteryTracker.batteryLevel = intent.getIntExtra( BatteryManager.EXTRA_LEVEL, -1 );
				if (BatteryTracker.batteryLevel == -1)
					BatteryTracker.batteryLevel = 100;

				BatteryTracker.batteryTemp = intent.getIntExtra( BatteryManager.EXTRA_TEMPERATURE, -1 );

				if (BatteryTracker.batteryLevelStart == -1)
					BatteryTracker.batteryLevelStart = BatteryTracker.batteryLevel;

			}

			if (MonitorActivity.this != null)
				MonitorActivity.this.updateUI();
		}
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.monitor );

		this.setTitle( this.getString( R.string.app_name ) + " - " + this.getString( R.string.textMonitor ) );

		plotViewMain = (PlotView) findViewById( R.id.plotMonitor1 );
		lblStatus = (TextView) findViewById( R.id.lblMonitorStatus );

		startUI();

		if (SensorProcessor.stream == null)
			SensorProcessor.stream = new SensorData();
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
			stopUI();
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


	/* (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem) */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		if (sensorManager == null)
		{
			sensorManager = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
		}


		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.help:
				// Space.showToast( this, R.string.helpMonitor );

				sensorManager.unregisterListener( this );
				sensor = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );

				switch (sensorCycle)
				{
					case 0:
						sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_GAME );
						sensorCycle = 1;
						break;

					case 1:
						sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_FASTEST );
						sensorCycle = 2;
						break;

					default:
					case 2:
						sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_UI );
						sensorCycle = 0;
						break;
				}

				updateUI();

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
							// Space.sleepEnd( false );
							//
							// if (SPTracker.requiresRating())
							// {
							// Intent in = new Intent( CopyOfMonitorActivity.this, RateActivity.class );
							// startActivity( in );
							// }

							MonitorActivity.this.finish();
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
		showDialog( DIALOG_EXIT );
		// super.onBackPressed();
	}


	@Override
	protected void onPause ()
	{
		super.onPause();

		this.stopUI();

		if (this.isFinishing())
		{
			Space.log( "Monitor::onPause for finish" );
			this.stopSensor();
			this.stopReceivers();
			LockAuthority.releaseNormal();
		}
	}


	// private long mNext

	@Override
	protected void onResume ()
	{
		super.onResume();

		this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );

		this.startUI();

		Space.log( "Monitor::onResume" );
	}


	@Override
	protected void onStart ()
	{
		super.onStart();

		LockAuthority.acquireNormal();

		this.startSensor();
		this.startReceivers();

		Space.log( "starting Monitor" );
	}


	@Override
	protected void onStop ()
	{
		super.onStop();

		this.stopSensor();
		this.stopReceivers();

		LockAuthority.releaseNormal();
		this.finish();
	}


	private synchronized void startReceivers ()
	{
		// BATTERY & TIME receiver
		if (brMonitor == null)
		{
			brMonitor = new MonitorBroadcastReceiver();
			registerReceiver( brMonitor, intentFilterTime );
			registerReceiver( brMonitor, intentFilterBattery );
		}
	}


	private synchronized void startSensor ()
	{
		// SENSOR
		if (sensor == null)
		{
			if (sensorManager == null)
			{
				sensorManager = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
			}


			sensor = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
			// sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_UI );
			sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_FASTEST );
		}
	}


	private void startUI ()
	{
		if (plotViewMain != null && plotViewMain.getNumPlots() <= 0)
		{
			if (plotMain == null)
			{
				plotMain = new SamplingPlot( "Activity", Plot.generatePlotPaint( 1f, 255, 38, 126, 202 ), PlotStyle.LINE,
						16 * 120 );
				plotMain.setAxis( "t", "s", 1f, "a", "g", 1f );
				plotMain.setViewport( 60, 30 );

			}
			plotViewMain.attachPlot( plotMain );

			// mPlotView.removeFlag( Flags.DRAW_AXES );
		}


		if (plotViewMain != null)
		{
			// UI
			plotViewMain.setEnabled( true );
			isDisplayOn = true;

			updateUI();
		}
	}


	private synchronized void stopReceivers ()
	{
		// BATTERY & TIME receiver
		if (brMonitor != null)
		{
			unregisterReceiver( brMonitor );
			brMonitor = null;
		}
	}


	private synchronized void stopSensor ()
	{
		// SENSOR
		if (sensor != null)
		{
			if (sensorManager == null)
			{
				sensorManager = ((SensorManager) getSystemService( Context.SENSOR_SERVICE ));
			}

			sensorManager.unregisterListener( this );
			sensor = null;
			sensorManager = null;
		}
	}


	private void stopUI ()
	{
		if (plotViewMain != null)
		{
			plotViewMain.setEnabled( false );
			isDisplayOn = false;
		}
	}


	public void updateUI ()
	{
		// STATUS
		if (lblStatus != null && isDisplayOn)
		{
			sbStat.setLength( 0 );

			sbStat.append( getString( R.string.textBattery ) ).append( ": " ).append( BatteryTracker.batteryLevel )
					.append( "% @ " ).append( (BatteryTracker.batteryTemp * 0.1f) ).append( "°  " );


			// if (SensorProcessor.stream != null)
			// {
			// sbStat.append( String.format( "T[ %d ]", SPTracker.varThreshold ) );
			//
			// if (SPTracker.millisNow < SPTracker.millisStartTime)
			// {
			// sbStat.append( String.format( "  TTL[ %d s ]",
			// (long) ( (SPTracker.millisStartTime - SPTracker.millisNow) * 0.001) ) );
			// }
			// }

			// stat.append( "  ^" ).append( maxSensorValue ).append( " " ).append( " ~" ).append( varThreshold );

			lblStatus.setText( sbStat.toString() );
		}
	}


	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	public void onAccuracyChanged (Sensor sensor, int accuracy)
	{
		// === TODO Auto-generated method stub

	}


	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged (SensorEvent event)
	{
		if (!SensorProcessor.newSensorEvent( event ))
		{
			finish();
		}

		plotMain.addValue( SensorProcessor.last, SensorProcessor.stream.m_millisNow );
	}

}
