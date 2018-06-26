/**
 * 
 */
package com.gradlspace.cys;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import com.gradlspace.cys.FileManager.FileRequest;




/**
 * @author Falling
 * 
 */
public class Dream
{
	public long		timestamp	= -1;
	public byte		emotion;
	public String	headline;
	public byte		rating;
	public String	text;
	public String	absolutePath;


	public static String getEmotionString (byte emotion)
	{
		String str;
		switch (emotion)
		{
			case 0:
				str = "Shame/Humiliation";
				break;
			case 1:
				str = "Fear/Terror";
				break;
			case 10:
				str = "Distress/Anguish";
				break;
			case 11:
				str = "Anger/Rage";
				break;
			case 100:
				str = "Contempt/Disgust";
				break;
			case 101:
				str = "Enjoyment/Joy";
				break;
			case 110:
				str = "Surprise";
				break;
			case 111:
				str = "Interest/Excitement";
				break;
			default:
				str = "";
				break;
		}

		return str;
	}


	public static byte getEmotionByte (String str)
	{
		if (str != null)
		{
			if (str.startsWith( "Shame" ))
			{
				return 0;
			}
			else if (str.startsWith( "Fear" ))
			{
				return 1;
			}
			else if (str.startsWith( "Dist" ))
			{
				return 10;
			}
			else if (str.startsWith( "Ang" ))
			{
				return 11;
			}
			else if (str.startsWith( "Cont" ))
			{
				return 100;
			}
			else if (str.startsWith( "Enj" ))
			{
				return 101;
			}
			else if (str.startsWith( "Sur" ))
			{
				return 110;
			}
			else if (str.startsWith( "Int" ))
			{
				return 111;
			}
		}

		return 127;
	}


	/**
	 * Save Dream to filename
	 * 
	 * @param con
	 * @param filename
	 * @return
	 */
	public boolean saveToFile (Context con, String filename)
	{
		try
		{
			if (timestamp <= 0)
				return true;

			Time tt = new Time();
			tt.set( timestamp );

			if (filename == null)
			{
				filename = "dream_" + tt.format2445() + FileManager.DREAM_EXT;
			}

			File dir = FileManager.getFilesDir( con, FileRequest.WRITE_SMALL );
			if (dir == null)
				return false;
			File f = new File( dir, filename );
			FileWriter fw = new FileWriter( f, true );

			absolutePath = f.getAbsolutePath();

			// write contents
			fw.write( "DREAM01_Cyclosleep\n" );
			fw.write( String.format( "Date: %s\n", tt.format3339( false ) ) );
			fw.write( String.format( "Emotion: %s\n", getEmotionString( emotion ) ) );
			fw.write( String.format( "Headline: %s\nRating: %d\nText: %s", headline, rating, text ) );

			fw.close();

			return true;

		}
		catch (IOException e)
		{
			Log.w( "Storage", "Error writing " + filename, e );
		}

		return false;
	}


	public boolean loadFromFile (Context con, String filename)
	{
		try
		{
			File f = new File( filename );

			BufferedReader reader = new BufferedReader( new FileReader( f ) );

			// no file size
			if (f.length() <= 2)
			{
				reader.close();
				return false;
			}


			// initialize string (line) splitter
			TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter( ' ' );
			String line = null;
			String next = null;

			// ==============> read lines
			while ( (line = reader.readLine()) != null)
			{
				// split
				splitter.setString( line );

				// ==============> iterate columns
				if (splitter.hasNext())
				{
					next = splitter.next();

					if (next.startsWith( "Date" ))
					{
						if (splitter.hasNext())
						{
							Time tt = new Time();
							tt.parse3339( splitter.next().trim() );
							timestamp = tt.toMillis( false );
						}
					}
					else if (next.startsWith( "Emotion" ))
					{
						if (splitter.hasNext())
						{
							emotion = Dream.getEmotionByte( splitter.next().trim() );
						}
					}
					else if (next.startsWith( "Headline" ))
					{
						if (splitter.hasNext())
						{
							headline = splitter.next().trim();
						}
					}
					else if (next.startsWith( "Rating" ))
					{
						if (splitter.hasNext())
						{
							rating = Byte.parseByte( splitter.next().trim() );
						}
					}
					else if (next.startsWith( "Text" ))
					{
						if (splitter.hasNext())
						{
							StringBuilder b = new StringBuilder();
							b.append( line.substring( 6 ) );

							while ( (line = reader.readLine()) != null)
							{
								b.append( line );
							}

							text = b.toString();
						}
					}

				}
				// <=============
			}
			// <=============

			reader.close();

			return true;
		}
		catch (Exception e)
		{
			// AUTOCATCH: Auto-generated catch block
			e.printStackTrace();
		}


		return false;
	}
}
