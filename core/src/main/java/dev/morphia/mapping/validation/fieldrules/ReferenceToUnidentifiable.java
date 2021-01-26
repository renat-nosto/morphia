package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.Key;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;
import dev.morphia.sofia.Sofia;

import java.util.Set;

/**
 * Checks that references point to mapped types.
 */
public class ReferenceToUnidentifiable extends PropertyConstraint {

    @Override
    protected void check(Mapper mapper, EntityModel entityModel, PropertyModel propertyModel, Set<ConstraintViolation> ve) {
        if (propertyModel.hasAnnotation(Reference.class)) {
            final Class realType = /*(mf.isScalarValue()) ? mf.getType() : */propertyModel.getNormalizedType();

            if (realType == null) {
                throw new MappingException("Type is null for this MappedField: " + propertyModel);
            }

            if (realType.equals(Key.class)) {
                ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(), Sofia.keyNotAllowedAsField()));
            } else {
                EntityModel model = mapper.getEntityModel(realType);
                if (model == null || model.getIdProperty() == null && !model.getType().isInterface()) {
                    ve.add(new ConstraintViolation(Level.FATAL, entityModel, propertyModel, getClass(),
                        propertyModel.getFullName() + " is annotated as a @" + Reference.class.getSimpleName() + " but the "
                        + propertyModel.getType().getName() + " class is missing the @" + Id.class.getSimpleName() + " annotation"));
                }
            }
        }
    }

}
