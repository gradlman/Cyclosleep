/**
 * 
 */
package com.gradlspace.cys;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;

import android.content.Context;
import android.text.format.Time;
import android.util.Log;

import com.gradlspace.cys.FileManager.FileRequest;
import com.gradlspace.cys.SleepStage.SleepPhase;




/**
 * @author Falling
 * 
 */
public class Hypnogram
{

	public ArrayList< SleepStage >	stages	= new ArrayList< SleepStage >();
	public byte						rating	= -1;
	public String					absolutePath;
	public Stats					stats	= new Stats();


	/**
	 * Structure that contains statistical information about the Hypnogram's data
	 * 
	 * @author Falling
	 * 
	 */
	public static class Stats
	{
		/** total timespan [milliseconds] covered by all stages */
		public long											timeSpan		= 0;

		public long											timeStart		= 0;
		public long											timeEnd			= 0;

		/** total timespan [milliseconds] of "quiet/good" sleep (DEEP|...|REM) */
		public long											sleepSpan		= 0;

		public long											sleepSpanDawn	= 0;

		/** the average phase (float!!) */
		public float										averagePhase	= 0f;


		/** occurrence count of each phase */
		public EnumMap< SleepStage.SleepPhase, Integer >	phaseCount		= new EnumMap< SleepStage.SleepPhase, Integer >(
																					SleepStage.SleepPhase.class );

		/** occurrence distribution of each phase weighted by span [normalized to 1] */
		public EnumMap< SleepStage.SleepPhase, Double >		phaseDistrib	= new EnumMap< SleepStage.SleepPhase, Double >(
																					SleepStage.SleepPhase.class );
	}


	public void clear ()
	{
		stages.clear();
		rating = -1;
		absolutePath = null;
	}


	/**
	 * Get the timespan covered by all recorded sleep stages and internally fill the sleepSpan with time slept.
	 * 
	 * @return
	 */
	public long getTimespan ()
	{
		if (stages.size() <= 0)
		{
			return 0;
		}

		stats.sleepSpan = 0;
		stats.timeSpan = 0;
		stats.sleepSpanDawn = 0;

		stats.timeStart = stages.get( 0 ).startMillis;

		for (SleepStage css : stages)
		{
			stats.timeSpan += css.durationMillis;
			if (css.phase == SleepPhase.DEEP || css.phase == SleepPhase.REM || css.phase == SleepPhase.TWILIGHT
					|| css.phase == SleepPhase.DAWN)
			{
				if (css.phase != SleepPhase.DAWN)
				{
					stats.sleepSpan += css.durationMillis;
					stats.sleepSpanDawn += css.durationMillis;
				}
				else
					stats.sleepSpanDawn += css.durationMillis;
			}
		}

		stats.timeEnd = stats.timeStart + stats.timeSpan;

		return stats.timeSpan;
	}


	/**
	 * (Re-)Calculates the statistics
	 */
	public void calculateStats ()
	{
		if (stages.size() < 1)
			return;

		getTimespan();
		getAveragePhase();
		countOccurrences();

	}


	/**
	 * Returns the average phase weighted by duration.
	 * 
	 * @return
	 */
	public float getAveragePhase ()
	{
		long avg = 0;

		for (SleepStage css : stages)
		{
			avg += css.phase.ordinal() * css.durationMillis;
		}

		long span = getTimespan();
		if (span <= 0)
			return 0f;

		stats.averagePhase = ((float) avg / span);

		return stats.averagePhase;
	}


	/**
	 * Counts phase occurrences to Stats.
	 * 
	 * @return
	 */
	public void countOccurrences ()
	{
		Integer entry = null;
		Double fent = null;

		stats.phaseCount.clear();
		stats.phaseDistrib.clear();

		// iterate hypnogram
		for (SleepStage css : stages)
		{
			entry = stats.phaseCount.get( css.phase );
			fent = stats.phaseDistrib.get( css.phase );
			if (entry == null)
			{
				// first entry of that stage
				stats.phaseCount.put( css.phase, 1 );
				if (stats.timeSpan != 0)
					stats.phaseDistrib.put( css.phase, (double) ((double) css.durationMillis / stats.timeSpan) );
				else
					stats.phaseDistrib.put( css.phase, 0d );
			}
			else
			{
				// add to previous entry
				stats.phaseCount.put( css.phase, entry + 1 );
				if (stats.timeSpan != 0)
					stats.phaseDistrib.put( css.phase, (double) fent + ((double) css.durationMillis / stats.timeSpan) );
				else
					stats.phaseDistrib.put( css.phase, 0d );
			}

			entry = null;
			fent = null;
		}

		// fill missing stages
		for (SleepStage.SleepPhase p : SleepStage.SleepPhase.values())
		{
			if (stats.phaseCount.get( p ) == null)
			{
				stats.phaseCount.put( p, 0 );
				stats.phaseDistrib.put( p, 0d );
			}
		}
	}


	/**
	 * Checks if the last stage entry has a valid duration and if not, fills it with the difference to timeEnd
	 * 
	 * @param timeEnd
	 */
	public void fillLastDuration (long timeEnd)
	{
		if (stages.size() > 1)
		{
			if (stages.get( stages.size() - 1 ).durationMillis <= 0)
			{
				stages.get( stages.size() - 1 ).durationMillis = timeEnd - stages.get( stages.size() - 1 ).startMillis;
			}
		}
	}


	/**
	 * Load Hypnogram from shf file.
	 * 
	 * @param con
	 * @param filename
	 * @param hasPath
	 *            Path in filename?
	 * @return
	 */
	public int loadFromFile (Context con, String filename, boolean hasPath)
	{
		BufferedReader buf = null;

		try
		{
			if (filename == null)
				return -1;

			if (hasPath == false)
			{
				File f = FileManager.getFilesDir( con, FileRequest.READ );
				if (f == null)
					return 0;

				buf = new BufferedReader( new FileReader( new File( f, filename ) ) );
			}
			else
			{
				buf = new BufferedReader( new FileReader( filename ) );
			}

			stages.clear();

			String[] strsplit = new String[ 4 ];


			// read header
			String line = buf.readLine();
			strsplit = line.split( "\\s+", 4 );
			if (strsplit.length > 3)
			{
				try
				{
					rating = Byte.parseByte( strsplit[ 3 ] );
				}
				catch (NumberFormatException e)
				{
					rating = -1;
				}
			}
			else
				rating = -1;

			SleepStage css = null;

			while ( (line = buf.readLine()) != null)
			{
				strsplit = line.split( "\\s+", 3 );

				css = new SleepStage();
				css.phase = SleepPhase.valueOf( SleepPhase.class, strsplit[ 0 ] );
				css.startMillis = new Long( strsplit[ 1 ] );
				css.durationMillis = new Long( strsplit[ 2 ] );

				if (css.durationMillis < 0 || css.startMillis < 0)
				{
					Log.e( "FileReader", "Error reading file! Probably no shf format." );
					buf.close();
					return 0;
				}

				stages.add( css );
			}

			buf.close();

			return stages.size();

		}
		catch (IOException e)
		{
			// Unable to create file, likely because external storage is
			// not currently mounted.
			Log.w( "Storage", "Error reading " + filename, e );
		}
		return 0;
	}


	/**
	 * Save Hypnogram to filename
	 * 
	 * @param con
	 * @param filename
	 * @return
	 */
	public boolean saveToFile (Context con, String filename)
	{
		try
		{
			if (stages.size() <= 0)
				return true;

			if (filename == null)
			{
				Time tt = new Time();
				tt.set( System.currentTimeMillis() );
				filename = "hypn_" + tt.format2445() + FileManager.HYPNO_EXT;
			}


			File dir = FileManager.getFilesDir( con, FileRequest.WRITE_SMALL );
			if (dir == null)
				return false;
			File f = new File( dir, filename );

			FileWriter fw = new FileWriter( f, true );

			absolutePath = f.getAbsolutePath();


			// write header
			fw.write( String.format( "SleepStage StartTimestamp DurationMillis %d\n", rating ) );

			// write all values
			for (SleepStage css : stages)
			{
				fw.write( css.phase.name() + " " + css.startMillis + " " + css.durationMillis + "\n" );
			}

			fw.close();

			return true;

		}
		catch (IOException e)
		{
			Log.w( "Storage", "Error writing " + filename, e );
		}

		return false;
	}

}
