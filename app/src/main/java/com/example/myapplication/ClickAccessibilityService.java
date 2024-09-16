package com.example.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;

public class ClickAccessibilityService extends AccessibilityService {

    private static ClickAccessibilityService instance;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public static ClickAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed for this use case
    }

    @Override
    public void onInterrupt() {
        // Not needed for this use case
    }

    // Method to perform click at a specified location (x, y)
    public void performClick(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 1));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    public void scheduleClickAtTime(int x, int y, long delayInMillis) {
        handler.postDelayed(() -> performClick(x, y), delayInMillis);
    }

    // Stop the handler and remove any pending callbacks
    public void cancelScheduledClick() {
        handler.removeCallbacksAndMessages(null);
    }
}
