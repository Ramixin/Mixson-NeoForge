package net.ramixin.mixson.inline;

import net.ramixin.mixson.MixsonError;
import net.ramixin.mixson.util.ResourceLocator;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import static net.ramixin.mixson.inline.Mixson.logEventRegistration;
import static net.ramixin.mixson.inline.Mixson.registrationError;

public class MixsonEventBuilder<T> {

    private MixsonCodec<T> codec;
    private ResourceLocator resourceLocator;
    private String eventName;
    private MixsonEvent<T> event;
    private boolean silentlyFail = false;
    private boolean assertive = false;
    private ResourceReference[] references = new ResourceReference[0];

    public MixsonEventBuilder<T> setCodec(MixsonCodec<T> codec) {
        this.codec = codec;
        return this;
    }

    public MixsonEventBuilder<T> setResourceLocator(ResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
        return this;
    }

    public MixsonEventBuilder<T> setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public MixsonEventBuilder<T> setEvent(MixsonEvent<T> event) {
        this.event = event;
        return this;
    }

    public MixsonEventBuilder<T> setSilentlyFail(boolean silentlyFail) {
        this.silentlyFail = silentlyFail;
        return this;
    }

    public MixsonEventBuilder<T> setAssertive(boolean assertive) {
        this.assertive = assertive;
        return this;
    }

    public MixsonEventBuilder<T> setReferences(ResourceReference... references) {
        this.references = references;
        return this;
    }

    protected @NotNull BuiltMixsonEvent<T> build(int priority, BiConsumer<Integer, BuiltResourceReference<T>> referenceCallback) {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(resourceLocator, "resource locator must be set");
        Objects.requireNonNull(event, "event must be set");
        Objects.requireNonNull(eventName, "event name must be set");
        boolean fail = event.ordinal() < 0 && event.ordinal() != -1;
        logEventRegistration(eventName, priority);
        UUID[] referenceIds = new UUID[references.length];
        for (int i = 0, referencesLength = references.length; i < referencesLength; i++) {
            ResourceReference ref = references[i];
            BuiltResourceReference<T> builtReference = new BuiltResourceReference<>(ref, codec);
            referenceIds[i] = builtReference.getUuid();
            referenceCallback.accept(ref.priority(), builtReference);
        }

        BuiltMixsonEvent<T> builtEvent = new BuiltMixsonEvent<>(codec, resourceLocator, eventName, event, silentlyFail, assertive, referenceIds);
        if(fail) registrationError(new MixsonError("event ordinal value cannot be negative"), builtEvent);
        return builtEvent;
    }

    protected boolean hasDifferentCodec(MixsonCodec<T> codec) {
        return !this.codec.equals(codec);
    }

}
