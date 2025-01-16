package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.atp.MixsonEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerativeCreationEvent {

    String value();

    boolean failSilently() default false;

    int ordinal() default -1;

    boolean external() default false;

    interface Builder {
        static BuiltAnnotationEvent build(GenerativeCreationEvent modificationEvent, String resourceId, String eventId) {
            return new BuiltAnnotationEvent(
                    new String[]{resourceId},
                    eventId,
                    0,
                    modificationEvent.failSilently(),
                    modificationEvent.ordinal(),
                    MixsonEventType.MODIFICATION
            );
        }
    }

}
