package net.ramixin.mixson.atp.processors;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.BuiltResourceReference;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.ResourceReference;
import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.atp.MixsonEventType;
import net.ramixin.mixson.atp.annotations.Reference;
import net.ramixin.mixson.atp.annotations.events.*;
import oshi.util.tuples.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public interface EventProcessors {

    static void handleModificationEvent(ModificationEvent event, Method method, Consumer<String> loggerCallback) {
        runRegisterAnnotation(ModificationEvent.Builder.build(event, method.getName()), method, loggerCallback);
    }

    static void handleCreationEvent(CreationEvent event, Method method, Consumer<String> loggerCallback) {
        runRegisterAnnotation(CreationEvent.Builder.build(event, method.getName()), method, loggerCallback);
    }

    static void handleDeletionEvent(DeletionEvent event, Method method, Consumer<String> loggerCallback) {
        runRegisterAnnotation(DeletionEvent.Builder.build(event, method.getName()), method, loggerCallback);
    }

    static void handleGenerativeModificationEvent(GenerativeModificationEvent event, Method method, Consumer<String> loggerCallback) {
        Pair<String[], String[]> context = EventPreprocessors.getGeneratedIdentifiers(event.value(), event.external(), method.getDeclaringClass());
        String[] resourceIds = context.getA();
        String[] eventIds = context.getB();
        for(int i = 0; resourceIds.length > i; i++) {
            runRegisterAnnotation(GenerativeModificationEvent.Builder.build(event, resourceIds[i], eventIds[i]), method, loggerCallback);
        }
    }

    static void handleGenerativeCreationEvent(GenerativeCreationEvent event, Method method, Consumer<String> loggerCallback) {
        Pair<String[], String[]> context = EventPreprocessors.getGeneratedIdentifiers(event.value(), event.external(), method.getDeclaringClass());
        String[] resourceIds = context.getA();
        String[] eventIds = context.getB();
        for(int i = 0; resourceIds.length > i; i++) {
            runRegisterAnnotation(GenerativeCreationEvent.Builder.build(event, resourceIds[i], eventIds[i]), method, loggerCallback);
        }
    }

    static void handleGenerativeDeletionEvent(GenerativeDeletionEvent event, Method method, Consumer<String> loggerCallback) {
        Pair<String[], String[]> context = EventPreprocessors.getGeneratedIdentifiers(event.value(), event.external(), method.getDeclaringClass());
        String[] resourceIds = context.getA();
        String[] eventIds = context.getB();
        for(int i = 0; resourceIds.length > i; i++) {
            runRegisterAnnotation(GenerativeDeletionEvent.Builder.build(event, resourceIds[i], eventIds[i]), method, loggerCallback);
        }
    }



    private static void runRegisterAnnotation(BuiltAnnotationEvent event, Method method, Consumer<String> loggerCallback) {
        String methodName = method.getName();
        if(event.eventType().getReturnType() != method.getReturnType()) throw new MixsonError(String.format("method '%s' must have return type '%s' for event type %s", methodName, event.eventType().getReturnType().getName(), event.eventType()));
        for(String resourceId : event.resourceIds()) if(resourceId.isEmpty()) throw new MixsonError(String.format("empty resource id found for method '%s'", methodName));
        loggerCallback.accept(String.format("Expanding method '%s' in class '%s' with eventId '%s'", methodName, method.getClass().getName(), event.eventId()));
        Parameter[] parameters = validateJsonParameter(method, event.eventType());
        List<ResourceReference> references = new ArrayList<>();
        List<ResourceLocation> referenceNames = new ArrayList<>();
        for (int i = event.eventType().providesJson() ? 1 : 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Reference reference = parameter.getAnnotation(Reference.class);
            if (reference == null) throw new MixsonError("non-provided parameters must be annotated with @Reference in method '"+methodName+'\'');
            if(parameter.getType() != BuiltResourceReference.class) throw new MixsonError(String.format("@Reference can only be applied to type 'BuiltResourceReference', not '%s' in method '%s'", parameter.getType().getName(), methodName));
            ResourceLocation name;
            if(reference.referenceId().isEmpty()) name = ResourceLocation.tryParse(BuiltAnnotationEvent.generateEventId(parameter.getName()));
            else name = ResourceLocation.tryParse(reference.referenceId());
            referenceNames.add(name);
            references.add(new ResourceReference(reference.priority(), ResourceLocation.tryParse(reference.value()), name));
        }
        ResourceReference[] referencesArray = references.toArray(ResourceReference[]::new);
        for(String resourceId : event.resourceIds())
            if(references.isEmpty()) finalizeStandardEvent(method, event, resourceId);
            else finalizeAdvancedEvent(method, event, resourceId, referenceNames, referencesArray);
    }

    private static void finalizeStandardEvent(Method method, BuiltAnnotationEvent event, String resourceId) {
        switch (event.eventType()) {
            case MODIFICATION ->
                    Mixson.registerModificationEvent(
                            event.priority(),
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            (elem) -> runEventIntermediate(method, null, null, elem),
                            event.failSilently()
                    );
            case CREATION ->
                    Mixson.registerCreationEvent(
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            () -> runEventIntermediate(method, null, null, null),
                            event.failSilently()
                    );
            case DELETION ->
                    Mixson.registerDeletionEvent(
                            event.priority(),
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            () -> runEventIntermediate(method, null, null, null),
                            event.failSilently()
                    );

        }
    }

    private static void finalizeAdvancedEvent(Method method, BuiltAnnotationEvent event, String resourceId, List<ResourceLocation> referenceNames, ResourceReference[] references) {
        switch (event.eventType()) {
            case MODIFICATION ->
                    Mixson.registerModificationEvent(
                            event.priority(),
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            (elem, embedded) -> runEventIntermediate(method, embedded, referenceNames, elem),
                            event.failSilently(),
                            references
                    );
            case CREATION ->
                    Mixson.registerCreationEvent(
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            (embedded) -> runEventIntermediate(method, embedded, referenceNames, null),
                            event.failSilently(),
                            references
                    );
            case DELETION ->
                    Mixson.registerDeletionEvent(
                            event.priority(),
                            ResourceLocation.tryParse(resourceId),
                            ResourceLocation.tryParse(event.eventId()),
                            (embedded) -> runEventIntermediate(method, embedded, referenceNames, null),
                            event.failSilently(),
                            references

                    );

        }
    }

    private static Parameter[] validateJsonParameter(Method method, MixsonEventType type) {
        Parameter[] parameters = method.getParameters();
        if(type.providesJson()) {
            if (parameters.length == 0) throw new MixsonError(String.format("method '%s' must have first parameter of type 'JsonElement'", method.getName()));
            Parameter jsonParameter = parameters[0];
            if(jsonParameter.getType() != JsonElement.class) throw new MixsonError(String.format("method '%s' must have first parameter of type 'JsonElement'", method.getName()));
            if(jsonParameter.isAnnotationPresent(Reference.class)) throw new MixsonError(String.format("method '%s' must not have first parameter annotated with @Reference", method.getName()));
        }
        return parameters;
    }

    private static Object[] buildParameters(HashMap<ResourceLocation, BuiltResourceReference> embeddedReferences, List<ResourceLocation> referenceNames, JsonElement elem) {
        int offset = elem == null ? 0 : 1;
        int count = referenceNames == null ? 0 : referenceNames.size();
        Object[] parameters = new Object[count + offset];
        if(elem != null) parameters[0] = elem;
        if(embeddedReferences == null) return parameters;
        for(int i = 0; i < count; i++) {
            ResourceLocation name = referenceNames.get(i);
            parameters[i+offset] = embeddedReferences.get(name);
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    private static <T> T runEventIntermediate(Method method, HashMap<ResourceLocation, BuiltResourceReference> embeddedReferences, List<ResourceLocation> referenceNames, JsonElement elem) {
        try {
            method.setAccessible(true);
            return (T) method.invoke(null, buildParameters(embeddedReferences, referenceNames, elem));
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new MixsonError(e);
        }
    }
}
