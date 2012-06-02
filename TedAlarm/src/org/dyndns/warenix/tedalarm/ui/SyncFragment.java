package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.EventList.EventListItem;
import org.dyndns.warenix.com.google.calendar.GoogleCalendarMaster;
import org.dyndns.warenix.google.calendar.provider.GoogleCalendarMeta;
import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.app.TedAlarmActivity;
import org.dyndns.warenix.util.WLog;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.GoogleAppInfo;
import com.google.api.GoogleOAuthAccessToken;
import com.google.api.ui.GoogleOAuthIntentService;

/**
 * A fragment that sync google calendar list and events to database
 */
public class SyncFragment extends SherlockFragment {
	private static final String TAG = "SyncFragment";

	private static final int SYNC_CALENDAR_NOTIFICATION_ID = 1;

	ProgressFragment mProgressFragment;

	boolean mSyncGoogleCalenderDone = true;

	protected static Object sSyncLock = new Object();

	private BroadcastReceiver mOAuthReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			WLog.d(TAG, String.format("received action[%s]", action));
			onSync(context, true);
		}
	};

	private Handler mOAuthHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			boolean result = data.getBoolean("result");
			String action = data.getString("action");
			if (result) {
				new SyncGoogleCalendarAsyncTask(getActivity()).execute();
			}
		}
	};

	public static SyncFragment newInstance() {
		SyncFragment f = new SyncFragment();
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		AlarmMaster.cancelSyncCalender(getActivity());
		AlarmMaster.scheduleSyncCalendar(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mProgressFragment != null && mSyncGoogleCalenderDone) {
			mProgressFragment.dismiss();
			mProgressFragment = null;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.alarm_edit_sync, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Context context = getActivity().getApplicationContext();

		int id = item.getItemId();
		switch (id) {
		case R.id.menu_sync:
			onSync(context, false);
		}
		return super.onOptionsItemSelected(item);
	}

	GoogleOAuthAccessToken mAccessToken;
	Messenger mMessenger = new Messenger(mOAuthHandler);

	public void onSync(Context context, boolean doInBackground) {
		if (canSync(context, doInBackground)) {
			if (doInBackground) {
				showNotification(context, SYNC_CALENDAR_NOTIFICATION_ID,
						"Synchronizing Google Calendar Events", "TedAlarm",
						"Synchronizing Google Calendar Events");
				syncGoogleCalendar(context);
				cancelNotification(context, SYNC_CALENDAR_NOTIFICATION_ID);
			} else {
				new SyncGoogleCalendarAsyncTask(context).execute();
			}
		}
	}

	private boolean canSync(Context context, boolean doInBackground) {
		WLog.i(TAG, "checking can sync");
		mAccessToken = GoogleOAuthAccessToken.load(context);
		if (mAccessToken.accessToken == null) {
			WLog.i(TAG, String.format("no google oauth token found"));

			Intent intent = GoogleOAuthIntentService.prepareActionOAuthIntent(
					mMessenger, doInBackground);
			context.startService(intent);

			registerOAuthReceiver(context);
		} else if (mAccessToken.hasExpired()) {
			WLog.i(TAG, String.format("google oauth token has expired"));

			Intent intent = GoogleOAuthIntentService
					.prepareActionRefreshTokenIntent(mMessenger,
							new GoogleAppInfo(context));
			context.startService(intent);
			registerOAuthReceiver(context);
		} else {
			return true;
		}
		return false;

	}

	private void registerOAuthReceiver(Context context) {
		IntentFilter intentFilter = new IntentFilter(
				GoogleOAuthIntentService.IntentAction.ACTION_OAUTH_DONE);
		context.registerReceiver(mOAuthReceiver, intentFilter);
	}

	private void showNotification(Context context, int id,
			CharSequence tickerText, CharSequence contentTitle,
			CharSequence contentText) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(ns);

		int icon = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Intent notificationIntent = new Intent(context, TedAlarmActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);

		// show
		mNotificationManager.notify(id, notification);
	}

	private void cancelNotification(Context context, int id) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(ns);
		// show
		mNotificationManager.cancel(id);
	}

	/**
	 * download latest calendar list and future events from google calendar
	 * 
	 * @param context
	 */
	private void syncGoogleCalendar(Context context) {
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
						.getFutureEvent(mAccessToken, calendarListItem.id, 7);
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
		cvs.put(GoogleCalendarMeta.TableAlarmColumns.COL_CALENDAR_ID,
				calendarListItem.calendarId);
		return cvs;
	}

	/**
	 * async task to sync google calendar
	 * 
	 * @author warren
	 * 
	 */
	class SyncGoogleCalendarAsyncTask extends AsyncTask<Void, Void, Void> {
		private Context mContext;

		public SyncGoogleCalendarAsyncTask(Context context) {
			mContext = context;
		}

		@Override
		protected Void doInBackground(Void... params) {
			synchronized (sSyncLock) {
				mSyncGoogleCalenderDone = false;
				syncGoogleCalendar(mContext);
				mSyncGoogleCalenderDone = true;
			}
			return null;
		}

		protected void onPreExecute() {
			if (mProgressFragment == null) {
				if (SyncFragment.this.isAdded()
						&& SyncFragment.this.isResumed()) {
					mProgressFragment = new ProgressFragment();
					mProgressFragment.show(getFragmentManager(), "sync");
				}
			}
		}

		@Override
		protected void onPostExecute(Void v) {
			if (mProgressFragment != null) {
				if (SyncFragment.this.isAdded()
						&& SyncFragment.this.isResumed()) {
					mProgressFragment.dismiss();
					mProgressFragment = null;
				}
			}
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

	// @Override
	// public void onOAuthSuccess(String code) {
	// WLog.i(TAG, "oauth success");
	//
	// }
	//
	// @Override
	// public void onOAuthFail(String errorCode) {
	// WLog.i(TAG, "oauth fail");
	// }
	//
	// @Override
	// public void onOAuthAccessTokenExchanged(GoogleOAuthAccessToken
	// accessToken) {
	// WLog.i(TAG, "oauth exchanged");
	// new SyncGoogleCalendarAsyncTask(getActivity()).execute();
	// }
}