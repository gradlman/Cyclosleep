/**
 * 
 */
package com.gradlspace.cys.activities;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
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

import com.gradlspace.cys.FileManager;
import com.gradlspace.cys.R;
import com.gradlspace.cys.Space;




/**
 * @author Falling
 * 
 */
public class ExplorerActivity extends ListActivity
{
	private ListAdapter	m_adapter;


	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.explorer );

		FileManager.readDataFiles( this );

		if (FileManager.dataFiles != null)
		{
			// Now create a new list adapter bound to the simpleadapter.
			m_adapter = new SimpleAdapter( this, FileManager.dataFiles, R.layout.explorer_list_item, new String[] { "img",
					"date", "mod", "type", "size" }, new int[] { R.id.imgExplorerList, R.id.lblExplorerListItem1,
					R.id.lblExplorerListItem2, R.id.lblExplorerListItem3, R.id.lblExplorerListItem4 } );

			// Bind to our new adapter.
			setListAdapter( m_adapter );
		}

		registerForContextMenu( getListView() );

		Space.showHint( this, R.string.textExplorerHint );
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
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.view:
				if (FileManager.dataFiles != null)
				{
					Space.startViewerActivity( this, FileManager.dataFiles.get( (int) info.id ).get( "file" ) );
				}
				return true;

			case R.id.email:
				if (Space.startSendActivity(	this,
												Space.SEND_TYPE_EMAIL,
												null,
												"Cyclosleep File",
												"File attached.",
												FileManager.dataFiles.get( (int) info.id ).get( "file" ) ) != 0)
				{
					Space.showToast( this, R.string.errorNoEmailClient );
				}
				return true;

			case R.id.share:
				if (Space.startSendActivity(	this,
												Space.SEND_TYPE_GENERIC,
												null,
												null,
												null,
												FileManager.dataFiles.get( (int) info.id ).get( "file" ) ) != 0)
				{
					Space.showToast( this, R.string.errorNoEmailClient );
				}
				return true;

			case R.id.delete:
				if (FileManager.dataFiles != null)
				{
					if (FileManager.deleteFile( FileManager.dataFiles.get( (int) info.id ).get( "file" ) ) == false)
					{
						Space.showToast( this, R.string.textErrorIO );
					}
					else
					{
						FileManager.dataFiles.remove( (int) info.id );
						this.onContentChanged();
					}
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
		inflater.inflate( R.menu.explorer_context_menu, menu );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.explorer_menu, menu );
		return true;
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.showlog:
				Space.startActivity( this, SleepLogActivity.class );
				finish();
				return true;

			case R.id.archive:
				// compress data files
				int num = FileManager.archiveFiles( this );
				Space.showToast( this, String.format( getString( R.string.textArchived ), num ) );
				// TODO: change this to loop through the ListAdapter and remove the entry where map."file" ==
				// deletedfile

				this.finish();
				return true;

			case R.id.purge:
				showDialog( 0 );

				// TODO: change this to loop through the ListAdapter and remove the entry where map."file" ==
				// deletedfile
				// this.finish();
				return true;

			default:
				return super.onOptionsItemSelected( item );
		}
	}


	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;
		if (id == 0)
		{
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( R.string.textPurge ).setMessage( R.string.textPurgeFiles )
					.setPositiveButton( R.string.textPurgeAll, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int id)
						{
							Space.showToast(	ExplorerActivity.this,
												String.format(	getString( R.string.textPurged ),
																FileManager.purgeAllFiles( ExplorerActivity.this ) ) );
							FileManager.dataFiles.clear();
							ExplorerActivity.this.onContentChanged();
						}
					} ).setNeutralButton( R.string.textPurgeOut, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int id)
						{
							Space.showToast(	ExplorerActivity.this,
												String.format(	getString( R.string.textPurged ),
																FileManager.purgeOldFiles( ExplorerActivity.this ) ) );
							ExplorerActivity.this.finish();
						}
					} ).setNegativeButton( R.string.textCancel, new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int id)
						{
							dialog.cancel();
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


	}

}
