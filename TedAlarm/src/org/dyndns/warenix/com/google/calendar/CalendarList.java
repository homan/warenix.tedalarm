package org.dyndns.warenix.com.google.calendar;

import java.util.ArrayList;

import org.dyndns.warenix.util.JSONUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CalendarList {

	public static class CalendarListItem {
		public String kind;
		public String etag;
		public String id;
		public String summary;
		public String description;
		public String timeZone;
		public String colorId;
		public String selected;
		public String accessRole;

		public static CalendarListItem factory(String jsonString) {
			JSONObject json;
			try {
				json = new JSONObject(jsonString);
				return factory(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		public static CalendarListItem factory(JSONObject json) {
			CalendarListItem item = new CalendarListItem();
			item.kind = JSONUtil.getString(json, "kind", null);
			item.etag = JSONUtil.getString(json, "etag", null);
			item.id = JSONUtil.getString(json, "id", null);
			item.summary = JSONUtil.getString(json, "summary", null);
			item.description = JSONUtil.getString(json, "description", null);
			item.colorId = JSONUtil.getString(json, "colorId", null);
			item.selected = JSONUtil.getString(json, "selected", null);
			item.accessRole = JSONUtil.getString(json, "accessRole", null);
			return item;
		}
	}

	public static ArrayList<CalendarListItem> factory(String jsonString) {
		ArrayList<CalendarListItem> list = null;

		JSONObject json;
		try {
			json = new JSONObject(jsonString);
			JSONArray itemsJSONArray = json.getJSONArray("items");
			int len = itemsJSONArray.length();
			JSONObject itemJson = null;
			list = new ArrayList<CalendarList.CalendarListItem>();
			for (int i = 0; i < len; ++i) {
				itemJson = itemsJSONArray.getJSONObject(i);
				list.add(CalendarListItem.factory(itemJson));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return list;
	}
}
