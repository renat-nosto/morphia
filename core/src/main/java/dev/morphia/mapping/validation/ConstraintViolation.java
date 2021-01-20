package dev.morphia.mapping.validation;


import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ConstraintViolation {
    private final EntityModel type;
    private final Class<? extends ClassConstraint> validator;
    private final String message;
    private final Level level;
    private PropertyModel field;

    /**
     * Creates a violation instance to record invalid mapping metadata
     *
     * @param level       the severity of the violation
     * @param entityModel the errant class
     * @param field       the errant field
     * @param validator   the constraint failed
     * @param message     the message for the failure
     */
    public ConstraintViolation(Level level, EntityModel entityModel, PropertyModel field,
                               Class<? extends ClassConstraint> validator, String message) {
        this(level, entityModel, validator, message);
        this.field = field;
    }

    /**
     * Creates a violation instance to record invalid mapping metadata
     *
     * @param level       the severity of the violation
     * @param entityModel the errant class
     * @param validator   the constraint failed
     * @param message     the message for the failure
     */
    public ConstraintViolation(Level level, EntityModel entityModel, Class<? extends ClassConstraint> validator,
                               String message) {
        this.level = level;
        this.type = entityModel;
        this.message = message;
        this.validator = validator;
    }

    /**
     * @return the severity of the violation
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return the qualified name of the failing mapping
     */
    public String getPrefix() {
        final String fn = (field != null) ? field.getName() : "";
        return type.getType().getName() + "." + fn;
    }

    /**
     * @return a human friendly version of the violation
     */
    public String render() {
        return String.format("%s complained about %s : %s", validator.getSimpleName(), getPrefix(), message);
    }

    /**
     * Levels of constraint violations
     */
    public enum Level {
        MINOR,
        INFO,
        WARNING,
        SEVERE,
        FATAL
    }
}
