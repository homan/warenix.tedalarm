package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.EventList.EventListItem;
import org.dyndns.warenix.com.google.calendar.GoogleCalendarMaster;
import org.dyndns.warenix.google.calendar.provider.GoogleCalendarMeta;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.util.WLog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.GoogleAppInfo;
import com.google.api.GoogleOAuthAccessToken;
import com.google.api.GoogleOAuthActivity;

/**
 * A fragment that sync google calendar list and events to database
 */
public class SyncFragment extends SherlockFragment {
	private static final String TAG = "SyncFragment";

	ProgressFragment mProgressFragment;

	public static SyncFragment newInstance() {
		SyncFragment f = new SyncFragment();
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.alarm_edit_sync, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_sync:
			onSync();
		}
		return super.onOptionsItemSelected(item);
	}

	GoogleOAuthAccessToken mAccessToken;

	void onSync() {
		Context context = getActivity().getApplicationContext();
		mAccessToken = GoogleOAuthAccessToken.load(context);
		final GoogleAppInfo appInfo = new GoogleAppInfo(context);

		if (mAccessToken.accessToken == null) {
			WLog.i(TAG, String.format("no google oauth token found"));
			GoogleOAuthActivity.startOauthActivity(context, appInfo, false,
					null);
		} else if (mAccessToken.hasExpired()) {
			WLog.i(TAG, String.format("google oauth token has expired"));
			GoogleOAuthActivity
					.startOauthActivity(context, appInfo, true, null);
		} else {
			new SyncGoogleCalendarAsyncTask().execute();
		}
	}

	/**
	 * download latest calendar list and future events from google calendar
	 * 
	 * @param context
	 */
	void syncGoogleCalendar(Context context) {
		ArrayList<CalendarListItem> calendarList = GoogleCalendarMaster
				.getAllCalendar(mAccessToken);
		if (calendarList == null) {
			WLog.d(TAG, "calendar list is null");
		} else {
			ContentValues cvs = null;
			Uri empsUri = Uri.parse("content://googlecalendar/"
					+ GoogleCalendarMeta.PATH_ALL_CALENDAR);
			// remove all calendar
			int rows = context.getContentResolver().delete(empsUri, null, null);
			WLog.d(TAG, String.format("removed [%d] rows", rows));

			// insert database
			for (CalendarListItem calendarListItem : calendarList) {
				WLog.i(TAG, String.format("test insert"));
				cvs = new ContentValues();
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_KIND,
						calendarListItem.kind);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ETAG,
						calendarListItem.etag);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ID,
						calendarListItem.id);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_SUMMARY,
						calendarListItem.summary);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_DESCRIPTION,
						calendarListItem.description);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_COLOR_ID,
						calendarListItem.colorId);
				cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ACCESS_ROLE,
						calendarListItem.accessRole);
				// URi of the new inserted item
				Uri newAlarm = context.getContentResolver()
						.insert(empsUri, cvs);
				WLog.d(TAG, String.format(
						"updated calender list item to new uri[%s]", newAlarm));

				ArrayList<EventListItem> eventList = GoogleCalendarMaster
						.getFutureEvent(mAccessToken, calendarListItem.id);
				if (eventList != null) {
					WLog.d(TAG, String.format("calender [%s] has [%d] events",
							calendarListItem.summary, eventList.size()));
					for (EventListItem event : eventList) {
						if (event.isAllDay()) {
							WLog.d(TAG, String.format(
									"event summary[%s] start[%s] end[%s]",
									event.summary, event.startTime,
									event.endTime));
							cvs = convertEventToContentValues(event);
							newAlarm = context.getContentResolver().insert(
									empsUri, cvs);
							WLog.d(TAG, String.format(
									"inserted event as uri[%s]", newAlarm));
						}
					}
				}
			}
		}

	}

	public ContentValues convertEventToContentValues(
			EventListItem calendarListItem) {
		ContentValues cvs = new ContentValues();
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_KIND,
				calendarListItem.kind);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ETAG,
				calendarListItem.etag);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ID,
				calendarListItem.id);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_SUMMARY,
				calendarListItem.summary);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_DESCRIPTION,
				calendarListItem.description);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_COLOR_ID,
				calendarListItem.colorId);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_ACCESS_ROLE,
				calendarListItem.accessRole);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_START_TIME,
				calendarListItem.startTime);
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_END_TIME,
				calendarListItem.endTime);
		return cvs;
	}

	/**
	 * async task to sync google calendar
	 * 
	 * @author warren
	 * 
	 */
	class SyncGoogleCalendarAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			syncGoogleCalendar(getActivity());
			return null;
		}

		protected void onPreExecute() {
			if (mProgressFragment == null) {
				mProgressFragment = new ProgressFragment();
				mProgressFragment.show(getFragmentManager(), "sync");
			}
		}

		@Override
		protected void onPostExecute(Void v) {
			mProgressFragment.dismiss();
			mProgressFragment = null;
		}
	}

	/**
	 * show a progress bar while downloading google calendar
	 * 
	 * @author warenix
	 * 
	 */
	static class ProgressFragment extends SherlockDialogFragment {
		@Override
		public Dialog onCreateDialog(final Bundle savedInstanceState) {
			final ProgressDialog dialog = new ProgressDialog(getActivity());
			//
			dialog.setTitle("Sync Google Calendars");
			dialog.setMessage("Downloading google calendars and events... Please wait.");
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}
	}
}