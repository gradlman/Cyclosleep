/**
 * 
 */
package com.gradlspace.widgets;


import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;




/**
 * @author Falling
 * 
 */
public class TimeEdit extends EditText
{
	/** Interval [in minutes] of progress-bar-oriented time representations */
	public static final int	TIME_PROGRESS_INTERVAL	= 30;
	/** Maximum progress integer for progress-bar-oriented time representations */
	public static final int	MAX_TIME_PROGRESS		= 1440 / TIME_PROGRESS_INTERVAL;	// 1440 = 24h * 60min
	public static final int	TIME_PROGRESS_DIVISOR	= 60 / TIME_PROGRESS_INTERVAL;


	/**
	 * Parses the given String for all possible variations of time information in the form of [(h)(h)](:)[(m)(m)]
	 * 
	 * @param str
	 *            The String to parse for a time.
	 * @return A new valid Time object or null if the given String is no valid time.
	 */
	public static Time parseTimeString (String str)
	{
		if (str != null && str.length() > 0)
		{
			Time t = new Time();

			try
			{
				t.setToNow();
				t.second = 0;
				int item = 0;

				// == [*]:[*]
				int colonIdx = str.indexOf( ':' );
				if (colonIdx == -1)
				{
					// NO colon

					// ==============> [**] (1 or 2 chars)
					if (str.length() < 3)
					{
						// Could be [HH], [mm] or invalid
						item = Integer.parseInt( str );
						if (item >= 0 && item <= 23)
						{
							// hour
							t.hour = item;
						}
						else if (item >= 24 && item <= 59)
						{
							// minute
							t.minute = item;
						}
						else
						{
							// invalid
							return null;
						}
					}
					// <=============
					// ==============> [***] (3 chars)
					else if (str.length() == 3)
					{
						// Could be [Hmm], [HHm], [HH?], [mm?] or invalid
						item = Integer.parseInt( str.substring( 0, 2 ) );
						if (item >= 0 && item <= 23)
						{
							// hour
							t.hour = item;
							item = Integer.parseInt( str.substring( 2, 3 ) );
							if (item >= 0 && item <= 9)
							{
								// minute
								t.minute = item;
							}
							else
							{
								// invalid 3rd char, ignore
							}
						}
						else if (item >= 24 && item <= 59)
						{
							t.minute = item;
							// ignore 3rd char
						}
						else
						{
							// try [Hmm]
							item = Integer.parseInt( str.substring( 0, 1 ) );
							if (item >= 0 && item <= 9)
							{
								t.hour = item;
								item = Integer.parseInt( str.substring( 1, 3 ) );
								if (item >= 0 && item <= 59)
								{
									// minute
									t.minute = item;
								}
								else
								{
									// invalid 3rd char, ignore
								}
							}
						}
					}
					// <=============
					// ==============> [*********] (more than 3 chars)
					else
					{
						// only valid options is [HHmm]
						item = Integer.parseInt( str.substring( 0, 2 ) );
						if (item >= 0 && item <= 23)
						{
							t.hour = item;
							item = Integer.parseInt( str.substring( 2, 4 ) );
							if (item >= 0 && item <= 59)
							{
								t.minute = item;
							}
						}
						else
						{
							// assume invalid
							return null;
						}
					}
					// <=============
				}
				else
				{
					// COLON
					// Could be [h]:[mm], [hh]:[m], [h]:[m], [hh]:[mm] or invalid
					if (colonIdx == 0 && str.length() > 2)
					{
						// :[mm]
						item = Integer.parseInt( str.substring( 1, 3 ) );
						if (item >= 0 && item <= 59)
						{
							t.minute = item;
						}
						else
						{
							// invalid
							return null;
						}
					}
					else if (colonIdx == 1)
					{
						// [h]:[mm]
						item = Integer.parseInt( str.substring( 0, 1 ) );
						if (item >= 0 && item <= 9)
						{
							t.hour = item;
							if (str.length() == 3)
							{
								// [h]:[m]
								item = Integer.parseInt( str.substring( 2, 3 ) );
								if (item >= 0 && item <= 9)
								{
									t.minute = item;
								}
								else
								{
									// invalid
									return null;
								}
							}
							else
							{
								// [h]:[mm]
								item = Integer.parseInt( str.substring( 2, 4 ) );
								if (item >= 0 && item <= 59)
								{
									t.minute = item;
								}
								else
								{
									// ignore minutes
								}
							}
						}
					}
					else
					{
						// colonIdx >= 2
						// [hh]:[mm]
						item = Integer.parseInt( str.substring( colonIdx - 2, colonIdx ) );
						if (item >= 0 && item <= 23)
						{
							t.hour = item;
							if (colonIdx == 2 && str.length() == 4)
							{
								// [hh]:[m]
								item = Integer.parseInt( str.substring( 3, 4 ) );
								if (item >= 0 && item <= 9)
								{
									t.minute = item;
								}
							}
							else if (str.length() > 4 && colonIdx < str.length() - 2)
							{
								// [hh]:[mm]
								item = Integer.parseInt( str.substring( colonIdx + 1, colonIdx + 3 ) );
								if (item >= 0 && item <= 59)
								{
									t.minute = item;
								}
							}
						}
						else
						{
							// invalid
							return null;
						}
					}
				}
			}
			catch (NumberFormatException e)
			{
				t = null;
			}
			catch (IndexOutOfBoundsException e)
			{
				t = null;
			}

			return t;
		}

		return null;
	}


	private Time		mTime			= new Time();
	private EditText	mHourSlave		= null;


	private SeekBar		mSeekerSlave	= null;


	/**
	 * @param context
	 */
	public TimeEdit (Context context)
	{
		super( context );
		initTimeEdit();
	}


	/**
	 * @param context
	 * @param attrs
	 */
	public TimeEdit (Context context, AttributeSet attrs)
	{
		super( context, attrs );
		initTimeEdit();
	}


	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public TimeEdit (Context context, AttributeSet attrs, int defStyle)
	{
		super( context, attrs, defStyle );
		initTimeEdit();
	}


	/**
	 * Retrieves the time contained in the text of this EditText and updates the internal time attribute.
	 * 
	 * @return The time of the TextView.
	 */
	public Time getTime ()
	{
		String str = this.getText().toString();

		Time t = TimeEdit.parseTimeString( str );

		if (t != null)
		{
			mTime = t;
		}

		return mTime;
	}


	/**
	 * Global initializations for TimeEdit.
	 */
	public void initTimeEdit ()
	{
		// ==============> FOCUS change listener
		this.setOnFocusChangeListener( new OnFocusChangeListener() {

			public void onFocusChange (View v, boolean hasFocus)
			{
				if (!hasFocus && m_SeekerLock == false)
				{
					TimeEdit.this.reflectTime();
				}
			}
		} );
		// <=============

		// ==============> Text change listener for TimeEdit text.
		this.addTextChangedListener( new TextWatcher() {

			public void afterTextChanged (Editable s)
			{
				if (TimeEdit.this.hasFocus() && m_SeekerLock == false)
				{
					// if we have the focus, check input
					String str = s.toString();
					if (str.length() == 2 && str.indexOf( ':' ) == -1)
					{
						if (Integer.parseInt( str ) <= 23)
							s.append( ':' );
						else if ( (Integer.parseInt( str.substring( 0, 1 ) ) >= 2 && Integer.parseInt( str.substring( 1, 2 ) ) >= 5)
								|| (Integer.parseInt( str.substring( 0, 1 ) ) >= 3 && Integer.parseInt( str.substring( 1, 2 ) ) >= 0))
							s.insert( 1, ":" );
					}
				}
			}


			public void beforeTextChanged (CharSequence s, int start, int count, int after)
			{}


			public void onTextChanged (CharSequence s, int start, int before, int count)
			{}
		} );
		// <=============
	}


	/**
	 * Convenience method to reparse the text and set the internal time.
	 */
	public void reflectTime ()
	{
		setTime( getTime(), null );
	}


	/**
	 * Sets a slave EditText that is watched for (hour) text changes.
	 * 
	 * @param hourSlave
	 */
	public void setHourSlave (EditText hourSlave)
	{
		mHourSlave = hourSlave;
		if (mHourSlave != null)
		{
			mHourSlave.addTextChangedListener( new TextWatcher() {

				public void afterTextChanged (Editable s)
				{}


				public void beforeTextChanged (CharSequence s, int start, int count, int after)
				{}


				public void onTextChanged (CharSequence s, int start, int before, int count)
				{
					// only reflect change if edittext has focus
					if (TimeEdit.this.mHourSlave.hasFocus() && m_SeekerLock == false)
					{
						try
						{
							float hours = Float.parseFloat( s.toString() );
							if (hours > 0)
							{
								TimeEdit.this.setTime(	(long) (System.currentTimeMillis() + hours * 3600000),
														TimeEdit.this.mHourSlave );
							}
						}
						catch (NumberFormatException e)
						{
							// do nothing
						}
					}
				}
			} );
		}
	}


	// private TransitionDrawable m_transDraw = null;

	protected static boolean	m_SeekerLock	= false;


	/**
	 * Sets the slave SeekBar that will be attached to this TimeEdit.
	 * 
	 * @param seekerSlave
	 */
	public void setSeekerSlave (SeekBar seekerSlave)
	{
		// m_transDraw = (TransitionDrawable) TimeEdit.this.getBackground();
		// m_transDraw.setCrossFadeEnabled( true );

		mSeekerSlave = seekerSlave;
		if (mSeekerSlave != null)
		{
			// set max progress
			mSeekerSlave.setMax( MAX_TIME_PROGRESS );

			// set on change listener
			mSeekerSlave.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
				private Time	mStartTime	= new Time();


				// private boolean mIsUserAction = false;


				public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser)
				{
					// Space.log( "onChange" );

					// only reflect change if the seeker has focus
					if (seekBar.isPressed())
					{
						// Read the current progress and set it as an hour:min representation in this TimeEdit.
						Time t = new Time( mStartTime );
						t.minute = progress * TIME_PROGRESS_INTERVAL % 60;
						t.hour = progress / TIME_PROGRESS_DIVISOR;
						TimeEdit.this.setTime( t, seekBar );
					}


				}


				public void onStartTrackingTouch (SeekBar seekBar)
				{
					mStartTime = TimeEdit.this.getTime();
					// m_transDraw.startTransition( 500 );
					m_SeekerLock = true;
				}


				public void onStopTrackingTouch (SeekBar seekBar)
				{
					// m_transDraw.reverseTransition( 500 );
					// Space.log( "stopTrack" );
					m_SeekerLock = false;
				}
			} );
		}
	}


	/**
	 * Sets the internal time and the text of the TextView.
	 * 
	 * @param millis
	 * @param masterControl
	 */
	public void setTime (long millis, View masterControl)
	{
		mTime.set( millis );
		this.setText( mTime.format( "%H:%M" ) );

		if (masterControl == null || masterControl == this)
		{
			updateHourSlaveText();
			updateSeekerSlave();
		}
		else if (masterControl == mHourSlave)
		{
			updateSeekerSlave();
		}
		else if (masterControl == mSeekerSlave)
		{
			updateHourSlaveText();
		}
	}


	/**
	 * Sets the internal time and the text of the TextView.
	 * 
	 * @param t
	 */
	public void setTime (Time t, View masterControl)
	{
		mTime = t;
		this.setText( t.format( "%H:%M" ) );

		if (masterControl == null || masterControl == this)
		{
			updateHourSlaveText();
			updateSeekerSlave();
		}
		else if (masterControl == mHourSlave)
		{
			updateSeekerSlave();
		}
		else if (masterControl == mSeekerSlave)
		{
			updateHourSlaveText();
		}
	}


	/**
	 * Updates the hour display in the HourSlave EditText, if one is set.
	 */
	public void updateHourSlaveText ()
	{
		if (mHourSlave != null)
		{
			long dist = mTime.toMillis( true ) - System.currentTimeMillis();

			if (dist < 0)
				dist += 86400000;

			mHourSlave.setText( String.format( "%.1f", dist / 3600000f ) );
		}
	}


	public void updateSeekerSlave ()
	{
		if (mSeekerSlave != null)
		{
			mSeekerSlave.setProgress( mTime.hour * TIME_PROGRESS_DIVISOR + mTime.minute / TIME_PROGRESS_INTERVAL );
		}
	}
}
