/**
 * 
 */
package com.gradlspace.cys;


import android.content.res.Resources;




/**
 * @author Falling
 * 
 */
public class SleepStage
{

	public enum SleepPhase
	{
		UNKNOWN,

		/**
		 * well, presumably stone dead, ex-parrot
		 */
		DEAD,

		/**
		 * Phase 3-4
		 */
		DEEP,

		/**
		 * Phase 1-2
		 */
		TWILIGHT,

		/**
		 * REM sleep
		 */
		REM,

		/**
		 * half-awake, brief awakening
		 */
		DAWN,

		/**
		 * awake
		 */
		AWAKE;


		/**
		 * Calculates the reverse ordinal plus ONE!! (ordinal() = 1 ==> ordinalRev() = 6
		 * 
		 * @return the reverse ordinal + 1
		 */
		public final int ordinalRev ()
		{
			return (7 - this.ordinal());
		}


		public final String getText (Resources res)
		{
			switch (this)
			{
				case DEAD:
					return res.getString( R.string.textStageDead );
				case DEEP:
					return res.getString( R.string.textStageDeep );
				case TWILIGHT:
					return res.getString( R.string.textStageTwilight );
				case REM:
					return res.getString( R.string.textStageRem );
				case DAWN:
					return res.getString( R.string.textStageDawn );
				case AWAKE:
					return res.getString( R.string.textStageAwake );
				default:
			}
			return "n/a";
		}
	}


	public SleepPhase	phase;
	public long			startMillis;
	public long			durationMillis;


	public SleepStage (SleepPhase phase, long startMillis, long durationMillis)
	{
		this.phase = phase;
		this.startMillis = startMillis;
		this.durationMillis = durationMillis;
	}


	public SleepStage ()
	{
		this.phase = SleepPhase.UNKNOWN;
		this.startMillis = 0;
		this.durationMillis = 0;
	}
}
