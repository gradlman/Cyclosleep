/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.R;
import com.gradlspace.widgets.SafeViewFlipper;




/**
 * @author Falling
 * 
 */
public class IntroActivity extends Activity
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.intro );

		final IntroActivity vthis = this;

		final SafeViewFlipper flipper = (SafeViewFlipper) findViewById( R.id.flipperIntro );
		final Button btnNext = (Button) findViewById( R.id.btnIntroNext );
		btnNext.setOnClickListener( new View.OnClickListener() {
			public void onClick (View v)
			{
				if (flipper.getCurrentView() == findViewById( R.id.layoutIntro99 ))
				{
					vthis.finish();
				}
				else
				{
					flipper.showNext();
				}
			}
		} );
		final Button btnPrev = (Button) findViewById( R.id.btnIntroPrev );
		btnPrev.setOnClickListener( new View.OnClickListener() {
			public void onClick (View v)
			{
				if (flipper.getCurrentView() != findViewById( R.id.layoutIntro00 ))
				{
					flipper.showPrevious();
				}
			}
		} );
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume ()
	{
		// Space.prefSetFirstStart( false );
		CyopsBoolean.IS_FIRST_START.set( false );

		super.onResume();
	}

}
