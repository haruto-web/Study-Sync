package com.example.studysync_project.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Adds a compact timer widget to each activity while a timer is active.
 */
public class GlobalTimerWidgetLifecycle implements Application.ActivityLifecycleCallbacks {

    private static final String PREFS_NAME = "global_timer_widget_state";
    private static final String KEY_POS_X = "position_x";
    private static final String KEY_POS_Y = "position_y";
    private static final String KEY_COLLAPSED = "collapsed";
    private static final float POSITION_UNSET = -1f;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Activity, Holder> holders = new WeakHashMap<>();

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (shouldSkipActivity(activity)) {
            return;
        }
        ensureWidget(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // no-op
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (shouldSkipActivity(activity)) {
            return;
        }
        ensureWidget(activity);
        startWidgetUpdates(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        stopWidgetUpdates(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // no-op
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // no-op
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        stopWidgetUpdates(activity);
        removeWidget(activity);
    }

    private void ensureWidget(Activity activity) {
        if (holders.containsKey(activity)) {
            return;
        }

        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof FrameLayout)) {
            return;
        }

        FrameLayout root = (FrameLayout) decor;
        View widget = LayoutInflater.from(activity).inflate(R.layout.view_global_timer_widget, root, false);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        widget.setLayoutParams(params);
        widget.setVisibility(View.GONE);

        root.addView(widget);

        Holder holder = new Holder();
        holder.root = widget;
        holder.touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
        holder.contentCard = widget.findViewById(R.id.card_global_timer_widget_content);
        holder.toggleButton = widget.findViewById(R.id.btn_global_timer_toggle);
        holder.timerValue = widget.findViewById(R.id.tv_global_timer_value);
        holder.moduleLabel = widget.findViewById(R.id.tv_global_timer_module);
        holder.progress = widget.findViewById(R.id.progress_global_timer);
        holder.collapsed = prefs(activity).getBoolean(KEY_COLLAPSED, false);
        holders.put(activity, holder);

        applyCollapsedState(activity, holder, false);
        attachDragAndTap(activity, holder, holder.contentCard, () -> openTimerScreen(activity));
        attachDragAndTap(activity, holder, holder.toggleButton, () -> {
            holder.collapsed = !holder.collapsed;
            applyCollapsedState(activity, holder, true);
        });

        widget.post(() -> applyInitialPosition(activity, holder));
    }

    private void removeWidget(Activity activity) {
        Holder holder = holders.remove(activity);
        if (holder == null || holder.root == null) {
            return;
        }

        ViewParent parent = holder.root.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(holder.root);
        }
    }

    private void startWidgetUpdates(Activity activity) {
        Holder holder = holders.get(activity);
        if (holder == null) {
            return;
        }

        stopWidgetUpdates(activity);

        holder.updater = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || holder.root == null) {
                    return;
                }

                bindTimerState(activity, holder);
                mainHandler.postDelayed(this, 1000L);
            }
        };

        mainHandler.post(holder.updater);
    }

    private void stopWidgetUpdates(Activity activity) {
        Holder holder = holders.get(activity);
        if (holder == null || holder.updater == null) {
            return;
        }

        mainHandler.removeCallbacks(holder.updater);
        holder.updater = null;
    }

    private void bindTimerState(Activity activity, Holder holder) {
        FocusTimerSessionStore.Snapshot state = FocusTimerSessionStore.getSnapshot(activity);
        if (!state.active) {
            holder.wasActive = false;
            holder.root.setVisibility(View.GONE);
            return;
        }

        if (!holder.wasActive) {
            holder.wasActive = true;

            // Always reveal the full widget when a new active session starts.
            if (holder.collapsed) {
                holder.collapsed = false;
                applyCollapsedState(activity, holder, true);
            }

            setWidgetPosition(activity, holder, holder.root.getX(), holder.root.getY(), false);
        }

        long now = System.currentTimeMillis();
        long left = FocusTimerSessionStore.getDisplayMillisLeft(state, now);
        int progress = FocusTimerSessionStore.getDisplayCompletionPercent(state, now);

        holder.timerValue.setText(FocusTimerSessionStore.formatTime(left));
        holder.progress.setProgress(progress);

        String runState = state.running
                ? activity.getString(R.string.global_timer_widget_running)
                : activity.getString(R.string.global_timer_widget_paused);
        holder.moduleLabel.setText(state.getDisplayModuleTitle() + " • " + runState);
        holder.root.setVisibility(View.VISIBLE);
    }

    private void attachDragAndTap(Activity activity, Holder holder, View touchView, Runnable tapAction) {
        if (touchView == null) {
            return;
        }

        touchView.setOnTouchListener((v, event) -> handleDragTouch(activity, holder, event, tapAction));
    }

    private boolean handleDragTouch(Activity activity, Holder holder, MotionEvent event, Runnable tapAction) {
        if (holder == null || holder.root == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                holder.downRawX = event.getRawX();
                holder.downRawY = event.getRawY();
                holder.startX = holder.root.getX();
                holder.startY = holder.root.getY();
                holder.dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - holder.downRawX;
                float dy = event.getRawY() - holder.downRawY;

                if (!holder.dragging
                        && (Math.abs(dx) > holder.touchSlop || Math.abs(dy) > holder.touchSlop)) {
                    holder.dragging = true;
                }

                if (holder.dragging) {
                    setWidgetPosition(activity, holder, holder.startX + dx, holder.startY + dy, false);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!holder.dragging && tapAction != null) {
                    tapAction.run();
                }
                setWidgetPosition(activity, holder, holder.root.getX(), holder.root.getY(), true);
                holder.dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                setWidgetPosition(activity, holder, holder.root.getX(), holder.root.getY(), true);
                holder.dragging = false;
                return true;
            default:
                return false;
        }
    }

    private void applyInitialPosition(Activity activity, Holder holder) {
        if (holder == null || holder.root == null) {
            return;
        }

        ViewParent parentObj = holder.root.getParent();
        if (!(parentObj instanceof View)) {
            return;
        }

        View parent = (View) parentObj;
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0 || holder.root.getWidth() <= 0 || holder.root.getHeight() <= 0) {
            holder.root.post(() -> applyInitialPosition(activity, holder));
            return;
        }

        SharedPreferences prefs = prefs(activity);
        float savedX = prefs.getFloat(KEY_POS_X, POSITION_UNSET);
        float savedY = prefs.getFloat(KEY_POS_Y, POSITION_UNSET);

        if (savedX == POSITION_UNSET || savedY == POSITION_UNSET) {
            float defaultX = Math.max(0f, parent.getWidth() - holder.root.getWidth() - dp(activity, 14));
            float defaultY = Math.max(0f, parent.getHeight() - holder.root.getHeight() - dp(activity, 84));
            setWidgetPosition(activity, holder, defaultX, defaultY, true);
            return;
        }

        setWidgetPosition(activity, holder, savedX, savedY, false);
    }

    private void setWidgetPosition(Activity activity, Holder holder, float desiredX, float desiredY, boolean persist) {
        if (holder == null || holder.root == null) {
            return;
        }

        float x = desiredX;
        float y = desiredY;

        ViewParent parentObj = holder.root.getParent();
        if (parentObj instanceof View) {
            View parent = (View) parentObj;
            int parentWidth = parent.getWidth();
            int parentHeight = parent.getHeight();
            int widgetWidth = holder.root.getWidth();
            int widgetHeight = holder.root.getHeight();

            if (parentWidth > 0 && widgetWidth > 0) {
                float maxX = Math.max(0f, parentWidth - widgetWidth);
                x = Math.max(0f, Math.min(desiredX, maxX));
            }
            if (parentHeight > 0 && widgetHeight > 0) {
                float maxY = Math.max(0f, parentHeight - widgetHeight);
                y = Math.max(0f, Math.min(desiredY, maxY));
            }
        }

        holder.root.setX(x);
        holder.root.setY(y);

        if (persist) {
            prefs(activity)
                    .edit()
                    .putFloat(KEY_POS_X, x)
                    .putFloat(KEY_POS_Y, y)
                    .apply();
        }
    }

    private void applyCollapsedState(Activity activity, Holder holder, boolean persist) {
        if (holder == null || holder.contentCard == null || holder.toggleButton == null) {
            return;
        }

        holder.contentCard.setVisibility(holder.collapsed ? View.GONE : View.VISIBLE);
        holder.toggleButton.setText(holder.collapsed ? ">" : "<");
        holder.toggleButton.setContentDescription(activity.getString(
                holder.collapsed ? R.string.global_timer_widget_show : R.string.global_timer_widget_hide
        ));

        if (persist) {
            prefs(activity).edit().putBoolean(KEY_COLLAPSED, holder.collapsed).apply();
        }

        if (holder.root != null) {
            holder.root.post(() -> setWidgetPosition(activity, holder, holder.root.getX(), holder.root.getY(), true));
        }
    }

    private void openTimerScreen(Activity activity) {
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).navigateTo(R.id.timerFragment);
            return;
        }

        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB_ID, R.id.timerFragment);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private boolean shouldSkipActivity(Activity activity) {
        if (activity == null) {
            return true;
        }

        String name = activity.getClass().getSimpleName();
        return "SplashActivity".equals(name)
                || "LoginActivity".equals(name)
                || "RegisterActivity".equals(name)
                || "VerifyEmailActivity".equals(name)
                || "TermsAndConditionsActivity".equals(name)
                || "OnboardingActivity".equals(name);
    }

    private static int dp(Activity activity, int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private SharedPreferences prefs(Activity activity) {
        return activity.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static final class Holder {
        View root;
        MaterialCardView contentCard;
        MaterialButton toggleButton;
        TextView timerValue;
        TextView moduleLabel;
        LinearProgressIndicator progress;
        Runnable updater;
        boolean collapsed;
        boolean wasActive;
        boolean dragging;
        int touchSlop;
        float downRawX;
        float downRawY;
        float startX;
        float startY;
    }
}
