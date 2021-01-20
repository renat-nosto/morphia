package dev.morphia.mapping.validation;

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.experimental.ConstructorCreator;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Set;

/**
 * Checks that named constructor parameters match field names
 */
public class ConstructorParameterNameConstraint implements ClassConstraint {
    @Override
    public void check(Mapper mapper, EntityModel model, Set<ConstraintViolation> ve) {
        Constructor<?> fullConstructor = ConstructorCreator.getFullConstructor(model);
        if (fullConstructor != null) {
            for (Parameter parameter : fullConstructor.getParameters()) {
                String name = ConstructorCreator.getParameterName(parameter);
                if (model.getProperty(name) == null) {
                    throw new ConstraintViolationException(
                        new ConstraintViolation(Level.FATAL, model, getClass(), Sofia.misnamedConstructorParameter(model.getType(), name)));
                }
            }
        }
    }
}
