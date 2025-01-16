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
public @interface GenerativeModificationEvent {

    String value();

    int priority() default Mixson.DEFAULT_PRIORITY;

    boolean failSilently() default false;

    int ordinal() default -1;

    boolean external() default false;

    interface Builder {
        static BuiltAnnotationEvent build(GenerativeModificationEvent modificationEvent, String resourceId, String eventId) {
            return new BuiltAnnotationEvent(
                    new String[]{resourceId},
                    eventId,
                    modificationEvent.priority(),
                    modificationEvent.failSilently(),
                    modificationEvent.ordinal(),
                    MixsonEventType.MODIFICATION
            );
        }
    }

}
