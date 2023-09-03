/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import java.lang.reflect.*;
import java.util.Arrays;


//Credits: https://github.com/dmulloy2/ProtocolLib/blob/master/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/Reflection.java
/**
 * An utility class that simplifies reflection in Bukkit plugins.
 *
 * @author Kristian
 */
public final class ReflectUtils {
    /**
     * An interface for invoking a specific constructor.
     */
    public interface ConstructorInvoker {
        /**
         * Invoke a constructor for a specific class.
         *
         * @param arguments - the arguments to pass to the constructor.
         * @return The constructed object.
         */
        public Object invoke(Object... arguments);
    }

    /**
     * An interface for invoking a specific method.
     */
    public interface MethodInvoker {
        /**
         * Invoke a method on a specific target object.
         *
         * @param target - the target object, or NULL for a static method.
         * @param arguments - the arguments to pass to the method.
         * @return The return value, or NULL if is void.
         */
        public Object invoke(Object target, Object... arguments);
    }

    /**
     * An interface for retrieving the field content.
     *
     * @param <T> - field type.
     */
    public interface FieldAccessor<T> {
        /**
         * Retrieve the content of a field.
         *
         * @param target - the target object, or NULL for a static field.
         * @return The value of the field.
         */
        public T get(Object target);

        /**
         * Set the content of a field.
         *
         * @param target - the target object, or NULL for a static field.
         * @param value - the new value of the field.
         */
        public void set(Object target, Object value);

        /**
         * Determine if the given object has this field.
         *
         * @param target - the object to test.
         * @return TRUE if it does, FALSE otherwise.
         */
        public boolean hasField(Object target);
    }

    private ReflectUtils() {
        // Seal class
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target - the target type.
     * @param name - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class
     * @param name - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(String className, String name, Class<T> fieldType) {
        return getField(getClass(className), name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target - the target type.
     * @param fieldType - a compatible field type.
     * @param index - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(Class<?> target, Class<T> fieldType, int index) {
        return getField(target, null, fieldType, index);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class
     * @param fieldType - a compatible field type.
     * @param index - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static <T> FieldAccessor<T> getField(String className, Class<T> fieldType, int index) {
        return getField(getClass(className), fieldType, index);
    }

    // Common method
    private static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int index) {
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && fieldType.isAssignableFrom(field.getType()) && index-- <= 0) {
                field.setAccessible(true);

                // A function for retrieving a specific field value
                return new FieldAccessor<T>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    public T get(Object target) {
                        try {
                            return (T) field.get(target);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public void set(Object target, Object value) {
                        try {
                            field.set(target, value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }

                    @Override
                    public boolean hasField(Object target) {
                        // target instanceof DeclaringClass
                        return field.getDeclaringClass().isAssignableFrom(target.getClass());
                    }
                };
            }
        }

        // Search in parent classes
        if (target.getSuperclass() != null)
            return getField(target.getSuperclass(), name, fieldType, index);

        throw new IllegalArgumentException("Cannot find field with type " + fieldType);
    }

    /**
     * Retrieves a field with a given type and parameters. This is most useful
     * when dealing with Collections.
     *
     * @param target the target class.
     * @param fieldType Type of the field
     * @param params Variable length array of type parameters
     * @return The field
     *
     * @throws IllegalArgumentException If the field cannot be found
     */
    public static Field getParameterizedField(Class<?> target, Class<?> fieldType, Class<?>... params) {
        for (Field field : target.getDeclaredFields()) {
            if (field.getType().equals(fieldType)) {
                Type type = field.getGenericType();
                if (type instanceof ParameterizedType) {
                    if (Arrays.equals(((ParameterizedType) type).getActualTypeArguments(), params))
                        return field;
                }
            }
        }

        throw new IllegalArgumentException("Unable to find a field with type " + fieldType + " and params " + Arrays.toString(params));
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param className - lookup name of the class
     * @param methodName - the method name, or NULL to skip.
     * @param params - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getMethod(String className, String methodName, Class<?>... params) {
        return getTypedMethod(getClass(className), methodName, null, params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param params - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        return getTypedMethod(clazz, methodName, null, params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param returnType - the expected return type, or NULL to ignore.
     * @param params - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static MethodInvoker getTypedMethod(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... params) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if ((methodName == null || method.getName().equals(methodName))
                && (returnType == null || method.getReturnType().equals(returnType))
                && Arrays.equals(method.getParameterTypes(), params)) {
                method.setAccessible(true);

                return new MethodInvoker() {

                    @Override
                    public Object invoke(Object target, Object... arguments) {
                        try {
                            return method.invoke(target, arguments);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot invoke method " + method, e);
                        }
                    }

                };
            }
        }

        // Search in every superclass
        if (clazz.getSuperclass() != null)
            return getMethod(clazz.getSuperclass(), methodName, params);

        throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
    }

    /**
     * Search for the first publically and privately defined constructor of the given name and parameter count.
     *
     * @param className - lookup name of the class
     * @param params - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static ConstructorInvoker getConstructor(String className, Class<?>... params) {
        return getConstructor(getClass(className), params);
    }

    /**
     * Search for the first publically and privately defined constructor of the given name and parameter count.
     *
     * @param clazz - a class to start with.
     * @param params - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static ConstructorInvoker getConstructor(Class<?> clazz, Class<?>... params) {
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), params)) {
                constructor.setAccessible(true);

                return new ConstructorInvoker() {

                    @Override
                    public Object invoke(Object... arguments) {
                        try {
                            return constructor.newInstance(arguments);
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                        }
                    }

                };
            }
        }

        throw new IllegalStateException(String.format("Unable to find constructor for %s (%s).", clazz, Arrays.asList(params)));
    }

    /**
     * Retrieve a class from its full name, without knowing its type on compile time.
     * <p>
     * This is useful when looking up fields by a NMS or OBC type.
     * <p>
     *
     * @see {@link #getClass()} for more information.
     * @param lookupName - the class name with variables.
     * @return The class.
     */
    public static Class<Object> getUntypedClass(String lookupName) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Class<Object> clazz = (Class) getClass(lookupName);
        return clazz;
    }

    /**
     * Retrieve a class from its full name with alternatives, without knowing its type on compile time.
     * <p>
     * This is useful when looking up fields by a NMS or OBC type.
     * <p>
     *
     * @see {@link #getClass()} for more information.
     * @param lookupName - the class name with variables.
     * @param aliases - alternative names for this class.
     * @return The class.
     */
    public static Class<Object> getUntypedClass(String lookupName, String... aliases) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Class<Object> clazz = (Class) getClass(lookupName, aliases);
        return clazz;
    }


    /**
     * Retrieve the first class that matches the full class name.
     * <p>
     * Strings enclosed with curly brackets - such as {TEXT} - will be replaced according to the following table:
     * <p>
     * <table border="1">
     * <tr>
     * <th>Variable</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td>{nms}</td>
     * <td>Actual package name of net.minecraft.server.VERSION</td>
     * </tr>
     * <tr>
     * <td>{obc}</td>
     * <td>Actual pacakge name of org.bukkit.craftbukkit.VERSION</td>
     * </tr>
     * <tr>
     * <td>{version}</td>
     * <td>The current Minecraft package VERSION, if any.</td>
     * </tr>
     * </table>
     *
     * @param lookupName - the class name with variables.
     * @param aliases - alternative names for this class.
     * @return Class object.
     * @throws RuntimeException If we are unable to find any of the given classes.
     */
    public static Class<?> getClass(String lookupName, String... aliases) {
        try {
            // Try the main class first
            return getClass(lookupName);
        } catch (RuntimeException e) {
            Class<?> success = null;

            // Try every alias too
            for (String alias : aliases) {
                try {
                    success = getClass(alias);
                    break;
                } catch (RuntimeException e1) {
                    // e1.printStackTrace();
                }
            }

            if (success != null) {
                return success;
            } else {
                // Hack failed
                throw new RuntimeException(String.format("Unable to find %s (%s)", lookupName, String.join(",", aliases)));
            }
        }
    }


    /**
     * Retrieve a class by its canonical name.
     *
     * @param canonicalName - the canonical name.
     * @return The class.
     */
    private static Class<?> getCanonicalClass(String canonicalName) {
        try {
            return Class.forName(canonicalName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find " + canonicalName, e);
        }
    }


}