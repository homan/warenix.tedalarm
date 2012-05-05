package org.dyndns.warenix.tedalarm.app;

import java.util.List;

import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.tedalarm.ui.AlarmEditFragment;
import org.dyndns.warenix.tedalarm.ui.AlarmEditFragment.AlarmEditListener;
import org.dyndns.warenix.tedalarm.ui.AlarmListFragment;
import org.dyndns.warenix.tedalarm.ui.AlarmListFragment.AlarmListListener;
import org.dyndns.warenix.tedalarm.ui.AlarmRingFragment;
import org.dyndns.warenix.tedalarm.ui.SyncFragment;
import org.dyndns.warenix.util.WLog;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Main activity to for user to interact. It will change different ui views
 * according to user actions.
 * 
 * @author warenix
 * 
 */
public class TedAlarmActivity extends SherlockFragmentActivity implements
		AlarmListListener, AlarmEditListener {

	static {
		WLog.setAppName("tedalarm");
	}

	private static final String TAG = "TedAlarmActivity";

	private Fragment mCurrentFragment;
	private Fragment mFragment1;

	/**
	 * from edit alarm view
	 */
	private static final int MESSAGE_ALARM_SAVED = 1;
	private static final int MESSAGE_ALARM_DELETED = 2;

	private Handler mHideEditAlarmViewHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_ALARM_DELETED:
			case MESSAGE_ALARM_SAVED:
				onBackPressed();
				break;
			}

		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		AlarmListFragment alarmListFragment = AlarmListFragment
				.newInstance(this);
		showFragment(alarmListFragment, false);

		Uri alarmUri = getIntent().getData();
		// test data
		// TedAlarm alarm = new TedAlarm();
		// alarm.id = 18;
		// alarmUri = AlarmMaster.convertAlarmToUri(alarm);
		if (alarmUri != null) {
			List<?> segments = alarmUri.getPathSegments();
			String id = (String) segments.get(0);
			Bundle args = AlarmRingFragment.prepareAlarmRingBundle(Long
					.parseLong(id));
			AlarmRingFragment alarmRingFragment = AlarmRingFragment
					.newInstance(args);
			showFragment(alarmRingFragment, true);
		}

		// show menu
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		mFragment1 = fm.findFragmentByTag("f1");
		if (mFragment1 == null) {
			mFragment1 = SyncFragment.newInstance();
			ft.add(mFragment1, "f1");
		}
		ft.commit();
		// testTedAlarmProvider();
		// testHolidayProvider();
	}

	private void testTedAlarmProvider() {
		Uri empsUri = Uri.parse("content://tedalarm/"
				+ TedAlarmMeta.PATH_SCHEDULED_ALARM);
		Cursor cursor = null;
		ContentValues cvs = null;

		// test query
		cursor = getContentResolver().query(empsUri, null, null, null, null);
		if (cursor == null) {
			WLog.i(TAG, String.format("cursor is null"));
		} else {
			WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		}

		// WLog.i(TAG, String.format("test insert"));
		// cvs = new ContentValues();
		// cvs.put(TedAlarmMeta.TableAlarm.COL_DESCRIPTION, "test insert alarm "
		// + new Date().toString());
		// cvs.put(TedAlarmMeta.TableAlarm.COL_REPEAT_MASK, 10);
		// cvs.put(TedAlarmMeta.TableAlarm.COL_SCHEDULED, 20);
		// cvs.put(TedAlarmMeta.TableAlarm.COL_START_TIME,
		// System.currentTimeMillis());
		// // URi of the new inserted item
		// Uri newAlarm = getContentResolver().insert(empsUri, cvs);
		// WLog.d(TAG, String.format("new uri[%s]", newAlarm));

		// WLog.i(TAG, String.format("test update"));
		// cvs = new ContentValues();
		// cvs.put(TedAlarmMeta.TableAlarm.COL_DESCRIPTION,
		// "Updatedtest insert alarm " + new Date().toString());
		// cvs.put(TedAlarmMeta.TableAlarm.COL_REPEAT_MASK, 110);
		// cvs.put(TedAlarmMeta.TableAlarm.COL_SCHEDULED, 120);
		// cvs.put(TedAlarmMeta.TableAlarm.COL_START_TIME,
		// System.currentTimeMillis());
		// // int rowsNumber = getContentResolver().update(empsUri, cvs,
		// // "=?", new String[] { "8" });
		// int rowsNumber = getContentResolver().update(newAlarm, cvs, null,
		// null);
		// WLog.i(TAG, String.format("test updated row[%d]", rowsNumber));

		// WLog.i(TAG, String.format("test delete"));
		// Uri deleteUri = Uri.parse("content://tedalarm");
		// // delete employee of id 8
		// // rowsNumber = getContentResolver().delete(deleteUri,
		// // TedAlarmMeta.TableAlarm.COL_ID + "=?", new String[] { "1" });
		// rowsNumber = getContentResolver().delete(newAlarm, null, null);
		// WLog.i(TAG, String.format("test delete row[%d]", rowsNumber));

	}

	void testHolidayProvider() {
		Uri empsUri = Uri.parse("content://tedalarm/holidays/" + 123);
		ContentValues cvs = null;

		// WLog.i(TAG, String.format("test insert"));
		// cvs = new ContentValues();
		// cvs.put(TedAlarmMeta.TableHolidayColumns.COL_ALARM_ID, "123");
		// cvs.put(TedAlarmMeta.TableHolidayColumns.COL_CALENDAR_ID,
		// "cal_123a");
		// // URi of the new inserted item
		// Uri newAlarm = getContentResolver().insert(empsUri, cvs);
		// WLog.d(TAG, String.format("new uri[%s]", newAlarm));

		// // test query
		// Cursor cursor = getContentResolver().query(empsUri, null, null, null,
		// null);
		// if (cursor == null) {
		// WLog.i(TAG, String.format("cursor is null"));
		// } else {
		// WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		// }

		WLog.i(TAG, String.format("test delete"));
		// delete employee of id 8
		// rowsNumber = getContentResolver().delete(deleteUri,
		// TedAlarmMeta.TableAlarm.COL_ID + "=?", new String[] { "1" });
		int rowsNumber = getContentResolver().delete(empsUri, null, null);
		WLog.i(TAG, String.format("test delete row[%d]", rowsNumber));
	}

	@Override
	public void onBackPressed() {
		int displayOption = getSupportActionBar().getDisplayOptions();
		boolean isHomeAsUp = (displayOption & ActionBar.DISPLAY_HOME_AS_UP) != 0;
		getSupportActionBar().setDisplayHomeAsUpEnabled(!isHomeAsUp);
		super.onBackPressed();
	}

	void showFragment(Fragment f, boolean enableBackstack) {
		if (mCurrentFragment == f) {
			return;
		}
		mCurrentFragment = f;

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.fragment_container, f);
		if (enableBackstack) {
			ft.addToBackStack(null);
		}
		ft.commitAllowingStateLoss();

		getSupportActionBar().setDisplayHomeAsUpEnabled(enableBackstack);
	}

	// +AlarmListListener
	@Override
	public void onAlarmClicked(int position, long id) {
		Bundle args = AlarmEditFragment.prepareEditAlarmBundle(id);
		showFragment(AlarmEditFragment.newInstance(this, args), true);
	}

	@Override
	public void onAddAlarmClicked() {
		Bundle args = AlarmEditFragment.prepareNewAlarmBundle();
		showFragment(AlarmEditFragment.newInstance(this, args), true);
	}

	// -AlarmListListener
	// +AlarmEditListener
	@Override
	public void onSave(final int actionType, final TedAlarm alarm) {

		new Thread() {
			public void run() {
				WLog.i(TAG, String.format("insert/update alarm"));
				ContentValues cvs;
				Uri updateUri = Uri.parse(String.format(
						"content://tedalarm/%d", alarm.id));

				cvs = new ContentValues();
				cvs.put(TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION,
						alarm.description);
				cvs.put(TedAlarmMeta.TableAlarmColumns.COL_REPEAT_MASK,
						alarm.repeatMask);
				cvs.put(TedAlarmMeta.TableAlarmColumns.COL_SCHEDULED,
						alarm.scheduled);
				cvs.put(TedAlarmMeta.TableAlarmColumns.COL_START_TIME,
						alarm.startTime);

				if (actionType == AlarmEditFragment.ACTION_TYPE_EDIT_ALARM) {
					int rowsNumber = getContentResolver().update(updateUri,
							cvs, null, null);
					WLog.i(TAG,
							String.format("updated alarm row[%d]", rowsNumber));
				} else if (actionType == AlarmEditFragment.ACTION_TYPE_NEW_ALARM) {
					Uri insertUri = Uri.parse("content://tedalarm");
					Uri newUri = getContentResolver().insert(insertUri, cvs);
					WLog.i(TAG,
							String.format("inserted alarm to url[%s]", newUri));
					List<String> segments = newUri.getPathSegments();
					alarm.id = Long.parseLong(segments.get(0));
				}

				AlarmMaster.removeAllAlarmHoliday(getApplicationContext(),
						alarm);
				AlarmMaster.addAllAlarmHoliday(getApplicationContext(), alarm);
				AlarmMaster.rescheduleAlarm(getApplicationContext(), alarm);

				mHideEditAlarmViewHandler.sendEmptyMessage(MESSAGE_ALARM_SAVED);
			}
		}.start();

	}

	@Override
	public void onDelete(final TedAlarm alarm) {
		new Thread() {
			public void run() {
				Uri deleteUri = Uri.parse(String.format(
						"content://tedalarm/%d", alarm.id));
				int rowsNumber = getContentResolver().delete(deleteUri, null,
						null);
				WLog.i(TAG, String.format("test delete row[%d]", rowsNumber));

				AlarmMaster.removeAllAlarmHoliday(getApplicationContext(),
						alarm);

				mHideEditAlarmViewHandler
						.sendEmptyMessage(MESSAGE_ALARM_DELETED);
			}
		}.start();
	}

}