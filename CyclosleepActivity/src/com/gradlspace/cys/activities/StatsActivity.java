/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.Hypnogram;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.Space;
import com.gradlspace.widgets.HypnogramView;

import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotFlag;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.Plot2D;
import de.lme.plotview.PlotView;
import de.lme.plotview.PlotView.Flags;




/**
 * @author Falling
 * 
 */
public class StatsActivity extends Activity
{
	private PlotView			m_plotView		= null;
	private HypnogramView		m_hypnoViews[]	= new HypnogramView[ 3 ];
	private static Hypnogram	m_hypnos[]		= new Hypnogram[ 3 ];
	private static Plot2D		m_statsPlot		= null;
	private static Plot2D		m_deepSleepPlot	= null;
	private static Plot2D		m_ratingPlot	= null;

	private TextView			m_txtStats		= null;


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.statistics );

		if (m_hypnoViews == null)
			m_hypnoViews = new HypnogramView[ 3 ];
		if (m_hypnos == null)
			m_hypnos = new Hypnogram[ 3 ];

		m_plotView = (PlotView) findViewById( R.id.viewPlot );
		m_hypnoViews[ 0 ] = (HypnogramView) findViewById( R.id.viewHypno1 );
		m_hypnoViews[ 1 ] = (HypnogramView) findViewById( R.id.viewHypno2 );
		m_hypnoViews[ 2 ] = (HypnogramView) findViewById( R.id.viewHypno3 );

		// disable autoscroll
		m_plotView.removeFlag( Flags.ENABLE_AUTO_SCROLL );
		m_plotView.addFlag( Flags.DISABLE_Y_USERSCROLL );

		m_txtStats = (TextView) findViewById( R.id.lblStats );


		// Sleep duration plot
		if (m_statsPlot == null)
		{
			m_statsPlot = new Plot2D( getString( R.string.textSleepDurations ), Plot.generatePlotPaint( 2f, 255, 70, 70, 210 ),
					PlotStyle.LINE, 128 );
			m_statsPlot.setAxis( "Day", "d", 1f, "Duration", "h", 1f );
			m_statsPlot.flags.remove( PlotFlag.LINK_ASPECT );
			m_statsPlot.setViewport( 7, 7, 15, 14 );

			m_deepSleepPlot = new Plot2D( getString( R.string.textSleepDurations ),
					Plot.generatePlotPaint( 2f, 255, 70, 210, 70 ), PlotStyle.LINE, 128 );
			m_deepSleepPlot.setAxis( "Day", "d", 1f, "Duration", "h", 1f );
			m_deepSleepPlot.flags.remove( PlotFlag.LINK_ASPECT );
			m_deepSleepPlot.setViewport( 7, 7, 15, 14 );

			m_ratingPlot = new Plot2D( getString( R.string.textSleepDurations ), Plot.generatePlotPaint( 2f, 128, 210, 70, 70 ),
					PlotStyle.STEM, 128 );
			m_ratingPlot.setAxis( "Day", "d", 1f, "Quality", "*", 1f );
			m_ratingPlot.flags.remove( PlotFlag.LINK_ASPECT );
			m_ratingPlot.setViewport( 7, 3, 15, 6 );

			int num = Space.s_dbData.m_sleepRecords.size();
			if (num > 14)
				num = 14;

			// draw the 14 most recent sleep records
			for (int i = 0; i < Space.s_dbData.m_sleepRecords.size() && i < 14; ++i)
			{
				// total sleep
				m_statsPlot.addValue( 8f, num - 1 - i, Space.s_dbData.m_sleepRecords.get( i ).durwake / 3600000f );

				// deep sleep
				m_deepSleepPlot.addValue( 8f, num - 1 - i, Space.s_dbData.m_sleepRecords.get( i ).durdeep / 3600000f );

				// quality rating
				if (Space.s_dbData.m_sleepRecords.get( i ).quality > 0)
				{
					m_ratingPlot.addValue( 4f, num - 1 - i, Space.s_dbData.m_sleepRecords.get( i ).quality );
				}
			}
		}

		m_plotView.attachPlot( m_statsPlot );
		m_plotView.attachPlot( m_deepSleepPlot );
		m_plotView.attachPlot( m_ratingPlot );

		m_txtStats.setText( String.format( "%s %.2f h", getString( R.string.textMainStats ), SPTracker.avgSleepHours ) );


		// Hypnogram 1 - most recent
		if (m_hypnos[ 0 ] == null)
		{
			m_hypnos[ 0 ] = new Hypnogram();

			// get intent filepath and load the file
			if (m_hypnos[ 0 ].loadFromFile( this, FileManager.getLastHypnoFile( this, 0 ), true ) == 0)
			{
				Space.showToast( this, "Error reading file!", Toast.LENGTH_SHORT );
				// mEventHist.clear();
			}
		}

		// Hypnogram 2
		if (m_hypnos[ 1 ] == null)
		{
			m_hypnos[ 1 ] = new Hypnogram();

			// get intent filepath and load the file
			if (m_hypnos[ 1 ].loadFromFile( this, FileManager.getLastHypnoFile( this, 1 ), true ) == 0)
			{
				Space.showToast( this, "Error reading file!", Toast.LENGTH_SHORT );
				// mEventHist.clear();
			}
		}

		// Hypnogram 3
		if (m_hypnos[ 2 ] == null)
		{
			m_hypnos[ 2 ] = new Hypnogram();

			// get intent filepath and load the file
			if (m_hypnos[ 2 ].loadFromFile( this, FileManager.getLastHypnoFile( this, 2 ), true ) == 0)
			{
				Space.showToast( this, "Error reading file!", Toast.LENGTH_SHORT );
				// mEventHist.clear();
			}
		}

		m_hypnoViews[ 0 ].setHypnogram( m_hypnos[ 0 ] );
		m_hypnoViews[ 1 ].setHypnogram( m_hypnos[ 1 ] );
		m_hypnoViews[ 2 ].setHypnogram( m_hypnos[ 2 ] );
	}


	protected Dialog onCreateDialog (int id)
	{
		return null;
	}


	@Override
	protected void onStart ()
	{
		super.onStart();
	}


	@Override
	protected void onStop ()
	{
		super.onStop();
	}


	@Override
	protected void onDestroy ()
	{
		super.onDestroy();

		if (isFinishing())
		{
			m_plotView = null;
			m_hypnoViews = null;
			m_hypnos = null;
			m_statsPlot = null;
		}
	}


	@Override
	protected void onResume ()
	{
		super.onResume();
	}


	@Override
	protected void onPause ()
	{
		if (isFinishing())
		{
			m_plotView = null;
			m_hypnoViews = null;
			m_hypnos = null;
			m_statsPlot = null;
		}
		super.onPause();
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();


	}

}
