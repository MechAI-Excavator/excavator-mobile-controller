package com.capstone.excavator;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * 浮动数字小键盘。
 *
 * <h3>使用方式</h3>
 * <pre>
 * NumpadView numpad = new NumpadView(context);
 *
 * // 绑定一个 input（EditText/TextView 均可），打开时会读取并记住初始值
 * // 编辑过程中会实时同步到 input；只有点「确认」才算提交；点外部关闭会回滚初始值
 * numpad.showFor(myEditText, anchorView, NumpadView.POSITION_BELOW);
 *
 * // 锚定到某个 View 旁边并展示
 * numpad.showAt(anchorView, NumpadView.POSITION_BELOW);
 *
 * // 也可以在别处主动关闭
 * numpad.dismiss();
 * </pre>
 *
 * <h3>支持的按键</h3>
 * 0-9，小数点（.），负号（−），退格（← / 清除），确认。
 */
public class NumpadView {

    /** 键盘出现在锚点视图的下方（默认）。 */
    public static final int POSITION_BELOW = 0;
    /** 键盘出现在锚点视图的上方。 */
    public static final int POSITION_ABOVE = 1;
    /** 键盘出现在锚点视图的右侧。 */
    public static final int POSITION_RIGHT = 2;
    /** 键盘出现在锚点视图的左侧。 */
    public static final int POSITION_LEFT  = 3;

    /** 点击「确认」时触发，传入当前输入的字符串。 */
    public interface OnConfirmListener {
        void onConfirm(String value);
    }

    /** 点击「清除」时触发（可选，用于额外的 UI 重置）。 */
    public interface OnClearListener {
        void onClear();
    }

    /** 键盘关闭时触发（点确认/点外部区域均会触发）。 */
    public interface OnDismissListener {
        void onDismiss();
    }

    private static final int MAX_LEN = 12;
    private static final int DEFAULT_GAP_PX = 4;

    private final Context context;
    private final View contentView;
    private final PopupWindow popup;

    private final StringBuilder buffer = new StringBuilder();

    private TextView boundInput;
    private String initialInputValue = "";
    private boolean confirmed = false;

    private OnConfirmListener onConfirmListener;
    private OnClearListener  onClearListener;
    private OnDismissListener onDismissListener;

    // ── Constructor ───────────────────────────────────────────────────────────

    public NumpadView(Context context) {
        this.context = context;
        contentView = LayoutInflater.from(context).inflate(R.layout.view_numpad, null, false);

        popup = new PopupWindow(
                contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true   // focusable → 点击外部自动关闭
        );
        popup.setElevation(24f);
        popup.setOutsideTouchable(true);
        popup.setOnDismissListener(() -> {
            // 没有点「确认」导致的关闭：回滚到初始值
            if (!confirmed) {
                rollbackBoundInput();
            }
            if (onDismissListener != null) onDismissListener.onDismiss();
        });

        bindKeys();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** 设置初始显示值（展示前调用）。 */
    public NumpadView setValue(String value) {
        buffer.setLength(0);
        if (value != null) buffer.append(value);
        syncToBoundInput();
        return this;
    }

    /** 绑定要实时同步的 input（可选：也可以直接用 showFor() 一步到位）。 */
    public NumpadView bindInput(TextView inputView) {
        beginEditing(inputView);
        return this;
    }

    /** 获取当前输入值字符串。 */
    public String getValue() {
        return buffer.toString();
    }

    public NumpadView setOnConfirmListener(OnConfirmListener l) {
        onConfirmListener = l;
        return this;
    }

    public NumpadView setOnClearListener(OnClearListener l) {
        onClearListener = l;
        return this;
    }

    public NumpadView setOnDismissListener(OnDismissListener l) {
        onDismissListener = l;
        return this;
    }

    /**
     * 绑定一个 input 并开启编辑会话：
     * - 记录 input 当前值作为「初始值」
     * - 输入过程中实时同步到 input
     * - 点「确认」才算正式提交
     * - 点外部关闭则回滚初始值
     */
    public void showFor(TextView inputView, View anchorView, int position) {
        showFor(inputView, anchorView, position, 0, 0);
    }

    public void showFor(TextView inputView, View anchorView, int position, int dxPx, int dyPx) {
        beginEditing(inputView);
        showAt(anchorView, position, dxPx, dyPx);
    }

    public void showFor(TextView inputView, View anchorView) {
        showFor(inputView, anchorView, POSITION_BELOW);
    }

    /**
     * 绑定 input，并在 window 绝对坐标 (x,y) 显示（px）。
     * 你可以自行调整 x/y，不依赖 anchor 的相对位置计算。
     */
    public void showForAtScreen(TextView inputView, View anyAttachedView, int xPx, int yPx) {
        beginEditing(inputView);
        confirmed = false;
        showAtScreen(anyAttachedView, xPx, yPx);
    }

    /**
     * 将键盘锚定到 anchorView 旁边并展示。
     *
     * @param anchorView 锚点 View
     * @param position   {@link #POSITION_BELOW} / {@link #POSITION_ABOVE} /
     *                   {@link #POSITION_RIGHT} / {@link #POSITION_LEFT}
     */
    public void showAt(View anchorView, int position) {
        showAt(anchorView, position, 0, 0);
    }

    /**
     * 将键盘锚定到 anchorView 旁边并展示，并附加偏移量（屏幕坐标 px）。
     *
     * @param anchorView 锚点 View
     * @param position   {@link #POSITION_BELOW}/{@link #POSITION_ABOVE}/{@link #POSITION_RIGHT}/{@link #POSITION_LEFT}
     * @param dxPx       在计算出的 x 基础上再偏移 dx（像素，右为正）
     * @param dyPx       在计算出的 y 基础上再偏移 dy（像素，下为正）
     */
    public void showAt(View anchorView, int position, int dxPx, int dyPx) {
        if (popup.isShowing()) return;
        confirmed = false;

        // 先测量弹窗内容大小
        contentView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int pw = contentView.getMeasuredWidth();
        int ph = contentView.getMeasuredHeight();

        int[] loc = new int[2];
        // 使用 window 坐标系，避免沉浸式/全屏下 screen 坐标与 PopupWindow 偏移不一致
        anchorView.getLocationInWindow(loc);
        int ax = loc[0];
        int ay = loc[1];
        int aw = anchorView.getWidth();
        int ah = anchorView.getHeight();

        int xOff, yOff;
        switch (position) {
            case POSITION_ABOVE:
                xOff = ax;
                yOff = ay - ph - DEFAULT_GAP_PX;
                break;
            case POSITION_RIGHT:
                xOff = ax + aw + DEFAULT_GAP_PX;
                yOff = ay;
                break;
            case POSITION_LEFT:
                xOff = ax - pw - DEFAULT_GAP_PX;
                yOff = ay;
                break;
            default: // BELOW
                xOff = ax;
                yOff = ay + ah + DEFAULT_GAP_PX;
                break;
        }

        // parent view 仅用于提供 windowToken；用 anchorView 本身更稳
        if (anchorView.getWindowToken() == null) {
            anchorView.post(() -> showAt(anchorView, position, dxPx, dyPx));
            return;
        }
        popup.showAtLocation(anchorView, Gravity.NO_GRAVITY, xOff + dxPx, yOff + dyPx);
    }

    /**
     * 锚定到 anchorView 下方（便捷重载）。
     */
    public void showAt(View anchorView) {
        showAt(anchorView, POSITION_BELOW);
    }

    /**
     * 直接在 window 绝对坐标系指定位置显示（px）。
     * parent view 只用于提供 windowToken；无需参与定位计算。
     */
    public void showAtScreen(View anyAttachedView, int xPx, int yPx) {
        if (popup.isShowing()) return;
        confirmed = false;
        if (anyAttachedView.getWindowToken() == null) {
            anyAttachedView.post(() -> showAtScreen(anyAttachedView, xPx, yPx));
            return;
        }
        popup.showAtLocation(anyAttachedView, Gravity.NO_GRAVITY, xPx, yPx);
    }

    /** 关闭键盘。 */
    public void dismiss() {
        if (popup.isShowing()) popup.dismiss();
    }

    /** 是否正在显示。 */
    public boolean isShowing() {
        return popup.isShowing();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void bindKeys() {
        int[] digitIds = {
                R.id.key1, R.id.key2, R.id.key3,
                R.id.key4, R.id.key5, R.id.key6,
                R.id.key7, R.id.key8, R.id.key9,
                R.id.key0
        };
        String[] digits = {"1","2","3","4","5","6","7","8","9","0"};

        for (int i = 0; i < digitIds.length; i++) {
            final String d = digits[i];
            contentView.findViewById(digitIds[i]).setOnClickListener(v -> appendChar(d));
        }

        contentView.findViewById(R.id.keyDot).setOnClickListener(v -> appendChar("."));
        contentView.findViewById(R.id.keyMinus).setOnClickListener(v -> toggleMinus());
        contentView.findViewById(R.id.keyClear).setOnClickListener(v -> onClear());
        contentView.findViewById(R.id.keyConfirm).setOnClickListener(v -> onConfirm());
    }

    private void appendChar(String c) {
        if (buffer.length() >= MAX_LEN) return;
        // 小数点只允许出现一次
        if (c.equals(".") && buffer.indexOf(".") >= 0) return;
        // 负号只允许在开头，且只有一个
        if (buffer.length() == 0 && c.equals(".")) {
            buffer.append("0");
        }
        buffer.append(c);
        syncToBoundInput();
    }

    /** 在开头加/去 负号（代替单独的 "−" 键）。 */
    private void toggleMinus() {
        if (buffer.length() > 0 && buffer.charAt(0) == '-') {
            buffer.deleteCharAt(0);
        } else {
            buffer.insert(0, '-');
        }
        syncToBoundInput();
    }

    private void onClear() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
        }
        syncToBoundInput();
        if (onClearListener != null) onClearListener.onClear();
    }

    private void onConfirm() {
        confirmed = true;
        String value = normalizeValueForCommit(buffer.toString());
        buffer.setLength(0);
        buffer.append(value);

        if (boundInput != null) boundInput.setText(value);
        if (onConfirmListener != null) onConfirmListener.onConfirm(value);
        dismiss();
    }

    private void beginEditing(TextView inputView) {
        boundInput = inputView;
        initialInputValue = (inputView.getText() == null) ? "" : inputView.getText().toString();
        buffer.setLength(0);
        buffer.append(initialInputValue);
        syncToBoundInput(); // requirement: 打开后也把当前值同步一遍
    }

    private void rollbackBoundInput() {
        if (boundInput != null) {
            boundInput.setText(initialInputValue);
        }
    }

    private void syncToBoundInput() {
        if (boundInput == null) return;
        // 编辑过程中不强行规范化（例如允许临时出现 "-" 或空串），只做实时展示
        boundInput.setText(buffer.toString());
    }

    private static String normalizeValueForCommit(String raw) {
        if (raw == null) return "0";
        String v = raw.trim();
        if (v.isEmpty() || v.equals("-") || v.equals(".")) return "0";
        if (v.equals("-.")) return "0";
        // "-0." / "0." 这种提交时允许保留小数点也可以，这里先不做额外裁剪
        return v;
    }
}
