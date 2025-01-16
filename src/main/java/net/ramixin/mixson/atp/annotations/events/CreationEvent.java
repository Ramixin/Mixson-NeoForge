package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.atp.MixsonEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationEvent {

    String[] value();

    String eventId() default "";

    boolean failSilently() default false;

    int ordinal() default -1;

    interface Builder {
        static BuiltAnnotationEvent build(CreationEvent creationEvent, String methodName) {
            return new BuiltAnnotationEvent(
                    creationEvent.value(),
                    creationEvent.eventId().isEmpty() ? BuiltAnnotationEvent.generateEventId(methodName) : creationEvent.eventId(),
                    0,
                    creationEvent.failSilently(),
                    creationEvent.ordinal(),
                    MixsonEventType.CREATION
            );
        }
    }

}
