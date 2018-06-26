/**
 * 
 */
package com.gradlspace.cys;


import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import com.gradlspace.cys.activities.DeviceTestActivity;




/**
 * @author Falling
 * 
 */
public class Guardian implements Thread.UncaughtExceptionHandler, Runnable
{

	private static Thread.UncaughtExceptionHandler	defaultUEH;

	private static Context							app		= null;

	private static StringBuilder					report	= new StringBuilder( 8192 );


	public void uncaughtException (Thread t, Throwable e)
	{
		collectReport( e );
		defaultUEH.uncaughtException( t, e );
	}


	public static void setHandler (Context activity)
	{
		if (Guardian.app == null)
		{
			Guardian.app = activity;
			Guardian.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler( new Guardian() );
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run ()
	{
		dispatchReport( null );
	}


	public static void clearReport ()
	{
		report = new StringBuilder( 8192 );
	}


	public static String collectSupportInfo (Context con)
	{
		report.setLength( 0 );

		// Package info
		PackageInfo info;
		try
		{
			info = con.getPackageManager().getPackageInfo( con.getPackageName(), 0 );
			report.append( "<App>\n" ).append( "   <PackageName>" ).append( info.packageName ).append( "</PackageName>\n" )
					.append( "   <VersionName>" ).append( info.versionName ).append( "</VersionName>\n" )
					.append( "   <VersionCode>" ).append( info.versionCode ).append( "</VersionCode>\n" ).append( "</App>\n\n" );

		}
		catch (NameNotFoundException ex)
		{}
		catch (NullPointerException ex)
		{}

		// Build info
		report.append( "<Build>\n" );
		report.append( "   <Brand>" ).append( Build.BRAND ).append( "</Brand>\n" );
		report.append( "   <Device>" ).append( Build.DEVICE ).append( "</Device>\n" );
		report.append( "   <Model>" ).append( Build.MODEL ).append( "</Model>\n" );
		report.append( "   <Label>" ).append( Build.ID ).append( "</Label>\n" );
		report.append( "   <Product>" ).append( Build.PRODUCT ).append( "</Product>\n" );
		report.append( "   <Tags>" ).append( Build.TAGS ).append( "</Tags>\n" );
		report.append( "   <Type>" ).append( Build.TYPE ).append( "</Type>\n" );
		report.append( "   <SDK>" ).append( Build.VERSION.SDK ).append( "</SDK>\n" );
		report.append( "   <Release>" ).append( Build.VERSION.RELEASE ).append( "</Release>\n" );
		report.append( "   <Incremental>" ).append( Build.VERSION.INCREMENTAL ).append( "</Incremental>\n" );
		report.append( "</Build>\n\n" );

		// Sensor info
		report.append( DeviceTestActivity.enumSensors( con ) );

		return report.toString();
	}


	/**
	 * Dispatches an issue report via email application.
	 * 
	 * @param additionalInfo
	 */
	public static void dispatchReport (String additionalInfo)
	{
		if (additionalInfo != null)
		{
			report.append( "<CustomReport>\n" ).append( "<Data>" ).append( additionalInfo )
					.append( "</Data>\n</CustomReport>\n\n" );
		}

		// Package info
		PackageInfo info;
		try
		{
			info = app.getPackageManager().getPackageInfo( app.getPackageName(), 0 );
			report.append( "<App>\n" ).append( "   <PackageName>" ).append( info.packageName ).append( "</PackageName>\n" )
					.append( "   <VersionName>" ).append( info.versionName ).append( "</VersionName>\n" )
					.append( "   <VersionCode>" ).append( info.versionCode ).append( "</VersionCode>\n" ).append( "</App>\n\n" );

		}
		catch (NameNotFoundException ex)
		{}
		catch (NullPointerException ex)
		{}

		if (Space.logBuffer != null)
		{
			report.append( "<LogBuffer>" ).append( Space.logBuffer ).append( "</LogBuffer>\n\n" );
		}

		// Build info
		report.append( "<Build>\n" );
		report.append( "   <Brand>" ).append( Build.BRAND ).append( "</Brand>\n" );
		report.append( "   <Device>" ).append( Build.DEVICE ).append( "</Device>\n" );
		report.append( "   <Model>" ).append( Build.MODEL ).append( "</Model>\n" );
		report.append( "   <Label>" ).append( Build.ID ).append( "</Label>\n" );
		report.append( "   <Product>" ).append( Build.PRODUCT ).append( "</Product>\n" );
		report.append( "   <Tags>" ).append( Build.TAGS ).append( "</Tags>\n" );
		report.append( "   <Type>" ).append( Build.TYPE ).append( "</Type>\n" );
		report.append( "   <SDK>" ).append( Build.VERSION.SDK ).append( "</SDK>\n" );
		report.append( "   <Release>" ).append( Build.VERSION.RELEASE ).append( "</Release>\n" );
		report.append( "   <Incremental>" ).append( Build.VERSION.INCREMENTAL ).append( "</Incremental>\n" );
		report.append( "</Build>\n\n" );

		// Sensor info
		if (DeviceTestActivity.configString != null)
			report.append( DeviceTestActivity.configString );

		FileOutputStream trace;

		if (Space.sendReport)
		{
			Intent i = new Intent( Intent.ACTION_SEND );
			i.setType( "message/rfc822" );
			i.putExtra( Intent.EXTRA_EMAIL, new String[] { "issues@gradlspace.com" } );
			i.putExtra( Intent.EXTRA_SUBJECT, "Cyclosleep Autogenerated Issue Report " );
			i.putExtra( Intent.EXTRA_TEXT, report.toString() );
			try
			{
				// Intent.createChooser( i, "Send crash report?" ) not working?
				app.startActivity( i );
			}
			catch (android.content.ActivityNotFoundException ex)
			{
				// Toast.makeText( this, "There are no email clients installed.", Toast.LENGTH_SHORT ).show();
				try
				{
					trace = app.openFileOutput( "stack.trace", Context.MODE_WORLD_READABLE );
					trace.write( report.toString().getBytes() );
					trace.close();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
		else
		{
			try
			{
				trace = app.openFileOutput( "stack.trace", Context.MODE_WORLD_READABLE );
				trace.write( report.toString().getBytes() );
				trace.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
	}


	public void collectReport (Throwable e)
	{
		Log.e( Space.TAG, "Unhandled Exception. Creating report..." );

		StackTraceElement[] arr = e.getStackTrace();
		report.append( "We are very sorry that you have encountered an unrecoverable error.\nIf you wish to help us fix the problem, send this report - just as it is - to issues@gradlspace.com\nThe address should already be filled in, just hit the Send button.\n\nThank you!\n\nIssue Report:\n\n" );
		report.append( "<Exception>" ).append( e.toString() ).append( "</Exception>\n\n" ).append( "<CallingStack>" );
		for (int i = 0; i < arr.length; i++)
		{
			report.append( "   " ).append( arr[ i ].toString() ).append( "\n" );
		}
		report.append( "</CallingStack>\n\n" );

		// If the exception was thrown in a background thread inside
		// AsyncTask, then the actual exception can be found with getCause
		report.append( "<AddStack>" );
		Throwable cause = e.getCause();
		if (cause != null)
		{
			report.append( cause.toString() ).append( "\n\n" );
			arr = cause.getStackTrace();
			for (int i = 0; i < arr.length; i++)
			{
				report.append( "   " ).append( arr[ i ].toString() ).append( "\n" );
			}
		}
		report.append( "</AddStack>\n\n" );

		this.run();
		// app.runOnUiThread( this );
	}
}
