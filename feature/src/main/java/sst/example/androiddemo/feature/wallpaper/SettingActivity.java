package sst.example.androiddemo.feature.wallpaper;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import sst.example.androiddemo.feature.R;

public class SettingActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        // add a validator to the "numberofCircles" preference so that it only
        // accepts numbers
        Preference circlePreference = getPreferenceScreen().findPreference(
                "numberOfCircles");
        // add the validator
        circlePreference.setOnPreferenceChangeListener(numberCheckListener);
    }

    Preference.OnPreferenceChangeListener numberCheckListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // check that the string is an integer
                    if (newValue != null && newValue.toString().length() > 0
                            && newValue.toString().matches("\\d*")) {
                        //输入任意个"d"    d*
                        //0-9之间的任意一个数字  \\d*
                        return true;
                    }
                    // If now create a message to the user
                    Toast.makeText(SettingActivity.this, "Invalid Input",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            };
}
