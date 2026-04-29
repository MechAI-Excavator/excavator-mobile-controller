package com.capstone.excavator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Reusable tooltip popup that points to a given anchor view.
 */
public final class HelpTooltip {

    private final Activity activity;
    private final String message;

    private PopupWindow popup;

    public HelpTooltip(Activity activity, String message) {
        this.activity = activity;
        this.message = message;
    }

    public void attach(View anchor) {
        if (anchor == null) return;
        anchor.setClickable(true);
        anchor.setOnClickListener(v -> toggle(anchor));
    }

    public void toggle(View anchor) {
        if (popup != null && popup.isShowing()) {
            dismiss();
        } else {
            show(anchor);
        }
    }

    public void dismiss() {
        if (popup != null) {
            popup.dismiss();
            popup = null;
        }
    }

    private void show(View anchor) {
        View content = LayoutInflater.from(activity).inflate(R.layout.view_level_help_tooltip, null, false);
        ImageView close = content.findViewById(R.id.tooltipClose);
        ImageView arrow = content.findViewById(R.id.tooltipArrow);
        FrameLayout arrowContainer = content.findViewById(R.id.tooltipArrowContainer);
        TextView tv = content.findViewById(R.id.tooltipText);

        if (tv != null && message != null) {
            tv.setText(message);
        }

        PopupWindow pw = new PopupWindow(
                content,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setTouchable(true);

        if (close != null) close.setOnClickListener(v -> dismiss());

        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int anchorX = loc[0];
        int anchorY = loc[1];
        int anchorW = anchor.getWidth();
        int anchorH = anchor.getHeight();

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int margin = dp(10);
        int popupY = anchorY + anchorH + dp(8);

        content.measure(
                View.MeasureSpec.makeMeasureSpec(screenW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.AT_MOST)
        );
        int popupW = content.getMeasuredWidth();

        int anchorCenterX = anchorX + anchorW / 2;
        int x = anchorCenterX - popupW / 2;
        if (x < margin) x = margin;
        if (x + popupW > screenW - margin) x = screenW - margin - popupW;

        final int popupX = x;
        final int anchorCenterXFinal = anchorCenterX;

        pw.showAtLocation(activity.getWindow().getDecorView(), Gravity.NO_GRAVITY, popupX, popupY);
        popup = pw;

        content.post(() -> {
            if (arrow == null || arrowContainer == null) return;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) arrow.getLayoutParams();
            lp.gravity = Gravity.END | Gravity.TOP;
            lp.leftMargin = 0;
            lp.rightMargin = dp(40);
            arrow.setLayoutParams(lp);
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                activity.getResources().getDisplayMetrics()
        );
    }
}

