/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gradlspace.cys.Dream;
import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.Hypnogram;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;
import com.gradlspace.widgets.HypnogramView;

import de.lme.plotview.Plot;
import de.lme.plotview.Plot1D;
import de.lme.plotview.PlotView;
import de.lme.plotview.PlotView.Flags;
import de.lme.plotview.PlotView.PlotProgressListener;




/**
 * @author Falling
 * 
 */
public class ViewerActivity extends Activity
{
	private PlotView					mPlotView		= null;
	private HypnogramView				mHypnoView		= null;
	private static Hypnogram			mHypnogram		= null;
	private static Plot1D				mSensorPlot		= null;
	private static Dream				mDream			= null;

	private TextView					mDreamDate		= null;
	private TextView					mDreamTheme		= null;
	private TextView					mDreamEmotion	= null;
	private TextView					mDreamRating	= null;
	private TextView					mDreamText		= null;

	private ProgressDialog				progressDialog;
	static final int					PROGRESS_DIALOG	= 0;

	private static LoadSensorPlotTask	m_loadTask		= null;


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );


		String str = getIntent().getData().getPath();
		if (str.endsWith( FileManager.HYPNO_EXT ))
		{
			setContentView( R.layout.viewer );

			mPlotView = (PlotView) findViewById( R.id.viewPlot );
			mHypnoView = (HypnogramView) findViewById( R.id.viewHypno );

			// HYPNO file
			mPlotView.setVisibility( View.GONE );
			mHypnoView.setVisibility( View.VISIBLE );

			if (mHypnogram == null)
			{
				mHypnogram = new Hypnogram();

				// get intent filepath and load the file
				if (mHypnogram.loadFromFile( this, str, true ) == 0)
				{
					Space.showToast( this, "Error reading file!", Toast.LENGTH_SHORT );
					// mEventHist.clear();
				}
			}

			mHypnoView.setHypnogram( mHypnogram );
		}
		else if (str.endsWith( FileManager.SSD_EXT ) || str.endsWith( FileManager.ZIP_EXT ))
		{
			setContentView( R.layout.viewer );

			mPlotView = (PlotView) findViewById( R.id.viewPlot );
			mHypnoView = (HypnogramView) findViewById( R.id.viewHypno );

			// SENSOR file
			// Space.showHint( this, R.string.errorNotSupported );

			mPlotView.setVisibility( View.VISIBLE );
			mHypnoView.setVisibility( View.GONE );

			// disable autoscroll
			mPlotView.removeFlag( Flags.ENABLE_AUTO_SCROLL );
			mPlotView.addFlag( Flags.DISABLE_Y_USERSCROLL );

			if (m_loadTask == null && mSensorPlot != null)
			{
				mPlotView.attachPlot( mSensorPlot );
			}
			else
				showDialog( PROGRESS_DIALOG );
		}
		else if (str.endsWith( FileManager.DREAM_EXT ))
		{
			setContentView( R.layout.viewer_dream );

			mDreamDate = (TextView) findViewById( R.id.lblDreamDate );
			mDreamTheme = (TextView) findViewById( R.id.lblDreamTheme );
			mDreamEmotion = (TextView) findViewById( R.id.lblDreamEmotion );
			mDreamRating = (TextView) findViewById( R.id.lblDreamRating );
			mDreamText = (TextView) findViewById( R.id.lblDreamText );

			if (mDream == null)
			{
				mDream = new Dream();
				mDream.loadFromFile( this, str );
			}

			if (mDream != null)
			{
				Time t = new Time();
				t.set( mDream.timestamp );
				mDreamDate.setText( t.format3339( false ) );
				mDreamTheme.setText( mDream.headline );
				mDreamEmotion.setText( Dream.getEmotionString( mDream.emotion ) );
				mDreamRating.setText( Byte.toString( mDream.rating ) );
				mDreamText.setText( mDream.text );
			}
		}
		else
		{
			Space.showToast( this, R.string.textUnknownFormat );
		}
	}


	private class LoadSensorPlotTask extends AsyncTask< String, Integer, Long >
	{
		private PlotProgressListener	mListener	= new PlotProgressListener() {
														private int	m_maxProgress	= 100;


														public void onUpdateProgress (int progress)
														{
															if (progressDialog != null && progress > progressDialog.getMax())
															{
																onSetMaxProgress( m_maxProgress );
															}
															publishProgress( progress );
														}


														public void onSetMaxProgress (int maxProgress)
														{
															m_maxProgress = maxProgress;
															if (progressDialog != null)
																progressDialog.setMax( maxProgress );
														}


														public boolean isCancelled ()
														{
															return LoadSensorPlotTask.this.isCancelled();
														}
													};


		protected Long doInBackground (String... paths)
		{
			if (mSensorPlot == null)
			{
				mSensorPlot = Plot1D.create( paths[ 0 ], ' ', 1, 2, 3, mListener );
				if (mSensorPlot != null)
				{
					mSensorPlot.setPaint( Plot.generatePlotPaint( 2f, 255, 165, 42, 42 ) );
				}

			}
			return 0L;
		}


		protected void onProgressUpdate (Integer... progress)
		{
			if (progressDialog != null)
				progressDialog.setProgress( progress[ 0 ] );
		}


		protected void onPostExecute (Long result)
		{
			if (mPlotView != null)
				mPlotView.attachPlot( mSensorPlot );

			if (progressDialog != null)
				progressDialog.dismiss();
		}


		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled ()
		{
			if (progressDialog != null)
				progressDialog.dismiss();

			mSensorPlot = null;
			super.onCancelled();
		}


	}


	protected Dialog onCreateDialog (int id)
	{
		switch (id)
		{
			case PROGRESS_DIALOG:
				if (progressDialog == null)
				{
					progressDialog = new ProgressDialog( this );
					progressDialog.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
					progressDialog.setMessage( "Loading..." );
				}
				return progressDialog;
			default:
				return null;
		}
	}


	@Override
	protected void onPrepareDialog (int id, Dialog dialog)
	{
		switch (id)
		{
			case PROGRESS_DIALOG:
				if (m_loadTask == null)
				{
					m_loadTask = new LoadSensorPlotTask();
					m_loadTask.execute( getIntent().getData().getPath() );
				}
				break;
		}
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


	public void cleanup ()
	{
		m_loadTask = null;
		progressDialog = null;
		mHypnogram = null;
		mSensorPlot = null;
		mPlotView = null;
		mHypnoView = null;
		mDream = null;
	}


	@Override
	protected void onDestroy ()
	{
		super.onDestroy();

		if (isFinishing())
		{
			cleanup();
		}
		else
		{
			// reconfig destory, kill loadtask
			if (m_loadTask != null)
			{
				m_loadTask.cancel( true );
				m_loadTask = null;
			}
		}
	}


	@Override
	protected void onResume ()
	{
		if (mPlotView != null)
		{
			mPlotView.setEnabled( true );
			mPlotView.invalidate();
		}
		if (mHypnoView != null)
		{
			mHypnoView.setEnabled( true );
			mHypnoView.invalidate();
		}
		super.onResume();
	}


	@Override
	protected void onPause ()
	{
		if (m_loadTask != null && m_loadTask.getStatus() != Status.RUNNING)
		{
			// loading task not running
			m_loadTask = null;
		}

		if (mPlotView != null)
		{
			mPlotView.setEnabled( false );
		}
		if (mHypnoView != null)
		{
			mHypnoView.setEnabled( false );
		}
		if (this.isFinishing())
		{
			cleanup();
		}
		super.onPause();
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();


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
		inflater.inflate( R.menu.viewer_menu, menu );
		return true;
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
			case R.id.help:
				Space.showToast( this, R.string.textHelpViewer );
				return true;

			default:
				return super.onOptionsItemSelected( item );
		}
	}

}
