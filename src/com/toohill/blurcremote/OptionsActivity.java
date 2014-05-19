package com.toohill.blurcremote;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class OptionsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
	}

}