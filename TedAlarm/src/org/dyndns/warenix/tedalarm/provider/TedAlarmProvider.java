package org.dyndns.warenix.tedalarm.provider;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * db access to alarm.
 * 
 * @author warenix
 * 
 */
public class TedAlarmProvider extends ContentProvider {
	public static final Uri CONTENT_URI = Uri.parse(String.format(
			"content://%s", TedAlarmMeta.AUTHORITY));

	static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		matcher.addURI(TedAlarmMeta.AUTHORITY, TedAlarmMeta.PATH_ALL_ALARM,
				TedAlarmMeta.PathType.ALL_ALARMS.ordinal());
		matcher.addURI(TedAlarmMeta.AUTHORITY, TedAlarmMeta.PATH_SINGLE_ALARM,
				TedAlarmMeta.PathType.SINGLE_ALARM.ordinal());
		matcher.addURI(TedAlarmMeta.AUTHORITY,
				TedAlarmMeta.PATH_SCHEDULED_ALARM,
				TedAlarmMeta.PathType.SCHEDULED_ALARMS.ordinal());
		matcher.addURI(TedAlarmMeta.AUTHORITY, TedAlarmMeta.PATH_ALL_HOLIDAYS,
				TedAlarmMeta.PathType.ALL_HOLIDAYS.ordinal());
	}

	protected DatabaseHelper db;

	@Override
	public int delete(Uri uri, String where, String[] args) {
		TedAlarmMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType == null) {
			return 0;
		}

		int rows = 0;
		SQLiteDatabase database = null;
		switch (pathType) {
		case SINGLE_ALARM:
			List<?> segments = uri.getPathSegments();
			String id = (String) segments.get(0);
			where = TedAlarmMeta.TableAlarmColumns.COL_ID + "=?";
			args = new String[] { id };
		case ALL_ALARMS:
			database = db.getWritableDatabase();
			rows = database.delete(TedAlarmMeta.TableAlarmColumns.TABLE_NAME,
					where, args);
			break;
		case ALL_HOLIDAYS:
			segments = uri.getPathSegments();
			id = (String) segments.get(1);
			where = TedAlarmMeta.TableHolidayColumns.COL_ALARM_ID + "=?";
			args = new String[] { id };
			database = db.getWritableDatabase();
			rows = database.delete(TedAlarmMeta.TableHolidayColumns.TABLE_NAME,
					where, args);
		}

		return rows;
	}

	@Override
	public String getType(Uri uri) {
		TedAlarmMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType != null) {
			switch (pathType) {
			case SINGLE_ALARM:
				return "tedalarm";
			}
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (values == null) {
			throw new IllegalArgumentException("ContentValues is null "
					+ uri.toString());
		}

		// not the Uri we're expecting
		long newID = 0;
		TedAlarmMeta.PathType pathType = matchUriToPathType(uri);

		if (pathType == TedAlarmMeta.PathType.ALL_ALARMS) {
			newID = db.getWritableDatabase().insert(
					TedAlarmMeta.TableAlarmColumns.TABLE_NAME, null, values);
			return Uri.withAppendedPath(uri, String.valueOf(newID));
		} else if (pathType == TedAlarmMeta.PathType.ALL_HOLIDAYS) {
			newID = db.getWritableDatabase().insert(
					TedAlarmMeta.TableHolidayColumns.TABLE_NAME, null, values);
			return Uri.withAppendedPath(uri, String.valueOf(newID));
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		db = new DatabaseHelper(this.getContext());
		return db != null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		builder.setTables(TedAlarmMeta.TableAlarmColumns.TABLE_NAME);

		String order = null;
		Cursor result = null;
		if (sortOrder != null)
			order = sortOrder;

		int match = matcher.match(uri);
		TedAlarmMeta.PathType pathType = TedAlarmMeta.PathType.class
				.getEnumConstants()[match];
		switch (pathType) {
		case SINGLE_ALARM:
			List<?> segments = uri.getPathSegments();
			String id = (String) segments.get(0);
			result = builder.query(db.getReadableDatabase(), projection,
					TedAlarmMeta.TableAlarmColumns.COL_ID + "=?",
					new String[] { id }, null, null, order);
			break;
		case SCHEDULED_ALARMS:
			result = builder.query(db.getReadableDatabase(), projection,
					TedAlarmMeta.TableAlarmColumns.COL_SCHEDULED + "=?",
					new String[] { "1" }, null, null, order);
			break;
		case ALL_ALARMS:
			result = builder.query(db.getReadableDatabase(), projection,
					selection, selectionArgs, null, null, order);
			break;
		case ALL_HOLIDAYS:
			builder.setTables(TedAlarmMeta.TableHolidayColumns.TABLE_NAME);
			segments = uri.getPathSegments();
			id = (String) segments.get(1);
			result = builder.query(db.getReadableDatabase(), projection,
					TedAlarmMeta.TableHolidayColumns.COL_ALARM_ID + "=?",
					new String[] { id }, null, null, order);
			break;
		}

		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		TedAlarmMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType == null) {
			return 0;
		}

		int rows = 0;
		switch (pathType) {
		case SINGLE_ALARM:
			List<?> segments = uri.getPathSegments();
			String id = (String) segments.get(0);
			rows = db.getWritableDatabase().update(
					TedAlarmMeta.TableAlarmColumns.TABLE_NAME, values,
					TedAlarmMeta.TableAlarmColumns.COL_ID + "=?",
					new String[] { id });
			break;
		// case ALL_HOLIDAYS:
		// segments = uri.getPathSegments();
		// id = (String) segments.get(0);
		// rows = db.getWritableDatabase().update(
		// TedAlarmMeta.TableHolidayColumns.TABLE_NAME, values,
		// TedAlarmMeta.TableAlarmColumns.COL_ID + "=?",
		// new String[] { id });
		// break;
		}

		return rows;
	}

	TedAlarmMeta.PathType matchUriToPathType(Uri uri) {
		int match = matcher.match(uri);
		if (match == -1) {
			return null;
		}

		TedAlarmMeta.PathType pathType = TedAlarmMeta.PathType.class
				.getEnumConstants()[match];
		return pathType;
	}

	class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, TedAlarmMeta.DB_NAME, null, TedAlarmMeta.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TedAlarmMeta.TableAlarmColumns.SQL_CREATE);
			db.execSQL(TedAlarmMeta.TableHolidayColumns.SQL_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(String.format("drop table if exists %s",
					TedAlarmMeta.TableAlarmColumns.TABLE_NAME));
			db.execSQL(String.format("drop table if exists %s",
					TedAlarmMeta.TableHolidayColumns.TABLE_NAME));
			onCreate(db);
		}
	}

}