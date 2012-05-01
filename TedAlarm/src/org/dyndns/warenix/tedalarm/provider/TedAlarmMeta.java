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

	// URiMatcher to match client URis
	public enum PathType {
		ALL_ALARMS, SINGLE_ALARM, SCHEDULED_ALARMS
	}

	// database scheme
	public static final String DB_NAME = "tedalarm";
	public static final int DB_VERSION = 2;

	public static class TableAlarm implements BaseColumns {
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

	// projections
	// These are the Contacts rows that we will retrieve.
	public static final String[] ALL_ALARM_LIST_PROJECTION = new String[] {
			TableAlarm.COL_ID, TableAlarm.COL_DESCRIPTION };

}
