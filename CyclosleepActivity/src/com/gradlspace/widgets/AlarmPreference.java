/**
 * 
 */
package com.gradlspace.widgets;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;
import com.gradlspace.cys.TimeTrigger;
import com.gradlspace.cys.Trigger;
import com.gradlspace.cys.TriggerHandler;




/**
 * @author Falling
 * 
 */
public class AlarmPreference extends Preference
{
	private int						mAlarmIdx			= -1;
	private ToggleButton			btnEnabled			= null;
	private TextView				lblTitleText		= null;
	private View.OnClickListener	mClickAreaListener	= null;


	/**
	 * @param context
	 */
	public AlarmPreference (Context context, int alarmIdx)
	{
		super( context );
		initPreference( alarmIdx );
	}


	protected void initPreference (int alarmIdx)
	{
		mAlarmIdx = alarmIdx;
		setLayoutResource( R.layout.pref_alarm );
		updateDisplay();
	}


	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public AlarmPreference (Context context, AttributeSet attrs, int defStyle)
	{
		super( context, attrs, defStyle );
		initPreference( attrs.getAttributeUnsignedIntValue( "com.gradlspace.cys", "id", -1 ) );
	}


	/**
	 * @param context
	 * @param attrs
	 */
	public AlarmPreference (Context context, AttributeSet attrs)
	{
		super( context, attrs );
		initPreference( attrs.getAttributeUnsignedIntValue( "com.gradlspace.cys", "id", -1 ) );
	}


	/**
	 * @param context
	 */
	public AlarmPreference (Context context)
	{
		super( context );
		initPreference( -1 );
	}


	public int getAlarmId ()
	{
		return mAlarmIdx;
	}


	/**
	 * Set a listener for the clickable area of this preference
	 * 
	 * @param l
	 */
	public void setOnClickAreaListener (View.OnClickListener l)
	{
		mClickAreaListener = l;
	}


	@Override
	protected void onBindView (View view)
	{
		super.onBindView( view );

		if (mClickAreaListener != null)
			view.findViewById( R.id.clickArea ).setOnClickListener( mClickAreaListener );

		lblTitleText = (TextView) view.findViewById( R.id.lblTitleText );

		btnEnabled = (ToggleButton) view.findViewById( R.id.btnEnabled );
		btnEnabled.setOnClickListener( new View.OnClickListener() {

			public void onClick (View v)
			{
				// enabled button was clicked
				TimeTrigger tt = (TimeTrigger) TriggerHandler.getTrigger( AlarmPreference.this.mAlarmIdx );

				if (AlarmPreference.this.btnEnabled.isChecked())
				{
					tt.enabled = true;
				}
				else
				{
					tt.enabled = false;
				}

				tt.persistValues( getContext(), null );
				Space.doTriggerUpdate( getContext(), false, false );
			}
		} );

		updateDisplay();
	}


	public void updateDisplay ()
	{
		Trigger t = TriggerHandler.getTrigger( mAlarmIdx );

		if (t == null || TimeTrigger.class.isInstance( t ) == false)
		{
			setTitle( R.string.textNotSet );
			if (lblTitleText != null)
				lblTitleText.setText( " " );
			setSummary( R.string.textNA );
			if (btnEnabled != null)
				btnEnabled.setEnabled( false );
		}
		else
		{
			setTitle( ((TimeTrigger) t).getTimeString() );
			if (lblTitleText != null)
				lblTitleText.setText( ((TimeTrigger) t).getName() );
			setSummary( t.getAlarmSound() );
			if (btnEnabled != null)
				btnEnabled.setChecked( t.enabled );
		}
	}


	@Override
	protected Object onGetDefaultValue (TypedArray a, int index)
	{
		// This preference type's value type is Integer, so we read the default
		// value from the attributes as an Integer.
		return a.getInteger( index, 0 );
	}


	@Override
	protected void onSetInitialValue (boolean restoreValue, Object defaultValue)
	{
		if (restoreValue)
		{
			// Restore state
			mAlarmIdx = getPersistedInt( mAlarmIdx );
		}
		else
		{
			// Set state
			int value = (Integer) defaultValue;
			mAlarmIdx = value;
			persistInt( value );
		}

		updateDisplay();
	}


	@Override
	protected Parcelable onSaveInstanceState ()
	{
		/*
		 * Suppose a client uses this preference type without persisting. We
		 * must save the instance state so it is able to, for example, survive
		 * orientation changes.
		 */

		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent())
		{
			// No need to save instance state since it's persistent
			return superState;
		}

		// Save the instance state
		final SavedState myState = new SavedState( superState );
		myState.alarmIdx = mAlarmIdx;
		return myState;
	}


	@Override
	protected void onRestoreInstanceState (Parcelable state)
	{
		if (!state.getClass().equals( SavedState.class ))
		{
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState( state );
			return;
		}

		// Restore the instance state
		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState( myState.getSuperState() );
		mAlarmIdx = myState.alarmIdx;
		notifyChanged();
	}


	/**
	 * SavedState, a subclass of {@link BaseSavedState}, will store the state of MyPreference, a subclass of Preference.
	 * <p>
	 * It is important to always call through to super methods.
	 */
	private static class SavedState extends BaseSavedState
	{
		int	alarmIdx;


		public SavedState (Parcel source)
		{
			super( source );

			// Restore the click counter
			alarmIdx = source.readInt();
		}


		@Override
		public void writeToParcel (Parcel dest, int flags)
		{
			super.writeToParcel( dest, flags );

			dest.writeInt( alarmIdx );
		}


		public SavedState (Parcelable superState)
		{
			super( superState );
		}


		@SuppressWarnings ("unused")
		public static final Parcelable.Creator< SavedState >	CREATOR	= new Parcelable.Creator< SavedState >() {
																			public SavedState createFromParcel (Parcel in)
																			{
																				return new SavedState( in );
																			}


																			public SavedState[] newArray (int size)
																			{
																				return new SavedState[ size ];
																			}
																		};
	}
}
