package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.atp.MixsonAnnotationProcessor;
import net.ramixin.mixson.inline.*;
import net.ramixin.mixson.inline.entries.EventEntry;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface MixsonUtil {

    static String identifierToPathString(String resourceId, String extension) {
        Identifier usable = Identifier.parse(resourceId);
        return usable.getNamespace() + '~' + usable.getPath().replaceFirst(String.format("\\%s", extension), "").replaceAll("/", "-");
    }

    static String stringToUsablePath(String string) {
        return string.replaceAll("[*|/\\\\:?<>\"]", "");
    }

    static Identifier removeExtension(Identifier id) {
        String stringId = id.getPath();
        for(int i = stringId.length()-1; i > 0; i--) if(stringId.charAt(i) == '.') return Identifier.fromNamespaceAndPath(id.getNamespace(), stringId.substring(0, i));
       return id;
    }

    static ResourceLocator getLocatorFromString(String resourceId) {
        if(resourceId.endsWith("*")) {
            String id = removeWildcard(resourceId);
            return resourceLoc -> resourceLoc.toString().startsWith(id);
        }
        else return resourceLoc -> resourceLoc.equals(Identifier.parse(resourceId));
    }

    static <T> void addComponent(T component, int priority, UUID uuid, Map<UUID, T> components, SortedMap<Integer, List<T>> orderedComponents) {
        components.put(uuid, component);
        List<T> componentSet;
        if(orderedComponents.get(priority) == null) componentSet = new ArrayList<>();
        else componentSet = orderedComponents.get(priority);
        componentSet.add(component);
        orderedComponents.put(priority, componentSet);
    }

    static String removeWildcard(String string) {
        return string.substring(0, string.length() - 1);
    }

    @SuppressWarnings("unchecked")
    static <T> EventContext<T> createContext(ContextCreationType creationType, Identifier resourceId, T file, EventEntry<T> entry, boolean markedForDeletion, Function<UUID, BuiltResourceReference<?>> referenceCallback, BiFunction<String, Integer, T> captureCallback) {
        BuiltMixsonEvent<T> event = entry.event();
        BuiltResourceReference<T>[] gatheredReferences = new BuiltResourceReference[event.referenceIds().length];
        for(int i = 0; i < event.referenceIds().length; i++) {
            BuiltResourceReference<T> ref = (BuiltResourceReference<T>) referenceCallback.apply(event.referenceIds()[i]);
            gatheredReferences[i] = ref;
        }
        return new EventContext<>(creationType, file, resourceId, entry, markedForDeletion, gatheredReferences, captureCallback);
    }

    static <T> Optional<T> getFile(MixsonCodec<T> codec, Resource resource, ErrorMessageProvider messageProvider, Identifier resourceId, TriConsumer<Exception, ErrorMessageProvider, Identifier> errorCallback) {
        try {
            return Optional.of(codec.deserialize(resource));
        } catch (IOException e) {
            errorCallback.accept(e, messageProvider, resourceId);
        }
        return Optional.empty();
    }

    static void loadATPMixsonEntries(String path, Runnable locatedCallback) {
        ModList.get().forEachModContainer((unused, modContainer) -> {
            List<? extends IConfigurable> mixsonConfig = modContainer.getModInfo().getOwningFile().getConfig().getConfigList("mixson");
            for(IConfigurable configurable : mixsonConfig) {
                if(!(configurable instanceof NightConfigWrapper wrapper)) continue;
                Optional<Object> maybeEntries = wrapper.getConfigElement(path);
                if(maybeEntries.isEmpty()) continue;
                String id = modContainer.getModId();
                if(!(maybeEntries.get() instanceof List<?> list)) throw new IllegalStateException(String.format("'%s' field in [[mixson]] section must be a list of strings in mod '%s'", path, id));
                if(!list.isEmpty())
                    locatedCallback.run();
                for(Object entry : list) {
                    if(!(entry instanceof String className)) throw new IllegalStateException(String.format("'%s' field in [[mixson]] section must only contain strings, but found: '%s' in mod '%s'", path, entry, id));
                    try {
                        MixsonAnnotationProcessor.processClass(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new MixsonError(String.format("class '%s' in 'mixson' field in mod '%s' does not exist", className, id));
                    }
                }
            }
        });
    }

}
