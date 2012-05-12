package org.dyndns.warenix.tedalarm.provider;

import android.provider.BaseColumns;

/**
 * Meta data for TedAlarmProvider. Stores all constants.
 * 
 * @author warenix
 * 
 */
public class TedAlarmMeta {
	// authority and paths
	public static final String AUTHORITY = "tedalarm";
	/**
	 * retrieve single alarm
	 */
	public static final String PATH_ALL_ALARM = null;
	/**
	 * retrieve all alarms
	 */
	public static final String PATH_SINGLE_ALARM = "#";
	/**
	 * retrieve all scheduled alarms
	 */
	public static final String PATH_SCHEDULED_ALARM = "scheduled";

	/**
	 * get all holidays for an alarm id
	 */
	public static final String PATH_ALL_HOLIDAYS = "holidays/#";

	// URiMatcher to match client URis
	public enum PathType {
		ALL_ALARMS, SINGLE_ALARM, SCHEDULED_ALARMS, //
		ALL_HOLIDAYS
	}

	// database scheme
	public static final String DB_NAME = "tedalarm";
	public static final int DB_VERSION = 3;

	public static class TableAlarmColumns implements BaseColumns {
		public static final String TABLE_NAME = "alarm";

		public static final String COL_ID = "_id";
		public static final String COL_DESCRIPTION = "description";
		public static final String COL_START_TIME = "start_time";
		public static final String COL_SCHEDULED = "scheduled";
		public static final String COL_REPEAT_MASK = "repeat_mask";

		public static String SQL_CREATE = "CREATE TABLE " + //
				TABLE_NAME + //
				" (" + //
				COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + //
				COL_DESCRIPTION + " TEXT, " + //
				COL_START_TIME + " INTEGER, " + //
				COL_SCHEDULED + " INTEGER, " + //
				COL_REPEAT_MASK + " INTEGER " + //
				" )";
	}

	public static class TableHolidayColumns implements BaseColumns {
		public static final String TABLE_NAME = "holiday";

		public static final String COL__ID = "_id";
		public static final String COL_CALENDAR_ID = "calendar_id";
		public static final String COL_ALARM_ID = "alarm_id";

		public static String SQL_CREATE = "CREATE TABLE "
				+ //
				TABLE_NAME
				+ //
				" ("
				+ //
				COL__ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ //
				COL_CALENDAR_ID + " TEXT, "
				+ //
				COL_ALARM_ID + " INTEGER,  "
				+ //
				"UNIQUE (" + COL_CALENDAR_ID + ", " + COL_ALARM_ID
				+ ") ON CONFLICT REPLACE " //
				+ " )";
	}

	// projections
	/**
	 * projection for displaying alarm in a list
	 */
	public static final String[] ALL_ALARM_LIST_PROJECTION = new String[] {
			TableAlarmColumns.COL_ID, TableAlarmColumns.COL_DESCRIPTION,
			TableAlarmColumns.COL_SCHEDULED };

}
