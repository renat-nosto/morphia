package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.PropertyAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static dev.morphia.mapping.conventions.FieldDiscovery.getMappedFieldName;

public class MethodDiscovery implements MorphiaConvention {
    private EntityModelBuilder entityModelBuilder;
    private Datastore datastore;

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        this.datastore = datastore;
        this.entityModelBuilder = builder;

        Map<String, Map<String, Type>> parameterization = builder.parameterization();
        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());
        for (Class<?> type : list) {
            processMethods(type, parameterization);
        }

    }

    private void processMethods(Class<?> type, Map<String, Map<String, Type>> parameterization) {

        Map<String, List<Method>> properties = Arrays.stream(type.getDeclaredMethods())
                                                     .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("set") ||
                                                                  m.getName().startsWith("is"))
                                                     .collect(Collectors.groupingBy(m -> m.getName().startsWith("get")
                                                                                         || m.getName().startsWith("set")
                                                                                         ? stripPrefix(m, 3)
                                                                                         : stripPrefix(m, 2)));

        for (Entry<String, List<Method>> entry : properties.entrySet()) {
            String name = entry.getKey();
            List<Method> methods = entry.getValue();
            if (methods.size() == 2) {
                Method getter;
                Method setter;
                if (methods.get(0).getName().startsWith("set")) {
                    setter = methods.get(0);
                    getter = methods.get(1);
                } else {
                    getter = methods.get(0);
                    setter = methods.get(1);
                }
                TypeData<?> typeData = TypeData.newInstance(getter);

                PropertyModelBuilder builder = entityModelBuilder.addProperty();
                builder.name(name)
                       .accessor(new PropertyAccessor(getter, setter))
                       .annotations(List.of(getter.getDeclaredAnnotations()))
                       .typeData(typeData)
                       .mappedName(getMappedFieldName(datastore.getMapper().getOptions(), builder));
                if (datastore.getMapper().isMappable(typeData.getType())) {
                    builder.propertyEntityModel(datastore.getMapper().getEntityModel(typeData.getType()));
                }
            }
        }

/*
        for (Method method : type.getDeclaredMethods()) {

            Type genericType = method.getGenericType();
            if (genericType instanceof TypeVariable) {
                Map<String, Type> properties = parameterization.get(currentClass.getName());
                if (properties != null) {
                    Type mapped = properties.get(((TypeVariable<?>) genericType).getName());
                    if (mapped instanceof Class) {
                        typeData = TypeData.newInstance(method.getGenericType(), (Class<?>) mapped);
                    }
                }
            }

            FieldModelBuilder fieldModelBuilder = FieldModel.builder()
                                                            .field(method)
                                                            .fieldName(method.getName())
                                                            .typeData(typeData)
                                                            .annotations(List.of(method.getDeclaredAnnotations()));
            fieldModelBuilder.mappedName(getMappedFieldName(fieldModelBuilder));

            entityModelBuilder.addModel(fieldModelBuilder);
        }
*/

    }

    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
