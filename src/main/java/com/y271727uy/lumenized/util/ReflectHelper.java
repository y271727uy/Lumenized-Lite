package com.y271727uy.lumenized.util;

import java.lang.reflect.Field;

public final class ReflectHelper {
    private ReflectHelper() {
    }

    public static Object getField(Object instance, String fieldName) {
        try {
            Field field = findField(instance.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read field '" + fieldName + "' from " + instance.getClass().getName(), e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
