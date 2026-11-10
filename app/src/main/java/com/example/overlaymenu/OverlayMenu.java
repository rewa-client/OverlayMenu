package com.example.overlaymenu;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayMenu extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Разрешите «Поверх других приложений»",
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } else {
            startService(new Intent(this, MenuService.class));
            Toast.makeText(this, "Полоска снизу активна", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    public static class MenuService extends Service {

        private static final String PREFS = "menu_prefs";
        private static final String K_FUNC_A = "func_a";
        private static final String K_FUNC_B = "func_b";
        private static final String K_FUNC_C = "func_c";
        private static final String K_SLIDER = "slider_value";
        private static final String K_HANDLE_X = "handle_x";
        private static final String K_HANDLE_Y = "handle_y";

        private WindowManager wm;
        private View handleView;
        private View menuView;
        private boolean opened = false;
        private SharedPreferences prefs;

        @Override
        public void onCreate() {
            super.onCreate();
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

            startForeground(1, buildNotification());
            createHandle();
            createMenu();
        }

        // ---------- Полоска снизу ----------
        private void createHandle() {
            TextView handle = new TextView(this);
            handle.setText("▲  MENU  ▲");
            handle.setTextColor(Color.WHITE);
            handle.setTextSize(11);
            handle.setGravity(Gravity.CENTER);
            handle.setBackgroundColor(0xCC000000);
            handle.setPadding(0, 12, 0, 12);

            handleView = handle;

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            p.gravity = Gravity.BOTTOM;
            p.x = prefs.getInt(K_HANDLE_X, 0);
            p.y = prefs.getInt(K_HANDLE_Y, 0);

            handle.setOnClickListener(v -> toggleMenu());

            handle.setOnTouchListener(new View.OnTouchListener() {
                int startY;
                float touchY;
                boolean dragging;
                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    WindowManager.LayoutParams lp =
                            (WindowManager.LayoutParams) v.getLayoutParams();
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = lp.y;
                            touchY = e.getRawY();
                            dragging = false;
                            return false;
                        case MotionEvent.ACTION_MOVE:
                            float dy = touchY - e.getRawY();
                            if (Math.abs(dy) > 20) dragging = true;
                            if (dragging) {
                                lp.y = (int) Math.max(0, startY + dy);
                                wm.updateViewLayout(v, lp);
                                return true;
                            }
                            return false;
                        case MotionEvent.ACTION_UP:
                            if (!dragging) {
                                v.performClick();
                            } else {
                                prefs.edit()
                                        .putInt(K_HANDLE_X, lp.x)
                                        .putInt(K_HANDLE_Y, lp.y)
                                        .apply();
                            }
                            return true;
                    }
                    return false;
                }
            });

            wm.addView(handleView, p);
        }

        // ---------- Меню ----------
        private void createMenu() {
            ScrollView scroll = new ScrollView(this);
            scroll.setBackgroundColor(0xF21E1E1E);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(40, 30, 40, 30);
            scroll.addView(root);

            TextView title = new TextView(this);
            title.setText("STANDARD MENU");
            title.setTextColor(0xFF00FF00);
            title.setTextSize(20);
            title.setPadding(0, 0, 0, 20);
            root.addView(title);

            addButton(root, "Кнопка 1", () ->
                    Toast.makeText(this, "Кнопка 1 нажата", Toast.LENGTH_SHORT).show());
            addButton(root, "Кнопка 2", () ->
                    Toast.makeText(this, "Кнопка 2 нажата", Toast.LENGTH_SHORT).show());
            addButton(root, "Кнопка 3", () ->
                    Toast.makeText(this, "Кнопка 3 нажата", Toast.LENGTH_SHORT).show());

            addCheckbox(root, "Функция A", K_FUNC_A);
            addCheckbox(root, "Функция B", K_FUNC_B);
            addCheckbox(root, "Функция C", K_FUNC_C);

            int savedSlider = prefs.getInt(K_SLIDER, 5);

            TextView sliderLabel = new TextView(this);
            sliderLabel.setText("Значение: " + savedSlider);
            sliderLabel.setTextColor(Color.WHITE);
            sliderLabel.setPadding(0, 20, 0, 5);
            root.addView(sliderLabel);

            SeekBar bar = new SeekBar(this);
            bar.setMax(100);
            bar.setProgress(savedSlider);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    sliderLabel.setText("Значение: " + p);
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {
                    prefs.edit().putInt(K_SLIDER, sb.getProgress()).apply();
                }
            });
            root.addView(bar);

            addButton(root, "Сбросить настройки", () -> {
                prefs.edit().clear().apply();
                Toast.makeText(this, "Настройки сброшены", Toast.LENGTH_SHORT).show();
                stopSelf();
            });

            addButton(root, "Закрыть меню", () -> toggleMenu());

            menuView = scroll;
            menuView.setVisibility(View.GONE);

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    dp(280),
                    dp(420),
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
            p.gravity = Gravity.CENTER;

            wm.addView(menuView, p);
        }

        private void addButton(LinearLayout parent, String text, Runnable action) {
            Button b = new Button(this);
            b.setText(text);
            b.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = 12;
            parent.addView(b, lp);
            b.setOnClickListener(v -> action.run());
        }

        private void addCheckbox(LinearLayout parent, String text, String key) {
            CheckBox cb = new CheckBox(this);
            cb.setText(text);
            cb.setTextColor(Color.WHITE);
            cb.setPadding(0, 10, 0, 10);
            cb.setChecked(prefs.getBoolean(key, false));
            cb.setOnCheckedChangeListener((v, checked) ->
                    prefs.edit().putBoolean(key, checked).apply());
            parent.addView(cb);
        }

        private int dp(int v) {
            return (int) (v * getResources().getDisplayMetrics().density);
        }

        private void toggleMenu() {
            opened = !opened;
            menuView.setVisibility(opened ? View.VISIBLE : View.GONE);
        }

        private int overlayType() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
        }

        private Notification buildNotification() {
            String ch = "overlay_ch";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.createNotificationChannel(new NotificationChannel(ch,
                        "Overlay", NotificationManager.IMPORTANCE_LOW));
                return new Notification.Builder(this, ch)
                        .setContentTitle("Меню активно")
                        .setSmallIcon(android.R.drawable.ic_menu_manage)
                        .build();
            }
            return new Notification.Builder(this)
                    .setContentTitle("Меню активно")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .build();
        }

        @Override
        public IBinder onBind(Intent i) { return null; }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (handleView != null) wm.removeView(handleView);
            if (menuView != null) wm.removeView(menuView);
        }
    }
}
