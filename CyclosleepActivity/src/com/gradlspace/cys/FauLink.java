/**
 * 
 */
package com.gradlspace.cys;


import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.CysInternalData.SleepRecord;




/**
 * @author Falling
 * 
 */
public class FauLink
{
	public static int uploadData ()
	{

		// eine roh sensor datei + 21 hypno's + bewertung + accel-facts

		// iterate all known sleep logs

		// check alread-uploaded flag in database

		// add if not uploaded yet


		// attach all to mail

		return 0;
	}


	/**
	 * Update Faup related variables.
	 */
	public static void update ()
	{
		if (Space.s_dbData == null || Space.s_dbData.m_sleepRecords == null || CyopsBoolean.FAUP_ENABLED.isNotEnabled())
		{
			// don't update if feature isn't enabled.
			return;
		}

		s_numCollected = 0;

		// count number of verbose recordings
		for (SleepRecord rec : Space.s_dbData.m_sleepRecords)
		{
			if (rec.sensfile != null)
			{
				if (rec.duration > Cyops.MILLIS_4H && FileManager.existsFile( rec.sensfile ))
					++s_numCollected;
			}
		}
	}


	/**
	 * @return Number of verbose recordings.
	 */
	public static int getNumCollected ()
	{
		return s_numCollected;
	}


	public static String getStatusText ()
	{
		return new String( "Data: " + s_numCollected + " / " + NUM_REQUIRED );
	}


	/**
	 * @return true if enough recordings are available to perform the upload.
	 */
	public static boolean isReadyForUpload ()
	{
		if (s_numCollected >= NUM_REQUIRED)
		{
			return true;
		}

		return false;
	}


	private static int			s_numCollected	= 0;

	private static final int	NUM_REQUIRED	= 14;
}
