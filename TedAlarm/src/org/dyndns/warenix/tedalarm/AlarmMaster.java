package org.dyndns.warenix.tedalarm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_HISTORY);
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
				WLog.d(TAG,
						String.format("schedule one shot:%s", alarm.toString()));
				AlarmMaster.scheduleAlarmOneShot(context, alarm);
			} else {
				WLog.d(TAG, String.format("schedule repeating:%s",
						alarm.toString()));
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
	 * encode hour:minute into a 4 digits number
	 * 
	 * @param hour
	 * @param minute
	 * @return a number representing alarm time
	 */
	public static long convertAlarmTime(int hour, int minute) {
		// long triggerAtTime = 0;
		// Date currentDate = new Date();
		// Date alarmDate = (Date) currentDate.clone();
		// alarmDate.setHours(hour);
		// alarmDate.setMinutes(minute);
		// alarmDate.setSeconds(0);
		// if (alarmDate.before(currentDate)) {
		// // advance to next day
		// Calendar c = Calendar.getInstance();
		// c.setTime(alarmDate);
		//
		// c.add(Calendar.DATE, 1); // number of days to add
		// triggerAtTime = c.getTimeInMillis();
		// } else {
		// triggerAtTime = alarmDate.getTime();
		// }
		// return triggerAtTime;
		return hour * 100 + minute;
	}

	public static String formatAlarmTime(long startTime) {
		Date d = convertAlarmTimeToDate(startTime);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		return sdf.format(d);
	}

	/**
	 * @param startTime
	 * @return the next alarm trigger time. If the time is before current, it
	 *         will return the same hour an minute on next day.
	 */
	public static long calculateNextTriggerTime(long startTime) {
		long triggerAtTime = 0;
		Date currentDate = new Date();
		// Date alarmDate = new Date();
		// alarmDate.setHours((int) startTime / 100);
		// alarmDate.setMinutes((int) startTime % 100);
		Date alarmDate = convertAlarmTimeToDate(startTime);
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

	public static Date convertAlarmTimeToDate(long startTime) {
		Date d = new Date();
		d.setHours((int) startTime / 100);
		d.setMinutes((int) startTime % 100);
		d.setSeconds(0);
		return d;
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
		long triggerAtTime = calculateNextTriggerTime(alarm.startTime);
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
		long triggerAtTime = calculateNextTriggerTime(alarm.startTime);
		am.setRepeating(1, triggerAtTime, AlarmManager.INTERVAL_DAY, operation);
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
		intent.setAction(TedAlarmIntent.ACTION_RING_ALARM);
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

	/**
	 * store chosen holiday calendars of an alarm
	 * 
	 * @param context
	 * @param alarm
	 */
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

	/**
	 * remove stored hoiday calendars of an alarm
	 * 
	 * @param context
	 * @param alarm
	 */
	public static void removeAllAlarmHoliday(Context context, TedAlarm alarm) {
		if (alarm.holidayList != null) {
			Uri empsUri = Uri.parse("content://tedalarm/holidays/" + alarm.id);
			int rowsAffected = context.getContentResolver().delete(empsUri,
					null, null);
			WLog.i(TAG, String.format("test delete row[%d]", rowsAffected));
		}
	}

	/**
	 * restore alarm by id in uri
	 * 
	 * @param context
	 * @param id
	 * @return
	 */
	public static TedAlarm restoryAlarmById(Context context, long id) {
		Uri empsUri = Uri.parse(String.format("content://tedalarm/%d", id));
		return restoryAlarmByUri(context, empsUri);
	}

	/**
	 * restore alarm by uri
	 * 
	 * @param context
	 * @param empsUri
	 * @return
	 */
	public static TedAlarm restoryAlarmByUri(Context context, Uri empsUri) {
		Cursor cursor = null;
		cursor = context.getContentResolver().query(empsUri, null, null, null,
				null);
		if (cursor == null) {
			WLog.i(TAG, String.format("cursor is null"));
			return null;
		}

		if (cursor.moveToFirst()) {
			return createAlarmFromCursor(cursor);
		}
		return null;
	}

	/**
	 * insert a new or update an existing alarm, determined by alarm is having
	 * alarm.id or not.
	 * 
	 * @param context
	 * @param alarm
	 * @return true if saved
	 */
	public static TedAlarm saveAlarm(Context context, TedAlarm alarm) {
		if (alarm.id != 0) {
			// update
			if (updateAlarm(context, alarm)) {
				return alarm;
			}
		} else {
			return insertAlarm(context, alarm);
		}
		return null;
	}

	/**
	 * update an existing alarm to database.
	 * 
	 * @param context
	 * @param alarm
	 * @return whether any row is updated
	 */
	public static boolean updateAlarm(Context context, TedAlarm alarm) {
		ContentValues cvs = prepareContentValuesFromAlarm(alarm);
		Uri updateUri = Uri.parse(String.format("content://tedalarm/%d",
				alarm.id));

		int rowsNumber = context.getContentResolver().update(updateUri, cvs,
				null, null);
		return rowsNumber > 0;
	}

	/**
	 * insert an alarm to database.
	 * 
	 * @param context
	 * @param alarm
	 * @return alarm object with id filled. null if failed.
	 */
	public static TedAlarm insertAlarm(Context context, TedAlarm alarm) {
		ContentValues cvs = prepareContentValuesFromAlarm(alarm);

		Uri insertUri = Uri.parse("content://tedalarm");
		Uri newUri = context.getContentResolver().insert(insertUri, cvs);
		WLog.i(TAG, String.format("inserted alarm to url[%s]", newUri));

		long alarmId = parseAlarmIdFromInsertUri(newUri);
		if (alarmId != -1) {
			alarm.id = alarmId;
			return alarm;
		}
		return null;
	}

	public static boolean deleteAlarm(Context context, TedAlarm alarm) {
		Uri deleteUri = Uri.parse(String.format("content://tedalarm/%d",
				alarm.id));
		int rowsAffected = context.getContentResolver().delete(deleteUri, null,
				null);
		return rowsAffected > 0;
	}

	/**
	 * prepare for database insert/update from a given alarm
	 * 
	 * @param alarm
	 * @return
	 */
	public static ContentValues prepareContentValuesFromAlarm(TedAlarm alarm) {
		ContentValues cvs;

		cvs = new ContentValues();
		if (alarm.id != 0) {
			cvs.put(TedAlarmMeta.TableAlarmColumns.COL_ID, alarm.id);
		}
		cvs.put(TedAlarmMeta.TableAlarmColumns.COL_DESCRIPTION,
				alarm.description);
		cvs.put(TedAlarmMeta.TableAlarmColumns.COL_REPEAT_MASK,
				alarm.repeatMask);
		cvs.put(TedAlarmMeta.TableAlarmColumns.COL_SCHEDULED, alarm.scheduled);
		cvs.put(TedAlarmMeta.TableAlarmColumns.COL_START_TIME, alarm.startTime);
		return cvs;
	}

	public static long parseAlarmIdFromInsertUri(Uri insertUri) {
		List<String> segments = insertUri.getPathSegments();
		long alarmId = Long.parseLong(segments.get(0));
		return alarmId;
	}

	/**
	 * checked is there any happening events right now.
	 * 
	 * @param context
	 * @param calendarId
	 * @return true if there is happening events right now.
	 */
	public static boolean hasHappeningEvent(Context context, String calendarId) {
		Uri empsUri = Uri.parse("content://googlecalendar/have_all_day_event/"
				+ calendarId);
		Cursor cursor = context.getContentResolver().query(empsUri, null, null,
				null, null);
		if (cursor != null) {
			return cursor.getCount() > 0;
		}
		return false;
	}

	/**
	 * check today is holiday as chosen by the alarm
	 * 
	 * @param alarm
	 * @return
	 */
	public static boolean isTodayHoliday(Context context, TedAlarm alarm) {
		if (alarm != null) {
			// TODO implement check calendars event
			ArrayList<String> calendarIdList = getCalendarIdOfAlarm(context,
					alarm.id);
			for (String calendarId : calendarIdList) {
				if (hasHappeningEvent(context, calendarId)) {
					WLog.d(TAG, String.format(
							"found an event in calender [%s]", calendarId));
					return true;
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * check if the alarm is scheduled to ring today by looking at the repeat
	 * mask.
	 * 
	 * @param alarm
	 * @return
	 */
	public static boolean isAlarmRingOnToday(TedAlarm alarm) {
		long repeatMask = alarm.repeatMask;
		if (repeatMask != 0L) {
			Calendar calendar = Calendar.getInstance();
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			int dayFlag = 1 << dayOfWeek;
			return (dayFlag & repeatMask) != 0;
		}
		// not a regular alarm
		return false;
	}

	public static void cancelSyncCalender(Context context) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 2,
				createSyncCalendarPendingIntent(context),
				PendingIntent.FLAG_NO_CREATE);
		am.cancel(operation);
		WLog.d(TAG, String.format("cancelled SyncCalendar"));
	}

	/**
	 * schedule sync google calendar daily
	 * 
	 * @param context
	 */
	public static void scheduleSyncCalendar(Context context) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		PendingIntent operation = PendingIntent.getBroadcast(context, 2,
				createSyncCalendarPendingIntent(context),
				PendingIntent.FLAG_CANCEL_CURRENT);
		long triggerAtTime = calculateNextTriggerTime(convertAlarmTime(00, 00));

		am.setRepeating(1, triggerAtTime, AlarmManager.INTERVAL_DAY, operation);
		WLog.d(TAG, String.format("schedule sysnc calendar, next fire at [%s]",
				new Date(triggerAtTime).toLocaleString()));
	}

	public static Intent createSyncCalendarPendingIntent(Context context) {
		Intent intent = new Intent(context, AlarmRingReceiver.class);
		intent.setAction(TedAlarmIntent.ACTION_SYNC_CALENDAR);
		return intent;
	}
}
