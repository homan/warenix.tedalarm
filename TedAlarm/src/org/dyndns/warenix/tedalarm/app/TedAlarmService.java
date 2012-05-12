package org.dyndns.warenix.tedalarm.app;

import org.dyndns.warenix.tedalarm.AlarmMaster;
import org.dyndns.warenix.tedalarm.TedAlarm;
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
		Uri alarmUri = intent.getData();
		if (alarmUri != null) {
			long alarmId = AlarmMaster.parseAlarmIdFromInsertUri(alarmUri);
			TedAlarm alarm = AlarmMaster.restoryAlarmById(
					getApplicationContext(), alarmId);
			if (!AlarmMaster.isTodayHoliday(getApplicationContext(), alarm)) {
				WLog.d(TAG, String.format("start alarm ring [%d]", alarmId));
				AlarmMaster.actionStartAlarmRing(getApplicationContext(),
						alarmUri);
			} else {
				WLog.d(TAG, String.format(
						"skip alarm [%d] because today is a holiday", alarmId));
			}

			updateAlarmIfOneShot(this.getBaseContext(), alarm);
		}
	}

	void updateAlarmIfOneShot(Context context, TedAlarm alarm) {
		if (alarm != null && alarm.repeatMask == 0) {
			// it is a one shot alarm
			alarm.scheduled = 0;
			AlarmMaster.saveAlarm(context, alarm);
		}
	}

}
