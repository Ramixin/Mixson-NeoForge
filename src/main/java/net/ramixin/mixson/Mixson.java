package net.ramixin.mixson;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.*;

@SuppressWarnings("unused")
public class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static DebugMode debugMode = DebugMode.OFF;
    private static final TreeMap<Integer, List<AssociatedMixsonEvent>> events = new TreeMap<>();
    private static final HashMap<UUID, BuiltResourceReference> references = new HashMap<>();
    public static final int DEFAULT_PRIORITY = 1000;

    // REGISTRATION METHODS

    public static void registerModificationEvent(ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event) {
        registerModificationEvent(DEFAULT_PRIORITY, resourceId, eventId, event);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event) {
        registerModificationEvent(priority, resourceId, eventId, event, false);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event, boolean silentlyFail) {
        logAction("Registering Modification Event '{}' on resource '{}' with priority {}", eventId, resourceId, priority);
        register(priority, resourceId, eventId, event, silentlyFail);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final AdvancedModificationEvent event, boolean silentlyFail, final ResourceReference... references) {
        logAction("Registering Advanced Modification Event '{}' on resource '{}' with priority {}", eventId, resourceId, priority);
        register(priority, resourceId, eventId, event, silentlyFail, buildReferences(eventId, silentlyFail, references).getKey());
    }

    public static void registerCreationEvent(ResourceLocation associatedResourceId, ResourceLocation resourceId, final CreationEvent event, boolean silentlyFail) {
        logAction("Registering Creation Event for resource '{}' associated with '{}' with priority {}", resourceId, associatedResourceId, DEFAULT_PRIORITY);
        register(DEFAULT_PRIORITY, associatedResourceId, resourceId, event, silentlyFail);
    }

    public static void registerCreationEvent(ResourceLocation associatedResourceId, ResourceLocation resourceId, final AdvancedCreationEvent event, boolean silentlyFail, final ResourceReference... references) {
        Map.Entry<UUID[], Integer> pair = buildReferences(resourceId, silentlyFail, references);
        logAction("Registering Advanced Creation Event for resource '{}' associated with '{}' with priority {}", resourceId, associatedResourceId, pair.getValue());
        register(pair.getValue(), associatedResourceId, resourceId, event, silentlyFail, pair.getKey());
    }

    public static void registerDeletionEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final DeletionEvent event, boolean silentlyFail) {
        logAction("Registering Deletion Event '{}' on resource '{}' with priority {}", eventId, resourceId, priority);
        register(priority, resourceId, eventId, event, silentlyFail);
    }

    public static void registerDeletionEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final AdvancedDeletionEvent event, boolean silentlyFail, final ResourceReference... references) {
        logAction("Registering Advanced Deletion Event '{}' on resource '{}' with priority {}", eventId, resourceId, priority);
        register(priority, resourceId, eventId, event, silentlyFail);
    }

    private static void register(int priority, ResourceLocation resourceId, ResourceLocation eventId, MixsonEventTypes.BaseEvent event, boolean silentlyFail, final UUID... referenceIds) {
        List<AssociatedMixsonEvent> eventSet;
        if(events.get(priority) == null) eventSet = new ArrayList<>();
        else eventSet = events.get(priority);
        eventSet.add(new AssociatedMixsonEvent(resourceId.withSuffix(".json"), eventId.withSuffix(".json"), event, silentlyFail, referenceIds));
        events.put(priority, eventSet);
    }

    // EXTERNAL RUN METHODS

    public static List<Resource> runEvents(List<Resource> original, ResourceLocation id) {
        JsonElement[] modifiedEntries = new JsonElement[original.size()];
        for (List<AssociatedMixsonEvent> eventSet : events.values()) for (AssociatedMixsonEvent event : eventSet) {
            if(!event.resourceId().equals(id)) continue;
            switch (event.event()) {
                case MixsonEventTypes.Creation unused -> {
                    JsonElement val = runCreationEvent(event);
                    if(val == null) continue;
                    original.add(buildResource(original.getFirst(), val));
                }
                case MixsonEventTypes.Deletion unused -> { if (runDeletionEvent(event)) return List.of(); }
                case MixsonEventTypes.Modification unused -> {
                    for (int i = 0; i < original.size(); i++) {
                        try {
                            if(modifiedEntries[i] == null) modifiedEntries[i] = JsonParser.parseReader(original.get(i).openAsReader());
                            JsonElement elem = modifiedEntries[i].getAsJsonObject();
                            modifiedEntries[i] = runModificationEvent(event, elem);
                        } catch (Exception e) {
                            error(e, event);
                        }
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + event.event());
            }
        }
        for (int i = 0; i < original.size(); i++) if(modifiedEntries[i] != null) {
            Resource resource = original.get(i);
            original.set(i, buildResource(resource, modifiedEntries[i]));
        }
        return original;
    }

    public static Map<ResourceLocation, Resource> runEvents(Map<ResourceLocation, Resource> original) {
        HashMap<ResourceLocation, JsonElement> modifiedEntries = new HashMap<>();
        for (List<AssociatedMixsonEvent> eventSet : Mixson.events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
            if (!original.containsKey(event.resourceId())) continue;
            switch (event.event()) {
                case MixsonEventTypes.Creation unused -> {
                    JsonElement val = runCreationEvent(event);
                    if(val == null) continue;
                    original.put(event.eventId(), buildResource(original.get(event.resourceId()), val));
                }
                case MixsonEventTypes.Deletion unused -> { if (runDeletionEvent(event)) original.remove(event.resourceId()); }
                case MixsonEventTypes.Modification unused -> {
                    try {
                        JsonElement elem = modifiedEntries.getOrDefault(event.resourceId(), JsonParser.parseReader(original.get(event.resourceId()).openAsReader()));
                        modifiedEntries.put(event.resourceId(), runModificationEvent(event, elem));
                    } catch (Exception e) {
                        error(e, event);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + event.event());
            }
        }
        for (Map.Entry<ResourceLocation, JsonElement> modifiedEntry : modifiedEntries.entrySet()) {
            Resource resource = original.get(modifiedEntry.getKey());
            if(resource == null) throw new IllegalStateException("resource was removed before modifications could be applied");
            original.put(modifiedEntry.getKey(), buildResource(resource, modifiedEntry.getValue()));
        }
        return original;
    }

    // INTERNAL RUN METHODS

    private static boolean runDeletionEvent(AssociatedMixsonEvent event) {
        logAction("Running Deletion Event '{}' on resource '{}'", event.eventId(), event.resourceId());
        if(event.referenceIds().length == 0) {
            if(event.event() instanceof DeletionEvent simpleEvent) return simpleEvent.run();
            else throw new MixsonError("Creation Events with no resource references must be of type DeletionEvent");
        } else {
            if(event.event() instanceof AdvancedDeletionEvent advancedEvent) {
                return advancedEvent.run(buildUsableReferences(event));
            }
            else throw new MixsonError("Creation Events with resource references must be of type AdvancedDeletionEvent");
        }
    }

    private static JsonElement runCreationEvent(AssociatedMixsonEvent event) {
        logAction("Running Creation Event '{}' on resource '{}'", event.eventId(), event.resourceId());
        if(event.referenceIds().length == 0) {
            if(event.event() instanceof CreationEvent simpleEvent) return simpleEvent.run();
            else throw new MixsonError("Creation Events with no resource references must be of type CreationEvent");
        } else {
            if(event.event() instanceof AdvancedCreationEvent advancedEvent) {
                return advancedEvent.run(buildUsableReferences(event));
            }
            else throw new MixsonError("Creation Events with resource references must be of type AdvancedCreationEvent");
        }
    }

    private static JsonElement runModificationEvent(AssociatedMixsonEvent event, JsonElement elem) {
        logAction("Running Modification Event '{}' on resource '{}'", event.eventId(), event.resourceId());
        if(event.referenceIds().length == 0) {
            if(event.event() instanceof ModificationEvent simpleEvent) return simpleEvent.run(elem);
            else throw new MixsonError("Modification Events with no resource references must be of type ModificationEvent");
        } else {
            if(event.event() instanceof AdvancedModificationEvent advancedEvent) {
                return advancedEvent.run(elem, buildUsableReferences(event));
            }
            else throw new MixsonError("Modification Events with resource references must be of type AdvancedModificationEvent");
        }
    }

    // MISC.

    private static void error(Exception e, AssociatedMixsonEvent event) {
        String errorString = String.format("Failed to modify json file '%s' with event '%s'\n", event.resourceId(), event.eventId());
        if(event.silentlyFail()) LOGGER.error(errorString, e);
        else throw new MixsonError(errorString+e);
    }

    public static boolean removeEvent(ResourceLocation eventId) {
        logAction("Removing event '{}'", eventId);
        for(List<AssociatedMixsonEvent> eventSet : events.values())
            for(AssociatedMixsonEvent event : eventSet) if(event.eventId().equals(eventId)) {
                eventSet.remove(event);
                return true;
            }
        return false;
    }

    // DEBUGGING STUFF

    /**
     * Beta Feature for an upcoming version. Not fully implemented
     **/
    public static void setDebugMode(DebugMode debugMode) {
        Mixson.debugMode = debugMode;
        LOGGER.info("Mixson Debug Mode has been set to: {}", debugMode);
    }

    private static void logAction(String action, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.info(action, args);
    }

    public enum DebugMode {

        OFF,
        LOG,
        EXPORT

    }

    // BUILD METHODS

    private static Resource buildResource(Resource assosiatedResource, JsonElement elem) {
        return new Resource(assosiatedResource.source(), () -> new ByteArrayInputStream(elem.toString().getBytes()), assosiatedResource::metadata);
    }

    private static HashMap<ResourceLocation, BuiltResourceReference> buildUsableReferences(AssociatedMixsonEvent event) {
        HashMap<ResourceLocation, BuiltResourceReference> gatheredReferences = new HashMap<>();
        for(int i = 0; i < event.referenceIds().length; i++) {
            BuiltResourceReference ref = references.get(event.referenceIds()[i]);
            gatheredReferences.put(ref.getReferenceId(), ref);
        }
        return gatheredReferences;
    }

    private static Map.Entry<UUID[], Integer> buildReferences(ResourceLocation eventId, boolean silentlyFail, ResourceReference... references) {
        UUID[] referenceIds = new UUID[references.length];
        int highest = DEFAULT_PRIORITY;
        for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
            ResourceReference ref = references[i];
            if(ref.priority() > highest) highest = ref.priority();
            UUID referenceUUID = UUID.randomUUID();
            referenceIds[i] = referenceUUID;
            register(ref.priority(), ref.resourceId(), ResourceLocation.fromNamespaceAndPath("mixson", "reference_event_" + eventId.getPath()), (ModificationEvent) (elem) -> {
                Mixson.references.get(referenceUUID).fulfill(elem);
                return elem;
            }, silentlyFail);
            Mixson.references.put(referenceUUID, new BuiltResourceReference(ref.resourceId(), ref.referenceId()));
        }
        return Map.entry(referenceIds, highest);
    }
}
