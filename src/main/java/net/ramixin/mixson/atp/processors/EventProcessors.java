package net.ramixin.mixson.atp.processors;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.atp.BuiltAnnotationEvent;
import net.ramixin.mixson.atp.annotations.Reference;
import net.ramixin.mixson.atp.annotations.events.GenerativeMixsonEvent;
import net.ramixin.mixson.atp.annotations.events.MixsonEvent;
import net.ramixin.mixson.inline.BuiltResourceReference;
import net.ramixin.mixson.inline.EventContext;
import net.ramixin.mixson.inline.Mixson;
import net.ramixin.mixson.inline.ResourceReference;
import org.slf4j.Logger;
import oshi.util.tuples.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public interface EventProcessors {

    static void handleMixsonEvent(MixsonEvent event, Method method, Logger logger) {
        System.out.println("handling MixsonEvent");
        runRegisterAnnotation(MixsonEvent.Builder.build(event, method.getName()), method, logger);
    }

    static void handleGenerativeMixsonEvent(GenerativeMixsonEvent event, Method method, Logger logger) {
        System.out.println("handling GenerativeMixsonEvent");
        Pair<String[], String[]> context = EventPreprocessors.getGeneratedIdentifiers(event.value(), event.external(), method.getDeclaringClass());
        String[] resourceIds = context.getA();
        String[] eventIds = context.getB();
        for(int i = 0; resourceIds.length > i; i++) {
            runRegisterAnnotation(GenerativeMixsonEvent.Builder.build(event, resourceIds[i], eventIds[i]), method, logger);
        }
    }

    private static void runRegisterAnnotation(BuiltAnnotationEvent event, Method method, Logger logger) {
        String methodName = method.getName();
        if(method.getReturnType() != void.class) throw new MixsonError("method '%s' must have return type void", methodName);
        for(String resourceId : event.resourceIds()) if(resourceId.isEmpty()) throw new MixsonError("empty resource id found for method '%s'", methodName);
        logger.info("Expanding method '{}' in class '{}' with eventName '{}' in class '{}",  methodName, method.getName(), event.eventName(), method.getDeclaringClass().getName());
        Parameter[] parameters = getAndValidateParameters(method);
        List<ResourceReference> referencesList = new ArrayList<>();
        List<ResourceLocation> referenceIdsList = new ArrayList<>();
        for(int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Reference reference = parameter.getAnnotation(Reference.class);
            if (reference == null)
                throw new MixsonError("proceeding parameters must be annotated with @Reference in method '%s'", methodName);
            String name;
            if (reference.referenceId().isEmpty()) name = BuiltAnnotationEvent.generateEventName(parameter.getName());
            else name = reference.referenceId();
            referenceIdsList.add(ResourceLocation.parse(name));
            referencesList.add(new ResourceReference(reference.priority(), reference.value(), name));
        }
        ResourceLocation[] referenceIds = referenceIdsList.toArray(ResourceLocation[]::new);
        ResourceReference[] references = referencesList.toArray(ResourceReference[]::new);
        for(String resourceId : event.resourceIds()) {
            Mixson.registerEvent(
                    event.priority(),
                    resourceId,
                    event.eventName(),
                    (context) -> runEventIntermediate(method, context, referenceIds, logger),
                    event.failSilently(),
                    references
            );
        }

    }

    private static Parameter[] getAndValidateParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        if(parameters.length == 0) throw new MixsonError("method '%s' must have first parameter of type 'EventContext'", method.getName());
        if(parameters[0].getType() != EventContext.class) throw new MixsonError("method '%s' must have first parameter of type 'EventContext'", method.getName());
        if(parameters[0].isAnnotationPresent(Reference.class)) throw new MixsonError("first parameter of method '%s' cannot have @Reference annotation", method.getName());
        for(int i = 1; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if(parameter.getType() != BuiltResourceReference.class) throw new MixsonError("method '%s' must have proceeding parameters of type 'BuiltResourceReference'", method.getName());
        }
        return parameters;
    }

    private static Object[] buildParameters(EventContext context, ResourceLocation[] referenceIds) {
        Object[] parameters = new Object[referenceIds.length + 1];
        parameters[0] = context;
        for(int i = 0; i < referenceIds.length; i++) {
            parameters[i+1] = context.getReference(String.valueOf(referenceIds[i]));
        }
        return parameters;
    }

    private static void runEventIntermediate(Method method, EventContext context, ResourceLocation[] referenceIds, Logger logger) {
        try {
            method.setAccessible(true);
            method.invoke(null, buildParameters(context, referenceIds));
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace(System.err);
            for(StackTraceElement stackTraceElement : e.getStackTrace()) logger.error(stackTraceElement.toString());
            throw new MixsonError(e);
        }
    }
}
