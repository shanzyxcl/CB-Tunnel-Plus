package com.cbtunnel.plus.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.cbtunnel.plus.harliesApplication;

public class BufferSizeDialog
{
	private AlertDialog.Builder adb;
	private SharedPreferences mPref;
	private static SharedPreferences.Editor editor;

	public BufferSizeDialog(Context c) {
		mPref = harliesApplication.getPrivateSharedPreferences();
		editor = mPref.edit();
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(40,0,40,0);
        ll.setLayoutParams(layoutParams);
		final TextInputLayout til = new TextInputLayout(c);
		final AppCompatEditText acet = new AppCompatEditText(c);
		acet.setHint("Send");
		acet.setText(mPref.getString("buffer_send", "16384"));
		til.addView(acet);
		final TextInputLayout til0 = new TextInputLayout(c);
		final AppCompatEditText acet0 = new AppCompatEditText(c);
		acet0.setHint("Receive");
		acet0.setText(mPref.getString("buffer_receive", "32768"));
		til0.addView(acet0);
		ll.addView(til);
		ll.addView(til0);
		adb = new AlertDialog.Builder(c);
		adb.setCancelable(false);
	    adb.setTitle("BufferSize");
	    adb.setMessage("Set the proxy socket buffer size\n\n[WARNING] This is for advanced user only, do not edit this if you don't know what is your doing.");
		adb.setView(ll);
		adb.setPositiveButton("SAVE", (p1, p2) -> {
            editor.putString("buffer_send", acet.getText().toString()).apply();
            editor.putString("buffer_receive", acet0.getText().toString()).apply();
			mListener.onSave();
        });
		adb.setNegativeButton("Cancel", null);
		adb.setNeutralButton("Reset", (p1, p2) -> {
			editor.putString("buffer_send", "16384").apply();
			editor.putString("buffer_receive", "32768").apply();
			mListener.onReset();
        });
	}

	private BufferSizeDialog.OnBufferDialogListener mListener;
	public interface OnBufferDialogListener {
		void onSave();
		void onReset();
	}

	public void setOnBufferDialogListener(BufferSizeDialog.OnBufferDialogListener listener){
		this.mListener = listener;
	}

	public void show(){
		adb.show();
	}
}
