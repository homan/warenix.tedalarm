package org.dyndns.warenix.tedalarm.ui;

import java.io.IOException;
import java.util.Date;

import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * When alarm is times up.
 * 
 * @author warenix
 * 
 */
public class AlarmRingFragment extends SherlockFragment implements
		OnClickListener {
	private static final String TAG = "AlarmRingFragment";

	public static final String BUNDLE_ALARM_ID = "alarm_id";

	TextView mDescription;
	TimePicker mStartTime;
	Button mStop;

	MediaPlayer mMediaPlayer;

	TedAlarm mAlarm;
	AlarmRingListener mListener;

	private static class InputParam {
		long alarmId;
	}

	protected InputParam mInputParam = new InputParam();

	public static AlarmRingFragment newInstance(AlarmRingListener listener,
			Bundle args) {
		AlarmRingFragment f = new AlarmRingFragment();
		f.mListener = listener;
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle args = getArguments();
		readInputBundle(args);

		mAlarm = AlarmMaster.restoryAlarmById(getActivity(),
				mInputParam.alarmId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.alarm_ring, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (AlarmMaster.isTodayHoliday(getActivity(), mAlarm)) {
			if (mListener != null) {
				WLog.d(TAG,
						String.format("stop alarm because today is holiday"));
				mListener.onStopAlarm(AlarmRingListener.STOP_REASON_HOLIDAY);
			}
		} else {
			// We have a menu item to show in action bar.
			setHasOptionsMenu(true);

			initView(getView());
			bindView();
			playSound(getActivity(), getAlarmUri());
		}
		updateAlarmIfOneShot();
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

	void readInputBundle(Bundle args) {
		if (args != null) {
			mInputParam.alarmId = args.getLong(BUNDLE_ALARM_ID);
		}
	}

	void initView(View view) {
		mDescription = (TextView) view.findViewById(R.id.description);
		mStartTime = (TimePicker) view.findViewById(R.id.start_time);
		mStop = (Button) view.findViewById(R.id.stop);
	}

	void bindView() {
		bindEditAlarmView();
		mStop.setOnClickListener(this);
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
			return;
		}
		WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		if (cursor.moveToFirst()) {
			mDescription
					.setText(cursor.getString(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION)));

			long startTimeMs = cursor
					.getLong(cursor
							.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_START_TIME));
			bindStartTimeView(startTimeMs);
		}
		cursor.close();
	}

	void bindStartTimeView(long startTimeMs) {
		Date d = new Date(startTimeMs);
		mStartTime.setCurrentHour(d.getHours());
		mStartTime.setCurrentMinute(d.getMinutes());
	}

	public static Bundle prepareAlarmRingBundle(long alarmId) {
		Bundle bundle = new Bundle();
		bundle.putLong(BUNDLE_ALARM_ID, alarmId);
		return bundle;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.stop:
			onStopClicked();
			break;
		}
	}

	void onStopClicked() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer = null;
		}

		if (mListener != null) {
			mListener.onStopAlarm(AlarmRingListener.STOP_REASON_USER_STOP);
		}
	}

	private void playSound(Context context, Uri alert) {
		mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.setDataSource(context, alert);
			final AudioManager audioManager = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setVolume(0.2f, 0.2f);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (IOException e) {
			System.out.println("OOPS");
		}
	}

	// Get an alarm sound. Try for an alarm. If none set, try notification,
	// Otherwise, ringtone.
	private Uri getAlarmUri() {
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alert == null) {
			alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if (alert == null) {
				alert = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			}
		}
		return alert;
	}

	public void onDetach() {
		onStopClicked();
		super.onDetach();
	}

	void updateAlarmIfOneShot() {
		TedAlarm alarm = AlarmMaster.restoryAlarmById(getActivity(),
				mInputParam.alarmId);
		if (alarm != null && alarm.repeatMask == 0) {
			alarm.scheduled = 0;
			// it is a one shot alarm
			AlarmMaster.saveAlarm(getActivity(), alarm);
		}
	}

	public interface AlarmRingListener {
		public static final int STOP_REASON_USER_STOP = 1;
		public static final int STOP_REASON_HOLIDAY = 2;

		/**
		 * when user stop the alarm
		 */
		public void onStopAlarm(int reasonCode);
	}

}
