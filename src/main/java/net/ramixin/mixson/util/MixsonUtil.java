package net.ramixin.mixson.util;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.inline.*;
import net.ramixin.mixson.inline.entries.EventEntry;
import org.apache.logging.log4j.util.TriConsumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public interface MixsonUtil {

    static ByteArrayOutputStream exportJson(JsonElement json) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(json.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos;
    }

    static String identifierToPathString(String resourceId, String extension) {
        ResourceLocation usable = ResourceLocation.parse(resourceId);
        return usable.getNamespace() + '~' + usable.getPath().replaceFirst("\\"+extension, "").replaceAll("/", "-");
    }

    static String stringToUsablePath(String string) {
        return string.replaceAll("[*|/\\\\:?<>\"]", "");
    }

    static ResourceLocation removeExtension(ResourceLocation id) {
        String stringId = id.getPath();
        for(int i = stringId.length()-1; i > 0; i--) if(stringId.charAt(i) == '.') return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), stringId.substring(0, i));
       return id;
    }

    static Function<ResourceLocation, Boolean> getLocatorFromString(String resourceId) {
        if(resourceId.endsWith("*")) {
            String id = removeWildcard(resourceId);
            return resourceLoc -> resourceLoc.toString().startsWith(id);
        }
        else return resourceLoc -> resourceLoc.equals(ResourceLocation.parse(resourceId));
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
    static <T> EventContext<T> createContext(ContextCreationType creationType, ResourceLocation resourceId, T file, EventEntry<T> entry, boolean markedForDeletion, Function<UUID, BuiltResourceReference<?>> referenceCallback) {
        BuiltMixsonEvent<T> event = entry.event();
        BuiltResourceReference<T>[] gatheredReferences = new BuiltResourceReference[event.referenceIds().length];
        for(int i = 0; i < event.referenceIds().length; i++) {
            BuiltResourceReference<T> ref = (BuiltResourceReference<T>) referenceCallback.apply(event.referenceIds()[i]);
            gatheredReferences[i] = ref;
        }
        return new EventContext<>(creationType, file, resourceId, entry, markedForDeletion, gatheredReferences);
    }

    static <T> Optional<T> getFile(MixsonCodec<T> codec, Resource resource, ErrorMessageProvider messageProvider, ResourceLocation resourceId, TriConsumer<Exception, ErrorMessageProvider, ResourceLocation> errorCallback) {
        try {
            return Optional.of(codec.deserialize(resource));
        } catch (IOException e) {
            errorCallback.accept(e, messageProvider, resourceId);
        }
        return Optional.empty();
    }

}
