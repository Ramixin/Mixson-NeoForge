package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.inline.Mixson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerativeMixsonEvent {

    String value();

    int priority() default Mixson.DEFAULT_PRIORITY;

    boolean failSilently() default false;

    int ordinal() default -1;

    boolean external() default false;

    interface Builder {
        static BuiltAnnotationEvent build(GenerativeMixsonEvent event, String resourceId, String eventName) {
            return new BuiltAnnotationEvent(
                    new String[]{resourceId},
                    eventName,
                    event.priority(),
                    event.failSilently(),
                    event.ordinal()
            );
        }
    }
}
