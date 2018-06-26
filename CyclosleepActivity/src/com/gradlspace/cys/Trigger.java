/**
 * 
 */
package com.gradlspace.cys;


import junit.framework.Assert;
import android.content.Context;
import android.content.SharedPreferences.Editor;




/**
 * @author Falling
 * 
 */
public abstract class Trigger
{
	public int			id			= -1;
	public boolean		enabled		= false;


	/**
	 * A given trigger name, can be null, only used for the UI
	 */
	protected String	mName		= null;

	/** additional trigger text */
	public String		text		= "";


	/**
	 * the soundfile for the alarm
	 */
	protected String	mAlarmSound	= "[none] Click to set!";


	public void setAlarmSound (String file)
	{
		mAlarmSound = file;
	}


	public String getAlarmSound ()
	{
		return mAlarmSound;
	}


	/**
	 * @return the mName
	 */
	public String getName ()
	{
		if (mName == null)
			return ("Alarm " + (id + 1));
		return mName;
	}


	/**
	 * @param mName
	 *            the mName to set
	 */
	public void setName (String name)
	{
		this.mName = name;
	}


	public String getConfigString (Context con)
	{
		return getName();
	}


	public abstract String getFireString ();


	/**
	 * Assigns this Trigger an unique id inside TriggerHandler.
	 * 
	 * @param id
	 *            unique id in TriggerHandler representing this trigger
	 */
	public final void assign (int id)
	{
		Assert.assertTrue( "id invalid", id >= 0 );
		this.id = id;
	}


	/**
	 * Called by TriggerHandler after this Trigger has been attached and a valid id has been assigned to it. When
	 * overriding this method you MUST call through to Trigger!
	 * 
	 * @param con
	 */
	public void onInit (Context con)
	{
		retrieveValues( con );
	}


	public abstract long onFire (Context con, boolean forceFire);


	public void onDestroy ()
	{
		this.id = -1;
	}


	/**
	 * Load persistent values from shared prefs. When overriding, you have to call through to Trigger!
	 * 
	 * @param con
	 */
	public void retrieveValues (Context con)
	{
		enabled = Cyops.isTriggerEnabled( id );
		text = Cyops.spref().getString( "pk_t_text_" + id, "" );
		setName( Cyops.spref().getString( "pk_t_name_" + id, null ) );
		setAlarmSound( Cyops.getTriggerSound( id ) );
	}


	/**
	 * Saves the current trigger settings to the shared prefs.
	 * 
	 * @param con
	 * @param ed
	 *            can be null, will be used if not null
	 */
	public void persistValues (Context con, Editor ed)
	{
		if (ed == null)
			ed = Cyops.spref().edit();

		ed.putString( "pk_t_name_" + id, mName );
		ed.putBoolean( Cyops.TRIGGER_ENABLED + id, enabled );
		ed.putString( Cyops.TRIGGER_SOUND + id, mAlarmSound );
		ed.putString( "pk_t_text_" + id, text );

		ed.commit();
	}
}
