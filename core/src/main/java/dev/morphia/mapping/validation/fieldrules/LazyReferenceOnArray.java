package dev.morphia.mapping.validation.fieldrules;

import dev.morphia.annotations.Reference;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.mapping.validation.ConstraintViolation;
import dev.morphia.mapping.validation.ConstraintViolation.Level;

import java.util.Set;

/**
 * Checks that lazy references aren't used in conjunction with arrays.
 */
public class LazyReferenceOnArray extends FieldConstraint {

    @Override
    protected void check(Mapper mapper, EntityModel model, PropertyModel mf, Set<ConstraintViolation> ve) {
        final Reference ref = mf.getAnnotation(Reference.class);
        if (ref != null && ref.lazy()) {
            final Class<?> type = mf.getType();
            if (type.isArray()) {
                ve.add(new ConstraintViolation(Level.FATAL, model, mf, getClass(),
                    "The lazy attribute cannot be used for an Array. If you need a lazy array "
                    + "please use ArrayList instead."));
            }
        }
    }

}
