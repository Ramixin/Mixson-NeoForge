package net.ramixin.mixson;


import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.ramixin.mixson.debug.CallCountEntry;
import net.ramixin.mixson.debug.DebugMode;
import net.ramixin.mixson.debug.MixsonCommand;
import net.ramixin.mixson.events.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Mod("mixson")
public final class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static DebugMode debugMode = DebugMode.OFF;
    private static final TreeMap<Integer, List<AssociatedMixsonEvent>> events = new TreeMap<>();
    private static final HashMap<ResourceLocation, CallCountEntry> callCounts = new HashMap<>();
    private static final HashMap<UUID, BuiltResourceReference> references = new HashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final int DEFAULT_PRIORITY = 1000;


    public Mixson(IEventBus bus, ModContainer container) {
        NeoForge.EVENT_BUS.register(MixsonCommand.class);
        //TestModInitializer.onInitialize();
    }

    // REGISTRATION METHODS

    public static void registerModificationEvent(ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event) {
        registerModificationEvent(DEFAULT_PRIORITY, resourceId, eventId, event);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event) {
        registerModificationEvent(priority, resourceId, eventId, event, false);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final ModificationEvent event, boolean silentlyFail) {
        register(priority, resourceId, eventId, event, silentlyFail, false);
    }

    public static void registerModificationEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final AdvancedModificationEvent event, boolean silentlyFail, final ResourceReference... references) {
        register(priority, resourceId, eventId, event, silentlyFail, false, buildReferences(eventId, silentlyFail, references).getKey());
    }

    public static void registerCreationEvent(ResourceLocation associatedResourceId, ResourceLocation resourceId, final CreationEvent event, boolean silentlyFail) {
        register(DEFAULT_PRIORITY, associatedResourceId, resourceId, event, silentlyFail, false);
    }

    public static void registerCreationEvent(ResourceLocation associatedResourceId, ResourceLocation resourceId, final AdvancedCreationEvent event, boolean silentlyFail, final ResourceReference... references) {
        Map.Entry<UUID[], Integer> pair = buildReferences(resourceId, silentlyFail, references);
        register(pair.getValue(), associatedResourceId, resourceId, event, silentlyFail, false, pair.getKey());
    }

    public static void registerDeletionEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final DeletionEvent event, boolean silentlyFail) {
        register(priority, resourceId, eventId, event, silentlyFail, false);
    }

    public static void registerDeletionEvent(int priority, ResourceLocation resourceId, ResourceLocation eventId, final AdvancedDeletionEvent event, boolean silentlyFail, final ResourceReference... references) {
        register(priority, resourceId, eventId, event, silentlyFail, false);
    }

    private static void register(int priority, ResourceLocation resourceId, ResourceLocation eventId, MixsonEventTypes.BaseEvent<?> event, boolean silentlyFail, boolean referenceEvent, final UUID... referenceIds) {
        if(!referenceEvent) logEventRegistration(event, eventId, resourceId, priority);
        List<AssociatedMixsonEvent> eventSet;
        if(events.get(priority) == null) eventSet = new ArrayList<>();
        else eventSet = events.get(priority);
        eventSet.add(new AssociatedMixsonEvent(resourceId.withSuffix(".json"), eventId.withSuffix(".json"), event, silentlyFail, referenceEvent, referenceIds));
        events.put(priority, eventSet);
    }

    // EXTERNAL RUN METHODS

    public static List<Resource> runNamespaceEvents(List<Resource> original, ResourceLocation id) {
        JsonElement[] modifiedEntries = new JsonElement[original.size()];
        for (List<AssociatedMixsonEvent> eventSet : events.values()) for (AssociatedMixsonEvent event : eventSet) {
            if(!event.resourceId().equals(id)) continue;
            int ordinal = event.event().ordinal();
            boolean runAll = ordinal == -1;
            incrementCallCounts(event, runAll ? original.size() : 1);
            if(runAll) for (int i = original.size()-1; i >= 0; i--) processSingleNamespaceEvent(original, event, i, modifiedEntries);
            else {
                if(ordinal < 0 || ordinal >= original.size()) ordinalError(ordinal, original.size()-1, event);
                processSingleNamespaceEvent(original, event, ordinal, modifiedEntries);
            }
        }
        for (int i = 0; i < original.size(); i++) if(modifiedEntries[i] != null) {
            Resource resource = original.get(i);
            original.set(i, buildResource(resource, modifiedEntries[i]));
        }
        return original;
    }

    public static Map<ResourceLocation, Resource> runStandardEvents(Map<ResourceLocation, Resource> original) {
        HashMap<ResourceLocation, JsonElement> modifiedEntries = new HashMap<>();
        for (List<AssociatedMixsonEvent> eventSet : events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
            if (!original.containsKey(event.resourceId())) continue;
            int ordinal = event.event().ordinal();
            if(ordinal != -1 && ordinal != 0) ordinalError(ordinal, 0, event);
            incrementCallCounts(event, 1);
            logEventRun(event);
            switch (event.event()) {
                case MixsonEventTypes.Creation creEvent -> {
                    JsonElement val = creEvent.runEvent(event, null, buildUsableReferences(event));
                    if(val == null) continue;
                    exportJson(gson.toJson(val), event);
                    original.put(event.eventId(), buildResource(original.get(event.resourceId()), val));
                }
                case MixsonEventTypes.Deletion delEvent -> {
                    if (delEvent.runEvent(event, null, buildUsableReferences(event))) {
                        exportJson("[ \"resource was deleted\" ]", event);
                        original.remove(event.resourceId());
                    }
                }
                case MixsonEventTypes.Modification modEvent -> {
                    try {
                        JsonElement elem;
                        if(modifiedEntries.containsKey(event.resourceId())) elem = modifiedEntries.get(event.resourceId());
                        else elem = JsonParser.parseReader(original.get(event.resourceId()).openAsReader());
                        JsonElement modifiedElem = modEvent.runEvent(event, elem, buildUsableReferences(event));
                        exportJson(gson.toJson(modifiedElem), event);
                        modifiedEntries.put(event.resourceId(), modifiedElem);
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

    public static Map<ResourceLocation, List<Resource>> runListEvents(Map<ResourceLocation, List<Resource>> original) {
        HashMap<Map.Entry<ResourceLocation, Integer>, JsonElement> modifiedEntries = new HashMap<>();
        for (List<AssociatedMixsonEvent> eventSet : events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
            if (!original.containsKey(event.resourceId())) continue;
            List<Resource> resources = original.get(event.resourceId());
            int ordinal = event.event().ordinal();
            boolean runAll = ordinal == -1;
            incrementCallCounts(event, runAll ? resources.size() : 1);
            if(runAll) for (int i = resources.size()-1; i >= 0; i--) processSingleListEvent(original, event, resources, i, modifiedEntries);
            else {
                if(event.event().ordinal() >= resources.size() || ordinal < 0) ordinalError(ordinal, resources.size()-1, event);
                processSingleListEvent(original, event, resources, ordinal, modifiedEntries);
            }
        }
        for (Map.Entry<Map.Entry<ResourceLocation, Integer>, JsonElement> modifiedEntry : Set.copyOf(modifiedEntries.entrySet())) {
            Map.Entry<ResourceLocation, Integer> ordinatedIdentifier = modifiedEntry.getKey();
            List<Resource> resources = original.get(ordinatedIdentifier.getKey());
            Resource resourceToMod = resources.get(ordinatedIdentifier.getValue());
            resources.set(ordinatedIdentifier.getValue(), buildResource(resourceToMod, modifiedEntry.getValue()));
        }
        return original;
    }

    // INTERNAL PROCESS METHODS

    private static void processSingleNamespaceEvent(List<Resource> original, AssociatedMixsonEvent event, int ordinal, JsonElement[] modifiedEntries) {
        logEventRun(event);
        switch (event.event()) {
            case MixsonEventTypes.Creation creEvent -> {
                JsonElement val = creEvent.runEvent(event, null, buildUsableReferences(event));
                if(val == null) return;
                exportJson(gson.toJson(val), event);
                original.add(buildResource(original.getFirst(), val));
            }
            case MixsonEventTypes.Deletion delEvent -> {
                if (delEvent.runEvent(event, null, buildUsableReferences(event))) {
                    exportJson("[ \"resource was deleted\" ]", event);
                    original.remove(ordinal);
                }
            }
            case MixsonEventTypes.Modification modEvent -> {
                JsonArray array = new JsonArray();
                for (int i = 0; i < original.size(); i++) {
                    try {
                        if(modifiedEntries[i] == null) modifiedEntries[i] = JsonParser.parseReader(original.get(i).openAsReader());
                        JsonElement elem = modifiedEntries[i].getAsJsonObject();
                        JsonElement modifiedElem =modEvent.runEvent(event, elem, buildUsableReferences(event));
                        array.add(modifiedElem);
                        modifiedEntries[i] = modifiedElem;
                    } catch (Exception e) {
                        error(e, event);
                    }
                }
                exportJson(gson.toJson(array), event);
            }
            default -> throw new IllegalStateException("Unexpected value: " + event.event());
        }
    }

    private static void processSingleListEvent(Map<ResourceLocation, List<Resource>> original, AssociatedMixsonEvent event, List<Resource> resources, int ordinal, HashMap<Map.Entry<ResourceLocation, Integer>, JsonElement> modifiedEntries) {
        Resource resource = resources.get(ordinal);
        logEventRun(event);
        switch (event.event()) {
            case MixsonEventTypes.Creation creEvent -> {
                JsonElement val = creEvent.runEvent(event, null, buildUsableReferences(event));
                if(val == null) return;
                exportJson(gson.toJson(val), event);
                if(original.containsKey(event.eventId())) resources.add(buildResource(resource, val));
                else {
                    List<Resource> newList = new ArrayList<>(1);
                    newList.add(buildResource(resource, val));
                    original.put(event.eventId(), newList);
                }
            }
            case MixsonEventTypes.Deletion delEvent -> {
                if (delEvent.runEvent(event, null, buildUsableReferences(event))) {
                    exportJson("[ \"resource was deleted\" ]", event);
                    resources.remove(ordinal);
                }
            }
            case MixsonEventTypes.Modification modEvent -> {
                try {
                    JsonElement elem;
                    Map.Entry<ResourceLocation, Integer> ordinatedIdentifier = Map.entry(event.resourceId(), ordinal);
                    if(modifiedEntries.containsKey(ordinatedIdentifier)) elem = modifiedEntries.get(ordinatedIdentifier);
                    else elem = JsonParser.parseReader(resource.openAsReader());
                    JsonElement modifiedElem = modEvent.runEvent(event, elem, buildUsableReferences(event));
                    exportJson(gson.toJson(modifiedElem), event);
                    modifiedEntries.put(Map.entry(event.resourceId(), ordinal), modifiedElem);
                } catch (Exception e) {
                    error(e, event);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + event.event());
        }
    }

    // MISC.

    private static void error(Exception e, AssociatedMixsonEvent event) {
        String errorString = String.format("Failed to modify json file '%s' with event '%s'\n", event.resourceId(), event.eventId());
        if(event.silentlyFail()) LOGGER.error(errorString, e);
        else throw new MixsonError(errorString+e);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, AssociatedMixsonEvent event) {
        error(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), event);
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

    public static void setDebugMode(DebugMode debugMode) {
        Mixson.debugMode = debugMode;
        LOGGER.info("Mixson Debug Mode has been set to: {}", debugMode);
    }

    private static void logEventRun(AssociatedMixsonEvent event) {
        logAction("Running {} '{}' on resource '{}'", event.event().getName(), event.eventId(), event.resourceId());
    }

    private static void logEventRegistration(MixsonEventTypes.BaseEvent<?> event, ResourceLocation eventId, ResourceLocation resourceId, int priority) {
        logAction("Registering {} '{}' on resource '{}' with priority {}", event.getName(), eventId, resourceId, priority);
    }

    private static void logAction(String action, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.info(action, args);
    }

    private static void exportJson(String text, AssociatedMixsonEvent event) {
        if(debugMode.ordinal() <= 1) return;
        if(event.referenceEvent()) return;
        Path dir = FMLPaths.GAMEDIR.get().resolve(".mixson").resolve(identifierToPathString(event.resourceId()));
        try {
            Files.createDirectories(dir);
            FileWriter writer = new FileWriter(dir.resolve(identifierToPathString(event.eventId())+".json").toFile());
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            Mixson.LOGGER.error("failed to export debug file", e);
        }

    }

    private static String identifierToPathString(ResourceLocation ResourceLocation) {
        return ResourceLocation.getNamespace() + '~' + ResourceLocation.getPath().replaceFirst("\\.json", "").replaceAll("/", "-");
    }

    private static void incrementCallCounts(AssociatedMixsonEvent event,  int fileOperations) {
        if(event.referenceEvent()) return;
        CallCountEntry pair = callCounts.getOrDefault(event.eventId(), CallCountEntry.DEFAULT);
        callCounts.put(event.eventId(), pair.update(fileOperations));
    }
    public static void assertEventRan(ResourceLocation eventId) {
        if(callCounts.getOrDefault(eventId, CallCountEntry.DEFAULT).eventCalls() == 0) throw new AssertionError(String.format("event '%s' was not run", eventId));
    }
    public static void assertEventRan(ResourceLocation eventId, int callCount) {
        CallCountEntry pair = callCounts.getOrDefault(eventId, CallCountEntry.DEFAULT);
        if(pair.eventCalls() != callCount) throw new AssertionError(String.format("event '%s' was expected to run %s time(s), but only ran %s time(s)", eventId, callCount, pair.eventCalls()));
    }
    public static void assertEventRan(ResourceLocation eventId, int callCount, int fileOperations) {
        CallCountEntry pair = callCounts.getOrDefault(eventId, CallCountEntry.DEFAULT);
        if(pair.eventCalls() != callCount) throw new AssertionError(String.format("event '%s' was expected to run %s time(s), but only ran %s time(s)", eventId, callCount, pair.eventCalls()));
        if(pair.fileOperations() != fileOperations) throw new AssertionError(String.format("event '%s' was expected to operation on  %s file(s), but only operated on %s file(s)", eventId, fileOperations, pair.fileOperations()));
    }


    static {

        try {

            FileUtils.deleteDirectory(FMLPaths.GAMEDIR.get().resolve(".mixson").toFile());
        } catch (IOException e) {
            Mixson.LOGGER.error("failed to delete .mixson debug directory", e);
        }

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
            }, silentlyFail, true);
            Mixson.references.put(referenceUUID, new BuiltResourceReference(ref.resourceId(), ref.referenceId()));
        }
        return Map.entry(referenceIds, highest);
    }

    public static void clearCalls() {
        callCounts.clear();
    }
    public static List<ResourceLocation> callCountsSet() {
        return callCounts
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    public static CallCountEntry getCallCount(ResourceLocation eventId) {
        return callCounts.getOrDefault(eventId, CallCountEntry.DEFAULT);
    }
}
