/**
 * 
 */
package com.gradlspace.cys;


import android.hardware.SensorEvent;
import de.lme.plotview.FloatValueList;
import de.lme.plotview.LmeFilter;
import de.lme.plotview.LmeFilter.WndIntFilter;




/**
 * @author Falling
 * 
 */
public class SensorProcessor
{
	public static SensorData		stream		= null;

	// Highpass Butterworth, Order 2, Fs 100, Fc 0.05
	//private static LmeFilter		highpassX	= new LmeFilter( 1, -2, 1, 0, 0, 1, -1.99556, 0.99557 );
	//private static LmeFilter		highpassY	= new LmeFilter( 1, -2, 1, 0, 0, 1, -1.99556, 0.99557 );
	//private static LmeFilter		highpassZ	= new LmeFilter( 1, -2, 1, 0, 0, 1, -1.99556, 0.99557 );
	
	private static LmeFilter		lowpassX	= new LmeFilter( 0.0625, 0.125, 0.0625, 0, 0, 1, -1.3, 0.5 );
	private static LmeFilter		lowpassY	= new LmeFilter( 0.0625, 0.125, 0.0625, 0, 0, 1, -1.3, 0.5 );
	private static LmeFilter		lowpassZ	= new LmeFilter( 0.0625, 0.125, 0.0625, 0, 0, 1, -1.3, 0.5 );
	
	private static LmeFilter		deriX		= new LmeFilter( 1, -2, 1, 0, 0, 0.0036, 0, 0);
	private static LmeFilter		deriY		= new LmeFilter( 1, -2, 1, 0, 0, 0.0036, 0, 0);
	private static LmeFilter		deriZ		= new LmeFilter( 1, -2, 1, 0, 0, 0.0036, 0, 0);

	//private static WndIntFilter		intX		= new WndIntFilter( 25 );
	//private static WndIntFilter		intY		= new WndIntFilter( 25 );
	//private static WndIntFilter		intZ		= new WndIntFilter( 25 );
	
	private static WndIntFilter		int1		= new WndIntFilter( 37 );
	private static WndIntFilter		int2		= new WndIntFilter( 74 );
	private static WndIntFilter		int4		= new WndIntFilter( 148 );
	private static WndIntFilter		int6		= new WndIntFilter( 232 );

	//private static LmeFilter		lowpassX	= new LmeFilter( 0.0947, 0.4053, 0.4053, 0.0947, 0d, 1, 0, 0 );
	//private static LmeFilter		lowpassY	= new LmeFilter( 0.0947, 0.4053, 0.4053, 0.0947, 0d, 1, 0, 0 );
	//private static LmeFilter		lowpassZ	= new LmeFilter( 0.0947, 0.4053, 0.4053, 0.0947, 0d, 1, 0, 0 );

	//private static FloatValueList	filtX		= new FloatValueList( 1024, true, true );
	//private static FloatValueList	filtY		= new FloatValueList( 1024, true, true );
	//private static FloatValueList	filtZ		= new FloatValueList( 1024, true, true );

	private static double			x, y, z;

	public static float				last;
	
	public static float				i1, i2, i4, i6;


	public static boolean newSensorEvent (SensorEvent event)
	{
		if (stream == null)
			return false;

		stream.add( event.timestamp, event.values[ 0 ], event.values[ 1 ], event.values[ 2 ] );

		/*
		//x = highpassX.next( event.values[ 0 ] );
		//y = highpassY.next( event.values[ 1 ] );
		//z = highpassZ.next( event.values[ 2 ] );
		
		x = intX.next( x );
		y = intY.next( y );
		z = intZ.next( z );

		x = lowpassX.next( x );
		y = lowpassY.next( y );
		z = lowpassZ.next( z );

		filtX.add( (float) x );
		filtY.add( (float) y );
		filtZ.add( (float) z );

		last = (float) (x + y + z);*/
		
		
		
		// 1
		x = lowpassX.next( event.values[ 0 ] );
		y = lowpassY.next( event.values[ 1 ] );
		z = lowpassZ.next( event.values[ 2 ] );
		
		// 2
		x = deriX.next( x );
		y = deriY.next( y );
		z = deriZ.next( z );

		// 3
		x = Math.abs( x );
		y = Math.abs( y );
		z = Math.abs( z );
		
		// 4
		last = (float) (x + y + z);
		
		// 5
		i1 = (float) int1.next( last );
		i2 = (float) int2.next( last );
		i4 = (float) int4.next( last );
		i6 = (float) int6.next( last );
		

		return true;
	}
}
