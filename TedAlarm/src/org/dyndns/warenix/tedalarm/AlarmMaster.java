package org.dyndns.warenix.tedalarm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.tedalarm.app.TedAlarmActivity;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class AlarmMaster {
	static final String TAG = "AlarmMaster";

	/**
	 * action to start alarm ring
	 * 
	 * @param context
	 * @param alarmUri
	 *            which alarm needs to ring
	 */
	public static void actionStartAlarmRing(Context context, Uri alarmUri) {
		Intent intent = new Intent(context, TedAlarmActivity.class);
		intent.setData(alarmUri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	/**
	 * get a list of scheduled alarm
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList<TedAlarm> getScheduledAlarm(Context context) {
		Uri empsUri = Uri.parse("content://tedalarm/"
				+ TedAlarmMeta.PATH_SCHEDULED_ALARM);
		Cursor cursor = null;
		cursor = context.getContentResolver().query(empsUri, null, null, null,
				null);
		if (cursor == null) {
			WLog.i(TAG, String.format("cursor is null"));
			return null;
		}
		WLog.d(TAG, String.format("cursor count[%d]", cursor.getCount()));
		ArrayList<TedAlarm> list = new ArrayList<TedAlarm>();
		while (cursor.moveToNext()) {
			list.add(createAlarmFromCursor(cursor));
		}
		return list;
	}

	/**
	 * rescheudle all alarms
	 * 
	 * @param context
	 */
	public static void rescheduleAllAlarms(Context context) {
		ArrayList<TedAlarm> scheduledALarmList = AlarmMaster
				.getScheduledAlarm(context);
		for (TedAlarm alarm : scheduledALarmList) {
			rescheduleAlarm(context, alarm);
		}
	}

	/**
	 * reschedule an alarm by first cancel it and then schedule it
	 * 
	 * @param context
	 * @param alarm
	 */
	public static void rescheduleAlarm(Context context, TedAlarm alarm) {
		AlarmMaster.cancelAlarm(context, alarm);
		if (alarm.scheduled == 1) {
			if (alarm.repeatMask == 0) {
				AlarmMaster.scheduleAlarmOneShot(context, alarm);
			} else {
				AlarmMaster.scheduleAlarmRepeat(context, alarm);
			}
		}
	}

	/**
	 * convert cursor to an alarm object
	 * 
	 * @param cursor
	 *            from alarm table with default projection
	 * @return
	 */
	public static TedAlarm createAlarmFromCursor(Cursor cursor) {
		TedAlarm alarm = new TedAlarm();
		alarm.id = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_ID));
		alarm.description = cursor
				.getString(cursor
						.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION));
		alarm.startTime = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_START_TIME));
		alarm.scheduled = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_SCHEDULED));
		alarm.repeatMask = cursor
				.getLong(cursor
						.getColumnIndex(TedAlarmMeta.TableAlarmColumns.COL_REPEAT_MASK));
		return alarm;
	}

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

	/**
	 * @param startTime
	 * @return the next alarm trigger time. If the time is before current, it
	 *         will return the same hour an minute on next day.
	 */
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

	/**
	 * cancel an alarm, no matter is it is scheduled or not
	 * 
	 * @param context
	 * @param alarm
	 */
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

	/**
	 * schedule an alarm to fire for one time only
	 * 
	 * @param context
	 * @param alarm
	 */
	public static void scheduleAlarmOneShot(Context context, TedAlarm alarm) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 1,
				createAlarmPendingIntent(context, alarm),
				PendingIntent.FLAG_ONE_SHOT);
		// am.setRepeating(1, triggerAtTime, 5 * 1000, operation);
		long triggerAtTime = convertAlarmTime(alarm.startTime);
		am.set(1, triggerAtTime, operation);
		WLog.d(TAG, String.format("set one shot alam, next fire at [%s]",
				new Date(triggerAtTime).toLocaleString()));
	}

	/**
	 * schedule an alarm to fire repeatedly. The interval is defined in the
	 * alarm.
	 * 
	 * @param context
	 * @param alarm
	 */
	public static void scheduleAlarmRepeat(Context context, TedAlarm alarm) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 1,
				createAlarmPendingIntent(context, alarm),
				PendingIntent.FLAG_CANCEL_CURRENT);
		long triggerAtTime = convertAlarmTime(alarm.startTime);
		am.setRepeating(1, triggerAtTime, alarm.repeatMask, operation);
		WLog.d(TAG, String.format(
				"set repeat alarm, next fire at [%s] interval[%d]", new Date(
						triggerAtTime).toLocaleString(), alarm.repeatMask));
	}

	/**
	 * Create an intent to alarm ring receiver
	 * 
	 * @param context
	 * @param alarm
	 * @return
	 */
	public static Intent createAlarmPendingIntent(Context context,
			TedAlarm alarm) {
		Intent intent = new Intent(context, AlarmRingReceiver.class);
		intent.setData(convertAlarmToUri(alarm));

		return intent;
	}

	/**
	 * get calendar ids from holiday
	 * 
	 * @param context
	 * @param alarm
	 * @return
	 */
	public static ArrayList<String> getCalendarIdOfAlarm(Context context,
			long alarmId) {
		Uri empsUri = Uri.parse("content://tedalarm/holidays/" + alarmId);
		Cursor cursor = context.getContentResolver().query(empsUri, null, null,
				null, null);
		if (cursor != null) {
			ArrayList<String> calendarIdList = new ArrayList<String>();
			while (cursor.moveToNext()) {
				String calendarId = cursor
						.getString(cursor
								.getColumnIndex(TedAlarmMeta.TableHolidayColumns.COL_CALENDAR_ID));
				calendarIdList.add(calendarId);
			}

			return calendarIdList;
		}
		return null;
	}

	public static void addAllAlarmHoliday(Context context, TedAlarm alarm) {
		if (alarm.holidayList != null) {
			Uri empsUri = Uri.parse("content://tedalarm/holidays/" + alarm.id);
			ContentValues cvs;

			for (CalendarListItem calendar : alarm.holidayList) {
				cvs = new ContentValues();
				cvs.put(TedAlarmMeta.TableHolidayColumns.COL_ALARM_ID, alarm.id);
				cvs.put(TedAlarmMeta.TableHolidayColumns.COL_CALENDAR_ID,
						calendar.id);
				Uri newUri = context.getContentResolver().insert(empsUri, cvs);
			}
		}
	}

	public static void removeAllAlarmHoliday(Context context, TedAlarm alarm) {
		if (alarm.holidayList != null) {
			Uri empsUri = Uri.parse("content://tedalarm/holidays/" + alarm.id);
			ContentValues cvs;

			cvs = new ContentValues();
			int rowsNumber = context.getContentResolver().delete(empsUri, null,
					null);
			WLog.i(TAG, String.format("test delete row[%d]", rowsNumber));
		}
	}
}
