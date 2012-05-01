package org.dyndns.warenix.tedalarm;

import java.util.Calendar;
import java.util.Date;

import org.dyndns.warenix.util.WLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * common methods for alarms
 * 
 * @author warenix
 * 
 */
public class AlarmUtils {
	static final String TAG = "AlarmMaster";

	/**
	 * calculate the next alarm trigger time from given start time.
	 * 
	 * @param hour
	 * @param minute
	 * @return If start time is before current time, it will return the start
	 *         time of next day
	 */
	public static long convertAlarmTime(int hour, int minute) {
		long triggerAtTime = 0;
		Date currentDate = new Date();
		Date alarmDate = (Date) currentDate.clone();
		alarmDate.setHours(hour);
		alarmDate.setMinutes(minute);
		alarmDate.setSeconds(0);
		if (alarmDate.before(currentDate)) {
			// advance to next day
			Calendar c = Calendar.getInstance();
			c.setTime(alarmDate);

			c.add(Calendar.DATE, 1); // number of days to add
			triggerAtTime = c.getTimeInMillis();
		} else {
			triggerAtTime = alarmDate.getTime();
		}
		return triggerAtTime;
	}

	public static long convertAlarmTime(long startTime) {
		long triggerAtTime = 0;
		Date currentDate = new Date();
		Date alarmDate = new Date(startTime);
		if (alarmDate.before(currentDate)) {
			// advance to next day
			Calendar c = Calendar.getInstance();
			c.setTime(alarmDate);

			c.add(Calendar.DATE, 1); // number of days to add
			triggerAtTime = c.getTimeInMillis();
		} else {
			triggerAtTime = alarmDate.getTime();
		}
		return triggerAtTime;
	}

	/**
	 * Uri to the alarm
	 * 
	 * @param alarm
	 * @return
	 */
	public static Uri convertAlarmToUri(TedAlarm alarm) {
		Uri empsUri = Uri.parse(String
				.format("content://tedalarm/%d", alarm.id));
		return empsUri;
	}

	public static void cancelAlarm(Context context, TedAlarm alarm) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 1,
				createAlarmPendingIntent(context, alarm),
				PendingIntent.FLAG_NO_CREATE);
		// am.setRepeating(1, triggerAtTime, 5 * 1000, operation);
		am.cancel(operation);
		WLog.d(TAG, String.format("cancelled alarm [%s]", alarm.toString()));
	}

	public static void setAlarmOneShot(Context context, TedAlarm alarm) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 1,
				createAlarmPendingIntent(context, alarm),
				PendingIntent.FLAG_ONE_SHOT);
		// am.setRepeating(1, triggerAtTime, 5 * 1000, operation);
		long triggerAtTime = AlarmUtils.convertAlarmTime(alarm.startTime);
		am.set(1, triggerAtTime, operation);
		WLog.d(TAG, String.format("set one shot alam, next fire at [%s]",
				new Date(triggerAtTime).toLocaleString()));
	}

	public static void setAlarmRepeat(Context context, TedAlarm alarm) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 1,
				createAlarmPendingIntent(context, alarm),
				PendingIntent.FLAG_UPDATE_CURRENT);
		long triggerAtTime = AlarmUtils.convertAlarmTime(alarm.startTime);
		am.setRepeating(1, triggerAtTime, alarm.repeatMask, operation);
		WLog.d(TAG, String.format(
				"set repeat alarm, next fire at [%s] interval[%d]", new Date(
						triggerAtTime).toLocaleString(), alarm.repeatMask));
	}

	/**
	 * Create an intent to alarm receiver
	 * 
	 * @param context
	 * @param alarm
	 * @return
	 */
	public static Intent createAlarmPendingIntent(Context context,
			TedAlarm alarm) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.setData(AlarmUtils.convertAlarmToUri(alarm));

		return intent;
	}

}
