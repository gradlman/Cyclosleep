/**
 * 
 */
package com.gradlspace.cys;


import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.Time;
import android.util.Log;

import com.gradlspace.cys.FileManager.FileRequest;




/**
 * @author Falling
 * 
 */
public class CysInternalData
{

	// Used for debugging and logging
	private static final String	TAG							= "CysInternalDatabase";

	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String	DATABASE_NAME				= "cys_data";

	/**
	 * The database version
	 */
	private static final int	DATABASE_VERSION			= 13;


	// Handle to a new DatabaseHelper.
	private DatabaseHelper		mOpenHelper;


	private static final String	SLEEP_RECORDS_TABLE			= "cys_sleeps";
	private static final String	SLEEP_RECORDS_ID			= "id";
	private static final String	SLEEP_RECORDS_FLAGS			= "flags";
	private static final String	SLEEP_RECORDS_TIME_START	= "tstart";
	private static final String	SLEEP_RECORDS_TIME_END		= "tend";
	private static final String	SLEEP_RECORDS_DURATION		= "duration";
	private static final String	SLEEP_RECORDS_DURATION_DEEP	= "durdeep";
	private static final String	SLEEP_RECORDS_DURATION_WAKE	= "durwake";
	private static final String	SLEEP_RECORDS_QUALITY		= "quality";
	private static final String	SLEEP_RECORDS_SENSFILE		= "sensfile";
	private static final String	SLEEP_RECORDS_HYPNOFILE		= "hypnofile";
	private static final String	SLEEP_RECORDS_DREAMFILE		= "dreamfile";
	private static final String	SLEEP_RECORDS_COMMENT		= "comment";


	/**
	 * Represents a single sleep record.
	 * 
	 * @author Falling
	 * 
	 */
	public static class SleepRecord
	{
		// flags
		public static final int	IGNORE	= 1 << 0;

		/** contains zipped files */
		public static final int	PACKED	= 1 << 1;

		/** record has been uploaded to FAU */
		public static final int	FAUPED	= 1 << 2;


		public int				id;
		public int				flags;
		public long				tstart;
		public long				tend;
		public long				duration;
		public long				durdeep;
		public long				durwake;
		public short			quality;
		public String			sensfile;
		public String			hypnofile;
		public String			dreamfile;
		public String			comment;
		public int				batteryUsage;


		public boolean isPacked ()
		{
			return ( (flags & PACKED) == PACKED);
		}


		public boolean isFauped ()
		{
			return ( (flags & FAUPED) == FAUPED);
		}
	}


	/** All known sleep recorded cached from the SQLlite db */
	public ArrayList< SleepRecord >	m_sleepRecords	= new ArrayList< CysInternalData.SleepRecord >();


	/**
	 * 
	 * This class helps open, create, and upgrade the database file. Set to package visibility for testing purposes.
	 */
	static class DatabaseHelper extends SQLiteOpenHelper
	{

		DatabaseHelper (Context context)
		{

			// calls the super constructor, requesting the default cursor factory.
			super( context, DATABASE_NAME, null, DATABASE_VERSION );
		}


		/**
		 * 
		 * Creates the underlying database.
		 */
		@Override
		public void onCreate (SQLiteDatabase db)
		{
			Space.logRelease( "Creating sleep_rec db structure..." );

			try
			{
				db.execSQL( "CREATE TABLE " + SLEEP_RECORDS_TABLE + " (" + SLEEP_RECORDS_ID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SLEEP_RECORDS_FLAGS + " INTEGER," + SLEEP_RECORDS_TIME_START
						+ " TIMESTAMP," + SLEEP_RECORDS_TIME_END + " TIMESTAMP," + SLEEP_RECORDS_DURATION + " INTEGER,"
						+ SLEEP_RECORDS_DURATION_DEEP + " INTEGER," + SLEEP_RECORDS_DURATION_WAKE + " INTEGER,"
						+ SLEEP_RECORDS_QUALITY + " TINYINT," + SLEEP_RECORDS_SENSFILE + " TEXT," + SLEEP_RECORDS_HYPNOFILE
						+ " TEXT," + SLEEP_RECORDS_DREAMFILE + " TEXT," + SLEEP_RECORDS_COMMENT + " TEXT" + ");" );

				Space.log( "...done." );
			}
			catch (SQLException e)
			{
				Space.log( "...error in SQL statement!" );
				e.printStackTrace();
			}

			// iterate all files in data-directory
			File dir = FileManager.getFilesDir( Space.get(), FileRequest.READ );
			File[] files = null;

			if (dir != null)
			{
				files = dir.listFiles();
			}

			if (dir == null || files == null)
			{
				Space.log( "no files found." );
				return;
			}

			int numHypnos = 0;
			int numTotal = 0;

			for (File f : files)
			{
				// check for file type
				String strFile = f.getAbsolutePath();
				if (!strFile.endsWith( FileManager.HYPNO_EXT ))
				{
					// skip all non-hypno files
					continue;
				}

				// A map to hold the new record's values.
				ContentValues values = new ContentValues();
				// values.put( SLEEP_RECORDS_ID, i );
				values.put( SLEEP_RECORDS_FLAGS, 0 );
				values.put( SLEEP_RECORDS_HYPNOFILE, strFile );

				Space.log( "..." + strFile );

				// load hypnogram file and read/calculate stats
				Hypnogram hypno = new Hypnogram();
				if (hypno.loadFromFile( Space.get(), strFile, true ) < 3)
				{
					// error
					continue;
				}

				hypno.calculateStats();

				values.put( SLEEP_RECORDS_TIME_START, hypno.stats.timeStart );
				values.put( SLEEP_RECORDS_TIME_END, hypno.stats.timeEnd );
				values.put( SLEEP_RECORDS_DURATION, hypno.stats.timeSpan );
				values.put( SLEEP_RECORDS_DURATION_DEEP, hypno.stats.sleepSpan );
				values.put( SLEEP_RECORDS_DURATION_WAKE, hypno.stats.sleepSpanDawn );
				values.put( SLEEP_RECORDS_QUALITY, hypno.rating );


				String dfile = "";
				long ddiff = Long.MAX_VALUE;
				String sfile = "";
				long sdiff = Long.MAX_VALUE;

				Time tt;
				long fileTime;
				long timeDiff;

				// find sens/dream/vsens files from same sleep
				for (File cf : files)
				{
					String checkFile = cf.getAbsolutePath();
					if (checkFile.endsWith( FileManager.HYPNO_EXT ))
					{
						// skip hypno files
						continue;
					}

					tt = FileManager.extractTimestamp( checkFile );

					if (tt != null)
					{
						// check and assign the path to the respective bucket.
						// this is done by checking if the timestamp encoded into the filename is within 4 hours of the
						// start/end
						fileTime = tt.toMillis( true );
						timeDiff = Math.abs( fileTime - hypno.stats.timeStart );

						// is within 4 hours?
						if (fileTime > hypno.stats.timeStart - Cyops.MILLIS_4H
								&& fileTime < hypno.stats.timeEnd + Cyops.MILLIS_4H)
						{
							if (checkFile.endsWith( FileManager.DREAM_EXT ))
							{
								ddiff = timeDiff;
								dfile = checkFile;
							}
							else if (checkFile.endsWith( FileManager.SSD_EXT ))
							{
								sdiff = timeDiff;
								sfile = checkFile;
							}
						}
					}
				}

				Space.log( "..... dfile: " + dfile + " : " + ddiff );
				Space.log( "..... sfile: " + sfile + " : " + sdiff );

				values.put( SLEEP_RECORDS_DREAMFILE, dfile );
				values.put( SLEEP_RECORDS_SENSFILE, sfile );

				values.put( SLEEP_RECORDS_COMMENT, "" );

				++numHypnos;
				++numTotal;

				if (dfile.length() > 2)
				{
					++numTotal;
				}

				if (sfile.length() > 2)
				{
					++numTotal;
				}


				// Performs the insert and returns the ID of the new note.
				long rowId = db.insert( SLEEP_RECORDS_TABLE, // The table to insert into.
										SLEEP_RECORDS_COMMENT, // A hack, SQLite sets this column value to null
										// if values is empty.
										values // A map of column names, and the values to insert
						// into the columns.
						);


				// If the insert succeeded, the row ID exists.
				if (rowId < 0)
				{
					// error
					Space.logRelease( "...error inserting record " + strFile + " into db!" );
				}

			}

			Space.logRelease( "..finished creating database. " + numHypnos + " [" + numTotal + "]" );
		}


		/**
		 * 
		 * Demonstrates that the provider must consider what happens when the underlying datastore is changed. In this
		 * sample, the database is upgraded the database by destroying the existing data. A real application should
		 * upgrade the database in place.
		 */
		@Override
		public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion)
		{

			// Logs that the database is being upgraded
			Log.w( TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data" );

			// Kills the table and existing data
			db.execSQL( "DROP TABLE IF EXISTS " + SLEEP_RECORDS_TABLE );

			// Recreates the database with a new version
			onCreate( db );
		}
	}


	/**
	 * Called once upon first launch of Cys in Space.initSpace(). It caches all known sleep record entries from the
	 * local SQLite db into m_sleepRecords. If the db doesn't exist, it is created via DatabaseHelper.onCreate().
	 */
	public CysInternalData ()
	{

		// Creates a new helper object. Note that the database itself isn't opened until
		// something tries to access it, and it's only created if it doesn't already exist.
		mOpenHelper = new DatabaseHelper( Space.get() );


		// most recent entries first
		String orderBy;
		orderBy = SLEEP_RECORDS_TIME_START + " DESC";

		// Opens the database object in "read" mode, since no writes need to be done.
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		/*
		 * Performs the query. If no problems occur trying to read the database, then a Cursor
		 * object is returned; otherwise, the cursor variable contains null. If no records were
		 * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
		 */
		Cursor c = db.query( SLEEP_RECORDS_TABLE, // db, // The database to query
								null, // The columns to return from the query: null == all
								null, // The columns for the where clause: null == all
								null, // The values for the where clause
								null, // don't group the rows
								null, // don't filter by row groups
								orderBy // The sort order
				);


		if (c == null)
		{
			// query failed, handle error.
			// return false;
		}
		else if (!c.moveToFirst())
		{
			// no media on the device
			// return true;
		}
		else
		{
			SleepRecord sr;

			int idColumn = c.getColumnIndex( SLEEP_RECORDS_ID );
			int idFlags = c.getColumnIndex( SLEEP_RECORDS_FLAGS );
			int idTStart = c.getColumnIndex( SLEEP_RECORDS_TIME_START );
			int idTEnd = c.getColumnIndex( SLEEP_RECORDS_TIME_END );
			int idDur = c.getColumnIndex( SLEEP_RECORDS_DURATION );
			int idDurd = c.getColumnIndex( SLEEP_RECORDS_DURATION_DEEP );
			int idDurw = c.getColumnIndex( SLEEP_RECORDS_DURATION_WAKE );
			int idQual = c.getColumnIndex( SLEEP_RECORDS_QUALITY );
			int idSens = c.getColumnIndex( SLEEP_RECORDS_SENSFILE );
			int idHypno = c.getColumnIndex( SLEEP_RECORDS_HYPNOFILE );
			int idDream = c.getColumnIndex( SLEEP_RECORDS_DREAMFILE );
			int idComm = c.getColumnIndex( SLEEP_RECORDS_COMMENT );

			do
			{
				sr = new SleepRecord();
				sr.id = c.getInt( idColumn );
				sr.flags = c.getInt( idFlags );
				sr.tstart = c.getLong( idTStart );
				sr.tend = c.getLong( idTEnd );
				sr.duration = c.getInt( idDur );
				sr.durdeep = c.getInt( idDurd );
				sr.durwake = c.getInt( idDurw );
				sr.quality = c.getShort( idQual );
				sr.sensfile = c.getString( idSens );
				sr.hypnofile = c.getString( idHypno );
				sr.dreamfile = c.getString( idDream );
				sr.comment = c.getString( idComm );
				m_sleepRecords.add( sr );

				Space.log( "db row " + sr.id + "  " + sr.tstart + "  " + sr.duration + "  " + sr.quality + "  " + sr.hypnofile );
			} while (c.moveToNext());

			c.close();
		}


		FauLink.update();
	}


	public void close ()
	{
		if (mOpenHelper != null)
		{
			mOpenHelper.close();
		}
	}


	/**
	 * Returns the sleep record that has the given database ID
	 * 
	 * @param idx
	 * @return
	 */
	public SleepRecord getRecordById (int idx)
	{
		for (SleepRecord rec : m_sleepRecords)
		{
			if (rec.id == idx)
			{
				return rec;
			}
		}

		return null;
	}


	/**
	 * @param verify
	 *            if set to true the flag AND the existence of the record file are verified.
	 * @return number of SleepRecords containing verbose sensor data.
	 */
	public int getNumVerbose (boolean verify)
	{
		int num = 0;
		for (SleepRecord rec : m_sleepRecords)
		{
			if (rec.sensfile != null)
			{
				if (!verify || FileManager.existsFile( rec.sensfile ))
					++num;
			}
		}

		return num;
	}


	/**
	 * Inserts the given sleep record into the database and the cache.
	 * 
	 * @param sr
	 */
	public void insert (SleepRecord sr)
	{
		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();


		// A map to hold the new record's values.
		ContentValues values = new ContentValues();
		// values.put( SLEEP_RECORDS_ID, sr.id );
		values.put( SLEEP_RECORDS_FLAGS, sr.flags );
		values.put( SLEEP_RECORDS_TIME_START, sr.tstart );
		values.put( SLEEP_RECORDS_TIME_END, sr.tend );
		values.put( SLEEP_RECORDS_DURATION, sr.duration );
		values.put( SLEEP_RECORDS_DURATION_DEEP, sr.durdeep );
		values.put( SLEEP_RECORDS_DURATION_WAKE, sr.durwake );
		values.put( SLEEP_RECORDS_QUALITY, sr.quality );
		values.put( SLEEP_RECORDS_SENSFILE, sr.sensfile );
		values.put( SLEEP_RECORDS_HYPNOFILE, sr.hypnofile );
		values.put( SLEEP_RECORDS_DREAMFILE, sr.dreamfile );
		values.put( SLEEP_RECORDS_COMMENT, sr.comment );

		Space.log( "SQL: inserting " + sr );


		// Performs the insert and returns the ID of the new note.
		long rowId = db.insert( SLEEP_RECORDS_TABLE, // The table to insert into.
								SLEEP_RECORDS_COMMENT, // A hack, SQLite sets this column value to null
								// if values is empty.
								values // A map of column names, and the values to insert
				// into the columns.
				);

		// If the insert succeeded, the row ID exists.
		if (rowId > 0)
		{
			Space.log( "success" );

			// retrieve ID assigned by mySQL with AUTO_INCREMENT
			Cursor c = db.query(	SLEEP_RECORDS_TABLE,
									new String[] { SLEEP_RECORDS_ID },
									"rowid = " + rowId,
									null,
									null,
									null,
									null );

			if (c == null)
			{
				// query failed, handle error.
				// return false;
			}
			else if (!c.moveToFirst())
			{
				// no media on the device
				// return true;
			}
			else
			{
				sr.id = c.getInt( c.getColumnIndex( SLEEP_RECORDS_ID ) );

				Space.log( "SQL: last_id == " + sr.id );

				c.close();
			}

			// add the record to the cache at the first position, since it is the chronologically latest record
			m_sleepRecords.add( 0, sr );

			db.close();

			// update sleep stats
			SPTracker.loadAvgSleep();

			return;
		}

		// If the insert didn't succeed, then the rowID is <= 0. Throws an exception.
		throw new SQLException( "Failed to insert row into " + SLEEP_RECORDS_TABLE );
	}


	/**
	 * 
	 * @param idx
	 */
	public boolean delete (int idx)
	{
		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		Space.log( "SQL: deleting " + idx );

		SleepRecord rec = getRecordById( idx );
		if (rec == null)
		{
			return false;
		}

		int count = db.delete( SLEEP_RECORDS_TABLE, SLEEP_RECORDS_ID + " = " + idx, null );

		db.close();

		if (count != 1)
		{
			Log.e( Space.TAG, "SQL delete error. Affected rows = " + count );
		}
		else
		{
			try
			{
				// delete the files
				if (rec.hypnofile != null)
				{
					FileManager.deleteFile( rec.hypnofile );
				}

				if (rec.sensfile != null)
				{
					FileManager.deleteFile( rec.sensfile );
				}

				if (rec.dreamfile != null)
				{
					FileManager.deleteFile( rec.dreamfile );
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// delete array entry
			for (int i = 0; i < m_sleepRecords.size(); ++i)
			{
				if (m_sleepRecords.get( i ).id == idx)
				{
					m_sleepRecords.remove( i );
					break;
				}
			}

			// update sleep stats
			SPTracker.loadAvgSleep();
		}

		return true;
	}


	/**
	 * Updates the given SleepRecord in the database with the data from the memory. Only flags, quality and comment can
	 * be updated.
	 * 
	 * @param sr
	 * @return true if update was successful
	 */
	public boolean update (SleepRecord sr)
	{
		if (sr == null || sr.id < 0)
		{
			return false;
		}

		// Opens the database object in "write" mode.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		Space.log( "SQL: updating " + sr.id );


		// A map to hold the new record's values.
		ContentValues values = new ContentValues();
		// values.put( SLEEP_RECORDS_ID, sr.id );
		values.put( SLEEP_RECORDS_FLAGS, sr.flags );
		// values.put( SLEEP_RECORDS_TIME_START, sr.tstart );
		// values.put( SLEEP_RECORDS_TIME_END, sr.tend );
		// values.put( SLEEP_RECORDS_DURATION, sr.duration );
		// values.put( SLEEP_RECORDS_DURATION_DEEP, sr.durdeep );
		// values.put( SLEEP_RECORDS_DURATION_WAKE, sr.durwake );
		values.put( SLEEP_RECORDS_QUALITY, sr.quality );
		// values.put( SLEEP_RECORDS_SENSFILE, sr.sensfile );
		// values.put( SLEEP_RECORDS_HYPNOFILE, sr.hypnofile );
		// values.put( SLEEP_RECORDS_DREAMFILE, sr.dreamfile );
		values.put( SLEEP_RECORDS_COMMENT, sr.comment );


		int count = db.update( SLEEP_RECORDS_TABLE, values, SLEEP_RECORDS_ID + " = " + sr.id, null );

		db.close();

		if (count != 1)
		{
			Log.e( Space.TAG, "SQL update error. Affected rows = " + count );
		}
		else
		{
			// success
		}

		return true;
	}

}
