/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Dream;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.Space;
import com.gradlspace.widgets.HypnogramView;




/**
 * @author Falling
 * 
 */
public class RateActivity extends Activity
{
	private boolean			rateSleep, rateDream;

	private HypnogramView	mHypnoView;
	private RatingBar		ratingSleep;
	private RatingBar		ratingDream;
	private Spinner			cmbEmotion;
	private EditText		txtHeadline;
	private EditText		txtText;
	private TextView		lblSleepDuration;


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.sleep_about );


		rateSleep = CyopsBoolean.RATE_SLEEP.get();
		rateDream = CyopsBoolean.RATE_DREAM.get();


		// mHypnoView.setVisibility( View.GONE );

		if (!rateDream)
		{
			findViewById( R.id.frmDream ).setVisibility( View.GONE );
		}
		else if (!rateSleep)
		{
			findViewById( R.id.frmSleep ).setVisibility( View.GONE );
		}

		// final TextView tex = (TextView) layout.findViewById( R.id.lblDreamMain );
		// tex.setText( "test" );

		SPTracker.getHypnogram().calculateStats();


		mHypnoView = (HypnogramView) findViewById( R.id.hypnoView );
		mHypnoView.setHypnogram( SPTracker.getHypnogram() );

		lblSleepDuration = (TextView) findViewById( R.id.lblSleepDuration );
		lblSleepDuration.setText( getString( R.string.textTimeSlept ) + ": "
				+ DateUtils.formatElapsedTime( SPTracker.getHypnogram().stats.sleepSpan / 1000 ) );

		ratingSleep = (RatingBar) findViewById( R.id.ratingSleep );

		ratingDream = (RatingBar) findViewById( R.id.ratingDream );
		cmbEmotion = ((Spinner) findViewById( R.id.cmbEmotion ));
		txtHeadline = ((EditText) findViewById( R.id.txtDreamTheme ));
		txtText = ((EditText) findViewById( R.id.txtDreamText ));

	}


	public void onClickOk (View v)
	{
		if (rateSleep)
		{
			SPTracker.getHypnogram().rating = (byte) ratingSleep.getRating();
		}

		if (rateDream)
		{
			// create a new dream and fill it
			Dream d = new Dream();
			d.timestamp = System.currentTimeMillis();
			d.rating = (byte) ratingDream.getRating();
			d.emotion = (byte) cmbEmotion.getSelectedItemPosition();
			d.headline = txtHeadline.getText().toString();
			d.text = txtText.getText().toString();

			// save it to the filesystem
			d.saveToFile( Space.get(), null );

			// saveToFile sets the path it saved the dream to, now we put it into the record structure
			SPTracker.sleep.dreamfile = d.absolutePath;
		}
		else
		{
			SPTracker.sleep.dreamfile = "";
		}


		// store the sleep record
		SPTracker.save();


		setResult( RESULT_OK );
		finish();
	}


	public void onClickCancel (View v)
	{
		setResult( RESULT_CANCELED );
		finish();
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
	}


	@Override
	protected void onResume ()
	{
		super.onResume();
	}


	@Override
	protected void onPause ()
	{
		super.onPause();
	}


	@Override
	public void onLowMemory ()
	{
		super.onLowMemory();
	}
}
