/**
 * 
 */
package com.gradlspace.cys;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.provider.Settings;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsString;




/**
 * @author Falling
 * 
 */
public class AudioHandler implements AudioManager.OnAudioFocusChangeListener
{

	private static AudioManager			sBoundAudioManager	= null;
	private static MediaPlayer			sMediaPlayer		= null;
	private static MediaPlayer			sSignalPlayer		= null;
	private static int					sMaxMusicVolume		= 0;
	private static int					sPrevMusicVolume	= -1;
	private static String				sPlayingSong		= null;
	private static InternalAlarm		sLastSignal			= null;
	private static ToneGenerator		s_toneGen			= null;
	private static BroadcastReceiver	s_mediaScanner		= null;
	private static IntentFilter			s_mediaFilter		= new IntentFilter( Intent.ACTION_MEDIA_SCANNER_FINISHED );


	/* (non-Javadoc)
	 * @see android.media.AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
	 */
	public void onAudioFocusChange (int focusChange)
	{
		switch (focusChange)
		{
			case AudioManager.AUDIOFOCUS_GAIN:
				// resume playback
				// if (mMediaPlayer == null) initMediaPlayer();
				// else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
				// mMediaPlayer.setVolume(1.0f, 1.0f);
				Space.log( "Audiofocus gained." );
				break;

			case AudioManager.AUDIOFOCUS_LOSS:
				// Lost focus for an unbounded amount of time: stop playback and release media player
				Space.log( "Audiofocus lost - All Sounds stopped." );
				sMediaPlayer.stop();
				// mMediaPlayer.release();
				// mMediaPlayer = null;
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				// Lost focus for a short time, but we have to stop
				// playback. We don't release the media player because playback
				// is likely to resume
				// if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
				Space.log( "Audiofocus loss transient - unhandled." );
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// Lost focus for a short time, but it's ok to keep playing
				// at an attenuated level
				// if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
				Space.log( "Audiofocus loss transient can duck - unhandled." );
				break;
		}
	}


	private static AudioManager.OnAudioFocusChangeListener	sChangeListener	= null;


	public enum InternalAlarm
	{
		BELL_CHILL (R.raw.bell_chill, "Bell \"Chill\""), BELL_HORN (R.raw.bell_horn, "Bell \"Containership\""), BELL_SIMPLE (
				R.raw.bell_simple, "Bell \"Simple\""), BELL_SOFT (R.raw.bell_soft, "Bell \"Soft\""), BELL_SIG (R.raw.bell_sig,
				"Bell \"Signal\"");

		private final int		resId;
		private final String	name;


		InternalAlarm (int resid, String name)
		{
			this.resId = resid;
			this.name = name;
		}


		int getResId ()
		{
			return resId;
		}


		String getName ()
		{
			return name;
		}
	}

	/**
	 * Nested class to organize the sound files
	 * 
	 * @author Falling
	 * 
	 */
	public static class Sounds
	{
		/**
		 * Array of Soundfile-Names
		 */
		public static String[]	mTitleArray	= null;

		/**
		 * Array of Filepaths
		 */
		// public static String[] mPathArray = null;
	}


	public static void registerMediaScannerListener ()
	{
		if (s_mediaScanner == null)
		{
			s_mediaScanner = new BroadcastReceiver() {
				private String	action	= null;


				@Override
				public void onReceive (Context context, Intent intent)
				{
					action = intent.getAction();
					if (action != null && action.equals( Intent.ACTION_MEDIA_SCANNER_FINISHED ))
					{
						AudioHandler.loadSounds( context, true );
					}
				}
			};
			Space.get().registerReceiver( s_mediaScanner, s_mediaFilter );
		}
	}


	/**
	 * Uses the contentResolver to fill the Sounds. arrays with all music files on the device.
	 * 
	 * @return false on serious failure
	 */
	public static boolean loadSounds (Context con, boolean force)
	{
		// check if sounds have already been loaded
		if (Sounds.mTitleArray != null && !force)
			return true;

		// try
		// {
		// // check if loader thread exists and is running
		// if (mLoaderThread != null)
		// {
		// mLoaderThread.join( 30000 );
		// return true;
		// }
		// }
		// catch (InterruptedException e)
		// {
		// Space.Log( "loadSounds: loading thread interrupted!" );
		// e.printStackTrace();
		// return false;
		// }


		final Context context = con;

		// mLoaderThread = new Thread( new Runnable() {
		// public void run ()
		// {

		ContentResolver contentResolver = context.getContentResolver();
		Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = contentResolver.query( uri, null, null, null, null );

		ArrayList< String > titleSet = new ArrayList< String >();
		// ArrayList< String > pathSet = new ArrayList< String >();

		if (cursor == null)
		{
			// query failed, handle error.
			return false;
		}
		else if (!cursor.moveToFirst())
		{
			// no media on the device
			return true;
		}
		else
		{
			int titleColumn = cursor.getColumnIndex( android.provider.MediaStore.Audio.Media.DISPLAY_NAME );
			// int idColumn = cursor.getColumnIndex( android.provider.MediaStore.Audio.Media._ID );
			do
			{
				titleSet.add( cursor.getString( titleColumn ) );
				// pathSet.add( uri + "/" + cursor.getString( titleColumn ) );
				// pathSet.add( cursor.getString( titleColumn ) );
				// ...process entry...
			} while (cursor.moveToNext());
		}

		if (titleSet.size() > 0)
		{
			Sounds.mTitleArray = new String[ titleSet.size() ];

			titleSet.toArray( Sounds.mTitleArray );
		}

		// }
		// } );

		// mLoaderThread.start();

		return true;
	}


	/**
	 * Checks if the media player is playing.
	 * 
	 * @param songFile
	 *            if not null will check if this particular song is playing
	 * @return
	 */
	public static boolean isPlaying (String songFile)
	{
		if (sPlayingSong == null)
			return false;

		if (songFile != null)
		{
			if (songFile.equals( sPlayingSong ))
				return true;
			else
				return false;
		}

		return sMediaPlayer.isPlaying();

	}


	/**
	 * Plays a signal sound.
	 * 
	 * @param con
	 */
	public static void playSignal (Context con, InternalAlarm signal)
	{
		if (sLastSignal != signal && sSignalPlayer != null)
		{
			cleanSignal();
		}

		if (sSignalPlayer == null)
		{
			sSignalPlayer = MediaPlayer.create( con, signal.getResId() );
		}
		if (sSignalPlayer != null)
		{
			sLastSignal = signal;

			if (!sSignalPlayer.isPlaying())
			{
				if (sChangeListener == null)
				{
					sBoundAudioManager = Space.getAudioManager( con );

					// onAudioFocusChange() is the only non-static method in this class, so we create an instance of
					// AudioHandler
					// as the change listener
					sChangeListener = new AudioHandler();

					// request AudioFocus
					if (sBoundAudioManager.requestAudioFocus(	sChangeListener,
																AudioManager.STREAM_MUSIC,
																AudioManager.AUDIOFOCUS_GAIN ) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
					{
						// could not get audio focus.
						Space.log( "Audiofocus gain failed!" );
					}
				}

				sSignalPlayer.setLooping( false );
				sSignalPlayer.start();
				sSignalPlayer.setVolume( 0.80f, 0.80f );
			}
		}
	}


	public static void playTone (int tone, int duration)
	{
		if (s_toneGen == null)
			s_toneGen = new ToneGenerator( AudioManager.STREAM_MUSIC, 60 );
		else
			s_toneGen.stopTone();

		s_toneGen.startTone( tone, duration );
	}


	public static void cleanSignal ()
	{
		if (sSignalPlayer != null)
		{
			// sSignalPlayer.stop();
			sSignalPlayer.release();
			sSignalPlayer = null;
		}

		if (s_toneGen != null)
		{
			s_toneGen.release();
			s_toneGen = null;
		}
	}


	/**
	 * Create sMediaPlayer from (Internet) stream.
	 * 
	 * @param con
	 * @param stream
	 * @return true on success, false on error
	 */
	public static boolean startStream (Context con, String stream)
	{
		if (sMediaPlayer != null)
		{
			sMediaPlayer.release();
			sMediaPlayer = null;
		}

		sMediaPlayer = new MediaPlayer();
		sMediaPlayer.setAudioStreamType( AudioManager.STREAM_MUSIC );
		try
		{
			Space.logRelease( "Trying stream..." + stream );

			sMediaPlayer.setDataSource( stream );
			sMediaPlayer.prepare(); // might take long! (for buffering, etc)

			return true;
		}
		catch (IllegalArgumentException e)
		{
			sMediaPlayer = null;
			e.printStackTrace();
			return false;
		}
		catch (IllegalStateException e)
		{
			sMediaPlayer = null;
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			sMediaPlayer.release();
			return false;
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
			return false;
		}
	}


	public static boolean confirmLoud (Context con)
	{
		// if (Space.spref().getBoolean( "pk_aa_nosounds", false ) == true)
		// return false;

		if (CyopsBoolean.NO_SOUNDS.isEnabled())
			return false;


		if (CyopsBoolean.IGNORE_SILENT.isNotEnabled()
				&& Space.getAudioManager( con ).getRingerMode() != AudioManager.RINGER_MODE_NORMAL)
		{
			Space.logRelease( "Suppressing sounds due to system silent-mode settings." );
			return false;
		}

		return true;
	}


	/**
	 * Will perform the alarm, that is: play the sound/ringtone, whatever
	 * 
	 * @param alarm
	 */
	public static int startSong (Context con, String songFile, float startVolume, boolean retryStream)
	{
		sBoundAudioManager = Space.getAudioManager( con );

		if (!confirmLoud( con ))
		{
			sBoundAudioManager = null;
			return 2;
		}

		Space.logRelease( "startSong: " + songFile );

		if (isPlaying( songFile ))
			return 3;

		Space.log( "stopping old song" );
		stopSong();


		String action = extractParam( songFile, false );

		sMaxMusicVolume = sBoundAudioManager.getStreamMaxVolume( android.media.AudioManager.STREAM_MUSIC );

		if (action.equals( "none" ) || action.equals( "int" ))
		{
			sMediaPlayer = MediaPlayer.create( con, InternalAlarm.BELL_CHILL.getResId() );
			sPlayingSong = new String( InternalAlarm.BELL_CHILL.getName() );
		}
		else if (action.equals( "ring" ))
		{
			sMediaPlayer = MediaPlayer.create( con, Settings.System.DEFAULT_RINGTONE_URI );
		}
		else if (action.equals( "riot" ))
		{
			sMediaPlayer = MediaPlayer.create( con, InternalAlarm.BELL_SIMPLE.getResId() );
			sPlayingSong = new String( InternalAlarm.BELL_SIMPLE.getName() );
		}
		else if (action.equals( "timer" ))
		{
			sMediaPlayer = MediaPlayer.create( con, InternalAlarm.BELL_SOFT.getResId() );
			sPlayingSong = new String( InternalAlarm.BELL_SOFT.getName() );
		}
		else if (action.equals( "stream" ))
		{
			String str = extractParam( songFile, true );

			if (!startStream( con, str ))
			{
				if (!retryStream)
				{
					return 1;
				}

				int timeout = Integer.parseInt( CyopsString.STREAM_TIMEOUT.get() ) * 1000;

				// since this requires a network connection, we
				// retry every 4 seconds for 40 seconds to compensate for possible net init stuff
				boolean res = false;
				long sleepEnd = System.currentTimeMillis() + timeout;
				while (System.currentTimeMillis() < sleepEnd)
				{
					// SystemClock.sleep( 4000 );
					try
					{
						Thread.sleep( 4000 );
					}
					catch (InterruptedException e)
					{
						// interrupted, just try again
					}
					res = startStream( con, str );
					if (res)
					{
						// stream successfully (connected), break and start playing
						break;
					}
				}

				if (!res)
				{
					sMediaPlayer = null;
					// network probably not reachable, revert to default alarm
					sMediaPlayer = MediaPlayer.create( con, InternalAlarm.BELL_CHILL.getResId() );
					sPlayingSong = new String( InternalAlarm.BELL_CHILL.getName() );
				}
			}
		}
		else
		{
			// random or specific file
			try
			{
				ContentResolver contentResolver = con.getContentResolver();

				if (action.equals( "random" ))
				{
					// select a random file from the content resolver
					Cursor cursor = contentResolver.query(	android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
															null,
															null,
															null,
															null );

					if (cursor != null)
					{
						if (cursor.getCount() > 0)
						{
							if (cursor.moveToPosition( new Random().nextInt( cursor.getCount() ) ))
							{
								int titleColumn = cursor.getColumnIndex( android.provider.MediaStore.Audio.Media._ID );
								if (titleColumn != -1)
								{
									songFile = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
											+ cursor.getString( titleColumn );
								}
							}
						}
					}
					else
					{
						// error
						sMediaPlayer = null;
					}
				}
				else
				{
					// ==============> specific file
					Cursor cursor = contentResolver.query(	android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
															null,
															android.provider.MediaStore.Audio.Media.DISPLAY_NAME
																	+ "= "
																	+ DatabaseUtils
																			.sqlEscapeString( extractParam( songFile, true ) ),
															null,
															null );

					if (cursor != null && cursor.moveToFirst())
					{
						int titleColumn = cursor.getColumnIndex( android.provider.MediaStore.Audio.Media._ID );
						if (titleColumn != -1)
						{
							songFile = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/"
									+ cursor.getString( titleColumn );
						}
					}
					else
					{
						// error
						sMediaPlayer = null;
					}
					// <=============
				}

				sMediaPlayer = MediaPlayer.create( con, Uri.parse( songFile ) );
				if (sMediaPlayer == null)
				{
					// fall back to default sound
					sMediaPlayer = MediaPlayer.create( con, InternalAlarm.BELL_CHILL.getResId() );
					sPlayingSong = new String( InternalAlarm.BELL_CHILL.getName() );
				}
			}
			catch (Exception e)
			{
				sMediaPlayer = null;
				e.printStackTrace();
			}
		}


		if (sMediaPlayer == null)
		{
			Space.showToast( con, "Error creating Media Player!" );
			sBoundAudioManager = null;
			return 4;
		}

		if (sPlayingSong == null)
		{
			sPlayingSong = songFile;
		}

		Space.log( "preparing to play" );

		if (sChangeListener == null)
		{
			// onAudioFocusChange() is the only non-static method in this class, so we create an instance of
			// AudioHandler
			// as the change listener
			sChangeListener = new AudioHandler();

			// request AudioFocus
			if (sBoundAudioManager.requestAudioFocus( sChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN ) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
			{
				// could not get audio focus.
				Space.log( "Audiofocus gain failed!" );
			}
		}

		sPrevMusicVolume = Space.getAudioManager( con ).getStreamVolume( android.media.AudioManager.STREAM_MUSIC );

		Space.getAudioManager( con )
				.setStreamVolume( android.media.AudioManager.STREAM_MUSIC, (int) sMaxMusicVolume, 0 /* AudioManager.FLAG_SHOW_UI */);

		if (startVolume < 0.005f || startVolume > 1.0f)
		{
			startVolume = 0.005f;
		}

		sMediaPlayer.setLooping( true );
		sMediaPlayer.start();
		sMediaPlayer.setVolume( startVolume, startVolume );
		Space.log( "playing" );

		return 0;
	}


	/**
	 * Stops the song currently playing
	 */
	public static void stopSong ()
	{
		if (sPlayingSong != null)
		{
			sPlayingSong = null;
		}

		if (sMediaPlayer != null)
		{
			if (sBoundAudioManager != null)
			{
				if (sPrevMusicVolume >= 0)
				{
					// reset stream volume
					sBoundAudioManager.setStreamVolume( android.media.AudioManager.STREAM_MUSIC, (int) sPrevMusicVolume, 0 );
					sPrevMusicVolume = -1;
				}

				if (sChangeListener != null)
				{
					sBoundAudioManager.abandonAudioFocus( sChangeListener );
					sChangeListener = null;
				}
				sBoundAudioManager = null;
			}

			// if (sMediaPlayer.isPlaying())
			// sMediaPlayer.stop();
			sMediaPlayer.release();
			sMediaPlayer = null;
		}
	}


	public static void setVolume (Context con, float volume)
	{
		if (sMediaPlayer == null)
			return;

		if (volume > 0.92f)
		{
			volume = 0.92f;
		}
		Space.log( "vol: " + volume );
		sMediaPlayer.setVolume( volume, volume );
	}


	public static void vibrate (Context con, int duration)
	{
		if (CyopsBoolean.NO_VIB.isEnabled())
		{
			return;
		}

		Space.getVibrator( con ).cancel();

		if (duration <= 0)
		{
			return;
		}
		else if (duration <= 50)
		{
			duration = 50;
		}

		Space.getVibrator( con ).vibrate( duration );
	}


	/**
	 * Extracts the path or action component of a soundAction string.
	 * 
	 * @param soundAction
	 * @param getPath
	 * @return
	 */
	public static String extractParam (String soundAction, boolean getPath)
	{
		if (soundAction == null)
			return "int";

		int idx = soundAction.indexOf( ']', 0 );
		if (idx < 1)
		{
			return soundAction;
		}

		if (getPath)
			return soundAction.substring( idx + 1 );

		return soundAction.substring( 1, idx );
	}


	public static void test (Context con)
	{

		String str = Cyops.getTriggerSound( 0 );

		sMediaPlayer = MediaPlayer.create( con, Uri.parse( str ) );

		if (sMediaPlayer == null)
		{
			Space.showToast( con, "Error creating Media Player!" );
			return;
		}

		sMediaPlayer.start();

		try
		{
			Thread.sleep( 1000 );
			int maxvol = Space.getAudioManager( con ).getStreamMaxVolume( android.media.AudioManager.STREAM_MUSIC );
			// int curvol = m_AudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
			Space.getAudioManager( con ).setStreamVolume( android.media.AudioManager.STREAM_MUSIC, maxvol, 0 );
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		sMediaPlayer.stop();
	}
}
