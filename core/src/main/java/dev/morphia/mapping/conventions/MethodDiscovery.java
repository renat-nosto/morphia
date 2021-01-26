package dev.morphia.mapping.conventions;

import dev.morphia.Datastore;
import dev.morphia.mapping.codec.MethodAccessor;
import dev.morphia.mapping.codec.pojo.EntityModelBuilder;
import dev.morphia.mapping.codec.pojo.PropertyModelBuilder;
import dev.morphia.mapping.codec.pojo.TypeData;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class MethodDiscovery implements MorphiaConvention {
    private EntityModelBuilder entityModelBuilder;
    private Datastore datastore;

    private boolean debug;

    @Override
    public void apply(Datastore datastore, EntityModelBuilder builder) {
        this.datastore = datastore;
        this.entityModelBuilder = builder;

        debug = builder.getType().getName().contains("SpecializedEntity");

        Map<String, Map<String, Type>> parameterization = builder.parameterization();
        List<Class<?>> list = new ArrayList<>(List.of(builder.getType()));
        list.addAll(builder.classHierarchy());
        for (Class<?> type : list) {
            processMethods(type, parameterization);
        }

    }

    private List<Annotation> discoverAnnotations(Method getter, Method setter) {
        return List.of(getter, setter).stream()
                   .flatMap(m -> Arrays.stream(m.getDeclaredAnnotations()))
                   .collect(Collectors.toList());
    }

    private TypeData<?> getTypeData(Map<String, Map<String, Type>> parameterization, Class<?> type, Method method) {
        TypeData<?> typeData = TypeData.newInstance(method);

        Type genericType = method.getGenericReturnType();
        if (genericType instanceof TypeVariable) {
            Map<String, Type> map = parameterization.get(type.getName());
            if (map != null) {
                Type mapped = map.get(((TypeVariable<?>) genericType).getName());
                if (mapped instanceof Class) {
                    typeData = TypeData.newInstance(method.getGenericReturnType(), (Class<?>) mapped);
                }
            }
        }
        return typeData;
    }

    private void processMethods(Class<?> type, Map<String, Map<String, Type>> parameterization) {
        class Methods {
            private final Method getter;
            private final Method setter;

            Methods(List<Method> methods) {
                List<Method> collect = methods.stream().sorted(Comparator.comparing(Method::getName))
                                              .collect(Collectors.toList());
                getter = collect.get(0);
                setter = collect.get(1);
            }
        }

        Map<String, List<Method>> properties = Arrays.stream(type.getDeclaredMethods())
                                                     .filter(m -> m.getName().startsWith("get")
                                                                  || m.getName().startsWith("set")
                                                                  || m.getName().startsWith("is"))
                                                     .collect(Collectors.groupingBy(m -> m.getName().startsWith("get")
                                                                                         || m.getName().startsWith("set")
                                                                                         ? stripPrefix(m, 3)
                                                                                         : stripPrefix(m, 2)));

        for (Entry<String, List<Method>> entry : properties.entrySet()) {
            List<Method> value = entry.getValue();
            if (value.size() == 2) {
                Methods methods = new Methods(value);
                TypeData<?> typeData = getTypeData(parameterization, type, methods.getter);

                PropertyModelBuilder builder = entityModelBuilder.addProperty();
                builder.name(entry.getKey())
                       .accessor(new MethodAccessor(methods.getter, methods.setter))
                       .annotations(discoverAnnotations(methods.getter, methods.setter))
                       .typeData(typeData)
                       .mappedName(builder.discoverMappedName(datastore.getMapper().getOptions()));
            }
        }
    }

    private String stripPrefix(Method method, int size) {
        String name = method.getName().substring(size);
        name = name.substring(0, 1).toLowerCase() + name.substring(1);

        return name;
    }
}
