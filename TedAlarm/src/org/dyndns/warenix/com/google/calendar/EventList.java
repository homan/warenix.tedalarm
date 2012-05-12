package org.dyndns.warenix.com.google.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.dyndns.warenix.util.JSONUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EventList {

	public static class EventListItem {
		static final long DAY_MS = 24 * 60 * 60 * 1000;

		public String kind;
		public String etag;
		public String id;
		public String status;
		public String htmlLink;
		public String created;
		public String updated;
		public String summary;
		public String description;
		public String timeZone;
		public String colorId;
		public String selected;
		public String accessRole;
		public long startTime;
		public long endTime;
		public String calendarId;

		public static EventListItem factory(String jsonString) {
			JSONObject json;
			try {
				json = new JSONObject(jsonString);
				return factory(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		public static EventListItem factory(JSONObject json) {
			EventListItem item = new EventListItem();
			item.kind = JSONUtil.getString(json, "kind", null);
			item.etag = JSONUtil.getString(json, "etag", null);
			item.id = JSONUtil.getString(json, "id", null);
			item.summary = JSONUtil.getString(json, "summary", null);
			item.description = JSONUtil.getString(json, "description", null);
			item.colorId = JSONUtil.getString(json, "colorId", null);
			item.selected = JSONUtil.getString(json, "selected", null);
			item.accessRole = JSONUtil.getString(json, "accessRole", null);
			try {
				JSONObject dateTimeJson = json.getJSONObject("start");
				String str = JSONUtil.getString(dateTimeJson, "dateTime", null);
				if (str == null) {
					str = JSONUtil.getString(dateTimeJson, "date", null);
				}
				item.startTime = parseDateTime(str);
			} catch (JSONException e) {
			}
			try {
				JSONObject dateTimeJson = json.getJSONObject("end");
				String str = JSONUtil.getString(dateTimeJson, "dateTime", null);
				if (str == null) {
					str = JSONUtil.getString(dateTimeJson, "date", null);
				}
				item.endTime = parseDateTime(str);
			} catch (JSONException e) {
			}
			try {
				JSONObject origanizer = json.getJSONObject("organizer");
				item.calendarId = JSONUtil.getString(origanizer, "email", null);
			} catch (JSONException e) {
			}

			return item;
		}

		public static long parseDateTime(String dateTimeStr) {
			try {
				SimpleDateFormat format2 = new SimpleDateFormat(
						"yyyy-MM-dd'T'hh:m:ssz");
				Date startTime = format2.parse(dateTimeStr);
				return startTime.getTime();
			} catch (ParseException e) {
				SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
				try {
					Date startTime = format2.parse(dateTimeStr);
					return startTime.getTime();
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}

			return 0;
		}

		public boolean isAllDay() {
			try {
				SimpleDateFormat format2 = new SimpleDateFormat(
						"yyyy-MM-dd'T'hh:m:ssz");
				Date startTime = format2.parse("2012-05-06T00:00:00+08:00");
				Date endTime = format2.parse("2012-05-07T01:00:00+08:00");
				long diff = endTime.getTime() - startTime.getTime();
				return diff >= DAY_MS;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	public static ArrayList<EventListItem> factory(String jsonString) {
		ArrayList<EventListItem> list = null;

		JSONObject json;
		try {
			json = new JSONObject(jsonString);
			JSONArray itemsJSONArray = json.getJSONArray("items");
			int len = itemsJSONArray.length();
			JSONObject itemJson = null;
			list = new ArrayList<EventList.EventListItem>();
			for (int i = 0; i < len; ++i) {
				itemJson = itemsJSONArray.getJSONObject(i);
				list.add(EventListItem.factory(itemJson));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return list;
	}
}
