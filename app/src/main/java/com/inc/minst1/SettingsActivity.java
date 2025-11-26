package com.inc.minst1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SettingsActivity extends AppCompatActivity {

    // --- Views ---
    TextView tvHours, tvMinutes, tvSnooze;
    ImageButton btnHoursPlus, btnHoursMinus, btnMinutesPlus, btnMinutesMinus, btnSnoozePlus, btnSnoozeMinus;
    TextView tvCurrentInterval, tvCurrentSnooze;
    Button btnSave;
    ImageButton btnBack;
    MaterialButton btnPreset20, btnPreset50;
    MaterialButton btnSnoozePreset10, btnSnoozePreset20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViews();
        setInitialValues();
        updateCurrentSettingsDisplay();
        setupListeners();
    }

    private void findViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHours = findViewById(R.id.tvHours);
        tvMinutes = findViewById(R.id.tvMinutes);
        tvSnooze = findViewById(R.id.tvSnooze);
        btnHoursPlus = findViewById(R.id.btnHoursPlus);
        btnHoursMinus = findViewById(R.id.btnHoursMinus);
        btnMinutesPlus = findViewById(R.id.btnMinutesPlus);
        btnMinutesMinus = findViewById(R.id.btnMinutesMinus);
        btnSnoozePlus = findViewById(R.id.btnSnoozePlus);
        btnSnoozeMinus = findViewById(R.id.btnSnoozeMinus);
        tvCurrentInterval = findViewById(R.id.tvCurrentInterval);
        tvCurrentSnooze = findViewById(R.id.tvCurrentSnooze);
        btnSave = findViewById(R.id.btnSaveSettings);
        btnPreset20 = findViewById(R.id.btnPreset20);
        btnPreset50 = findViewById(R.id.btnPreset50);
        btnSnoozePreset10 = findViewById(R.id.btnSnoozePreset10);
        btnSnoozePreset20 = findViewById(R.id.btnSnoozePreset20);
        Button btnSetAppGoals = findViewById(R.id.btnSetAppGoals);

        btnSetAppGoals.setOnClickListener(v -> {
            startActivity(new Intent(this, SetGoalsActivity.class));
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Listeners for hours (now jumps by 2)
        btnHoursPlus.setOnClickListener(v -> updateValue(tvHours, 2, 0, 5, "h"));      // Changed from 1 to 2
        btnHoursMinus.setOnClickListener(v -> updateValue(tvHours, -2, 0, 5, "h"));     // Changed from -1 to -2

        // Listeners for minutes (now jumps by 2)
        btnMinutesPlus.setOnClickListener(v -> updateValue(tvMinutes, 2, 0, 59, "m"));  // Changed from 1 to 2
        btnMinutesMinus.setOnClickListener(v -> updateValue(tvMinutes, -2, 0, 59, "m")); // Changed from -1 to -2

        // Listeners for snooze (now jumps by 2)
        btnSnoozePlus.setOnClickListener(v -> updateValue(tvSnooze, 2, 1, 60, " min")); // Changed from 1 to 2
        btnSnoozeMinus.setOnClickListener(v -> updateValue(tvSnooze, -2, 1, 60, " min"));// Changed from -1 to -2

        // --- (The rest of the listeners are unchanged) ---
        btnPreset20.setOnClickListener(v -> setPreset(0, 20));
        btnPreset50.setOnClickListener(v -> setPreset(0, 50));

        btnSnoozePreset10.setOnClickListener(v -> tvSnooze.setText("10 min"));
        btnSnoozePreset20.setOnClickListener(v -> tvSnooze.setText("20 min"));

        btnSave.setOnClickListener(v -> {
            saveSettings();
            finish();
        });
    }

    private void setInitialValues() {
        long interval = Prefs.getInterval(this);
        long snooze = Prefs.getSnooze(this);
        long hours = TimeUnit.MILLISECONDS.toHours(interval);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        long snoozeMinutes = TimeUnit.MILLISECONDS.toMinutes(snooze);

        tvHours.setText(String.format(Locale.US, "%dh", hours));
        tvMinutes.setText(String.format(Locale.US, "%dm", minutes));
        tvSnooze.setText(String.format(Locale.US, "%d min", snoozeMinutes));
    }

    private void saveSettings() {
        try {
            int hours = Integer.parseInt(tvHours.getText().toString().replace("h", "").trim());
            int minutes = Integer.parseInt(tvMinutes.getText().toString().replace("m", "").trim());
            int snoozeMinutes = Integer.parseInt(tvSnooze.getText().toString().replace(" min", "").trim());

            long newInterval = TimeUnit.MINUTES.toMillis(hours * 60L + minutes);
            if (newInterval <= 0) {
                newInterval = Prefs.defaultInterval();
            }

            Prefs.setInterval(this, newInterval);
            Prefs.setSnooze(this, TimeUnit.MINUTES.toMillis(snoozeMinutes));

            updateCurrentSettingsDisplay();

        } catch (NumberFormatException e) {
            Log.e("SettingsActivity", "Error parsing settings values", e);
        }
    }

    private void setPreset(int hours, int minutes) {
        tvHours.setText(String.format(Locale.US, "%dh", hours));
        tvMinutes.setText(String.format(Locale.US, "%dm", minutes));
    }

    private void updateValue(TextView tv, int delta, int min, int max, String unit) {
        try {
            String currentText = tv.getText().toString().replace(unit, "").trim();
            int currentValue = Integer.parseInt(currentText);
            int newValue = currentValue + delta;

            if (newValue < min) newValue = min;
            if (newValue > max) newValue = max;

            tv.setText(String.format(Locale.US, "%d%s", newValue, unit));
        } catch (NumberFormatException e) {
            Log.e("SettingsActivity", "Error updating value", e);
        }
    }

    private void updateCurrentSettingsDisplay() {
        tvCurrentInterval.setText(formatMillisToReadable(Prefs.getInterval(this)));
        tvCurrentSnooze.setText(formatMillisToReadable(Prefs.getSnooze(this)));
    }

    private String formatMillisToReadable(long millis) {
        if (millis <= 0) {
            return "Not set";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%d hr %d min", hours, minutes);
        } else {
            return String.format(Locale.US, "%d min", minutes);
        }
    }
}