package net.ramixin.mixson.inline;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.ramixin.mixson.HexRecord;
import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.atp.MixsonAnnotationProcessor;
import net.ramixin.mixson.debug.CallCountEntry;
import net.ramixin.mixson.debug.DebugMode;
import net.ramixin.mixson.debug.MixsonCommand;
import net.ramixin.mixson.inline.entries.AbstractEntry;
import net.ramixin.mixson.inline.entries.EventEntry;
import net.ramixin.mixson.inline.entries.ReferenceEntry;
import net.ramixin.mixson.util.ErrorMessageProvider;
import net.ramixin.mixson.util.MixsonUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static net.ramixin.mixson.util.MixsonUtil.*;

@SuppressWarnings("unused")
@Mod("mixson")
public final class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static DebugMode debugMode = DebugMode.OFF;
    private static final Map<UUID, BuiltMixsonEvent<?>> events = Collections.synchronizedMap(new HashMap<>());
    private static final SortedMap<Integer, List<BuiltMixsonEvent<?>>> orderedEvents = Collections.synchronizedSortedMap(new TreeMap<>());
    private static final Map<UUID, CallCountEntry> callCounts = Collections.synchronizedMap(new HashMap<>());
    private static final Map<UUID, BuiltResourceReference<?>> references = Collections.synchronizedMap(new HashMap<>());
    private static final SortedMap<Integer, List<BuiltResourceReference<?>>> orderedReferences = Collections.synchronizedSortedMap(new TreeMap<>());
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final int DEFAULT_PRIORITY = 1000;
    public static final MixsonCodec<JsonElement> JSON_ELEMENT_CODEC = MixsonCodec.of("json", r -> JsonParser.parseReader(r.openAsReader()), (r, x) -> new Resource(r.source(), () -> new ByteArrayInputStream(x.toString().getBytes()), r::metadata), gson::toJson);

    // REGISTRATION METHODS

    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, ResourceReference... references) {
        return registerEvent(priority, resourceId, eventName, event, false, references);
    }

    public static UUID registerEvent(int priority, String resourceId, String eventName, MixsonEvent<JsonElement> event, boolean silentlyFail, ResourceReference... references) {
        return registerEvent(JSON_ELEMENT_CODEC, priority, resourceId, eventName, event, silentlyFail, references);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference... references) {
        return finalizeEventRegistration(priority, buildMixsonEvent(codec, priority, resourceId, eventName, event, silentlyFail, references, Mixson::finalizeReferenceRegistration));
    }

    // SEPARATED REGISTRATION

    private static <T> UUID finalizeEventRegistration(int priority, BuiltMixsonEvent<T> builtEvent) {
        addComponent(builtEvent, priority, builtEvent.uuid(), events, orderedEvents);
        return builtEvent.uuid();
    }

    private static <T> void finalizeReferenceRegistration(int priority, BuiltResourceReference<T> builtReference) {
        addComponent(builtReference, priority, builtReference.getUuid(), references, orderedReferences);
    }

    private static <T> @NotNull BuiltMixsonEvent<T> buildMixsonEvent(MixsonCodec<T>  codec, int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean silentlyFail, ResourceReference[] references, BiConsumer<Integer, BuiltResourceReference<T>> referenceCallback) {
        boolean fail = event.ordinal() < 0 && event.ordinal() != -1;
        logEventRegistration(eventName, resourceId, priority);
        UUID[] referenceIds = new UUID[references.length];
        int highest = DEFAULT_PRIORITY;
        for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
            ResourceReference ref = references[i];
            if(ref.priority() > highest) highest = ref.priority();
            BuiltResourceReference<T> builtReference = new BuiltResourceReference<>(ref, codec);
            referenceIds[i] = builtReference.getUuid();
            referenceCallback.accept(i, builtReference);
        }

        BuiltMixsonEvent<T> builtEvent = new BuiltMixsonEvent<>(codec, resourceId, eventName, event, silentlyFail, referenceIds);
        if(fail) error(new MixsonError("event ordinal value cannot be negative"), builtEvent);
        return builtEvent;
    }

    // INIT

    public Mixson(IEventBus bus, net.neoforged.fml.ModContainer container) {
        ModList.get().forEachModContainer((unused, modContainer) -> {
            List<? extends IConfigurable> mixsonConfig = modContainer.getModInfo().getOwningFile().getConfig().getConfigList("mixson");
            for(IConfigurable configurable : mixsonConfig) {
                if(!(configurable instanceof NightConfigWrapper wrapper)) continue;
                Optional<Object> maybeEntries = wrapper.getConfigElement("config");
                if(maybeEntries.isEmpty()) continue;
                String id = modContainer.getModId();
                if(!(maybeEntries.get() instanceof List<?> list)) throw new IllegalStateException("config field in [[mixson]] section must be a list of strings in mod +'"+id+"'");
                for(Object entry : list) {
                    if(!(entry instanceof String className)) throw new IllegalStateException("config field in [[mixson]] section mustonly contain strings, but found: "+entry+" in mod '"+id+"'");
                    try {
                        MixsonAnnotationProcessor.processClass(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new MixsonError(String.format("class '%s' in 'mixson' field in mod '%s' does not exist", className, id));
                    }
                }
            }
        });
        NeoForge.EVENT_BUS.register(MixsonCommand.class);
        //TestModInitializer.onInitialize();
    }

    // EXTERNAL RUN METHODS

    public static Map<ResourceLocation, Resource> runStandardEvents(Map<ResourceLocation, Resource> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<ResourceLocation> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                ResourceLocation resourceId = ref.getResourceId();
                if(!original.containsKey(resourceId)) continue;
                if(ordinal >= 1) ordinalError(ordinal, 0, ref);
                fulfillReference(original.get(resourceId), ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            String extension = event.codec().extensionAndDot();
            String rawResourceId = event.resourceId();
            if(rawResourceId.endsWith("*")) {
                String prefix = removeWildcard(rawResourceId);
                for(ResourceLocation resourceId : original.keySet()) {
                    if(!resourceId.getPath().startsWith(prefix)) continue;
                    if(!resourceId.toString().endsWith(extension)) continue;
                    incrementCallCounts(event, original.size());
                    processStandardEvent(original, runtime, markedForDeletion, eventEntry, resourceId);
                }
                continue;
            }
            ResourceLocation resourceId = ResourceLocation.parse(rawResourceId).withSuffix(extension);
            if(!original.containsKey(resourceId)) continue;
            incrementCallCounts(event, original.size());
            processStandardEvent(original, runtime, markedForDeletion, eventEntry, resourceId);
        }
        filledReferences.forEach(uuid -> references.get(uuid).clear());
        for(ResourceLocation id : markedForDeletion) original.remove(id);
        return original;
    }

    private static <T> void fulfillReference(Resource resource, BuiltResourceReference<T> ref, Set<UUID> filledReferences) {
        try {
            T file = ref.getCodec().deserialize(resource);
            ref.fulfill(file);
            filledReferences.add(ref.getUuid());
        } catch (IOException e) {
            error(e, ref);
        }
    }

    public static Map<ResourceLocation, List<Resource>> runListEvents(Map<ResourceLocation, List<Resource>> original) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Pair<ResourceLocation, Integer>> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                if(!original.containsKey(ref.getResourceId())) continue;
                List<Resource> resources = original.get(ref.getResourceId());
                if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, ref);
                Resource resource = resources.get(ordinal);
                fulfillReference(resource, ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            String extension = event.codec().extensionAndDot();
            String rawResourceId = event.resourceId();
            if(rawResourceId.endsWith("*")) {
                String prefix = removeWildcard(rawResourceId);
                for(ResourceLocation resourceId : original.keySet()) {
                    if(!resourceId.getPath().startsWith(prefix)) continue;
                    if(!resourceId.toString().endsWith(extension)) continue;
                    incrementCallCounts(event, original.size());
                    prepareListEventProcessing(original, runtime, markedForDeletion, eventEntry, resourceId);
                }
                continue;
            }
            ResourceLocation resourceId = ResourceLocation.parse(rawResourceId).withSuffix(extension);
            if(!original.containsKey(resourceId)) continue;
            incrementCallCounts(event, original.get(resourceId).size());
            prepareListEventProcessing(original, runtime, markedForDeletion, eventEntry, resourceId);
        }
        filledReferences.forEach(uuid->references.get(uuid).clear());

        for(Pair<ResourceLocation, Integer> pairId : markedForDeletion) {
            List<Resource> resources = original.get(pairId.getFirst());
            resources.set(pairId.getSecond(), null);
        }
        for(ResourceLocation resourceId : original.keySet()) {
            List<Resource> resources = original.get(resourceId);
            resources.removeIf(Objects::isNull);
        }
        return original;
    }

    public static List<Resource> runNamespaceEvents(List<Resource> original, ResourceLocation id) {
        MixsonRuntime runtime = new MixsonRuntime(orderedEvents, orderedReferences);
        final Set<Integer> markedForDeletion = new HashSet<>();
        final Set<UUID> filledReferences = new HashSet<>();
        while(runtime.hasFinished()) {
            AbstractEntry entry = runtime.pop();
            if(entry instanceof ReferenceEntry<?> referenceEntry) {
                int ordinal = entry.getOrdinal();
                BuiltResourceReference<?> ref = referenceEntry.reference();
                if(ref.getResourceId() != id) continue;
                if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, ref);
                Resource resource = original.get(ordinal);
                fulfillReference(resource, ref, filledReferences);
                continue;
            }
            EventEntry<?> eventEntry = (EventEntry<?>) entry;
            BuiltMixsonEvent<?> event = eventEntry.event();
            String extension = event.codec().extensionAndDot();
            String rawResourceId = event.resourceId();
            if(rawResourceId.endsWith("*")) continue;
            if(!id.toString().endsWith(extension)) continue;
            ResourceLocation resourceId = ResourceLocation.parse(rawResourceId).withSuffix(extension);
            if(!id.equals(resourceId)) continue;
            int ordinal = eventEntry.getOrdinal();
            if(ordinal >= original.size()) ordinalError(ordinal, original.size()-1, event);
            if(ordinal == -1) {
                incrementCallCounts(event, original.size());
                for (int i = 0; i < original.size(); i++)
                    processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, resourceId, i);
            }
            else {
                incrementCallCounts(event, 1);
                processNamespaceEvent(original, runtime, markedForDeletion, eventEntry, resourceId, ordinal);
            }
        }
        filledReferences.forEach(uuid->references.get(uuid).clear());

        markedForDeletion.stream().sorted(Comparator.reverseOrder()).forEach(clazzInt -> original.remove((int) clazzInt));
        return original;
    }

    // INTERNAL RUN METHODS

    private static <T> void processNamespaceEvent(List<Resource> original, MixsonRuntime runtime, Set<Integer> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId, int i) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(i);
        Optional<T> file = getFile(event.codec(), resource, event, Mixson::error);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, i, file.get());
            for(T createdEntry : context.getIndexedCreatedResources())
                original.add(event.codec().serialize(resource, createdEntry));
            original.set(i, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            error(e, event);
        }
    }

    private static <T> void prepareListEventProcessing(Map<ResourceLocation, List<Resource>> original, MixsonRuntime runtime, Set<Pair<ResourceLocation, Integer>> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        List<Resource> resources = original.get(resourceId);
        int ordinal = eventEntry.getOrdinal();
        if(ordinal >= resources.size()) ordinalError(ordinal, resources.size()-1, event);
        if(ordinal == -1)
            for(int i = 0; i < resources.size(); i++)
                processListEvent(original, resourceId, resources, i, event, runtime, eventEntry, markedForDeletion);
        else
            processListEvent(original, resourceId, resources, ordinal, event, runtime, eventEntry, markedForDeletion);
    }

    private static <T> void processListEvent(Map<ResourceLocation, List<Resource>> original, ResourceLocation resourceId, List<Resource> resources, int ordinal, BuiltMixsonEvent<T> event, MixsonRuntime runtime, EventEntry<T> eventEntry, Set<Pair<ResourceLocation, Integer>> markedForDeletion) {
        Resource resource = resources.get(ordinal);
        Pair<ResourceLocation, Integer> pairedId = Pair.of(resourceId, ordinal);
        Optional<T> file = getFile(event.codec(), resources.get(ordinal), event, Mixson::error);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, pairedId, file.get());
            context.getCancelledFutures().forEach(runtime::cancelEvent);
            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet()) {
                List<Resource> createdResources = original.computeIfAbsent(createdEntry.getKey(), (unused) -> new ArrayList<>());
                createdResources.add(event.codec().serialize(resource, createdEntry.getValue()));
            }
            resources.set(ordinal, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            error(e, event);
        }
    }


    private static <T> void processStandardEvent(Map<ResourceLocation, Resource> original, MixsonRuntime runtime, Set<ResourceLocation> markedForDeletion, EventEntry<T> eventEntry, ResourceLocation resourceId) {
        BuiltMixsonEvent<T> event = eventEntry.event();
        Resource resource = original.get(resourceId);
        Optional<T> file = getFile(event.codec(), resource, event, Mixson::error);
        if (file.isEmpty()) return;
        try {
            EventContext<T> context = processContext(runtime, markedForDeletion, eventEntry, event, resourceId, resourceId, file.get());
            for(Map.Entry<ResourceLocation, T> createdEntry : context.getIdentifiedCreatedResources().entrySet())
                original.put(createdEntry.getKey(), event.codec().serialize(resource, createdEntry.getValue()));
            original.put(resourceId, event.codec().serialize(resource, context.getFile()));
        } catch (Exception e) {
            error(e, event);
        }
    }

    private static <N, T> @NotNull EventContext<T> processContext(MixsonRuntime runtime, Set<N> markedForDeletion, EventEntry<T> eventEntry, BuiltMixsonEvent<T> event, ResourceLocation resourceId, N indexer, T file) {
        EventContext<T> context = MixsonUtil.createContext(ContextCreationType.IDENTIFIED, resourceId, file, eventEntry, markedForDeletion.contains(indexer), uuid -> runtime.getReference(uuid, references::get));
        logEventRun(event);
        event.event().runEvent(context);
        exportDebugFile(event.codec().serializeOutputFile(context.getFile()), event);
        if(context.isMarkedForDeletion()) markedForDeletion.add(indexer);
        else markedForDeletion.remove(indexer);
        context.getCancelledFutures().forEach(runtime::cancelEvent);
        List<AbstractEntry> appendable = new ArrayList<>();
        for(HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]> createdEvent : context.getCreatedEvents()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.apply((integer, string, string2, mixsonEvent, aBoolean, resourceReferences) -> buildMixsonEvent(event.codec(), integer, string, string2, mixsonEvent, aBoolean, resourceReferences, (integer1, reference) -> {
                appendable.add(new ReferenceEntry<>(integer1, reference));
                finalizeReferenceRegistration(integer1, reference);
            }));
            appendable.add(new EventEntry<>(createdEvent.first(), builtEvent));
            finalizeEventRegistration(createdEvent.first(), builtEvent);
        }
        for(HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]> createdEvent : context.getCreatedRuntimeEvents()) {
            BuiltMixsonEvent<T> builtEvent = createdEvent.apply((integer, string, string2, mixsonEvent, aBoolean, resourceReferences) -> buildMixsonEvent(event.codec(), integer, string, string2, mixsonEvent, aBoolean, resourceReferences, (integer1, reference) -> appendable.add(new ReferenceEntry<>(integer1, reference))));
            appendable.add(new EventEntry<>(createdEvent.first(), builtEvent));
        }
        appendable.forEach(runtime::insertEntry);
        return context;
    }

    // ERRORS

    private static void error(Exception e, ErrorMessageProvider errorMessageProvider) {
        if(errorMessageProvider.failSilently()) LOGGER.error(errorMessageProvider.getMessage(), e);
        else throw new MixsonError(errorMessageProvider.getMessage()+e);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, BuiltMixsonEvent<?> event) {
        error(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), event);
    }

    private static void ordinalError(int ordinal, int maxOrdinal, BuiltResourceReference<?> reference) {
        error(new MixsonError("ordinal value '"+ordinal+"' points to no value. Max Ordinal Value: "+maxOrdinal), reference);
    }

    // MISC. PUBLICS

    public static boolean removeEvent(UUID uuid) {
        for(List<BuiltMixsonEvent<?>> eventSet : orderedEvents.values()) eventSet.removeIf(event -> event.uuid().equals(uuid));
        return events.remove(uuid) != null;
    }

    public static String getEventName(UUID uuid) {
        return events.get(uuid).eventName();
    }

    // DEBUGGING STUFF

    public static void setDebugMode(DebugMode debugMode) {
        Mixson.debugMode = debugMode;
        LOGGER.info("Mixson Debug Mode has been set to: {}", debugMode);
    }

    private static void logEventRun(BuiltMixsonEvent<?> event) {
        logEventRun(event.eventName(), ResourceLocation.parse(event.resourceId()));
    }

    private static void logEventRun(String eventName, ResourceLocation resourceId) {
        logAction("Running '{}' on resource '{}'", eventName, resourceId);
    }

    private static void logEventRegistration(String eventName, String resourceId, int priority) {
        logAction("Registering '{}' on resource '{}' with priority {}", eventName, resourceId, priority);
    }

    private static void logAction(String action, Object... args) {
        if(debugMode.ordinal() > 0) LOGGER.info(action, args);
    }

    private static void incrementCallCounts(BuiltMixsonEvent<?> event, int fileOperations) {
        CallCountEntry pair = callCounts.getOrDefault(event.uuid(), CallCountEntry.DEFAULT);
        callCounts.put(event.uuid(), pair.update(fileOperations));
    }

    private static void exportDebugFile(String text, BuiltMixsonEvent<?> event) {
        if(debugMode.ordinal() <= 1) return;
        Path dir = FMLPaths.GAMEDIR.get().resolve(".mixson").resolve(identifierToPathString(ResourceLocation.parse(event.resourceId())));
        try {
            Files.createDirectories(dir);
            FileWriter writer = new FileWriter(dir.resolve(stringToUsablePath(event.eventName())+".json").toFile());
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            LOGGER.error("failed to export debug file", e);
        }

    }

    public static void clearCalls() {
        callCounts.clear();
    }

    public static List<UUID> getCallCountsOrder() {
        return callCounts
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static CallCountEntry getCallCount(UUID uuid) {
        return callCounts.getOrDefault(uuid, CallCountEntry.DEFAULT);
    }

    static {

        try {
            FileUtils.deleteDirectory(FMLPaths.GAMEDIR.get().resolve(".mixson").toFile());
        } catch (IOException e) {
            LOGGER.error("failed to delete .mixson debug directory", e);
        }

    }

}
