package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.atp.MixsonEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModificationEvent {

    String[] value();

    String eventId() default "";

    int priority() default Mixson.DEFAULT_PRIORITY;

    boolean failSilently() default false;

    int ordinal() default -1;

    interface Builder {
        static BuiltAnnotationEvent build(ModificationEvent modificationEvent, String methodName) {
            return new BuiltAnnotationEvent(
                    modificationEvent.value(),
                    modificationEvent.eventId().isEmpty() ? BuiltAnnotationEvent.generateEventId(methodName) : modificationEvent.eventId(),
                    modificationEvent.priority(),
                    modificationEvent.failSilently(),
                    modificationEvent.ordinal(),
                    MixsonEventType.MODIFICATION
            );
        }
    }

}
