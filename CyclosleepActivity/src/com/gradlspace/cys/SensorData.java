/**
 * 
 */
package com.gradlspace.cys;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.Time;
import android.util.Log;

import com.gradlspace.cys.FileManager.FileRequest;
import com.gradlspace.cys.activities.DeviceTestActivity;

import de.lme.plotview.LongValueList;




/**
 * SensorData manages all values reported by the accelerometer.
 * 
 * @author Falling
 * 
 */
public class SensorData
{
	public static final int			DEFAULT_BUFFER_SIZE	= 8192;
	private int						m_maxBufferSize		= DEFAULT_BUFFER_SIZE;

	public static final int			DEFAULT_SCALAR		= 1000000;

	protected static final String	NEWLINE				= System.getProperty( "line.separator" );

	private String					m_fileHeader		= null;
	private String					m_filePath			= null;

	/** Millisecond timestamp of class instantiation. */
	private long					m_initMillis		= 0;
	/** Nanosecond timestamp of class instantiation. */
	private long					m_initNanos			= 0;

	/** Difference between millisNow and initMillis. */
	private long					m_millisDiff		= 0;
	/** Current timestamp in absolute milliseconds. */
	public long						m_millisNow			= 0;

	private String					m_strDateTime		= null;
	private String					m_sensorInfo		= "< />";

	protected LongValueList			time;
	protected LongValueList			x;
	protected LongValueList			y;
	protected LongValueList			z;

	protected ReentrantLock			m_dataLock			= new ReentrantLock();

	/** Whether to flush data to file if circ buffers are full. */
	public boolean					m_autoSave			= true;


	public SensorData ()
	{
		m_initMillis = System.currentTimeMillis();
		m_initNanos = System.nanoTime();
		m_millisNow = m_initMillis;

		Time tt = new Time();
		tt.set( m_initMillis );
		m_strDateTime = tt.format2445();

		m_filePath = FileManager.getFilesDirName( Space.get(), FileRequest.WRITE, "sens_" + m_strDateTime + FileManager.SSD_EXT );

		m_sensorInfo = DeviceTestActivity.enumDefaultAccel( Space.get() );
		m_fileHeader = buildHeaderString( Space.get() );

		// allocate arrays
		m_dataLock.lock();
		time = new LongValueList( m_maxBufferSize, false );
		x = new LongValueList( m_maxBufferSize, false );
		y = new LongValueList( m_maxBufferSize, false );
		z = new LongValueList( m_maxBufferSize, false );
		m_dataLock.unlock();
	}


	public String getFilePath ()
	{
		return m_filePath;
	}


	public String buildHeaderString (Context con)
	{
		StringBuilder str = new StringBuilder();

		str.append( "<SensorData " );

		PackageInfo info;

		// package info
		try
		{
			info = con.getPackageManager().getPackageInfo( con.getPackageName(), 0 );
			str.append( "package=\"" + info.packageName + "\" version=\"" + info.versionCode + "\" " );
		}
		catch (NameNotFoundException ex)
		{
			str.append( "package=\"n/a\" version=\"n/a\" " );
		}
		catch (NullPointerException ex)
		{
			str.append( "package=\"n/a\" version=\"n/a\" " );
		}

		// time + datetime
		str.append( "timestamp=\"" + m_initMillis + "\" datetime=\"" + m_strDateTime + "\" />\n" );

		// sensor info
		str.append( m_sensorInfo ).append( "\n" );

		// data def
		str.append( "<Data style=\"sampled\" cell_sep=\"\\n\" col_sep=\" \" col_def=\"Timediff[ms] x[m/s^2]{1000000} y[m/s^2]{1000000} z[m/s^2]{1000000}\" />\n" );

		return str.toString();
	}


	/**
	 * Adds new event values, updates the timestamp and flushes data to file if necessary.
	 * 
	 * @param nanoEventTime
	 * @param pX
	 * @param pY
	 * @param pZ
	 */
	public void add (long nanoEventTime, float pX, float pY, float pZ)
	{
		// calc millisecond diff to start
		m_millisDiff = (long) ( (nanoEventTime - m_initNanos) * 0.000001);

		// calc current time based on event timestamp diff
		m_millisNow = m_initMillis + m_millisDiff;

		// store values to circ buffers
		m_dataLock.lock();

		time.add( m_millisDiff );
		x.add( (long) (pX * DEFAULT_SCALAR) );
		y.add( (long) (pY * DEFAULT_SCALAR) );
		z.add( (long) (pZ * DEFAULT_SCALAR) );

		if (m_autoSave && time.head == m_maxBufferSize - 1)
		{
			saveToFile( Space.get() );
		}

		m_dataLock.unlock();
	}


	private transient int			tIdx	= 0;
	private transient StringBuilder	tStr	= new StringBuilder( 128 );


	public boolean saveToFile (Context con)
	{
		File f = null;

		if (time == null || time.head <= 0)
		{
			return true;
		}

		try
		{
			f = new File( m_filePath );

			FileWriter fw = new FileWriter( f, true );

			// new m_file? append header?
			if (f.length() < 5)
			{
				fw.write( m_fileHeader );
			}

			// write all data values
			for (tIdx = 0; tIdx <= time.head; ++tIdx)
			{
				tStr.setLength( 0 );
				tStr.append( time.values[ tIdx ] ).append( " " ).append( x.values[ tIdx ] ).append( " " )
						.append( y.values[ tIdx ] ).append( " " ).append( z.values[ tIdx ] ).append( NEWLINE );
				fw.write( tStr.toString() );
			}

			fw.close();

			return true;
		}
		catch (IOException e)
		{
			if (f != null)
				Log.w( Space.TAG, "Error writing " + f.getAbsolutePath(), e );
			else
				Log.w( Space.TAG, "Error writing " + m_filePath, e );
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}

		return false;
	}

}
