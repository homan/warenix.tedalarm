package org.dyndns.warenix.tedalarm.ui;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

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
