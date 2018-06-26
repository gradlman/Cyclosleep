/**
 * 
 */
package com.gradlspace.cys.activities;


import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.gradlspace.cys.AudioHandler;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;
import com.gradlspace.cys.TimeTrigger;
import com.gradlspace.cys.TriggerHandler;




/**
 * @author Falling
 * 
 */
public class AlarmEditActivity extends Activity
{
	public static final String	EXTRA_ALARM_ID	= "com.gradlspace.cys.alarmid";

	private int					mAlarmIdx		= -1;

	private Spinner				cmbSound;
	private RadioGroup			recGroup;
	private TextView			lblSound;
	private ToggleButton		dialogBtnEnabled;
	private TimePicker			timePicker;
	private EditText			txtName;
	private EditText			txtMessage;
	private ToggleButton		mon, tue, wed, thu, fri, sat, sun;
	private DatePicker			recDate;
	private CheckBox			sunrise;
	private MediaPlayer			m_MediaPlayer	= new MediaPlayer();


	/** Called when the activity is first created. */
	@Override
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.alarm_edit );

		mAlarmIdx = this.getIntent().getIntExtra( EXTRA_ALARM_ID, 0 );

		// get trigger for this preference
		TimeTrigger tt = (TimeTrigger) TriggerHandler.getTrigger( mAlarmIdx );

		cmbSound = ((Spinner) findViewById( R.id.cmdSound ));
		recGroup = ((RadioGroup) findViewById( R.id.groupRec ));
		recGroup.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			public void onCheckedChanged (RadioGroup group, int checkedId)
			{}
		} );

		// ===== Sound lbl
		lblSound = (TextView) findViewById( R.id.lblSound );
		lblSound.setText( tt.getAlarmSound() );

		// ===== ENABLED
		dialogBtnEnabled = ((ToggleButton) findViewById( R.id.btnEnabled ));
		dialogBtnEnabled.setChecked( tt.enabled );


		// ===== TIME
		Time loadTime = new Time();
		loadTime.set( tt.getFireTime() );

		timePicker = ((TimePicker) findViewById( R.id.pickerTime ));
		if (DateFormat.is24HourFormat( this ))
			timePicker.setIs24HourView( true );
		timePicker.setCurrentHour( loadTime.hour );
		timePicker.setCurrentMinute( loadTime.minute );


		// ===== NAME
		txtName = ((EditText) findViewById( R.id.txtName ));
		txtName.setText( tt.getName() );

		txtMessage = ((EditText) findViewById( R.id.txtMessage ));
		txtMessage.setText( tt.text );
		// txtMessage.setHint( "" );

		mon = ((ToggleButton) findViewById( R.id.btnMon ));
		tue = ((ToggleButton) findViewById( R.id.btnTue ));
		wed = ((ToggleButton) findViewById( R.id.btnWed ));
		thu = ((ToggleButton) findViewById( R.id.btnThu ));
		fri = ((ToggleButton) findViewById( R.id.btnFri ));
		sat = ((ToggleButton) findViewById( R.id.btnSat ));
		sun = ((ToggleButton) findViewById( R.id.btnSun ));

		String[] days = this.getResources().getStringArray( R.array.arrayDaysAbbr );
		mon.setTextOff( days[ 0 ] );
		mon.setTextOn( days[ 0 ] );
		mon.setText( days[ 0 ] );
		tue.setTextOff( days[ 1 ] );
		tue.setTextOn( days[ 1 ] );
		tue.setText( days[ 1 ] );
		wed.setTextOff( days[ 2 ] );
		wed.setTextOn( days[ 2 ] );
		wed.setText( days[ 2 ] );
		thu.setTextOff( days[ 3 ] );
		thu.setTextOn( days[ 3 ] );
		thu.setText( days[ 3 ] );
		fri.setTextOff( days[ 4 ] );
		fri.setTextOn( days[ 4 ] );
		fri.setText( days[ 4 ] );
		sat.setTextOff( days[ 5 ] );
		sat.setTextOn( days[ 5 ] );
		sat.setText( days[ 5 ] );
		sun.setTextOff( days[ 6 ] );
		sun.setTextOn( days[ 6 ] );
		sun.setText( days[ 6 ] );

		if (tt.recurrence != null)
		{
			if (tt.recurrence.contains( "mon" ))
				mon.setChecked( true );
			if (tt.recurrence.contains( "tue" ))
				tue.setChecked( true );
			if (tt.recurrence.contains( "wed" ))
				wed.setChecked( true );
			if (tt.recurrence.contains( "thu" ))
				thu.setChecked( true );
			if (tt.recurrence.contains( "fri" ))
				fri.setChecked( true );
			if (tt.recurrence.contains( "sat" ))
				sat.setChecked( true );
			if (tt.recurrence.contains( "sun" ))
				sun.setChecked( true );

			if (tt.recurrence.contains( "once" ))
			{
				recGroup.check( R.id.radioOnce );
			}
			else if (tt.recurrence.contains( "daily" ))
			{
				recGroup.check( R.id.radioDay );
			}
			else
			{
				recGroup.check( R.id.radioWeek );
			}
		}


		recDate = ((DatePicker) findViewById( R.id.pickerDate ));
		recDate.updateDate( loadTime.year, loadTime.month, loadTime.monthDay );
		// recDate.setEnabled( false );

		sunrise = ((CheckBox) findViewById( R.id.checkSunrise ));
		sunrise.setChecked( false );
		sunrise.setEnabled( false );


		// ===== POPULATE SOUND SPINNER
		ArrayAdapter< CharSequence > adapter = ArrayAdapter.createFromResource( this,
																				R.array.prefSoundArray,
																				android.R.layout.simple_spinner_item );
		adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
		cmbSound.setAdapter( adapter );
		cmbSound.setOnItemSelectedListener( new OnItemSelectedListener() {

			public void onItemSelected (AdapterView< ? > parent, View view, int pos, long id)
			{
				Space.log( "Selected: " + parent.getItemAtPosition( pos ).toString() );

				AlertDialog.Builder intBuilder = null;
				AlertDialog alert = null;

				switch (pos)
				{
					case 0:
						break;

					case 1:
						// internal
						lblSound.setText( "[int]" );
						break;

					case 2:
						// local file
						if (AudioHandler.loadSounds( AlarmEditActivity.this, false ))
						{
							if (AudioHandler.Sounds.mTitleArray == null || AudioHandler.Sounds.mTitleArray.length <= 0)
								break;

							intBuilder = new AlertDialog.Builder( AlarmEditActivity.this );
							intBuilder.setTitle( R.string.prefSnd + " [" + AudioHandler.Sounds.mTitleArray.length + "]" );

							// click listener
							intBuilder.setItems( AudioHandler.Sounds.mTitleArray, new DialogInterface.OnClickListener() {
								public void onClick (DialogInterface dialog, int item)
								{
									// Toast.makeText( getContext(), AudioHandler.Sounds.mPathArray[ item ],
									// Toast.LENGTH_SHORT )
									// .show();
									lblSound.setText( "[file]" + AudioHandler.Sounds.mTitleArray[ item ] );
								}
							} );

							alert = intBuilder.create();
							alert.show();
						}
						break;

					case 3:
						// ringtone
						lblSound.setText( "[ring]" );
						break;

					case 4:
						// iradio / stream
						intBuilder = new AlertDialog.Builder( AlarmEditActivity.this );
						intBuilder.setTitle( R.string.prefSetStream );
						intBuilder.setMessage( R.string.textEnterUrl );
						final EditText input = new EditText( AlarmEditActivity.this );
						input.setInputType( InputType.TYPE_TEXT_VARIATION_URI );
						intBuilder.setView( input );

						if (lblSound.getText().toString().startsWith( "[stream]" ))
						{
							input.setText( AudioHandler.extractParam( lblSound.getText().toString(), true ) );
						}

						input.setHint( "http://" );

						intBuilder.setPositiveButton( R.string.textOk, new DialogInterface.OnClickListener() {
							public void onClick (DialogInterface dlg, int whichButton)
							{
								// Send an intent with the URL of the song to play. This is expected by
								// MusicService.
								lblSound.setText( "[stream]" + input.getText().toString() );

								// Intent i = new Intent(MusicService.ACTION_URL);
								// Uri uri = Uri.parse(input.getText().toString());
								// i.setData(uri);
								// startService(i);
								m_MediaPlayer.release();

								Space.showHint( AlarmEditActivity.this, R.string.hintRadioStream );
							}
						} );
						intBuilder.setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
							public void onClick (DialogInterface dlg, int whichButton)
							{
								m_MediaPlayer.release();
							}

						} );
						intBuilder.setNeutralButton( R.string.textTestPrev, new DialogInterface.OnClickListener() {
							public void onClick (DialogInterface dlg, int whichButton)
							{
								int error = 0;
								m_MediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
								try
								{
									m_MediaPlayer.setDataSource( input.getText().toString() );
									m_MediaPlayer.prepare(); // might take long! (for buffering, etc)
									m_MediaPlayer.start();
								}
								catch (IllegalArgumentException e)
								{
									error = 1;
								}
								catch (IllegalStateException e)
								{
									error = 2;
									e.printStackTrace();
								}
								catch (IOException e)
								{
									error = 3;
									e.printStackTrace();
								}
								catch (SecurityException e)
								{
									error = 4;
									e.printStackTrace();
								}

								if (error != 0)
								{
									// "http://www.vorbis.com/music/Epoq-Lepidoptera.ogg"
									Space.showToast( AlarmEditActivity.this, R.string.textError );
								}
								else
								{
									lblSound.setText( "[stream]" + input.getText().toString() );
									Space.showHint( AlarmEditActivity.this, R.string.hintRadioStream );
								}
							}
						} );

						intBuilder.show();
						break;

					case 5:
						// random
						lblSound.setText( "[random]" );
						break;

					case 6:
					default:
						// none
						lblSound.setText( "[none]" );
						break;
				}

			}


			public void onNothingSelected (AdapterView< ? > parent)
			{}
		} );
	}


	public void onClickOk (View v)
	{
		TimeTrigger tt = (TimeTrigger) TriggerHandler.getTrigger( mAlarmIdx );

		// clear focus in case user had entry-box still focused. Otherwise the latest edit won't be committed.
		timePicker.clearFocus();
		recDate.clearFocus();

		// get time from ui
		Time tm = new Time();
		tm.setToNow();

		tm.hour = timePicker.getCurrentHour();
		tm.minute = timePicker.getCurrentMinute();
		tm.second = 0;

		tt.enabled = dialogBtnEnabled.isChecked();
		tt.setAlarmSound( (String) lblSound.getText() );
		tt.setName( txtName.getText().toString() );
		tt.text = txtMessage.getText().toString();

		// check recurrence settings
		switch (recGroup.getCheckedRadioButtonId())
		{
			case R.id.radioDay:
				tt.recurrence = "daily";
				break;

			case R.id.radioOnce:
				tt.recurrence = "once";
				tm.monthDay = recDate.getDayOfMonth();
				tm.month = recDate.getMonth();
				tm.year = recDate.getYear();
				break;

			case R.id.radioWeek:
				StringBuilder strb = new StringBuilder( 32 );
				if (mon.isChecked())
					strb.append( "mon" );
				if (tue.isChecked())
					strb.append( "tue" );
				if (wed.isChecked())
					strb.append( "wed" );
				if (thu.isChecked())
					strb.append( "thu" );
				if (fri.isChecked())
					strb.append( "fri" );
				if (sat.isChecked())
					strb.append( "sat" );
				if (sun.isChecked())
					strb.append( "sun" );
				tt.recurrence = strb.toString();
				break;
		}

		// set the final trigger time
		tt.setTriggerTime( tm.toMillis( true ) );

		// save to prefs
		tt.persistValues( this, null );

		// enforce values
		Space.doTriggerUpdate( this, false, false );

		// RESULT_OK means "changed" for our OptionsActivity
		setResult( RESULT_OK );
		finish();
	}


	public void onClickCancel (View v)
	{
		setResult( RESULT_CANCELED );
		finish();
	}


	public void onClickPreview (View v)
	{
		if ( ((ToggleButton) findViewById( R.id.btnPreview )).isChecked())
		{
			if (AudioHandler.startSong( this, (String) lblSound.getText(), 0.5f, false ) != 0)
			{
				Space.showToast( this, R.string.textErrorMedia );
				((ToggleButton) findViewById( R.id.btnPreview )).setChecked( false );
			}
		}
		else
		{
			AudioHandler.stopSong();
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume ()
	{
		super.onResume();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause ()
	{
		m_MediaPlayer.release();
		AudioHandler.stopSong();
		super.onPause();
	}


}
