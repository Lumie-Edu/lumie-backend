package com.lumie.common.test;

import java.lang.reflect.Field;

/**
 * Shared test utility for setting private/package-private fields via reflection.
 *
 * <p>Walks the full superclass hierarchy so that fields declared in a parent class
 * (e.g. {@code BaseEntity#id}) are reachable from a concrete subclass instance.
 *
 * <p>Usage:
 * <pre>
 *   ReflectionTestUtils.setField(entity, "id", 1L);
 * </pre>
 */
public final class ReflectionTestUtils {

    private ReflectionTestUtils() {
    }

    /**
     * Sets {@code fieldName} on {@code target} to {@code value}, walking the
     * entire superclass hierarchy until the field is found.
     *
     * @throws NoSuchFieldException if no field with that name exists anywhere in the hierarchy
     * @throws IllegalAccessException if the field cannot be made accessible (should not happen in tests)
     */
    public static void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                fieldName + " not found in hierarchy of " + target.getClass().getName());
    }
}
