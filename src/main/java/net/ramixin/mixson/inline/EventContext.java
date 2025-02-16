package net.ramixin.mixson.inline;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.HexRecord;
import net.ramixin.mixson.inline.entries.EventEntry;

import java.util.*;

@SuppressWarnings("unused")
public class EventContext<T> {

    private final ContextCreationType creationType;
    private final T file;
    private final ResourceLocation resourceId;
    private final EventEntry<T> entry;
    private final HashMap<ResourceLocation, BuiltResourceReference<T>> references = new HashMap<>();
    private boolean markedForDeletion;
    private final Set<UUID> cancelledFutures = new HashSet<>();
    private final HashMap<ResourceLocation, T> identifiedCreatedResources = new HashMap<>();
    private final List<T> indexedCreatedResources = new ArrayList<>();
    private final List<HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]>> createdRuntimeEvents = new ArrayList<>();
    private final List<HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]>> createdEvents = new ArrayList<>();

    public EventContext(ContextCreationType creationType, T file, ResourceLocation resourceId, EventEntry<T> entry, boolean markedForDeletion, BuiltResourceReference<T>[] references) {
        this.creationType = creationType;
        this.file = file;
        this.resourceId = resourceId;
        this.entry = entry;
        this.markedForDeletion = markedForDeletion;
        for(BuiltResourceReference<T> ref : references) this.references.put(ref.getReferenceId(), ref);
    }

    public T getFile() {
        return this.file;
    }

    public ResourceLocation getResourceId() {
        return this.resourceId;
    }

    public String getEventName() {
        return getEvent().eventName();
    }

    public BuiltMixsonEvent<T> getEvent() {
        return entry.event();
    }

    public int getPriority() {
        return entry.priority();
    }

    public BuiltResourceReference<T> getReference(String id) {
        return this.references.get(ResourceLocation.parse(id));
    }

    public void markForDeletion(boolean shouldDelete) {
        this.markedForDeletion = shouldDelete;
    }

    public void createResource(ResourceLocation id, T elem) {
        if(creationType != ContextCreationType.IDENTIFIED) throw new IllegalCallerException(String.format("cannot created identified resources for event '%s'", getEventName()));
        this.identifiedCreatedResources.put(id, elem);
    }

    public void createResource(T elem) {
        if(creationType != ContextCreationType.INDEXED) throw new IllegalCallerException(String.format("cannot created indexed resources for event '%s'", getEventName()));
        this.indexedCreatedResources.add(elem);
    }

    protected HashMap<ResourceLocation, T> getIdentifiedCreatedResources() {
        return this.identifiedCreatedResources;
    }

    protected List<T> getIndexedCreatedResources() {
        return indexedCreatedResources;
    }

    public void cancelFutureEvent(UUID uuid) {
        this.cancelledFutures.add(uuid);
    }

    public void registerRuntimeEvent(int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        createdRuntimeEvents.add(new HexRecord<>(priority, resourceId, eventName, event, failSilently, references));
    }

    public void registerDualEvent(int priority, String resourceId, String eventName, MixsonEvent<T> event, boolean failSilently, ResourceReference... references) {
        createdEvents.add(new HexRecord<>(priority, resourceId, eventName, event, failSilently, references));
    }

    protected List<HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]>> getCreatedRuntimeEvents() {
        return createdRuntimeEvents;
    }

    protected List<HexRecord<Integer, String, String, MixsonEvent<T>, Boolean, ResourceReference[]>> getCreatedEvents() {
        return createdEvents;
    }


    protected boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    protected Set<UUID> getCancelledFutures() {
        return this.cancelledFutures;
    }

    public ContextCreationType getCreationType() {
        return creationType;
    }
}
