package com.termux.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BatterySaverOverlay {

    private static final int TAP_THRESHOLD = 2;
    private static final long TAP_TIMEOUT_MS = 1000;

    private Activity activity;
    private View overlayView;
    private TextView hintTextView;
    private int tapCount = 0;
    private long lastTapTime = 0;
    private Handler tapResetHandler;
    private Runnable tapResetRunnable;
    private float originalBrightness = -1f;
    private boolean isActive = false;

    public BatterySaverOverlay(Activity activity) {
        this.activity = activity;
        this.tapResetHandler = new Handler(Looper.getMainLooper());
        createOverlay();
    }

    private void createOverlay() {
        FrameLayout overlayContainer = new FrameLayout(activity);
        overlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayContainer.setBackgroundColor(0xFF000000);
        overlayContainer.setVisibility(View.GONE);
        overlayContainer.setClickable(true);
        overlayContainer.setFocusable(true);

        hintTextView = new TextView(activity);
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        hintParams.gravity = Gravity.CENTER;
        hintTextView.setLayoutParams(hintParams);
        hintTextView.setText("Battery Saver Active\n\nDouble tap to unlock");
        hintTextView.setTextColor(Color.parseColor("#666666"));
        hintTextView.setTextSize(18);
        hintTextView.setGravity(Gravity.CENTER);
        hintTextView.setAlpha(0.0f);

        overlayContainer.addView(hintTextView);
        overlayView = overlayContainer;

        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                handleTap();
                return true;
            }
        });

        overlayView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    public void attachToActivity() {
        if (activity != null && overlayView.getParent() == null) {
            ViewGroup rootView = activity.findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.addView(overlayView);
            }
        }
    }

    public void detachFromActivity() {
        if (overlayView != null && overlayView.getParent() != null) {
            ((ViewGroup) overlayView.getParent()).removeView(overlayView);
        }
    }

    private void handleTap() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTapTime > TAP_TIMEOUT_MS) {
            tapCount = 0;
        }

        tapCount++;
        lastTapTime = currentTime;

        if (isActive) {
            showTapProgress();
        }

        if (tapResetRunnable != null) {
            tapResetHandler.removeCallbacks(tapResetRunnable);
        }

        tapResetRunnable = () -> {
            tapCount = 0;
            if (isActive && hintTextView != null) {
                hintTextView.animate().alpha(0.0f).setDuration(300).start();
            }
        };
        tapResetHandler.postDelayed(tapResetRunnable, TAP_TIMEOUT_MS);

        if (tapCount >= TAP_THRESHOLD) {
            tapCount = 0;
            if (isActive) {
                deactivate();
            } else {
                activate();
            }
        }
    }

    private void showTapProgress() {
        if (hintTextView == null) return;

        hintTextView.setText("Battery Saver Active\n\nTap " + tapCount + " of 2 to unlock");
        hintTextView.animate().alpha(0.8f).setDuration(150).start();
    }

    public void activate() {
        if (isActive || activity == null) return;

        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
        originalBrightness = layoutParams.screenBrightness;

        layoutParams.screenBrightness = 0.01f;
        activity.getWindow().setAttributes(layoutParams);

        if (overlayView != null) {
            overlayView.setVisibility(View.VISIBLE);
            overlayView.bringToFront();
        }

        if (hintTextView != null) {
            hintTextView.setText("Battery Saver Active\n\nDouble tap to unlock");
            hintTextView.postDelayed(() -> {
                hintTextView.animate().alpha(0.6f).setDuration(800).start();
            }, 500);
            hintTextView.postDelayed(() -> {
                hintTextView.animate().alpha(0.0f).setDuration(1000).start();
            }, 3000);
        }

        isActive = true;

        Toast.makeText(activity, "Battery saver ON - Double tap to unlock", Toast.LENGTH_LONG).show();
    }

    public void deactivate() {
        if (!isActive || activity == null) return;

        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
        layoutParams.screenBrightness = originalBrightness;
        activity.getWindow().setAttributes(layoutParams);

        if (overlayView != null) {
            overlayView.setVisibility(View.GONE);
        }

        if (hintTextView != null) {
            hintTextView.setAlpha(0.0f);
        }

        isActive = false;

        Toast.makeText(activity, "Battery saver OFF", Toast.LENGTH_SHORT).show();
    }

    public boolean isActive() {
        return isActive;
    }

    public void toggle() {
        if (isActive) {
            deactivate();
        } else {
            activate();
        }
    }

    public void cleanup() {
        if (tapResetRunnable != null) {
            tapResetHandler.removeCallbacks(tapResetRunnable);
        }
        detachFromActivity();
    }
}
