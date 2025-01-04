package net.ramixin.mixson;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.events.MixsonEventTypes;

import java.util.UUID;

public record AssociatedMixsonEvent(ResourceLocation resourceId, ResourceLocation eventId, MixsonEventTypes.BaseEvent<?> event, boolean silentlyFail, boolean referenceEvent, UUID... referenceIds) {
}
