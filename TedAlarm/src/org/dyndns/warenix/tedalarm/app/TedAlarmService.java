package org.dyndns.warenix.tedalarm.app;

import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.TedAlarm;
import org.dyndns.warenix.tedalarm.TedAlarmIntent;
import org.dyndns.warenix.tedalarm.ui.SyncFragment;
import org.dyndns.warenix.util.WLog;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * When an alarm is received, it will be checked whether it is the right time to
 * start ringing. D
 * 
 * @author warenix
 * 
 */
public class TedAlarmService extends IntentService {
	private static final String TAG = "TedAlarmService";

	public TedAlarmService() {
		this("TedAlarmService");
	}

	public TedAlarmService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();

		if (TedAlarmIntent.ACTION_RING_ALARM.equals(action)) {
			handleAlarmRing(intent);
		} else if (TedAlarmIntent.ACTION_SYNC_CALENDAR.equals(action)) {
			handleSyncCalendar(intent);
		}
	}

	void handleAlarmRing(Intent intent) {
		Uri alarmUri = intent.getData();
		if (alarmUri != null) {
			TedAlarm alarm = AlarmMaster.restoryAlarmByUri(
					getApplicationContext(), alarmUri);
			if (alarm == null) {
				WLog.d(TAG, String.format("cannot restore alarm url [%s]",
						alarmUri));
			} else {
				boolean ruleSingleShotAlarm = alarm.repeatMask == 0L;
				boolean ruleRepeatAlarmOnToday = AlarmMaster
						.isAlarmRingOnToday(alarm);
				boolean ruleRingOnNonHoliday = !AlarmMaster.isTodayHoliday(
						getApplicationContext(), alarm);

				if (ruleSingleShotAlarm
						|| (ruleRepeatAlarmOnToday && ruleRingOnNonHoliday)) {
					WLog.d(TAG,
							String.format("start alarm ring [%s]", alarmUri));
					AlarmMaster.actionStartAlarmRing(getApplicationContext(),
							alarmUri);
				} else {
					WLog.d(TAG, String.format("skip alarm [%s]", alarmUri));
				}

				updateAlarmIfOneShot(this.getBaseContext(), alarm);
			}
		} else {
			WLog.d(TAG, String.format("no alarm?"));
		}
	}

	void handleSyncCalendar(Intent intent) {
		WLog.i(TAG, String.format("received action to sync calendar"));
		SyncFragment.newInstance().onSync(getApplicationContext(),
				intent.getBooleanExtra("doInBackground", true));
	}

	void updateAlarmIfOneShot(Context context, TedAlarm alarm) {
		if (alarm != null && alarm.repeatMask == 0) {
			// it is a one shot alarm
			alarm.scheduled = 0;
			AlarmMaster.saveAlarm(context, alarm);
		}
	}

}
