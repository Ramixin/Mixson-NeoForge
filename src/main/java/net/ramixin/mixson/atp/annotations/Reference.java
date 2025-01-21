package net.ramixin.mixson.atp.annotations;

import net.ramixin.mixson.inline.Mixson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {

    String value();

    int priority() default Mixson.DEFAULT_PRIORITY;

    String referenceId() default "";

}
