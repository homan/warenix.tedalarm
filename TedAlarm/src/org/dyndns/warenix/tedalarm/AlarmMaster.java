package org.dyndns.warenix.tedalarm;

import java.util.ArrayList;

import org.dyndns.warenix.tedalarm.app.TedAlarmActivity;
import org.dyndns.warenix.tedalarm.provider.TedAlarmMeta;
import org.dyndns.warenix.util.WLog;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class AlarmMaster {
	static final String TAG = "AlarmMaster";

	public static void startAlarmActivity(Context context, Uri alarmUri) {
		Intent intent = new Intent(context, TedAlarmActivity.class);
		intent.setData(alarmUri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	public static ArrayList<TedAlarm> getScheduledAlarm(Context context) {
		Uri empsUri = Uri.parse("content://tedalarm/"
				+ TedAlarmMeta.PATH_SCHEDULED_ALARM);
		Cursor cursor = null;
		ContentValues cvs = null;

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

	public static TedAlarm createAlarmFromCursor(Cursor cursor) {
		TedAlarm alarm = new TedAlarm();
		alarm.id = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarm.COL_ID));
		alarm.description = cursor.getString(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarm.COL_DESCRIPTION));
		alarm.startTime = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarm.COL_START_TIME));
		alarm.scheduled = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarm.COL_SCHEDULED));
		alarm.repeatMask = cursor.getLong(cursor
				.getColumnIndex(TedAlarmMeta.TableAlarm.COL_REPEAT_MASK));
		return alarm;
	}
}
