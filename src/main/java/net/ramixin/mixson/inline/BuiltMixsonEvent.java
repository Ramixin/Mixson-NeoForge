package net.ramixin.mixson.inline;

import net.ramixin.mixson.util.ErrorMessageProvider;

import java.util.UUID;

public record BuiltMixsonEvent<T>(MixsonCodec<T> codec, UUID uuid, String resourceId, String eventName, MixsonEvent<T> event, boolean silentlyFail, UUID... referenceIds) implements ErrorMessageProvider {

    public BuiltMixsonEvent(MixsonCodec<T> codec, String resourceId, String eventId, MixsonEvent<T> event, boolean silentlyFail, UUID... referenceIds) {
        this(codec, UUID.randomUUID(), resourceId, eventId, event, silentlyFail, referenceIds);
    }

    @Override
    public String getMessage() {
        return String.format("Failed to interact with json file '%s' with event '%s'\n", resourceId, eventName);
    }

    @Override
    public boolean failSilently() {
        return silentlyFail;
    }
}
