/**
 * 
 */
package com.gradlspace.cys;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.zip.GZIPOutputStream;

import android.content.Context;
import android.os.StatFs;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.Cyops.CyopsString;




/**
 * @author Falling
 * 
 */
public class FileManager
{
	/**
	 * minimum free space required for file writing default = 12mb
	 */
	public static final long									MIN_FREE_SPACE			= 12000000;

	/** minimum free space required for small file writing */
	public static final long									MIN_FREE_SPACE_SMALL	= 500000;


	public static final String									SSD_EXT					= ".ssd";
	public static final String									HYPNO_EXT				= ".shf";
	public static final String									DREAM_EXT				= ".sdf";
	public static final String									ZIP_EXT					= ".gz";

	public static ArrayList< LinkedHashMap< String, String >>	dataFiles				= null;
	public static ArrayList< LinkedHashMap< String, String >>	sleepLog				= null;

	public static Thread										workThread				= null;


	public enum FileRequest
	{
		READ, WRITE_SMALL, WRITE
	}


	/**
	 * Unused.
	 * 
	 * @param filePath
	 * @return
	 */
	public static String swapExtension (String filePath)
	{
		// get the filename
		File f = new File( filePath );
		String fname = f.getName();

		// make sure it's an SSD
		if (fname != null && fname.endsWith( FileManager.SSD_EXT ))
		{
			// strip SSD ext, append SSV and create File object
			File fv = new File( filePath.substring( 0, filePath.length() - FileManager.SSD_EXT.length() )
					.concat( FileManager.DREAM_EXT ) );

			if (fv.exists())
			{
				return fv.getAbsolutePath();
			}
		}

		return null;
	}


	/**
	 * @param filePath
	 * @return
	 */
	public static Time extractTimestamp (String filePath)
	{
		// file name
		File f = new File( filePath );
		String fileName = f.getName();
		if (fileName == null || fileName.length() < 10)
		{
			return null;
		}

		// number of extension chars
		int numExt = 4;
		if (fileName.endsWith( FileManager.ZIP_EXT ))
		{
			numExt = 8;
		}

		Time tt = new Time();

		// try to parse time
		try
		{
			// parse "_***.ext"
			String fileBase = fileName.substring( fileName.lastIndexOf( '_' ) + 1, fileName.length() - numExt );

			// has RFC 2445 "T"
			int t = fileBase.lastIndexOf( 'T' );

			if (t == -1)
			{
				// no, try to parse as UTC milliseconds
				tt.set( Long.parseLong( fileBase ) );
			}
			else
			{
				// parse as RFC 2445
				tt.parse( fileBase );
			}
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		catch (NullPointerException e)
		{
			return null;
		}
		catch (TimeFormatException e)
		{
			return null;
		}
		catch (IndexOutOfBoundsException e)
		{
			return null;
		}

		// return the filled time object
		return tt;
	}


	public static int createSleepLog (Context con)
	{
		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir == null)
		{
			Space.showToast( con, R.string.textErrorIO );
			return 0;
		}

		File[] files = dir.listFiles();

		ArrayList< File > list = new ArrayList< File >( files.length );
		Collections.addAll( list, files );
		Collections.sort( list, new Comparator< File >() {

			public int compare (File lhs, File rhs)
			{
				// sort by date, most recent first
				return rhs.getName().compareTo( lhs.getName() );
			}

		} );

		// File file = new File( getExternalFilesDir( null ), newValue.toString() );

		sleepLog = new ArrayList< LinkedHashMap< String, String >>( list.size() );
		LinkedHashMap< String, String > map;

		String str;
		boolean gz = false;
		Time tt = new Time();
		Hypnogram hypno;
		Dream dream;
		String strDuration = con.getString( R.string.textDuration );
		String strImg = Integer.toString( R.drawable.ic_file_hypno );
		String strImgDream = Integer.toString( R.drawable.ic_file_dream );
		String strRating = con.getString( R.string.textRating ) + ": ";

		// fill mListdata with mapped entries
		for (File f : list)
		{
			gz = false;


			str = f.getName();
			if (str.length() < 5)
				continue;

			if (str.endsWith( FileManager.HYPNO_EXT ))
			{
				hypno = new Hypnogram();
				if (hypno.loadFromFile( con, f.getAbsolutePath(), true ) <= 3)
					continue;

				hypno.calculateStats();

				map = new LinkedHashMap< String, String >( 6 );
				map.put( "file", f.getAbsolutePath() );
				map.put( "type", strDuration );
				map.put( "img", strImg );

				tt.set( hypno.stats.timeStart );
				map.put( "date", tt.format( "%Y.%m.%d %H:%M" ) );

				// tt.set( f.lastModified() );
				if (hypno.rating == -1)
					map.put( "mod", "n/a" );
				else
					map.put( "mod", strRating + Byte.toString( hypno.rating ) );

				map.put( "size", String.format( "%.2f h", hypno.stats.timeSpan / 3600000d ) );

				sleepLog.add( map );
			}
			else if (str.endsWith( FileManager.DREAM_EXT ))
			{
				dream = new Dream();
				if (!dream.loadFromFile( con, f.getAbsolutePath() ))
					continue;

				map = new LinkedHashMap< String, String >( 6 );
				map.put( "file", f.getAbsolutePath() );
				map.put( "type", dream.headline );
				map.put( "img", strImgDream );

				tt.set( dream.timestamp );
				map.put( "date", tt.format( "%Y.%m.%d %H:%M" ) );

				map.put( "mod", "n/a" );

				map.put( "size", "--" );

				sleepLog.add( map );
			}
			else
				continue;
		}

		if (sleepLog == null)
			return 0;

		return sleepLog.size();
	}


	/**
	 * Fills the dataFiles array with information about our data files in the filesystem.
	 * 
	 * @param con
	 * @return
	 */
	public static int readDataFiles (Context con)
	{
		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir == null)
		{
			Space.showToast( con, R.string.textErrorIO );
			return 0;
		}

		File[] files = dir.listFiles();

		ArrayList< File > list = new ArrayList< File >( files.length );
		Collections.addAll( list, files );
		Collections.sort( list, new Comparator< File >() {

			public int compare (File lhs, File rhs)
			{
				// sort by date, most recent first
				return rhs.getName().compareTo( lhs.getName() );
			}

		} );

		// File file = new File( getExternalFilesDir( null ), newValue.toString() );

		dataFiles = new ArrayList< LinkedHashMap< String, String >>( list.size() );
		LinkedHashMap< String, String > map;

		String str;
		boolean gz = false;
		Time tt = new Time();

		// fill mListdata with mapped entries
		for (File f : list)
		{
			gz = false;
			map = new LinkedHashMap< String, String >( 6 );

			str = f.getName();
			if (str.length() < 5)
				continue;

			map.put( "file", f.getAbsolutePath() );

			if (str.endsWith( FileManager.ZIP_EXT ))
			{
				str = str.substring( 0, str.length() - FileManager.ZIP_EXT.length() );
				gz = true;
			}

			if (str.endsWith( FileManager.SSD_EXT ))
			{
				if (gz)
					map.put( "type", "Sensor Data*" );
				else
					map.put( "type", "Sensor Data" );
				map.put( "img", Integer.toString( R.drawable.ic_file_plot ) );
			}
			else if (str.endsWith( FileManager.HYPNO_EXT ))
			{
				map.put( "type", "Hypnogram" );
				map.put( "img", Integer.toString( R.drawable.ic_file_hypno ) );
			}
			else if (str.endsWith( FileManager.DREAM_EXT ))
			{
				map.put( "type", "Dream" );
				map.put( "img", Integer.toString( R.drawable.ic_file_dream ) );
			}
			else
			{
				map.put( "type", "Unknown" );
				map.put( "img", Integer.toString( R.drawable.ic_file_unknown ) );
				map.put( "date", "unknown" );
				tt.set( f.lastModified() );
				map.put( "mod", tt.format( "%Y.%m.%d %H:%M" ) );
				map.put( "size", Formatter.formatFileSize( con, f.length() ) );
				dataFiles.add( map );
				continue;
			}

			// extract date time info
			tt = extractTimestamp( str );
			if (tt != null)
			{
				str = tt.format( "%Y.%m.%d %H:%M" );
			}
			else
			{
				str = "n/a";
			}
			// try
			// {
			// str = str.substring( str.lastIndexOf( '_' ) + 1, str.length() - 4 );
			// int t = str.lastIndexOf( 'T' );
			// if (t == -1)
			// {
			// tt.set( Long.parseLong( str ) );
			// }
			// else
			// {
			// tt.parse( str );
			// }
			// str = tt.format( "%Y.%m.%d %H:%M" );
			// }
			// catch (NumberFormatException e)
			// {
			// Space.log( "unknown file format in ExplorerActivity." );
			// }
			// catch (NullPointerException e)
			// {
			// Space.log( "unknown file format in ExplorerActivity." );
			// }
			// catch (TimeFormatException e)
			// {
			// Space.log( "unknown file format in ExplorerActivity." );
			// }

			map.put( "date", str );

			tt.set( f.lastModified() );
			map.put( "mod", tt.format( "%Y.%m.%d %H:%M" ) );

			map.put( "size", Formatter.formatFileSize( con, f.length() ) );

			dataFiles.add( map );
		}

		if (dataFiles == null)
			return 0;

		return dataFiles.size();
	}


	/**
	 * Returns the free space (in bytes) available at the given location in the filesystem.
	 * 
	 * @param path
	 * @return
	 */
	public static long getFreeSpace (String path)
	{
		StatFs fs = new StatFs( path );
		return ((long) fs.getBlockSize() * (long) fs.getAvailableBlocks());
	}


	/**
	 * Convenience method to get an absolute file path.
	 * 
	 * @param con
	 * @param request
	 * @param filename
	 * @return
	 */
	public static String getFilesDirName (Context con, FileRequest request, String filename)
	{
		File dir = FileManager.getFilesDir( con, request );
		if (dir == null)
		{
			// some sort of error, probably too little disk space or disk not mounted
			Log.e( "saveToFile", "File directory problems." );
			return null;
		}
		return new File( dir, filename ).getAbsolutePath();
	}


	/**
	 * Returns a directory File respecting user settings and available free space, or null if there were problems.
	 * 
	 * @param con
	 * @param request
	 *            specifies what you intend to do with the directory
	 * @return directory handle as File or null on any error
	 */
	public static File getFilesDir (Context con, FileRequest request)
	{
		File f = null;
		long free = 0;

		// check prefs for custom path
		// String dataPath = Space.spref().getString( "pk_data_path", "ext" );
		String dataPath = CyopsString.DATA_PATH.get();


		if (dataPath.equals( "int" ))
		{
			// check internal files dir
			f = con.getFilesDir();
			free = getFreeSpace( f.getAbsolutePath() );

			// check for enough free space
			if (request == FileRequest.READ || (request == FileRequest.WRITE_SMALL && free > MIN_FREE_SPACE_SMALL)
					|| (request == FileRequest.WRITE && free > MIN_FREE_SPACE))
				return f;
		}

		// if we are still here although "int" was set, we can assume there wasn't enough space on internal media
		if (dataPath.equals( "ext" ) || dataPath.equals( "int" ))
		{
			// check external files dir
			f = con.getExternalFilesDir( null );
			if (f != null)
			{
				free = getFreeSpace( f.getAbsolutePath() );

				// check for enough free space
				if (request == FileRequest.READ || (request == FileRequest.WRITE_SMALL && free > MIN_FREE_SPACE_SMALL)
						|| (request == FileRequest.WRITE && free > MIN_FREE_SPACE))
					return f;
			}

			// here we just do not have enough space, return null
			return null;
		}

		// user defined path
		f = con.getDir( dataPath, Context.MODE_WORLD_READABLE );
		free = getFreeSpace( f.getAbsolutePath() );

		// check for enough free space
		if (request == FileRequest.READ || (request == FileRequest.WRITE_SMALL && free > MIN_FREE_SPACE_SMALL)
				|| (request == FileRequest.WRITE && free > MIN_FREE_SPACE))
			return f;

		return null;
	}


	/**
	 * Compresses the given file if it isn't already compressed.
	 * 
	 * @param filenameIn
	 * @return The filename of the compressed file.
	 */
	public static String compressFile (String filenameIn)
	{
		return compressFile( filenameIn, filenameIn.concat( FileManager.ZIP_EXT ) );
	}


	/**
	 * Compress the given file to the out file.
	 * 
	 * @param filenameIn
	 * @param filenameOut
	 * @return
	 */
	public static String compressFile (String filenameIn, String filenameOut)
	{
		int read = 0;
		byte[] data = new byte[ 1024 ];

		if (filenameIn.endsWith( ZIP_EXT ))
			return filenameIn;

		try
		{
			FileInputStream fileIn = new FileInputStream( filenameIn );
			GZIPOutputStream zipOut = new GZIPOutputStream( new FileOutputStream( filenameOut ) );

			while ( (read = fileIn.read( data, 0, 1024 )) != -1)
			{
				zipOut.write( data, 0, read );
			}

			zipOut.close();
			fileIn.close();
		}
		catch (FileNotFoundException e)
		{
			filenameOut = filenameIn;
			e.printStackTrace();
		}
		catch (IOException e)
		{
			filenameOut = filenameIn;
			e.printStackTrace();
		}


		return filenameOut;
	}


	/**
	 * Archive files. Compress raw data files.
	 * 
	 * @param con
	 * @return
	 */
	public static int archiveFiles (Context con)
	{
		int archived = 0;
		File outFile;

		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir != null)
		{
			String[] files = dir.list();
			String str;
			for (int i = 0; i < files.length; i++)
			{
				File file = new File( dir, files[ i ] );
				str = file.getAbsolutePath();

				if (str.endsWith( FileManager.ZIP_EXT ))
				{
					// already zipped
					continue;
				}

				if (str.endsWith( FileManager.SSD_EXT ))
				{
					// compress file
					outFile = new File( FileManager.compressFile( str ) );
					if (outFile != null)
					{
						archived++;
						// delete original file
						deleteFile( file );
					}
				}
			}
		}

		return archived;
	}


	public static boolean deleteFile (File f)
	{
		boolean res = f.delete();
		if (res)
		{
			Space.logRelease( "Deleted " + f.getPath() );
		}
		else
		{
			Log.e( Space.TAG, "Error deleting file " + f.getPath() );
		}
		return res;
	}


	public static boolean deleteFile (String path)
	{
		if (path == null || path.length() < 2)
		{
			return true;
		}
		return deleteFile( new File( path ) );
	}


	public static boolean existsFile (String path)
	{
		if (path == null)
		{
			return false;
		}
		File f = new File( path );
		return f.exists();
	}


	/**
	 * Purges all "old" (older than the set expire time) data files. Depending on the (user) settings, this may or may
	 * not include hypno and dream files.
	 * 
	 * @param con
	 * @return
	 */
	public static int purgeOldFiles (Context con)
	{
		int purged = 0;
		// purge files
		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir == null)
		{
			Space.showToast( con, R.string.textErrorIO );
			return purged;
		}

		long expTime = Long.parseLong( CyopsString.DATA_EXPIRE.get() ) * 86400000L;

		boolean includeHypno = CyopsBoolean.DATA_INCLUDE_HYPNO.get();
		boolean includeDream = CyopsBoolean.DATA_INCLUDE_DREAM.get();

		String[] files = dir.list();
		for (int i = 0; i < files.length; i++)
		{
			File file = new File( dir, files[ i ] );
			if ( (!includeHypno && file.getName().endsWith( FileManager.HYPNO_EXT ))
					|| (!includeDream && file.getName().endsWith( FileManager.DREAM_EXT )))
			{
				continue;
			}

			if (file.lastModified() < (System.currentTimeMillis() - expTime)
					|| (file.getName().endsWith( FileManager.SSD_EXT ) && file.length() < 64000)
					|| (file.getName().endsWith( FileManager.ZIP_EXT ) && file.length() < 128)
					|| (file.getName().endsWith( FileManager.HYPNO_EXT ) && file.length() < 128))
			{
				if (deleteFile( file ))
				{
					++purged;
				}
			}
		}

		return purged;
	}


	public static int purgeAllFiles (Context con)
	{
		int purged = 0;
		// purge files
		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir == null)
		{
			Space.showToast( con, R.string.textErrorIO );
			return purged;
		}

		String[] files = dir.list();
		for (int i = 0; i < files.length; i++)
		{
			File file = new File( dir, files[ i ] );
			if (file.getName().endsWith( FileManager.SSD_EXT ) || file.getName().endsWith( FileManager.ZIP_EXT )
					|| file.getName().endsWith( FileManager.HYPNO_EXT ))
			{
				if (deleteFile( file ))
				{
					++purged;
				}
			}
		}

		return purged;
	}


	/**
	 * Returns the file path of the lastCount last hypno file.
	 * 
	 * @param con
	 * @param lastCount
	 *            If == 0 returns the most recent hypno file.
	 * @return
	 */
	public static String getLastHypnoFile (Context con, int lastCount)
	{
		File dir = FileManager.getFilesDir( con, FileRequest.READ );
		if (dir == null)
		{
			Space.showToast( con, R.string.textErrorIO );
			return null;
		}

		File[] files = dir.listFiles();

		ArrayList< File > list = new ArrayList< File >();
		Collections.addAll( list, files );
		Collections.sort( list, new Comparator< File >() {

			public int compare (File lhs, File rhs)
			{
				// sort by date, most recent first
				return rhs.getName().compareTo( lhs.getName() );
			}

		} );

		String str;

		// find most recent hypnogram = first hypno found since we sorted most recent first
		for (File f : list)
		{
			str = f.getName();
			if (str.length() < 5)
				continue;

			if (str.endsWith( FileManager.HYPNO_EXT ))
			{
				if (lastCount <= 0)
					return f.getAbsolutePath();
				--lastCount;
			}
		}

		return null;
	}
}
