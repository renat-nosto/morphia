package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.ArrayFieldAccessor;
import dev.morphia.mapping.codec.FieldAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;
import org.bson.codecs.pojo.PropertyAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FieldDiscovery implements MorphiaConvention {

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        Map<String, Map<String, Type>> parameterization = builder.parameterization();
        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());

        for (Class<?> type : list) {
            for (Field field : type.getDeclaredFields()) {
                PropertyModelBuilder propertyModelBuilder = builder.addProperty();

                propertyModelBuilder
                    .name(field.getName())
                    .typeData(getTypeData(parameterization, type, field))
                    .annotations(List.of(field.getDeclaredAnnotations()))
                    .accessor(getAccessor(field, propertyModelBuilder))
                    .modifiers(field.getModifiers())
                    .mappedName(propertyModelBuilder.discoverMappedName(datastore.getMapper().getOptions()));
            }
        }
    }

    private PropertyAccessor<? super Object> getAccessor(Field field, PropertyModelBuilder property) {
        return field.getType().isArray() && !field.getType().getComponentType().equals(byte.class)
               ? new ArrayFieldAccessor(property.typeData(), field)
               : new FieldAccessor(field);
    }

    private TypeData<?> getTypeData(Map<String, Map<String, Type>> parameterization, Class<?> type, Field field) {
        TypeData<?> typeData = TypeData.newInstance(field);

        Type genericType = field.getGenericType();
        if (genericType instanceof TypeVariable) {
            Map<String, Type> map = parameterization.get(type.getName());
            if (map != null) {
                Type mapped = map.get(((TypeVariable<?>) genericType).getName());
                if (mapped instanceof Class) {
                    typeData = TypeData.newInstance(field.getGenericType(), (Class<?>) mapped);
                }
            }
        }
        return typeData;
    }

}
