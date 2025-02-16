package net.ramixin.mixson.atp.annotations.events;

import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.inline.Mixson;
import net.ramixin.mixson.inline.MixsonCodec;

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

    String codec() default "";

    interface Builder {
        static <T> BuiltAnnotationEvent<T> build(MixsonEvent event, String methodName, MixsonCodec<T> codec) {
            return new BuiltAnnotationEvent<>(
                    event.value(),
                    event.eventName().isEmpty() ? BuiltAnnotationEvent.generateEventName(methodName) : event.eventName(),
                    event.priority(),
                    event.failSilently(),
                    event.ordinal(),
                    codec
            );
        }
    }

}
