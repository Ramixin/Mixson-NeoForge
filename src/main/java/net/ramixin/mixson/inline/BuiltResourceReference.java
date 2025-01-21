package net.ramixin.mixson.inline;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public class BuiltResourceReference implements ErrorMessageProvider {

    private JsonElement resource;

    private final ResourceLocation referenceId;

    private final ResourceLocation resourceId;

    private final int ordinal;

    private final UUID uuid = UUID.randomUUID();

    protected BuiltResourceReference(ResourceReference reference) {
        if(reference.ordinal() == -1) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be -1", reference.referenceId()));
        if(reference.ordinal() < 0) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be negative", reference.referenceId()));
        this.resourceId = ResourceLocation.parse(reference.resourceId()).withSuffix(".json");
        this.referenceId = ResourceLocation.parse(reference.referenceId());
        this.ordinal = reference.ordinal();
    }

    public Optional<JsonElement> retrieve() {
        if(resource == null) return Optional.empty();
        return Optional.of(resource);
    }

    protected void fulfill(JsonElement elem) {
        this.resource = elem.deepCopy();
    }

    public ResourceLocation getReferenceId() {
        return referenceId;
    }

    public ResourceLocation getResourceId() {
        return resourceId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public String getMessage() {
        return String.format("Failed to capture json file '%s' for reference '%s'\n", resourceId, referenceId);
    }

    @Override
    public boolean failSilently() {
        return false;
    }

    protected void clear() {
        resource = null;
    }
}
