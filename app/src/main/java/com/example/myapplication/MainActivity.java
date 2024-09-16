package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

public class MainActivity extends Activity {

    private Button button1, button2, button3, button4, button5;
    private TextView status1, status2, status3, status4, status5;
    private Button toggleModeButton;

    private boolean isConfigMode = true;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;
    private Handler mainHandler;

    private boolean[] isButtonRunning = new boolean[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executorService = Executors.newFixedThreadPool(5);
        scheduledExecutorService = Executors.newScheduledThreadPool(5);
        mainHandler = new Handler(Looper.getMainLooper());

        initializeViews();
        loadButtonConfigs();
        setupButtonListeners();

        toggleModeButton.setOnClickListener(v -> toggleMode());

        // Check if accessibility service is enabled
        checkAccessibilityService();
    }

    private void checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled(this, ClickAccessibilityService.class)) {
            new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Not Enabled")
                .setMessage("The app requires the Accessibility Service to be enabled to function properly. Would you like to enable it now?")
                .setPositiveButton("Yes", (dialog, which) -> openAccessibilitySettings())
                .setNegativeButton("No", (dialog, which) -> Toast.makeText(this, "App functionality will be limited", Toast.LENGTH_LONG).show())
                .show();
        }
    }

    private void initializeViews() {
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);
        button5 = findViewById(R.id.button5);

        status1 = findViewById(R.id.infoText1);
        status2 = findViewById(R.id.infoText2);
        status3 = findViewById(R.id.infoText3);
        status4 = findViewById(R.id.infoText4);
        status5 = findViewById(R.id.infoText5);

        toggleModeButton = findViewById(R.id.toggleButton);
    }

    private void setupButtonListeners() {
        button1.setOnClickListener(v -> onButtonClick(0));
        button2.setOnClickListener(v -> onButtonClick(1));
        button3.setOnClickListener(v -> onButtonClick(2));
        button4.setOnClickListener(v -> onButtonClick(3));
        button5.setOnClickListener(v -> onButtonClick(4));
    }

    private void onButtonClick(int buttonIndex) {
        if (isConfigMode) {
            Intent intent = new Intent(MainActivity.this, ButtonConfigActivity.class);
            intent.putExtra("buttonId", buttonIndex + 1);
            startActivity(intent);
        } else {
            toggleButtonThread(buttonIndex);
        }
    }

    private void toggleButtonThread(int buttonIndex) {
        if (isButtonRunning[buttonIndex]) {
            stopButtonThread(buttonIndex);
        } else {
            startButtonThread(buttonIndex);
        }
    }

    private void startButtonThread(int buttonIndex) {
        isButtonRunning[buttonIndex] = true;
        updateButtonColor(buttonIndex);
        
        executorService.execute(() -> {
            try {
                while (isButtonRunning[buttonIndex]) {
                    long remainingTime = getTimeUntilClick(buttonIndex);
                    if (remainingTime > 100) {

                        updateRemainingTime(buttonIndex, remainingTime);
                        Thread.sleep(Math.min(remainingTime, 10)); // Sleep for remaining time or 100ms, whichever is smaller
                    } else {
                        //print to the console that the button has been clicked
                        Log.d("MainActivity", "Button " + (buttonIndex + 1) + " clicked");
                        Log.d("MainActivity", "Remaining time: " + remainingTime);
                        performClick(buttonIndex);
                        stopButtonThread(buttonIndex);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private long getTimeUntilClick(int buttonIndex) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String buttonTime = preferences.getString("buttonTime_" + (buttonIndex + 1), null);
        
        if (buttonTime == null || buttonTime.equals("Time not set")) return -1;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault());
            Date targetTime = sdf.parse(buttonTime);
            Date currentTime = new Date();
            
            Calendar targetCalendar = Calendar.getInstance();
            targetCalendar.setTime(targetTime);
            
            Calendar currentCalendar = Calendar.getInstance();
            currentCalendar.setTime(currentTime);
            
            // Set the target time to today
            targetCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
            targetCalendar.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
            targetCalendar.set(Calendar.DAY_OF_MONTH, currentCalendar.get(Calendar.DAY_OF_MONTH));
            
            // Calculate time difference
            long timeDiff = targetCalendar.getTimeInMillis() - currentCalendar.getTimeInMillis();
            
            // If the time has already passed today, set it for tomorrow
            if (timeDiff < 0) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1);
                timeDiff = targetCalendar.getTimeInMillis() - currentCalendar.getTimeInMillis();
            }
            
            return timeDiff;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void updateRemainingTime(int buttonIndex, long remainingTime) {
        TextView status = getStatusByIndex(buttonIndex);
        Button button = getButtonByIndex(buttonIndex);
        if (remainingTime >= 0) {
            String timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d.%03d",
                    TimeUnit.MILLISECONDS.toHours(remainingTime),
                    TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60,
                    remainingTime % 1000);
            mainHandler.post(() -> {
                status.setText("Time remaining: " + timeString);
                button.setText(timeString);
            });
        } else {
            mainHandler.post(() -> {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String buttonName = preferences.getString("buttonName_" + (buttonIndex + 1), "Button " + (buttonIndex + 1));
                button.setText(buttonName);
                updateButtonDisplay(button, status, preferences, buttonIndex + 1);
            });
        }
    }

    private TextView getStatusByIndex(int index) {
        switch (index) {
            case 0: return status1;
            case 1: return status2;
            case 2: return status3;
            case 3: return status4;
            case 4: return status5;
            default: throw new IllegalArgumentException("Invalid status index");
        }
    }

    private void stopButtonThread(int buttonIndex) {
        isButtonRunning[buttonIndex] = false;
        updateButtonColor(buttonIndex);
        // Reset the button display
        Button button = getButtonByIndex(buttonIndex);
        TextView status = getStatusByIndex(buttonIndex);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mainHandler.post(() -> updateButtonDisplay(button, status, preferences, buttonIndex + 1));
    }

    private boolean isTimeToClick(int buttonIndex) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String buttonTime = preferences.getString("buttonTime_" + (buttonIndex + 1), null);
        
        if (buttonTime == null) return false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault());
            Date targetTime = sdf.parse(buttonTime);
            Date currentTime = new Date();

            //return true if the difference between the current time and the button time is less than 100ms
            return targetTime.getTime() - currentTime.getTime() < 100;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void performClick(int buttonIndex) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        float clickX = preferences.getFloat("buttonX_" + (buttonIndex + 1), -1);
        float clickY = preferences.getFloat("buttonY_" + (buttonIndex + 1), -1);

        if (clickX != -1 && clickY != -1) {
            if (isAccessibilityServiceEnabled(this, ClickAccessibilityService.class)) {
                ClickAccessibilityService service = ClickAccessibilityService.getInstance();
                if (service != null) {
                    service.performClick((int) clickX, (int) clickY);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Click performed at the specified time", Toast.LENGTH_SHORT).show();
                        stopButtonThread(buttonIndex);
                    });
                } else {
                    Toast.makeText(this, "ClickAccessibilityService is not available", Toast.LENGTH_LONG).show();
                }
            } else {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Accessibility Service is not enabled", Toast.LENGTH_LONG).show();
                    openAccessibilitySettings();
                });
            }
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),  Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        
        startActivity(intent);
    }

    private void updateButtonColor(int buttonIndex) {
        Button button = getButtonByIndex(buttonIndex);
        int color = isButtonRunning[buttonIndex] ? Color.GREEN : Color.RED;
        mainHandler.post(() -> button.setBackgroundColor(color));
    }

    private Button getButtonByIndex(int index) {
        switch (index) {
            case 0: return button1;
            case 1: return button2;
            case 2: return button3;
            case 3: return button4;
            case 4: return button5;
            default: throw new IllegalArgumentException("Invalid button index");
        }
    }

    private void toggleMode() {
        isConfigMode = !isConfigMode;
        toggleModeButton.setText(isConfigMode ? "Switch to Activation Mode" : "Switch to Configuration Mode");
        Toast.makeText(this, isConfigMode ? "Now in Configuration Mode" : "Now in Activation Mode", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadButtonConfigs();
        // Check accessibility service status on resume as well
        checkAccessibilityService();
    }

    private void loadButtonConfigs() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        updateButtonDisplay(button1, status1, preferences, 1);
        updateButtonDisplay(button2, status2, preferences, 2);
        updateButtonDisplay(button3, status3, preferences, 3);
        updateButtonDisplay(button4, status4, preferences, 4);
        updateButtonDisplay(button5, status5, preferences, 5);
    }

    private void updateButtonDisplay(Button button, TextView status, SharedPreferences preferences, int buttonId) {
        String buttonName = preferences.getString("buttonName_" + buttonId, "Button " + buttonId);
        String buttonTime = preferences.getString("buttonTime_" + buttonId, "Time not set");
        float buttonX = preferences.getFloat("buttonX_" + buttonId, -1);
        float buttonY = preferences.getFloat("buttonY_" + buttonId, -1);

        button.setText(buttonName);
        if (buttonX != -1 && buttonY != -1) {
            status.setText(String.format(Locale.getDefault(), "Time: %s, X: %.2f, Y: %.2f", buttonTime, buttonX, buttonY));
        } else {
            status.setText(String.format(Locale.getDefault(), "Time: %s, Coordinates: Not set", buttonTime));
        }
        button.setBackgroundColor(isButtonRunning[buttonId - 1] ? Color.GREEN : Color.RED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
    }
}
