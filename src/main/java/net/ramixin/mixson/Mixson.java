package net.ramixin.mixson;


import com.google.gson.JsonElement;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;
import net.ramixin.mixson.entries.AbstractEntry;
import net.ramixin.mixson.entries.EventEntry;
import net.ramixin.mixson.entries.ReferenceEntry;
import net.ramixin.mixson.enums.DebugOption;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.AbstractHook;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static net.ramixin.mixson.util.MixsonUtil.*;

@SuppressWarnings("unused")
public final class Mixson {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mixson");
    private static int debugOptionFlags = 0;
    private static final MixsonRegistry<MixsonEvent<?>> eventRegistry = new MixsonRegistry<>(MixsonEvent::uuid, MixsonEvent::priority);
    private static final MixsonRegistry<ResourceReference<?>> referenceRegistry = new MixsonRegistry<>(ResourceReference::getUuid, ResourceReference::getPriority);

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static final int DEFAULT_PRIORITY = 1000;

    private Mixson() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // EVENT REGISTRATION METHODS

    public static UUID registerEvent(int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<JsonElement> event) {
        return registerEvent(MixsonCodecs.JSON_ELEMENT, priority, lifetime, errorPolicy, eventName, resourcePredicate, event);
    }

    public static <T> UUID registerEvent(MixsonCodec<T> codec, int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<T> event) {
        return registerEvent(new MixsonEventBuilder<T>()
                .setCodec(codec)
                .setPriority(priority)
                .setLifetime(lifetime)
                .setErrorPolicy(errorPolicy)
                .setEventName(eventName)
                .setResourcePredicate(resourcePredicate)
                .setEvent(event)
        );
    }

    public static <T> UUID registerEvent(MixsonEventBuilder<T> builder) {
        MixsonEvent<T> builtEvent = builder.build();
        if(builtEvent.lifetime() == Lifetime.DEFERRED)
            return eventRegistry.registerDeferred(builtEvent);
        else
            return eventRegistry.register(builtEvent);
    }

    public static ResourceReference<JsonElement> registerReference(int priority, Index index, String referenceName) {
        return registerReference(MixsonCodecs.JSON_ELEMENT, priority, index, referenceName);
    }

    public static <T> ResourceReference<T> registerReference(MixsonCodec<T> codec, int priority, Index index, String referenceName) {
        return registerReference(new ResourceReferenceBuilder<T>()
                .setCodec(codec)
                .setIndex(index)
                .setReferenceName(referenceName)
                .setPriority(priority)
        );
    }

    public static <T> ResourceReference<T> registerReference(ResourceReferenceBuilder<T> builder) {
        ResourceReference<T> ref = builder.build();
        referenceRegistry.register(ref);
        return ref;
    }

    // EXTERNAL RUN METHODS

    public static <T> T processHook(AbstractHook<T> hook) {
        lock.readLock().lock();
        MixsonRuntime<T> runtime = new MixsonRuntime<>(hook, eventRegistry, referenceRegistry, LOGGER::error);
        while(runtime.isRunning()) {
            AbstractEntry entry = runtime.pop();
            switch(entry) {
                case ReferenceEntry<?> referenceEntry -> handleReference(referenceEntry, runtime);
                //noinspection rawtypes
                case EventEntry eventEntry -> //noinspection unchecked
                        handleEvent(eventEntry, runtime);
                default -> throw new IllegalStateException("Unexpected value: " + entry);
            }
        }
        lock.readLock().unlock();
        return hook.getAttachedResources();
    }

    private static <T, R> void handleReference(ReferenceEntry<R> referenceEntry, MixsonRuntime<T> runtime) {
        ResourceReference<R> ref = referenceEntry.reference();
        try {
            processReference(ref, runtime);
        } catch (IOException e) {
            runtime.error(e, ref, ref.getResourceId());
        }
    }

    private static <T, R> void processReference(ResourceReference<R> ref, MixsonRuntime<T> runtime) throws IOException {
        Optional<List<Resource>> maybeResource = runtime.getHook().captureFiles(ref.getIndex(), ref.getCodec().extensionAndDot());
        if(maybeResource.isEmpty()) return;
        List<Resource> resource = maybeResource.get();
        if(resource.isEmpty()) return;
        if(resource.size() > 1) {
            runtime.error(new MixsonException("resource reference cannot match more than 1 resource"), ref, ref.getIndex().id());
            return;
        }
        R file = ref.getCodec().deserialize(resource.getFirst());
        ref.fulfill(file);
    }


    private static <T, R> void handleEvent(EventEntry<T> eventEntry, MixsonRuntime<R> runtime) {
        MixsonEvent<T> event = eventEntry.event();
        List<Map.Entry<Index, Resource>> entries = runtime.getHook().getMatching(event.getWrappedPredicate());
        if(entries.isEmpty() && eventEntry.event().assertive()) throw new MixsonException("assertion on event '%s' failed", event.eventName());
        if(entries.isEmpty()) return;
        if(event.lifetime() == Lifetime.ONCE)
            removeEvent(event.uuid());
        entries.sort(Comparator
                .comparing((Map.Entry<Index, Resource> o) -> o.getKey().id())
                .thenComparingInt(o -> o.getKey().ordinal())
        );
        Set<Index> markedForDeletion = new HashSet<>();
        logExtra("begun processing event '{}'", event.eventName());
        long fileStartTime = System.nanoTime();
        for(Map.Entry<Index, Resource> resourceEntry : entries) {
            try {
                processEvent(eventEntry, runtime, resourceEntry, markedForDeletion);
            } catch (IOException e) {
                runtime.error(e, event, resourceEntry.getKey().id());
            }
        }
        String ext = eventEntry.event().codec().extensionAndDot();
        for(Index deletionIndex : markedForDeletion.stream().sorted().toList())
            runtime.getHook().delete(deletionIndex, ext);
        logExtra("successfully finished processing event '{}' in {}", event.eventName(), timestamp(fileStartTime));
    }

    private static <T, R> void processEvent(EventEntry<T> eventEntry, MixsonRuntime<R> runtime, Map.Entry<Index, Resource> resourceEntry, Set<Index> markedForDeletion) throws IOException {
        MixsonEvent<T> event = eventEntry.event();
        AbstractHook<R> hook = runtime.getHook();
        T file = deserializeFile(event.codec(), resourceEntry.getValue(), error -> runtime.error(error, event, resourceEntry.getKey().id())).orElse(null);
        EventContext<T> context = new EventContext<>(file, resourceEntry.getKey(), eventEntry, runtime, resourceEntry, markedForDeletion.contains(resourceEntry.getKey()));
        if(getDebugFlag(DebugOption.EXPORT_UNPATCHED_FILE))
            exportDebugFile(event.codec(), file, event.eventName(), resourceEntry.getKey().id().toString(), event.codec().extensionAndDot(), false);
        logBasic("Running '{}' on resource '{}'", event.eventName(), resourceEntry.getKey().id());
        long fileStartTime = System.nanoTime();
        event.event().runEvent(context);
        logExtra("Finished running '{}' on resource '{}' in {}", event.eventName(), resourceEntry.getKey().id(), timestamp(fileStartTime));
        Optional<T> debugExport = context.getDebugExport();
        if(debugExport.isPresent() && getDebugFlag(DebugOption.EXPORT_PATCHED_FILE))
            exportDebugFile(event.codec(), debugExport.get(), event.eventName(), resourceEntry.getKey().id().toString(), event.codec().extensionAndDot(), true);
        if(context.isMarkedForDeletion()) markedForDeletion.add(resourceEntry.getKey());
        else markedForDeletion.remove(resourceEntry.getKey());
        for(UUID cancelledFuture : context.getCancelledFutures())
            runtime.cancelEvent(cancelledFuture);
        context.cleanupCapturedFiles();
        for(UUID uuid : context.getPulledFutures()) {
            Optional<MixsonEvent<?>> pulledDeferred = eventRegistry.pullDeferred(uuid);
            if(pulledDeferred.isPresent()) {
                runtime.insertEntry(new EventEntry<>(pulledDeferred.get().priority(), pulledDeferred.get()));
                continue;
            }
            Optional<MixsonEvent<?>> pulledEvent = eventRegistry.get(uuid);
            if(pulledEvent.isPresent()) {
                runtime.insertEntry(new EventEntry<>(pulledEvent.get().priority(), pulledEvent.get()));
                continue;
            }
            Optional<ResourceReference<?>> pulledRef = referenceRegistry.get(uuid);
            if(pulledRef.isPresent()) {
                runtime.insertEntry(new ReferenceEntry<>(pulledRef.get().getPriority(), pulledRef.get()));
                continue;
            }
            throw new IllegalArgumentException("failed to locate event or reference with uuid of "+uuid);
        }

        hook.insert(resourceEntry.getKey(), event.codec().serialize(resourceEntry.getValue(), context.getFile()), event.codec().extensionAndDot(), true);
        for(Map.Entry<Index, T> createdResource : context.getCreatedResources().entrySet())
            hook.insert(createdResource.getKey(), event.codec().serialize(resourceEntry.getValue(), createdResource.getValue()), event.codec().extensionAndDot(), false);
    }

    // ERRORS

    static void registrationError(Exception e, ErrorMessageProvider errorMessageProvider) {
        if(errorMessageProvider.getErrorPolicy() != ErrorPolicy.THROW) LOGGER.error(errorMessageProvider.getRegistrationErrorMessage(), e);
        else throw new MixsonException(errorMessageProvider.getRegistrationErrorMessage(), e);
    }

    // MISC. PUBLICS

    /** @Deprecated Use {@link #removeEvent(UUID)} or {@link #removeReference(UUID)} instead**/
    @Deprecated
    public static boolean remove(UUID uuid) {
        return removeEvent(uuid) || removeReference(uuid);
    }

    public static boolean removeEvent(UUID uuid) {
        return eventRegistry.unregister(uuid);
    }

    public static boolean removeReference(UUID uuid) {
        return referenceRegistry.unregister(uuid);
    }

    /** @Deprecated Use {@link #hasEvent(UUID)} or {@link #hasReference(UUID)} instead**/
    @Deprecated
    public static boolean has(UUID uuid) {
        return hasEvent(uuid) || hasReference(uuid);
    }

    public static boolean hasEvent(UUID uuid) {
        return eventRegistry.contains(uuid);
    }

    public static boolean hasReference(UUID uuid) {
        return referenceRegistry.contains(uuid);
    }

    public static String getEventName(UUID uuid) {
        MixsonEvent<?> event = eventRegistry.get(uuid).orElseThrow();
        return event.eventName();
    }

    public static Runnable lockEventProcessing() {
        lock.writeLock().lock();
        return () -> lock.writeLock().unlock();
    }

    // DEBUGGING STUFF

    public static void enableDebugOption(DebugOption option) {
        Mixson.debugOptionFlags |= option.getMask();
    }

    private static boolean getDebugFlag(DebugOption option) {
        return (Mixson.debugOptionFlags & option.getMask()) > 0;
    }

    static void logBasic(String action, Object... args) {
        if(getDebugFlag(DebugOption.BASIC_LOGGING)) LOGGER.info(action, args);
    }

    private static void logExtra(String action, Object... args) {
        if(getDebugFlag(DebugOption.EXTRA_LOGGING)) LOGGER.info(action, args);
    }

    private static <T> void exportDebugFile(MixsonCodec<T> codec, T resource, String eventName, String resourceId, String extension, boolean patched) {
        Path rawDir = FMLPaths.GAMEDIR.get().resolve(".mixson");
        Path patchDir;
        if(patched) patchDir = rawDir.resolve("patched");
        else patchDir = rawDir.resolve("unpatched");
        Path dir = patchDir.resolve(identifierToPathString(resourceId, extension));
        try {
            Files.createDirectories(dir);
            FileOutputStream fos = new FileOutputStream(dir.resolve(stringToUsablePath(eventName)+extension).toFile());
            fos.write(codec.export(resource).toByteArray());
            fos.close();
        } catch (IOException e) {
            LOGGER.error("failed to export debug file", e);
        }
    }

    static {
        try {
            FileUtils.deleteDirectory(FMLPaths.GAMEDIR.get().resolve(".mixson").toFile());
        } catch (Exception e) {
            LOGGER.error("failed to delete .mixson debug directory", e);
        }
    }
}
