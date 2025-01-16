package net.ramixin.mixson.atp;

import net.ramixin.mixson.atp.annotations.Generator;
import net.ramixin.mixson.atp.annotations.events.*;
import net.ramixin.mixson.atp.processors.EventPreprocessors;
import net.ramixin.mixson.atp.processors.EventProcessors;
import org.apache.logging.log4j.util.TriConsumer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MixsonAnnotationProcessor {

    public static void processClass(Class<?> clazz, Consumer<String> loggerCallback) {
        for(Method method : clazz.getDeclaredMethods()) preprocessMethod(method, loggerCallback);
        for(Method method : clazz.getDeclaredMethods()) processMethod(method, loggerCallback);
    }

    private static void processMethod(Method method, Consumer<String> loggerCallback) {
        enforceExclusivity(method, ModificationEvent.class, CreationEvent.class, DeletionEvent.class, GenerativeModificationEvent.class, GenerativeCreationEvent.class, GenerativeDeletionEvent.class);
        processAnnotation(ModificationEvent.class, method, loggerCallback, EventProcessors::handleModificationEvent);
        processAnnotation(CreationEvent.class, method, loggerCallback, EventProcessors::handleCreationEvent);
        processAnnotation(DeletionEvent.class, method, loggerCallback, EventProcessors::handleDeletionEvent);
        processAnnotation(GenerativeModificationEvent.class, method, loggerCallback, EventProcessors::handleGenerativeModificationEvent);
        processAnnotation(GenerativeCreationEvent.class, method, loggerCallback, EventProcessors::handleGenerativeCreationEvent);
        processAnnotation(GenerativeDeletionEvent.class, method, loggerCallback, EventProcessors::handleGenerativeDeletionEvent);
    }

    private static void preprocessMethod(Method method, Consumer<String> loggerCallback) {
        processAnnotation(Generator.class, method, loggerCallback, EventPreprocessors::handleGenerator);
    }

    private static <T extends Annotation> void processAnnotation(Class<T> annotationClazz, Method method, Consumer<String> loggerCallback, TriConsumer<T, Method, Consumer<String>> callback) {
        T annotation = method.getAnnotation(annotationClazz);
        if(annotation == null) return;
        callback.accept(annotation, method, loggerCallback);
    }

    @SafeVarargs
    private static void enforceExclusivity(Method method, Class<? extends Annotation>... annotations) {
        boolean safe = true;
        for(Class<? extends Annotation> annotation : annotations) if(method.isAnnotationPresent(annotation)) {
            if(safe) safe = false;
            else throw new IllegalStateException(String.format("method '%s' has multiple annotations of type '%s'", method.getName(), Arrays.stream(annotations).map(Class::getName).collect(Collectors.toList())));
        }
    }
}
