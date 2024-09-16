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

import android.view.ViewGroup;
import android.graphics.Color;

public class ButtonConfigActivity extends AppCompatActivity {

    private EditText nameEditText, timeEditText, gapTimeEditText;
    private TextView coordinateTextView;
    private Button setCoordinatesButton, saveButton;
    private int buttonId;
    private float clickX = -1, clickY = -1;  // Coordinates for click
    private boolean isSettingCoordinates = false;  // Flag for setting coordinates
    private View overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_config);

        nameEditText = findViewById(R.id.nameEditText);
        timeEditText = findViewById(R.id.timeEditText);
        gapTimeEditText = findViewById(R.id.gapTimeEditText);
        coordinateTextView = findViewById(R.id.coordinateTextView);
        setCoordinatesButton = findViewById(R.id.setCoordinatesButton);
        saveButton = findViewById(R.id.saveButton);

        // Get button ID from intent
        buttonId = getIntent().getIntExtra("buttonId", -1);

        // Load saved preferences if available
        loadButtonConfig(buttonId);

        // Create overlay view
        overlayView = new View(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT);
        overlayView.setVisibility(View.GONE);

        // Add overlay to the root layout
        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(overlayView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Set listener for setting coordinates
        setCoordinatesButton.setOnClickListener(v -> {
            isSettingCoordinates = true;
            overlayView.setVisibility(View.VISIBLE);
            Toast.makeText(ButtonConfigActivity.this, "Touch anywhere on the screen to set coordinates.", Toast.LENGTH_LONG).show();
        });

        // Capture touch event for the overlay to set click coordinates
        overlayView.setOnTouchListener((v, event) -> {
            if (isSettingCoordinates && event.getAction() == MotionEvent.ACTION_DOWN) {
                clickX = event.getRawX();
                clickY = event.getRawY();
                coordinateTextView.setText(String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", clickX, clickY));
                isSettingCoordinates = false;
                overlayView.setVisibility(View.GONE);
                return true;
            }
            return false;
        });

        // Save button click listener
        saveButton.setOnClickListener(v -> {
            // Validate inputs
            if (nameEditText.getText().toString().isEmpty() || 
                timeEditText.getText().toString().isEmpty() ||
                gapTimeEditText.getText().toString().isEmpty()) {
                Toast.makeText(ButtonConfigActivity.this, "Please enter all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if coordinates are set, either from previous config or new input
            if (clickX == -1 || clickY == -1) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                clickX = preferences.getFloat("buttonX_" + buttonId, -1);
                clickY = preferences.getFloat("buttonY_" + buttonId, -1);
                
                if (clickX == -1 || clickY == -1) {
                    Toast.makeText(ButtonConfigActivity.this, "Please set the coordinates.", Toast.LENGTH_SHORT).show();
                    return;
                }
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
        
        // Save GAP time without buttonId
        editor.putString("gapTime", gapTimeEditText.getText().toString());
        
        editor.apply();
    }

    private void loadButtonConfig(int buttonId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Load saved name, time, and coordinates
        String buttonName = preferences.getString("buttonName_" + buttonId, "");
        String buttonTime = preferences.getString("buttonTime_" + buttonId, "");
        clickX = preferences.getFloat("buttonX_" + buttonId, -1);
        clickY = preferences.getFloat("buttonY_" + buttonId, -1);

        // Load GAP time (without buttonId)
        String gapTime = preferences.getString("gapTime", "");

        nameEditText.setText(buttonName);
        timeEditText.setText(buttonTime);
        gapTimeEditText.setText(gapTime);
        
        if (clickX != -1 && clickY != -1) {
            coordinateTextView.setText(String.format(Locale.getDefault(), "X: %.2f, Y: %.2f", clickX, clickY));
        }
    }
}
