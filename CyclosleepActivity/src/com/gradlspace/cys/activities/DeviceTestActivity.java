/**
 * 
 */
package com.gradlspace.cys.activities;


import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.FloatMath;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.LockAuthority.LockMode;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;

import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.PlotView;
import de.lme.plotview.PlotView.Flags;
import de.lme.plotview.SamplingPlot;




/**
 * @author Falling
 * 
 */
public class DeviceTestActivity extends Activity implements SensorEventListener
{
	private PlotView			mPlotView		= null;

	private static SamplingPlot	mActPlot		= null;
	private TextView			m_lblStatus		= null;
	private Button				m_btnTest		= null;
	private static boolean		m_isDisplayOn	= true;

	public static SamplingPlot	stream			= null;
	public static final int		NUM_VALUES		= 8192;

	private Sensor				mSensor			= null;
	private SensorManager		mSensorMan		= null;

	private int					m_synchro		= 0;
	private long				m_millisOff		= 0;
	private static int			m_next			= 0;

	private long				mMillisNow;
	public static String		configString	= null;


	/**
	 * @param con
	 * @return xml-like string containing default accelerometer information.
	 */
	public static String enumDefaultAccel (Context con)
	{
		StringBuilder str = new StringBuilder( 128 );

		SensorManager man = (SensorManager) con.getSystemService( Context.SENSOR_SERVICE );

		if (man == null)
			return "<nosensor />";

		Sensor s = man.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );

		if (s == null)
			return "<nosensor />";

		str.append( "<Accel " ).append( "name=\"" ).append( s.getName() ).append( "\" version=\"" ).append( s.getVersion() )
				.append( "\"" );
		str.append( " vendor=\"" ).append( s.getVendor() ).append( "\"" );
		str.append( " power=\"" ).append( s.getPower() ).append( " mA\"" );
		str.append( " res=\"" ).append( s.getResolution() ).append( "\" maxRange=\"" ).append( s.getMaximumRange() )
				.append( "\" />" );

		return str.toString();
	}


	/**
	 * Enumerates all system sensors and returns them xml-formatted.
	 * 
	 * @param con
	 * @return
	 */
	public static String enumSensors (Context con)
	{
		if (configString != null)
			return configString;

		StringBuilder configStr = new StringBuilder( 2048 );

		configStr.append( "<SensorConfiguration>\n" );

		SensorManager man = (SensorManager) con.getSystemService( Context.SENSOR_SERVICE );

		List< Sensor > allSensors = man.getSensorList( Sensor.TYPE_ALL );
		for (Sensor s : allSensors)
		{
			configStr.append( "<Sensor id=\"" ).append( allSensors.indexOf( s ) ).append( "\"\n" );
			configStr.append( "  type=\"" ).append( s.getType() ).append( "\"\n" );
			configStr.append( "  name=\"" ).append( s.getName() ).append( "\"\n" );
			configStr.append( "  version=\"" ).append( s.getVersion() ).append( "\"\n" );
			configStr.append( "  vendor=\"" ).append( s.getVendor() ).append( "\"\n" );
			configStr.append( "  power=\"" ).append( s.getPower() ).append( " mA\"\n" );
			configStr.append( "  resolution=\"" ).append( s.getResolution() ).append( "\"\n" );
			configStr.append( "  maxRange=\"" ).append( s.getMaximumRange() ).append( "\" />\n" );
		}


		// probe gyroscopes
		// if (man.getDefaultSensor( Sensor.TYPE_GYROSCOPE ) != null)
		// {
		// List< Sensor > gravSensors = man.getSensorList( Sensor.TYPE_GYROSCOPE );
		// for (Sensor s : gravSensors)
		// {
		// // if ( (gravSensors.get( i ).getVendor().contains( "Google Inc." )) && (gravSensors.get( i
		// // ).getVersion() == 3))
		// // {
		// // // Use the version 3 gravity sensor.
		// // mSensor = gravSensors.get( i );
		// // }
		// configStr.append( "<Gyroscope \n" );
		// configStr.append( "  name=\"" ).append( s.getName() ).append( "\" version=\"" ).append( s.getVersion() )
		// .append( "\"\n" );
		// configStr.append( "  vendor=\"" ).append( s.getVendor() ).append( "\"\n" );
		// configStr.append( "  power=\"" ).append( s.getPower() ).append( " mA\"\n" );
		// configStr.append( "  resolution=\"" ).append( s.getResolution() ).append( "\" maxRange=\"" )
		// .append( s.getMaximumRange() ).append( "\" />\n" );
		// }
		//
		// // mSensor = mSensorManager.getDefaultSensor( Sensor.TYPE_GYROSCOPE );
		// }
		// else
		// {
		// configStr.append( "<Gyroscope na />\n" );
		// }
		//
		// // probe accelerometers
		// if (man.getDefaultSensor( Sensor.TYPE_ACCELEROMETER ) != null)
		// {
		// List< Sensor > accSensors = man.getSensorList( Sensor.TYPE_ACCELEROMETER );
		// for (Sensor s : accSensors)
		// {
		// configStr.append( "<Accelerometer \n" );
		// configStr.append( "  name=\"" ).append( s.getName() ).append( "\" version=\"" ).append( s.getVersion() )
		// .append( "\"\n" );
		// configStr.append( "  vendor=\"" ).append( s.getVendor() ).append( "\"\n" );
		// configStr.append( "  power=\"" ).append( s.getPower() ).append( " mA\"\n" );
		// configStr.append( "  resolution=\"" ).append( s.getResolution() ).append( "\" maxRange=\"" )
		// .append( s.getMaximumRange() ).append( "\" />\n" );
		// }
		// }
		// else
		// {
		// // no accelerometers
		// configStr.append( "<Accelerometer na />" );
		// }

		man = null;

		configStr.append( "</SensorConfiguration>\n" );

		configString = configStr.toString();
		return configString;
	}


	private int					mSensorValue	= 0;
	private static final int	MAGNITUDE		= 1000;
	private static final float	alpha			= 0.0625f, beta = 0.9375f;
	private transient float		gravity[]		= new float[ 3 ];
	private transient float		lastval[]		= null;


	/**
	 * Inits persistent objects.
	 * 
	 * @param nonPersistent
	 * @param semiPersistent
	 * @param persistent
	 */
	private void finalInit ()
	{
		// ACCEL STREAM
		if (stream == null)
		{
			stream = new SamplingPlot( "stream", Plot.generatePlotPaint( 1f, 255, 199, 45, 45 ), PlotStyle.LINE, NUM_VALUES,
					false );

			mMillisNow = System.currentTimeMillis();

			mActPlot = new SamplingPlot( "Activity", Plot.generatePlotPaint( 1f, 255, 199, 45, 45 ), PlotStyle.LINE, NUM_VALUES );
			mActPlot.setViewport( 60, 30 );
		}

		if (mPlotView != null && mPlotView.getNumPlots() <= 0)
		{
			mPlotView.attachPlot( mActPlot );
			mPlotView.removeFlag( Flags.DRAW_AXES );
			// mPlotView.removeFlag( Flags.DRAW_AXES );
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
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.device_test );

		mPlotView = (PlotView) findViewById( R.id.plotMonitor1 );
		mPlotView.addFlag( Flags.DISABLE_Y_USERSCROLL );

		// mPlotView.removeFlag( Flags.DRAW_X_AXIS );
		// mPlotView.removeFlag( Flags.DRAW_Y_AXIS );

		m_lblStatus = (TextView) findViewById( R.id.lblMonitorStatus );
		m_btnTest = (Button) findViewById( R.id.btnDeviceTest );


		Space.log( "Tester::create" );

		setState();

		finalInit();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy ()
	{
		super.onDestroy();

		this.stopSensor();

		if (this.isFinishing())
		{
			Space.log( "Tester::onDestroy for finish" );
			this.stopSensor();
			LockAuthority.releaseNormal();
		}
		else
		{
			Space.log( "Tester::onDestroy for reconfig" );
		}
	}


	@Override
	protected void onPause ()
	{
		super.onPause();


		if (this.isFinishing())
		{
			Space.log( "Tester::onPause for finish" );
			this.stopSensor();
			m_next = 0;

			LockAuthority.releaseNormal();
		}
		else
		{
			Space.log( "Tester::onPause for pause" );

			PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
			if (!pm.isScreenOn())
			{
				Space.log( "Screen OFF" );
				m_isDisplayOn = false;
				m_millisOff = System.currentTimeMillis();
				m_synchro = 0;

				if (m_next == 5)
				{
					SystemClock.sleep( 1000 );
					LockAuthority.releaseNormal();
					LockAuthority.setMode( LockMode.NOSENSOR );
					LockAuthority.acquireNormal();
				}
			}
		}
	}


	private void setState ()
	{
		switch (m_next)
		{
			case 0:
				m_lblStatus.setText( R.string.devtestScreenOff );
				m_isDisplayOn = true;
				m_btnTest.setVisibility( View.INVISIBLE );
				break;
			case 1:
				m_lblStatus.setText( R.string.devtestFirstSuccess );
				m_btnTest.setText( R.string.textFinish );
				m_btnTest.setVisibility( View.VISIBLE );
				LockAuthority.setMode( LockMode.DEFAULT );
				CyopsBoolean.IS_CRIPPLED.set( false );
				// Space.spref().edit().putBoolean( "pk_g_crippled", false ).commit();
				break;
			case 2:
				m_lblStatus.setText( R.string.devtestFirstFail );
				m_btnTest.setText( R.string.textNext );
				m_btnTest.setVisibility( View.VISIBLE );
				CyopsBoolean.IS_CRIPPLED.set( true );
				// Space.spref().edit().putBoolean( "pk_g_crippled", true ).commit();
				break;
			case 3:
				m_lblStatus.setText( R.string.devtestSecondSuccess );
				m_btnTest.setText( R.string.textFinish );
				m_btnTest.setVisibility( View.VISIBLE );
				break;
			case 4:
				m_lblStatus.setText( R.string.devtestSecondFail );
				m_btnTest.setText( R.string.textClose );
				m_btnTest.setVisibility( View.VISIBLE );
				break;

			case 5:
				m_lblStatus.setText( R.string.devtestSecond );
				m_btnTest.setVisibility( View.INVISIBLE );
				break;
		}
	}


	// private long mNext

	@Override
	protected void onResume ()
	{
		super.onResume();

		if (!m_isDisplayOn)
		{
			PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
			if (pm.isScreenOn())
			{
				m_isDisplayOn = true;

				if (m_next == 5)
				{
					if (m_millisOff + 1500 > System.currentTimeMillis())
					{
						m_next = 3;
					}
					else
						m_next = 4;

				}
				else
				{
					long div = (System.currentTimeMillis() - m_millisOff) / 60;

					// Space.log( "end " + div + "  s " + m_synchro + "    " + (div - m_synchro) + "  " + (div / 2) );

					// check how many points are "lost"
					if (m_synchro < 3 || div - m_synchro > div / 2)
					{
						if (m_next == 0)
							m_next = 2;
						else if (m_next == 5)
							m_next = 4;
					}
					else
					{
						if (m_next == 0)
							m_next = 1;
						else if (m_next == 5)
							m_next = 3;
					}
				}

				setState();
			}
		}

		Space.log( "Tester::onResume" );
	}


	public void onClickTest (View v)
	{
		if (m_next == 1 || m_next == 3 || m_next == 4)
		{
			this.finish();
		}

		m_next = 5;
		setState();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged (SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// get current time
			mMillisNow = System.currentTimeMillis();

			++m_synchro;

			if (lastval == null)
			{
				lastval = new float[ 3 ];
				lastval[ 0 ] = event.values[ 0 ];
				lastval[ 1 ] = event.values[ 1 ];
				lastval[ 2 ] = event.values[ 2 ];
			}

			gravity[ 0 ] = (event.values[ 0 ] - lastval[ 0 ]) * alpha + beta * gravity[ 0 ];
			gravity[ 1 ] = (event.values[ 1 ] - lastval[ 1 ]) * alpha + beta * gravity[ 1 ];
			gravity[ 2 ] = (event.values[ 2 ] - lastval[ 2 ]) * alpha + beta * gravity[ 2 ];

			lastval[ 0 ] = event.values[ 0 ];
			lastval[ 1 ] = event.values[ 1 ];
			lastval[ 2 ] = event.values[ 2 ];


			// calc absolute vector length of x/y/z accel
			mSensorValue = (int) (FloatMath.sqrt( gravity[ 0 ] * gravity[ 0 ] + gravity[ 1 ] * gravity[ 1 ] + gravity[ 2 ]
					* gravity[ 2 ] ) * MAGNITUDE);


			if (stream == null)
				return;

			stream.addValueFast( mSensorValue, mMillisNow );
			mActPlot.addValue( mSensorValue, mMillisNow );
		}
	}


	@Override
	protected void onStart ()
	{
		super.onStart();

		LockAuthority.acquireNormal();
		this.startSensor();

		Space.log( "Tester::onStart" );
	}


	@Override
	protected void onStop ()
	{
		super.onStop();

		// Space.log( "Tester::onStop" );
		//
		// this.stopSensor();
		// LockAuthority.releaseNormal();
	}


	private synchronized void startSensor ()
	{
		// SENSOR
		if (mSensor == null)
		{
			if (mSensorMan == null)
			{
				mSensorMan = ((SensorManager) this.getSystemService( Context.SENSOR_SERVICE ));
			}

			mSensor = mSensorMan.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
			mSensorMan.registerListener( this, mSensor, SensorManager.SENSOR_DELAY_UI );
		}

		m_synchro = 0;
	}


	private synchronized void stopSensor ()
	{
		// SENSOR
		if (mSensor != null)
		{
			if (mSensorMan == null)
			{
				mSensorMan = ((SensorManager) this.getSystemService( Context.SENSOR_SERVICE ));
			}

			mSensorMan.unregisterListener( this );
			mSensor = null;

			mSensorMan = null;
		}
	}
}
