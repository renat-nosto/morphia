package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Version;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.mapping.codec.pojo.FieldModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FieldDiscovery implements MorphiaConvention {
    private Datastore datastore;
    private EntityModelBuilder entityModelBuilder;

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        this.datastore = datastore;
        this.entityModelBuilder = builder;

        Map<String, Map<String, Type>> parameterization = builder.parameterization();
        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());
        for (Class<?> type : list) {
            processFields(type, parameterization);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String getMappedFieldName(FieldModelBuilder fieldBuilder) {
        MapperOptions options = datastore.getMapper().getOptions();
        if (fieldBuilder.hasAnnotation(Id.class)) {
            return "_id";
        } else if (fieldBuilder.hasAnnotation(Property.class)) {
            final Property mv = fieldBuilder.getAnnotation(Property.class);
            if (!mv.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mv.value();
            }
        } else if (fieldBuilder.hasAnnotation(Reference.class)) {
            final Reference mr = fieldBuilder.getAnnotation(Reference.class);
            if (!mr.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return mr.value();
            }
        } else if (fieldBuilder.hasAnnotation(Version.class)) {
            final Version me = fieldBuilder.getAnnotation(Version.class);
            if (!me.value().equals(Mapper.IGNORED_FIELDNAME)) {
                return me.value();
            }
        }

        return options.getFieldNaming().apply(fieldBuilder.name());
    }

    private void processFields(Class<?> currentClass, Map<String, Map<String, Type>> parameterization) {
        for (Field field : currentClass.getDeclaredFields()) {
            TypeData<?> typeData = TypeData.newInstance(field);

            Type genericType = field.getGenericType();
            if (genericType instanceof TypeVariable) {
                Map<String, Type> map = parameterization.get(currentClass.getName());
                if (map != null) {
                    Type mapped = map.get(((TypeVariable<?>) genericType).getName());
                    if (mapped instanceof Class) {
                        typeData = TypeData.newInstance(field.getGenericType(), (Class<?>) mapped);
                    }
                }
            }

            FieldModelBuilder fieldModelBuilder = FieldModel.builder()
                                                            .field(field)
                                                            .fieldName(field.getName())
                                                            .typeData(typeData)
                                                            .annotations(List.of(field.getDeclaredAnnotations()));
            fieldModelBuilder.mappedName(getMappedFieldName(fieldModelBuilder));

            entityModelBuilder.addModel(fieldModelBuilder);
        }
    }
}
