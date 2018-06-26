/**
 * 
 */
package com.gradlspace.cys;


import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;




/**
 * @author Falling
 * 
 */
public class Cyops
{
	// ==============================================
	// == Static settings
	// ==============================================

	public static final long	MILLIS_1H		= 3600000;

	public static final long	MILLIS_PRESTART	= 90 * 60000;

	/** milliseconds of 24 hours */
	public static final long	MILLIS_24H		= 24 * MILLIS_1H;
	public static final long	MILLIS_12H		= 12 * MILLIS_1H;
	public static final long	MILLIS_4H		= 4 * MILLIS_1H;
	public static final long	MILLIS_2H		= 2 * MILLIS_1H;


	/**
	 * Wrapper for the default shared preferences
	 * 
	 * @return
	 */
	public static SharedPreferences spref ()
	{
		return PreferenceManager.getDefaultSharedPreferences( Space.get() );
	}


	/**
	 * Cys settings of boolean type
	 * 
	 * @author Falling
	 * 
	 */
	public enum CyopsBoolean
	{
		// @formatter:off
		SCIENCE_ENABLED ( "pk_science", true ),
		FAUP_ENABLED ( "pk_faup_enabled", false ),
		
		SLEEPING ( "pk_sm_on", false ),		
		AUTOROT ( "pk_sm_autorot", true ),
		AUTOROT_WAS_SET ( "pk_ar_set", false ),	
		APMODE ( "pk_sm_apmode", true ),
		APMODE_WAS_SET ( "pk_apm_set", false ),
		
		
		DATA_ENABLED ( "pk_data_enable", true ),
		DATA_ZIP ( "pk_data_zip", true ),
		DATA_AUTO_CLEAN ( "pk_data_autocleanup", true ),
		DATA_INCLUDE_HYPNO ( "pk_data_inchypno", false ),
		DATA_INCLUDE_DREAM ( "pk_data_incdream", false ),
		DATA_NOSENSOR ( "pk_data_nosensor", true ),
		
		RATE_SLEEP ( "pk_aa_sleeprating", true ),
		RATE_DREAM ( "pk_dream_ask", false ),
		
		NO_SOUNDS ( "pk_aa_nosounds", false ),
		IGNORE_SILENT ( "pk_aa_ignoresilent", false ),
		NO_VIB ( "pk_aa_novib", false ),
		
		ONLY_ONCE ( "pk_aa_onlyonce", true ),
		ALARM_RIOT ( "pk_aa_riot", true ),
		SPEECH_ATIME ( "pk_speech_atime", false ),
		SPEECH_MSG ( "pk_speech_message", false ),
		
		SPT_NO_AUTOADAPT ( "pk_spt_noautoadapt", false ),
				
		
		REPORT_BUG ( "pk_g_report", true ),
		EULA_ACCEPTED ( "pk_eula", false ),
		IS_FIRST_START ( "pk_first", true ),
		
		/** Returns whether simple mode is set. Meaning the app should behave just like any other alarm app - no fancy sensor
		 *  tracking, monitoring or sleep phase detection */
		IS_SIMPLE_MODE ( "pk_g_simplemode", false ),
		
		IS_CRIPPLED ( "pk_g_crippled", false ),
		
		QUICKTIMER ( "pk_g_quicktimer", false ),
		
		PERSISTENT_ICON ( "pk_g_persistent", true ),
		
		SHOW_HINTS ( "pk_g_hints", true ),
		
		VERBOSE ( "pk_data_verbose", false ),
		
		LAST ("null", false);
		// @formatter:on

		public final boolean	defaultValue;
		public final String		key;


		CyopsBoolean (String key, boolean defaultValue)
		{
			this.defaultValue = defaultValue;
			this.key = key;
		}


		public boolean get ()
		{
			return spref().getBoolean( key, defaultValue );
		}


		public boolean isEnabled ()
		{
			return spref().getBoolean( key, defaultValue );
		}


		public boolean isNotEnabled ()
		{
			return (spref().getBoolean( key, defaultValue ) == false);
		}


		public void set (boolean newValue)
		{
			spref().edit().putBoolean( key, newValue ).commit();
		}


		public void disable ()
		{
			spref().edit().putBoolean( key, false ).commit();
		}


		public void enable ()
		{
			spref().edit().putBoolean( key, true ).commit();
		}
	}


	/**
	 * Cys settings of int type
	 * 
	 * @author Falling
	 * 
	 */
	public enum CyopsInt
	{
		// @formatter:off	
		BATT_USAGE ( "pk_battery_usage", 20 ),
		
		LAF_SECURE ( "pk_laf_sec", 0 ),
		LAF_NORMAL ( "pk_laf_norm", 0 ),
		
		LAST ("null", 0);
		// @formatter:on

		public final int	defaultValue;
		public final String	key;


		CyopsInt (String key, int defaultValue)
		{
			this.defaultValue = defaultValue;
			this.key = key;
		}


		public int get ()
		{
			return spref().getInt( key, defaultValue );
		}


		public void set (int newValue)
		{
			spref().edit().putInt( key, newValue ).commit();
		}
	}


	/**
	 * Cys settings of long type
	 * 
	 * @author Falling
	 * 
	 */
	public enum CyopsLong
	{
		// @formatter:off	
		SM_START_TIME ( "pk_sm_starttime", 0 ),
		
		LAST ("null", 0);
		// @formatter:on

		public final long	defaultValue;
		public final String	key;


		CyopsLong (String key, long defaultValue)
		{
			this.defaultValue = defaultValue;
			this.key = key;
		}


		public long get ()
		{
			return spref().getLong( key, defaultValue );
		}


		public void set (long newValue)
		{
			spref().edit().putLong( key, newValue ).commit();
		}
	}


	/**
	 * Cys settings of float type
	 * 
	 * @author Falling
	 * 
	 */
	public enum CyopsFloat
	{
		// @formatter:off	
		LAST ("null", 0f);
		// @formatter:on

		public final float	defaultValue;
		public final String	key;


		CyopsFloat (String key, float defaultValue)
		{
			this.defaultValue = defaultValue;
			this.key = key;
		}


		public float get ()
		{
			return spref().getFloat( key, defaultValue );
		}


		public void set (float newValue)
		{
			spref().edit().putFloat( key, newValue ).commit();
		}
	}


	/**
	 * Cys settings of String type
	 * 
	 * @author Falling
	 * 
	 */
	public enum CyopsString
	{
		// @formatter:off	
		PREFIRE_TIME ( "pk_aa_prefire", "20" ),
		
		SM_ACTION ( "pk_sm_action", "ask" ),
		
		STREAM_TIMEOUT ( "pk_aa_streamtimeout", "40" ),
		
		DATA_PATH ( "pk_data_path", "ext" ),
		DATA_EXPIRE ( "pk_data_expire", "30" ),
		
		SPT_AWAKE ( "pk_spt_awake", "480" ),
		SPT_DAWN ( "pk_spt_dawn", "250" ),
		SPT_REM ( "pk_spt_rem", "60" ),
		SPT_TWIL ( "pk_spt_twil", "40" ),		
		SPT_VAR_THRES ( "pk_spt_varth", "250" ),
		
		ALARM_TIMEOUT ( "pk_aa_atimeout", "600000" ),
		ALARM_COMMIT ( "pk_aa_commita", "none" ),
		
		SPEECH_INTERVAL ( "pk_speech_inter", "3" ),
		
		CRIPPLE_WORKAROUND ( "pk_sm_crippleMode", "none" ),
		
		LAST ("null", "null");
		// @formatter:on

		public final String	defaultValue;
		public final String	key;


		CyopsString (String key, String defaultValue)
		{
			this.defaultValue = defaultValue;
			this.key = key;
		}


		public String get ()
		{
			return spref().getString( key, defaultValue );
		}


		public void set (String newValue)
		{
			spref().edit().putString( key, newValue ).commit();
		}


		public boolean setInt (int newValue)
		{
			return spref().edit().putString( key, Integer.toString( newValue ) ).commit();
		}
	}


	// ==============================================
	// == Special/meta setting types
	// ==============================================

	public static final String	TRIGGER_SOUND	= "pk_t_sound_";
	public static final String	TIMER_SOUND		= "pk_timer_sound";

	public static final String	TRIGGER_ENABLED	= "pk_t_enabled_";
	public static final String	TRIGGER_TIME	= "pk_t_time_";
	public static final String	TRIGGER_RECC	= "pk_t_rec_";


	/**
	 * Alarm song filename. If idx == -1, the sound for the quick timer is returned
	 * 
	 * @param con
	 * @param idx
	 * @return
	 */
	public static String getTriggerSound (int idx)
	{
		if (idx == -1)
		{
			return spref().getString( TIMER_SOUND, "[trigger]" );
		}

		return spref().getString( TRIGGER_SOUND + idx, "[none] Click to set!" );
	}


	/**
	 * Alarm state (enabled/disabled)
	 * 
	 * @param idx
	 * @return
	 */
	public static boolean isTriggerEnabled (int idx)
	{
		return spref().getBoolean( TRIGGER_ENABLED + idx, false );
	}


	public static void setEulaAccepted (boolean yes)
	{
		Editor ed = spref().edit();
		ed.putBoolean( CyopsBoolean.EULA_ACCEPTED.key, yes );
		if (ed.commit() == false)
		{
			Log.e( "EULAmanager", "Error committing eula prefs!" );
		}
	}


	// public static String prefGetCrippleWorkaround ()
	// {
	// return Space.spref().getString( "pk_sm_crippleMode", "none" );
	// }


	public static boolean isVerbose ()
	{
		return (CyopsBoolean.FAUP_ENABLED.isEnabled() || CyopsBoolean.VERBOSE.isEnabled());
	}

}
