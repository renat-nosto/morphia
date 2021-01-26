package dev.morphia.mapping.codec.pojo;

import dev.morphia.Datastore;
import dev.morphia.annotations.Entity;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.conventions.MorphiaConvention;
import dev.morphia.sofia.Sofia;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for EntityModels
 *
 * @morphia.internal
 * @since 2.0
 */
@SuppressWarnings("UnusedReturnValue")
public class EntityModelBuilder {
    private final Datastore datastore;
    private final List<PropertyModelBuilder> propertyModels = new ArrayList<>();
    private final Class<?> type;
    private final Map<Class<? extends Annotation>, Annotation> annotationsMap = new HashMap<>();
    private final Set<Class<?>> classes = new LinkedHashSet<>();
    private final Set<Class<?>> interfaces = new LinkedHashSet<>();
    private final Set<Annotation> annotations = new LinkedHashSet<>();
    private boolean discriminatorEnabled;
    private String discriminator;
    private String discriminatorKey;
    private String idPropertyName;
    private String versionPropertyName;
    private final List<EntityModel> interfaceModels;
    private EntityModel superclass;
    private final Map<String, Map<String, Type>> parameterization;
    /**
     * Create a builder
     *
     * @param datastore the datastore to use
     * @param type      the entity type
     */
    public EntityModelBuilder(Datastore datastore, Class<?> type) {
        this.datastore = datastore;
        this.type = type;

        buildHierarchy(this.type);
        parameterization = findParameterization(type);
        propagateTypes();

        if (!Object.class.equals(type.getSuperclass())) {
            this.superclass = datastore.getMapper().getEntityModel(type.getSuperclass());
        }

        this.interfaceModels = interfaces.stream()
                                         .map(i -> datastore.getMapper().getEntityModel(i))
                                         .filter(Objects::nonNull)
                                         .collect(Collectors.toList());

    }

    /**
     * @param datastore  the datastore
     * @param annotation the annotation
     * @param clazz      the type
     * @param <T>        the class type
     * @param <A>        the annotation type
     * @morphia.internal
     */
    public <T, A extends Annotation> EntityModelBuilder(Datastore datastore, A annotation, Class<T> clazz) {
        this(datastore, clazz);
        LinkedHashSet<Annotation> temp = new LinkedHashSet<>();
        temp.add(annotation);
        temp.addAll(annotations);
        annotations.clear();
        annotations.addAll(temp);
    }

    /**
     * Adds a property to the model
     *
     * @return the new PropertyModelBuilder
     */
    public PropertyModelBuilder addProperty() {
        PropertyModelBuilder builder = PropertyModel.builder(datastore);
        propertyModels.add(builder);
        return builder;
    }

    /**
     * @return the parent class hierarchy
     * @since 2.2
     */
    public Set<Class<?>> classHierarchy() {
        return classes;
    }

    private Map<String, Map<String, Type>> findParameterization(Class<?> type) {
        if (type.getSuperclass() == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Map<String, Type>> parentMap = findParameterization(type.getSuperclass());
        Map<String, Type> typeMap = mapArguments(type.getSuperclass(), type.getGenericSuperclass());

        parentMap.put(type.getSuperclass().getName(), typeMap);
        return parentMap;
    }

    private Map<String, Type> mapArguments(Class<?> type, Type typeSignature) {
        Map<String, Type> map = new HashMap<>();
        if (type != null && typeSignature instanceof ParameterizedType) {
            TypeVariable<?>[] typeParameters = type.getTypeParameters();
            if (typeParameters.length != 0) {
                Type[] arguments = ((ParameterizedType) typeSignature).getActualTypeArguments();
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable<?> typeParameter = typeParameters[i];
                    map.put(typeParameter.getName(), arguments[i]);
                }
            }
        }
        return map;
    }

    private Set<Class<?>> findParentClasses(Class<?> type) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        while (type != null && !type.isEnum() && !type.equals(Object.class)) {
            classes.add(type);
            annotations.addAll(Set.of(type.getAnnotations()));
            type = type.getSuperclass();
        }
        return classes;
    }

    /**
     * @return the annotation on this model
     */
    public Set<Annotation> annotations() {
        return annotations;
    }

    /**
     * Creates a new ClassModel instance based on the mapping data provided.
     *
     * @return the new instance
     */
    public EntityModel build() {
        annotations.forEach(a -> annotationsMap.putIfAbsent(a.annotationType(), a));

        for (MorphiaConvention convention : datastore.getMapper().getOptions().getConventions()) {
            convention.apply(datastore, this);
        }

        if (discriminatorEnabled) {
            Objects.requireNonNull(discriminatorKey, Sofia.notNull("discriminatorKey"));
            Objects.requireNonNull(discriminator, Sofia.notNull("discriminator"));
        }

        return new EntityModel(this);
    }

    /**
     * Sets the discriminator
     *
     * @param discriminator the discriminator
     * @return this
     */
    public EntityModelBuilder discriminator(String discriminator) {
        this.discriminator = discriminator;
        return this;
    }

    /**
     * @return the discriminator
     */
    public String discriminator() {
        return discriminator;
    }

    /**
     * Sets the mapped key name to use when storing the discriminator value
     *
     * @param key the key to use
     * @return this
     */
    public EntityModelBuilder discriminatorKey(String key) {
        discriminatorKey = key;
        return this;
    }

    /**
     * @return the discriminator key
     */
    public String discriminatorKey() {
        return discriminatorKey;
    }

    /**
     * Enables or disables the use of a discriminator when serializing
     *
     * @param enabled true to enable the use of a discriminator
     * @return this
     */
    public EntityModelBuilder enableDiscriminator(boolean enabled) {
        this.discriminatorEnabled = enabled;
        return this;
    }

    /**
     * @return the name of the id property
     */
    public String idPropertyName() {
        return idPropertyName;
    }

    /**
     * Sets the name of the id property
     *
     * @param name the name
     * @return this
     */
    public EntityModelBuilder idPropertyName(String name) {
        this.idPropertyName = name;
        return this;
    }

    /**
     * @param type the annotation class
     * @param <A>  the annotation type
     * @return the annotation or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        return (A) annotationsMap.get(type);
    }

    /**
     * The type of this model
     *
     * @return the type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @param type the annotation class
     * @return the annotation if it exists
     */
    public boolean hasAnnotation(Class<? extends Annotation> type) {
        for (Annotation annotation : annotations) {
            if (type.equals(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a property by its name
     *
     * @param name the name
     * @return the property
     * @throws NoSuchElementException if no value is present
     */
    public PropertyModelBuilder propertyModelByName(String name) throws NoSuchElementException {
        return propertyModels.stream().filter(f -> f.name().equals(name))
                             .findFirst()
                             .orElseThrow();
    }

    /**
     * @return the properties on this model
     */
    public List<PropertyModelBuilder> propertyModels() {
        return propertyModels;
    }

    /**
     * @return the name of the version property
     */
    public String versionPropertyName() {
        return versionPropertyName;
    }

    /**
     * Sets the name of the version property
     *
     * @param name the name
     * @return this
     */
    public EntityModelBuilder versionPropertyName(String name) {
        this.versionPropertyName = name;
        return this;
    }

    /**
     * @return the interfaces implemented by this model or its super types
     */
    public List<EntityModel> interfaces() {
        return interfaceModels;
    }

    /**
     * @return true if the discriminator is enabled
     */
    public boolean isDiscriminatorEnabled() {
        return discriminatorEnabled;
    }

    /**
     * @return the super class of this type or null
     */
    public EntityModel superclass() {
        return superclass;
    }

    protected Map<Class<? extends Annotation>, Annotation> annotationsMap() {
        return annotationsMap;
    }

    protected String getCollectionName() {
        Entity entityAn = getAnnotation(Entity.class);
        return entityAn != null && !entityAn.value().equals(Mapper.IGNORED_FIELDNAME)
               ? entityAn.value()
               : datastore.getMapper().getOptions().getCollectionNaming().apply(type.getSimpleName());
    }

    protected Datastore getDatastore() {
        return datastore;
    }

    private void buildHierarchy(Class<?> type) {
        annotations.addAll(Set.of(type.getAnnotations()));
        interfaces.addAll(findInterfaces(type));

        classes.addAll(findParentClasses(type.getSuperclass()));
        classes.forEach(c -> interfaces.addAll(findInterfaces(c)));
    }

    private List<? extends Class<?>> findInterfaces(Class<?> type) {
        List<Class<?>> list = new ArrayList<>();
        List<Class<?>> interfaces = Arrays.asList(type.getInterfaces());
        annotations.addAll(Set.of(type.getAnnotations()));
        list.addAll(interfaces);
        list.addAll(interfaces.stream()
                              .flatMap(i -> findInterfaces(i).stream())
                              .collect(Collectors.toList()));

        return list;
    }


    private void propagateTypes() {
        List<Map<String, Type>> parameters = new ArrayList<>(parameterization.values());

        for (int index = 0; index < parameters.size(); index++) {
            Map<String, Type> current = parameters.get(index);
            if (index + 1 < parameters.size()) {
                for (Entry<String, Type> entry : current.entrySet()) {
                    int peek = index + 1;
                    while (entry.getValue() instanceof TypeVariable) {
                        TypeVariable<?> typeVariable = (TypeVariable<?>) entry.getValue();
                        Map<String, Type> next = parameters.get(peek++);
                        entry.setValue(next.get(typeVariable.getName()));
                    }
                }
            }
        }
    }

    public TypeData<?> getTypeData(Class<?> type, TypeData<?> suggested, Type genericType) {

        if (genericType instanceof TypeVariable) {
            Map<String, Type> map = parameterization.get(type.getName());
            if (map != null) {
                Type mapped = map.get(((TypeVariable<?>) genericType).getName());
                if (mapped instanceof Class) {
                    suggested = TypeData.newInstance(genericType, (Class<?>) mapped);
                }
            }
        }
        return suggested;
    }

}
