package net.ramixin.mixson.inline;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.util.ErrorMessageProvider;
import net.ramixin.mixson.util.MixsonUtil;
import net.ramixin.mixson.util.ResourceLocator;

import java.util.UUID;

public record BuiltMixsonEvent<T>(MixsonCodec<T> codec, UUID uuid, ResourceLocator resourceLocator, String eventName, MixsonEvent<T> event, boolean silentlyFail, boolean assertive, UUID... referenceIds) implements ErrorMessageProvider {

    public BuiltMixsonEvent(MixsonCodec<T> codec, ResourceLocator resourceLocator, String eventId, MixsonEvent<T> event, boolean silentlyFail, boolean assertive, UUID... referenceIds) {
        this(codec, UUID.randomUUID(), resourceLocator, eventId, event, silentlyFail, assertive, referenceIds);
    }

    public boolean isApplicable(ResourceLocation resourceId) {
        if(!resourceId.getPath().endsWith(codec().extensionAndDot())) return false;
        return resourceLocator.apply(MixsonUtil.removeExtension(resourceId));
    }

    @Override
    public String getRuntimeMessage(ResourceLocation resourceId) {
        return String.format("Failed to interact with %s file '%s' with event '%s'\n", codec.extensionAndDot(), resourceId, eventName);
    }

    @Override
    public String getRegistrationMessage() {
        return String.format("Failed to register event %s\n", eventName);
    }

    @Override
    public boolean failSilently() {
        return silentlyFail;
    }
}
