package dev.morphia.mapping.codec;

import dev.morphia.mapping.MappingException;

import java.lang.reflect.Method;

public class PropertyAccessor implements org.bson.codecs.pojo.PropertyAccessor<Object> {
    private final Method getter;
    private final Method setter;

    public PropertyAccessor(Method getter, Method setter) {
        this.getter = getter;
        this.setter = setter;

        getter.setAccessible(true);
        setter.setAccessible(true);
    }

    @Override
    public <S> Object get(S instance) {
        try {
            return getter.invoke(instance);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }

    @Override
    public <S> void set(S instance, Object value) {
        try {
            setter.invoke(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new MappingException(e.getMessage(), e);
        }
    }
}
