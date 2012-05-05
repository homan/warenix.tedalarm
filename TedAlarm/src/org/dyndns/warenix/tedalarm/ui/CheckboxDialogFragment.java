package org.dyndns.warenix.tedalarm.ui;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class CheckboxDialogFragment extends SherlockDialogFragment {
	OnCheckboxDialogListener mCheckboxDialogListener;
	String[] items;
	boolean[] itemsChecked;

	public static CheckboxDialogFragment newInstance(ArrayList<String> items,
			boolean[] itemChecked) {
		CheckboxDialogFragment f = new CheckboxDialogFragment();
		f.items = items.toArray(new String[items.size()]);
		if (itemChecked != null) {
			f.itemsChecked = itemChecked.clone();
		} else {
			f.itemsChecked = new boolean[items.size()];
		}
		return f;
	}

	public void setOnCheckboxDialogListener(OnCheckboxDialogListener listener) {
		mCheckboxDialogListener = listener;
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity())
				// .setIcon(R.drawable.icon)
				.setTitle("Choose holiday calendars")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mCheckboxDialogListener != null) {
							mCheckboxDialogListener.onOk(itemsChecked);
						}
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (mCheckboxDialogListener != null) {
									mCheckboxDialogListener.onCancel();
								}
							}
						})
				.setMultiChoiceItems(items, itemsChecked,
						new DialogInterface.OnMultiChoiceClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which, boolean isChecked) {
							}
						}).create();
	}

	public interface OnCheckboxDialogListener {
		public void onOk(boolean[] itemsChecked);

		public void onCancel();
	}
}
