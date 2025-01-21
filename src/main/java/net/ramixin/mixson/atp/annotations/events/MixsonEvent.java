package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.inline.Mixson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MixsonEvent {

    String[] value();

    String eventName() default "";

    int priority() default Mixson.DEFAULT_PRIORITY;

    boolean failSilently() default false;

    int ordinal() default -1;

    interface Builder {
        static BuiltAnnotationEvent build(MixsonEvent event, String methodName) {
            return new BuiltAnnotationEvent(
                    event.value(),
                    event.eventName().isEmpty() ? BuiltAnnotationEvent.generateEventName(methodName) : event.eventName(),
                    event.priority(),
                    event.failSilently(),
                    event.ordinal()
            );
        }
    }

}
