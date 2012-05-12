package org.dyndns.warenix.google.calendar.provider;

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

public class GoogleCalendarProvider extends ContentProvider {
	public static final Uri CONTENT_URI = Uri.parse(String.format(
			"content://%s", GoogleCalendarMeta.AUTHORITY));

	static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		matcher.addURI(GoogleCalendarMeta.AUTHORITY,
				GoogleCalendarMeta.PATH_ALL_CALENDAR,
				GoogleCalendarMeta.PathType.ALL_CALENDARS.ordinal());
		matcher.addURI(GoogleCalendarMeta.AUTHORITY,
				GoogleCalendarMeta.PATH_ALL_DAY_EVENT,
				GoogleCalendarMeta.PathType.ALL_DAY_EVENT.ordinal());
		matcher.addURI(GoogleCalendarMeta.AUTHORITY,
				GoogleCalendarMeta.PATH_SINGLE_ALL_DAY_EVENT,
				GoogleCalendarMeta.PathType.SINGLE_ALL_DAY_EVENT.ordinal());
		matcher.addURI(GoogleCalendarMeta.AUTHORITY,
				GoogleCalendarMeta.PATH_HAVE_ALL_DAY_EVENT,
				GoogleCalendarMeta.PathType.HAVE_ALL_DAY_EVENT.ordinal());
	}

	protected DatabaseHelper db;

	@Override
	public int delete(Uri uri, String where, String[] args) {
		GoogleCalendarMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType == null) {
			return 0;
		}

		int rows = 0;
		SQLiteDatabase database = null;
		switch (pathType) {
		// case SINGLE_CALENDAR:
		// List<?> segments = uri.getPathSegments();
		// String id = (String) segments.get(0);
		// where = GoogleCalendarMeta.TableAlarmColumns.COL_ID + "=?";
		// args = new String[] { id };
		case ALL_CALENDARS:
			database = db.getWritableDatabase();
			rows = database.delete(
					GoogleCalendarMeta.TableAlarmColumns.TABLE_NAME, where,
					args);
			break;
		}

		return rows;
	}

	@Override
	public String getType(Uri uri) {
		GoogleCalendarMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType != null) {
			switch (pathType) {
			case ALL_DAY_EVENT:
				return "event";
			case SINGLE_CALENDAR:
			case ALL_CALENDARS:
				return "googlecalendar";
			}
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// not the Uri we're expecting
		long newID = 0;
		GoogleCalendarMeta.PathType pathType = matchUriToPathType(uri);

		if (pathType != GoogleCalendarMeta.PathType.ALL_CALENDARS) {
			throw new IllegalArgumentException("Wrong insert URi "
					+ uri.toString());
		}

		if (values != null) {
			newID = db.getWritableDatabase().insert(
					GoogleCalendarMeta.TableAlarmColumns.TABLE_NAME, null,
					values);
			return Uri.withAppendedPath(uri, String.valueOf(newID));
		} else
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

		builder.setTables(GoogleCalendarMeta.TableAlarmColumns.TABLE_NAME);

		String order = null;
		Cursor result = null;
		if (sortOrder != null)
			order = sortOrder;

		int match = matcher.match(uri);
		GoogleCalendarMeta.PathType pathType = GoogleCalendarMeta.PathType.class
				.getEnumConstants()[match];
		List<?> segments;
		String id;

		switch (pathType) {
		case SINGLE_CALENDAR:
			segments = uri.getPathSegments();
			id = (String) segments.get(0);
			result = builder.query(db.getReadableDatabase(), projection,
					GoogleCalendarMeta.TableAlarmColumns.COL_ID + "=?",
					new String[] { id }, null, null, order);
			break;
		case ALL_DAY_EVENT:
			result = builder.query(db.getReadableDatabase(), projection,
					GoogleCalendarMeta.TableAlarmColumns.COL_KIND + "=?",
					new String[] { "calendar#event" }, null, null, order);
			break;
		// case SCHEDULED_CALENDARS:
		// result = builder.query(db.getReadableDatabase(), projection,
		// GoogleCalendarMeta.TableAlarmColumns.COL_SCHEDULED + "=?",
		// new String[] { "1" }, null, null, order);
		// break;
		case SINGLE_ALL_DAY_EVENT:
			segments = uri.getPathSegments();
			id = (String) segments.get(1);
			result = builder
					.query(db.getReadableDatabase(),
							projection,
							GoogleCalendarMeta.TableAlarmColumns.COL_KIND
									+ "=?"
									+ " AND "
									+ GoogleCalendarMeta.TableAlarmColumns.COL_CALENDAR_ID
									+ "=?",
							new String[] { "calendar#event", id }, null, null,
							order);
			break;
		case HAVE_ALL_DAY_EVENT:
			segments = uri.getPathSegments();
			id = (String) segments.get(1);
			String withinToday = String.format("? between %s and %s ",
					GoogleCalendarMeta.TableAlarmColumns.COL_START_TIME,
					GoogleCalendarMeta.TableAlarmColumns.COL_END_TIME);
			result = builder
					.query(db.getReadableDatabase(),
							projection,
							GoogleCalendarMeta.TableAlarmColumns.COL_KIND
									+ "=?"
									+ " AND "
									+ GoogleCalendarMeta.TableAlarmColumns.COL_CALENDAR_ID
									+ "=?" + " AND " + withinToday,
							new String[] { "calendar#event", id,
									"" + System.currentTimeMillis() }, null,
							null, order);
			break;
		case ALL_CALENDARS:
			result = builder.query(db.getReadableDatabase(), projection,
					GoogleCalendarMeta.TableAlarmColumns.COL_KIND + "=?",
					new String[] { "calendar#calendarListEntry" }, null, null,
					order);
			break;
		}

		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		GoogleCalendarMeta.PathType pathType = matchUriToPathType(uri);
		if (pathType == null) {
			return 0;
		}

		int rows = 0;
		switch (pathType) {
		case SINGLE_CALENDAR:
			if (values != null) {
				List<?> segments = uri.getPathSegments();
				String id = (String) segments.get(0);
				rows = db.getWritableDatabase().update(
						GoogleCalendarMeta.TableAlarmColumns.TABLE_NAME,
						values,
						GoogleCalendarMeta.TableAlarmColumns.COL_ID + "=?",
						new String[] { id });

			}
			break;
		}

		return rows;
	}

	GoogleCalendarMeta.PathType matchUriToPathType(Uri uri) {
		int match = matcher.match(uri);
		if (match == -1) {
			return null;
		}

		GoogleCalendarMeta.PathType pathType = GoogleCalendarMeta.PathType.class
				.getEnumConstants()[match];
		return pathType;
	}

	class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, GoogleCalendarMeta.DB_NAME, null,
					GoogleCalendarMeta.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(GoogleCalendarMeta.TableAlarmColumns.SQL_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(String.format("drop table %s",
					GoogleCalendarMeta.TableAlarmColumns.TABLE_NAME));
			onCreate(db);
		}
	}

}
