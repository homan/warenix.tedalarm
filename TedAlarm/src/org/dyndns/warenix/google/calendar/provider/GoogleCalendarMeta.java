package org.dyndns.warenix.google.calendar.provider;

import android.provider.BaseColumns;

public class GoogleCalendarMeta {
	// authority and paths
	public static final String AUTHORITY = "googlecalendar";

	/**
	 * retrieve single alarm
	 */
	public static final String PATH_ALL_CALENDAR = "calendars";
	public static final String PATH_ALL_DAY_EVENT = "all_day_event";
	public static final String PATH_SINGLE_ALL_DAY_EVENT = "all_day_event/*";
	public static final String PATH_HAVE_ALL_DAY_EVENT = "have_all_day_event/*";

	// URiMatcher to match client URis
	public enum PathType {
		ALL_CALENDARS, SINGLE_CALENDAR, ALL_DAY_EVENT, SINGLE_ALL_DAY_EVENT, HAVE_ALL_DAY_EVENT,
	}

	// database scheme
	public static final String DB_NAME = "googlecalendar";
	public static final int DB_VERSION = 4;

	public static class TableAlarmColumns implements BaseColumns {
		public static final String TABLE_NAME = "calendar";

		public static final String COL__ID = "_id";
		public static final String COL_ID = "id";
		public static final String COL_KIND = "kind";
		public static final String COL_ETAG = "etag";
		public static final String COL_SUMMARY = "summary";
		public static final String COL_DESCRIPTION = "description";
		public static final String COL_COLOR_ID = "colorId";
		public static final String COL_SELECTED = "selected";
		public static final String COL_ACCESS_ROLE = "accessRole";
		public static final String COL_START_TIME = "startTime";
		public static final String COL_END_TIME = "endTime";
		public static final String COL_CALENDAR_ID = "calendar_id";

		public static String SQL_CREATE = "CREATE TABLE " + //
				TABLE_NAME + //
				" (" + //
				COL__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + //
				COL_ID + " TEXT, " + //
				COL_KIND + " TEXT, " + //
				COL_ETAG + " TEXT, " + //
				COL_SUMMARY + " TEXT, " + //
				COL_DESCRIPTION + " TEXT, " + //
				COL_COLOR_ID + " TEXT, " + //
				COL_SELECTED + " TEXT, " + //
				COL_ACCESS_ROLE + " TEXT, " + //
				COL_START_TIME + " INTEGER, " + //
				COL_END_TIME + " INTEGER, " + //
				COL_CALENDAR_ID + " TEXT, " + //
				"UNIQUE (" + COL_ID + ") ON CONFLICT REPLACE "//
				+ " )";
	}

	// projections
	/**
	 * projection for displaying alarm in a list
	 */
	public static final String[] ALL_CALENDAR_LIST_PROJECTION = new String[] {
			TableAlarmColumns.COL_ID, TableAlarmColumns.COL_DESCRIPTION };
}
