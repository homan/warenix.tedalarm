package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;

import org.dyndns.warenix.com.google.calendar.CalendarList.CalendarListItem;
import org.dyndns.warenix.com.google.calendar.GoogleCalendarMaster;
import org.dyndns.warenix.util.WLog;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.google.api.GoogleAppInfo;
import com.google.api.GoogleOAuthAccessToken;
import com.google.api.GoogleOAuthActivity;

public class DatePickerDialogFragment extends DialogFragment {
	private static final String TAG = "DatePickerDialogFragment";

	private Fragment mFragment;

	public DatePickerDialogFragment(Fragment callback) {
		mFragment = callback;
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new DatePickerDialog(getActivity(),
				(OnDateSetListener) mFragment, 1980, 7, 16);
	}

}
