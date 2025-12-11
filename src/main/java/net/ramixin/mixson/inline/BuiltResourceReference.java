package net.ramixin.mixson.inline;

import net.minecraft.resources.Identifier;
import net.ramixin.mixson.util.ErrorMessageProvider;

import java.util.Optional;
import java.util.UUID;

public class BuiltResourceReference<T> implements ErrorMessageProvider {

    private T resource;

    private final Identifier referenceId;

    private final Identifier resourceId;

    private final int ordinal;

    private final UUID uuid = UUID.randomUUID();

    private final MixsonCodec<T> codec;

    protected BuiltResourceReference(ResourceReference reference, MixsonCodec<T> codec) {
        if(reference.ordinal() == -1) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be -1", reference.referenceId()));
        if(reference.ordinal() < 0) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be negative", reference.referenceId()));
        this.resourceId = Identifier.parse(reference.resourceId());
        this.referenceId = Identifier.parse(reference.referenceId());
        this.ordinal = reference.ordinal();
        this.codec = codec;
    }

    public Optional<T> retrieve() {
        if(resource == null) return Optional.empty();
        return Optional.of(resource);
    }

    public void fulfill(T elem) {
        this.resource = elem;
    }

    public Identifier getReferenceId() {
        return referenceId;
    }

    public Identifier getResourceId() {
        return resourceId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public MixsonCodec<T> getCodec() {
        return codec;
    }

    @Override
    public String getRuntimeMessage(Identifier resourceId) {
        return String.format("Failed to capture %s file '%s' for reference '%s'\n", codec.extensionAndDot(), resourceId, referenceId);
    }

    @Override
    public String getRegistrationMessage() {
        return String.format("Failed to register reference '%s' for file '%s'\n", referenceId, resourceId);
    }

    @Override
    public boolean failSilently() {
        return false;
    }

    protected void clear() {
        resource = null;
    }
}
