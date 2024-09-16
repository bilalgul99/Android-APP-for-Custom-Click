package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ButtonConfigActivity extends AppCompatActivity {

    private EditText nameEditText, timeEditText;
    private TextView coordinateTextView;
    private Button setCoordinatesButton, saveButton;
    private int buttonId;
    private float clickX = -1, clickY = -1;  // Coordinates for click
    private boolean isSettingCoordinates = false;  // Flag for setting coordinates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_config);

        nameEditText = findViewById(R.id.nameEditText);
        timeEditText = findViewById(R.id.timeEditText);
        coordinateTextView = findViewById(R.id.coordinateTextView);
        setCoordinatesButton = findViewById(R.id.setCoordinatesButton);
        saveButton = findViewById(R.id.saveButton);

        // Get button ID from intent
        buttonId = getIntent().getIntExtra("buttonId", -1);

        // Load saved preferences if available
        loadButtonConfig(buttonId);

        // Set listener for setting coordinates
        setCoordinatesButton.setOnClickListener(v -> {
            isSettingCoordinates = true;
            Toast.makeText(ButtonConfigActivity.this, "Touch anywhere on the screen to set coordinates.", Toast.LENGTH_LONG).show();
        });

        // Capture touch event for the screen to set click coordinates
        findViewById(R.id.touchOverlay).setOnTouchListener((v, event) -> {
            if (isSettingCoordinates && event.getAction() == MotionEvent.ACTION_DOWN) {
                clickX = event.getRawX();
                clickY = event.getRawY();
                coordinateTextView.setText(String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", clickX, clickY));
                isSettingCoordinates = false;
                return true;
            }
            return false;
        });

        // Save button click listener
        saveButton.setOnClickListener(v -> {
            // Validate inputs
            if (nameEditText.getText().toString().isEmpty() || timeEditText.getText().toString().isEmpty()) {
                Toast.makeText(ButtonConfigActivity.this, "Please enter all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (clickX == -1 || clickY == -1) {
                Toast.makeText(ButtonConfigActivity.this, "Please set the coordinates.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save data to SharedPreferences
            saveButtonConfig(buttonId);
            Toast.makeText(ButtonConfigActivity.this, "Configuration saved.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveButtonConfig(int buttonId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        // Save name, time, and coordinates
        editor.putString("buttonName_" + buttonId, nameEditText.getText().toString());
        editor.putString("buttonTime_" + buttonId, timeEditText.getText().toString());
        editor.putFloat("buttonX_" + buttonId, clickX);
        editor.putFloat("buttonY_" + buttonId, clickY);
        editor.apply();
    }

    private void loadButtonConfig(int buttonId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Load saved name, time, and coordinates
        String buttonName = preferences.getString("buttonName_" + buttonId, "");
        String buttonTime = preferences.getString("buttonTime_" + buttonId, "");
        float buttonX = preferences.getFloat("buttonX_" + buttonId, -1);
        float buttonY = preferences.getFloat("buttonY_" + buttonId, -1);

        nameEditText.setText(buttonName);
        timeEditText.setText(buttonTime);
        if (buttonX != -1 && buttonY != -1) {
            coordinateTextView.setText(String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", buttonX, buttonY));
        }
    }
}
