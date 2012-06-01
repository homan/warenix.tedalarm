package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.GoogleCalendarMaster;
import org.dyndns.warenix.tedalarm.ui.SyncFragment.ProgressFragment;
import org.dyndns.warenix.util.WLog;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * A fragment that sync google calendar list and events to database
 */
public class DebugFragment extends SherlockFragment {
	private static final String TAG = "DebugFragment";

	ProgressFragment mProgressFragment;

	public static DebugFragment newInstance() {
		DebugFragment f = new DebugFragment();
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		test();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		}
		return super.onOptionsItemSelected(item);
	}

	void test() {
		ArrayList<CalendarListItem> list = GoogleCalendarMaster
				.getCalendarList(getActivity());
		WLog.d(TAG,
				"get calendar list:"
						+ (list == null ? "not found" : list.size()
								+ " calendars found"));
	}

}