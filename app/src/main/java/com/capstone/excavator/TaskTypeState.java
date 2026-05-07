package com.capstone.excavator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局任务类型状态单例。
 */
public final class TaskTypeState {

    public enum Type {
        NONE,
        LEVEL,
        DITCH,
        SLOPE
    }

    public interface OnTypeChangeListener {
        void onTypeChanged(@NonNull Type newType, @NonNull Type oldType);
    }

    private static final TaskTypeState INSTANCE = new TaskTypeState();

    private volatile Type current = Type.NONE;
    private final CopyOnWriteArrayList<OnTypeChangeListener> listeners = new CopyOnWriteArrayList<>();

    private TaskTypeState() {
    }

    public static TaskTypeState getInstance() {
        return INSTANCE;
    }

    @NonNull
    public Type getType() {
        return current;
    }

    public void setType(@NonNull Type newType) {
        Type old = current;
        if (old == newType) {
            return;
        }
        current = newType;
        for (OnTypeChangeListener listener : listeners) {
            listener.onTypeChanged(newType, old);
        }
    }

    public void addListener(@NonNull OnTypeChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(@Nullable OnTypeChangeListener listener) {
        listeners.remove(listener);
    }
}
