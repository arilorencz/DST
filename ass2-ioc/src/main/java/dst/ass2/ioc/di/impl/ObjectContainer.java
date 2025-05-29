package dst.ass2.ioc.di.impl;

import dst.ass2.ioc.di.*;
import dst.ass2.ioc.di.annotation.Component;
import dst.ass2.ioc.di.annotation.Initialize;
import dst.ass2.ioc.di.annotation.Property;
import dst.ass2.ioc.di.annotation.Inject;
import dst.ass2.ioc.di.annotation.Scope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectContainer implements IObjectContainer {

    private Properties properties;
    private final Map<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> locks = new ConcurrentHashMap<>();

    public ObjectContainer(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public <T> T getObject(Class<T> type) throws InjectionException {
        Component component = type.getAnnotation(Component.class);
        if (component == null) {
            throw new InvalidDeclarationException("Class " + type.getName() + " is not annotated with @Component");
        }

        if (component.scope() == Scope.SINGLETON) {
            return getSingleton(type);
        } else {
            return createAndInject(type);
        }
    }

    private <T> T getSingleton(Class<T> type) {
        Object existing = singletonCache.get(type);
        if (existing != null) {
            return (T) existing;
        }

        Object lock = locks.computeIfAbsent(type, k -> new Object());
        synchronized (lock) {
            existing = singletonCache.get(type);
            if (existing == null) {
                existing = createAndInject(type);
                singletonCache.put(type, existing);
            }
            return (T) existing;
        }
    }

    private <T> T createAndInject(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            injectFields(instance, type);
            invokeInitializeMethods(instance, type);

            return instance;

        } catch (NoSuchMethodException e) {
            throw new ObjectCreationException("No default constructor for " + type.getName(), e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ObjectCreationException("Failed to create instance of " + type.getName(), e);
        }
    }

    private void injectFields(Object instance, Class<?> type) {
        Map<String, String> propertySnapshot = new HashMap<>();
        for (Field field : getAllFields(type)) {
            field.setAccessible(true);

            //properties
            if (field.isAnnotationPresent(Property.class)) {
                Property prop = field.getAnnotation(Property.class);
                String key = prop.value();

                String rawValue = propertySnapshot.computeIfAbsent(key, k -> {
                    String v = properties.getProperty(k);
                    if (v == null) {
                        throw new ObjectCreationException("Missing required property: " + k);
                    }
                    return v;
                });

                try {
                    Object value = convertValue(field.getType(), rawValue);
                    field.set(instance, value);
                } catch (TypeConversionException e) {
                    throw e; // propagate as is
                } catch (Exception e) {
                    throw new ObjectCreationException("Failed to inject property: " + key, e);
                }
            }

            //injections
            if (field.isAnnotationPresent(Inject.class)) {
                Inject inject = field.getAnnotation(Inject.class);
                Class<?> injectType = inject.targetType() != Void.class ? inject.targetType() : field.getType();

                try {
                    Object value = getObject(injectType);
                    field.set(instance, value);
                } catch (InjectionException e) {
                    if (!inject.optional())
                        throw e;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    if (!inject.optional())
                        throw new InvalidDeclarationException("Injection failed for field: " + field.getName(), e);
                }
            }
        }
    }

    private void invokeInitializeMethods(Object instance, Class<?> type) {
        Set<String> invokedInitializers = new HashSet<>();

        for (Method method : getAllMethods(type)) {
            if (!method.isAnnotationPresent(Initialize.class) || invokedInitializers.contains(method.getName())) {
                continue;
            }

            if (method.getParameterCount() > 0) {
                throw new InvalidDeclarationException("@Initialize method must not have parameters: " + method.getName());
            }

            invokedInitializers.add(method.getName());
            method.setAccessible(true);

            try {
                method.invoke(instance);
            } catch (Exception e) {
                throw new ObjectCreationException("Failed to invoke @Initialize method: " + method.getName(), e);
            }
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        while (type != null && type != Object.class) {
            fields.addAll(Arrays.asList(type.getDeclaredFields()));
            type = type.getSuperclass();
        }
        return fields;
    }

    private List<Method> getAllMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        while (type != null && type != Object.class) {
            methods.addAll(Arrays.asList(type.getDeclaredMethods()));
            type = type.getSuperclass();
        }
        return methods;
    }

    private Object convertValue(Class<?> targetType, String value) {
        try {
            if (targetType == String.class) return value;
            if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
            if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
            if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
            if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
            if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
            if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(value);
            if (targetType == char.class || targetType == Character.class) {
                if (value.length() != 1) throw new IllegalArgumentException("Expected single character but got: " + value);
                return value.charAt(0);
            }
        } catch (Exception e) {
            throw new TypeConversionException("Cannot convert value: " + value + " to type: " + targetType, e);
        }

        throw new TypeConversionException("Unsupported field type: " + targetType);
    }
}
