package com.spaghetti.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * ThreadUtil is a namespace for common useful thread functions
 *
 * @author bohdloss
 *
 */
public final class ReflectionUtil {

	private ReflectionUtil() {
	}

	/**
	 * Obtains a private field with the given {@code name} from the given
	 * {@code cls} and if not found, the superclass of {@code cls} will be searched,
	 * and so on iteratively.<br>
	 * Once the field is found, {@code private/protected} and {@code final}
	 * restrictions are removed from it, then it is returned
	 * <p>
	 * This method will throw a RuntimeException if the field couldn't be obtained
	 * because of some exception, or if the field does not exist
	 *
	 * @param cls  The class to start searching for the field
	 * @param name The name of the field to search for
	 * @return The Field
	 */
	public static Field getPrivateField(Class<?> cls, String name) {
		try {
			Field result = null;
			while (result == null) {
				try {
					// Remove private restrictions
					result = cls.getDeclaredField(name);
					result.setAccessible(true);
				} catch (NoSuchFieldException nofield) {
					cls = cls.getSuperclass();
				}
			}
			return result;
		} catch (Throwable t) {
			throw new RuntimeException("Couldn't obtain field " + cls.getName() + "." + name, t);
		}
	}

	/**
	 * Obtains the value of a private field, making it accessible if it is private.
	 * <p>
	 * If an exception is thrown, it is converted into a {@link RuntimeException} so
	 * the caller doesn't need to explicitly catch it
	 *
	 * @param field The field to read from
	 * @param obj   The object to read from
	 * @return The value of the field
	 */
	public static Object readField(Field field, Object obj) {
		try {
			field.setAccessible(true);
			return field.get(obj);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * Writes the value of a private field, making it accessible if it is private
	 * and removing the {@code final} modifier
	 * <p>
	 * If an exception is thrown, it is converted into a {@link RuntimeException} so
	 * the caller doesn't need to explicitly catch it
	 *
	 * @param field The field to write to
	 * @param obj   The object to write to
	 * @param value The value to write into the field
	 */
	public static void writeField(Field field, Object obj, Object value) {
		try {
			// Remove private restrictions
			field.setAccessible(true);

			// Write
			field.set(obj, value);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * Obtains a private method with the given {@code name} and {@code arguments}
	 * vararg from the given {@code cls} and if not found, the superclass of
	 * {@code cls} will be searched, and so on iteratively.<br>
	 * Once the method is found, {@code private/protected} restrictions are removed
	 * from it, then it is returned
	 * <p>
	 * This method will throw a RuntimeException if the field couldn't be obtained
	 * because of some exception, or if the field does not exist
	 *
	 * @param cls       The class to start searching for the method
	 * @param name      The name of the method to search for
	 * @param arguments The argument types the method accepts
	 * @return The Method
	 */
	public static Method getPrivateMethod(Class<?> cls, String name, Class<?>... arguments) {
		try {
			Method result = null;
			while (result == null) {
				try {
					// Remove private restrictions
					result = cls.getDeclaredMethod(name, arguments);
					result.setAccessible(true);
				} catch (NoSuchMethodException nofield) {
					cls = cls.getSuperclass();
				}
			}
			return result;
		} catch (Throwable t) {
			throw new RuntimeException("Couldn't obtain method " + cls.getName() + "." + name, t);
		}
	}

}
