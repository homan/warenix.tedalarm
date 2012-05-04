package org.dyndns.warenix.tedalarm.ui;

import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Display all alarms. And allow user to create/delete
 * 
 * @author warenix
 * 
 */
public class AlarmListFragment extends SherlockListFragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "AlarmListFragment";

	// This is the Adapter being used to display the list's data.
	SimpleCursorAdapter mAdapter;

	// If non-null, this is the current filter the user has provided.
	String mCurFilter;

	AlarmListListener mAlarmListListener;

	public static AlarmListFragment newInstance(
			AlarmListListener alarmListListener) {
		AlarmListFragment f = new AlarmListFragment();
		f.mAlarmListListener = alarmListListener;
		return f;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Give some text to display if there is no data. In a real
		// application this would come from a resource.
		setEmptyText("No alarms");

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		// Create an empty adapter we will use to display the loaded data.
		mAdapter = new SimpleCursorAdapter(
				getActivity(),
				android.R.layout.simple_list_item_1,
				null,
				new String[] { TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION },
				new int[] { android.R.id.text1 }, 0);
		setListAdapter(mAdapter);

		// Start out with a progress indicator.
		setListShown(false);

		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onResume() {
		super.onResume();
		onRefresh();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.alarm_list_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_add:
			onAdd();
			return true;
		case R.id.menu_refresh:
			onRefresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// Insert desired behavior here.
		WLog.i("FragmentComplexList", "Item clicked: " + id);
		if (mAlarmListListener != null) {
			mAlarmListListener.onAlarmClicked(position, id);
		}
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created. This
		// sample only has one Loader, so we don't care about the ID.
		// First, pick the base URI to use depending on whether we are
		// currently filtering.

		Uri empsUri = Uri.parse("content://tedalarm");
		return new CursorLoader(getActivity(), empsUri,
				TedAlarmMeta.ALL_ALARM_LIST_PROJECTION, null, null, null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Swap the new cursor in. (The framework will take care of closing the
		// old cursor once we return.)
		mAdapter.swapCursor(data);

		// The list should now be shown.
		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// above is about to be closed. We need to make sure we are no
		// longer using it.
		mAdapter.swapCursor(null);
	}

	/**
	 * listen to alarm list event
	 * 
	 * @author warenix
	 * 
	 */
	public interface AlarmListListener {
		public void onAlarmClicked(int position, long id);

		public void onAddAlarmClicked();
	}

	protected void onAdd() {
		if (mAlarmListListener != null) {
			mAlarmListListener.onAddAlarmClicked();
		}
	}

	public void onRefresh() {
		getLoaderManager().restartLoader(0, null, this);
	}

}
