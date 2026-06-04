package com.y271727uy.lumenized.client.postprocessing;

import com.mojang.blaze3d.shaders.BlendMode;
import com.y271727uy.lumenized.LumenizedConstants;

import java.lang.reflect.Field;

final class BlendModeState {
    private static final Field LAST_APPLIED_FIELD;

    static {
        Field field = null;
        try {
            field = BlendMode.class.getDeclaredField("lastApplied");
            field.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            LumenizedConstants.LOGGER.error("Failed to access BlendMode.lastApplied", e);
        }
        LAST_APPLIED_FIELD = field;
    }

    private BlendModeState() {
    }

    static BlendMode getLastApplied() {
        if (LAST_APPLIED_FIELD == null) {
            return null;
        }
        try {
            return (BlendMode) LAST_APPLIED_FIELD.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read BlendMode.lastApplied", e);
        }
    }

    static void setLastApplied(BlendMode mode) {
        if (LAST_APPLIED_FIELD == null) {
            return;
        }
        try {
            LAST_APPLIED_FIELD.set(null, mode);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to write BlendMode.lastApplied", e);
        }
    }
}
