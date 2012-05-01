package org.dyndns.warenix.tedalarm;

import org.dyndns.warenix.util.WLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Uri alarmUri = intent.getData();
		WLog.d(TAG, String.format("received alarm[%s]", alarmUri));

		AlarmMaster.startAlarmActivity(context, alarmUri);
	}

}