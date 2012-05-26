package org.dyndns.warenix.com.google.calendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.EventList.EventListItem;
import org.dyndns.warenix.google.calendar.provider.GoogleCalendarMeta;
import org.dyndns.warenix.net.util.WebContent;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.api.GoogleOAuthAccessToken;

public class GoogleCalendarMaster {
	static final String TAG = "GoogleCalendarMaster";

	/**
	 * get a list of all my calendars
	 * 
	 * @param accessToken
	 * @return
	 */
	public static ArrayList<CalendarListItem> getAllCalendar(
			GoogleOAuthAccessToken accessToken) {
		String url = String
				.format("https://www.googleapis.com/calendar/v3/users/me/calendarList?&output=json&ck=1255643091105&client=scroll&oauth_token=%s",
						accessToken.accessToken);
		Log.d(TAG, "url:" + url);
		try {
			String responseJsonString = WebContent.httpGet(url);
			Log.d(TAG, responseJsonString);

			return CalendarList.factory(responseJsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * get a list of future events from now to given days later in a particular
	 * calendar
	 * 
	 * @param accessToken
	 * @param calendarId
	 * @param day
	 * @return
	 */
	public static ArrayList<EventListItem> getFutureEvent(
			GoogleOAuthAccessToken accessToken, String calendarId, int day) {
		// RFC 3339 timestamp format
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd'T'hh:m:ss'Z'");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		String timeMin = format.format(new Date());

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, day); // number of days to add
		Date maxDate = new Date(c.getTimeInMillis());
		String timeMax = format.format(maxDate);

		// String calendarId =
		// "en.hong_kong%23holiday%40group.v.calendar.google.com";
		String url = String
				.format("https://www.googleapis.com/calendar/v3/calendars/%s/events?&output=json&ck=1255643091105&client=scroll&oauth_token=%s&timeMin=%s&timeMax=%s",
						calendarId, accessToken.accessToken, timeMin, timeMax);
		Log.d(TAG, "url:" + url);
		try {
			String responseJsonString = WebContent.httpGet(url);
			Log.d(TAG, responseJsonString);

			return EventList.factory(responseJsonString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * get a list of calender
	 * 
	 * @param context
	 * @return null when there's no stored calendar
	 */
	public static ArrayList<CalendarListItem> getCalendarList(Context context) {
		Uri empsUri = Uri.parse("content://googlecalendar/"
				+ GoogleCalendarMeta.PATH_ALL_CALENDAR);
		Cursor cursor = context.getContentResolver().query(empsUri, null, null,
				null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				ArrayList<CalendarListItem> items = new ArrayList<CalendarListItem>();
				while (cursor.moveToNext()) {
					CalendarListItem calendar = createCalendarFromCursor(cursor);
					items.add(calendar);
				}
				return items;
			}
		}
		return null;
	}

	static CalendarListItem createCalendarFromCursor(Cursor cursor) {
		CalendarListItem calendar = new CalendarListItem();
		calendar.kind = cursor.getString(cursor
				.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_KIND));
		calendar.id = cursor.getString(cursor
				.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_ID));
		calendar.etag = cursor.getString(cursor
				.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_ETAG));

		calendar.summary = cursor
				.getString(cursor
						.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_SUMMARY));
		calendar.description = cursor
				.getString(cursor
						.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_DESCRIPTION));
		calendar.colorId = cursor
				.getString(cursor
						.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_COLOR_ID));
		calendar.selected = cursor
				.getString(cursor
						.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_SELECTED));
		calendar.accessRole = cursor
				.getString(cursor
						.getColumnIndex(GoogleCalendarMeta.TableAlarmColumns.COL_ACCESS_ROLE));

		return calendar;
	}
}
