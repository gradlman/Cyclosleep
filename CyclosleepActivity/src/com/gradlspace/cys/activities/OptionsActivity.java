/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;
import android.view.View;

import com.gradlspace.cys.Cyops;
import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.LockAuthority;
import com.gradlspace.cys.LockAuthority.LockMode;
import com.gradlspace.cys.R;
import com.gradlspace.cys.SPTracker;
import com.gradlspace.cys.Space;
import com.gradlspace.widgets.AlarmPreference;




/**
 * @author Falling
 * 
 */
public class OptionsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
		Preference.OnPreferenceClickListener
{

	public static final int	TTS_CHECK	= 1;
	public static final int	ALARM_EDIT	= 2;

	private AlarmPreference	mApref0		= null;
	private AlarmPreference	mApref1		= null;
	private AlarmPreference	mApref2		= null;
	private AlarmPreference	mApref3		= null;


	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		addPreferencesFromResource( R.xml.prefs );

		Preference pref = (Preference) findPreference( "pk_g_calibrate" );
		pref.setOnPreferenceClickListener( this );
		// pref.setDefaultValue( defaultValue )

		pref = (Preference) findPreference( "pk_g_reset" );
		pref.setOnPreferenceClickListener( this );

		pref = (Preference) findPreference( "pk_custom" );
		pref.setOnPreferenceClickListener( this );

		pref = (Preference) findPreference( "pk_g_persistent" );
		pref.setOnPreferenceClickListener( this );

		// ALARM 0
		mApref0 = (AlarmPreference) findPreference( "pk_alarm0" );
		mApref0.setOnClickAreaListener( new View.OnClickListener() {

			public void onClick (View v)
			{
				if (mApref0 != null)
				{
					Intent in = new Intent( OptionsActivity.this, AlarmEditActivity.class );
					in.putExtra( AlarmEditActivity.EXTRA_ALARM_ID, mApref0.getAlarmId() );
					OptionsActivity.this.startActivityForResult( in, ALARM_EDIT );
				}
			}
		} );

		mApref1 = (AlarmPreference) findPreference( "pk_alarm1" );
		mApref1.setOnClickAreaListener( new View.OnClickListener() {

			public void onClick (View v)
			{
				if (mApref1 != null)
				{
					Intent in = new Intent( OptionsActivity.this, AlarmEditActivity.class );
					in.putExtra( AlarmEditActivity.EXTRA_ALARM_ID, mApref1.getAlarmId() );
					OptionsActivity.this.startActivityForResult( in, ALARM_EDIT );
				}
			}
		} );

		mApref2 = (AlarmPreference) findPreference( "pk_alarm2" );
		mApref2.setOnClickAreaListener( new View.OnClickListener() {

			public void onClick (View v)
			{
				if (mApref2 != null)
				{
					Intent in = new Intent( OptionsActivity.this, AlarmEditActivity.class );
					in.putExtra( AlarmEditActivity.EXTRA_ALARM_ID, mApref2.getAlarmId() );
					OptionsActivity.this.startActivityForResult( in, ALARM_EDIT );
				}
			}
		} );

		mApref3 = (AlarmPreference) findPreference( "pk_alarm3" );
		mApref3.setOnClickAreaListener( new View.OnClickListener() {

			public void onClick (View v)
			{
				if (mApref3 != null)
				{
					Intent in = new Intent( OptionsActivity.this, AlarmEditActivity.class );
					in.putExtra( AlarmEditActivity.EXTRA_ALARM_ID, mApref3.getAlarmId() );
					OptionsActivity.this.startActivityForResult( in, ALARM_EDIT );
				}
			}
		} );

		// sensor bug
		pref = (Preference) findPreference( "pk_sm_crippleMode" );
		if (CyopsBoolean.IS_CRIPPLED.isEnabled())
		{
			pref.setEnabled( true );
			pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {

				public boolean onPreferenceChange (Preference preference, Object newValue)
				{
					if (CyopsString.CRIPPLE_WORKAROUND.get().equals( "none" ))
					{
						LockAuthority.setMode( LockMode.DEFAULT );
					}
					else
					{
						LockAuthority.setMode( LockMode.NOSENSOR );
					}
					return true;
				}
			} );
		}
		else
		{
			pref.setEnabled( false );
		}


		// SPEECH ENABLE BUTTON
		pref = (Preference) findPreference( "pk_speech_enable" );
		pref.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {

			public boolean onPreferenceChange (Preference preference, Object newValue)
			{
				if ((Boolean) newValue == true)
				{
					// check tts data
					Space.log( "checking tts data..." );
					Intent checkIntent = new Intent();
					checkIntent.setAction( TextToSpeech.Engine.ACTION_CHECK_TTS_DATA );
					startActivityForResult( checkIntent, TTS_CHECK );
				}
				return true;
			}
		} );

	}


	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		if (requestCode == TTS_CHECK)
		{
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
			{
				// success, create the TTS instance
			}
			else
			{
				// missing data, install it
				Space.log( "tts data missing, trying to install..." );
				Intent installIntent = new Intent();
				installIntent.setAction( TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA );
				startActivity( installIntent );
			}
		}
		else if (requestCode == ALARM_EDIT)
		{
			if (resultCode == RESULT_OK)
			{
				// changed settings, update
				mApref0.updateDisplay();
				mApref1.updateDisplay();
				mApref2.updateDisplay();
				mApref3.updateDisplay();
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart ()
	{
		Space.showHint( this, R.string.hintPressAlarm );
		super.onStart();
	}


	@Override
	protected void onPause ()
	{
		SPTracker.init();
		super.onPause();
	}


	/* (non-Javadoc)
	 * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
	 */
	public boolean onPreferenceChange (Preference arg0, Object arg1)
	{
		return false;
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
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textResetPrefs ).setMessage( R.string.textResetPrefsBody )
					.setPositiveButton( R.string.textYes, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							Cyops.spref().edit().clear().commit();
							SPTracker.init();
							Space.doTriggerUpdate( Space.get(), true, false );
							LockAuthority.reset();
							OptionsActivity.this.finish();
						}
					} ).setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// do nothing
						}
					} ).create();
		}
		return dialog;
	}


	/* (non-Javadoc)
	 * @see android.preference.Preference.OnPreferenceClickListener#onPreferenceClick(android.preference.Preference)
	 */
	public boolean onPreferenceClick (Preference preference)
	{
		String key = preference.getKey();
		if (key != null && key.equals( "pk_g_calibrate" ))
		{
			Space.startActivity( this, MonitorActivity.class, Space.ACTION_MONITOR_CALIBRATE, null, 0 );
			return true;
		}
		else if (key != null && key.equals( "pk_custom" ))
		{
			Space.showToast( this, R.string.prefCustomWarn );
		}
		else if (key != null && key.equals( "pk_g_reset" ))
		{
			Space.resetOperations();
			showDialog( 0 );
		}
		else if (key != null && key.equals( "pk_g_persistent" ))
		{
			Space.doTriggerUpdate( this, true, false );
		}
		return false;
	}
}