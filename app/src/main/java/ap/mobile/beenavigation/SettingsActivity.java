package ap.mobile.beenavigation;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

/**
 * Created by Aryo on 29/08/2017.
 */

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = this.getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.toolbar));
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            bindPreferenceValueToSummary(findPreference("pref_basepath"));
            bindPreferenceValueToSummary(findPreference("pref_cost"));
            bindPreferenceValueToSummary(findPreference("pref_walkingDistance"));
            bindPreferenceValueToSummary(findPreference("pref_priority"));
            bindPreferenceValueToSummary(findPreference("pref_simplify_distance"));

        }
    }

    private static void bindPreferenceValueToSummary(Preference preference) {
        preference.setOnPreferenceChangeListener(preferenceChangeListener);
        if (preference.getKey().equals("pref_priority"))
            preferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));
        else
            preferenceChangeListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
    }

    private static Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference.getKey().equals("pref_walkingDistance"))
                preference.setSummary(newValue.toString() + " m");
            else if (preference.getKey().equals("pref_simplify_distance"))
                preference.setSummary(newValue.toString() + " m");
            else if (preference.getKey().equals("pref_cost"))
                preference.setSummary("Rp " + newValue.toString());
            else if (preference.getKey().equals("pref_basepath"))
                preference.setSummary(newValue.toString());
            else if (preference.getKey().equals("pref_priority")) {
                preference.setSummary((boolean) newValue ? "Cost over Distance" : "Distance over Cost");
            } else preference.setSummary(newValue.toString());
            return true;
        }
    };
}
