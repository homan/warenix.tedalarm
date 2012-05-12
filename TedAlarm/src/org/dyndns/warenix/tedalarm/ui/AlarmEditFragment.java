package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;
import java.util.Date;

import org.dyndns.warenix.com.google.calendar.CalendarList;
import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.GoogleCalendarMaster;
import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.tedalarm.ui.CheckboxDialogFragment.OnCheckboxDialogListener;
import org.dyndns.warenix.util.WLog;

import android.app.DatePickerDialog.OnDateSetListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.GoogleOAuthAccessToken;

/**
 * UI to create/edit/save an alarm. If input bundle doesn't contain an
 * BUNDLE_ALARM_ID, it will treat it as a create action. Otherwise, it will load
 * the saved alarm from database and present a UI for user to edit.
 * 
 * @author warenix
 * 
 */
public class AlarmEditFragment extends SherlockFragment implements
		OnDateSetListener, OnCheckboxDialogListener {
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
	View mWeekDay;
	Button mHoliday;

	static ArrayList<CalendarListItem> sCalendarList;
	boolean[] mCalendarChecked;

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

	/**
	 * prepare input bundle for action new alarm
	 * 
	 * @return
	 */
	public static Bundle prepareNewAlarmBundle() {
		Bundle bundle = new Bundle();
		bundle.putInt(BUNDLE_ACTION_TYPE, ACTION_TYPE_NEW_ALARM);
		return bundle;
	}

	/**
	 * prepare input bundle for action edit alarm
	 * 
	 * @return
	 */
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
		readInputBundle(args);
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

		if (sCalendarList == null) {
			sCalendarList = GoogleCalendarMaster.getCalendarList(getActivity());
			if (sCalendarList != null) {
				mCalendarChecked = new boolean[sCalendarList.size()];
			}
		}

		initView(getView());
		bindView();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (ACTION_TYPE_NEW_ALARM == mInputParam.actionType) {
			inflater.inflate(R.menu.alarm_new_menu, menu);
		} else if (ACTION_TYPE_EDIT_ALARM == mInputParam.actionType) {
			inflater.inflate(R.menu.alarm_edit_menu, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		// case android.R.id.home:
		// AlarmMaster.actionStartAlarmRing(getActivity(), null);
		// return true;
		case R.id.menu_save:
			onSave();
			return true;
		case R.id.menu_delete:
			onDelete();
			return true;
		case R.id.menu_cancel:
			onCancel();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void readInputBundle(Bundle args) {
		if (args != null) {
			mInputParam.actionType = args.getInt(BUNDLE_ACTION_TYPE);
			switch (mInputParam.actionType) {
			case ACTION_TYPE_EDIT_ALARM:
				mInputParam.alarmId = args.getLong(BUNDLE_ALARM_ID);
				break;
			}
		}
	}

	/**
	 * init ui elements
	 * 
	 * @param view
	 */
	void initView(View view) {
		mDescription = (EditText) view.findViewById(R.id.description);
		mStartTime = (TimePicker) view.findViewById(R.id.start_time);
		mScheduled = (CheckBox) view.findViewById(R.id.scheduled);
		mRepeat = (CheckBox) view.findViewById(R.id.repeat);
		mWeekDay = view.findViewById(R.id.week_day);
		mHoliday = (Button) view.findViewById(R.id.holiday);

		mRepeat.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				mWeekDay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
			}
		});

		mHoliday.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showCalendarList();
			}
		});
	}

	/**
	 * set values ui elements
	 */
	void bindView() {
		switch (mInputParam.actionType) {
		case ACTION_TYPE_EDIT_ALARM:
			bindForActionEditAlarm();
			break;
		case ACTION_TYPE_NEW_ALARM:
			bindForActionNewAlarm();
			break;
		}
	}

	/**
	 * set values for new alarm action
	 */
	void bindForActionNewAlarm() {
		bindStartTimeView(System.currentTimeMillis());
	}

	/**
	 * set values for edit alarm action
	 */
	void bindForActionEditAlarm() {
		Cursor cursor;
		Uri empsUri = Uri.parse(String.format("content://tedalarm/%d",
				mInputParam.alarmId));
		WLog.d(TAG, String.format("empsUri[%s]", empsUri));
		cursor = getActivity().getContentResolver().query(empsUri, null, null,
				null, null);
		if (cursor == null) {
			WLog.i(TAG, String.format("cursor is null"));
			return;
		}
		if (cursor.moveToFirst()) {
			mDescription
					.setText(cursor.getString(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION)));

			long startTimeMs = cursor
					.getLong(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_START_TIME));
			bindStartTimeView(startTimeMs);
			boolean scheduled = cursor
					.getLong(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_SCHEDULED)) != 0L;
			mScheduled.setChecked(scheduled);

			boolean repeatMask = cursor
					.getLong(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_REPEAT_MASK)) != 0L;
			mRepeat.setChecked(repeatMask);
			mWeekDay.setVisibility(repeatMask ? View.VISIBLE : View.GONE);
			cursor.close();

			buildCalenderCheckedFromDb();
			buildCheckedCalendarList();
			bindHoliday(buildCheckedCalendarList());
		}
	}

	void bindStartTimeView(long startTimeMs) {
		Date d = new Date(startTimeMs);
		mStartTime.setCurrentHour(d.getHours());
		mStartTime.setCurrentMinute(d.getMinutes());
	}

	GoogleOAuthAccessToken mAccessToken;

	void showCalendarList() {
		if (sCalendarList != null) {
			ArrayList<String> items = new ArrayList<String>();
			for (CalendarListItem calendar : sCalendarList) {
				items.add(calendar.summary);
			}
			CheckboxDialogFragment f = CheckboxDialogFragment.newInstance(
					items, mCalendarChecked);
			f.setOnCheckboxDialogListener(this);
			f.show(getFragmentManager(), "holiday");
		}
	}

	/**
	 * Create an alarm object from UI view elements
	 * 
	 * @return
	 */
	protected TedAlarm createAlarmFromView() {
		TedAlarm alarm = new TedAlarm();
		alarm.description = mDescription.getText().toString().trim();
		alarm.id = mInputParam.alarmId;
		alarm.startTime = AlarmMaster.convertAlarmTime(
				mStartTime.getCurrentHour(), mStartTime.getCurrentMinute());
		alarm.description = mDescription.getText().toString().trim();
		alarm.scheduled = mScheduled.isChecked() ? 1L : 0L;
		// FIXME: hardcoded repeat interval to be 5 sec
		alarm.repeatMask = mRepeat.isChecked() ? 5L * 1000 : 0L;
		alarm.holidayList = buildCheckedCalendarList();
		return alarm;
	}

	/**
	 * want to save current alarm
	 */
	protected void onSave() {
		if (mAlarmEditListener != null) {
			TedAlarm alarm = createAlarmFromView();
			mAlarmEditListener.onSave(mInputParam.actionType, alarm);
		}
	}

	/**
	 * want to delete current alarm
	 */
	protected void onDelete() {
		if (mAlarmEditListener != null) {
			TedAlarm alarm = createAlarmFromView();
			mAlarmEditListener.onDelete(alarm);
		}
	}

	@Override
	public void onCancel() {
	}

	/**
	 * listener will be notified to event occurs about editing an alarm
	 * 
	 * @author warenix
	 * 
	 */
	public interface AlarmEditListener {
		/**
		 * when an alarm is wanted to be saved
		 * 
		 * @param actionId
		 * @param alarm
		 */
		public void onSave(int actionId, TedAlarm alarm);

		/**
		 * when an alarm is wanted to be deleted
		 * 
		 * @param alarm
		 */
		public void onDelete(TedAlarm alarm);

		/**
		 * when user do not want to add a new alarm
		 */
		public void onCancel();
	}

	// OnDateSetListener
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {

	}

	@Override
	public void onOk(boolean[] itemsChecked) {
		mCalendarChecked = itemsChecked;
		int count = 0;
		for (int i = 0; i < itemsChecked.length; ++i) {
			if (itemsChecked[i]) {
				++count;
			}
		}

		bindHoliday(buildCheckedCalendarList());

	}

	void buildCalenderCheckedFromDb() {
		ArrayList<String> checkedCalendarList = AlarmMaster
				.getCalendarIdOfAlarm(getActivity(), mInputParam.alarmId);
		if (sCalendarList != null) {
			mCalendarChecked = new boolean[sCalendarList.size()];
			CalendarListItem calendar = null;
			for (int i = 0; i < sCalendarList.size(); ++i) {
				calendar = sCalendarList.get(i);
				for (String checkedCalenderId : checkedCalendarList) {
					if (calendar.id.equals(checkedCalenderId)) {
						mCalendarChecked[i] = true;
						break;
					}
				}
			}
		}
	}

	/**
	 * display holiday with right values
	 * 
	 * @param checkedCalendarList
	 */
	private void bindHoliday(ArrayList<CalendarListItem> checkedCalendarList) {
		if (checkedCalendarList != null) {
			String allCalendarName = "";
			for (CalendarListItem calendar : checkedCalendarList) {
				allCalendarName += "\"" + calendar.summary + "\" ";
			}
			mHoliday.setText(String.format("Holiday +%s", allCalendarName));
		}
	}

	/**
	 * get a list of checked calendar
	 * 
	 * @return
	 */
	ArrayList<CalendarListItem> buildCheckedCalendarList() {
		if (mCalendarChecked != null) {
			ArrayList<CalendarListItem> checkedCalendarList = new ArrayList<CalendarList.CalendarListItem>();
			for (int i = 0; i < mCalendarChecked.length; ++i) {
				if (mCalendarChecked[i]) {
					checkedCalendarList.add(sCalendarList.get(i));
				}
			}
			return checkedCalendarList;
		}
		return null;
	}

}
