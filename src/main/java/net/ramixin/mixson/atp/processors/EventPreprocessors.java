package net.ramixin.mixson.atp.processors;

import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.atp.GeneratorContext;
import net.ramixin.mixson.atp.annotations.Generator;
import oshi.util.tuples.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.function.Consumer;

public class EventPreprocessors {

    private static final HashMap<Class<?>, HashMap<String, Pair<String[], String[]>>> generatedIdentifierHash = new HashMap<>();


    public static void handleGenerator(Generator generator, Method method, Consumer<String> stringConsumer) {
        Class<?> clazz = method.getDeclaringClass();
        if(method.getParameterCount() != 1) throw new MixsonError(String.format("method '%s' must have exactly one parameter of type 'GeneratorContext'", method.getName()));
        Parameter parameter = method.getParameters()[0];
        if(parameter.getType() != GeneratorContext.class) throw new MixsonError(String.format("method '%s' must have exactly one parameter of type 'GeneratorContext'", method.getName()));
        try {
            method.setAccessible(true);
            GeneratorContext context = new GeneratorContext();
            method.invoke(null, context);
            if(!generatedIdentifierHash.containsKey(clazz)) generatedIdentifierHash.put(clazz, new HashMap<>());
            HashMap<String, Pair<String[], String[]>> clazzHash = generatedIdentifierHash.get(clazz);
            clazzHash.put(generator.value(), context.collect());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Pair<String[], String[]> getGeneratedIdentifiers(String generatorId, boolean external, Class<?> clazz) {
        if(!external) {
            HashMap<String, Pair<String[], String[]>> clazzHash = generatedIdentifierHash.get(clazz);
            if(clazzHash == null) throw new MixsonError(String.format("no generator with id '%s' exists in the class '%s'", generatorId, clazz.getName()));
            Pair<String[], String[]> pair = clazzHash.get(generatorId);
            if(pair == null) throw new MixsonError(String.format("no generator with id '%s' exists in the class '%s'", generatorId, clazz.getName()));
            return pair;
        }
        for(HashMap<String, Pair<String[], String[]>> clazzHash : generatedIdentifierHash.values()) {
            Pair<String[], String[]> pair = clazzHash.get(generatorId);
            if(pair != null) return pair;
        }
        throw new MixsonError(String.format("no generator with id '%s' has been found", generatorId));
    }
}
