/**
 * 
 */
package com.gradlspace.cys.activities;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.gradlspace.cys.Cyops.CyopsBoolean;
import com.gradlspace.cys.CysInternalData;
import com.gradlspace.cys.CysInternalData.SleepRecord;
import com.gradlspace.cys.FauLink;
import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;




/**
 * @author Falling
 * 
 */
public class SleepLogActivity extends ListActivity
{
	private static final int							DIALOG_FAU_UPLOAD	= 1;
	private static final int							DIALOG_DELETE		= 2;

	private ListAdapter									m_adapter;

	public ArrayList< LinkedHashMap< String, String >>	sleepLog			= null;

	public static int									m_selectedId		= -1;


	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.explorer );

		// FileManager.createSleepLog( this );
		sleepLog = new ArrayList< LinkedHashMap< String, String >>( Space.s_dbData.m_sleepRecords.size() );
		LinkedHashMap< String, String > map;

		Time tt = new Time();
		String strDuration = getString( R.string.textDuration );
		String strImg = Integer.toString( R.drawable.ic_file_hypno );
		String strRating = getString( R.string.textRating ) + ": ";

		// fill mListdata with mapped entries
		for (CysInternalData.SleepRecord rec : Space.s_dbData.m_sleepRecords)
		{
			map = new LinkedHashMap< String, String >( 6 );
			map.put( "file", Integer.toString( rec.id ) );
			map.put( "type", strDuration );
			map.put( "img", strImg );

			tt.set( rec.tstart );
			map.put( "date", tt.format( "%Y.%m.%d %H:%M" ) );

			// tt.set( f.lastModified() );
			if (rec.quality == -1)
				map.put( "mod", "n/a" );
			else
				map.put( "mod", strRating + Short.toString( rec.quality ) );

			map.put( "size", String.format( "%.2f h", rec.duration / 3600000d ) );

			sleepLog.add( map );
		}


		if (sleepLog != null)
		{
			// Now create a new list adapter bound to the simpleadapter.
			m_adapter = new SimpleAdapter( this, sleepLog, R.layout.explorer_list_item, new String[] { "img", "date", "mod",
					"type", "size" }, new int[] { R.id.imgExplorerList, R.id.lblExplorerListItem1, R.id.lblExplorerListItem2,
					R.id.lblExplorerListItem3, R.id.lblExplorerListItem4 } );

			// Bind to our new adapter.
			setListAdapter( m_adapter );
		}

		registerForContextMenu( getListView() );
	}


	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id)
	{
		super.onListItemClick( l, v, position, id );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected (MenuItem item)
	{
		File file;
		String str;
		Intent i;
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.viewHypno:
				if (sleepLog != null)
				{
					SleepRecord rec = Space.s_dbData.getRecordById( Integer
							.parseInt( sleepLog.get( (int) info.id ).get( "file" ) ) );

					if (rec != null && rec.hypnofile != null && rec.hypnofile.length() > 3)
					{
						file = new File( rec.hypnofile );
						i = new Intent( Intent.ACTION_VIEW, Uri.fromFile( file ), Space.get(), ViewerActivity.class );
						startActivity( i );
					}
				}
				return true;

			case R.id.viewDream:
				if (sleepLog != null)
				{
					SleepRecord rec = Space.s_dbData.getRecordById( Integer
							.parseInt( sleepLog.get( (int) info.id ).get( "file" ) ) );

					if (rec != null && rec.dreamfile.length() > 3)
					{
						file = new File( rec.dreamfile );
						i = new Intent( Intent.ACTION_VIEW, Uri.fromFile( file ), Space.get(), ViewerActivity.class );
						startActivity( i );
					}
				}
				return true;

			case R.id.emailHypno:
				i = new Intent( Intent.ACTION_SEND );
				i.setType( "message/rfc822" );
				i.putExtra( Intent.EXTRA_SUBJECT, "Cyclosleep File" );
				i.putExtra( Intent.EXTRA_TEXT, "File attached." );

				// str = FileManager.sleepLog.get( (int) info.id ).get( "file" );

				SleepRecord rec = Space.s_dbData.getRecordById( Integer.parseInt( sleepLog.get( (int) info.id ).get( "file" ) ) );

				if (rec == null)
				{
					return true;
				}

				str = rec.hypnofile;

				if (CyopsBoolean.DATA_ZIP.isEnabled())
				{
					str = FileManager.compressFile( str );
				}

				file = new File( str );
				i.putExtra( Intent.EXTRA_STREAM, Uri.fromFile( file ) );
				try
				{
					// Intent.createChooser( i, "Send crash report?" ) not working?
					startActivity( i );
				}
				catch (android.content.ActivityNotFoundException ex)
				{
					Space.showToast( this, R.string.errorNoEmailClient );
				}
				return true;

			case R.id.share:
				i = new Intent( Intent.ACTION_SEND );
				i.setType( "*/*" );

				str = FileManager.sleepLog.get( (int) info.id ).get( "file" );


				if (CyopsBoolean.DATA_ZIP.isEnabled())
				{
					str = FileManager.compressFile( str );
				}

				file = new File( str );
				i.putExtra( Intent.EXTRA_STREAM, Uri.fromFile( file ) );
				try
				{
					// Intent.createChooser( i, "Send crash report?" ) not working?
					startActivity( i );
				}
				catch (android.content.ActivityNotFoundException ex)
				{
					Space.showToast( this, R.string.errorNoEmailClient );
				}
				return true;

			case R.id.delete:
				if (sleepLog != null)
				{
					m_selectedId = (int) info.id;
					showDialog( DIALOG_DELETE );
				}
				return true;

			default:
				return super.onContextItemSelected( item );
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu( menu, v, menuInfo );
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.sleeplog_context_menu, menu );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.sleeplog_menu, menu );
		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.showraw:
				Space.startActivity( this, ExplorerActivity.class );
				finish();
				return true;

			case R.id.fau_upload:
				showDialog( DIALOG_FAU_UPLOAD );
				return true;

			default:
				return super.onOptionsItemSelected( item );
		}
	}


	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;

		if (id == DIALOG_FAU_UPLOAD)
		{
			// fau upload
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.fauUploadDlgTitle ).setMessage( R.string.fauUploadDlgMsg )
					.setPositiveButton( R.string.textUpload, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							FauLink.uploadData();
							// if (Space.startSendActivity( this,
							// Space.SEND_TYPE_EMAIL,
							// "sleeplogs@cs.fau.de",
							// "Cyclosleep Files",
							// "Files attached.",
							// null ) != 0)
							// {
							// Space.showToast( this, R.string.errorNoEmailClient );
							// }
						}
					} ).setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{

						}
					} ).create();
		}
		else if (id == DIALOG_DELETE)
		{
			// really delete dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.deleteDlgTitle ).setMessage( R.string.deleteDlgMsg )
					.setPositiveButton( R.string.textYes, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							if (m_selectedId >= 0)
							{
								if (Space.s_dbData.delete( Integer.parseInt( sleepLog.get( m_selectedId ).get( "file" ) ) ))
								{
									sleepLog.remove( m_selectedId );
									SleepLogActivity.this.onContentChanged();
								}
								else
								{
									Space.showToast( SleepLogActivity.this, R.string.textErrorIO );
								}
							}
						}
					} ).setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{

						}
					} ).create();
		}

		return dialog;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart ()
	{
		super.onStart();

		Space.showHint( this, R.string.textExplorerHint );
	}
}
