package org.dyndns.warenix.tedalarm.ui;

import java.util.Date;

import org.dyndns.warenix.tedalarm.AlarmUtils;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.R.id;
import org.dyndns.warenix.tedalarm.R.layout;
import org.dyndns.warenix.tedalarm.R.menu;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * UI to edit/ create an alarm.
 * 
 * @author warenix
 * 
 */
public class AlarmEditFragment extends SherlockFragment {
	private static final String TAG = "AlarmEditFragment";

	protected AlarmEditListener mAlarmEditListener;

	public static final String BUNDLE_ACTION_TYPE = "action_type";
	public static final String BUNDLE_ALARM_ID = "alarm_id";

	public static final int ACTION_TYPE_NEW_ALARM = 1;
	public static final int ACTION_TYPE_EDIT_ALARM = 2;

	EditText mDescription;
	TimePicker mStartTime;
	CheckBox mScheduled;
	CheckBox mRepeat;

	private static class InputParam {
		long alarmId;
		int actionType;
	}

	protected InputParam mInputParam = new InputParam();

	public static AlarmEditFragment newInstance(
			AlarmEditListener alarmEditListener, Bundle args) {
		AlarmEditFragment f = new AlarmEditFragment();
		f.mAlarmEditListener = alarmEditListener;
		f.setArguments(args);
		return f;
	}

	public static Bundle prepareNewAlarmBundle() {
		Bundle bundle = new Bundle();
		bundle.putInt(BUNDLE_ACTION_TYPE, ACTION_TYPE_NEW_ALARM);
		return bundle;
	}

	public static Bundle prepareEditAlarmBundle(long alarmId) {
		Bundle bundle = new Bundle();
		bundle.putInt(BUNDLE_ACTION_TYPE, ACTION_TYPE_EDIT_ALARM);
		bundle.putLong(BUNDLE_ALARM_ID, alarmId);
		return bundle;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		readInputParam(args);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.alarm_edit, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		initView(getView());
		bindView();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.alarm_edit_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_save:
			onSave();
			return true;
		case R.id.menu_delete:
			onDelete();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void readInputParam(Bundle args) {
		if (args != null) {
			mInputParam.actionType = args.getInt(BUNDLE_ACTION_TYPE);
			switch (mInputParam.actionType) {
			case ACTION_TYPE_EDIT_ALARM:
				mInputParam.alarmId = args.getLong(BUNDLE_ALARM_ID);
				break;
			}
		}
	}

	void initView(View view) {
		mDescription = (EditText) view.findViewById(R.id.description);
		mStartTime = (TimePicker) view.findViewById(R.id.start_time);
		mScheduled = (CheckBox) view.findViewById(R.id.scheduled);
		mRepeat = (CheckBox) view.findViewById(R.id.repeat);
	}

	void bindView() {
		switch (mInputParam.actionType) {
		case ACTION_TYPE_EDIT_ALARM:
			bindEditAlarmView();
			break;
		case ACTION_TYPE_NEW_ALARM:
			bindNewAlarmView();
			break;
		}
	}

	void bindNewAlarmView() {
		bindStartTimeView(System.currentTimeMillis());
	}

	void bindEditAlarmView() {
		Cursor cursor;
		Uri empsUri = Uri.parse(String.format("content://tedalarm/%d",
				mInputParam.alarmId));
		WLog.d(TAG, String.format("empsUri[%s]", empsUri));
		cursor = getActivity().getContentResolver().query(empsUri, null, null,
				null, null);
		if (cursor == null) {
			WLog.i(TAG, String.format("cursor is null"));
		} else {
			WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		}

		if (cursor.moveToFirst()) {
			mDescription.setText(cursor.getString(cursor
					.getColumnIndex(TedAlarmMeta.TableAlarm.COL_DESCRIPTION)));

			long startTimeMs = cursor.getLong(cursor
					.getColumnIndex(TedAlarmMeta.TableAlarm.COL_START_TIME));
			bindStartTimeView(startTimeMs);
			boolean scheduled = cursor.getLong(cursor
					.getColumnIndex(TedAlarmMeta.TableAlarm.COL_SCHEDULED)) != 0L;
			mScheduled.setChecked(scheduled);

			boolean repeatMask = cursor.getLong(cursor
					.getColumnIndex(TedAlarmMeta.TableAlarm.COL_REPEAT_MASK)) != 0L;
			mRepeat.setChecked(repeatMask);
		}
		cursor.close();
	}

	void bindStartTimeView(long startTimeMs) {
		Date d = new Date(startTimeMs);
		mStartTime.setCurrentHour(d.getHours());
		mStartTime.setCurrentMinute(d.getMinutes());
	}

	/**
	 * Create an alarm object from UI view elements
	 * 
	 * @return
	 */
	private TedAlarm createAlarmFromView() {
		TedAlarm alarm = new TedAlarm();
		alarm.description = mDescription.getText().toString().trim();
		alarm.id = mInputParam.alarmId;
		alarm.startTime = AlarmUtils.convertAlarmTime(
				mStartTime.getCurrentHour(), mStartTime.getCurrentMinute());
		alarm.description = mDescription.getText().toString().trim();
		alarm.scheduled = mScheduled.isChecked() ? 1L : 0L;
		alarm.repeatMask = mRepeat.isChecked() ? 5L * 1000 : 0L;
		return alarm;
	}

	protected void onSave() {
		if (mAlarmEditListener != null) {
			TedAlarm alarm = createAlarmFromView();
			mAlarmEditListener.onSave(mInputParam.actionType, alarm);
		}
	}

	protected void onDelete() {
		if (mAlarmEditListener != null) {
			TedAlarm alarm = createAlarmFromView();
			mAlarmEditListener.onDelete(alarm);
		}
	}

	public interface AlarmEditListener {
		public void onSave(int actionId, TedAlarm alarm);

		public void onDelete(TedAlarm alarm);
	}
}
