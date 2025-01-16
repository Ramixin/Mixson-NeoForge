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
public @interface DeletionEvent {

    String[] value();

    String eventId() default "";

    int priority() default Mixson.DEFAULT_PRIORITY;

    boolean failSilently() default false;

    int ordinal() default -1;

    interface Builder {
        static BuiltAnnotationEvent build(DeletionEvent deletionEvent, String methodName) {
            return new BuiltAnnotationEvent(
                    deletionEvent.value(),
                    deletionEvent.eventId().isEmpty() ? BuiltAnnotationEvent.generateEventId(methodName) : deletionEvent.eventId(),
                    deletionEvent.priority(),
                    deletionEvent.failSilently(),
                    deletionEvent.ordinal(),
                    MixsonEventType.DELETION
            );
        }
    }

}
