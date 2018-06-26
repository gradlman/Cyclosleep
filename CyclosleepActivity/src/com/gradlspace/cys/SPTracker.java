/**
 * 
 */
package com.gradlspace.cys;


import android.content.SharedPreferences.Editor;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsInt;
import com.gradlspace.cys.Cyops.CyopsLong;
import com.gradlspace.cys.Cyops.CyopsString;
import com.gradlspace.cys.CysInternalData.SleepRecord;
import com.gradlspace.cys.SleepStage.SleepPhase;




/**
 * Sleep Phase Tracker Static class that uses every input it gets for determining the current sleep phase
 * 
 * @author Falling
 * 
 */
public class SPTracker
{

	/**
	 * list of known cycles
	 */
	private static Hypnogram					mHypnogram				= new Hypnogram();

	/** Default all-night activity stream */
	public static SensorData					stream					= null;
	public static SleepRecord					sleep					= null;

	public static int							sAwakeMin;
	public static int							sDawnMin;
	public static int							sRemMin;
	public static int							sTwilightMin;


	public static final long					THRESHOLD_BLOCK_MILLIS	= 4 * 60000;
	public static final float					REDUCTION_MULT			= 0.8f;
	public static final float					REDUCTION_MULT_STRICT	= 0.5f;
	// public static final long THRESHOLD_ADAPT_MILLIS = 30 * 60000;

	public static final long					MIN_TIMESPAN			= 3600000;


	/** Threshold used for alarm triggers */
	public static int							varThreshold			= 250;

	public static int							batteryUsage			= 20;
	public static int							batteryLevel			= 100;
	public static int							batteryTemp				= 0;
	public static int							batteryLevelStart		= -1;


	/** maximum encountered sensor value */
	public static int							maxSensorValue			= 0;
	public static int							curSensorValue			= 0;
	public static float							meanSensorValue			= 2;


	public static long							millisNow				= 0;
	public static boolean						fireOnNext				= false;


	// public static String streamHeader = null;
	// public static final int NUM_VALUES = 8192;
	public static boolean						saveData				= false;
	public static boolean						autoAdapt				= false;

	public static boolean						isPrestart				= false;

	public static int							runAct					= 0;
	public static boolean						peaked					= false;
	public static boolean						calibrate				= false;
	public static final long					PPM_DURATION			= 60000;
	public static long							spanMillis				= PPM_DURATION;
	public static long							minute					= 5000;

	/** Holds the timestamp in milliseconds when to start with threshold tracking */
	public static long							millisStartTime			= 0;
	// public static float thresholdMultiplier = 0.3f;

	public static float							avgSleepHours			= 0f;
	// public static ArrayList< Float > avgSleepHourHist = new ArrayList< Float >( 14 );

	// public static PeakDetectionFilter peaks = new PeakDetectionFilter( 2, 0 );

	private static OnSleepPhaseChangeListener	sChangeListener			= null;


	// public static String s_lastStreamFile = "";


	// public static String s_lastVerbFile = "";


	public static class OnSleepPhaseChangeListener
	{
		/**
		 * Called upon transition from one sleep phase to another.
		 * 
		 * @param from
		 *            Previous sleep phase
		 * @param to
		 *            New sleep phase
		 */
		public void onPhaseChanged (SleepPhase from, SleepPhase to)
		{

		}
	}


	public static void loadSettings ()
	{
		// SharedPreferences p = Cyops.spref();
		sAwakeMin = Integer.parseInt( CyopsString.SPT_AWAKE.get() );
		// sAwakeMin = Integer.parseInt( p.getString( "pk_spt_awake", "480" ) );
		sDawnMin = Integer.parseInt( CyopsString.SPT_DAWN.get() );
		sRemMin = Integer.parseInt( CyopsString.SPT_REM.get() );
		sTwilightMin = Integer.parseInt( CyopsString.SPT_TWIL.get() );

		if (sAwakeMin > 800 || sDawnMin > 700 || sRemMin > 600 || sTwilightMin > 500)
		{
			sAwakeMin = 800;
			sDawnMin = 450;
			sRemMin = 150;
			sTwilightMin = 100;
		}
		else if (sAwakeMin < 50 || sDawnMin < 35 || sRemMin < 20 || sTwilightMin < 16)
		{
			sAwakeMin = 50;
			sDawnMin = 35;
			sRemMin = 20;
			sTwilightMin = 16;
		}

		// autoAdapt = !p.getBoolean( "pk_spt_noautoadapt", false );
		autoAdapt = CyopsBoolean.SPT_NO_AUTOADAPT.isNotEnabled();


		// if (MonitorActivity.sCalibrationMode != Calibrator.NULL || CyopsBoolean.DATA_NOSENSOR.isEnabled())
		// {
		// saveData = false;
		// }
		// else
		{
			saveData = CyopsBoolean.DATA_ENABLED.get();
		}

		varThreshold = Integer.parseInt( CyopsString.SPT_VAR_THRES.get() );
	}


	public static void saveSettings ()
	{
		Editor ed = Cyops.spref().edit();

		ed.putString( CyopsString.SPT_AWAKE.key, Integer.toString( sAwakeMin ) );
		ed.putString( CyopsString.SPT_DAWN.key, Integer.toString( sDawnMin ) );
		ed.putString( CyopsString.SPT_REM.key, Integer.toString( sRemMin ) );
		ed.putString( CyopsString.SPT_TWIL.key, Integer.toString( sTwilightMin ) );
		ed.putString( CyopsString.SPT_VAR_THRES.key, Integer.toString( varThreshold ) );

		ed.commit();
	}


	/**
	 * Called to initialize preference settings or reset the Tracker.
	 * 
	 * @param con
	 */
	public static void init ()
	{
		loadSettings();

		// DEBUG: override settings
		// sAwakeMin = 480;
		// sDawnMin = 250;
		// sRemMin = 60;
		// sTwilightMin = 40;

		sChangeListener = null;
		reset();

		loadAvgSleep();
	}


	public static void reset ()
	{
		Space.log( "Hypno reset" );
		// mHypnogram.clear();
		mHypnogram = new Hypnogram();
	}


	/**
	 * @return true if the current hypnogram should be rated by the RateActivity.
	 */
	public static boolean requiresRating ()
	{
		if (mHypnogram.stages.size() > 4 && mHypnogram.getTimespan() >= MIN_TIMESPAN)
		{
			if (CyopsBoolean.RATE_SLEEP.isNotEnabled() && CyopsBoolean.RATE_DREAM.isNotEnabled())
			{
				return false;
			}

			return true;
		}

		return false;
	}


	/**
	 * Saves the hypnogram, puts the sleep record into the database and updates threshold values.
	 * 
	 * @param p_sleepRec
	 *            The sleep record structure to use (should contain dream-file path), can be <code>null</code>
	 */
	public static void save ()
	{
		Space.log( "saving hypnogram..." );

		// fill last stage duration and calculate hypnogram stats
		mHypnogram.fillLastDuration( System.currentTimeMillis() );
		mHypnogram.calculateStats();

		// if hypnogram is too small, the sleep record is not saved
		if (!requiresRating())
		{
			Space.log( "...too small" );
			return;
		}

		float duration = mHypnogram.stats.sleepSpanDawn / 3600000f;
		long durationLong = mHypnogram.stats.sleepSpanDawn;

		if (isPrestart)
		{
			// monitor was prestarted, so we retrieve the sleeptime from our sleepStart mark
			long start = CyopsLong.SM_START_TIME.get();
			if (start == 0)
			{
				duration = 0f;
			}
			else
			{
				duration = (System.currentTimeMillis() - start) / 3600000f;
			}

			durationLong = System.currentTimeMillis() - start;
		}

		// if the sleep took more than one hour then adapt the thresholds
		if (duration > 1)
		{
			// // update sleep duration history
			// if (avgSleepHourHist.size() > 13)
			// avgSleepHourHist.remove( 0 );
			//
			// avgSleepHourHist.add( duration );
			// saveAvgSleep();

			// adapt thresholds
			if (autoAdapt)
			{
				int numAwake = mHypnogram.stats.phaseCount.get( SleepPhase.AWAKE );
				int numDawn = mHypnogram.stats.phaseCount.get( SleepPhase.DAWN );
				int numDeep = mHypnogram.stats.phaseCount.get( SleepPhase.DEEP );

				Space.logRelease( "autoAdapt: " + numAwake + "  " + numDawn + "  " + numDeep );

				if (duration > 3 && numAwake < 2 && numDawn < 2)
				{
					// threshold is too high
					if (varThreshold > 50)
					{
						varThreshold *= REDUCTION_MULT_STRICT;
						if (sRemMin > 28)
						{
							sAwakeMin *= REDUCTION_MULT_STRICT;
							sDawnMin *= REDUCTION_MULT_STRICT;
							sTwilightMin *= REDUCTION_MULT_STRICT;
							sRemMin *= REDUCTION_MULT_STRICT;
						}
						Space.logRelease( "Thresholds adjusted: " + varThreshold );
						saveSettings();
					}
				}
				else if ( (duration > 3 && duration < 7 && numAwake < 2 && numDawn < 5)
						|| (duration > 6 && numAwake < 4 && numDawn < 5) || (duration < 2 && numAwake < 1))
				{
					// threshold is too high
					if (varThreshold > 40)
					{
						varThreshold *= REDUCTION_MULT;
						if (sRemMin > 20)
						{
							sAwakeMin *= REDUCTION_MULT;
							sDawnMin *= REDUCTION_MULT;
							sTwilightMin *= REDUCTION_MULT;
							sRemMin *= REDUCTION_MULT;
						}
						Space.logRelease( "Thresholds adjusted: " + varThreshold );
						saveSettings();
					}
				}
				else if ( (duration < 8 && numAwake > 16 && numDawn > 24) || (duration > 7 && numAwake > 24 && numDawn > 32)
						|| (duration < 2 && numAwake > 12))
				{
					// threshold is too low
					if (varThreshold < 400)
					{
						varThreshold /= REDUCTION_MULT;
						if (sAwakeMin < 620)
						{
							sAwakeMin /= REDUCTION_MULT;
							sDawnMin /= REDUCTION_MULT;
							sTwilightMin /= REDUCTION_MULT;
							sRemMin /= REDUCTION_MULT;
						}
						Space.logRelease( "Thresholds adjusted: " + varThreshold );
						saveSettings();
					}
				}
			}
		}

		// save the actual hypnogram file
		mHypnogram.saveToFile( Space.get(), null );

		// check if a sleep record was provided
		// if (p_sleepRec == null)
		// {
		// p_sleepRec = new SleepRecord();
		// }

		// general sleep stats
		sleep.tstart = mHypnogram.stats.timeStart;
		sleep.tend = sleep.tstart + mHypnogram.stats.timeSpan;
		sleep.duration = mHypnogram.stats.timeSpan;
		sleep.durdeep = mHypnogram.stats.sleepSpan;
		sleep.durwake = durationLong;
		sleep.quality = mHypnogram.rating;
		sleep.comment = "Bat=" + batteryUsage + "; VT=" + varThreshold + "; RemT=" + sRemMin;

		Space.log( "quality (" + mHypnogram.rating + ") saved: " + sleep.quality );

		// fill the latest hypnogram and stream file paths into the record
		sleep.hypnofile = mHypnogram.absolutePath;
		// p_sleepRec.sensfile = s_lastStreamFile;

		// add record entry to the database
		Space.s_dbData.insert( sleep );

		// remove histogram from cache
		reset();
	}


	/**
	 * Loads sleep hour history from preference string.
	 * 
	 * @return
	 */
	public static float loadAvgSleep ()
	{
		avgSleepHours = 0f;

		long avgsum = 0;
		int i = 0;
		long dur;
		for (; i < 14 && i < Space.s_dbData.m_sleepRecords.size(); ++i)
		{
			dur = Space.s_dbData.m_sleepRecords.get( i ).durwake;

			// avgSleepHourHist.add( dur / 3600000f );
			avgsum += dur;
		}
		if (i > 0)
		{
			avgSleepHours = (avgsum / i) / 3600000f;
		}

		Space.log( "avg sleep: " + avgSleepHours );


		return avgSleepHours;
	}


	/**
	 * Returns the current hypnogram.
	 * 
	 * @return
	 */
	public static Hypnogram getHypnogram ()
	{
		return mHypnogram;
	}


	/**
	 * Sets a listener to be called when the sleep phase changed.
	 * 
	 * @param l
	 */
	public static void setOnPhaseChangeListener (OnSleepPhaseChangeListener l)
	{
		sChangeListener = l;
	}


	public static void initTracking ()
	{
		if (stream != null)
		{
			return;
		}


		loadSettings();

		// stream_plot = new SamplingPlot( "stream", Plot.generatePlotPaint( 1f, 255, 199, 45, 45 ), PlotStyle.LINE,
		// SensorData.DEFAULT_BUFFER_SIZE, false );
		//
		// Time tt = new Time();
		// tt.set( millisNow );
		// stream.setFile( FileManager.getFilesDirName( Space.get(), FileRequest.WRITE, "sens_" + tt.format2445()
		// + FileManager.SSD_EXT ) );

		stream = new SensorData();
		millisNow = stream.m_millisNow;

		sleep = new SleepRecord();
		sleep.sensfile = stream.getFilePath();

		// s_lastStreamFile = stream.getFilePath();


		// verbose stream
		// if (Cyops.isVerbose())
		// {
		// verbosePlot = new Plot3D( "verb_stream", Plot.generatePlotPaint(), PlotStyle.LINE, SPTracker.NUM_VALUES,
		// false );
		//
		// verbosePlot.setFile( FileManager.getFilesDirName( Space.get(), FileRequest.WRITE, "vsens_" + tt.format2445()
		// + FileManager.SSV_EXT ) );
		//
		// s_lastVerbFile = verbosePlot.getFile();
		// }

		// save stream header
		// streamHeader = buildHeaderString( Space.get(), millisNow, DeviceTestActivity.enumDefaultAccel( Space.get() )
		// );

		reset();
		runAct = sAwakeMin;


		// thresholdMultiplier = Float.parseFloat( Space.spref().getString( "pk_spt_thtop", "5.0" ) );
		// if (thresholdMultiplier >= 10f)
		// thresholdMultiplier = 10f;
		// else if (thresholdMultiplier < 4.1f)
		// thresholdMultiplier = 4.1f;

		// wait THRESHOLD_BLOCK_MILLIS msecs
		millisStartTime = millisNow + THRESHOLD_BLOCK_MILLIS;
		fireOnNext = false;
		peaked = false;
		// valuesAboveMean = 0;
		calibrate = false;


	}


	public static void saveFinalStream ()
	{
		if (stream != null)
		{
			stream.saveToFile( Space.get() );

			// FIXME: put battery status into separate class / settings
			SPTracker.batteryUsage = batteryLevelStart - batteryLevel;

			if (SPTracker.batteryUsage > 4)
			{
				CyopsInt.BATT_USAGE.set( SPTracker.batteryUsage );
				// Space.prefSetBatteryUsage( SPTracker.batteryUsage );
			}

			stream = null;
		}
	}


	/**
	 * Returns the current Sleep Phase we are probably in
	 * 
	 * @return
	 */
	public static SleepPhase getCurrentPhase ()
	{
		if (mHypnogram.stages.size() > 0)
		{
			return mHypnogram.stages.get( mHypnogram.stages.size() - 1 ).phase;
		}
		return SleepPhase.UNKNOWN;
	}


	/**
	 * Transit from the current sleep phase to newPhase
	 * 
	 * @param newPhase
	 */
	public static void transitPhase (long timeMillis, SleepPhase newPhase)
	{
		SleepPhase oldPhase = getCurrentPhase();

		if (oldPhase == newPhase)
		{
			return;
		}

		mHypnogram.stages.add( new SleepStage( newPhase, timeMillis, 0 ) );

		// fill in the duration of last stage
		if (mHypnogram.stages.size() > 1)
		{
			SleepStage ss = mHypnogram.stages.get( mHypnogram.stages.size() - 2 );
			ss.durationMillis = timeMillis - ss.startMillis;
		}

		// notify listener
		if (sChangeListener != null)
		{
			sChangeListener.onPhaseChanged( oldPhase, newPhase );
		}
	}


	/**
	 * Evaluate parameters from the sensors
	 * 
	 */
	public static SleepPhase evaluateValue (long ppm)
	{
		if (ppm > sAwakeMin)
			return SleepPhase.AWAKE;
		else if (ppm > sDawnMin)
			return SleepPhase.DAWN;
		else if (ppm > sRemMin)
			return SleepPhase.REM;
		else if (ppm > sTwilightMin)
			return SleepPhase.TWILIGHT;
		else
			return SleepPhase.DEEP;
	}


	/**
	 * Called on every monitor sensor change
	 * 
	 * @return true for fireOnNext
	 */
	public static boolean onSensorUpdate ()
	{
		// millisNow = System.currentTimeMillis();

		// add stream value
		// stream.addValueFast( curSensorValue, millisNow );

		// save stream data?
		// if (saveData && stream.values.head == NUM_VALUES - 1)
		// {
		// stream.saveToFile( Space.get(), null, streamHeader );
		// }

		if (meanSensorValue < 12)
			meanSensorValue = 12;

		if (calibrate)
		{
			if (meanSensorValue > 30)
				meanSensorValue = 30;
		}


		// we only check thresholds if we are above the mean
		if (curSensorValue > meanSensorValue)
		{
			if (curSensorValue > meanSensorValue * 1.75f)
			{
				++runAct;

				if (curSensorValue > meanSensorValue * 2f)
				{
					++runAct;

					if (curSensorValue > meanSensorValue * 2.5f)
					{
						// ++valuesAboveMean;
						++runAct;

						if (curSensorValue > meanSensorValue * 3f)
						{
							// ++valuesAboveMean;
							++runAct;

							if (curSensorValue > meanSensorValue * 4f)
							{
								++runAct;

								if (curSensorValue > meanSensorValue * 6f)
								// if (curSensorValue > meanSensorValue * thresholdMultiplier)
								{
									runAct += 8;
								}


							}

							if (runAct > varThreshold)
							{
								fireOnNext = true;
								peaked = true;
							}
						}
					}
				}
			}
		}


		if (runAct > 800)
		{
			fireOnNext = true;
			peaked = true;
			runAct = 800;
		}


		return fireOnNext;
	}
}
