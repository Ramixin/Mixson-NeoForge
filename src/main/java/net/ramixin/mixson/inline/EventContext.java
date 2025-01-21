package net.ramixin.mixson.inline;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.HexRecord;
import net.ramixin.mixson.inline.events.MixsonEvent;

import java.util.*;

@SuppressWarnings("unused")
public class EventContext {

    private final ContextCreationType creationType;
    private final JsonElement file;
    private final ResourceLocation resourceId;
    private final EventEntry entry;
    private final HashMap<ResourceLocation, BuiltResourceReference> references = new HashMap<>();
    private boolean markedForDeletion;
    private final Set<UUID> cancelledFutures = new HashSet<>();
    private final HashMap<ResourceLocation, JsonElement> identifiedCreatedResources = new HashMap<>();
    private final List<JsonElement> indexedCreatedResources = new ArrayList<>();
    private final List<HexRecord<Integer, String, String, MixsonEvent, Boolean, ResourceReference[]>> createdRuntimeEvents = new ArrayList<>();
    private final List<HexRecord<Integer, String, String, MixsonEvent, Boolean, ResourceReference[]>> createdEvents = new ArrayList<>();

    protected EventContext(ContextCreationType creationType, JsonElement file, ResourceLocation resourceId, EventEntry entry, boolean markedForDeletion, BuiltResourceReference... references) {
        this.creationType = creationType;
        this.file = file;
        this.resourceId = resourceId;
        this.entry = entry;
        this.markedForDeletion = markedForDeletion;
        for(BuiltResourceReference ref : references) this.references.put(ref.getReferenceId(), ref);
    }

    public JsonElement getFile() {
        return this.file;
    }

    public ResourceLocation getResourceId() {
        return this.resourceId;
    }

    public String getEventName() {
        return getEvent().eventName();
    }

    public BuiltMixsonEvent getEvent() {
        return entry.event();
    }

    public int getPriority() {
        return entry.priority();
    }

    public BuiltResourceReference getReference(String id) {
        return this.references.get(ResourceLocation.parse(id));
    }

    public void markForDeletion(boolean shouldDelete) {
        this.markedForDeletion = shouldDelete;
    }

    public void createResource(ResourceLocation id, JsonElement elem) {
        if(creationType != ContextCreationType.IDENTIFIED) throw new IllegalCallerException(String.format("cannot created identified resources for event '%s'", getEventName()));
        this.identifiedCreatedResources.put(id, elem);
    }

    public void createResource(JsonElement elem) {
        if(creationType != ContextCreationType.INDEXED) throw new IllegalCallerException(String.format("cannot created indexed resources for event '%s'", getEventName()));
        this.indexedCreatedResources.add(elem);
    }

    protected HashMap<ResourceLocation, JsonElement> getIdentifiedCreatedResources() {
        return this.identifiedCreatedResources;
    }

    protected List<JsonElement> getIndexedCreatedResources() {
        return indexedCreatedResources;
    }

    public void cancelFutureEvent(UUID uuid) {
        this.cancelledFutures.add(uuid);
    }

    public void registerRuntimeEvent(int priority, String resourceId, String eventName, MixsonEvent event, boolean failSilently, ResourceReference... references) {
        createdRuntimeEvents.add(new HexRecord<>(priority, resourceId, eventName, event, failSilently, references));
    }

    public void registerDualEvent(int priority, String resourceId, String eventName, MixsonEvent event, boolean failSilently, ResourceReference... references) {
        createdEvents.add(new HexRecord<>(priority, resourceId, eventName, event, failSilently, references));
    }

    protected List<HexRecord<Integer, String, String, MixsonEvent, Boolean, ResourceReference[]>> getCreatedRuntimeEvents() {
        return createdRuntimeEvents;
    }

    protected List<HexRecord<Integer, String, String, MixsonEvent, Boolean, ResourceReference[]>> getCreatedEvents() {
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
