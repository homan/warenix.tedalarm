package org.dyndns.warenix.tedalarm.ui;

import java.io.IOException;
import java.util.Date;

import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.R;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.app.Activity;
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
import android.view.Window;
import android.view.WindowManager;
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

	Object mVolumeLock = new Object();

	TedAlarm mAlarm;
	AlarmRingListener mListener;

	static final long sAdjustVolumeTimeMS = 2000;
	static final float sAlarmVolume[] = { 0.05f, 0.1f, 0.15f, 0.2f, 0.6f, 0.8f,
			1.0f };
	Thread mAlarmVolumeAdjsuter;

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

		unlockScreen();

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

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		initView(getView());
		bindView();
		playSound(getActivity(), getAlarmUri());
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
			WLog.d(TAG, String.format("user clicked stop alarm"));
			onStopClicked();
			break;
		}
	}

	void onStopClicked() {
		if (mAlarmVolumeAdjsuter != null) {
			mAlarmVolumeAdjsuter.interrupt();
			mAlarmVolumeAdjsuter = null;
		}

		WLog.d(TAG, String.format("stop ringing"));
		if (mMediaPlayer != null) {
			synchronized (mVolumeLock) {
				mMediaPlayer.stop();
				mMediaPlayer = null;
			}
		}

		removeUnlockScreen();

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
				float volume = sAlarmVolume[0];
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setVolume(volume, volume);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
				WLog.d(TAG, String.format("start ringing now"));

				if (mAlarmVolumeAdjsuter == null) {
					mAlarmVolumeAdjsuter = new Thread() {
						public void run() {
							int count = 0;
							float volume;
							while (count < sAlarmVolume.length) {
								try {
									Thread.sleep(sAdjustVolumeTimeMS);
									volume = sAlarmVolume[count++];
									synchronized (mVolumeLock) {
										if (mMediaPlayer != null) {
											mMediaPlayer.setVolume(volume,
													volume);
											WLog.d(TAG, String.format(
													"adjust volume to [%f]",
													volume));
										} else {
											break;
										}
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
									break;
								}

							}
							WLog.i(TAG,
									String.format("stop adjust alarm volume"));
						}
					};
					mAlarmVolumeAdjsuter.start();
					WLog.i(TAG, String.format("start adjust alarm volume"));
				}
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

	public void onAttach(Activity activity) {
		WLog.d(TAG, String.format("onAttach"));
		super.onAttach(activity);
	}

	public void onDestroyView() {
		WLog.i(TAG, String.format("exiting, stop alarm if needed"));
		onStopClicked();
		super.onDestroyView();
	}

	public void onDetach() {
		WLog.d(TAG, String.format("onDetach"));
		super.onDetach();
	}

	public interface AlarmRingListener {
		public static final int STOP_REASON_USER_STOP = 1;
		public static final int STOP_REASON_HOLIDAY = 2;

		/**
		 * when user stop the alarm
		 */
		public void onStopAlarm(int reasonCode);
	}

	private void unlockScreen() {
		WLog.d(TAG, String.format("unlock screen"));
		final Window win = getActivity().getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	}

	private void removeUnlockScreen() {
		WLog.d(TAG, String.format("remove unlock screen flags"));
		final Window win = getActivity().getWindow();
		win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
	}

}
