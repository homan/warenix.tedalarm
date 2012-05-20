package org.dyndns.warenix.tedalarm;

import org.dyndns.warenix.util.WLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Alarm ring receiver when a scheduled alarm in about to fire.
 * 
 * @author warenix
 * 
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String TAG = "BootReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		WLog.i(TAG, "device boot up, reshedule all alarms");
		AlarmMaster.rescheduleAllAlarms(context);
	}

}