package net.ramixin.mixson.inline;

import net.ramixin.mixson.inline.events.MixsonEvent;

import java.util.UUID;

public record BuiltMixsonEvent(UUID uuid, String resourceId, String eventName, MixsonEvent event, boolean silentlyFail, UUID... referenceIds) implements ErrorMessageProvider {

    public BuiltMixsonEvent(String resourceId, String eventId, MixsonEvent event, boolean silentlyFail, UUID... referenceIds) {
        this(UUID.randomUUID(), resourceId, eventId, event, silentlyFail, referenceIds);
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
