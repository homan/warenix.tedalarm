package org.dyndns.warenix.tedalarm.ui;

import java.io.IOException;
import java.util.Date;

import org.dyndns.warenix.tedalarm.R;
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
import android.widget.EditText;
import android.widget.TimePicker;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class AlarmRingFragment extends SherlockFragment implements
		OnClickListener {
	private static final String TAG = "AlarmRingFragment";

	public static final String BUNDLE_ALARM_ID = "alarm_id";

	EditText mDescription;
	TimePicker mStartTime;
	Button mStop;

	MediaPlayer mMediaPlayer;

	private static class InputParam {
		long alarmId;
	}

	protected InputParam mInputParam = new InputParam();

	public static AlarmRingFragment newInstance(Bundle args) {
		AlarmRingFragment f = new AlarmRingFragment();
		f.setArguments(args);
		return f;
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
		return inflater.inflate(R.layout.alarm_ring, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		initView(getView());
		bindView();
		playSound(getActivity(), getAlarmUri());
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
			return true;
		case R.id.menu_delete:
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	void readInputBundle(Bundle args) {
		if (args != null) {
			mInputParam.alarmId = args.getLong(BUNDLE_ALARM_ID);
		}
	}

	void initView(View view) {
		mDescription = (EditText) view.findViewById(R.id.description);
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
		} else {
			WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		}

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

}
