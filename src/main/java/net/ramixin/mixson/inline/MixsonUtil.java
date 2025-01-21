package net.ramixin.mixson.inline;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MixsonUtil {

    static String identifierToPathString(ResourceLocation ResourceLocation) {
        return ResourceLocation.getNamespace() + '~' + ResourceLocation.getPath().replaceFirst("\\.json", "").replaceAll("/", "-");
    }

    static String stringToUsablePath(String string) {
        return string.replaceAll("[*|/\\\\:?<>\"]", "");
    }

    static Resource buildResource(Resource assosiatedResource, JsonElement elem) {
        return new Resource(assosiatedResource.source(), () -> new ByteArrayInputStream(elem.toString().getBytes()), assosiatedResource::metadata);
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

    static EventContext createContext(ContextCreationType creationType, ResourceLocation resourceId, JsonElement file, EventEntry entry, boolean markedForDeletion, Function<UUID, BuiltResourceReference> referenceCallback) {
        BuiltMixsonEvent event = entry.event();
        BuiltResourceReference[] gatheredReferences = new BuiltResourceReference[event.referenceIds().length];
        for(int i = 0; i < event.referenceIds().length; i++) {
            BuiltResourceReference ref = referenceCallback.apply(event.referenceIds()[i]);
            gatheredReferences[i] = ref;
        }
        return new EventContext(creationType, file, resourceId, entry, markedForDeletion, gatheredReferences);
    }

    static @Nullable JsonElement getFile(boolean contains, Pair<JsonElement, Resource> modifiedEntries, Resource resource, ErrorMessageProvider messageProvider, BiConsumer<Exception, ErrorMessageProvider> errorCallback) {
        JsonElement file;
        try {
            if (contains) file = modifiedEntries.getFirst();
            else file = JsonParser.parseReader(resource.openAsReader());
        } catch (IOException e) {
            errorCallback.accept(e, messageProvider);
            file = null;
        }
        return file;
    }

}
