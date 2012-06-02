package org.dyndns.warenix.tedalarm;

import org.dyndns.warenix.tedalarm.app.TedAlarmService;
import org.dyndns.warenix.util.WLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Alarm ring receiver when a scheduled alarm in about to fire.
 * 
 * @author warenix
 * 
 */
public class AlarmRingReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Uri alarmUri = intent.getData();
		WLog.d(TAG, String.format("received alarm[%s]", alarmUri));

		Intent serviceIntent = new Intent(context, TedAlarmService.class);
		serviceIntent.setAction(intent.getAction());
		serviceIntent.setData(alarmUri);
		context.startService(serviceIntent);
	}

}